package com.lin.thermonuclear.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.lin.thermonuclear.gui.PrototypeGui;

import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public abstract class PrototypeMultiblockBase<T extends PrototypeMultiblockBase<T>>
    extends MTEEnhancedMultiBlockBase<T> {

    public static final int BOX_SIZE = 3;
    public static final int OFFSET_X = 1;
    public static final int OFFSET_Y = 1;
    public static final int OFFSET_Z = 0;
    public static final int CYCLE_TICKS = 20;
    public static final Casings CASING = Casings.HeatProofMachineCasing;
    private static final String PIECE = "box";
    // StructureLib order is [depth][top-to-bottom row]; ~ must match offset (1, 1, 0).
    private static final String[][] SHAPE = { { "CCC", "C~C", "CCC" }, { "CCC", "C-C", "CCC" },
        { "CCC", "CCC", "CCC" } };

    protected final StartupProgress startup = new StartupProgress();
    protected boolean running;
    protected String status = "stopped";
    protected double fullLoadEUt;
    protected long producedEUt;
    protected long sentEUt;
    protected long discardedEUt;
    protected boolean powerLimited;
    protected double inputRate;
    protected double outputRate;
    private double energyFraction;
    private boolean hadStructure;
    private IStructureDefinition<T> definition;
    protected double cycleEUt;
    protected boolean cycleAdvancesStartup = true;
    private List<FluidStack> cycleFluids;
    private final List<Runnable> outputCommits = new ArrayList<>();
    private final List<MTEHatch> cycleDynamos = new ArrayList<>();

    protected PrototypeMultiblockBase(int id, String name, String regional) {
        super(id, name, regional);
    }

    protected PrototypeMultiblockBase(String name) {
        super(name);
    }

    protected abstract boolean usesItemBusses();

    protected abstract boolean usesDynamo();

    protected abstract boolean requiresFluidHatches();

    protected abstract boolean processCycle();

    protected abstract int startupTicks();

    protected abstract int decayTicks();

    public abstract String nameKey();

    public abstract String[] detailKeys();

    public abstract String[] detailValues();

    protected void beforeProcessingCycle() {}

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(nameKey());
    }

    @Override
    public IStructureDefinition<T> getStructureDefinition() {
        if (definition == null) {
            definition = StructureDefinition.<T>builder()
                .addShape(PIECE, SHAPE)
                .addElement(
                    'C',
                    GTStructureUtility.<T>ofHatchAdderOptional(
                        (machine, tile, texture) -> machine.addPrototypeHatch(tile, texture),
                        CASING.textureId,
                        1,
                        CASING.getBlock(),
                        CASING.meta))
                .build();
        }
        return definition;
    }

    protected boolean addPrototypeHatch(IGregTechTileEntity tile, int texture) {
        if (tile == null || tile.getMetaTileEntity() == null) return false;
        if (usesItemBusses() || tile.getMetaTileEntity() instanceof IDualInputHatch) {
            if (addInputBusToMachineList(tile, texture)) return true;
        }
        if (usesItemBusses() && addOutputBusToMachineList(tile, texture)) return true;
        if (addInputHatchToMachineList(tile, texture) || addOutputHatchToMachineList(tile, texture)) return true;
        return usesDynamo() && (addDynamoToMachineList(tile, texture) || addExoticDynamoToMachineList(tile, texture)
            || addLaserSourceToMachineList(tile, texture));
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        // The .133 parent does not clear its exotic dynamo list during a structure rescan.
        mExoticDynamoHatches.clear();
        cycleDynamos.clear();
    }

    @Override
    protected void onStructureCheckFinished(IGregTechTileEntity tile) {
        super.onStructureCheckFinished(tile);
        cycleDynamos.clear();
        cycleDynamos.addAll(mDynamoHatches);
        cycleDynamos.addAll(mExoticDynamoHatches);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        if (!checkPiece(PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z, errors)) return;
        if (usesItemBusses()) {
            if (mInputBusses.isEmpty() && mDualInputHatches.isEmpty()) {
                errors.add(StructureErrors.missingHatch(HatchElement.InputBus));
            }
            if (mOutputBusses.isEmpty()) errors.add(StructureErrors.missingHatch(HatchElement.OutputBus));
        }
        if (requiresFluidHatches()) {
            if (!hasFluidInputs()) errors.add(StructureErrors.missingHatch(HatchElement.InputHatch));
            if (mOutputHatches.isEmpty()) errors.add(StructureErrors.missingHatch(HatchElement.OutputHatch));
        }
        if (!usesItemBusses() && usesDynamo() && mDynamoHatches.isEmpty() && mExoticDynamoHatches.isEmpty()) {
            errors.add(StructureErrors.missingHatch(HatchElement.Dynamo));
        }
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(PIECE, stack, hintsOnly, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = CASING.getCasingTexture();
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(
                active ? Textures.BlockIcons.OVERLAY_FRONT_LARGE_BOILER_ACTIVE
                    : Textures.BlockIcons.OVERLAY_FRONT_LARGE_BOILER)
            .extFacing()
            .build() };
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tooltip = new MultiblockTooltipBuilder()
            .addMachineType(StatCollector.translateToLocal(nameKey()))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.prototype"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.no_maintenance"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip." + machineKind()))
            .beginStructureBlock(BOX_SIZE, BOX_SIZE, BOX_SIZE, true)
            .addController(StatCollector.translateToLocal("thermonuclear.structure.controller"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.box"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.hatches"));
        if (usesItemBusses()) {
            tooltip.addInputBus("1+", "Shell", 1)
                .addOutputBus("1+", "Shell", 1);
        }
        tooltip.addInputHatch(requiresFluidHatches() ? "1+" : "0+", "Shell", 1)
            .addOutputHatch(requiresFluidHatches() ? "1+" : "0+", "Shell", 1);
        if (usesDynamo()) tooltip.addDynamoHatch(usesItemBusses() ? "0+" : "1+", "Shell", 1);
        return tooltip.toolTipFinisher();
    }

    protected abstract String machineKind();

    @Override
    public RecipeMap<?> getRecipeMap() {
        return null;
    }

    @Override
    public boolean shouldCheckMaintenance() {
        return false;
    }

    @Override
    public int getRepairStatus() {
        return getIdealStatus();
    }

    @Override
    public void checkMaintenance() {
        fixAllIssues();
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        if (!tile.isServerSide()) {
            super.onPostTick(tile, tick);
            return;
        }
        boolean formed = mMachine || hadStructure;
        double previousStartup = startup.get();
        running = false;
        producedEUt = sentEUt = discardedEUt = 0;
        powerLimited = false;
        // A reload or an unloaded neighbour chunk is not a dismantled shell. Defer checks and processing.
        // Radius two contains every orientation of this 3x3x3 front-controller box.
        if (!tile.getWorld()
            .checkChunksExist(
                tile.getXCoord() - 2,
                tile.getYCoord() - 2,
                tile.getZCoord() - 2,
                tile.getXCoord() + 2,
                tile.getYCoord() + 2,
                tile.getZCoord() + 2)) {
            status = "chunk_unloaded";
            tile.setActive(false);
            return;
        }
        super.onPostTick(tile, tick);
        if (mStartUpCheck >= 0) {
            status = "checking";
            tile.setActive(false);
            return;
        }
        if (formed && !mMachine) startup.clear();
        hadStructure = mMachine;
        if (!running) {
            if (mMaxProgresstime <= 0) startup.decay(decayTicks());
            if (!mMachine) status = "structure";
            else if (!tile.isAllowedToWork()) status = "stopped";
            if (mMaxProgresstime <= 0) inputRate = outputRate = fullLoadEUt = 0;
        }
        tile.setActive(running);
        if (startup.get() != previousStartup) markDirty();
    }

    @Override
    protected void runMachine(IGregTechTileEntity tile, long tick) {
        if (!tile.isAllowedToWork()) {
            mMaxProgresstime = mProgresstime = 0;
            cycleEUt = 0;
            return;
        }
        super.runMachine(tile, tick);
    }

    @Override
    public final CheckRecipeResult checkProcessing() {
        cycleEUt = inputRate = outputRate = fullLoadEUt = 0;
        cycleAdvancesStartup = true;
        outputCommits.clear();
        beforeProcessingCycle();
        if (!processCycle()) return failureResult();
        mMaxProgresstime = CYCLE_TICKS;
        mProgresstime = 0;
        mEUt = 0;
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
        mOutputItems = null;
        mOutputFluids = null;
        status = "running";
        return cycleEUt > 0 ? CheckRecipeResultRegistry.GENERATING : CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private CheckRecipeResult failureResult() {
        return switch (status) {
            case "fuel", "steam" -> CheckRecipeResultRegistry.NO_FUEL_FOUND;
            case "rotor" -> CheckRecipeResultRegistry.NO_TURBINE_FOUND;
            case "spent_full" -> CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
            case "output_full" -> CheckRecipeResultRegistry.FLUID_OUTPUT_FULL;
            default -> SimpleCheckRecipeResult.ofFailure("thermonuclear.status." + status);
        };
    }

    @Override
    public void startRecipeProcessing() {
        super.startRecipeProcessing();
        cycleFluids = new ArrayList<>(getStoredFluids());
        for (IDualInputHatch hatch : mDualInputHatches) {
            cycleFluids.addAll(Arrays.asList(hatch.getAllFluids()));
        }
    }

    @Override
    public void endRecipeProcessing() {
        try {
            super.endRecipeProcessing();
            if (checkRecipeResult.wasSuccessful()) {
                outputCommits.forEach(Runnable::run);
            } else {
                mMaxProgresstime = mProgresstime = 0;
                cycleEUt = 0;
            }
            updateSlots();
            for (var hatch : mInputHatches) if (hatch.isValid()) hatch.markDirty();
            for (var bus : mInputBusses) if (bus.isValid()) bus.markDirty();
            for (var hatch : mDualInputHatches) {
                if (((MetaTileEntity) hatch).isValid()) ((MetaTileEntity) hatch).markDirty();
            }
        } finally {
            outputCommits.clear();
            cycleFluids = null;
        }
    }

    protected final void commitOutput(Runnable commit) {
        outputCommits.add(commit);
    }

    protected final boolean hasFluidInputs() {
        return !mInputHatches.isEmpty() || mDualInputHatches.stream()
            .anyMatch(IDualInputHatch::supportsFluids);
    }

    protected final List<ItemStack> cycleItems() {
        List<ItemStack> items = new ArrayList<>();
        for (IDualInputHatch hatch : mDualInputHatches) {
            items.addAll(Arrays.asList(hatch.getAllItems()));
        }
        Set<GTUtility.ItemId> meItems = new HashSet<>();
        for (MTEHatchInputBus bus : mInputBusses) {
            if (!bus.isValid()) continue;
            // Keep hatch/slot traversal stable even when multiple ME buses expose the same stock.
            for (int slot = 0; slot < bus.getSizeInventory(); slot++) {
                ItemStack stack = bus.getStackInSlot(slot);
                if (stack == null || stack.stackSize <= 0) continue;
                if (bus instanceof MTEHatchInputBusME && !meItems.add(GTUtility.ItemId.createNoCopy(stack))) continue;
                items.add(stack);
            }
        }
        return items;
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        running = true;
        if (cycleEUt > 0) generate(cycleEUt * startup.next(startupTicks()));
        status = powerLimited ? "power_discarded" : "running";
        if (startupTicks() > 0) {
            if (cycleAdvancesStartup) startup.advance(startupTicks());
            else startup.decay(decayTicks());
        }
        return true;
    }

    protected boolean fail(String reason) {
        status = reason;
        return false;
    }

    protected long available(Fluid fluid) {
        if (fluid == null) return 0;
        long amount = 0;
        Set<FluidStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (FluidStack stack : cycleFluids) {
            if (stack != null && stack.getFluid() == fluid && seen.add(stack)) {
                amount = saturatingAdd(amount, Math.max(0, stack.amount));
            }
        }
        return amount;
    }

    protected void consume(Fluid fluid, int amount) {
        Set<FluidStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (FluidStack stack : cycleFluids) {
            if (stack == null || stack.getFluid() != fluid || !seen.add(stack)) continue;
            int used = Math.min(amount, Math.max(0, stack.amount));
            stack.amount -= used;
            amount -= used;
            if (amount == 0) return;
        }
        if (amount > 0) throw new IllegalStateException("Cycle input changed during recipe processing");
    }

    protected FluidEjectionHelper prepareOutputs(FluidStack... outputs) {
        for (FluidStack output : outputs) {
            if (output == null || output.amount <= 0) return null;
        }
        FluidEjectionHelper helper = new FluidEjectionHelper(getOutputHatches(), true);
        // One shared transaction set reserves each output slot only once for the entire batch.
        return helper.ejectFluids(Arrays.asList(outputs), 1) == 1 ? helper : null;
    }

    protected long dynamoRating() {
        long total = 0;
        for (MTEHatch hatch : dynamos()) {
            if (!hatch.isValid()) continue;
            total = saturatingAdd(total, rating(hatch));
        }
        return total;
    }

    private List<MTEHatch> dynamos() {
        return cycleDynamos;
    }

    private static long rating(MTEHatch hatch) {
        long volts = Math.max(0, hatch.maxEUOutput());
        long amps = Math.max(0, hatch.maxAmperesOut());
        return amps == 0 ? 0 : volts > Long.MAX_VALUE / amps ? Long.MAX_VALUE : volts * amps;
    }

    protected void generate(double amount) {
        if (!Double.isFinite(amount) || amount <= 0) return;
        double total = amount + energyFraction;
        producedEUt = (long) Math.min(total, Long.MAX_VALUE);
        energyFraction = total < Long.MAX_VALUE ? StartupProgress.fraction(total - producedEUt) : 0;
        long remaining = producedEUt;
        long spaceAvailable = 0;
        for (MTEHatch hatch : dynamos()) {
            if (!hatch.isValid()) continue;
            IGregTechTileEntity base = hatch.getBaseMetaTileEntity();
            if (base == null) continue;
            long allowance = Math.min(rating(hatch), Math.max(0, base.getEUCapacity() - base.getStoredEU()));
            spaceAvailable = saturatingAdd(spaceAvailable, allowance);
            long send = Math.min(remaining, allowance);
            if (send <= 0) continue;
            if (base.increaseStoredEnergyUnits(send, false)) {
                sentEUt += send;
                remaining -= send;
            }
        }
        discardedEUt = remaining;
        powerLimited = discardedEUt > 0 || spaceAvailable == 0;
        // Even sub-EU output is forfeited when all dynamos are blocked; it is never banked for later.
        if (powerLimited) energyFraction = 0;
    }

    private static long saturatingAdd(long a, long b) {
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }

    @Override
    public long getEUtForDamageCalc() {
        // The parent applies wear before onRunningTick, so the reset display counter is not usable here.
        return (long) Math.min(Long.MAX_VALUE, cycleEUt * startup.next(startupTicks()));
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setDouble("tnStartup", startup.get());
        nbt.setDouble("tnEnergyFraction", energyFraction);
        nbt.setBoolean("tnHadStructure", mMachine || hadStructure);
        nbt.setInteger("tnCycleVersion", 1);
        nbt.setDouble("tnCycleEUt", cycleEUt);
        nbt.setBoolean("tnCycleAdvancesStartup", cycleAdvancesStartup);
        nbt.setDouble("tnFullLoadEUt", fullLoadEUt);
        nbt.setDouble("tnInputRate", inputRate);
        nbt.setDouble("tnOutputRate", outputRate);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        startup.restore(nbt.getDouble("tnStartup"));
        energyFraction = StartupProgress.fraction(nbt.getDouble("tnEnergyFraction"));
        hadStructure = nbt.getBoolean("tnHadStructure");
        // Only the new 20-tick cycle can resume; old one-tick prototypes have no prepaid cycle.
        mOutputItems = null;
        mOutputFluids = null;
        mEUt = 0;
        if (nbt.getInteger("tnCycleVersion") == 1 && mMaxProgresstime == CYCLE_TICKS
            && mProgresstime >= 0
            && mProgresstime < CYCLE_TICKS) {
            cycleEUt = finiteRate(nbt.getDouble("tnCycleEUt"));
            cycleAdvancesStartup = !nbt.hasKey("tnCycleAdvancesStartup") || nbt.getBoolean("tnCycleAdvancesStartup");
            fullLoadEUt = finiteRate(nbt.getDouble("tnFullLoadEUt"));
            inputRate = finiteRate(nbt.getDouble("tnInputRate"));
            outputRate = finiteRate(nbt.getDouble("tnOutputRate"));
        } else {
            mMaxProgresstime = mProgresstime = 0;
            cycleEUt = 0;
        }
        fixAllIssues();
    }

    private static double finiteRate(double value) {
        return Double.isFinite(value) && value > 0 ? value : 0;
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setBoolean("tnGenerator", usesDynamo());
        tag.setLong("tnProducedEUt", running ? producedEUt : 0);
        tag.setLong("tnSentEUt", running ? sentEUt : 0);
    }

    @Override
    public void getWailaBody(ItemStack stack, List<String> tooltip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(stack, tooltip, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();
        if (!tag.getBoolean("tnGenerator")) return;
        tooltip
            .add(StatCollector.translateToLocalFormatted("thermonuclear.waila.produced", tag.getLong("tnProducedEUt")));
        tooltip.add(StatCollector.translateToLocalFormatted("thermonuclear.waila.sent", tag.getLong("tnSentEUt")));
    }

    @Override
    public void onRemoval() {
        startup.clear();
        energyFraction = 0;
        super.onRemoval();
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new PrototypeGui<>(this);
    }

    public String[] displayKeys() {
        String[] common = { "status", "startup", "full_load", "dynamo_limit", "produced", "sent", "discarded",
            "input_rate", "output_rate" };
        String[] details = detailKeys();
        String[] keys = Arrays.copyOf(common, common.length + details.length);
        System.arraycopy(details, 0, keys, common.length, details.length);
        return keys;
    }

    public String[] displayValues() {
        String[] common = { "thermonuclear.status." + status, decimal(startup.get() * 100), decimal(fullLoadEUt),
            Long.toString(dynamoRating()), Long.toString(producedEUt), Long.toString(sentEUt),
            Long.toString(discardedEUt), decimal(inputRate), decimal(outputRate) };
        String[] details = detailValues();
        String[] values = Arrays.copyOf(common, common.length + details.length);
        System.arraycopy(details, 0, values, common.length, details.length);
        return values;
    }

    public static String decimal(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    @Override
    public String[] getInfoData() {
        String[] keys = displayKeys();
        String[] values = displayValues();
        for (int i = 0; i < keys.length; i++) {
            String value = values[i];
            values[i] = StatCollector.translateToLocal("thermonuclear.gui." + keys[i]) + ": "
                + (value.startsWith("thermonuclear.") ? StatCollector.translateToLocal(value) : value);
        }
        return values;
    }

    @Override
    public boolean supportsVoidProtection() {
        return false;
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public boolean supportsMaintenanceIssueHoverable() {
        return false;
    }

    @Override
    public boolean showRecipeTextInGUI() {
        return true;
    }
}
