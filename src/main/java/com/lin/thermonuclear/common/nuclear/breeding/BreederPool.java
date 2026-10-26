package com.lin.thermonuclear.common.nuclear.breeding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.lin.thermonuclear.common.nuclear.fuel.FuelBatch;

/** Independent controller inventory. Completed rods retain their capacity reservation until output commits. */
public final class BreederPool {

    private final List<ItemStack> working = new ArrayList<>();
    private final List<ItemStack> completed = new ArrayList<>();

    private float lowestProgress = 0.0f;
    private float highestProgress = 0.0f;

    public int workingCount() {
        return count(working);
    }

    public int completedCount() {
        return count(completed);
    }

    private static int count(List<ItemStack> stacks) {
        int count = 0;
        for (ItemStack stack : stacks) count += stack.stackSize;
        return count;
    }

    /** Stages copies in the pool; physical input stacks are only consumed after the host transaction succeeds. */
    public Runnable prepareLoad(List<ItemStack> inputs, int maxRods) {
        int available = maxRods - workingCount() - completedCount();
        List<ItemStack> sources = new ArrayList<>();
        List<Integer> amounts = new ArrayList<>();
        Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ItemStack source : inputs) {
            if (available <= 0) break;
            if (source == null || source.stackSize <= 0 || !GTBreederRod.accepts(source) || !seen.add(source)) continue;
            int amount = Math.min(available, source.stackSize);
            ItemStack batch = source.copy();
            batch.stackSize = amount;
            add(working, batch);
            sources.add(source);
            amounts.add(amount);
            available -= amount;
        }
        return () -> {
            for (int i = 0; i < sources.size(); i++) {
                if (sources.get(i).stackSize < amounts.get(i)) {
                    throw new IllegalStateException("Breeder input changed during recipe processing");
                }
            }
            for (int i = 0; i < sources.size(); i++) sources.get(i).stackSize -= amounts.get(i);
        };
    }

    public void advance(long pulses) {
        if (pulses <= 0) return;
        Iterator<ItemStack> iterator = working.iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            if (!GTBreederRod.accepts(stack)) continue;
            int target = stack.getMaxDamage();
            long remaining = target - (long) Math.max(0, stack.getItemDamage());
            // Compare before multiplying: no overflow even for an external adapter's very large pulse count.
            if (remaining <= 0 || pulses >= (remaining + 3) / 4) {
                add(completed, GTBreederRod.product(stack));
                iterator.remove();
            } else {
                stack.setItemDamage(Math.max(0, stack.getItemDamage()) + (int) (pulses * 4));
            }
        }
        updateSummary();
    }

    private static void add(List<ItemStack> pool, ItemStack stack) {
        for (ItemStack existing : pool) {
            if (FuelBatch.sameType(existing, stack)) {
                existing.stackSize += stack.stackSize;
                return;
            }
        }
        pool.add(stack);
    }

    public List<ItemStack> copyCompleted() {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : completed) result.add(stack.copy());
        return result;
    }

    public void finishOutput(List<ItemStack> remaining) {
        completed.clear();
        for (ItemStack stack : remaining) if (stack.stackSize > 0) completed.add(stack);
    }

    private void updateSummary() {
        if (working.isEmpty()) return;
        double lowest = 100;
        double highest = 0;
        for (ItemStack stack : working) {
            if (stack.getMaxDamage() <= 0) continue;
            double progress = Math.max(0, Math.min(100, stack.getItemDamage() * 100.0 / stack.getMaxDamage()));
            lowest = Math.min(lowest, progress);
            highest = Math.max(highest, progress);
        }
        lowestProgress = (float) lowest;
        highestProgress = (float) highest;
    }

    public float getLowestProgress() {
        return lowestProgress;
    }

    public float getHighestProgress() {
        return highestProgress;
    }

    public void save(NBTTagCompound nbt) {
        saveList(nbt, "tnBreederWorking", working);
        saveList(nbt, "tnBreederCompleted", completed);
    }

    private static void saveList(NBTTagCompound nbt, String key, List<ItemStack> stacks) {
        NBTTagList list = new NBTTagList();
        for (ItemStack stack : stacks) {
            NBTTagCompound entry = new NBTTagCompound();
            FuelBatch.save(entry, "rod", "count", stack);
            list.appendTag(entry);
        }
        nbt.setTag(key, list);
    }

    public void load(NBTTagCompound nbt) {
        loadList(nbt, "tnBreederWorking", working);
        loadList(nbt, "tnBreederCompleted", completed);
    }

    private static void loadList(NBTTagCompound nbt, String key, List<ItemStack> stacks) {
        stacks.clear();
        NBTTagList list = nbt.getTagList(key, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            ItemStack stack = FuelBatch.load(list.getCompoundTagAt(i), "rod", "count");
            if (stack != null) add(stacks, stack);
        }
    }
}
