package com.lin.thermonuclear.block;

import java.util.List;

import net.minecraft.util.StatCollector;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public enum BlockTooltip {

    LOW_PRESSURE_TURBINE_SHAFT("low_pressure_turbine_shaft", 1),
    HIGH_PRESSURE_TURBINE_SHAFT("high_pressure_turbine_shaft", 1),
    TURBINE_BLADE("turbine_blade", 1),
    FUEL_ROD_TIER_1("fuel_rod_tier_1", 1),
    FUEL_ROD_TIER_2("fuel_rod_tier_2", 1),
    FUEL_ROD_TIER_3("fuel_rod_tier_3", 1),
    FUEL_ROD_TIER_4("fuel_rod_tier_4", 1);

    private final String[] translationKeys;

    BlockTooltip(String blockName, int lines) {
        translationKeys = new String[lines];
        for (int i = 0; i < lines; i++) {
            translationKeys[i] = "thermonuclear.tooltip." + blockName + "." + i;
        }
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(List<String> tooltip) {
        for (String key : translationKeys) {
            tooltip.add(StatCollector.translateToLocal(key));
        }
    }
}
