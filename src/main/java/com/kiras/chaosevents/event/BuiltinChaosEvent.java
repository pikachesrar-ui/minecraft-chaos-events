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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * The first complete built-in event catalogue. The event engine sees every
 * enum value through the {@link ChaosEvent} interface, so future events can be
 * moved into separate classes without changing session/timer code.
 */
public enum BuiltinChaosEvent implements ChaosEvent {

    FEATHERWEIGHT("featherweight", "Пёрышко", EventScope.ANY, false),
    CRUSHING_GRAVITY("crushing_gravity", "Сокрушительная гравитация", EventScope.ANY, true),
    OVERCLOCK("overclock", "Разгон мира", EventScope.ANY, false),
    TEMPORAL_DRAG("temporal_drag", "Вязкое время", EventScope.ANY, false),
    TOTAL_DARKNESS("total_darkness", "Абсолютная тьма", EventScope.ANY, true),
    GLOWING_PREY("glowing_prey", "Светящаяся добыча", EventScope.ANY, false),
    REGEN_TIDE("regen_tide", "Волна восстановления", EventScope.ANY, false),
    TOXIC_AIR("toxic_air", "Ядовитый воздух", EventScope.ANY, true),
    HUNGER_GAMES("hunger_games", "Голодные игры", EventScope.ANY, true),
    SKYHOOK("skyhook", "Небесный крюк", EventScope.ANY, true),
    CHAOS_ROULETTE("chaos_roulette", "Рулетка эффектов", EventScope.ANY, false),
    KINETIC_STORM("kinetic_storm", "Кинетический шторм", EventScope.ANY, true),

    THUNDER_CROWN("thunder_crown", "Грозовая корона", EventScope.OVERWORLD, true),
    ENDLESS_RAIN("endless_rain", "Бесконечный ливень", EventScope.OVERWORLD, false),
    BLOOD_MOON("blood_moon", "Кровавая луна", EventScope.OVERWORLD, true),
    ZOMBIE_SIEGE("zombie_siege", "Осада мертвецов", EventScope.OVERWORLD, true),
    SKELETON_VOLLEY("skeleton_volley", "Костяной залп", EventScope.OVERWORLD, true),
    SPIDER_BLOOM("spider_bloom", "Паучий цвет", EventScope.OVERWORLD, true),
    CREEPER_MIGRATION("creeper_migration", "Миграция криперов", EventScope.OVERWORLD, true),
    BOUNTY_RAIN("bounty_rain", "Дождь припасов", EventScope.OVERWORLD, false),

    NETHER_FEVER("nether_fever", "Адская лихорадка", EventScope.NETHER, false),
    INFERNAL_HUNGER("infernal_hunger", "Ненасытный Ад", EventScope.NETHER, true),
    BLAZE_SWARM("blaze_swarm", "Рой ифритов", EventScope.NETHER, true),
    MAGMA_MARCH("magma_march", "Марш магмы", EventScope.NETHER, true),
    WITHERED_AIR("withered_air", "Иссушенный воздух", EventScope.NETHER, true),
    SOUL_SLOW("soul_slow", "Тяжесть душ", EventScope.NETHER, false),
    FIRESTORM("firestorm", "Огненный шторм", EventScope.NETHER, true),
    GOLD_RUSH("gold_rush", "Золотая горячка", EventScope.NETHER, false),

    VOID_LIGHTNESS("void_lightness", "Лёгкость Бездны", EventScope.END, false),
    ENDER_STATIC("ender_static", "Эндер-помехи", EventScope.END, true),
    ENDERMAN_CONVERGENCE("enderman_convergence", "Схождение эндерменов", EventScope.END, true),
    SHULKER_ECHO("shulker_echo", "Эхо шалкеров", EventScope.END, true),
    DRAGON_BREATH("dragon_breath", "Дыхание дракона", EventScope.END, true),
    CHORUS_SHIFT("chorus_shift", "Хоровой сдвиг", EventScope.END, false),
    VOID_SILENCE("void_silence", "Тишина Бездны", EventScope.END, true),
    PEARL_RAIN("pearl_rain", "Жемчужный дождь", EventScope.END, false);

