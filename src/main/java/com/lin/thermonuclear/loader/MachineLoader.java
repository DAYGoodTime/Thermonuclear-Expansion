package com.lin.thermonuclear.loader;

import com.lin.thermonuclear.machine.MTEDebugStructureBuilder;
import com.lin.thermonuclear.machine.MTEHeatExchangeStation;
import com.lin.thermonuclear.machine.MTELargeSteamTurbine;
import com.lin.thermonuclear.machine.hatch.MTEBreederInputBus;
import com.lin.thermonuclear.machine.hatch.MTEBreederOutputBus;
import com.lin.thermonuclear.machine.nuclear.MTENuclearPowerPlant;
import com.lin.thermonuclear.api.TNCItemList;

import gregtech.api.GregTechAPI;

public final class MachineLoader {

    // Approved by DAYGood_Time: start at 32100 and increment, replacing the out-of-range 45600 proposal.
    public static final int NUCLEAR_ID = 32100;
    public static final int HEAT_EXCHANGE_ID = 32101;
    public static final int STEAM_TURBINE_ID = 32102;
    public static final int BREEDER_INPUT_ID = 32103;
    public static final int BREEDER_OUTPUT_ID = 32104;
    // Debug-only template host; shares the approved incrementing range.
    public static final int DEBUG_STRUCTURE_ID = 32105;

    private MachineLoader() {}

    public static void register() {
        for (int id : new int[] { NUCLEAR_ID, HEAT_EXCHANGE_ID, STEAM_TURBINE_ID, BREEDER_INPUT_ID, BREEDER_OUTPUT_ID,
            DEBUG_STRUCTURE_ID }) {
            if (id <= 0 || id >= GregTechAPI.METATILEENTITIES.length) {
                throw new IllegalArgumentException("Thermonuclear machine ID outside GT5U registry: " + id);
            }
            if (GregTechAPI.METATILEENTITIES[id] != null) {
                throw new IllegalStateException(
                    "Thermonuclear approved machine ID is occupied: " + id
                        + ". No automatic reassignment is permitted.");
            }
        }
        TNCItemList.NuclearPowerPlant.set(
            ItemLoader.register(
                new MTENuclearPowerPlant(NUCLEAR_ID, "thermonuclear.nuclear_power_plant", "Nuclear Power Plant")
                    .getStackForm(1)));
        TNCItemList.HeatExchangeStation.set(
            ItemLoader.register(
                new MTEHeatExchangeStation(
                    HEAT_EXCHANGE_ID,
                    "thermonuclear.heat_exchange_station",
                    "Heat Exchange Station").getStackForm(1)));
        TNCItemList.LargeSteamTurbine.set(
            ItemLoader.register(
                new MTELargeSteamTurbine(STEAM_TURBINE_ID, "thermonuclear.large_steam_turbine", "Large Steam Turbine")
                    .getStackForm(1)));
        TNCItemList.BreederInputBus.set(
            ItemLoader.register(
                new MTEBreederInputBus(BREEDER_INPUT_ID, "thermonuclear.breeder_input", "Breeder Input Bus")
                    .getStackForm(1)));
        TNCItemList.BreederOutputBus.set(
            ItemLoader.register(
                new MTEBreederOutputBus(BREEDER_OUTPUT_ID, "thermonuclear.breeder_output", "Breeder Output Bus")
                    .getStackForm(1)));
        TNCItemList.DebugStructureBuilder.set(
            ItemLoader.register(
                new MTEDebugStructureBuilder(
                    DEBUG_STRUCTURE_ID,
                    "thermonuclear.debug_structure_builder",
                    "Debug Structure Builder").getStackForm(1)));
    }
}
