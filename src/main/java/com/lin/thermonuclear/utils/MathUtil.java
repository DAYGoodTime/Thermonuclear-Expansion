package com.lin.thermonuclear.utils;

public class MathUtil {

    public static int getIntClamp(long value) {
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return Math.toIntExact(value);
    }

    public static long parseLongSafe(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
