package com.lin.thermonuclear.item;

import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.lin.thermonuclear.registry.NaquadahFuelTier;

/** Distinct tier items; vanilla item damage stores used fuel cycles, not a tier index. */
public final class ItemNaquadahFuelRod extends ItemBase {

    private final NaquadahFuelTier tier;
    private final boolean depleted;

    private ItemNaquadahFuelRod(NaquadahFuelTier tier, boolean depleted) {
        super(
            depleted ? tier.depletedItemName() : tier.itemName(),
            "gregtech:" + (depleted ? "gt.depletedRodNaquadah" : "gt.rodNaquadah"));
        this.tier = tier;
        this.depleted = depleted;
        if (!depleted) {
            setMaxDamage(tier.maxCycles());
            setNoRepair();
        }
    }

    public static Map<NaquadahFuelTier, ItemNaquadahFuelRod> registerVariants() {
        return registerVariants(false);
    }

    public static Map<NaquadahFuelTier, ItemNaquadahFuelRod> registerVariants(boolean depleted) {
        return registerVariants(NaquadahFuelTier.values(), tier -> new ItemNaquadahFuelRod(tier, depleted));
    }

    public NaquadahFuelTier tier() {
        return tier;
    }

    public boolean isDepleted() {
        return depleted;
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return !depleted && stack.getItemDamage() > 0 ? 1 : super.getItemStackLimit(stack);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "thermonuclear.tooltip.naquadah_fuel_rod.capacity",
                NaquadahFuelTier.FUEL_LITRES));
        if (depleted) {
            tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.naquadah_fuel_rod.depleted"));
            return;
        }
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "thermonuclear.tooltip.naquadah_fuel_rod.durability",
                Math.max(0, tier.maxCycles() - Math.max(0, stack.getItemDamage())),
                tier.maxCycles()));
        tooltip.add(
            StatCollector
                .translateToLocalFormatted("thermonuclear.tooltip.naquadah_fuel_rod.direct", tier.directEUt()));
        tooltip.add(
            StatCollector
                .translateToLocalFormatted("thermonuclear.tooltip.naquadah_fuel_rod.heat", tier.baseHeatPerCycle()));
        tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.naquadah_fuel_rod.use"));
        tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.naquadah_fuel_rod.reflection"));
    }
}
