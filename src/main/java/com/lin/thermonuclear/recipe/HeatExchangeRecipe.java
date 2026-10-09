package com.lin.thermonuclear.recipe;

import net.minecraftforge.fluids.Fluid;

import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.registry.WorkingFluids;

public enum HeatExchangeRecipe {

    IC2_COOLANT("ic2"),
    SUPER_COOLANT("super");

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
        return this == IC2_COOLANT ? WorkingFluids.ic2HotCoolant : WorkingFluids.hotSuperCoolant;
    }

    public Fluid cold() {
        return this == IC2_COOLANT ? WorkingFluids.ic2Coolant : WorkingFluids.superCoolant;
    }

    public double coolantPerHeat() {
        return this == IC2_COOLANT ? Config.nuclearPowerPlant.ic2CoolantPerHeat
            : Config.nuclearPowerPlant.superCoolantPerHeat;
    }

    public double steamEfficiency() {
        return this == SUPER_COOLANT ? 5 : 1;
    }

    public boolean supportsSteam(HeatExchangeSteam steam) {
        return switch (this) {
            case IC2_COOLANT -> steam == HeatExchangeSteam.ORDINARY || steam == HeatExchangeSteam.SUPERHEATED;
            case SUPER_COOLANT -> steam == HeatExchangeSteam.SUPERCRITICAL;
        };
    }

    public double steamPerHotCoolant(HeatExchangeSteam steam) {
        return supportsSteam(steam) ? steam.steamPerHotCoolant() * steamEfficiency() : 0;
    }

}