    private static final int SHORT_EFFECT_TICKS = 100;
    private static final List<Holder<MobEffect>> ROULETTE_EFFECTS = List.of(
            MobEffects.MOVEMENT_SPEED,
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.DIG_SPEED,
            MobEffects.DIG_SLOWDOWN,
            MobEffects.JUMP,
            MobEffects.REGENERATION,
            MobEffects.WEAKNESS,
            MobEffects.GLOWING,
            MobEffects.DARKNESS,
            MobEffects.SLOW_FALLING
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

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public boolean harsh() {
        return harsh;
    }

    @Override
    public boolean isEligible(MinecraftServer server) {
        return scope.hasEligiblePlayer(server);
    }

    @Override
    public void start(MinecraftServer server) {
        switch (this) {
            case THUNDER_CROWN -> setWeather(server, true, true);
            case ENDLESS_RAIN -> setWeather(server, true, false);
            case BLOOD_MOON -> setTime(server, 18000L);
            case TOTAL_DARKNESS -> playForPlayers(server, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0F, 0.7F);
            case FIRESTORM -> playForPlayers(server, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 0.65F);
            case ENDER_STATIC, VOID_SILENCE -> playForPlayers(server, SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.0F, 0.6F);
            default -> {
            }
        }
    }

    @Override
    public void tick(MinecraftServer server, int elapsedTicks, int remainingTicks) {
        if (elapsedTicks % 40 == 0) {
            applyRecurringEffects(server);
        }

        if (elapsedTicks > 0 && elapsedTicks % 100 == 0) {
            applyFiveSecondPulse(server);
        }

        if (elapsedTicks > 0 && elapsedTicks % 200 == 0) {
            applyTenSecondPulse(server);
        }
    }

    private void applyRecurringEffects(MinecraftServer server) {
        switch (this) {
            case FEATHERWEIGHT -> forPlayers(server, player -> {
                effect(player, MobEffects.SLOW_FALLING, SHORT_EFFECT_TICKS, 0);
                effect(player, MobEffects.JUMP, SHORT_EFFECT_TICKS, 1);
            });
            case CRUSHING_GRAVITY -> forPlayers(server, player -> {
                effect(player, MobEffects.MOVEMENT_SLOWDOWN, SHORT_EFFECT_TICKS, 2);
                effect(player, MobEffects.WEAKNESS, SHORT_EFFECT_TICKS, 1);
            });
            case OVERCLOCK -> forPlayers(server, player -> {
                effect(player, MobEffects.MOVEMENT_SPEED, SHORT_EFFECT_TICKS, 1);
                effect(player, MobEffects.DIG_SPEED, SHORT_EFFECT_TICKS, 1);
            });
            case TEMPORAL_DRAG -> forPlayers(server, player -> {
                effect(player, MobEffects.MOVEMENT_SLOWDOWN, SHORT_EFFECT_TICKS, 1);
                effect(player, MobEffects.DIG_SLOWDOWN, SHORT_EFFECT_TICKS, 1);
            });
            case TOTAL_DARKNESS -> forPlayers(server, player -> effect(player, MobEffects.DARKNESS, SHORT_EFFECT_TICKS, 0));
            case GLOWING_PREY -> forPlayers(server, player -> effect(player, MobEffects.GLOWING, SHORT_EFFECT_TICKS, 0));
            case REGEN_TIDE -> forPlayers(server, player -> {
                effect(player, MobEffects.REGENERATION, SHORT_EFFECT_TICKS, 0);
                effect(player, MobEffects.ABSORPTION, SHORT_EFFECT_TICKS, 0);
            });
            case TOXIC_AIR -> forPlayers(server, player -> effect(player, MobEffects.POISON, SHORT_EFFECT_TICKS, 0));
            case HUNGER_GAMES -> forPlayers(server, player -> effect(player, MobEffects.HUNGER, SHORT_EFFECT_TICKS, 1));
            case NETHER_FEVER -> forPlayers(server, player -> {
                effect(player, MobEffects.FIRE_RESISTANCE, SHORT_EFFECT_TICKS, 0);
                effect(player, MobEffects.MOVEMENT_SPEED, SHORT_EFFECT_TICKS, 1);
            });
            case INFERNAL_HUNGER -> forPlayers(server, player -> {
                effect(player, MobEffects.HUNGER, SHORT_EFFECT_TICKS, 2);
                effect(player, MobEffects.WEAKNESS, SHORT_EFFECT_TICKS, 0);
            });
            case WITHERED_AIR -> forPlayers(server, player -> effect(player, MobEffects.WITHER, SHORT_EFFECT_TICKS, 0));
            case SOUL_SLOW -> forPlayers(server, player -> {
                effect(player, MobEffects.MOVEMENT_SLOWDOWN, SHORT_EFFECT_TICKS, 1);
                effect(player, MobEffects.DARKNESS, SHORT_EFFECT_TICKS, 0);
            });
            case VOID_LIGHTNESS -> forPlayers(server, player -> {
                effect(player, MobEffects.SLOW_FALLING, SHORT_EFFECT_TICKS, 0);
                effect(player, MobEffects.JUMP, SHORT_EFFECT_TICKS, 1);
            });
            case ENDER_STATIC -> forPlayers(server, player -> {
                effect(player, MobEffects.CONFUSION, SHORT_EFFECT_TICKS, 0);
                effect(player, MobEffects.DARKNESS, SHORT_EFFECT_TICKS, 0);
            });
            case VOID_SILENCE -> forPlayers(server, player -> {
                effect(player, MobEffects.BLINDNESS, SHORT_EFFECT_TICKS, 0);
                effect(player, MobEffects.WEAKNESS, SHORT_EFFECT_TICKS, 1);
            });
            default -> {
            }
        }
    }

    private void applyFiveSecondPulse(MinecraftServer server) {
        switch (this) {
            case SKYHOOK, SHULKER_ECHO -> forPlayers(server, player -> effect(player, MobEffects.LEVITATION, 35, 1));
            case CHAOS_ROULETTE -> forPlayers(server, player -> {
                Holder<MobEffect> effect = ROULETTE_EFFECTS.get(ThreadLocalRandom.current().nextInt(ROULETTE_EFFECTS.size()));
                effect(player, effect, 100, ThreadLocalRandom.current().nextInt(2));
            });
            case KINETIC_STORM -> forPlayers(server, BuiltinChaosEvent::randomPush);
            case THUNDER_CROWN -> forPlayers(server, player ->
                    player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                            SoundSource.WEATHER, 1.5F, 0.8F + ThreadLocalRandom.current().nextFloat() * 0.4F));
            case FIRESTORM -> forPlayers(server, player -> {
                player.setSecondsOnFire(3);
                player.serverLevel().sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0,
                        player.getZ(), 24, 1.0, 1.0, 1.0, 0.05);
            });
            case DRAGON_BREATH -> forPlayers(server, player -> {
                effect(player, MobEffects.POISON, 80, 0);
                player.serverLevel().sendParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + 1.0,
                        player.getZ(), 30, 1.2, 0.8, 1.2, 0.02);
            });
            case CHORUS_SHIFT -> forPlayers(server, BuiltinChaosEvent::rotateHotbar);
            default -> {
            }
        }
    }

    private void applyTenSecondPulse(MinecraftServer server) {
        switch (this) {
            case BLOOD_MOON, ZOMBIE_SIEGE -> spawnForPlayers(server, EntityType.ZOMBIE, BLOOD_MOON == this ? 1 : 2);
            case SKELETON_VOLLEY -> spawnForPlayers(server, EntityType.SKELETON, 2);
            case SPIDER_BLOOM -> spawnForPlayers(server, EntityType.SPIDER, 2);
            case CREEPER_MIGRATION -> spawnForPlayers(server, EntityType.CREEPER, 1);
            case BOUNTY_RAIN -> forPlayers(server, BuiltinChaosEvent::dropOverworldBounty);
            case BLAZE_SWARM -> spawnForPlayers(server, EntityType.BLAZE, 1);
            case MAGMA_MARCH -> spawnForPlayers(server, EntityType.MAGMA_CUBE, 2);
            case GOLD_RUSH -> forPlayers(server, player -> {
                player.drop(new ItemStack(Items.GOLD_NUGGET, ThreadLocalRandom.current().nextInt(1, 5)), false);
                effect(player, MobEffects.LUCK, 220, 0);
            });
            case ENDERMAN_CONVERGENCE -> spawnForPlayers(server, EntityType.ENDERMAN, 2);
            case PEARL_RAIN -> forPlayers(server, player ->
                    player.drop(new ItemStack(Items.ENDER_PEARL, 1), false));
            default -> {
            }
        }
    }

    @Override
    public void stop(MinecraftServer server) {
        if (this == THUNDER_CROWN || this == ENDLESS_RAIN) {
            for (ServerLevel level : server.getAllLevels()) {
                if (scope.matches(level)) {
                    level.setWeatherParameters(6000, 0, false, false);
                }
            }
        }
    }

    private void forPlayers(MinecraftServer server, Consumer<ServerPlayer> action) {
        server.getPlayerList().getPlayers().stream()
                .filter(scope::matches)
                .forEach(action);
    }

    private void playForPlayers(MinecraftServer server, net.minecraft.sounds.SoundEvent sound,
                                SoundSource source, float volume, float pitch) {
        forPlayers(server, player -> player.serverLevel().playSound(
                null, player.blockPosition(), sound, source, volume, pitch
        ));
    }

    private void setWeather(MinecraftServer server, boolean raining, boolean thundering) {
        for (ServerLevel level : server.getAllLevels()) {
            if (scope.matches(level)) {
                level.setWeatherParameters(0, 20 * 60 * 10, raining, thundering);
            }
        }
    }

    private void setTime(MinecraftServer server, long dayTime) {
        for (ServerLevel level : server.getAllLevels()) {
            if (scope.matches(level)) {
                level.setDayTime(dayTime);
            }
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
            if (mob == null) {
                continue;
            }

            int dx = random.nextInt(-8, 9);
            int dz = random.nextInt(-8, 9);
            double x = base.getX() + 0.5 + dx;
            double y = base.getY() + 1.0;
            double z = base.getZ() + 0.5 + dz;
            float yaw = (float) (random.nextDouble() * 360.0);

            mob.moveTo(x, y, z, yaw, 0.0F);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
        }
    }

    private static void randomPush(ServerPlayer player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 push = new Vec3(
                random.nextDouble(-0.9, 0.9),
                random.nextDouble(0.25, 0.65),
                random.nextDouble(-0.9, 0.9)
        );
        player.setDeltaMovement(player.getDeltaMovement().add(push));
    }

    private static void rotateHotbar(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        ItemStack last = inventory.getItem(8).copy();
        for (int slot = 8; slot > 0; slot--) {
            inventory.setItem(slot, inventory.getItem(slot - 1).copy());
        }
        inventory.setItem(0, last);
        inventory.setChanged();
    }

    private static void dropOverworldBounty(ServerPlayer player) {
        List<ItemStack> choices = new ArrayList<>();
        choices.add(new ItemStack(Items.BREAD, 2));
        choices.add(new ItemStack(Items.ARROW, 4));
        choices.add(new ItemStack(Items.IRON_NUGGET, 5));
        choices.add(new ItemStack(Items.SLIME_BALL, 2));
        choices.add(new ItemStack(Items.TORCH, 4));

        ItemStack selected = choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
        player.drop(selected, false);
    }
}
