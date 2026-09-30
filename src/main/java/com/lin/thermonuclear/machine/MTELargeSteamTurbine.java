package com.lin.thermonuclear.machine;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.registry.WorkingFluids;

import gregtech.api.enums.GTValues;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.items.MetaGeneratedTool;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.TurbineStatCalculator;
import gregtech.common.items.MetaGeneratedTool01;

public final class MTELargeSteamTurbine extends PrototypeMultiblockBase<MTELargeSteamTurbine> {

    private int condensationRemainder;
    private ItemStack rotorReference;
    private NBTTagCompound rotorIdentity;
    private int flowLimit;
    private long rotorDurability;
    private long rotorMaxDurability;

    @Override
    public void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        if (slot == 1 && startup != null && getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isServerSide()) {
            // GUI and item-handler extraction/insertion also pass through this hook; tool wear does not.
            startup.clear();
            rotorReference = null;
            rotorIdentity = null;
        }
    }

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
    public boolean isCorrectMachinePart(ItemStack stack) {
        return stack != null && stack.stackSize == 1
            && stack.getItem() instanceof MetaGeneratedTool01
            && stack.getItemDamage() >= 170
            && stack.getItemDamage() <= 179
            && MetaGeneratedTool.getToolMaxDamage(stack) > MetaGeneratedTool.getToolDamage(stack);
    }

    @Override
    protected void beforeServerTick() {
        ItemStack rotor = getControllerSlot();
        if (!isCorrectMachinePart(rotor)) {
            startup.clear();
            rotorReference = null;
            rotorIdentity = null;
            rotorDurability = rotorMaxDurability = 0;
            flowLimit = 0;
            return;
        }
        ItemStack identityStack = rotor.copy();
        identityStack.getTagCompound()
            .getCompoundTag("GT.ToolStats")
            .removeTag("Damage");
        NBTTagCompound identity = identityStack.writeToNBT(new NBTTagCompound());
        // An object replacement catches even an identical new rotor. Across load, the saved fingerprint takes over.
        if ((rotorReference != null && rotorReference != rotor)
            || (rotorIdentity != null && !rotorIdentity.equals(identity))) startup.clear();
        rotorIdentity = identity;
        rotorReference = rotor;
        TurbineStatCalculator stats = new TurbineStatCalculator((MetaGeneratedTool) rotor.getItem(), rotor);
        rotorDurability = stats.getCurrentDurability();
        rotorMaxDurability = stats.getMaxDurability();
    }

    @Override
    protected boolean tickPrototype(long tick) {
        ItemStack rotor = getControllerSlot();
        if (!isCorrectMachinePart(rotor)) return fail("rotor");
        if (WorkingFluids.steam == null || WorkingFluids.distilledWater == null) return fail("fluids_missing");
        long rating = dynamoRating();
        if (rating <= 0) return fail("hatches");
        TurbineStatCalculator stats = new TurbineStatCalculator((MetaGeneratedTool) rotor.getItem(), rotor);
        double efficiency = Math.min(1, stats.getSteamEfficiency() * Config.rotorEfficiencyMultiplier);
        double rotorFlow = stats.getOptimalSteamFlow() * Config.rotorCapacityMultiplier;
        if (!Double.isFinite(efficiency) || efficiency <= 0 || !Double.isFinite(rotorFlow) || rotorFlow < 1) {
            return fail("invalid_value");
        }
        // "Optimal" is only a rotor capacity attribute here; there is no matching-flow requirement or penalty.
        flowLimit = (int) Math.min(1000000000, Math.floor(rotorFlow));
        fullLoadEUt = Math.min(rating, flowLimit * 0.5 * efficiency);
        flowLimit = (int) Math.min(flowLimit, Math.max(1, Math.floor(fullLoadEUt / (0.5 * efficiency))));
        int steam = (int) Math.min(flowLimit, available(WorkingFluids.steam));
        if (steam <= 0) return fail("steam");
        int water = (steam + condensationRemainder) / GTValues.STEAM_PER_WATER;
        // Reserve a water path even before the first integer litre accumulates.
        FluidEjectionHelper outputs = prepareOutputs(new FluidStack(WorkingFluids.distilledWater, Math.max(1, water)));
        if (outputs == null) return fail("output_full");
        if (water == 0) outputs = null;
        consume(WorkingFluids.steam, steam);
        if (outputs != null) outputs.commit();
        condensationRemainder = (steam + condensationRemainder) % GTValues.STEAM_PER_WATER;
        inputRate = steam;
        outputRate = water;
        generate(Math.min(fullLoadEUt, steam * 0.5 * efficiency) * startup.next(Config.turbineStartupTicks));
        // Parent checks disabled maintenance independently of its component damage path.
        // Keep its counter, random chance, damage factors and tool NBT untouched.
        if (!doRandomMaintenanceDamage() || !isCorrectMachinePart(getControllerSlot())) {
            startup.clear();
            return fail("rotor");
        }
        return true;
    }

    @Override
    public int getDamageToComponent(ItemStack stack) {
        return 1;
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setInteger("tnCondensationRemainder", condensationRemainder);
        if (rotorIdentity != null) nbt.setTag("tnRotorIdentity", rotorIdentity.copy());
        else nbt.removeTag("tnRotorIdentity");
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        condensationRemainder = Math.max(0, nbt.getInteger("tnCondensationRemainder")) % GTValues.STEAM_PER_WATER;
        rotorIdentity = nbt.hasKey("tnRotorIdentity") ? nbt.getCompoundTag("tnRotorIdentity") : null;
        rotorReference = null;
    }

    @Override
    public void setItemNBT(NBTTagCompound nbt) {
        super.setItemNBT(nbt);
        // Preserve already-consumed steam's sub-litre water entitlement, but never startup or power.
        nbt.setInteger("tnCondensationRemainder", condensationRemainder);
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
    protected boolean requiresFluidHatches() {
        return true;
    }

    @Override
    protected int startupTicks() {
        return Config.turbineStartupTicks;
    }

    @Override
    protected int decayTicks() {
        return Config.turbineDecayTicks;
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
    public String[] detailKeys() {
        return new String[] { "rotor", "steam_limit", "condensation" };
    }

    @Override
    public String[] detailValues() {
        return new String[] { rotorDurability + " / " + rotorMaxDurability, flowLimit + " L/t",
            condensationRemainder + " / " + GTValues.STEAM_PER_WATER + " L steam" };
    }
}
