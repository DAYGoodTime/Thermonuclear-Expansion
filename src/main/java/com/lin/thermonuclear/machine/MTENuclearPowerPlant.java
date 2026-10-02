package com.lin.thermonuclear.machine;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.gui.NuclearPowerPlantGui;
import com.lin.thermonuclear.nuclear.FuelBatch;
import com.lin.thermonuclear.nuclear.FuelRodAdapter;
import com.lin.thermonuclear.nuclear.GTFuelRodAdapter;
import com.lin.thermonuclear.nuclear.IC2FuelRodAdapter;
import com.lin.thermonuclear.nuclear.NuclearEfficiencyPolicy;
import com.lin.thermonuclear.recipe.HeatExchangeRecipe;
import com.lin.thermonuclear.recipe.HeatExchangeSteam;
import com.lin.thermonuclear.registry.WorkingFluids;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.ItemEjectionHelper;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public final class MTENuclearPowerPlant extends PrototypeMultiblockBase<MTENuclearPowerPlant> {

    private enum CoolingFluid {
        IC2,
        SUPER,
        DISTILLED
    }

    private static final FuelRodAdapter[] ADAPTERS = { new IC2FuelRodAdapter(), new GTFuelRodAdapter() };
    private final NuclearEfficiencyPolicy efficiencyPolicy = NuclearEfficiencyPolicy.CONFIGURED;
    private NuclearOperatingMode mode = NuclearOperatingMode.DIRECT_GENERATION;
    private ItemStack workingFuel;
    private ItemStack pendingDepleted;
    private double fuelFraction;
    private final double[] coolantFractions = new double[HeatExchangeRecipe.values().length];
    private double distilledWaterFraction;
    private CoolingFluid selectedCoolingFluid;
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
                if (!selectCoolingFluid()) return fail("coolant");
                if (selectedCoolingFluid == CoolingFluid.DISTILLED) {
                    fullLoadHeatRate = fuel.heatPerCycle(workingFuel) * Config.fuelCyclesPerSecond
                        / 20
                        * efficiency
                        * Config.nuclearDistilledWaterPerHeat
                        * workingFuel.stackSize;
                    double water = fuel.heatPerCycle(workingFuel) * cycles
                        * efficiency
                        * Config.nuclearDistilledWaterPerHeat
                        * workingFuel.stackSize;
                    double accumulated = water + distilledWaterFraction;
                    if (!Double.isFinite(accumulated) || accumulated < 0 || accumulated > Integer.MAX_VALUE - 1) {
                        return fail("invalid_value");
                    }
                    int waterAmount = (int) Math.floor(accumulated);
                    int steamAmount = (int) Math.floor(waterAmount * Config.nuclearSteamPerDistilledWater);
                    if (available(WorkingFluids.distilledWater) < Math.max(1, waterAmount)) return fail("water");
                    FluidEjectionHelper outputs = prepareOutputs(
                        new FluidStack(HeatExchangeSteam.ORDINARY.fluid(), Math.max(1, steamAmount)));
                    if (outputs == null) return fail("output_full");
                    if (waterAmount > 0) {
                        consume(WorkingFluids.distilledWater, waterAmount);
                        commitOutput(outputs::commit);
                    }
                    distilledWaterFraction = StartupProgress.fraction(accumulated - waterAmount);
                    inputRate = waterAmount / (double) CYCLE_TICKS;
                    outputRate = steamAmount / (double) CYCLE_TICKS;
                } else {
                    HeatExchangeRecipe coolant = selectedCoolingFluid == CoolingFluid.IC2
                        ? HeatExchangeRecipe.IC2_COOLANT
                        : HeatExchangeRecipe.SUPER_COOLANT;
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

    private boolean selectCoolingFluid() {
        if (selectedCoolingFluid != null) return availableCoolingFluid(selectedCoolingFluid);
        for (HeatExchangeRecipe coolant : HeatExchangeRecipe.values()) {
            if (coolant.hot() != null && coolant.cold() != null && available(coolant.cold()) > 0) {
                selectedCoolingFluid = coolant == HeatExchangeRecipe.IC2_COOLANT ? CoolingFluid.IC2 : CoolingFluid.SUPER;
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
        nbt.setString(
            "tnSelectedCoolant",
            selectedCoolingFluid == null ? "" : selectedCoolingFluid.name().toLowerCase(java.util.Locale.ROOT));
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
    protected boolean requiresFluidHatches() {
        return false;
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
            selectedCoolingFluid == null || mode == NuclearOperatingMode.DIRECT_GENERATION ? "thermonuclear.recipe.none"
                : selectedCoolingFluid == CoolingFluid.IC2 ? HeatExchangeRecipe.IC2_COOLANT.translationKey()
                    : selectedCoolingFluid == CoolingFluid.SUPER ? HeatExchangeRecipe.SUPER_COOLANT.translationKey()
                        : "thermonuclear.recipe.distilled",
            decimal(fullLoadHeatRate),
            pendingDepleted == null ? "-" : pendingDepleted.stackSize + " x " + pendingDepleted.getDisplayName() };
    }
}
