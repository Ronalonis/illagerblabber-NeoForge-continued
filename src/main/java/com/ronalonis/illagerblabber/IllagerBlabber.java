package com.ronalonis.illagerblabber;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod("illagerblabber")
public class IllagerBlabber {
    public static final String MODID = "illagerblabber";

    public IllagerBlabber(IEventBus modEventBus, ModContainer modContainer) {
        IllagerSounds.registerAll(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, IllagerBlabberConfig.SERVER_CONFIG);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }
}
