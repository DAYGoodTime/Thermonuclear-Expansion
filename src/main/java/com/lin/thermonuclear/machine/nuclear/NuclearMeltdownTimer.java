package com.lin.thermonuclear.machine.nuclear;

public final class NuclearMeltdownTimer {

    public static final int DELAY_TICKS = 200;
    private int elapsedTicks;

    public boolean advance(double heat, int capacity) {
        if (capacity <= 0 || heat <= capacity) {
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
