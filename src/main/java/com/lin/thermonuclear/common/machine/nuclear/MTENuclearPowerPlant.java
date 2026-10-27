package com.lin.thermonuclear.common.machine.nuclear;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableMap;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureWalker;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.Thermonuclear;
import com.lin.thermonuclear.api.CoolantType;
import com.lin.thermonuclear.api.FuelRodAdapter;
import com.lin.thermonuclear.client.gui.NuclearPowerPlantGui;
import com.lin.thermonuclear.common.block.BlockFuelRod;
import com.lin.thermonuclear.common.block.BlockMachineComponent;
import com.lin.thermonuclear.common.loader.BlockLoader;
import com.lin.thermonuclear.common.machine.base.CycleContext;
import com.lin.thermonuclear.common.machine.base.TNCCycleMachineBase;
import com.lin.thermonuclear.common.machine.hatch.MTEBreederInputBus;
import com.lin.thermonuclear.common.machine.hatch.MTEBreederOutputBus;
import com.lin.thermonuclear.common.machine.nuclear.NuclearModuleStructure.Position;
import com.lin.thermonuclear.common.nuclear.breeding.BreederPool;
import com.lin.thermonuclear.common.nuclear.fuel.WorkingFuelPool;
import com.lin.thermonuclear.common.nuclear.thermal.NuclearCoolingMath;
import com.lin.thermonuclear.common.nuclear.thermal.NuclearHeatBuffer;
import com.lin.thermonuclear.common.nuclear.thermal.NuclearMeltdownTimer;
import com.lin.thermonuclear.common.registry.DefaultCoolants;
import com.lin.thermonuclear.common.registry.StructureChannels;
import com.lin.thermonuclear.common.registry.WorkingFluids;
import com.lin.thermonuclear.utils.StartupProgress;

import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.IOutputBus;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.ItemEjectionHelper;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.SimpleShutDownReason;
import gregtech.common.blocks.ItemMachines;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.tileentities.machines.IDualInputHatch;
import ic2.core.init.MainConfig;
import ic2.core.util.ConfigUtil;
import tectech.thing.metaTileEntity.hatch.MTEHatchDynamoMulti;

public final class MTENuclearPowerPlant extends TNCCycleMachineBase<MTENuclearPowerPlant> {

    private static final String PIECE = "nuclear_power_plant";
    private static final int OFFSET_X = NuclearPowerPlantStructure.OFFSET_X;
    private static final int OFFSET_Y = NuclearPowerPlantStructure.OFFSET_Y;
    private static final int OFFSET_Z = NuclearPowerPlantStructure.OFFSET_Z;
    private static final Casings CASING = Casings.HeatProofMachineCasing;
    private static final int CONTAINMENT_TEXTURE = 221;
    private static final String[][] SHAPE = NuclearPowerPlantStructure.SHAPE;
    private static final List<int[]> WATER_OFFSETS = structureOffsets('G');
    private static final List<int[]> FUEL_ROD_OFFSETS = structureOffsets('D');
    private static final List<Casings> PIPE_CASINGS = new ArrayList<>(
        Arrays.asList(Casings.SteelPipeCasing, Casings.TitaniumPipeCasing, Casings.TungstensteelPipeCasing));
    private static final IStructureDefinition<MTENuclearPowerPlant> STRUCTURE = StructureDefinition
        .<MTENuclearPowerPlant>builder()
        .addShape(PIECE, SHAPE)
        .addShape(NuclearModuleStructure.MODULE, NuclearModuleStructure.shape())
        .addShape("nuclear_preview_levels", new String[][] { { "X" } })
        .addElement('X', StructureChannels.EXTRA.use(pipeChoices(false)))
        .addElement('P', tieredPipe())
        .addElement('S', GTStructureUtility.ofFrame(Materials.Steel))
        .addElement(
            'T',
            StructureUtility
                .ofBlock(Casings.SolidSteelMachineCasing.getBlock(), Casings.SolidSteelMachineCasing.getBlockMeta()))
        .addElement('C', tieredPipe())
        .addElement('D', tieredFuelRod())
        .addElement('F', StructureUtility.ofBlock(BlockLoader.containmentCasing, 0))
        .addElement('G', StructureUtility.ofChain(GTStructureUtility.ofAnyWater(false), StructureUtility.isAir()))
        .addElement(
            'B',
            GTStructureUtility.buildHatchAdder(MTENuclearPowerPlant.class)
                .atLeast(
                    HatchElement.InputBus,
                    HatchElement.OutputBus,
                    HatchElement.Dynamo.or(HatchElement.ExoticDynamo)
                        .or(HatchElement.LaserSource),
                    HatchElement.Maintenance)
                .adder(MTENuclearPowerPlant::addServiceHatch)
                .hatchItemFilterAnd(machine -> stack -> {
                    IMetaTileEntity hatch = ItemMachines.getMetaTileEntity(stack);
                    // Breeder buses are optional; they cannot satisfy the ordinary fuel IO counts.
                    return !(hatch instanceof IDualInputHatch) && !(hatch instanceof MTEBreederInputBus)
                        && !(hatch instanceof MTEBreederOutputBus);
                })
                .shouldSkip((machine, tile) -> tile != null && isServiceHatchCandidate(tile.getMetaTileEntity()))
                .casingIndex(CONTAINMENT_TEXTURE)
                .hint(1)
                .buildAndChain(BlockLoader.containmentCasing, 0))
        .addElement(
            'A',
            GTStructureUtility.buildHatchAdder(MTENuclearPowerPlant.class)
                .atLeast(HatchElement.InputHatch, HatchElement.OutputHatch)
                .adder(MTENuclearPowerPlant::addCoolantHatch)
                .hatchItemFilterAnd(
                    machine -> stack -> !(ItemMachines.getMetaTileEntity(stack) instanceof IDualInputHatch))
                .casingIndex(CONTAINMENT_TEXTURE)
                .hint(2)
                .exclusive()
                .build())
        .build();

    private int fuelRodTier = -1;
    private int pipeTier = -1;
    private final List<MTEBreederInputBus> breederInputs = new ArrayList<>();
    private final List<MTEBreederOutputBus> breederOutputs = new ArrayList<>();
    private final NuclearExpansionState expansion = new NuclearExpansionState();
    private boolean needsWaterFill;

    private static IStructureElement<MTENuclearPowerPlant> tieredPipe() {
        return StructureChannels.REACTOR_PIPE.use(pipeChoices(true));
    }

