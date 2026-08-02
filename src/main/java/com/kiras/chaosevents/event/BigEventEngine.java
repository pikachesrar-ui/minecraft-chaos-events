package com.kiras.chaosevents.event;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Owns the complete lifecycle: break -> event -> cleanup -> next break. */
public final class BigEventEngine {
    private static final int TICKS_PER_SECOND = 20;
    private static final int MIN_BREAK_SECONDS = 5 * 60;
    private static final int MAX_BREAK_SECONDS = 10 * 60;
    private static final int MIN_NORMAL_DURATION_SECONDS = 4 * 60;
    private static final int MAX_NORMAL_DURATION_SECONDS = 8 * 60;
    private static final int MIN_HARSH_DURATION_SECONDS = 3 * 60;
    private static final int MAX_HARSH_DURATION_SECONDS = 5 * 60;
    private static final int MIN_MOB_WAVE_DURATION_SECONDS = 20;
    private static final int MAX_MOB_WAVE_DURATION_SECONDS = 40;
    private static final String PREFIX = "[Chaos Events] ";

    private static final Set<String> MOB_WAVE_EVENT_IDS = Set.of(
            "hunters_mark",
            "blood_moon",
            "zombie_siege",
            "skeleton_volley",
            "spider_bloom",
            "creeper_migration",
            "blaze_swarm",
            "magma_march",
            "piglin_hunt",
            "enderman_convergence",
            "chicken_rain",
            "rainbow_sheep",
            "slime_overload",
            "vex_assault",
            "bee_swarm",
            "silverfish_infestation",
            "phantom_sky",
            "random_creepers"
    );

