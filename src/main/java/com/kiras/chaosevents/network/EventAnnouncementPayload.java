package com.kiras.chaosevents.network;

import com.kiras.chaosevents.ChaosEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EventAnnouncementPayload(String title, String description, int durationSeconds)
        implements CustomPacketPayload {
    public static final Type<EventAnnouncementPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ChaosEvents.MODID, "event_announcement")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, EventAnnouncementPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            EventAnnouncementPayload::title,
            ByteBufCodecs.STRING_UTF8,
            EventAnnouncementPayload::description,
            ByteBufCodecs.VAR_INT,
            EventAnnouncementPayload::durationSeconds,
            EventAnnouncementPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
