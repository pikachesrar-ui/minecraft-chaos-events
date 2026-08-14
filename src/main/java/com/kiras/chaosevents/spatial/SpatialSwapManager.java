package com.kiras.chaosevents.spatial;

import com.kiras.chaosevents.config.ChaosConfigCategory;
import com.kiras.chaosevents.config.ChaosConfigEntry;
import com.kiras.chaosevents.config.ChaosConfigManager;
import com.kiras.chaosevents.event.BigEventPlayerPolicy;
import com.kiras.chaosevents.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.item.ItemStack;
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

/** Runtime state of reversible spatial swaps and the dedicated spatial-swap big event. */
public final class SpatialSwapManager {
    public static final String CONFIG_AUTOMATIC_SWAP = "automatic_swap";
    public static final String CONFIG_DIAMOND_SWAP = "diamond_swap";
    public static final String CONFIG_BIG_EVENT_SWAP = "big_event_swap";

    private static final int TICKS_PER_SECOND = 20;
    private static final int RETURN_WINDOW_TICKS = 10 * TICKS_PER_SECOND;
    private static final int AUTOMATIC_SWAP_RETRY_SECONDS = 60;
    private static final String PREFIX = "[Пространственный сдвиг] ";

    private static final Map<UUID, StoredPosition> ORIGINAL_POSITIONS = new HashMap<>();
    private static final Set<UUID> PARTICIPANTS = new HashSet<>();
    private static final Set<UUID> ACTIVATED_ANCHORS = new HashSet<>();

    private static boolean sessionActive;
    private static boolean returnActive;
    private static boolean dedicatedEventActive;
    private static boolean diamondSwapUsedForSession;
    private static int anchorWindowTicks;
    private static int ticksUntilAutomaticSwap;

    private SpatialSwapManager() {
    }

    public static synchronized void startSession() {
        sessionActive = true;
        returnActive = false;
        dedicatedEventActive = false;
        diamondSwapUsedForSession = false;
        anchorWindowTicks = 0;
        ORIGINAL_POSITIONS.clear();
        PARTICIPANTS.clear();
        ACTIVATED_ANCHORS.clear();
        scheduleAutomaticSwap();
    }

    public static void stopSession(MinecraftServer server) {
        clearReturnContext(server);
        synchronized (SpatialSwapManager.class) {
            sessionActive = false;
            dedicatedEventActive = false;
            diamondSwapUsedForSession = false;
            ticksUntilAutomaticSwap = 0;
        }
    }

    /** Ticks return anchors and the independent configurable swap timer at normal Chaos Events speed. */
    public static void tickSession(MinecraftServer server) {
        tickAnchorWindow(server);

        if (!ChaosConfigManager.isEnabled(ChaosConfigCategory.SWAP, CONFIG_AUTOMATIC_SWAP)) {
            synchronized (SpatialSwapManager.class) {
                ticksUntilAutomaticSwap = 0;
            }
            return;
        }

        boolean triggerAutomaticSwap = false;
        synchronized (SpatialSwapManager.class) {
            if (!sessionActive) return;
            if (ticksUntilAutomaticSwap <= 0) scheduleAutomaticSwap();
            if (ticksUntilAutomaticSwap > 0) ticksUntilAutomaticSwap--;
            if (ticksUntilAutomaticSwap > 0) return;

            if (returnActive || dedicatedEventActive || eligiblePlayers(server).size() < 2) {
                ticksUntilAutomaticSwap = AUTOMATIC_SWAP_RETRY_SECONDS * TICKS_PER_SECOND;
                return;
            }

            scheduleAutomaticSwap();
            triggerAutomaticSwap = true;
        }

        if (triggerAutomaticSwap) {
            List<ServerPlayer> players = eligiblePlayers(server);
            broadcast(server, "Плановый пространственный сбой! Игроки меняются текущими позициями.");
            beginReversibleSwap(server, players);
        }
    }

    public static synchronized boolean canStart(MinecraftServer server) {
        return ChaosConfigManager.isEnabled(ChaosConfigCategory.SWAP, CONFIG_BIG_EVENT_SWAP)
                && !returnActive
                && eligiblePlayers(server).size() >= 2;
    }

