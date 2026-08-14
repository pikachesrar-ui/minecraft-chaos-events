package com.kiras.chaosevents.core;

import com.kiras.chaosevents.event.AcceleratedTimeEvent;
import com.kiras.chaosevents.event.BigEventEngine;
import com.kiras.chaosevents.integration.PlacesRealitySlipManager;
import com.kiras.chaosevents.prank.MicroPrankEngine;
import com.kiras.chaosevents.spatial.SpatialSwapManager;
import com.kiras.chaosevents.trivia.TriviaEngine;
import net.minecraft.server.MinecraftServer;

/** Central lifecycle controller for one server Chaos Events session. */
public final class ChaosSessionManager {
    public enum State { STOPPED, RUNNING, PAUSED }

    private static State state = State.STOPPED;

    private ChaosSessionManager() {}

    public static synchronized boolean start(MinecraftServer server) {
        if (state != State.STOPPED) return false;
        state = State.RUNNING;
        startEngines();
        return true;
    }

    public static synchronized boolean pause(MinecraftServer server) {
        if (state != State.RUNNING) return false;
        BigEventEngine.pauseActiveEvent(server);
        state = State.PAUSED;
        return true;
    }

    public static synchronized boolean resume(MinecraftServer server) {
        if (state != State.PAUSED) return false;
        BigEventEngine.resumeActiveEvent(server);
        state = State.RUNNING;
        return true;
    }

    public static synchronized boolean stop(MinecraftServer server) {
        if (state == State.STOPPED) return false;
        stopEngines(server);
        state = State.STOPPED;
        return true;
    }

    /**
     * Applies a saved configuration without requiring a Minecraft/server restart.
     * Running sessions are cleanly stopped and started again so event pools are rebuilt
     * from the new enabled/disabled selections. A paused session remains paused.
     */
    public static synchronized boolean restartAfterConfigurationChange(MinecraftServer server) {
        State previousState = state;
        if (previousState == State.STOPPED) {
            BigEventEngine.reset();
            MicroPrankEngine.reset();
            TriviaEngine.reset();
            SpatialSwapManager.reset();
            PlacesRealitySlipManager.reset();
            return false;
        }

        stopEngines(server);
        state = State.RUNNING;
        startEngines();
        if (previousState == State.PAUSED) {
            BigEventEngine.pauseActiveEvent(server);
            state = State.PAUSED;
        }
        return true;
    }

    public static synchronized void reset() {
        state = State.STOPPED;
        BigEventEngine.reset();
        MicroPrankEngine.reset();
        TriviaEngine.reset();
        SpatialSwapManager.reset();
        PlacesRealitySlipManager.reset();
    }

    public static synchronized void shutdown(MinecraftServer server) {
        if (state != State.STOPPED) {
            stopEngines(server);
        }
        state = State.STOPPED;
    }

    /**
     * The Places return safety loop is deliberately independent from the public session state.
     * A player must still return after 5-10 real minutes even if the session is paused or world
     * acceleration changes how often auxiliary systems run.
     */
    public static void tick(MinecraftServer server) {
        PlacesRealitySlipManager.tickPendingReturns(server);

        synchronized (ChaosSessionManager.class) {
            if (state != State.RUNNING) return;
        }

        boolean playerInPlaces = PlacesRealitySlipManager.hasAnyPlayerInPlaces(server);
        if (playerInPlaces) {
            // A large event may have started before the player crossed into Places. Stop it cleanly
            // and freeze the large-event break while any player remains in a Places dimension.
            BigEventEngine.skipActiveEvent(server);
        } else {
            BigEventEngine.tick(server);
        }

        if (AcceleratedTimeEvent.INSTANCE.shouldTickAuxiliarySystems()) {
            PlacesRealitySlipManager.tick(server);

            // A hidden Places trigger may have fired just above. End a previously running large
            // event immediately, before another server tick can apply it in the foreign dimension.
            playerInPlaces = PlacesRealitySlipManager.hasAnyPlayerInPlaces(server);
            if (playerInPlaces) {
                BigEventEngine.skipActiveEvent(server);
            } else {
                SpatialSwapManager.tickSession(server);
            }

            MicroPrankEngine.tick(server);
            TriviaEngine.tick(server);
        }
    }

    public static synchronized boolean forceBigEvent(MinecraftServer server) {
        return state == State.RUNNING
                && !PlacesRealitySlipManager.hasAnyPlayerInPlaces(server)
                && BigEventEngine.forceRandomEvent(server);
    }

    public static synchronized boolean skipBigEvent(MinecraftServer server) {
        return state != State.STOPPED && BigEventEngine.skipActiveEvent(server);
    }

    public static synchronized boolean forceSpatialEvent(MinecraftServer server) {
        return state == State.RUNNING
                && !PlacesRealitySlipManager.hasAnyPlayerInPlaces(server)
                && BigEventEngine.forceSpatialEvent(server);
    }

    public static synchronized boolean forceAcceleratedTimeEvent(MinecraftServer server) {
        return state == State.RUNNING
                && !PlacesRealitySlipManager.hasAnyPlayerInPlaces(server)
                && BigEventEngine.forceAcceleratedTimeEvent(server);
    }

    public static synchronized boolean forceMicroPrank(MinecraftServer server) {
        return state == State.RUNNING && MicroPrankEngine.forceRandomPrank(server);
    }

    public static synchronized boolean forceTrivia(MinecraftServer server) {
        return state == State.RUNNING && TriviaEngine.forceQuestion(server);
    }

    public static synchronized State getState() { return state; }

    public static synchronized boolean isRunning() { return state == State.RUNNING; }

    public static synchronized String getStateName() {
        return switch (state) {
            case STOPPED -> "ОСТАНОВЛЕНА";
            case RUNNING -> "ЗАПУЩЕНА";
            case PAUSED -> "ПРИОСТАНОВЛЕНА";
        };
    }

    public static String getBigEventStatus() { return BigEventEngine.getStatusText(); }
    public static String getMicroPrankStatus() { return MicroPrankEngine.getStatusText(); }
    public static String getTriviaStatus() { return TriviaEngine.getStatusText(); }
    public static String getSpatialStatus() { return SpatialSwapManager.getStatusText(); }

    private static void startEngines() {
        BigEventEngine.startSession();
        MicroPrankEngine.startSession();
        TriviaEngine.startSession();
        SpatialSwapManager.startSession();
        PlacesRealitySlipManager.startSession();
    }

    private static void stopEngines(MinecraftServer server) {
        BigEventEngine.stopSession(server);
        MicroPrankEngine.stopSession(server);
        TriviaEngine.stopSession();
        SpatialSwapManager.stopSession(server);
        PlacesRealitySlipManager.stopSession(server);
    }
}
