package com.kiras.chaosevents.client;

import com.kiras.chaosevents.ChaosEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Displays one numbered PNG from a matching PNG/OGG screamer pair. */
@OnlyIn(Dist.CLIENT)
public final class ScreamerScreen extends Screen {
    private final Screen previousScreen;
    private final int slot;
    private int ticksRemaining;

    public ScreamerScreen(Screen previousScreen, int slot, int ticksRemaining) {
        super(Component.empty());
        this.previousScreen = previousScreen;
        this.slot = slot;
        this.ticksRemaining = ticksRemaining;
    }

    @Override
    public void tick() {
        ticksRemaining--;
        if (ticksRemaining <= 0 && minecraft != null) {
            minecraft.setScreen(previousScreen);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation customTexture = textureForSlot(slot);
        if (slot > 0 && minecraft != null
                && minecraft.getResourceManager().getResource(customTexture).isPresent()) {
            graphics.fill(0, 0, width, height, 0xFF000000);
            graphics.blit(customTexture, 0, 0, 0.0F, 0.0F, width, height, width, height);
            return;
        }
        renderFallback(graphics);
    }

    private static ResourceLocation textureForSlot(int slot) {
        return ResourceLocation.fromNamespaceAndPath(
                ChaosEvents.MODID,
                "textures/gui/screamers/" + Math.max(1, slot) + ".png"
        );
    }

    private void renderFallback(GuiGraphics graphics) {
        boolean alternate = Math.floorMod(slot, 2) == 0;
        int background = alternate ? 0xFF000008 : 0xFF150000;
        int face = alternate ? 0xFFBFC7D8 : 0xFFE6D4C4;
        int accent = alternate ? 0xFF6D3CFF : 0xFFFF1010;
        graphics.fill(0, 0, width, height, background);

        int cx = width / 2;
        int cy = height / 2;
        int faceWidth = Math.max(160, width / 3);
        int faceHeight = Math.max(190, height * 2 / 3);
        int left = cx - faceWidth / 2;
        int top = cy - faceHeight / 2;

        graphics.fill(left, top, left + faceWidth, top + faceHeight, face);
        graphics.fill(left + faceWidth / 8, top + faceHeight / 4,
                left + faceWidth * 3 / 8, top + faceHeight / 2, 0xFF000000);
        graphics.fill(left + faceWidth * 5 / 8, top + faceHeight / 4,
                left + faceWidth * 7 / 8, top + faceHeight / 2, 0xFF000000);
        graphics.fill(left + faceWidth / 5, top + faceHeight * 3 / 5,
                left + faceWidth * 4 / 5, top + faceHeight * 9 / 10, 0xFF000000);
        graphics.fill(left + faceWidth / 4, top + faceHeight / 3,
                left + faceWidth / 3, top + faceHeight * 5 / 12, accent);
        graphics.fill(left + faceWidth * 2 / 3, top + faceHeight / 3,
                left + faceWidth * 3 / 4, top + faceHeight * 5 / 12, accent);

        int jitter = (ticksRemaining % 4) - 2;
        graphics.drawCenteredString(font, alternate ? "НЕ ОБОРАЧИВАЙСЯ" : "RUN",
                cx + jitter, Math.max(8, top - 18), accent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
