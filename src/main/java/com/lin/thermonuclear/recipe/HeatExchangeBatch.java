package com.lin.thermonuclear.recipe;

/** Whole-litre hot-fluid batch, with unused prepaid water carried as a steam allowance. */
public final class HeatExchangeBatch {

    public static final int STEAM_PER_WATER = 160;

    private final int hot;
    private final int water;
    private final int steam;
    private final int waterCredit;
    private final double steamRemainder;

    private HeatExchangeBatch(int hot, int water, int steam, int waterCredit, double steamRemainder) {
        this.hot = hot;
        this.water = water;
        this.steam = steam;
        this.waterCredit = waterCredit;
        this.steamRemainder = steamRemainder;
    }

    public int hot() {
        return hot;
    }

    public int water() {
        return water;
    }

    public int steam() {
        return steam;
    }

    public int waterCredit() {
        return waterCredit;
    }

    public double steamRemainder() {
        return steamRemainder;
    }

    public static HeatExchangeBatch forHot(int hot, double steamPerHot, double remainder, int waterCredit) {
        if (hot <= 0 || !Double.isFinite(steamPerHot)
            || steamPerHot <= 0
            || !Double.isFinite(remainder)
            || remainder < 0
            || remainder >= 1
            || waterCredit < 0
            || waterCredit >= STEAM_PER_WATER) return null;
        double total = hot * steamPerHot + remainder;
        double wholeSteam = Math.floor(total);
        if (!Double.isFinite(total) || wholeSteam > Integer.MAX_VALUE) return null;
        int steam = (int) wholeSteam;
        long unpaidSteam = Math.max(0L, (long) steam - waterCredit);
        int water = (int) ((unpaidSteam + STEAM_PER_WATER - 1) / STEAM_PER_WATER);
        int remainingCredit = (int) ((long) water * STEAM_PER_WATER + waterCredit - steam);
        return new HeatExchangeBatch(hot, water, steam, remainingCredit, total - steam);
    }
}
