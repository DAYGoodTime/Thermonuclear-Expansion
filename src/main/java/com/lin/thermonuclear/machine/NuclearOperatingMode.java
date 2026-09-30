package com.lin.thermonuclear.machine;

public enum NuclearOperatingMode {

    DIRECT_GENERATION("direct"),
    HEAT_SUPPLY("heat");

    private final String id;

    NuclearOperatingMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "thermonuclear.mode." + id;
    }

    public NuclearOperatingMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static NuclearOperatingMode fromId(String id) {
        for (NuclearOperatingMode mode : values()) {
            if (mode.id.equals(id)) return mode;
        }
        return DIRECT_GENERATION;
    }
}
