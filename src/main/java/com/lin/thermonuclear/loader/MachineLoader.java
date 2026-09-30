package com.lin.thermonuclear.loader;

import net.minecraft.item.ItemStack;

import com.lin.thermonuclear.machine.MTEHeatExchangeStation;
import com.lin.thermonuclear.machine.MTELargeSteamTurbine;
import com.lin.thermonuclear.machine.MTENuclearPowerPlant;
import com.lin.thermonuclear.registry.ModItems;

import gregtech.api.GregTechAPI;

public final class MachineLoader {

    // Approved by DAYGood_Time: start at 32100 and increment, replacing the out-of-range 45600 proposal.
    public static final int NUCLEAR_ID = 32100;
    public static final int HEAT_EXCHANGE_ID = 32101;
    public static final int STEAM_TURBINE_ID = 32102;
    public static ItemStack nuclearPowerPlant;
    public static ItemStack heatExchangeStation;
    public static ItemStack largeSteamTurbine;

    private MachineLoader() {}

    public static void register() {
        for (int id : new int[] { NUCLEAR_ID, HEAT_EXCHANGE_ID, STEAM_TURBINE_ID }) {
            if (id <= 0 || id >= GregTechAPI.METATILEENTITIES.length) {
                throw new IllegalArgumentException("Thermonuclear machine ID outside GT5U registry: " + id);
            }
            if (GregTechAPI.METATILEENTITIES[id] != null) {
                throw new IllegalStateException(
                    "Thermonuclear approved machine ID is occupied: " + id
                        + ". No automatic reassignment is permitted.");
            }
        }
        nuclearPowerPlant = ModItems.register(
            new MTENuclearPowerPlant(NUCLEAR_ID, "thermonuclear.nuclear_power_plant", "Nuclear Power Plant")
                .getStackForm(1));
        heatExchangeStation = ModItems.register(
            new MTEHeatExchangeStation(HEAT_EXCHANGE_ID, "thermonuclear.heat_exchange_station", "Heat Exchange Station")
                .getStackForm(1));
        largeSteamTurbine = ModItems.register(
            new MTELargeSteamTurbine(STEAM_TURBINE_ID, "thermonuclear.large_steam_turbine", "Large Steam Turbine")
                .getStackForm(1));
    }
}
