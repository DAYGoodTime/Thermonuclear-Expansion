package com.lin.thermonuclear.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.lin.thermonuclear.nuclear.RareEarthBreedingRoute;

import gregtech.api.GregTechAPI;

/** Each route has distinct working/completed items; vanilla damage stores breeding progress only. */
public final class ItemRareEarthBreederRod extends Item {

    // User-approved prototype: the same target as the thorium breeder rod.
    public static final int REQUIRED_PROGRESS = 10000;
    private final RareEarthBreedingRoute route;
    private final boolean bred;

    public ItemRareEarthBreederRod(RareEarthBreedingRoute route, boolean bred) {
        this.route = route;
        this.bred = bred;
        setUnlocalizedName("thermonuclear." + route.itemName(bred));
        setTextureName("gregtech:" + (bred ? "gt.depletedRodThorium" : "gt.rodThorium"));
        setCreativeTab(GregTechAPI.TAB_GREGTECH);
        if (!bred) {
            setMaxDamage(REQUIRED_PROGRESS);
            setNoRepair();
        }
    }

    public RareEarthBreedingRoute route() {
        return route;
    }

    public boolean isBred() {
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
        tooltip.add(
            StatCollector.translateToLocal(
                "thermonuclear.tooltip." + (bred ? "bred_rare_earth_rod" : "rare_earth_breeder_rod")));
        if (!bred) {
            tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "thermonuclear.tooltip.breeder_progress",
                    stack.getItemDamage(),
                    getMaxDamage()));
        }
    }
}
