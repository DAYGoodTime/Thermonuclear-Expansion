package com.lin.thermonuclear.block;

import java.util.Map;

public final class BlockFuelRod extends BlockTieredMachineComponent {

    private BlockFuelRod(int tier) {
        super(
            "fuel_rod_tier_" + tier,
            tier,
            "fuel_rod/fuel_rod_side_" + tier,
            "fuel_rod/fuel_rod_top_" + tier,
            "fuel_rod/fuel_rod_bottom_" + tier);
        addInfo("thermonuclear.tooltip.fuel_rod.0", heatCapacity(tier), maxParallel(tier));
    }

    public static int heatCapacity(int tier) {
        return switch (tier) {
            case 1 -> 50000;
            case 2 -> 100000;
            case 3 -> 1000000;
            case 4 -> Integer.MAX_VALUE;
            default -> 0;
        };
    }

    public static int maxParallel(int tier) {
        return switch (tier) {
            case 1 -> 8;
            case 2 -> 32;
            case 3 -> 128;
            case 4 -> 512;
            default -> 0;
        };
    }

    public static Map<Integer, BlockFuelRod> registerTiers() {
        return registerTiers(BlockFuelRod::new, 1, 2, 3, 4);
    }
}
