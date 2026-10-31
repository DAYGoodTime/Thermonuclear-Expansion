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

    public static long saturatingAdd(long a, long b) {
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }

    /** Multiplies nonnegative quantities, saturating at the long limit. */
    public static long saturatingMultiply(long a, long b) {
        return a > 0 && b > Long.MAX_VALUE / a ? Long.MAX_VALUE : a * b;
    }
}
