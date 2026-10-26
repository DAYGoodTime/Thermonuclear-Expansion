package com.lin.thermonuclear.machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.google.common.collect.ImmutableMap;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.lin.thermonuclear.Thermonuclear;
import com.lin.thermonuclear.api.CoolantType;
import com.lin.thermonuclear.api.HeatExchangeRule;
import com.lin.thermonuclear.api.HeatExchangeRules;
import com.lin.thermonuclear.gui.HeatExchangeStationGui;
import com.lin.thermonuclear.machine.base.CycleContext;
import com.lin.thermonuclear.machine.base.TNCCycleMachineBase;
import com.lin.thermonuclear.recipe.heatexchange.DefaultHeatExchangeRules;
import com.lin.thermonuclear.recipe.heatexchange.HeatExchangeBatch;
import com.lin.thermonuclear.recipe.heatexchange.HeatExchangeRecipes;
import com.lin.thermonuclear.registry.SteamType;
import com.lin.thermonuclear.registry.WorkingFluids;

import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.IOutputHatch;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public final class MTEHeatExchangeStation extends TNCCycleMachineBase<MTEHeatExchangeStation> {

    private static final String PIECE = "heat_exchange_station";
    private static final int WIDTH = 9;
    private static final int HEIGHT = 14;
    private static final int LENGTH = 7;
    // G is at (4, 12, 1) in the scanner export. It faces the preceding slice, which is the outside.
    private static final int OFFSET_X = 4;
    private static final int OFFSET_Y = 12;
    private static final int OFFSET_Z = 1;
    private static final Casings CASING = Casings.SolidSteelMachineCasing;
    // StructureLib order: [depth][top-to-bottom row]. Empty spaces retain the scanner's skip semantics.
    private static final String[][] SHAPE = {
        { "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ",
            "         ", "         ", "         ", "         ", "         ", "DDDDDDDDD" },
        { "         ", "  DDDDD  ", "  DDDDD  ", "  DDDDD  ", "  DDDDD  ", "  DDDDD  ", "  DDDDD  ", "  DDDDD  ",
            "  DDDDD  ", "  DDDDD  ", "  DDDDD  ", "  DDDDD  ", "  D ~ D  ", "DDDDDDDDD" },
        { "     EEE ", "  DDDEDE ", "  D   DE ", "  DEE DE ", "  D  EDE ", "  D   DE ", "  D   DE ", "  DEE DE ",
            "  D  EDE ", "  D   DE ", "  DE  DE ", "  DEDDDE ", "   E   E ", "CDDDDDDDA" },
        { "         ", "  DDDDD  ", "  DE  D  ", "  D   D  ", "  D  ED  ", "  D   D  ", "  DE  D  ", "  D   D  ",
            "  D  ED  ", "  D   D  ", "  DE  D  ", "  DDDDD  ", "         ", "DDDDDDDDD" },
        { " EEE     ", " EDEDDD  ", " EDE  D  ", " ED   D  ", " ED   D  ", " ED EED  ", " EDE  D  ", " ED   D  ",
            " ED   D  ", " ED EED  ", " EDE  D  ", " EDDDED  ", " E   E   ", "BDDDDDDDF" },
        { "         ", "  DDDDD  ", "  DDDDD  ", "  DDDDD  ", "  DDDDD  ", "  DDDDD  ", "  DDDDD  ", "  DDDDD  ",
            "  DDDDD  ", "  DDDDD  ", "  DDDDD  ", "  DDDDD  ", "  D   D  ", "DDDDDDDDD" },
        { "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ",
            "         ", "         ", "         ", "         ", "         ", "DDDDDDDDD" } };
    private static final IStructureDefinition<MTEHeatExchangeStation> STRUCTURE = StructureDefinition
        .<MTEHeatExchangeStation>builder()
        .addShape(PIECE, SHAPE)
        .addElement('A', inputHatch(1, MTEHeatExchangeStation::addColdInput))
        .addElement('B', outputHatch(2, MTEHeatExchangeStation::addColdOutput))
        .addElement('C', inputHatch(3, MTEHeatExchangeStation::addHotInput))
        .addElement('D', Casings.SolidSteelMachineCasing.asElement())
        .addElement('E', Casings.BronzePipeCasing.asElement())
        .addElement('F', outputHatch(4, MTEHeatExchangeStation::addHotOutput))
        .build();

    private static IStructureElement<MTEHeatExchangeStation> inputHatch(int hint, HatchAdder adder) {
        return GTStructureUtility.buildHatchAdder(MTEHeatExchangeStation.class)
            .anyOf(HatchElement.InputHatch)
            .adder(adder::add)
            .casingIndex(CASING.textureId)
            .hint(hint)
            .exclusive()
            .build();
    }

    private static IStructureElement<MTEHeatExchangeStation> outputHatch(int hint, HatchAdder adder) {
        return GTStructureUtility.buildHatchAdder(MTEHeatExchangeStation.class)
            .anyOf(HatchElement.OutputHatch)
            .adder(adder::add)
            .casingIndex(CASING.textureId)
            .hint(hint)
            .exclusive()
            .build();
    }

    @FunctionalInterface
    private interface HatchAdder {

        boolean add(MTEHeatExchangeStation machine, IGregTechTileEntity tile, int texture);
    }

    private final List<MTEHatch> coldInputs = new ArrayList<>();
    private final List<MTEHatch> hotInputs = new ArrayList<>();
    private final List<IOutputHatch> coldOutputs = new ArrayList<>();
    private final List<IOutputHatch> hotOutputs = new ArrayList<>();

    private boolean addColdInput(IGregTechTileEntity tile, int texture) {
        return addGrouped(tile, coldInputs, texture);
    }

    private boolean addHotInput(IGregTechTileEntity tile, int texture) {
        return addGrouped(tile, hotInputs, texture);
    }

    private boolean addColdOutput(IGregTechTileEntity tile, int texture) {
        return addOutputGrouped(tile, coldOutputs, texture);
    }

    private boolean addHotOutput(IGregTechTileEntity tile, int texture) {
        return addOutputGrouped(tile, hotOutputs, texture);
    }

    private boolean addGrouped(IGregTechTileEntity tile, List<MTEHatch> group, int texture) {
        boolean added = addInputHatchToMachineList(tile, texture);
        if (added && tile.getMetaTileEntity() instanceof MTEHatch hatch) group.add(hatch);
        return added;
    }

    private boolean addOutputGrouped(IGregTechTileEntity tile, List<IOutputHatch> group, int texture) {
        boolean added = addOutputHatchToMachineList(tile, texture);
        if (added && tile.getMetaTileEntity() instanceof IOutputHatch hatch) group.add(hatch);
        return added;
    }

    @Override
    public IStructureDefinition<MTEHeatExchangeStation> getStructureDefinition() {
        return STRUCTURE;
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        if (!checkPiece(PIECE, OFFSET_X, OFFSET_Y, OFFSET_Z, errors)) return;
        if (coldInputs.isEmpty() || hotInputs.isEmpty())
            errors.add(StructureErrors.missingHatch(HatchElement.InputHatch));
        if (coldOutputs.isEmpty() || hotOutputs.isEmpty())
            errors.add(StructureErrors.missingHatch(HatchElement.OutputHatch));
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

    @Override
    protected Casings casing() {
        return CASING;
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        coldInputs.clear();
        hotInputs.clear();
        coldOutputs.clear();
        hotOutputs.clear();
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType(StatCollector.translateToLocal(nameKey()))
            .addMarkdown(
                new ResourceLocation(Thermonuclear.MODID, "heat-exchange-station"),
                ImmutableMap.<String, Object>builder()
                    .put("ic2_coefficient", decimal(DefaultHeatExchangeRules.IC2_COOLANT.steamPerHotCoolant()))
                    .put("super_coefficient", decimal(DefaultHeatExchangeRules.SUPER_COOLANT.steamPerHotCoolant()))
                    .put(
                        "nak_coefficient",
                        decimal(DefaultHeatExchangeRules.NAK_COMPOSITE_COOLANT.steamPerHotCoolant()))
                    .put("steam_per_water", NumberFormatUtil.formatNumber(HeatExchangeBatch.STEAM_PER_WATER))
                    .build())
            .beginStructureBlock(WIDTH, HEIGHT, LENGTH, true)
            .addController(StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.controller"))
            .addCasing("253", StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.casings"), false)
            .addCasing("55", StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.pipes"), false)
            .addInputHatch("1", StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.cold_input"), 1)
            .addOutputHatch("1", StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.cold_output"), 2)
            .addInputHatch("1", StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.hot_input"), 3)
            .addOutputHatch("1", StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.hot_output"), 4)
            .toolTipFinisher();
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new HeatExchangeStationGui(this);
    }

    private HeatExchangeRule selected;
    private final Map<String, Double> steamRemainders = new HashMap<>();
    private int waterCredit;
    private double waterRate;

    public MTEHeatExchangeStation(int id, String name, String regional) {
        super(id, name, regional);
    }

    public MTEHeatExchangeStation(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MTEHeatExchangeStation(mName);
    }

    @Override
    protected boolean processCycle(CycleContext context) {
        if (WorkingFluids.distilledWater == null) return fail("fluids_missing");
        selected = selectRecipe();
        if (selected == null) return fail("hot_fluid");
        HeatExchangeRule recipe = selected;
        CoolantType coolant = recipe.coolant();
        SteamType steamType = recipe.steam();
        long waterAvailable = available(WorkingFluids.distilledWater, coldInputs);
        HeatExchangeBatch minimum = exchangeBatch(recipe, 1);
        if (minimum == null) return fail("output_full");
        if (minimum.water() > waterAvailable) return fail("water");
        int hotLimit = (int) Math.min(available(coolant.hot(), hotInputs), Integer.MAX_VALUE);
        int hot = acceptedHot(recipe, hotLimit, waterAvailable);
        if (hot == 0) return fail("output_full");
        HeatExchangeBatch batch = exchangeBatch(recipe, hot);
        int water = batch.water();
        int steam = batch.steam();
        FluidEjectionHelper coldOutput = prepareOutputs(coldOutputs, new FluidStack(coolant.cold(), hot));
        FluidEjectionHelper steamOutput = steam > 0
            ? prepareOutputs(hotOutputs, new FluidStack(steamType.fluid(), steam))
            : null;
        if (coldOutput == null || (steam > 0 && steamOutput == null)) return fail("output_full");
        // All capacity reservations and both input checks precede any real mutation.
        context.deferInput(() -> {
            consume(coolant.hot(), hot, hotInputs);
            if (water > 0) consume(WorkingFluids.distilledWater, water, coldInputs);
        });
        context.onCommit(coldOutput::commit);
        if (steam > 0) context.onCommit(steamOutput::commit);
        final String steamId = steamType.id();
        context.onCommit(() -> {
            waterCredit = batch.waterCredit();
            steamRemainders.put(steamId, batch.steamRemainder());
        });
        inputLitresPerTick = hot / (double) CYCLE_TICKS;
        waterRate = water / (double) CYCLE_TICKS;
        outputLitresPerTick = steam / (double) CYCLE_TICKS;
        return true;
    }

    private HeatExchangeRule selectRecipe() {
        // Keep the previous hot fluid while available; never pool different fluids within one cycle.
        if (selected != null && hasHotFluid(selected)) return selected;
        for (HeatExchangeRule rule : HeatExchangeRules.getAllRules()) {
            if (hasHotFluid(rule)) return rule;
        }
        return null;
    }

    private boolean hasHotFluid(HeatExchangeRule rule) {
        CoolantType coolant = rule.coolant();
        return available(coolant.hot(), hotInputs) >= 1;
    }

    private HeatExchangeBatch exchangeBatch(HeatExchangeRule recipe, int hot) {
        SteamType steam = recipe.steam();
        return HeatExchangeBatch
            .forHot(hot, recipe.steamPerHotCoolant(), steamRemainders.getOrDefault(steam.id(), 0.0), waterCredit);
    }

    private int acceptedHot(HeatExchangeRule recipe, int limit, long waterAvailable) {
        CoolantType coolant = recipe.coolant();
        Fluid coldFluid = coolant.cold();
        Fluid steamFluid = recipe.steam()
            .fluid();
        int lower = 0;
        int upper = limit;
        while (lower < upper) {
            int hot = lower + (int) (((long) upper - lower + 1) / 2);
            HeatExchangeBatch batch = exchangeBatch(recipe, hot);
            // Each probe has independent reservations. Only the final batch is committed.
            boolean fits = batch != null && batch.water() <= waterAvailable
                && prepareOutputs(coldOutputs, new FluidStack(coldFluid, hot)) != null
                && (batch.steam() == 0
                    || prepareOutputs(hotOutputs, new FluidStack(steamFluid, batch.steam())) != null);
            if (fits) lower = hot;
            else upper = hot - 1;
        }
        return lower;
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setString("tnExchangeRecipe", selected == null ? "" : selected.id());
        saveExchangeCredit(nbt);
        nbt.setDouble("tnExchangeWaterRate", waterRate);
    }

    @Override
    public void setItemNBT(NBTTagCompound nbt) {
        super.setItemNBT(nbt);
        saveExchangeCredit(nbt);
    }

    private void saveExchangeCredit(NBTTagCompound nbt) {
        nbt.setInteger("tnExchangeWaterCredit", waterCredit);
        for (SteamType steam : SteamType.values()) {
            nbt.setDouble("tnExchangeSteamRemainder_" + steam.id(), steamRemainders.getOrDefault(steam.id(), 0.0));
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        int savedCredit = nbt.getInteger("tnExchangeWaterCredit");
        waterCredit = savedCredit >= 0 && savedCredit < HeatExchangeBatch.STEAM_PER_WATER ? savedCredit : 0;
        double savedRate = nbt.getDouble("tnExchangeWaterRate");
        waterRate = Double.isFinite(savedRate) && savedRate >= 0 ? savedRate : 0;
        selected = HeatExchangeRules.find(nbt.getString("tnExchangeRecipe"));
        steamRemainders.clear();
        // Old steam modes no longer select a path. Keep each steam's credit without converting ordinary to superheated.
        for (SteamType steam : SteamType.values()) {
            double remainder = nbt.getDouble("tnExchangeSteamRemainder_" + steam.id());
            if (Double.isFinite(remainder) && remainder >= 0 && remainder < 1) {
                steamRemainders.put(steam.id(), remainder);
            }
        }
    }

    @Override
    protected boolean usesItemBusses() {
        return false;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return HeatExchangeRecipes.DISPLAY;
    }

    @Override
    protected boolean usesDynamo() {
        return false;
    }

    @Override
    protected int startupTicks() {
        return 0;
    }

    @Override
    protected int decayTicks() {
        return 1;
    }

    @Override
    public String nameKey() {
        return "thermonuclear.machine.heat_exchange.name";
    }

    @Override
    public Map<String, String> displayInfo() {
        Map<String, String> info = commonInfo();
        info.put(
            "steam_type",
            selected == null ? "thermonuclear.recipe.none"
                : selected.steam()
                    .translationKey());
        info.put(
            "recipe",
            selected == null ? "thermonuclear.recipe.none"
                : selected.coolant()
                    .nameKey());
        if (selected != null) info.put("steam_multiplier", decimal(selected.steamPerHotCoolant()) + " L/L");
        info.put("hot_input", decimalPerCycle(inputLitresPerTick));
        info.put("water_input", decimalPerCycle(inputLitresPerTick > 0 ? waterRate : 0));
        info.put("steam_output", decimalPerCycle(outputLitresPerTick));
        info.put("coolant_return", decimalPerCycle(inputLitresPerTick));
        if (mMaxProgresstime > 0) info.put("cycle", decimal(mProgresstime / (double) CYCLE_TICKS) + " / 1");
        return info;
    }

}
