package com.lin.thermonuclear.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ItemBlockMachineComponent extends ItemBlock {

    private final BlockMachineComponent component;

    public ItemBlockMachineComponent(Block block) {
        super(block);
        this.component = (BlockMachineComponent) block;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        component.addInformation(stack, player, tooltip, advanced);
    }
}
