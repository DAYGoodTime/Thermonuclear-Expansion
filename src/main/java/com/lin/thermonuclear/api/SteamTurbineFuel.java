package com.lin.thermonuclear.api;

import net.minecraftforge.fluids.Fluid;

public interface SteamTurbineFuel {

    /**
     * Fluid as Fuel
     */
    Fluid getWorkingFluid();

    /**
     * Energy Capacity (x Eu/L(mb))
     */
    double euPerLitre();

    /**
     * Require Shaft Tier
     * Advance Fluid need Advance Shaft
     */
    default int requireShaftTier() {
        return 1;
    }

    /**
     * lang key for Fluid Name
     */
    String nameKey();

    default boolean supportShaftTier(int shaftTier) {
        return shaftTier >= requireShaftTier();
    }
}
