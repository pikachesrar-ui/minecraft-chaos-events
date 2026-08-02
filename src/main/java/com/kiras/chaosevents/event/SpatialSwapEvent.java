package com.kiras.chaosevents.event;

import com.kiras.chaosevents.spatial.SpatialSwapManager;
import net.minecraft.server.MinecraftServer;

public enum SpatialSwapEvent implements ChaosEvent {
    INSTANCE;

    @Override
    public String id() {
        return "spatial_swap";
    }

    @Override
    public String displayName() {
        return "Пространственный сдвиг";
    }

    @Override
    public boolean harsh() {
        return true;
    }

    @Override
    public boolean isEligible(MinecraftServer server) {
        return SpatialSwapManager.canStart(server);
    }

    @Override
    public void start(MinecraftServer server) {
        SpatialSwapManager.startEvent(server);
    }

    @Override
    public void tick(MinecraftServer server, int elapsedTicks, int remainingTicks) {
        SpatialSwapManager.tickEvent(server);
    }

    @Override
    public void stop(MinecraftServer server) {
        SpatialSwapManager.stopEvent(server);
    }
}
