package com.lin.thermonuclear.common.machine.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import appeng.me.GridAccessException;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.IDualInputInventory;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;

/** Native recipe inventories are alternatives, never additional stock for another buffer. */
final class CycleInputCandidates implements Iterator<CycleContext> {

    private final List<List<Input>> groups = new ArrayList<>();
    private final int[] selected;
    private boolean available = true;

    CycleInputCandidates(List<List<MTEHatch>> partitions, List<MTEHatchInputBus> busses,
        Predicate<IDualInputHatch> fluidOnly) {
        List<ItemStack> ordinaryItems = collectItems(busses);
        for (List<MTEHatch> partition : partitions) {
            List<Input> alternatives = new ArrayList<>();
            Input ordinary = new Input();
            ordinary.items.addAll(ordinaryItems);
            Set<MTEHatch> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (MTEHatch hatch : partition) {
                if (!hatch.isValid() || !seen.add(hatch)) continue;
                if (hatch instanceof IDualInputHatch dual) {
                    ItemStack[] shared = fluidOnly.test(dual) ? null : dual.getSharedItems();
                    for (Iterator<? extends IDualInputInventory> it = dual.inventories(); it.hasNext();) {
                        IDualInputInventory inventory = it.next();
                        if (inventory == null || inventory.isEmpty()) continue;
                        Input input = new Input();
                        if (dual.supportsFluids()) {
                            FluidStack[] fluids = inventory.getFluidInputs();
                            if (fluids != null) input.fluids
                                .add(new FluidInput(hatch, FluidInputSources.fromStacks(Arrays.asList(fluids), null)));
                        }
                        if (fluidOnly.test(dual)) {
                            // Fluid ports do not replace the reactor's independent fuel busses.
                            input.items.addAll(ordinaryItems);
                        } else {
                            if (shared != null) input.items.addAll(Arrays.asList(shared));
                            ItemStack[] items = inventory.getItemInputs();
                            if (items != null) input.items.addAll(Arrays.asList(items));
                        }
                        alternatives.add(input);
                    }
                } else {
                    ordinary.fluids.add(new FluidInput(hatch, FluidInputSources.open(hatch)));
                }
            }
            alternatives.add(ordinary);
            groups.add(alternatives);
        }
        selected = new int[groups.size()];
    }

    @Override
    public boolean hasNext() {
        return available;
    }

    @Override
    public CycleContext next() {
        if (!available) throw new NoSuchElementException();
        FluidInputInventory fluids = new FluidInputInventory();
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            Input input = groups.get(i)
                .get(selected[i]);
            for (FluidInput entry : input.fluids) fluids.add(entry.hatch, entry.sources);
            items.addAll(input.items);
        }
        // Enumerate independent structural ports lazily; never materialize their Cartesian product.
        available = false;
        for (int i = selected.length - 1; i >= 0; i--) {
            if (++selected[i] < groups.get(i)
                .size()) {
                available = true;
                break;
            }
            selected[i] = 0;
        }
        return new CycleContext(fluids, items);
    }

    private static List<ItemStack> collectItems(List<MTEHatchInputBus> busses) {
        List<ItemStack> items = new ArrayList<>();
        Map<Object, Map<GTUtility.ItemId, Integer>> networks = new IdentityHashMap<>();
        Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (MTEHatchInputBus bus : busses) {
            if (!bus.isValid() || bus instanceof IDualInputHatch) continue;
            Map<GTUtility.ItemId, Integer> network = null;
            Set<ItemStack> localItems = Collections.newSetFromMap(new IdentityHashMap<>());
            if (bus instanceof MTEHatchInputBusME me) {
                ItemStack[] physical = me.getRealInventory();
                for (int slot = 0; slot < physical.length; slot++) {
                    if (slot == me.getCircuitSlot() || me.isValidSlot(slot)) localItems.add(physical[slot]);
                }
                if (me.isAllowedToWork()) {
                    try {
                        Object monitor = me.getProxy()
                            .getStorage()
                            .getItemInventory();
                        network = networks.computeIfAbsent(monitor, key -> new HashMap<>());
                    } catch (GridAccessException ignored) {}
                }
            }
            for (int slot = bus.getSizeInventory() - 1; slot >= 0; slot--) {
                ItemStack stack = bus.getStackInSlot(slot);
                if (stack == null || stack.stackSize <= 0 || !seen.add(stack)) continue;
                // Classify original physical stacks through native slot APIs, not virtual slot offsets.
                if (bus instanceof MTEHatchInputBusME && !localItems.contains(stack)) {
                    if (network == null) continue;
                    Integer index = network.putIfAbsent(GTUtility.ItemId.create(stack), items.size());
                    if (index != null) {
                        if (stack.stackSize > items.get(index).stackSize) items.set(index, stack);
                        continue;
                    }
                }
                items.add(stack);
            }
        }
        return items;
    }

    private static final class Input {

        final List<FluidInput> fluids = new ArrayList<>();
        final List<ItemStack> items = new ArrayList<>();
    }

    private static final class FluidInput {

        final MTEHatch hatch;
        final List<FluidInputSources.Source> sources;

        FluidInput(MTEHatch hatch, List<FluidInputSources.Source> sources) {
            this.hatch = hatch;
            this.sources = sources;
        }
    }
}
