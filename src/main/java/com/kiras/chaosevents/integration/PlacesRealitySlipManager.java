package com.kiras.chaosevents.integration;

import com.kiras.chaosevents.ChaosEvents;
import com.kiras.chaosevents.event.BigEventEngine;
import com.kiras.chaosevents.spatial.SpatialSwapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Optional runtime bridge for Places 0.4.9 on NeoForge 1.21.1.
 *
 * <p>When Places is present, a running Chaos Events session can cause rare, deliberately
 * unannounced reality slips into Places. Every player entering Places while the session is active,
 * including through native Places doors, is returned to the exact pre-entry origin after a random
 * 3-9 minute wall-clock stay. Leaving Places through one of its own exits cancels that player's
 * pending return. Lethal damage during a managed stay also returns the player instead of allowing
 * the fatal hit to be applied.</p>
 */
public final class PlacesRealitySlipManager {
    private static final String PLACES_MOD_ID = "places";
    private static final String PLACES_NAMESPACE = "places";

    private static final int TICKS_PER_SECOND = 20;
    private static final int SCHEDULED_MIN_SECONDS = 40 * 60;
    private static final int SCHEDULED_MAX_SECONDS = 90 * 60;
    private static final int SCHEDULE_RETRY_SECONDS = 60;
    private static final int TRIGGER_COOLDOWN_SECONDS = 10 * 60;
    private static final int RETURN_MIN_SECONDS = 3 * 60;
    private static final int RETURN_MAX_SECONDS = 9 * 60;
    private static final int DEEP_CAVE_CHECK_SECONDS = 20;

    private static final int ENDER_PEARL_CHANCE = 100;
    private static final int BED_CHANCE = 90;
    private static final int PAIRED_BED_CHANCE = 12;
    private static final int ADDITIONAL_GROUP_MEMBER_CHANCE_PERCENT = 40;
    private static final double NEARBY_BED_PLAYER_DISTANCE_SQUARED = 6.0 * 6.0;
    private static final int DARK_DOOR_CHANCE = 80;
    private static final int DEEP_CAVE_CHANCE = 180;

    private static final Map<UUID, SlipRecord> ACTIVE_SLIPS = new HashMap<>();
    private static final Map<UUID, StoredPosition> LAST_NON_PLACES_POSITIONS = new HashMap<>();
    private static final Set<UUID> PLAYERS_IN_PLACES = new HashSet<>();
    private static final Map<PlacesDestination, Method> PORTAL_PROCEDURES =
            new EnumMap<>(PlacesDestination.class);
    private static final Set<PlacesDestination> UNAVAILABLE_DESTINATIONS =
            new HashSet<>();

    private static boolean sessionActive;
    private static int ticksUntilScheduledSlip;
    private static int triggerCooldownTicks;
    private static int deepCaveCheckTicks;
    private static UUID lastTarget;

    private PlacesRealitySlipManager() {
    }

