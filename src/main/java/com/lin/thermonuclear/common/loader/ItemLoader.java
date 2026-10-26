package com.lin.thermonuclear.common.loader;

import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;
import com.lin.thermonuclear.api.TNCItemList;
import com.lin.thermonuclear.common.item.ItemNaquadahFuelRod;
import com.lin.thermonuclear.common.item.ItemRareEarthBreederRod;
import com.lin.thermonuclear.common.item.ItemWirelessUpgradeChip;
import com.lin.thermonuclear.common.nuclear.breeding.RareEarthBreedingRoute;
import com.lin.thermonuclear.common.registry.NaquadahFuelTier;

public final class ItemLoader {

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
        Map<NaquadahFuelTier, ItemNaquadahFuelRod> naquadahFuelRods = ItemNaquadahFuelRod.registerVariants();
        for (NaquadahFuelTier tier : NaquadahFuelTier.values()) {
            ItemNaquadahFuelRod rod = naquadahFuelRods.get(tier);
            TNCItemList.naquadahFuelRod(tier, false)
                .set(rod);
            TNCItemList.naquadahFuelRod(tier, true)
                .set(rod.depletedStack());
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

    public static ItemNaquadahFuelRod naquadahFuelRod(NaquadahFuelTier tier) {
        return (ItemNaquadahFuelRod) TNCItemList.naquadahFuelRod(tier, false)
            .getItem();
    }

    public static Item depletedNaquadahFuelRod(NaquadahFuelTier tier) {
        return TNCItemList.naquadahFuelRod(tier, true)
            .getItem();
    }
}
