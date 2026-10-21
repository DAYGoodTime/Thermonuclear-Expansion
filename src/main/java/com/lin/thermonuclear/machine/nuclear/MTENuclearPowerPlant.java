package com.lin.thermonuclear.machine.nuclear;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.tuple.Pair;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureWalker;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.api.CoolantType;
import com.lin.thermonuclear.api.FuelRodAdapter;
import com.lin.thermonuclear.api.FuelRodAdapters;
import com.lin.thermonuclear.block.BlockFuelRod;
import com.lin.thermonuclear.block.BlockMachineComponent;
import com.lin.thermonuclear.gui.NuclearPowerPlantGui;
import com.lin.thermonuclear.loader.BlockLoader;
import com.lin.thermonuclear.machine.base.ThermonuclearMultiblockBase;
import com.lin.thermonuclear.machine.hatch.MTEBreederInputBus;
import com.lin.thermonuclear.machine.hatch.MTEBreederOutputBus;
import com.lin.thermonuclear.machine.nuclear.NuclearModuleStructure.Position;
import com.lin.thermonuclear.registry.DefaultCoolants;
import com.lin.thermonuclear.registry.WorkingFluids;
import com.lin.thermonuclear.utils.StartupProgress;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.INEIPreviewModifier;
import gregtech.api.interfaces.IOutputBus;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
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

