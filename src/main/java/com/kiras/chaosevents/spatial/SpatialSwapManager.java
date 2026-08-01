package com.kiras.chaosevents.spatial;

import com.kiras.chaosevents.registry.ModItems;
import net.minecraft.core.BlockPos;
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

/** Runtime state of the spatial-swap big event. */
public final class SpatialSwapManager {
    private static final int RETURN_WINDOW_TICKS = 10 * 20;
    private static final String PREFIX = "[Пространственный сдвиг] ";

    private static final Map<UUID, StoredPosition> ORIGINAL_POSITIONS = new HashMap<>();
    private static final Set<UUID> PARTICIPANTS = new HashSet<>();
    private static final Set<UUID> ACTIVATED_ANCHORS = new HashSet<>();

    private static boolean active;
    private static boolean diamondSwapUsed;
    private static int anchorWindowTicks;

    private SpatialSwapManager() {
    }

    public static synchronized boolean canStart(MinecraftServer server) {
        return server.getPlayerList().getPlayers().size() >= 2;
    }

    public static boolean startEvent(MinecraftServer server) {
        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        if (players.size() < 2) {
            return false;
        }

        synchronized (SpatialSwapManager.class) {
            active = true;
            diamondSwapUsed = false;
            anchorWindowTicks = 0;
            ORIGINAL_POSITIONS.clear();
            PARTICIPANTS.clear();
            ACTIVATED_ANCHORS.clear();

            for (ServerPlayer player : players) {
                PARTICIPANTS.add(player.getUUID());
                ORIGINAL_POSITIONS.put(player.getUUID(), StoredPosition.capture(player));
            }
        }

        swapCurrentPositions(server);
        for (ServerPlayer player : players) {
            giveAnchor(player);
        }
        broadcast(server, "Игроки поменялись местами, в том числе между измерениями.");
        broadcast(server, "У каждого есть пространственный якорь. После активации первого у всех будет 10 секунд, чтобы активировать свои.");
        broadcast(server, "Первая добытая алмазная руда вызовет дополнительный обмен.");
        return true;
    }

    public static synchronized void tick(MinecraftServer server) {
        if (!active || anchorWindowTicks <= 0) {
            return;
        }

        anchorWindowTicks--;
        if (anchorWindowTicks == 0) {
            ACTIVATED_ANCHORS.clear();
            broadcast(server, "Окно синхронизации закрылось. Якоря можно попробовать активировать снова.");
        }
    }

    public static InteractionResult activateAnchor(MinecraftServer server, ServerPlayer player, InteractionHand hand) {
        boolean restore;
        int activated;
        int required;

        synchronized (SpatialSwapManager.class) {
            if (!active || !PARTICIPANTS.contains(player.getUUID())) {
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
        boolean trigger;
        synchronized (SpatialSwapManager.class) {
            trigger = active
                    && !diamondSwapUsed
                    && PARTICIPANTS.contains(player.getUUID())
                    && (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE));
            if (trigger) {
                diamondSwapUsed = true;
            }
        }

        if (trigger) {
            broadcast(server, player.getGameProfile().getName() + " потревожил алмазную жилу. Пространство снова меняется!");
            swapCurrentPositions(server);
        }
        return trigger;
    }

    public static void stopEvent(MinecraftServer server) {
        synchronized (SpatialSwapManager.class) {
            if (!active) {
                return;
            }
            active = false;
            anchorWindowTicks = 0;
            ACTIVATED_ANCHORS.clear();
        }

        removeAllAnchors(server);
        synchronized (SpatialSwapManager.class) {
            ORIGINAL_POSITIONS.clear();
            PARTICIPANTS.clear();
            diamondSwapUsed = false;
        }
    }

    public static synchronized void reset() {
        active = false;
        diamondSwapUsed = false;
        anchorWindowTicks = 0;
        ORIGINAL_POSITIONS.clear();
        PARTICIPANTS.clear();
        ACTIVATED_ANCHORS.clear();
    }

    private static void restoreOriginalPositions(MinecraftServer server) {
        Map<UUID, StoredPosition> originals;
        synchronized (SpatialSwapManager.class) {
            if (!active) {
                return;
            }
            originals = new HashMap<>(ORIGINAL_POSITIONS);
        }

        for (Map.Entry<UUID, StoredPosition> entry : originals.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                teleport(server, player, entry.getValue());
            }
        }
        broadcast(server, "Все якоря синхронизированы. Игроки возвращены на исходные позиции.");
        stopEvent(server);
    }

    private static void swapCurrentPositions(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers().stream()
                .filter(player -> isParticipant(player.getUUID()))
                .toList();
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

    private static synchronized boolean isParticipant(UUID id) {
        return active && PARTICIPANTS.contains(id);
    }

    private static int countOnlineParticipants(MinecraftServer server) {
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (PARTICIPANTS.contains(player.getUUID())) {
                count++;
            }
        }
        return count;
    }

    private static int countOnlineActivated(MinecraftServer server) {
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (PARTICIPANTS.contains(player.getUUID()) && ACTIVATED_ANCHORS.contains(player.getUUID())) {
                count++;
            }
        }
        return count;
    }

    private static void giveAnchor(ServerPlayer player) {
        removeAnchors(player);
        ItemStack anchor = new ItemStack(ModItems.SPATIAL_ANCHOR.get());
        if (!player.getInventory().add(anchor.copy())) {
            player.drop(anchor.copy(), false);
        }
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

    private static void broadcast(MinecraftServer server, String text) {
        Component component = Component.literal(PREFIX + text);
        server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(component));
    }

    public static synchronized boolean isActive() {
        return active;
    }

    public static synchronized String getStatusText() {
        if (!active) {
            return "пространственный сдвиг не активен";
        }
        if (anchorWindowTicks > 0) {
            return "окно якорей: " + String.format("%.1f", anchorWindowTicks / 20.0) + " сек.";
        }
        return "пространственный сдвиг активен, якоря ожидают синхронизации";
    }

    private record StoredPosition(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
        static StoredPosition capture(ServerPlayer player) {
            return new StoredPosition(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot());
        }
    }
}
