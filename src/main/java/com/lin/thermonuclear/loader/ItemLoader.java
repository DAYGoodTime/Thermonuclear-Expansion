package com.lin.thermonuclear.loader;

import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;
import com.lin.thermonuclear.item.ItemRareEarthBreederRod;
import com.lin.thermonuclear.item.ItemWirelessUpgradeChip;
import com.lin.thermonuclear.machine.nuclear.RareEarthBreedingRoute;
import com.lin.thermonuclear.registry.TNItemList;

public final class ItemLoader {

    private ItemLoader() {}

    public static void register() {
        ItemWirelessUpgradeChip wirelessUpgradeChip = new ItemWirelessUpgradeChip();
        wirelessUpgradeChip.register();
        TNItemList.WirelessUpgradeChip.set(wirelessUpgradeChip);
        Map<RareEarthBreedingRoute, ItemRareEarthBreederRod> rareEarthBreederRods = ItemRareEarthBreederRod
            .registerVariants(false);
        Map<RareEarthBreedingRoute, ItemRareEarthBreederRod> bredRareEarthRods = ItemRareEarthBreederRod
            .registerVariants(true);
        for (RareEarthBreedingRoute route : RareEarthBreedingRoute.values()) {
            TNItemList.rareEarthRod(route, false)
                .set(rareEarthBreederRods.get(route));
            TNItemList.rareEarthRod(route, true)
                .set(bredRareEarthRods.get(route));
        }
    }

    public static ItemStack register(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            throw new IllegalArgumentException("Cannot register an empty Thermonuclear item");
        }
        // GT controllers share an item with other mods; register exact metadata, never a wildcard.
        AnimatedTooltipHandler.addItemTooltip(
            stack,
            () -> EnumChatFormatting.AQUA + StatCollector.translateToLocal("thermonuclear.tooltip.mod"));
        return stack;
    }
}
