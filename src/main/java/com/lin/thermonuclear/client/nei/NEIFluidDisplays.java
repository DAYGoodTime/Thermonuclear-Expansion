package com.lin.thermonuclear.client.nei;

import java.util.Map;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

public final class NEIFluidDisplays {

    private NEIFluidDisplays() {}

    public static FluidStack[] available(FluidStack[] fluids) {
        if (fluids == null) return new FluidStack[0];
        Map<Fluid, Integer> fluidIDs = FluidRegistry.getRegisteredFluidIDsByFluid();
        // Preserve slot positions and backend arrays; only omit fluids absent from the active server ID map.
        FluidStack[] displays = fluids.clone();
        for (int i = 0; i < displays.length; i++) {
            FluidStack stack = displays[i];
            if (stack == null) continue;
            Fluid fluid = stack.getFluid();
            if (fluid == null) {
                displays[i] = null;
                continue;
            }
            Integer id = fluidIDs.get(fluid);
            if (id == null || id <= 0 || FluidRegistry.getFluid(id) != fluid) {
                displays[i] = null;
            }
        }
        return displays;
    }
}
