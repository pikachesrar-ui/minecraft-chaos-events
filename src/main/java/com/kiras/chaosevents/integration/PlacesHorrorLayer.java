package com.kiras.chaosevents.integration;

import com.kiras.chaosevents.ChaosEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ambient horror director used only while players are inside Places dimensions.
 *
 * <p>When Server-Side Horror is installed, its autonomous random events are suspended for the
 * duration of a running Chaos Events session. Chaos then calls the safe ambient pieces directly,
 * so horror stays associated with {@code places:*}. The particle jumpscare is deliberately never
 * used and its autonomous flag is forced off while the integration is active.</p>
 */
public final class PlacesHorrorLayer {
    private static final String SERVER_SIDE_HORROR_MOD_ID = "serversidehorror";

    private static final long FIRST_EVENT_MIN_MS = 12_000L;
    private static final long FIRST_EVENT_MAX_MS = 25_000L;
    private static final long EVENT_MIN_MS = 22_000L;
    private static final long EVENT_MAX_MS = 48_000L;
    private static final long SHADOW_MIN_MS = 1_400L;
    private static final long SHADOW_MAX_MS = 3_200L;

    private static final Map<UUID, PlayerHorrorState> PLAYER_STATES = new HashMap<>();
    private static final Map<UUID, ShadowRecord> SHADOWS = new HashMap<>();
    private static final ServerSideHorrorBridge HORROR_BRIDGE = new ServerSideHorrorBridge();

    private static boolean sessionActive;

    private PlacesHorrorLayer() {
    }

    public static synchronized void startSession(MinecraftServer server) {
        sessionActive = true;
        PLAYER_STATES.clear();
        removeAllShadows(server);
        HORROR_BRIDGE.enterControlledMode();
        ChaosEvents.LOGGER.info(
                "Places horror layer started (Server-Side Horror integration: {})",
                HORROR_BRIDGE.isAvailable() ? "enabled" : "vanilla fallback"
        );
    }

    public static synchronized void stopSession(MinecraftServer server) {
        sessionActive = false;
        PLAYER_STATES.clear();
        removeAllShadows(server);
        HORROR_BRIDGE.leaveControlledMode();
    }

    public static synchronized void reset() {
        sessionActive = false;
        PLAYER_STATES.clear();
        SHADOWS.clear();
        HORROR_BRIDGE.leaveControlledMode();
    }

