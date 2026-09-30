package com.lin.thermonuclear.machine;

import java.util.Collections;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.nuclear.FuelRodAdapter;
import com.lin.thermonuclear.nuclear.GTFuelRodAdapter;
import com.lin.thermonuclear.nuclear.IC2FuelRodAdapter;
import com.lin.thermonuclear.nuclear.NuclearEfficiencyPolicy;
import com.lin.thermonuclear.recipe.HeatExchangeRecipe;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.ItemEjectionHelper;

public final class MTENuclearPowerPlant extends PrototypeMultiblockBase<MTENuclearPowerPlant> {

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
        for (FuelRodAdapter candidate : ADAPTERS) {
            if (candidate.accepts(stack)) return candidate;
        }
        return null;
    }

    private void takeFuel() {
        if (workingFuel != null || pendingDepleted != null) return;
        for (MTEHatchInputBus bus : mInputBusses) {
            if (!bus.isValid()) continue;
            for (int slot = 0; slot < bus.getSizeInventory(); slot++) {
                if (!bus.isValidSlot(slot) || slot == bus.getCircuitSlot()) continue;
                ItemStack stack = bus.getStackInSlot(slot);
                if (stack == null || stack.stackSize <= 0 || adapter(stack) == null) continue;
                workingFuel = stack.copy();
                workingFuel.stackSize = 1;
                bus.decrStackSize(slot, 1);
                bus.markDirty();
                return;
            }
        }
    }

    private boolean flushDepleted() {
        if (pendingDepleted == null) return true;
        ItemEjectionHelper outputs = new ItemEjectionHelper(getOutputBusses(), true);
        if (outputs.ejectItems(Collections.singletonList(pendingDepleted), 1) != 1) return false;
        outputs.commit();
        pendingDepleted = null;
        return true;
    }

    @Override
    protected boolean tickPrototype(long tick) {
        consumedFuelCycles = 0;
        if (getOutputBusses().isEmpty() || mInputBusses.stream()
            .noneMatch(MTEHatchInputBus::isValid)) {
            return fail("hatches");
        }
        if (!flushDepleted()) return fail("spent_full");
        takeFuel();
        FuelRodAdapter fuel = adapter(workingFuel);
        if (fuel == null) return fail("fuel");
        if (fuel.remainingCycles(workingFuel) == 0) {
            pendingDepleted = fuel.depleted(workingFuel);
            workingFuel = null;
            fuelFraction = 0;
            return fail(flushDepleted() ? "fuel" : "spent_full");
        }
        double efficiency = efficiencyPolicy.efficiency(mode);
        if (!Double.isFinite(efficiency) || efficiency <= 0 || efficiency > 1) return fail("invalid_value");
        double ramp = startup.next(Config.nuclearStartupTicks);
        double remaining = fuel.remainingCycles(workingFuel) - fuelFraction;
        double cycles = Math
            .min(remaining, Config.fuelCyclesPerSecond / 20.0 * (mode == NuclearOperatingMode.HEAT_SUPPLY ? ramp : 1));
        if (!Double.isFinite(cycles) || cycles <= 0) return fail("invalid_value");
        switch (mode) {
            case DIRECT_GENERATION -> {
                if (dynamoRating() <= 0) return fail("hatches");
                double baseEUt = fuel.baseEUt(workingFuel);
                if (!Double.isFinite(baseEUt) || baseEUt <= 0) return fail("invalid_value");
                fullLoadEUt = baseEUt * Config.fuelCyclesPerSecond * efficiency;
                // No coolant access in this path; ramp is applied to output, not fuel consumption.
                generate(baseEUt * cycles * 20 * efficiency * ramp);
            }
            case HEAT_SUPPLY -> {
                if (mInputHatches.isEmpty() || mOutputHatches.isEmpty()) return fail("hatches");
                HeatExchangeRecipe coolant = chooseCoolant();
                if (coolant == null) return fail("coolant");
                selectedCoolant = coolant;
                fullLoadHeatRate = fuel.heatPerCycle(workingFuel) * Config.fuelCyclesPerSecond
                    / 20
                    * efficiency
                    * coolant.coolantPerHeat();
                double litres = fuel.heatPerCycle(workingFuel) * cycles * efficiency * coolant.coolantPerHeat();
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
                    outputs.commit();
                }
                coolantFractions[coolant.ordinal()] = StartupProgress.fraction(accumulated - amount);
                inputRate = outputRate = amount;
            }
        }
        consumedFuelCycles = cycles;
        double used = fuelFraction + cycles;
        int damage = (int) Math.floor(used);
        if (damage > 0) fuel.consumeCycles(workingFuel, damage);
        fuelFraction = StartupProgress.fraction(used - damage);
        if (fuel.remainingCycles(workingFuel) == 0) {
            pendingDepleted = fuel.depleted(workingFuel);
            workingFuel = null;
            fuelFraction = 0;
            // If the output bus is full, retain exactly this one spent rod, and refuse another fuel.
            flushDepleted();
        }
        return true;
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
    protected void beforeServerTick() {
        consumedFuelCycles = 0;
        fullLoadHeatRate = 0;
    }

    private void saveFuelState(NBTTagCompound nbt) {
        nbt.setString("tnNuclearMode", mode.id());
        nbt.setDouble("tnFuelFraction", fuelFraction);
        if (workingFuel != null) nbt.setTag("tnWorkingFuel", workingFuel.writeToNBT(new NBTTagCompound()));
        else nbt.removeTag("tnWorkingFuel");
        if (pendingDepleted != null) nbt.setTag("tnPendingDepleted", pendingDepleted.writeToNBT(new NBTTagCompound()));
        else nbt.removeTag("tnPendingDepleted");
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
        mode = NuclearOperatingMode.fromId(nbt.getString("tnNuclearMode"));
        workingFuel = nbt.hasKey("tnWorkingFuel") ? ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("tnWorkingFuel"))
            : null;
        pendingDepleted = nbt.hasKey("tnPendingDepleted")
            ? ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("tnPendingDepleted"))
            : null;
        if (workingFuel != null) workingFuel.stackSize = 1;
        if (pendingDepleted != null) pendingDepleted.stackSize = 1;
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
        return new String[] { "mode", "fuel", "fuel_remaining", "fuel_rate", "coolant", "heat_limit", "pending_spent" };
    }

    @Override
    public String[] detailValues() {
        FuelRodAdapter fuel = adapter(workingFuel);
        return new String[] { mode.translationKey(), workingFuel == null ? "-" : workingFuel.getDisplayName(),
            fuel == null ? "0" : decimal(fuel.remainingCycles(workingFuel) - fuelFraction), decimal(consumedFuelCycles),
            selectedCoolant == null || mode == NuclearOperatingMode.DIRECT_GENERATION ? "thermonuclear.recipe.none"
                : selectedCoolant.translationKey(),
            decimal(fullLoadHeatRate), pendingDepleted == null ? "-" : pendingDepleted.getDisplayName() };
    }
}