    private static final ServerBossEvent EVENT_TIMER = new ServerBossEvent(
            Component.literal("Chaos Events"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS
    );

    public enum Phase { STOPPED, WAITING, ACTIVE }

    private static final List<ChaosEvent> EVENTS;
    static {
        List<ChaosEvent> events = new ArrayList<>();
        for (BuiltinChaosEvent event : BuiltinChaosEvent.values()) {
            if (event != BuiltinChaosEvent.KINETIC_STORM
                    && event != BuiltinChaosEvent.METEOR_BARRAGE
                    && event != BuiltinChaosEvent.TOXIC_AIR) {
                events.add(event);
            }
        }
        events.add(SurfaceToxicAirEvent.INSTANCE);
        events.add(ExternalDisasterEvent.TORNADO);
        events.add(ExternalDisasterEvent.METEOR_SHOWER);
        events.add(AcceleratedTimeEvent.INSTANCE);
        events.add(SpatialSwapEvent.INSTANCE);
        events.addAll(List.of(ExpandedChaosEvent.values()));
        EVENTS = List.copyOf(events);
    }

    private static final Set<String> USED_EVENT_IDS = new HashSet<>();

    private static Phase phase = Phase.STOPPED;
    private static ChaosEvent activeEvent;
    private static int ticksRemaining;
    private static int activeDurationTicks;
    private static int elapsedTicks;
    private static String lastEventId;

    private BigEventEngine() {}

    public static synchronized void startSession() {
        activeEvent = null;
        elapsedTicks = 0;
        activeDurationTicks = 0;
        lastEventId = null;
        USED_EVENT_IDS.clear();
        phase = Phase.WAITING;
        hideBossBar();
        scheduleBreak();
    }

    public static void tick(MinecraftServer server) {
        ChaosEvent eventToTick = null;
        int elapsed = 0;
        int remaining = 0;
        boolean startNewEvent = false;
        boolean finishCurrentEvent = false;

        synchronized (BigEventEngine.class) {
            if (phase == Phase.STOPPED) return;
            if (ticksRemaining > 0) ticksRemaining--;
            if (phase == Phase.WAITING) {
                startNewEvent = ticksRemaining <= 0;
            } else if (phase == Phase.ACTIVE && activeEvent != null) {
                elapsedTicks++;
                eventToTick = activeEvent;
                elapsed = elapsedTicks;
                remaining = ticksRemaining;
                finishCurrentEvent = ticksRemaining <= 0;
            }
        }

        if (eventToTick != null && !finishCurrentEvent) {
            eventToTick.tick(server, elapsed, remaining);
            updateBossBar(server);
        }
        if (finishCurrentEvent) finishActiveEvent(server, true);
        else if (startNewEvent) startRandomEvent(server);
    }

    public static synchronized void stopSession(MinecraftServer server) {
        if (activeEvent != null) activeEvent.stop(server);
        activeEvent = null;
        elapsedTicks = 0;
        activeDurationTicks = 0;
        ticksRemaining = 0;
        USED_EVENT_IDS.clear();
        phase = Phase.STOPPED;
        hideBossBar();
    }

    public static synchronized void pauseActiveEvent(MinecraftServer server) {
        if (activeEvent != null) activeEvent.pause(server);
    }

    public static synchronized void resumeActiveEvent(MinecraftServer server) {
        if (activeEvent != null) activeEvent.resume(server);
    }

    public static synchronized void reset() {
        activeEvent = null;
        elapsedTicks = 0;
        activeDurationTicks = 0;
        ticksRemaining = 0;
        lastEventId = null;
        USED_EVENT_IDS.clear();
        phase = Phase.STOPPED;
        hideBossBar();
    }

    public static boolean forceRandomEvent(MinecraftServer server) {
        synchronized (BigEventEngine.class) {
            if (phase == Phase.STOPPED) return false;
        }
        finishActiveEvent(server, false);
        startRandomEvent(server);
        return true;
    }

    public static boolean skipActiveEvent(MinecraftServer server) {
        synchronized (BigEventEngine.class) {
            if (phase != Phase.ACTIVE || activeEvent == null) return false;
        }
        finishActiveEvent(server, false);
        return true;
    }

    public static boolean forceSpatialEvent(MinecraftServer server) {
        synchronized (BigEventEngine.class) {
            if (phase == Phase.STOPPED || !SpatialSwapEvent.INSTANCE.isEligible(server)) return false;
        }
        finishActiveEvent(server, false);
        startSelectedEvent(server, SpatialSwapEvent.INSTANCE);
        return true;
    }

    public static boolean forceAcceleratedTimeEvent(MinecraftServer server) {
        synchronized (BigEventEngine.class) {
            if (phase == Phase.STOPPED || !AcceleratedTimeEvent.INSTANCE.isEligible(server)) return false;
        }
        finishActiveEvent(server, false);
        startSelectedEvent(server, AcceleratedTimeEvent.INSTANCE);
        return true;
    }

    private static void startRandomEvent(MinecraftServer server) {
        ChaosEvent selected;
        synchronized (BigEventEngine.class) {
            List<ChaosEvent> eligible = new ArrayList<>();
            for (ChaosEvent event : EVENTS) {
                if (event.isEligible(server)) eligible.add(event);
            }

            if (eligible.isEmpty()) {
                phase = Phase.WAITING;
                ticksRemaining = 20 * TICKS_PER_SECOND;
                return;
            }

            List<ChaosEvent> candidates = eligible.stream()
                    .filter(event -> !USED_EVENT_IDS.contains(event.id()))
                    .toList();

            if (candidates.isEmpty()) {
                USED_EVENT_IDS.clear();
                candidates = eligible.stream()
                        .filter(event -> eligible.size() == 1 || !event.id().equals(lastEventId))
                        .toList();
                if (candidates.isEmpty()) candidates = eligible;
            }

            selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        }
        startSelectedEvent(server, selected);
    }

    private static void startSelectedEvent(MinecraftServer server, ChaosEvent selected) {
        int selectedDurationTicks;
        synchronized (BigEventEngine.class) {
            activeEvent = selected;
            lastEventId = selected.id();
            USED_EVENT_IDS.add(selected.id());
            elapsedTicks = 0;
            phase = Phase.ACTIVE;
            ticksRemaining = chooseDurationTicks(selected);
            activeDurationTicks = ticksRemaining;
            selectedDurationTicks = ticksRemaining;
        }
        selected.start(server);
        announceEventStart(server, selected, selectedDurationTicks);
        updateBossBar(server);
    }

    private static void announceEventStart(MinecraftServer server, ChaosEvent event, int durationTicks) {
        int ticksPerSecond = Math.max(1, event.timerTicksPerSecond());
        int durationSeconds = Math.max(1, durationTicks / ticksPerSecond);
        String description = event.description().isBlank() ? descriptionFor(event.id()) : event.description();

        Component title = Component.literal(event.displayName())
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        Component subtitle = Component.literal(
                        "Длительность: " + formatSeconds(durationSeconds) + " • " + description
                )
                .withStyle(ChatFormatting.GOLD);
        Component chatMessage = Component.literal(
                PREFIX + "Начался большой ивент: " + event.displayName()
                        + " — " + description
                        + " (" + formatSeconds(durationSeconds) + ")"
        );

        server.getPlayerList().getPlayers().forEach(player -> {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 20));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.MASTER, 0.85F, 1.0F);
            player.sendSystemMessage(chatMessage);
        });
    }

    private static String descriptionFor(String eventId) {
        return switch (eventId) {
            case "gravity_failure" -> "Гравитация постоянно подбрасывает игроков";
            case "crushing_gravity" -> "Движение, сила и добыча сильно ослаблены";
            case "berserker_rush" -> "Скорость и урон растут вместе с голодом";
            case "time_quicksand" -> "Время замедляет движение и работу";
            case "time_acceleration" -> "Мир ускорен в 10 раз, но игроки и их транспорт остаются в обычном времени";
            case "total_darkness" -> "Тьма и слепота скрывают всё вокруг";
            case "hunters_mark" -> "Игроки отмечены и становятся целью мобов";
            case "life_drain" -> "Иссушение постепенно отнимает здоровье";
            case "toxic_air" -> "Ядовитый воздух действует только под открытым небом";
            case "famine" -> "Сытость быстро исчезает";
            case "skyhook" -> "Небо регулярно утягивает игроков вверх";
            case "chaos_roulette" -> "Проклятия постоянно меняются";
            case "kinetic_storm" -> "Настоящий торнадо Weather2 движется к игрокам";
            case "lightning_hunt" -> "Молнии преследуют игроков";
            case "meteor_barrage" -> "Настоящий метеоритный дождь обрушивается с неба";
            case "blood_moon" -> "Ночь вызывает смешанные волны нежити";
            case "zombie_siege" -> "Зомби непрерывно окружают игроков";
            case "skeleton_volley" -> "Скелеты устраивают дальний расстрел";
            case "spider_bloom" -> "Вокруг игроков появляются стаи пауков";
            case "creeper_migration" -> "Криперы массово сходятся к игрокам";
            case "lava_geysers" -> "Лава подбрасывает и поджигает игроков";
            case "infernal_hunger" -> "Ад высасывает сытость и силу";
            case "blaze_swarm" -> "Ифриты атакуют регулярными волнами";
            case "magma_march" -> "Магмовые кубы заполняют окрестности";
            case "withered_air" -> "Воздух Незера вызывает иссушение";
            case "soul_crush" -> "Души давят тьмой и слабостью";
            case "firestorm" -> "Огненные вспышки постоянно поджигают";
            case "piglin_hunt" -> "Жестокие пиглины начинают охоту";
            case "void_lightness" -> "Бездна нарушает прыжки и падение";
            case "ender_static" -> "Помехи телепортируют и дезориентируют";
            case "enderman_convergence" -> "Эндермены собираются вокруг игроков";
            case "shulker_echo" -> "Левитация повторяется волнами";
            case "dragon_breath" -> "Драконье дыхание отравляет пространство";
            case "chorus_shift" -> "Телепортации перемешивают игроков и хотбар";
            case "void_silence" -> "Слепота и слабость накрывают Край";
            case "end_crystal_storm" -> "Рядом появляются опасные кристаллы Края";
            case "spatial_swap" -> "Игроки меняются местами между измерениями";
            default -> "Переживите воздействие хаоса";
        };
    }

    private static void finishActiveEvent(MinecraftServer server, boolean announce) {
        ChaosEvent finished;
        synchronized (BigEventEngine.class) {
            finished = activeEvent;
            activeEvent = null;
            elapsedTicks = 0;
            activeDurationTicks = 0;
            if (phase != Phase.STOPPED) {
                phase = Phase.WAITING;
                scheduleBreak();
            }
        }
        hideBossBar();
        if (finished != null) {
            finished.stop(server);
            if (announce) broadcast(server, "Большой ивент завершён. Хаос ненадолго отступил.");
        }
    }

    private static int chooseDurationTicks(ChaosEvent event) {
        int min;
        int max;
        if (MOB_WAVE_EVENT_IDS.contains(event.id())) {
            min = MIN_MOB_WAVE_DURATION_SECONDS;
            max = MAX_MOB_WAVE_DURATION_SECONDS;
        } else {
            min = event.harsh() ? MIN_HARSH_DURATION_SECONDS : MIN_NORMAL_DURATION_SECONDS;
            max = event.harsh() ? MAX_HARSH_DURATION_SECONDS : MAX_NORMAL_DURATION_SECONDS;
        }
        int durationSeconds = ThreadLocalRandom.current().nextInt(min, max + 1);
        return durationSeconds * Math.max(1, event.timerTicksPerSecond());
    }

    private static void scheduleBreak() {
        ticksRemaining = ThreadLocalRandom.current().nextInt(MIN_BREAK_SECONDS, MAX_BREAK_SECONDS + 1)
                * TICKS_PER_SECOND;
    }

    private static void updateBossBar(MinecraftServer server) {
        ChaosEvent event;
        int remaining;
        int total;
        int seconds;
        synchronized (BigEventEngine.class) {
            if (phase != Phase.ACTIVE || activeEvent == null || activeDurationTicks <= 0) {
                hideBossBar();
                return;
            }
            event = activeEvent;
            remaining = Math.max(0, ticksRemaining);
            total = Math.max(1, activeDurationTicks);
            int ticksPerSecond = Math.max(1, event.timerTicksPerSecond());
            seconds = (remaining + ticksPerSecond - 1) / ticksPerSecond;
        }

        EVENT_TIMER.setName(Component.literal(event.displayName() + " • осталось " + formatSeconds(seconds)));
        EVENT_TIMER.setProgress(Math.max(0.0F, Math.min(1.0F, (float) remaining / total)));
        EVENT_TIMER.setVisible(true);
        server.getPlayerList().getPlayers().forEach(EVENT_TIMER::addPlayer);
    }

    private static void hideBossBar() {
        EVENT_TIMER.setVisible(false);
        EVENT_TIMER.removeAllPlayers();
    }

    private static void broadcast(MinecraftServer server, String text) {
        Component message = Component.literal(PREFIX + text);
        server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(message));
    }

    public static synchronized Phase getPhase() { return phase; }

    public static synchronized int getSecondsRemaining() {
        if (phase == Phase.STOPPED || ticksRemaining <= 0) return 0;
        int ticksPerSecond = phase == Phase.ACTIVE && activeEvent != null
                ? Math.max(1, activeEvent.timerTicksPerSecond())
                : TICKS_PER_SECOND;
        return (ticksRemaining + ticksPerSecond - 1) / ticksPerSecond;
    }

    public static synchronized String getStatusText() {
        return switch (phase) {
            case STOPPED -> "большие ивенты остановлены";
            case WAITING -> "перерыв до большого ивента: " + formatSeconds(getSecondsRemaining());
            case ACTIVE -> "активен «" + activeEvent.displayName() + "», осталось: "
                    + formatSeconds(getSecondsRemaining());
        };
    }

    public static int getRegisteredEventCount() { return EVENTS.size(); }

    private static String formatSeconds(int totalSeconds) {
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
