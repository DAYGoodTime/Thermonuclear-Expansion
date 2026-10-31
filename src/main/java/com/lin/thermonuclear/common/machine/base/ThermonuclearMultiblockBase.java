package com.lin.thermonuclear.common.machine.base;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.lin.thermonuclear.api.TNCItemList;
import com.lin.thermonuclear.utils.MathUtil;

import gregtech.GTMod;
import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTClientPreference;
import gregtech.api.util.GTUtility;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.common.blocks.ItemMachines;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.tileentities.machines.IDualInputHatch;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.overlay.tooltiprenderers.TTRenderStack;

public abstract class ThermonuclearMultiblockBase<T extends ThermonuclearMultiblockBase<T>>
    extends MTEEnhancedMultiBlockBase<T> implements ISurvivalConstructable {

    protected boolean running;
    protected String status = "stopped";
    protected long producedEUt;
    private boolean wirelessUpgrade;
    private UUID wirelessOwner;
    private final List<MTEHatch> energyDynamo = new ArrayList<>();
    private final Set<IDualInputHatch> fluidOnlyDualInputs = Collections.newSetFromMap(new IdentityHashMap<>());
    private ItemStack[] displayItems;
    private FluidStack[] displayFluids;
    private OutputDisplaySnapshot outputDisplay = OutputDisplaySnapshot.EMPTY;

    protected ThermonuclearMultiblockBase(int id, String name, String regional) {
        super(id, name, regional);
    }

    protected ThermonuclearMultiblockBase(String name) {
        super(name);
    }

    @Override
    public abstract int survivalConstruct(ItemStack stack, int elementBudget, ISurvivalBuildEnvironment env);

    protected abstract boolean usesItemBusses();

    protected abstract boolean usesDynamo();

    public final boolean isWirelessMode() {
        return usesDynamo() && wirelessUpgrade && mDynamoHatches.isEmpty() && mExoticDynamoHatches.isEmpty();
    }

    private UUID wirelessOwner() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        return tile == null ? null : tile.getOwnerUuid();
    }

    protected final boolean checkWirelessOwner() {
        return !isWirelessMode() || wirelessOwner() != null || fail("wireless_owner_missing");
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity tile, EntityPlayer player) {
        if (!tile.isServerSide()) return true;
        ItemStack held = player.getHeldItem();
        if (!usesDynamo() || !TNCItemList.WirelessUpgradeChip.isStackEqual(held)) {
            return super.onRightclick(tile, player);
        }
        if (wirelessUpgrade) {
            player.addChatComponentMessage(new ChatComponentTranslation("thermonuclear.chat.wireless.already"));
            return true;
        }
        if (!player.capabilities.isCreativeMode) {
            held.stackSize--;
            if (held.stackSize == 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            player.inventory.markDirty();
        }
        wirelessUpgrade = true;
        markDirty();
        // The turbine may currently be unformed solely because its dynamo is missing.
        onMachineBlockUpdate();
        player.addChatComponentMessage(new ChatComponentTranslation("thermonuclear.chat.wireless.installed"));
        return true;
    }

    public abstract String nameKey();

    public abstract Map<String, String> displayInfo();

    public final OutputDisplaySnapshot outputDisplaySnapshot() {
        // Cycle commits replace these arrays; native completion consumes and clears them in the same tick.
        if (displayItems != mOutputItems || displayFluids != mOutputFluids) {
            outputDisplay = OutputDisplaySnapshot.capture(mOutputItems, mOutputFluids);
            displayItems = mOutputItems;
            displayFluids = mOutputFluids;
        }
        return outputDisplay;
    }

    private void clearOutputDisplay() {
        displayItems = null;
        displayFluids = null;
        outputDisplay = OutputDisplaySnapshot.EMPTY;
    }

    @Override
    protected void outputAfterRecipe() {
        super.outputAfterRecipe();
        clearOutputDisplay();
    }

    @Override
    public void stopMachine(ShutDownReason reason) {
        super.stopMachine(reason);
        clearOutputDisplay();
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(nameKey());
    }

    protected abstract Casings casing();

    @Override
    protected void onStructureCheckFinished(IGregTechTileEntity tile) {
        super.onStructureCheckFinished(tile);
        energyDynamo.clear();
        energyDynamo.addAll(mDynamoHatches);
        energyDynamo.addAll(mExoticDynamoHatches);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = casing().getCasingTexture();
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing, TextureFactory.builder()
            .addIcon(
                active ? Textures.BlockIcons.OVERLAY_FRONT_LARGE_BOILER_ACTIVE
                    : Textures.BlockIcons.OVERLAY_FRONT_LARGE_BOILER)
            .extFacing()
            .build() };
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return null;
    }

    @Override
    public boolean shouldCheckMaintenance() {
        return false;
    }

    @Override
    public int getRepairStatus() {
        return getIdealStatus();
    }

    @Override
    public void checkMaintenance() {
        fixAllIssues();
    }

    protected final boolean hasFluidInputs() {
        return !mInputHatches.isEmpty() || mDualInputHatches.stream()
            .anyMatch(IDualInputHatch::supportsFluids);
    }

    protected static boolean isFluidInputCandidate(IMetaTileEntity hatch) {
        if (hatch instanceof IDualInputHatch dual) return hatch instanceof MTEHatch && dual.supportsFluids();
        return hatch != null && HatchElement.InputHatch.matchesHatch(hatch)
            && !HatchElement.InputHatch.mteBlacklist()
                .contains(hatch.getClass());
    }

    protected static boolean isFluidInputItem(ItemStack stack) {
        return isFluidInputCandidate(ItemMachines.getMetaTileEntity(stack));
    }

    @Override
    public boolean addInputHatchToMachineList(IGregTechTileEntity tile, int texture) {
        if (tile == null || !isFluidInputCandidate(tile.getMetaTileEntity())) return false;
        if (!super.addInputHatchToMachineList(tile, texture)) return false;
        if (tile.getMetaTileEntity() instanceof IDualInputHatch dual) fluidOnlyDualInputs.add(dual);
        return true;
    }

    protected final boolean isFluidOnlyDualInput(IDualInputHatch hatch) {
        return fluidOnlyDualInputs.contains(hatch);
    }

    protected final int fluidInputHatchCount() {
        return mInputHatches.size() + fluidOnlyDualInputs.size();
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        fluidOnlyDualInputs.clear();
    }

    protected boolean fail(String reason) {
        status = reason;
        return false;
    }

    protected long dynamoRating() {
        long total = 0;
        for (MTEHatch hatch : energyDynamo) {
            if (!hatch.isValid()) continue;
            total = MathUtil.saturatingAdd(total, rating(hatch));
        }
        return total;
    }

    private static long rating(MTEHatch hatch) {
        long volts = Math.max(0, hatch.maxEUOutput());
        long amps = Math.max(0, hatch.maxAmperesOut());
        return amps == 0 ? 0 : volts > Long.MAX_VALUE / amps ? Long.MAX_VALUE : volts * amps;
    }

    protected void generate(long euPerOperation) {
        if (euPerOperation <= 0) return;
        producedEUt = euPerOperation;
        if (isWirelessMode()) {
            UUID owner = wirelessOwner();
            if (owner != null && !owner.equals(wirelessOwner)) {
                WirelessNetworkManager.strongCheckOrAddUser(owner);
                wirelessOwner = owner;
            }
            if (owner != null) WirelessNetworkManager.addEUToGlobalEnergyMap(owner, euPerOperation);
            return;
        }
        long remaining = euPerOperation;
        // 无脑灌入全部能源仓
        for (MTEHatch hatch : energyDynamo) {
            if (!hatch.isValid()) continue;
            IGregTechTileEntity base = hatch.getBaseMetaTileEntity();
            if (base == null) continue;
            long stored = Math.max(0, base.getStoredEU());
            long capacity = base.getEUCapacity();
            if (stored >= capacity) continue;
            long amount = Math.min(remaining, Math.min(rating(hatch), capacity - stored));
            if (amount > 0 && base.increaseStoredEnergyUnits(amount, false)) {
                remaining -= amount;
                if (remaining == 0) break;
            }
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setBoolean("tnWirelessUpgrade", wirelessUpgrade);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        clearOutputDisplay();
        wirelessUpgrade = usesDynamo() && nbt.getBoolean("tnWirelessUpgrade");
        wirelessOwner = null;
    }

    @Override
    public void setItemNBT(NBTTagCompound nbt) {
        super.setItemNBT(nbt);
        nbt.setBoolean("tnWirelessUpgrade", wirelessUpgrade);
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        // GT .264 serializes every pending fragment in super. Supply its scalar contract directly instead.
        tag.setBoolean("hasProblems", getRepairStatus() < getIdealStatus());
        tag.setFloat("efficiency", mEfficiency / 100.0F);
        tag.setInteger("progress", mProgresstime);
        tag.setInteger("maxProgress", mMaxProgresstime);
        tag.setBoolean("incompleteStructure", (getErrorDisplayID() & 64) != 0);
        tag.setInteger("maxParallelRecipes", getMaxParallelRecipes());
        if (getTrueParallel() != getMaxParallelRecipes()) tag.setInteger("powerPanelMaxParallel", getTrueParallel());
        tag.setBoolean("isLockedToRecipe", false);
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null) {
            tag.setBoolean("isActive", base.isActive());
            tag.setBoolean("isAllowedToWork", base.isAllowedToWork());
            tag.setLong("energyTier", getInputVoltageTier());
            tag.setLong("energyUsage", usesDynamo() && running ? -producedEUt : 0);
            GTClientPreference preference = GTMod.proxy.getClientPreference(player.getUniqueID());
            if (preference != null && preference.isWailaAverageNSEnabled()) {
                base.startTimeStatistics();
                long sum = 0;
                int count = 0;
                for (int time : base.getTimeStatistics()) {
                    if (time != 0) {
                        sum += time;
                        count++;
                    }
                }
                if (count > 0) tag.setInteger("averageNS", (int) (sum / count));
            }
        }
        outputDisplaySnapshot().writeWailaPreview(tag);
        tag.setBoolean("tnWirelessUpgrade", usesDynamo() && wirelessUpgrade);
        getExtraWailaNBT(player, tile, tag, world, x, y, z);
    }

    @Override
    public void getExtraWailaBody(ItemStack stack, List<String> tooltip, NBTTagCompound tag,
        IWailaDataAccessor accessor, IWailaConfigHandler config) {
        super.getExtraWailaBody(stack, tooltip, tag, accessor, config);
        if (!tag.getBoolean("isActive") || tag.getInteger("tnOutputKinds") == 0) return;
        tooltip.add(StatCollector.translateToLocal("GT5U.waila.producing"));
        NBTTagList preview = tag.getTagList("tnOutputPreview", 10);
        int shown = Math.min(3, preview.tagCount());
        for (int i = 0; i < shown; i++) {
            try {
                OutputDisplaySnapshot.Entry entry = OutputDisplaySnapshot.decode(
                    preview.getCompoundTagAt(i)
                        .getByteArray("data"));
                String icon = entry.item != null ? TTRenderStack.create(entry.item, true)
                    : entry.fluid != null
                        ? TTRenderStack.create(GTUtility.getFluidDisplayStack(entry.fluid, false), true)
                        : "";
                tooltip.add(
                    "  " + icon
                        + EnumChatFormatting.AQUA
                        + outputDisplayName(entry)
                        + EnumChatFormatting.RESET
                        + " x "
                        + EnumChatFormatting.GOLD
                        + formatNumber(entry.amount)
                        + (entry.fluidKind ? " L" : ""));
                if (entry.detailsOmitted)
                    tooltip.add(StatCollector.translateToLocal("thermonuclear.gui.output_details_omitted"));
            } catch (IOException invalidRecord) {
                tooltip.add(StatCollector.translateToLocal("thermonuclear.gui.output_unavailable"));
            }
        }
        int remaining = tag.getInteger("tnOutputKinds") - shown;
        if (remaining > 0) tooltip
            .add(StatCollector.translateToLocalFormatted("GT5U.waila.producing.andmore", formatNumber(remaining)));
    }

    public static String outputDisplayName(OutputDisplaySnapshot.Entry entry) {
        if (entry.item != null) return entry.item.getDisplayName();
        if (entry.fluid != null) return entry.fluid.getLocalizedName();
        return StatCollector.translateToLocal("thermonuclear.gui.output_unavailable");
    }

    @Override
    public void getWailaBody(ItemStack stack, List<String> tooltip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(stack, tooltip, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();
        if (tag.getBoolean("tnWirelessUpgrade")) {
            tooltip.add(StatCollector.translateToLocal("thermonuclear.gui.wireless"));
        }
    }

    @Override
    protected abstract MTEMultiBlockBaseGui<?> getGui();

    protected final Map<String, String> commonInfo() {
        Map<String, String> info = new LinkedHashMap<>();
        boolean processingFailed = !running && (status.equals("running")) && !checkRecipeResult.wasSuccessful();
        info.put("status", "thermonuclear.status." + (processingFailed ? "processing_failed" : status));
        return info;
    }

    public static String formatInfo(String key, String value) {
        if (value == null || value.isEmpty()) return "";
        String labelKey = "thermonuclear.gui." + key;
        String label = StatCollector.translateToLocal(labelKey);
        int separator = labelKey.lastIndexOf('_');
        if (!StatCollector.canTranslate(labelKey) && separator >= 0) {
            String index = labelKey.substring(separator + 1);
            String template = labelKey.substring(0, separator);
            if (index.matches("[0-9]+") && StatCollector.canTranslate(template)) {
                label = StatCollector.translateToLocalFormatted(template, index);
            }
        }
        String text = value.startsWith("thermonuclear.") ? StatCollector.translateToLocal(value) : value;
        if (key.equals("status")) {
            String state = value.substring("thermonuclear.status.".length());
            String severity = switch (state) {
                case "running" -> "normal";
                case "stopped", "stopped_cooling", "stopped_passive" -> "idle";
                case "checking" -> "waiting";
                default -> "error";
            };
            EnumChatFormatting color = switch (severity) {
                case "normal" -> EnumChatFormatting.GREEN;
                case "idle" -> EnumChatFormatting.GRAY;
                case "waiting" -> EnumChatFormatting.YELLOW;
                case "warning" -> EnumChatFormatting.GOLD;
                default -> EnumChatFormatting.RED;
            };
            return color.toString() + EnumChatFormatting.BOLD
                + StatCollector.translateToLocal("thermonuclear.gui.severity." + severity)
                + EnumChatFormatting.RESET
                + " "
                + color
                + text
                + EnumChatFormatting.RESET;
        }
        return EnumChatFormatting.GRAY + label
            + ": "
            + (key.equals("mode") || key.equals("steam_type") ? EnumChatFormatting.GOLD : EnumChatFormatting.AQUA)
            + text
            + EnumChatFormatting.RESET;
    }

    public static String decimal(double value) {
        return String.format(Locale.ROOT, "%,.3f", value)
            .replaceAll("0+$", "")
            .replaceAll("\\.$", "");
    }

    @Override
    public String[] getInfoData() {
        return displayInfo().entrySet()
            .stream()
            .map(entry -> formatInfo(entry.getKey(), entry.getValue()))
            .toArray(String[]::new);
    }

    @Override
    public boolean supportsVoidProtection() {
        return false;
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public boolean supportsMaintenanceIssueHoverable() {
        return false;
    }

    @Override
    public boolean showRecipeTextInGUI() {
        return true;
    }
}