    private static Integer pipeBlockTier(Block block, int meta) {
        for (int i = 0; i < PIPE_CASINGS.size(); i++) {
            Casings casing = PIPE_CASINGS.get(i);
            if (block == casing.getBlock() && meta == casing.getBlockMeta()) return i + 1;
        }
        return null;
    }

    private static Integer fuelBlockTier(Block block, int meta) {
        if (meta != BlockMachineComponent.AXIS_Y) return null;
        return block instanceof BlockFuelRod rod ? rod.tier() : null;
    }

    private static IStructureElement<MTENuclearPowerPlant> pipeChoices(boolean setMainTier) {
        return StructureUtility.<MTENuclearPowerPlant, Integer>ofBlocksTiered(
            MTENuclearPowerPlant::pipeBlockTier,
            PIPE_CASINGS.stream()
                .map(casing -> Pair.of(casing.getBlock(), casing.getBlockMeta()))
                .collect(Collectors.toList()),
            -1,
            (machine, tier) -> { if (setMainTier) machine.pipeTier = tier; },
            machine -> machine.pipeTier);
    }

    private static IStructureElement<MTENuclearPowerPlant> tieredFuelRod() {
        return StructureChannels.FUEL_ROD.use(
            StructureUtility.<MTENuclearPowerPlant, Integer>ofBlocksTiered(
                MTENuclearPowerPlant::fuelBlockTier,
                Arrays.stream(BlockLoader.fuelRods())
                    .map(block -> Pair.<Block, Integer>of(block, BlockMachineComponent.AXIS_Y))
                    .collect(Collectors.toList()),
                -1,
                (machine, tier) -> machine.fuelRodTier = tier,
                machine -> machine.fuelRodTier));
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (direction, rotation, flip) -> direction.offsetY == 0 && direction != ForgeDirection.UNKNOWN
            && rotation == Rotation.NORMAL
            && flip == Flip.NONE;
    }

    private int[] worldPosition(Position position) {
        int[] offset = new int[3];
        getExtendedFacing().getWorldOffset(new int[] { position.x, position.y, position.z }, offset);
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        offset[0] += tile.getXCoord();
        offset[1] += tile.getYCoord();
        offset[2] += tile.getZCoord();
        return offset;
    }

    private void scanExpansion(IGregTechTileEntity tile, List<StructureError> errors) {
        // Only after the main check: tiered P/D now compare without invoking setters. Never reset per module.
        if (pipeTier <= 0 || fuelRodTier <= 0) throw new IllegalStateException("Main structure tiers not established");
        NuclearModuleSearch.Result expansionScan = new NuclearModuleSearch(new NuclearModuleSearch.Reader() {

            @Override
            public boolean loaded(Position position) {
                int[] p = worldPosition(position);
                return modulePositionLoaded(tile.getWorld(), p[0], p[1], p[2]);
            }

            @Override
            public boolean matches(Position origin, Runnable beforeRead, Runnable onUnloaded) {
                // iterate() returns void, including on early rejection: retain the outcome explicitly.
                class ModuleWalker implements IStructureWalker<MTENuclearPowerPlant> {

                    boolean matched = true;

                    @Override
                    public boolean visit(IStructureElement<MTENuclearPowerPlant> element, World world, int x, int y,
                        int z, int a, int b, int c) {
                        beforeRead.run();
                        if (!modulePositionLoaded(world, x, y, z)) {
                            onUnloaded.run();
                            matched = false;
                            return false;
                        }
                        matched = element.check(MTENuclearPowerPlant.this, world, x, y, z);
                        return matched;
                    }

                    @Override
                    public boolean blockNotLoaded(IStructureElement<MTENuclearPowerPlant> element, World world, int x,
                        int y, int z, int a, int b, int c) {
                        beforeRead.run();
                        onUnloaded.run();
                        matched = false;
                        return false;
                    }
                }
                ModuleWalker walker = new ModuleWalker();
                // Do not use GT checkPiece(): optional candidates must not update main bounds or publish errors.
                STRUCTURE.iterate(
                    NuclearModuleStructure.MODULE,
                    tile.getWorld(),
                    getExtendedFacing(),
                    tile.getXCoord(),
                    tile.getYCoord(),
                    tile.getZCoord(),
                    -origin.x,
                    -origin.y,
                    -origin.z,
                    walker);
                return walker.matched;
            }
        }, System::nanoTime, NuclearModuleStructure.moduleLimit(pipeTier)).scan();
        if (expansionScan.timedOut) {
            expansion.timeOut();
            errors.add(StructureErrors.of("thermonuclear.structure.nuclear.expansion_timeout"));
            stopExpansion("expansion_timeout");
            return;
        }
        // Match the existing allowed-to-work switch, including an enabled machine waiting for fuel.
        boolean changedWhileEnabled = expansion.accept(expansionScan)
            && (tile.isAllowedToWork() || mMaxProgresstime > 0);
        if (changedWhileEnabled) stopExpansion("expansion_changed");
        markDirty();
    }

    private static boolean modulePositionLoaded(World world, int x, int y, int z) {
        return y >= 0 && y < world.getHeight()
            && world.getChunkProvider()
                .chunkExists(x >> 4, z >> 4);
    }

    private void stopExpansion(String reason) {
        stopMachine(SimpleShutDownReason.ofNormal("thermonuclear.status." + reason));
        checkRecipeResult = SimpleCheckRecipeResult.ofFailurePersistOnShutdown("thermonuclear.status." + reason);
        running = false;
        operatingEUt = fullLoadEUt = fullLoadHeatRate = 0;
        inputLitresPerTick = outputLitresPerTick = stoppedInputRate = stoppedOutputRate = 0;
        startup.clear();
        status = reason;
        getBaseMetaTileEntity().setActive(false);
        // Inventory, heat, fuel wear and breeder transactions belong to their existing persistent pools.
        markDirty();
    }

    @Override
    protected void runMachine(IGregTechTileEntity tile, long tick) {
        if (!expansion.validated()) return;
        super.runMachine(tile, tick);
    }

    private static List<int[]> structureOffsets(char element) {
        List<int[]> offsets = new ArrayList<>();
        for (int z = 0; z < SHAPE.length; z++) {
            for (int y = 0; y < SHAPE[z].length; y++) {
                String row = SHAPE[z][y];
                for (int x = 0; x < row.length(); x++) {
                    if (row.charAt(x) == element) offsets.add(new int[] { x - OFFSET_X, y - OFFSET_Y, z - OFFSET_Z });
                }
            }
        }
        return offsets;
    }

