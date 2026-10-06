package com.lin.thermonuclear.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

/** Registry of fuel-rod adapters supplied by this mod and other integrations. */
public final class FuelRodAdapters {

    private static final List<FuelRodAdapter> ADAPTERS = new ArrayList<>();

    private FuelRodAdapters() {}

    public static void register(FuelRodAdapter adapter) {
        if (adapter == null) throw new IllegalArgumentException("Fuel rod adapter cannot be null");
        ADAPTERS.add(adapter);
    }

    public static List<FuelRodAdapter> all() {
        return Collections.unmodifiableList(ADAPTERS);
    }

    public static FuelRodAdapter find(ItemStack stack) {
        for (FuelRodAdapter adapter : ADAPTERS) {
            if (adapter.accepts(stack)) return adapter;
        }
        return null;
    }
}
