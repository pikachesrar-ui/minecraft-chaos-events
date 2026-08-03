package com.kiras.chaosevents.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Independently implemented events based only on the user-provided mechanic descriptions.
 * No code, assets, data files, or implementation details from another mod are included.
 */
public enum InternetChaosEvent implements ChaosEvent {
    ACID_RAIN("acid_rain", "Кислотный дождь", "Дождь обжигает игроков под открытым небом; иногда начинается усиленная гроза", true, 35, 70),
    ADRENALINE_RUSH("adrenaline_rush", "Выброс адреналина", "Все получают скорость, силу и спешку", false, 25, 45),
    ADVANCE_TIME("advance_time", "Скачок времени", "Время мира быстро перескакивает вперёд", false, 10, 20),
    GAMEMODE_TWO("gamemode_two", "Режим приключений", "Все временно переводятся в режим приключений", true, 25, 50),
    ANGRY_BEES("angry_bees", "Злые пчёлы", "Возле игроков появляются разъярённые пчёлы", true, 20, 40),
    ANIMAL_PARTY("animal_party", "Звериная вечеринка", "Вокруг игроков появляются случайные животные", false, 20, 40),
    ANVIL("anvil", "Наковальня", "Наковальня выдаётся, ставится рядом или падает сверху", true, 8, 15),
    RAINING_ANVILS("raining_anvils", "Дождь из наковален", "Наковальни падают с неба возле игроков", true, 20, 40),
    ARROWSTORM("arrowstorm", "Ливень стрел", "Стрелы падают с неба", true, 20, 40),
    BEES("bees", "Пчёлы", "Возле игроков появляются обычные пчёлы", false, 20, 40),
    BOIL_WATER_NOTICE("boil_water_notice", "Кипячение воды", "Вода отравляет вошедших в неё игроков", true, 30, 60),
    BOUNCY_MOBS("bouncy_mobs", "Прыгучие мобы", "Все существа, кроме игроков, получают усиленный прыжок", false, 25, 50),
    CLEAR_SKIES("clear_skies", "Ясное небо", "Погода немедленно очищается", false, 8, 15),
    COBWEBS("cobwebs", "Паутина", "Вокруг игроков появляется паутина", true, 15, 30),
    CREEPER_PARTY("creeper_party", "Вечеринка криперов", "Возле каждого игрока появляются четыре крипера", true, 20, 40),
    CURSED("cursed", "Проклятие", "Все игроки получают невезение", true, 25, 50),
    DAMAGE_RULES("damage_rules", "Переписанные правила урона", "Случайно меняются правила урона от огня, падения, утопления и замерзания", true, 30, 60),
    DAY_WEATHER_RULES("day_weather_rules", "Сломанные циклы мира", "Случайно останавливаются циклы дня и погоды", true, 30, 60),
    DESERT_GANG("desert_gang", "Пустынная банда", "Возле игроков появляется группа пустынных существ", true, 20, 40),
    DIG_HOLE("dig_hole", "Яма под ногами", "Под игроками внезапно исчезает несколько блоков", true, 8, 15),
    DIOS_BEST_FRIEND("dios_best_friend", "Лучший друг Дио", "Каждый получает тотем бессмертия", false, 8, 15),
    INVISIBLE_WORLD("invisible_world", "Куда всё пропало?", "Живые существа становятся невидимыми", true, 25, 50),
    EXTRA_HEALTH("extra_health", "Запасные сердца", "Все временно получают дополнительные сердца", false, 30, 60),
    FAMINE_TWO("famine_two", "Голод", "Все игроки получают сильный голод", true, 25, 50),
    FIRE_SKIN("fire_skin", "Огненная кожа", "Все получают огнестойкость", false, 30, 60),
    FIRE_STORM_TWO("fire_storm_two", "Огненный дождь", "С неба падают огненные заряды", true, 20, 40),
    FISH("fish", "Рыбный день", "Каждый получает случайную рыбу", false, 8, 15),
    RAINING_FOOD("raining_food", "Дождь из еды", "Еда падает с неба", false, 20, 40),
    FOOD_RECALL("food_recall", "Отзыв продуктов", "Съеденная пища вызывает отрицательные эффекты", true, 30, 60),
    GLOWING("glowing", "Светящиеся силуэты", "Живые существа начинают светиться сквозь стены", false, 25, 50),
    GOATS("goats", "Нашествие коз", "Возле игроков появляются козы", false, 20, 40),
    HUNGRY_HUNGRY_SLIME("hungry_hungry_slime", "Голодный-голодный слизень", "Неуязвимый слизень растёт до финального взрыва", true, 25, 45),
    I_CANT_SEE("i_cant_see", "Я ничего не вижу", "Ночное зрение спорит со слепотой", true, 25, 50),
    IGNITE_PLAYER("ignite_player", "Воспламенение", "Игроки периодически загораются", true, 20, 40),
    INFESTED_MOBS("infested_mobs", "Заражённые мобы", "Неигровые существа получают эффект заражения", true, 25, 50),
    INTEREST_INCOME("interest_income", "Проценты по вкладу", "Все получают золотой самородок", false, 8, 15),
    INVENTORY_RULES("inventory_rules", "Правила инвентаря", "Временно меняется правило сохранения инвентаря", true, 30, 60),
    INVULNERABLE("invulnerable", "Неуязвимость", "Все игроки временно почти неуязвимы", false, 20, 40),
    JUNGLE_GANG("jungle_gang", "Банда джунглей", "Возле игроков появляется группа существ из джунглей", true, 20, 40),
    KILLER_BUNNY("killer_bunny", "Кролик-убийца", "Возле игрока появляется крайне недружелюбный кролик", true, 20, 40),
    KNOCKBACK_SWORD("knockback_sword", "Меч отбрасывания", "Каждый получает хрупкий деревянный меч с огромным отбрасыванием", false, 8, 15),
    LAUNCH_PLAYER_UP("launch_player_up", "Запуск вверх", "Игроков резко подбрасывает, но падение смягчается", true, 12, 25),
    LOW_GRAVITY("low_gravity", "Низкая гравитация", "Игроки высоко прыгают и медленно падают", false, 30, 60),
    NETHER_GANG("nether_gang", "Адская банда", "Возле игроков появляется группа существ Незера", true, 20, 40),
    I_CAN_SEE("i_can_see", "Я вижу!", "Все получают ночное зрение", false, 30, 60),
    ABSOLUTELY_NOTHING("absolutely_nothing", "Абсолютно ничего", "Ничего не происходит", false, 10, 20),
    ONE_WITH_THE_SEA("one_with_the_sea", "Единение с морем", "Игроки получают силу проводника, дыхание и грацию дельфина", false, 30, 60),
    OOZING_MOBS("oozing_mobs", "Сочащиеся мобы", "Неигровые существа получают эффект сочения", true, 25, 50),
    ORBITAL_BOMBARDMENT("orbital_bombardment", "Орбитальная бомбардировка", "С неба падают активированные вагонетки с динамитом", true, 20, 40),
    SPAWN_PANDA("spawn_panda", "Панда", "Возле каждого игрока появляется панда", false, 15, 30),
    PLAGUE("plague", "Чума", "Все получают замедление, усталость, слабость и иссушение", true, 20, 40),
    POISON_ATTACK("poison_attack", "Ядовитая атака", "Игроки получают сильное отравление", true, 20, 40),
    POWDER_SNOW("powder_snow", "Снег!", "Вокруг игроков появляется рыхлый снег", true, 15, 30),
    RED_LIGHT("red_light", "Красный свет", "Игроки замирают, пока не загорится зелёный", true, 20, 35),
    RISK_OF_RAIN("risk_of_rain", "Риск дождя", "Начинается дождь или гроза", false, 30, 60),
    SANDSTORM("sandstorm", "Песчаная буря", "С неба падает песок", true, 20, 40),
    SATURATION_TWO("saturation_two", "Насыщение", "Голод и насыщение восстанавливаются", false, 25, 50),
    SCULK_PRANK("sculk_prank", "Скалк-шутка", "Тьма накрывает игроков, а рядом иногда появляется скалк", true, 20, 40),
    SHIPPING_REQUEST("shipping_request", "Запрос на доставку", "С неба прибывает вагонетка с припасами", false, 12, 25),
    SLEEP_RULES("sleep_rules", "Правила сна", "Временно меняется процент игроков, необходимый для сна", true, 30, 60),
    SMITE_RANDOM_PLAYER("smite_random_player", "Кара небес", "Случайного игрока поражает молния", true, 8, 15),
    SMOKE_CLOUD("smoke_cloud", "Дымовая завеса", "Игроков окружает плотный дым", true, 20, 40),
    SNIFFER("sniffer", "Нюхач", "Возле игроков появляется нюхач", false, 15, 30),
    SNOW_GANG("snow_gang", "Снежная банда", "Возле игроков появляется группа снежных существ", true, 20, 40),
    SPAWN_GOLEM("spawn_golem", "Голем", "Возле игроков появляется железный или снежный голем", false, 15, 30),
    SPAWN_RANDOM_PET("spawn_random_pet", "Случайный питомец", "Каждый получает случайного приручённого питомца", false, 15, 30),
    SPEEDY_MOBS("speedy_mobs", "Скоростные мобы", "Все неигровые существа получают скорость", true, 25, 50),
    SUGAR_RUSH("sugar_rush", "Сахарный прилив", "Все игроки получают скорость", false, 25, 50),
    TELEPORT_PLAYERS("teleport_players", "Случайный сдвиг", "Игроков переносит на случайное расстояние", true, 12, 25),
    THE_CHILD("the_child", "Ребёнок", "Возле игроков появляется маленький зомби", true, 20, 40),
    THE_END("the_end", "Конец?", "Случайный игрок ненадолго отправляется в Край, затем возвращается", true, 15, 25),
    U_TURN("u_turn", "Разворот", "Все игроки мгновенно поворачиваются на 180 градусов", false, 8, 15),
    WATCH_OUT("watch_out", "Берегись!", "Через мгновение сверху падает опасный сюрприз", true, 10, 20),
    STRINGY_MOBS("stringy_mobs", "Липкие мобы", "Неигровые существа получают эффект плетения", true, 25, 50),
    WINDSTORM("windstorm", "Ветряная буря", "Появляются бризы, а с неба летят заряды ветра", true, 20, 40),
    LEADER_OF_THE_PACK("leader_of_the_pack", "Вожак стаи", "Возле игроков появляются волки, а игроки получают кости", false, 20, 40),
    WUNGUS("wungus", "Вунгус", "Все игроки получают регенерацию", false, 25, 50),
    RAINING_XP("raining_xp", "Дождь из опыта", "С неба падает большое количество опыта", false, 20, 40),
    YOU_FEEL_SICK("you_feel_sick", "Тебе нехорошо", "Все игроки получают тошноту", true, 25, 50);

