package com.lin.thermonuclear.common.nuclear.thermal;

public final class NuclearMeltdownTimer {

    public static final int DELAY_TICKS = 200;
    private int elapsedTicks;

    public boolean checkMelted(long heat, long capacity) {
        if (heat <= capacity) {
            // 热量安全就重置计时
            reset();
            return false;
        }
        elapsedTicks = Math.min(DELAY_TICKS, elapsedTicks + 1);
        return elapsedTicks == DELAY_TICKS;
    }

    public int elapsedTicks() {
        return elapsedTicks;
    }

    public void restore(int ticks) {
        elapsedTicks = Math.max(0, Math.min(DELAY_TICKS, ticks));
    }

    public void reset() {
        elapsedTicks = 0;
    }
}
