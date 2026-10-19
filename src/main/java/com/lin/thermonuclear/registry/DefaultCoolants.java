package com.lin.thermonuclear.registry;

import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import net.minecraftforge.fluids.Fluid;

import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.api.CoolantType;

public enum DefaultCoolants implements CoolantType {

    IC2("ic2", () -> WorkingFluids.ic2Coolant, () -> WorkingFluids.ic2HotCoolant,
        () -> Config.nuclearPowerPlant.ic2CoolantPerHeat),
    SUPER("super", () -> WorkingFluids.superCoolant, () -> WorkingFluids.hotSuperCoolant,
        () -> Config.nuclearPowerPlant.superCoolantPerHeat),
    NAK_COMPOSITE("nak_composite", () -> WorkingFluids.nakCompositeCoolant, () -> WorkingFluids.hotNakCompositeCoolant,
        () -> Config.nuclearPowerPlant.nakCompositeCoolantPerHeat);

    private final String id;
    private final Supplier<Fluid> cold;
    private final Supplier<Fluid> hot;
    private final DoubleSupplier coolantPerHeat;

    DefaultCoolants(String id, Supplier<Fluid> cold, Supplier<Fluid> hot, DoubleSupplier coolantPerHeat) {
        this.id = id;
        this.cold = cold;
        this.hot = hot;
        this.coolantPerHeat = coolantPerHeat;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String nameKey() {
        return "thermonuclear.recipe." + id;
    }

    @Override
    public Fluid cold() {
        return cold.get();
    }

    @Override
    public Fluid hot() {
        return hot.get();
    }

    @Override
    public double coolantPerHeat() {
        return coolantPerHeat.getAsDouble();
    }
}
