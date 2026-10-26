package com.lin.thermonuclear.common.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.lin.thermonuclear.Thermonuclear;

public final class ItemWirelessUpgradeChip extends ItemBase {

    public ItemWirelessUpgradeChip() {
        super("wireless_upgrade_chip", Thermonuclear.MODID + ":wireless_upgrade_chip");
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.wireless_chip.0"));
        tooltip.add(StatCollector.translateToLocal("thermonuclear.tooltip.wireless_chip.1"));
    }
}
