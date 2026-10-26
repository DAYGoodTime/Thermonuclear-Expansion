package com.lin.thermonuclear.item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.lin.thermonuclear.loader.ItemLoader;
import com.lin.thermonuclear.registry.NaquadahFuelTier;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.items.ItemRadioactiveCellIC;
import ic2.api.reactor.IReactor;

public final class ItemNaquadahFuelRod extends ItemRadioactiveCellIC {

    private final NaquadahFuelTier tier;

    private ItemNaquadahFuelRod(NaquadahFuelTier tier, ItemStack depleted) {
        super(
            "thermonuclear." + tier.itemName(),
            "Naquadah Based Fuel Rod " + tier.name()
                .replace("MK_", "Mk "),
            1,
            tier.maxCycles(),
            tier.directEUt() / 2.0F,
            0,
            tier.baseHeatPerCycle(),
            depleted,
            false,
            0);
        this.tier = tier;
        // GT's processChamber rounds final pulse heat with Math.round(float), saturating at Integer.MAX_VALUE.
        // Keep the full coefficient here; only the native IC2 path is capped, not our nuclear power plant.
    }

    public static Map<NaquadahFuelTier, ItemNaquadahFuelRod> registerVariants() {
        Map<NaquadahFuelTier, ItemNaquadahFuelRod> rods = new LinkedHashMap<>();
        for (NaquadahFuelTier tier : NaquadahFuelTier.values()) {
            ItemNaquadahFuelRod rod = new ItemNaquadahFuelRod(
                tier,
                new ItemStack(ItemLoader.depletedNaquadahFuelRod(tier)));
            rods.put(tier, rod);
            ItemLoader.register(new ItemStack(rod));
        }
        return Collections.unmodifiableMap(rods);
    }

    public NaquadahFuelTier tier() {
        return tier;
    }

    @Override
    public void processChamber(IReactor reactor, ItemStack stack, int x, int y, boolean heatRun) {
        int previousHeat = reactor.getHeat();
        super.processChamber(reactor, stack, x, y, heatRun);
        // This is a single cell: GT adds at most one nonnegative int heat value per heat pass.
        // IC2's addHeat uses unchecked int addition, so restore saturation after a positive sum wraps.
        if (heatRun && previousHeat >= 0 && reactor.getHeat() < 0) reactor.setHeat(Integer.MAX_VALUE);
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return getDamageOfStack(stack) > 0 ? 1 : super.getItemStackLimit(stack);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return StatCollector.translateToLocal("item.thermonuclear." + tier.itemName() + ".name");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister icons) {
        mIcon = icons.registerIcon("gregtech:gt.rodNaquadah");
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        // Show GT's actual advDmg durability, not the generic 100-point visual damage bar.
        addAdditionalToolTips(tooltip, stack, player);
    }
}
