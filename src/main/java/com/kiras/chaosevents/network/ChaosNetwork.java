package com.kiras.chaosevents.network;

import com.kiras.chaosevents.client.ClientChaosConfigHandler;
import com.kiras.chaosevents.client.ClientEventAnnouncementHandler;
import com.kiras.chaosevents.client.ClientScreamerHandler;
import com.kiras.chaosevents.config.ChaosConfigCatalog;
import com.kiras.chaosevents.config.ChaosConfigCategory;
import com.kiras.chaosevents.config.ChaosConfigEntry;
import com.kiras.chaosevents.config.ChaosConfigManager;
import com.kiras.chaosevents.core.ChaosSessionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;

public final class ChaosNetwork {
    private static final String PREFIX = "[Chaos Events] ";

    private ChaosNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("3")
                .playToClient(ScreamerPayload.TYPE, ScreamerPayload.STREAM_CODEC, ChaosNetwork::handleScreamer)
                .playToClient(ConfigOpenPayload.TYPE, ConfigOpenPayload.STREAM_CODEC, ChaosNetwork::handleConfigOpen)
                .playToClient(EventAnnouncementPayload.TYPE, EventAnnouncementPayload.STREAM_CODEC,
                        ChaosNetwork::handleEventAnnouncement)
                .playToServer(ConfigSavePayload.TYPE, ConfigSavePayload.STREAM_CODEC, ChaosNetwork::handleConfigSave);
    }

    private static void handleScreamer(ScreamerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientScreamerHandler.show(payload);
            }
        });
    }

    private static void handleConfigOpen(ConfigOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientChaosConfigHandler.open(payload);
            }
        });
    }

    private static void handleEventAnnouncement(EventAnnouncementPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientEventAnnouncementHandler.show(payload);
            }
        });
    }

    private static void handleConfigSave(ConfigSavePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!player.createCommandSourceStack().hasPermission(2)) {
                player.sendSystemMessage(Component.literal(PREFIX + "недостаточно прав для изменения настроек."));
                return;
            }

            ChaosConfigCategory category = ChaosConfigCategory.fromId(payload.category());
            if (category == null) {
                return;
            }

            int minInterval = payload.minIntervalSeconds();
            int maxInterval = payload.maxIntervalSeconds();
            if (minInterval < ChaosConfigCategory.MIN_ALLOWED_INTERVAL_SECONDS
                    || maxInterval > ChaosConfigCategory.MAX_ALLOWED_INTERVAL_SECONDS
                    || minInterval > maxInterval) {
                player.sendSystemMessage(Component.literal(PREFIX + "некорректный диапазон интервала."));
                return;
            }

            Set<String> validIds = new HashSet<>();
            for (ChaosConfigEntry entry : ChaosConfigCatalog.entries(category)) {
                validIds.add(entry.id());
            }
            Set<String> disabled = ChaosConfigManager.decodeDisabled(payload.disabledIds());
            disabled.retainAll(validIds);
            ChaosConfigManager.replaceDisabled(category, disabled);
            ChaosConfigManager.setIntervalSeconds(category, minInterval, maxInterval);
            ChaosConfigManager.save();

            MinecraftServer server = player.getServer();
            boolean restarted = server != null && ChaosSessionManager.restartAfterConfigurationChange(server);
            player.sendSystemMessage(Component.literal(PREFIX + "настройки «" + category.displayName()
                    + "» сохранены. Интервал: " + formatSeconds(minInterval) + "–" + formatSeconds(maxInterval) + ". "
                    + (restarted
                    ? "Активная сессия Chaos Events перезапущена с новыми настройками."
                    : "Они будут применены при следующем /chaos start.")));
        });
    }

    public static void sendScreamer(ServerPlayer player, int variant, int durationTicks) {
        PacketDistributor.sendToPlayer(player, new ScreamerPayload(variant, durationTicks));
    }

    public static void openConfigBook(ServerPlayer player, ChaosConfigCategory category) {
        PacketDistributor.sendToPlayer(player, new ConfigOpenPayload(
                category.id(),
                ChaosConfigManager.encodeDisabled(ChaosConfigManager.getDisabled(category)),
                ChaosConfigManager.getMinIntervalSeconds(category),
                ChaosConfigManager.getMaxIntervalSeconds(category)
        ));
    }

    public static void sendEventAnnouncement(ServerPlayer player, String title, String description, int durationSeconds) {
        PacketDistributor.sendToPlayer(player, new EventAnnouncementPayload(title, description, durationSeconds));
    }

    private static String formatSeconds(int totalSeconds) {
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
