package com.lin.thermonuclear.common.machine.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.lin.thermonuclear.Thermonuclear;
import com.lin.thermonuclear.utils.StartupProgress;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTUtility;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.common.tileentities.machines.IDualInputHatch;

public abstract class TNCCycleMachineBase<T extends TNCCycleMachineBase<T>> extends ThermonuclearMultiblockBase<T> {

    public static final int CYCLE_TICKS = 20;
    protected static final int MAX_PENDING_STACKS = 65_536;
    protected static final long MAX_FLUID_OUTPUT = (long) Integer.MAX_VALUE * MAX_PENDING_STACKS;

    protected final StartupProgress startup = new StartupProgress();
    protected double fullLoadEUt;
    protected double inputLitresPerTick;
    protected double outputLitresPerTick;
    // Prepaid operating power in EU/t, not total cycle energy; excludes startup.
    protected double operatingEUt;
    protected boolean cycleAdvancesStartup = true;
    private boolean hadStructure;
    private CycleContext cycle;
    private CycleInputCandidates candidates;
    private boolean prepared;
    private boolean productionCycle;

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
        producedEUt = 0;
        super.onPostTick(tile, tick);
        if (mStartUpCheck >= 0) {
            status = "checking";
            tile.setActive(false);
            syncStartupEfficiency();
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
        syncStartupEfficiency();
        if (startup.get() != previousStartup) markDirty();
    }

    private void syncStartupEfficiency() {
        // Mirror startup for GT readouts only; generation already applies the startup multiplier.
        mEfficiency = startupTicks() > 0 ? (int) (startup.get() * 10000)
            : (running || mMaxProgresstime > 0 ? 10000 : 0);
        mEfficiencyIncrease = 0;
    }

    // Native scheduling finishes prepaid work even when the ordinary work switch is off.
    protected void clearCycleOutputRoutes() {}

    @Override
    protected void outputAfterRecipe() {
        super.outputAfterRecipe();
        clearCycleOutputRoutes();
    }

    @Override
    public void stopMachine(ShutDownReason reason) {
        super.stopMachine(reason);
        clearCycleOutputRoutes();
        operatingEUt = 0;
        running = false;
        producedEUt = 0;
    }

    protected final FluidStack[] splitFluidOutput(Fluid fluid, long amount) {
        return splitFluidOutput(new FluidStack(fluid, 1), amount);
    }

    protected final FluidStack[] splitFluidOutput(FluidStack template, long amount) {
        if (template == null) throw new IllegalArgumentException("Cannot split a null fluid");
        if (amount < 0) throw new IllegalArgumentException("Negative fluid output");
        long count = amount == 0 ? 0 : (amount - 1) / Integer.MAX_VALUE + 1;
        if (count > MAX_PENDING_STACKS) throw new CycleContext.OutputLimitException(true);
        return amount == 0 ? new FluidStack[0] : GTUtility.splitFluidStack(template, amount);
    }

    protected final void queueFluidOutput(CycleContext context, Fluid fluid, long amount) {
        queueFluidOutput(context, new FluidStack(fluid, 1), amount);
    }

    protected final void queueFluidOutput(CycleContext context, FluidStack template, long amount) {
        FluidStack[] pending = splitFluidOutput(template, amount);
        context.reserveFluidOutputs(pending.length, MAX_PENDING_STACKS);
        if (!productionCycle) {
            // 待机状态下不会处理输出，需要自行进行输出
            context.onCommit(() -> addFluidOutputs(pending));
            return;
        }
        context.onCommit(() -> { mOutputFluids = append(mOutputFluids, pending); });
    }

    private static FluidStack[] append(FluidStack[] current, FluidStack[] added) {
        int size = current == null ? 0 : current.length;
        FluidStack[] result = Arrays.copyOf(current == null ? new FluidStack[0] : current, size + added.length);
        System.arraycopy(added, 0, result, size, added.length);
        return result;
    }

