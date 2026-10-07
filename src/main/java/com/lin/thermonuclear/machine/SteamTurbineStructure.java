package com.lin.thermonuclear.machine;

import java.util.Arrays;

/** Geometry from dev/SteamTurbineStructure.txt, rotated to put the side-wall controller facing out. */
final class SteamTurbineStructure {

    private static final int EXPORT_WIDTH = 15;
    private static final int EXPORT_LENGTH = 30;
    static final int WIDTH = EXPORT_LENGTH;
    static final int HEIGHT = 15;
    static final int LENGTH = EXPORT_WIDTH;
    static final int OFFSET_X = 2;
    static final int OFFSET_Y = 13;
    static final int OFFSET_Z = 0;
    // ceil(hypot(29 - OFFSET_X, 14)): covers the rectangle under any horizontal facing.
    static final int CHUNK_RADIUS = 31;
    private static final String TOP = "   CCCCCCCCC   ";
    private static final String FLOOR = "CCCCCCCCCCCCCCC";

    private SteamTurbineStructure() {}

    static String[][] createShape() {
        String[][] exported = new String[EXPORT_LENGTH][];
        exported[0] = portLayer('B');
        exported[1] = capLayer(true);
        for (int z = 2; z <= 27; z++) exported[z] = shaftLayer();
        exported[2][OFFSET_Y] = "C             ~";
        exported[3] = bladeLayer(
            "  C         C  ",
            " C FFFFF     C ",
            "C   FFFF    F C",
            "C    FFF   FF C",
            "C     FF  FFF C",
            "C      F FFFF C",
            "C FFFFFGFFFFF C",
            "C FFFF F      C",
            "C FFF  FF     C",
            "C FF   FFF    C",
            "C F    FFFF   C",
            "C      FFFFF  C",
            "C             C");
        exported[5] = bladeLayer(
            "  CFFF      C  ",
            " CFFF        C ",
            "CFFF          C",
            "CFFF          C",
            "CFF           C",
            "CF            C",
            "C      G      C",
            "CF           FC",
            "CFF         FFC",
            "CFFF       FFFC",
            "CFFFF     FFFFC",
            "CFFFFF   FFFFFC",
            "CFFFFFF FFFFFFC");
        exported[7] = bladeLayer(
            "  C         C  ",
            " C           C ",
            "C   FFFF      C",
            "C    FFF   F  C",
            "C     FF  FF  C",
            "C      F FFF  C",
            "C  FFFFGFFFF  C",
            "C  FFF F      C",
            "C  FF  FF     C",
            "C  F   FFF    C",
            "C      FFFF   C",
            "C             C",
            "C             C");
        exported[9] = bladeLayer(
            "  CFFFFFFFFFC  ",
            " CFFFFF FFFFFC ",
            "CFFFFF   FFFFFC",
            "CFFFF     FFFFC",
            "CFFF       FFFC",
            "CFF         FFC",
            "CF     G     FC",
            "CFF         FFC",
            "CFFF       FFFC",
            "CFFFF     FFFFC",
            "CFFFFF   FFFFFC",
            "CFFFFFF FFFFFFC",
            "CFFFFFFFFFFFFFC");
        exported[11] = bladeLayer(
            "  C         C  ",
            " C           C ",
            "C             C",
            "C    FFF      C",
            "C     FF  F   C",
            "C      F FF   C",
            "C   FFFGFFF   C",
            "C   FF F      C",
            "C   F  FF     C",
            "C      FFF    C",
            "C             C",
            "C             C",
            "C             C");
        exported[13] = bladeLayer(
            "  CFFFFFFFFFC  ",
            " CFFFFFFFFFFFC ",
            "CFFFFFF FFFFFFC",
            "CFFFFF   FFFFFC",
            "CFFFF     FFFFC",
            "CFFF       FFFC",
            "CFF    G    FFC",
            "CFFF       FFFC",
            "CFFFF     FFFFC",
            "CFFFFF   FFFFFC",
            "CFFFFFF FFFFFFC",
            "CFFFFFFFFFFFFFC",
            "CFFFFFFFFFFFFFC");
        exported[16] = exported[13].clone();
        exported[18] = bladeLayer(
            "  C         C  ",
            " C           C ",
            "C             C",
            "C      FFF    C",
            "C   F  FF     C",
            "C   FF F      C",
            "C   FFFGFFF   C",
            "C      F FF   C",
            "C     FF  F   C",
            "C    FFF      C",
            "C             C",
            "C             C",
            "C             C");
        exported[20] = exported[9].clone();
        exported[22] = bladeLayer(
            "  C         C  ",
            " C           C ",
            "C      FFFF   C",
            "C  F   FFF    C",
            "C  FF  FF     C",
            "C  FFF F      C",
            "C  FFFFGFFFF  C",
            "C      F FFF  C",
            "C     FF  FF  C",
            "C    FFF   F  C",
            "C   FFFF      C",
            "C             C",
            "C             C");
        exported[24] = bladeLayer(
            "  C      FFFC  ",
            " C        FFFC ",
            "C          FFFC",
            "C          FFFC",
            "C           FFC",
            "C            FC",
            "C      G      C",
            "CF           FC",
            "CFF         FFC",
            "CFFF       FFFC",
            "CFFFF     FFFFC",
            "CFFFFF   FFFFFC",
            "CFFFFFF FFFFFFC");
        exported[26] = bladeLayer(
            "  C         C  ",
            " C     FFFFF C ",
            "C F    FFFF   C",
            "C FF   FFF    C",
            "C FFF  FF     C",
            "C FFFF F      C",
            "C FFFFFGFFFFF C",
            "C      F FFFF C",
            "C     FF  FFF C",
            "C    FFF   FF C",
            "C   FFFF    F C",
            "C  FFFFF      C",
            "C             C");
        exported[28] = capLayer(false);
        exported[29] = portLayer('A');
        String[][] rotated = new String[LENGTH][HEIGHT];
        // (x', y', z') = (z, y, 14 - x): export +X outside becomes the controller's front (-depth).
        for (int z = 0; z < LENGTH; z++) {
            for (int y = 0; y < HEIGHT; y++) {
                char[] row = new char[WIDTH];
                for (int x = 0; x < WIDTH; x++) row[x] = exported[x][y].charAt(EXPORT_WIDTH - 1 - z);
                rotated[z][y] = new String(row);
            }
        }
        return rotated;
    }

