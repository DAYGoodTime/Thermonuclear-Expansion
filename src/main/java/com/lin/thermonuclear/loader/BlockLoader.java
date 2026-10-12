package com.lin.thermonuclear.loader;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import com.lin.thermonuclear.block.BlockAxialMachineComponent;
import com.lin.thermonuclear.block.BlockTooltip;
import com.lin.thermonuclear.block.BlockTurbineBlade;
import com.lin.thermonuclear.block.ItemBlockMachineComponent;
import com.lin.thermonuclear.registry.ModItems;

import cpw.mods.fml.common.registry.GameRegistry;

public final class BlockLoader {

    public static Block lowPressureTurbineShaft;
    public static Block highPressureTurbineShaft;
    public static Block turbineBlade;
    public static Block fuelRodTier1;
    public static Block fuelRodTier2;
    public static Block fuelRodTier3;
    public static Block fuelRodTier4;

    private BlockLoader() {}

    public static void register() {
        lowPressureTurbineShaft = registerShaft(
            "low_pressure_turbine_shaft",
            1,
            BlockTooltip.LOW_PRESSURE_TURBINE_SHAFT);
        highPressureTurbineShaft = registerShaft(
            "high_pressure_turbine_shaft",
            4,
            BlockTooltip.HIGH_PRESSURE_TURBINE_SHAFT);
        turbineBlade = registerBlock(new BlockTurbineBlade(), "turbine_blade", BlockTooltip.TURBINE_BLADE);
        fuelRodTier1 = registerFuelRod(1, BlockTooltip.FUEL_ROD_TIER_1);
        fuelRodTier2 = registerFuelRod(2, BlockTooltip.FUEL_ROD_TIER_2);
        fuelRodTier3 = registerFuelRod(3, BlockTooltip.FUEL_ROD_TIER_3);
        fuelRodTier4 = registerFuelRod(4, BlockTooltip.FUEL_ROD_TIER_4);
    }

    private static Block registerShaft(String name, int textureVariant, BlockTooltip tooltip) {
        String prefix = "turbine_shaft/turbine_shaft_";
        return registerBlock(
            name,
            prefix + "side_" + textureVariant,
            prefix + "top_bottom_" + textureVariant,
            prefix + "top_bottom_" + textureVariant,
            tooltip);
    }

    private static Block registerFuelRod(int tier, BlockTooltip tooltip) {
        String prefix = "fuel_rod/fuel_rod_";
        return registerBlock(
            "fuel_rod_tier_" + tier,
            prefix + "side_" + tier,
            prefix + "top_" + tier,
            prefix + "bottom_" + tier,
            tooltip);
    }

    private static Block registerBlock(String name, String side, String top, String bottom, BlockTooltip tooltip) {
        return registerBlock(new BlockAxialMachineComponent(name, side, top, bottom), name, tooltip);
    }

    private static Block registerBlock(Block block, String name, BlockTooltip tooltip) {
        GameRegistry.registerBlock(block, ItemBlockMachineComponent.class, name, tooltip);
        ModItems.register(new ItemStack(block));
        return block;
    }
}