    private static boolean isServiceHatchCandidate(IMetaTileEntity hatch) {
        if (hatch == null || hatch instanceof IDualInputHatch) return false;
        // .205 ExoticDynamo has no candidate classes; preserve already installed multi-amp dynamos.
        return HatchElement.InputBus.matchesHatch(hatch) || HatchElement.OutputBus.matchesHatch(hatch)
            || HatchElement.Dynamo.matchesHatch(hatch)
            || hatch instanceof MTEHatchDynamoMulti
            || HatchElement.LaserSource.matchesHatch(hatch)
            || HatchElement.Maintenance.matchesHatch(hatch);
    }

    public int getFuelRodLimit() {
        return Math.min(getMaxParallel(), getTrueParallel());
    }

    @Override
    public int getMaxParallelRecipes() {
        // The GT input field requires a nonempty range even before the structure tier is known.
        return Math.max(1, getMaxParallel());
    }

    private void resetMachineTier() {
        fuelRodTier = -1;
        pipeTier = -1;
    }

    public int getFuelRodTier() {
        return fuelRodTier;
    }

    public int getMaxParallel() {
        return BlockFuelRod.maxParallel(fuelRodTier);
    }

    public long getHeatCapacity() {
        return BlockFuelRod.heatCapacity(fuelRodTier);
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        breederInputs.clear();
        breederOutputs.clear();
    }

    private boolean addServiceHatch(IGregTechTileEntity tile, int texture) {
        if (tile == null || tile.getMetaTileEntity() == null || tile.getMetaTileEntity() instanceof IDualInputHatch)
            return false;
        if (tile.getMetaTileEntity() instanceof MTEBreederInputBus bus) {
            bus.updateTexture(texture);
            bus.updateCraftingIcon(getMachineCraftingIcon());
            addIfSmartInput(bus);
            return breederInputs.add(bus);
        }
        if (tile.getMetaTileEntity() instanceof MTEBreederOutputBus bus) {
            bus.updateTexture(texture);
            bus.updateCraftingIcon(getMachineCraftingIcon());
            addIfSmartInput(bus);
            return breederOutputs.add(bus);
        }
        return addInputBusToMachineList(tile, texture) || addOutputBusToMachineList(tile, texture)
            || addMaintenanceToMachineList(tile, texture)
            || addDynamoToMachineList(tile, texture)
            || addExoticDynamoToMachineList(tile, texture)
            || addLaserSourceToMachineList(tile, texture);
    }

    private boolean addCoolantHatch(IGregTechTileEntity tile, int texture) {
        if (tile == null || tile.getMetaTileEntity() == null || tile.getMetaTileEntity() instanceof IDualInputHatch)
            return false;
        return addInputHatchToMachineList(tile, texture) || addOutputHatchToMachineList(tile, texture);
    }

    @Override
    public IStructureDefinition<MTENuclearPowerPlant> getStructureDefinition() {
        return STRUCTURE;
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        needsWaterFill = false;
        expansion.invalidate();
        resetMachineTier();
        if (!checkPiece(PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z, errors)) {
            return;
        }
        if (mInputBusses.isEmpty()) {
            errors.add(StructureErrors.missingHatch(HatchElement.InputBus));
        }
        if (mOutputBusses.isEmpty()) errors.add(StructureErrors.missingHatch(HatchElement.OutputBus));
        // 增殖总线可选，但是需要同时存在输入输出总线
        if (breederInputs.isEmpty() != breederOutputs.isEmpty()) {
            errors.add(StructureErrors.of("thermonuclear.structure.nuclear.breeder_io"));
        }
        if (mInputHatches.size() != 1 || mOutputHatches.size() != 1) {
            errors.add(StructureErrors.of("thermonuclear.structure.nuclear.coolant_io"));
        }
        if (errors.isEmpty()) {
            scanExpansion(tile, errors);
            needsWaterFill = errors.isEmpty();
        }
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(PIECE, stack, hintsOnly, OFFSET_X, OFFSET_Y, OFFSET_Z);
        // The channel selects a convenient example layout, not the allowed module connections.
        List<Position> examples = NuclearModuleStructure
            .preview(ChannelDataAccessor.getChannelData(stack, StructureChannels.EXTRA.get()));
        for (Position origin : examples) {
            buildModule(stack, hintsOnly, origin);
        }
    }

    @Override
    public int survivalConstruct(ItemStack stack, int elementBudget, ISurvivalBuildEnvironment env) {
        // Main formation does not imply that the extra channel's example modules have been built.
        int built = survivalBuildPiece(PIECE, stack, OFFSET_X, OFFSET_Y, OFFSET_Z, elementBudget, env, false, true);
        if (built >= 0) return built;
        for (Position origin : NuclearModuleStructure
            .preview(ChannelDataAccessor.getChannelData(stack, StructureChannels.EXTRA.get()))) {
            built = survivalBuildPiece(
                NuclearModuleStructure.MODULE,
                stack,
                -origin.x,
                -origin.y,
                -origin.z,
                elementBudget,
                env,
                false,
                true);
            if (built >= 0) return built;
        }
        return -1;
    }

    private void buildModule(ItemStack stack, boolean hintsOnly, Position origin) {
        buildPiece(NuclearModuleStructure.MODULE, stack, hintsOnly, -origin.x, -origin.y, -origin.z);
    }

