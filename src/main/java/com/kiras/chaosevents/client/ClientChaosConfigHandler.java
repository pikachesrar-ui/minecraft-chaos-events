package com.kiras.chaosevents.client;

import com.kiras.chaosevents.config.ChaosConfigCategory;
import com.kiras.chaosevents.config.ChaosConfigManager;
import com.kiras.chaosevents.network.ConfigOpenPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientChaosConfigHandler {
    private ClientChaosConfigHandler() {
    }

    public static void open(ConfigOpenPayload payload) {
        ChaosConfigCategory category = ChaosConfigCategory.fromId(payload.category());
        if (category == null) {
            return;
        }
        Minecraft.getInstance().setScreen(new ChaosConfigScreen(
                category,
                ChaosConfigManager.decodeDisabled(payload.disabledIds())
        ));
    }
}
