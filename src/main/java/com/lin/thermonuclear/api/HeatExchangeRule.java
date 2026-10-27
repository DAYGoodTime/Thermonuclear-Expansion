package com.lin.thermonuclear.api;

import com.lin.thermonuclear.common.registry.SteamType;

public interface HeatExchangeRule {

    String id();

    CoolantType coolant();

    SteamType steam();

    int steamPerHotCoolant();
}
