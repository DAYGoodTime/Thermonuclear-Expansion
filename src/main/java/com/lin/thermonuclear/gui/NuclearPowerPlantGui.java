package com.lin.thermonuclear.gui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.lin.thermonuclear.machine.MTENuclearPowerPlant;

import gregtech.api.modularui2.GTGuiTextures;

public final class NuclearPowerPlantGui extends ThermonuclearMultiblockGui<MTENuclearPowerPlant> {

    public NuclearPowerPlantGui(MTENuclearPowerPlant machine) {
        super(machine);
    }

    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager manager) {
        Flow row = super.createLeftPanelGapRow(parent, manager);
        InteractionSyncHandler change = new InteractionSyncHandler().setOnMousePressed(mouse -> {
            // Optimistic client callbacks must never mutate the machine's authoritative mode.
            if (baseMetaTileEntity.isServerSide()) multiblock.requestModeChange();
        });
        manager.syncValue("tnChangeMode", change);
        return row.child(
            new ButtonWidget<>().size(18)
                .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                .tooltipBuilder(t -> t.addLine(IKey.lang("thermonuclear.gui.switch_mode")))
                .syncHandler(change));
    }
}
