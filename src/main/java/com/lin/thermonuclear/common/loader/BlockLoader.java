package com.lin.thermonuclear.common.loader;

import com.lin.thermonuclear.api.TNCItemList;
import com.lin.thermonuclear.common.block.BlockContainmentCasing;
import com.lin.thermonuclear.common.block.BlockFuelRod;
import com.lin.thermonuclear.common.block.BlockTurbineBlade;
import com.lin.thermonuclear.common.block.BlockTurbineShaft;

public final class BlockLoader {

    private static BlockTurbineShaft[] turbineShafts;
    private static BlockFuelRod[] fuelRods;
    public static BlockTurbineBlade turbineBlade;
    public static BlockContainmentCasing containmentCasing;

    private BlockLoader() {}

    public static void register() {
        turbineShafts = BlockTurbineShaft.registerTiers();
        turbineBlade = new BlockTurbineBlade();
        turbineBlade.register();
        fuelRods = BlockFuelRod.registerTiers();
        containmentCasing = new BlockContainmentCasing();
        containmentCasing.register();
        TNCItemList.LowPressureTurbineShaft.set(turbineShaft(1));
        TNCItemList.HighPressureTurbineShaft.set(turbineShaft(2));
        TNCItemList.TurbineBlade.set(turbineBlade);
        TNCItemList.FuelRodTier1.set(fuelRod(1));
        TNCItemList.FuelRodTier2.set(fuelRod(2));
        TNCItemList.FuelRodTier3.set(fuelRod(3));
        TNCItemList.FuelRodTier4.set(fuelRod(4));
        TNCItemList.ContainmentCasing.set(containmentCasing);
    }

    public static BlockTurbineShaft turbineShaft(int tier) {
        if (tier < 1 || tier > turbineShafts.length) {
            throw new IllegalArgumentException("Unknown turbine shaft tier: " + tier);
        }
        BlockTurbineShaft block = turbineShafts[tier - 1];
        if (block == null) throw new IllegalArgumentException("Unknown turbine shaft tier: " + tier);
        return block;
    }

    public static BlockTurbineShaft[] turbineShafts() {
        return turbineShafts.clone();
    }

    public static BlockFuelRod fuelRod(int tier) {
        if (tier < 1 || tier > fuelRods.length) {
            throw new IllegalArgumentException("Unknown fuel rod tier: " + tier);
        }
        BlockFuelRod block = fuelRods[tier - 1];
        if (block == null) throw new IllegalArgumentException("Unknown fuel rod tier: " + tier);
        return block;
    }

    public static BlockFuelRod[] fuelRods() {
        return fuelRods.clone();
    }
}
