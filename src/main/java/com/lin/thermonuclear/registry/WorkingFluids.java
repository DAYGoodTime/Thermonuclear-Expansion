package com.lin.thermonuclear.registry;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.lin.thermonuclear.Thermonuclear;

import gregtech.api.enums.Materials;
import gregtech.api.util.GTModHandler;
import gtnhlanth.common.register.WerkstoffMaterialPool;

public final class WorkingFluids {

    public static Fluid steam;
    public static Fluid superheatedSteam;
    public static Fluid supercriticalSteam;
    public static Fluid ultraSupercriticalSteam;
    public static Fluid distilledWater;
    public static Fluid ic2Coolant;
    public static Fluid ic2HotCoolant;
    public static Fluid superCoolant;
    public static Fluid hotSuperCoolant;
    public static Fluid nakCompositeCoolant;
    public static Fluid hotNakCompositeCoolant;

    private WorkingFluids() {}

    public static void resolve() {
        steam = required("GT ordinary steam", Materials.Steam.getGas(1));
        superheatedSteam = required("IC2 superheated steam", FluidRegistry.getFluidStack("ic2superheatedsteam", 1));
        supercriticalSteam = required("GT supercritical steam", FluidRegistry.getFluidStack("supercriticalsteam", 1));
        ultraSupercriticalSteam = required(
            "Thermonuclear ultra-supercritical steam",
            FluidRegistry.getFluidStack(ModFluids.ULTRA_SUPERCRITICAL_STEAM_NAME, 1));
        distilledWater = required("IC2 distilled water", GTModHandler.getDistilledWater(1));
        ic2Coolant = required("IC2 coolant", GTModHandler.getIC2Coolant(1));
        ic2HotCoolant = required("IC2 hot coolant", GTModHandler.getHotCoolant(1));
        superCoolant = required("GT super coolant", Materials.SuperCoolant.getFluid(1));
        hotSuperCoolant = required("BW hot super coolant", WerkstoffMaterialPool.HotSuperCoolant.getFluidOrGas(1));
        nakCompositeCoolant = required(
            "Thermonuclear NaK composite coolant",
            FluidRegistry.getFluidStack(ModFluids.NAK_COMPOSITE_COOLANT_NAME, 1));
        hotNakCompositeCoolant = required(
            "Thermonuclear hot NaK composite coolant",
            FluidRegistry.getFluidStack(ModFluids.HOT_NAK_COMPOSITE_COOLANT_NAME, 1));
    }

    private static Fluid required(String name, FluidStack stack) {
        if (stack == null || stack.getFluid() == null) {
            Thermonuclear.LOG.error("Missing prototype working fluid: {}. Its processing path is disabled.", name);
            return null;
        }
        return stack.getFluid();
    }

    public static FluidStack stack(Fluid fluid, int amount) {
        return fluid == null ? null : new FluidStack(fluid, amount);
    }
}
