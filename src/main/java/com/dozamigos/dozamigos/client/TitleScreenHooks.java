package com.dozamigos.dozamigos.client;

import com.dozamigos.dozamigos.client.gui.UpdatePopupScreen;
import com.dozamigos.dozamigos.client.gui.UpdaterScreen;
import com.dozamigos.dozamigos.config.DozamigosConfig;
import com.dozamigos.dozamigos.sync.UpdatePlan;
import com.dozamigos.dozamigos.sync.UpdateService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TitleScreenHooks {
    private static boolean checkedThisSession = false;
    private static boolean popupQueued = false;

    private TitleScreenHooks() {}

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen title)) {
            return;
        }

        if (DozamigosConfig.SHOW_TITLE_BUTTON.get()) {
            int x = title.width / 2 + 104;
            int y = title.height / 4 + 48 + 72;
            event.addListener(Button.builder(Component.translatable("dozamigos.button.menu"), b ->
                    Minecraft.getInstance().setScreen(new UpdaterScreen(title))
            ).bounds(x, y, 98, 20).build());
        }

        maybeCheck(title);
    }

    private static void maybeCheck(TitleScreen title) {
        if (!DozamigosConfig.CHECK_ON_TITLE.get() || checkedThisSession) {
            return;
        }
        String url = DozamigosConfig.MANIFEST_URL.get();
        if (url == null || url.isBlank()) {
            return;
        }
        checkedThisSession = true;
        UpdateService.checkAsync().whenComplete((plan, err) -> {
            if (err != null || plan == null || plan.isEmpty()) {
                return;
            }
            popupQueued = true;
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (popupQueued && mc.screen instanceof TitleScreen) {
                    popupQueued = false;
                    mc.setScreen(new UpdatePopupScreen(mc.screen, plan));
                }
            });
        });
    }

    public static void resetSessionFlag() {
        checkedThisSession = false;
    }

    public static UpdatePlan peekPlan() {
        return UpdateService.lastPlan();
    }
}
