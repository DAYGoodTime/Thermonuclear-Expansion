package com.lin.thermonuclear.recipe;

import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.lin.thermonuclear.api.ModRecipeLoader;
import com.lin.thermonuclear.loader.ItemLoader;
import com.lin.thermonuclear.registry.NaquadahFuelTier;

import bartworks.system.material.Werkstoff;
import goodgenerator.items.GGMaterial;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;

public final class NaquadahFuelRodRecipes implements ModRecipeLoader {

    @Override
    public void loadRecipes() {
        for (NaquadahFuelTier tier : NaquadahFuelTier.values()) {
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
                .eut(TierEU.RECIPE_LV)
                .addTo(RecipeMaps.cannerRecipes);

            // Only depleted items can be drained; used working rods cannot recover fresh fuel.
            GTValues.RA.stdBuilder()
                .itemInputs(depletedRod.copy())
                .itemOutputs(ItemList.IC2_Fuel_Rod_Empty.get(1))
                .fluidOutputs(depletedFuel)
                .duration(10 * SECONDS)
                .eut(TierEU.RECIPE_LV)
                .addTo(RecipeMaps.cannerRecipes);

            // Display only: FuelRodAdapters and the reactor's transactional batch logic perform consumption.
            GTValues.RA.stdBuilder()
                .itemInputs(workingRod)
                .itemOutputs(depletedRod)
                .setNEIDesc(
                    StatCollector.translateToLocal("thermonuclear.nei.naquadah_fuel_rod.use"),
                    StatCollector
                        .translateToLocalFormatted("thermonuclear.nei.naquadah_fuel_rod.cycles", tier.maxCycles()),
                    StatCollector
                        .translateToLocalFormatted("thermonuclear.tooltip.naquadah_fuel_rod.direct", tier.directEUt()),
                    StatCollector.translateToLocalFormatted(
                        "thermonuclear.tooltip.naquadah_fuel_rod.heat",
                        tier.baseHeatPerCycle()),
                    StatCollector.translateToLocal("thermonuclear.tooltip.naquadah_fuel_rod.use"),
                    StatCollector.translateToLocal("thermonuclear.tooltip.naquadah_fuel_rod.reflection"))
                .duration(tier.maxCycles() * SECONDS)
                .eut(0)
                .fake()
                .addTo(RecipeMaps.ic2NuclearFakeRecipes);
        }
    }

    private static FluidStack fluid(NaquadahFuelTier tier, boolean depleted) {
        Werkstoff material = switch (tier) {
            case MK_I -> depleted ? GGMaterial.naquadahBasedFuelMkIDepleted : GGMaterial.naquadahBasedFuelMkI;
            case MK_II -> depleted ? GGMaterial.naquadahBasedFuelMkIIDepleted : GGMaterial.naquadahBasedFuelMkII;
            case MK_III -> depleted ? GGMaterial.naquadahBasedFuelMkIIIDepleted : GGMaterial.naquadahBasedFuelMkIII;
            case MK_IV -> depleted ? GGMaterial.naquadahBasedFuelMkIVDepleted : GGMaterial.naquadahBasedFuelMkIV;
            case MK_V -> depleted ? GGMaterial.naquadahBasedFuelMkVDepleted : GGMaterial.naquadahBasedFuelMkV;
            case MK_VI -> depleted ? GGMaterial.naquadahBasedFuelMkVIDepleted : GGMaterial.naquadahBasedFuelMkVI;
        };
        FluidStack stack = material.getFluidOrGas(NaquadahFuelTier.FUEL_LITRES);
        if (stack == null || stack.getFluid() == null)
            throw new IllegalStateException("Missing naquadah fuel fluid for " + tier + ", depleted=" + depleted);
        return stack;
    }
}
