package com.lin.thermonuclear.block;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

public final class BlockTurbineBlade extends BlockMachineComponent {

    @SideOnly(Side.CLIENT)
    private Block steelBlock;

    @SideOnly(Side.CLIENT)
    private int steelMetadata;

    public BlockTurbineBlade() {
        super("turbine_blade");
        addInfo("thermonuclear.tooltip.turbine_blade.0");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        ItemStack steel = GTOreDictUnificator.get(OrePrefixes.block, Materials.Steel, 1);
        steelBlock = Block.getBlockFromItem(steel.getItem());
        steelMetadata = steel.getItemDamage();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        // Resolve the source icon lazily: GT's texture containers are filled during atlas stitching.
        return steelBlock.getIcon(side, steelMetadata);
    }
}
