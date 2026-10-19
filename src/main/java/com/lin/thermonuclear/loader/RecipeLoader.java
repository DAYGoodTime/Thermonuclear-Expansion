package com.lin.thermonuclear.loader;

import com.lin.thermonuclear.api.ModRecipeLoader;
import com.lin.thermonuclear.recipe.BlockRecipes;
import com.lin.thermonuclear.recipe.FluidRecipes;
import com.lin.thermonuclear.recipe.NuclearRecipes;

public final class RecipeLoader {

    private RecipeLoader() {}

    public static void register() {
        ModRecipeLoader[] loaders = new ModRecipeLoader[] { new BlockRecipes(), new FluidRecipes(),
            new NuclearRecipes() };
        // load all recipes
        for (ModRecipeLoader loader : loaders) {
            loader.loadRecipes();
        }
    }
}
