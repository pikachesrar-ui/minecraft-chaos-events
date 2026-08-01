package com.kiras.chaosevents.event;

import net.minecraft.server.MinecraftServer;

/**
 * Contract for one timed global Chaos Events event.
 */
public interface ChaosEvent {

    String id();

    String displayName();

    boolean harsh();

    boolean isEligible(MinecraftServer server);

    void start(MinecraftServer server);

    void tick(MinecraftServer server, int elapsedTicks, int remainingTicks);

    void stop(MinecraftServer server);
}
