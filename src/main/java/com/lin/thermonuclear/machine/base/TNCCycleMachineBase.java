package com.lin.thermonuclear.machine.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.lin.thermonuclear.utils.StartupProgress;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;

public abstract class TNCCycleMachineBase<T extends TNCCycleMachineBase<T>> extends ThermonuclearMultiblockBase<T> {

    public static final int CYCLE_TICKS = 20;

    protected final StartupProgress startup = new StartupProgress();
    protected double fullLoadEUt;
    protected double inputLitresPerTick;
    protected double outputLitresPerTick;
    // Prepaid operating power in EU/t, not total cycle energy; excludes startup.
    protected double operatingEUt;
    protected boolean cycleAdvancesStartup = true;
    private boolean hadStructure;
    private CycleContext cycle;
    private boolean prepared;

    protected TNCCycleMachineBase(int id, String name, String regional) {
        super(id, name, regional);
    }

    protected TNCCycleMachineBase(String name) {
        super(name);
    }

    protected abstract boolean processCycle(CycleContext context);

    protected abstract int startupTicks();

    protected abstract int decayTicks();

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
            if (mMaxProgresstime <= 0) inputLitresPerTick = outputLitresPerTick = fullLoadEUt = 0;
        }
        tile.setActive(running);
        if (startup.get() != previousStartup) markDirty();
    }

    @Override
    protected void runMachine(IGregTechTileEntity tile, long tick) {
        if (!tile.isAllowedToWork()) {
            mMaxProgresstime = mProgresstime = 0;
            operatingEUt = 0;
            return;
        }
        super.runMachine(tile, tick);
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (operatingEUt > 0 && !checkWirelessOwner()) return false;
        running = true;
        status = "running";
        if (operatingEUt > 0) generate(operatingEUt * startup.next(startupTicks()));
        if (startupTicks() > 0) {
            if (cycleAdvancesStartup) startup.advance(startupTicks());
            else startup.decay(decayTicks());
        }
        return true;
    }

    @Override
    public long getEUtForDamageCalc() {
        // Wear is calculated before onRunningTick, so the reset display counter cannot be used.
        return (long) Math.min(Long.MAX_VALUE, operatingEUt * startup.next(startupTicks()));
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setDouble("tnStartup", startup.get());
        nbt.setBoolean("tnHadStructure", mMachine || hadStructure);
        nbt.setInteger("tnCycleVersion", 1);
        // Retain the existing save keys and their tick-based units.
        nbt.setDouble("tnCycleEUt", operatingEUt);
        nbt.setBoolean("tnCycleAdvancesStartup", cycleAdvancesStartup);
        nbt.setDouble("tnFullLoadEUt", fullLoadEUt);
        nbt.setDouble("tnInputRate", inputLitresPerTick);
        nbt.setDouble("tnOutputRate", outputLitresPerTick);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        startup.restore(nbt.getDouble("tnStartup"));
        hadStructure = nbt.getBoolean("tnHadStructure");
        mOutputItems = null;
        mOutputFluids = null;
        mEUt = 0;
        if (nbt.getInteger("tnCycleVersion") == 1 && mMaxProgresstime == CYCLE_TICKS
            && mProgresstime >= 0
            && mProgresstime < CYCLE_TICKS) {
            operatingEUt = finiteRate(nbt.getDouble("tnCycleEUt"));
            cycleAdvancesStartup = !nbt.hasKey("tnCycleAdvancesStartup") || nbt.getBoolean("tnCycleAdvancesStartup");
            fullLoadEUt = finiteRate(nbt.getDouble("tnFullLoadEUt"));
            inputLitresPerTick = finiteRate(nbt.getDouble("tnInputRate"));
            outputLitresPerTick = finiteRate(nbt.getDouble("tnOutputRate"));
        } else {
            mMaxProgresstime = mProgresstime = 0;
            operatingEUt = fullLoadEUt = inputLitresPerTick = outputLitresPerTick = 0;
            cycleAdvancesStartup = true;
        }
    }

    private static double finiteRate(double value) {
        return Double.isFinite(value) && value > 0 ? value : 0;
    }

    @Override
    public void onRemoval() {
        startup.clear();
        super.onRemoval();
    }

    protected final void addStartupInfo(Map<String, String> info) {
        info.put("startup", decimal(startup.get() * 100));
    }

    protected final void addGenerationInfo(Map<String, String> info) {
        info.put("full_load", decimal(fullLoadEUt));
    }

    protected static String decimalPerCycle(double valuePerTick) {
        return decimal(valuePerTick * CYCLE_TICKS);
    }

    protected void beforeProcessingCycle(CycleContext context) {}

    protected final CycleContext cycleContext() {
        if (cycle == null) throw new IllegalStateException("No active processing cycle");
        return cycle;
    }

    protected final boolean isProcessingCycle() {
        return cycle != null;
    }

    protected final long available(Fluid fluid) {
        return availableFromStacks(fluid, cycleContext().availableFluids());
    }

    protected final void consume(Fluid fluid, long amount) {
        consumeFromStacks(fluid, amount, cycleContext().availableFluids());
    }

    @Override
    public final void startRecipeProcessing() {
        if (cycle != null) throw new IllegalStateException("A processing cycle is already open");
        super.startRecipeProcessing();
        try {
            List<FluidStack> fluids = new ArrayList<>(getStoredFluids());
            for (IDualInputHatch hatch : mDualInputHatches) fluids.addAll(Arrays.asList(hatch.getAllFluids()));
            cycle = new CycleContext(fluids, collectItems());
            prepared = false;
        } catch (RuntimeException | Error exception) {
            checkRecipeResult = SimpleCheckRecipeResult.ofFailure("thermonuclear.status.processing_failed");
            try {
                super.endRecipeProcessing();
            } catch (RuntimeException | Error cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }

    private List<ItemStack> collectItems() {
        List<ItemStack> items = new ArrayList<>();
        for (IDualInputHatch hatch : mDualInputHatches) items.addAll(Arrays.asList(hatch.getAllItems()));
        Set<GTUtility.ItemId> meItems = new HashSet<>();
        for (MTEHatchInputBus bus : mInputBusses) {
            if (!bus.isValid()) continue;
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
    public final CheckRecipeResult checkProcessing() {
        if (cycle == null) return executeCycle(this::processCycle, true);
        checkRecipeResult = prepareCycle(this::processCycle, true);
        return checkRecipeResult;
    }

    private CheckRecipeResult prepareCycle(Predicate<CycleContext> processor, boolean advancesStartup) {
        if (prepared) throw new IllegalStateException("Processing cycle was already prepared");
        prepared = true;
        operatingEUt = inputLitresPerTick = outputLitresPerTick = fullLoadEUt = 0;
        cycleAdvancesStartup = advancesStartup;
        try {
            beforeProcessingCycle(cycleContext());
            if (!processor.test(cycleContext())) return failureResult();
            cycleContext().applyInputs();
            if (advancesStartup) {
                mMaxProgresstime = CYCLE_TICKS;
                mProgresstime = 0;
                mEUt = 0;
                mEfficiency = mEfficiencyIncrease = 10000;
                mOutputItems = null;
                mOutputFluids = null;
                status = "running";
            }
            return operatingEUt > 0 ? CheckRecipeResultRegistry.GENERATING : CheckRecipeResultRegistry.SUCCESSFUL;
        } catch (RuntimeException | Error exception) {
            checkRecipeResult = SimpleCheckRecipeResult.ofFailure("thermonuclear.status.processing_failed");
            try {
                endRecipeProcessing();
            } catch (RuntimeException | Error cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }

    private CheckRecipeResult failureResult() {
        return switch (status) {
            case "fuel", "steam" -> CheckRecipeResultRegistry.NO_FUEL_FOUND;
            case "spent_full" -> CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
            case "output_full" -> CheckRecipeResultRegistry.FLUID_OUTPUT_FULL;
            default -> SimpleCheckRecipeResult.ofFailure("thermonuclear.status." + status);
        };
    }

    protected final boolean processIdleCycle(Predicate<CycleContext> processor) {
        CheckRecipeResult previousResult = checkRecipeResult;
        try {
            return executeCycle(processor, false).wasSuccessful();
        } finally {
            // Idle cooling must not replace the production/shutdown diagnostic.
            checkRecipeResult = previousResult;
        }
    }

    private CheckRecipeResult executeCycle(Predicate<CycleContext> processor, boolean advancesStartup) {
        startRecipeProcessing();
        try {
            checkRecipeResult = prepareCycle(processor, advancesStartup);
        } finally {
            if (cycle != null) endRecipeProcessing();
        }
        return checkRecipeResult;
    }

    @Override
    public final void endRecipeProcessing() {
        CycleContext context = cycleContext();
        boolean committed = false;
        try {
            if (!prepared)
                checkRecipeResult = SimpleCheckRecipeResult.ofFailure("thermonuclear.status.processing_failed");
            // Native ME extraction can still reject an otherwise successful recipe.
            super.endRecipeProcessing();
            if (prepared && checkRecipeResult.wasSuccessful()) {
                context.commit();
                committed = true;
                markDirty();
            }
        } finally {
            try {
                if (!committed) {
                    context.rollback();
                }
                updateSlots();
                for (var hatch : mInputHatches) if (hatch.isValid()) hatch.markDirty();
                for (var bus : mInputBusses) if (bus.isValid()) bus.markDirty();
                for (var hatch : mDualInputHatches) {
                    if (((MetaTileEntity) hatch).isValid()) hatch.markDirty();
                }
            } finally {
                // A rollback callback may throw too; never leave a prepaid cycle or its context alive.
                if (!committed) {
                    mMaxProgresstime = mProgresstime = 0;
                    operatingEUt = inputLitresPerTick = outputLitresPerTick = fullLoadEUt = 0;
                }
                cycle = null;
                prepared = false;
            }
        }
    }
}
