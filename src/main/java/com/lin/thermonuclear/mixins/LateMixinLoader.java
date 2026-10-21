package com.lin.thermonuclear.mixins;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

@LateMixin
public class LateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.thermonuclear.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        if (FMLLaunchHandler.side()
            .isClient() && loadedMods.contains("blockrenderer6343")) {
            return Collections.singletonList("blockrenderer6343.MixinGuiMultiblockHandler");
        }
        return Collections.emptyList();
    }
}