    /** Uses wall-clock scheduling so accelerated TPS never turns ambience into spam. */
    public static void tick(MinecraftServer server) {
        synchronized (PlacesHorrorLayer.class) {
            if (!sessionActive) {
                return;
            }
        }

        long now = System.currentTimeMillis();
        tickShadows(server, now);

        Set<UUID> currentlyInside = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator() || !PlacesRealitySlipManager.isInPlacesDimension(player)) {
                continue;
            }
            currentlyInside.add(player.getUUID());
            tickPlayer(server, player, now);
        }

        synchronized (PlacesHorrorLayer.class) {
            PLAYER_STATES.keySet().removeIf(id -> !currentlyInside.contains(id));
        }
    }

    /** Operator/debug entry point. Returns false when the player is not currently inside Places. */
    public static boolean forceRandomEvent(MinecraftServer server, ServerPlayer player) {
        if (player == null || !PlacesRealitySlipManager.isInPlacesDimension(player)) {
            return false;
        }
        PlayerHorrorState state;
        synchronized (PlacesHorrorLayer.class) {
            state = PLAYER_STATES.computeIfAbsent(
                    player.getUUID(),
                    id -> new PlayerHorrorState(System.currentTimeMillis(), null, 0)
            );
        }
        triggerRandomEvent(server, player, state, System.currentTimeMillis());
        return true;
    }

    public static String getStatusText() {
        if (!ModList.get().isLoaded(SERVER_SIDE_HORROR_MOD_ID)) {
            return "Places horror: встроенные эффекты";
        }
        return "Places horror: Server-Side Horror под управлением Chaos";
    }

    private static void tickPlayer(MinecraftServer server, ServerPlayer player, long now) {
        PlayerHorrorState state;
        synchronized (PlacesHorrorLayer.class) {
            state = PLAYER_STATES.get(player.getUUID());
            if (state == null) {
                state = new PlayerHorrorState(now + randomDelay(FIRST_EVENT_MIN_MS, FIRST_EVENT_MAX_MS), null, 0);
                PLAYER_STATES.put(player.getUUID(), state);
                return;
            }
            if (now < state.nextEventAtMillis()) {
                return;
            }
        }

        HorrorEvent chosen = triggerRandomEvent(server, player, state, now);
        synchronized (PlacesHorrorLayer.class) {
            PLAYER_STATES.put(
                    player.getUUID(),
                    new PlayerHorrorState(
                            now + randomDelay(EVENT_MIN_MS, EVENT_MAX_MS),
                            chosen == null ? state.lastEvent() : chosen,
                            state.eventsTriggered() + (chosen == null ? 0 : 1)
                    )
            );
        }
    }

    private static HorrorEvent triggerRandomEvent(
            MinecraftServer server,
            ServerPlayer player,
            PlayerHorrorState state,
            long now
    ) {
        List<WeightedEvent> pool = buildEventPool(server, player, state);
        if (pool.isEmpty()) {
            return null;
        }

        int total = pool.stream().mapToInt(WeightedEvent::weight).sum();
        int roll = ThreadLocalRandom.current().nextInt(total);
        HorrorEvent chosen = pool.getFirst().event();
        for (WeightedEvent candidate : pool) {
            roll -= candidate.weight();
            if (roll < 0) {
                chosen = candidate.event();
                break;
            }
        }

        boolean success = switch (chosen) {
            case FOOTSTEPS -> triggerFootsteps(player);
            case FAKE_MINING -> triggerFakeMining(player);
            case DISTANT_SOUND -> triggerDistantSound(player);
            case LIGHT_FLICKER -> triggerLightFlicker(player);
            case DOOR_MOVE -> triggerDoorMove(player);
            case SHADOW_GLIMPSE -> triggerShadowGlimpse(player, now);
            case PHASE_SPLIT -> triggerPhaseSplit(server, player);
        };

        if (success) {
            ChaosEvents.LOGGER.debug(
                    "Places horror event {} triggered for {} in {}",
                    chosen,
                    player.getGameProfile().getName(),
                    player.level().dimension().location()
            );
            return chosen;
        }

        // Failed environmental events (no nearby door/safe shadow position/etc.) should not make the
        // player wait another full ambience interval. Fall back to the two effects that work almost
        // everywhere in Places.
        if (chosen != HorrorEvent.FOOTSTEPS && triggerFootsteps(player)) {
            return HorrorEvent.FOOTSTEPS;
        }
        if (chosen != HorrorEvent.DISTANT_SOUND && triggerDistantSound(player)) {
            return HorrorEvent.DISTANT_SOUND;
        }
        return null;
    }

    private static List<WeightedEvent> buildEventPool(
            MinecraftServer server,
            ServerPlayer player,
            PlayerHorrorState state
    ) {
        String dimension = player.level().dimension().location().getPath();
        boolean groupNearby = server.getPlayerList().getPlayers().stream()
                .anyMatch(other -> other != player
                        && PlacesRealitySlipManager.isInPlacesDimension(other)
                        && other.level() == player.level());

        List<WeightedEvent> pool = new ArrayList<>();
        add(pool, state, HorrorEvent.FOOTSTEPS, weightFor(HorrorEvent.FOOTSTEPS, dimension));
        add(pool, state, HorrorEvent.FAKE_MINING, weightFor(HorrorEvent.FAKE_MINING, dimension));
        add(pool, state, HorrorEvent.DISTANT_SOUND, weightFor(HorrorEvent.DISTANT_SOUND, dimension));
        add(pool, state, HorrorEvent.LIGHT_FLICKER, weightFor(HorrorEvent.LIGHT_FLICKER, dimension));
        add(pool, state, HorrorEvent.DOOR_MOVE, weightFor(HorrorEvent.DOOR_MOVE, dimension));
        add(pool, state, HorrorEvent.SHADOW_GLIMPSE, weightFor(HorrorEvent.SHADOW_GLIMPSE, dimension));
        if (groupNearby && state.eventsTriggered() >= 2) {
            add(pool, state, HorrorEvent.PHASE_SPLIT, weightFor(HorrorEvent.PHASE_SPLIT, dimension));
        }
        return pool;
    }

    private static void add(
            List<WeightedEvent> pool,
            PlayerHorrorState state,
            HorrorEvent event,
            int weight
    ) {
        if (weight <= 0 || event == state.lastEvent()) {
            return;
        }
        pool.add(new WeightedEvent(event, weight));
    }

    private static int weightFor(HorrorEvent event, String dimension) {
        int base = switch (event) {
            case FOOTSTEPS -> 30;
            case FAKE_MINING -> 20;
            case DISTANT_SOUND -> 22;
            case LIGHT_FLICKER -> 14;
            case DOOR_MOVE -> 10;
            case SHADOW_GLIMPSE -> 7;
            case PHASE_SPLIT -> 4;
        };

        return switch (dimension) {
            case "rooms_0" -> base + switch (event) {
                case FOOTSTEPS -> 10;
                case FAKE_MINING -> 8;
                case DOOR_MOVE -> 8;
                default -> 0;
            };
            case "warp_tunnel" -> base + switch (event) {
                case DISTANT_SOUND -> 12;
                case LIGHT_FLICKER -> 10;
                case SHADOW_GLIMPSE -> 4;
                default -> 0;
            };
            case "red_road_dim" -> base + switch (event) {
                case DISTANT_SOUND -> 10;
                case SHADOW_GLIMPSE -> 12;
                case DOOR_MOVE -> -6;
                default -> 0;
            };
            case "structure_bridge_dim" -> base + switch (event) {
                case FOOTSTEPS, DISTANT_SOUND -> 8;
                case PHASE_SPLIT -> 3;
                default -> 0;
            };
            case "pools_dim", "aquarium_tunnels_dim" -> base + switch (event) {
                case DISTANT_SOUND -> 8;
                case LIGHT_FLICKER -> 7;
                case FAKE_MINING -> -6;
                default -> 0;
            };
            default -> base;
        };
    }

    private static boolean triggerFootsteps(ServerPlayer player) {
        return HORROR_BRIDGE.fakeSteps(player);
    }

    private static boolean triggerFakeMining(ServerPlayer player) {
        return HORROR_BRIDGE.fakeMining(player);
    }

    private static boolean triggerDistantSound(ServerPlayer player) {
        return HORROR_BRIDGE.playScarySound(player, ThreadLocalRandom.current().nextInt(8, 19));
    }

    private static boolean triggerLightFlicker(ServerPlayer player) {
        int duration = ThreadLocalRandom.current().nextInt(10, 25);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration, 0, true, false, false));
        return true;
    }

    private static boolean triggerDoorMove(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        List<BlockPos> doors = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-9, -3, -9), center.offset(9, 3, 9))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock && state.hasProperty(DoorBlock.OPEN)) {
                doors.add(pos.immutable());
            }
        }
        if (doors.isEmpty()) {
            return false;
        }

        BlockPos chosen = doors.get(ThreadLocalRandom.current().nextInt(doors.size()));
        BlockState state = level.getBlockState(chosen);
        if (!(state.getBlock() instanceof DoorBlock)) {
            return false;
        }

        boolean open = state.getValue(DoorBlock.OPEN);
        setDoorOpenState(level, chosen, !open);
        return true;
    }

    private static void setDoorOpenState(ServerLevel level, BlockPos pos, boolean open) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock) || !state.hasProperty(DoorBlock.OPEN)) {
            return;
        }
        level.setBlock(pos, state.setValue(DoorBlock.OPEN, open), 3);

        BlockPos above = pos.above();
        BlockState upper = level.getBlockState(above);
        if (upper.getBlock() == state.getBlock() && upper.hasProperty(DoorBlock.OPEN)) {
            level.setBlock(above, upper.setValue(DoorBlock.OPEN, open), 3);
        }
        BlockPos below = pos.below();
        BlockState lower = level.getBlockState(below);
        if (lower.getBlock() == state.getBlock() && lower.hasProperty(DoorBlock.OPEN)) {
            level.setBlock(below, lower.setValue(DoorBlock.OPEN, open), 3);
        }
    }

    private static boolean triggerShadowGlimpse(ServerPlayer player, long now) {
        BlockPos spawn = findSafePosition(player.serverLevel(), player.blockPosition(), 8, 15, 36);
        if (spawn == null) {
            return false;
        }

        removeShadowFor(player.getServer(), player.getUUID());

        ArmorStand shadow = new ArmorStand(
                player.serverLevel(),
                spawn.getX() + 0.5D,
                spawn.getY(),
                spawn.getZ() + 0.5D
        );
        shadow.setInvisible(true);
        shadow.setNoGravity(true);
        shadow.setInvulnerable(true);
        shadow.setSilent(true);
        shadow.setCustomNameVisible(false);
        shadow.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        shadow.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        shadow.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        shadow.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));

        double dx = player.getX() - shadow.getX();
        double dz = player.getZ() - shadow.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        shadow.setYRot(yaw);
        shadow.setYHeadRot(yaw);
        shadow.setYBodyRot(yaw);

        if (!player.serverLevel().addFreshEntity(shadow)) {
            return false;
        }

        synchronized (PlacesHorrorLayer.class) {
            SHADOWS.put(
                    player.getUUID(),
                    new ShadowRecord(
                            shadow.getUUID(),
                            player.level().dimension().location().toString(),
                            now + randomDelay(SHADOW_MIN_MS, SHADOW_MAX_MS)
                    )
            );
        }
        return true;
    }

    private static boolean triggerPhaseSplit(MinecraftServer server, ServerPlayer player) {
        boolean hasCompanion = server.getPlayerList().getPlayers().stream()
                .anyMatch(other -> other != player
                        && other.level() == player.level()
                        && PlacesRealitySlipManager.isInPlacesDimension(other));
        if (!hasCompanion) {
            return false;
        }

        BlockPos destination = findSafePosition(player.serverLevel(), player.blockPosition(), 5, 11, 48);
        if (destination == null) {
            return false;
        }

        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 22, 0, true, false, false));
        boolean moved = player.teleportTo(
                player.serverLevel(),
                destination.getX() + 0.5D,
                destination.getY(),
                destination.getZ() + 0.5D,
                Set.<RelativeMovement>of(),
                player.getYRot(),
                player.getXRot()
        );
        if (moved) {
            player.fallDistance = 0.0F;
        }
        return moved;
    }

    private static BlockPos findSafePosition(
            ServerLevel level,
            BlockPos center,
            int minRadius,
            int maxRadius,
            int attempts
    ) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < attempts; attempt++) {
            double angle = random.nextDouble(Math.PI * 2.0D);
            int radius = random.nextInt(minRadius, maxRadius + 1);
            int x = center.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * radius);
            for (int dy = -4; dy <= 4; dy++) {
                BlockPos candidate = new BlockPos(x, center.getY() + dy, z);
                BlockPos floorPos = candidate.below();
                BlockState floor = level.getBlockState(floorPos);
                if (level.isEmptyBlock(candidate)
                        && level.isEmptyBlock(candidate.above())
                        && floor.isFaceSturdy(level, floorPos, Direction.UP)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static void tickShadows(MinecraftServer server, long now) {
        List<UUID> removeTargets = new ArrayList<>();
        synchronized (PlacesHorrorLayer.class) {
            for (Map.Entry<UUID, ShadowRecord> entry : SHADOWS.entrySet()) {
                ServerPlayer target = server.getPlayerList().getPlayer(entry.getKey());
                ShadowRecord record = entry.getValue();
                if (target == null
                        || !PlacesRealitySlipManager.isInPlacesDimension(target)
                        || now >= record.despawnAtMillis()) {
                    removeTargets.add(entry.getKey());
                    continue;
                }
                ServerLevel level = target.serverLevel();
                var entity = level.getEntity(record.entityId());
                if (entity == null || target.distanceToSqr(entity) <= 25.0D) {
                    removeTargets.add(entry.getKey());
                }
            }
        }
        removeTargets.forEach(id -> removeShadowFor(server, id));
    }

    private static synchronized void removeShadowFor(MinecraftServer server, UUID targetId) {
        if (server == null) {
            SHADOWS.remove(targetId);
            return;
        }
        ShadowRecord record = SHADOWS.remove(targetId);
        if (record == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            var entity = level.getEntity(record.entityId());
            if (entity != null) {
                entity.discard();
                return;
            }
        }
    }

    private static synchronized void removeAllShadows(MinecraftServer server) {
        if (server == null) {
            SHADOWS.clear();
            return;
        }
        for (ShadowRecord record : new ArrayList<>(SHADOWS.values())) {
            for (ServerLevel level : server.getAllLevels()) {
                var entity = level.getEntity(record.entityId());
                if (entity != null) {
                    entity.discard();
                    break;
                }
            }
        }
        SHADOWS.clear();
    }

    private static long randomDelay(long minInclusive, long maxInclusive) {
        return ThreadLocalRandom.current().nextLong(minInclusive, maxInclusive + 1L);
    }

    private enum HorrorEvent {
        FOOTSTEPS,
        FAKE_MINING,
        DISTANT_SOUND,
        LIGHT_FLICKER,
        DOOR_MOVE,
        SHADOW_GLIMPSE,
        PHASE_SPLIT
    }

    private record WeightedEvent(HorrorEvent event, int weight) {
    }

    private record PlayerHorrorState(long nextEventAtMillis, HorrorEvent lastEvent, int eventsTriggered) {
    }

    private record ShadowRecord(UUID entityId, String dimensionId, long despawnAtMillis) {
    }

    /** Reflection keeps Server-Side Horror optional for normal Chaos Events installations. */
    private static final class ServerSideHorrorBridge {
        private static final String COMMON_CLASS = "com.mars.serversidehorror.CommonClass";
        private static final String CONFIG_CLASS = "com.mars.serversidehorror.ServersideHorrorConfig";

        private static final String[] AUTONOMOUS_FLAGS = {
                "herobrine_starer_enable",
                "fake_joiner_enable",
                "jumpscare_enable",
                "long_night_enable",
                "break_torches_enable",
                "replace_torches_enable",
                "random_lightning_enable",
                "fake_mining_enable",
                "fake_steps_enable",
                "setting_up_new_traps_enable",
                "burn_down_house_enable",
                "joining_on_bedrock_enable",
                "joining_in_dungeon_enable",
                "removing_leaves_enable",
                "random_signs_enable",
                "random_fake_joiner_enable",
                "starer_enable",
                "heads_from_list_enable",
                "scary_sound_enable",
                "random_heads_enable",
                "old_villages_enable",
                "traps_enable"
        };

        private static final List<String> PLACES_SOUNDS = List.of(
                "minecraft:block.bell.resonate",
                "minecraft:block.bell.use",
                "minecraft:entity.arrow.hit",
                "minecraft:item.trident.hit_ground",
                "minecraft:item.crossbow.hit",
                "minecraft:entity.polar_bear.ambient",
                "minecraft:entity.polar_bear.warning"
        );

        private final Map<String, Boolean> originalFlags = new HashMap<>();
        private List<String> originalScarySounds;
        private Method fakeSteps;
        private Method fakeMining;
        private Method playScarySound;
        private boolean controlledMode;
        private boolean broken;

        boolean isAvailable() {
            return !broken && ModList.get().isLoaded(SERVER_SIDE_HORROR_MOD_ID);
        }

        synchronized void enterControlledMode() {
            if (!isAvailable() || controlledMode) {
                return;
            }
            try {
                Class<?> configClass = Class.forName(CONFIG_CLASS);
                originalFlags.clear();
                for (String name : AUTONOMOUS_FLAGS) {
                    Field field = configClass.getField(name);
                    originalFlags.put(name, field.getBoolean(null));
                    field.setBoolean(null, false);
                }

                Field scarySounds = configClass.getField("scary_sound_list");
                @SuppressWarnings("unchecked")
                List<String> currentSounds = (List<String>) scarySounds.get(null);
                originalScarySounds = currentSounds == null ? null : new ArrayList<>(currentSounds);
                scarySounds.set(null, new ArrayList<>(PLACES_SOUNDS));

                Class<?> commonClass = Class.forName(COMMON_CLASS);
                Field jumpScares = commonClass.getField("TO_BE_JUMP_SCARED");
                Object pending = jumpScares.get(null);
                if (pending instanceof List<?> list) {
                    list.clear();
                }

                fakeSteps = commonClass.getMethod("fakeSteps", ServerPlayer.class);
                fakeMining = commonClass.getMethod("fakeMining", ServerPlayer.class);
                playScarySound = commonClass.getMethod("playScarySound", ServerPlayer.class, int.class);
                controlledMode = true;
                ChaosEvents.LOGGER.info(
                        "Server-Side Horror autonomous events suspended; Places director owns ambience and jumpscares are disabled"
                );
            } catch (ReflectiveOperationException | RuntimeException exception) {
                broken = true;
                ChaosEvents.LOGGER.warn(
                        "Server-Side Horror integration could not enter controlled mode; using vanilla Places ambience",
                        exception
                );
            }
        }

        synchronized void leaveControlledMode() {
            if (!controlledMode) {
                originalFlags.clear();
                originalScarySounds = null;
                return;
            }
            try {
                Class<?> configClass = Class.forName(CONFIG_CLASS);
                for (Map.Entry<String, Boolean> entry : originalFlags.entrySet()) {
                    configClass.getField(entry.getKey()).setBoolean(null, entry.getValue());
                }
                if (originalScarySounds != null) {
                    configClass.getField("scary_sound_list").set(null, new ArrayList<>(originalScarySounds));
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                ChaosEvents.LOGGER.warn("Could not restore Server-Side Horror configuration", exception);
            } finally {
                originalFlags.clear();
                originalScarySounds = null;
                controlledMode = false;
            }
        }

        boolean fakeSteps(ServerPlayer player) {
            if (!ensureReady()) {
                return triggerVanillaFootstepFallback(player);
            }
            return invokeVoid(fakeSteps, player) || triggerVanillaFootstepFallback(player);
        }

        boolean fakeMining(ServerPlayer player) {
            if (!ensureReady()) {
                return triggerVanillaMiningFallback(player);
            }
            return invokeVoid(fakeMining, player) || triggerVanillaMiningFallback(player);
        }

        boolean playScarySound(ServerPlayer player, int radius) {
            if (!ensureReady()) {
                return triggerVanillaFootstepFallback(player);
            }
            try {
                Object result = playScarySound.invoke(null, player, radius);
                return result instanceof Boolean value && value;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                markBroken(exception);
                return triggerVanillaFootstepFallback(player);
            }
        }

        private synchronized boolean ensureReady() {
            if (!isAvailable()) {
                return false;
            }
            if (!controlledMode) {
                enterControlledMode();
            }
            return controlledMode && !broken;
        }

        private boolean invokeVoid(Method method, ServerPlayer player) {
            if (method == null) {
                return false;
            }
            try {
                method.invoke(null, player);
                return true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                markBroken(exception);
                return false;
            }
        }

        private synchronized void markBroken(Exception exception) {
            if (!broken) {
                ChaosEvents.LOGGER.warn(
                        "Server-Side Horror runtime bridge failed; falling back to vanilla Places ambience",
                        exception
                );
            }
            broken = true;
        }

        private static boolean triggerVanillaFootstepFallback(ServerPlayer player) {
            // The fallback is intentionally subtle: a short darkness pulse still gives the player a
            // local-only cue instead of broadcasting fake noises to every nearby companion.
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 8, 0, true, false, false));
            return true;
        }

        private static boolean triggerVanillaMiningFallback(ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 12, 0, true, false, false));
            return true;
        }
    }
}
