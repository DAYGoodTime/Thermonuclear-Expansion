package com.lin.thermonuclear.common.item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.lin.thermonuclear.common.loader.ItemLoader;
import com.lin.thermonuclear.common.registry.NaquadahFuelTier;

public final class ItemNaquadahFuelRod extends ItemFuelBase {

    private final NaquadahFuelTier tier;

    private ItemNaquadahFuelRod(NaquadahFuelTier tier) {
        super(
            tier.itemName(),
            "gregtech:gt.rodNaquadah",
            tier.maxCycles(),
            tier.directEUt() / 2.0F,
            tier.baseHeatPerCycle(),
            registerDepleted(tier.depletedItemName(), "gregtech:gt.depletedRodNaquadah"));
        this.tier = tier;
    }

    public static Map<NaquadahFuelTier, ItemNaquadahFuelRod> registerVariants() {
        Map<NaquadahFuelTier, ItemNaquadahFuelRod> rods = new LinkedHashMap<>();
        for (NaquadahFuelTier tier : NaquadahFuelTier.values()) {
            ItemNaquadahFuelRod rod = new ItemNaquadahFuelRod(tier);
            rods.put(tier, rod);
            ItemLoader.register(new ItemStack(rod));
        }
        return Collections.unmodifiableMap(rods);
    }

    public NaquadahFuelTier tier() {
        return tier;
    }
}
