package com.lin.thermonuclear.common.machine;

/** Geometry from dev/SteamTurbineStructure.txt, with the controller facing out of the inlet end cap. */
final class SteamTurbineStructure {

    static final int WIDTH = 15;
    static final int HEIGHT = 15;
    static final int LENGTH = 27;
    static final int OFFSET_X = 7;
    static final int OFFSET_Y = 13;
    static final int OFFSET_Z = 1;
    // spotless:off
    // Normal scan order: SHAPE[z][y].charAt(x); '~' is the real controller anchor.
    static final String[][] SHAPE = { { // z = 0
        "               ", "               ", "               ", "               ", "               ",
        "               ", "       E       ", "      EBE      ", "       E       ", "               ",
        "               ", "               ", "               ", "               ", "               ", },
        { // z = 1
            "   CCCCCCCCC   ", "  CCCCCCCCCCC  ", " CCCCCCCCCCCCC ", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC",
            "CCCCCCCCCCCCCCC", "CCCCCCCECCCCCCC", "CCCCCCEDECCCCCC", "CCCCCCCECCCCCCC", "CCCCCCCCCCCCCCC",
            "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", " CCCCCCCCCCCCC ", "  CCCCC~CCCCC  ", "   CCCCCCCCC   ", },
        { // z = 2
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C             C", "C             C",
            "C             C", "C             C", "C      G      C", "C             C", "C             C",
            "C             C", "C             C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 3
            "   CCCCCCCCC   ", "  C         C  ", " C FFFFF     C ", "C   FFFF    F C", "C    FFF   FF C",
            "C     FF  FFF C", "C      F FFFF C", "C FFFFFGFFFFF C", "C FFFF F      C", "C FFF  FF     C",
            "C FF   FFF    C", "C F    FFFF   C", " C     FFFFF C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 4
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C             C", "C             C",
            "C             C", "C             C", "C      G      C", "C             C", "C             C",
            "C             C", "C             C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 5
            "   CCCCCCCCC   ", "  CFFFF FFFFC  ", " CFFFF   FFFFC ", "CFFFF     FFFFC", "CFFF       FFFC",
            "CFF         FFC", "CF           FC", "C      G      C", "CF           FC", "CFF         FFC",
            "CFFF       FFFC", "CFFFF     FFFFC", " CFFFF   FFFFC ", "  CFFFF FFFFC  ", "   CCCCCCCCC   ", },
        { // z = 6
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C             C", "C             C",
            "C             C", "C             C", "C      G      C", "C             C", "C             C",
            "C             C", "C             C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 7
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C   FFFF      C", "C    FFF   F  C",
            "C     FF  FF  C", "C      F FFF  C", "C  FFFFGFFFF  C", "C  FFF F      C", "C  FF  FF     C",
            "C  F   FFF    C", "C      FFFF   C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 8
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C             C", "C             C",
            "C             C", "C             C", "C      G      C", "C             C", "C             C",
            "C             C", "C             C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 9
            "   CCCCCCCCC   ", "  CFFFFFFFFFC  ", " CFFFFF FFFFFC ", "CFFFFF   FFFFFC", "CFFFF     FFFFC",
            "CFFF       FFFC", "CFF         FFC", "CF     G     FC", "CFF         FFC", "CFFF       FFFC",
            "CFFFF     FFFFC", "CFFFFF   FFFFFC", " CFFFFF FFFFFC ", "  CFFFFFFFFFC  ", "   CCCCCCCCC   ", },
        { // z = 10
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C             C", "C             C",
            "C             C", "C             C", "C      G      C", "C             C", "C             C",
            "C             C", "C             C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 11
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C             C", "C    FFF      C",
            "C     FF  F   C", "C      F FF   C", "C   FFFGFFF   C", "C   FF F      C", "C   F  FF     C",
            "C      FFF    C", "C             C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 12
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C             C", "C             C",
            "C             C", "C             C", "C      G      C", "C             C", "C             C",
            "C             C", "C             C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 13
            "   CCCCCCCCC   ", "  CFFFFFFFFFC  ", " CFFFFFFFFFFFC ", "CFFFFFF FFFFFFC", "CFFFFF   FFFFFC",
            "CFFFF     FFFFC", "CFFF       FFFC", "CFF    G    FFC", "CFFF       FFFC", "CFFFF     FFFFC",
            "CFFFFF   FFFFFC", "CFFFFFF FFFFFFC", " CFFFFFFFFFFFC ", "  CFFFFFFFFFC  ", "   CCCCCCCCC   ", },
        { // z = 14
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C             C", "C             C",
            "C             C", "C             C", "C      G      C", "C             C", "C             C",
            "C             C", "C             C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 15
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C             C", "C      FFF    C",
            "C   F  FF     C", "C   FF F      C", "C   FFFGFFF   C", "C      F FF   C", "C     FF  F   C",
            "C    FFF      C", "C             C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 16
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C             C", "C             C",
            "C             C", "C             C", "C      G      C", "C             C", "C             C",
            "C             C", "C             C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 17
            "   CCCCCCCCC   ", "  CFFFFFFFFFC  ", " CFFFFF FFFFFC ", "CFFFFF   FFFFFC", "CFFFF     FFFFC",
            "CFFF       FFFC", "CFF         FFC", "CF     G     FC", "CFF         FFC", "CFFF       FFFC",
            "CFFFF     FFFFC", "CFFFFF   FFFFFC", " CFFFFF FFFFFC ", "  CFFFFFFFFFC  ", "   CCCCCCCCC   ", },
        { // z = 18
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C             C", "C             C",
            "C             C", "C             C", "C      G      C", "C             C", "C             C",
            "C             C", "C             C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 19
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C      FFFF   C", "C  F   FFF    C",
            "C  FF  FF     C", "C  FFF F      C", "C  FFFFGFFFF  C", "C      F FFF  C", "C     FF  FF  C",
            "C    FFF   F  C", "C   FFFF      C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 20
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C             C", "C             C",
            "C             C", "C             C", "C      G      C", "C             C", "C             C",
            "C             C", "C             C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 21
            "   CCCCCCCCC   ", "  CFFFF FFFFC  ", " CFFFF   FFFFC ", "CFFFF     FFFFC", "CFFF       FFFC",
            "CFF         FFC", "CF           FC", "C      G      C", "CF           FC", "CFF         FFC",
            "CFFF       FFFC", "CFFFF     FFFFC", " CFFFF   FFFFC ", "  CFFFF FFFFC  ", "   CCCCCCCCC   ", },
        { // z = 22
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C             C", "C             C",
            "C             C", "C             C", "C      G      C", "C             C", "C             C",
            "C             C", "C             C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 23
            "   CCCCCCCCC   ", "  C         C  ", " C     FFFFF C ", "C F    FFFF   C", "C FF   FFF    C",
            "C FFF  FF     C", "C FFFF F      C", "C FFFFFGFFFFF C", "C      F FFFF C", "C     FF  FFF C",
            "C    FFF   FF C", "C   FFFF    F C", " C FFFFF     C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 24
            "   CCCCCCCCC   ", "  C         C  ", " C           C ", "C             C", "C             C",
            "C             C", "C             C", "C      G      C", "C             C", "C             C",
            "C             C", "C             C", " C           C ", "  C         C  ", "   CCCCCCCCC   ", },
        { // z = 25
            "   CCCCCCCCC   ", "  CCCCCCCCCCC  ", " CCCCCCCCCCCCC ", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC",
            "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCDCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC",
            "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", " CCCCCCCCCCCCC ", "  CCCCCCCCCCC  ", "   CCCCCCCCC   ", },
        { // z = 26
            "               ", "               ", "               ", "               ", "               ",
            "               ", "       E       ", "      EAE      ", "       E       ", "               ",
            "               ", "               ", "               ", "               ", "               ", }, };
    // spotless:on
    private SteamTurbineStructure() {}
}
