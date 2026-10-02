package com.lin.thermonuclear.machine;

import java.util.Arrays;

/** Geometry of dev/nulcearStructure.txt, rotated so the controller faces out of its side wall. */
final class NuclearPowerPlantStructure {

    private static final int EXPORT_WIDTH = 27;
    private static final int EXPORT_LENGTH = 32;
    private static final int EXPORT_CONTROLLER_X = 15;
    private static final int EXPORT_CONTROLLER_Z = 4;
    static final int WIDTH = EXPORT_LENGTH;
    static final int HEIGHT = 25;
    static final int LENGTH = EXPORT_WIDTH;
    static final int OFFSET_X = EXPORT_CONTROLLER_Z;
    static final int OFFSET_Y = 21;
    static final int OFFSET_Z = EXPORT_WIDTH - 1 - EXPORT_CONTROLLER_X;
    // Covers the horizontal bounding rectangle even when the controller is rotated or flipped.
    static final int CHUNK_RADIUS = 31;
    private static final int CENTRE = 12;
    private static final String EMPTY = "                           ";
    private static final String FLOOR = "CCCCCCCCCCCCCCCCCCCCCCCCCCC";
    private static final String NECK_CAP = "         CCCCCCC           ";
    private static final String NECK_WALL = "         C     C           ";
    private static final String NECK_HATCH = "         C     G           ";
    private static final String NECK_FLOOR = "        CCCCCCCCC          ";
    private static final String CORE_GLASS = "   C   CBBBBBBBBBC   C     ";
    private static final String CORE_FLOOR = "   C   CCCCCCCCCCC   C     ";
    private static final String PIPE_FLOOR = "   C   CCCCCCCCCCAAAAAAAAAI";
    private static final String PIPE_WATER = "   C   CHHHHHHHHHCCCCCCCCCC";
    private static final String WET_CORE = "   C   CFFFFFFFFFC   C     ";
    private static final String CORE_WATER = "   C   CHHHHHHHHHC   C     ";
    private static final String LONG_CORE_FLOOR = "   C   CCCCCCCCCCCCCCCCCCCC";
    private static final String CORE_BASE = "    C  CCCCCCCCCCC  C      ";

    private NuclearPowerPlantStructure() {}

