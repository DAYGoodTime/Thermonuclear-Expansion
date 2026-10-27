package com.lin.thermonuclear.utils;

public class MathUtil {

    public static int getIntClamp(long value){
        if(value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return Math.toIntExact(value);
    }
}
