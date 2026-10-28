package com.lin.thermonuclear.api;

import net.minecraftforge.fluids.Fluid;

public interface CoolantType {

    String id();

    String nameKey();

    Fluid cold();

    Fluid hot();

    int heatPreCoolant();
}
