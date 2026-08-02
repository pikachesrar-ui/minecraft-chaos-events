package com.kiras.chaosevents.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Original NeoForge implementations inspired only by broad chaos-gameplay ideas.
 * No code, assets or shaders from external mods are included here.
 */
public enum ExpandedChaosEvent implements ChaosEvent {
    ENTITY_MAGNET("entity_magnet", "Голодный магнит", "Все вокруг неудержимо тянется к игрокам", EventScope.ANY, true),
    FORCEFIELD("forcefield", "Силовое поле", "Игроки отталкивают существ, предметы и снаряды", EventScope.ANY, true),
    BOUNCY_WORLD("bouncy_world", "Пружинящий мир", "Поверхность постоянно подбрасывает живых существ", EventScope.ANY, true),
    SLIPPERY_WORLD("slippery_world", "Скользкий мир", "Движение по земле становится почти неуправляемым", EventScope.ANY, true),
    FLING_ENTITIES("fling_entities", "Массовый подброс", "Все ближайшие сущности регулярно взлетают", EventScope.ANY, true),
    TELEPORT_NEARBY_ENTITIES("teleport_nearby_entities", "Сбор существ", "Ближайшие сущности внезапно перемещаются к игрокам", EventScope.ANY, true),
    IGNITE_NEARBY_ENTITIES("ignite_nearby_entities", "Живая растопка", "Существа вокруг игроков периодически загораются", EventScope.ANY, true),
    HIGHLIGHT_MOBS("highlight_mobs", "Живые силуэты", "Все ближайшие существа видны сквозь стены", EventScope.ANY, false),
    INVISIBLE_HOSTILES("invisible_hostiles", "Невидимые хищники", "Враждебные существа скрываются из виду", EventScope.ANY, true),
    INVISIBLE_EVERYONE("invisible_everyone", "Исчезнувший мир", "Живые существа и игроки становятся невидимыми", EventScope.ANY, true),
    ITEM_RAIN("item_rain", "Предметный ливень", "С неба падают случайные полезные и бесполезные предметы", EventScope.ANY, false),
    XP_RAIN("xp_rain", "Опытный дождь", "Небо разбрасывает сферы опыта", EventScope.ANY, false),
    CHICKEN_RAIN("chicken_rain", "Куриный дождь", "Сверху непрерывно падают курицы", EventScope.OVERWORLD, false),
    ARROW_RAIN("arrow_rain", "Дождь из стрел", "Над игроками появляются падающие стрелы", EventScope.ANY, true),
    RAINBOW_SHEEP("rainbow_sheep", "Радужное стадо", "Разноцветные овцы заполняют окрестности", EventScope.OVERWORLD, false),
    SLIME_OVERLOAD("slime_overload", "Перегрузка слизнями", "Слизни разных размеров окружают игроков", EventScope.ANY, true),
    VEX_ASSAULT("vex_assault", "Атака досаждателей", "Досаждатели прибывают небольшими волнами", EventScope.ANY, true),
    BEE_SWARM("bee_swarm", "Разъярённый рой", "Стаи пчёл выбирают игроков целью", EventScope.OVERWORLD, true),
    SILVERFISH_INFESTATION("silverfish_infestation", "Живая кладка", "Чешуйницы выползают рядом с игроками", EventScope.ANY, true),
    PHANTOM_SKY("phantom_sky", "Неспящее небо", "Фантомы атакуют независимо от времени сна", EventScope.OVERWORLD, true),
    RANDOM_BLINK("random_blink", "Пространственные рывки", "Всех игроков регулярно сдвигает в случайную сторону", EventScope.ANY, true),
    HOTBAR_TEMPEST("hotbar_tempest", "Буря в хотбаре", "Предметы на панели постоянно меняются местами", EventScope.ANY, true),
    HAUNTED_CONTAINERS("haunted_containers", "Голодные сундуки", "Вокруг слышатся открывающиеся и захлопывающиеся сундуки", EventScope.ANY, false),
    FORCED_MOUNTS("forced_mounts", "Время верховой езды", "Каждый игрок получает внезапного скакуна", EventScope.OVERWORLD, false),
    COBWEB_TRAPS("cobweb_traps", "Паутинные ловушки", "Вокруг игроков возникают временные паутины", EventScope.ANY, true),
    FROST_PATH("frost_path", "Истинный ледоход", "Вода рядом с игроками временно превращается в лёд", EventScope.OVERWORLD, false),
    RAINBOW_PATH("rainbow_path", "Радужный путь", "След игроков временно окрашивает поверхность", EventScope.ANY, false),
    FIRE_TRAIL("fire_trail", "Огненный след", "За игроками остаются временные языки пламени", EventScope.ANY, true),
    EXPLOSIVE_MINING("explosive_mining", "Взрывоопасные инструменты", "Каждый добытый блок вызывает безопасную ударную волну", EventScope.ANY, true),
    HEALING_WAVE("healing_wave", "Волна восстановления", "Здоровье игроков постепенно восполняется", EventScope.ANY, false),
    SATIATION("satiation", "Бесконечный пир", "Голод и истощение перестают быть проблемой", EventScope.ANY, false),
    RESISTANCE("resistance", "Несокрушимость", "Игроки получают сильное сопротивление урону", EventScope.ANY, false),
    NIGHT_VISION("night_vision", "Всевидение", "Темнота перестаёт скрывать окружающий мир", EventScope.ANY, false),
    STARTER_SUPPLY("starter_supply", "Аварийные припасы", "Игрокам выдаётся небольшой набор расходников", EventScope.ANY, false),
    GEAR_REPAIR("gear_repair", "Саморемонт", "Повреждённое снаряжение постепенно чинится", EventScope.ANY, false),
    GEAR_DAMAGE("gear_damage", "Ржавчина", "Снаряжение игроков постепенно теряет прочность", EventScope.ANY, true),
    RANDOM_ORE("random_ore", "Минеральная удача", "Игроки периодически получают случайное сырьё", EventScope.ANY, false),
    SKYWARD_SURGE("skyward_surge", "На луну", "Небо резко подбрасывает игроков и смягчает падение", EventScope.ANY, true),
    INTENSE_STORM("intense_storm", "Великая гроза", "Над обычным миром начинается затяжная гроза", EventScope.OVERWORLD, true),
    RANDOM_CREEPERS("random_creepers", "Криперофобия", "Криперы неожиданно появляются небольшими группами", EventScope.OVERWORLD, true);

