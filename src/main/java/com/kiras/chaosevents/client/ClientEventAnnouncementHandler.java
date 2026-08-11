package com.kiras.chaosevents.client;

import com.kiras.chaosevents.ChaosEvents;
import com.kiras.chaosevents.network.EventAnnouncementPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT, modid = ChaosEvents.MODID)
public final class ClientEventAnnouncementHandler {
    private static final long DISPLAY_MILLIS = 5_500L;

    private static String title = "";
    private static String description = "";
    private static int durationSeconds;
    private static long visibleUntil;

    private ClientEventAnnouncementHandler() {
    }

    public static void show(EventAnnouncementPayload payload) {
        title = payload.title();
        description = payload.description();
        durationSeconds = Math.max(0, payload.durationSeconds());
        visibleUntil = System.currentTimeMillis() + DISPLAY_MILLIS;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (System.currentTimeMillis() >= visibleUntil) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int panelSize = Math.max(140, Math.min(230, Math.min(screenWidth - 36, screenHeight - 36)));
        int left = (screenWidth - panelSize) / 2;
        int top = (screenHeight - panelSize) / 2;
        int right = left + panelSize;
        int bottom = top + panelSize;
        int textWidth = panelSize - 28;

        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, 0xCC5A1010);
        graphics.fill(left, top, right, bottom, 0xD0141414);

        List<FormattedCharSequence> titleLines = font.split(net.minecraft.network.chat.Component.literal(title), textWidth);
        List<FormattedCharSequence> descriptionLines = font.split(
                net.minecraft.network.chat.Component.literal(description), textWidth);
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.addAll(titleLines);
        lines.addAll(descriptionLines);

        int maxTextLines = Math.max(3, (panelSize - 58) / (font.lineHeight + 3));
        if (lines.size() > maxTextLines) {
            lines = new ArrayList<>(lines.subList(0, maxTextLines));
        }

        int totalHeight = lines.size() * (font.lineHeight + 3) + 22;
        int y = top + Math.max(12, (panelSize - totalHeight) / 2);
        int titleCount = Math.min(titleLines.size(), lines.size());

        for (int index = 0; index < lines.size(); index++) {
            int color = index < titleCount ? 0xFF6B6B : 0xFFD37A;
            graphics.drawCenteredString(font, lines.get(index), screenWidth / 2, y, color);
            y += font.lineHeight + 3;
        }

        String duration = "Длительность: " + formatSeconds(durationSeconds);
        graphics.drawCenteredString(font, duration, screenWidth / 2, bottom - 22, 0xFFFFFF);
    }

    private static String formatSeconds(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
