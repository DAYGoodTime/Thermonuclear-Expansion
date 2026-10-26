package com.lin.thermonuclear.common.registry;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.lin.thermonuclear.Thermonuclear;
import com.lin.thermonuclear.common.loader.BlockLoader;

import gregtech.api.structure.IStructureChannels;

/**
 * StructureLib subchannels used by this mod's multiblocks.
 * <p>
 * Registration runs during init, after blocks exist, so every channel shows up in the blueprint projector's
 * autocomplete list with its description. Tiered blocks are registered as indicators so they can be dragged into the
 * projector GUI or picked with a middle-click.
 * <p>
 * The reactor pipe channel must not reuse GT's {@code pipe}: GT maps the same pipe casings to that channel starting at
 * Bronze, which does not match this mod's Steel-based tiers. GT also owns those casing items, so re-registering them
 * here would make middle-click picking ambiguous; {@link #REACTOR_PIPE} therefore carries only its description.
 */
public enum StructureChannels implements IStructureChannels {

    REACTOR_PIPE("reactor_pipe"),
    FUEL_ROD("fuel_rod"),
    TURBINE_SHAFT("turbine_shaft"),
    EXTRA("extra");

    private final String channel;

    StructureChannels(String channel) {
        this.channel = channel;
    }

    @Override
    public String get() {
        return channel;
    }

    @Override
    public String getDefaultTooltip() {
        return StatCollector.translateToLocal(descriptionKey());
    }

    @Override
    public void registerAsIndicator(ItemStack indicator, int channelValue) {
        StructureLibAPI.registerChannelItem(channel, Thermonuclear.MODID, channelValue, indicator);
    }

    private String descriptionKey() {
        return "channels." + Thermonuclear.MODID + "." + channel;
    }

    public static void register() {
        for (StructureChannels value : values()) {
            StructureLibAPI.registerChannelDescription(value.channel, Thermonuclear.MODID, value.descriptionKey());
        }
        for (int tier = 1; tier <= BlockLoader.fuelRods().length; tier++) {
            FUEL_ROD
                .registerAsIndicator(new ItemStack(BlockLoader.fuelRod(tier), 1, OreDictionary.WILDCARD_VALUE), tier);
        }
        for (int tier = 1; tier <= BlockLoader.turbineShafts().length; tier++) {
            TURBINE_SHAFT.registerAsIndicator(
                new ItemStack(BlockLoader.turbineShaft(tier), 1, OreDictionary.WILDCARD_VALUE),
                tier);
        }
    }
}
