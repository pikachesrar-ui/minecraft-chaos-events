package com.kiras.chaosevents.client;

import com.kiras.chaosevents.ChaosEvents;
import com.kiras.chaosevents.network.EventAnnouncementPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
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
        int textWidth = Math.max(140, Math.min(300, screenWidth - 36));

        List<FormattedCharSequence> titleLines = font.split(Component.literal(title), textWidth);
        List<FormattedCharSequence> descriptionLines = font.split(Component.literal(description), textWidth);
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.addAll(titleLines);
        lines.addAll(descriptionLines);

        int maxTextLines = Math.max(3, (screenHeight - 90) / (font.lineHeight + 3));
        if (lines.size() > maxTextLines) {
            lines = new ArrayList<>(lines.subList(0, maxTextLines));
        }

        int totalHeight = lines.size() * (font.lineHeight + 3) + font.lineHeight + 10;
        int y = Math.max(18, (screenHeight - totalHeight) / 2);
        int titleCount = Math.min(titleLines.size(), lines.size());

        for (int index = 0; index < lines.size(); index++) {
            int color = index < titleCount ? 0xFF6B6B : 0xFFD37A;
            graphics.drawCenteredString(font, lines.get(index), screenWidth / 2, y, color);
            y += font.lineHeight + 3;
        }

        y += 5;
        String duration = "Длительность: " + formatSeconds(durationSeconds);
        graphics.drawCenteredString(font, duration, screenWidth / 2, y, 0xFFFFFF);
    }

    private static String formatSeconds(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
