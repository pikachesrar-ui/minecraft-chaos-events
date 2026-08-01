package com.kiras.chaosevents.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/** Thirty-five substantial built-in events. SpatialSwapEvent is registered as event number thirty-six. */
public enum BuiltinChaosEvent implements ChaosEvent {
    GRAVITY_FAILURE("gravity_failure", "Отказ гравитации", EventScope.ANY, true),
    CRUSHING_GRAVITY("crushing_gravity", "Сокрушительная гравитация", EventScope.ANY, true),
    BERSERKER_RUSH("berserker_rush", "Ярость берсерка", EventScope.ANY, true),
    TIME_QUICKSAND("time_quicksand", "Временная трясина", EventScope.ANY, true),
    TOTAL_DARKNESS("total_darkness", "Абсолютная тьма", EventScope.ANY, true),
    HUNTERS_MARK("hunters_mark", "Метка охотника", EventScope.ANY, true),
    LIFE_DRAIN("life_drain", "Похищение жизни", EventScope.ANY, true),
    TOXIC_AIR("toxic_air", "Ядовитый воздух", EventScope.ANY, true),
    FAMINE("famine", "Великий голод", EventScope.ANY, true),
    SKYHOOK("skyhook", "Небесный крюк", EventScope.ANY, true),
    CHAOS_ROULETTE("chaos_roulette", "Рулетка проклятий", EventScope.ANY, true),
    KINETIC_STORM("kinetic_storm", "Кинетический шторм", EventScope.ANY, true),

    LIGHTNING_HUNT("lightning_hunt", "Охота молний", EventScope.OVERWORLD, true),
    METEOR_BARRAGE("meteor_barrage", "Метеоритный обстрел", EventScope.OVERWORLD, true),
    BLOOD_MOON("blood_moon", "Кровавая луна", EventScope.OVERWORLD, true),
    ZOMBIE_SIEGE("zombie_siege", "Осада мертвецов", EventScope.OVERWORLD, true),
    SKELETON_VOLLEY("skeleton_volley", "Костяной расстрел", EventScope.OVERWORLD, true),
    SPIDER_BLOOM("spider_bloom", "Паучье гнездо", EventScope.OVERWORLD, true),
    CREEPER_MIGRATION("creeper_migration", "Миграция криперов", EventScope.OVERWORLD, true),

    LAVA_GEYSERS("lava_geysers", "Лавовые гейзеры", EventScope.NETHER, true),
    INFERNAL_HUNGER("infernal_hunger", "Ненасытный Ад", EventScope.NETHER, true),
    BLAZE_SWARM("blaze_swarm", "Рой ифритов", EventScope.NETHER, true),
    MAGMA_MARCH("magma_march", "Марш магмы", EventScope.NETHER, true),
    WITHERED_AIR("withered_air", "Иссушенный воздух", EventScope.NETHER, true),
    SOUL_CRUSH("soul_crush", "Давление душ", EventScope.NETHER, true),
    FIRESTORM("firestorm", "Огненный шторм", EventScope.NETHER, true),
    PIGLIN_HUNT("piglin_hunt", "Охота пиглинов", EventScope.NETHER, true),

    VOID_LIGHTNESS("void_lightness", "Срыв в Бездну", EventScope.END, true),
    ENDER_STATIC("ender_static", "Эндер-помехи", EventScope.END, true),
    ENDERMAN_CONVERGENCE("enderman_convergence", "Схождение эндерменов", EventScope.END, true),
    SHULKER_ECHO("shulker_echo", "Эхо шалкеров", EventScope.END, true),
    DRAGON_BREATH("dragon_breath", "Дыхание дракона", EventScope.END, true),
    CHORUS_SHIFT("chorus_shift", "Хоровой разлом", EventScope.END, true),
    VOID_SILENCE("void_silence", "Тишина Бездны", EventScope.END, true),
    END_CRYSTAL_STORM("end_crystal_storm", "Шторм кристаллов Края", EventScope.END, true);

    private static final int SHORT_EFFECT_TICKS = 100;
    private static final List<Holder<MobEffect>> ROULETTE_EFFECTS = List.of(
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.DIG_SLOWDOWN,
            MobEffects.WEAKNESS,
            MobEffects.DARKNESS,
            MobEffects.BLINDNESS,
            MobEffects.CONFUSION,
            MobEffects.HUNGER,
            MobEffects.POISON,
            MobEffects.LEVITATION,
            MobEffects.WITHER
    );

