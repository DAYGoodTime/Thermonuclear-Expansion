package com.lin.thermonuclear.common.block;

import com.lin.thermonuclear.Config;

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

    public static long heatCapacity(int tier) {
        return tier >= 1 && tier <= 4 ? Config.nuclearPowerPlant.fuelRodHeatCapacityValues[tier - 1] : 0L;
    }

    public static int maxParallel(int tier) {
        return tier >= 1 && tier <= 4 ? Config.nuclearPowerPlant.fuelRodMaxParallel[tier - 1] : 0;
    }

    public static BlockFuelRod[] registerTiers() {
        return registerTiers(BlockFuelRod::new, new BlockFuelRod[4]);
    }
}
