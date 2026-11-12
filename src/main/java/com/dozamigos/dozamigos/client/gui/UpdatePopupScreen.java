package com.dozamigos.dozamigos.client.gui;

import com.dozamigos.dozamigos.sync.ChangeKind;
import com.dozamigos.dozamigos.sync.FileKind;
import com.dozamigos.dozamigos.sync.UpdatePlan;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Popup de update. Nao chama parent.render() completo: no GuiGraphics 1.20.1 o texto
 * dos widgets do title screen flusha DEPOIS dos fills e furava o painel.
 */
public class UpdatePopupScreen extends Screen {
    private static final int COL_GOLD = 0xFFE8C547;
    private static final int COL_MUTED = 0xFF9A9A9A;
    private static final int COL_ADDED = 0xFF55FF55;
    private static final int COL_UPDATED = 0xFFFFFF55;
    private static final int COL_REMOVED = 0xFFFF5555;
    private static final int COL_PANEL = 0xFF141414;
    private static final int COL_PANEL_INNER = 0xFF1C1C1C;
    private static final int COL_BORDER = 0xFF5A5A5A;
    private static final int COL_HEADER = 0xFF252018;

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
        ).bounds(cx - 105, cy + 62, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("dozamigos.popup.later"), b ->
                onClose()
        ).bounds(cx + 5, cy + 62, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // So o fundo (panorama/dirt) — sem labels dos botoes do menu.
        if (parent != null) {
            parent.renderBackground(g);
        } else {
            renderBackground(g);
        }
        drawFrostedDim(g);

        int cx = width / 2;
        int cy = height / 2;
        int left = cx - 150;
        int top = cy - 88;
        int right = cx + 150;
        int bottom = cy + 96;

        g.fill(left - 3, top - 3, right + 3, bottom + 3, 0x88000000);
        g.fill(left, top, right, bottom, COL_PANEL);
        g.fill(left + 1, top + 1, right - 1, bottom - 1, COL_PANEL_INNER);
        g.renderOutline(left, top, right - left, bottom - top, COL_BORDER);
        g.renderOutline(left + 1, top + 1, right - left - 2, bottom - top - 2, 0xFF2A2A2A);

        g.fill(left + 2, top + 2, right - 2, top + 28, COL_HEADER);
        g.fill(left + 2, top + 27, right - 2, top + 28, 0xFF3D3424);
        g.drawCenteredString(font, title, cx, top + 10, COL_GOLD);

        if (!plan.packVersion().isBlank()) {
            g.drawCenteredString(font, Component.translatable("dozamigos.screen.pack", plan.packVersion()), cx, top + 36, COL_MUTED);
        }

        int rowY = top + 54;
        drawKindRow(g, left + 14, rowY, FileKind.MOD, "dozamigos.popup.mods");
        drawKindRow(g, left + 14, rowY + 22, FileKind.FILE, "dozamigos.popup.files");

        g.drawCenteredString(font, Component.translatable("dozamigos.popup.hint"), cx, cy + 40, COL_MUTED);
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Dim em camadas no lugar de blur real (1.20.1 nao expoe blur de menu). */
    private void drawFrostedDim(GuiGraphics g) {
        g.fill(0, 0, width, height, 0x66000000);
        g.fill(0, 0, width, height, 0x55000000);
        g.fillGradient(0, 0, width, height, 0xC0101018, 0xE0080810);
    }

    private void drawKindRow(GuiGraphics g, int x, int y, FileKind kind, String labelKey) {
        g.drawString(font, Component.translatable(labelKey), x, y + 3, COL_MUTED, false);
        int chipX = x + 58;
        chipX = drawMiniChip(g, chipX, y, "+", plan.count(ChangeKind.ADDED, kind), COL_ADDED);
        chipX = drawMiniChip(g, chipX + 6, y, "~", plan.count(ChangeKind.UPDATED, kind), COL_UPDATED);
        drawMiniChip(g, chipX + 6, y, "-", plan.count(ChangeKind.REMOVED, kind), COL_REMOVED);
    }

    private int drawMiniChip(GuiGraphics g, int x, int y, String mark, long count, int color) {
        String text = mark + " " + count;
        int w = font.width(text) + 10;
        g.fill(x, y, x + w, y + 14, 0xFF101010);
        g.fill(x, y, x + 2, y + 14, color);
        g.renderOutline(x, y, w, 14, 0xFF333333);
        g.drawString(font, text, x + 5, y + 3, color, false);
        return x + w;
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