    private final String id;
    private final String displayName;
    private final EventScope scope;
    private final boolean harsh;

    BuiltinChaosEvent(String id, String displayName, EventScope scope, boolean harsh) {
        this.id = id;
        this.displayName = displayName;
        this.scope = scope;
        this.harsh = harsh;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return displayName; }
    @Override public boolean harsh() { return harsh; }
    @Override public boolean isEligible(MinecraftServer server) { return scope.hasEligiblePlayer(server); }

    @Override
    public void start(MinecraftServer server) {
        switch (this) {
            case BLOOD_MOON -> setTime(server, 18000L);
            case LIGHTNING_HUNT -> setWeather(server, true, true);
            case TOTAL_DARKNESS -> playForPlayers(server, SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.4F, 0.65F);
            case FIRESTORM, LAVA_GEYSERS -> playForPlayers(server, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.2F, 0.65F);
            case ENDER_STATIC, VOID_SILENCE, END_CRYSTAL_STORM ->
                    playForPlayers(server, SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.3F, 0.55F);
            default -> { }
        }
    }

    @Override
    public void tick(MinecraftServer server, int elapsedTicks, int remainingTicks) {
        if (elapsedTicks % 40 == 0) applyRecurringEffects(server);
        if (elapsedTicks > 0 && elapsedTicks % 100 == 0) applyFiveSecondPulse(server);
        if (elapsedTicks > 0 && elapsedTicks % 200 == 0) applyTenSecondPulse(server);
    }

    private void applyRecurringEffects(MinecraftServer server) {
        switch (this) {
            case GRAVITY_FAILURE -> forPlayers(server, player -> {
                effect(player, MobEffects.SLOW_FALLING, SHORT_EFFECT_TICKS, 0);
                effect(player, MobEffects.JUMP, SHORT_EFFECT_TICKS, 3);
            });
            case CRUSHING_GRAVITY -> forPlayers(server, player -> {
                effect(player, MobEffects.MOVEMENT_SLOWDOWN, SHORT_EFFECT_TICKS, 3);
                effect(player, MobEffects.WEAKNESS, SHORT_EFFECT_TICKS, 2);
                effect(player, MobEffects.DIG_SLOWDOWN, SHORT_EFFECT_TICKS, 2);
            });
            case BERSERKER_RUSH -> forPlayers(server, player -> {
                effect(player, MobEffects.MOVEMENT_SPEED, SHORT_EFFECT_TICKS, 2);
                effect(player, MobEffects.DAMAGE_BOOST, SHORT_EFFECT_TICKS, 1);
                effect(player, MobEffects.HUNGER, SHORT_EFFECT_TICKS, 2);
            });
            case TIME_QUICKSAND -> forPlayers(server, player -> {
                effect(player, MobEffects.MOVEMENT_SLOWDOWN, SHORT_EFFECT_TICKS, 3);
                effect(player, MobEffects.DIG_SLOWDOWN, SHORT_EFFECT_TICKS, 3);
                effect(player, MobEffects.CONFUSION, SHORT_EFFECT_TICKS, 0);
            });
            case TOTAL_DARKNESS -> forPlayers(server, player -> {
                effect(player, MobEffects.DARKNESS, SHORT_EFFECT_TICKS, 0);
                effect(player, MobEffects.BLINDNESS, 45, 0);
            });
            case HUNTERS_MARK -> forPlayers(server, player -> {
                effect(player, MobEffects.GLOWING, SHORT_EFFECT_TICKS, 0);
                effect(player, MobEffects.BAD_OMEN, SHORT_EFFECT_TICKS, 0);
            });
            case LIFE_DRAIN -> forPlayers(server, player -> {
                effect(player, MobEffects.WITHER, 65, 0);
                effect(player, MobEffects.WEAKNESS, SHORT_EFFECT_TICKS, 1);
            });
            case TOXIC_AIR -> forPlayers(server, player -> {
                effect(player, MobEffects.POISON, 75, 1);
                effect(player, MobEffects.CONFUSION, SHORT_EFFECT_TICKS, 0);
            });
            case FAMINE -> forPlayers(server, player -> {
                effect(player, MobEffects.HUNGER, SHORT_EFFECT_TICKS, 3);
                player.getFoodData().addExhaustion(1.5F);
            });
            case INFERNAL_HUNGER -> forPlayers(server, player -> {
                effect(player, MobEffects.HUNGER, SHORT_EFFECT_TICKS, 3);
                effect(player, MobEffects.WEAKNESS, SHORT_EFFECT_TICKS, 1);
            });
            case WITHERED_AIR -> forPlayers(server, player -> effect(player, MobEffects.WITHER, 70, 1));
            case SOUL_CRUSH -> forPlayers(server, player -> {
                effect(player, MobEffects.MOVEMENT_SLOWDOWN, SHORT_EFFECT_TICKS, 3);
                effect(player, MobEffects.DARKNESS, SHORT_EFFECT_TICKS, 0);
                effect(player, MobEffects.WEAKNESS, SHORT_EFFECT_TICKS, 1);
            });
            case VOID_LIGHTNESS -> forPlayers(server, player -> {
                effect(player, MobEffects.SLOW_FALLING, SHORT_EFFECT_TICKS, 0);
                effect(player, MobEffects.JUMP, SHORT_EFFECT_TICKS, 4);
            });
            case ENDER_STATIC -> forPlayers(server, player -> {
                effect(player, MobEffects.CONFUSION, SHORT_EFFECT_TICKS, 1);
                effect(player, MobEffects.DARKNESS, SHORT_EFFECT_TICKS, 0);
            });
            case VOID_SILENCE -> forPlayers(server, player -> {
                effect(player, MobEffects.BLINDNESS, SHORT_EFFECT_TICKS, 0);
                effect(player, MobEffects.WEAKNESS, SHORT_EFFECT_TICKS, 2);
            });
            default -> { }
        }
    }

