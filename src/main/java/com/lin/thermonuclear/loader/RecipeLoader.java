package com.lin.thermonuclear.loader;

import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.item.ItemStack;

import goodgenerator.items.GGMaterial;
import goodgenerator.util.ItemRefer;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;

public final class RecipeLoader {

    private RecipeLoader() {}

    public static void register() {
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
