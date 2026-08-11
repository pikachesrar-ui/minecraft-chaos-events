package com.kiras.chaosevents.network;

import com.kiras.chaosevents.ChaosEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ConfigOpenPayload(String category, String disabledIds) implements CustomPacketPayload {
    public static final Type<ConfigOpenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ChaosEvents.MODID, "config_open")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigOpenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ConfigOpenPayload::category,
            ByteBufCodecs.STRING_UTF8,
            ConfigOpenPayload::disabledIds,
            ConfigOpenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
