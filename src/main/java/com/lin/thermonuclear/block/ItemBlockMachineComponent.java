package com.lin.thermonuclear.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import com.lin.thermonuclear.loader.BlockLoader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.util.GTSplit;

public final class ItemBlockMachineComponent extends ItemBlock {

    public ItemBlockMachineComponent(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        Block block = Block.getBlockFromItem(stack.getItem());
        boolean shaft = block == BlockLoader.lowPressureTurbineShaft || block == BlockLoader.highPressureTurbineShaft;
        GTSplit.splitLocalizedFormatted(
            tooltip,
            shaft ? "thermonuclear.tooltip.turbine_shaft" : "thermonuclear.tooltip.component");
    }
}
