package com.lin.thermonuclear.recipe;

import java.util.Arrays;

import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.lin.thermonuclear.loader.MachineLoader;

import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;

public final class SteamTurbineRecipes {

    public static final int DISPLAY_LITRES = 1000;
    public static final RecipeMap<RecipeMapBackend> DISPLAY = RecipeMapBuilder.of("thermonuclear.recipe.steam_turbine")
        .maxIO(0, 0, 1, 0)
        .minInputs(0, 1)
        .dontUseProgressBar()
        .frontend(SteamTurbineRecipeFrontend::new)
        .neiHandlerInfo(builder -> builder.setDisplayStack(MachineLoader.largeSteamTurbine))
        .neiSpecialInfoFormatter(
            info -> Arrays.asList(
                StatCollector.translateToLocalFormatted(
                    "thermonuclear.nei.steam_energy",
                    Double.toString(info.recipe.mSpecialValue / (double) DISPLAY_LITRES),
                    info.recipe.mSpecialValue),
                StatCollector.translateToLocal("thermonuclear.nei.steam_modifiers")))
        .build();

    private static boolean registered;

    private SteamTurbineRecipes() {}

    public static void register() {
        if (registered) return;
        registered = true;
        for (SteamTurbineFuel fuel : SteamTurbineFuel.values()) {
            if (fuel.fluid() == null) continue;
            // This map is documentation only; the turbine's transactional cycle performs the conversion.
            GTValues.RA.stdBuilder()
                .fluidInputs(new FluidStack(fuel.fluid(), DISPLAY_LITRES))
                .eut(0)
                .duration(20)
                .specialValue(fuel.displayEnergy(DISPLAY_LITRES))
                .fake()
                .addTo(DISPLAY);
        }
    }
}
