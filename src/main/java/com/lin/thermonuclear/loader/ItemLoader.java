package com.lin.thermonuclear.loader;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.lin.thermonuclear.item.ItemRareEarthBreederRod;
import com.lin.thermonuclear.nuclear.RareEarthBreedingRoute;
import com.lin.thermonuclear.registry.ModItems;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ItemLoader {

    private static final Map<RareEarthBreedingRoute, Item> rareEarthBreederRods = new EnumMap<>(
        RareEarthBreedingRoute.class);
    private static final Map<RareEarthBreedingRoute, Item> bredRareEarthRods = new EnumMap<>(
        RareEarthBreedingRoute.class);

    private ItemLoader() {}

    public static void register() {
        for (RareEarthBreedingRoute route : RareEarthBreedingRoute.values()) {
            rareEarthBreederRods.put(route, register(route.itemName(false), new ItemRareEarthBreederRod(route, false)));
            bredRareEarthRods.put(route, register(route.itemName(true), new ItemRareEarthBreederRod(route, true)));
        }
    }

    public static Item rareEarthRod(RareEarthBreedingRoute route, boolean bred) {
        return (bred ? bredRareEarthRods : rareEarthBreederRods).get(route);
    }

    private static Item register(String name, Item item) {
        GameRegistry.registerItem(item, name);
        ModItems.register(new ItemStack(item));
        return item;
    }
}
