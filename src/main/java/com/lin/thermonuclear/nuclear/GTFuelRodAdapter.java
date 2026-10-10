package com.lin.thermonuclear.nuclear;

import net.minecraft.item.ItemStack;

import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.api.FuelRodAdapter;

import gregtech.api.items.ItemRadioactiveCellIC;

public final class GTFuelRodAdapter implements FuelRodAdapter {

    @Override
    public boolean accepts(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemRadioactiveCellIC rod)) return false;
        return rod.sEnergy > 0 && rod.sHeat > 0 && rod.sDepleted != null;
    }

    @Override
    public int remainingCycles(ItemStack stack) {
        ItemRadioactiveCellIC rod = (ItemRadioactiveCellIC) stack.getItem();
        return Math.max(0, rod.getMaxDamageEx() - Math.max(0, rod.getDamageOfStack(stack)));
    }

    @Override
    public void consumeCycles(ItemStack stack, int cycles) {
        ItemRadioactiveCellIC rod = (ItemRadioactiveCellIC) stack.getItem();
        rod.setDamageForStack(stack, Math.max(0, rod.getDamageOfStack(stack)) + cycles);
    }

    @Override
    public double baseEUt(ItemStack stack) {
        ItemRadioactiveCellIC rod = (ItemRadioactiveCellIC) stack.getItem();
        int pulsesPerCell = 1 + rod.numberOfCells / 2
            + (stack.stackSize > 4 ? Config.nuclearPowerPlant.nuclearMaxReflectorCount : 0);
        return rod.sEnergy * (double) rod.numberOfCells * pulsesPerCell;
    }

    @Override
    public double heatPerCycle(ItemStack stack) {
        ItemRadioactiveCellIC rod = (ItemRadioactiveCellIC) stack.getItem();
        int pulses = 1 + rod.numberOfCells / 2
            + (stack.stackSize > 4 ? Config.nuclearPowerPlant.nuclearMaxReflectorCount : 0);
        // GT rounds heat separately for each cell, as its processChamber does.
        return (double) rod.numberOfCells * Math.round(pulses * (pulses + 1) / 2 * rod.sHeat);
    }

    @Override
    public ItemStack depleted(ItemStack stack) {
        ItemStack result = ((ItemRadioactiveCellIC) stack.getItem()).sDepleted.copy();
        result.stackSize = 1;
        return result;
    }
}
