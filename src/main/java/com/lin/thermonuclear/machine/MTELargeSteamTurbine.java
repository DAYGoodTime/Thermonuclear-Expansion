package com.lin.thermonuclear.machine;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.tuple.Pair;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.block.BlockAxialMachineComponent;
import com.lin.thermonuclear.gui.LargeSteamTurbineGui;
import com.lin.thermonuclear.loader.BlockLoader;
import com.lin.thermonuclear.recipe.ProcessingCycleMath;
import com.lin.thermonuclear.recipe.SteamTurbineFuel;
import com.lin.thermonuclear.recipe.SteamTurbineMath;
import com.lin.thermonuclear.recipe.SteamTurbineRecipes;
import com.lin.thermonuclear.registry.WorkingFluids;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.INEIPreviewModifier;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.SimpleShutDownReason;
import gregtech.common.blocks.ItemMachines;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public final class MTELargeSteamTurbine extends ThermonuclearMultiblockBase<MTELargeSteamTurbine>
    implements INEIPreviewModifier {

    private static final String PIECE = "large_steam_turbine";
    private static final int OFFSET_X = SteamTurbineStructure.OFFSET_X;
    private static final int OFFSET_Y = SteamTurbineStructure.OFFSET_Y;
    private static final int OFFSET_Z = SteamTurbineStructure.OFFSET_Z;
    private static final Casings CASING = Casings.SolidSteelMachineCasing;
    private static final IStructureDefinition<MTELargeSteamTurbine> STRUCTURE = StructureDefinition
        .<MTELargeSteamTurbine>builder()
        .addShape(PIECE, SteamTurbineStructure.createShape())
        .addElement('A', fluidHatch(false, 1))
        .addElement('B', fluidHatch(true, 2))
        .addElement(
            'C',
            GTStructureUtility.buildHatchAdder(MTELargeSteamTurbine.class)
                .anyOf(HatchElement.Dynamo, HatchElement.ExoticDynamo, HatchElement.LaserSource)
                .casingIndex(CASING.textureId)
                .hint(3)
                .buildAndChain(CASING.asElement()))
        .addElement('D', Casings.SteelGearBoxCasing.asElement())
        .addElement('E', Casings.SteelPipeCasing.asElement())
        .addElement('F', StructureUtility.ofBlock(GregTechAPI.sBlockMetal6, 13))
        .addElement(
            'G',
            StructureUtility.withChannel(
                "turbine_shaft",
                StructureUtility.defer(
                    (MTELargeSteamTurbine machine) -> shaftElement(
                        BlockAxialMachineComponent.axisMetadata(
                            machine.getExtendedFacing()
                                .getRelativeLeftInWorld())))))
        .build();

    private int shaftTier;
    private boolean constructing;
    private boolean previewConstruction;
    private EntityPlayer previewPlayer;

    private static IStructureElement<MTELargeSteamTurbine> shaftElement(int axisMetadata) {
        // The 26 G positions run along local X after rotating the exported structure.
        return StructureUtility.<MTELargeSteamTurbine, Integer>ofBlocksTiered((block, meta) -> {
            if (meta != axisMetadata) return null;
            if (block == BlockLoader.lowPressureTurbineShaft) return 1;
            if (block == BlockLoader.highPressureTurbineShaft) return 2;
            return null;
        },
            Arrays.asList(
                Pair.of(BlockLoader.lowPressureTurbineShaft, axisMetadata),
                Pair.of(BlockLoader.highPressureTurbineShaft, axisMetadata)),
            0,
            (machine, tier) -> machine.shaftTier = tier,
            machine -> machine.shaftTier);
    }

    private static IStructureElement<MTELargeSteamTurbine> fluidHatch(boolean input, int hint) {
        return StructureUtility.ofChain(
            GTStructureUtility.buildHatchAdder(MTELargeSteamTurbine.class)
                .anyOf(input ? HatchElement.InputHatch : HatchElement.OutputHatch)
                .casingIndex(CASING.textureId)
                .hint(hint)
                .build(),
            hatchPreviewPlacement(input));
    }

    private static IStructureElement<MTELargeSteamTurbine> hatchPreviewPlacement(boolean input) {
        return new IStructureElement<>() {

            @Override
            public boolean check(MTELargeSteamTurbine machine, World world, int x, int y, int z) {
                return false;
            }

            @Override
            public boolean spawnHint(MTELargeSteamTurbine machine, World world, int x, int y, int z,
                ItemStack trigger) {
                return false;
            }

            @Override
            public boolean placeBlock(MTELargeSteamTurbine machine, World world, int x, int y, int z,
                ItemStack trigger) {
                EntityPlayer player = machine.previewPlayer;
                // NEI's DummyWorld cannot use GT's WorldServer fake-player hatch placer.
                if (!machine.previewConstruction || !machine.constructing
                    || player == null
                    || player.getUniqueID() == null
                    || world instanceof WorldServer
                    || !world.isAirBlock(x, y, z)) return false;
                ItemStack representative = (input ? ItemList.Hatch_Input_ULV : ItemList.Hatch_Output_ULV).get(1);
                if (!(representative.getItem() instanceof ItemMachines item) || !item.placeBlockAt(
                    representative,
                    player,
                    world,
                    x,
                    y,
                    z,
                    ForgeDirection.UP.ordinal(),
                    0.5f,
                    0.5f,
                    0.5f,
                    0)) return false;
                if (world.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile) {
                    // The inlet is at local -X, the outlet at +X after the structure rotation.
                    tile.setFrontFacing(
                        input ? machine.getExtendedFacing()
                            .getRelativeLeftInWorld()
                            : machine.getExtendedFacing()
                                .getRelativeRightInWorld());
                    if (tile.getMetaTileEntity() instanceof MTEHatch hatch) hatch.updateTexture(CASING.textureId);
                }
                return true;
            }
        };
    }

    public int getShaftTier() {
        return shaftTier;
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        shaftTier = 0;
    }

    @Override
    public IStructureDefinition<MTELargeSteamTurbine> getStructureDefinition() {
        return STRUCTURE;
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        shaftTier = 0;
        if (!checkPiece(PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z, errors)) {
            shaftTier = 0;
            return;
        }
        if (!hasFluidInputs()) errors.add(StructureErrors.missingHatch(HatchElement.InputHatch));
        if (mOutputHatches.isEmpty()) errors.add(StructureErrors.missingHatch(HatchElement.OutputHatch));
        if (mDynamoHatches.isEmpty() && mExoticDynamoHatches.isEmpty()) {
            errors.add(StructureErrors.missingHatch(HatchElement.Dynamo));
        }
        if (!errors.isEmpty()) shaftTier = 0;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        constructing = true;
        try {
            buildPiece(PIECE, stack, hintsOnly, OFFSET_X, OFFSET_Y, OFFSET_Z);
        } finally {
            constructing = false;
            previewConstruction = false;
            previewPlayer = null;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onPreviewConstruct(ItemStack trigger) {
        previewPlayer = Minecraft.getMinecraft().thePlayer;
        previewConstruction = previewPlayer != null && previewPlayer.getUniqueID() != null;
        if (!previewConstruction) previewPlayer = null;
    }

    @Override
    protected Casings casing() {
        return CASING;
    }

    @Override
    protected int structureChunkRadius() {
        return SteamTurbineStructure.CHUNK_RADIUS;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return machineTooltip()
            .beginStructureBlock(
                SteamTurbineStructure.WIDTH,
                SteamTurbineStructure.HEIGHT,
                SteamTurbineStructure.LENGTH,
                true)
            .addController(StatCollector.translateToLocal("thermonuclear.structure.turbine.controller"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.turbine.blocks"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.turbine.shafts"))
            .addStructureHint("thermonuclear.structure.turbine.output", 1)
            .addStructureHint("thermonuclear.structure.turbine.input", 2)
            .addStructureHint("thermonuclear.structure.turbine.dynamo", 3)
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.hatches"))
            .addInputHatch("1", "B", 2)
            .addOutputHatch("1", "A", 1)
            .addDynamoHatch("1+", "C", 3)
            .toolTipFinisher();
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new LargeSteamTurbineGui(this);
    }

    private int condensationRemainderLitres;
    private long effectiveSteamLimitLitresPerCycle;
    private long steamLimitLitresPerCycle = Long.MAX_VALUE;
    private SteamTurbineFuel currentFuel;

    public MTELargeSteamTurbine(int id, String name, String regional) {
        super(id, name, regional);
    }

    public MTELargeSteamTurbine(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MTELargeSteamTurbine(mName);
    }

    @Override
    protected boolean processCycle() {
        currentFuel = null;
        if (WorkingFluids.distilledWater == null) return fail("fluids_missing");
        if (steamLimitLitresPerCycle == 0) return fail("steam_limit_zero");
        boolean unsupportedSteam = false;
        for (SteamTurbineFuel fuel : SteamTurbineFuel.values()) {
            if (fuel.fluid() != null && available(fuel.fluid()) > 0) {
                if (!fuel.supportsShaftTier(shaftTier)) {
                    unsupportedSteam = true;
                    continue;
                }
                currentFuel = fuel;
                break;
            }
        }
        if (currentFuel == null) return fail(unsupportedSteam ? "shaft_steam_unsupported" : "steam");
        double effectiveEUPerLitre = currentFuel.euPerLitre();
        effectiveSteamLimitLitresPerCycle = SteamTurbineMath
            .steamLimitLitresPerCycle(steamLimitLitresPerCycle, GTValues.STEAM_PER_WATER, condensationRemainderLitres);
        double fullLoadSteamLitresPerTick = ProcessingCycleMath
            .litresPerTick(effectiveSteamLimitLitresPerCycle, CYCLE_TICKS);
        fullLoadEUt = SteamTurbineMath.generationEUt(fullLoadSteamLitresPerTick, effectiveEUPerLitre);
        long steamLitresPerCycle = Math.min(effectiveSteamLimitLitresPerCycle, available(currentFuel.fluid()));
        if (steamLitresPerCycle <= 0) return fail("steam");
        int waterLitresPerCycle = (int) ((steamLitresPerCycle + condensationRemainderLitres)
            / GTValues.STEAM_PER_WATER);
        // Reserve a water path even before the first integer litre accumulates.
        FluidEjectionHelper outputs = prepareOutputs(
            new FluidStack(WorkingFluids.distilledWater, Math.max(1, waterLitresPerCycle)));
        if (outputs == null) return fail("output_full");
        if (waterLitresPerCycle == 0) outputs = null;
        consume(currentFuel.fluid(), steamLitresPerCycle);
        if (outputs != null) commitOutput(outputs::commit);
        final int remainingSteamLitres = (int) ((steamLitresPerCycle + condensationRemainderLitres)
            % GTValues.STEAM_PER_WATER);
        commitOutput(() -> condensationRemainderLitres = remainingSteamLitres);
        // Integer batches stay at the fluid transaction boundary; all operating rates use ticks.
        inputLitresPerTick = ProcessingCycleMath.litresPerTick(steamLitresPerCycle, CYCLE_TICKS);
        outputLitresPerTick = ProcessingCycleMath.litresPerTick(waterLitresPerCycle, CYCLE_TICKS);
        operatingEUt = SteamTurbineMath.generationEUt(inputLitresPerTick, effectiveEUPerLitre);
        return true;
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        // A structure downgrade must not resume prepaid ultra-supercritical generation on low-pressure shafts.
        if (currentFuel != null && !currentFuel.supportsShaftTier(shaftTier)) {
            stopMachine(SimpleShutDownReason.ofCritical("thermonuclear.status.shaft_steam_unsupported"));
            return fail("shaft_steam_unsupported");
        }
        return super.onRunningTick(stack);
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setInteger("tnCondensationRemainder", condensationRemainderLitres);
        nbt.setLong("tnSteamLimitPerCycle", steamLimitLitresPerCycle);
        nbt.setLong("tnTurbineCycleLimit", effectiveSteamLimitLitresPerCycle);
        if (currentFuel != null) nbt.setString("tnTurbineFuel", currentFuel.name());
        else nbt.removeTag("tnTurbineFuel");
        nbt.removeTag("tnRotorIdentity");
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        condensationRemainderLitres = Math.max(0, nbt.getInteger("tnCondensationRemainder")) % GTValues.STEAM_PER_WATER;
        steamLimitLitresPerCycle = nbt.hasKey("tnSteamLimitPerCycle") ? Math.max(0, nbt.getLong("tnSteamLimitPerCycle"))
            : Long.MAX_VALUE;
        effectiveSteamLimitLitresPerCycle = SteamTurbineMath.steamLimitLitresPerCycle(
            nbt.getLong("tnTurbineCycleLimit"),
            GTValues.STEAM_PER_WATER,
            condensationRemainderLitres);
        currentFuel = null;
        for (SteamTurbineFuel fuel : SteamTurbineFuel.values()) {
            if (fuel.name()
                .equals(nbt.getString("tnTurbineFuel"))) currentFuel = fuel;
        }
    }

    @Override
    public void setItemNBT(NBTTagCompound nbt) {
        super.setItemNBT(nbt);
        // Preserve already-consumed steam's sub-litre water entitlement, but never startup or power.
        nbt.setInteger("tnCondensationRemainder", condensationRemainderLitres);
        nbt.setLong("tnSteamLimitPerCycle", steamLimitLitresPerCycle);
    }

    public long getSteamLimitLitresPerCycle() {
        return steamLimitLitresPerCycle;
    }

    public void setSteamLimitFromGui(String text) {
        if (getBaseMetaTileEntity() == null || !getBaseMetaTileEntity().isServerSide()) return;
        long limitLitresPerCycle = SteamTurbineMath.parseLimit(text, steamLimitLitresPerCycle);
        if (limitLitresPerCycle != steamLimitLitresPerCycle) {
            // Already-prepaid generation is unchanged; the new cap starts with the next cycle.
            steamLimitLitresPerCycle = limitLitresPerCycle;
            markDirty();
        }
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return SteamTurbineRecipes.DISPLAY;
    }

    @Override
    protected boolean usesItemBusses() {
        return false;
    }

    @Override
    protected boolean usesDynamo() {
        return true;
    }

    @Override
    protected int startupTicks() {
        return Config.largeSteamTurbine.turbineStartupTicks;
    }

    @Override
    protected int decayTicks() {
        return Config.largeSteamTurbine.turbineDecayTicks;
    }

    @Override
    public String nameKey() {
        return "thermonuclear.machine.steam_turbine.name";
    }

    @Override
    protected String machineKind() {
        return "steam_turbine";
    }

    @Override
    public String[] displayKeys() {
        return new String[] { "status", "shaft_tier", "startup", "full_load", "produced", "turbine_steam_type",
            "steam_cycle_limit", "steam_input", "water_output" };
    }

    @Override
    public Map<String, String> displayInfo() {
        Map<String, String> info = commonInfo();
        info.put("shaft_tier", Integer.toString(getShaftTier()));
        addStartupInfo(info);
        addGenerationInfo(info);
        info.put("turbine_steam_type", currentFuel == null ? "thermonuclear.recipe.none" : currentFuel.nameKey());
        info.put("steam_cycle_limit", Long.toString(steamLimitLitresPerCycle));
        info.put("steam_input", decimal(inputLitresPerTick));
        info.put("water_output", decimal(outputLitresPerTick));
        return info;
    }
}
