package com.kiras.chaosevents.event;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Toxic air affects only players whose head is exposed to the sky. */
public final class SurfaceToxicAirEvent implements ChaosEvent {
    public static final SurfaceToxicAirEvent INSTANCE = new SurfaceToxicAirEvent();

    private static final int REFRESH_INTERVAL_TICKS = 10;
    private static final int EFFECT_DURATION_TICKS = 30;

    private final Set<UUID> affectedPlayers = new HashSet<>();

    private SurfaceToxicAirEvent() {
    }

    @Override
    public String id() {
        return "toxic_air";
    }

    @Override
    public String displayName() {
        return "Ядовитый воздух";
    }

    @Override
    public String description() {
        return "На открытой поверхности воздух отравляет и дезориентирует; крыша и подземелье защищают";
    }

    @Override
    public boolean harsh() {
        return true;
    }

    @Override
    public boolean isEligible(MinecraftServer server) {
        return !server.getPlayerList().getPlayers().isEmpty();
    }

    @Override
    public void start(MinecraftServer server) {
        affectedPlayers.clear();
    }

    @Override
    public void tick(MinecraftServer server, int elapsedTicks, int remainingTicks) {
        if (elapsedTicks % REFRESH_INTERVAL_TICKS != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator()) {
                clearEventEffects(player);
                continue;
            }

            if (isExposedToSky(player)) {
                applyEffect(player, MobEffects.POISON, 1);
                applyEffect(player, MobEffects.CONFUSION, 0);
                affectedPlayers.add(player.getUUID());
            } else {
                clearEventEffects(player);
            }
        }
    }

    @Override
    public void stop(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            clearEventEffects(player);
        }
        affectedPlayers.clear();
    }

    private static boolean isExposedToSky(ServerPlayer player) {
        return player.serverLevel().canSeeSky(player.blockPosition().above());
    }

    private static void applyEffect(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        player.addEffect(new MobEffectInstance(
                effect,
                EFFECT_DURATION_TICKS,
                amplifier,
                false,
                false,
                true
        ));
    }

    private void clearEventEffects(ServerPlayer player) {
        if (!affectedPlayers.remove(player.getUUID())) return;
        removeMatchingEffect(player, MobEffects.POISON, 1);
        removeMatchingEffect(player, MobEffects.CONFUSION, 0);
    }

    private static void removeMatchingEffect(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance active = player.getEffect(effect);
        if (active != null
                && active.getAmplifier() == amplifier
                && active.getDuration() <= EFFECT_DURATION_TICKS) {
            player.removeEffect(effect);
        }
    }
}
