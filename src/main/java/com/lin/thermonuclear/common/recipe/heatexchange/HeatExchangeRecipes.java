package com.lin.thermonuclear.common.recipe.heatexchange;

import java.util.Arrays;

import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.lin.thermonuclear.api.CoolantType;
import com.lin.thermonuclear.api.HeatExchangeRule;
import com.lin.thermonuclear.api.HeatExchangeRules;
import com.lin.thermonuclear.api.ModRecipeLoader;
import com.lin.thermonuclear.api.TNCItemList;
import com.lin.thermonuclear.client.nei.NEIFluidDisplays;
import com.lin.thermonuclear.common.registry.SteamType;
import com.lin.thermonuclear.common.registry.WorkingFluids;

import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;

public final class HeatExchangeRecipes implements ModRecipeLoader {

    public static final int DISPLAY_HOT_LITRES = 1000;

    public static final RecipeMap<RecipeMapBackend> DISPLAY = RecipeMapBuilder.of("thermonuclear.recipe.heat_exchange")
        .maxIO(0, 0, 2, 2)
        .minInputs(0, 2)
        .dontUseProgressBar()
        .frontend(HeatExchangeRecipeFrontend::new)
        .neiFluidInputsGetter(recipe -> NEIFluidDisplays.available(recipe.mFluidInputs))
        .neiFluidOutputsGetter(recipe -> NEIFluidDisplays.available(recipe.mFluidOutputs))
        .neiHandlerInfo(
            builder -> builder.setDisplayStack(TNCItemList.HeatExchangeStation.get(1))
                .setHeight(190)
                .setMultipleWidgetsAllowed(false))
        .neiSpecialInfoFormatter(
            info -> Arrays.asList(
                StatCollector.translateToLocalFormatted(
                    "thermonuclear.nei.heat_exchange_water",
                    HeatExchangeBatch.STEAM_PER_WATER)))
        .build();

    @Override
    public void loadRecipes() {
        for (HeatExchangeRule recipe : HeatExchangeRules.getAllRules()) {
            CoolantType coolant = recipe.coolant();
            SteamType steam = recipe.steam();
            HeatExchangeBatch batch = HeatExchangeBatch.calcUsage(DISPLAY_HOT_LITRES, Integer.MAX_VALUE, recipe);
            if (batch == null) continue;
            GTValues.RA.stdBuilder()
                .fluidInputs(
                    new FluidStack(coolant.hot(), batch.hot()),
                    new FluidStack(WorkingFluids.distilledWater, batch.water()))
                .fluidOutputs(new FluidStack(coolant.cold(), batch.hot()), new FluidStack(steam.fluid(), batch.steam()))
                .duration(20)
                .eut(0)
                .fake()
                .addTo(DISPLAY);
        }
    }
}
