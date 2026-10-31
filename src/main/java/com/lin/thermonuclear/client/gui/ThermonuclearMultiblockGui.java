package com.lin.thermonuclear.client.gui;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.ObjectValue;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.FluidDisplayWidget;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.lin.thermonuclear.common.machine.base.OutputDisplaySnapshot;
import com.lin.thermonuclear.common.machine.base.ThermonuclearMultiblockBase;

import gregtech.api.enums.VoidingMode;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GTUtility;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.gui.modularui.adapter.CheckRecipeResultAdapter;
import gregtech.common.gui.modularui.adapter.ShutdownReasonAdapter;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public abstract class ThermonuclearMultiblockGui<T extends ThermonuclearMultiblockBase<?>>
    extends MTEMultiBlockBaseGui<T> {

    private OutputDisplaySyncHandler outputDisplay;

    protected ThermonuclearMultiblockGui(T machine) {
        super(machine);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager manager) {
        // GT .264 registers/copies full output arrays in super. Register its scalar UI contract without those handlers.
        manager.syncValue("structureErrors", multiblock.getStructureErrorsSyncer());
        manager.syncValue("errorID", new IntSyncValue(multiblock::getErrorDisplayID, multiblock::setErrorDisplayID));
        manager.syncValue(
            "machineActive",
            new BooleanSyncValue(baseMetaTileEntity::isActive, baseMetaTileEntity::setActive));
        manager.syncValue(
            "machine",
            new BooleanSyncValue(() -> multiblock.mMachine, value -> multiblock.mMachine = value));
        manager
            .syncValue("wrench", new BooleanSyncValue(() -> multiblock.mWrench, value -> multiblock.mWrench = value));
        manager.syncValue(
            "screwdriver",
            new BooleanSyncValue(() -> multiblock.mScrewdriver, value -> multiblock.mScrewdriver = value));
        manager.syncValue(
            "softMallet",
            new BooleanSyncValue(() -> multiblock.mSoftMallet, value -> multiblock.mSoftMallet = value));
        manager.syncValue(
            "hardHammer",
            new BooleanSyncValue(() -> multiblock.mHardHammer, value -> multiblock.mHardHammer = value));
        manager.syncValue(
            "solderingTool",
            new BooleanSyncValue(() -> multiblock.mSolderingTool, value -> multiblock.mSolderingTool = value));
        manager.syncValue(
            "crowbar",
            new BooleanSyncValue(() -> multiblock.mCrowbar, value -> multiblock.mCrowbar = value));
        manager.syncValue("totalRunTime", new LongSyncValue(multiblock::getTotalRunTime, multiblock::setTotalRunTime));
        manager.syncValue(
            "lastWorkingTick",
            new LongSyncValue(multiblock::getLastWorkingTick, multiblock::setLastWorkingTick));
        manager.syncValue(
            "shutdownDuration",
            new LongSyncValue(() -> (multiblock.getTotalRunTime() - multiblock.getLastWorkingTick()) / 20));
        BooleanSyncValue shutdown = new BooleanSyncValue(
            baseMetaTileEntity::wasShutdown,
            baseMetaTileEntity::setShutdownStatus);
        GenericSyncValue<ShutDownReason, ?> reason = GenericSyncValue.builder(ShutDownReason.class)
            .getter(baseMetaTileEntity::getLastShutDownReason)
            .setter(baseMetaTileEntity::setShutDownReason)
            .adapter(new ShutdownReasonAdapter())
            .build();
        manager.syncValue("wasShutdown", shutdown);
        manager.syncValue("shutdownReason", reason);
        manager.syncValue(
            "checkRecipeResult",
            GenericSyncValue.builder(CheckRecipeResult.class)
                .getter(multiblock::getCheckRecipeResult)
                .setter(multiblock::setCheckRecipeResult)
                .adapter(new CheckRecipeResultAdapter())
                .build());
        manager.syncValue(
            "progressTime",
            new IntSyncValue(() -> multiblock.mProgresstime, value -> multiblock.mProgresstime = value));
        manager.syncValue(
            "maxProgressTime",
            new IntSyncValue(() -> multiblock.mMaxProgresstime, value -> multiblock.mMaxProgresstime = value));
        manager.syncValue("powerSwitch", new BooleanSyncValue(multiblock::isAllowedToWork, value -> {
            if (!baseMetaTileEntity.isServerSide() || isPowerSwitchDisabled()) return;
            if (value) multiblock.enableWorking();
            else if (multiblock.maxProgresstime() > 0) {
                multiblock.disableWorking();
                shutdown.setValue(true);
                reason.setValue(ShutDownReasonRegistry.NONE);
            } else multiblock.stopMachine(ShutDownReasonRegistry.NONE);
        }).allowC2S());
        IntSyncValue structure = new IntSyncValue(
            multiblock::getStructureUpdateTime,
            multiblock::setStructureUpdateTime).allowC2S();
        manager.syncValue("structureUpdate", structure);
        manager.syncValue(
            "structureUpdateButton",
            new BooleanSyncValue(() -> structure.getValue() > -20, value -> { if (value) structure.setValue(1); })
                .allowC2S());
        manager.syncValue(
            "recipeLock",
            new BooleanSyncValue(multiblock::isRecipeLockingEnabled, multiblock::setRecipeLocking).allowC2S());
        manager.syncValue(
            "batchMode",
            new BooleanSyncValue(multiblock::isBatchModeEnabled, multiblock::setBatchMode).allowC2S());
        manager.syncValue(
            "machineMode",
            new IntSyncValue(multiblock::getMachineMode, multiblock::setMachineMode).allowC2S());
        manager.syncValue(
            "inputSeparation",
            new BooleanSyncValue(multiblock::isInputSeparationEnabled, multiblock::setInputSeparation).allowC2S());
        manager.syncValue(
            "voidExcess",
            new EnumSyncValue<>(VoidingMode.class, multiblock::getVoidingMode, multiblock::setVoidingMode).allowC2S());
        manager.syncValue(
            "maintCount",
            new IntSyncValue(() -> multiblock.getIdealStatus() - multiblock.getRepairStatus()));
        manager
            .syncValue("mufflerSyncer", new BooleanSyncValue(multiblock::isMuffled, multiblock::setMuffled).allowC2S());
        outputDisplay = new OutputDisplaySyncHandler(
            multiblock::outputDisplaySnapshot,
            () -> multiblock.mMaxProgresstime);
        manager.syncValue("tnOutputDisplay", outputDisplay);
    }

    @Override
    protected int getBasePanelHeight() {
        return super.getBasePanelHeight() + 60;
    }

    @Override
    protected int getTerminalRowHeight() {
        return super.getTerminalRowHeight() + 60;
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager manager, ModularPanel parent) {
        ListWidget<IWidget, ?> list = new ListWidget<>().fullWidth()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .collapseDisabledChild();
        // Client-side machine state cannot determine the server's conditional keys or their order.
        // Sync one ordered snapshot without a setter; localize it only when rendering on the client.
        GenericListSyncHandler<Map.Entry<String, String>> info = GenericListSyncHandler.<Map.Entry<String, String>>builder()
            .getter(() -> {
                List<Map.Entry<String, String>> entries = new ArrayList<>();
                multiblock.displayInfo()
                    .forEach((key, value) -> {
                        // Other displays retain the full snapshot; only the terminal delegates these rows to GT.
                        if (!isNativeTerminalInfo(key)) entries.add(new AbstractMap.SimpleImmutableEntry<>(key, value));
                    });
                return entries;
            })
            .serializer((buffer, entry) -> {
                NetworkUtils.writeStringSafe(buffer, entry.getKey());
                NetworkUtils.writeStringSafe(buffer, entry.getValue());
            })
            .deserializer(
                buffer -> new AbstractMap.SimpleImmutableEntry<>(
                    NetworkUtils.readStringSafe(buffer),
                    NetworkUtils.readStringSafe(buffer)))
            .immutableCopy()
            .build();
        manager.syncValue("tnDisplayInfo", info);
        list.child(
            IKey.dynamic(() -> formatTerminalInfo(info.getValue()))
                .asWidget()
                .fullWidth()
                .textAlign(Alignment.TopLeft)
                .marginBottom(2));
        // Do not construct GT's private recipe widget: it sends the complete output list back from the client.
        IntSyncValue startup = new IntSyncValue(multiblock::getmStartUpCheck);
        manager.syncValue("startupCheck", startup);
        list.childIf(
            multiblock.supportsMachineModeSwitch(),
            () -> IKey
                .dynamic(
                    () -> StatCollector
                        .translateToLocalFormatted("gt.interact.desc.mb.mode", multiblock.getMachineModeName()))
                .asWidget()
                .fullWidth());
        list.child(
            IKey.lang("GT5U.multiblock.startup")
                .asWidget()
                .fullWidth()
                .setEnabledIf(w -> startup.getValue() > 0));
        list.childIf(
            multiblock.hasRunningText(),
            () -> IKey.lang("gt.interact.desc.mb.running")
                .asWidget()
                .fullWidth()
                .setEnabledIf(w -> multiblock.getErrorDisplayID() == 0 && baseMetaTileEntity.isActive()));
        list.child(createShutdownDurationWidget(manager));
        list.child(createShutdownReasonWidget(manager));
        list.child(createStructureErrorWidget(manager));
        list.child(
            IKey.dynamic(
                () -> multiblock.getCheckRecipeResult()
                    .getDisplayString())
                .asWidget()
                .fullWidth()
                .setEnabledIf(
                    w -> multiblock.shouldDisplayCheckRecipeResult()
                        && GTUtility.isStringValid(
                            multiblock.getCheckRecipeResult()
                                .getDisplayString())
                        && (multiblock.isAllowedToWork() || baseMetaTileEntity.isActive()
                            || multiblock.getCheckRecipeResult()
                                .persistsOnShutdown())));
        list.child(
            IKey.dynamic(multiblock::generateCurrentRecipeInfoString)
                .asWidget()
                .fullWidth()
                .setEnabledIf(w -> multiblock.mMaxProgresstime > 0));
        list.child(createOutputDisplay());
        return list;
    }

    private Flow createOutputDisplay() {
        // Before the first page (or after clearing outputs), every child is collapsed. MUI needs a positive fallback.
        Flow column = Flow.column()
            .fullWidth()
            .coverChildrenHeight(1)
            .collapseDisabledChild()
            .setEnabledIf(
                w -> !outputDisplay.entries()
                    .isEmpty());
        for (int i = 0; i < OutputDisplaySnapshot.MAX_PAGE_ENTRIES; i++) {
            final int index = i;
            ItemDisplayWidget item = new ItemDisplayWidget().displayAmount(false)
                .disableThemeBackground(true)
                .item(
                    new ObjectValue.Dynamic<>(
                        ItemStack.class,
                        () -> entry(index) == null ? null : entry(index).item,
                        ignored -> {}))
                .size(16)
                .setEnabledIf(w -> entry(index) != null && entry(index).item != null);
            FluidDisplayWidget fluid = new FluidDisplayWidget().displayAmount(false)
                .disableThemeBackground(true)
                .value(
                    new ObjectValue.Dynamic<>(
                        FluidStack.class,
                        () -> entry(index) == null ? null : entry(index).fluid,
                        ignored -> {}))
                .size(16)
                .setEnabledIf(w -> entry(index) != null && entry(index).fluid != null);
            column.child(
                Flow.row()
                    .fullWidth()
                    .coverChildrenHeight(0)
                    .collapseDisabledChild()
                    .child(item)
                    .child(fluid)
                    .child(
                        IKey.dynamic(() -> formatOutput(index))
                            .asWidget()
                            .widthRel(0.85f)
                            .scale(0.75f)
                            .textAlign(Alignment.TopLeft)
                            .tooltipAutoUpdate(true)
                            .tooltipBuilder(tooltip -> {
                                OutputDisplaySnapshot.Entry entry = entry(index);
                                if (entry == null) return;
                                tooltip.addLine(ThermonuclearMultiblockBase.outputDisplayName(entry));
                                tooltip.addLine(formatNumber(entry.amount) + (entry.fluidKind ? " L" : ""));
                                if (entry.detailsOmitted)
                                    tooltip.addLine(IKey.lang("thermonuclear.gui.output_details_omitted"));
                            }))
                    .setEnabledIf(w -> entry(index) != null));
        }
        column.child(
            Flow.row()
                .fullWidth()
                .height(18)
                .child(
                    new ButtonWidget<>().size(18)
                        .overlay(IKey.str("<"))
                        .onMousePressed(button -> {
                            outputDisplay.turnPage(-1);
                            return true;
                        })
                        .setEnabledIf(w -> outputDisplay.page() > 0))
                .child(
                    IKey.dynamic(
                        () -> StatCollector.translateToLocalFormatted(
                            "thermonuclear.gui.output_page",
                            outputDisplay.page() + 1,
                            outputDisplay.pageCount(),
                            outputDisplay.total()))
                        .asWidget()
                        .widthRel(0.7f))
                .child(
                    new ButtonWidget<>().size(18)
                        .overlay(IKey.str(">"))
                        .onMousePressed(button -> {
                            outputDisplay.turnPage(1);
                            return true;
                        })
                        .setEnabledIf(w -> outputDisplay.page() + 1 < outputDisplay.pageCount()))
                .setEnabledIf(w -> outputDisplay.pageCount() > 1));
        return column;
    }

    private OutputDisplaySnapshot.Entry entry(int index) {
        return index < outputDisplay.entries()
            .size() ? outputDisplay.entries()
                .get(index) : null;
    }

    private String formatOutput(int index) {
        OutputDisplaySnapshot.Entry entry = entry(index);
        if (entry == null) return "";
        String rate = showOutputRates() && outputDisplay.duration() > 0
            ? GTUtility.appendRate(entry.fluidKind, entry.amount, true, outputDisplay.duration())
            : "";
        return EnumChatFormatting.AQUA + ThermonuclearMultiblockBase.outputDisplayName(entry)
            + "\n"
            + EnumChatFormatting.RESET
            + GTUtility.formatShortenedLong(entry.amount)
            + (entry.fluidKind ? " L" : "")
            + " "
            + rate
            + (entry.detailsOmitted ? "\n" + StatCollector.translateToLocal("thermonuclear.gui.output_details_omitted")
                : "");
    }

    private boolean isNativeTerminalInfo(String key) {
        return switch (key) {
            case "cycle" -> true;
            // Stopped cooling is immediate and has no native recipe output readout.
            case "steam_output", "coolant_return", "water_output", "hot_output" -> multiblock.mMaxProgresstime > 0;
            default -> false;
        };
    }

    private String formatTerminalInfo(List<Map.Entry<String, String>> entries) {
        StringJoiner lines = new StringJoiner("\n");
        for (Map.Entry<String, String> entry : entries) {
            if (entry.getValue()
                .isEmpty()) continue;
            lines.add(ThermonuclearMultiblockBase.formatInfo(entry.getKey(), entry.getValue()));
        }
        return lines.toString();
    }
}
