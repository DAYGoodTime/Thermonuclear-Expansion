package com.lin.thermonuclear.common.machine.nuclear;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Formal module fragment: P = main-tier pipe, F = containment, D = main-tier axial fuel rod. */
final class NuclearModuleStructure {

    static final int MAX_MODULES = 5;
    static final String MODULE = "nuclear_module";
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
    // Both entrances share the same grid, including their common back-right neighbour.
    static final Position BACK = new Position(MAIN.max.x - WIDTH + 1, MAIN.min.y, MAIN.max.z + 1);
    static final Position RIGHT = new Position(MAIN.max.x + 1, MAIN.min.y, BACK.z - LENGTH);
    // spotless:off
    // Exact Normal Scan body from dev/nuc-child.txt, with A/B/C mapped to P/F/D.
    private static final String[][] TEMPLATE = {
        { "                    ", "                    ", "                    ", "                    ",
            "                    ", "                    ", "                    ", "                    ",
            "                    ", "                    ", "                    ", "                    ",
            "                    ", "                    ", "                    ", "                    ",
            "                    ", "                    ", "                    ", "                    ",
            "                    ", "                    ", "                    ", "    FFFFFFFFFFF     " },
        { "                    ", "                    ", "                    ", "                    ",
            "                    ", "                    ", "                    ", "                    ",
            "     FFFFFFFFF      ", "     FFFFFFFFF      ", "     FFFFFFFFF      ", "     FFFFFFFFF      ",
            "     FFFFFFFFF      ", "     FFFFFFFFF      ", "     FFFFFFFFF      ", "     FFFFFFFFF      ",
            "     FFFFFFFFF      ", "     FFFFFFFFF      ", "     FFFFFFFFF      ", "     FFFFFFFFF      ",
            "     FFFFFFFFF      ", "     FFFFFFFFF      ", "     FFFFFFFFF      ", "   FFFFFFFFFFFFF    " },
        { "                    ", "                    ", "                    ", "                    ",
            "                    ", "                    ", "                    ", "     FFFFFFFFF      ",
            "    F         F     ", "    F         F     ", "    F         F     ", "    F         F     ",
            "    F         F     ", "    F         F     ", "    F         F     ", "    F         F     ",
            "    F         F     ", "    F         F     ", "    F         F     ", "    F         F     ",
            "    F         F     ", "    F         F     ", "    F         F     ", "  FFFFPFFFFFPFFFF   " },
        { "                    ", "                    ", "                    ", "                    ",
            "                    ", "                    ", "     FFFFFFFFF      ", "    F         F     ",
            "   F           F    ", "   F           F    ", "   F           F    ", "   F           F    ",
            "   F           F    ", "   F           F    ", "   F           F    ", "   F           F    ",
            "   F           F    ", "   F           F    ", "   F           F    ", "   F           F    ",
            "   F           F    ", "   F           F    ", "   F           F    ", " FFFFFPFFFFFPFFFFF  " },
        { "                    ", "                    ", "                    ", "                    ",
            "                    ", "     FFFFFFFFF      ", "    F         F     ", "   F           F    ",
            "  F             F   ", "  F             F   ", "  F             F   ", "  F             F   ",
            "  F             F   ", "  F             F   ", "  F             F   ", "  F             F   ",
            "  F             F   ", "  F             F   ", "  F             F   ", "  F             F   ",
            "  F             F   ", "  F             F   ", "  F             F   ", "FFFFFFPFFFFFPFFFFFFF" },
        { "                    ", "                    ", "                    ", "                    ",
            "     FFFFFFFFF      ", "    F         F     ", "   F           F    ", "  F             F   ",
            " F               F  ", " F               F  ", " F               F  ", " F               F  ",
            " F               F  ", " F               F  ", " F               F  ", " F               F  ",
            " F               F  ", " F  FFFFFFFFFFF  F  ", " F  FFFFFFFFFFF  F  ", " F  FFFFFFFFFFF  F  ",
            " F  FFFFFFFFFFF  F  ", " F  FFFFFFFFFFF  F  ", " F  FFPFFFFFPFF  F  ", "FFFFFFPFFFFFPFFFFFFF" },
        { "                    ", "                    ", "                    ", "     FFFFFFFFF      ",
            "    F         F     ", "   F           F    ", "  F             F   ", " F               F  ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F   F         F   F ", "F   F         F   F ", "F   F         F   F ",
            "F   F         F   F ", "F   F         F   F ", "F   FFFFFFFFFFF   F ", "FFFFFFFFFFFFFFFFFFFF" },
        { "                    ", "                    ", "      FFFFFFF       ", "     F       F      ",
            "    F         F     ", "   F           F    ", "  F             F   ", " F               F  ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F   F         F   F ", "F   F         F   F ", "F   F         F   F ",
            "F   F         F   F ", "F   F         F   F ", "F   FFFFFFFFFFF   F ", "FFFFFFFFFFFFFFFFFFFF" },
        { "                    ", "       FFFFF        ", "      F     F       ", "     F       F      ",
            "    F         F     ", "   F           F    ", "  F             F   ", " F               F  ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F   F         F   F ", "F   F  D D D  F   F ", "F   F  D D D  F   F ",
            "F   F  D D D  F   F ", "F   F  D D D  F   F ", "F   FFFFFFFFFFF   F ", "FFFFFFFFFFFFFFFFFFFF" },
        { "        FFF         ", "       F   F        ", "      F     F       ", "     F       F      ",
            "    F         F     ", "   F           F    ", "  F             F   ", " F               F  ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F   F         F   F ", "F   F   D D   F   F ", "F   F   D D   F   F ",
            "F   F   D D   F   F ", "F   F   D D   F   F ", "F   FFFFFFFFFFF   F ", "FFFFFFFFFFFFFFFFFFFF" },
        { "        FFF         ", "       F   F        ", "      F     F       ", "     F       F      ",
            "    F         F     ", "   F           F    ", "  F             F   ", " F               F  ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F   F         F   F ", "F   F  D D D  F   F ", "F   F  D D D  F   F ",
            "F   F  D D D  F   F ", "F   F  D D D  F   F ", "F   FFFFFFFFFFF   F ", "FFFFFFFFFFFFFFFFFFFF" },
        { "        FFF         ", "       F   F        ", "      F     F       ", "     F       F      ",
            "    F         F     ", "   F           F    ", "  F             F   ", " F               F  ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F   F         F   F ", "F   F   D D   F   F ", "F   F   D D   F   F ",
            "F   F   D D   F   F ", "F   F   D D   F   F ", "F   FFFFFFFFFFF   F ", "FFFFFFFFFFFFFFFFFFFF" },
        { "                    ", "       FFFFF        ", "      F     F       ", "     F       F      ",
            "    F         F     ", "   F           F    ", "  F             F   ", " F               F  ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F   F         F   F ", "F   F  D D D  F   F ", "F   F  D D D  F   F ",
            "F   F  D D D  F   F ", "F   F  D D D  F   F ", "F   FFFFFFFFFFF   F ", "FFFFFFFFFFFFFFFFFFFF" },
        { "                    ", "                    ", "      FFFFFFF       ", "     F       F      ",
            "    F         F     ", "   F           F    ", "  F             F   ", " F               F  ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F   F         F   F ", "F   F         F   F ", "F   F         F   F ",
            "F   F         F   F ", "F   F         F   F ", "F   FFFFFFFFFFF   F ", "FFFFFFFFFFFFFFFFFFFF" },
        { "                    ", "                    ", "                    ", "     FFFFFFFFF      ",
            "    F         F     ", "   F           F    ", "  F             F   ", " F               F  ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F                 F ", "F                 F ", "F                 F ",
            "F                 F ", "F   F         F   F ", "F   F         F   F ", "F   F         F   F ",
            "F   F         F   F ", "F   F         F   F ", "F   FFFFFFFFFFF   F ", "FFFFFFFFFFFFFFFFFFFF" },
        { "                    ", "                    ", "                    ", "                    ",
            "     FFFFFFFFF      ", "    F         F     ", "   F           F    ", "  F             F   ",
            " F               F  ", " F               F  ", " F               F  ", " F               F  ",
            " F               F  ", " F               F  ", " F               F  ", " F               F  ",
            " F               F  ", " F  FFFFFFFFFFF  F  ", " F  FFFFFFFFFFF  F  ", " F  FFFFFFFFFFF  F  ",
            " F  FFFFFFFFFFF  F  ", " F  FFFFFFFFFFF  F  ", " F  FFFFFFFFPFF  F  ", "FFFFFFFFFFFFPFFFFFFF" },
        { "                    ", "                    ", "                    ", "                    ",
            "                    ", "     FFFFFFFFF      ", "    F         F     ", "   F           F    ",
            "  F             F   ", "  F             F   ", "  F             F   ", "  F             F   ",
            "  F             F   ", "  F             F   ", "  F             F   ", "  F             F   ",
            "  F             F   ", "  F             F   ", "  F             F   ", "  F             F   ",
            "  F             F   ", "  F             F   ", "  F             F   ", "FFFFFFFFFFFFPFFFFFF " },
        { "                    ", "                    ", "                    ", "                    ",
            "                    ", "                    ", "     FFFFFFFFF      ", "    F         F     ",
            "   F           F    ", "   F           F    ", "   F           F    ", "   F           F    ",
            "   F           F    ", "   F           F    ", "   F           F    ", "   F           F    ",
            "   F           F    ", "   F           F    ", "   F           F    ", "   F           F    ",
            "   F           F    ", "   F           F    ", "   F           F    ", " FFFFFFFFFFFPFFFFF  " },
        { "                    ", "                    ", "                    ", "                    ",
            "                    ", "                    ", "                    ", "     FFFFFFFFF      ",
            "    F         F     ", "    F         F     ", "    F         F     ", "    F         F     ",
            "    F         F     ", "    F         F     ", "    F         F     ", "    F         F     ",
            "    F         F     ", "    F         F     ", "    F         F     ", "    F         F     ",
            "    F         F     ", "    F         F     ", "    F         F     ", "  FFFFFFFFFFPFFFF   " },
        { "                    ", "                    ", "                    ", "                    ",
            "                    ", "                    ", "                    ", "                    ",
            "     FFFFFFFFF      ", "     FFFFFFFFF      ", "     FFFFFFFFF      ", "     FFFFFFFFF      ",
            "     FFFFFFFFF      ", "     FFFFFFFFF      ", "     FFFFFFFFF      ", "     FFFFFFFFF      ",
            "     FFFFFFFFF      ", "     FFFFFFFFF      ", "     FFFFFFFFF      ", "     FFFFFFFFF      ",
            "     FFFFFFFFF      ", "     FFFFFFFFF      ", "     FFFFFFFFF      ", "   FFFFFFFFFFFFF    " } };
    // spotless:on
    private static final List<Position> FUEL_RODS = createFuelRods();

