package com.lin.thermonuclear.common.recipe;

import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.lin.thermonuclear.api.ModRecipeLoader;
import com.lin.thermonuclear.common.item.ItemNaquadahFuelRod;
import com.lin.thermonuclear.common.loader.ItemLoader;

import bartworks.system.material.Werkstoff;
import goodgenerator.items.GGMaterial;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;

public final class NaquadahFuelRodRecipes implements ModRecipeLoader {

    @Override
    public void loadRecipes() {
        for (int tier = 1; tier <= ItemNaquadahFuelRod.TIER_COUNT; tier++) {
            ItemStack workingRod = new ItemStack(ItemLoader.naquadahFuelRod(tier));
            ItemStack depletedRod = new ItemStack(ItemLoader.depletedNaquadahFuelRod(tier));
            FluidStack fuel = fluid(tier, false);
            FluidStack depletedFuel = fluid(tier, true);

            // Prototype processing cost follows this mod's existing LV, 10-second canner recipes.
            GTValues.RA.stdBuilder()
                .itemInputs(ItemList.IC2_Fuel_Rod_Empty.get(1))
                .fluidInputs(fuel)
                .itemOutputs(workingRod.copy())
                .duration(10 * SECONDS)
                .eut(TierEU.RECIPE_ZPM)
                .addTo(RecipeMaps.cannerRecipes);

            // Only depleted items can be drained; used working rods cannot recover fresh fuel.
            GTValues.RA.stdBuilder()
                .itemInputs(depletedRod.copy())
                .itemOutputs(ItemList.IC2_Fuel_Rod_Empty.get(1))
                .fluidOutputs(depletedFuel)
                .duration(10 * SECONDS)
                .eut(TierEU.RECIPE_ZPM)
                .addTo(RecipeMaps.centrifugeRecipes);

        }
    }

    private static FluidStack fluid(int tier, boolean depleted) {
        Werkstoff material = switch (tier) {
            case 1 -> depleted ? GGMaterial.naquadahBasedFuelMkIDepleted : GGMaterial.naquadahBasedFuelMkI;
            case 2 -> depleted ? GGMaterial.naquadahBasedFuelMkIIDepleted : GGMaterial.naquadahBasedFuelMkII;
            case 3 -> depleted ? GGMaterial.naquadahBasedFuelMkIIIDepleted : GGMaterial.naquadahBasedFuelMkIII;
            case 4 -> depleted ? GGMaterial.naquadahBasedFuelMkIVDepleted : GGMaterial.naquadahBasedFuelMkIV;
            case 5 -> depleted ? GGMaterial.naquadahBasedFuelMkVDepleted : GGMaterial.naquadahBasedFuelMkV;
            case 6 -> depleted ? GGMaterial.naquadahBasedFuelMkVIDepleted : GGMaterial.naquadahBasedFuelMkVI;
            default -> throw new IllegalArgumentException("Unknown naquadah fuel rod tier: " + tier);
        };
        FluidStack stack = material.getFluidOrGas(ItemNaquadahFuelRod.FUEL_LITRES);
        if (stack == null || stack.getFluid() == null)
            throw new IllegalStateException("Missing naquadah fuel fluid for " + tier + ", depleted=" + depleted);
        return stack;
    }
}
