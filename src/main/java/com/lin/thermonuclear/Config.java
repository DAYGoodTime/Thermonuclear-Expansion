package com.lin.thermonuclear;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static String greeting = "Hello World";

    // Prototype defaults, not an approved GTNH balance or survival progression.
    public static double nuclearDirectEfficiency = 1.0;
    public static double nuclearHeatEfficiency = 1.0;
    public static double fuelCyclesPerSecond = 1.0;
    public static int nuclearFuelRodsPerPipeTier = 256;
    public static double ic2CoolantPerHeat = 1.0;
    public static double superCoolantPerHeat = 0.25;
    public static int nuclearMaxReflectorCount = 4;
    public static double nuclearDirectOutputMultiplier = 2.0;
    public static double nuclearHeatOutputMultiplier = 10.0;
    public static double nuclearDistilledWaterPerHeat = 1.0;
    public static double nuclearSteamPerDistilledWater = 10.0;
    public static int exchangeHotFluidPerCycle = 100;
    public static int exchangeCycleTicks = 20;
    public static int ic2HotCoolantPerWater = 1;
    public static int hotSuperCoolantPerWater = 1;
    public static double ordinarySteamPerHotCoolant = 160.0;
    public static double superheatedSteamPerHotCoolant = 160.0;
    public static double supercriticalSteamPerHotCoolant = 160.0;
    public static double rotorEfficiencyMultiplier = 1.5;
    public static double rotorCapacityMultiplier = 2.0;
    public static int nuclearStartupTicks = 1200;
    public static int nuclearDecayTicks = 600;
    public static int turbineStartupTicks = 600;
    public static int turbineDecayTicks = 300;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        greeting = configuration.getString("greeting", Configuration.CATEGORY_GENERAL, greeting, "How shall I greet?");

        configuration.setCategoryComment("prototype", "TEST DEFAULTS ONLY; not final GTNH balance.");
        nuclearDirectEfficiency = number(configuration, "nuclearDirectEfficiency", 1, 0.001, 1);
        nuclearHeatEfficiency = number(configuration, "nuclearHeatEfficiency", 1, 0.001, 1);
        fuelCyclesPerSecond = number(configuration, "fuelCyclesPerSecond", 1, 0.001, 100);
        nuclearFuelRodsPerPipeTier = integer(configuration, "nuclearFuelRodsPerPipeTier", 256, 1, 256);
        ic2CoolantPerHeat = number(configuration, "ic2CoolantLitresPerFuelHeatUnit", 1, 0.001, 1000);
        superCoolantPerHeat = number(configuration, "superCoolantLitresPerFuelHeatUnit", 0.25, 0.001, 1000);
        nuclearMaxReflectorCount = integer(configuration, "nuclearMaxReflectorCount", 4, 0, 6);
        nuclearDirectOutputMultiplier = number(configuration, "nuclearDirectOutputMultiplier", 2, 0.001, 100);
        nuclearHeatOutputMultiplier = number(configuration, "nuclearHeatOutputMultiplier", 10, 0.001, 100);
        nuclearDistilledWaterPerHeat = number(configuration, "nuclearDistilledWaterPerHeat", 1, 0.001, 1000);
        nuclearSteamPerDistilledWater = number(configuration, "nuclearSteamPerDistilledWater", 10, 0.001, 1000000);
        exchangeHotFluidPerCycle = integer(configuration, "exchangeHotFluidPerCycle", 100, 1, 1000000);
        exchangeCycleTicks = integer(configuration, "exchangeCycleTicks", 20, 1, 72000);
        ic2HotCoolantPerWater = integer(configuration, "ic2HotCoolantLitresPerWater", 1, 1, 1000000);
        hotSuperCoolantPerWater = integer(configuration, "hotSuperCoolantLitresPerWater", 1, 1, 1000000);
        ordinarySteamPerHotCoolant = number(configuration, "ordinarySteamLitresPerHotCoolant", 160, 0.001, 1000000);
        superheatedSteamPerHotCoolant = number(
            configuration,
            "superheatedSteamLitresPerHotCoolant",
            160,
            0.001,
            1000000);
        supercriticalSteamPerHotCoolant = number(
            configuration,
            "supercriticalSteamLitresPerHotCoolant",
            160,
            0.001,
            1000000);
        rotorEfficiencyMultiplier = number(configuration, "rotorEfficiencyMultiplier", 1.5, 1, 100);
        rotorCapacityMultiplier = number(configuration, "rotorCapacityMultiplier", 2, 1, 100);
        nuclearStartupTicks = integer(configuration, "nuclearStartupTicks", 1200, 1, 720000);
        nuclearDecayTicks = integer(configuration, "nuclearDecayTicks", 600, 1, 720000);
        turbineStartupTicks = integer(configuration, "turbineStartupTicks", 600, 1, 720000);
        turbineDecayTicks = integer(configuration, "turbineDecayTicks", 300, 1, 720000);

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    private static double number(Configuration config, String key, double fallback, double min, double max) {
        double value = config.get("prototype", key, fallback, "Prototype test value; range " + min + " to " + max)
            .getDouble(fallback);
        if (!Double.isFinite(value)) value = fallback;
        value = Math.max(min, Math.min(max, value));
        config.get("prototype", key, fallback)
            .set(value);
        return value;
    }

    private static int integer(Configuration config, String key, int fallback, int min, int max) {
        int value = config.getInt(key, "prototype", fallback, min, max, "Prototype test value.");
        config.get("prototype", key, fallback)
            .set(value);
        return value;
    }
}
