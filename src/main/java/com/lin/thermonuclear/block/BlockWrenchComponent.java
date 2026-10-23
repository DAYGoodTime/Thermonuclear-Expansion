package com.lin.thermonuclear.block;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import gregtech.api.GregTechAPI;

/** Axial component using GT casing harvesting and the GT wrench's Forge rotation hooks. */
public abstract class BlockWrenchComponent extends BlockMachineComponent {

    /** Same harvest level as GT's BlockCasingsAbstract. */
    private static final int WRENCH_LEVEL = 2;

    protected BlockWrenchComponent(String name, String sideTexture, String topTexture, String bottomTexture) {
        super(name, true, sideTexture, topTexture, bottomTexture);
        setHarvestLevel("wrench", WRENCH_LEVEL);
        GregTechAPI.registerMachineBlock(this, -1);
    }

    @Override
    protected boolean canSilkHarvest() {
        // Axis metadata belongs to the placed block, not to its item variant.
        return false;
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
    public ForgeDirection[] getValidRotations(World world, int x, int y, int z) {
        // Any face can be wrenched; GT resolves the clicked face through GTUtility.determineWrenchingSide.
        return ForgeDirection.VALID_DIRECTIONS;
    }

    @Override
    public boolean rotateBlock(World world, int x, int y, int z, ForgeDirection axis) {
        if (world.isRemote || axis == null || axis == ForgeDirection.UNKNOWN) return false;
        int rotated = axisMetadata(axis);
        if (rotated == world.getBlockMetadata(x, y, z)) return false;
        if (!world.setBlockMetadataWithNotify(x, y, z, rotated, 3)) return false;
        // Metadata-only rotation must also enter GT's native structure-update path.
        GregTechAPI.causeMachineUpdate(world, x, y, z);
        return true;
    }
}
