package com.kiras.chaosevents.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

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

    /** Optional event-owned text used by announcements. */
    default String description() {
        return "";
    }

    /** Called when the whole Chaos Events session is paused. */
    default void pause(MinecraftServer server) {
    }

    /** Called when a paused Chaos Events session resumes. */
    default void resume(MinecraftServer server) {
    }

    /** Called when one player becomes isolated from this event by entering Places. */
    default void excludePlayer(MinecraftServer server, ServerPlayer player) {
    }

    /** Called when a previously isolated Places player returns while this event is still active. */
    default void includePlayer(MinecraftServer server, ServerPlayer player) {
    }

    /** Number of event-engine ticks that represent one displayed second. */
    default int timerTicksPerSecond() {
        return 20;
    }

    /** Optional per-event duration override. A negative value keeps engine defaults. */
    default int minimumDurationSeconds() {
        return -1;
    }

    /** Optional per-event duration override. A negative value keeps engine defaults. */
    default int maximumDurationSeconds() {
        return -1;
    }
}
