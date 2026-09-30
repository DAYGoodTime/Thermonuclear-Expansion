package com.lin.thermonuclear.nuclear;

import java.lang.reflect.Method;

import net.minecraft.item.ItemStack;

import ic2.core.init.MainConfig;
import ic2.core.util.ConfigUtil;

public interface FuelRodAdapter {

    boolean accepts(ItemStack stack);

    int remainingCycles(ItemStack stack);

    void consumeCycles(ItemStack stack, int cycles);

    double baseEUt(ItemStack stack);

    double heatPerCycle(ItemStack stack);

    ItemStack depleted(ItemStack stack);

    // One durability unit is one reactor cycle (20 server ticks), not one game tick.
    // IC2 getOfferedEnergy and GT's own nuclear recipe descriptions both use 5, not GTNL's 25.
    static double nuclearEnergyMultiplier() {
        double value = ConfigUtil.getFloat(MainConfig.get(), "balance/energy/generator/nuclear");
        return Double.isFinite(value) && value > 0 ? 5 * value : 0;
    }

    static boolean inheritsSemantics(Class<?> actual, Class<?> verifiedBase) {
        for (Class<?> type = actual; type != verifiedBase; type = type.getSuperclass()) {
            if (type == null) return false;
            for (Method method : type.getDeclaredMethods()) {
                switch (method.getName()) {
                    case "processChamber", "acceptUraniumPulse", "getFinalHeat", "getDepletedStack", "getCustomDamage", "getMaxCustomDamage", "setCustomDamage", "applyCustomDamage", "getDamageOfStack", "getMaxDamageEx", "setDamageForStack", "damageItemStack" -> {
                        return false;
                    }
                    default -> {}
                }
            }
        }
        return true;
    }
}
