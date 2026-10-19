package com.lin.thermonuclear.machine.nuclear;

import java.util.ArrayList;
import java.util.List;

/** All positions use StructureLib ABC: right, down, back, relative to A's controller. */
final class NuclearModuleStructure {

    static final int MAX_MODULES = 5;
    static final Position FIRST = new Position(19, 2, 19);
    static final Position[] EXAMPLE = { FIRST, FIRST.add(-8, 0, 0), FIRST.add(0, 0, 8) };
    static final int[][] DIRECTIONS = { { 0, 1 }, { -1, 0 }, { 1, 0 }, { 0, -1 } };
    static final String MODULE = "nuclear_module";
    static final String PIPE_X = "nuclear_connection_x";
    static final String PIPE_Z = "nuclear_connection_z";
    private static final String[][] TEMPLATE = { { "  K  ", " K K ", "FFFFF" }, { "     ", "K   K", "FFFFF" },
        { "K   K", "     ", "FFFFF" }, { "     ", "K   K", "FFFFF" }, { "  K  ", " K K ", "FFFFF" } };

    private NuclearModuleStructure() {}

    static int moduleLimit(int pipeTier) {
        return switch (pipeTier) {
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> MAX_MODULES;
            default -> 0;
        };
    }

    static String[][] shape() {
        String[][] copy = new String[TEMPLATE.length][];
        for (int i = 0; i < copy.length; i++) copy[i] = TEMPLATE[i].clone();
        return copy;
    }

    static List<RequiredBlock> blocks(Position centre) {
        List<RequiredBlock> blocks = new ArrayList<>();
        for (int z = 0; z < TEMPLATE.length; z++) {
            for (int y = 0; y < TEMPLATE[z].length; y++) {
                for (int x = 0; x < TEMPLATE[z][y].length(); x++) {
                    char material = TEMPLATE[z][y].charAt(x);
                    if (material != ' ') blocks.add(new RequiredBlock(centre.add(x - 2, y - 1, z - 2), material));
                }
            }
        }
        return blocks;
    }

    static List<RequiredBlock> connection(Position from, int dx, int dz) {
        List<RequiredBlock> blocks = new ArrayList<>();
        // Endpoints occupy the two ignored holes; no surrounding-air restriction.
        for (int i = 2; i <= 6; i++) blocks.add(new RequiredBlock(from.add(dx * i, 0, dz * i), 'L'));
        return blocks;
    }

    static List<RequiredBlock> entrance() {
        return connection(FIRST.add(0, 0, -8), 0, 1);
    }

    static final class RequiredBlock {

        final Position position;
        final char material;

        RequiredBlock(Position position, char material) {
            this.position = position;
            this.material = material;
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
