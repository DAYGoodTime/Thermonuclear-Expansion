package com.lin.thermonuclear.common.nuclear.fuel;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.lin.thermonuclear.api.FuelRodAdapter;
import com.lin.thermonuclear.api.FuelRodAdapters;
import com.lin.thermonuclear.utils.StartupProgress;

/** Each occupied slot owns its wear fraction and retains spent rods until output commits. */
public final class WorkingFuelPool {

    public final List<Batch> batches = new ArrayList<>();
    private final List<Batch> staged = new ArrayList<>();
    private final List<FuelBatch.Plan> plans = new ArrayList<>();

    public static final class Batch {

        public final ItemStack fuel;
        public final FuelRodAdapter adapter;
        public final boolean spent;
        double fraction;
        private int remainingCycles;

        Batch(ItemStack fuel, double fraction, boolean spent) {
            this.fuel = fuel;
            this.spent = spent;
            adapter = spent ? null : FuelRodAdapters.find(fuel);
            remainingCycles = adapter == null ? 0 : adapter.remainingCycles(fuel);
            this.fraction = spent ? 0 : StartupProgress.fraction(fraction);
        }

        public double remaining() {
            return Math.max(0, remainingCycles - fraction);
        }

        public ItemStack output() {
            ItemStack result = fuel.copy();
            if (!spent && adapter != null) {
                if (fraction > 0) adapter.consumeCycles(result, 1);
                if (adapter.remainingCycles(result) == 0) {
                    result = adapter.depleted(result);
                    result.stackSize = fuel.stackSize;
                }
            }
            return result;
        }

        public void advance(double cycles) {
            double used = fraction + cycles;
            int damage = (int) Math.floor(used);
            if (damage > 0) adapter.consumeCycles(fuel, damage);
            remainingCycles = adapter.remainingCycles(fuel);
            fraction = StartupProgress.fraction(used - damage);
        }
    }

    public void prepareLoad(List<ItemStack> inputs, int slots, int rodsPerBatch) {
        Map<ItemStack, Integer> reserved = new IdentityHashMap<>();
        while (batches.size() < slots) {
            FuelBatch.Plan plan = FuelBatch.prepare(inputs, FuelRodAdapters.all(), rodsPerBatch, reserved);
            if (plan == null) break;
            Batch batch = new Batch(plan.fuel, 0, false);
            batches.add(batch);
            staged.add(batch);
            plans.add(plan);
        }
    }

    public void consumeInputs() {
        FuelBatch.Plan.consumeInputs(plans);
        clearPlans();
    }

    public void discardStaged() {
        batches.removeAll(staged);
        clearPlans();
    }

    public void clearPlans() {
        staged.clear();
        plans.clear();
    }

    public void finishSpent() {
        for (int i = 0; i < batches.size(); i++) {
            Batch batch = batches.get(i);
            if (!batch.spent && batch.adapter != null && batch.remainingCycles == 0) {
                ItemStack spent = batch.adapter.depleted(batch.fuel);
                spent.stackSize = batch.fuel.stackSize;
                batches.set(i, new Batch(spent, 0, true));
            }
        }
    }

    public boolean checkSlots(int slots, int rodsPerBatch) {
        if (batches.size() > slots) return true;
        for (Batch batch : batches) if (!batch.spent && batch.fuel.stackSize > rodsPerBatch) return true;
        return false;
    }

    public int workingCount() {
        int count = 0;
        for (Batch batch : batches) if (!batch.spent) count += batch.fuel.stackSize;
        return count;
    }

    public int spentCount() {
        int count = 0;
        for (Batch batch : batches) if (batch.spent) count += batch.fuel.stackSize;
        return count;
    }

    public void save(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Batch batch : batches) {
            NBTTagCompound entry = new NBTTagCompound();
            FuelBatch.save(entry, "fuel", "count", batch.fuel);
            entry.setDouble("fraction", batch.fraction);
            entry.setBoolean("spent", batch.spent);
            list.appendTag(entry);
        }
        nbt.setTag("tnFuelBatches", list);
        nbt.removeTag("tnWorkingFuel");
        nbt.removeTag("tnFuelCount");
        nbt.removeTag("tnFuelFraction");
        nbt.removeTag("tnPendingDepleted");
        nbt.removeTag("tnPendingDepletedCount");
    }

    public void load(NBTTagCompound nbt) {
        batches.clear();
        clearPlans();
        if (nbt.hasKey("tnFuelBatches", 9)) {
            NBTTagList list = nbt.getTagList("tnFuelBatches", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound entry = list.getCompoundTagAt(i);
                ItemStack fuel = FuelBatch.load(entry, "fuel", "count");
                if (fuel != null) batches.add(new Batch(fuel, entry.getDouble("fraction"), entry.getBoolean("spent")));
            }
        } else {
            ItemStack working = FuelBatch.load(nbt, "tnWorkingFuel", "tnFuelCount");
            if (working != null) batches.add(new Batch(working, nbt.getDouble("tnFuelFraction"), false));
            ItemStack spent = FuelBatch.load(nbt, "tnPendingDepleted", "tnPendingDepletedCount");
            if (spent != null) batches.add(new Batch(spent, 0, true));
        }
    }
}
