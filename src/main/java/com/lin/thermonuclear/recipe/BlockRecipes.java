package com.lin.thermonuclear.recipe;

import static gregtech.api.util.GTRecipeBuilder.INGOTS;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import com.lin.thermonuclear.api.ModRecipeLoader;
import com.lin.thermonuclear.registry.TNItemList;

import goodgenerator.items.GGMaterial;
import goodgenerator.util.ItemRefer;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.enums.ToolDictNames;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gtPlusPlus.core.material.MaterialsAlloy;

public class BlockRecipes implements ModRecipeLoader {

    @Override
    public void loadRecipes() {

        // Nuclear Machine
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTModHandler.getIC2Item("nuclearReactor", 32),
                TNItemList.ContainmentCasing.get(63),
                GTModHandler.getIC2Item("reactorChamber", 64))
            .itemOutputs(TNItemList.NuclearPowerPlant.get(1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);
        // Heat Exchanger Machine
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Machine_Multi_HeatExchanger.get(16),
                ItemList.Casing_SolidSteel.get(64),
                ItemList.Casing_Pipe_Bronze.get(64),
                GTModHandler.getIC2Item("reactorHeatSwitchDiamond", 6))
            .itemOutputs(TNItemList.HeatExchangeStation.get(1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);
        // Steam Turbine Machine
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.SteamTurbine.get(64),
                ItemList.Casing_SolidSteel.get(64),
                GTOreDictUnificator.get(OrePrefixes.gear, Materials.Steel, 64))
            .itemOutputs(TNItemList.LargeSteamTurbine.get(1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);
        // Turbine Blade
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.rotor, Materials.Steel, 1),
                GTOreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Steel, 4))
            .itemOutputs(TNItemList.TurbineBlade.get(1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);
        // Turbine Shaft Tier 1
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Electric_Motor_MV.get(2),
                ItemList.Casing_Gearbox_Steel.get(1),
                GGMaterial.incoloy903.get(OrePrefixes.plateDouble, 4))
            .fluidInputs(Materials.Lubricant.getFluid(2000))
            .itemOutputs(TNItemList.LowPressureTurbineShaft.get(1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(RecipeMaps.assemblerRecipes);
        // Turbine Shaft Tier 2
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Electric_Motor_EV.get(2),
                ItemList.Casing_Gearbox_TungstenSteel.get(1),
                MaterialsAlloy.INCOLOY_MA956.getIngot(4))
            .fluidInputs(Materials.Lubricant.getFluid(2000))
            .itemOutputs(TNItemList.HighPressureTurbineShaft.get(1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_IV)
            .addTo(RecipeMaps.assemblerRecipes);
        // Fuel Rod Tier 1
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.IC2_Fuel_Rod_Empty.get(32),
                GGMaterial.zircaloy4.get(OrePrefixes.frameGt, 32),
                GGMaterial.zircaloy4.get(OrePrefixes.ring, 64),
                GGMaterial.zircaloy4.get(OrePrefixes.ring, 64))
            .itemOutputs(TNItemList.FuelRodTier1.get(1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);
        // Fuel Rod Tier 2
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.IC2_Fuel_Rod_Empty.get(64),
                GGMaterial.zircaloy4.get(OrePrefixes.frameGt, 64),
                ItemList.Neutron_Reflector.get(8),
                ItemList.Electric_Pump_IV.get(8),
                TNItemList.FuelRodTier1.get(1))
            .itemOutputs(TNItemList.FuelRodTier2.get(1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);
        // Fuel Rod Tier 3
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemRefer.Advanced_Fuel_Rod.get(64),
                ItemList.Neutron_Reflector.get(16),
                ItemList.Electric_Pump_LuV.get(8),
                TNItemList.FuelRodTier2.get(1))
            .itemOutputs(TNItemList.FuelRodTier3.get(1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);
        // Fuel Rod Tier 4
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemRefer.Advanced_Fuel_Rod.get(64),
                ItemRefer.Advanced_Fuel_Rod.get(64),
                ItemList.Neutron_Reflector.get(32),
                ItemList.Electric_Pump_ZPM.get(8),
                TNItemList.FuelRodTier3.get(1))
            .itemOutputs(TNItemList.FuelRodTier4.get(1))
            .duration(10 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.assemblerRecipes);
        // Breeder Input Bus
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.IC2_Fuel_Rod_Empty.get(8),
                ItemList.Hull_HV.get(1),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Thorium, 8))
            .circuit(1)
            .itemOutputs(TNItemList.BreederInputBus.get(1))
            .duration(100)
            .eut(TierEU.RECIPE_HV)
            .addTo(RecipeMaps.assemblerRecipes);

        GTModHandler.addShapelessCraftingRecipe(
            TNItemList.BreederInputBus.get(1),
            GTModHandler.RecipeBits.BITS | GTModHandler.RecipeBits.DO_NOT_CHECK_FOR_COLLISIONS,
            new Object[] { TNItemList.BreederOutputBus.get(1), ToolDictNames.craftingToolScrewdriver });
        // Breeder Output Bus
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.IC2_Fuel_Rod_Empty.get(8),
                ItemList.Hull_HV.get(1),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Thorium, 8))
            .circuit(2)
            .itemOutputs(TNItemList.BreederOutputBus.get(1))
            .duration(100)
            .eut(TierEU.RECIPE_HV)
            .addTo(RecipeMaps.assemblerRecipes);

        GTModHandler.addShapelessCraftingRecipe(
            TNItemList.BreederOutputBus.get(1),
            GTModHandler.RecipeBits.BITS | GTModHandler.RecipeBits.DO_NOT_CHECK_FOR_COLLISIONS,
            new Object[] { TNItemList.BreederInputBus.get(1), ToolDictNames.craftingToolScrewdriver });
        // Containment Casing
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Steel, 9),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 12))
            .itemOutputs(TNItemList.ContainmentCasing.get(9))
            .fluidInputs(Materials.Concrete.getMolten(INGOTS * 9 * 2))
            .duration(30 * SECONDS)
            .eut(4)
            .addTo(RecipeMaps.chemicalBathRecipes);
    }
}
