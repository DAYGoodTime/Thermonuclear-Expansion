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
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.lin.thermonuclear.Config;
import com.lin.thermonuclear.gui.HeatExchangeStationGui;
import com.lin.thermonuclear.recipe.HeatExchangeRecipe;
import com.lin.thermonuclear.recipe.HeatExchangeSteam;
import com.lin.thermonuclear.registry.WorkingFluids;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.INEIPreviewModifier;
import gregtech.api.interfaces.IOutputHatch;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.blocks.ItemMachines;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public final class MTEHeatExchangeStation extends ThermonuclearMultiblockBase<MTEHeatExchangeStation>
    implements INEIPreviewModifier {

    private static final String PIECE = "heat_exchange_station";
    private static final int WIDTH = 9;
    private static final int HEIGHT = 14;
    private static final int LENGTH = 7;
    private static final int CHUNK_RADIUS = WIDTH - 1;
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
            " ED   D  ", " ED EED  ", " EDE  D  ", " EDDDED  ", " E   E   ", "FDDDDDDDB" },
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
        return StructureUtility.ofChain(
            GTStructureUtility.buildHatchAdder(MTEHeatExchangeStation.class)
                .anyOf(HatchElement.InputHatch)
                .adder(adder::add)
                .casingIndex(CASING.textureId)
                .hint(hint)
                .build(),
            hatchPreviewPlacement(true));
    }

    private static IStructureElement<MTEHeatExchangeStation> outputHatch(int hint, HatchAdder adder) {
        return StructureUtility.ofChain(
            GTStructureUtility.buildHatchAdder(MTEHeatExchangeStation.class)
                .anyOf(HatchElement.OutputHatch)
                .adder(adder::add)
                .casingIndex(CASING.textureId)
                .hint(hint)
                .build(),
            hatchPreviewPlacement(false));
    }

    private static IStructureElement<MTEHeatExchangeStation> hatchPreviewPlacement(boolean input) {
        return new IStructureElement<>() {

            @Override
            public boolean check(MTEHeatExchangeStation machine, World world, int x, int y, int z) {
                return false;
            }

            @Override
            public boolean spawnHint(MTEHeatExchangeStation machine, World world, int x, int y, int z,
                ItemStack trigger) {
                return false;
            }

            @Override
            public boolean placeBlock(MTEHeatExchangeStation machine, World world, int x, int y, int z,
                ItemStack trigger) {
                EntityPlayer player = machine.previewPlayer;
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
                    tile.setFrontFacing(
                        machine.getExtendedFacing()
                            .getRelativeForwardInWorld());
                    if (tile.getMetaTileEntity() instanceof MTEHatch hatch) hatch.updateTexture(CASING.textureId);
                }
                return true;
            }
        };
    }

    @FunctionalInterface
    private interface HatchAdder {

        boolean add(MTEHeatExchangeStation machine, IGregTechTileEntity tile, int texture);
    }

    private final List<MTEHatch> coldInputs = new ArrayList<>();
    private final List<MTEHatch> hotInputs = new ArrayList<>();
    private final List<IOutputHatch> coldOutputs = new ArrayList<>();
    private final List<IOutputHatch> hotOutputs = new ArrayList<>();
    private boolean constructing;
    private boolean previewConstruction;
    private EntityPlayer previewPlayer;

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
    public void clearHatches() {
        super.clearHatches();
        coldInputs.clear();
        hotInputs.clear();
        coldOutputs.clear();
        hotOutputs.clear();
    }

    @Override
    protected int structureChunkRadius() {
        return CHUNK_RADIUS;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return machineTooltip().beginStructureBlock(WIDTH, HEIGHT, LENGTH, true)
            .addController(StatCollector.translateToLocal("thermonuclear.structure.controller"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.box"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.cold_input"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.cold_output"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.hot_input"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.heat_exchange.hot_output"))
            .addStructureInfo(StatCollector.translateToLocal("thermonuclear.structure.hatches"))
            .addStructureHint("thermonuclear.structure.heat_exchange.cold_input", 1)
            .addStructureHint("thermonuclear.structure.heat_exchange.cold_output", 2)
            .addStructureHint("thermonuclear.structure.heat_exchange.hot_input", 3)
            .addStructureHint("thermonuclear.structure.heat_exchange.hot_output", 4)
            .toolTipFinisher();
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new HeatExchangeStationGui(this);
    }

    private HeatExchangeRecipe selected;
    private HeatExchangeSteam selectedSteam = HeatExchangeSteam.ORDINARY;
    private final double[] steamRemainders = new double[HeatExchangeSteam.values().length];
    private double throughputRemainder;

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
    protected boolean processCycle() {
        if (WorkingFluids.distilledWater == null) return fail("fluids_missing");
        HeatExchangeRecipe recipe = null;
        // Selection is per cycle. Never pool the heat or consume the second hot fluid.
        if (selected != null && selected.hot() != null
            && selected.cold() != null
            && available(selected.hot(), hotInputs) >= selected.hotPerWater()) recipe = selected;
        if (recipe == null) {
            for (HeatExchangeRecipe candidate : HeatExchangeRecipe.values()) {
                if (candidate.hot() != null && candidate.cold() != null
                    && available(candidate.hot(), hotInputs) >= candidate.hotPerWater()) {
                    recipe = candidate;
                    break;
                }
            }
        }
        if (recipe == null) {
            selected = null;
            return fail("hot_fluid");
        }
        if (selected != recipe) throughputRemainder = 0;
        selected = recipe;
        if (selectedSteam.fluid() == null) return fail("fluids_missing");
        if (available(WorkingFluids.distilledWater, coldInputs) <= 0) return fail("water");
        double budget = Config.exchangeHotFluidPerCycle * (double) CYCLE_TICKS / Config.exchangeCycleTicks
            + throughputRemainder;
        long hotLimit = Math.min(available(recipe.hot(), hotInputs), (long) Math.floor(budget));
        double steamMultiplier = recipe.steamPerHotCoolant(selectedSteam);
        long maximumHotForSteam = (long) Math
            .floor((Integer.MAX_VALUE - steamRemainders[selectedSteam.ordinal()]) / steamMultiplier);
        int water = (int) Math.min(
            Math.min(hotLimit / recipe.hotPerWater(), available(WorkingFluids.distilledWater, coldInputs)),
            Math.min(maximumHotForSteam / recipe.hotPerWater(), Integer.MAX_VALUE / recipe.hotPerWater()));
        int hot = water * recipe.hotPerWater();
        double steamTotal = hot * steamMultiplier + steamRemainders[selectedSteam.ordinal()];
        int steam = (int) Math.floor(steamTotal);
        FluidEjectionHelper coldOutput = prepareOutputs(
            coldOutputs,
            new FluidStack(recipe.cold(), Math.max(recipe.hotPerWater(), hot)));
        FluidEjectionHelper steamOutput = steam > 0
            ? prepareOutputs(hotOutputs, new FluidStack(selectedSteam.fluid(), steam))
            : null;
        if (coldOutput == null || (steam > 0 && steamOutput == null)) return fail("output_full");
        final double remainder = budget % recipe.hotPerWater();
        final double steamRemainder = steamTotal - steam;
        final int steamIndex = selectedSteam.ordinal();
        commitOutput(() -> {
            throughputRemainder = remainder;
            steamRemainders[steamIndex] = steamRemainder;
        });
        if (water == 0) return true;
        // All capacity reservations and both input checks precede any real mutation.
        consume(recipe.hot(), hot, hotInputs);
        consume(WorkingFluids.distilledWater, water, coldInputs);
        commitOutput(coldOutput::commit);
        if (steam > 0) commitOutput(steamOutput::commit);
        inputRate = hot / (double) CYCLE_TICKS;
        outputRate = steam / (double) CYCLE_TICKS;
        return true;
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setDouble("tnExchangeThroughputRemainder", throughputRemainder);
        nbt.setString("tnExchangeRecipe", selected == null ? "" : selected.id());
        nbt.setString("tnExchangeSteam", selectedSteam.id());
        for (HeatExchangeSteam steam : HeatExchangeSteam.values()) {
            nbt.setDouble("tnExchangeSteamRemainder_" + steam.id(), steamRemainders[steam.ordinal()]);
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        selected = null;
        for (HeatExchangeRecipe recipe : HeatExchangeRecipe.values()) {
            if (recipe.id()
                .equals(nbt.getString("tnExchangeRecipe"))) selected = recipe;
        }
        selectedSteam = HeatExchangeSteam.ORDINARY;
        for (HeatExchangeSteam steam : HeatExchangeSteam.values()) {
            if (steam.id()
                .equals(nbt.getString("tnExchangeSteam"))) selectedSteam = steam;
            double remainder = nbt.getDouble("tnExchangeSteamRemainder_" + steam.id());
            steamRemainders[steam.ordinal()] = Double.isFinite(remainder) && remainder >= 0 && remainder < 1 ? remainder
                : 0;
        }
        throughputRemainder = nbt.getDouble("tnExchangeThroughputRemainder");
        if (!Double.isFinite(throughputRemainder) || selected == null
            || throughputRemainder < 0
            || throughputRemainder >= selected.hotPerWater()) throughputRemainder = 0;
    }

    @Override
    protected boolean usesItemBusses() {
        return false;
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
    protected String machineKind() {
        return "heat_exchange";
    }

    @Override
    public String[] displayKeys() {
        return new String[] { "status", "steam_type", "recipe", "steam_multiplier", "throughput", "hot_input",
            "water_input", "steam_output", "coolant_return", "cycle" };
    }

    @Override
    public Map<String, String> displayInfo() {
        Map<String, String> info = commonInfo();
        info.put("steam_type", selectedSteam.translationKey());
        info.put("recipe", selected == null ? "thermonuclear.recipe.none" : selected.translationKey());
        if (selected != null) {
            info.put("steam_multiplier", decimal(selected.steamPerHotCoolant(selectedSteam)) + " L/L");
        }
        info.put("throughput", decimal(Config.exchangeHotFluidPerCycle / (double) Config.exchangeCycleTicks) + " L/t");
        info.put("hot_input", decimal(inputRate));
        info.put("water_input", decimal(selected == null ? 0 : inputRate / selected.hotPerWater()));
        info.put("steam_output", decimal(outputRate));
        info.put("coolant_return", decimal(inputRate));
        if (mMaxProgresstime > 0) info.put("cycle", mProgresstime + " / " + CYCLE_TICKS);
        return info;
    }

    public boolean requestSteamChange() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null || !tile.isServerSide() || tile.isAllowedToWork() || running) return false;
        selectedSteam = HeatExchangeSteam.values()[(selectedSteam.ordinal() + 1) % HeatExchangeSteam.values().length];
        markDirty();
        return true;
    }
}