    protected final FluidEjectionHelper prepareOutputs(List<? extends gregtech.api.interfaces.IOutputHatch> hatches,
        Fluid fluid, long amount) {
        return prepareOutputs(hatches, new FluidStack(fluid, 1), amount);
    }

    protected final FluidEjectionHelper prepareOutputs(List<? extends gregtech.api.interfaces.IOutputHatch> hatches,
        FluidStack template, long amount) {
        FluidStack[] outputs = splitFluidOutput(template, amount);
        if (outputs.length == 0) return null;
        FluidEjectionHelper helper = cycleContext().fluidOutputCheck(hatches);
        return helper.ejectFluids(Arrays.asList(outputs), 1) == 1 ? helper : null;
    }

    protected final void queueItemOutput(CycleContext context, ItemStack output) {
        queueItemOutputs(context, new ItemStack[] { output });
    }

    protected final ItemStack[] splitItemOutput(ItemStack template, long amount) {
        if (template == null) throw new IllegalArgumentException("Cannot split a null item");
        if (amount < 0) throw new IllegalArgumentException("Negative item output");
        long count = amount == 0 ? 0 : (amount - 1) / Integer.MAX_VALUE + 1;
        if (count > MAX_PENDING_STACKS) throw new CycleContext.OutputLimitException(false);
        ItemStack[] outputs = new ItemStack[(int) count];
        long remaining = amount;
        for (int i = 0; i < outputs.length; i++) {
            int size = (int) Math.min(remaining, Integer.MAX_VALUE);
            outputs[i] = GTUtility.copyAmountUnsafe(size, template);
            remaining -= size;
        }
        return outputs;
    }

    protected final void queueItemOutput(CycleContext context, ItemStack template, long amount) {
        queueItemOutputs(context, splitItemOutput(template, amount));
    }

