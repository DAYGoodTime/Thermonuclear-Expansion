package com.lin.thermonuclear.common.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.lin.thermonuclear.Thermonuclear;
import com.lin.thermonuclear.common.loader.ItemLoader;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class BlockMachineComponent extends Block {

    public static final int AXIS_Y = 0;
    public static final int AXIS_X = 4;
    public static final int AXIS_Z = 8;

    private final String registryName;
    private final List<TooltipLine> info = new ArrayList<>();
    private final boolean axial;
    private final String sideTexture;
    private final String topTexture;
    private final String bottomTexture;

    @SideOnly(Side.CLIENT)
    private IIcon topIcon;

    @SideOnly(Side.CLIENT)
    private IIcon bottomIcon;

    protected BlockMachineComponent(String name) {
        this(name, false, null, null, null);
    }

    protected BlockMachineComponent(String name, boolean axial, String sideTexture, String topTexture,
        String bottomTexture) {
        super(Material.iron);
        this.registryName = name;
        this.axial = axial;
        this.sideTexture = sideTexture;
        this.topTexture = topTexture;
        this.bottomTexture = bottomTexture;
        setBlockName(Thermonuclear.MODID + "." + name);
        if (sideTexture != null) setBlockTextureName(Thermonuclear.MODID + ":" + sideTexture);
        setCreativeTab(CreativeTabs.tabBlock);
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 1);
    }

    public final void register() {
        GameRegistry.registerBlock(this, ItemBlockMachineComponent.class, registryName);
        ItemLoader.register(new ItemStack(this));
    }

    protected final void addInfo(String translationKey, Object... arguments) {
        info.add(new TooltipLine(translationKey, arguments));
    }

    @Override
    public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        for (TooltipLine line : info) {
            tooltip.add(line.translate());
        }
    }

    private static final class TooltipLine {

        private final String translationKey;
        private final Object[] arguments;

        private TooltipLine(String translationKey, Object[] arguments) {
            this.translationKey = Objects.requireNonNull(translationKey, "Tooltip translation key");
            this.arguments = arguments.clone();
        }

        @SideOnly(Side.CLIENT)
        private String translate() {
            // Keep keys until hover time so changing the client language also updates shared info.
            return arguments.length == 0 ? StatCollector.translateToLocal(translationKey)
                : StatCollector.translateToLocalFormatted(translationKey, arguments);
        }
    }

    public static int axisMetadata(ForgeDirection direction) {
        if (direction.offsetX != 0) return AXIS_X;
        if (direction.offsetZ != 0) return AXIS_Z;
        return AXIS_Y;
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
        int metadata) {
        return axial ? axisMetadata(ForgeDirection.getOrientation(side))
            : super.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, metadata);
    }

    @Override
    public int getRenderType() {
        // Vanilla's log renderer also rotates side UVs to follow the metadata axis.
        return axial ? 31 : super.getRenderType();
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
        if (axial) {
            int axis = metadata & 12;
            int bottomSide = axis == AXIS_X ? 4 : axis == AXIS_Z ? 2 : 0;
            side = side == bottomSide ? 0 : side == bottomSide + 1 ? 1 : 2;
        }
        return side == 0 ? bottomIcon : side == 1 ? topIcon : blockIcon;
    }
}
