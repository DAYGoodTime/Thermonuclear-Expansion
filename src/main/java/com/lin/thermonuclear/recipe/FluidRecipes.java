package com.lin.thermonuclear.recipe;

import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraftforge.fluids.FluidStack;

import com.lin.thermonuclear.api.ModRecipeLoader;
import com.lin.thermonuclear.registry.ModFluids;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;

public class FluidRecipes implements ModRecipeLoader {

    @Override
    public void loadRecipes() {
        // Na-K-Super-Coolant only mix in industrial mixer map, not the single-block mixer map
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Redstone, 64),
                GTOreDictUnificator.get(OrePrefixes.dust, Materials.Glowstone, 64))
            .fluidInputs(Materials.SodiumPotassium.getFluid(1000), Materials.SuperCoolant.getFluid(1000))
            .fluidOutputs(new FluidStack(ModFluids.nakCompositeCoolant, 1000))
            .duration(20 * SECONDS)
            .eut(TierEU.RECIPE_LuV)
            .addTo(RecipeMaps.mixerNonCellRecipes);
    }
}
