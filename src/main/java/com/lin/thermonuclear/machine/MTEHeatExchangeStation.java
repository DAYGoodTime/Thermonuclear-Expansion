package com.lin.thermonuclear.machine;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.gui.HeatExchangeStationGui;
import com.lin.thermonuclear.recipe.HeatExchangeRecipe;
import com.lin.thermonuclear.registry.WorkingFluids;

import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public final class MTEHeatExchangeStation extends ThermonuclearMultiblockBase<MTEHeatExchangeStation> {

    private static final String PIECE = "heat_exchange_station";
    private static final int SIZE = 3;
    private static final int OFFSET_X = 1;
    private static final int OFFSET_Y = 1;
    private static final int OFFSET_Z = 0;
    private static final Casings CASING = Casings.HeatProofMachineCasing;
    // StructureLib order: [depth][top-to-bottom row], with the controller at (1, 1, 0).
    private static final String[][] SHAPE = { { "CCC", "C~C", "CCC" }, { "CCC", "C-C", "CCC" },
        { "CCC", "CCC", "CCC" } };
    private static final IStructureDefinition<MTEHeatExchangeStation> STRUCTURE = StructureDefinition
        .<MTEHeatExchangeStation>builder()
        .addShape(PIECE, SHAPE)
        .addElement(
            'C',
            GTStructureUtility.<MTEHeatExchangeStation>ofHatchAdderOptional(
                (machine, tile, texture) -> machine.addMachineHatch(tile, texture),
                CASING.textureId,
                1,
                CASING.getBlock(),
                CASING.meta))
        .build();

    @Override
    public IStructureDefinition<MTEHeatExchangeStation> getStructureDefinition() {
        return STRUCTURE;
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        if (!checkPiece(PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z, errors)) return;
        if (!hasFluidInputs()) errors.add(StructureErrors.missingHatch(HatchElement.InputHatch));
        if (mOutputHatches.isEmpty()) errors.add(StructureErrors.missingHatch(HatchElement.OutputHatch));
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(PIECE, stack, hintsOnly, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }

    @Override
    protected Casings casing() {
        return CASING;
    }

    @Override
    protected int structureChunkRadius() {
        return SIZE - 1;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return machineTooltip().beginStructureBlock(SIZE, SIZE, SIZE, true)
            .addController(StatCollector.translateToLocal("thermonuclear.structure.controller"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.box"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.hatches"))
            .addInputHatch("1+", "Shell", 1)
            .addOutputHatch("1+", "Shell", 1)
            .toolTipFinisher();
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new HeatExchangeStationGui(this);
    }

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
