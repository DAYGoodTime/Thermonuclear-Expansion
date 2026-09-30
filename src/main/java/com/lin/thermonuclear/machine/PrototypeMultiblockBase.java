package com.lin.thermonuclear.machine;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchDynamo;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public abstract class PrototypeMultiblockBase<T extends PrototypeMultiblockBase<T>>
    extends MTEEnhancedMultiBlockBase<T> {

    public static final int BOX_SIZE = 3;
    public static final int OFFSET_X = 1;
    public static final int OFFSET_Y = 1;
    public static final int OFFSET_Z = 0;
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

    protected PrototypeMultiblockBase(int id, String name, String regional) {
        super(id, name, regional);
    }

    protected PrototypeMultiblockBase(String name) {
        super(name);
    }

    protected abstract boolean usesItemBusses();

    protected abstract boolean usesDynamo();

    protected abstract boolean requiresFluidHatches();

    protected abstract boolean tickPrototype(long tick);

    protected abstract int startupTicks();

    protected abstract int decayTicks();

    public abstract String nameKey();

    public abstract String[] detailKeys();

    public abstract String[] detailValues();

    protected void beforeServerTick() {}

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
        Class<?> type = tile.getMetaTileEntity()
            .getClass();
        // This prototype deliberately uses physical standard hatches, not ME, void or custom IO.
        if (type == MTEHatchInput.class) return addInputToMachineList(tile, texture);
        if (type == MTEHatchOutput.class) return addOutputToMachineList(tile, texture);
        if (usesItemBusses() && type == MTEHatchInputBus.class) return addInputToMachineList(tile, texture);
        if (usesItemBusses() && type == MTEHatchOutputBus.class) return addOutputToMachineList(tile, texture);
        return usesDynamo() && type == MTEHatchDynamo.class && addDynamoToMachineList(tile, texture);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        if (!checkPiece(PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z, errors)) return;
        if (usesItemBusses()) {
            if (mInputBusses.isEmpty()) errors.add(StructureErrors.missingHatch(HatchElement.InputBus));
            if (mOutputBusses.isEmpty()) errors.add(StructureErrors.missingHatch(HatchElement.OutputBus));
        }
        if (requiresFluidHatches()) {
            if (mInputHatches.isEmpty()) errors.add(StructureErrors.missingHatch(HatchElement.InputHatch));
            if (mOutputHatches.isEmpty()) errors.add(StructureErrors.missingHatch(HatchElement.OutputHatch));
        }
        if (!usesItemBusses() && usesDynamo() && mDynamoHatches.isEmpty()) {
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
        running = false;
        producedEUt = sentEUt = discardedEUt = 0;
        powerLimited = false;
        inputRate = outputRate = fullLoadEUt = 0;
        beforeServerTick();
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
            startup.decay(decayTicks());
            mMaxProgresstime = mProgresstime = 0;
            if (!mMachine) status = "structure";
            else if (!tile.isAllowedToWork()) status = "stopped";
        }
        // Parent runMachine is intentionally replaced: no second recipe commit or EU buffer.
        tile.setActive(running);
        markDirty();
    }

    @Override
    protected void runMachine(IGregTechTileEntity tile, long tick) {
        if (!tile.isAllowedToWork()) return;
        running = tickPrototype(tick);
        mMaxProgresstime = running ? 1 : 0;
        mProgresstime = 0;
        mEUt = 0;
        mEfficiency = 10000;
        if (running) {
            status = powerLimited ? "power_discarded" : "running";
            if (startupTicks() > 0) startup.advance(startupTicks());
        }
    }

    protected boolean fail(String reason) {
        status = reason;
        return false;
    }

    protected long available(Fluid fluid) {
        if (fluid == null) return 0;
        return depleteInputQuantity(new FluidStack(fluid, Integer.MAX_VALUE), true);
    }

    protected void consume(Fluid fluid, int amount) {
        if (amount > 0 && !depleteInput(new FluidStack(fluid, amount))) {
            throw new IllegalStateException("Prototype input changed within a server-tick transaction");
        }
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
        for (MTEHatchDynamo hatch : mDynamoHatches) {
            if (!hatch.isValid()) continue;
            total = saturatingAdd(total, rating(hatch));
        }
        return total;
    }

    private static long rating(MTEHatchDynamo hatch) {
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
        for (MTEHatchDynamo hatch : mDynamoHatches) {
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
        return producedEUt;
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setDouble("tnStartup", startup.get());
        nbt.setDouble("tnEnergyFraction", energyFraction);
        nbt.setBoolean("tnHadStructure", mMachine || hadStructure);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        startup.restore(nbt.getDouble("tnStartup"));
        energyFraction = StartupProgress.fraction(nbt.getDouble("tnEnergyFraction"));
        hadStructure = nbt.getBoolean("tnHadStructure");
        // Never resume a parent recipe or reload an energy output buffer.
        mOutputItems = null;
        mOutputFluids = null;
        mMaxProgresstime = mProgresstime = mEUt = 0;
        fixAllIssues();
    }

    @Override
    public void onRemoval() {
        startup.clear();
        energyFraction = 0;
        super.onRemoval();
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new PrototypeGui(this);
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
        return false;
    }
}
