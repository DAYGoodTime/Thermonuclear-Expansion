package com.lin.thermonuclear.loader;

import java.util.Map;

import com.lin.thermonuclear.item.ItemRareEarthBreederRod;
import com.lin.thermonuclear.item.ItemWirelessUpgradeChip;
import com.lin.thermonuclear.machine.nuclear.RareEarthBreedingRoute;

public final class ItemLoader {

    private static Map<RareEarthBreedingRoute, ItemRareEarthBreederRod> rareEarthBreederRods;
    private static Map<RareEarthBreedingRoute, ItemRareEarthBreederRod> bredRareEarthRods;
    public static ItemWirelessUpgradeChip wirelessUpgradeChip;

    private ItemLoader() {}

    public static void register() {
        wirelessUpgradeChip = new ItemWirelessUpgradeChip();
        wirelessUpgradeChip.register();
        rareEarthBreederRods = ItemRareEarthBreederRod.registerVariants(false);
        bredRareEarthRods = ItemRareEarthBreederRod.registerVariants(true);
    }

    public static ItemRareEarthBreederRod rareEarthRod(RareEarthBreedingRoute route, boolean bred) {
        ItemRareEarthBreederRod item = (bred ? bredRareEarthRods : rareEarthBreederRods).get(route);
        if (item == null) throw new IllegalArgumentException("Unknown rare earth breeding route: " + route);
        return item;
    }
}
