package com.lin.thermonuclear.common.machine.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraftforge.fluids.FluidStack;

import appeng.me.GridAccessException;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputDebug;
import gregtech.api.metatileentity.implementations.MTEHatchMultiInput;
import gregtech.common.tileentities.machines.MTEHatchInputME;

/** Reads original hatch stacks after the native recipe-processing lifecycle has started. */
final class FluidInputSources {

    private FluidInputSources() {}

    static List<Source> open(Object hatch) {
        List<Source> sources = new ArrayList<>();
        if (hatch instanceof MTEHatchInputDebug debug) {
            for (FluidStack fluid : debug.getFluidList()) {
                if (fluid != null) sources.add(new Source(fluid.copy(), true));
            }
            return sources;
        }
        List<FluidStack> stacks;
        Object sharedStock = null;
        if (hatch instanceof MTEHatchInputME me) {
            if (!me.isAllowedToWork()) return sources;
            try {
                // Identify duplicate native ME views without querying or extracting network quantities.
                sharedStock = me.getProxy()
                    .getStorage()
                    .getFluidInventory();
            } catch (GridAccessException ignored) {
                return sources;
            }
            stacks = Arrays.asList(me.getStoredFluids());
        } else if (hatch instanceof MTEHatchMultiInput multi) {
            stacks = Arrays.asList(multi.getStoredFluid());
        } else if (hatch instanceof MTEHatchInput input) {
            stacks = Collections.singletonList(input.getDrainableStack());
        } else {
            return sources;
        }
        return fromStacks(stacks, sharedStock);
    }

    static List<Source> fromStacks(List<FluidStack> stacks, Object sharedStock) {
        List<Source> sources = new ArrayList<>();
        Set<FluidStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (FluidStack stack : stacks) {
            if (stack != null && stack.amount > 0 && seen.add(stack))
                sources.add(new Source(stack, false, sharedStock == null ? stack : sharedStock));
        }
        return sources;
    }

    static final class Source {

        private final FluidStack stack;
        private final FluidStack template;
        private final boolean infinite;
        private final Object identity;

        Source(FluidStack stack, boolean infinite) {
            this(stack, infinite, stack);
        }

        Source(FluidStack stack, boolean infinite, Object identity) {
            this.stack = stack;
            this.template = stack.copy();
            this.template.amount = 1;
            this.infinite = infinite;
            this.identity = identity;
        }

        FluidStack fluid() {
            return stack;
        }

        Object storageIdentity() {
            return identity;
        }

        long available() {
            return !template.isFluidEqual(stack) ? 0 : infinite ? Long.MAX_VALUE : Math.max(0, stack.amount);
        }

        void consume(long amount) {
            // Only original local/transaction stacks change; the hatch owns external network extraction.
            if (!infinite) stack.amount -= (int) amount;
        }
    }
}
