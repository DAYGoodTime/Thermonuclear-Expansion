package com.lin.thermonuclear.loader;

import java.util.Collection;
import java.util.Map;

import com.lin.thermonuclear.block.BlockContainmentCasing;
import com.lin.thermonuclear.block.BlockFuelRod;
import com.lin.thermonuclear.block.BlockTurbineBlade;
import com.lin.thermonuclear.block.BlockTurbineShaft;

public final class BlockLoader {

    private static Map<Integer, BlockTurbineShaft> turbineShafts;
    private static Map<Integer, BlockFuelRod> fuelRods;
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
    }

    public static BlockTurbineShaft turbineShaft(int tier) {
        BlockTurbineShaft block = turbineShafts.get(tier);
        if (block == null) throw new IllegalArgumentException("Unknown turbine shaft tier: " + tier);
        return block;
    }

    public static Collection<BlockTurbineShaft> turbineShafts() {
        return turbineShafts.values();
    }

    public static BlockFuelRod fuelRod(int tier) {
        BlockFuelRod block = fuelRods.get(tier);
        if (block == null) throw new IllegalArgumentException("Unknown fuel rod tier: " + tier);
        return block;
    }

    public static Collection<BlockFuelRod> fuelRods() {
        return fuelRods.values();
    }
}
