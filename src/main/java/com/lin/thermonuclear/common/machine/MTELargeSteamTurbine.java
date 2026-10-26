package com.lin.thermonuclear.common.machine;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableMap;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.Thermonuclear;
import com.lin.thermonuclear.api.SteamTurbineFuel;
import com.lin.thermonuclear.api.SteamTurbineFuels;
import com.lin.thermonuclear.common.block.BlockMachineComponent;
import com.lin.thermonuclear.common.block.BlockTurbineShaft;
import com.lin.thermonuclear.client.gui.LargeSteamTurbineGui;
import com.lin.thermonuclear.common.loader.BlockLoader;
import com.lin.thermonuclear.common.machine.base.CycleContext;
import com.lin.thermonuclear.common.machine.base.TNCCycleMachineBase;
import com.lin.thermonuclear.common.recipe.steamturbine.DefaultSteamTurbineFuels;
import com.lin.thermonuclear.common.recipe.steamturbine.SteamTurbineRecipes;
import com.lin.thermonuclear.common.registry.StructureChannels;
import com.lin.thermonuclear.common.registry.WorkingFluids;
import com.lin.thermonuclear.utils.SteamTurbineMath;

import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.SimpleShutDownReason;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public final class MTELargeSteamTurbine extends TNCCycleMachineBase<MTELargeSteamTurbine> {

    private static final String PIECE = "large_steam_turbine";
    private static final int OFFSET_X = SteamTurbineStructure.OFFSET_X;
    private static final int OFFSET_Y = SteamTurbineStructure.OFFSET_Y;
    private static final int OFFSET_Z = SteamTurbineStructure.OFFSET_Z;
    private static final Casings CASING = Casings.SolidSteelMachineCasing;
    private static final IStructureDefinition<MTELargeSteamTurbine> STRUCTURE = StructureDefinition
        .<MTELargeSteamTurbine>builder()
        .addShape(PIECE, SteamTurbineStructure.SHAPE)
        .addElement('A', fluidHatch(false, 1))
        .addElement('B', fluidHatch(true, 2))
        .addElement(
            'C',
            GTStructureUtility.buildHatchAdder(MTELargeSteamTurbine.class)
                .atLeast(
                    HatchElement.Dynamo.or(HatchElement.ExoticDynamo)
                        .or(HatchElement.LaserSource))
                .casingIndex(CASING.textureId)
                .hint(3)
                .buildAndChain(CASING.asElement()))
        .addElement('D', Casings.SteelGearBoxCasing.asElement())
        .addElement('E', Casings.SteelPipeCasing.asElement())
        .addElement('F', StructureUtility.ofBlock(BlockLoader.turbineBlade, 0))
        .addElement(
            'G',
            StructureChannels.TURBINE_SHAFT.use(
                StructureUtility.defer(
                    (MTELargeSteamTurbine machine) -> shaftElement(
                        BlockMachineComponent.axisMetadata(
                            machine.getExtendedFacing()
                                .getRelativeBackInWorld())))))
        .build();

    private int shaftTier;

    private static IStructureElement<MTELargeSteamTurbine> shaftElement(int axisMetadata) {
        // The 23 G positions run along local depth, perpendicular to the inlet end cap.
        return StructureUtility.<MTELargeSteamTurbine, Integer>ofBlocksTiered((block, meta) -> {
            if (meta != axisMetadata) return null;
            return block instanceof BlockTurbineShaft shaft ? shaft.tier() : null;
        },
            Arrays.stream(BlockLoader.turbineShafts())
                .map(block -> Pair.<Block, Integer>of(block, axisMetadata))
                .collect(Collectors.toList()),
            0,
            (machine, tier) -> machine.shaftTier = tier,
            machine -> machine.shaftTier);
    }

    private static IStructureElement<MTELargeSteamTurbine> fluidHatch(boolean input, int hint) {
        return GTStructureUtility.buildHatchAdder(MTELargeSteamTurbine.class)
            .anyOf(input ? HatchElement.InputHatch : HatchElement.OutputHatch)
            .casingIndex(CASING.textureId)
            .hint(hint)
            .exclusive()
            .build();
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
        if (mDynamoHatches.isEmpty() && mExoticDynamoHatches.isEmpty() && !isWirelessMode()) {
            errors.add(StructureErrors.missingHatch(HatchElement.Dynamo));
        }
        if (!errors.isEmpty()) shaftTier = 0;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(PIECE, stack, hintsOnly, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(PIECE, stack, OFFSET_X, OFFSET_Y, OFFSET_Z, elementBudget, env, false, true);
    }

    public static double litresPerTick(long litresPerCycle, int cycleTicks) {
        if (litresPerCycle <= 0 || cycleTicks <= 0) return 0;
        return litresPerCycle / (double) cycleTicks;
    }

    @Override
    protected Casings casing() {
        return CASING;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType(StatCollector.translateToLocal(nameKey()))
            .addMarkdown(
                new ResourceLocation(Thermonuclear.MODID, "large-steam-turbine"),
                ImmutableMap.<String, Object>builder()
                    .put("steam_energy", decimal(DefaultSteamTurbineFuels.STEAM.euPerLitre()))
                    .put("superheated_energy", decimal(DefaultSteamTurbineFuels.OVER_HEAT_STEAM.euPerLitre()))
                    .put("supercritical_energy", decimal(DefaultSteamTurbineFuels.SUPERCRITICAL_STEAM.euPerLitre()))
                    .put(
                        "ultra_supercritical_energy",
                        decimal(DefaultSteamTurbineFuels.ULTRA_SUPERCRITICAL_STEAM.euPerLitre()))
                    .put("startup_ticks", NumberFormatUtil.formatNumber(Config.largeSteamTurbine.turbineStartupTicks))
                    .put("decay_ticks", NumberFormatUtil.formatNumber(Config.largeSteamTurbine.turbineDecayTicks))
                    .put("steam_per_water", NumberFormatUtil.formatNumber(GTValues.STEAM_PER_WATER))
                    .put("cycle_ticks", NumberFormatUtil.formatNumber(CYCLE_TICKS))
                    .build())
            .beginStructureBlock(
                SteamTurbineStructure.WIDTH,
                SteamTurbineStructure.HEIGHT,
                SteamTurbineStructure.LENGTH,
                true)
            .addController(StatCollector.translateToLocal("thermonuclear.structure.turbine.controller"))
            .addCasing("1407", StatCollector.translateToLocal("thermonuclear.structure.turbine.blocks.0"), false)
            .addCasing("2", StatCollector.translateToLocal("thermonuclear.structure.turbine.blocks.1"), false)
            .addCasing("12", StatCollector.translateToLocal("thermonuclear.structure.turbine.blocks.2"), false)
            .addCasing("700", StatCollector.translateToLocal("thermonuclear.structure.turbine.blocks.3"), false)
            .addCasing("23", StatCollector.translateToLocal("thermonuclear.structure.turbine.shafts"), true)
            .addStructureHint("thermonuclear.structure.turbine.casing_hint", 3)
            .addInputHatch("1", StatCollector.translateToLocal("thermonuclear.structure.turbine.input.location"), 2)
            .addOutputHatch("1", StatCollector.translateToLocal("thermonuclear.structure.turbine.output.location"), 1)
            .addDynamoHatch(
                StatCollector.translateToLocal("thermonuclear.structure.turbine.dynamo.quantity"),
                StatCollector.translateToLocal("thermonuclear.structure.turbine.dynamo.location"),
                3)
            .addStructureInfo("")
            .addSubChannel(StructureChannels.TURBINE_SHAFT)
            .toolTipFinisher();
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new LargeSteamTurbineGui(this);
    }

    private int condensationRemainderLitres;
    private long effectiveSteamLimitLitresPerCycle;
    private long steamLimitLitresPerCycle = Long.MAX_VALUE;
    private SteamTurbineFuel currentFuel;

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
    protected boolean processCycle(CycleContext context) {
        currentFuel = null;
        if (!checkWirelessOwner()) return false;
        if (WorkingFluids.distilledWater == null) return fail("fluids_missing");
        if (steamLimitLitresPerCycle == 0) return fail("steam_limit_zero");
        boolean unsupportedSteam = false;
        for (SteamTurbineFuel fuel : SteamTurbineFuels.getAllFuels()) {
            if (fuel.getWorkingFluid() != null && available(fuel.getWorkingFluid()) > 0) {
                if (!fuel.supportShaftTier(shaftTier)) {
                    unsupportedSteam = true;
                    continue;
                }
                currentFuel = fuel;
                break;
            }
        }
        if (currentFuel == null) return fail(unsupportedSteam ? "shaft_steam_unsupported" : "steam");
        double effectiveEUPerLitre = currentFuel.euPerLitre();
        effectiveSteamLimitLitresPerCycle = SteamTurbineMath
            .steamLimitLitresPerCycle(steamLimitLitresPerCycle, GTValues.STEAM_PER_WATER, condensationRemainderLitres);
        double fullLoadSteamLitresPerTick = litresPerTick(effectiveSteamLimitLitresPerCycle, CYCLE_TICKS);
        fullLoadEUt = SteamTurbineMath.generationEUt(fullLoadSteamLitresPerTick, effectiveEUPerLitre);
        long steamLitresPerCycle = Math
            .min(effectiveSteamLimitLitresPerCycle, available(currentFuel.getWorkingFluid()));
        if (steamLitresPerCycle <= 0) return fail("steam");
        int waterLitresPerCycle = (int) ((steamLitresPerCycle + condensationRemainderLitres)
            / GTValues.STEAM_PER_WATER);
        // Reserve a water path even before the first integer litre accumulates.
        FluidEjectionHelper outputs = prepareOutputs(
            new FluidStack(WorkingFluids.distilledWater, Math.max(1, waterLitresPerCycle)));
        if (outputs == null) return fail("output_full");
        if (waterLitresPerCycle == 0) outputs = null;
        context.deferInput(() -> consume(currentFuel.getWorkingFluid(), steamLitresPerCycle));
        if (outputs != null) context.onCommit(outputs::commit);
        final int remainingSteamLitres = (int) ((steamLitresPerCycle + condensationRemainderLitres)
            % GTValues.STEAM_PER_WATER);
        context.onCommit(() -> condensationRemainderLitres = remainingSteamLitres);
        // Integer batches stay at the fluid transaction boundary; all operating rates use ticks.
        inputLitresPerTick = litresPerTick(steamLitresPerCycle, CYCLE_TICKS);
        outputLitresPerTick = litresPerTick(waterLitresPerCycle, CYCLE_TICKS);
        operatingEUt = SteamTurbineMath.generationEUt(inputLitresPerTick, effectiveEUPerLitre);
        return true;
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        // A structure downgrade must not resume prepaid ultra-supercritical generation on low-pressure shafts.
        if (currentFuel != null && !currentFuel.supportShaftTier(shaftTier)) {
            stopMachine(SimpleShutDownReason.ofCritical("thermonuclear.status.shaft_steam_unsupported"));
            return fail("shaft_steam_unsupported");
        }
        return super.onRunningTick(stack);
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setInteger("tnCondensationRemainder", condensationRemainderLitres);
        nbt.setLong("tnSteamLimitPerCycle", steamLimitLitresPerCycle);
        nbt.setLong("tnTurbineCycleLimit", effectiveSteamLimitLitresPerCycle);
        if (currentFuel != null) nbt.setString("tnTurbineFuel", currentFuel.nameKey());
        else nbt.removeTag("tnTurbineFuel");
        nbt.removeTag("tnRotorIdentity");
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        condensationRemainderLitres = Math.max(0, nbt.getInteger("tnCondensationRemainder")) % GTValues.STEAM_PER_WATER;
        steamLimitLitresPerCycle = nbt.hasKey("tnSteamLimitPerCycle") ? Math.max(0, nbt.getLong("tnSteamLimitPerCycle"))
            : Long.MAX_VALUE;
        effectiveSteamLimitLitresPerCycle = SteamTurbineMath.steamLimitLitresPerCycle(
            nbt.getLong("tnTurbineCycleLimit"),
            GTValues.STEAM_PER_WATER,
            condensationRemainderLitres);
        currentFuel = null;
        for (SteamTurbineFuel fuel : SteamTurbineFuels.getAllFuels()) {
            if (fuel.nameKey()
                .equals(nbt.getString("tnTurbineFuel"))) currentFuel = fuel;
        }
    }

    @Override
    public void setItemNBT(NBTTagCompound nbt) {
        super.setItemNBT(nbt);
        // Preserve already-consumed steam's sub-litre water entitlement, but never startup or power.
        nbt.setInteger("tnCondensationRemainder", condensationRemainderLitres);
        nbt.setLong("tnSteamLimitPerCycle", steamLimitLitresPerCycle);
    }

    public long getSteamLimitLitresPerCycle() {
        return steamLimitLitresPerCycle;
    }

    public void setSteamLimitFromGui(String text) {
        if (getBaseMetaTileEntity() == null || !getBaseMetaTileEntity().isServerSide()) return;
        long limitLitresPerCycle = SteamTurbineMath.parseLimit(text, steamLimitLitresPerCycle);
        if (limitLitresPerCycle != steamLimitLitresPerCycle) {
            // Already-prepaid generation is unchanged; the new cap starts with the next cycle.
            steamLimitLitresPerCycle = limitLitresPerCycle;
            markDirty();
        }
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return SteamTurbineRecipes.SteamTurbineDisplayMap;
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
        return Config.largeSteamTurbine.turbineStartupTicks;
    }

    @Override
    protected int decayTicks() {
        return Config.largeSteamTurbine.turbineDecayTicks;
    }

    @Override
    public String nameKey() {
        return "thermonuclear.machine.steam_turbine.name";
    }

    @Override
    public Map<String, String> displayInfo() {
        Map<String, String> info = commonInfo();
        info.put("shaft_tier", Integer.toString(getShaftTier()));
        addStartupInfo(info);
        addGenerationInfo(info);
        info.put("turbine_steam_type", currentFuel == null ? "thermonuclear.recipe.none" : currentFuel.nameKey());
        info.put("steam_cycle_limit", Long.toString(steamLimitLitresPerCycle));
        info.put("steam_input", decimalPerCycle(inputLitresPerTick));
        info.put("water_output", decimalPerCycle(outputLitresPerTick));
        return info;
    }
}
