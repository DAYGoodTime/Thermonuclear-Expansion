package com.lin.thermonuclear.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.lin.thermonuclear.Tags;
import com.lin.thermonuclear.registry.ModFluids;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.util.GTUtility;

@SideOnly(Side.CLIENT)
public final class NEIThermonuclearConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        // Forge registration alone does not expose a fluid in the non-debug GT item list.
        ItemStack display = GTUtility.getFluidDisplayStack(ModFluids.ultraSupercriticalSteam);
        if (display == null) return;
        // NEI entries override this item's default variants; preserve GT's debug fluid list too.
        List<ItemStack> defaults = new ArrayList<>();
        display.getItem()
            .getSubItems(display.getItem(), null, defaults);
        boolean included = false;
        for (ItemStack stack : defaults) {
            API.addItemListEntry(stack);
            if (stack.getItemDamage() == display.getItemDamage()) included = true;
        }
        if (!included) API.addItemListEntry(display);
    }

    @Override
    public String getName() {
        return "Thermonuclear NEI Plugin";
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }
}
