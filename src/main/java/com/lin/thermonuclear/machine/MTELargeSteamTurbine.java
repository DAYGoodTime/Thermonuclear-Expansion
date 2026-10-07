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
import gregtech.api.items.MetaGeneratedTool;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.TurbineStatCalculator;
import gregtech.common.blocks.ItemMachines;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.items.MetaGeneratedTool01;

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
    protected void beforeProcessingCycle() {
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
    protected boolean processCycle() {
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
        int steam = (int) Math.min(
            Math.min((long) flowLimit * CYCLE_TICKS, Integer.MAX_VALUE - condensationRemainder),
            available(WorkingFluids.steam));
        if (steam <= 0) return fail("steam");
        int water = (steam + condensationRemainder) / GTValues.STEAM_PER_WATER;
        // Reserve a water path even before the first integer litre accumulates.
        FluidEjectionHelper outputs = prepareOutputs(new FluidStack(WorkingFluids.distilledWater, Math.max(1, water)));
        if (outputs == null) return fail("output_full");
        if (water == 0) outputs = null;
        consume(WorkingFluids.steam, steam);
        if (outputs != null) commitOutput(outputs::commit);
        final int remainder = (steam + condensationRemainder) % GTValues.STEAM_PER_WATER;
        commitOutput(() -> condensationRemainder = remainder);
        inputRate = steam / (double) CYCLE_TICKS;
        outputRate = water / (double) CYCLE_TICKS;
        cycleEUt = Math.min(fullLoadEUt, steam * 0.5 * efficiency / CYCLE_TICKS);
        return true;
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (!isCorrectMachinePart(getControllerSlot())) {
            startup.clear();
            return false;
        }
        return super.onRunningTick(stack);
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (tile.isServerSide() && !isCorrectMachinePart(getControllerSlot())) startup.clear();
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
    public String[] displayKeys() {
        return new String[] { "status", "shaft_tier", "startup", "full_load", "produced", "rotor", "steam_limit",
            "steam_input", "water_output" };
    }

    @Override
    public Map<String, String> displayInfo() {
        Map<String, String> info = commonInfo();
        info.put("shaft_tier", Integer.toString(getShaftTier()));
        addStartupInfo(info);
        addGenerationInfo(info);
        info.put("rotor", rotorDurability + " / " + rotorMaxDurability);
        info.put("steam_limit", flowLimit + " L/t");
        info.put("steam_input", decimal(inputRate));
        info.put("water_output", decimal(outputRate));
        return info;
    }
}
