package com.lin.thermonuclear.recipe;

import net.minecraftforge.fluids.Fluid;

import com.lin.thermonuclear.registry.WorkingFluids;

public enum HeatExchangeSteam {

    ORDINARY("ordinary"),
    SUPERHEATED("superheated"),
    SUPERCRITICAL("supercritical");

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

    public Fluid fluid() {
        return switch (this) {
            case ORDINARY -> WorkingFluids.steam;
            case SUPERHEATED -> WorkingFluids.superheatedSteam;
            case SUPERCRITICAL -> WorkingFluids.supercriticalSteam;
        };
    }
}
