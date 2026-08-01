package com.kiras.chaosevents.network;

import com.kiras.chaosevents.client.ClientScreamerHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ChaosNetwork {
    private ChaosNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(ScreamerPayload.TYPE, ScreamerPayload.STREAM_CODEC, ChaosNetwork::handleScreamer);
    }

    private static void handleScreamer(ScreamerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientScreamerHandler.show(payload);
            }
        });
    }

    public static void sendScreamer(ServerPlayer player, int variant, int durationTicks) {
        PacketDistributor.sendToPlayer(player, new ScreamerPayload(variant, durationTicks));
    }
}
