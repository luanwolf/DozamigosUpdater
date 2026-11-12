package com.dozamigos.dozamigos.client.gui;

import com.dozamigos.dozamigos.sync.ChangeKind;
import com.dozamigos.dozamigos.sync.ModChange;
import com.dozamigos.dozamigos.sync.UpdatePlan;
import com.dozamigos.dozamigos.sync.UpdateService;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class UpdaterScreen extends Screen {
    private static final int COL_ADDED = 0xFF55FF55;
    private static final int COL_UPDATED = 0xFFFFFF55;
    private static final int COL_REMOVED = 0xFFFF5555;
    private static final int COL_GOLD = 0xFFE8C547;
    private static final int COL_MUTED = 0xFF9A9A9A;
    private static final int COL_PANEL = 0xEE141414;
    private static final int COL_PANEL_INNER = 0xFF1C1C1C;
    private static final int COL_BORDER = 0xFF5A5A5A;
    private static final int COL_HEADER = 0xFF252018;

    private final Screen parent;
    private ChangeList list;
    private Button applyButton;
    private Button refreshButton;
    private Button restartButton;
    private String status = "";
    private boolean statusIsError = false;
    private boolean busy = false;
    private UpdatePlan plan = UpdateService.lastPlan();

    private int panelLeft;
    private int panelTop;
    private int panelRight;
    private int panelBottom;

    /** No-arg para FancyMenu "Open Screen" pelo FQCN. */
    public UpdaterScreen() {
        this(Minecraft.getInstance().screen);
    }

    public UpdaterScreen(Screen parent) {
        super(Component.translatable("dozamigos.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelLeft = Math.max(20, width / 2 - 200);
        panelRight = Math.min(width - 20, width / 2 + 200);
        panelTop = 28;
        panelBottom = height - 36;

        int listTop = panelTop + 52;
        int listBottom = panelBottom - 48;
        list = new ChangeList(minecraft, width, height, listTop, listBottom, 28);
        addWidget(list);

        int btnY = height - 28;
        refreshButton = addRenderableWidget(Button.builder(Component.translatable("dozamigos.button.refresh"), b -> refresh())
                .bounds(width / 2 - 206, btnY, 96, 20).build());
        applyButton = addRenderableWidget(Button.builder(Component.translatable("dozamigos.button.apply"), b -> apply())
                .bounds(width / 2 - 106, btnY, 96, 20).build());
        restartButton = addRenderableWidget(Button.builder(Component.translatable("dozamigos.button.restart"), b -> restartGame())
                .bounds(width / 2 - 6, btnY, 96, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(width / 2 + 94, btnY, 96, 20).build());

        reloadList();
        if (plan == null || (plan.isEmpty() && UpdateService.lastError() == null && !UpdateService.isBusy())) {
            refresh();
        } else if (UpdateService.lastError() != null && (plan == null || plan.isEmpty())) {
            status = Component.translatable("dozamigos.status.error", UpdateService.lastError()).getString();
            statusIsError = true;
        }
        updateButtons();
    }

    private void restartGame() {
        if (minecraft != null) {
            minecraft.stop();
        }
    }

    private void refresh() {
        if (busy) {
            return;
        }
        busy = true;
        statusIsError = false;
        status = Component.translatable("dozamigos.status.checking").getString();
        updateButtons();
        UpdateService.checkAsync().whenComplete((result, err) -> {
            if (minecraft == null) {
                return;
            }
            minecraft.execute(() -> {
                busy = false;
                if (err != null) {
                    Throwable root = err.getCause() == null ? err : err.getCause();
                    String msg = root.getMessage() == null ? root.toString() : root.getMessage();
                    status = Component.translatable("dozamigos.status.error", msg).getString();
                    statusIsError = true;
                } else {
                    plan = result;
                    statusIsError = false;
                    status = result.isEmpty()
                            ? Component.translatable("dozamigos.status.uptodate").getString()
                            : Component.translatable(
                                    "dozamigos.status.found",
                                    result.count(ChangeKind.ADDED),
                                    result.count(ChangeKind.UPDATED),
                                    result.count(ChangeKind.REMOVED)
                            ).getString();
                    reloadList();
                }
                updateButtons();
            });
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (busy) {
            String msg = UpdateService.progressMessage();
            if (msg != null && !msg.isBlank()) {
                status = msg;
                statusIsError = false;
            }
        }
    }

    private void apply() {
        if (busy || plan == null || plan.isEmpty()) {
            return;
        }
        busy = true;
        statusIsError = false;
        status = Component.translatable("dozamigos.status.applying").getString();
        updateButtons();
        UpdatePlan snapshot = plan;
        UpdateService.applyAsync(snapshot, msg -> {
            if (minecraft != null) {
                minecraft.execute(() -> {
                    status = msg;
                    statusIsError = false;
                });
            }
        }).whenComplete((result, err) -> {
            if (minecraft == null) {
                return;
            }
            minecraft.execute(() -> {
                busy = false;
                if (err != null) {
                    Throwable root = err.getCause() == null ? err : err.getCause();
                    String msg = root.getMessage() == null ? root.toString() : root.getMessage();
                    status = Component.translatable("dozamigos.status.error", msg).getString();
                    statusIsError = true;
                } else if (result.ok()) {
                    status = Component.translatable("dozamigos.status.done", result.applied()).getString();
                    statusIsError = false;
                    plan = new UpdatePlan(snapshot.packVersion(), List.of());
                    reloadList();
                } else {
                    status = Component.translatable("dozamigos.status.partial", result.applied(), result.errors().size()).getString();
                    statusIsError = true;
                }
                updateButtons();
            });
        });
    }

    private void reloadList() {
        list.clearAll();
        if (plan == null) {
            return;
        }
        for (ModChange change : plan.changes()) {
            list.addChange(new ChangeEntry(change));
        }
    }

    private void updateButtons() {
        applyButton.active = !busy && plan != null && !plan.isEmpty();
        refreshButton.active = !busy;
        // Ativo sempre; depois do apply o status ja pede reinicio.
        restartButton.active = !busy;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fillGradient(0, 0, width, height, 0xA0101010, 0xD0101010);

        drawPanel(g, panelLeft, panelTop, panelRight, panelBottom);
        drawHeader(g);
        drawLegend(g);

        list.render(g, mouseX, mouseY, partialTick);

        if (list.children().isEmpty()) {
            drawEmptyState(g);
        }

        drawStatusBar(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics g, int left, int top, int right, int bottom) {
        g.fill(left - 2, top - 2, right + 2, bottom + 2, 0x66000000);
        g.fill(left, top, right, bottom, COL_PANEL);
        g.fill(left + 1, top + 1, right - 1, bottom - 1, COL_PANEL_INNER);
        g.renderOutline(left, top, right - left, bottom - top, COL_BORDER);
        g.renderOutline(left + 1, top + 1, right - left - 2, bottom - top - 2, 0xFF2A2A2A);
    }

    private void drawHeader(GuiGraphics g) {
        int left = panelLeft + 2;
        int right = panelRight - 2;
        int top = panelTop + 2;
        int bottom = panelTop + 28;
        g.fill(left, top, right, bottom, COL_HEADER);
        g.fill(left, bottom - 1, right, bottom, 0xFF3D3424);

        g.drawCenteredString(font, title, width / 2, top + 6, COL_GOLD);

        String pack = plan == null || plan.packVersion().isBlank()
                ? Component.translatable("dozamigos.screen.pack.unknown").getString()
                : Component.translatable("dozamigos.screen.pack", plan.packVersion()).getString();
        g.drawCenteredString(font, pack, width / 2, top + 16, COL_MUTED);
    }

    private void drawLegend(GuiGraphics g) {
        int y = panelTop + 34;
        int x = panelLeft + 10;
        long added = plan == null ? 0 : plan.count(ChangeKind.ADDED);
        long updated = plan == null ? 0 : plan.count(ChangeKind.UPDATED);
        long removed = plan == null ? 0 : plan.count(ChangeKind.REMOVED);

        x = drawChip(g, x, y, "+", Component.translatable("dozamigos.legend.added_short").getString(), added, COL_ADDED);
        x = drawChip(g, x + 6, y, "~", Component.translatable("dozamigos.legend.updated_short").getString(), updated, COL_UPDATED);
        drawChip(g, x + 6, y, "-", Component.translatable("dozamigos.legend.removed_short").getString(), removed, COL_REMOVED);
    }

    private int drawChip(GuiGraphics g, int x, int y, String mark, String label, long count, int color) {
        String text = mark + " " + label + " " + count;
        int w = font.width(text) + 10;
        int h = 12;
        g.fill(x, y, x + w, y + h, 0xFF101010);
        g.fill(x, y, x + 2, y + h, color);
        g.renderOutline(x, y, w, h, 0xFF333333);
        g.drawString(font, text, x + 5, y + 2, color, false);
        return x + w;
    }

    private void drawEmptyState(GuiGraphics g) {
        int cx = width / 2;
        int cy = (panelTop + panelBottom) / 2 + 8;
        Component main;
        Component hint;
        int mainColor;
        if (statusIsError) {
            main = Component.translatable("dozamigos.empty.error");
            hint = Component.translatable("dozamigos.empty.error.hint");
            mainColor = COL_REMOVED;
        } else if (busy) {
            main = Component.translatable("dozamigos.empty.checking");
            hint = Component.literal("");
            mainColor = COL_GOLD;
        } else {
            main = Component.translatable("dozamigos.empty.uptodate");
            hint = Component.translatable("dozamigos.empty.uptodate.hint");
            mainColor = COL_ADDED;
        }
        g.drawCenteredString(font, main, cx, cy - 6, mainColor);
        if (!hint.getString().isEmpty()) {
            g.drawCenteredString(font, hint, cx, cy + 8, COL_MUTED);
        }
    }

    private void drawStatusBar(GuiGraphics g) {
        int left = panelLeft + 2;
        int right = panelRight - 2;
        int top = panelBottom - 44;
        int bottom = panelBottom - 2;
        g.fill(left, top, right, bottom, 0xFF121212);
        g.fill(left, top, right, top + 1, 0xFF3A3A3A);

        int color = statusIsError ? COL_REMOVED : COL_GOLD;
        int maxW = right - left - 12;
        String label = status == null ? "" : status;
        if (busy) {
            label = UpdateService.progressPercent() + "%  " + label;
        }
        List<FormattedCharSequence> lines = font.split(Component.literal(label), maxW);
        int y = top + 4;
        for (int i = 0; i < Math.min(lines.size(), 1); i++) {
            g.drawCenteredString(font, lines.get(i), width / 2, y, color);
        }

        if (busy) {
            drawProgressBar(g, left + 8, top + 18, right - left - 16, 12, UpdateService.progressPercent());
        }
    }

    private void drawProgressBar(GuiGraphics g, int x, int y, int w, int h, int percent) {
        int pct = Math.max(0, Math.min(100, percent));
        g.fill(x, y, x + w, y + h, 0xFF0A0A0A);
        g.renderOutline(x, y, w, h, 0xFF3A3A3A);
        int fill = Math.max(0, (w - 2) * pct / 100);
        if (fill > 0) {
            g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, 0xFF3D8B40);
            g.fill(x + 1, y + 1, x + 1 + fill, y + 3, 0xFF55C75A);
        }
        g.drawCenteredString(font, pct + "%", x + w / 2, y + (h - 8) / 2, 0xFFECECEC);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private class ChangeList extends ObjectSelectionList<ChangeEntry> {
        ChangeList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
            super(mc, width, height, top, bottom, itemHeight);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
        }

        void clearAll() {
            clearEntries();
        }

        void addChange(ChangeEntry entry) {
            addEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return panelRight - panelLeft - 24;
        }

        @Override
        protected int getScrollbarPosition() {
            return panelRight - 10;
        }

        @Override
        protected void renderBackground(GuiGraphics g) {
            // panel already drawn
        }
    }

    private class ChangeEntry extends ObjectSelectionList.Entry<ChangeEntry> {
        private final ModChange change;

        ChangeEntry(ModChange change) {
            this.change = change;
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            int accent = switch (change.kind()) {
                case ADDED -> COL_ADDED;
                case UPDATED -> COL_UPDATED;
                case REMOVED -> COL_REMOVED;
            };
            String badge = switch (change.kind()) {
                case ADDED -> "+";
                case UPDATED -> "~";
                case REMOVED -> "-";
            };

            int rowL = left;
            int rowR = left + rowWidth;
            int bg = hovering ? 0xFF2A2A2A : (index % 2 == 0 ? 0xFF181818 : 0xFF151515);
            g.fill(rowL, top, rowR, top + rowHeight - 2, bg);
            g.fill(rowL, top, rowL + 3, top + rowHeight - 2, accent);

            // badge pill
            int bx = rowL + 8;
            int by = top + 6;
            g.fill(bx, by, bx + 12, by + 12, 0xFF0A0A0A);
            g.renderOutline(bx, by, 12, 12, accent);
            g.drawCenteredString(font, badge, bx + 6, by + 2, accent);

            String titleLine = "[" + change.targetLabel() + "] " + change.id();
            String file;
            if (change.kind() == ChangeKind.UPDATED
                    && change.oldFile() != null
                    && change.newFile() != null
                    && !change.oldFile().equals(change.newFile())) {
                file = change.oldFile() + "  →  " + change.newFile();
            } else {
                file = change.displayFile();
            }

            int textX = bx + 18;
            int maxTextW = Math.max(16, rowR - textX - 6);
            g.drawString(font, ellipsize(titleLine, maxTextW), textX, top + 3, 0xFFECECEC, false);
            g.drawString(font, ellipsize(file, maxTextW), textX, top + 14, COL_MUTED, false);
        }

        private String ellipsize(String text, int maxWidth) {
            if (font.width(text) <= maxWidth) {
                return text;
            }
            String dots = "...";
            int budget = Math.max(0, maxWidth - font.width(dots));
            return font.plainSubstrByWidth(text, budget) + dots;
        }

        @Override
        public Component getNarration() {
            return Component.literal(change.displayFile());
        }
    }
}
