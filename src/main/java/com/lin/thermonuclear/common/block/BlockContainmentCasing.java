package com.lin.thermonuclear.common.block;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;

public final class BlockContainmentCasing extends BlockMachineComponent {

    @SideOnly(Side.CLIENT)
    private Block reinforcedConcreteBlock;

    @SideOnly(Side.CLIENT)
    private int reinforcedConcreteMetadata;

    public BlockContainmentCasing() {
        super("containment_casing");
        GregTechAPI.registerMachineBlock(this, -1);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        if (GregTechAPI.isMachineBlock(this, world.getBlockMetadata(x, y, z))) {
            GregTechAPI.causeMachineUpdate(world, x, y, z);
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        super.breakBlock(world, x, y, z, block, metadata);
        if (GregTechAPI.isMachineBlock(this, metadata)) {
            GregTechAPI.causeMachineUpdate(world, x, y, z);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        ItemStack reinforcedConcrete = ItemList.Block_ReinforcedConcrete.get(1);
        reinforcedConcreteBlock = Block.getBlockFromItem(reinforcedConcrete.getItem());
        reinforcedConcreteMetadata = reinforcedConcrete.getItemDamage();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        // Resolve lazily after GT's texture containers have been populated during atlas stitching.
        return reinforcedConcreteBlock.getIcon(side, reinforcedConcreteMetadata);
    }
}
