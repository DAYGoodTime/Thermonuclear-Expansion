package com.lin.thermonuclear.common.machine.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraftforge.fluids.FluidStack;

import com.lin.thermonuclear.common.machine.base.FluidInputSources.Source;
import com.lin.thermonuclear.utils.MathUtil;

import gregtech.api.util.GTUtility;

/** One cycle's long-sized stock views and reservations, shared by global and position-specific inputs. */
final class FluidInputInventory {

    private final List<Stock> stocks = new ArrayList<>();
    private final Map<FluidStack, Stock> stocksByReference = new IdentityHashMap<>();
    private final Map<Object, Map<GTUtility.FluidId, Stock>> stocksByIdentity = new IdentityHashMap<>();
    private final List<Reservation> reservations = new ArrayList<>();
    private boolean applied;

    void add(Object hatch, List<Source> sources) {
        for (Source source : sources) {
            FluidStack fluid = Objects.requireNonNull(source.fluid())
                .copy();
            fluid.amount = 1;
            Object identity = Objects.requireNonNull(source.storageIdentity());
            long amount = source.available();
            if (amount < 0) throw new IllegalArgumentException("Negative fluid stock");
            Map<GTUtility.FluidId, Stock> inventory = stocksByIdentity
                .computeIfAbsent(identity, key -> new HashMap<>());
            GTUtility.FluidId fluidId = GTUtility.FluidId.create(fluid);
            Stock stock = stocksByReference.get(source.fluid());
            if (stock == null) stock = inventory.get(fluidId);
            if (stock == null) {
                stock = new Stock(fluid);
                stocks.add(stock);
            }
            inventory.put(fluidId, stock);
            stocksByReference.put(source.fluid(), stock);
            stock.views.add(new View(hatch, source, amount));
        }
    }

    long available(FluidStack fluid, Collection<?> hatches) {
        if (fluid == null) return 0;
        long total = 0;
        for (Map.Entry<Stock, View> entry : views(fluid, hatches).entrySet()) {
            total = MathUtil.saturatingAdd(total, remaining(entry.getKey(), entry.getValue()));
        }
        return total;
    }

    void reserve(FluidStack fluid, long amount, Collection<?> hatches) {
        if (applied) throw new IllegalStateException("Fluid inputs already applied");
        if (amount < 0) throw new IllegalArgumentException("Negative fluid consumption");
        if (amount == 0) return;
        if (fluid == null) throw new IllegalArgumentException("Cannot consume a null fluid");
        if (available(fluid, hatches) < amount) throw new IllegalStateException("Insufficient cycle fluid input");
        // Keep structure order and reserve each original stack only once across positional views.
        Map<Stock, View> views = views(fluid, hatches);
        for (Stock stock : stocks) {
            View view = views.get(stock);
            if (view == null) continue;
            long used = Math.min(amount, remaining(stock, view));
            if (used == 0) continue;
            stock.reserved += used;
            reservations.add(new Reservation(view.source, used));
            amount -= used;
            if (amount == 0) return;
        }
    }

    boolean validate() {
        if (applied) throw new IllegalStateException("Fluid inputs already applied");
        Map<FluidStack, Long> totals = new IdentityHashMap<>();
        for (Reservation reservation : reservations) {
            totals.merge(reservation.source.fluid(), reservation.amount, MathUtil::saturatingAdd);
        }
        // Validate the complete plan before changing even the first original stack.
        for (Reservation reservation : reservations) {
            if (reservation.source.available() < totals.get(reservation.source.fluid())) return false;
        }
        return true;
    }

    void apply() {
        if (applied) throw new IllegalStateException("Fluid inputs already applied");
        applied = true;
        for (Reservation reservation : reservations) reservation.source.consume(reservation.amount);
    }

    private Map<Stock, View> views(FluidStack requested, Collection<?> hatches) {
        Map<Stock, View> result = new IdentityHashMap<>();
        for (Stock stock : stocks) {
            if (!stock.fluid.isFluidEqual(requested)) continue;
            View largest = null;
            for (View view : stock.views) {
                if (hatches != null && hatches.stream()
                    .noneMatch(hatch -> hatch == view.hatch)) continue;
                if (largest == null || view.amount > largest.amount) largest = view;
            }
            if (largest != null) result.put(stock, largest);
        }
        return result;
    }

    private static long remaining(Stock stock, View view) {
        return view.amount <= stock.reserved ? 0 : view.amount - stock.reserved;
    }

    private static final class Stock {

        final FluidStack fluid;
        final List<View> views = new ArrayList<>();
        long reserved;

        Stock(FluidStack fluid) {
            this.fluid = fluid;
        }
    }

    private static final class View {

        final Object hatch;
        final Source source;
        final long amount;

        View(Object hatch, Source source, long amount) {
            this.hatch = hatch;
            this.source = source;
            this.amount = amount;
        }
    }

    private static final class Reservation {

        final Source source;
        final long amount;

        Reservation(Source source, long amount) {
            this.source = source;
            this.amount = amount;
        }
    }
}