    static String[][] createShape() {
        String[][] shape = new String[EXPORT_LENGTH][];
        shape[0] = layer();
        shape[1] = layer(NECK_FLOOR);
        shape[2] = layer(NECK_CAP, NECK_CAP, NECK_CAP, NECK_CAP, NECK_CAP, NECK_FLOOR);
        for (int depth = 3; depth <= 8; depth++) {
            shape[depth] = layer(
                NECK_CAP,
                NECK_HATCH,
                NECK_HATCH,
                NECK_HATCH,
                NECK_WALL,
                depth < 7 ? NECK_FLOOR : FLOOR);
        }
        // The export's E is the controller, not the scanner used to produce its offset header.
        shape[EXPORT_CONTROLLER_Z][OFFSET_Y] = "         C     ~           ";
        shape[9] = layer(NECK_CAP, NECK_WALL, NECK_WALL, NECK_WALL, NECK_WALL, FLOOR);
        shape[10] = solidCapLayer();
        shape[11] = dome(8, 4, 5, FLOOR);
        shape[12] = dome(7, 4, 6, FLOOR);
        shape[13] = dome(6, 4, 7, FLOOR);
        shape[14] = dome(5, 4, 8, CORE_BASE, CORE_BASE, CORE_BASE, CORE_BASE, CORE_BASE, CORE_BASE, FLOOR);
        shape[15] = dome(4, 4, 9, CORE_GLASS, WET_CORE, WET_CORE, WET_CORE, CORE_WATER, LONG_CORE_FLOOR, FLOOR);
        shape[16] = dome(3, 3, 9, CORE_GLASS, WET_CORE, WET_CORE, WET_CORE, PIPE_WATER, PIPE_FLOOR, FLOOR);
        String frameRow = "   C   CFFDFDFDFFC   C     ";
        String frameWater = "   C   CHHDHDHDHHC   C     ";
        shape[17] = dome(2, 2, 9, CORE_GLASS, frameRow, frameRow, frameRow, frameWater, LONG_CORE_FLOOR, FLOOR);
        String innerFrameRow = "   C   CFFFD DFFFC   C     ";
        String innerFrameWater = "   C   CHHHD DHHHC   C     ";
        shape[18] = dome(
            1,
            1,
            9,
            CORE_GLASS,
            innerFrameRow,
            innerFrameRow,
            innerFrameRow,
            innerFrameWater,
            CORE_FLOOR,
            FLOOR);
        String centreFrameRow = "   C   CFFD D DFFC   C     ";
        String centreFrameWater = "   C   CHHD D DHHC   C     ";
        shape[19] = dome(
            1,
            1,
            9,
            CORE_GLASS,
            centreFrameRow,
            centreFrameRow,
            centreFrameRow,
            centreFrameWater,
            CORE_FLOOR,
            FLOOR);
        shape[20] = shape[18].clone();
        shape[21] = shape[17].clone();
        shape[22] = shape[16].clone();
        shape[23] = dome(
            4,
            4,
            9,
            CORE_GLASS,
            WET_CORE,
            WET_CORE,
            WET_CORE,
            CORE_WATER,
            "   C   CCCCCCCCCCC   CCCCCC",
            FLOOR);
        shape[24] = shape[14].clone();
        shape[25] = shape[13].clone();
        shape[26] = shape[12].clone();
        shape[27] = shape[11].clone();
        shape[28] = shape[10].clone();
        for (int depth = 29; depth < EXPORT_LENGTH; depth++) shape[depth] = layer(FLOOR);
        for (String[] slice : shape) {
            for (String row : slice) {
                if (row.length() != EXPORT_WIDTH)
                    throw new IllegalStateException("Invalid nuclear structure row width");
            }
        }
        return faceControllerOutward(shape);
    }

    private static String[][] faceControllerOutward(String[][] exported) {
        String[][] rotated = new String[LENGTH][HEIGHT];
        // Export +X is the wall's outside. Map it to -depth, the controller's front, without mirroring.
        for (int depth = 0; depth < LENGTH; depth++) {
            int exportX = EXPORT_WIDTH - 1 - depth;
            for (int row = 0; row < HEIGHT; row++) {
                char[] blocks = new char[WIDTH];
                for (int x = 0; x < WIDTH; x++) blocks[x] = exported[x][row].charAt(exportX);
                rotated[depth][row] = new String(blocks);
            }
        }
        return rotated;
    }

    private static String[] layer(String... bottomRows) {
        String[] rows = new String[HEIGHT];
        Arrays.fill(rows, EMPTY);
        System.arraycopy(bottomRows, 0, rows, HEIGHT - bottomRows.length, bottomRows.length);
        return rows;
    }

    private static String[] solidCapLayer() {
        String[] rows = layer(FLOOR);
        Arrays.fill(rows, 9, 24, NECK_FLOOR);
        return rows;
    }

    private static String[] dome(int top, int capRadius, int wallRadius, String... bottomRows) {
        String[] rows = layer(bottomRows);
        char[] cap = EMPTY.toCharArray();
        Arrays.fill(cap, CENTRE - capRadius, CENTRE + capRadius + 1, 'C');
        rows[top] = new String(cap);
        for (int y = top + 1; y < HEIGHT - bottomRows.length; y++) {
            int radius = Math.min(wallRadius, capRadius + y - top);
            char[] wall = EMPTY.toCharArray();
            wall[CENTRE - radius] = wall[CENTRE + radius] = 'C';
            rows[y] = new String(wall);
        }
        return rows;
    }
}
