package com.lin.thermonuclear.common.registry;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraftforge.fluids.Fluid;

import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.api.CoolantType;

public enum DefaultCoolants implements CoolantType {

    DistilledWater("distilled_water", () -> WorkingFluids.distilledWater, () -> WorkingFluids.steam,
        () -> Config.nuclearPowerPlant.heatPerDistilledWater),
    IC2("ic2", () -> WorkingFluids.ic2Coolant, () -> WorkingFluids.ic2HotCoolant,
        () -> Config.nuclearPowerPlant.heatPreIc2Coolant),
    SUPER("super", () -> WorkingFluids.superCoolant, () -> WorkingFluids.hotSuperCoolant,
        () -> Config.nuclearPowerPlant.heatPreSuperCoolant),
    NAK_COMPOSITE("nak_composite", () -> WorkingFluids.nakCompositeCoolant, () -> WorkingFluids.hotNakCompositeCoolant,
        () -> Config.nuclearPowerPlant.heatPreNakCompositeCoolant),;

    private final String id;
    private final Supplier<Fluid> cold;
    private final Supplier<Fluid> hot;
    private final IntSupplier heatPerCoolant;

    DefaultCoolants(String id, Supplier<Fluid> cold, Supplier<Fluid> hot, IntSupplier heatPerCoolant) {
        this.id = id;
        this.cold = cold;
        this.hot = hot;
        this.heatPerCoolant = heatPerCoolant;
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
    public int heatPreCoolant() {
        return heatPerCoolant.getAsInt();
    }
}
