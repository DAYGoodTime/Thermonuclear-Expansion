package com.lin.thermonuclear.nuclear;

import net.minecraft.item.ItemStack;

import ic2.core.Ic2Items;
import ic2.core.item.reactor.ItemReactorMOX;
import ic2.core.item.reactor.ItemReactorUranium;

public final class IC2FuelRodAdapter implements FuelRodAdapter {

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
        // No temperature simulation: uranium and MOX use their zero-heat self-pulse output.
        return cells * (1 + cells / 2) * FuelRodAdapter.nuclearEnergyMultiplier();
    }

    @Override
    public double heatPerCycle(ItemStack stack) {
        int cells = ((ItemReactorUranium) stack.getItem()).numberOfCells;
        int pulses = 1 + cells / 2;
        return 4.0 * cells * pulses * (pulses + 1) / 2;
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
