package com.lin.thermonuclear.recipe;

import java.util.Arrays;

import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.lin.thermonuclear.loader.MachineLoader;
import com.lin.thermonuclear.registry.WorkingFluids;

import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;

public final class HeatExchangeRecipes {

    public static final int DISPLAY_HOT_LITRES = 1000;
    public static final RecipeMap<RecipeMapBackend> DISPLAY = RecipeMapBuilder.of("thermonuclear.recipe.heat_exchange")
        .maxIO(0, 0, 2, 2)
        .minInputs(0, 2)
        .dontUseProgressBar()
        .frontend(HeatExchangeRecipeFrontend::new)
        .neiHandlerInfo(
            builder -> builder.setDisplayStack(MachineLoader.heatExchangeStation)
                .setHeight(190)
                .setMultipleWidgetsAllowed(false))
        .neiSpecialInfoFormatter(
            info -> Arrays.asList(
                StatCollector.translateToLocalFormatted(
                    "thermonuclear.nei.heat_exchange_water",
                    HeatExchangeBatch.STEAM_PER_WATER))).build();
    private static boolean registered;

    private HeatExchangeRecipes() {}

    public static void register() {
        if (registered) return;
        registered = true;
        if (WorkingFluids.distilledWater == null) return;
        for (HeatExchangeRecipe recipe : HeatExchangeRecipe.values()) {
            if (recipe.hot() == null || recipe.cold() == null) continue;
            for (HeatExchangeSteam steam : HeatExchangeSteam.values()) {
                if (!recipe.supportsSteam(steam) || steam.fluid() == null) continue;
                // Preview a fresh 1000 L batch using the same rounding and prepaid-water logic as the machine.
                HeatExchangeBatch batch = HeatExchangeBatch
                    .forHot(DISPLAY_HOT_LITRES, recipe.steamPerHotCoolant(steam), 0, 0);
                if (batch == null) continue;
                GTValues.RA.stdBuilder()
                    .fluidInputs(
                        new FluidStack(recipe.hot(), batch.hot()),
                        new FluidStack(WorkingFluids.distilledWater, batch.water()))
                    .fluidOutputs(
                        new FluidStack(recipe.cold(), batch.hot()),
                        new FluidStack(steam.fluid(), batch.steam()))
                    .eut(0)
                    .duration(20)
                    .specialValue(batch.waterCredit())
                    .fake()
                    .addTo(DISPLAY);
            }
        }
    }
}
