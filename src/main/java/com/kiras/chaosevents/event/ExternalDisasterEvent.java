package com.kiras.chaosevents.event;

import com.kiras.chaosevents.integration.ExternalDisasterIntegration;
import net.minecraft.server.MinecraftServer;

/** Real disasters supplied by optional third-party mods. */
public enum ExternalDisasterEvent implements ChaosEvent {
    TORNADO("kinetic_storm", "Торнадо"),
    METEOR_SHOWER("meteor_barrage", "Метеоритный обстрел");

    private final String id;
    private final String displayName;

    ExternalDisasterEvent(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public boolean harsh() {
        return true;
    }

    @Override
    public boolean isEligible(MinecraftServer server) {
        return switch (this) {
            case TORNADO -> ExternalDisasterIntegration.canStartTornado(server);
            case METEOR_SHOWER -> ExternalDisasterIntegration.canStartMeteorShower(server);
        };
    }

    @Override
    public void start(MinecraftServer server) {
        switch (this) {
            case TORNADO -> ExternalDisasterIntegration.startTornado(server);
            case METEOR_SHOWER -> ExternalDisasterIntegration.startMeteorShower(server);
        }
    }

    @Override
    public void tick(MinecraftServer server, int elapsedTicks, int remainingTicks) {
        // The installed disaster mod owns the active simulation after creation.
    }

    @Override
    public void stop(MinecraftServer server) {
        if (this == TORNADO) {
            ExternalDisasterIntegration.stopTornado();
        }
    }
}
