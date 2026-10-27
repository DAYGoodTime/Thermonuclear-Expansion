package com.lin.thermonuclear.common.recipe.heatexchange;

import com.lin.thermonuclear.api.CoolantType;
import com.lin.thermonuclear.api.HeatExchangeRule;
import com.lin.thermonuclear.common.registry.DefaultCoolants;
import com.lin.thermonuclear.common.registry.SteamType;

public enum DefaultHeatExchangeRules implements HeatExchangeRule {

    IC2_COOLANT(DefaultCoolants.IC2, SteamType.SUPERHEATED, 20),
    SUPER_COOLANT(DefaultCoolants.SUPER, SteamType.SUPERCRITICAL, 50),
    NAK_COMPOSITE_COOLANT(DefaultCoolants.NAK_COMPOSITE, SteamType.ULTRA_SUPERCRITICAL, 100);

    private final CoolantType coolant;
    private final SteamType steam;
    private final int steamPerHotCoolant;

    DefaultHeatExchangeRules(CoolantType coolant, SteamType steam, int steamPerHotCoolant) {
        this.coolant = coolant;
        this.steam = steam;
        this.steamPerHotCoolant = steamPerHotCoolant;
    }

    @Override
    public String id() {
        return coolant.id();
    }

    @Override
    public CoolantType coolant() {
        return coolant;
    }

    @Override
    public SteamType steam() {
        return steam;
    }

    @Override
    public int steamPerHotCoolant() {
        return steamPerHotCoolant;
    }

}
