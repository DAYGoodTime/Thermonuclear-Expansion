package com.lin.thermonuclear.nuclear;

import java.util.function.IntPredicate;

/** Integer fluid conversion limits; fractional heat remains in the reactor. */
public final class NuclearCoolingMath {

    private NuclearCoolingMath() {}

    public static int coolingLimit(double heat, double litresPerHeat, long available, double steamPerWater,
        double steamRemainder) {
        double limit = Math.min(Integer.MAX_VALUE, Math.min(available, Math.floor(heat * litresPerHeat)));
        if (steamPerWater > 0) {
            limit = Math.min(limit, Math.floor((Integer.MAX_VALUE - steamRemainder) / steamPerWater));
        }
        int amount = (int) Math.max(0, limit);
        // Multiplication and division can straddle an integer boundary differently.
        if (amount > 0 && amount / litresPerHeat > heat) amount--;
        return amount;
    }

    public static double remainingHeat(double heat, int amount, double litresPerHeat) {
        return Math.max(0, heat - amount / litresPerHeat);
    }

    public static double passiveCooling(double heat, double heatPerSecond, int ticks) {
        return Math.max(0, heat - heatPerSecond * ticks / 20.0);
    }

    public static int acceptedAmount(int limit, IntPredicate accepts) {
        int low = 0;
        int high = limit;
        while (low < high) {
            int amount = (int) (low + ((long) high - low + 1) / 2);
            if (accepts.test(amount)) low = amount;
            else high = amount - 1;
        }
        return low;
    }

    public static int steamOutput(int water, double steamPerWater, double remainder) {
        return (int) Math.floor(water * steamPerWater + remainder);
    }

    public static double steamRemainder(int water, double steamPerWater, double remainder) {
        double total = water * steamPerWater + remainder;
        return total - Math.floor(total);
    }
}
