package com.kiras.chaosevents.client;

import com.kiras.chaosevents.ChaosEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ScreamerScreen extends Screen {
    private static final ResourceLocation[] TEXTURES = {
            ResourceLocation.fromNamespaceAndPath(ChaosEvents.MODID, "textures/gui/screamer_1.png"),
            ResourceLocation.fromNamespaceAndPath(ChaosEvents.MODID, "textures/gui/screamer_2.png")
    };

    private final Screen previousScreen;
    private final int variant;
    private int ticksRemaining;

    public ScreamerScreen(Screen previousScreen, int variant, int ticksRemaining) {
        super(Component.empty());
        this.previousScreen = previousScreen;
        this.variant = Math.floorMod(variant, TEXTURES.length);
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, width, height, 0xFF000000);
        guiGraphics.blit(TEXTURES[variant], 0, 0, 0.0F, 0.0F, width, height, 256, 256);
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
