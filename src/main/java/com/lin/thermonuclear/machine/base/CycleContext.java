package com.lin.thermonuclear.machine.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public final class CycleContext {

    private enum Phase {
        PREPARING,
        INPUTS_APPLIED,
        COMMITTED,
        ROLLED_BACK
    }

    private final List<FluidStack> availableFluids;
    private final List<ItemStack> availableItems;
    private final List<Runnable> inputs = new ArrayList<>();
    private final List<Runnable> commits = new ArrayList<>();
    private final List<Runnable> rollbacks = new ArrayList<>();
    private final List<Runnable> cleanups = new ArrayList<>();
    private Phase phase = Phase.PREPARING;

    CycleContext(List<FluidStack> fluids, List<ItemStack> items) {
        availableFluids = Collections.unmodifiableList(new ArrayList<>(fluids));
        availableItems = Collections.unmodifiableList(new ArrayList<>(items));
    }

    public List<FluidStack> availableFluids() {
        return availableFluids;
    }

    public List<ItemStack> availableItems() {
        return availableItems;
    }

    public void deferInput(Runnable action) {
        require(Phase.PREPARING);
        inputs.add(action);
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

    void applyInputs() {
        require(Phase.PREPARING);
        phase = Phase.INPUTS_APPLIED;
        for (Runnable input : inputs) input.run();
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
        inputs.clear();
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
}
