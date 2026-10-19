package com.lin.thermonuclear.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SteamTurbineFuels {

    private static final List<SteamTurbineFuel> fuels = new ArrayList<SteamTurbineFuel>();

    public static void addFuel(SteamTurbineFuel fuel) {
        fuels.add(fuel);
    }

    public static void addFuels(SteamTurbineFuel[] fuel_list) {
        fuels.addAll(Arrays.asList(fuel_list));
    }

    public static List<SteamTurbineFuel> getAllFuels() {
        return Collections.unmodifiableList(fuels);
    }
}