    public static synchronized void startSession(MinecraftServer server) {
        sessionActive = true;
        ticksUntilScheduledSlip = randomScheduledDelayTicks();
        triggerCooldownTicks = 0;
        deepCaveCheckTicks = DEEP_CAVE_CHECK_SECONDS * TICKS_PER_SECOND;
        lastTarget = null;
        ACTIVE_SLIPS.clear();
        LAST_NON_PLACES_POSITIONS.clear();
        PLAYERS_IN_PLACES.clear();
        if (isPlacesLoaded()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!isInPlacesDimension(player)) {
                    LAST_NON_PLACES_POSITIONS.put(player.getUUID(), StoredPosition.capture(player));
                }
            }
        }
    }

    public static void stopSession(MinecraftServer server) {
        returnAllOnlinePlayers(server);
        synchronized (PlacesRealitySlipManager.class) {
            sessionActive = false;
            ticksUntilScheduledSlip = 0;
            triggerCooldownTicks = 0;
            deepCaveCheckTicks = 0;
            lastTarget = null;
            ACTIVE_SLIPS.clear();
            LAST_NON_PLACES_POSITIONS.clear();
            PLAYERS_IN_PLACES.clear();
        }
    }

    public static synchronized void reset() {
        sessionActive = false;
        ticksUntilScheduledSlip = 0;
        triggerCooldownTicks = 0;
        deepCaveCheckTicks = 0;
        lastTarget = null;
        ACTIVE_SLIPS.clear();
        LAST_NON_PLACES_POSITIONS.clear();
        PLAYERS_IN_PLACES.clear();
    }

    /** Runs only hidden trigger scheduling. Safety returns are ticked separately every server tick. */
    public static void tick(MinecraftServer server) {
        if (!isPlacesLoaded()) {
            return;
        }

        synchronized (PlacesRealitySlipManager.class) {
            if (!sessionActive) {
                return;
            }
            if (triggerCooldownTicks > 0) {
                triggerCooldownTicks--;
            }
            if (ticksUntilScheduledSlip > 0) {
                ticksUntilScheduledSlip--;
            }
            if (deepCaveCheckTicks > 0) {
                deepCaveCheckTicks--;
            }
        }

        if (shouldCheckDeepCaves()) {
            checkDeepCaves(server);
        }

        boolean scheduled;
        synchronized (PlacesRealitySlipManager.class) {
            scheduled = ticksUntilScheduledSlip <= 0;
        }
        if (scheduled) {
            tryScheduledSlip(server);
        }
    }

    /**
     * Safety return loop. It intentionally uses System.currentTimeMillis and is called directly by
     * ChaosSessionManager every server tick, so pause state, accelerated TPS and auxiliary timers
     * cannot prevent a due return.
     */
    public static void tickPendingReturns(MinecraftServer server) {
        if (!isPlacesLoaded()) {
            return;
        }

        long now = System.currentTimeMillis();
        List<UUID> due = new ArrayList<>();
        List<UUID> escaped = new ArrayList<>();
        List<ServerPlayer> newlyEntered = new ArrayList<>();
        List<ServerPlayer> newlyExited = new ArrayList<>();

        synchronized (PlacesRealitySlipManager.class) {
            if (sessionActive) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    UUID id = player.getUUID();
                    if (isInPlacesDimension(player)) {
                        if (PLAYERS_IN_PLACES.add(id)) {
                            newlyEntered.add(player);
                        }
                        StoredPosition origin = LAST_NON_PLACES_POSITIONS.get(id);
                        if (!ACTIVE_SLIPS.containsKey(id) && origin != null) {
                            int returnDelaySeconds = randomReturnDelaySeconds();
                            ACTIVE_SLIPS.put(id, new SlipRecord(
                                    origin,
                                    now + returnDelaySeconds * 1000L
                            ));
                            ChaosEvents.LOGGER.info(
                                    "Registered native Places entry for {}; automatic return in {} seconds",
                                    player.getGameProfile().getName(), returnDelaySeconds
                            );
                        }
                    } else {
                        if (PLAYERS_IN_PLACES.remove(id)) {
                            newlyExited.add(player);
                        }
                        LAST_NON_PLACES_POSITIONS.put(id, StoredPosition.capture(player));
                    }
                }
            }

            for (Map.Entry<UUID, SlipRecord> entry : ACTIVE_SLIPS.entrySet()) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null && !isInPlacesDimension(player)) {
                    escaped.add(entry.getKey());
                } else if (now >= entry.getValue().returnAtMillis()) {
                    due.add(entry.getKey());
                }
            }
            escaped.forEach(ACTIVE_SLIPS::remove);
        }

        for (ServerPlayer player : newlyEntered) {
            isolatePlayerFromGlobalEvents(server, player);
        }
        for (ServerPlayer player : newlyExited) {
            BigEventEngine.includePlayer(server, player);
        }

        for (UUID id : due) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                continue;
            }
            if (!isInPlacesDimension(player)) {
                synchronized (PlacesRealitySlipManager.class) {
                    ACTIVE_SLIPS.remove(id);
                }
                continue;
            }
            returnManagedPlayer(server, player, "automatic timer");
        }
    }

    /** Called from the shared right-click-item event. Returns true when normal item use should be cancelled. */
    public static boolean onRightClickItem(MinecraftServer server, ServerPlayer player, ItemStack stack) {
        if (!stack.is(Items.ENDER_PEARL) || !canRandomlyTrigger(server, player)) {
            return false;
        }
        if (ThreadLocalRandom.current().nextInt(ENDER_PEARL_CHANCE) != 0) {
            return false;
        }
        return triggerSlip(server, player, "ender_pearl", false);
    }

    /** Called from the shared right-click-block event. Returns true when normal interaction should be cancelled. */
    public static boolean onRightClickBlock(MinecraftServer server, ServerPlayer player, BlockState state) {
        if (!canRandomlyTrigger(server, player)) {
            return false;
        }

        if (state.getBlock() instanceof BedBlock) {
            ServerPlayer sleepingPartner = findNearbySleepingPartner(server, player);
            if (sleepingPartner != null
                    && ThreadLocalRandom.current().nextInt(PAIRED_BED_CHANCE) == 0
                    && triggerGroupSlip(server, List.of(player, sleepingPartner), "paired_beds", false)) {
                return true;
            }
            if (ThreadLocalRandom.current().nextInt(BED_CHANCE) == 0) {
                return triggerSlip(server, player, "bed", false);
            }
        }

        if (state.getBlock() instanceof DoorBlock
                && isVeryDark(player)
                && ThreadLocalRandom.current().nextInt(DARK_DOOR_CHANCE) == 0) {
            return triggerSlip(server, player, "dark_door", false);
        }
        return false;
    }

    public static boolean forceSlip(MinecraftServer server, ServerPlayer player) {
        if (!isPlacesLoaded() || player == null || isInPlacesDimension(player) || SpatialSwapManager.isActive()) {
            return false;
        }
        return triggerSlip(server, player, "operator_test", true);
    }

    /**
     * Called from LivingDamageEvent after armor/effects have produced the final damage value.
     * Returning true means the caller must cancel the damage event.
     */
    public static boolean rescueFromLethalDamage(MinecraftServer server, ServerPlayer player, float finalDamage) {
        if (player == null || finalDamage < player.getHealth() || !isInPlacesDimension(player)) {
            return false;
        }
        synchronized (PlacesRealitySlipManager.class) {
            if (!ACTIVE_SLIPS.containsKey(player.getUUID())) {
                StoredPosition origin = LAST_NON_PLACES_POSITIONS.get(player.getUUID());
                if (!sessionActive || origin == null) {
                    return false;
                }
                ACTIVE_SLIPS.put(player.getUUID(), new SlipRecord(
                        origin,
                        System.currentTimeMillis() + randomReturnDelaySeconds() * 1000L
                ));
            }
        }
        return returnManagedPlayer(server, player, "lethal damage");
    }

    public static boolean isInPlacesDimension(ServerPlayer player) {
        return player != null && isPlacesDimension(player.level());
    }

    public static boolean isPlacesDimension(Level level) {
        return level != null
                && PLACES_NAMESPACE.equals(level.dimension().location().getNamespace());
    }

    /** True for both Chaos-managed slips and players who entered Places using its native portals. */
    public static boolean hasAnyPlayerInPlaces(MinecraftServer server) {
        return isPlacesLoaded()
                && server.getPlayerList().getPlayers().stream().anyMatch(PlacesRealitySlipManager::isInPlacesDimension);
    }

    public static synchronized boolean hasManagedSlip(ServerPlayer player) {
        return player != null && ACTIVE_SLIPS.containsKey(player.getUUID());
    }

    public static synchronized String getStatusText() {
        if (!isPlacesLoaded()) {
            return "Places: не установлен";
        }
        if (!sessionActive) {
            return "Places: интеграция остановлена";
        }

        long now = System.currentTimeMillis();
        long nearestReturn = ACTIVE_SLIPS.values().stream()
                .mapToLong(SlipRecord::returnAtMillis)
                .min()
                .orElse(-1L);
        String returnText = nearestReturn < 0
                ? "нет активного возврата"
                : "ближайший возврат через " + formatSeconds((int) Math.max(0L, (nearestReturn - now + 999L) / 1000L));
        int seconds = Math.max(0, (ticksUntilScheduledSlip + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
        return "Places: скрытый сдвиг через ~" + formatSeconds(seconds) + ", " + returnText;
    }

    public static boolean isPlacesLoaded() {
        return ModList.get().isLoaded(PLACES_MOD_ID);
    }

    private static boolean returnManagedPlayer(MinecraftServer server, ServerPlayer player, String reason) {
        SlipRecord record;
        synchronized (PlacesRealitySlipManager.class) {
            record = ACTIVE_SLIPS.get(player.getUUID());
        }
        if (record == null || !teleport(server, player, record.origin())) {
            return false;
        }

        player.fallDistance = 0.0F;
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0F, 0.75F);
        synchronized (PlacesRealitySlipManager.class) {
            ACTIVE_SLIPS.remove(player.getUUID());
            PLAYERS_IN_PLACES.remove(player.getUUID());
        }
        BigEventEngine.includePlayer(server, player);
        ChaosEvents.LOGGER.info("Places reality slip returned {} to the original position ({})",
                player.getGameProfile().getName(), reason);
        return true;
    }

    private static boolean shouldCheckDeepCaves() {
        synchronized (PlacesRealitySlipManager.class) {
            if (deepCaveCheckTicks > 0) {
                return false;
            }
            deepCaveCheckTicks = DEEP_CAVE_CHECK_SECONDS * TICKS_PER_SECOND;
            return true;
        }
    }

    private static void checkDeepCaves(MinecraftServer server) {
        if (!canAnyRandomSlipStart(server)) {
            return;
        }

        List<ServerPlayer> candidates = server.getPlayerList().getPlayers().stream()
                .filter(player -> canRandomlyTrigger(server, player))
                .filter(player -> player.getY() <= 32.0)
                .filter(PlacesRealitySlipManager::isVeryDark)
                .toList();

        for (ServerPlayer player : candidates) {
            if (ThreadLocalRandom.current().nextInt(DEEP_CAVE_CHANCE) == 0
                    && triggerSlip(server, player, "deep_cave", false)) {
                return;
            }
        }
    }

    private static void tryScheduledSlip(MinecraftServer server) {
        if (!canAnyRandomSlipStart(server)) {
            synchronized (PlacesRealitySlipManager.class) {
                ticksUntilScheduledSlip = SCHEDULE_RETRY_SECONDS * TICKS_PER_SECOND;
            }
            return;
        }

        List<ServerPlayer> candidates = server.getPlayerList().getPlayers().stream()
                .filter(player -> !player.isSpectator())
                .filter(player -> !isInPlacesDimension(player))
                .filter(player -> !player.getUUID().equals(lastTarget))
                .toList();
        if (candidates.isEmpty()) {
            candidates = server.getPlayerList().getPlayers().stream()
                    .filter(player -> !player.isSpectator())
                    .filter(player -> !isInPlacesDimension(player))
                    .toList();
        }

        if (!candidates.isEmpty()) {
            ServerPlayer target = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            triggerSlip(server, target, "scheduled", false);
        }

        synchronized (PlacesRealitySlipManager.class) {
            ticksUntilScheduledSlip = randomScheduledDelayTicks();
        }
    }

    private static boolean canRandomlyTrigger(MinecraftServer server, ServerPlayer player) {
        synchronized (PlacesRealitySlipManager.class) {
            return sessionActive
                    && triggerCooldownTicks <= 0
                    && canAnyRandomSlipStart(server)
                    && player != null
                    && !player.isSpectator()
                    && !isInPlacesDimension(player);
        }
    }

    private static boolean canAnyRandomSlipStart(MinecraftServer server) {
        return isPlacesLoaded()
                && hasSupportedDestination(server)
                && !SpatialSwapManager.isActive()
                && !hasAnyPlayerInPlaces(server)
                && !hasOnlineActiveSlip(server);
    }

    private static boolean hasOnlineActiveSlip(MinecraftServer server) {
        synchronized (PlacesRealitySlipManager.class) {
            for (UUID id : ACTIVE_SLIPS.keySet()) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null && isInPlacesDimension(player)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean triggerSlip(MinecraftServer server, ServerPlayer player, String reason, boolean ignoreCooldown) {
        return triggerGroupSlip(server, List.of(player), reason, ignoreCooldown);
    }

    private static boolean triggerGroupSlip(
            MinecraftServer server,
            List<ServerPlayer> requestedPlayers,
            String reason,
            boolean ignoreCooldown
    ) {
        if (!isPlacesLoaded() || SpatialSwapManager.isActive()) {
            return false;
        }

        List<ServerPlayer> validatedPlayers = requestedPlayers.stream()
                .filter(player -> player != null && !player.isSpectator() && !isInPlacesDimension(player))
                .distinct()
                .toList();
        if (validatedPlayers.isEmpty() || validatedPlayers.size() != requestedPlayers.size()) {
            return false;
        }

        List<ServerPlayer> players = ensureMinimumSlipGroup(server, validatedPlayers);

        synchronized (PlacesRealitySlipManager.class) {
            if (!ignoreCooldown && (!sessionActive || triggerCooldownTicks > 0 || hasOnlineActiveSlip(server))) {
                return false;
            }
        }

        PlacesDestination destination = chooseDestination(server);
        if (destination == null) {
            ChaosEvents.LOGGER.warn("Places reality slip has no available safe destination");
            return false;
        }
        if (!refreshDestination(server, destination)) {
            ChaosEvents.LOGGER.warn("Places reality slip cancelled because {} could not be refreshed",
                    destination.dimensionId());
            return false;
        }

        Map<UUID, StoredPosition> origins = new HashMap<>();
        List<ServerPlayer> moved = new ArrayList<>();
        for (ServerPlayer player : players) {
            origins.put(player.getUUID(), StoredPosition.capture(player));
            if (!invokePlacesPortal(player, destination) || !isInPlacesDimension(player)) {
                ServerPlayer leader = moved.isEmpty() ? null : moved.getFirst();
                if (leader != null && moveToSlipLeader(player, leader)) {
                    ChaosEvents.LOGGER.info(
                            "Places portal fallback moved {} next to group leader {} in {}",
                            player.getGameProfile().getName(),
                            leader.getGameProfile().getName(),
                            destination.dimensionId()
                    );
                } else {
                    ChaosEvents.LOGGER.warn("Places reality slip did not move {} into {}",
                            player.getGameProfile().getName(), destination.dimensionId());
                    for (ServerPlayer movedPlayer : moved) {
                        teleport(server, movedPlayer, origins.get(movedPlayer.getUUID()));
                    }
                    return false;
                }
            }
            moved.add(player);
        }

        int returnDelaySeconds = randomReturnDelaySeconds();
        long returnAtMillis = System.currentTimeMillis() + returnDelaySeconds * 1000L;
        synchronized (PlacesRealitySlipManager.class) {
            for (ServerPlayer player : players) {
                UUID id = player.getUUID();
                StoredPosition origin = origins.get(id);
                ACTIVE_SLIPS.put(id, new SlipRecord(origin, returnAtMillis));
                LAST_NON_PLACES_POSITIONS.put(id, origin);
                PLAYERS_IN_PLACES.add(id);
            }
            lastTarget = players.getFirst().getUUID();
            triggerCooldownTicks = TRIGGER_COOLDOWN_SECONDS * TICKS_PER_SECOND;
        }
        for (ServerPlayer player : players) {
            isolatePlayerFromGlobalEvents(server, player);
            ChaosEvents.LOGGER.info(
                    "Places reality slip triggered for {} ({}) into {}, automatic return in {} seconds",
                    player.getGameProfile().getName(), reason, destination.dimensionId(), returnDelaySeconds);
        }
        return true;
    }

    /**
     * Builds the social group for a Chaos-triggered Places slip.
     *
     * <p>If exactly two eligible players are online, both always travel together. With three or
     * more eligible players, at least two always travel and every remaining eligible player has an
     * independent 40% chance to join the same slip. Requested multi-player groups (for example
     * paired beds) are preserved and can still gain additional companions.</p>
     */
    private static List<ServerPlayer> ensureMinimumSlipGroup(
            MinecraftServer server,
            List<ServerPlayer> requestedPlayers
    ) {
        List<ServerPlayer> result = new ArrayList<>(requestedPlayers);
        List<ServerPlayer> companions = new ArrayList<>(server.getPlayerList().getPlayers().stream()
                .filter(player -> !player.isSpectator())
                .filter(player -> !isInPlacesDimension(player))
                .filter(player -> requestedPlayers.stream().noneMatch(
                        requested -> requested.getUUID().equals(player.getUUID())))
                .toList());

        ThreadLocalRandom random = ThreadLocalRandom.current();
        while (result.size() < 2 && !companions.isEmpty()) {
            result.add(companions.remove(random.nextInt(companions.size())));
        }

        while (!companions.isEmpty()) {
            ServerPlayer candidate = companions.remove(random.nextInt(companions.size()));
            if (random.nextInt(100) < ADDITIONAL_GROUP_MEMBER_CHANCE_PERCENT) {
                result.add(candidate);
            }
        }

        return List.copyOf(result);
    }

    /**
     * Fallback for a Places 0.4.9 quirk where the native portal procedure can move the first member
     * of a group but fail to move a later member. The already moved player is the authoritative
     * destination, so companions are placed beside that player instead of splitting the group.
     */
    private static boolean moveToSlipLeader(ServerPlayer player, ServerPlayer leader) {
        if (player == null || leader == null || !isInPlacesDimension(leader)) {
            return false;
        }
        return player.teleportTo(
                leader.serverLevel(),
                leader.getX() + 0.75D,
                leader.getY(),
                leader.getZ() + 0.75D,
                Set.<RelativeMovement>of(),
                player.getYRot(),
                player.getXRot()
        );
    }

    private static ServerPlayer findNearbySleepingPartner(MinecraftServer server, ServerPlayer player) {
        return server.getPlayerList().getPlayers().stream()
                .filter(candidate -> candidate != player)
                .filter(candidate -> candidate.level() == player.level())
                .filter(ServerPlayer::isSleeping)
                .filter(candidate -> !candidate.isSpectator())
                .filter(candidate -> !isInPlacesDimension(candidate))
                .filter(candidate -> candidate.distanceToSqr(player) <= NEARBY_BED_PLAYER_DISTANCE_SQUARED)
                .findAny()
                .orElse(null);
    }

    private static PlacesDestination chooseDestination(MinecraftServer server) {
        List<PlacesDestination> available = new ArrayList<>();
        for (PlacesDestination destination : PlacesDestination.values()) {
            if (server.getLevel(destination.dimension()) != null && resolvePortalProcedure(destination) != null) {
                available.add(destination);
            }
        }
        return available.isEmpty()
                ? null
                : available.get(ThreadLocalRandom.current().nextInt(available.size()));
    }

    private static boolean hasSupportedDestination(MinecraftServer server) {
        for (PlacesDestination destination : PlacesDestination.values()) {
            if (server.getLevel(destination.dimension()) != null && resolvePortalProcedure(destination) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean invokePlacesPortal(ServerPlayer player, PlacesDestination destination) {
        try {
            Method method = resolvePortalProcedure(destination);
            if (method == null) {
                return false;
            }
            method.invoke(null, (LevelAccessor) player.serverLevel(), (Entity) player);
            return true;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            ChaosEvents.LOGGER.error("Failed to invoke Places portal procedure for {}",
                    destination.dimensionId(), exception);
            return false;
        }
    }

    private static void isolatePlayerFromGlobalEvents(MinecraftServer server, ServerPlayer player) {
        BigEventEngine.excludePlayer(server, player);
        SpatialSwapManager.excludePlayer(server, player);
    }

    /**
     * Restores the native Places start templates before a Chaos-managed visit. Places templates
     * contain explicit air blocks, so placing them again also removes player modifications.
     */
    private static boolean refreshDestination(MinecraftServer server, PlacesDestination destination) {
        ServerLevel level = server.getLevel(destination.dimension());
        if (level == null) {
            return false;
        }

        List<LoadedTemplate> templates = new ArrayList<>();
        for (TemplatePlacement placement : destination.refreshTemplates()) {
            StructureTemplate template = level.getStructureManager().getOrCreate(
                    ResourceLocation.fromNamespaceAndPath(PLACES_NAMESPACE, placement.templateName()));
            if (template.getSize().getX() <= 0 || template.getSize().getY() <= 0
                    || template.getSize().getZ() <= 0) {
                ChaosEvents.LOGGER.warn("Places refresh template {} is empty or unavailable",
                        placement.templateName());
                return false;
            }
            templates.add(new LoadedTemplate(placement, template));
        }

        for (LoadedTemplate loaded : templates) {
            BlockPos origin = loaded.placement().origin();
            var size = loaded.template().getSize();
            AABB bounds = new AABB(
                    origin.getX(), origin.getY(), origin.getZ(),
                    origin.getX() + size.getX(), origin.getY() + size.getY(), origin.getZ() + size.getZ()
            );
            level.getEntities((Entity) null, bounds, entity -> !(entity instanceof ServerPlayer))
                    .forEach(Entity::discard);
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.NONE)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(false);
        for (LoadedTemplate loaded : templates) {
            BlockPos origin = loaded.placement().origin();
            if (!loaded.template().placeInWorld(level, origin, origin, settings, level.random, 3)) {
                ChaosEvents.LOGGER.warn("Places refresh failed while placing template {}",
                        loaded.placement().templateName());
                return false;
            }
        }
        ChaosEvents.LOGGER.info("Refreshed Places arrival area for {}", destination.dimensionId());
        return true;
    }

    private static synchronized Method resolvePortalProcedure(PlacesDestination destination) {
        Method cached = PORTAL_PROCEDURES.get(destination);
        if (cached != null) {
            return cached;
        }
        if (UNAVAILABLE_DESTINATIONS.contains(destination)) {
            return null;
        }
        try {
            Class<?> procedure = Class.forName(destination.procedureClass());
            Method method = procedure.getMethod("execute", LevelAccessor.class, Entity.class);
            PORTAL_PROCEDURES.put(destination, method);
            ChaosEvents.LOGGER.info("Places safe destination enabled: {} using {}",
                    destination.dimensionId(), destination.procedureClass());
            return method;
        } catch (ReflectiveOperationException exception) {
            ChaosEvents.LOGGER.warn(
                    "Places destination {} is unavailable because its 0.4.9 portal procedure was not found",
                    destination.dimensionId(), exception);
            UNAVAILABLE_DESTINATIONS.add(destination);
            return null;
        }
    }

    private static boolean isVeryDark(ServerPlayer player) {
        return player.serverLevel().getMaxLocalRawBrightness(player.blockPosition()) <= 2;
    }

    private static void returnAllOnlinePlayers(MinecraftServer server) {
        Map<UUID, SlipRecord> snapshot;
        synchronized (PlacesRealitySlipManager.class) {
            snapshot = new HashMap<>(ACTIVE_SLIPS);
        }
        for (Map.Entry<UUID, SlipRecord> entry : snapshot.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && isInPlacesDimension(player)) {
                teleport(server, player, entry.getValue().origin());
            }
        }
    }

    private static boolean teleport(MinecraftServer server, ServerPlayer player, StoredPosition position) {
        ServerLevel target = server.getLevel(position.dimension());
        if (target == null) {
            ChaosEvents.LOGGER.warn("Cannot return {} from Places: origin dimension {} is unavailable",
                    player.getGameProfile().getName(), position.dimension().location());
            return false;
        }
        player.teleportTo(target, position.x(), position.y(), position.z(), Set.<RelativeMovement>of(),
                position.yaw(), position.pitch());
        return true;
    }

    private static int randomScheduledDelayTicks() {
        return ThreadLocalRandom.current().nextInt(SCHEDULED_MIN_SECONDS, SCHEDULED_MAX_SECONDS + 1)
                * TICKS_PER_SECOND;
    }

    private static int randomReturnDelaySeconds() {
        return ThreadLocalRandom.current().nextInt(RETURN_MIN_SECONDS, RETURN_MAX_SECONDS + 1);
    }

    private static String formatSeconds(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private record SlipRecord(StoredPosition origin, long returnAtMillis) {
    }

    /**
     * Native Places 0.4.9 portal procedures with fixed, mod-owned arrival points. Context-sensitive
     * transitions (Pocket Room and Ridge portals) are deliberately excluded from random slips.
     */
    private enum PlacesDestination {
        LEVEL_ZERO("rooms_0", "Level0PortalEntityCollidesInTheBlockProcedure",
                template("y_start_room", -23, 26, -23)),
        MANILA("manila_dim", "ManillaPortalEntityCollidesInTheBlockProcedure",
                template("manilla_room_dim", -23, 26, -23)),
        RED_ROAD("red_road_dim", "RedRoadPortalEntityCollidesInTheBlockProcedure",
                template("rr_west_wall", -70, 77, -23),
                template("rr_houses", -23, 77, -23),
                template("rr_east_wall", 24, 77, -23)),
        THE_END("the_end_dim", "TheEndPortalEntityCollidesInTheBlockProcedure",
                template("the_end_echo", -23, 26, -23),
                template("the_end_echo_add", -23, 26, 24)),
        STRUCTURE_BRIDGE("structure_bridge_dim", "StructureBridgePortalOnEntityProcedure",
                template("structure_bridge_start", -23, 50, -23),
                template("structure_bridge_bottom_1", -11, 2, -23)),
        WARP_TUNNEL("warp_tunnel", "WarpTunnelTeleportEntityCollidesProcedure",
                template("warp_tunnel_empty", 0, 20, 0));

        private static final String PROCEDURE_PACKAGE = "net.mcreator.places.procedures.";

        private final String dimensionId;
        private final ResourceKey<Level> dimension;
        private final String procedureClass;
        private final List<TemplatePlacement> refreshTemplates;

        PlacesDestination(String dimensionPath, String procedureClassName, TemplatePlacement... refreshTemplates) {
            this.dimensionId = PLACES_NAMESPACE + ":" + dimensionPath;
            this.dimension = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(PLACES_NAMESPACE, dimensionPath)
            );
            this.procedureClass = PROCEDURE_PACKAGE + procedureClassName;
            this.refreshTemplates = List.of(refreshTemplates);
        }

        String dimensionId() {
            return dimensionId;
        }

        ResourceKey<Level> dimension() {
            return dimension;
        }

        String procedureClass() {
            return procedureClass;
        }

        List<TemplatePlacement> refreshTemplates() {
            return refreshTemplates;
        }

        private static TemplatePlacement template(String name, int x, int y, int z) {
            return new TemplatePlacement(name, new BlockPos(x, y, z));
        }
    }

    private record TemplatePlacement(String templateName, BlockPos origin) {
    }

    private record LoadedTemplate(TemplatePlacement placement, StructureTemplate template) {
    }

    private record StoredPosition(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
        static StoredPosition capture(ServerPlayer player) {
            return new StoredPosition(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot());
        }
    }
}
