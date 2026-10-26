package com.lin.thermonuclear.common.machine.nuclear;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.LongSupplier;

import com.lin.thermonuclear.common.machine.nuclear.NuclearModuleStructure.Bounds;
import com.lin.thermonuclear.common.machine.nuclear.NuclearModuleStructure.Position;

/** Disposable BFS: only a complete, loaded, matching body may become a connecting node. */
final class NuclearModuleSearch {

    static final long TIMEOUT_NANOS = 10_000_000_000L;

    interface Reader {

        boolean loaded(Position position);

        // Invoke beforeRead for every visited element, including unloaded rejections, to enforce the scan deadline.
        boolean matches(Position origin, Runnable beforeRead, Runnable onUnloaded);
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
            for (Position root : NuclearModuleStructure.roots()) {
                if (body(root)) accept(root, queue);
                if (result.capped) return result;
            }
            while (!queue.isEmpty()) {
                deadline();
                for (Position to : NuclearModuleStructure.neighbours(queue.removeFirst())) {
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
        Bounds bounds = NuclearModuleStructure.bounds(origin);
        if (bounds.intersects(NuclearModuleStructure.MAIN)) return false;
        for (Position valid : result.valid) {
            if (bounds.intersects(NuclearModuleStructure.bounds(valid))) return false;
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
        boolean matched = reader.matches(origin, () -> {
            deadline();
            result.reads++;
        }, () -> result.unloaded++);
        deadline();
        return matched;
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
