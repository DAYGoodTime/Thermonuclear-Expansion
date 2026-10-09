package com.lin.thermonuclear.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;

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
        List<ItemStack> displays = new ArrayList<>();
        for (Fluid fluid : new Fluid[] { ModFluids.ultraSupercriticalSteam, ModFluids.nakCompositeCoolant,
            ModFluids.hotNakCompositeCoolant }) {
            ItemStack display = GTUtility.getFluidDisplayStack(fluid);
            if (display != null) displays.add(display);
        }
        if (displays.isEmpty()) return;
        ItemStack display = displays.get(0);
        // NEI entries override this item's default variants; preserve GT's debug fluid list too.
        List<ItemStack> defaults = new ArrayList<>();
        display.getItem()
            .getSubItems(display.getItem(), null, defaults);
        for (ItemStack stack : defaults) {
            API.addItemListEntry(stack);
        }
        for (ItemStack entry : displays) {
            boolean included = defaults.stream()
                .anyMatch(stack -> stack.getItemDamage() == entry.getItemDamage());
            if (!included) API.addItemListEntry(entry);
        }
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
