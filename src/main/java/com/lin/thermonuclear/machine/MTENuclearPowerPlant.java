package com.lin.thermonuclear.machine;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.gui.NuclearPowerPlantGui;
import com.lin.thermonuclear.nuclear.FuelBatch;
import com.lin.thermonuclear.nuclear.FuelRodAdapter;
import com.lin.thermonuclear.nuclear.GTFuelRodAdapter;
import com.lin.thermonuclear.nuclear.IC2FuelRodAdapter;
import com.lin.thermonuclear.nuclear.NuclearEfficiencyPolicy;
import com.lin.thermonuclear.recipe.HeatExchangeRecipe;

import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.ItemEjectionHelper;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public final class MTENuclearPowerPlant extends ThermonuclearMultiblockBase<MTENuclearPowerPlant> {

    private static final String PIECE = "nuclear_power_plant";
    private static final int SIZE = 3;
    private static final int OFFSET_X = 1;
    private static final int OFFSET_Y = 1;
    private static final int OFFSET_Z = 0;
    private static final Casings CASING = Casings.HeatProofMachineCasing;
    // StructureLib order: [depth][top-to-bottom row], with the controller at (1, 1, 0).
    private static final String[][] SHAPE = { { "CCC", "C~C", "CCC" }, { "CCC", "C-C", "CCC" },
        { "CCC", "CCC", "CCC" } };
    private static final IStructureDefinition<MTENuclearPowerPlant> STRUCTURE = StructureDefinition
        .<MTENuclearPowerPlant>builder()
        .addShape(PIECE, SHAPE)
        .addElement(
            'C',
            GTStructureUtility.<MTENuclearPowerPlant>ofHatchAdderOptional(
                (machine, tile, texture) -> machine.addMachineHatch(tile, texture),
                CASING.textureId,
                1,
                CASING.getBlock(),
                CASING.meta))
        .build();

    @Override
    public IStructureDefinition<MTENuclearPowerPlant> getStructureDefinition() {
        return STRUCTURE;
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        if (!checkPiece(PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z, errors)) return;
        if (mInputBusses.isEmpty() && mDualInputHatches.isEmpty()) {
            errors.add(StructureErrors.missingHatch(HatchElement.InputBus));
        }
        if (mOutputBusses.isEmpty()) errors.add(StructureErrors.missingHatch(HatchElement.OutputBus));
        // Dynamo and coolant hatches are checked at processing time for the selected operating mode.
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(PIECE, stack, hintsOnly, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }

    @Override
    protected Casings casing() {
        return CASING;
    }

    @Override
    protected int structureChunkRadius() {
        return SIZE - 1;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return machineTooltip().beginStructureBlock(SIZE, SIZE, SIZE, true)
            .addController(StatCollector.translateToLocal("thermonuclear.structure.controller"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.box"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.hatches"))
            .addInputBus("1+", "Shell", 1)
            .addOutputBus("1+", "Shell", 1)
            .addInputHatch("0+", "Shell", 1)
            .addOutputHatch("0+", "Shell", 1)
            .addDynamoHatch("0+", "Shell", 1)
            .toolTipFinisher();
    }

    private static final FuelRodAdapter[] ADAPTERS = { new IC2FuelRodAdapter(), new GTFuelRodAdapter() };
    private final NuclearEfficiencyPolicy efficiencyPolicy = NuclearEfficiencyPolicy.CONFIGURED;
    private NuclearOperatingMode mode = NuclearOperatingMode.DIRECT_GENERATION;
    private ItemStack workingFuel;
    private ItemStack pendingDepleted;
    private double fuelFraction;
    private final double[] coolantFractions = new double[HeatExchangeRecipe.values().length];
    private HeatExchangeRecipe selectedCoolant;
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
        return FuelBatch.findAdapter(stack, ADAPTERS);
    }

    private void takeFuel() {
        if (workingFuel != null || pendingDepleted != null) return;
        fuelPlan = FuelBatch.prepare(cycleItems(), ADAPTERS);
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
                fullLoadEUt = baseEUt * Config.fuelCyclesPerSecond * efficiency * workingFuel.stackSize;
                // No coolant access in this path; ramp is applied to output, not fuel consumption.
                cycleEUt = baseEUt * cycles * efficiency * workingFuel.stackSize;
            }
            case HEAT_SUPPLY -> {
                if (!hasFluidInputs() || mOutputHatches.isEmpty()) return fail("hatches");
                HeatExchangeRecipe coolant = chooseCoolant();
                if (coolant == null) return fail("coolant");
                selectedCoolant = coolant;
                fullLoadHeatRate = fuel.heatPerCycle(workingFuel) * Config.fuelCyclesPerSecond
                    / 20
                    * efficiency
                    * coolant.coolantPerHeat()
                    * workingFuel.stackSize;
                double litres = fuel.heatPerCycle(workingFuel) * cycles
                    * efficiency
                    * coolant.coolantPerHeat()
                    * workingFuel.stackSize;
                double accumulated = litres + coolantFractions[coolant.ordinal()];
                if (!Double.isFinite(accumulated) || accumulated < 0 || accumulated > Integer.MAX_VALUE - 1) {
                    return fail("invalid_value");
                }
                int amount = (int) Math.floor(accumulated);
                // Fractional progress must still have a real coolant and output path available.
                if (available(coolant.cold()) < Math.max(1, amount)) return fail("coolant");
                FluidEjectionHelper outputs = prepareOutputs(new FluidStack(coolant.hot(), Math.max(1, amount)));
                if (outputs == null) return fail("output_full");
                if (amount > 0) {
                    consume(coolant.cold(), amount);
                    commitOutput(outputs::commit);
                }
                coolantFractions[coolant.ordinal()] = StartupProgress.fraction(accumulated - amount);
                inputRate = outputRate = amount / (double) CYCLE_TICKS;
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

    private HeatExchangeRecipe chooseCoolant() {
        if (selectedCoolant != null && selectedCoolant.hot() != null
            && selectedCoolant.cold() != null
            && available(selectedCoolant.cold()) > 0) return selectedCoolant;
        for (HeatExchangeRecipe coolant : HeatExchangeRecipe.values()) {
            if (coolant.hot() != null && coolant.cold() != null && available(coolant.cold()) > 0) return coolant;
        }
        return null;
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
        nbt.setString("tnSelectedCoolant", selectedCoolant == null ? "" : selectedCoolant.id());
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
        selectedCoolant = null;
        for (HeatExchangeRecipe coolant : HeatExchangeRecipe.values()) {
            coolantFractions[coolant.ordinal()] = StartupProgress
                .fraction(nbt.getDouble("tnCoolantFraction_" + coolant.id()));
            if (coolant.id()
                .equals(nbt.getString("tnSelectedCoolant"))) selectedCoolant = coolant;
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
        return Config.nuclearStartupTicks;
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
    public String[] detailKeys() {
        return new String[] { "mode", "fuel", "fuel_count", "fuel_remaining", "fuel_rate", "coolant", "heat_limit",
            "pending_spent" };
    }

    @Override
    public String[] detailValues() {
        FuelRodAdapter fuel = adapter(workingFuel);
        return new String[] { mode.translationKey(), workingFuel == null ? "-" : workingFuel.getDisplayName(),
            workingFuel == null ? "0" : Integer.toString(workingFuel.stackSize),
            fuel == null ? "0" : decimal(fuel.remainingCycles(workingFuel) - fuelFraction), decimal(consumedFuelCycles),
            selectedCoolant == null || mode == NuclearOperatingMode.DIRECT_GENERATION ? "thermonuclear.recipe.none"
                : selectedCoolant.translationKey(),
            decimal(fullLoadHeatRate),
            pendingDepleted == null ? "-" : pendingDepleted.stackSize + " x " + pendingDepleted.getDisplayName() };
    }
}