    private static final int SHORT_EFFECT = 80;
    private static final int LONG_EFFECT = 140;
    private static final List<Item> FISH_ITEMS = List.of(Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH);
    private static final List<Item> FOOD_ITEMS = List.of(
            Items.BREAD, Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.CARROT,
            Items.BAKED_POTATO, Items.APPLE, Items.COOKIE, Items.PUMPKIN_PIE
    );
    private static final List<EntityType<? extends Mob>> ANIMALS = List.of(
            EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN, EntityType.RABBIT
    );
    private static final List<EntityType<? extends Mob>> DESERT_MOBS = List.of(
            EntityType.HUSK, EntityType.SPIDER, EntityType.RABBIT, EntityType.CAMEL
    );
    private static final List<EntityType<? extends Mob>> JUNGLE_MOBS = List.of(
            EntityType.OCELOT, EntityType.PARROT, EntityType.PANDA, EntityType.SPIDER
    );
    private static final List<EntityType<? extends Mob>> NETHER_MOBS = List.of(
            EntityType.ZOMBIFIED_PIGLIN, EntityType.MAGMA_CUBE, EntityType.BLAZE, EntityType.HOGLIN
    );
    private static final List<EntityType<? extends Mob>> SNOW_MOBS = List.of(
            EntityType.STRAY, EntityType.POLAR_BEAR, EntityType.SNOW_GOLEM, EntityType.RABBIT
    );

