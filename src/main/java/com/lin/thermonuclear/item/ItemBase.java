package com.lin.thermonuclear.item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.lin.thermonuclear.Thermonuclear;
import com.lin.thermonuclear.loader.ItemLoader;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;

public abstract class ItemBase extends Item {

    private final String registryName;

    protected ItemBase(String name, String texture) {
        registryName = name;
        setUnlocalizedName(Thermonuclear.MODID + "." + name);
        setTextureName(texture);
        setCreativeTab(GregTechAPI.TAB_GREGTECH);
    }

    public final void register() {
        GameRegistry.registerItem(this, registryName);
        ItemLoader.register(new ItemStack(this));
    }

    protected static <K, T extends ItemBase> Map<K, T> registerVariants(K[] variants, Function<K, T> factory) {
        Map<K, T> items = new LinkedHashMap<>();
        for (K variant : variants) {
            if (items.putIfAbsent(variant, factory.apply(variant)) != null) {
                throw new IllegalArgumentException("Duplicate item variant: " + variant);
            }
        }
        for (T item : items.values()) {
            item.register();
        }
        return Collections.unmodifiableMap(items);
    }
}
