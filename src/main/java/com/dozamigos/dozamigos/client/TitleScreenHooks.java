package com.dozamigos.dozamigos.client;

import com.dozamigos.dozamigos.client.fancymenu.FancyMenuBridge;
import com.dozamigos.dozamigos.client.gui.UpdatePopupScreen;
import com.dozamigos.dozamigos.client.gui.UpdaterScreen;
import com.dozamigos.dozamigos.config.DozamigosConfig;
import com.dozamigos.dozamigos.sync.UpdatePlan;
import com.dozamigos.dozamigos.sync.UpdateService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TitleScreenHooks {
    private static final int BTN_W = 200;
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 24;
    private static final int BANNER_GAP = 16;
    private static final int COL_GOLD = 0xFFE8C547;
    private static final int COL_MUTED = 0xFFCCCCCC;
    private static final int COL_PANEL = 0xCC1A1410;

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

        ServerStatusMonitor.ensureProbing();
        maybeCheck(title);
    }

    /**
     * FancyMenu pinta o layout do title no Post — redesenha o popup por cima.
     * Banner de manutencao tambem aqui (LOWEST): so desenho, sem widget, pra nao
     * roubar clique dos botoes do FancyMenu / vanilla.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof UpdatePopupScreen popup) {
            popup.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            return;
        }
        if (!(event.getScreen() instanceof TitleScreen title)) {
            return;
        }
        ServerStatusMonitor.ensureProbing();
        // Com FancyMenu o aviso e um elemento editavel no layout (requisito dozamigos_server_maintenance).
        if (FancyMenuBridge.isPresent()) {
            return;
        }
        if (DozamigosConfig.SHOW_MAINTENANCE.get()
                && ServerStatusMonitor.status() == ServerStatusMonitor.Status.OFFLINE) {
            drawMaintenanceBanner(event.getGuiGraphics(), title);
        }
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

    /**
     * Painel a direita da coluna de botoes. Nao adiciona listener — o historico
     * de FancyMenu furando overlay / botao deslocado exige desenho puro no Post.
     */
    private static void drawMaintenanceBanner(GuiGraphics g, TitleScreen title) {
        Font font = Minecraft.getInstance().font;
        Component line1 = Component.translatable("dozamigos.maintenance.title");
        Component line2 = Component.translatable("dozamigos.maintenance.hint");
        int textW = Math.max(font.width(line1), font.width(line2));
        int pad = 8;
        int w = textW + pad * 2;
        int h = 34;
        int x = findMenuRight(title) + BANNER_GAP;
        int y = title.height / 2 - h / 2;
        if (x + w > title.width - 4) {
            x = Math.max(4, title.width - w - 4);
        }

        // ponytail: pulso leve de alpha/cor (~1.2s) pra chamar atencao sem widget
        float pulse = 0.55f + 0.45f * (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 380.0));
        int titleColor = lerpArgb(0xFFAA7700, COL_GOLD, pulse);
        int borderColor = lerpArgb(0xFF664400, COL_GOLD, pulse);
        int panelA = (int) (0x99 + pulse * 0x44) << 24;
        int panel = (COL_PANEL & 0x00FFFFFF) | panelA;

        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF000000);
        g.fill(x, y, x + w, y + h, panel);
        g.renderOutline(x, y, w, h, borderColor);
        g.drawString(font, line1, x + pad, y + 7, titleColor, false);
        g.drawString(font, line2, x + pad, y + 19, COL_MUTED, false);
        g.flush();
    }

    private static int lerpArgb(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = (int) (((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
        int g = (int) (((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Borda direita da coluna central de botoes (vanilla ~200px ou FancyMenu ~120px). */
    private static int findMenuRight(TitleScreen title) {
        int fallback = title.width / 2 + 100;
        int best = title.width / 2;
        int yMin = title.height / 5;
        int yMax = title.height * 4 / 5;
        for (GuiEventListener child : title.children()) {
            if (!(child instanceof AbstractWidget w)) {
                continue;
            }
            if (w.getY() < yMin || w.getY() > yMax) {
                continue;
            }
            if (w.getWidth() < 60 || w.getWidth() > 240) {
                continue;
            }
            int cx = w.getX() + w.getWidth() / 2;
            if (Math.abs(cx - title.width / 2) > title.width / 3) {
                continue;
            }
            best = Math.max(best, w.getX() + w.getWidth());
        }
        return best > title.width / 2 ? best : fallback;
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
