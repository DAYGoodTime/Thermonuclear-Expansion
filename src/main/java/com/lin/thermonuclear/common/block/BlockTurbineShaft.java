package com.lin.thermonuclear.common.block;

public final class BlockTurbineShaft extends BlockTieredMachineComponent {

    private BlockTurbineShaft(int tier) {
        super(
            tier == 1 ? "low_pressure_turbine_shaft" : "high_pressure_turbine_shaft",
            tier,
            "turbine_shaft/turbine_shaft_side_" + textureVariant(tier),
            "turbine_shaft/turbine_shaft_top_bottom_" + textureVariant(tier),
            "turbine_shaft/turbine_shaft_top_bottom_" + textureVariant(tier));
        addInfo("thermonuclear.tooltip.turbine_shaft.0");
        if (tier == 2) addInfo("thermonuclear.tooltip.turbine_shaft.1");
    }

    private static int textureVariant(int tier) {
        return switch (tier) {
            case 1 -> 1;
            case 2 -> 4;
            default -> throw new IllegalArgumentException("Unknown turbine shaft tier: " + tier);
        };
    }

    public static BlockTurbineShaft[] registerTiers() {
        return registerTiers(BlockTurbineShaft::new, new BlockTurbineShaft[2]);
    }
}
