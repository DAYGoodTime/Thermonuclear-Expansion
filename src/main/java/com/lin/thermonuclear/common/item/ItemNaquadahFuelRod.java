package com.lin.thermonuclear.common.item;

public final class ItemNaquadahFuelRod extends ItemFuelBase {

    public static final int TIER_COUNT = 6;
    public static final int FUEL_LITRES = 250;

    private static final String[] TIER_NAMES = { "mk_i", "mk_ii", "mk_iii", "mk_iv", "mk_v", "mk_vi" };

    private static final int[] MAX_CYCLES = { 750, 875, 1000, 1250, 2000, 3000 };
    private static final long[] DIRECT_EUT = { 975_000L, 2_300_000L, 9_511_000L, 88_540_000L, 399_576_000L,
        2_077_795_200L };
    private static final long[] BASE_HEAT_PER_CYCLE = { 1_485_715L, 3_504_762L, 14_492_953L, 134_918_096L, 608_877_715L,
        3_166_164_115L };

    private final int tier;

    public ItemNaquadahFuelRod(int tier) {
        super(
            itemName(tier),
            "gregtech:gt.rodNaquadah",
            MAX_CYCLES[tier - 1],
            DIRECT_EUT[tier - 1],
            BASE_HEAT_PER_CYCLE[tier - 1],
            registerDepleted("depleted_" + itemName(tier), "gregtech:gt.depletedRodNaquadah"));
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    private static String itemName(int tier) {
        if (tier < 1 || tier > TIER_COUNT) {
            throw new IllegalArgumentException("Unknown naquadah fuel rod tier: " + tier);
        }
        return "naquadah_fuel_rod_" + TIER_NAMES[tier - 1];
    }
}
