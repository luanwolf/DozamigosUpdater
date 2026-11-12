package com.dozamigos.dozamigos.client;

import com.dozamigos.dozamigos.client.gui.UpdatePopupScreen;
import com.dozamigos.dozamigos.client.gui.UpdaterScreen;
import com.dozamigos.dozamigos.config.DozamigosConfig;
import com.dozamigos.dozamigos.sync.UpdatePlan;
import com.dozamigos.dozamigos.sync.UpdateService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TitleScreenHooks {
    private static final int BTN_W = 200;
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 24;

    private static boolean checkedThisSession = false;
    private static boolean popupQueued = false;

    private TitleScreenHooks() {}

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen title)) {
            return;
        }

        if (DozamigosConfig.SHOW_TITLE_BUTTON.get()) {
            placeAboveSingleplayer(event, title);
        }

        maybeCheck(title);
    }

    /** Empurra o menu pra baixo e coloca o botao no lugar do Singleplayer original. */
    private static void placeAboveSingleplayer(ScreenEvent.Init.Post event, TitleScreen title) {
        int insertY = title.height / 4 + 48;
        int x = title.width / 2 - BTN_W / 2;

        List<AbstractWidget> toShift = new ArrayList<>();
        for (var listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget w && w.getY() >= insertY) {
                toShift.add(w);
            }
        }
        for (AbstractWidget w : toShift) {
            w.setY(w.getY() + BTN_GAP);
        }

        event.addListener(Button.builder(Component.translatable("dozamigos.button.menu"), b ->
                Minecraft.getInstance().setScreen(new UpdaterScreen(title))
        ).bounds(x, insertY, BTN_W, BTN_H).build());
    }

    private static void maybeCheck(TitleScreen title) {
        if (!DozamigosConfig.CHECK_ON_TITLE.get() || checkedThisSession) {
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
