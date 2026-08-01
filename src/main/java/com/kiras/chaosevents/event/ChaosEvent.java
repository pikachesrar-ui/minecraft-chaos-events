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

    /** Called when the whole Chaos Events session is paused. */
    default void pause(MinecraftServer server) {
    }

    /** Called when a paused Chaos Events session resumes. */
    default void resume(MinecraftServer server) {
    }

    /** Number of event-engine ticks that represent one displayed second. */
    default int timerTicksPerSecond() {
        return 20;
    }
}
