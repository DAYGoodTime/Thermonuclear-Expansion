package com.lin.thermonuclear.registry;

/** User-approved prototype values for a sealed 250 L fuel rod. */
public enum NaquadahFuelTier {

    MK_I("mk_i", 750, 975_000L, 1_485_715L),
    MK_II("mk_ii", 875, 2_300_000L, 3_504_762L),
    MK_III("mk_iii", 1000, 9_511_000L, 14_492_953L),
    MK_IV("mk_iv", 1250, 88_540_000L, 134_918_096L),
    MK_V("mk_v", 2000, 399_576_000L, 608_877_715L),
    MK_VI("mk_vi", 3000, 2_077_795_200L, 3_166_164_115L);

    public static final int FUEL_LITRES = 250;

    private final String id;
    private final int maxCycles;
    private final long directEUt;
    private final long baseHeatPerCycle;

    NaquadahFuelTier(String id, int maxCycles, long directEUt, long baseHeatPerCycle) {
        this.id = id;
        this.maxCycles = maxCycles;
        this.directEUt = directEUt;
        this.baseHeatPerCycle = baseHeatPerCycle;
    }

    public String itemName() {
        return "naquadah_fuel_rod_" + id;
    }

    public String depletedItemName() {
        return "depleted_" + itemName();
    }

    public int maxCycles() {
        return maxCycles;
    }

    /** Target actual output with default settings, not the adapter value before the direct-output multiplier. */
    public long directEUt() {
        return directEUt;
    }

    /** Rounded up before the reactor's heat-output multiplier; deliberately wider than int. */
    public long baseHeatPerCycle() {
        return baseHeatPerCycle;
    }
}
