package com.lin.thermonuclear.machine;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

import com.lin.thermonuclear.machine.NuclearModuleStructure.Position;
import com.lin.thermonuclear.machine.NuclearModuleStructure.RequiredBlock;

/** One synchronous, disposable scan. It never calls A's hatch/structure checker. */
final class NuclearModuleSearch {

    static final long TIMEOUT_NANOS = 10_000_000_000L;

    interface Reader {

        boolean loaded(Position position);

        boolean matches(RequiredBlock block);
    }

    static final class Result {

        int modules;
        int candidates;
        int edges;
        int reads;
        int unloaded;
        long elapsedNanos;
        boolean timedOut;
        boolean capped;
    }

    private final Reader reader;
    private final LongSupplier clock;
    private final int moduleLimit;
    private final Set<Position> occupied;
    private final Map<Position, Boolean> bodies = new HashMap<>();
    private final Set<Position> valid = new HashSet<>();
    private final Result result = new Result();
    private long started;

    NuclearModuleSearch(Reader reader, Set<Position> mainBlocks, LongSupplier clock, int moduleLimit) {
        if (moduleLimit < 0 || moduleLimit > NuclearModuleStructure.MAX_MODULES)
            throw new IllegalArgumentException("Invalid module limit: " + moduleLimit);
        this.reader = reader;
        this.occupied = new HashSet<>(mainBlocks);
        this.clock = clock;
        this.moduleLimit = moduleLimit;
    }

    Result scan() {
        started = clock.getAsLong();
        try {
            if (moduleLimit == 0) return result;
            result.edges++;
            if (!check(NuclearModuleStructure.entrance()) || !body(NuclearModuleStructure.FIRST)) return result;
            ArrayDeque<Position> queue = new ArrayDeque<>();
            accept(NuclearModuleStructure.FIRST, queue);
            if (valid.size() == moduleLimit) {
                result.capped = true;
                return result;
            }
            Set<Set<Position>> edges = new HashSet<>();
            while (!queue.isEmpty()) {
                deadline();
                Position from = queue.removeFirst();
                for (int[] direction : NuclearModuleStructure.DIRECTIONS) {
                    deadline();
                    Position to = from.add(direction[0] * 8, 0, direction[1] * 8);
                    Set<Position> edge = new HashSet<>();
                    edge.add(from);
                    edge.add(to);
                    if (!edges.add(edge)) continue;
                    result.edges++;
                    // A is never a second root or a module; module occupancy rejects its required blocks.
                    if (!check(NuclearModuleStructure.connection(from, direction[0], direction[1]))) continue;
                    if (valid.contains(to) || !body(to)) continue;
                    accept(to, queue);
                    deadline();
                    if (valid.size() == moduleLimit) {
                        result.capped = true;
                        return result;
                    }
                }
            }
            return result;
        } catch (Timeout ignored) {
            result.timedOut = true;
            // A timeout carries diagnostics, never a partially usable level.
            result.modules = 0;
            return result;
        } finally {
            result.elapsedNanos = clock.getAsLong() - started;
        }
    }

    private void accept(Position centre, ArrayDeque<Position> queue) {
        valid.add(centre);
        for (RequiredBlock block : NuclearModuleStructure.blocks(centre)) occupied.add(block.position);
        queue.addLast(centre);
        result.modules = valid.size();
    }

    private boolean body(Position centre) {
        deadline();
        Boolean cached = bodies.get(centre);
        if (cached != null) return cached;
        result.candidates++;
        List<RequiredBlock> blocks = NuclearModuleStructure.blocks(centre);
        boolean complete = true;
        for (RequiredBlock block : blocks) {
            deadline();
            if (occupied.contains(block.position)) {
                complete = false;
                break;
            }
        }
        complete = complete && check(blocks);
        bodies.put(centre, complete);
        return complete;
    }

    private boolean check(List<RequiredBlock> blocks) {
        // Check every required chunk before the first world block read in this batch.
        for (RequiredBlock block : blocks) {
            deadline();
            if (!reader.loaded(block.position)) {
                result.unloaded++;
                return false;
            }
        }
        for (RequiredBlock block : blocks) {
            deadline();
            result.reads++;
            if (!reader.matches(block)) return false;
        }
        deadline();
        return true;
    }

    private void deadline() {
        if (clock.getAsLong() - started >= TIMEOUT_NANOS) throw new Timeout();
    }

    private static final class Timeout extends RuntimeException {

        private static final long serialVersionUID = 1L;

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }
}
