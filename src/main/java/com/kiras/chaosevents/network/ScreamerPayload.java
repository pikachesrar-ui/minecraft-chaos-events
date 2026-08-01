package com.kiras.chaosevents.network;

import com.kiras.chaosevents.ChaosEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ScreamerPayload(int variant, int durationTicks) implements CustomPacketPayload {
    public static final Type<ScreamerPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ChaosEvents.MODID, "screamer")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ScreamerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ScreamerPayload::variant,
            ByteBufCodecs.VAR_INT,
            ScreamerPayload::durationTicks,
            ScreamerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