    public static boolean startEvent(MinecraftServer server) {
        if (!ChaosConfigManager.isEnabled(ChaosConfigCategory.SWAP, CONFIG_BIG_EVENT_SWAP)) {
            return false;
        }

        List<ServerPlayer> players = eligiblePlayers(server);
        if (players.size() < 2) {
            return false;
        }

        synchronized (SpatialSwapManager.class) {
            dedicatedEventActive = true;
        }
        beginReversibleSwap(server, players);
        broadcast(server, "Игроки поменялись местами, в том числе между измерениями.");
        if (isDiamondSwapAvailable()) {
            broadcast(server, "Первая добытая за эту сессию алмазная руда вызовет ещё один обмен.");
        }
        return true;
    }

    /** The independent session tick owns anchor timing to avoid double ticking during the big event. */
    public static void tickEvent(MinecraftServer server) {
    }

    public static InteractionResult activateAnchor(MinecraftServer server, ServerPlayer player, InteractionHand hand) {
        boolean restore;
        int activated;
        int required;

        synchronized (SpatialSwapManager.class) {
            if (!BigEventPlayerPolicy.canAffect(player)
                    || !returnActive
                    || !PARTICIPANTS.contains(player.getUUID())) {
                return InteractionResult.PASS;
            }

            if (anchorWindowTicks <= 0) {
                anchorWindowTicks = RETURN_WINDOW_TICKS;
                ACTIVATED_ANCHORS.clear();
            }

            ACTIVATED_ANCHORS.add(player.getUUID());
            required = countOnlineParticipants(server);
            activated = countOnlineActivated(server);
            restore = required >= 2 && activated >= required;
        }

        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_FRAME_FILL,
                SoundSource.PLAYERS, 1.0F, 1.2F);
        broadcast(server, player.getGameProfile().getName() + " активировал якорь: " + activated + "/" + required + ".");

        if (restore) {
            restoreOriginalPositions(server);
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean onBlockBroken(MinecraftServer server, ServerPlayer player, BlockState state) {
        if (!ChaosConfigManager.isEnabled(ChaosConfigCategory.SWAP, CONFIG_DIAMOND_SWAP)) {
            return false;
        }

        boolean trigger;
        boolean preserveExistingReturn;
        synchronized (SpatialSwapManager.class) {
            trigger = sessionActive
                    && !diamondSwapUsedForSession
                    && eligiblePlayers(server).size() >= 2
                    && (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE));
            preserveExistingReturn = returnActive;
            if (trigger) {
                diamondSwapUsedForSession = true;
            }
        }

        if (trigger) {
            broadcast(server, player.getGameProfile().getName()
                    + " первым за сессию сломал алмазную руду. Пространство меняется!");
            if (preserveExistingReturn) {
                swapCurrentParticipants(server);
            } else {
                beginReversibleSwap(server, eligiblePlayers(server));
            }
        }
        return trigger;
    }

    public static void stopEvent(MinecraftServer server) {
        boolean wasDedicated;
        synchronized (SpatialSwapManager.class) {
            wasDedicated = dedicatedEventActive;
            dedicatedEventActive = false;
        }
        if (wasDedicated) {
            clearReturnContext(server);
        }
    }

    public static synchronized void reset() {
        sessionActive = false;
        returnActive = false;
        dedicatedEventActive = false;
        diamondSwapUsedForSession = false;
        anchorWindowTicks = 0;
        ticksUntilAutomaticSwap = 0;
        ORIGINAL_POSITIONS.clear();
        PARTICIPANTS.clear();
        ACTIVATED_ANCHORS.clear();
    }

    /** Removes a player who crossed into Places from all current and future swap effects. */
    public static void excludePlayer(MinecraftServer server, ServerPlayer player) {
        boolean noLongerEnoughParticipants;
        synchronized (SpatialSwapManager.class) {
            UUID id = player.getUUID();
            PARTICIPANTS.remove(id);
            ORIGINAL_POSITIONS.remove(id);
            ACTIVATED_ANCHORS.remove(id);
            noLongerEnoughParticipants = returnActive && PARTICIPANTS.size() < 2;
        }
        removeAnchors(player);
        if (noLongerEnoughParticipants) {
            clearReturnContext(server);
        }
    }

