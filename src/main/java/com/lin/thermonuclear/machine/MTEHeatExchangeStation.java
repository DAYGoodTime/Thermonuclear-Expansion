package com.lin.thermonuclear.machine;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.recipe.HeatExchangeRecipe;
import com.lin.thermonuclear.registry.WorkingFluids;

import gregtech.api.enums.GTValues;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.FluidEjectionHelper;

public final class MTEHeatExchangeStation extends PrototypeMultiblockBase<MTEHeatExchangeStation> {

    private HeatExchangeRecipe selected;
    private double throughputRemainder;
    private int lastHotAmount;
    private int lastWaterAmount;

    public MTEHeatExchangeStation(int id, String name, String regional) {
        super(id, name, regional);
    }

    public MTEHeatExchangeStation(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MTEHeatExchangeStation(mName);
    }

    @Override
    protected boolean processCycle() {
        if (WorkingFluids.steam == null || WorkingFluids.distilledWater == null) return fail("fluids_missing");
        HeatExchangeRecipe recipe = null;
        // Selection is per cycle. Never pool the heat or consume the second hot fluid.
        if (selected != null && selected.hot() != null
            && selected.cold() != null
            && available(selected.hot()) >= selected.hotPerWater()) recipe = selected;
        if (recipe == null) {
            for (HeatExchangeRecipe candidate : HeatExchangeRecipe.values()) {
                if (candidate.hot() != null && candidate.cold() != null
                    && available(candidate.hot()) >= candidate.hotPerWater()) {
                    recipe = candidate;
                    break;
                }
            }
        }
        if (recipe == null) {
            selected = null;
            return fail("hot_fluid");
        }
        if (selected != recipe) throughputRemainder = 0;
        selected = recipe;
        if (available(WorkingFluids.distilledWater) <= 0) return fail("water");
        double budget = Config.exchangeHotFluidPerCycle * (double) CYCLE_TICKS / Config.exchangeCycleTicks
            + throughputRemainder;
        long hotLimit = Math.min(available(recipe.hot()), (long) Math.floor(budget));
        int water = (int) Math.min(
            Math.min(hotLimit / recipe.hotPerWater(), available(WorkingFluids.distilledWater)),
            Math.min(Integer.MAX_VALUE / GTValues.STEAM_PER_WATER, Integer.MAX_VALUE / recipe.hotPerWater()));
        int hot = water * recipe.hotPerWater();
        int steam = water * GTValues.STEAM_PER_WATER;
        FluidEjectionHelper outputs = prepareOutputs(
            new FluidStack(WorkingFluids.steam, Math.max(GTValues.STEAM_PER_WATER, steam)),
            new FluidStack(recipe.cold(), Math.max(recipe.hotPerWater(), hot)));
        if (outputs == null) return fail("output_full");
        final double remainder = budget % recipe.hotPerWater();
        commitOutput(() -> throughputRemainder = remainder);
        if (water == 0) return true;
        // All capacity reservations and both input checks precede any real mutation.
        consume(recipe.hot(), hot);
        consume(WorkingFluids.distilledWater, water);
        commitOutput(outputs::commit);
        lastHotAmount = hot;
        lastWaterAmount = water;
        inputRate = hot / (double) CYCLE_TICKS;
        outputRate = steam / (double) CYCLE_TICKS;
        return true;
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setDouble("tnExchangeThroughputRemainder", throughputRemainder);
        nbt.setString("tnExchangeRecipe", selected == null ? "" : selected.id());
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        selected = null;
        for (HeatExchangeRecipe recipe : HeatExchangeRecipe.values()) {
            if (recipe.id()
                .equals(nbt.getString("tnExchangeRecipe"))) selected = recipe;
        }
        throughputRemainder = nbt.getDouble("tnExchangeThroughputRemainder");
        if (!Double.isFinite(throughputRemainder) || selected == null
            || throughputRemainder < 0
            || throughputRemainder >= selected.hotPerWater()) throughputRemainder = 0;
    }

    @Override
    protected boolean usesItemBusses() {
        return false;
    }

    @Override
    protected boolean usesDynamo() {
        return false;
    }

    @Override
    protected boolean requiresFluidHatches() {
        return true;
    }

    @Override
    protected int startupTicks() {
        return 0;
    }

    @Override
    protected int decayTicks() {
        return 1;
    }

    @Override
    public String nameKey() {
        return "thermonuclear.machine.heat_exchange.name";
    }

    @Override
    protected String machineKind() {
        return "heat_exchange";
    }

    @Override
    public String[] detailKeys() {
        return new String[] { "recipe", "throughput", "cycle", "last_batch" };
    }

    @Override
    public String[] detailValues() {
        return new String[] { selected == null ? "thermonuclear.recipe.none" : selected.translationKey(),
            Config.exchangeHotFluidPerCycle + " L / " + Config.exchangeCycleTicks + " t",
            mProgresstime + " / " + CYCLE_TICKS,
            lastHotAmount + " L hot; "
                + lastWaterAmount
                + " L water; "
                + lastWaterAmount * GTValues.STEAM_PER_WATER
                + " L steam" };
    }
}