    private void applyFiveSecondPulse(MinecraftServer server) {
        switch (this) {
            case GRAVITY_FAILURE, SKYHOOK, SHULKER_ECHO ->
                    forPlayers(server, player -> effect(player, MobEffects.LEVITATION, 42, 2));
            case CHAOS_ROULETTE -> forPlayers(server, player -> {
                Holder<MobEffect> selected = ROULETTE_EFFECTS.get(ThreadLocalRandom.current().nextInt(ROULETTE_EFFECTS.size()));
                effect(player, selected, 120, ThreadLocalRandom.current().nextInt(1, 3));
            });
            case KINETIC_STORM -> forPlayers(server, BuiltinChaosEvent::violentPush);
            case LIGHTNING_HUNT -> forPlayers(server, BuiltinChaosEvent::strikeLightning);
            case METEOR_BARRAGE -> forPlayers(server, player -> spawnMeteor(player, 2));
            case LAVA_GEYSERS -> forPlayers(server, player -> {
                player.igniteForSeconds(5.0F);
                player.setDeltaMovement(player.getDeltaMovement().add(0.0, 1.2, 0.0));
                player.serverLevel().sendParticles(ParticleTypes.LAVA, player.getX(), player.getY(), player.getZ(),
                        35, 1.5, 0.25, 1.5, 0.15);
            });
            case FIRESTORM -> forPlayers(server, player -> {
                player.igniteForSeconds(5.0F);
                player.serverLevel().sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0,
                        player.getZ(), 45, 1.4, 1.2, 1.4, 0.08);
            });
            case DRAGON_BREATH -> forPlayers(server, player -> {
                effect(player, MobEffects.POISON, 100, 1);
                player.serverLevel().sendParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + 1.0,
                        player.getZ(), 45, 1.5, 1.0, 1.5, 0.04);
            });
            case CHORUS_SHIFT -> forPlayers(server, player -> {
                randomTeleport(player, 14);
                rotateHotbar(player);
            });
            case ENDER_STATIC -> forPlayers(server, player -> randomTeleport(player, 9));
            case END_CRYSTAL_STORM -> forPlayers(server, BuiltinChaosEvent::spawnEndCrystal);
            default -> { }
        }
    }

    private void applyTenSecondPulse(MinecraftServer server) {
        switch (this) {
            case HUNTERS_MARK -> {
                spawnForPlayers(server, EntityType.ZOMBIE, 2);
                spawnForPlayers(server, EntityType.SKELETON, 1);
            }
            case BLOOD_MOON -> {
                spawnForPlayers(server, EntityType.ZOMBIE, 2);
                spawnForPlayers(server, EntityType.SKELETON, 2);
                spawnForPlayers(server, EntityType.SPIDER, 1);
            }
            case ZOMBIE_SIEGE -> spawnForPlayers(server, EntityType.ZOMBIE, 4);
            case SKELETON_VOLLEY -> spawnForPlayers(server, EntityType.SKELETON, 3);
            case SPIDER_BLOOM -> spawnForPlayers(server, EntityType.SPIDER, 4);
            case CREEPER_MIGRATION -> spawnForPlayers(server, EntityType.CREEPER, 2);
            case BLAZE_SWARM -> spawnForPlayers(server, EntityType.BLAZE, 2);
            case MAGMA_MARCH -> spawnForPlayers(server, EntityType.MAGMA_CUBE, 4);
            case PIGLIN_HUNT -> spawnForPlayers(server, EntityType.PIGLIN_BRUTE, 2);
            case ENDERMAN_CONVERGENCE -> spawnForPlayers(server, EntityType.ENDERMAN, 3);
            default -> { }
        }
    }

    @Override
    public void stop(MinecraftServer server) {
        if (this == LIGHTNING_HUNT) {
            for (ServerLevel level : server.getAllLevels()) {
                if (scope.matches(level)) level.setWeatherParameters(6000, 0, false, false);
            }
        }
    }

    private void forPlayers(MinecraftServer server, Consumer<ServerPlayer> action) {
        server.getPlayerList().getPlayers().stream().filter(scope::matches).forEach(action);
    }

    private void playForPlayers(MinecraftServer server, net.minecraft.sounds.SoundEvent sound,
                                SoundSource source, float volume, float pitch) {
        forPlayers(server, player -> player.serverLevel().playSound(null, player.blockPosition(), sound, source, volume, pitch));
    }

    private void setWeather(MinecraftServer server, boolean raining, boolean thundering) {
        for (ServerLevel level : server.getAllLevels()) {
            if (scope.matches(level)) level.setWeatherParameters(0, 20 * 60 * 10, raining, thundering);
        }
    }

    private void setTime(MinecraftServer server, long dayTime) {
        for (ServerLevel level : server.getAllLevels()) {
            if (scope.matches(level)) level.setDayTime(dayTime);
        }
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
    }

    private void spawnForPlayers(MinecraftServer server, EntityType<? extends Mob> type, int count) {
        forPlayers(server, player -> spawnNear(player, type, count));
    }

    private static void spawnNear(ServerPlayer player, EntityType<? extends Mob> type, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ServerLevel level = player.serverLevel();
        BlockPos base = player.blockPosition();
        for (int i = 0; i < count; i++) {
            Mob mob = type.create(level);
            if (mob == null) continue;
            double x = base.getX() + 0.5 + random.nextInt(-8, 9);
            double y = base.getY() + 1.0;
            double z = base.getZ() + 0.5 + random.nextInt(-8, 9);
            mob.moveTo(x, y, z, random.nextFloat() * 360.0F, 0.0F);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
        }
    }

    private static void strikeLightning(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning == null) return;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        lightning.moveTo(player.getX() + random.nextInt(-4, 5), player.getY(), player.getZ() + random.nextInt(-4, 5));
        level.addFreshEntity(lightning);
    }

    private static void spawnMeteor(ServerPlayer player, int count) {
        ServerLevel level = player.serverLevel();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            PrimedTnt tnt = EntityType.TNT.create(level);
            if (tnt == null) continue;
            tnt.moveTo(player.getX() + random.nextInt(-10, 11), player.getY() + random.nextInt(12, 22),
                    player.getZ() + random.nextInt(-10, 11));
            tnt.setFuse(random.nextInt(45, 75));
            level.addFreshEntity(tnt);
        }
    }

    private static void spawnEndCrystal(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        EndCrystal crystal = EntityType.END_CRYSTAL.create(level);
        if (crystal == null) return;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        crystal.moveTo(player.getX() + random.nextInt(-7, 8), player.getY() + 1.0,
                player.getZ() + random.nextInt(-7, 8));
        crystal.setShowBottom(false);
        level.addFreshEntity(crystal);
    }

    private static void violentPush(ServerPlayer player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 push = new Vec3(random.nextDouble(-1.8, 1.8), random.nextDouble(0.5, 1.25), random.nextDouble(-1.8, 1.8));
        player.setDeltaMovement(player.getDeltaMovement().add(push));
    }

    private static void randomTeleport(ServerPlayer player, int radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        player.randomTeleport(player.getX() + random.nextInt(-radius, radius + 1),
                player.getY() + random.nextInt(-3, 5),
                player.getZ() + random.nextInt(-radius, radius + 1), true);
    }

    private static void rotateHotbar(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        ItemStack last = inventory.getItem(8).copy();
        for (int slot = 8; slot > 0; slot--) inventory.setItem(slot, inventory.getItem(slot - 1).copy());
        inventory.setItem(0, last);
        inventory.setChanged();
    }
}
