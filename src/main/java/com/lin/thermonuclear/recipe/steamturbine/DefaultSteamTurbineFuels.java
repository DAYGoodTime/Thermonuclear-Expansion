package com.lin.thermonuclear.recipe.steamturbine;

import java.util.function.Supplier;

import net.minecraftforge.fluids.Fluid;

import com.lin.thermonuclear.api.SteamTurbineFuel;
import com.lin.thermonuclear.registry.WorkingFluids;

public enum DefaultSteamTurbineFuels implements SteamTurbineFuel {

    STEAM("steam", () -> WorkingFluids.steam, 1.5, 1),
    OVER_HEAT_STEAM("overheat", () -> WorkingFluids.superheatedSteam, 4.5, 1),
    SUPERCRITICAL_STEAM("supercritical", () -> WorkingFluids.supercriticalSteam, 7.5, 1),
    ULTRA_SUPERCRITICAL_STEAM("ultrasupercritical", () -> WorkingFluids.ultraSupercriticalSteam, 10.5, 2);

    private final String key;
    private final Supplier<Fluid> fluid;
    private final double eUPerLitre;
    private final int requireShaftTier;

    DefaultSteamTurbineFuels(String key, Supplier<Fluid> fluid, double eUPerLitre, int requireShaftTier) {
        this.key = key;
        this.fluid = fluid;
        this.eUPerLitre = eUPerLitre;
        this.requireShaftTier = requireShaftTier;
    }

    @Override
    public Fluid getWorkingFluid() {
        return fluid.get();
    }

    @Override
    public double euPerLitre() {
        return eUPerLitre;
    }

    @Override
    public int requireShaftTier() {
        return requireShaftTier;
    }

    @Override
    public String nameKey() {
        return "thermonuclear.turbine.fuel." + key;
    }
}
