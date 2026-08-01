package com.kiras.chaosevents.client;

import com.kiras.chaosevents.ChaosEvents;
import com.kiras.chaosevents.network.ScreamerPayload;
import com.kiras.chaosevents.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@OnlyIn(Dist.CLIENT)
public final class ClientScreamerHandler {
    private static int lastSlot = -1;

    private ClientScreamerHandler() {
    }

    public static void show(ScreamerPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        int slot = chooseAvailablePair(minecraft);
        if (slot > 0) {
            minecraft.player.playSound(ModSounds.screamer(slot), 2.0F, 1.0F);
        } else {
            minecraft.player.playSound(SoundEvents.WARDEN_ROAR, 2.0F, 0.8F);
        }

        minecraft.setScreen(new ScreamerScreen(
                minecraft.screen,
                slot,
                Math.max(8, payload.durationTicks())
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
