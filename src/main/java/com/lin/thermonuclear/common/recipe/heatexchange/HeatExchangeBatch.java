package com.lin.thermonuclear.common.recipe.heatexchange;

import com.lin.thermonuclear.api.HeatExchangeRule;

/** A whole-litre heat-exchange batch calculated without narrowing intermediate values. */
public final class HeatExchangeBatch {

    public static final int STEAM_PER_WATER = 160;

    private final long hot;
    private final long water;
    private final long steam;

    private HeatExchangeBatch(long hot, long water, long steam) {
        this.hot = hot;
        this.water = water;
        this.steam = steam;
    }

    public long hot() {
        return hot;
    }

    public long water() {
        return water;
    }

    public long steam() {
        return steam;
    }

    public static HeatExchangeBatch calcUsage(long hot, long waterAvailable, HeatExchangeRule recipe) {
        if (hot < 0 || waterAvailable < 0) throw new IllegalArgumentException("Negative heat-exchange input");
        if (recipe.steamPerHotCoolant() <= 0) return null;
        if (hot <= 0 || waterAvailable <= 0) return new HeatExchangeBatch(0, 0, 0);
        long steamPerHot = recipe.steamPerHotCoolant();
        long waterHotCost = multiplyDivideFloor(waterAvailable, STEAM_PER_WATER, steamPerHot);
        // Limit the batch before multiplication; saturating only the product would consume excess hot coolant.
        long hotUsed = Math.min(Math.min(hot, waterHotCost), Long.MAX_VALUE / steamPerHot);
        long steam = hotUsed * steamPerHot;
        long waterUsed = steam / STEAM_PER_WATER;
        return new HeatExchangeBatch(hotUsed, waterUsed, steam);
    }

    private static long multiplyDivideFloor(long value, long multiplier, long divisor) {
        java.math.BigInteger result = java.math.BigInteger.valueOf(value)
            .multiply(java.math.BigInteger.valueOf(multiplier))
            .divide(java.math.BigInteger.valueOf(divisor));
        return result.min(java.math.BigInteger.valueOf(Long.MAX_VALUE))
            .longValue();
    }
}
