package com.lin.thermonuclear.gui;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.lin.thermonuclear.machine.PrototypeMultiblockBase;

import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public class PrototypeGui<T extends PrototypeMultiblockBase<?>> extends MTEMultiBlockBaseGui<T> {

    public PrototypeGui(T machine) {
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
        return list;
    }
}