    @Override
    protected Casings casing() {
        return CASING;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType(StatCollector.translateToLocal(nameKey()))
            .addMachineType(StatCollector.translateToLocal("thermonuclear.structure.machine"))
            .addMarkdown(
                new ResourceLocation(Thermonuclear.MODID, "nuclear-power-plant"),
                ImmutableMap.<String, Object>builder()
                    .put("parallel_1", NumberFormatUtil.formatNumber(BlockFuelRod.maxParallel(1)))
                    .put("parallel_2", NumberFormatUtil.formatNumber(BlockFuelRod.maxParallel(2)))
                    .put("parallel_3", NumberFormatUtil.formatNumber(BlockFuelRod.maxParallel(3)))
                    .put("parallel_4", NumberFormatUtil.formatNumber(BlockFuelRod.maxParallel(4)))
                    .put("capacity_1", NumberFormatUtil.formatNumber(BlockFuelRod.heatCapacity(1)))
                    .put("capacity_2", NumberFormatUtil.formatNumber(BlockFuelRod.heatCapacity(2)))
                    .put("capacity_3", NumberFormatUtil.formatNumber(BlockFuelRod.heatCapacity(3)))
                    .put("capacity_4", NumberFormatUtil.formatNumber(BlockFuelRod.heatCapacity(4)))
                    .put("meltdown_seconds", decimal(NuclearMeltdownTimer.DELAY_TICKS / 20.0))
                    .put("cycle_ticks", NumberFormatUtil.formatNumber(CYCLE_TICKS))
                    .put("steel_modules", NumberFormatUtil.formatNumber(NuclearModuleStructure.moduleLimit(1)))
                    .put("titanium_modules", NumberFormatUtil.formatNumber(NuclearModuleStructure.moduleLimit(2)))
                    .put("tungstensteel_modules", NumberFormatUtil.formatNumber(NuclearModuleStructure.moduleLimit(3)))
                    .build())
            .beginStructureBlock(
                NuclearPowerPlantStructure.WIDTH,
                NuclearPowerPlantStructure.HEIGHT,
                NuclearPowerPlantStructure.LENGTH,
                true)
            .addController(StatCollector.translateToLocal("thermonuclear.structure.nuclear.controller"))
            .addCasing("52", StatCollector.translateToLocal("thermonuclear.structure.nuclear.fuel_rods"), true)
            .addCasing("1805", StatCollector.translateToLocal("thermonuclear.structure.nuclear.concrete"), false)
            .addCasing("122", StatCollector.translateToLocal("thermonuclear.structure.nuclear.steel_casings"), false)
            .addCasing("353", StatCollector.translateToLocal("thermonuclear.structure.nuclear.water"), false)
            .addCasing("14", StatCollector.translateToLocal("thermonuclear.structure.nuclear.pipe_casings"), false)
            .addCasing("7", StatCollector.translateToLocal("thermonuclear.structure.nuclear.outlet"), false)
            .addCasing("14", StatCollector.translateToLocal("thermonuclear.structure.nuclear.frames"), false)
            .addStructureHint(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.expansion.1"))
            .addStructureHint(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.expansion.2"))
            .addStructureHint(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.expansion.3"))
            .addStructureHint(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.expansion.4"))
            .addStructureHint("thermonuclear.structure.nuclear.blocks.3", 1)
            .addInputBus("1+", StatCollector.translateToLocal("thermonuclear.structure.nuclear.services.location"), 1)
            .addOutputBus("1+", StatCollector.translateToLocal("thermonuclear.structure.nuclear.services.location"), 1)
            .addMiscHatch(
                "0+",
                StatCollector.translateToLocal("thermonuclear.hatch.breeder_input.name"),
                StatCollector.translateToLocal("thermonuclear.structure.nuclear.breeder.location"),
                1)
            .addMiscHatch(
                "0+",
                StatCollector.translateToLocal("thermonuclear.hatch.breeder_output.name"),
                StatCollector.translateToLocal("thermonuclear.structure.nuclear.breeder.location"),
                1)
            .addInputHatch(
                "1",
                StatCollector.translateToLocal("thermonuclear.structure.nuclear.coolant_io.location"),
                2)
            .addOutputHatch(
                "1",
                StatCollector.translateToLocal("thermonuclear.structure.nuclear.coolant_io.location"),
                2)
            .addDynamoHatch(
                "0+",
                StatCollector.translateToLocal("thermonuclear.structure.nuclear.services.location"),
                1)
            .addMaintenanceHatch(
                "0+",
                StatCollector.translateToLocal("thermonuclear.structure.nuclear.services.location"),
                1)
            .addStructureInfo("")
            .addSubChannel(StructureChannels.REACTOR_PIPE)
            .addSubChannel(StructureChannels.FUEL_ROD)
            .addSubChannel(StructureChannels.EXTRA)
            .toolTipFinisher("Thermonuclear Expansion[TNC]");
    }

    private enum CoolingFluid {

        IC2(DefaultCoolants.IC2),
        SUPER(DefaultCoolants.SUPER),
        NAK_COMPOSITE(DefaultCoolants.NAK_COMPOSITE),
        DISTILLED(null);

        private final CoolantType coolant;

        CoolingFluid(CoolantType coolant) {
            this.coolant = coolant;
        }
    }

    private NuclearOperatingMode mode = NuclearOperatingMode.DIRECT_GENERATION;
    private final WorkingFuelPool workingFuels = new WorkingFuelPool();
    private final BreederPool breederPool = new BreederPool();
    private long breederPulses;
    private double distilledSteamRemainder;
    private CoolingFluid selectedCoolingFluid;
    private final NuclearHeatBuffer reactorHeat = new NuclearHeatBuffer();
    private final NuclearMeltdownTimer meltdownTimer = new NuclearMeltdownTimer();
    private double fullLoadHeatRate;
    private double stoppedInputRate;
    private double stoppedOutputRate;

    public MTENuclearPowerPlant(int id, String name, String regional) {
        super(id, name, regional);
    }

    public MTENuclearPowerPlant(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MTENuclearPowerPlant(mName);
    }

    public void requestModeChange() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null || !tile.isServerSide() || tile.isAllowedToWork() || running || mMaxProgresstime > 0) return;
        mode = mode.next();
        // Work fuel, durability and all material fractions belong to the machine, not to its mode.
        markDirty();
    }

    public int getFuelBatchLimit() {
        return expansion.batchLimit();
    }

    public void requestFuelEjection() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null || !tile.isServerSide()
            || !mMachine
            || tile.isAllowedToWork()
            || running
            || mMaxProgresstime > 0
            || isProcessingCycle()
            || workingFuels.workingCount() == 0
            || getOutputBusses().isEmpty()) return;

        ItemEjectionHelper outputs = new ItemEjectionHelper(getOutputBusses(), true);
        List<WorkingFuelPool.Batch> batches = new ArrayList<>();
        List<Integer> amounts = new ArrayList<>();
        for (WorkingFuelPool.Batch batch : workingFuels.batches) {
            if (batch.spent) continue;
            // Exported items cannot represent fractional wear; round it up on the exported copies only.
            int ejected = outputs.ejectStack(batch.output());
            if (ejected <= 0) continue;
            batches.add(batch);
            amounts.add(ejected);
        }
        if (batches.isEmpty()) return;
        outputs.commit();
        for (int i = 0; i < batches.size(); i++) {
            WorkingFuelPool.Batch batch = batches.get(i);
            batch.fuel.stackSize -= amounts.get(i);
            if (batch.fuel.stackSize == 0) workingFuels.batches.remove(batch);
        }
        updateSlots();
        markDirty();
    }

    private int breederOutputStart = -1;

    @Override
    protected void clearCycleOutputRoutes() {
        breederOutputStart = -1;
    }

