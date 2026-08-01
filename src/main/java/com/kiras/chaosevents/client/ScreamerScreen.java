package com.kiras.chaosevents.client;

import com.kiras.chaosevents.ChaosEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Uses custom 256x256 PNGs when present, otherwise draws a procedural fallback. */
@OnlyIn(Dist.CLIENT)
public final class ScreamerScreen extends Screen {
    private static final ResourceLocation[] CUSTOM_TEXTURES = {
            ResourceLocation.fromNamespaceAndPath(ChaosEvents.MODID, "textures/gui/screamer_1.png"),
            ResourceLocation.fromNamespaceAndPath(ChaosEvents.MODID, "textures/gui/screamer_2.png")
    };

    private final Screen previousScreen;
    private final int variant;
    private int ticksRemaining;

    public ScreamerScreen(Screen previousScreen, int variant, int ticksRemaining) {
        super(Component.empty());
        this.previousScreen = previousScreen;
        this.variant = Math.floorMod(variant, CUSTOM_TEXTURES.length);
        this.ticksRemaining = ticksRemaining;
    }

    @Override
    public void tick() {
        ticksRemaining--;
        if (ticksRemaining <= 0 && minecraft != null) minecraft.setScreen(previousScreen);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation customTexture = CUSTOM_TEXTURES[variant];
        if (minecraft != null && minecraft.getResourceManager().getResource(customTexture).isPresent()) {
            graphics.fill(0, 0, width, height, 0xFF000000);
            graphics.blit(customTexture, 0, 0, 0.0F, 0.0F, width, height, 256, 256);
            return;
        }
        renderFallback(graphics);
    }

    private void renderFallback(GuiGraphics graphics) {
        int background = variant == 0 ? 0xFF150000 : 0xFF000008;
        int face = variant == 0 ? 0xFFE6D4C4 : 0xFFBFC7D8;
        int accent = variant == 0 ? 0xFFFF1010 : 0xFF6D3CFF;
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
        graphics.drawCenteredString(font, variant == 0 ? "RUN" : "НЕ ОБОРАЧИВАЙСЯ",
                cx + jitter, Math.max(8, top - 18), accent);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
}