    private NuclearModuleStructure() {}

    static int moduleLimit(int pipeTier) {
        // Retain the established pipe-tier module budget, not a budget for removed connection segments.
        return switch (pipeTier) {
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> MAX_MODULES;
            default -> 0;
        };
    }

    static String[][] shape() {
        return TEMPLATE;
    }

    private static List<Position> createFuelRods() {
        if (TEMPLATE.length != LENGTH) throw new IllegalStateException("Module slice count");
        List<Position> rods = new ArrayList<>();
        for (int z = 0; z < TEMPLATE.length; z++) {
            if (TEMPLATE[z].length != HEIGHT) throw new IllegalStateException("Module height");
            for (int y = 0; y < TEMPLATE[z].length; y++) {
                if (TEMPLATE[z][y].length() != WIDTH) throw new IllegalStateException("Module width");
                for (int x = 0; x < TEMPLATE[z][y].length(); x++) {
                    char material = TEMPLATE[z][y].charAt(x);
                    if (material == ' ') continue;
                    if (material != 'P' && material != 'F' && material != 'D')
                        throw new IllegalStateException("Module material");
                    if (material == 'D') rods.add(new Position(x, y, z));
                }
            }
        }
        return java.util.Collections.unmodifiableList(rods);
    }

    static List<Position> fuelRods() {
        return FUEL_RODS;
    }

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

    /** Controller-relative StructureLib ABC coordinates (right, down, back). */
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
