package com.lin.thermonuclear.machine;

import java.util.Arrays;

/** Geometry of dev/nulcearStructure.txt; the annex controller faces export -Z. */
final class NuclearPowerPlantStructure {

    static final int WIDTH = 29;
    static final int HEIGHT = 24;
    static final int LENGTH = 21;
    static final int OFFSET_X = 2;
    static final int OFFSET_Y = 20;
    static final int OFFSET_Z = 7;
    // ceil(hypot(28 - OFFSET_X, 20 - OFFSET_Z)), including rotated/flipped horizontal facings.
    static final int CHUNK_RADIUS = 30;
    private static final int CENTRE = 18;
    private static final String EMPTY = "                             ";
    private static final String FULL_FLOOR = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
    private static final String FRONT_FLOOR = "      FFFFFFFFFFFFFFFFFFFFFFF";
    private static final String BACK_FLOOR = "        FFFFFFFFFFFFFFFFFFFFF";
    private static final String CAP = "              FFFFFFFFF      ";
    private static final String CORE_BASE = "          F  FFFFFFFFFFF  F  ";
    private static final String CORE_WATER = "         F   FGGGGGGGGGF   F ";
    private static final String CORE_FLOOR = "         F   FFFFFFFFFFF   F ";

    private NuclearPowerPlantStructure() {}

    static String[][] createShape() {
        String[][] shape = new String[LENGTH][];
        shape[0] = layer("               F     F       ", "              FAF   FAF      ", FRONT_FLOOR);
        shape[1] = solidCapLayer(FRONT_FLOOR);
        shape[1][22] = "              FCFFFFFCF      ";
        shape[1][23] = "      FFFFFFFFFCFFFFFCFFFFFFF";
        for (int z = 2; z <= 18; z++) {
            int top = Math.max(0, Math.max(9 - z, z - 11));
            int capRadius = Math.min(4, top + 1);
            int wallRadius = Math.min(9, Math.min(z + 3, 23 - z));
            shape[z] = dome(top, capRadius, wallRadius, z < 6 ? FRONT_FLOOR : z <= 14 ? FULL_FLOOR : BACK_FLOOR);
            if (z <= 5) shape[z][23] = "      FFFFFFFFFCFFFFFCFFFFFFF";
        }
        for (int z : new int[] { 5, 15 }) {
            Arrays.fill(shape[z], 17, 23, CORE_BASE);
        }
        shape[5][22] = "          F  FFCFFFFFCFF  F  ";
        for (int z = 6; z <= 14; z++) {
            // The export's glass roof E is now source water, identical to G.
            Arrays.fill(shape[z], 17, 22, CORE_WATER);
            shape[z][22] = CORE_FLOOR;
        }
        for (int z = 8; z <= 12; z++) {
            String rods = z % 2 == 0 ? "         F   FGGDGDGDGGF   F " : "         F   FGGGDGDGGGF   F ";
            Arrays.fill(shape[z], 18, 22, rods);
        }
        for (int z = 7; z <= 13; z++) {
            shape[z][18] = " FFFFFFFFF" + shape[z][18].substring(10);
            for (int y = 19; y <= 21; y++) {
                String annex = z == 7 || z == 13 ? " FBBBBBBFF" : " B       F";
                shape[z][y] = annex + shape[z][y].substring(10);
            }
            shape[z][22] = (z == 7 || z == 13 ? " FFFFFFFFF" : " F       F") + shape[z][22].substring(10);
        }
        // H is the real controller; the offset header belongs to the scanner outside the build.
        shape[OFFSET_Z][OFFSET_Y] = " F~BBBBBFF   FGGGGGGGGGF   F ";
        shape[19] = solidCapLayer(BACK_FLOOR);
        shape[20] = layer(BACK_FLOOR);
        for (String[] slice : shape) {
            for (String row : slice) {
                if (row.length() != WIDTH) throw new IllegalStateException("Invalid nuclear structure row width");
            }
        }
        return shape;
    }

    private static String[] layer(String... bottomRows) {
        String[] rows = new String[HEIGHT];
        Arrays.fill(rows, EMPTY);
        System.arraycopy(bottomRows, 0, rows, HEIGHT - bottomRows.length, bottomRows.length);
        return rows;
    }

    private static String[] solidCapLayer(String floor) {
        String[] rows = layer(floor);
        Arrays.fill(rows, 8, 23, CAP);
        return rows;
    }

    private static String[] dome(int top, int capRadius, int wallRadius, String floor) {
        String[] rows = layer(floor);
        char[] cap = EMPTY.toCharArray();
        Arrays.fill(cap, CENTRE - capRadius, CENTRE + capRadius + 1, 'F');
        rows[top] = new String(cap);
        for (int y = top + 1; y < HEIGHT - 1; y++) {
            int radius = Math.min(wallRadius, capRadius + y - top);
            char[] wall = EMPTY.toCharArray();
            wall[CENTRE - radius] = wall[CENTRE + radius] = 'F';
            rows[y] = new String(wall);
        }
        return rows;
    }
}
