package com.dozamigos.dozamigos.client;

import com.dozamigos.dozamigos.client.gui.UpdaterScreen;
import com.mojang.brigadier.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ClientCommands {
    private ClientCommands() {}

    @SubscribeEvent
    public static void onRegister(RegisterClientCommandsEvent event) {
        // FancyMenu: action "sendmessage" / comando cliente -> /dozamigos
        event.getDispatcher().register(Commands.literal("dozamigos").executes(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.setScreen(new UpdaterScreen(mc.screen)));
            return Command.SINGLE_SUCCESS;
        }));
    }
}
