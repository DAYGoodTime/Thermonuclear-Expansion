package com.lin.thermonuclear.common.loader;

import com.lin.thermonuclear.api.ModRecipeLoader;
import com.lin.thermonuclear.common.recipe.BlockRecipes;
import com.lin.thermonuclear.common.recipe.FluidRecipes;
import com.lin.thermonuclear.common.recipe.NaquadahFuelRodRecipes;
import com.lin.thermonuclear.common.recipe.NuclearRecipes;
import com.lin.thermonuclear.common.recipe.heatexchange.HeatExchangeRecipes;
import com.lin.thermonuclear.common.recipe.steamturbine.SteamTurbineRecipes;

public final class RecipeLoader {

    private RecipeLoader() {}

    public static void register() {
        ModRecipeLoader[] loaders = new ModRecipeLoader[] { new BlockRecipes(), new FluidRecipes(),
            new NuclearRecipes(), new NaquadahFuelRodRecipes(), new HeatExchangeRecipes(), new SteamTurbineRecipes() };
        // load all recipes
        for (ModRecipeLoader loader : loaders) {
            loader.loadRecipes();
        }
    }
}
