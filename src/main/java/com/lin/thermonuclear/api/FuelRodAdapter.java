package com.lin.thermonuclear.api;

import net.minecraft.item.ItemStack;

/** Adapts one external fuel-rod item implementation to the nuclear reactor. */
public interface FuelRodAdapter {

    boolean accepts(ItemStack stack);

    int remainingCycles(ItemStack stack);

    void consumeCycles(ItemStack stack, int cycles);

    double baseEUt(ItemStack stack);

    double heatPerCycle(ItemStack stack);

    ItemStack depleted(ItemStack stack);
}
