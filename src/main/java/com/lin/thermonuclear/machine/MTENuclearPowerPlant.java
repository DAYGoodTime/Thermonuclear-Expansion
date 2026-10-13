package com.lin.thermonuclear.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.api.FuelRodAdapter;
import com.lin.thermonuclear.api.FuelRodAdapters;
import com.lin.thermonuclear.block.BlockFuelRod;
import com.lin.thermonuclear.block.BlockMachineComponent;
import com.lin.thermonuclear.gui.NuclearPowerPlantGui;
import com.lin.thermonuclear.loader.BlockLoader;
import com.lin.thermonuclear.machine.hatch.MTEBreederInputBus;
import com.lin.thermonuclear.machine.hatch.MTEBreederOutputBus;
import com.lin.thermonuclear.nuclear.BreederPool;
import com.lin.thermonuclear.nuclear.FuelBatch;
import com.lin.thermonuclear.nuclear.NuclearCoolingMath;
import com.lin.thermonuclear.nuclear.NuclearEfficiencyPolicy;
import com.lin.thermonuclear.nuclear.NuclearMeltdownTimer;
import com.lin.thermonuclear.recipe.HeatExchangeRecipe;
import com.lin.thermonuclear.recipe.HeatExchangeSteam;
import com.lin.thermonuclear.registry.WorkingFluids;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.INEIPreviewModifier;
import gregtech.api.interfaces.IOutputBus;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
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
    private static final int CONCRETE_META = 13;
    private static final int CONCRETE_TEXTURE = 221;
    private static final String[][] SHAPE = NuclearPowerPlantStructure.createShape();
    private static final List<int[]> COOLANT_OFFSETS = structureOffsets('A');
    private static final List<int[]> WATER_OFFSETS = structureOffsets('G');
    private static final List<int[]> FUEL_ROD_OFFSETS = structureOffsets('D');
    private static final IStructureDefinition<MTENuclearPowerPlant> STRUCTURE = StructureDefinition
        .<MTENuclearPowerPlant>builder()
        .addShape(PIECE, SHAPE)
        .addElement(
            'C',
            StructureUtility.ofChain(
                StructureUtility.ofBlock(GregTechAPI.sBlockCasings2, 12),
                StructureUtility.ofBlock(GregTechAPI.sBlockCasings2, 13),
                StructureUtility.ofBlock(GregTechAPI.sBlockCasings2, 14),
                StructureUtility.ofBlock(GregTechAPI.sBlockCasings2, 15),
                StructureUtility.ofBlock(GregTechAPI.sBlockCasings8, 1),
                StructureUtility.ofBlock(GregTechAPI.sBlockCasings9, 0)))
        .addElement(
            'D',
            StructureUtility.withChannel(
                "fuel_rod",
                StructureUtility.<MTENuclearPowerPlant, Integer>ofBlocksTiered((block, meta) -> {
                    if (!BlockMachineComponent.isAxisMetadata(meta)) return null;
                    return block instanceof BlockFuelRod rod ? rod.tier() : null;
                },
                    BlockLoader.fuelRods()
                        .stream()
                        .map(block -> Pair.<Block, Integer>of(block, BlockMachineComponent.AXIS_Y))
                        .collect(Collectors.toList()),
                    -1,
                    (machine, tier) -> machine.fuelRodTier = tier,
                    machine -> machine.fuelRodTier)))
        .addElement('F', StructureUtility.ofBlock(GregTechAPI.sBlockReinforced, CONCRETE_META))
        .addElement('G', GTStructureUtility.ofAnyWater())
        .addElement(
            'B',
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
                .casingIndex(CONCRETE_TEXTURE)
                .hint(1)
                .buildAndChain(GregTechAPI.sBlockReinforced, CONCRETE_META))
        .addElement(
            'A',
            StructureUtility.ofChain(
                GTStructureUtility.buildHatchAdder(MTENuclearPowerPlant.class)
                    .anyOf(HatchElement.InputHatch, HatchElement.OutputHatch)
                    .adder(MTENuclearPowerPlant::addCoolantHatch)
                    .hatchItemFilterAnd(
                        machine -> stack -> !(ItemMachines.getMetaTileEntity(stack) instanceof IDualInputHatch)
                            && machine.isCoolantConstructionCandidate(stack))
                    .casingIndex(CONCRETE_TEXTURE)
                    .hint(2)
                    .exclusive()
                    .build(),
                coolantPreviewPlacement()))
        .build();

    private int fuelRodTier = -1;
    private final List<MTEBreederInputBus> breederInputs = new ArrayList<>();
    private final List<MTEBreederOutputBus> breederOutputs = new ArrayList<>();
    private boolean constructing;
    private boolean previewConstruction;
    private EntityPlayer previewPlayer;

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
                    if (tile.getMetaTileEntity() instanceof MTEHatch hatch) hatch.updateTexture(CONCRETE_TEXTURE);
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
        fuelRodTier = -1;
        if (!checkPiece(PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z, errors)) {
            fuelRodTier = -1;
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
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onPreviewConstruct(ItemStack trigger) {
        // ItemMachines assigns this owner before input hatches look up UUID-keyed default-mode preferences.
        previewPlayer = Minecraft.getMinecraft().thePlayer;
        previewConstruction = previewPlayer != null && previewPlayer.getUniqueID() != null;
        if (!previewConstruction) previewPlayer = null;
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
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.support_any"))
            .beginStructureBlock(
                NuclearPowerPlantStructure.WIDTH,
                NuclearPowerPlantStructure.HEIGHT,
                NuclearPowerPlantStructure.LENGTH,
                true)
            .addController(StatCollector.translateToLocal("thermonuclear.structure.nuclear.controller"))
            .addCasing("52", StatCollector.translateToLocal("thermonuclear.structure.nuclear.fuel_rods"), true)
            .addCasing("2006", StatCollector.translateToLocal("thermonuclear.structure.nuclear.concrete"), false)
            .addCasing("353", StatCollector.translateToLocal("thermonuclear.structure.nuclear.water"), false)
            .addCasing("14", StatCollector.translateToLocal("thermonuclear.structure.nuclear.pipe_casings"), false)
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

        IC2(HeatExchangeRecipe.IC2_COOLANT),
        SUPER(HeatExchangeRecipe.SUPER_COOLANT),
        NAK_COMPOSITE(HeatExchangeRecipe.NAK_COMPOSITE_COOLANT),
        DISTILLED(null);

        private final HeatExchangeRecipe recipe;

        CoolingFluid(HeatExchangeRecipe recipe) {
            this.recipe = recipe;
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
    private double consumedFuelCycles;
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
        Runnable consumeBreeders = breederPool.prepareLoad(inputs);
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
        consumedFuelCycles = 0;
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
                if (dynamoRating() <= 0) return fail("hatches");
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
        consumedFuelCycles = cycles * workingFuel.stackSize;
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
        HeatExchangeRecipe coolant = selectedCoolingFluid.recipe;
        Fluid input = distilled ? WorkingFluids.distilledWater : coolant.cold();
        Fluid output = distilled ? HeatExchangeSteam.ORDINARY.fluid() : coolant.hot();
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
            HeatExchangeRecipe coolant = candidate.recipe;
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
        Fluid input = fluid == CoolingFluid.DISTILLED ? WorkingFluids.distilledWater : fluid.recipe.cold();
        return input != null && available(input) > 0;
    }

    @Override
    protected void beforeProcessingCycle() {
        consumedFuelCycles = 0;
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
        consumedFuelCycles = fullLoadHeatRate = 0;
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
        for (int[] offset : FUEL_ROD_OFFSETS) {
            getExtendedFacing().getWorldOffset(offset, worldOffset);
            int x = tile.getXCoord() + worldOffset[0];
            int y = tile.getYCoord() + worldOffset[1];
            int z = tile.getZCoord() + worldOffset[2];
            if (world.getBlock(x, y, z) instanceof BlockFuelRod) world.setBlock(x, y, z, Blocks.lava, 0, 3);
        }
        stopMachine(SimpleShutDownReason.ofCritical("thermonuclear.status.meltdown"));
        mMachine = false;
        fuelRodTier = -1;
        running = false;
        operatingEUt = fullLoadEUt = fullLoadHeatRate = 0;
        inputLitresPerTick = outputLitresPerTick = stoppedInputRate = stoppedOutputRate = 0;
        consumedFuelCycles = 0;
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
        return new String[] { "status", "mode", "fuel_rod_tier", "max_parallel", "fuel_limit", "startup", "full_load",
            "fuel", "fuel_count", "fuel_remaining", "fuel_rate", "heat", "heat_capacity", "heat_rate", "coolant",
            "heat_limit", "water_limit", "coolant_input", "hot_output", "water_input", "steam_output", "pending_spent",
            "breeder_count", "breeder_limit", "breeder_pulses", "breeder_rate", "breeder_progress", "breeder_pending" };
    }

    @Override
    public Map<String, String> displayInfo() {
        Map<String, String> info = commonInfo();
        info.put("mode", mode.translationKey());
        if (mMachine && fuelRodTier > 0) {
            info.put("fuel_rod_tier", Integer.toString(getFuelRodTier()));
            info.put("max_parallel", Integer.toString(getMaxParallel()));
            info.put("fuel_limit", Integer.toString(getFuelRodLimit()));
        }
        if (mode == NuclearOperatingMode.DIRECT_GENERATION) info.put("startup", decimal(100));
        else addStartupInfo(info);
        if (mode == NuclearOperatingMode.DIRECT_GENERATION) addGenerationInfo(info);
        info.put("fuel", workingFuel == null ? "thermonuclear.recipe.none" : workingFuel.getDisplayName());
        if (workingFuel != null) {
            info.put("fuel_count", Integer.toString(workingFuel.stackSize));
            info.put(
                "fuel_remaining",
                workingFuelAdapter == null ? "0" : decimal(workingFuelRemainingCycles - fuelFraction));
            info.put("fuel_rate", decimal(running ? consumedFuelCycles : 0));
        }
        info.put("heat", decimal(reactorHeat));
        info.put("heat_capacity", Integer.toString(getHeatCapacity()));
        info.put("heat_rate", decimalPerCycle(running ? fullLoadHeatRate : 0));
        if (mode == NuclearOperatingMode.HEAT_SUPPLY || stoppedInputRate > 0) {
            info.put(
                "coolant",
                selectedCoolingFluid == null ? "thermonuclear.recipe.none"
                    : selectedCoolingFluid == CoolingFluid.DISTILLED ? "thermonuclear.recipe.distilled"
                        : selectedCoolingFluid.recipe.translationKey());
            boolean water = selectedCoolingFluid == CoolingFluid.DISTILLED;
            if (selectedCoolingFluid != null) {
                double litresPerHeat = water ? Config.nuclearPowerPlant.nuclearDistilledWaterPerHeat
                    : selectedCoolingFluid.recipe.coolantPerHeat();
                info.put(water ? "water_limit" : "heat_limit", decimalPerCycle(fullLoadHeatRate * litresPerHeat));
                info.put(water ? "water_input" : "coolant_input", decimalPerCycle(inputLitresPerTick));
                info.put(water ? "steam_output" : "hot_output", decimalPerCycle(outputLitresPerTick));
            }
        }
        if (pendingDepleted != null) {
            info.put("pending_spent", pendingDepleted.stackSize + " x " + pendingDepleted.getDisplayName());
        }
        info.put("breeder_count", Integer.toString(breederPool.workingCount()));
        info.put("breeder_limit", Integer.toString(BreederPool.MAX_RODS));
        info.put("breeder_pulses", Long.toString(running ? breederPulses : 0));
        info.put("breeder_rate", Long.toString(running ? breederPulses * 4 : 0));
        info.put("breeder_progress", breederPool.progressSummary());
        info.put("breeder_pending", Integer.toString(breederPool.completedCount()));
        return info;
    }
}