    @Override
    public boolean addItemOutputs(ItemStack[] outputItems) {
        if (breederOutputStart < 0) return super.addItemOutputs(outputItems);
        boolean complete = true;
        if (breederOutputStart > 0) {
            complete = super.addItemOutputs(java.util.Arrays.copyOfRange(outputItems, 0, breederOutputStart));
        }
        List<IOutputBus> outputs = new ArrayList<>();
        for (MTEBreederOutputBus bus : breederOutputs) if (bus.isValid()) outputs.add(bus);
        // Rebuild the helper against current inventories, never commit a cycle-start reservation.
        ItemEjectionHelper helper = new ItemEjectionHelper(outputs, protectsExcessItem());
        int ejected = helper.ejectItems(
            java.util.Arrays.asList(java.util.Arrays.copyOfRange(outputItems, breederOutputStart, outputItems.length)),
            1);
        helper.commit();
        return complete && ejected == 1;
    }

    private boolean flushDepleted(CycleContext context) {
        ItemEjectionHelper outputs = new ItemEjectionHelper(getOutputBusses(), true);
        List<WorkingFuelPool.Batch> batches = new ArrayList<>();
        List<Integer> amounts = new ArrayList<>();
        for (WorkingFuelPool.Batch batch : workingFuels.batches) {
            if (!batch.spent) continue;
            int ejected = outputs.ejectStack(batch.fuel.copy());
            if (ejected <= 0) continue;
            batches.add(batch);
            amounts.add(ejected);
            ItemStack pending = batch.fuel.copy();
            pending.stackSize = ejected;
            queueItemOutput(context, pending);
        }
        if (batches.isEmpty()) return false;
        context.onCommit(() -> {
            for (int i = 0; i < batches.size(); i++) {
                WorkingFuelPool.Batch batch = batches.get(i);
                batch.fuel.stackSize -= amounts.get(i);
                if (batch.fuel.stackSize == 0) workingFuels.batches.remove(batch);
            }
        });
        return true;
    }

    private void processBreeders(CycleContext context, long pulses) {
        breederPulses = pulses;
        if (pulses <= 0 || breederInputs.isEmpty() || breederOutputs.isEmpty()) return;
        List<ItemStack> inputs = new ArrayList<>();
        for (MTEBreederInputBus bus : breederInputs) {
            if (!bus.isValid()) continue;
            for (int slot = 0; slot < bus.getCircuitSlot(); slot++) inputs.add(bus.getStackInSlot(slot));
        }
        Runnable consumeBreeders = breederPool.prepareLoad(inputs, getFuelRodLimit());
        context.deferInput(() -> {
            consumeBreeders.run();
            for (MTEBreederInputBus bus : breederInputs) {
                if (!bus.isValid()) continue;
                bus.updateSlots();
                bus.markDirty();
            }
        });
        breederPool.advance(pulses);
    }

    private boolean flushBreederProducts(CycleContext context) {
        if (breederPool.completedCount() == 0 || breederOutputs.isEmpty()) return false;
        List<IOutputBus> outputs = new ArrayList<>();
        for (MTEBreederOutputBus bus : breederOutputs) if (bus.isValid()) outputs.add(bus);
        if (outputs.isEmpty()) return false;
        // One helper reserves all products together, including partial output of different rod types.
        ItemEjectionHelper helper = new ItemEjectionHelper(outputs, true);
        List<ItemStack> remaining = breederPool.copyCompleted();
        int ejected = 0;
        List<ItemStack> pending = new ArrayList<>();
        for (ItemStack stack : remaining) {
            ItemStack product = stack.copy();
            int amount = helper.ejectStack(stack);
            if (amount <= 0) continue;
            product.stackSize = amount;
            pending.add(product);
            ejected += amount;
        }
        if (ejected == 0) return false;
        // Ordinary products are queued first; this boundary survives GT's item-array NBT round trip.
        context.onCommit(() -> breederOutputStart = mOutputItems == null ? 0 : mOutputItems.length);
        for (ItemStack product : pending) queueItemOutput(context, product);
        context.onCommit(() -> breederPool.finishOutput(remaining));
        return true;
    }

    @Override
    protected boolean processCycle(CycleContext context) {
        boolean successful = processNuclearCycle(context);
        if (successful) {
            flushDepleted(context);
            flushBreederProducts(context);
        } else {
            workingFuels.discardStaged();
            if ("fuel".equals(status) || "spent_full".equals(status)
                || "fuel_limit".equals(status)
                || "output_full".equals(status)) {
                boolean spentOutput = flushDepleted(context);
                boolean breederOutput = flushBreederProducts(context);
                if (spentOutput || breederOutput) {
                    cycleAdvancesStartup = false;
                    successful = true;
                }
            }
        }
        return successful;
    }

