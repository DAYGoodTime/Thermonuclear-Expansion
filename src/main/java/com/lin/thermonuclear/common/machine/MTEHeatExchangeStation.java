package com.lin.thermonuclear.common.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.google.common.collect.ImmutableMap;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.lin.thermonuclear.Thermonuclear;
import com.lin.thermonuclear.api.CoolantType;
import com.lin.thermonuclear.api.HeatExchangeRule;
import com.lin.thermonuclear.api.HeatExchangeRules;
import com.lin.thermonuclear.client.gui.HeatExchangeStationGui;
import com.lin.thermonuclear.common.machine.base.CycleContext;
import com.lin.thermonuclear.common.machine.base.TNCCycleMachineBase;
import com.lin.thermonuclear.common.recipe.heatexchange.DefaultHeatExchangeRules;
import com.lin.thermonuclear.common.recipe.heatexchange.HeatExchangeBatch;
import com.lin.thermonuclear.common.recipe.heatexchange.HeatExchangeRecipes;
import com.lin.thermonuclear.common.registry.SteamType;
import com.lin.thermonuclear.common.registry.WorkingFluids;

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
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public final class MTEHeatExchangeStation extends TNCCycleMachineBase<MTEHeatExchangeStation> {

    private static final String PIECE = "heat_exchange_station";
    private static final int WIDTH = 9;
    private static final int HEIGHT = 14;
    private static final int LENGTH = 7;
    private static final int OFFSET_X = 4;
    private static final int OFFSET_Y = 12;
    private static final int OFFSET_Z = 1;
    private static final Casings CASING = Casings.SolidSteelMachineCasing;
    // spotless:off
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
    // spotless:on
    private static final IStructureDefinition<MTEHeatExchangeStation> STRUCTURE = StructureDefinition
        .<MTEHeatExchangeStation>builder()
        .addShape(PIECE, SHAPE)
        .addElement('A', inputHatch(1, MTEHeatExchangeStation::addWaterInput))
        .addElement('B', outputHatch(2, MTEHeatExchangeStation::addColdOutput))
        .addElement('C', inputHatch(3, MTEHeatExchangeStation::addHotInput))
        .addElement('D', Casings.SolidSteelMachineCasing.asElement())
        .addElement('E', Casings.BronzePipeCasing.asElement())
        .addElement('F', outputHatch(4, MTEHeatExchangeStation::addSteamOutput))
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

    private final List<MTEHatch> waterInputs = new ArrayList<>();
    private final List<MTEHatch> hotInputs = new ArrayList<>();
    private final List<IOutputHatch> coldOutputs = new ArrayList<>();
    private final List<IOutputHatch> steamOutputs = new ArrayList<>();

    private boolean addWaterInput(IGregTechTileEntity tile, int texture) {
        return addGrouped(tile, waterInputs, texture);
    }

    private boolean addHotInput(IGregTechTileEntity tile, int texture) {
        return addGrouped(tile, hotInputs, texture);
    }

    private boolean addColdOutput(IGregTechTileEntity tile, int texture) {
        return addOutputGrouped(tile, coldOutputs, texture);
    }

    private boolean addSteamOutput(IGregTechTileEntity tile, int texture) {
        return addOutputGrouped(tile, steamOutputs, texture);
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
        if (waterInputs.isEmpty() || hotInputs.isEmpty())
            errors.add(StructureErrors.missingHatch(HatchElement.InputHatch));
        if (coldOutputs.isEmpty() || steamOutputs.isEmpty())
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
        waterInputs.clear();
        hotInputs.clear();
        coldOutputs.clear();
        steamOutputs.clear();
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
//            .addInputHatch("1", StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.cold_input"), 1)
//            .addOutputHatch("1", StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.cold_output"), 2)
//            .addInputHatch("1", StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.hot_input"), 3)
//            .addOutputHatch("1", StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.hot_output"), 4)
            .addStructureHint("thermonuclear.structure.heat_exchange.water",1)
            .addStructureHint("thermonuclear.structure.heat_exchange.coolant",2)
            .addStructureHint("thermonuclear.structure.heat_exchange.hot_coolant",3)
            .addStructureHint("thermonuclear.structure.heat_exchange.steam",4)
            .toolTipFinisher();
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new HeatExchangeStationGui(this);
    }

    private HeatExchangeRule selected;
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
        selected = selectRecipe();
        if (selected == null) return fail("hot_fluid");
        HeatExchangeRule recipe = selected;
        CoolantType coolant = recipe.coolant();
        SteamType steamType = recipe.steam();
        long waterAvailable = available(WorkingFluids.distilledWater, waterInputs);
        HeatExchangeBatch minimum = HeatExchangeBatch.calcUsage(1, waterAvailable, recipe);
        if (minimum == null) return fail("invalid_value");
        if (minimum.water() > waterAvailable) return fail("water");
        HeatExchangeBatch result = HeatExchangeBatch.calcUsage(available(coolant.hot(), hotInputs),
            waterAvailable, recipe);
        if (result == null) return fail("invalid_value");
        FluidEjectionHelper coldOutput = prepareOutputs(coldOutputs, new FluidStack(coolant.cold(), result.hot()));
        FluidEjectionHelper steamOutput = prepareOutputs(steamOutputs, new FluidStack(steamType.fluid(), result.steam()));
        if (coldOutput == null || steamOutput == null) return fail("output_full");
        // 准备处理输入消费
        context.deferInput(() -> {
            consume(coolant.hot(), result.hot(), hotInputs);
            consume(WorkingFluids.distilledWater, result.water(), waterInputs);
        });
        //TODO output support long
        queueFluidOutput(context, new FluidStack(coolant.cold(), result.hot()));
        queueFluidOutput(context, new FluidStack(steamType.fluid(), result.steam()));
        inputLitresPerTick = result.hot() / (double) CYCLE_TICKS;
        waterRate = result.water() / (double) CYCLE_TICKS;
        outputLitresPerTick = result.steam() / (double) CYCLE_TICKS;
        return true;
    }

    @Override
    protected boolean addFluidOutputs(FluidStack[] outputFluids) {
        boolean complete = true;
        for (int i = 0; i < outputFluids.length; i++) {
            complete &= addFluidOutputs(new FluidStack[]{outputFluids[i]}, i == 0 ? coldOutputs : steamOutputs);
        }
        return complete;
    }

    private HeatExchangeRule selectRecipe() {
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

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setString("tnExchangeRecipe", selected == null ? "" : selected.id());
    }

    @Override
    public void setItemNBT(NBTTagCompound nbt) {
        super.setItemNBT(nbt);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        selected = HeatExchangeRules.find(nbt.getString("tnExchangeRecipe"));
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
        info.put("water_input",decimal(waterRate));
        return info;
    }

}
