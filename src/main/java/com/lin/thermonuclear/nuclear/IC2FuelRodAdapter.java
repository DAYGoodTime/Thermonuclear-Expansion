package com.lin.thermonuclear.nuclear;

import net.minecraft.item.ItemStack;

import com.lin.thermonuclear.Config;

import ic2.core.Ic2Items;
import ic2.core.item.reactor.ItemReactorMOX;
import ic2.core.item.reactor.ItemReactorUranium;

public final class IC2FuelRodAdapter implements FuelRodAdapter {

    private static final int MAX_REFLECTOR_COUNT = 4;
    private static final double OUTPUT_DOUBLING = 2.0;
    private static final double HEAT_OUTPUT_MULTIPLIER = 10.0;

    @Override
    public boolean accepts(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemReactorUranium rod)) return false;
        Class<?> base = rod instanceof ItemReactorMOX ? ItemReactorMOX.class : ItemReactorUranium.class;
        return FuelRodAdapter.inheritsSemantics(rod.getClass(), base)
            && (rod.numberOfCells == 1 || rod.numberOfCells == 2 || rod.numberOfCells == 4)
            && rod.getMaxCustomDamage(stack) > 0
            && depleted(stack) != null;
    }

    @Override
    public int remainingCycles(ItemStack stack) {
        ItemReactorUranium rod = (ItemReactorUranium) stack.getItem();
        return Math.max(0, rod.getMaxCustomDamage(stack) - Math.max(0, rod.getCustomDamage(stack)));
    }

    @Override
    public void consumeCycles(ItemStack stack, int cycles) {
        ItemReactorUranium rod = (ItemReactorUranium) stack.getItem();
        rod.setCustomDamage(stack, Math.max(0, rod.getCustomDamage(stack)) + cycles);
    }

    @Override
    public double baseEUt(ItemStack stack) {
        int cells = ((ItemReactorUranium) stack.getItem()).numberOfCells;
        // Bake the four-sided maximum reflector layout into the prototype output.
        int pulses = cells * (1 + cells / 2 + MAX_REFLECTOR_COUNT);
        return pulses * FuelRodAdapter.nuclearEnergyMultiplier() * OUTPUT_DOUBLING;
    }

    @Override
    public double heatPerCycle(ItemStack stack) {
        int cells = ((ItemReactorUranium) stack.getItem()).numberOfCells;
        int pulses = 1 + cells / 2 + MAX_REFLECTOR_COUNT;
        return 4.0 * cells * pulses * (pulses + 1) / 2 * HEAT_OUTPUT_MULTIPLIER;
    }

    @Override
    public ItemStack depleted(ItemStack stack) {
        ItemReactorUranium rod = (ItemReactorUranium) stack.getItem();
        ItemStack result = rod instanceof ItemReactorMOX ? switch (rod.numberOfCells) {
            case 1 -> Ic2Items.reactorDepletedMOXSimple;
            case 2 -> Ic2Items.reactorDepletedMOXDual;
            case 4 -> Ic2Items.reactorDepletedMOXQuad;
            default -> null;
        } : switch (rod.numberOfCells) {
            case 1 -> Ic2Items.reactorDepletedUraniumSimple;
            case 2 -> Ic2Items.reactorDepletedUraniumDual;
            case 4 -> Ic2Items.reactorDepletedUraniumQuad;
            default -> null;
        };
        if (result == null) return null;
        result = result.copy();
        result.stackSize = 1;
        return result;
    }
}
