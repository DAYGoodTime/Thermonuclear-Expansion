package com.lin.thermonuclear.recipe;

import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.RecipeMapFrontend;
import gregtech.nei.RecipeDisplayInfo;

public final class SteamTurbineRecipeFrontend extends RecipeMapFrontend {

    public SteamTurbineRecipeFrontend(BasicUIPropertiesBuilder ui, NEIRecipePropertiesBuilder nei) {
        super(ui, nei);
    }

    @Override
    protected void drawEnergyInfo(RecipeDisplayInfo info) {
        // The display recipe has no EU input; its total generated EU is shown by the special formatter.
    }

    @Override
    protected void drawDurationInfo(RecipeDisplayInfo info) {
        // 1000 L describes a conversion ratio, not a fixed-throughput 20-tick production recipe.
    }
}
