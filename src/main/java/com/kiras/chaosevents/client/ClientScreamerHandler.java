package com.kiras.chaosevents.client;

import com.kiras.chaosevents.ChaosEvents;
import com.kiras.chaosevents.network.ScreamerPayload;
import com.kiras.chaosevents.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@OnlyIn(Dist.CLIENT)
public final class ClientScreamerHandler {
    private static final int MIN_DURATION_TICKS = 8;
    private static final int MAX_DURATION_TICKS = 3 * 20;
    private static int lastSlot = -1;

    private ClientScreamerHandler() {
    }

    public static void show(ScreamerPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        int slot = chooseAvailablePair(minecraft);
        SoundEvent sound;
        float pitch;
        if (slot > 0) {
            sound = ModSounds.screamer(slot);
            pitch = 1.0F;
        } else {
            sound = SoundEvents.WARDEN_ROAR;
            pitch = 0.8F;
        }

        SoundInstance soundInstance = SimpleSoundInstance.forUI(sound, pitch, 2.0F);
        minecraft.getSoundManager().play(soundInstance);

        int durationTicks = Math.max(
                MIN_DURATION_TICKS,
                Math.min(MAX_DURATION_TICKS, payload.durationTicks())
        );
        minecraft.setScreen(new ScreamerScreen(
                minecraft.screen,
                slot,
                durationTicks,
                soundInstance
        ));
    }

    private static int chooseAvailablePair(Minecraft minecraft) {
        List<Integer> available = new ArrayList<>();
        for (int slot = 1; slot <= ModSounds.SCREAMER_SLOT_COUNT; slot++) {
            if (hasCompletePair(minecraft, slot)) {
                available.add(slot);
            }
        }

        if (available.size() > 1) {
            available.remove(Integer.valueOf(lastSlot));
        }
        if (available.isEmpty()) {
            return -1;
        }

        int selected = available.get(ThreadLocalRandom.current().nextInt(available.size()));
        lastSlot = selected;
        return selected;
    }

    private static boolean hasCompletePair(Minecraft minecraft, int slot) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
                ChaosEvents.MODID,
                "textures/gui/screamers/" + slot + ".png"
        );
        ResourceLocation sound = ResourceLocation.fromNamespaceAndPath(
                ChaosEvents.MODID,
                "sounds/screamers/" + slot + ".ogg"
        );
        return minecraft.getResourceManager().getResource(texture).isPresent()
                && minecraft.getResourceManager().getResource(sound).isPresent();
    }
}
