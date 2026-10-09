package com.lin.thermonuclear.recipe;

import java.util.function.Supplier;

import net.minecraftforge.fluids.Fluid;

import com.lin.thermonuclear.registry.WorkingFluids;

public enum SteamTurbineFuel {

    ORDINARY("ordinary", () -> WorkingFluids.steam, 3),
    SUPERHEATED("superheated", () -> WorkingFluids.superheatedSteam, 9),
    SUPERCRITICAL("supercritical", () -> WorkingFluids.supercriticalSteam, 15),
    ULTRA_SUPERCRITICAL("ultrasupercritical", () -> WorkingFluids.ultraSupercriticalSteam, 21);

    private final String key;
    private final Supplier<Fluid> fluid;
    private final int twiceEUPerLitre;

    SteamTurbineFuel(String key, Supplier<Fluid> fluid, int twiceEUPerLitre) {
        this.key = key;
        this.fluid = fluid;
        this.twiceEUPerLitre = twiceEUPerLitre;
    }

    public Fluid fluid() {
        return fluid.get();
    }

    public boolean supportsShaftTier(int shaftTier) {
        return shaftTier >= (this == ULTRA_SUPERCRITICAL ? 2 : 1);
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
