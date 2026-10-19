package com.lin.thermonuclear.recipe.steamturbine;

import java.util.function.Supplier;

import net.minecraftforge.fluids.Fluid;

import com.lin.thermonuclear.registry.WorkingFluids;

public enum SteamTurbineFuel {

    ORDINARY("ordinary", () -> WorkingFluids.steam, 3, 1),
    SUPERHEATED("superheated", () -> WorkingFluids.superheatedSteam, 9, 1),
    SUPERCRITICAL("supercritical", () -> WorkingFluids.supercriticalSteam, 15, 1),
    ULTRA_SUPERCRITICAL("ultrasupercritical", () -> WorkingFluids.ultraSupercriticalSteam, 21, 2);

    private final String key;
    private final Supplier<Fluid> fluid;
    private final int twiceEUPerLitre;
    private final int requireShaftTier;

    SteamTurbineFuel(String key, Supplier<Fluid> fluid, int twiceEUPerLitre, int requireShaftTier) {
        this.key = key;
        this.fluid = fluid;
        this.twiceEUPerLitre = twiceEUPerLitre;
        this.requireShaftTier = requireShaftTier;
    }

    public Fluid fluid() {
        return fluid.get();
    }

    public boolean supportsShaftTier(int shaftTier) {
        return shaftTier >= (this == ULTRA_SUPERCRITICAL ? 2 : 1);
    }

    public int supportsShaftTier() {
        return this.requireShaftTier;
    }

    public String nameKey() {
        return "thermonuclear.steam." + key;
    }

    public double euPerLitre() {
        return twiceEUPerLitre / 2.0;
    }

    public int displayEnergy(int litres) {
        return litres * twiceEUPerLitre / 2;
    }
}
