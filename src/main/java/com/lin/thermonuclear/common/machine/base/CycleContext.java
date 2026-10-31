package com.lin.thermonuclear.common.machine.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import gregtech.api.interfaces.IOutputHatch;
import gregtech.api.util.FluidEjectionHelper;

public final class CycleContext {

    private enum Phase {
        PREPARING,
        INPUTS_APPLIED,
        COMMITTED,
        ROLLED_BACK
    }

    private final FluidInputInventory fluidInputs;
    private final ItemInputInventory itemInputs = new ItemInputInventory();
    private final Map<List<? extends IOutputHatch>, FluidEjectionHelper> fluidOutputChecks = new HashMap<>();
    private final List<Runnable> commits = new ArrayList<>();
    private final List<Runnable> rollbacks = new ArrayList<>();
    private final List<Runnable> cleanups = new ArrayList<>();
    private Phase phase = Phase.PREPARING;
    private int fluidOutputStacks;
    private int itemOutputStacks;

    CycleContext(FluidInputInventory fluids, List<ItemStack> items) {
        fluidInputs = fluids;
        itemInputs.add(items);
    }

    FluidInputInventory fluidInputs() {
        return fluidInputs;
    }

    public List<ItemStack> availableItems() {
        return itemInputs.inputs();
    }

    public void addItemInputs(Collection<ItemStack> items) {
        require(Phase.PREPARING);
        itemInputs.add(items);
    }

    public long availableItem(ItemStack source) {
        return itemInputs.available(source);
    }

    public long availableItems(ItemStack template, Collection<ItemStack> scope) {
        return itemInputs.available(template, scope);
    }

    public void reserveItem(ItemStack source, long amount) {
        require(Phase.PREPARING);
        itemInputs.reserve(source, amount);
    }

    public void reserveItems(ItemStack template, long amount, Collection<ItemStack> scope) {
        require(Phase.PREPARING);
        itemInputs.reserve(template, amount, scope);
    }

    FluidEjectionHelper fluidOutputCheck(List<? extends IOutputHatch> hatches) {
        require(Phase.PREPARING);
        return fluidOutputChecks
            .computeIfAbsent(new ArrayList<>(hatches), key -> new FluidEjectionHelper(key, true, true));
    }

    public void onCommit(Runnable action) {
        require(Phase.PREPARING);
        commits.add(action);
    }

    public void onRollback(Runnable action) {
        require(Phase.PREPARING);
        rollbacks.add(action);
    }

    public void onClose(Runnable action) {
        require(Phase.PREPARING);
        cleanups.add(action);
    }

    void reserveFluidOutputs(int count, int limit) {
        require(Phase.PREPARING);
        if (count < 0 || (long) fluidOutputStacks + count > limit) throw new OutputLimitException(true);
        fluidOutputStacks += count;
    }

    void reserveItemOutputs(int count, int limit) {
        require(Phase.PREPARING);
        if (count < 0 || (long) itemOutputStacks + count > limit) throw new OutputLimitException(false);
        itemOutputStacks += count;
    }

    boolean applyInputs() {
        require(Phase.PREPARING);
        // Both plans must pass before either kind of original stack is modified.
        if (!fluidInputs.validate() || !itemInputs.validate()) return false;
        fluidInputs.apply();
        itemInputs.apply();
        phase = Phase.INPUTS_APPLIED;
        return true;
    }

    void commit() {
        require(Phase.INPUTS_APPLIED);
        phase = Phase.COMMITTED;
        try {
            for (Runnable commit : commits) commit.run();
        } finally {
            close();
        }
    }

    void rollback() {
        if (phase == Phase.COMMITTED || phase == Phase.ROLLED_BACK) return;
        phase = Phase.ROLLED_BACK;
        try {
            for (int i = rollbacks.size() - 1; i >= 0; i--) rollbacks.get(i)
                .run();
        } finally {
            close();
        }
    }

    private void close() {
        fluidOutputChecks.clear();
        commits.clear();
        rollbacks.clear();
        try {
            for (Runnable cleanup : cleanups) cleanup.run();
        } finally {
            cleanups.clear();
        }
    }

    private void require(Phase expected) {
        if (phase != expected) throw new IllegalStateException("Cycle is " + phase + ", expected " + expected);
    }

    static final class OutputLimitException extends RuntimeException {

        final boolean fluid;

        OutputLimitException(boolean fluid) {
            super("Too many pending " + (fluid ? "fluid" : "item") + " stacks");
            this.fluid = fluid;
        }
    }
}
