package com.lin.thermonuclear.mixins.late.blockrenderer6343;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.lin.thermonuclear.machine.nuclear.MTENuclearPowerPlant;

@Pseudo
@Mixin(targets = "blockrenderer6343.integration.nei.GuiMultiblockHandler", remap = false)
public abstract class MixinGuiMultiblockHandler {

    @Shadow
    protected static IConstructable renderingController;

    @Redirect(
        method = "loadChannels",
        at = @At(
            value = "INVOKE",
            target = "Lorg/apache/commons/lang3/StringUtils;capitalize(Ljava/lang/String;)Ljava/lang/String;"))
    private String thermonuclear$preserveExtraChannelName(String channel) {
        // Keep the visible identifier usable in the blueprint's case-sensitive channel editor.
        if (renderingController instanceof MTENuclearPowerPlant && "extra".equals(channel)) return channel;
        return StringUtils.capitalize(channel);
    }
}
