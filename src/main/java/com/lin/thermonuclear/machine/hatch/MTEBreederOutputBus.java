package com.lin.thermonuclear.machine.hatch;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.common.gui.modularui.hatch.MTEHatchOutputBusGui;

public final class MTEBreederOutputBus extends MTEHatchOutputBus {

    private static final int STORAGE_TIER = 7;
    private static final int STORAGE_SLOTS = getSlots(STORAGE_TIER);

    public MTEBreederOutputBus(int id, String name, String regional) {
        // Inventory size is independent of the LV tier used by the inherited textures.
        super(id, name, regional, 1, STORAGE_SLOTS);
    }

    private MTEBreederOutputBus(String name, String[] description, ITexture[][][] textures) {
        super(name, 1, STORAGE_SLOTS, description, textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MTEBreederOutputBus(mName, mDescriptionArray, mTextures);
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal("thermonuclear.hatch.breeder_output.name");
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings uiSettings) {
        return new MTEHatchOutputBusGui(this) {

            @Override
            protected int getDimension() {
                return STORAGE_TIER + 1;
            }
        }.build(data, syncManager, uiSettings);
    }

    @Override
    public String[] getDescription() {
        return new String[] { StatCollector.translateToLocal("thermonuclear.hatch.breeder_output.desc"),
            StatCollector.translateToLocal("thermonuclear.tooltip.mod") };
    }
}
