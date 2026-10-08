package com.lin.thermonuclear.recipe;

/** Converts integer fluid batches to tick-based rates at the processing boundary. */
public final class ProcessingCycleMath {

    private ProcessingCycleMath() {}

    public static double litresPerTick(long litresPerCycle, int cycleTicks) {
        if (litresPerCycle <= 0 || cycleTicks <= 0) return 0;
        return litresPerCycle / (double) cycleTicks;
    }
}
