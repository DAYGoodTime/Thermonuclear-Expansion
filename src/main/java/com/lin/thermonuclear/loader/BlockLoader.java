package com.lin.thermonuclear.loader;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import com.lin.thermonuclear.block.BlockAxialMachineComponent;
import com.lin.thermonuclear.block.ItemBlockMachineComponent;
import com.lin.thermonuclear.registry.ModItems;

import cpw.mods.fml.common.registry.GameRegistry;

public final class BlockLoader {

    public static Block lowPressureTurbineShaft;
    public static Block highPressureTurbineShaft;
    public static Block fuelRodTier1;
    public static Block fuelRodTier2;
    public static Block fuelRodTier3;
    public static Block fuelRodTier4;

    private BlockLoader() {}

    public static void register() {
        lowPressureTurbineShaft = registerShaft("low_pressure_turbine_shaft", 1);
        highPressureTurbineShaft = registerShaft("high_pressure_turbine_shaft", 4);
        fuelRodTier1 = registerFuelRod(1);
        fuelRodTier2 = registerFuelRod(2);
        fuelRodTier3 = registerFuelRod(3);
        fuelRodTier4 = registerFuelRod(4);
    }

    private static Block registerShaft(String name, int textureVariant) {
        String prefix = "turbine_shaft/turbine_shaft_";
        return registerBlock(
            name,
            prefix + "side_" + textureVariant,
            prefix + "top_bottom_" + textureVariant,
            prefix + "top_bottom_" + textureVariant);
    }

    private static Block registerFuelRod(int tier) {
        String prefix = "fuel_rod/fuel_rod_";
        return registerBlock(
            "fuel_rod_tier_" + tier,
            prefix + "side_" + tier,
            prefix + "top_" + tier,
            prefix + "bottom_" + tier);
    }

    private static Block registerBlock(String name, String side, String top, String bottom) {
        Block block = new BlockAxialMachineComponent(name, side, top, bottom);
        GameRegistry.registerBlock(block, ItemBlockMachineComponent.class, name);
        ModItems.register(new ItemStack(block));
        return block;
    }
}
