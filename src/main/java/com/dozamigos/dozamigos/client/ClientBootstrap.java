package com.dozamigos.dozamigos.client;

import com.dozamigos.dozamigos.DozamigosMod;
import com.dozamigos.dozamigos.client.fancymenu.FancyMenuBridge;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ClientBootstrap {
    private ClientBootstrap() {}

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientBootstrap::onClientSetup);
        MinecraftForge.EVENT_BUS.register(TitleScreenHooks.class);
        MinecraftForge.EVENT_BUS.register(ClientCommands.class);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            FancyMenuBridge.tryRegister();
            DozamigosMod.LOGGER.info("Dozamigos client pronto");
        });
    }
}