    private static void tickAnchorWindow(MinecraftServer server) {
        boolean expired = false;
        synchronized (SpatialSwapManager.class) {
            if (!sessionActive || !returnActive || anchorWindowTicks <= 0) {
                return;
            }
            anchorWindowTicks--;
            if (anchorWindowTicks == 0) {
                ACTIVATED_ANCHORS.clear();
                expired = true;
            }
        }
        if (expired) {
            broadcast(server, "Окно синхронизации закрылось. Якоря можно попробовать активировать снова.");
        }
    }

    private static void beginReversibleSwap(MinecraftServer server, List<ServerPlayer> players) {
        if (players.size() < 2) {
            return;
        }

        removeAllAnchors(server);
        synchronized (SpatialSwapManager.class) {
            returnActive = true;
            anchorWindowTicks = 0;
            ORIGINAL_POSITIONS.clear();
            PARTICIPANTS.clear();
            ACTIVATED_ANCHORS.clear();
            for (ServerPlayer player : players) {
                PARTICIPANTS.add(player.getUUID());
                ORIGINAL_POSITIONS.put(player.getUUID(), StoredPosition.capture(player));
            }
        }

        swapPlayers(server, players);
        for (ServerPlayer player : players) {
            giveAnchor(player);
        }
        broadcast(server, "Каждый участник получил пространственный якорь. После активации первого у всех будет 10 секунд, чтобы активировать свои и вернуться назад.");
    }

    private static void restoreOriginalPositions(MinecraftServer server) {
        Map<UUID, StoredPosition> originals;
        synchronized (SpatialSwapManager.class) {
            if (!returnActive) {
                return;
            }
            originals = new HashMap<>(ORIGINAL_POSITIONS);
        }

        for (Map.Entry<UUID, StoredPosition> entry : originals.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (BigEventPlayerPolicy.canAffect(player)) {
                teleport(server, player, entry.getValue());
            }
        }
        broadcast(server, "Все якоря синхронизированы. Игроки возвращены на исходные позиции.");
        clearReturnContext(server);
    }

    private static void clearReturnContext(MinecraftServer server) {
        removeAllAnchors(server);
        synchronized (SpatialSwapManager.class) {
            returnActive = false;
            anchorWindowTicks = 0;
            ORIGINAL_POSITIONS.clear();
            PARTICIPANTS.clear();
            ACTIVATED_ANCHORS.clear();
        }
    }

    private static void swapCurrentParticipants(MinecraftServer server) {
        List<ServerPlayer> players;
        synchronized (SpatialSwapManager.class) {
            players = server.getPlayerList().getPlayers().stream()
                    .filter(BigEventPlayerPolicy::canAffect)
                    .filter(player -> PARTICIPANTS.contains(player.getUUID()))
                    .toList();
        }
        swapPlayers(server, players);
    }

