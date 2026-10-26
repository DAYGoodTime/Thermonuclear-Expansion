package com.lin.thermonuclear.loader;

import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;
import com.lin.thermonuclear.api.TNCItemList;
import com.lin.thermonuclear.item.ItemDepletedNaquadahFuelRod;
import com.lin.thermonuclear.item.ItemNaquadahFuelRod;
import com.lin.thermonuclear.item.ItemRareEarthBreederRod;
import com.lin.thermonuclear.item.ItemWirelessUpgradeChip;
import com.lin.thermonuclear.machine.nuclear.RareEarthBreedingRoute;
import com.lin.thermonuclear.registry.NaquadahFuelTier;

public final class ItemLoader {

    private static Map<NaquadahFuelTier, ItemNaquadahFuelRod> naquadahFuelRods;
    private static Map<NaquadahFuelTier, ItemDepletedNaquadahFuelRod> depletedNaquadahFuelRods;

    private ItemLoader() {}

    public static void register() {
        ItemWirelessUpgradeChip wirelessUpgradeChip = new ItemWirelessUpgradeChip();
        wirelessUpgradeChip.register();
        TNCItemList.WirelessUpgradeChip.set(wirelessUpgradeChip);
        Map<RareEarthBreedingRoute, ItemRareEarthBreederRod> rareEarthBreederRods = ItemRareEarthBreederRod
            .registerVariants(false);
        Map<RareEarthBreedingRoute, ItemRareEarthBreederRod> bredRareEarthRods = ItemRareEarthBreederRod
            .registerVariants(true);
        for (RareEarthBreedingRoute route : RareEarthBreedingRoute.values()) {
            TNCItemList.rareEarthRod(route, false)
                .set(rareEarthBreederRods.get(route));
            TNCItemList.rareEarthRod(route, true)
                .set(bredRareEarthRods.get(route));
        }
        depletedNaquadahFuelRods = ItemDepletedNaquadahFuelRod.registerVariants();
        naquadahFuelRods = ItemNaquadahFuelRod.registerVariants();
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

    public static ItemNaquadahFuelRod naquadahFuelRod(NaquadahFuelTier tier) {
        ItemNaquadahFuelRod item = naquadahFuelRods.get(tier);
        if (item == null) throw new IllegalArgumentException("Unknown naquadah fuel tier: " + tier);
        return item;
    }

    public static ItemDepletedNaquadahFuelRod depletedNaquadahFuelRod(NaquadahFuelTier tier) {
        ItemDepletedNaquadahFuelRod item = depletedNaquadahFuelRods.get(tier);
        if (item == null) throw new IllegalArgumentException("Unknown depleted naquadah fuel tier: " + tier);
        return item;
    }
}
