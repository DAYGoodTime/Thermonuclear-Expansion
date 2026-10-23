package com.lin.thermonuclear.block;

import java.util.function.IntFunction;

public abstract class BlockTieredMachineComponent extends BlockWrenchComponent {

    private final int tier;

    protected BlockTieredMachineComponent(String name, int tier, String sideTexture, String topTexture,
        String bottomTexture) {
        super(name, sideTexture, topTexture, bottomTexture);
        if (tier < 1) throw new IllegalArgumentException("Component tier must be positive");
        this.tier = tier;
    }

    public final int tier() {
        return tier;
    }

    protected static <T extends BlockTieredMachineComponent> T[] registerTiers(IntFunction<T> factory, T[] array) {
        for (int i = 0; i < array.length; i++) {
            T block = factory.apply(i + 1);
            if (block.tier() != i + 1) {
                throw new IllegalArgumentException("Invalid component tier: " + block.tier());
            }
            block.register();
            array[i] = block;
        }
        return array;
    }
}
