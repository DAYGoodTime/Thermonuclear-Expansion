package com.lin.thermonuclear.recipe;

import net.minecraftforge.fluids.Fluid;

import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.registry.WorkingFluids;

public enum HeatExchangeRecipe {

    IC2_COOLANT("ic2"),
    SUPER_COOLANT("super"),
    NAK_COMPOSITE_COOLANT("nak_composite");

    private final String id;

    HeatExchangeRecipe(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "thermonuclear.recipe." + id;
    }

    public Fluid hot() {
        return switch (this) {
            case IC2_COOLANT -> WorkingFluids.ic2HotCoolant;
            case SUPER_COOLANT -> WorkingFluids.hotSuperCoolant;
            case NAK_COMPOSITE_COOLANT -> WorkingFluids.hotNakCompositeCoolant;
        };
    }

    public Fluid cold() {
        return switch (this) {
            case IC2_COOLANT -> WorkingFluids.ic2Coolant;
            case SUPER_COOLANT -> WorkingFluids.superCoolant;
            case NAK_COMPOSITE_COOLANT -> WorkingFluids.nakCompositeCoolant;
        };
    }

    public double coolantPerHeat() {
        return switch (this) {
            case IC2_COOLANT -> Config.nuclearPowerPlant.ic2CoolantPerHeat;
            case SUPER_COOLANT -> Config.nuclearPowerPlant.superCoolantPerHeat;
            case NAK_COMPOSITE_COOLANT -> Config.nuclearPowerPlant.nakCompositeCoolantPerHeat;
        };
    }

    public double steamEfficiency() {
        return this == SUPER_COOLANT ? 5 : 1;
    }

    public boolean supportsSteam(HeatExchangeSteam steam) {
        return switch (this) {
            case IC2_COOLANT -> steam == HeatExchangeSteam.ORDINARY || steam == HeatExchangeSteam.SUPERHEATED;
            case SUPER_COOLANT -> steam == HeatExchangeSteam.SUPERCRITICAL;
            case NAK_COMPOSITE_COOLANT -> steam == HeatExchangeSteam.ULTRA_SUPERCRITICAL;
        };
    }

    public double steamPerHotCoolant(HeatExchangeSteam steam) {
        return supportsSteam(steam) ? steam.steamPerHotCoolant() * steamEfficiency() : 0;
    }

}
