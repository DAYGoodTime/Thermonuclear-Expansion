package com.lin.thermonuclear.client.gui;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.lin.thermonuclear.common.machine.base.ThermonuclearMultiblockBase;

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
        // Build the full native terminal once: its output widgets own the sync change listeners.
        list.child(
            super.createTerminalTextWidget(manager, parent).coverChildrenHeight(0)
                .collapseDisabledChild());
        return list;
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