    protected final void queueItemOutputs(CycleContext context, ItemStack[] pending) {
        context.reserveItemOutputs(pending.length, MAX_PENDING_STACKS);
        ItemStack[] copies = new ItemStack[pending.length];
        for (int i = 0; i < pending.length; i++) {
            if (pending[i] == null || pending[i].stackSize <= 0)
                throw new IllegalArgumentException("Invalid item output");
            copies[i] = pending[i].copy();
        }
        context.onCommit(() -> {
            int size = mOutputItems == null ? 0 : mOutputItems.length;
            ItemStack[] outputs = Arrays
                .copyOf(mOutputItems == null ? new ItemStack[0] : mOutputItems, size + copies.length);
            System.arraycopy(copies, 0, outputs, size, copies.length);
            mOutputItems = outputs;
        });
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setDouble("tnStartup", startup.get());
        nbt.setBoolean("tnHadStructure", mMachine || hadStructure);
        nbt.setInteger("tnCycleVersion", 1);
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
        mEUt = 0;
        // GT restores pending stacks and progress; never reconstruct products from consumed inputs.
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
            mOutputItems = null;
            mOutputFluids = null;
            clearCycleOutputRoutes();
            operatingEUt = fullLoadEUt = inputLitresPerTick = outputLitresPerTick = 0;
            cycleAdvancesStartup = true;
        }
        syncStartupEfficiency();
    }

    private static double finiteRate(double value) {
        return Double.isFinite(value) && value > 0 ? value : 0;
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        running = true;
        status = "running";
        generate((long) (operatingEUt * startup.next(startupTicks())));
        if (startupTicks() > 0) {
            if (cycleAdvancesStartup) startup.advance(startupTicks());
            else startup.decay(decayTicks());
        }
        return true;
    }

    @Override
    public long getEUtForDamageCalc() {
        // Wear is calculated before onRunningTick, so the reset display counter cannot be used.
        return (long) Math.min(Long.MAX_VALUE, operatingEUt * (long) startup.next(startupTicks()));
    }

    @Override
    public void onRemoval() {
        startup.clear();
        mEfficiency = mEfficiencyIncrease = 0;
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
        return available(fluid == null ? null : new FluidStack(fluid, 1), null);
    }

    protected final void consume(Fluid fluid, long amount) {
        consume(new FluidStack(fluid, 1), amount, null);
    }

    protected final long available(Fluid fluid, List<MTEHatch> hatches) {
        return available(fluid == null ? null : new FluidStack(fluid, 1), hatches);
    }

    protected final long available(FluidStack template, List<MTEHatch> hatches) {
        return cycleContext().fluidInputs()
            .available(template, hatches);
    }

    protected final void consume(Fluid fluid, long amount, List<MTEHatch> hatches) {
        consume(new FluidStack(fluid, 1), amount, hatches);
    }

    protected final void consume(FluidStack template, long amount, List<MTEHatch> hatches) {
        cycleContext().fluidInputs()
            .reserve(template, amount, hatches);
    }

    protected List<List<MTEHatch>> fluidInputPartitions() {
        List<MTEHatch> hatches = new ArrayList<>(mInputHatches);
        for (IDualInputHatch hatch : mDualInputHatches) {
            if (hatch instanceof MTEHatch input) hatches.add(input);
        }
        return Collections.singletonList(hatches);
    }

    @Override
    public final void startRecipeProcessing() {
        if (cycle != null) {
            Thermonuclear.LOG.warn("CYCLES already started,skip this");
            return;
        }
        super.startRecipeProcessing();
        try {
            candidates = new CycleInputCandidates(fluidInputPartitions(), mInputBusses, this::isFluidOnlyDualInput);
            cycle = candidates.next();
            prepared = false;
        } catch (RuntimeException | Error exception) {
            checkRecipeResult = SimpleCheckRecipeResult.ofFailure("thermonuclear.status.processing_failed");
            try {
                super.endRecipeProcessing();
            } catch (RuntimeException | Error cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            } finally {
                candidates = null;
            }
            throw exception;
        }
    }

    @Override
    public final @NotNull CheckRecipeResult checkProcessing() {
        if (cycle == null) return executeCycle(this::processCycle, true);
        checkRecipeResult = prepareCycle(this::processCycle, true);
        return checkRecipeResult;
    }

    private CheckRecipeResult prepareCycle(Predicate<CycleContext> processor, boolean advancesStartup) {
        if (prepared) throw new IllegalStateException("Processing cycle was already prepared");
        prepared = true;
        productionCycle = advancesStartup;
        String failure = null;
        try {
            while (true) {
                if (productionCycle) {
                    mOutputItems = null;
                    mOutputFluids = null;
                    clearCycleOutputRoutes();
                }
                operatingEUt = inputLitresPerTick = outputLitresPerTick = fullLoadEUt = 0;
                cycleAdvancesStartup = advancesStartup;
                beforeProcessingCycle(cycleContext());
                boolean successful;
                try {
                    successful = processor.test(cycleContext());
                } catch (CycleContext.OutputLimitException exception) {
                    successful = fail(exception.fluid ? "output_full" : "spent_full");
                }
                if (successful) {
                    if (!cycleContext().applyInputs()) {
                        status = "processing_failed";
                        return failureResult();
                    }
                    if (advancesStartup) {
                        mMaxProgresstime = CYCLE_TICKS;
                        mProgresstime = 0;
                        mEUt = 0;
                        syncStartupEfficiency();
                        status = "running";
                    }
                    return operatingEUt > 0 ? CheckRecipeResultRegistry.GENERATING
                        : CheckRecipeResultRegistry.SUCCESSFUL;
                }
                if (failure == null || inputMissing(failure)) failure = status;
                if (!candidates.hasNext()) {
                    status = failure;
                    return failureResult();
                }
                cycle.rollback();
                cycle = candidates.next();
            }
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

    private static boolean inputMissing(String status) {
        return switch (status) {
            case "fuel", "steam", "hot_fluid", "water" -> true;
            default -> false;
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
                    mOutputItems = null;
                    mOutputFluids = null;
                    clearCycleOutputRoutes();
                    operatingEUt = inputLitresPerTick = outputLitresPerTick = fullLoadEUt = 0;
                }
                cycle = null;
                candidates = null;
                prepared = false;
                productionCycle = false;
            }
        }
    }

}
