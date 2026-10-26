package com.lin.thermonuclear.item;

import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.lin.thermonuclear.registry.NaquadahFuelTier;

public final class ItemDepletedNaquadahFuelRod extends ItemBase {

    private ItemDepletedNaquadahFuelRod(NaquadahFuelTier tier) {
        super(tier.depletedItemName(), "gregtech:gt.depletedRodNaquadah");
    }

    public static Map<NaquadahFuelTier, ItemDepletedNaquadahFuelRod> registerVariants() {
        return registerVariants(NaquadahFuelTier.values(), ItemDepletedNaquadahFuelRod::new);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.naquadah_fuel_rod.depleted"));
    }
}
