package com.kiras.chaosevents.core;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Stores the current state and server-side timers of the Chaos Events session.
 */
public final class ChaosSessionManager {

    private static final int TICKS_PER_SECOND = 20;
    private static final int MIN_BIG_EVENT_DELAY_SECONDS = 5 * 60;
    private static final int MAX_BIG_EVENT_DELAY_SECONDS = 10 * 60;
    private static final String PREFIX = "[Chaos Events] ";

    public enum State {
        STOPPED,
        RUNNING,
        PAUSED
    }

    private static State state = State.STOPPED;
    private static int ticksUntilNextBigEvent;

    private ChaosSessionManager() {
    }

    public static synchronized boolean start() {
        if (state != State.STOPPED) {
            return false;
        }

        state = State.RUNNING;
        scheduleNextBigEvent();
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
        ticksUntilNextBigEvent = 0;
        return true;
    }

    public static synchronized void reset() {
        state = State.STOPPED;
        ticksUntilNextBigEvent = 0;
    }

    /**
     * Called once after every server tick. The countdown only moves while the
     * session is running, so pausing the session freezes the timer exactly.
     */
    public static void tick(MinecraftServer server) {
        boolean shouldTriggerEvent = false;

        synchronized (ChaosSessionManager.class) {
            if (state != State.RUNNING) {
                return;
            }

            if (ticksUntilNextBigEvent > 0) {
                ticksUntilNextBigEvent--;
            }

            if (ticksUntilNextBigEvent <= 0) {
                shouldTriggerEvent = true;
                scheduleNextBigEvent();
            }
        }

        if (shouldTriggerEvent) {
            runTestBigEvent(server);
        }
    }

    private static void scheduleNextBigEvent() {
        int delaySeconds = ThreadLocalRandom.current().nextInt(
                MIN_BIG_EVENT_DELAY_SECONDS,
                MAX_BIG_EVENT_DELAY_SECONDS + 1
        );
        ticksUntilNextBigEvent = delaySeconds * TICKS_PER_SECOND;
    }

    /**
     * Temporary global event used to verify that the timer works for every
     * connected player. It will later be replaced by the real event selector.
     */
    private static void runTestBigEvent(MinecraftServer server) {
        Component message = Component.literal(
                PREFIX + "ГЛОБАЛЬНЫЙ ТЕСТОВЫЙ ИВЕНТ: пространство стало нестабильным!"
        );

        server.getPlayerList().getPlayers()
                .forEach(player -> player.sendSystemMessage(message));
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

    public static synchronized int getSecondsUntilNextBigEvent() {
        if (state == State.STOPPED || ticksUntilNextBigEvent <= 0) {
            return 0;
        }

        return (ticksUntilNextBigEvent + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
    }

    public static synchronized String getFormattedTimeUntilNextBigEvent() {
        if (state == State.STOPPED) {
            return "не запланирован";
        }

        int totalSeconds = getSecondsUntilNextBigEvent();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
