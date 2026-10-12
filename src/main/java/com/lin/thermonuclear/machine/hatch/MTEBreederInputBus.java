package com.lin.thermonuclear.machine.hatch;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.lin.thermonuclear.nuclear.GTBreederRod;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.common.gui.modularui.hatch.MTEHatchInputBusGui;

public final class MTEBreederInputBus extends MTEHatchInputBus {

    private static final int STORAGE_TIER = 7;
    private static final int STORAGE_SLOTS = getSlots(STORAGE_TIER);

    public MTEBreederInputBus(int id, String name, String regional) {
        // Keep LV textures while using ZPM storage, plus the input bus's reserved circuit slot.
        super(id, name, regional, 1, STORAGE_SLOTS + 1);
    }

    private MTEBreederInputBus(String name, String[] description, ITexture[][][] textures) {
        super(name, 1, STORAGE_SLOTS + 1, description, textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MTEBreederInputBus(mName, mDescriptionArray, mTextures);
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal("thermonuclear.hatch.breeder_input.name");
    }

    @Override
    public String[] getDescription() {
        return new String[] { StatCollector.translateToLocal("thermonuclear.hatch.breeder_input.desc.0"),
            StatCollector.translateToLocal("thermonuclear.hatch.breeder_input.desc.1"),
            StatCollector.translateToLocal("thermonuclear.tooltip.mod") };
    }

    @Override
    public boolean allowSelectCircuit() {
        return false;
    }

    @Override
    public int getCircuitSlot() {
        return STORAGE_SLOTS;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings uiSettings) {
        return new MTEHatchInputBusGui(this) {

            @Override
            protected int getDimension() {
                return STORAGE_TIER + 1;
            }
        }.build(data, syncManager, uiSettings);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot >= 0 && slot < getCircuitSlot() && GTBreederRod.accepts(stack);
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity tile, int slot, ForgeDirection side, ItemStack stack) {
        return isItemValidForSlot(slot, stack) && super.allowPutStack(tile, slot, side, stack);
    }
}