    private boolean processNuclearCycle(CycleContext context) {
        if (getOutputBusses().isEmpty() || (mInputBusses.isEmpty() && mDualInputHatches.isEmpty())) {
            return fail("hatches");
        }
        // 每周期开始前先进行冷却
        long heatBeforeCooling = reactorHeat.stored();
        double remainderBeforeCooling = reactorHeat.remainder();
        if (mode == NuclearOperatingMode.HEAT_SUPPLY && !coolReactor(context)) return false;
        boolean cooled = reactorHeat.stored() < heatBeforeCooling
            || (reactorHeat.stored() == heatBeforeCooling && reactorHeat.remainder() < remainderBeforeCooling);
        workingFuels.finishSpent();
        if (workingFuels.exceeds(getFuelBatchLimit(), getMaxParallel())) {
            return coolingOnly(cooled, "fuel_limit");
        }

        workingFuels.prepareLoad(context.availableItems(), getFuelBatchLimit(), getFuelRodLimit());
        if (workingFuels.workingCount() == 0)
            return coolingOnly(cooled, workingFuels.spentCount() > 0 ? "spent_full" : "fuel");
        double efficiency = switch (mode) {
            case DIRECT_GENERATION -> Config.nuclearPowerPlant.nuclearDirectEfficiency;
            case HEAT_SUPPLY -> Config.nuclearPowerPlant.nuclearHeatEfficiency;
        };
        double ramp = startup.averageNext(Config.nuclearPowerPlant.nuclearStartupTicks, CYCLE_TICKS);
        boolean direct = mode == NuclearOperatingMode.DIRECT_GENERATION;
        if (direct) {
            if (!checkWirelessOwner()) return false;
            if (!isWirelessMode() && dynamoRating() <= 0) return fail("hatches");
        }
        double speed = Config.nuclearPowerPlant.fuelCyclesPerSecond;
        double multiplier = direct ? nuclearEnergyMultiplier() : Config.nuclearPowerPlant.nuclearHeatOutputMultiplier;
        List<WorkingFuelPool.Batch> active = new ArrayList<>();
        List<Double> advances = new ArrayList<>();
        double fullOutput = 0;
        double actualOutput = 0;
        BigDecimal actualHeat = BigDecimal.ZERO;
        long pulses = 0;
        for (WorkingFuelPool.Batch batch : workingFuels.batches) {
            if (batch.spent) continue;
            FuelRodAdapter fuel = batch.adapter;
            if (fuel == null) return coolingOnly(cooled, "invalid_value");
            double cycles = Math.min(batch.remaining(), speed * (direct ? 1 : ramp));
            double perCycle = (direct ? fuel.baseEUt(batch.fuel) : fuel.heatPerCycle(batch.fuel)) * multiplier
                * efficiency
                * batch.fuel.stackSize;
            if (!Double.isFinite(cycles) || cycles <= 0 || !Double.isFinite(perCycle) || perCycle <= 0)
                return coolingOnly(cooled, "invalid_value");
            fullOutput += perCycle * speed;
            actualOutput += perCycle * cycles;
            if (!direct) {
                actualHeat = actualHeat.add(
                    BigDecimal.valueOf(fuel.heatPerCycle(batch.fuel))
                        .multiply(BigDecimal.valueOf(multiplier))
                        .multiply(BigDecimal.valueOf(efficiency))
                        .multiply(BigDecimal.valueOf(batch.fuel.stackSize))
                        .multiply(BigDecimal.valueOf(cycles)));
            }
            long batchPulses = (long) Math.max(0, fuel.cells(batch.fuel)) * batch.fuel.stackSize;
            pulses = Long.MAX_VALUE - pulses < batchPulses ? Long.MAX_VALUE : pulses + batchPulses;
            active.add(batch);
            advances.add(cycles);
        }
        if (!Double.isFinite(fullOutput) || !Double.isFinite(actualOutput)) return coolingOnly(cooled, "invalid_value");
        // Validate every batch before advancing any of them; aggregate outputs never cross-multiply batches.
        if (direct) {
            fullLoadEUt = fullOutput;
            operatingEUt = actualOutput;
        } else {
            fullLoadHeatRate = fullOutput / CYCLE_TICKS;
            reactorHeat.add(actualHeat);
        }
        processBreeders(context, pulses);
        for (int i = 0; i < active.size(); i++) active.get(i)
            .advance(advances.get(i));
        workingFuels.finishSpent();
        context.deferInput(workingFuels::consumeInputs);
        return true;
    }

    private boolean coolingOnly(boolean cooled, String reason) {
        // A cooling-only success must not retain a staged fuel batch without paying for its inputs.
        workingFuels.discardStaged();
        cycleAdvancesStartup = false;
        return cooled || fail(reason);
    }

    private boolean coolReactor(CycleContext context) {
        if (!hasFluidInputs() || mOutputHatches.isEmpty()) return fail("hatches");
        selectCoolingFluid();
        if (selectedCoolingFluid == null || !availableCoolingFluid(selectedCoolingFluid)) return true;
        boolean distilled = selectedCoolingFluid == CoolingFluid.DISTILLED;
        CoolantType coolant = selectedCoolingFluid.coolant;
        Fluid input = distilled ? WorkingFluids.distilledWater : coolant.cold();
        Fluid output = distilled ? WorkingFluids.steam : coolant.hot();
        if (output == null) return fail("fluids_missing");
        double litresPerHeat = distilled ? Config.nuclearPowerPlant.nuclearDistilledWaterPerHeat
            : coolant.coolantPerHeat();
        int limit = NuclearCoolingMath.coolingLimit(
            reactorHeat.stored(),
            reactorHeat.remainder(),
            litresPerHeat,
            available(input),
            distilled ? Config.nuclearPowerPlant.nuclearSteamPerDistilledWater : 0,
            distilledSteamRemainder);
        if (limit == 0) return true;
        // Each probe is a fresh simulation; completion will check current output inventories again.
        int amount = NuclearCoolingMath.acceptedAmount(limit, candidate -> {
            int produced = distilled ? NuclearCoolingMath
                .steamOutput(candidate, Config.nuclearPowerPlant.nuclearSteamPerDistilledWater, distilledSteamRemainder)
                : candidate;
            return produced == 0 || prepareOutputs(new FluidStack(output, produced)) != null;
        });
        if (amount == 0) return fail("output_full");
        int produced = distilled
            ? NuclearCoolingMath
                .steamOutput(amount, Config.nuclearPowerPlant.nuclearSteamPerDistilledWater, distilledSteamRemainder)
            : amount;
        FluidEjectionHelper accepted = produced > 0 ? prepareOutputs(new FluidStack(output, produced)) : null;
        if (produced > 0 && accepted == null) return fail("output_full");
        reactorHeat.remove(amount / litresPerHeat);
        if (distilled) distilledSteamRemainder = NuclearCoolingMath
            .steamRemainder(amount, Config.nuclearPowerPlant.nuclearSteamPerDistilledWater, distilledSteamRemainder);
        context.deferInput(() -> consume(input, amount));
        if (produced > 0) queueFluidOutput(context, new FluidStack(output, produced));
        inputLitresPerTick = amount / (double) CYCLE_TICKS;
        outputLitresPerTick = produced / (double) CYCLE_TICKS;
        return true;
    }

    private boolean processStoppedCoolingCycle(CycleContext context) {
        return coolReactor(context) && inputLitresPerTick > 0;
    }

    private static double nuclearEnergyMultiplier() {
        double ic2Value = 1.0;
        ic2.core.util.Config config = MainConfig.get();
        if (config != null) {
            ic2Value = ConfigUtil.getFloat(config, "balance/energy/generator/nuclear");;
        }
        double outputMultiplier = ic2Value * Config.nuclearPowerPlant.nuclearDirectOutputMultiplier;
        return Double.isFinite(outputMultiplier) && outputMultiplier > 0 ? outputMultiplier : 0;
    }

    private boolean selectCoolingFluid() {
        if (selectedCoolingFluid != null) return availableCoolingFluid(selectedCoolingFluid);
        for (CoolingFluid candidate : CoolingFluid.values()) {
            CoolantType coolant = candidate.coolant;
            if (coolant != null && coolant.hot() != null && coolant.cold() != null && available(coolant.cold()) > 0) {
                selectedCoolingFluid = candidate;
                return true;
            }
        }
        if (WorkingFluids.distilledWater != null && available(WorkingFluids.distilledWater) > 0) {
            selectedCoolingFluid = CoolingFluid.DISTILLED;
            return true;
        }
        return false;
    }

