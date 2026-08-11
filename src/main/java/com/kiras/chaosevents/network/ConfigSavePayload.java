package com.kiras.chaosevents.network;

import com.kiras.chaosevents.ChaosEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ConfigSavePayload(String category, String disabledIds) implements CustomPacketPayload {
    public static final Type<ConfigSavePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ChaosEvents.MODID, "config_save")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSavePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ConfigSavePayload::category,
            ByteBufCodecs.STRING_UTF8,
            ConfigSavePayload::disabledIds,
            ConfigSavePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
