package com.lin.thermonuclear.machine;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.lin.thermonuclear.block.BlockMachineComponent;
import com.lin.thermonuclear.loader.BlockLoader;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;

/**
 * Debug-only controller for assembling draft structures through the StructureLib hologram projector and survival
 * auto-place. It forms when the skeleton matches but never processes recipes, and the draft accepts no hatches;
 * the current geometry is dev/nuc-child.txt.
 */
public final class MTEDebugStructureBuilder extends MTEEnhancedMultiBlockBase<MTEDebugStructureBuilder>
    implements ISurvivalConstructable {

    private static final String PIECE = "debug_structure_draft";
    private static final Casings CASING = Casings.SolidSteelMachineCasing;
    private static final int OFFSET_X = DebugStructureBuilderShape.OFFSET_X;
    private static final int OFFSET_Y = DebugStructureBuilderShape.OFFSET_Y;
    private static final int OFFSET_Z = DebugStructureBuilderShape.OFFSET_Z;
    private static final IStructureDefinition<MTEDebugStructureBuilder> STRUCTURE = StructureDefinition
        .<MTEDebugStructureBuilder>builder()
        .addShape(PIECE, DebugStructureBuilderShape.SHAPE)
        .addElement('A', Casings.SteelPipeCasing.asElement())
        .addElement('B', StructureUtility.ofBlock(BlockLoader.containmentCasing, 0))
        .addElement('C', StructureUtility.ofBlock(BlockLoader.fuelRod(1), BlockMachineComponent.AXIS_Y))
        .build();

    public MTEDebugStructureBuilder(int id, String name, String regional) {
        super(id, name, regional);
    }

    public MTEDebugStructureBuilder(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MTEDebugStructureBuilder(mName);
    }

    @Override
    public IStructureDefinition<MTEDebugStructureBuilder> getStructureDefinition() {
        return STRUCTURE;
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        checkPiece(PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z, errors);
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(PIECE, stack, hintsOnly, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        return survivalBuildPiece(PIECE, stackSize, OFFSET_X, OFFSET_Y, OFFSET_Z, elementBudget, env, false, true);
    }

    @Override
    @Nonnull
    public CheckRecipeResult checkProcessing() {
        // Template host only; report "no recipe" instead of driving any processing logic.
        return CheckRecipeResultRegistry.NO_RECIPE;
    }

    @Override
    public boolean shouldCheckMaintenance() {
        return false;
    }

    @Override
    public int getRepairStatus() {
        return getIdealStatus();
    }

    @Override
    public void checkMaintenance() {
        fixAllIssues();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = CASING.getCasingTexture();
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(
                active ? Textures.BlockIcons.OVERLAY_FRONT_LARGE_BOILER_ACTIVE
                    : Textures.BlockIcons.OVERLAY_FRONT_LARGE_BOILER)
            .extFacing()
            .build() };
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal("thermonuclear.machine.debug_structure.name");
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder()
            .addMachineType(StatCollector.translateToLocal("thermonuclear.machine.debug_structure.name"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.debug_structure.0"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.debug_structure.1"))
            .addSeparator()
            .beginStructureBlock(
                DebugStructureBuilderShape.WIDTH,
                DebugStructureBuilderShape.HEIGHT,
                DebugStructureBuilderShape.LENGTH,
                true)
            .addController(StatCollector.translateToLocal("thermonuclear.structure.debug_structure.controller"))
            .addCasing(
                Integer.toString(DebugStructureBuilderShape.count('B')),
                StatCollector.translateToLocal("thermonuclear.structure.debug_structure.casings"),
                false)
            .addCasing(
                Integer.toString(DebugStructureBuilderShape.count('A')),
                StatCollector.translateToLocal("thermonuclear.structure.debug_structure.pipes"),
                false)
            .addCasing(
                Integer.toString(DebugStructureBuilderShape.count('C')),
                StatCollector.translateToLocal("thermonuclear.structure.debug_structure.fuel_rods"),
                false)
            .toolTipFinisher();
    }
}