    private static final int SHORT_EFFECT_TICKS = 100;
    private static final int MAX_TEMPORARY_BLOCKS = 2048;
    private static final List<Item> RAIN_ITEMS = List.of(
            Items.STICK, Items.FEATHER, Items.BREAD, Items.COOKED_BEEF, Items.ARROW,
            Items.SLIME_BALL, Items.FIREWORK_ROCKET, Items.EXPERIENCE_BOTTLE,
            Items.IRON_INGOT, Items.GOLD_INGOT, Items.EMERALD, Items.ENDER_PEARL
    );
    private static final List<Item> ORE_REWARDS = List.of(
            Items.COAL, Items.RAW_IRON, Items.RAW_COPPER, Items.RAW_GOLD,
            Items.REDSTONE, Items.LAPIS_LAZULI, Items.QUARTZ, Items.EMERALD
    );
    private static final List<Block> RAINBOW_BLOCKS = List.of(
            Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE,
            Blocks.LIME_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.BLUE_CONCRETE,
            Blocks.PURPLE_CONCRETE, Blocks.MAGENTA_CONCRETE, Blocks.PINK_CONCRETE
    );

    private final String id;
    private final String displayName;
    private final String description;
    private final EventScope scope;
    private final boolean harsh;
    private final Map<WorldPos, ChangedBlock> temporaryBlocks = new LinkedHashMap<>();
    private boolean running;

