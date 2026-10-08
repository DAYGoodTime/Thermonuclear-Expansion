package com.lin.thermonuclear.recipe;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.RecipeMapFrontend;
import gregtech.nei.RecipeDisplayInfo;

public final class HeatExchangeRecipeFrontend extends RecipeMapFrontend {

    public HeatExchangeRecipeFrontend(BasicUIPropertiesBuilder ui, NEIRecipePropertiesBuilder nei) {
        super(ui, nei);
    }

    @Override
    protected void drawEnergyInfo(RecipeDisplayInfo info) {
        // This page documents fluid conversion, not an electrical recipe.
    }

    @Override
    protected void drawDurationInfo(RecipeDisplayInfo info) {
        // A normalized batch does not prescribe machine throughput or a fixed processing time.
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void drawSpecialInfo(RecipeDisplayInfo info) {
        // GT's default formatter draws unwrapped lines; both languages must fit the recipe panel.
        for (String text : getNEIProperties().neiSpecialInfoFormatter.format(info)) {
            info.drawTextMultipleLines(Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(text, 160));
        }
    }
}
