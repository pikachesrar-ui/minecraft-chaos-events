package com.kiras.chaosevents.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public enum EventScope {
    ANY,
    OVERWORLD,
    NETHER,
    END;

    public boolean matches(ServerPlayer player) {
        return BigEventPlayerPolicy.canAffect(player) && matches(player.serverLevel());
    }

    public boolean matches(ServerLevel level) {
        return switch (this) {
            case ANY -> true;
            case OVERWORLD -> level.dimension() == Level.OVERWORLD;
            case NETHER -> level.dimension() == Level.NETHER;
            case END -> level.dimension() == Level.END;
        };
    }

    public boolean hasEligiblePlayer(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream().anyMatch(this::matches);
    }
}
