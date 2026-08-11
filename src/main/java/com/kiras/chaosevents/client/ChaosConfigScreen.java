package com.kiras.chaosevents.client;

import com.kiras.chaosevents.config.ChaosConfigCatalog;
import com.kiras.chaosevents.config.ChaosConfigCategory;
import com.kiras.chaosevents.config.ChaosConfigEntry;
import com.kiras.chaosevents.config.ChaosConfigManager;
import com.kiras.chaosevents.network.ConfigSavePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ChaosConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 34;
    private static final int HEADER_HEIGHT = 45;
    private static final int FOOTER_HEIGHT = 48;

    private final ChaosConfigCategory category;
    private final List<ChaosConfigEntry> entries;
    private final Set<String> disabledIds;

    private int page;
    private int pageSize;
    private int panelX;
    private int panelWidth;

    public ChaosConfigScreen(ChaosConfigCategory category, Set<String> disabledIds) {
        super(Component.literal("Chaos Events — " + category.displayName()));
        this.category = category;
        this.entries = ChaosConfigCatalog.entries(category);
        this.disabledIds = new HashSet<>(disabledIds);
    }

    @Override
    protected void init() {
        panelWidth = Math.max(260, Math.min(500, width - 32));
        panelX = (width - panelWidth) / 2;
        pageSize = Math.max(4, Math.min(10, (height - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT));
        page = Math.max(0, Math.min(page, Math.max(0, getPageCount() - 1)));
        rebuildControls();
    }

    private void rebuildControls() {
        clearWidgets();
        int start = page * pageSize;
        int end = Math.min(entries.size(), start + pageSize);
        int toggleX = panelX + panelWidth - 82;

        for (int index = start; index < end; index++) {
            ChaosConfigEntry entry = entries.get(index);
            int row = index - start;
            int y = HEADER_HEIGHT + row * ROW_HEIGHT + 6;
            boolean enabled = !disabledIds.contains(entry.id());
            Component text = Component.literal(enabled ? "ВКЛ" : "ВЫКЛ");
            addRenderableWidget(Button.builder(text, button -> {
                        if (disabledIds.contains(entry.id())) {
                            disabledIds.remove(entry.id());
                        } else {
                            disabledIds.add(entry.id());
                        }
                        rebuildControls();
                    })
                    .bounds(toggleX, y, 70, 20)
                    .build());
        }

        int footerY = height - 34;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                    if (page > 0) {
                        page--;
                        rebuildControls();
                    }
                })
                .bounds(panelX, footerY, 32, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                    if (page + 1 < getPageCount()) {
                        page++;
                        rebuildControls();
                    }
                })
                .bounds(panelX + 38, footerY, 32, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Сохранить"), button -> saveAndClose())
                .bounds(panelX + panelWidth - 190, footerY, 90, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), button -> onClose())
                .bounds(panelX + panelWidth - 94, footerY, 94, 20)
                .build());
    }

    private int getPageCount() {
        if (entries.isEmpty() || pageSize <= 0) {
            return 1;
        }
        return (entries.size() + pageSize - 1) / pageSize;
    }

    private void saveAndClose() {
        PacketDistributor.sendToServer(new ConfigSavePayload(
                category.id(),
                ChaosConfigManager.encodeDisabled(disabledIds)
        ));
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        guiGraphics.drawCenteredString(font,
                "Страница " + (page + 1) + "/" + getPageCount() + " • отключено: " + disabledIds.size(),
                width / 2, 28, 0xB8B8B8);

        int start = page * pageSize;
        int end = Math.min(entries.size(), start + pageSize);
        int textWidth = panelWidth - 100;

        for (int index = start; index < end; index++) {
            ChaosConfigEntry entry = entries.get(index);
            int row = index - start;
            int y = HEADER_HEIGHT + row * ROW_HEIGHT;
            boolean enabled = !disabledIds.contains(entry.id());
            int background = enabled ? 0x551B3A24 : 0x553A1B1B;
            guiGraphics.fill(panelX, y, panelX + panelWidth, y + ROW_HEIGHT - 2, background);

            var lines = font.split(Component.literal(entry.label()), textWidth);
            if (!lines.isEmpty()) {
                guiGraphics.drawString(font, lines.getFirst(), panelX + 7, y + 5, 0xFFFFFF, false);
            }
            if (lines.size() > 1) {
                guiGraphics.drawString(font, lines.get(1), panelX + 7, y + 17, 0xD0D0D0, false);
            } else if (entry.description() != null && !entry.description().isBlank()) {
                var descriptionLines = font.split(Component.literal(entry.description()), textWidth);
                if (!descriptionLines.isEmpty()) {
                    guiGraphics.drawString(font, descriptionLines.getFirst(), panelX + 7, y + 17, 0xA0A0A0, false);
                }
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
