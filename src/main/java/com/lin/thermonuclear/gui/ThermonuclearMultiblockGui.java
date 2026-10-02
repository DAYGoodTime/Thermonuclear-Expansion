package com.lin.thermonuclear.gui;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.lin.thermonuclear.machine.ThermonuclearMultiblockBase;

import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public abstract class ThermonuclearMultiblockGui<T extends ThermonuclearMultiblockBase<?>>
    extends MTEMultiBlockBaseGui<T> {

    protected ThermonuclearMultiblockGui(T machine) {
        super(machine);
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
        String[] keys = multiblock.displayKeys();
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            // Getter-only sync values remain S2C-only. No client setter can alter machine state.
            StringSyncValue value = new StringSyncValue(
                () -> multiblock.displayInfo()
                    .getOrDefault(key, ""));
            manager.syncValue("tnLine_" + key, value);
            list.child(
                IKey.dynamic(() -> ThermonuclearMultiblockBase.formatInfo(key, value.getStringValue()))
                    .asWidget()
                    .fullWidth()
                    .textAlign(Alignment.CenterLeft)
                    .setEnabledIf(
                        widget -> !value.getStringValue()
                            .isEmpty())
                    .marginBottom(2));
            if (key.equals("status")) {
                list.child(
                    IKey.dynamic(
                        () -> EnumChatFormatting.RED + multiblock.getCheckRecipeResult()
                            .getDisplayString() + EnumChatFormatting.RESET)
                        .asWidget()
                        .fullWidth()
                        .textAlign(Alignment.CenterLeft)
                        .marginBottom(2)
                        .setEnabledIf(
                            widget -> value.getStringValue()
                                .equals("thermonuclear.status.processing_failed")));
            }
        }
        list.child(createShutdownDurationWidget(manager))
            .child(createShutdownReasonWidget(manager))
            .child(createStructureErrorWidget(manager));
        return list;
    }
}
