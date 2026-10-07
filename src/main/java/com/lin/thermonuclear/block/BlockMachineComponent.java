package com.lin.thermonuclear.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.IIcon;

import com.lin.thermonuclear.Thermonuclear;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class BlockMachineComponent extends Block {

    private final String sideTexture;
    private final String topTexture;
    private final String bottomTexture;

    @SideOnly(Side.CLIENT)
    private IIcon topIcon;

    @SideOnly(Side.CLIENT)
    private IIcon bottomIcon;

    public BlockMachineComponent(String name, String sideTexture, String topTexture, String bottomTexture) {
        super(Material.iron);
        this.sideTexture = sideTexture;
        this.topTexture = topTexture;
        this.bottomTexture = bottomTexture;
        setBlockName(Thermonuclear.MODID + "." + name);
        setBlockTextureName(Thermonuclear.MODID + ":" + sideTexture);
        setCreativeTab(CreativeTabs.tabBlock);
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(Thermonuclear.MODID + ":" + sideTexture);
        topIcon = register.registerIcon(Thermonuclear.MODID + ":" + topTexture);
        bottomIcon = register.registerIcon(Thermonuclear.MODID + ":" + bottomTexture);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return side == 0 ? bottomIcon : side == 1 ? topIcon : blockIcon;
    }
}
