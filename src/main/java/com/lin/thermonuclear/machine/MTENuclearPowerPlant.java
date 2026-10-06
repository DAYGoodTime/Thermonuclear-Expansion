package com.lin.thermonuclear.machine;

import java.util.ArrayList;
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

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.api.FuelRodAdapter;
import com.lin.thermonuclear.api.FuelRodAdapters;
import com.lin.thermonuclear.gui.NuclearPowerPlantGui;
import com.lin.thermonuclear.nuclear.FuelBatch;
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
import gregtech.api.enums.Materials;
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
            'A',
            GTStructureUtility
                .chainItemPipeCasings(-1, (machine, tier) -> machine.pipeTier = tier, machine -> machine.pipeTier))
        .addElement('B', Casings.ReinforcedGlass.asElement())
        .addElement('C', StructureUtility.ofBlock(GregTechAPI.sBlockReinforced, CONCRETE_META))
        .addElement('D', GTStructureUtility.ofFrame(Materials.Steel))
        .addElement('F', GTStructureUtility.ofAnyWater())
        .addElement('H', GTStructureUtility.ofAnyWater())
        .addElement(
            'G',
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
            'I',
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

    private int pipeTier = -1;
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
                    if (row.charAt(x) == 'I') offsets.add(new int[] { x - OFFSET_X, y - OFFSET_Y, z - OFFSET_Z });
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
        // Preview placement cannot rely on a complete structure recheck; inspect the two real I positions.
        ItemStack representative = coolantConstructionStack();
        return stack != null && stack.getItem() == representative.getItem()
            && stack.getItemDamage() == representative.getItemDamage();
    }

    private ItemStack coolantConstructionStack() {
        return (hasConstructedCoolantInput() ? ItemList.Hatch_Output_ULV : ItemList.Hatch_Input_ULV).get(1);
    }

    public int getPipeTier() {
        return pipeTier;
    }

    public int getFuelRodLimit() {
        return pipeTier < 1 || pipeTier > 8 ? 0 : Config.nuclearFuelRodsPerPipeTier * pipeTier;
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        pipeTier = -1;
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
        if (!checkPiece(PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z, errors)) return;
        if (mInputBusses.isEmpty()) {
            errors.add(StructureErrors.missingHatch(HatchElement.InputBus));
        }
        if (mOutputBusses.isEmpty()) errors.add(StructureErrors.missingHatch(HatchElement.OutputBus));
        if (mInputHatches.size() != 1 || mOutputHatches.size() != 1) {
            errors.add(StructureErrors.of("thermonuclear.structure.nuclear.coolant_io"));
        }
        // Dynamos remain mode-dependent; maintenance hatches are optional and do not enable failures.
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
        return machineTooltip()
            .beginStructureBlock(
                NuclearPowerPlantStructure.WIDTH,
                NuclearPowerPlantStructure.HEIGHT,
                NuclearPowerPlantStructure.LENGTH,
                true)
            .addController(StatCollector.translateToLocal("thermonuclear.structure.nuclear.controller"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.nuclear.blocks"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.nuclear.pipes"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.nuclear.services"))
            .addInputBus("1+", "G", 1)
            .addOutputBus("1+", "G", 1)
            .addInputHatch("1", "I", 2)
            .addOutputHatch("1", "I", 2)
            .addDynamoHatch("0+", "G", 1)
            .addMaintenanceHatch("0+", "G", 1)
            .toolTipFinisher();
    }

    private enum CoolingFluid {
        IC2,
        SUPER,
        DISTILLED
    }

    private final NuclearEfficiencyPolicy efficiencyPolicy = NuclearEfficiencyPolicy.CONFIGURED;
    private NuclearOperatingMode mode = NuclearOperatingMode.DIRECT_GENERATION;
    private ItemStack workingFuel;
    private ItemStack pendingDepleted;
    private double fuelFraction;
    private final double[] coolantFractions = new double[HeatExchangeRecipe.values().length];
    private double distilledWaterFraction;
    private CoolingFluid selectedCoolingFluid;
    private double reactorHeat;
    private double consumedFuelCycles;
    private double fullLoadHeatRate;
    private NBTTagCompound fuelSnapshot;
    private FuelBatch.Plan fuelPlan;

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

    private static FuelRodAdapter adapter(ItemStack stack) {
        return FuelRodAdapters.find(stack);
    }

    private void takeFuel() {
        if (workingFuel != null || pendingDepleted != null) return;
        fuelPlan = FuelBatch.prepare(cycleItems(), FuelRodAdapters.all(), getFuelRodLimit());
        workingFuel = fuelPlan == null ? null : fuelPlan.fuel;
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
        consumedFuelCycles = 0;
        if (getOutputBusses().isEmpty() || (mInputBusses.isEmpty() && mDualInputHatches.isEmpty())) {
            return fail("hatches");
        }
        if (pendingDepleted != null) {
            cycleAdvancesStartup = false;
            return flushDepleted() || fail("spent_full");
        }
        // Keep paid-for batches intact after a downgrade; pause instead of discarding or over-processing them.
        if (getFuelRodLimit() <= 0 || (workingFuel != null && workingFuel.stackSize > getFuelRodLimit())) {
            return fail("fuel_limit");
        }
        takeFuel();
        FuelRodAdapter fuel = adapter(workingFuel);
        if (fuel == null) return fail("fuel");
        if (fuel.remainingCycles(workingFuel) == 0) {
            cycleAdvancesStartup = false;
            pendingDepleted = depletedBatch(fuel);
            workingFuel = null;
            fuelFraction = 0;
            return flushDepleted() || fail("spent_full");
        }
        double efficiency = efficiencyPolicy.efficiency(mode);
        if (!Double.isFinite(efficiency) || efficiency <= 0 || efficiency > 1) return fail("invalid_value");
        double ramp = startup.averageNext(Config.nuclearStartupTicks, CYCLE_TICKS);
        double remaining = fuel.remainingCycles(workingFuel) - fuelFraction;
        double cycles = Math
            .min(remaining, Config.fuelCyclesPerSecond * (mode == NuclearOperatingMode.HEAT_SUPPLY ? ramp : 1));
        if (!Double.isFinite(cycles) || cycles <= 0) return fail("invalid_value");
        switch (mode) {
            case DIRECT_GENERATION -> {
                if (dynamoRating() <= 0) return fail("hatches");
                double baseEUt = fuel.baseEUt(workingFuel);
                if (!Double.isFinite(baseEUt) || baseEUt <= 0) return fail("invalid_value");
                double outputMultiplier = nuclearEnergyMultiplier();
                fullLoadEUt = baseEUt * outputMultiplier
                    * Config.fuelCyclesPerSecond
                    * efficiency
                    * workingFuel.stackSize;
                // No coolant access in this path; ramp is applied to output, not fuel consumption.
                cycleEUt = baseEUt * outputMultiplier * cycles * efficiency * workingFuel.stackSize;
            }
            case HEAT_SUPPLY -> {
                if (!hasFluidInputs() || mOutputHatches.isEmpty()) return fail("hatches");
                // Cooling happens before new heat is generated, so a reactor can recover from a full load.
                if (selectedCoolingFluid == null || !availableCoolingFluid(selectedCoolingFluid)) {
                    selectCoolingFluid();
                }
                double heatPerFuelCycle = fuel.heatPerCycle(workingFuel) * Config.nuclearHeatOutputMultiplier
                    * efficiency
                    * workingFuel.stackSize;
                if (!Double.isFinite(heatPerFuelCycle) || heatPerFuelCycle <= 0) return fail("invalid_value");
                double cooledHeat = reactorHeat;
                int coolingAmount = 0;
                FluidEjectionHelper outputs = null;
                Fluid coolingFluid = null;
                double heatPerCoolingUnit = 0;
                boolean distilled = selectedCoolingFluid == CoolingFluid.DISTILLED;
                if (selectedCoolingFluid != null && availableCoolingFluid(selectedCoolingFluid)) {
                    if (distilled) {
                        coolingFluid = WorkingFluids.distilledWater;
                        heatPerCoolingUnit = 1 / Config.nuclearDistilledWaterPerHeat;
                    } else {
                        HeatExchangeRecipe coolant = selectedCoolingFluid == CoolingFluid.IC2
                            ? HeatExchangeRecipe.IC2_COOLANT
                            : HeatExchangeRecipe.SUPER_COOLANT;
                        coolingFluid = coolant.cold();
                        heatPerCoolingUnit = 1 / coolant.coolantPerHeat();
                    }
                    long available = available(coolingFluid);
                    coolingAmount = (int) Math
                        .min(Integer.MAX_VALUE, Math.min(available, Math.ceil(reactorHeat / heatPerCoolingUnit)));
                    if (coolingAmount > 0) {
                        cooledHeat = Math.max(0, reactorHeat - coolingAmount * heatPerCoolingUnit);
                        Fluid outputFluid = distilled ? HeatExchangeSteam.ORDINARY.fluid()
                            : (selectedCoolingFluid == CoolingFluid.IC2 ? WorkingFluids.ic2HotCoolant
                                : WorkingFluids.hotSuperCoolant);
                        int outputAmount = distilled
                            ? Math.max(1, (int) Math.floor(coolingAmount * Config.nuclearSteamPerDistilledWater))
                            : coolingAmount;
                        if (outputAmount <= 0) return fail("invalid_value");
                        outputs = prepareOutputs(new FluidStack(outputFluid, outputAmount));
                        if (outputs == null) return fail("output_full");
                    }
                }
                double room = Config.nuclearHeatCapacity - cooledHeat;
                cycles = Math.min(cycles, room / heatPerFuelCycle);
                if (!Double.isFinite(cycles) || cycles <= 0) return fail("heat_full");
                double generatedHeat = heatPerFuelCycle * cycles;
                reactorHeat = cooledHeat + generatedHeat;
                fullLoadHeatRate = heatPerFuelCycle * Config.fuelCyclesPerSecond / CYCLE_TICKS;
                inputRate = coolingAmount / (double) CYCLE_TICKS;
                outputRate = inputRate;
                if (coolingAmount > 0) {
                    consume(coolingFluid, coolingAmount);
                    commitOutput(outputs::commit);
                }
            }
        }
        consumedFuelCycles = cycles * workingFuel.stackSize;
        double used = fuelFraction + cycles;
        int damage = (int) Math.floor(used);
        if (damage > 0) fuel.consumeCycles(workingFuel, damage);
        fuelFraction = StartupProgress.fraction(used - damage);
        if (fuel.remainingCycles(workingFuel) == 0) {
            pendingDepleted = depletedBatch(fuel);
            workingFuel = null;
            fuelFraction = 0;
            // Retain the entire spent batch when output is blocked; never load a second batch behind it.
            flushDepleted();
        }
        if (fuelPlan != null) fuelPlan.consumeInputs();
        return true;
    }

    private static double nuclearEnergyMultiplier() {
        double ic2Value = 1.0;
        ic2.core.util.Config config = MainConfig.get();
        if (config != null) {
            ic2Value = ConfigUtil.getFloat(config, "balance/energy/generator/nuclear");;
        }
        double outputMultiplier = ic2Value * Config.nuclearDirectOutputMultiplier;
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
        fuelSnapshot = null;
        fuelPlan = null;
    }

    private boolean selectCoolingFluid() {
        if (selectedCoolingFluid != null) return availableCoolingFluid(selectedCoolingFluid);
        for (HeatExchangeRecipe coolant : HeatExchangeRecipe.values()) {
            if (coolant.hot() != null && coolant.cold() != null && available(coolant.cold()) > 0) {
                selectedCoolingFluid = coolant == HeatExchangeRecipe.IC2_COOLANT ? CoolingFluid.IC2
                    : CoolingFluid.SUPER;
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
        return switch (fluid) {
            case IC2 -> WorkingFluids.ic2Coolant != null && available(WorkingFluids.ic2Coolant) > 0;
            case SUPER -> WorkingFluids.superCoolant != null && available(WorkingFluids.superCoolant) > 0;
            case DISTILLED -> WorkingFluids.distilledWater != null && available(WorkingFluids.distilledWater) > 0;
        };
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
        for (HeatExchangeRecipe coolant : HeatExchangeRecipe.values()) {
            nbt.setDouble("tnCoolantFraction_" + coolant.id(), coolantFractions[coolant.ordinal()]);
        }
        nbt.setDouble("tnDistilledWaterFraction", distilledWaterFraction);
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
    }

    private void loadFuelState(NBTTagCompound nbt) {
        mode = NuclearOperatingMode.fromId(nbt.getString("tnNuclearMode"));
        workingFuel = FuelBatch.load(nbt, "tnWorkingFuel", "tnFuelCount");
        pendingDepleted = FuelBatch.load(nbt, "tnPendingDepleted", "tnPendingDepletedCount");
        fuelFraction = workingFuel == null ? 0 : StartupProgress.fraction(nbt.getDouble("tnFuelFraction"));
        selectedCoolingFluid = null;
        for (HeatExchangeRecipe coolant : HeatExchangeRecipe.values()) {
            coolantFractions[coolant.ordinal()] = StartupProgress
                .fraction(nbt.getDouble("tnCoolantFraction_" + coolant.id()));
        }
        String selected = nbt.getString("tnSelectedCoolant");
        if ("ic2".equals(selected)) selectedCoolingFluid = CoolingFluid.IC2;
        if ("super".equals(selected)) selectedCoolingFluid = CoolingFluid.SUPER;
        if ("distilled".equals(selected)) selectedCoolingFluid = CoolingFluid.DISTILLED;
        distilledWaterFraction = StartupProgress.fraction(nbt.getDouble("tnDistilledWaterFraction"));
        double savedHeat = nbt.getDouble("tnReactorHeat");
        reactorHeat = Double.isFinite(savedHeat) ? Math.max(0, Math.min(Config.nuclearHeatCapacity, savedHeat)) : 0;
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (tile.isServerSide() && !tile.isAllowedToWork() && !running) selectedCoolingFluid = null;
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
        return mode == NuclearOperatingMode.DIRECT_GENERATION ? 0 : Config.nuclearStartupTicks;
    }

    @Override
    protected int decayTicks() {
        return Config.nuclearDecayTicks;
    }

    @Override
    public String nameKey() {
        return "thermonuclear.machine.nuclear.name";
    }

    @Override
    protected String machineKind() {
        return "nuclear";
    }

    @Override
    public String[] displayKeys() {
        // Keep sync identities stable while the same open GUI switches operating mode.
        return new String[] { "status", "mode", "startup", "full_load", "produced", "pipe_tier", "fuel_limit", "fuel",
            "fuel_count", "fuel_remaining", "fuel_rate", "heat", "heat_capacity", "heat_rate", "coolant", "heat_limit",
            "water_limit", "coolant_input", "hot_output", "water_input", "steam_output", "pending_spent" };
    }

    @Override
    public Map<String, String> displayInfo() {
        Map<String, String> info = commonInfo();
        info.put("mode", mode.translationKey());
        if (mMachine && pipeTier > 0) {
            info.put("pipe_tier", Integer.toString(pipeTier));
            info.put("fuel_limit", Integer.toString(getFuelRodLimit()));
        }
        if (mode == NuclearOperatingMode.DIRECT_GENERATION) info.put("startup", decimal(100));
        else addStartupInfo(info);
        if (mode == NuclearOperatingMode.DIRECT_GENERATION) addGenerationInfo(info);
        FuelRodAdapter fuel = adapter(workingFuel);
        info.put("fuel", workingFuel == null ? "thermonuclear.recipe.none" : workingFuel.getDisplayName());
        if (workingFuel != null) {
            info.put("fuel_count", Integer.toString(workingFuel.stackSize));
            info.put("fuel_remaining", fuel == null ? "0" : decimal(fuel.remainingCycles(workingFuel) - fuelFraction));
            info.put("fuel_rate", decimal(running ? consumedFuelCycles : 0));
        }
        info.put("heat", decimal(reactorHeat));
        info.put("heat_capacity", Integer.toString(Config.nuclearHeatCapacity));
        info.put("heat_rate", decimal(running ? fullLoadHeatRate : 0));
        if (mode == NuclearOperatingMode.HEAT_SUPPLY) {
            info.put(
                "coolant",
                selectedCoolingFluid == null ? "thermonuclear.recipe.none"
                    : selectedCoolingFluid == CoolingFluid.IC2 ? HeatExchangeRecipe.IC2_COOLANT.translationKey()
                        : selectedCoolingFluid == CoolingFluid.SUPER ? HeatExchangeRecipe.SUPER_COOLANT.translationKey()
                            : "thermonuclear.recipe.distilled");
            boolean water = selectedCoolingFluid == CoolingFluid.DISTILLED;
            if (selectedCoolingFluid != null) {
                info.put(water ? "water_limit" : "heat_limit", decimal(fullLoadHeatRate));
                info.put(water ? "water_input" : "coolant_input", decimal(inputRate));
                info.put(water ? "steam_output" : "hot_output", decimal(outputRate));
            }
        }
        if (pendingDepleted != null) {
            info.put("pending_spent", pendingDepleted.stackSize + " x " + pendingDepleted.getDisplayName());
        }
        return info;
    }
}
