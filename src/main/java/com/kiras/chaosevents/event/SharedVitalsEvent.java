package com.kiras.chaosevents.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Links the health and hunger pools of all players affected by the active large event. */
public final class SharedVitalsEvent implements ChaosEvent {
    public static final SharedVitalsEvent INSTANCE = new SharedVitalsEvent();

    private static final float EPSILON = 0.001F;
    private static final float LETHAL_DAMAGE = 1_000_000.0F;

    private final Map<UUID, Vitals> lastSynchronized = new HashMap<>();
    private boolean active;
    private float sharedHealth;
    private int sharedFood;
    private float sharedSaturation;
    private float sharedExhaustion;

    private SharedVitalsEvent() {
    }

    @Override
    public String id() {
        return "shared_vitals";
    }

    @Override
    public String displayName() {
        return "Одна жизнь на всех";
    }

    @Override
    public boolean harsh() {
        return true;
    }

    @Override
    public String description() {
        return "Здоровье и голод игроков связаны: урон, лечение, еда и истощение передаются всем.";
    }

    @Override
    public boolean isEligible(MinecraftServer server) {
        return BigEventPlayerPolicy.eligiblePlayers(server).stream()
                .filter(ServerPlayer::isAlive)
                .count() >= 2;
    }

    @Override
    public void start(MinecraftServer server) {
        List<ServerPlayer> players = livingEligiblePlayers(server);
        active = !players.isEmpty();
        lastSynchronized.clear();
        if (!active) return;

        sharedHealth = players.stream().map(ServerPlayer::getHealth).min(Float::compare).orElse(1.0F);
        sharedFood = players.stream().mapToInt(player -> player.getFoodData().getFoodLevel()).min().orElse(20);
        sharedSaturation = players.stream()
                .map(player -> player.getFoodData().getSaturationLevel())
                .min(Float::compare)
                .orElse(0.0F);
        sharedExhaustion = players.stream()
                .map(player -> player.getFoodData().getExhaustionLevel())
                .max(Float::compare)
                .orElse(0.0F);
        sharedSaturation = clamp(sharedSaturation, 0.0F, sharedFood);

        synchronize(players);
    }

    @Override
    public void tick(MinecraftServer server, int elapsedTicks, int remainingTicks) {
        if (!active) return;

        List<ServerPlayer> linkedPlayers = BigEventPlayerPolicy.eligiblePlayers(server);
        Set<UUID> linkedIds = new HashSet<>();
        for (ServerPlayer player : linkedPlayers) linkedIds.add(player.getUUID());
        lastSynchronized.keySet().retainAll(linkedIds);

        for (ServerPlayer player : linkedPlayers) {
            if (player.isAlive() && !lastSynchronized.containsKey(player.getUUID())) {
                applySharedVitals(player);
                lastSynchronized.put(player.getUUID(), Vitals.capture(player));
            }
        }

        float healthDelta = 0.0F;
        float foodDelta = 0.0F;
        float saturationDelta = 0.0F;
        float exhaustionDelta = 0.0F;

        for (ServerPlayer player : linkedPlayers) {
            Vitals before = lastSynchronized.get(player.getUUID());
            if (before == null) continue;
            Vitals now = Vitals.capture(player);
            healthDelta = lossFirstDelta(healthDelta, now.health() - before.health());
            foodDelta = lossFirstDelta(foodDelta, now.food() - before.food());
            saturationDelta = lossFirstDelta(saturationDelta, now.saturation() - before.saturation());
            exhaustionDelta = gainFirstDelta(exhaustionDelta, now.exhaustion() - before.exhaustion());
        }

        float maximumSharedHealth = linkedPlayers.stream()
                .filter(ServerPlayer::isAlive)
                .map(ServerPlayer::getMaxHealth)
                .min(Float::compare)
                .orElse(0.0F);
        sharedHealth = clamp(sharedHealth + healthDelta, 0.0F, maximumSharedHealth);
        sharedFood = clamp(Math.round(sharedFood + foodDelta), 0, 20);
        sharedSaturation = clamp(sharedSaturation + saturationDelta, 0.0F, sharedFood);
        sharedExhaustion = clamp(sharedExhaustion + exhaustionDelta, 0.0F, 40.0F);

        if (sharedHealth <= 0.0F) {
            propagateDeath(linkedPlayers);
        } else {
            synchronize(linkedPlayers);
        }
    }

    @Override
    public void stop(MinecraftServer server) {
        active = false;
        lastSynchronized.clear();
    }

    @Override
    public void excludePlayer(MinecraftServer server, ServerPlayer player) {
        lastSynchronized.remove(player.getUUID());
    }

    @Override
    public void includePlayer(MinecraftServer server, ServerPlayer player) {
        if (!active || !player.isAlive()) return;
        applySharedVitals(player);
        lastSynchronized.put(player.getUUID(), Vitals.capture(player));
    }

    private void synchronize(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            if (!player.isAlive()) continue;
            applySharedVitals(player);
            lastSynchronized.put(player.getUUID(), Vitals.capture(player));
        }
    }

    private void applySharedVitals(ServerPlayer player) {
        player.setHealth(Math.min(sharedHealth, player.getMaxHealth()));
        FoodData food = player.getFoodData();
        food.setFoodLevel(sharedFood);
        food.setSaturation(sharedSaturation);
        food.setExhaustion(sharedExhaustion);
    }

    private void propagateDeath(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            if (player.isAlive()) {
                player.hurt(player.damageSources().generic(), LETHAL_DAMAGE);
            }
        }

        List<ServerPlayer> survivors = players.stream().filter(ServerPlayer::isAlive).toList();
        if (!survivors.isEmpty()) {
            sharedHealth = survivors.stream()
                    .map(ServerPlayer::getHealth)
                    .min(Float::compare)
                    .orElse(1.0F);
            synchronize(survivors);
        }

        for (ServerPlayer player : players) {
            lastSynchronized.put(player.getUUID(), Vitals.capture(player));
        }
    }

    private static List<ServerPlayer> livingEligiblePlayers(MinecraftServer server) {
        return BigEventPlayerPolicy.eligiblePlayers(server).stream()
                .filter(ServerPlayer::isAlive)
                .toList();
    }

    private static float lossFirstDelta(float current, float candidate) {
        if (candidate < -EPSILON) {
            return current < -EPSILON ? Math.min(current, candidate) : candidate;
        }
        if (current < -EPSILON) return current;
        return candidate > current ? candidate : current;
    }

    private static float gainFirstDelta(float current, float candidate) {
        if (candidate > EPSILON) {
            return current > EPSILON ? Math.max(current, candidate) : candidate;
        }
        if (current > EPSILON) return current;
        return candidate < current ? candidate : current;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record Vitals(float health, int food, float saturation, float exhaustion) {
        private static Vitals capture(ServerPlayer player) {
            FoodData food = player.getFoodData();
            return new Vitals(
                    player.getHealth(),
                    food.getFoodLevel(),
                    food.getSaturationLevel(),
                    food.getExhaustionLevel()
            );
        }
    }
}
