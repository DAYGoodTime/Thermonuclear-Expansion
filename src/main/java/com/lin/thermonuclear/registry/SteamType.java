package com.lin.thermonuclear.registry;

import net.minecraftforge.fluids.Fluid;

public enum SteamType {

    ORDINARY("ordinary"),
    SUPERHEATED("superheated"),
    SUPERCRITICAL("supercritical"),
    ULTRA_SUPERCRITICAL("ultrasupercritical");

    private final String id;

    SteamType(String id) {
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
            case ULTRA_SUPERCRITICAL -> WorkingFluids.ultraSupercriticalSteam;
        };
    }
}