public final class MTENuclearPowerPlant extends ThermonuclearMultiblockBase<MTENuclearPowerPlant>
    implements INEIPreviewModifier {

    private static final String PIECE = "nuclear_power_plant";
    private static final int OFFSET_X = NuclearPowerPlantStructure.OFFSET_X;
    private static final int OFFSET_Y = NuclearPowerPlantStructure.OFFSET_Y;
    private static final int OFFSET_Z = NuclearPowerPlantStructure.OFFSET_Z;
    private static final Casings CASING = Casings.HeatProofMachineCasing;
    // The containment casing currently reuses GT's reinforced-concrete texture.
    private static final int CONTAINMENT_TEXTURE = 221;
    private static final String[][] SHAPE = NuclearPowerPlantStructure.SHAPE;
    private static final List<int[]> COOLANT_OFFSETS = structureOffsets('A');
    private static final List<int[]> WATER_OFFSETS = structureOffsets('G');
    private static final List<int[]> FUEL_ROD_OFFSETS = structureOffsets('D');
    private static final List<int[]> SERVICE_OFFSETS = structureOffsets('B');
    private static final Set<Position> MAIN_BLOCKS = mainBlocks();
    private static final List<Casings> PIPE_CASINGS = new ArrayList<>(
        Arrays.asList(Casings.SteelPipeCasing, Casings.TitaniumPipeCasing, Casings.TungstensteelPipeCasing));
    private static final IStructureDefinition<MTENuclearPowerPlant> STRUCTURE = StructureDefinition
        .<MTENuclearPowerPlant>builder()
        .addShape(PIECE, SHAPE)
        .addShape(NuclearModuleStructure.MODULE, NuclearModuleStructure.shape())
        // BR6343 discovers named sliders by enumerating all definition elements, not construct() branches.
        // This metadata-only fragment exposes exactly three extra levels; it is never checked or built.
        .addShape("nuclear_preview_levels", new String[][] { { "X" } })
        .addElement('X', StructureUtility.withChannel("extra", pipeChoices(false)))
        .addElement('P', tieredPipe())
        .addElement('S', GTStructureUtility.ofFrame(Materials.Steel))
        .addElement(
            'T',
            StructureUtility
                .ofBlock(Casings.SolidSteelMachineCasing.getBlock(), Casings.SolidSteelMachineCasing.getBlockMeta()))
        .addElement('C', tieredPipe())
        .addElement('D', tieredFuelRod())
        .addElement('F', StructureUtility.ofBlock(BlockLoader.containmentCasing, 0))
        .addElement('G', GTStructureUtility.ofAnyWater())
        .addElement(
            'B',
            StructureUtility.ofChain(
                servicePreviewPlacement(),
                GTStructureUtility.buildHatchAdder(MTENuclearPowerPlant.class)
                    .anyOf(
                        HatchElement.InputBus,
                        HatchElement.OutputBus,
                        HatchElement.Dynamo,
                        HatchElement.ExoticDynamo,
                        HatchElement.LaserSource,
                        HatchElement.Maintenance)
                    .adder(MTENuclearPowerPlant::addServiceHatch)
                    .hatchItemFilter(machine -> stack -> isServiceHatchCandidate(ItemMachines.getMetaTileEntity(stack)))
                    .shouldSkip((machine, tile) -> tile != null && isServiceHatchCandidate(tile.getMetaTileEntity()))
                    .casingIndex(CONTAINMENT_TEXTURE)
                    .hint(1)
                    .buildAndChain(BlockLoader.containmentCasing, 0)))
        .addElement(
            'A',
            StructureUtility.ofChain(
                GTStructureUtility.buildHatchAdder(MTENuclearPowerPlant.class)
                    .anyOf(HatchElement.InputHatch, HatchElement.OutputHatch)
                    .adder(MTENuclearPowerPlant::addCoolantHatch)
                    .hatchItemFilterAnd(
                        machine -> stack -> !(ItemMachines.getMetaTileEntity(stack) instanceof IDualInputHatch)
                            && machine.isCoolantConstructionCandidate(stack))
                    .casingIndex(CONTAINMENT_TEXTURE)
                    .hint(2)
                    .exclusive()
                    .build(),
                coolantPreviewPlacement()))
        .build();

    private int fuelRodTier = -1;
    private int pipeTier = -1;
    private final List<MTEBreederInputBus> breederInputs = new ArrayList<>();
    private final List<MTEBreederOutputBus> breederOutputs = new ArrayList<>();
    private boolean constructing;
    private boolean previewConstruction;
    private EntityPlayer previewPlayer;
    private int expansionLevel = 1;
    private boolean expansionKnown;
    private boolean expansionValidated;
    private String expansionState = "checking";
    private NuclearModuleSearch.Result expansionScan;
    private Set<Position> validatedModules = java.util.Collections.emptySet();
    private ExtendedFacing mainChunkFacing;
    private int mainChunkX, mainChunkY, mainChunkZ;
    private boolean mainFitsWorld;
    private final Set<Long> mainChunks = new HashSet<>();

    private static IStructureElement<MTENuclearPowerPlant> tieredPipe() {
        return StructureUtility.withChannel("pipe", pipeChoices(true));
    }

    private static Integer pipeBlockTier(Block block, int meta) {
        for (int i = 0; i < PIPE_CASINGS.size(); i++) {
            Casings casing = PIPE_CASINGS.get(i);
            if (block == casing.getBlock() && meta == casing.getBlockMeta()) return i + 1;
        }
        return null;
    }

    private static Integer fuelBlockTier(Block block, int meta) {
        if (!BlockMachineComponent.isAxisMetadata(meta)) return null;
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
        return StructureUtility.withChannel(
            "fuel_rod",
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

    private static Set<Position> mainBlocks() {
        Set<Position> blocks = new HashSet<>();
        for (int z = 0; z < SHAPE.length; z++) {
            for (int y = 0; y < SHAPE[z].length; y++) {
                for (int x = 0; x < SHAPE[z][y].length(); x++) {
                    if (SHAPE[z][y].charAt(x) != ' ')
                        blocks.add(new Position(x - OFFSET_X, y - OFFSET_Y, z - OFFSET_Z));
                }
            }
        }
        return blocks;
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

    @Override
    protected boolean requiredStructureChunksLoaded(IGregTechTileEntity tile) {
        // Only A can suspend the entire machine. Remote expansion chunks are handled as absent edges/bodies.
        if (mainChunkFacing != getExtendedFacing() || mainChunkX != tile.getXCoord()
            || mainChunkY != tile.getYCoord()
            || mainChunkZ != tile.getZCoord()) {
            mainChunkFacing = getExtendedFacing();
            mainChunkX = tile.getXCoord();
            mainChunkY = tile.getYCoord();
            mainChunkZ = tile.getZCoord();
            mainChunks.clear();
            mainFitsWorld = true;
            for (Position position : MAIN_BLOCKS) {
                int[] world = worldPosition(position);
                mainFitsWorld &= world[1] >= 0 && world[1] < tile.getWorld()
                    .getHeight();
                mainChunks.add(((long) (world[0] >> 4) << 32) ^ ((world[2] >> 4) & 0xffffffffL));
            }
        }
        for (long chunk : mainChunks) {
            if (!tile.getWorld()
                .getChunkProvider()
                .chunkExists((int) (chunk >> 32), (int) chunk)) {
                expansionValidated = false;
                expansionState = "chunk_unloaded";
                // Revalidate before any prepaid cycle resumes after A's chunks return.
                mUpdate = 1;
                return false;
            }
        }
        return true;
    }

    private void scanExpansion(IGregTechTileEntity tile, List<StructureError> errors) {
        // Only after the main check: tiered P/D now compare without invoking setters. Never reset per module.
        if (pipeTier <= 0 || fuelRodTier <= 0) throw new IllegalStateException("Main structure tiers not established");
        expansionScan = new NuclearModuleSearch(new NuclearModuleSearch.Reader() {

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
            expansionState = "expansion_timeout";
            errors.add(StructureErrors.of("thermonuclear.structure.nuclear.expansion_timeout"));
            stopExpansion("expansion_timeout");
            return;
        }
        int next = 1 + expansionScan.modules;
        // Match the existing allowed-to-work switch, including an enabled machine waiting for fuel.
        boolean changedWhileEnabled = expansionKnown && expansionLevel != next
            && (tile.isAllowedToWork() || mMaxProgresstime > 0);
        expansionLevel = next;
        validatedModules = new HashSet<>(expansionScan.valid);
        expansionKnown = expansionValidated = true;
        expansionState = expansionScan.capped ? "expansion_capped" : "expansion_valid";
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
        if (!expansionValidated) return;
        super.runMachine(tile, tick);
    }

    private static IStructureElement<MTENuclearPowerPlant> servicePreviewPlacement() {
        return new IStructureElement<>() {

            @Override
            public boolean check(MTENuclearPowerPlant machine, World world, int x, int y, int z) {
                return false;
            }

            @Override
            public boolean spawnHint(MTENuclearPowerPlant machine, World world, int x, int y, int z,
                ItemStack trigger) {
                return false;
            }

            @Override
            public boolean placeBlock(MTENuclearPowerPlant machine, World world, int x, int y, int z,
                ItemStack trigger) {
                if (!machine.previewConstruction || !machine.constructing
                    || machine.previewPlayer == null
                    || world instanceof WorldServer
                    || !world.isAirBlock(x, y, z)) return false;
                // Fixed minimum legal fuel IO + dynamo; remaining service slots use containment casings.
                ItemList[] hatches = { ItemList.Hatch_Input_Bus_ULV, ItemList.Hatch_Output_Bus_ULV,
                    ItemList.Hatch_Dynamo_ULV };
                for (int i = 0; i < hatches.length; i++) {
                    int[] offset = SERVICE_OFFSETS.get(i);
                    int[] p = machine.worldPosition(new Position(offset[0], offset[1], offset[2]));
                    if (p[0] == x && p[1] == y && p[2] == z)
                        return machine.placePreviewHatch(hatches[i].get(1), world, x, y, z);
                }
                return world.setBlock(x, y, z, BlockLoader.containmentCasing, 0, 2);
            }
        };
    }

    private boolean placePreviewHatch(ItemStack representative, World world, int x, int y, int z) {
        if (!(representative.getItem() instanceof ItemMachines item) || !item.placeBlockAt(
            representative,
            previewPlayer,
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
            tile.setFrontFacing(getExtendedFacing().getRelativeForwardInWorld());
            if (tile.getMetaTileEntity() instanceof MTEHatch hatch) hatch.updateTexture(CONTAINMENT_TEXTURE);
        }
        return true;
    }

    private static IStructureElement<MTENuclearPowerPlant> coolantPreviewPlacement() {
        return new IStructureElement<>() {

            @Override
            public boolean check(MTENuclearPowerPlant machine, World world, int x, int y, int z) {
                // A rendering fallback must never introduce an alternative valid structure block.
                return false;
            }

            @Override
            public boolean spawnHint(MTENuclearPowerPlant machine, World world, int x, int y, int z,
                ItemStack trigger) {
                return false;
            }

            @Override
            public boolean placeBlock(MTENuclearPowerPlant machine, World world, int x, int y, int z,
                ItemStack trigger) {
                if (!machine.previewConstruction || !machine.constructing
                    || machine.previewPlayer == null
                    || machine.previewPlayer.getUniqueID() == null
                    || world instanceof WorldServer
                    || !world.isAirBlock(x, y, z)) return false;
                // GT's normal hatch placer needs a WorldServer fake player; NEI's DummyWorld cannot supply one.
                ItemStack representative = machine.coolantConstructionStack();
                if (!(representative.getItem() instanceof ItemMachines item) || !item.placeBlockAt(
                    representative,
                    machine.previewPlayer,
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
                    tile.setFrontFacing(
                        machine.getExtendedFacing()
                            .getRelativeForwardInWorld());
                    if (tile.getMetaTileEntity() instanceof MTEHatch hatch) hatch.updateTexture(CONTAINMENT_TEXTURE);
                }
                return true;
            }
        };
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

    private boolean hasConstructedCoolantInput() {
        IGregTechTileEntity controller = getBaseMetaTileEntity();
        if (controller == null) return false;
        int[] worldOffset = new int[3];
        for (int[] offset : COOLANT_OFFSETS) {
            getExtendedFacing().getWorldOffset(offset, worldOffset);
            if (controller.getWorld()
                .getTileEntity(
                    controller.getXCoord() + worldOffset[0],
                    controller.getYCoord() + worldOffset[1],
                    controller.getZCoord() + worldOffset[2]) instanceof IGregTechTileEntity tile) {
                IMetaTileEntity hatch = tile.getMetaTileEntity();
                if (!(hatch instanceof IDualInputHatch) && HatchElement.InputHatch.matchesHatch(hatch)) return true;
            }
        }
        return false;
    }

    private static boolean isServiceHatchCandidate(IMetaTileEntity hatch) {
        if (hatch == null || hatch instanceof IDualInputHatch) return false;
        // .133 ExoticDynamo has no candidate classes; withMteClass still delegates matchesHatch to that empty list.
        return HatchElement.InputBus.matchesHatch(hatch) || HatchElement.OutputBus.matchesHatch(hatch)
            || HatchElement.Dynamo.matchesHatch(hatch)
            || hatch instanceof MTEHatchDynamoMulti
            || HatchElement.LaserSource.matchesHatch(hatch)
            || HatchElement.Maintenance.matchesHatch(hatch);
    }

    private boolean isCoolantConstructionCandidate(ItemStack stack) {
        if (!constructing) return true;
        // Creative/NEI construction uses one ULV hatch of each kind; candidate queries remain tier-independent.
        // Preview placement cannot rely on a complete structure recheck; inspect the two real A positions.
        ItemStack representative = coolantConstructionStack();
        return stack != null && stack.getItem() == representative.getItem()
            && stack.getItemDamage() == representative.getItemDamage();
    }

    private ItemStack coolantConstructionStack() {
        return (hasConstructedCoolantInput() ? ItemList.Hatch_Output_ULV : ItemList.Hatch_Input_ULV).get(1);
    }

    public int getFuelRodLimit() {
        return Math.min(getMaxParallel(), getTrueParallel());
    }

    @Override
    public int getMaxParallelRecipes() {
        // The GT input field requires a nonempty range even before the structure tier is known.
        return Math.max(1, getMaxParallel());
    }

    public int getFuelRodTier() {
        return fuelRodTier;
    }

    public int getMaxParallel() {
        return BlockFuelRod.maxParallel(fuelRodTier);
    }

    public int getHeatCapacity() {
        return BlockFuelRod.heatCapacity(fuelRodTier);
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        breederInputs.clear();
        breederOutputs.clear();
        fuelRodTier = -1;
        pipeTier = -1;
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
        expansionValidated = false;
        expansionState = "structure";
        if (!requiredStructureChunksLoaded(tile)) {
            errors.add(StructureErrors.of("thermonuclear.status.chunk_unloaded"));
            return;
        }
        if (!mainFitsWorld) {
            errors.add(StructureErrors.of("thermonuclear.structure.nuclear.world_bounds"));
            return;
        }
        fuelRodTier = -1;
        pipeTier = -1;
        if (!checkPiece(PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z, errors)) {
            fuelRodTier = -1;
            pipeTier = -1;
            return;
        }
        if (mInputBusses.isEmpty()) {
            errors.add(StructureErrors.missingHatch(HatchElement.InputBus));
        }
        if (mOutputBusses.isEmpty()) errors.add(StructureErrors.missingHatch(HatchElement.OutputBus));
        // Breeding is optional, but dedicated IO must be installed as a pair and never satisfies normal fuel IO.
        if (breederInputs.isEmpty() != breederOutputs.isEmpty()) {
            errors.add(StructureErrors.of("thermonuclear.structure.nuclear.breeder_io"));
        }
        if (mInputHatches.size() != 1 || mOutputHatches.size() != 1) {
            errors.add(StructureErrors.of("thermonuclear.structure.nuclear.coolant_io"));
        }
        // Dynamos remain mode-dependent; maintenance hatches are optional and do not enable failures.
        if (!errors.isEmpty()) {
            fuelRodTier = -1;
            pipeTier = -1;
        } else {
            scanExpansion(tile, errors);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onPreviewConstruct(ItemStack trigger) {
        // ItemMachines assigns this owner before input hatches look up UUID-keyed default-mode preferences.
        previewPlayer = Minecraft.getMinecraft().thePlayer;
        previewConstruction = true;
        if (previewPlayer != null && previewPlayer.getUniqueID() == null) previewPlayer = null;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        constructing = true;
        try {
            buildPiece(PIECE, stack, hintsOnly, OFFSET_X, OFFSET_Y, OFFSET_Z);
            // The channel selects a convenient example layout, not the allowed module connections.
            List<Position> examples = NuclearModuleStructure
                .preview(ChannelDataAccessor.getChannelData(stack, "extra"));
            for (Position origin : examples) {
                buildModule(stack, hintsOnly, origin);
            }
        } finally {
            constructing = false;
            previewConstruction = false;
            previewPlayer = null;
        }
    }

    private void buildModule(ItemStack stack, boolean hintsOnly, Position origin) {
        buildPiece(NuclearModuleStructure.MODULE, stack, hintsOnly, -origin.x, -origin.y, -origin.z);
    }

    @Override
    protected Casings casing() {
        return CASING;
    }

    @Override
    protected int structureChunkRadius() {
        return NuclearPowerPlantStructure.CHUNK_RADIUS;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType(StatCollector.translateToLocal(nameKey()))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.prototype"))
            .addSeparator()
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.direct.title"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.3"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.direct.formula.0"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.direct.formula.1"))
            .addSeparator()
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.heat.title"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.heat_mode"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.heat.formula.0"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.heat.formula.1"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.cooling"))
            .addSeparator()
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.features"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.formula.variables"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.formula.pulses"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.formula.limits"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.0"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.heat_capacity"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.1"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.2"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.meltdown"))
            .addSeparator()
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.breeding.0"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.breeding.1"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.breeding.2"))
            .addSeparator()
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.expansion.0"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.expansion.1"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.expansion.2"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.expansion.3"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.expansion.4"))
            .addSeparator()
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.wireless.0"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.wireless.1"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.support_any"))
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
            .toolTipFinisher();
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

    private final NuclearEfficiencyPolicy efficiencyPolicy = NuclearEfficiencyPolicy.CONFIGURED;
    private NuclearOperatingMode mode = NuclearOperatingMode.DIRECT_GENERATION;
    private ItemStack workingFuel;
    private FuelRodAdapter workingFuelAdapter;
    private int workingFuelRemainingCycles;
    private ItemStack pendingDepleted;
    private final BreederPool breederPool = new BreederPool();
    private long breederPulses;
    private double fuelFraction;
    private double distilledSteamRemainder;
    private CoolingFluid selectedCoolingFluid;
    private double reactorHeat;
    private final NuclearMeltdownTimer meltdownTimer = new NuclearMeltdownTimer();
    private double fullLoadHeatRate;
    private NBTTagCompound fuelSnapshot;
    private FuelBatch.Plan fuelPlan;
    private Runnable coolingInputCommit;
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

    public boolean requestModeChange() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null || !tile.isServerSide() || tile.isAllowedToWork() || running) return false;
        mode = mode.next();
        // Work fuel, durability and all material fractions belong to the machine, not to its mode.
        markDirty();
        return true;
    }

    private void setWorkingFuel(ItemStack stack) {
        workingFuel = stack;
        // Resolve only when the batch changes, including NBT restoration and transaction rollback.
        workingFuelAdapter = stack == null ? null : FuelRodAdapters.find(stack);
        workingFuelRemainingCycles = workingFuelAdapter == null ? 0 : workingFuelAdapter.remainingCycles(stack);
    }

    public boolean requestFuelEjection() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null || !tile.isServerSide()
            || !mMachine
            || tile.isAllowedToWork()
            || running
            || fuelSnapshot != null
            || workingFuel == null
            || getOutputBusses().isEmpty()) return false;

        ItemStack output = workingFuel.copy();
        // Rod items only store whole cycles: round up exported wear instead of refunding fractional fuel use.
        if (fuelFraction > 0 && workingFuelAdapter != null) {
            workingFuelAdapter.consumeCycles(output, 1);
            if (workingFuelAdapter.remainingCycles(output) == 0) {
                output = workingFuelAdapter.depleted(output);
                output.stackSize = workingFuel.stackSize;
            }
        }
        ItemEjectionHelper outputs = new ItemEjectionHelper(getOutputBusses(), true);
        int ejected = outputs.ejectStack(output);
        if (ejected <= 0) return false;
        outputs.commit();
        workingFuel.stackSize -= ejected;
        if (workingFuel.stackSize == 0) {
            setWorkingFuel(null);
            fuelFraction = 0;
        }
        updateSlots();
        markDirty();
        return true;
    }

    private void takeFuel() {
        if (workingFuel != null || pendingDepleted != null) return;
        fuelPlan = FuelBatch.prepare(cycleItems(), FuelRodAdapters.all(), getFuelRodLimit());
        setWorkingFuel(fuelPlan == null ? null : fuelPlan.fuel);
    }

    private boolean flushDepleted() {
        if (pendingDepleted == null) return true;
        ItemEjectionHelper outputs = new ItemEjectionHelper(getOutputBusses(), true);
        ItemStack remaining = pendingDepleted.copy();
        if (outputs.ejectStack(remaining) <= 0) return false;
        commitOutput(() -> {
            outputs.commit();
            pendingDepleted = remaining.stackSize > 0 ? remaining : null;
        });
        return true;
    }

    private void processBreeders(long pulses) {
        breederPulses = pulses;
        if (pulses <= 0 || breederInputs.isEmpty() || breederOutputs.isEmpty()) return;
        List<ItemStack> inputs = new ArrayList<>();
        for (MTEBreederInputBus bus : breederInputs) {
            if (!bus.isValid()) continue;
            for (int slot = 0; slot < bus.getCircuitSlot(); slot++) inputs.add(bus.getStackInSlot(slot));
        }
        Runnable consumeBreeders = breederPool.prepareLoad(inputs, getFuelRodLimit());
        commitOutput(() -> {
            consumeBreeders.run();
            for (MTEBreederInputBus bus : breederInputs) {
                if (!bus.isValid()) continue;
                bus.updateSlots();
                bus.markDirty();
            }
        });
        breederPool.advance(pulses);
    }

    private boolean flushBreederProducts() {
        if (breederPool.completedCount() == 0 || breederOutputs.isEmpty()) return false;
        List<IOutputBus> outputs = new ArrayList<>();
        for (MTEBreederOutputBus bus : breederOutputs) if (bus.isValid()) outputs.add(bus);
        if (outputs.isEmpty()) return false;
        // One helper reserves all products together, including partial output of different rod types.
        ItemEjectionHelper helper = new ItemEjectionHelper(outputs, true);
        List<ItemStack> remaining = breederPool.copyCompleted();
        int ejected = 0;
        for (ItemStack stack : remaining) ejected += helper.ejectStack(stack);
        if (ejected == 0) return false;
        commitOutput(() -> {
            helper.commit();
            breederPool.finishOutput(remaining);
        });
        return true;
    }

    @Override
    protected boolean processCycle() {
        coolingInputCommit = null;
        try {
            boolean successful = processNuclearCycle();
            if (successful) {
                flushBreederProducts();
            } else if (("fuel".equals(status) || "spent_full".equals(status)
                || "fuel_limit".equals(status)
                || "output_full".equals(status)) && flushBreederProducts()) {
                    // Recover breeder products independently of ordinary fuel shortages, pauses and blocked outputs.
                    cycleAdvancesStartup = false;
                    successful = true;
                }
            // A rejected heat cycle must not consume coolant while its outputs are discarded.
            if (successful && coolingInputCommit != null) coolingInputCommit.run();
            return successful;
        } finally {
            coolingInputCommit = null;
        }
    }

    private boolean processNuclearCycle() {
        if (getOutputBusses().isEmpty() || (mInputBusses.isEmpty() && mDualInputHatches.isEmpty())) {
            return fail("hatches");
        }
        double heatBeforeCooling = reactorHeat;
        if (mode == NuclearOperatingMode.HEAT_SUPPLY && !coolReactor()) return false;
        boolean cooled = reactorHeat < heatBeforeCooling;
        if (pendingDepleted != null) {
            cycleAdvancesStartup = false;
            return flushDepleted() || coolingOnly(cooled, "spent_full");
        }
        // Keep paid-for batches intact after a downgrade; pause instead of discarding or over-processing them.
        if (getMaxParallel() <= 0 || (workingFuel != null && workingFuel.stackSize > getMaxParallel())) {
            return coolingOnly(cooled, "fuel_limit");
        }
        takeFuel();
        FuelRodAdapter fuel = workingFuelAdapter;
        if (fuel == null) return coolingOnly(cooled, "fuel");
        if (workingFuelRemainingCycles == 0) {
            cycleAdvancesStartup = false;
            pendingDepleted = depletedBatch(fuel);
            setWorkingFuel(null);
            fuelFraction = 0;
            return flushDepleted() || coolingOnly(cooled, "spent_full");
        }
        double efficiency = efficiencyPolicy.efficiency(mode);
        if (!Double.isFinite(efficiency) || efficiency <= 0 || efficiency > 1)
            return coolingOnly(cooled, "invalid_value");
        double ramp = startup.averageNext(Config.nuclearPowerPlant.nuclearStartupTicks, CYCLE_TICKS);
        double remaining = workingFuelRemainingCycles - fuelFraction;
        double cycles = Math.min(
            remaining,
            Config.nuclearPowerPlant.fuelCyclesPerSecond * (mode == NuclearOperatingMode.HEAT_SUPPLY ? ramp : 1));
        if (!Double.isFinite(cycles) || cycles <= 0) return coolingOnly(cooled, "invalid_value");
        switch (mode) {
            case DIRECT_GENERATION -> {
                if (!checkWirelessOwner()) return false;
                if (!isWirelessMode() && dynamoRating() <= 0) return fail("hatches");
                double baseEUt = fuel.baseEUt(workingFuel);
                if (!Double.isFinite(baseEUt) || baseEUt <= 0) return fail("invalid_value");
                double outputMultiplier = nuclearEnergyMultiplier();
                fullLoadEUt = baseEUt * outputMultiplier
                    * Config.nuclearPowerPlant.fuelCyclesPerSecond
                    * efficiency
                    * workingFuel.stackSize;
                // No coolant access in this path; ramp is applied to output, not fuel consumption.
                operatingEUt = baseEUt * outputMultiplier * cycles * efficiency * workingFuel.stackSize;
            }
            case HEAT_SUPPLY -> {
                double heatPerFuelCycle = fuel.heatPerCycle(workingFuel)
                    * Config.nuclearPowerPlant.nuclearHeatOutputMultiplier
                    * efficiency
                    * workingFuel.stackSize;
                if (!Double.isFinite(heatPerFuelCycle) || heatPerFuelCycle <= 0)
                    return coolingOnly(cooled, "invalid_value");
                fullLoadHeatRate = heatPerFuelCycle * Config.nuclearPowerPlant.fuelCyclesPerSecond / CYCLE_TICKS;
                double generatedHeat = heatPerFuelCycle * cycles;
                if (!Double.isFinite(generatedHeat) || !Double.isFinite(reactorHeat + generatedHeat))
                    return coolingOnly(cooled, "invalid_value");
                reactorHeat += generatedHeat;
            }
        }
        processBreeders((long) Math.max(0, fuel.cells(workingFuel)) * workingFuel.stackSize);
        double used = fuelFraction + cycles;
        int damage = (int) Math.floor(used);
        if (damage > 0) fuel.consumeCycles(workingFuel, damage);
        workingFuelRemainingCycles = fuel.remainingCycles(workingFuel);
        fuelFraction = StartupProgress.fraction(used - damage);
        if (workingFuelRemainingCycles == 0) {
            pendingDepleted = depletedBatch(fuel);
            setWorkingFuel(null);
            fuelFraction = 0;
            // Retain the entire spent batch when output is blocked; never load a second batch behind it.
            flushDepleted();
        }
        if (fuelPlan != null) fuelPlan.consumeInputs();
        return true;
    }

    private boolean coolingOnly(boolean cooled, String reason) {
        // A cooling-only success must not retain a staged fuel batch without paying for its inputs.
        if (fuelPlan != null) {
            setWorkingFuel(null);
            fuelPlan = null;
        }
        cycleAdvancesStartup = false;
        return cooled || fail(reason);
    }

    private boolean coolReactor() {
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
            reactorHeat,
            litresPerHeat,
            available(input),
            distilled ? Config.nuclearPowerPlant.nuclearSteamPerDistilledWater : 0,
            distilledSteamRemainder);
        if (limit == 0) return true;
        // Each probe is a fresh simulation; only the final accepted reservation is committed.
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
        reactorHeat = NuclearCoolingMath.remainingHeat(reactorHeat, amount, litresPerHeat);
        if (distilled) distilledSteamRemainder = NuclearCoolingMath
            .steamRemainder(amount, Config.nuclearPowerPlant.nuclearSteamPerDistilledWater, distilledSteamRemainder);
        coolingInputCommit = () -> consume(input, amount);
        if (accepted != null) commitOutput(accepted::commit);
        inputLitresPerTick = amount / (double) CYCLE_TICKS;
        outputLitresPerTick = produced / (double) CYCLE_TICKS;
        return true;
    }

    private boolean processStoppedCoolingCycle() {
        coolingInputCommit = null;
        try {
            if (!coolReactor() || coolingInputCommit == null) return false;
            coolingInputCommit.run();
            return true;
        } finally {
            coolingInputCommit = null;
        }
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

    private ItemStack depletedBatch(FuelRodAdapter fuel) {
        ItemStack depleted = fuel.depleted(workingFuel);
        depleted.stackSize = workingFuel.stackSize;
        return depleted;
    }

    @Override
    public void startRecipeProcessing() {
        super.startRecipeProcessing();
        fuelSnapshot = new NBTTagCompound();
        saveFuelState(fuelSnapshot);
        fuelPlan = null;
    }

    @Override
    public void endRecipeProcessing() {
        super.endRecipeProcessing();
        if (!checkRecipeResult.wasSuccessful() && fuelSnapshot != null) {
            // ME extraction failures must not leave a copied internal batch that was never paid for.
            loadFuelState(fuelSnapshot);
        }
        if (checkRecipeResult.wasSuccessful()) markDirty();
        fuelSnapshot = null;
        fuelPlan = null;
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
    protected void beforeProcessingCycle() {
        fullLoadHeatRate = 0;
        breederPulses = 0;
    }

    private void saveFuelState(NBTTagCompound nbt) {
        nbt.setString("tnNuclearMode", mode.id());
        nbt.setDouble("tnFuelFraction", fuelFraction);
        FuelBatch.save(nbt, "tnWorkingFuel", "tnFuelCount", workingFuel);
        FuelBatch.save(nbt, "tnPendingDepleted", "tnPendingDepletedCount", pendingDepleted);
        breederPool.save(nbt);
        nbt.setLong("tnBreederPulses", breederPulses);
        nbt.setDouble("tnDistilledSteamRemainder", distilledSteamRemainder);
        nbt.setDouble("tnReactorHeat", reactorHeat);
        nbt.setString(
            "tnSelectedCoolant",
            selectedCoolingFluid == null ? ""
                : selectedCoolingFluid.name()
                    .toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setInteger("tnExpansionLevel", expansionLevel);
        nbt.setBoolean("tnExpansionKnown", expansionKnown);
        saveFuelState(nbt);
        nbt.setInteger("tnOverheatTicks", meltdownTimer.elapsedTicks());
        nbt.setDouble("tnFullLoadHeatRate", fullLoadHeatRate);
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
        int savedLevel = nbt.getInteger("tnExpansionLevel");
        expansionKnown = nbt.getBoolean("tnExpansionKnown") && savedLevel >= 1
            && savedLevel <= 1 + NuclearModuleStructure.MAX_MODULES;
        expansionLevel = expansionKnown ? savedLevel : 1;
        // History supports change detection, never formation or permission to resume a saved cycle.
        expansionValidated = false;
        expansionState = "checking";
        expansionScan = null;
        validatedModules = java.util.Collections.emptySet();
        loadFuelState(nbt);
        meltdownTimer.restore(nbt.getInteger("tnOverheatTicks"));
        double savedRate = nbt.getDouble("tnFullLoadHeatRate");
        fullLoadHeatRate = Double.isFinite(savedRate) && savedRate > 0 ? savedRate : 0;
    }

    private void loadFuelState(NBTTagCompound nbt) {
        mode = NuclearOperatingMode.fromId(nbt.getString("tnNuclearMode"));
        setWorkingFuel(FuelBatch.load(nbt, "tnWorkingFuel", "tnFuelCount"));
        pendingDepleted = FuelBatch.load(nbt, "tnPendingDepleted", "tnPendingDepletedCount");
        breederPool.load(nbt);
        breederPulses = Math.max(0, nbt.getLong("tnBreederPulses"));
        fuelFraction = workingFuel == null ? 0 : StartupProgress.fraction(nbt.getDouble("tnFuelFraction"));
        selectedCoolingFluid = null;
        String selected = nbt.getString("tnSelectedCoolant");
        if ("ic2".equals(selected)) selectedCoolingFluid = CoolingFluid.IC2;
        if ("super".equals(selected)) selectedCoolingFluid = CoolingFluid.SUPER;
        if ("nak_composite".equals(selected)) selectedCoolingFluid = CoolingFluid.NAK_COMPOSITE;
        if ("distilled".equals(selected)) selectedCoolingFluid = CoolingFluid.DISTILLED;
        distilledSteamRemainder = StartupProgress.fraction(nbt.getDouble("tnDistilledSteamRemainder"));
        double savedHeat = nbt.getDouble("tnReactorHeat");
        // Structure tiers are not known during load or transaction rollback. Preserve heat after a downgrade.
        reactorHeat = Double.isFinite(savedHeat) ? Math.max(0, savedHeat) : 0;
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (!tile.isServerSide()) return;
        if ("chunk_unloaded".equals(status) || mStartUpCheck >= 0) return;
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
        if (reactorHeat <= 0) {
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
                double previousHeat = reactorHeat;
                reactorHeat = NuclearCoolingMath
                    .passiveCooling(reactorHeat, Config.nuclearPowerPlant.nuclearPassiveCoolingPerSecond, CYCLE_TICKS);
                if (reactorHeat != previousHeat) markDirty();
            }
        }
        inputLitresPerTick = stoppedInputRate;
        outputLitresPerTick = stoppedOutputRate;
        status = stoppedInputRate > 0 ? "stopped_cooling"
            : reactorHeat > 0 && Config.nuclearPowerPlant.nuclearPassiveCoolingPerSecond > 0 ? "stopped_passive"
                : "stopped";
        updateMeltdown(tile);
    }

    private void updateMeltdown(IGregTechTileEntity tile) {
        int previousTicks = meltdownTimer.elapsedTicks();
        boolean meltdown = meltdownTimer.advance(reactorHeat, getHeatCapacity());
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
        for (Position origin : validatedModules) {
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
        expansionValidated = false;
        expansionState = "structure";
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
    public String[] displayKeys() {
        // Keep sync identities stable while the same open GUI switches operating mode.
        return new String[] { "status", "mode", "expansion_count", "fuel_rod_tier", "fuel_limit", "startup",
            "full_load", "fuel", "fuel_count", "fuel_remaining", "heat", "heat_capacity", "heat_rate", "coolant",
            "heat_limit", "water_limit", "coolant_input", "hot_output", "water_input", "steam_output", "pending_spent",
            "breeder_count", "breeder_rate", "breeder_progress" };
    }

    @Override
    public Map<String, String> displayInfo() {
        Map<String, String> info = commonInfo();
        addGenerationInfo(info);
        info.put("mode", mode.translationKey());
        if (expansionValidated) {
            info.put("expansion_count", (expansionLevel - 1) + " / " + NuclearModuleStructure.moduleLimit(pipeTier));
        }
        if (mMachine && fuelRodTier > 0) {
            info.put("fuel_rod_tier", Integer.toString(getFuelRodTier()));
            info.put("fuel_limit", Integer.toString(getFuelRodLimit()));
        }
        boolean hasWorkingFuel = workingFuel != null;
        info.put("fuel", hasWorkingFuel ? workingFuel.getDisplayName() : "thermonuclear.recipe.none");
        if (hasWorkingFuel) {
            info.put("fuel_count", Integer.toString(workingFuel.stackSize));
            info.put(
                "fuel_remaining",
                workingFuelAdapter == null ? "0" : decimal(workingFuelRemainingCycles - fuelFraction));
        }
        switch (mode) {
            case DIRECT_GENERATION:
                info.put("startup", decimal(100));
                break;
            case HEAT_SUPPLY:
                addStartupInfo(info);
                info.put("heat", decimal(reactorHeat));
                info.put("heat_capacity", Integer.toString(getHeatCapacity()));
                info.put("heat_rate", decimalPerCycle(running ? fullLoadHeatRate : 0));
                info.put(
                    "coolant",
                    selectedCoolingFluid == null ? "thermonuclear.recipe.none"
                        : selectedCoolingFluid == CoolingFluid.DISTILLED ? "thermonuclear.recipe.distilled"
                            : selectedCoolingFluid.coolant.nameKey());
                boolean water = selectedCoolingFluid == CoolingFluid.DISTILLED;
                if (selectedCoolingFluid != null) {
                    double litresPerHeat = water ? Config.nuclearPowerPlant.nuclearDistilledWaterPerHeat
                        : selectedCoolingFluid.coolant.coolantPerHeat();
                    info.put(water ? "water_limit" : "heat_limit", decimalPerCycle(fullLoadHeatRate * litresPerHeat));
                    info.put(water ? "water_input" : "coolant_input", decimalPerCycle(inputLitresPerTick));
                    info.put(water ? "steam_output" : "hot_output", decimalPerCycle(outputLitresPerTick));
                }
                break;
        }
        if (pendingDepleted != null) {
            info.put("pending_spent", pendingDepleted.stackSize + " x " + pendingDepleted.getDisplayName());
            info.put("breeder_count", Integer.toString(breederPool.workingCount()));
            info.put("breeder_rate", Long.toString(running ? breederPulses * 4 : 0));
            info.put("breeder_progress", Float.toString(breederPool.getHighestProgress()));
        }
        return info;
    }
}
