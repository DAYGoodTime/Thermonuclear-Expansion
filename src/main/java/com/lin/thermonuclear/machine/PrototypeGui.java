package com.lin.thermonuclear.machine;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

final class PrototypeGui extends MTEMultiBlockBaseGui<PrototypeMultiblockBase<?>> {

    PrototypeGui(PrototypeMultiblockBase<?> machine) {
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
        ListWidget<IWidget, ?> list = super.createTerminalTextWidget(manager, parent);
        String[] keys = multiblock.displayKeys();
        for (int i = 0; i < keys.length; i++) {
            final int index = i;
            final String label = "thermonuclear.gui." + keys[i];
            // Getter-only sync values remain S2C-only. No client setter can alter machine state.
            StringSyncValue value = new StringSyncValue(() -> multiblock.displayValues()[index]);
            manager.syncValue("tnLine" + index, value);
            list.child(IKey.dynamic(() -> {
                String text = value.getStringValue();
                if (text.startsWith("thermonuclear.")) text = StatCollector.translateToLocal(text);
                return StatCollector.translateToLocal(label) + ": " + text;
            })
                .asWidget()
                .fullWidth()
                .marginBottom(2));
        }
        if (multiblock instanceof MTENuclearPowerPlant reactor) {
            InteractionSyncHandler change = new InteractionSyncHandler().setOnMousePressed(mouse -> {
                // This callback is also invoked optimistically on the client; the server guard is essential.
                if (baseMetaTileEntity.isServerSide()) reactor.requestModeChange();
            });
            manager.syncValue("tnChangeMode", change);
            list.child(
                new ButtonWidget<>().size(18)
                    .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                    .tooltipBuilder(t -> t.addLine(IKey.lang("thermonuclear.gui.switch_mode")))
                    .syncHandler(change));
        }
        return list;
    }
}
