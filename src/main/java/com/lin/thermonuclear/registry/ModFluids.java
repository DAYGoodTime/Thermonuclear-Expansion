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
    public static final String NAK_COMPOSITE_COOLANT_NAME = Thermonuclear.MODID + ".nakcompositecoolant";
    public static final String HOT_NAK_COMPOSITE_COOLANT_NAME = Thermonuclear.MODID + ".hotnakcompositecoolant";
    public static final Fluid nakCompositeCoolant = new CompositeCoolant(NAK_COMPOSITE_COOLANT_NAME, false);
    public static final Fluid hotNakCompositeCoolant = new CompositeCoolant(HOT_NAK_COMPOSITE_COOLANT_NAME, true);

    private ModFluids() {}

    public static void register() {
        Fluid[] fluids = { ultraSupercriticalSteam, nakCompositeCoolant, hotNakCompositeCoolant };
        // Check the full set before registering any fluid; never silently reuse an occupied approved name.
        for (Fluid fluid : fluids) {
            if (FluidRegistry.isFluidRegistered(fluid.getName())) {
                throw new IllegalStateException("Thermonuclear approved fluid name is occupied: " + fluid.getName());
            }
        }
        for (Fluid fluid : fluids) {
            if (!FluidRegistry.registerFluid(fluid)) {
                throw new IllegalStateException("Unable to register Thermonuclear fluid: " + fluid.getName());
            }
        }
    }

    private static final class CompositeCoolant extends Fluid {

        private final boolean hot;

        private CompositeCoolant(String name, boolean hot) {
            super(name);
            setUnlocalizedName(name);
            this.hot = hot;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon getStillIcon() {
            Fluid reference = hot ? WorkingFluids.hotSuperCoolant : WorkingFluids.superCoolant;
            IIcon icon = reference == null ? null : reference.getStillIcon();
            return icon == null ? FluidRegistry.WATER.getStillIcon() : icon;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IIcon getFlowingIcon() {
            Fluid reference = hot ? WorkingFluids.hotSuperCoolant : WorkingFluids.superCoolant;
            IIcon icon = reference == null ? null : reference.getFlowingIcon();
            return icon == null ? getStillIcon() : icon;
        }
    }

    private static final class UltraSupercriticalSteam extends Fluid {

        private UltraSupercriticalSteam() {
            super(ULTRA_SUPERCRITICAL_STEAM_NAME);
            setUnlocalizedName(ULTRA_SUPERCRITICAL_STEAM_NAME);
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
