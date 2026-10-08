package com.lin.thermonuclear;

import com.lin.thermonuclear.api.FuelRodAdapters;
import com.lin.thermonuclear.loader.BlockLoader;
import com.lin.thermonuclear.loader.MachineLoader;
import com.lin.thermonuclear.nuclear.GTFuelRodAdapter;
import com.lin.thermonuclear.recipe.HeatExchangeRecipes;
import com.lin.thermonuclear.recipe.SteamTurbineRecipes;
import com.lin.thermonuclear.registry.ModFluids;
import com.lin.thermonuclear.registry.WorkingFluids;

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

        Thermonuclear.LOG.info(Config.general.greeting);
        Thermonuclear.LOG.info("Thermonuclear prototype at version " + Tags.VERSION);
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        FuelRodAdapters.register(new GTFuelRodAdapter());
        MachineLoader.register();
    }

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {}

    public void loadComplete(FMLLoadCompleteEvent event) {
        // BW/GT material loaders have finished before resolving their registered fluid instances.
        WorkingFluids.resolve();
        HeatExchangeRecipes.register();
        SteamTurbineRecipes.register();
    }

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {}
}
