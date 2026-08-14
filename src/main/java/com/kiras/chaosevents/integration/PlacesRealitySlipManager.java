package com.kiras.chaosevents.integration;

import com.kiras.chaosevents.ChaosEvents;
import com.kiras.chaosevents.spatial.SpatialSwapManager;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Optional runtime bridge for Places 0.4.9 on NeoForge 1.21.1.
 *
 * <p>When Places is present, a running Chaos Events session can cause rare, deliberately
 * unannounced reality slips into Places. A player moved by Chaos Events is returned to the exact
 * origin after a random 5-10 minute wall-clock stay. Leaving Places through one of its own exits
 * cancels that pending return. Lethal damage during a managed slip also returns the player instead
 * of allowing the fatal hit to be applied.</p>
 */
public final class PlacesRealitySlipManager {
    private static final String PLACES_MOD_ID = "places";
    private static final String PLACES_NAMESPACE = "places";
    private static final String LEVEL_ZERO_PROCEDURE =
            "net.mcreator.places.procedures.Level0PortalEntityCollidesInTheBlockProcedure";

    private static final int TICKS_PER_SECOND = 20;
    private static final int SCHEDULED_MIN_SECONDS = 45 * 60;
    private static final int SCHEDULED_MAX_SECONDS = 120 * 60;
    private static final int SCHEDULE_RETRY_SECONDS = 60;
    private static final int TRIGGER_COOLDOWN_SECONDS = 10 * 60;
    private static final int RETURN_MIN_SECONDS = 5 * 60;
    private static final int RETURN_MAX_SECONDS = 10 * 60;
    private static final int DEEP_CAVE_CHECK_SECONDS = 20;

    private static final int ENDER_PEARL_CHANCE = 100;
    private static final int BED_CHANCE = 90;
    private static final int DARK_DOOR_CHANCE = 80;
    private static final int DEEP_CAVE_CHANCE = 180;

    private static final ResourceKey<Level> LEVEL_ZERO = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(PLACES_NAMESPACE, "rooms_0")
    );

    private static final Map<UUID, SlipRecord> ACTIVE_SLIPS = new HashMap<>();

    private static boolean sessionActive;
    private static int ticksUntilScheduledSlip;
    private static int triggerCooldownTicks;
    private static int deepCaveCheckTicks;
    private static UUID lastTarget;
    private static Method levelZeroProcedure;
    private static boolean reflectionResolved;

    private PlacesRealitySlipManager() {
    }

    public static synchronized void startSession() {
        sessionActive = true;
        ticksUntilScheduledSlip = randomScheduledDelayTicks();
        triggerCooldownTicks = 0;
        deepCaveCheckTicks = DEEP_CAVE_CHECK_SECONDS * TICKS_PER_SECOND;
        lastTarget = null;
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
        }
    }

    public static synchronized void reset() {
        sessionActive = false;
        ticksUntilScheduledSlip = 0;
        triggerCooldownTicks = 0;
        deepCaveCheckTicks = 0;
        lastTarget = null;
        ACTIVE_SLIPS.clear();
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

        synchronized (PlacesRealitySlipManager.class) {
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

        if (state.getBlock() instanceof BedBlock
                && ThreadLocalRandom.current().nextInt(BED_CHANCE) == 0) {
            return triggerSlip(server, player, "bed", false);
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
                return false;
            }
        }
        return returnManagedPlayer(server, player, "lethal damage");
    }

    public static boolean isInPlacesDimension(ServerPlayer player) {
        return player != null
                && PLACES_NAMESPACE.equals(player.level().dimension().location().getNamespace());
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
        }
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
                && server.getLevel(LEVEL_ZERO) != null
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
        if (!isPlacesLoaded() || player == null || isInPlacesDimension(player) || SpatialSwapManager.isActive()) {
            return false;
        }
        synchronized (PlacesRealitySlipManager.class) {
            if (!ignoreCooldown && (!sessionActive || triggerCooldownTicks > 0 || hasOnlineActiveSlip(server))) {
                return false;
            }
        }

        StoredPosition origin = StoredPosition.capture(player);
        if (!invokePlacesLevelZero(player)) {
            return false;
        }
        if (!isInPlacesDimension(player)) {
            ChaosEvents.LOGGER.warn("Places reality slip did not move {} into a Places dimension",
                    player.getGameProfile().getName());
            return false;
        }

        int returnDelaySeconds = randomReturnDelaySeconds();
        long returnAtMillis = System.currentTimeMillis() + returnDelaySeconds * 1000L;
        synchronized (PlacesRealitySlipManager.class) {
            ACTIVE_SLIPS.put(player.getUUID(), new SlipRecord(origin, returnAtMillis));
            lastTarget = player.getUUID();
            triggerCooldownTicks = TRIGGER_COOLDOWN_SECONDS * TICKS_PER_SECOND;
        }
        ChaosEvents.LOGGER.info("Places reality slip triggered for {} ({}), automatic return in {} seconds",
                player.getGameProfile().getName(), reason, returnDelaySeconds);
        return true;
    }

    private static boolean invokePlacesLevelZero(ServerPlayer player) {
        try {
            Method method = resolveLevelZeroProcedure();
            if (method == null) {
                return false;
            }
            method.invoke(null, (LevelAccessor) player.serverLevel(), (Entity) player);
            return true;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            ChaosEvents.LOGGER.error("Failed to invoke Places level-zero portal procedure", exception);
            return false;
        }
    }

    private static synchronized Method resolveLevelZeroProcedure() {
        if (reflectionResolved) {
            return levelZeroProcedure;
        }
        reflectionResolved = true;
        try {
            Class<?> procedure = Class.forName(LEVEL_ZERO_PROCEDURE);
            levelZeroProcedure = procedure.getMethod("execute", LevelAccessor.class, Entity.class);
            ChaosEvents.LOGGER.info("Places integration enabled using {}", LEVEL_ZERO_PROCEDURE);
        } catch (ReflectiveOperationException exception) {
            ChaosEvents.LOGGER.warn(
                    "Places is installed, but the supported 0.4.9 portal procedure was not found; reality slips are disabled",
                    exception);
            levelZeroProcedure = null;
        }
        return levelZeroProcedure;
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

    private record StoredPosition(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
        static StoredPosition capture(ServerPlayer player) {
            return new StoredPosition(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot());
        }
    }
}
