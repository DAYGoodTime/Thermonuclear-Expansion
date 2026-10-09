package com.lin.thermonuclear.recipe;

import net.minecraftforge.fluids.Fluid;

import com.lin.thermonuclear.registry.WorkingFluids;

public enum HeatExchangeSteam {

    ORDINARY("ordinary"),
    SUPERHEATED("superheated"),
    SUPERCRITICAL("supercritical"),
    ULTRA_SUPERCRITICAL("ultrasupercritical");

    private final String id;

    HeatExchangeSteam(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "thermonuclear.steam." + id;
    }

    public double steamPerHotCoolant() {
        return switch (this) {
            case ORDINARY -> 40;
            case SUPERHEATED -> 20;
            case SUPERCRITICAL -> 10;
            case ULTRA_SUPERCRITICAL -> 100;
        };
    }

    public Fluid fluid() {
        return switch (this) {
            case ORDINARY -> WorkingFluids.steam;
            case SUPERHEATED -> WorkingFluids.superheatedSteam;
            case SUPERCRITICAL -> WorkingFluids.supercriticalSteam;
            case ULTRA_SUPERCRITICAL -> WorkingFluids.ultraSupercriticalSteam;
        };
    }
}
