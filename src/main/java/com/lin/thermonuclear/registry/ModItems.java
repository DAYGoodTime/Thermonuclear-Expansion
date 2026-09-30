package com.lin.thermonuclear.registry;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;

public final class ModItems {

    private ModItems() {}

    public static ItemStack register(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            throw new IllegalArgumentException("Cannot register an empty Thermonuclear item");
        }
        // GT controllers share an item with other mods; register exact metadata, never a wildcard.
        AnimatedTooltipHandler.addItemTooltip(
            stack,
            () -> EnumChatFormatting.AQUA + StatCollector.translateToLocal("thermonuclear.tooltip.mod"));
        return stack;
    }
}
