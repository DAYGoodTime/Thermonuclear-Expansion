package com.lin.thermonuclear.block;

import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class BlockAxialMachineComponent extends BlockMachineComponent {

    public static final int AXIS_Y = 0;
    public static final int AXIS_X = 4;
    public static final int AXIS_Z = 8;

    public BlockAxialMachineComponent(String name, String sideTexture, String topTexture, String bottomTexture) {
        super(name, sideTexture, topTexture, bottomTexture);
    }

    public static boolean isAxisMetadata(int metadata) {
        return metadata == AXIS_Y || metadata == AXIS_X || metadata == AXIS_Z;
    }

    public static int axisMetadata(ForgeDirection direction) {
        if (direction.offsetX != 0) return AXIS_X;
        if (direction.offsetZ != 0) return AXIS_Z;
        return AXIS_Y;
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
        int metadata) {
        return axisMetadata(ForgeDirection.getOrientation(side));
    }

    @Override
    public int getRenderType() {
        // Vanilla's log renderer also rotates side UVs to follow the metadata axis.
        return 31;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        int axis = metadata & 12;
        int bottomSide = axis == AXIS_X ? 4 : axis == AXIS_Z ? 2 : 0;
        int topSide = bottomSide + 1;
        return super.getIcon(side == bottomSide ? 0 : side == topSide ? 1 : 2, 0);
    }
}