    private static String[] shaftLayer() {
        return bladeLayer(
            "  C         C  ",
            " C           C ",
            "C             C",
            "C             C",
            "C             C",
            "C             C",
            "C      G      C",
            "C             C",
            "C             C",
            "C             C",
            "C             C",
            "C             C",
            "C             C");
    }

    private static String[] bladeLayer(String... innerRows) {
        if (innerRows.length != HEIGHT - 2) throw new IllegalStateException("Invalid turbine layer height");
        String[] layer = new String[HEIGHT];
        layer[0] = TOP;
        System.arraycopy(innerRows, 0, layer, 1, innerRows.length);
        layer[HEIGHT - 1] = FLOOR;
        for (String row : layer) {
            if (row.length() != EXPORT_WIDTH) throw new IllegalStateException("Invalid turbine row width");
        }
        return layer;
    }

    private static String[] capLayer(boolean inlet) {
        String[] layer = new String[HEIGHT];
        Arrays.fill(layer, FLOOR);
        layer[0] = TOP;
        layer[1] = "  CCCCCCCCCCC  ";
        layer[2] = " CCCCCCCCCCCCC ";
        layer[7] = inlet ? "CCCCCCEDECCCCCC" : "CCCCCCCDCCCCCCC";
        if (inlet) layer[6] = layer[8] = "CCCCCCCECCCCCCC";
        return layer;
    }

    private static String[] portLayer(char port) {
        String[] layer = new String[HEIGHT];
        Arrays.fill(layer, "               ");
        layer[6] = layer[8] = "       E       ";
        layer[7] = "      E" + port + "E      ";
        return layer;
    }
}
