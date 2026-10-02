package com.lin.thermonuclear.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.casing.Casings;
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
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public abstract class ThermonuclearMultiblockBase<T extends ThermonuclearMultiblockBase<T>>
    extends MTEEnhancedMultiBlockBase<T> {

    public static final int CYCLE_TICKS = 20;

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
    protected double cycleEUt;
    protected boolean cycleAdvancesStartup = true;
    private List<FluidStack> cycleFluids;
    private final List<Runnable> outputCommits = new ArrayList<>();
    private final List<MTEHatch> cycleDynamos = new ArrayList<>();

    protected ThermonuclearMultiblockBase(int id, String name, String regional) {
        super(id, name, regional);
    }

    protected ThermonuclearMultiblockBase(String name) {
        super(name);
    }

    protected abstract boolean usesItemBusses();

    protected abstract boolean usesDynamo();

    protected abstract boolean processCycle();

    protected abstract int startupTicks();

    protected abstract int decayTicks();

    public abstract String nameKey();

    public abstract String[] displayKeys();

    public abstract Map<String, String> displayInfo();

    protected void beforeProcessingCycle() {}

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(nameKey());
    }

    protected abstract Casings casing();

    protected abstract int structureChunkRadius();

    protected boolean addMachineHatch(IGregTechTileEntity tile, int texture) {
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
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = casing().getCasingTexture();
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(
                active ? Textures.BlockIcons.OVERLAY_FRONT_LARGE_BOILER_ACTIVE
                    : Textures.BlockIcons.OVERLAY_FRONT_LARGE_BOILER)
            .extFacing()
            .build() };
    }

    protected MultiblockTooltipBuilder machineTooltip() {
        MultiblockTooltipBuilder tooltip = new MultiblockTooltipBuilder()
            .addMachineType(StatCollector.translateToLocal(nameKey()))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.prototype"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.no_maintenance"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip." + machineKind()));
        return tooltip;
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
        int radius = structureChunkRadius();
        if (!tile.getWorld()
            .checkChunksExist(
                tile.getXCoord() - radius,
                tile.getYCoord() - radius,
                tile.getZCoord() - radius,
                tile.getXCoord() + radius,
                tile.getYCoord() + radius,
                tile.getZCoord() + radius)) {
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
        long[] before = storedDynamoEnergy();
        boolean hadSpace = hasDynamoSpace();
        // GT5U .133 uses int injection counters and explodes above the aggregate dynamo rating.
        // The inherited entry point dispatches to addEnergyOutputMultipleDynamos(..., true).
        long requested = Math.min(producedEUt, Math.min(dynamoRating(), Integer.MAX_VALUE));
        if (requested > 0) addEnergyOutput(requested);
        long[] after = storedDynamoEnergy();
        sentEUt = 0;
        for (int i = 0; i < before.length; i++) {
            sentEUt = saturatingAdd(sentEUt, Math.max(0, after[i] - before[i]));
        }
        sentEUt = Math.min(producedEUt, sentEUt);
        discardedEUt = producedEUt - sentEUt;
        powerLimited = discardedEUt > 0 || !hadSpace;
        // Even sub-EU output is forfeited when all dynamos are blocked; it is never banked for later.
        if (powerLimited) energyFraction = 0;
    }

    private long[] storedDynamoEnergy() {
        long[] stored = new long[cycleDynamos.size()];
        for (int i = 0; i < cycleDynamos.size(); i++) {
            MTEHatch hatch = cycleDynamos.get(i);
            if (!hatch.isValid()) continue;
            IGregTechTileEntity base = hatch.getBaseMetaTileEntity();
            if (base != null) stored[i] = Math.max(0, base.getStoredEU());
        }
        return stored;
    }

    private boolean hasDynamoSpace() {
        for (MTEHatch hatch : dynamos()) {
            if (!hatch.isValid() || rating(hatch) <= 0) continue;
            IGregTechTileEntity base = hatch.getBaseMetaTileEntity();
            if (base != null && base.getStoredEU() < base.getEUCapacity()) return true;
        }
        return false;
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
    protected abstract MTEMultiBlockBaseGui<?> getGui();

    protected final Map<String, String> commonInfo() {
        Map<String, String> info = new LinkedHashMap<>();
        boolean processingFailed = !running && (status.equals("running") || status.equals("power_discarded"))
            && !checkRecipeResult.wasSuccessful();
        info.put("status", "thermonuclear.status." + (processingFailed ? "processing_failed" : status));
        return info;
    }

    protected final void addStartupInfo(Map<String, String> info) {
        info.put("startup", decimal(startup.get() * 100));
    }

    protected final void addGenerationInfo(Map<String, String> info) {
        info.put("full_load", decimal(fullLoadEUt));
        info.put("produced", String.format(Locale.ROOT, "%,d", running ? producedEUt : 0));
    }

    public static String formatInfo(String key, String value) {
        if (value == null || value.isEmpty()) return "";
        String label = StatCollector.translateToLocal("thermonuclear.gui." + key);
        String text = value.startsWith("thermonuclear.") ? StatCollector.translateToLocal(value) : value;
        if (key.equals("status")) {
            String state = value.substring("thermonuclear.status.".length());
            String severity = switch (state) {
                case "running" -> "normal";
                case "stopped" -> "idle";
                case "checking", "chunk_unloaded" -> "waiting";
                case "power_discarded" -> "warning";
                default -> "error";
            };
            EnumChatFormatting color = switch (severity) {
                case "normal" -> EnumChatFormatting.GREEN;
                case "idle" -> EnumChatFormatting.GRAY;
                case "waiting" -> EnumChatFormatting.YELLOW;
                case "warning" -> EnumChatFormatting.GOLD;
                default -> EnumChatFormatting.RED;
            };
            return color.toString() + EnumChatFormatting.BOLD
                + StatCollector.translateToLocal("thermonuclear.gui.severity." + severity)
                + EnumChatFormatting.RESET
                + " "
                + color
                + text
                + EnumChatFormatting.RESET;
        }
        return EnumChatFormatting.GRAY + label
            + ": "
            + (key.equals("mode") || key.equals("steam_type") ? EnumChatFormatting.GOLD : EnumChatFormatting.AQUA)
            + text
            + EnumChatFormatting.RESET;
    }

    public static String decimal(double value) {
        return String.format(Locale.ROOT, "%,.3f", value)
            .replaceAll("0+$", "")
            .replaceAll("\\.$", "");
    }

    @Override
    public String[] getInfoData() {
        return displayInfo().entrySet()
            .stream()
            .map(entry -> formatInfo(entry.getKey(), entry.getValue()))
            .toArray(String[]::new);
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
        return false;
    }
}
