package com.lin.thermonuclear.common.recipe.steamturbine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.lin.thermonuclear.api.ModRecipeLoader;
import com.lin.thermonuclear.api.SteamTurbineFuel;
import com.lin.thermonuclear.api.SteamTurbineFuels;
import com.lin.thermonuclear.api.TNCItemList;
import com.lin.thermonuclear.client.nei.NEIFluidDisplays;

import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.api.recipe.metadata.SimpleRecipeMetadataKey;

public class SteamTurbineRecipes implements ModRecipeLoader {

    public static final int DISPLAY_LITRES = 1000;
    public static final RecipeMetadataKey<Integer> REQUIRE_SHAFT_TIER = SimpleRecipeMetadataKey
        .create(Integer.class, "require_shaft_tier");
    public static final RecipeMap<RecipeMapBackend> SteamTurbineDisplayMap = RecipeMapBuilder
        .of("thermonuclear.recipe.steam_turbine")
        .maxIO(0, 0, 1, 0)
        .minInputs(0, 1)
        .dontUseProgressBar()
        .frontend(SteamTurbineRecipeFrontend::new)
        .neiFluidInputsGetter(recipe -> NEIFluidDisplays.available(recipe.mFluidInputs))
        .neiHandlerInfo(builder -> builder.setDisplayStack(TNCItemList.LargeSteamTurbine.get(1)))
        .neiSpecialInfoFormatter(info -> {
            List<String> lines = new ArrayList<>();
            lines.add(
                StatCollector.translateToLocalFormatted(
                    "thermonuclear.nei.steam_energy",
                    Double.toString(info.recipe.mSpecialValue / (double) DISPLAY_LITRES),
                    info.recipe.mSpecialValue));
            if (info.recipe.getMetadataOrDefault(REQUIRE_SHAFT_TIER, 1) == 2) {
                lines.add(StatCollector.translateToLocal("thermonuclear.nei.steam_modifiers"));
            }
            return lines;
        })
        .build();

    @Override
    public void loadRecipes() {
        for (SteamTurbineFuel fuel : SteamTurbineFuels.getAllFuels()) {
            Fluid fuelFluid = fuel.getWorkingFluid();
            if (fuelFluid == null) continue;
            // This map is documentation only; the turbine's transactional cycle performs the conversion.
            GTValues.RA.stdBuilder()
                .fluidInputs(new FluidStack(fuelFluid, DISPLAY_LITRES))
                .eut(0)
                .duration(20)
                .specialValue((int) (fuel.euPerLitre() * DISPLAY_LITRES))
                .metadata(REQUIRE_SHAFT_TIER, fuel.requireShaftTier())
                .fake()
                .addTo(SteamTurbineDisplayMap);
        }
    }
}
