package com.lin.thermonuclear.machine.nuclear;

/** Exact 20 x 24 x 20 Normal Scan body from dev/nuc-child.txt; no debug controller or added slice. */
final class NuclearModuleShape {

    private NuclearModuleShape() {}

    static String[][] create() {
        // Draft A/B/C are explicitly remapped to formal P/F/D; repeated rows are dictionary-compressed only.
        String[] rows = { "                    ", "    FFFFFFFFFFF     ", "     FFFFFFFFF      ",
            "   FFFFFFFFFFFFF    ", "    F         F     ", "  FFFFPFFFFFPFFFF   ", "   F           F    ",
            " FFFFFPFFFFFPFFFFF  ", "  F             F   ", "FFFFFFPFFFFFPFFFFFFF", " F               F  ",
            " F  FFFFFFFFFFF  F  ", " F  FFPFFFFFPFF  F  ", "F                 F ", "F   F         F   F ",
            "F   FFFFFFFFFFF   F ", "FFFFFFFFFFFFFFFFFFFF", "      FFFFFFF       ", "     F       F      ",
            "       FFFFF        ", "      F     F       ", "F   F  D D D  F   F ", "        FFF         ",
            "       F   F        ", "F   F   D D   F   F ", " F  FFFFFFFFPFF  F  ", "FFFFFFFFFFFFPFFFFFFF",
            "FFFFFFFFFFFFPFFFFFF ", " FFFFFFFFFFFPFFFFF  ", "  FFFFFFFFFFPFFFF   " };
        // spotless:off
        int[][] slices = {
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3 },
            { 0, 0, 0, 0, 0, 0, 0, 2, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5 },
            { 0, 0, 0, 0, 0, 0, 2, 4, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7 },
            { 0, 0, 0, 0, 0, 2, 4, 6, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 9 },
            { 0, 0, 0, 0, 2, 4, 6, 8, 10, 10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 12, 9 },
            { 0, 0, 0, 2, 4, 6, 8, 10, 13, 13, 13, 13, 13, 13, 13, 13, 13, 14, 14, 14, 14, 14, 15, 16 },
            { 0, 0, 17, 18, 4, 6, 8, 10, 13, 13, 13, 13, 13, 13, 13, 13, 13, 14, 14, 14, 14, 14, 15, 16 },
            { 0, 19, 20, 18, 4, 6, 8, 10, 13, 13, 13, 13, 13, 13, 13, 13, 13, 14, 21, 21, 21, 21, 15, 16 },
            { 22, 23, 20, 18, 4, 6, 8, 10, 13, 13, 13, 13, 13, 13, 13, 13, 13, 14, 24, 24, 24, 24, 15, 16 },
            { 22, 23, 20, 18, 4, 6, 8, 10, 13, 13, 13, 13, 13, 13, 13, 13, 13, 14, 21, 21, 21, 21, 15, 16 },
            { 22, 23, 20, 18, 4, 6, 8, 10, 13, 13, 13, 13, 13, 13, 13, 13, 13, 14, 24, 24, 24, 24, 15, 16 },
            { 0, 19, 20, 18, 4, 6, 8, 10, 13, 13, 13, 13, 13, 13, 13, 13, 13, 14, 21, 21, 21, 21, 15, 16 },
            { 0, 0, 17, 18, 4, 6, 8, 10, 13, 13, 13, 13, 13, 13, 13, 13, 13, 14, 14, 14, 14, 14, 15, 16 },
            { 0, 0, 0, 2, 4, 6, 8, 10, 13, 13, 13, 13, 13, 13, 13, 13, 13, 14, 14, 14, 14, 14, 15, 16 },
            { 0, 0, 0, 0, 2, 4, 6, 8, 10, 10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 25, 26 },
            { 0, 0, 0, 0, 0, 2, 4, 6, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 27 },
            { 0, 0, 0, 0, 0, 0, 2, 4, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 28 },
            { 0, 0, 0, 0, 0, 0, 0, 2, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 29 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3 }
        };
        // spotless:on
        String[][] shape = new String[slices.length][];
        for (int z = 0; z < slices.length; z++) {
            shape[z] = new String[slices[z].length];
            for (int y = 0; y < slices[z].length; y++) shape[z][y] = rows[slices[z][y]];
        }
        return shape;
    }
}
