package com.kiras.chaosevents.client;

import com.kiras.chaosevents.network.ScreamerPayload;
import com.kiras.chaosevents.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientScreamerHandler {
    private ClientScreamerHandler() {
    }

    public static void show(ScreamerPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        minecraft.player.playSound(
                ModSounds.SCREAMER.get(),
                2.0F,
                payload.variant() == 0 ? 0.72F : 0.92F
        );
        minecraft.setScreen(new ScreamerScreen(
                minecraft.screen,
                Math.floorMod(payload.variant(), 2),
                Math.max(8, payload.durationTicks())
        ));
    }
}
