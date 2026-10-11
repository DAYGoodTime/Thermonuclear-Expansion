package com.lin.thermonuclear.machine;

import java.util.ArrayList;
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
import com.lin.thermonuclear.block.BlockAxialMachineComponent;
import com.lin.thermonuclear.gui.NuclearPowerPlantGui;
import com.lin.thermonuclear.loader.BlockLoader;
import com.lin.thermonuclear.nuclear.FuelBatch;
import com.lin.thermonuclear.nuclear.NuclearCoolingMath;
import com.lin.thermonuclear.nuclear.NuclearEfficiencyPolicy;
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
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTStructureUtility;
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
    private static final List<int[]> COOLANT_OFFSETS = coolantOffsets();
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
                    if (!BlockAxialMachineComponent.isAxisMetadata(meta)) return null;
                    if (block == BlockLoader.fuelRodTier1) return 1;
                    if (block == BlockLoader.fuelRodTier2) return 2;
                    if (block == BlockLoader.fuelRodTier3) return 3;
                    if (block == BlockLoader.fuelRodTier4) return 4;
                    return null;
                },
                    Arrays.asList(
                        Pair.of(BlockLoader.fuelRodTier1, 0),
                        Pair.of(BlockLoader.fuelRodTier2, 0),
                        Pair.of(BlockLoader.fuelRodTier3, 0),
                        Pair.of(BlockLoader.fuelRodTier4, 0)),
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

    private static List<int[]> coolantOffsets() {
        List<int[]> offsets = new ArrayList<>();
        for (int z = 0; z < SHAPE.length; z++) {
            for (int y = 0; y < SHAPE[z].length; y++) {
                String row = SHAPE[z][y];
                for (int x = 0; x < row.length(); x++) {
                    if (row.charAt(x) == 'A') offsets.add(new int[] { x - OFFSET_X, y - OFFSET_Y, z - OFFSET_Z });
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
        return switch (fuelRodTier) {
            case 1 -> 8;
            case 2 -> 32;
            case 3 -> 128;
            case 4 -> 512;
            default -> 0;
        };
    }

    public int getHeatCapacity() {
        return switch (fuelRodTier) {
            case 1 -> 50000;
            case 2 -> 100000;
            case 3 -> 1000000;
            case 4 -> Integer.MAX_VALUE;
            default -> 0;
        };
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        fuelRodTier = -1;
    }

    private boolean addServiceHatch(IGregTechTileEntity tile, int texture) {
        if (tile == null || tile.getMetaTileEntity() == null || tile.getMetaTileEntity() instanceof IDualInputHatch)
            return false;
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
        return machineTooltip().addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.0"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.1"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.2"))
            .addInfo(StatCollector.translateToLocal("thermonuclear.tooltip.nuclear.3"))
            .beginStructureBlock(
                NuclearPowerPlantStructure.WIDTH,
                NuclearPowerPlantStructure.HEIGHT,
                NuclearPowerPlantStructure.LENGTH,
                true)
            .addController(StatCollector.translateToLocal("thermonuclear.structure.nuclear.controller"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.nuclear.blocks.0"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.nuclear.blocks.1"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.nuclear.blocks.2"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.nuclear.blocks.3"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.nuclear.blocks.4"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.nuclear.pipes.0"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.nuclear.pipes.1"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.nuclear.services.0"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.nuclear.services.1"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.nuclear.services.2"))
            .addStructureHint("thermonuclear.structure.nuclear.services.0", 1)
            .addStructureHint("thermonuclear.structure.nuclear.coolant_io", 2)
            .addInputBus("1+", StatCollector.translateToLocal("thermonuclear.structure.nuclear.services.location"), 1)
            .addOutputBus("1+", StatCollector.translateToLocal("thermonuclear.structure.nuclear.services.location"), 1)
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
    private double fuelFraction;
    private double distilledSteamRemainder;
    private CoolingFluid selectedCoolingFluid;
    private double reactorHeat;
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

    @Override
    protected boolean processCycle() {
        coolingInputCommit = null;
        try {
            boolean successful = processNuclearCycle();
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
                if (generatedHeat > getHeatCapacity() - reactorHeat) {
                    stopMachine(SimpleShutDownReason.ofCritical("thermonuclear.status.heat_overflow"));
                    markDirty();
                    return fail("heat_overflow");
                }
                reactorHeat += generatedHeat;
            }
        }
        consumedFuelCycles = cycles * workingFuel.stackSize;
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
    }

    private void saveFuelState(NBTTagCompound nbt) {
        nbt.setString("tnNuclearMode", mode.id());
        nbt.setDouble("tnFuelFraction", fuelFraction);
        FuelBatch.save(nbt, "tnWorkingFuel", "tnFuelCount", workingFuel);
        FuelBatch.save(nbt, "tnPendingDepleted", "tnPendingDepletedCount", pendingDepleted);
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
        nbt.setDouble("tnFullLoadHeatRate", fullLoadHeatRate);
    }

    @Override
    public void setItemNBT(NBTTagCompound nbt) {
        super.setItemNBT(nbt);
        // Internal work fuel is not a normal inventory slot: carry it in the controller drop without restoring startup.
        saveFuelState(nbt);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        loadFuelState(nbt);
        double savedRate = nbt.getDouble("tnFullLoadHeatRate");
        fullLoadHeatRate = Double.isFinite(savedRate) && savedRate > 0 ? savedRate : 0;
    }

    private void loadFuelState(NBTTagCompound nbt) {
        mode = NuclearOperatingMode.fromId(nbt.getString("tnNuclearMode"));
        setWorkingFuel(FuelBatch.load(nbt, "tnWorkingFuel", "tnFuelCount"));
        pendingDepleted = FuelBatch.load(nbt, "tnPendingDepleted", "tnPendingDepletedCount");
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
        reactorHeat = Double.isFinite(savedHeat) ? Math.max(0, Math.min(Integer.MAX_VALUE, savedHeat)) : 0;
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (!tile.isServerSide()) return;
        if (tile.isAllowedToWork() || running) {
            stoppedInputRate = stoppedOutputRate = 0;
            return;
        }
        if (!mMachine || mStartUpCheck >= 0 || "chunk_unloaded".equals(status)) {
            stoppedInputRate = stoppedOutputRate = 0;
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
        if ("thermonuclear.status.heat_overflow".equals(
            tile.getLastShutDownReason()
                .getKey())) {
            status = "heat_overflow";
        } else {
            status = stoppedInputRate > 0 ? "stopped_cooling"
                : reactorHeat > 0 && Config.nuclearPowerPlant.nuclearPassiveCoolingPerSecond > 0 ? "stopped_passive"
                    : "stopped";
        }
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
            "heat_limit", "water_limit", "coolant_input", "hot_output", "water_input", "steam_output",
            "pending_spent" };
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
        info.put("heat_rate", decimal(running ? fullLoadHeatRate : 0));
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
                info.put(water ? "water_limit" : "heat_limit", decimal(fullLoadHeatRate * litresPerHeat));
                info.put(water ? "water_input" : "coolant_input", decimal(inputLitresPerTick));
                info.put(water ? "steam_output" : "hot_output", decimal(outputLitresPerTick));
            }
        }
        if (pendingDepleted != null) {
            info.put("pending_spent", pendingDepleted.stackSize + " x " + pendingDepleted.getDisplayName());
        }
        return info;
    }
}
