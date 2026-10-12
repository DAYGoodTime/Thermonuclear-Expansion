package com.lin.thermonuclear.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import gregtech.api.GregTechAPI;

/** Progress is stored as vanilla item damage, matching the independent breeder pool. */
public final class ItemThoriumBreederRod extends Item {

    // Prototype target matches the existing GT lithium/glowstone breeder rods, not final balance.
    public static final int REQUIRED_PROGRESS = 10000;
    private final boolean bred;

    public ItemThoriumBreederRod(boolean bred) {
        this.bred = bred;
        setUnlocalizedName("thermonuclear." + (bred ? "bred_thorium_rod" : "thorium_breeder_rod"));
        setTextureName("gregtech:" + (bred ? "gt.depletedRodThorium" : "gt.rodThorium"));
        setCreativeTab(GregTechAPI.TAB_GREGTECH);
        if (!bred) {
            setMaxDamage(REQUIRED_PROGRESS);
            setNoRepair();
        }
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
        if (bred) {
            tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.bred_thorium_rod"));
        } else {
            tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.thorium_breeder_rod.0"));
            tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.thorium_breeder_rod.1"));
        }
        if (!bred) {
            tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "thermonuclear.tooltip.breeder_progress",
                    stack.getItemDamage(),
                    getMaxDamage()));
        }
    }
}
