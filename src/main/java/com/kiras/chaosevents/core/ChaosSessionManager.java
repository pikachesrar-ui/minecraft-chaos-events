package com.kiras.chaosevents.core;

import com.kiras.chaosevents.event.BigEventEngine;
import com.kiras.chaosevents.prank.MicroPrankEngine;
import net.minecraft.server.MinecraftServer;

/**
 * Central lifecycle controller for one server Chaos Events session.
 */
public final class ChaosSessionManager {

    public enum State {
        STOPPED,
        RUNNING,
        PAUSED
    }

    private static State state = State.STOPPED;

    private ChaosSessionManager() {
    }

    public static synchronized boolean start(MinecraftServer server) {
        if (state != State.STOPPED) {
            return false;
        }

        state = State.RUNNING;
        BigEventEngine.startSession();
        MicroPrankEngine.startSession();
        return true;
    }

    public static synchronized boolean pause() {
        if (state != State.RUNNING) {
            return false;
        }

        state = State.PAUSED;
        return true;
    }

    public static synchronized boolean resume() {
        if (state != State.PAUSED) {
            return false;
        }

        state = State.RUNNING;
        return true;
    }

    public static synchronized boolean stop(MinecraftServer server) {
        if (state == State.STOPPED) {
            return false;
        }

        BigEventEngine.stopSession(server);
        MicroPrankEngine.stopSession(server);
        state = State.STOPPED;
        return true;
    }

    public static synchronized void reset() {
        state = State.STOPPED;
        BigEventEngine.reset();
        MicroPrankEngine.reset();
    }

    public static synchronized void shutdown(MinecraftServer server) {
        if (state != State.STOPPED) {
            BigEventEngine.stopSession(server);
            MicroPrankEngine.stopSession(server);
        }
        state = State.STOPPED;
    }

    /**
     * Called after every server tick. A paused session deliberately executes
     * neither engine, so all countdowns and temporary prank restorations freeze.
     */
    public static void tick(MinecraftServer server) {
        synchronized (ChaosSessionManager.class) {
            if (state != State.RUNNING) {
                return;
            }
        }

        BigEventEngine.tick(server);
        MicroPrankEngine.tick(server);
    }

    public static synchronized boolean forceBigEvent(MinecraftServer server) {
        return state == State.RUNNING && BigEventEngine.forceRandomEvent(server);
    }

    public static synchronized boolean forceMicroPrank(MinecraftServer server) {
        return state == State.RUNNING && MicroPrankEngine.forceRandomPrank(server);
    }

    public static synchronized State getState() {
        return state;
    }

    public static synchronized String getStateName() {
        return switch (state) {
            case STOPPED -> "ОСТАНОВЛЕНА";
            case RUNNING -> "ЗАПУЩЕНА";
            case PAUSED -> "ПРИОСТАНОВЛЕНА";
        };
    }

    public static String getBigEventStatus() {
        return BigEventEngine.getStatusText();
    }

    public static String getMicroPrankStatus() {
        return MicroPrankEngine.getStatusText();
    }
}
