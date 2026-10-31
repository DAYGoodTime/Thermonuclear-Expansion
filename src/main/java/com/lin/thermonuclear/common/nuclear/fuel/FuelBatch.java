package com.lin.thermonuclear.common.nuclear.fuel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ObjLongConsumer;
import java.util.function.ToLongFunction;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.lin.thermonuclear.api.FuelRodAdapter;

public final class FuelBatch {

    private FuelBatch() {}

    static Plan prepare(List<ItemStack> inputs, List<FuelRodAdapter> adapters, int maxRods,
        Map<ItemStack, Long> reserved, ToLongFunction<ItemStack> available) {
        if (maxRods <= 0) throw new IllegalArgumentException("Invalid fuel batch limit");
        ItemStack selected = null;
        int lowestLifetime = Integer.MAX_VALUE;
        for (ItemStack stack : inputs) {
            if (stack == null || available.applyAsLong(stack) <= reserved.getOrDefault(stack, 0L)) continue;
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
            long remaining = available.applyAsLong(stack) - reserved.getOrDefault(stack, 0L);
            if (remaining <= 0) continue;
            int amount = (int) Math.min(maxRods - batch.stackSize, remaining);
            sources.add(stack);
            amounts.add(amount);
            batch.stackSize += amount;
            reserved.put(stack, reserved.getOrDefault(stack, 0L) + amount);
            if (batch.stackSize == maxRods) break;
        }
        return new Plan(batch, sources, amounts);
    }

    private static FuelRodAdapter findAdapter(ItemStack stack, List<FuelRodAdapter> adapters) {
        for (FuelRodAdapter adapter : adapters) {
            if (adapter.accepts(stack)) return adapter;
        }
        return null;
    }

    public static final class Plan {

        public final ItemStack fuel;
        private final List<ItemStack> sources;
        private final List<Integer> amounts;

        private Plan(ItemStack fuel, List<ItemStack> sources, List<Integer> amounts) {
            this.fuel = fuel;
            this.sources = sources;
            this.amounts = amounts;
        }

        static void reserveInputs(List<Plan> plans, ObjLongConsumer<ItemStack> reserve) {
            for (Plan plan : plans) {
                for (int i = 0; i < plan.sources.size(); i++) {
                    reserve.accept(plan.sources.get(i), plan.amounts.get(i));
                }
            }
        }
    }

    public static boolean sameType(ItemStack first, ItemStack second) {
        return first.getItem() == second.getItem() && first.getItemDamage() == second.getItemDamage()
            && ItemStack.areItemStackTagsEqual(first, second);
    }

    public static void save(NBTTagCompound nbt, String key, String countKey, ItemStack stack) {
        if (stack == null) {
            nbt.removeTag(key);
            nbt.removeTag(countKey);
            return;
        }
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
        if (count <= 0) return null;
        stack.stackSize = count;
        return stack;
    }
}
