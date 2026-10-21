package com.lin.thermonuclear.machine.nuclear;

import java.util.Arrays;
import java.util.List;

/** Integer block bounds in controller-relative StructureLib ABC (right, down, back). */
final class NuclearModuleGeometry {

    static final int WIDTH = 20;
    static final int HEIGHT = 24;
    static final int LENGTH = 20;
    static final Bounds MAIN = new Bounds(
        new Position(
            -NuclearPowerPlantStructure.OFFSET_X,
            -NuclearPowerPlantStructure.OFFSET_Y,
            -NuclearPowerPlantStructure.OFFSET_Z),
        NuclearPowerPlantStructure.WIDTH,
        NuclearPowerPlantStructure.HEIGHT,
        NuclearPowerPlantStructure.LENGTH);
    // Align the back module with the main core, and the right module with its depth.
    // Both roots belong to the same 20-block grid, including the shared back-right neighbour.
    static final Position BACK = new Position(MAIN.max.x - WIDTH + 1, MAIN.min.y, MAIN.max.z + 1);
    static final Position RIGHT = new Position(MAIN.max.x + 1, MAIN.min.y, BACK.z - LENGTH);

    private NuclearModuleGeometry() {}

    static List<Position> roots() {
        return Arrays.asList(BACK, RIGHT);
    }

    static Bounds bounds(Position origin) {
        return new Bounds(origin, WIDTH, HEIGHT, LENGTH);
    }

    static List<Position> neighbours(Position origin) {
        return Arrays.asList(
            origin.add(0, 0, LENGTH),
            origin.add(-WIDTH, 0, 0),
            origin.add(WIDTH, 0, 0),
            origin.add(0, 0, -LENGTH));
    }

    static List<Position> preview(int extra) {
        return switch (Math.max(1, Math.min(3, extra))) {
            case 1 -> java.util.Collections.emptyList();
            case 2 -> Arrays.asList(BACK);
            default -> Arrays.asList(BACK, BACK.add(0, 0, LENGTH));
        };
    }

    static final class Bounds {

        final Position min;
        final Position max;

        Bounds(Position origin, int width, int height, int length) {
            min = origin;
            max = origin.add(width - 1, height - 1, length - 1);
        }

        boolean intersects(Bounds other) {
            return min.x <= other.max.x && max.x >= other.min.x
                && min.y <= other.max.y
                && max.y >= other.min.y
                && min.z <= other.max.z
                && max.z >= other.min.z;
        }
    }

    static final class Position {

        final int x;
        final int y;
        final int z;

        Position(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Position add(int dx, int dy, int dz) {
            return new Position(x + dx, y + dy, z + dz);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Position position)) return false;
            return x == position.x && y == position.y && z == position.z;
        }

        @Override
        public int hashCode() {
            return (x * 31 + y) * 31 + z;
        }
    }
}