    private final String id;
    private final String displayName;
    private final String description;
    private final boolean harsh;
    private final int minDurationSeconds;
    private final int maxDurationSeconds;

    private boolean running;
    private boolean acidStorm;
    private boolean greenLight;
    private final Map<UUID, GameType> previousGameModes = new HashMap<>();
    private final Map<UUID, StoredPosition> storedPositions = new HashMap<>();
    private final Set<UUID> hungrySlimes = new HashSet<>();
    private Boolean oldFallDamage;
    private Boolean oldFireDamage;
    private Boolean oldDrowningDamage;
    private Boolean oldFreezeDamage;
    private Boolean oldDaylightCycle;
    private Boolean oldWeatherCycle;
    private Boolean oldKeepInventory;
    private Integer oldSleepingPercentage;

    InternetChaosEvent(String id, String displayName, String description, boolean harsh,
                       int minDurationSeconds, int maxDurationSeconds) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.harsh = harsh;
        this.minDurationSeconds = minDurationSeconds;
        this.maxDurationSeconds = maxDurationSeconds;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return displayName; }
    @Override public String description() { return description; }
    @Override public boolean harsh() { return harsh; }
    @Override public int minimumDurationSeconds() { return minDurationSeconds; }
    @Override public int maximumDurationSeconds() { return maxDurationSeconds; }

    @Override
    public boolean isEligible(MinecraftServer server) {
        if (server.getPlayerList().getPlayers().isEmpty()) return false;
        return switch (this) {
            case ACID_RAIN, CLEAR_SKIES, RISK_OF_RAIN -> server.overworld() != null;
            case THE_END -> server.getLevel(Level.END) != null;
            default -> true;
        };
    }

    @Override
    public void start(MinecraftServer server) {
        running = true;
        greenLight = false;
        previousGameModes.clear();
        storedPositions.clear();
        hungrySlimes.clear();

        switch (this) {
            case ACID_RAIN -> {
                acidStorm = ThreadLocalRandom.current().nextInt(10) == 0;
                server.overworld().setWeatherParameters(0, 20 * 60 * 10, true, acidStorm);
                if (acidStorm) broadcast(server, "Кислотный шторм! Урон под открытым небом усилен.");
            }
            case ADRENALINE_RUSH -> forPlayers(server, player -> {
                effect(player, MobEffects.MOVEMENT_SPEED, LONG_EFFECT, 2);
                effect(player, MobEffects.DAMAGE_BOOST, LONG_EFFECT, 1);
                effect(player, MobEffects.DIG_SPEED, LONG_EFFECT, 2);
            });
            case ADVANCE_TIME -> advanceTime(server);
            case GAMEMODE_TWO -> forPlayers(server, player -> {
                previousGameModes.put(player.getUUID(), player.gameMode.getGameModeForPlayer());
                player.setGameMode(GameType.ADVENTURE);
            });
            case ANGRY_BEES -> forPlayers(server, player -> spawnMobs(player, EntityType.BEE, 4, 5, true));
            case ANIMAL_PARTY -> forPlayers(server, player -> spawnRandomGroup(player, ANIMALS, 5));
            case ANVIL -> forPlayers(server, InternetChaosEvent::anvilChoice);
            case BEES -> forPlayers(server, player -> spawnMobs(player, EntityType.BEE, 4, 5, false));
            case CLEAR_SKIES -> clearWeather(server);
            case COBWEBS -> forPlayers(server, InternetChaosEvent::placeCobwebs);
            case CREEPER_PARTY -> forPlayers(server, InternetChaosEvent::spawnCreeperParty);
            case DAMAGE_RULES -> changeDamageRules(server);
            case DAY_WEATHER_RULES -> changeDayWeatherRules(server);
            case DESERT_GANG -> forPlayers(server, player -> spawnRandomGroup(player, DESERT_MOBS, 4));
            case DIG_HOLE -> forPlayers(server, InternetChaosEvent::digHole);
            case DIOS_BEST_FRIEND -> giveAll(server, new ItemStack(Items.TOTEM_OF_UNDYING));
            case EXTRA_HEALTH -> forPlayers(server, player -> {
                int amplifier = ThreadLocalRandom.current().nextInt(10) == 0 ? 9 : 4;
                effect(player, MobEffects.HEALTH_BOOST, 20 * maxDurationSeconds + 40, amplifier);
                player.heal(player.getMaxHealth());
            });
            case FISH -> forPlayers(server, player -> giveOrDrop(player,
                    new ItemStack(FISH_ITEMS.get(ThreadLocalRandom.current().nextInt(FISH_ITEMS.size())))));
            case GOATS -> forPlayers(server, player -> spawnMobs(player, EntityType.GOAT, 4, 5, false));
            case HUNGRY_HUNGRY_SLIME -> forPlayers(server, this::spawnHungrySlime);
            case INTEREST_INCOME -> giveAll(server, new ItemStack(Items.GOLD_NUGGET));
            case INVENTORY_RULES -> changeInventoryRule(server);
            case JUNGLE_GANG -> forPlayers(server, player -> spawnRandomGroup(player, JUNGLE_MOBS, 4));
            case KILLER_BUNNY -> forPlayers(server, player -> runAtPlayer(player,
                    "summon minecraft:rabbit ~2 ~ ~ {RabbitType:99}"));
            case KNOCKBACK_SWORD -> forPlayers(server, player -> runAsPlayer(player,
                    "give @s minecraft:wooden_sword[minecraft:enchantments={levels:{\"minecraft:knockback\":10}}] 1"));
            case LAUNCH_PLAYER_UP -> forPlayers(server, player -> {
                effect(player, MobEffects.SLOW_FALLING, 20 * 18, 0);
                effect(player, MobEffects.DAMAGE_RESISTANCE, 20 * 18, 4);
                player.setDeltaMovement(player.getDeltaMovement().add(0.0, 2.6, 0.0));
            });
            case NETHER_GANG -> forPlayers(server, player -> spawnRandomGroup(player, NETHER_MOBS, 4));
            case ORBITAL_BOMBARDMENT -> forPlayers(server, player -> spawnFallingTntMinecart(player, 2));
            case SPAWN_PANDA -> forPlayers(server, player -> spawnMobs(player, EntityType.PANDA, 1, 3, false));
            case POWDER_SNOW -> forPlayers(server, InternetChaosEvent::placePowderSnow);
            case RISK_OF_RAIN -> {
                boolean thunder = ThreadLocalRandom.current().nextBoolean();
                server.overworld().setWeatherParameters(0, 20 * 60 * 10, true, thunder);
            }
            case SHIPPING_REQUEST -> forPlayers(server, InternetChaosEvent::shippingRequest);
            case SLEEP_RULES -> changeSleepRule(server);
            case SMITE_RANDOM_PLAYER -> smiteRandomPlayer(server);
            case SNIFFER -> forPlayers(server, player -> spawnMobs(player, EntityType.SNIFFER, 1, 3, false));
            case SNOW_GANG -> forPlayers(server, player -> spawnRandomGroup(player, SNOW_MOBS, 4));
            case SPAWN_GOLEM -> forPlayers(server, InternetChaosEvent::spawnRandomGolem);
            case SPAWN_RANDOM_PET -> forPlayers(server, InternetChaosEvent::spawnRandomPet);
            case TELEPORT_PLAYERS -> forPlayers(server, InternetChaosEvent::teleportPlayer);
            case THE_CHILD -> forPlayers(server, player -> runAtPlayer(player,
                    "summon minecraft:zombie ~2 ~ ~ {IsBaby:1b}"));
            case THE_END -> sendRandomPlayerToEnd(server);
            case U_TURN -> forPlayers(server, player -> player.teleportTo(
                    player.serverLevel(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot() + 180.0F, player.getXRot()));
            case WATCH_OUT -> broadcast(server, "Берегитесь. Сюрприз уже летит сверху.");
            case LEADER_OF_THE_PACK -> forPlayers(server, player -> {
                spawnMobs(player, EntityType.WOLF, 4, 5, false);
                giveOrDrop(player, new ItemStack(Items.BONE, 12));
            });
            default -> { }
        }
    }

    @Override
    public void tick(MinecraftServer server, int elapsedTicks, int remainingTicks) {
        if (!running) return;

        if (elapsedTicks % 10 == 0) {
            switch (this) {
                case BOIL_WATER_NOTICE -> forPlayers(server, player -> {
                    if (player.isInWaterOrBubble()) effect(player, MobEffects.POISON, 35, 1);
                });
                case RED_LIGHT -> tickRedLight(server, elapsedTicks, remainingTicks);
                default -> { }
            }
        }

        if (elapsedTicks % 40 == 0) {
            switch (this) {
                case ACID_RAIN -> forPlayers(server, player -> damageFromAcidRain(player, acidStorm));
                case ADRENALINE_RUSH -> forPlayers(server, player -> {
                    effect(player, MobEffects.MOVEMENT_SPEED, SHORT_EFFECT, 2);
                    effect(player, MobEffects.DAMAGE_BOOST, SHORT_EFFECT, 1);
                    effect(player, MobEffects.DIG_SPEED, SHORT_EFFECT, 2);
                });
                case BOUNCY_MOBS -> forNonPlayers(server, entity -> effect(entity, MobEffects.JUMP, SHORT_EFFECT, 4));
                case CURSED -> forPlayers(server, player -> effect(player, MobEffects.UNLUCK, SHORT_EFFECT, 2));
                case INVISIBLE_WORLD -> forLiving(server, entity -> effect(entity, MobEffects.INVISIBILITY, SHORT_EFFECT, 0));
                case FAMINE_TWO -> forPlayers(server, player -> effect(player, MobEffects.HUNGER, SHORT_EFFECT, 3));
                case FIRE_SKIN -> forPlayers(server, player -> effect(player, MobEffects.FIRE_RESISTANCE, SHORT_EFFECT, 0));
                case GLOWING -> forLiving(server, entity -> effect(entity, MobEffects.GLOWING, SHORT_EFFECT, 0));
                case I_CANT_SEE -> forPlayers(server, player -> {
                    effect(player, MobEffects.NIGHT_VISION, SHORT_EFFECT, 0);
                    effect(player, MobEffects.BLINDNESS, 55, 0);
                });
                case INFESTED_MOBS -> forNonPlayers(server, entity -> effect(entity, MobEffects.INFESTED, SHORT_EFFECT, 0));
                case INVULNERABLE -> forPlayers(server, player -> effect(player, MobEffects.DAMAGE_RESISTANCE, SHORT_EFFECT, 255));
                case LOW_GRAVITY -> forPlayers(server, player -> {
                    effect(player, MobEffects.JUMP, SHORT_EFFECT, 3);
                    effect(player, MobEffects.SLOW_FALLING, SHORT_EFFECT, 0);
                });
                case I_CAN_SEE -> forPlayers(server, player -> effect(player, MobEffects.NIGHT_VISION, SHORT_EFFECT, 0));
                case ONE_WITH_THE_SEA -> forPlayers(server, player -> {
                    effect(player, MobEffects.CONDUIT_POWER, SHORT_EFFECT, 0);
                    effect(player, MobEffects.WATER_BREATHING, SHORT_EFFECT, 0);
                    effect(player, MobEffects.DOLPHINS_GRACE, SHORT_EFFECT, 0);
                });
                case OOZING_MOBS -> forNonPlayers(server, entity -> effect(entity, MobEffects.OOZING, SHORT_EFFECT, 0));
                case PLAGUE -> forPlayers(server, player -> {
                    effect(player, MobEffects.MOVEMENT_SLOWDOWN, SHORT_EFFECT, 2);
                    effect(player, MobEffects.DIG_SLOWDOWN, SHORT_EFFECT, 2);
                    effect(player, MobEffects.WEAKNESS, SHORT_EFFECT, 2);
                    effect(player, MobEffects.WITHER, 55, 0);
                });
                case POISON_ATTACK -> forPlayers(server, player -> effect(player, MobEffects.POISON, 55, 3));
                case SATURATION_TWO -> forPlayers(server, player -> {
                    player.getFoodData().setFoodLevel(20);
                    player.getFoodData().setSaturation(10.0F);
                });
                case SCULK_PRANK -> forPlayers(server, player -> {
                    effect(player, MobEffects.DARKNESS, SHORT_EFFECT, 0);
                    if (ThreadLocalRandom.current().nextInt(5) == 0) placeSculk(player);
                });
                case SPEEDY_MOBS -> forNonPlayers(server, entity -> effect(entity, MobEffects.MOVEMENT_SPEED, SHORT_EFFECT, 3));
                case SUGAR_RUSH -> forPlayers(server, player -> effect(player, MobEffects.MOVEMENT_SPEED, SHORT_EFFECT, 2));
                case STRINGY_MOBS -> forNonPlayers(server, entity -> effect(entity, MobEffects.WEAVING, SHORT_EFFECT, 0));
                case WUNGUS -> forPlayers(server, player -> effect(player, MobEffects.REGENERATION, SHORT_EFFECT, 1));
                case YOU_FEEL_SICK -> forPlayers(server, player -> effect(player, MobEffects.CONFUSION, SHORT_EFFECT, 1));
                default -> { }
            }
        }

        if (elapsedTicks > 0 && elapsedTicks % 80 == 0) {
            switch (this) {
                case ADVANCE_TIME -> advanceTime(server);
                case RAINING_ANVILS -> forPlayers(server, player -> dropAnvils(player, 2));
                case ARROWSTORM -> forPlayers(server, player -> arrowStorm(player, 5));
                case FIRE_STORM_TWO -> forPlayers(server, player -> fireStorm(player, 3));
                case RAINING_FOOD -> forPlayers(server, player -> rainFood(player, 5));
                case IGNITE_PLAYER -> forPlayers(server, player -> player.igniteForSeconds(4.0F));
                case ORBITAL_BOMBARDMENT -> forPlayers(server, player -> spawnFallingTntMinecart(player, 1));
                case SANDSTORM -> forPlayers(server, player -> sandStorm(player, 4));
                case SMOKE_CLOUD -> forPlayers(server, InternetChaosEvent::smokeCloud);
                case WINDSTORM -> forPlayers(server, InternetChaosEvent::windStorm);
                case RAINING_XP -> forPlayers(server, player -> rainExperience(player, 35));
                default -> { }
            }
        }

        if (elapsedTicks > 0 && elapsedTicks % 100 == 0) {
            switch (this) {
                case ANGRY_BEES -> forPlayers(server, player -> spawnMobs(player, EntityType.BEE, 2, 5, true));
                case ANIMAL_PARTY -> forPlayers(server, player -> spawnRandomGroup(player, ANIMALS, 2));
                case BEES -> forPlayers(server, player -> spawnMobs(player, EntityType.BEE, 2, 5, false));
                case CREEPER_PARTY -> forPlayers(server, player -> spawnMobs(player, EntityType.CREEPER, 2, 6, true));
                case DESERT_GANG -> forPlayers(server, player -> spawnRandomGroup(player, DESERT_MOBS, 2));
                case GOATS -> forPlayers(server, player -> spawnMobs(player, EntityType.GOAT, 2, 5, false));
                case HUNGRY_HUNGRY_SLIME -> growHungrySlimes(server);
                case JUNGLE_GANG -> forPlayers(server, player -> spawnRandomGroup(player, JUNGLE_MOBS, 2));
                case NETHER_GANG -> forPlayers(server, player -> spawnRandomGroup(player, NETHER_MOBS, 2));
                case SNOW_GANG -> forPlayers(server, player -> spawnRandomGroup(player, SNOW_MOBS, 2));
                case THE_CHILD -> forPlayers(server, player -> runAtPlayer(player,
                        "summon minecraft:zombie ~2 ~ ~ {IsBaby:1b}"));
                case WATCH_OUT -> {
                    if (elapsedTicks == 100) forPlayers(server, player -> dropAnvils(player, 3));
                }
                default -> { }
            }
        }
    }

    @Override
    public void stop(MinecraftServer server) {
        running = false;

        switch (this) {
            case ACID_RAIN, RISK_OF_RAIN -> clearWeather(server);
            case GAMEMODE_TWO -> restoreGameModes(server);
            case DAMAGE_RULES -> restoreDamageRules(server);
            case DAY_WEATHER_RULES -> restoreDayWeatherRules(server);
            case INVENTORY_RULES -> restoreInventoryRule(server);
            case SLEEP_RULES -> restoreSleepRule(server);
            case HUNGRY_HUNGRY_SLIME -> finishHungrySlimes(server);
            case THE_END -> restoreStoredPlayers(server);
            case RED_LIGHT -> forPlayers(server, player -> {
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.removeEffect(MobEffects.DIG_SLOWDOWN);
            });
            default -> { }
        }

        previousGameModes.clear();
        storedPositions.clear();
        hungrySlimes.clear();
    }

    public static void onFoodConsumed(ServerPlayer player) {
        if (!FOOD_RECALL.running) return;
        List<Holder<MobEffect>> effects = List.of(
                MobEffects.CONFUSION, MobEffects.HUNGER, MobEffects.WEAKNESS,
                MobEffects.MOVEMENT_SLOWDOWN, MobEffects.POISON, MobEffects.BLINDNESS
        );
        Holder<MobEffect> selected = effects.get(ThreadLocalRandom.current().nextInt(effects.size()));
        effect(player, selected, 20 * ThreadLocalRandom.current().nextInt(5, 13),
                selected == MobEffects.POISON ? 1 : 0);
        player.sendSystemMessage(Component.literal("[Отзыв продуктов] Эта еда оказалась испорченной."));
    }

    private static void forPlayers(MinecraftServer server, Consumer<ServerPlayer> action) {
        server.getPlayerList().getPlayers().forEach(action);
    }

    private static void forLiving(MinecraftServer server, Consumer<LivingEntity> action) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && living.isAlive()) action.accept(living);
            }
        }
    }

    private static void forNonPlayers(MinecraftServer server, Consumer<LivingEntity> action) {
        forLiving(server, living -> {
            if (!(living instanceof ServerPlayer)) action.accept(living);
        });
    }

    private static void effect(LivingEntity entity, Holder<MobEffect> effect, int duration, int amplifier) {
        entity.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
    }

    private static void giveAll(MinecraftServer server, ItemStack stack) {
        forPlayers(server, player -> giveOrDrop(player, stack.copy()));
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack.copy())) player.drop(stack.copy(), false);
    }

    private static void broadcast(MinecraftServer server, String text) {
        Component message = Component.literal("[Chaos Events] " + text);
        forPlayers(server, player -> player.sendSystemMessage(message));
    }

    private static void runAsPlayer(ServerPlayer player, String command) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command);
    }

    private static void runAtPlayer(ServerPlayer player, String command) {
        runAsPlayer(player, command);
    }

    private static void advanceTime(MinecraftServer server) {
        long jump = ThreadLocalRandom.current().nextLong(1000L, 12001L);
        for (ServerLevel level : server.getAllLevels()) level.setDayTime(level.getDayTime() + jump);
    }

    private static void clearWeather(MinecraftServer server) {
        server.overworld().setWeatherParameters(20 * 60 * 10, 0, false, false);
    }

    private static void damageFromAcidRain(ServerPlayer player, boolean storm) {
        if (!player.serverLevel().isRainingAt(player.blockPosition().above())) return;
        float damage = storm ? 4.0F : 2.0F;
        player.hurt(player.damageSources().magic(), damage);
        effect(player, MobEffects.POISON, 50, storm ? 1 : 0);
    }

    private static void spawnMobs(ServerPlayer player, EntityType<? extends Mob> type,
                                  int count, int radius, boolean targetPlayer) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < count; i++) {
            Mob mob = type.create(level);
            if (mob == null) continue;
            mob.moveTo(player.getX() + random.nextDouble(-radius, radius),
                    player.getY() + 0.5,
                    player.getZ() + random.nextDouble(-radius, radius),
                    random.nextFloat() * 360.0F, 0.0F);
            mob.setPersistenceRequired();
            if (targetPlayer) mob.setTarget(player);
            if (mob instanceof Bee bee && targetPlayer) {
                bee.setTarget(player);
                bee.setPersistentAngerTarget(player.getUUID());
            }
            level.addFreshEntity(mob);
        }
    }

    private static void spawnRandomGroup(ServerPlayer player, List<EntityType<? extends Mob>> types, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            spawnMobs(player, types.get(random.nextInt(types.size())), 1, 6, true);
        }
    }

    private static void anvilChoice(ServerPlayer player) {
        int choice = ThreadLocalRandom.current().nextInt(3);
        if (choice == 0) {
            giveOrDrop(player, new ItemStack(Items.ANVIL));
        } else if (choice == 1) {
            dropAnvils(player, 1);
        } else {
            BlockPos pos = player.blockPosition().relative(player.getDirection());
            if (player.serverLevel().getBlockState(pos).isAir()) {
                player.serverLevel().setBlockAndUpdate(pos, Blocks.ANVIL.defaultBlockState());
            } else {
                giveOrDrop(player, new ItemStack(Items.ANVIL));
            }
        }
    }

    private static void dropAnvils(ServerPlayer player, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(-4, 5);
            int z = random.nextInt(-4, 5);
            runAtPlayer(player, "summon minecraft:falling_block ~" + x + " ~14 ~" + z
                    + " {BlockState:{Name:\"minecraft:anvil\"},Time:1,DropItem:0b,HurtEntities:1b}");
        }
    }

    private static void arrowStorm(ServerPlayer player, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(-7, 8);
            int z = random.nextInt(-7, 8);
            runAtPlayer(player, "summon minecraft:arrow ~" + x + " ~16 ~" + z
                    + " {Motion:[0.0d,-1.5d,0.0d],pickup:0b}");
        }
    }

    private static void fireStorm(ServerPlayer player, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(-6, 7);
            int z = random.nextInt(-6, 7);
            runAtPlayer(player, "summon minecraft:small_fireball ~" + x + " ~14 ~" + z
                    + " {Motion:[0.0d,-1.0d,0.0d]}");
        }
    }

    private static void rainFood(ServerPlayer player, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            Item item = FOOD_ITEMS.get(random.nextInt(FOOD_ITEMS.size()));
            ItemStack stack = new ItemStack(item, random.nextInt(1, 4));
            Entity entity = new net.minecraft.world.entity.item.ItemEntity(
                    player.serverLevel(),
                    player.getX() + random.nextDouble(-7.0, 7.0),
                    player.getY() + random.nextDouble(9.0, 15.0),
                    player.getZ() + random.nextDouble(-7.0, 7.0),
                    stack
            );
            entity.setDeltaMovement(0.0, -0.2, 0.0);
            player.serverLevel().addFreshEntity(entity);
        }
    }

    private static void placeCobwebs(ServerPlayer player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 8; i++) {
            BlockPos pos = player.blockPosition().offset(random.nextInt(-3, 4), random.nextInt(0, 3), random.nextInt(-3, 4));
            if (player.serverLevel().getBlockState(pos).isAir()) {
                player.serverLevel().setBlockAndUpdate(pos, Blocks.COBWEB.defaultBlockState());
            }
        }
    }

    private static void spawnCreeperParty(ServerPlayer player) {
        for (int i = 0; i < 4; i++) {
            if (ThreadLocalRandom.current().nextInt(10) == 0) {
                runAtPlayer(player, "summon minecraft:creeper ~" + (i - 1) + " ~ ~2 {powered:1b}");
            } else {
                spawnMobs(player, EntityType.CREEPER, 1, 5, true);
            }
        }
    }

    private static void digHole(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos base = player.blockPosition();
        for (int y = 1; y <= 4; y++) {
            BlockPos pos = base.below(y);
            if (!level.getBlockState(pos).isAir() && level.getBlockState(pos).getDestroySpeed(level, pos) >= 0) {
                level.destroyBlock(pos, false, player);
            }
        }
        effect(player, MobEffects.SLOW_FALLING, 100, 0);
    }

    private static void placePowderSnow(ServerPlayer player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            BlockPos pos = player.blockPosition().offset(random.nextInt(-3, 4), random.nextInt(-1, 2), random.nextInt(-3, 4));
            BlockState state = player.serverLevel().getBlockState(pos);
            if (state.isAir()) player.serverLevel().setBlockAndUpdate(pos, Blocks.POWDER_SNOW.defaultBlockState());
        }
    }

    private static void placeSculk(ServerPlayer player) {
        BlockPos pos = player.blockPosition().below();
        if (player.serverLevel().getBlockState(pos).getDestroySpeed(player.serverLevel(), pos) >= 0) {
            player.serverLevel().setBlockAndUpdate(pos, Blocks.SCULK.defaultBlockState());
        }
    }

    private static void sandStorm(ServerPlayer player, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(-7, 8);
            int z = random.nextInt(-7, 8);
            runAtPlayer(player, "summon minecraft:falling_block ~" + x + " ~14 ~" + z
                    + " {BlockState:{Name:\"minecraft:sand\"},Time:1,DropItem:0b,HurtEntities:1b}");
        }
    }

    private static void smokeCloud(ServerPlayer player) {
        player.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE,
                player.getX(), player.getY() + 1.0, player.getZ(),
                180, 5.0, 2.5, 5.0, 0.04);
        effect(player, MobEffects.BLINDNESS, 50, 0);
    }

    private static void windStorm(ServerPlayer player) {
        spawnMobs(player, EntityType.BREEZE, 1, 7, true);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 4; i++) {
            int x = random.nextInt(-6, 7);
            int z = random.nextInt(-6, 7);
            runAtPlayer(player, "summon minecraft:wind_charge ~" + x + " ~10 ~" + z
                    + " {Motion:[0.0d,-0.8d,0.0d]}");
        }
    }

    private static void rainExperience(ServerPlayer player, int amount) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            ExperienceOrb.award(player.serverLevel(),
                    player.position().add(random.nextDouble(-6.0, 6.0), random.nextDouble(6.0, 12.0), random.nextDouble(-6.0, 6.0)),
                    Math.max(1, amount / 6));
        }
    }

    private void spawnHungrySlime(ServerPlayer player) {
        Slime slime = EntityType.SLIME.create(player.serverLevel());
        if (slime == null) return;
        slime.setSize(2, true);
        slime.setInvulnerable(true);
        slime.setPersistenceRequired();
        slime.setTarget(player);
        slime.moveTo(player.getX() + 3.0, player.getY(), player.getZ() + 3.0,
                ThreadLocalRandom.current().nextFloat() * 360.0F, 0.0F);
        player.serverLevel().addFreshEntity(slime);
        hungrySlimes.add(slime.getUUID());
    }

    private void growHungrySlimes(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (UUID id : new ArrayList<>(hungrySlimes)) {
                Entity entity = level.getEntity(id);
                if (entity instanceof Slime slime) {
                    slime.setSize(Math.min(16, slime.getSize() + 1), true);
                    level.sendParticles(ParticleTypes.ITEM_SLIME,
                            slime.getX(), slime.getY() + 1.0, slime.getZ(), 30, 1.0, 1.0, 1.0, 0.1);
                }
            }
        }
    }

    private void finishHungrySlimes(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (UUID id : hungrySlimes) {
                Entity entity = level.getEntity(id);
                if (entity instanceof Slime slime) {
                    slime.setInvulnerable(false);
                    level.explode(slime, slime.getX(), slime.getY(), slime.getZ(),
                            Math.min(6.0F, 2.0F + slime.getSize() / 4.0F), Level.ExplosionInteraction.NONE);
                    slime.discard();
                }
            }
        }
    }

    private static void spawnFallingTntMinecart(ServerPlayer player, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(-6, 7);
            int z = random.nextInt(-6, 7);
            runAtPlayer(player, "summon minecraft:tnt_minecart ~" + x + " ~14 ~" + z + " {TNTFuse:60}");
        }
    }

    private static void shippingRequest(ServerPlayer player) {
        runAtPlayer(player, "summon minecraft:chest_minecart ~ ~10 ~ {Motion:[0.0d,-0.5d,0.0d]}");
        giveOrDrop(player, new ItemStack(Items.BREAD, 8));
        giveOrDrop(player, new ItemStack(Items.IRON_INGOT, 4));
        giveOrDrop(player, new ItemStack(Items.TORCH, 16));
    }

    private static void spawnRandomGolem(ServerPlayer player) {
        EntityType<? extends Mob> type = ThreadLocalRandom.current().nextBoolean()
                ? EntityType.IRON_GOLEM : EntityType.SNOW_GOLEM;
        spawnMobs(player, type, 1, 4, false);
    }

    private static void spawnRandomPet(ServerPlayer player) {
        int choice = ThreadLocalRandom.current().nextInt(3);
        Mob mob = switch (choice) {
            case 0 -> EntityType.WOLF.create(player.serverLevel());
            case 1 -> EntityType.CAT.create(player.serverLevel());
            default -> EntityType.PARROT.create(player.serverLevel());
        };
        if (mob == null) return;
        mob.moveTo(player.getX() + 2.0, player.getY(), player.getZ() + 2.0,
                player.getYRot(), 0.0F);
        mob.setPersistenceRequired();
        if (mob instanceof Wolf wolf) wolf.tame(player);
        if (mob instanceof Cat cat) cat.tame(player);
        if (mob instanceof Parrot parrot) parrot.tame(player);
        player.serverLevel().addFreshEntity(mob);
    }

    private static void teleportPlayer(ServerPlayer player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        effect(player, MobEffects.DAMAGE_RESISTANCE, 160, 4);
        effect(player, MobEffects.SLOW_FALLING, 160, 0);
        effect(player, MobEffects.FIRE_RESISTANCE, 160, 0);
        double x = player.getX() + random.nextInt(-64, 65);
        double z = player.getZ() + random.nextInt(-64, 65);
        double y = Math.max(player.serverLevel().getMinBuildHeight() + 2,
                Math.min(player.serverLevel().getMaxBuildHeight() - 2, player.getY() + random.nextInt(-12, 21)));
        player.randomTeleport(x, y, z, true);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void sendRandomPlayerToEnd(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        ServerPlayer player = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        ServerLevel end = server.getLevel(Level.END);
        if (end == null) return;
        storedPositions.put(player.getUUID(), StoredPosition.capture(player));
        BlockPos platform = new BlockPos(100, 50, 0);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                end.setBlockAndUpdate(platform.offset(x, -1, z), Blocks.OBSIDIAN.defaultBlockState());
                end.setBlockAndUpdate(platform.offset(x, 0, z), Blocks.AIR.defaultBlockState());
                end.setBlockAndUpdate(platform.offset(x, 1, z), Blocks.AIR.defaultBlockState());
            }
        }
        player.teleportTo(end, platform.getX() + 0.5, platform.getY(), platform.getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot());
        effect(player, MobEffects.DAMAGE_RESISTANCE, 20 * 30, 4);
    }

    private void restoreStoredPlayers(MinecraftServer server) {
        for (Map.Entry<UUID, StoredPosition> entry : storedPositions.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) continue;
            StoredPosition pos = entry.getValue();
            ServerLevel level = server.getLevel(pos.dimension());
            if (level != null) {
                player.teleportTo(level, pos.x(), pos.y(), pos.z(), Set.of(), pos.yaw(), pos.pitch());
            }
        }
    }

    private static void tickRedLight(MinecraftServer server, int elapsedTicks, int remainingTicks) {
        int total = elapsedTicks + remainingTicks;
        boolean shouldBeGreen = elapsedTicks >= total / 2;
        if (RED_LIGHT.greenLight != shouldBeGreen) {
            RED_LIGHT.greenLight = shouldBeGreen;
            broadcast(server, shouldBeGreen ? "ЗЕЛЁНЫЙ СВЕТ!" : "КРАСНЫЙ СВЕТ!");
        }
        if (!shouldBeGreen) {
            forPlayers(server, player -> {
                effect(player, MobEffects.MOVEMENT_SLOWDOWN, 30, 255);
                effect(player, MobEffects.DIG_SLOWDOWN, 30, 255);
            });
        } else {
            forPlayers(server, player -> {
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.removeEffect(MobEffects.DIG_SLOWDOWN);
                effect(player, MobEffects.MOVEMENT_SPEED, 30, 1);
            });
        }
    }

    private void changeDamageRules(MinecraftServer server) {
        GameRules rules = server.overworld().getGameRules();
        oldFallDamage = rules.getBoolean(GameRules.RULE_FALL_DAMAGE);
        oldFireDamage = rules.getBoolean(GameRules.RULE_FIRE_DAMAGE);
        oldDrowningDamage = rules.getBoolean(GameRules.RULE_DROWNING_DAMAGE);
        oldFreezeDamage = rules.getBoolean(GameRules.RULE_FREEZE_DAMAGE);
        rules.getRule(GameRules.RULE_FALL_DAMAGE).set(ThreadLocalRandom.current().nextBoolean(), server);
        rules.getRule(GameRules.RULE_FIRE_DAMAGE).set(ThreadLocalRandom.current().nextBoolean(), server);
        rules.getRule(GameRules.RULE_DROWNING_DAMAGE).set(ThreadLocalRandom.current().nextBoolean(), server);
        rules.getRule(GameRules.RULE_FREEZE_DAMAGE).set(ThreadLocalRandom.current().nextBoolean(), server);
    }

    private void restoreDamageRules(MinecraftServer server) {
        GameRules rules = server.overworld().getGameRules();
        if (oldFallDamage != null) rules.getRule(GameRules.RULE_FALL_DAMAGE).set(oldFallDamage, server);
        if (oldFireDamage != null) rules.getRule(GameRules.RULE_FIRE_DAMAGE).set(oldFireDamage, server);
        if (oldDrowningDamage != null) rules.getRule(GameRules.RULE_DROWNING_DAMAGE).set(oldDrowningDamage, server);
        if (oldFreezeDamage != null) rules.getRule(GameRules.RULE_FREEZE_DAMAGE).set(oldFreezeDamage, server);
    }

    private void changeDayWeatherRules(MinecraftServer server) {
        GameRules rules = server.overworld().getGameRules();
        oldDaylightCycle = rules.getBoolean(GameRules.RULE_DAYLIGHT);
        oldWeatherCycle = rules.getBoolean(GameRules.RULE_WEATHER_CYCLE);
        rules.getRule(GameRules.RULE_DAYLIGHT).set(ThreadLocalRandom.current().nextBoolean(), server);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(ThreadLocalRandom.current().nextBoolean(), server);
    }

    private void restoreDayWeatherRules(MinecraftServer server) {
        GameRules rules = server.overworld().getGameRules();
        if (oldDaylightCycle != null) rules.getRule(GameRules.RULE_DAYLIGHT).set(oldDaylightCycle, server);
        if (oldWeatherCycle != null) rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(oldWeatherCycle, server);
    }

    private void changeInventoryRule(MinecraftServer server) {
        GameRules rules = server.overworld().getGameRules();
        oldKeepInventory = rules.getBoolean(GameRules.RULE_KEEPINVENTORY);
        rules.getRule(GameRules.RULE_KEEPINVENTORY).set(ThreadLocalRandom.current().nextBoolean(), server);
    }

    private void restoreInventoryRule(MinecraftServer server) {
        if (oldKeepInventory != null) {
            server.overworld().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(oldKeepInventory, server);
        }
    }

    private void changeSleepRule(MinecraftServer server) {
        GameRules rules = server.overworld().getGameRules();
        oldSleepingPercentage = rules.getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
        rules.getRule(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE)
                .set(ThreadLocalRandom.current().nextInt(1, 101), server);
    }

    private void restoreSleepRule(MinecraftServer server) {
        if (oldSleepingPercentage != null) {
            server.overworld().getGameRules().getRule(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE)
                    .set(oldSleepingPercentage, server);
        }
    }

    private void restoreGameModes(MinecraftServer server) {
        for (Map.Entry<UUID, GameType> entry : previousGameModes.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) player.setGameMode(entry.getValue());
        }
    }

    private static void smiteRandomPlayer(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        ServerPlayer player = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        runAtPlayer(player, "summon minecraft:lightning_bolt ~ ~ ~");
    }

    private record StoredPosition(ResourceKey<Level> dimension, double x, double y, double z,
                                  float yaw, float pitch) {
        private static StoredPosition capture(ServerPlayer player) {
            return new StoredPosition(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot());
        }
    }
}
