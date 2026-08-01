package com.kiras.chaosevents.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Owns the complete lifecycle of large global events:
 * break -> start -> active ticks -> stop -> next break.
 */
public final class BigEventEngine {

    private static final int TICKS_PER_SECOND = 20;
    private static final int MIN_BREAK_SECONDS = 5 * 60;
    private static final int MAX_BREAK_SECONDS = 10 * 60;
    private static final int MIN_NORMAL_DURATION_SECONDS = 4 * 60;
    private static final int MAX_NORMAL_DURATION_SECONDS = 8 * 60;
    private static final int MIN_HARSH_DURATION_SECONDS = 3 * 60;
    private static final int MAX_HARSH_DURATION_SECONDS = 5 * 60;
    private static final String PREFIX = "[Chaos Events] ";

    public enum Phase {
        STOPPED,
        WAITING,
        ACTIVE
    }

    private static final List<ChaosEvent> EVENTS = List.copyOf(Arrays.asList(BuiltinChaosEvent.values()));

    private static Phase phase = Phase.STOPPED;
    private static ChaosEvent activeEvent;
    private static int ticksRemaining;
    private static int elapsedTicks;
    private static String lastEventId;

    private BigEventEngine() {
    }

    public static synchronized void startSession() {
        activeEvent = null;
        elapsedTicks = 0;
        phase = Phase.WAITING;
        scheduleBreak();
    }

    public static void tick(MinecraftServer server) {
        ChaosEvent eventToTick = null;
        int elapsed = 0;
        int remaining = 0;
        boolean startNewEvent = false;
        boolean finishCurrentEvent = false;

        synchronized (BigEventEngine.class) {
            if (phase == Phase.STOPPED) {
                return;
            }

            if (ticksRemaining > 0) {
                ticksRemaining--;
            }

            if (phase == Phase.WAITING) {
                if (ticksRemaining <= 0) {
                    startNewEvent = true;
                }
            } else if (phase == Phase.ACTIVE && activeEvent != null) {
                elapsedTicks++;
                eventToTick = activeEvent;
                elapsed = elapsedTicks;
                remaining = ticksRemaining;
                if (ticksRemaining <= 0) {
                    finishCurrentEvent = true;
                }
            }
        }

        if (eventToTick != null && !finishCurrentEvent) {
            eventToTick.tick(server, elapsed, remaining);
        }

        if (finishCurrentEvent) {
            finishActiveEvent(server, true);
        } else if (startNewEvent) {
            startRandomEvent(server);
        }
    }

    public static synchronized void stopSession(MinecraftServer server) {
        if (activeEvent != null) {
            activeEvent.stop(server);
        }
        activeEvent = null;
        elapsedTicks = 0;
        ticksRemaining = 0;
        phase = Phase.STOPPED;
    }

    public static synchronized void reset() {
        activeEvent = null;
        elapsedTicks = 0;
        ticksRemaining = 0;
        lastEventId = null;
        phase = Phase.STOPPED;
    }

    public static boolean forceRandomEvent(MinecraftServer server) {
        synchronized (BigEventEngine.class) {
            if (phase == Phase.STOPPED) {
                return false;
            }
        }

        finishActiveEvent(server, false);
        startRandomEvent(server);
        return true;
    }

    private static void startRandomEvent(MinecraftServer server) {
        ChaosEvent selected;

        synchronized (BigEventEngine.class) {
            List<ChaosEvent> candidates = new ArrayList<>();
            for (ChaosEvent event : EVENTS) {
                if (event.isEligible(server) && !event.id().equals(lastEventId)) {
                    candidates.add(event);
                }
            }

            if (candidates.isEmpty()) {
                for (ChaosEvent event : EVENTS) {
                    if (event.isEligible(server)) {
                        candidates.add(event);
                    }
                }
            }

            if (candidates.isEmpty()) {
                phase = Phase.WAITING;
                ticksRemaining = 20 * TICKS_PER_SECOND;
                return;
            }

            selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            activeEvent = selected;
            lastEventId = selected.id();
            elapsedTicks = 0;
            phase = Phase.ACTIVE;
            ticksRemaining = chooseDurationTicks(selected.harsh());
        }

        selected.start(server);
        broadcast(server, "Начался большой ивент: " + selected.displayName() + "!");
    }

    private static void finishActiveEvent(MinecraftServer server, boolean announce) {
        ChaosEvent finished;

        synchronized (BigEventEngine.class) {
            finished = activeEvent;
            activeEvent = null;
            elapsedTicks = 0;

            if (phase != Phase.STOPPED) {
                phase = Phase.WAITING;
                scheduleBreak();
            }
        }

        if (finished != null) {
            finished.stop(server);
            if (announce) {
                broadcast(server, "Большой ивент завершён. Хаос ненадолго отступил.");
            }
        }
    }

    private static int chooseDurationTicks(boolean harsh) {
        int min = harsh ? MIN_HARSH_DURATION_SECONDS : MIN_NORMAL_DURATION_SECONDS;
        int max = harsh ? MAX_HARSH_DURATION_SECONDS : MAX_NORMAL_DURATION_SECONDS;
        return ThreadLocalRandom.current().nextInt(min, max + 1) * TICKS_PER_SECOND;
    }

    private static void scheduleBreak() {
        ticksRemaining = ThreadLocalRandom.current().nextInt(
                MIN_BREAK_SECONDS,
                MAX_BREAK_SECONDS + 1
        ) * TICKS_PER_SECOND;
    }

    private static void broadcast(MinecraftServer server, String text) {
        Component message = Component.literal(PREFIX + text);
        server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(message));
    }

    public static synchronized Phase getPhase() {
        return phase;
    }

    public static synchronized int getSecondsRemaining() {
        if (phase == Phase.STOPPED || ticksRemaining <= 0) {
            return 0;
        }
        return (ticksRemaining + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
    }

    public static synchronized String getStatusText() {
        return switch (phase) {
            case STOPPED -> "большие ивенты остановлены";
            case WAITING -> "перерыв до большого ивента: " + formatSeconds(getSecondsRemaining());
            case ACTIVE -> "большой ивент активен, осталось: " + formatSeconds(getSecondsRemaining());
        };
    }

    public static int getRegisteredEventCount() {
        return EVENTS.size();
    }

    private static String formatSeconds(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
