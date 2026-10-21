package com.lin.thermonuclear.machine.nuclear;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.LongSupplier;

import com.lin.thermonuclear.machine.nuclear.NuclearModuleGeometry.Bounds;
import com.lin.thermonuclear.machine.nuclear.NuclearModuleGeometry.Position;
import com.lin.thermonuclear.machine.nuclear.NuclearModuleStructure.RequiredBlock;

/** Disposable BFS: only a complete, loaded, matching body may become a connecting node. */
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
        final Set<Position> valid = new LinkedHashSet<>();
    }

    private final Reader reader;
    private final LongSupplier clock;
    private final int moduleLimit;
    private final Set<Position> visited = new HashSet<>();
    private final Result result = new Result();
    private long started;

    NuclearModuleSearch(Reader reader, LongSupplier clock, int moduleLimit) {
        if (moduleLimit < 0 || moduleLimit > NuclearModuleStructure.MAX_MODULES)
            throw new IllegalArgumentException("Invalid module limit: " + moduleLimit);
        this.reader = reader;
        this.clock = clock;
        this.moduleLimit = moduleLimit;
    }

    Result scan() {
        started = clock.getAsLong();
        try {
            if (moduleLimit == 0) return result;
            ArrayDeque<Position> queue = new ArrayDeque<>();
            // Seed both entrances before expanding either branch; the global budget still wins immediately.
            for (Position root : NuclearModuleGeometry.roots()) {
                if (body(root)) accept(root, queue);
                if (result.capped) return result;
            }
            while (!queue.isEmpty()) {
                deadline();
                for (Position to : NuclearModuleGeometry.neighbours(queue.removeFirst())) {
                    result.edges++;
                    if (body(to)) accept(to, queue);
                    if (result.capped) return result;
                }
            }
            return result;
        } catch (Timeout ignored) {
            result.timedOut = true;
            result.modules = 0;
            result.valid.clear();
            return result;
        } finally {
            result.elapsedNanos = clock.getAsLong() - started;
        }
    }

    private void accept(Position origin, ArrayDeque<Position> queue) {
        deadline();
        result.valid.add(origin);
        queue.addLast(origin);
        result.modules = result.valid.size();
        result.capped = result.modules == moduleLimit;
    }

    private boolean body(Position origin) {
        deadline();
        if (!visited.add(origin)) return false;
        Bounds bounds = NuclearModuleGeometry.bounds(origin);
        if (bounds.intersects(NuclearModuleGeometry.MAIN)) return false;
        for (Position valid : result.valid) {
            if (bounds.intersects(NuclearModuleGeometry.bounds(valid))) return false;
        }
        result.candidates++;
        // Preflight the entire bounding box, including ignored air, without loading chunks.
        for (int z = bounds.min.z; z <= bounds.max.z; z++) {
            for (int x = bounds.min.x; x <= bounds.max.x; x++) {
                for (int y : new int[] { bounds.min.y, bounds.max.y }) {
                    deadline();
                    if (!reader.loaded(new Position(x, y, z))) {
                        result.unloaded++;
                        return false;
                    }
                }
            }
        }
        for (RequiredBlock block : NuclearModuleStructure.blocks()) {
            deadline();
            Position local = block.position;
            result.reads++;
            if (!reader.matches(new RequiredBlock(origin.add(local.x, local.y, local.z), block.material))) return false;
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
