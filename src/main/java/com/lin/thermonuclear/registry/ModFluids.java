package com.lin.thermonuclear.registry;

import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import com.lin.thermonuclear.Thermonuclear;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ModFluids {

    public static final String ULTRA_SUPERCRITICAL_STEAM_NAME = Thermonuclear.MODID + ".ultrasupercriticalsteam";
    public static final Fluid ultraSupercriticalSteam = new UltraSupercriticalSteam();

    private ModFluids() {}

    public static void register() {
        if (FluidRegistry.isFluidRegistered(ULTRA_SUPERCRITICAL_STEAM_NAME)) {
            throw new IllegalStateException(
                "Thermonuclear approved fluid name is occupied: " + ULTRA_SUPERCRITICAL_STEAM_NAME);
        }
        if (!FluidRegistry.registerFluid(ultraSupercriticalSteam)) {
            throw new IllegalStateException(
                "Unable to register Thermonuclear fluid: " + ULTRA_SUPERCRITICAL_STEAM_NAME);
        }
    }

    private static final class UltraSupercriticalSteam extends Fluid {

        private UltraSupercriticalSteam() {
            super(ULTRA_SUPERCRITICAL_STEAM_NAME);
            setGaseous(true);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon getStillIcon() {
            Fluid reference = FluidRegistry.getFluid("supercriticalsteam");
            IIcon icon = reference == null ? null : reference.getStillIcon();
            return icon == null ? FluidRegistry.WATER.getStillIcon() : icon;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon getFlowingIcon() {
            Fluid reference = FluidRegistry.getFluid("supercriticalsteam");
            IIcon icon = reference == null ? null : reference.getFlowingIcon();
            return icon == null ? getStillIcon() : icon;
        }
    }
}
