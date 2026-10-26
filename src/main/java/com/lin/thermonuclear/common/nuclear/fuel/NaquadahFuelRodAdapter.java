package com.lin.thermonuclear.common.nuclear.fuel;

import net.minecraft.item.ItemStack;

import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.api.FuelRodAdapter;
import com.lin.thermonuclear.common.item.ItemNaquadahFuelRod;

/** Sealed liquid fuel follows GT single-cell reflection and contributes one breeder pulse per rod. */
public final class NaquadahFuelRodAdapter implements FuelRodAdapter {

    @Override
    public boolean accepts(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemNaquadahFuelRod;
    }

    @Override
    public int cells(ItemStack stack) {
        return 1;
    }

    private static int pulses(ItemStack stack) {
        return 1 + (stack.stackSize > 4 ? Config.nuclearPowerPlant.nuclearMaxReflectorCount : 0);
    }

    @Override
    public int remainingCycles(ItemStack stack) {
        ItemNaquadahFuelRod rod = (ItemNaquadahFuelRod) stack.getItem();
        return Math.max(
            0,
            rod.tier()
                .maxCycles() - Math.max(0, rod.getDamageOfStack(stack)));
    }

    @Override
    public void consumeCycles(ItemStack stack, int cycles) {
        if (cycles < 0) throw new IllegalArgumentException("Cannot consume negative fuel cycles");
        ItemNaquadahFuelRod rod = (ItemNaquadahFuelRod) stack.getItem();
        long used = (long) Math.max(0, rod.getDamageOfStack(stack)) + cycles;
        rod.setDamageForStack(
            stack,
            (int) Math.min(
                rod.tier()
                    .maxCycles(),
                used));
    }

    @Override
    public double baseEUt(ItemStack stack) {
        // Compensate the default 2x direct-output multiplier, not the user's configured multiplier.
        return ((ItemNaquadahFuelRod) stack.getItem()).tier()
            .directEUt() / 2.0 * pulses(stack);
    }

    @Override
    public long heatPerCycle(ItemStack stack) {
        int pulses = pulses(stack);
        return ((ItemNaquadahFuelRod) stack.getItem()).tier()
            .baseHeatPerCycle() * pulses * (pulses + 1) / 2;
    }

    @Override
    public ItemStack depleted(ItemStack stack) {
        return ((ItemNaquadahFuelRod) stack.getItem()).depletedStack();
    }
}
