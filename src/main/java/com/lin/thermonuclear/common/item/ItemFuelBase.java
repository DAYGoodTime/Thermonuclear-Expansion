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

    protected ItemFuelBase(String name, String texture, int cycles, float energy, float heat, ItemStack depleted) {
        // GT registers working fuel under its historical namespace and creates the native NEI recipe.
        super(Thermonuclear.MODID + "." + name, name, 1, cycles, energy, 0, heat, depleted, false, 0);
        displayNameKey = "item." + Thermonuclear.MODID + "." + name + ".name";
        this.texture = texture;
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
        // Single-cell GT heat is capped to int; IC2's unchecked hull addition can still wrap.
        if (heatRun && previousHeat >= 0 && reactor.getHeat() < 0) reactor.setHeat(Integer.MAX_VALUE);
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        // Lifetime is stored in advDmg; normal NBT equality keeps different lifetimes apart.
        // Vanilla otherwise rejects stacking damaged items even with identical NBT.
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
        // Display advDmg rather than the generic 100-point visual damage bar.
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
