package com.lin.thermonuclear.nuclear;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class FuelBatch {

    public static final int MAX_RODS = 2048;

    private FuelBatch() {}

    public static Plan prepare(List<ItemStack> inputs, FuelRodAdapter[] adapters, int maxRods) {
        if (maxRods <= 0 || maxRods > MAX_RODS) throw new IllegalArgumentException("Invalid fuel batch limit");
        ItemStack selected = null;
        int lowestLifetime = Integer.MAX_VALUE;
        for (ItemStack stack : inputs) {
            if (stack == null || stack.stackSize <= 0) continue;
            FuelRodAdapter adapter = findAdapter(stack, adapters);
            if (adapter == null) continue;
            int lifetime = adapter.remainingCycles(stack);
            if (lifetime > 0 && (selected == null || lifetime < lowestLifetime)) {
                selected = stack;
                lowestLifetime = lifetime;
            }
        }
        if (selected == null) return null;

        ItemStack batch = selected.copy();
        batch.stackSize = 0;
        List<ItemStack> sources = new ArrayList<>();
        List<Integer> amounts = new ArrayList<>();
        Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ItemStack stack : inputs) {
            if (stack == null || stack.stackSize <= 0 || !sameType(batch, stack) || !seen.add(stack)) continue;
            int amount = Math.min(maxRods - batch.stackSize, stack.stackSize);
            sources.add(stack);
            amounts.add(amount);
            batch.stackSize += amount;
            if (batch.stackSize == maxRods) break;
        }
        return new Plan(batch, sources, amounts);
    }

    public static final class Plan {

        public final ItemStack fuel;
        private final List<ItemStack> sources;
        private final List<Integer> amounts;
        private boolean consumed;

        private Plan(ItemStack fuel, List<ItemStack> sources, List<Integer> amounts) {
            this.fuel = fuel;
            this.sources = sources;
            this.amounts = amounts;
        }

        public void consumeInputs() {
            if (consumed) throw new IllegalStateException("Fuel inputs already consumed");
            for (int i = 0; i < sources.size(); i++) {
                if (sources.get(i).stackSize < amounts.get(i)) {
                    throw new IllegalStateException("Fuel input changed during recipe processing");
                }
            }
            // Mutate GT recipe references, not physical slots: ME and DEBUG use virtual stacks.
            for (int i = 0; i < sources.size(); i++) {
                sources.get(i).stackSize -= amounts.get(i);
            }
            consumed = true;
        }
    }

    public static boolean sameType(ItemStack first, ItemStack second) {
        return first.getItem() == second.getItem() && first.getItemDamage() == second.getItemDamage()
            && ItemStack.areItemStackTagsEqual(first, second);
    }

    public static FuelRodAdapter findAdapter(ItemStack stack, FuelRodAdapter[] adapters) {
        for (FuelRodAdapter adapter : adapters) {
            if (adapter.accepts(stack)) return adapter;
        }
        return null;
    }

    public static void save(NBTTagCompound nbt, String key, String countKey, ItemStack stack) {
        if (stack == null) {
            nbt.removeTag(key);
            nbt.removeTag(countKey);
            return;
        }
        // Minecraft 1.7.10 serializes Count as a signed byte; keep the batch count separately.
        ItemStack representative = stack.copy();
        representative.stackSize = 1;
        nbt.setTag(key, representative.writeToNBT(new NBTTagCompound()));
        nbt.setInteger(countKey, stack.stackSize);
    }

    public static ItemStack load(NBTTagCompound nbt, String key, String countKey) {
        if (!nbt.hasKey(key)) return null;
        ItemStack stack = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(key));
        if (stack == null) return null;
        int count = nbt.hasKey(countKey) ? nbt.getInteger(countKey) : stack.stackSize;
        if (count <= 0 || count > MAX_RODS) return null;
        stack.stackSize = count;
        return stack;
    }
}