    private boolean availableCoolingFluid(CoolingFluid fluid) {
        Fluid input = fluid == CoolingFluid.DISTILLED ? WorkingFluids.distilledWater : fluid.coolant.cold();
        return input != null && available(input) > 0;
    }

    @Override
    protected void beforeProcessingCycle(CycleContext context) {
        NBTTagCompound snapshot = new NBTTagCompound();
        saveFuelState(snapshot);
        context.onRollback(() -> {
            loadFuelState(snapshot);
            fullLoadHeatRate = 0;
        });
        context.onClose(workingFuels::clearPlans);
        workingFuels.clearPlans();
        fullLoadHeatRate = 0;
        breederPulses = 0;
    }

    private void saveFuelState(NBTTagCompound nbt) {
        nbt.setString("tnNuclearMode", mode.id());
        workingFuels.save(nbt);
        breederPool.save(nbt);
        nbt.setLong("tnBreederPulses", breederPulses);
        nbt.setDouble("tnDistilledSteamRemainder", distilledSteamRemainder);
        nbt.setLong("tnReactorHeat", reactorHeat.stored());
        nbt.setDouble("tnReactorHeatRemainder", reactorHeat.remainder());
        nbt.setString(
            "tnSelectedCoolant",
            selectedCoolingFluid == null ? ""
                : selectedCoolingFluid.name()
                    .toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        expansion.save(nbt);
        saveFuelState(nbt);
        nbt.setInteger("tnOverheatTicks", meltdownTimer.elapsedTicks());
        nbt.setDouble("tnFullLoadHeatRate", fullLoadHeatRate);
        nbt.setInteger("tnBreederOutputStart", breederOutputStart);
    }

    @Override
    public void setItemNBT(NBTTagCompound nbt) {
        super.setItemNBT(nbt);
        // Internal work fuel is not a normal inventory slot: carry it in the controller drop without restoring startup.
        saveFuelState(nbt);
        nbt.setInteger("tnOverheatTicks", meltdownTimer.elapsedTicks());
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        int savedOutputStart = nbt.hasKey("tnBreederOutputStart") ? nbt.getInteger("tnBreederOutputStart") : -1;
        breederOutputStart = mMaxProgresstime > 0 && mOutputItems != null
            && savedOutputStart >= 0
            && savedOutputStart < mOutputItems.length ? savedOutputStart : -1;
        expansion.load(nbt);
        loadFuelState(nbt);
        meltdownTimer.restore(nbt.getInteger("tnOverheatTicks"));
        double savedRate = nbt.getDouble("tnFullLoadHeatRate");
        fullLoadHeatRate = Double.isFinite(savedRate) && savedRate > 0 ? savedRate : 0;
    }

    private void loadFuelState(NBTTagCompound nbt) {
        mode = NuclearOperatingMode.fromId(nbt.getString("tnNuclearMode"));
        workingFuels.load(nbt);
        breederPool.load(nbt);
        breederPulses = Math.max(0, nbt.getLong("tnBreederPulses"));
        selectedCoolingFluid = null;
        String selected = nbt.getString("tnSelectedCoolant");
        if ("ic2".equals(selected)) selectedCoolingFluid = CoolingFluid.IC2;
        if ("super".equals(selected)) selectedCoolingFluid = CoolingFluid.SUPER;
        if ("nak_composite".equals(selected)) selectedCoolingFluid = CoolingFluid.NAK_COMPOSITE;
        if ("distilled".equals(selected)) selectedCoolingFluid = CoolingFluid.DISTILLED;
        distilledSteamRemainder = StartupProgress.fraction(nbt.getDouble("tnDistilledSteamRemainder"));
        // Structure tiers are not known during load or transaction rollback. Preserve heat after a downgrade.
        reactorHeat.restore(nbt.getLong("tnReactorHeat"), nbt.getDouble("tnReactorHeatRemainder"));
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (!tile.isServerSide()) return;
        if (mStartUpCheck >= 0) return;
        if (mMachine && needsWaterFill
            && tick % 20 == 0
            && GTStructureUtility
                .fillStructureWithWater(tile, getExtendedFacing(), SHAPE, OFFSET_X, OFFSET_Y, OFFSET_Z, 'G')) {
            needsWaterFill = false;
        }
        if (!mMachine) {
            stoppedInputRate = stoppedOutputRate = 0;
            if (meltdownTimer.elapsedTicks() != 0) {
                meltdownTimer.reset();
                markDirty();
            }
            if ("thermonuclear.status.meltdown".equals(
                tile.getLastShutDownReason()
                    .getKey()))
                status = "meltdown";
            return;
        }
        if (tile.isAllowedToWork() || running) {
            stoppedInputRate = stoppedOutputRate = 0;
            updateMeltdown(tile);
            return;
        }
        if (reactorHeat.isEmpty()) {
            stoppedInputRate = stoppedOutputRate = 0;
            selectedCoolingFluid = null;
        } else if (tick % CYCLE_TICKS == 0) {
            // A stopped reactor may select whichever supported fluid is currently supplied.
            selectedCoolingFluid = null;
            if (processIdleCycle(this::processStoppedCoolingCycle)) {
                stoppedInputRate = inputLitresPerTick;
                stoppedOutputRate = outputLitresPerTick;
            } else {
                stoppedInputRate = stoppedOutputRate = 0;
                long previousHeat = reactorHeat.stored();
                double previousRemainder = reactorHeat.remainder();
                reactorHeat.remove(Config.nuclearPowerPlant.nuclearPassiveCoolingPerSecond * CYCLE_TICKS / 20.0);
                if (reactorHeat.stored() != previousHeat || reactorHeat.remainder() != previousRemainder) markDirty();
            }
        }
        inputLitresPerTick = stoppedInputRate;
        outputLitresPerTick = stoppedOutputRate;
        status = stoppedInputRate > 0 ? "stopped_cooling"
            : !reactorHeat.isEmpty() && Config.nuclearPowerPlant.nuclearPassiveCoolingPerSecond > 0 ? "stopped_passive"
                : "stopped";
        updateMeltdown(tile);
    }

    private void updateMeltdown(IGregTechTileEntity tile) {
        int previousTicks = meltdownTimer.elapsedTicks();
        boolean meltdown = meltdownTimer.advance(reactorHeat.stored(), reactorHeat.remainder(), getHeatCapacity());
        if (meltdownTimer.elapsedTicks() != previousTicks) markDirty();
        if (meltdownTimer.elapsedTicks() > 0) status = "heat_overflow";
        if (meltdown) {
            meltCore(tile);
        }
    }

    private void meltCore(IGregTechTileEntity tile) {
        World world = tile.getWorld();
        int[] worldOffset = new int[3];
        List<int[]> rods = new ArrayList<>(FUEL_ROD_OFFSETS);
        for (Position origin : expansion.modules()) {
            for (Position local : NuclearModuleStructure.fuelRods()) {
                rods.add(new int[] { origin.x + local.x, origin.y + local.y, origin.z + local.z });
            }
        }
        // Validated modules are not persisted or chunk tickets. Defer all melting if any target is unloaded.
        for (int[] offset : rods) {
            getExtendedFacing().getWorldOffset(offset, worldOffset);
            if (!world.getChunkProvider()
                .chunkExists((tile.getXCoord() + worldOffset[0]) >> 4, (tile.getZCoord() + worldOffset[2]) >> 4))
                return;
        }
        boolean dry = true;
        // Remove every G water block before notifying neighbours or placing any lava, avoiding solidified rods.
        for (int[] offset : WATER_OFFSETS) {
            getExtendedFacing().getWorldOffset(offset, worldOffset);
            int x = tile.getXCoord() + worldOffset[0];
            int y = tile.getYCoord() + worldOffset[1];
            int z = tile.getZCoord() + worldOffset[2];
            if (GTUtility.isWater(world.getBlock(x, y, z))) {
                world.setBlock(x, y, z, Blocks.air, 0, 2);
                if (GTUtility.isWater(world.getBlock(x, y, z))) dry = false;
            }
        }
        // A failed water replacement is retried next tick; never pour lava into a still-wet core.
        if (!dry) return;
        for (int[] offset : rods) {
            getExtendedFacing().getWorldOffset(offset, worldOffset);
            int x = tile.getXCoord() + worldOffset[0];
            int y = tile.getYCoord() + worldOffset[1];
            int z = tile.getZCoord() + worldOffset[2];
            if (world.getBlock(x, y, z) instanceof BlockFuelRod) world.setBlock(x, y, z, Blocks.lava, 0, 3);
        }
        stopMachine(SimpleShutDownReason.ofCritical("thermonuclear.status.meltdown"));
        mMachine = false;
        // Melting removes the structure water deliberately; never refill a destroyed core.
        needsWaterFill = false;
        expansion.invalidate();
        fuelRodTier = -1;
        running = false;
        operatingEUt = fullLoadEUt = fullLoadHeatRate = 0;
        inputLitresPerTick = outputLitresPerTick = stoppedInputRate = stoppedOutputRate = 0;
        startup.clear();
        meltdownTimer.reset();
        status = "meltdown";
        tile.setActive(false);
        markDirty();
    }

    @Override
    protected boolean usesItemBusses() {
        return true;
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new NuclearPowerPlantGui(this);
    }

    @Override
    protected boolean usesDynamo() {
        return true;
    }

    @Override
    protected int startupTicks() {
        return mode == NuclearOperatingMode.DIRECT_GENERATION ? 0 : Config.nuclearPowerPlant.nuclearStartupTicks;
    }

    @Override
    protected int decayTicks() {
        return Config.nuclearPowerPlant.nuclearDecayTicks;
    }

    @Override
    public String nameKey() {
        return "thermonuclear.machine.nuclear.name";
    }

    @Override
    public Map<String, String> displayInfo() {
        Map<String, String> info = commonInfo();
        info.put("mode", mode.translationKey());
        if (expansion.validated()) {
            info.put(
                "expansion_count",
                (expansion.batchLimit() - 1) + " / " + NuclearModuleStructure.moduleLimit(pipeTier));
        }
        if (mMachine && fuelRodTier > 0) {
            info.put("fuel_rod_tier", Integer.toString(getFuelRodTier()));
            info.put("fuel_limit", Integer.toString(getFuelRodLimit()));
        }
        if (mode == NuclearOperatingMode.DIRECT_GENERATION) {
            addGenerationInfo(info);
        } else {
            addStartupInfo(info);
            addHeatSupplyInfo(info);
        }
        info.put("fuel_batches", workingFuels.batches.size() + " / " + getFuelBatchLimit());
        info.put("fuel_count", Integer.toString(workingFuels.workingCount()));
        for (int i = 0; i < workingFuels.batches.size(); i++) {
            WorkingFuelPool.Batch batch = workingFuels.batches.get(i);
            info.put("fuel_batch_" + (i + 1), batch.fuel.stackSize + " x " + batch.fuel.getDisplayName());
            if (!batch.spent) info.put("fuel_batch_remaining_" + (i + 1), decimal(batch.remaining()));
        }
        if (workingFuels.spentCount() > 0) info.put("pending_spent", Integer.toString(workingFuels.spentCount()));
        if (breederPool.workingCount() > 0) {
            info.put("breeder_count", Integer.toString(breederPool.workingCount()));
            info.put("breeder_rate", Long.toString(running ? breederPulses * 4 : 0));
            info.put("breeder_progress", Float.toString(breederPool.getHighestProgress()));
        }
        return info;
    }

    private void addHeatSupplyInfo(Map<String, String> info) {
        info.put("heat", Long.toString(reactorHeat.stored()));
        info.put("heat_capacity", Long.toString(getHeatCapacity()));
        info.put("heat_rate", decimalPerCycle(running ? fullLoadHeatRate : 0));
        info.put(
            "coolant",
            selectedCoolingFluid == null ? "thermonuclear.recipe.none"
                : selectedCoolingFluid == CoolingFluid.DISTILLED ? "thermonuclear.recipe.distilled"
                    : selectedCoolingFluid.coolant.nameKey());
        if (selectedCoolingFluid == null) return;
        boolean water = selectedCoolingFluid == CoolingFluid.DISTILLED;
        double litresPerHeat = water ? Config.nuclearPowerPlant.nuclearDistilledWaterPerHeat
            : selectedCoolingFluid.coolant.coolantPerHeat();
        if (water) {
            info.put("water_limit", decimalPerCycle(fullLoadHeatRate * litresPerHeat));
            info.put("water_input", decimalPerCycle(inputLitresPerTick));
            info.put("steam_output", decimalPerCycle(outputLitresPerTick));
        } else {
            info.put("heat_limit", decimalPerCycle(fullLoadHeatRate * litresPerHeat));
            info.put("coolant_input", decimalPerCycle(inputLitresPerTick));
            info.put("hot_output", decimalPerCycle(outputLitresPerTick));
        }
    }
}