    private static void swapPlayers(MinecraftServer server, List<ServerPlayer> players) {
        if (players.size() < 2) {
            return;
        }

        List<StoredPosition> positions = players.stream().map(StoredPosition::capture).toList();
        int offset = ThreadLocalRandom.current().nextInt(1, players.size());
        for (int i = 0; i < players.size(); i++) {
            StoredPosition destination = positions.get((i + offset) % positions.size());
            teleport(server, players.get(i), destination);
        }

        for (ServerPlayer player : players) {
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 1.2F, 0.65F);
        }
    }

    private static void teleport(MinecraftServer server, ServerPlayer player, StoredPosition position) {
        ServerLevel target = server.getLevel(position.dimension());
        if (target == null) {
            return;
        }
        player.teleportTo(target, position.x(), position.y(), position.z(), Set.<RelativeMovement>of(),
                position.yaw(), position.pitch());
    }

    private static int countOnlineParticipants(MinecraftServer server) {
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (BigEventPlayerPolicy.canAffect(player) && PARTICIPANTS.contains(player.getUUID())) {
                count++;
            }
        }
        return count;
    }

    private static int countOnlineActivated(MinecraftServer server) {
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (BigEventPlayerPolicy.canAffect(player)
                    && PARTICIPANTS.contains(player.getUUID())
                    && ACTIVATED_ANCHORS.contains(player.getUUID())) {
                count++;
            }
        }
        return count;
    }

    private static void giveAnchor(ServerPlayer player) {
        removeAnchors(player);
        ItemStack anchor = new ItemStack(ModItems.SPATIAL_ANCHOR.get());
        if (!player.getInventory().add(anchor)) {
            player.drop(anchor, false);
        }
        player.getInventory().setChanged();
    }

    private static void removeAllAnchors(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(SpatialSwapManager::removeAnchors);
    }

    private static void removeAnchors(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.SPATIAL_ANCHOR.get())) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
        player.getInventory().setChanged();
    }

    private static synchronized void scheduleAutomaticSwap() {
        if (!ChaosConfigManager.isEnabled(ChaosConfigCategory.SWAP, CONFIG_AUTOMATIC_SWAP)) {
            ticksUntilAutomaticSwap = 0;
            return;
        }
        int minSeconds = ChaosConfigManager.getMinIntervalSeconds(ChaosConfigCategory.SWAP);
        int maxSeconds = ChaosConfigManager.getMaxIntervalSeconds(ChaosConfigCategory.SWAP);
        ticksUntilAutomaticSwap = ThreadLocalRandom.current().nextInt(minSeconds, maxSeconds + 1)
                * TICKS_PER_SECOND;
    }

    private static synchronized boolean isDiamondSwapAvailable() {
        return ChaosConfigManager.isEnabled(ChaosConfigCategory.SWAP, CONFIG_DIAMOND_SWAP)
                && !diamondSwapUsedForSession;
    }

    private static void broadcast(MinecraftServer server, String text) {
        Component component = Component.literal(PREFIX + text);
        eligiblePlayers(server).forEach(player -> player.sendSystemMessage(component));
    }

    private static List<ServerPlayer> eligiblePlayers(MinecraftServer server) {
        return new ArrayList<>(BigEventPlayerPolicy.eligiblePlayers(server));
    }

    public static synchronized boolean isActive() {
        return returnActive || dedicatedEventActive;
    }

    public static synchronized String getStatusText() {
        if (!sessionActive) {
            return "пространственные свапы остановлены";
        }

        String automaticStatus;
        if (!ChaosConfigManager.isEnabled(ChaosConfigCategory.SWAP, CONFIG_AUTOMATIC_SWAP)) {
            automaticStatus = "плановый свап отключён";
        } else {
            int seconds = Math.max(0, (ticksUntilAutomaticSwap + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
            automaticStatus = "плановый свап через " + formatSeconds(seconds);
        }

        if (!returnActive) {
            return automaticStatus;
        }
        if (anchorWindowTicks > 0) {
            return "окно якорей: " + String.format("%.1f", anchorWindowTicks / 20.0)
                    + " сек.; " + automaticStatus;
        }
        return "якоря ожидают синхронизации; " + automaticStatus;
    }

    public static List<ChaosConfigEntry> getConfigEntries() {
        return List.of(
                new ChaosConfigEntry(CONFIG_AUTOMATIC_SWAP, "Плановый свап",
                        "Периодически меняет местами всех игроков по настраиваемому интервалу и выдаёт якоря возврата."),
                new ChaosConfigEntry(CONFIG_DIAMOND_SWAP, "Свап за первую алмазную руду",
                        "Один раз за сессию первая сломанная алмазная руда меняет игроков местами."),
                new ChaosConfigEntry(CONFIG_BIG_EVENT_SWAP, "Большой ивент «Пространственный сдвиг»",
                        "Разрешает пространственный сдвиг появляться в пуле больших событий.")
        );
    }

    private static String formatSeconds(int totalSeconds) {
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private record StoredPosition(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
        static StoredPosition capture(ServerPlayer player) {
            return new StoredPosition(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot());
        }
    }
}
