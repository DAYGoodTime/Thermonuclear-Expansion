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

public final class ItemLoader {

    private static final TNCItemList[] NAQUADAH_FUEL_RODS = { TNCItemList.NaquadahFuelRodMkI,
        TNCItemList.NaquadahFuelRodMkII, TNCItemList.NaquadahFuelRodMkIII, TNCItemList.NaquadahFuelRodMkIV,
        TNCItemList.NaquadahFuelRodMkV, TNCItemList.NaquadahFuelRodMkVI };
    private static final TNCItemList[] DEPLETED_NAQUADAH_FUEL_RODS = { TNCItemList.DepletedNaquadahFuelRodMkI,
        TNCItemList.DepletedNaquadahFuelRodMkII, TNCItemList.DepletedNaquadahFuelRodMkIII,
        TNCItemList.DepletedNaquadahFuelRodMkIV, TNCItemList.DepletedNaquadahFuelRodMkV,
        TNCItemList.DepletedNaquadahFuelRodMkVI };

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
            route.workingRod()
                .set(rareEarthBreederRods.get(route));
            route.bredRod()
                .set(bredRareEarthRods.get(route));
        }
        for (int tier = 1; tier <= ItemNaquadahFuelRod.TIER_COUNT; tier++) {
            ItemNaquadahFuelRod rod = new ItemNaquadahFuelRod(tier);
            register(new ItemStack(rod));
            NAQUADAH_FUEL_RODS[tier - 1].set(rod);
            DEPLETED_NAQUADAH_FUEL_RODS[tier - 1].set(rod.depletedStack());
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

    public static ItemNaquadahFuelRod naquadahFuelRod(int tier) {
        checkNaquadahFuelTier(tier);
        return (ItemNaquadahFuelRod) NAQUADAH_FUEL_RODS[tier - 1].getItem();
    }

    public static Item depletedNaquadahFuelRod(int tier) {
        checkNaquadahFuelTier(tier);
        return DEPLETED_NAQUADAH_FUEL_RODS[tier - 1].getItem();
    }

    private static void checkNaquadahFuelTier(int tier) {
        if (tier < 1 || tier > ItemNaquadahFuelRod.TIER_COUNT) {
            throw new IllegalArgumentException("Unknown naquadah fuel rod tier: " + tier);
        }
    }
}
