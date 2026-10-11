package com.lin.thermonuclear.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.lin.thermonuclear.loader.BlockLoader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ItemBlockMachineComponent extends ItemBlock {

    public ItemBlockMachineComponent(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block == BlockLoader.turbineBlade) {
            tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.turbine_blade"));
            return;
        }
        boolean shaft = block == BlockLoader.lowPressureTurbineShaft || block == BlockLoader.highPressureTurbineShaft;
        if (shaft) {
            tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.turbine_shaft.0"));
            tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.turbine_shaft.1"));
            tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.turbine_shaft.2"));
        } else {
            tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.component.0"));
            tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.component.1"));
            tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.component.2"));
        }
    }
}
