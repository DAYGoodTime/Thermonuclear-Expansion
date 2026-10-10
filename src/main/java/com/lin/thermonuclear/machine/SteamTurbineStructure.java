package com.lin.thermonuclear.machine;

import java.util.Arrays;

/** Geometry from dev/SteamTurbineStructure.txt, with the controller facing out of the inlet end cap. */
final class SteamTurbineStructure {

    private static final int EXPORT_WIDTH = 15;
    private static final int EXPORT_LENGTH = 27;
    static final int WIDTH = EXPORT_WIDTH;
    static final int HEIGHT = 15;
    static final int LENGTH = EXPORT_LENGTH;
    static final int OFFSET_X = 7;
    static final int OFFSET_Y = 13;
    static final int OFFSET_Z = 1;
    // ceil(hypot(7, 26 - OFFSET_Z)): covers the rectangle under any horizontal facing.
    static final int CHUNK_RADIUS = 26;
    private static final String TOP = "   CCCCCCCCC   ";
    private static final String FLOOR = TOP;

    private SteamTurbineStructure() {}

    static String[][] createShape() {
        String[][] exported = new String[EXPORT_LENGTH][];
        exported[0] = portLayer('B');
        exported[1] = capLayer(true);
        for (int z = 2; z <= 24; z++) exported[z] = shaftLayer();
        exported[1][OFFSET_Y] = "  CCCCC~CCCCC  ";
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
            " C     FFFFF C ",
            "  C         C  ");
        exported[5] = bladeLayer(
            "  CFFFF FFFFC  ",
            " CFFFF   FFFFC ",
            "CFFFF     FFFFC",
            "CFFF       FFFC",
            "CFF         FFC",
            "CF           FC",
            "C      G      C",
            "CF           FC",
            "CFF         FFC",
            "CFFF       FFFC",
            "CFFFF     FFFFC",
            " CFFFF   FFFFC ",
            "  CFFFF FFFFC  ");
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
            " C           C ",
            "  C         C  ");
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
            " CFFFFF FFFFFC ",
            "  CFFFFFFFFFC  ");
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
            " C           C ",
            "  C         C  ");
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
            " CFFFFFFFFFFFC ",
            "  CFFFFFFFFFC  ");
        exported[15] = bladeLayer(
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
            " C           C ",
            "  C         C  ");
        exported[17] = exported[9].clone();
        exported[19] = bladeLayer(
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
            " C           C ",
            "  C         C  ");
        exported[21] = exported[5].clone();
        exported[23] = bladeLayer(
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
            " C FFFFF     C ",
            "  C         C  ");
        exported[25] = capLayer(false);
        exported[26] = portLayer('A');
        return exported;
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
            " C           C ",
            "  C         C  ");
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
        Arrays.fill(layer, "CCCCCCCCCCCCCCC");
        layer[0] = TOP;
        layer[1] = "  CCCCCCCCCCC  ";
        layer[2] = " CCCCCCCCCCCCC ";
        layer[12] = layer[2];
        layer[13] = layer[1];
        layer[14] = FLOOR;
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
