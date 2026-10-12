package com.lin.thermonuclear.block;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntFunction;

public abstract class BlockTieredMachineComponent extends BlockMachineComponent {

    private final int tier;

    protected BlockTieredMachineComponent(String name, int tier, String sideTexture, String topTexture,
        String bottomTexture) {
        super(name, true, sideTexture, topTexture, bottomTexture);
        if (tier < 1) throw new IllegalArgumentException("Component tier must be positive");
        this.tier = tier;
    }

    public final int tier() {
        return tier;
    }

    protected static <T extends BlockTieredMachineComponent> Map<Integer, T> registerTiers(IntFunction<T> factory,
        int... tiers) {
        Map<Integer, T> blocks = new LinkedHashMap<>();
        for (int tier : tiers) {
            T block = factory.apply(tier);
            if (block.tier() != tier || blocks.putIfAbsent(tier, block) != null) {
                throw new IllegalArgumentException("Invalid or duplicate component tier: " + tier);
            }
        }
        // Keep separate registry names: metadata is already reserved for the saved placement axis.
        for (T block : blocks.values()) {
            block.register();
        }
        return Collections.unmodifiableMap(blocks);
    }
}
