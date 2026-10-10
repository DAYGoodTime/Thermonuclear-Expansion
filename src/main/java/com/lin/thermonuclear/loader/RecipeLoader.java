package com.lin.thermonuclear.loader;

import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.lin.thermonuclear.registry.ModFluids;

import goodgenerator.items.GGMaterial;
import goodgenerator.util.ItemRefer;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gtPlusPlus.core.material.MaterialsAlloy;

public final class RecipeLoader {

    private RecipeLoader() {}

    public static void register() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTModHandler.getIC2Item("nuclearReactor", 32),
                GTModHandler.getIC2Item("reinforcedStone", 64),
                GTModHandler.getIC2Item("reactorChamber", 64))
            .itemOutputs(MachineLoader.nuclearPowerPlant.copy())
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Machine_Multi_HeatExchanger.get(16),
                ItemList.Casing_SolidSteel.get(64),
                ItemList.Casing_Pipe_Bronze.get(64),
                GTModHandler.getIC2Item("reactorHeatSwitchDiamond", 6))
            .itemOutputs(MachineLoader.heatExchangeStation.copy())
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.SteamTurbine.get(64),
                ItemList.Casing_SolidSteel.get(64),
                GTOreDictUnificator.get(OrePrefixes.gear, Materials.Steel, 64))
            .itemOutputs(MachineLoader.largeSteamTurbine.copy())
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.rotor, Materials.Steel, 1),
                GTOreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Steel, 4))
            .itemOutputs(new ItemStack(BlockLoader.turbineBlade, 1, 0))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);

        // Two fluid inputs require the industrial mixer map, not the single-block mixer map.
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 64),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Glowstone, 64))
            .fluidInputs(Materials.SodiumPotassium.getFluid(1000), Materials.SuperCoolant.getFluid(1000))
            .fluidOutputs(new FluidStack(ModFluids.nakCompositeCoolant, 1000))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_LuV)
            .addTo(RecipeMaps.mixerNonCellRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Electric_Motor_MV.get(2),
                ItemList.Casing_Gearbox_Steel.get(1),
                GGMaterial.incoloy903.get(OrePrefixes.plateDouble, 4))
            .fluidInputs(Materials.Lubricant.getFluid(2000))
            .itemOutputs(new ItemStack(BlockLoader.lowPressureTurbineShaft, 1, 0))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(RecipeMaps.assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Electric_Motor_EV.get(2),
                ItemList.Casing_Gearbox_TungstenSteel.get(1),
                MaterialsAlloy.INCOLOY_MA956.getIngot(4))
            .fluidInputs(Materials.Lubricant.getFluid(2000))
            .itemOutputs(new ItemStack(BlockLoader.highPressureTurbineShaft, 1, 0))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(RecipeMaps.assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.IC2_Fuel_Rod_Empty.get(32),
                GGMaterial.zircaloy4.get(OrePrefixes.frameGt, 32),
                GGMaterial.zircaloy4.get(OrePrefixes.ring, 64),
                GGMaterial.zircaloy4.get(OrePrefixes.ring, 64))
            .itemOutputs(new ItemStack(BlockLoader.fuelRodTier1, 1, 0))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.IC2_Fuel_Rod_Empty.get(64),
                GGMaterial.zircaloy4.get(OrePrefixes.frameGt, 64),
                ItemList.Neutron_Reflector.get(8),
                ItemList.Electric_Pump_IV.get(8),
                new ItemStack(BlockLoader.fuelRodTier1, 1, 0))
            .itemOutputs(new ItemStack(BlockLoader.fuelRodTier2, 1, 0))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemRefer.Advanced_Fuel_Rod.get(64),
                ItemList.Neutron_Reflector.get(16),
                ItemList.Electric_Pump_LuV.get(8),
                new ItemStack(BlockLoader.fuelRodTier2, 1, 0))
            .itemOutputs(new ItemStack(BlockLoader.fuelRodTier3, 1, 0))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemRefer.Advanced_Fuel_Rod.get(64),
                ItemRefer.Advanced_Fuel_Rod.get(64),
                ItemList.Neutron_Reflector.get(32),
                ItemList.Electric_Pump_ZPM.get(8),
                new ItemStack(BlockLoader.fuelRodTier3, 1, 0))
            .itemOutputs(new ItemStack(BlockLoader.fuelRodTier4, 1, 0))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
