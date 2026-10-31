package com.lin.thermonuclear.common.machine.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.lin.thermonuclear.utils.MathUtil;

/** Original recipe stacks; templates are copied only to detect identity changes before consumption. */
final class ItemInputInventory {

    private final Map<ItemStack, Stock> stocks = new IdentityHashMap<>();
    private final List<ItemStack> inputs = new ArrayList<>();

    void add(Collection<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack == null || stack.stackSize <= 0 || stocks.containsKey(stack)) continue;
            stocks.put(stack, new Stock(stack));
            inputs.add(stack);
        }
    }

    List<ItemStack> inputs() {
        return Collections.unmodifiableList(inputs);
    }

    long available(ItemStack source) {
        Stock stock = stocks.get(source);
        return stock == null ? 0 : Math.max(0, stock.amount - stock.reserved);
    }

    long available(ItemStack template, Collection<ItemStack> scope) {
        long total = 0;
        for (ItemStack source : inputs) {
            if (contains(scope, source) && sameType(stocks.get(source).template, template)) {
                total = MathUtil.saturatingAdd(total, available(source));
            }
        }
        return total;
    }

    void reserve(ItemStack source, long amount) {
        if (amount < 0 || amount > available(source)) throw new IllegalStateException("Insufficient cycle item input");
        if (amount > 0) stocks.get(source).reserved += amount;
    }

    void reserve(ItemStack template, long amount, Collection<ItemStack> scope) {
        if (amount < 0 || amount > available(template, scope))
            throw new IllegalStateException("Insufficient cycle item input");
        for (ItemStack source : inputs) {
            if (amount == 0) break;
            if (!contains(scope, source) || !sameType(stocks.get(source).template, template)) continue;
            long used = Math.min(amount, available(source));
            reserve(source, used);
            amount -= used;
        }
    }

    boolean validate() {
        for (ItemStack source : inputs) {
            Stock stock = stocks.get(source);
            if (stock.reserved > 0 && (!sameType(stock.template, source) || source.stackSize < stock.reserved))
                return false;
        }
        return true;
    }

    void apply() {
        for (ItemStack source : inputs) source.stackSize -= (int) stocks.get(source).reserved;
    }

    private static boolean contains(Collection<ItemStack> scope, ItemStack source) {
        return scope == null || scope.stream()
            .anyMatch(stack -> stack == source);
    }

    private static boolean sameType(ItemStack first, ItemStack second) {
        return first != null && second != null
            && first.getItem() == second.getItem()
            && first.getItemDamage() == second.getItemDamage()
            && ItemStack.areItemStackTagsEqual(first, second);
    }

    private static final class Stock {

        final ItemStack template;
        final long amount;
        long reserved;

        Stock(ItemStack stack) {
            template = stack.copy();
            template.stackSize = 1;
            amount = stack.stackSize;
        }
    }
}
