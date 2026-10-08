package com.lin.thermonuclear.gui;

import java.util.regex.Pattern;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.lin.thermonuclear.machine.MTELargeSteamTurbine;
import com.lin.thermonuclear.recipe.SteamTurbineMath;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.widget.settings.SettingsPanel;

public final class LargeSteamTurbineGui extends ThermonuclearMultiblockGui<MTELargeSteamTurbine> {

    public LargeSteamTurbineGui(MTELargeSteamTurbine machine) {
        super(machine);
    }

    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager manager) {
        IPanelHandler parameters = manager.syncedPanel("tnTurbineParameters", true, (sync, handler) -> {
            StringSyncValue limit = new StringSyncValue(
                () -> Long.toString(multiblock.getSteamLimitLitresPerCycle()),
                multiblock::setSteamLimitFromGui).allowC2S();
            sync.syncValue("tnSteamLimit", limit);
            ModularPanel panel = new ModularPanel("tnTurbineParameters").coverChildrenWidth(80)
                .coverChildrenHeight()
                .relative(parent)
                .topRel(0)
                .leftRel(1)
                .padding(4)
                .child(ButtonWidget.panelCloseButton());
            return panel.child(
                Flow.column()
                    .coverChildren()
                    .childPadding(10)
                    .child(
                        IKey.lang("tt.gui.parameter.header")
                            .asWidget())
                    .child(
                        SettingsPanel.builder()
                            .addStringEditor(
                                IKey.lang("thermonuclear.gui.steam_cycle_limit"),
                                limit,
                                (p, sm, field) -> field.setMaxLength(19)
                                    .setPattern(Pattern.compile("[0-9]*"))
                                    .setValidator(
                                        text -> Long.toString(
                                            SteamTurbineMath.parseLimit(
                                                text,
                                                SteamTurbineMath.parseLimit(limit.getStringValue(), Long.MAX_VALUE))))
                                    .width(125))
                            .build(panel, sync, getBasePanelHeight() - 8))
                    .child(
                        IKey.lang("thermonuclear.gui.steam_limit_help.0")
                            .asWidget()
                            .width(200))
                    .child(
                        IKey.lang("thermonuclear.gui.steam_limit_help.1")
                            .asWidget()
                            .width(200)));
        });
        return super.createLeftPanelGapRow(parent, manager).child(
            new ButtonWidget<>().size(18)
                .overlay(
                    GTGuiTextures.OVERLAY_BUTTON_EDIT_PARAMETERS_ENABLED.asIcon()
                        .size(16))
                .tooltipBuilder(t -> t.addLine(IKey.lang("tt.gui.tooltip.edit_parameters")))
                .onMousePressed(mouse -> {
                    if (parameters.isPanelOpen()) parameters.closePanel();
                    else parameters.openPanel();
                    return true;
                }));
    }
}
