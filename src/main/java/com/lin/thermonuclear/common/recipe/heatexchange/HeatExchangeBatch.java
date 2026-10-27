package com.lin.thermonuclear.common.recipe.heatexchange;

import com.lin.thermonuclear.api.HeatExchangeRule;
import com.lin.thermonuclear.utils.MathUtil;

/**
 * Whole-litre hot-fluid batch, with unused prepaid water carried as a steam allowance.
 */
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

    //TODO long support
    public int hot() {
        return MathUtil.getIntClamp(hot);
    }

    public int water() {
        return MathUtil.getIntClamp(water);
    }

    public int steam() {
        return MathUtil.getIntClamp(steam);
    }

    public static HeatExchangeBatch calcUsage(long hot,long waterAvailable ,HeatExchangeRule recipe){
        if(recipe.steamPerHotCoolant() <= 0) return null;
        long waterHotCost = waterAvailable*STEAM_PER_WATER / recipe.steamPerHotCoolant();
        long hotUsed = Math.min(hot,waterHotCost);
        long steam = hotUsed*recipe.steamPerHotCoolant();
        long waterUsed = (steam/STEAM_PER_WATER);
        return new HeatExchangeBatch(hotUsed,waterUsed,steam);
    }
}
