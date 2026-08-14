package com.kiras.chaosevents.event;

import com.kiras.chaosevents.integration.PlacesRealitySlipManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Central player filter shared by every large-event implementation. */
public final class BigEventPlayerPolicy {
    private BigEventPlayerPolicy() {
    }

    /** Players inside any Places dimension are isolated until they leave it. */
    public static boolean canAffect(ServerPlayer player) {
        return player != null && !PlacesRealitySlipManager.isInPlacesDimension(player);
    }

    public static boolean hasEligiblePlayer(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream().anyMatch(BigEventPlayerPolicy::canAffect);
    }

    public static List<ServerPlayer> eligiblePlayers(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream()
                .filter(BigEventPlayerPolicy::canAffect)
                .toList();
    }
}
