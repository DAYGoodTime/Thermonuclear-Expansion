package com.lin.thermonuclear.recipe;

import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.lin.thermonuclear.api.ModRecipeLoader;
import com.lin.thermonuclear.machine.nuclear.RareEarthBreedingRoute;
import com.lin.thermonuclear.api.TNCItemList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;

public class NuclearRecipes implements ModRecipeLoader {

    @Override
    public void loadRecipes() {
        // Load All Breeding Material Recipes
        for (RareEarthBreedingRoute route : RareEarthBreedingRoute.values()) {
            ItemStack inputDust = route.inputMaterial()
                .getDust(1);
            ItemStack outputDust = route.outputMaterial()
                .getDust(1);
            if (inputDust == null || outputDust == null) {
                throw new IllegalStateException("Missing elemental dust for " + route.itemName(false));
            }
            ItemStack workingRod = TNCItemList.rareEarthRod(route, false)
                .get(1);
            ItemStack completedRod = TNCItemList.rareEarthRod(route, true)
                .get(1);

            GTValues.RA.stdBuilder()
                .itemInputs(ItemList.IC2_Fuel_Rod_Empty.get(1), inputDust)
                .itemOutputs(workingRod.copy())
                .duration(10 * SECONDS)
                .eut(TierEU.RECIPE_LV)
                .addTo(RecipeMaps.cannerRecipes);

            GTValues.RA.stdBuilder()
                .itemInputs(completedRod.copy())
                .itemOutputs(outputDust, ItemList.IC2_Fuel_Rod_Empty.get(1))
                .duration(25 * SECONDS)
                .eut(TierEU.RECIPE_MV)
                .addTo(RecipeMaps.centrifugeRecipes);

            // Display only: the independent breeder pool performs this element-only conversion.
            GTValues.RA.stdBuilder()
                .itemInputs(workingRod)
                .itemOutputs(completedRod)
                .setNEIDesc(
                    StatCollector.translateToLocal("thermonuclear.nei.rare_earth_breeding.0"),
                    StatCollector.translateToLocal("thermonuclear.nei.rare_earth_breeding.1"),
                    StatCollector.translateToLocal("thermonuclear.nei.rare_earth_breeding.2"))
                .duration(0)
                .eut(0)
                .addTo(RecipeMaps.ic2NuclearFakeRecipes);
        }
    }
}
