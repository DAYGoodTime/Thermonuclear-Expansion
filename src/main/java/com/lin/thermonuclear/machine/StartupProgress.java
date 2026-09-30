package com.lin.thermonuclear.machine;

public final class StartupProgress {

    private double progress;

    public double get() {
        return progress;
    }

    public double next(int startupTicks) {
        return Math.min(1, progress + 1.0 / Math.max(1, startupTicks));
    }

    public void advance(int startupTicks) {
        progress = next(startupTicks);
    }

    public double averageNext(int startupTicks, int ticks) {
        double total = 0;
        for (int tick = 1; tick <= ticks; tick++) {
            total += Math.min(1, progress + tick / (double) Math.max(1, startupTicks));
        }
        return total / ticks;
    }

    public void decay(int decayTicks) {
        progress = Math.max(0, progress - 1.0 / Math.max(1, decayTicks));
    }

    public void clear() {
        progress = 0;
    }

    public void restore(double saved) {
        progress = Double.isFinite(saved) ? Math.max(0, Math.min(1, saved)) : 0;
    }

    public static double fraction(double saved) {
        return Double.isFinite(saved) && saved >= 0 && saved < 1 ? saved : 0;
    }
}