    ExpandedChaosEvent(String id, String displayName, String description, EventScope scope, boolean harsh) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.scope = scope;
        this.harsh = harsh;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return displayName; }
    @Override public String description() { return description; }
    @Override public boolean harsh() { return harsh; }
    @Override public boolean isEligible(MinecraftServer server) { return scope.hasEligiblePlayer(server); }

    @Override
    public void start(MinecraftServer server) {
        running = true;
        temporaryBlocks.clear();
        switch (this) {
            case INTENSE_STORM -> setWeather(server, true, true);
            case STARTER_SUPPLY -> forPlayers(server, ExpandedChaosEvent::giveStarterSupply);
            case GEAR_REPAIR -> forPlayers(server, player -> repairGear(player, true));
            case GEAR_DAMAGE -> forPlayers(server, player -> damageGear(player, true));
            case RANDOM_ORE -> forPlayers(server, ExpandedChaosEvent::giveRandomOre);
            case FORCED_MOUNTS -> forPlayers(server, ExpandedChaosEvent::forceMount);
            case SKYWARD_SURGE -> forPlayers(server, ExpandedChaosEvent::launchSkyward);
            case HAUNTED_CONTAINERS -> forPlayers(server, player -> hauntedSounds(player, 5));
            default -> { }
        }
    }

    @Override
    public void tick(MinecraftServer server, int elapsedTicks, int remainingTicks) {
        if (!running) return;

        if (elapsedTicks % 2 == 0) {
            switch (this) {
                case ENTITY_MAGNET -> forPlayers(server, player -> applyField(player, true));
                case FORCEFIELD -> forPlayers(server, player -> applyField(player, false));
                default -> { }
            }
        }

        if (elapsedTicks % 10 == 0) {
            switch (this) {
                case BOUNCY_WORLD -> forPlayers(server, ExpandedChaosEvent::bounceNearby);
                case SLIPPERY_WORLD -> forPlayers(server, ExpandedChaosEvent::slipNearby);
                case RAINBOW_PATH -> forPlayers(server, this::paintRainbowPath);
                case FIRE_TRAIL -> forPlayers(server, this::placeFireTrail);
                case FROST_PATH -> forPlayers(server, this::freezeNearbyWater);
                default -> { }
            }
        }

        if (elapsedTicks % 40 == 0) {
            switch (this) {
                case HIGHLIGHT_MOBS -> forPlayers(server, player -> effectNearbyLiving(player, MobEffects.GLOWING, false));
                case INVISIBLE_HOSTILES -> forPlayers(server, player -> effectNearbyLiving(player, MobEffects.INVISIBILITY, true));
                case INVISIBLE_EVERYONE -> {
                    forPlayers(server, player -> effect(player, MobEffects.INVISIBILITY, SHORT_EFFECT_TICKS, 0));
                    forPlayers(server, player -> effectNearbyLiving(player, MobEffects.INVISIBILITY, false));
                }
                case HEALING_WAVE -> forPlayers(server, player -> player.heal(2.0F));
                case SATIATION -> forPlayers(server, player -> {
                    player.getFoodData().setFoodLevel(20);
                    player.getFoodData().setSaturation(10.0F);
                });
                case RESISTANCE -> forPlayers(server, player -> effect(player, MobEffects.DAMAGE_RESISTANCE, SHORT_EFFECT_TICKS, 2));
                case NIGHT_VISION -> forPlayers(server, player -> effect(player, MobEffects.NIGHT_VISION, 240, 0));
                case GEAR_REPAIR -> forPlayers(server, player -> repairGear(player, false));
                case GEAR_DAMAGE -> forPlayers(server, player -> damageGear(player, false));
                default -> { }
            }
        }

        if (elapsedTicks > 0 && elapsedTicks % 100 == 0) {
            switch (this) {
                case FLING_ENTITIES -> forPlayers(server, ExpandedChaosEvent::flingNearby);
                case IGNITE_NEARBY_ENTITIES -> forPlayers(server, ExpandedChaosEvent::igniteNearby);
                case ITEM_RAIN -> forPlayers(server, player -> rainItems(player, 5));
                case XP_RAIN -> forPlayers(server, player -> rainExperience(player, 5));
                case CHICKEN_RAIN -> forPlayers(server, player -> rainMobs(player, EntityType.CHICKEN, 3, 12));
                case ARROW_RAIN -> forPlayers(server, player -> rainArrows(player, 5));
                case RAINBOW_SHEEP -> forPlayers(server, player -> spawnRainbowSheep(player, 2));
                case HAUNTED_CONTAINERS -> forPlayers(server, player -> hauntedSounds(player, 4));
                case COBWEB_TRAPS -> forPlayers(server, this::placeCobwebTrap);
                case SKYWARD_SURGE -> forPlayers(server, ExpandedChaosEvent::launchSkyward);
                default -> { }
            }
        }

        if (elapsedTicks > 0 && elapsedTicks % 200 == 0) {
            switch (this) {
                case TELEPORT_NEARBY_ENTITIES -> forPlayers(server, ExpandedChaosEvent::teleportNearbyEntities);
                case SLIME_OVERLOAD -> forPlayers(server, player -> spawnSlimes(player, 4));
                case VEX_ASSAULT -> forPlayers(server, player -> spawnTargetingMobs(player, EntityType.VEX, 2));
                case BEE_SWARM -> forPlayers(server, player -> spawnTargetingMobs(player, EntityType.BEE, 4));
                case SILVERFISH_INFESTATION -> forPlayers(server, player -> spawnTargetingMobs(player, EntityType.SILVERFISH, 5));
                case PHANTOM_SKY -> forPlayers(server, player -> spawnTargetingMobs(player, EntityType.PHANTOM, 2));
                case RANDOM_BLINK -> forPlayers(server, player -> randomTeleport(player, 20));
                case HOTBAR_TEMPEST -> forPlayers(server, ExpandedChaosEvent::shuffleHotbar);
                case RANDOM_ORE -> forPlayers(server, ExpandedChaosEvent::giveRandomOre);
                case RANDOM_CREEPERS -> forPlayers(server, player -> spawnTargetingMobs(player, EntityType.CREEPER, 2));
                default -> { }
            }
        }
    }

    @Override
    public void stop(MinecraftServer server) {
        running = false;
        restoreTemporaryBlocks(server);
        if (this == INTENSE_STORM) {
            for (ServerLevel level : server.getAllLevels()) {
                if (scope.matches(level)) level.setWeatherParameters(6000, 0, false, false);
            }
        }
    }

    public static void onBlockBroken(ServerPlayer player, BlockPos brokenPos) {
        if (!EXPLOSIVE_MINING.running) return;

        ServerLevel level = player.serverLevel();
        double x = brokenPos.getX() + 0.5;
        double y = brokenPos.getY() + 0.5;
        double z = brokenPos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 4, 0.45, 0.45, 0.45, 0.02);
        level.playSound(null, brokenPos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.65F, 1.35F);

        AABB area = new AABB(x - 3.5, y - 3.5, z - 3.5, x + 3.5, y + 3.5, z + 3.5);
        for (Entity entity : level.getEntities(player, area, Entity::isAlive)) {
            Vec3 away = entity.position().subtract(new Vec3(x, y, z));
            if (away.lengthSqr() < 0.01) away = new Vec3(0.0, 1.0, 0.0);
            entity.setDeltaMovement(entity.getDeltaMovement().add(away.normalize().scale(0.75).add(0.0, 0.25, 0.0)));
        }
    }

    private void forPlayers(MinecraftServer server, Consumer<ServerPlayer> action) {
        server.getPlayerList().getPlayers().stream().filter(scope::matches).forEach(action);
    }

    private static void applyField(ServerPlayer player, boolean attract) {
        forNearbyEntities(player, attract ? 24.0 : 13.0, entity -> {
            Vec3 delta = player.position().add(0.0, 1.0, 0.0).subtract(entity.position());
            if (!attract) delta = delta.scale(-1.0);
            double distance = Math.max(1.0, delta.length());
            double strength = attract ? 0.16 : 0.28;
            Vec3 impulse = delta.normalize().scale(strength * Math.min(2.5, 8.0 / distance));
            entity.setDeltaMovement(entity.getDeltaMovement().add(impulse));
        });
    }

    private static void bounceNearby(ServerPlayer player) {
        if (player.onGround()) {
            Vec3 movement = player.getDeltaMovement();
            player.setDeltaMovement(movement.x, Math.max(0.75, movement.y), movement.z);
        }
        forNearbyEntities(player, 30.0, entity -> {
            if (entity.onGround()) {
                Vec3 movement = entity.getDeltaMovement();
                entity.setDeltaMovement(movement.x, Math.max(0.7, movement.y), movement.z);
            }
        });
    }

    private static void slipNearby(ServerPlayer player) {
        accelerateHorizontally(player, 1.12, 1.8);
        forNearbyEntities(player, 30.0, entity -> accelerateHorizontally(entity, 1.10, 2.2));
    }

    private static void accelerateHorizontally(Entity entity, double multiplier, double cap) {
        if (!entity.onGround()) return;
        Vec3 movement = entity.getDeltaMovement();
        double x = movement.x * multiplier;
        double z = movement.z * multiplier;
        double horizontal = Math.sqrt(x * x + z * z);
        if (horizontal > cap) {
            x = x / horizontal * cap;
            z = z / horizontal * cap;
        }
        entity.setDeltaMovement(x, movement.y, z);
    }

    private static void flingNearby(ServerPlayer player) {
        forNearbyEntities(player, 32.0, entity -> entity.setDeltaMovement(entity.getDeltaMovement().add(
                ThreadLocalRandom.current().nextDouble(-0.65, 0.65),
                ThreadLocalRandom.current().nextDouble(0.8, 1.55),
                ThreadLocalRandom.current().nextDouble(-0.65, 0.65)
        )));
    }

    private static void teleportNearbyEntities(ServerPlayer player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        forNearbyEntities(player, 24.0, entity -> entity.teleportTo(
                player.getX() + random.nextDouble(-3.5, 3.5),
                player.getY() + random.nextDouble(0.0, 2.0),
                player.getZ() + random.nextDouble(-3.5, 3.5)
        ));
    }

    private static void igniteNearby(ServerPlayer player) {
        forNearbyEntities(player, 18.0, entity -> entity.igniteForSeconds(5.0F));
    }

    private static void effectNearbyLiving(ServerPlayer player, Holder<MobEffect> effect, boolean hostilesOnly) {
        forNearbyEntities(player, 42.0, entity -> {
            if (entity instanceof LivingEntity living && (!hostilesOnly || living instanceof Enemy)) {
                living.addEffect(new MobEffectInstance(effect, SHORT_EFFECT_TICKS, 0, false, false, true));
            }
        });
    }

    private static void forNearbyEntities(ServerPlayer player, double radius, Consumer<Entity> action) {
        AABB box = player.getBoundingBox().inflate(radius);
        for (Entity entity : player.serverLevel().getEntities(player, box,
                entity -> entity.isAlive() && !isPlayerProtectedEntity(entity))) {
            action.accept(entity);
        }
    }

    private static boolean isPlayerProtectedEntity(Entity entity) {
        if (entity instanceof ServerPlayer) return true;
        for (Entity passenger : entity.getPassengers()) {
            if (isPlayerProtectedEntity(passenger)) return true;
        }
        return false;
    }

    private static void rainItems(ServerPlayer player, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < count; i++) {
            Item item = RAIN_ITEMS.get(random.nextInt(RAIN_ITEMS.size()));
            ItemEntity entity = new ItemEntity(level,
                    player.getX() + random.nextDouble(-8.0, 8.0),
                    player.getY() + random.nextDouble(10.0, 17.0),
                    player.getZ() + random.nextDouble(-8.0, 8.0),
                    new ItemStack(item));
            entity.setDeltaMovement(random.nextDouble(-0.08, 0.08), -0.25, random.nextDouble(-0.08, 0.08));
            level.addFreshEntity(entity);
        }
    }

    private static void rainExperience(ServerPlayer player, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < count; i++) {
            ExperienceOrb.award(level, new Vec3(
                    player.getX() + random.nextDouble(-7.0, 7.0),
                    player.getY() + random.nextDouble(6.0, 12.0),
                    player.getZ() + random.nextDouble(-7.0, 7.0)
            ), random.nextInt(2, 9));
        }
    }

    private static void rainMobs(ServerPlayer player, EntityType<? extends Mob> type, int count, int height) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < count; i++) {
            Mob mob = type.create(level);
            if (mob == null) continue;
            mob.moveTo(player.getX() + random.nextDouble(-8.0, 8.0),
                    player.getY() + height + random.nextDouble(0.0, 6.0),
                    player.getZ() + random.nextDouble(-8.0, 8.0),
                    random.nextFloat() * 360.0F, 0.0F);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
        }
    }

    private static void rainArrows(ServerPlayer player, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < count; i++) {
            Arrow arrow = EntityType.ARROW.create(level);
            if (arrow == null) continue;
            arrow.moveTo(player.getX() + random.nextDouble(-8.0, 8.0),
                    player.getY() + random.nextDouble(13.0, 20.0),
                    player.getZ() + random.nextDouble(-8.0, 8.0));
            arrow.setOwner(player);
            arrow.setDeltaMovement(random.nextDouble(-0.08, 0.08), -1.65, random.nextDouble(-0.08, 0.08));
            level.addFreshEntity(arrow);
        }
    }

    private static void spawnRainbowSheep(ServerPlayer player, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < count; i++) {
            Sheep sheep = EntityType.SHEEP.create(level);
            if (sheep == null) continue;
            sheep.setColor(DyeColor.byId(random.nextInt(16)));
            sheep.moveTo(player.getX() + random.nextDouble(-8.0, 8.0), player.getY() + 1.0,
                    player.getZ() + random.nextDouble(-8.0, 8.0), random.nextFloat() * 360.0F, 0.0F);
            sheep.setPersistenceRequired();
            level.addFreshEntity(sheep);
        }
    }

    private static void spawnSlimes(ServerPlayer player, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < count; i++) {
            Slime slime = EntityType.SLIME.create(level);
            if (slime == null) continue;
            slime.setSize(random.nextInt(1, 5), true);
            slime.moveTo(player.getX() + random.nextDouble(-9.0, 9.0), player.getY() + 1.0,
                    player.getZ() + random.nextDouble(-9.0, 9.0), random.nextFloat() * 360.0F, 0.0F);
            slime.setTarget(player);
            slime.setPersistenceRequired();
            level.addFreshEntity(slime);
        }
    }

    private static void spawnTargetingMobs(ServerPlayer player, EntityType<? extends Mob> type, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < count; i++) {
            Mob mob = type.create(level);
            if (mob == null) continue;
            mob.moveTo(player.getX() + random.nextDouble(-9.0, 9.0), player.getY() + random.nextDouble(1.0, 5.0),
                    player.getZ() + random.nextDouble(-9.0, 9.0), random.nextFloat() * 360.0F, 0.0F);
            mob.setTarget(player);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
        }
    }

    private static void hauntedSounds(ServerPlayer player, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < count; i++) {
            BlockPos soundPos = player.blockPosition().offset(random.nextInt(-10, 11), random.nextInt(-2, 4), random.nextInt(-10, 11));
            SoundEvent sound = random.nextBoolean() ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE;
            level.playSound(null, soundPos, sound, SoundSource.BLOCKS, 1.1F, random.nextFloat(0.65F, 1.2F));
        }
    }

    private static void forceMount(ServerPlayer player) {
        if (player.isPassenger()) return;
        ServerLevel level = player.serverLevel();
        Horse horse = EntityType.HORSE.create(level);
        if (horse == null) return;
        horse.moveTo(player.getX() + 1.0, player.getY(), player.getZ() + 1.0, player.getYRot(), 0.0F);
        horse.setPersistenceRequired();
        level.addFreshEntity(horse);
        player.startRiding(horse, true);
    }

    private void placeCobwebTrap(ServerPlayer player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 3; i++) {
            BlockPos pos = player.blockPosition().offset(random.nextInt(-2, 3), random.nextInt(0, 2), random.nextInt(-2, 3));
            if (level.getBlockState(pos).isAir()) {
                replaceTemporarily(level, pos, Blocks.COBWEB.defaultBlockState());
            }
        }
    }

    private void freezeNearbyWater(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition().below();
        for (BlockPos mutable : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 1, 3))) {
            BlockPos pos = mutable.immutable();
            if (level.getBlockState(pos).is(Blocks.WATER)) {
                replaceTemporarily(level, pos, Blocks.FROSTED_ICE.defaultBlockState());
            }
        }
    }

    private void paintRainbowPath(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition().below();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0.0F) return;
        Block block = RAINBOW_BLOCKS.get(ThreadLocalRandom.current().nextInt(RAINBOW_BLOCKS.size()));
        replaceTemporarily(level, pos, block.defaultBlockState());
    }

    private void placeFireTrail(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
            replaceTemporarily(level, pos, Blocks.FIRE.defaultBlockState());
        }
    }

    private void replaceTemporarily(ServerLevel level, BlockPos pos, BlockState replacement) {
        if (temporaryBlocks.size() >= MAX_TEMPORARY_BLOCKS || !level.isInWorldBounds(pos)) return;
        WorldPos key = new WorldPos(level.dimension(), pos.immutable());
        if (temporaryBlocks.containsKey(key)) return;
        BlockState original = level.getBlockState(pos);
        if (original.hasBlockEntity()) return;
        if (level.setBlockAndUpdate(pos, replacement)) {
            temporaryBlocks.put(key, new ChangedBlock(original, replacement));
        }
    }

    private void restoreTemporaryBlocks(MinecraftServer server) {
        for (Map.Entry<WorldPos, ChangedBlock> entry : new ArrayList<>(temporaryBlocks.entrySet())) {
            ServerLevel level = server.getLevel(entry.getKey().dimension());
            if (level == null) continue;
            BlockPos pos = entry.getKey().pos();
            ChangedBlock changed = entry.getValue();
            if (level.getBlockState(pos).equals(changed.placed())) {
                level.setBlockAndUpdate(pos, changed.original());
            }
        }
        temporaryBlocks.clear();
    }

    private void setWeather(MinecraftServer server, boolean raining, boolean thundering) {
        for (ServerLevel level : server.getAllLevels()) {
            if (scope.matches(level)) level.setWeatherParameters(0, 20 * 60 * 10, raining, thundering);
        }
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
    }

    private static void randomTeleport(ServerPlayer player, int radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        player.randomTeleport(player.getX() + random.nextInt(-radius, radius + 1),
                player.getY() + random.nextInt(-4, 7),
                player.getZ() + random.nextInt(-radius, radius + 1), true);
    }

    private static void shuffleHotbar(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<ItemStack> stacks = new ArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) stacks.add(inventory.getItem(slot).copy());
        Collections.shuffle(stacks);
        for (int slot = 0; slot < 9; slot++) inventory.setItem(slot, stacks.get(slot));
        inventory.setChanged();
    }

    private static void giveStarterSupply(ServerPlayer player) {
        giveOrDrop(player, new ItemStack(Items.BREAD, 8));
        giveOrDrop(player, new ItemStack(Items.TORCH, 16));
        giveOrDrop(player, new ItemStack(Items.COOKED_BEEF, 4));
        giveOrDrop(player, new ItemStack(Items.ARROW, 12));
    }

    private static void repairGear(ServerPlayer player, boolean strong) {
        int divisor = strong ? 3 : 20;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isDamageableItem() || stack.getDamageValue() <= 0) continue;
            int amount = Math.max(1, stack.getMaxDamage() / divisor);
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - amount));
        }
        player.getInventory().setChanged();
    }

    private static void damageGear(ServerPlayer player, boolean strong) {
        int divisor = strong ? 8 : 35;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isDamageableItem()) continue;
            int amount = Math.max(1, stack.getMaxDamage() / divisor);
            stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, stack.getDamageValue() + amount));
        }
        player.getInventory().setChanged();
    }

    private static void giveRandomOre(ServerPlayer player) {
        Item ore = ORE_REWARDS.get(ThreadLocalRandom.current().nextInt(ORE_REWARDS.size()));
        giveOrDrop(player, new ItemStack(ore, ThreadLocalRandom.current().nextInt(1, 5)));
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack.copy())) player.drop(stack.copy(), false);
    }

    private static void launchSkyward(ServerPlayer player) {
        player.setDeltaMovement(player.getDeltaMovement().add(
                ThreadLocalRandom.current().nextDouble(-0.25, 0.25),
                ThreadLocalRandom.current().nextDouble(1.6, 2.35),
                ThreadLocalRandom.current().nextDouble(-0.25, 0.25)
        ));
        effect(player, MobEffects.SLOW_FALLING, 220, 0);
    }

    private record WorldPos(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private record ChangedBlock(BlockState original, BlockState placed) {
    }
}
