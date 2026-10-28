package com.lin.thermonuclear.common.machine.base;

import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.lin.thermonuclear.api.TNCItemList;
import com.lin.thermonuclear.utils.MathUtil;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchInputDebug;
import gregtech.api.metatileentity.implementations.MTEHatchMultiInput;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.MTEHatchInputME;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public abstract class ThermonuclearMultiblockBase<T extends ThermonuclearMultiblockBase<T>>
    extends MTEEnhancedMultiBlockBase<T> implements ISurvivalConstructable {

    protected boolean running;
    protected String status = "stopped";
    protected long producedEUt;
    private boolean wirelessUpgrade;
    private UUID wirelessOwner;
    private final List<MTEHatch> energyDynamo = new ArrayList<>();

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

    protected boolean fail(String reason) {
        status = reason;
        return false;
    }

    protected final long availableFromStacks(Fluid fluid, List<FluidStack> fluids) {
        if (fluid == null) return 0;
        FluidStack requested = new FluidStack(fluid, 1);
        long amount = 0;
        Set<FluidStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (FluidStack stack : fluids) {
            if (stack != null && stack.isFluidEqual(requested) && seen.add(stack)) {
                amount = MathUtil.saturatingAdd(amount, Math.max(0, stack.amount));
            }
        }
        return amount;
    }

    protected long available(Fluid fluid, List<MTEHatch> hatches) {
        return availableFromStacks(fluid, hatchFluids(hatches));
    }

    private List<FluidStack> hatchFluids(List<MTEHatch> hatches) {
        List<FluidStack> fluids = new ArrayList<>();
        List<FluidStack> meFluids = new ArrayList<>();
        for (MTEHatch hatch : hatches) {
            if (hatch == null || !hatch.isValid()) continue;
            if (hatch instanceof MTEHatchInputME meHatch) {
                for (FluidStack stack : meHatch.getStoredFluids()) {
                    // ME hatches can expose the same network stock; retain one original transaction stack.
                    if (stack != null && meFluids.stream()
                        .noneMatch(stack::isFluidEqual)) {
                        meFluids.add(stack);
                        fluids.add(stack);
                    }
                }
            } else if (hatch instanceof MTEHatchMultiInput multiInputHatch) {
                fluids.addAll(Arrays.asList(multiInputHatch.getStoredFluid()));
            } else if (hatch instanceof MTEHatchInputDebug debugHatch) {
                for (FluidStack template : debugHatch.getFluidList()) {
                    if (template == null) continue;
                    FluidStack stack = template.copy();
                    stack.amount = Integer.MAX_VALUE;
                    fluids.add(stack);
                }
            } else {
                fluids.add(hatch.getDrainableStack());
            }
        }
        return fluids;
    }

    protected final void consumeFromStacks(Fluid fluid, long amount, List<FluidStack> fluids) {
        if (amount < 0) throw new IllegalArgumentException("Negative fluid consumption");
        FluidStack requested = fluid == null ? null : new FluidStack(fluid, 1);
        Set<FluidStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (FluidStack stack : fluids) {
            if (stack == null || !stack.isFluidEqual(requested) || !seen.add(stack)) continue;
            int used = (int) Math.min(amount, Math.max(0, stack.amount));
            stack.amount -= used;
            amount -= used;
            if (amount == 0) return;
        }
        if (amount > 0) throw new IllegalStateException("Cycle input changed during recipe processing");
    }

    protected void consume(Fluid fluid, int amount, List<MTEHatch> hatches) {
        consumeFromStacks(fluid, amount, hatchFluids(hatches));
    }

    protected FluidEjectionHelper prepareOutputs(FluidStack... outputs) {
        for (FluidStack output : outputs) {
            if (output == null || output.amount <= 0) return null;
        }
        FluidEjectionHelper helper = new FluidEjectionHelper(getOutputHatches(), true);
        // One shared transaction set reserves each output slot only once for the entire batch.
        return helper.ejectFluids(Arrays.asList(outputs), 1) == 1 ? helper : null;
    }

    protected FluidEjectionHelper prepareOutputs(List<? extends gregtech.api.interfaces.IOutputHatch> hatches,
        FluidStack... outputs) {
        for (FluidStack output : outputs) {
            if (output == null || output.amount <= 0) return null;
        }
        FluidEjectionHelper helper = new FluidEjectionHelper(hatches, true);
        return helper.ejectFluids(Arrays.asList(outputs), 1) == 1 ? helper : null;
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
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setBoolean("tnWirelessUpgrade", usesDynamo() && wirelessUpgrade);
        if (usesDynamo()) {
            tag.setLong("energyUsage", running ? -producedEUt : 0);
        }
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
