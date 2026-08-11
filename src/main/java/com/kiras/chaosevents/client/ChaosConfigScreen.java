package com.kiras.chaosevents.client;

import com.kiras.chaosevents.config.ChaosConfigCatalog;
import com.kiras.chaosevents.config.ChaosConfigCategory;
import com.kiras.chaosevents.config.ChaosConfigEntry;
import com.kiras.chaosevents.config.ChaosConfigManager;
import com.kiras.chaosevents.network.ConfigSavePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ChaosConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 34;
    private static final int HEADER_HEIGHT = 104;
    private static final int FOOTER_HEIGHT = 48;

    private final ChaosConfigCategory category;
    private final List<ChaosConfigEntry> entries;
    private final Set<String> disabledIds;

    private int page;
    private int pageSize;
    private int panelX;
    private int panelWidth;
    private String minIntervalText;
    private String maxIntervalText;
    private String validationMessage = "";
    private EditBox minIntervalBox;
    private EditBox maxIntervalBox;

    public ChaosConfigScreen(ChaosConfigCategory category, Set<String> disabledIds,
                             int minIntervalSeconds, int maxIntervalSeconds) {
        super(Component.literal("Chaos Events — " + category.displayName()));
        this.category = category;
        this.entries = ChaosConfigCatalog.entries(category);
        this.disabledIds = new HashSet<>(disabledIds);
        this.minIntervalText = formatSeconds(minIntervalSeconds);
        this.maxIntervalText = formatSeconds(maxIntervalSeconds);
    }

    @Override
    protected void init() {
        panelWidth = Math.max(260, Math.min(500, width - 32));
        panelX = (width - panelWidth) / 2;
        pageSize = Math.max(2, Math.min(10, (height - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT));
        page = Math.max(0, Math.min(page, Math.max(0, getPageCount() - 1)));
        rebuildControls();
    }

    private void rebuildControls() {
        preserveIntervalText();
        clearWidgets();

        int intervalY = 49;
        int fieldWidth = 72;
        int minX = panelX + 30;
        int maxX = panelX + 142;

        minIntervalBox = new EditBox(font, minX, intervalY, fieldWidth, 20,
                Component.literal("Минимальный интервал"));
        minIntervalBox.setMaxLength(8);
        minIntervalBox.setFilter(ChaosConfigScreen::isIntervalText);
        minIntervalBox.setValue(minIntervalText);
        addRenderableWidget(minIntervalBox);

        maxIntervalBox = new EditBox(font, maxX, intervalY, fieldWidth, 20,
                Component.literal("Максимальный интервал"));
        maxIntervalBox.setMaxLength(8);
        maxIntervalBox.setFilter(ChaosConfigScreen::isIntervalText);
        maxIntervalBox.setValue(maxIntervalText);
        addRenderableWidget(maxIntervalBox);

        addRenderableWidget(Button.builder(Component.literal("Сброс"), button -> {
                    minIntervalText = formatSeconds(category.defaultMinIntervalSeconds());
                    maxIntervalText = formatSeconds(category.defaultMaxIntervalSeconds());
                    validationMessage = "";
                    rebuildControls();
                })
                .bounds(panelX + panelWidth - 72, 74, 72, 20)
                .build());

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

    private void preserveIntervalText() {
        if (minIntervalBox != null) {
            minIntervalText = minIntervalBox.getValue();
        }
        if (maxIntervalBox != null) {
            maxIntervalText = maxIntervalBox.getValue();
        }
    }

    private int getPageCount() {
        if (entries.isEmpty() || pageSize <= 0) {
            return 1;
        }
        return (entries.size() + pageSize - 1) / pageSize;
    }

    private void saveAndClose() {
        preserveIntervalText();
        Integer minSeconds = parseInterval(minIntervalText);
        Integer maxSeconds = parseInterval(maxIntervalText);
        if (minSeconds == null || maxSeconds == null) {
            validationMessage = "Введите время как M:SS или количество секунд.";
            return;
        }
        if (minSeconds < ChaosConfigCategory.MIN_ALLOWED_INTERVAL_SECONDS
                || maxSeconds > ChaosConfigCategory.MAX_ALLOWED_INTERVAL_SECONDS) {
            validationMessage = "Допустимый интервал: от 0:10 до 360:00.";
            return;
        }
        if (minSeconds > maxSeconds) {
            validationMessage = "Минимальный интервал не может быть больше максимального.";
            return;
        }

        PacketDistributor.sendToServer(new ConfigSavePayload(
                category.id(),
                ChaosConfigManager.encodeDisabled(disabledIds),
                minSeconds,
                maxSeconds
        ));
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, category.intervalLabel(), width / 2, 27, 0xFFD37A);

        guiGraphics.drawString(font, "От", panelX + 8, 55, 0xD0D0D0, false);
        guiGraphics.drawString(font, "до", panelX + 118, 55, 0xD0D0D0, false);
        guiGraphics.drawString(font, "Формат: M:SS или секунды", panelX, 80, 0x909090, false);

        if (validationMessage.isBlank()) {
            guiGraphics.drawCenteredString(font,
                    "Страница " + (page + 1) + "/" + getPageCount() + " • отключено: " + disabledIds.size(),
                    width / 2, 94, 0xB8B8B8);
        } else {
            guiGraphics.drawCenteredString(font, validationMessage, width / 2, 94, 0xFF6666);
        }

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

    private static boolean isIntervalText(String value) {
        if (value == null || value.length() > 8) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!Character.isDigit(character) && character != ':') {
                return false;
            }
        }
        return true;
    }

    private static Integer parseInterval(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            int colon = trimmed.indexOf(':');
            if (colon < 0) {
                return Integer.parseInt(trimmed);
            }
            if (colon == 0 || colon != trimmed.lastIndexOf(':') || colon == trimmed.length() - 1) {
                return null;
            }
            int minutes = Integer.parseInt(trimmed.substring(0, colon));
            int seconds = Integer.parseInt(trimmed.substring(colon + 1));
            if (minutes < 0 || seconds < 0 || seconds >= 60) {
                return null;
            }
            return Math.addExact(Math.multiplyExact(minutes, 60), seconds);
        } catch (NumberFormatException | ArithmeticException exception) {
            return null;
        }
    }

    private static String formatSeconds(int totalSeconds) {
        int safeSeconds = Math.max(0, totalSeconds);
        return String.format("%d:%02d", safeSeconds / 60, safeSeconds % 60);
    }
}
