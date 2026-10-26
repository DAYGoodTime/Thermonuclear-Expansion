package com.lin.thermonuclear.machine.nuclear;

import java.math.BigDecimal;

import com.lin.thermonuclear.utils.StartupProgress;

/** Whole heat stays exact above 2^53; efficiency and cooling retain their sub-unit remainder. */
public final class NuclearHeatBuffer {

    private static final BigDecimal MAX_HEAT = BigDecimal.valueOf(Long.MAX_VALUE);
    private long stored;
    private double remainder;

    public long stored() {
        return stored;
    }

    public double remainder() {
        return remainder;
    }

    public boolean isEmpty() {
        return stored == 0 && remainder == 0;
    }

    public void restore(long heat, double fraction) {
        stored = Math.max(0, heat);
        remainder = stored == Long.MAX_VALUE ? 0 : StartupProgress.fraction(fraction);
    }

    public void add(BigDecimal amount) {
        if (amount.signum() < 0) throw new IllegalArgumentException("Invalid heat addition");
        change(amount);
    }

    public void remove(double amount) {
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid heat removal");
        change(
            BigDecimal.valueOf(amount)
                .negate());
    }

    private void change(BigDecimal amount) {
        // Do not convert stored to double: a small cooling step would otherwise disappear at high heat.
        BigDecimal total = BigDecimal.valueOf(stored)
            .add(BigDecimal.valueOf(remainder))
            .add(amount);
        if (total.signum() <= 0) {
            stored = 0;
            remainder = 0;
        } else if (total.compareTo(MAX_HEAT) >= 0) {
            stored = Long.MAX_VALUE;
            remainder = 0;
        } else {
            stored = total.longValue();
            remainder = total.subtract(BigDecimal.valueOf(stored))
                .doubleValue();
            if (remainder >= 1) {
                stored++;
                remainder = 0;
            }
        }
    }
}
