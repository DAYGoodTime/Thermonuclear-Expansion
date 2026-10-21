package com.lin.thermonuclear.block;

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
            case 1 -> 100000;
            case 2 -> 1000000;
            case 3 -> 100_000_000;
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

    public static BlockFuelRod[] registerTiers() {
        return registerTiers(BlockFuelRod::new, new BlockFuelRod[4]);
    }
}
