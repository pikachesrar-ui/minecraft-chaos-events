package com.kiras.chaosevents.core;

/**
 * Stores the current state of the Chaos Events session.
 * Timers and event data will be added here in later versions.
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

    public static synchronized boolean start() {
        if (state != State.STOPPED) {
            return false;
        }
        state = State.RUNNING;
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

    public static synchronized boolean stop() {
        if (state == State.STOPPED) {
            return false;
        }
        state = State.STOPPED;
        return true;
    }

    public static synchronized void reset() {
        state = State.STOPPED;
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
}
