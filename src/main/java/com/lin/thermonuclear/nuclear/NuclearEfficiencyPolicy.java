package com.lin.thermonuclear.nuclear;

import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.machine.NuclearOperatingMode;

@FunctionalInterface
public interface NuclearEfficiencyPolicy {

    NuclearEfficiencyPolicy CONFIGURED = mode -> switch (mode) {
        case DIRECT_GENERATION -> Config.nuclearPowerPlant.nuclearDirectEfficiency;
        case HEAT_SUPPLY -> Config.nuclearPowerPlant.nuclearHeatEfficiency;
    };

    double efficiency(NuclearOperatingMode mode);
}
