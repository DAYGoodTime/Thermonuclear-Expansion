package com.lin.thermonuclear;

import com.lin.thermonuclear.api.FuelRodAdapters;
import com.lin.thermonuclear.api.HeatExchangeRules;
import com.lin.thermonuclear.api.SteamTurbineFuels;
import com.lin.thermonuclear.common.loader.BlockLoader;
import com.lin.thermonuclear.common.loader.ItemLoader;
import com.lin.thermonuclear.common.loader.MachineLoader;
import com.lin.thermonuclear.common.loader.RecipeLoader;
import com.lin.thermonuclear.common.nuclear.fuel.GTFuelRodAdapter;
import com.lin.thermonuclear.common.nuclear.fuel.NaquadahFuelRodAdapter;
import com.lin.thermonuclear.common.recipe.heatexchange.DefaultHeatExchangeRules;
import com.lin.thermonuclear.common.recipe.steamturbine.DefaultSteamTurbineFuels;
import com.lin.thermonuclear.common.registry.ModFluids;
import com.lin.thermonuclear.common.registry.StructureChannels;
import com.lin.thermonuclear.common.registry.WorkingFluids;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        ModFluids.register();
        BlockLoader.register();
        ItemLoader.register();

        Thermonuclear.LOG.info(Config.general.greeting);
        Thermonuclear.LOG.info("Thermonuclear prototype at version " + Tags.VERSION);
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        FuelRodAdapters.register(new NaquadahFuelRodAdapter());
        FuelRodAdapters.register(new GTFuelRodAdapter());
        MachineLoader.register();
        // After block registration so indicator stacks resolve and the channels appear in the projector autocomplete.
        StructureChannels.register();
    }

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {}

    public void loadComplete(FMLLoadCompleteEvent event) {
        // BW/GT material loaders have finished before resolving their registered fluid instances.
        WorkingFluids.resolve();
        for (DefaultHeatExchangeRules rule : DefaultHeatExchangeRules.values()) {
            HeatExchangeRules.register(rule);
        }
        HeatExchangeRules.resolve();
        SteamTurbineFuels.addFuels(DefaultSteamTurbineFuels.values());
        RecipeLoader.register();
    }

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {}
}
