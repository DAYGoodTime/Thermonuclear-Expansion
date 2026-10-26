package com.lin.thermonuclear.common.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public abstract class ItemBreederRod extends ItemBase {

    private final boolean bred;

    protected ItemBreederRod(String name, boolean bred, int requiredProgress) {
        super(name, "gregtech:" + (bred ? "gt.depletedRodThorium" : "gt.rodThorium"));
        if (requiredProgress < 1) throw new IllegalArgumentException("Breeding progress must be positive");
        this.bred = bred;
        if (!bred) {
            setMaxDamage(requiredProgress);
            setNoRepair();
        }
    }

    public final boolean isBred() {
        return bred;
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return !bred && stack.getItemDamage() > 0 ? 1 : super.getItemStackLimit(stack);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return bred ? 0 : 1.0 - (double) stack.getItemDamage() / getMaxDamage();
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        if (!bred) {
            tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "thermonuclear.tooltip.breeder_progress",
                    stack.getItemDamage(),
                    getMaxDamage()));
        }
    }
}
