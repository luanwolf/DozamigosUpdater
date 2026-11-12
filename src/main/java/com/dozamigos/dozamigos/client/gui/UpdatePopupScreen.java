package com.dozamigos.dozamigos.client.gui;

import com.dozamigos.dozamigos.sync.ChangeKind;
import com.dozamigos.dozamigos.sync.UpdatePlan;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Popup estilo Minecraft quando o pack tem mudancas. */
public class UpdatePopupScreen extends Screen {
    private static final int COL_GOLD = 0xFFE8C547;
    private static final int COL_MUTED = 0xFF9A9A9A;

    private final Screen parent;
    private final UpdatePlan plan;

    public UpdatePopupScreen(Screen parent, UpdatePlan plan) {
        super(Component.translatable("dozamigos.popup.title"));
        this.parent = parent;
        this.plan = plan;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        addRenderableWidget(Button.builder(Component.translatable("dozamigos.popup.open"), b ->
                minecraft.setScreen(new UpdaterScreen(parent))
        ).bounds(cx - 105, cy + 48, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("dozamigos.popup.later"), b ->
                onClose()
        ).bounds(cx + 5, cy + 48, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (parent != null) {
            parent.render(g, 0, 0, partialTick);
        }
        g.fillGradient(0, 0, width, height, 0xC0101010, 0xE0101010);

        int cx = width / 2;
        int cy = height / 2;
        int left = cx - 140;
        int top = cy - 70;
        int right = cx + 140;
        int bottom = cy + 80;

        g.fill(left - 2, top - 2, right + 2, bottom + 2, 0x66000000);
        g.fill(left, top, right, bottom, 0xEE141414);
        g.fill(left + 1, top + 1, right - 1, bottom - 1, 0xFF1C1C1C);
        g.renderOutline(left, top, right - left, bottom - top, 0xFF5A5A5A);

        g.fill(left + 2, top + 2, right - 2, top + 26, 0xFF252018);
        g.fill(left + 2, top + 25, right - 2, top + 26, 0xFF3D3424);
        g.drawCenteredString(font, title, cx, top + 10, COL_GOLD);

        if (!plan.packVersion().isBlank()) {
            g.drawCenteredString(font, Component.translatable("dozamigos.screen.pack", plan.packVersion()), cx, top + 34, COL_MUTED);
        }

        drawMiniChip(g, cx - 100, cy - 8, "+", plan.count(ChangeKind.ADDED), 0xFF55FF55);
        drawMiniChip(g, cx - 30, cy - 8, "~", plan.count(ChangeKind.UPDATED), 0xFFFFFF55);
        drawMiniChip(g, cx + 40, cy - 8, "-", plan.count(ChangeKind.REMOVED), 0xFFFF5555);

        g.drawCenteredString(font, Component.translatable("dozamigos.popup.hint"), cx, cy + 20, COL_MUTED);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawMiniChip(GuiGraphics g, int x, int y, String mark, long count, int color) {
        String text = mark + " " + count;
        int w = font.width(text) + 10;
        g.fill(x, y, x + w, y + 14, 0xFF101010);
        g.fill(x, y, x + 2, y + 14, color);
        g.renderOutline(x, y, w, 14, 0xFF333333);
        g.drawString(font, text, x + 5, y + 3, color, false);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
