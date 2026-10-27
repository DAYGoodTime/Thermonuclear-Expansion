package com.lin.thermonuclear.common.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.lin.thermonuclear.Thermonuclear;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.items.ItemRadioactiveCellIC;
import ic2.api.reactor.IReactor;

public abstract class ItemFuelBase extends ItemRadioactiveCellIC {

    private final String displayNameKey;
    private final String texture;
    private final int maxCycles;
    private final long directEUt;
    private final long baseHeatPerCycle;

    protected ItemFuelBase(String name, String texture, int maxCycles, long directEUt, long baseHeatPerCycle,
        ItemStack depleted) {
        // GT registers working fuel under its historical namespace and creates the native NEI recipe.
        super(
            Thermonuclear.MODID + "." + name,
            name,
            1,
            maxCycles,
            directEUt / 2.0F,
            0,
            baseHeatPerCycle,
            depleted,
            false,
            0);
        displayNameKey = "item." + Thermonuclear.MODID + "." + name + ".name";
        this.texture = texture;
        this.maxCycles = maxCycles;
        this.directEUt = directEUt;
        this.baseHeatPerCycle = baseHeatPerCycle;
    }

    public final int maxCycles() {
        return maxCycles;
    }

    public final long directEUt() {
        return directEUt;
    }

    public final long baseHeatPerCycle() {
        return baseHeatPerCycle;
    }

    protected static ItemStack registerDepleted(String name, String texture) {
        DepletedFuel item = new DepletedFuel(name, texture);
        item.register();
        return new ItemStack(item);
    }

    public final ItemStack depletedStack() {
        return sDepleted.copy();
    }

    @Override
    public void processChamber(IReactor reactor, ItemStack stack, int x, int y, boolean heatRun) {
        int previousHeat = reactor.getHeat();
        super.processChamber(reactor, stack, x, y, heatRun);
        if (heatRun && previousHeat >= 0 && reactor.getHeat() < 0) reactor.setHeat(Integer.MAX_VALUE);
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        return false;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getDamageOfStack(stack) > 0;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return StatCollector.translateToLocal(displayNameKey);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister icons) {
        mIcon = icons.registerIcon(texture);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        addAdditionalToolTips(tooltip, stack, player);
    }

    private static final class DepletedFuel extends ItemBase {

        private DepletedFuel(String name, String texture) {
            super(name, texture);
        }

        @Override
        public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
            super.addInformation(stack, player, tooltip, advanced);
        }
    }
}
