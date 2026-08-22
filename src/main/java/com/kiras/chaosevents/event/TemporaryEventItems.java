package com.kiras.chaosevents.event;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

/** Marks and removes items created by one active large-event run. */
public final class TemporaryEventItems {
    private static final String RUN_TAG = "chaosevents_temporary_event_run";
    private static final String EVENT_TAG = "chaosevents_temporary_event_id";

    private static ActiveRun activeRun;

    private TemporaryEventItems() {}

    public static synchronized void begin(String eventId) {
        activeRun = new ActiveRun(eventId, UUID.randomUUID().toString());
    }

    public static synchronized ItemStack mark(ItemStack stack) {
        if (stack.isEmpty() || activeRun == null) return stack;
        ActiveRun run = activeRun;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(RUN_TAG, run.token());
            tag.putString(EVENT_TAG, run.eventId());
        });
        return stack;
    }

    public static void finish(MinecraftServer server, String eventId) {
        ActiveRun finished;
        synchronized (TemporaryEventItems.class) {
            if (activeRun == null || !activeRun.eventId().equals(eventId)) return;
            finished = activeRun;
            activeRun = null;
        }
        removeRun(server, finished.token());
    }

    public static void removeFromPlayerForActiveEvent(ServerPlayer player, String eventId) {
        String token;
        synchronized (TemporaryEventItems.class) {
            if (activeRun == null || !activeRun.eventId().equals(eventId)) return;
            token = activeRun.token();
        }
        removeFromPlayer(player, stack -> hasRun(stack, token));
    }

    /** Removes old tagged items after a restart or when a hidden container is opened later. */
    public static void purgeInactive(ServerPlayer player) {
        removeFromPlayer(player, TemporaryEventItems::isInactive);
    }

    public static void purgeInactive(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            purgeInactive(player);
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (ItemEntity item : level.getEntities(EntityType.ITEM,
                    entity -> isInactive(entity.getItem()))) {
                item.discard();
            }
        }
    }

    public static void purgeContainer(AbstractContainerMenu menu) {
        boolean changed = false;
        for (Slot slot : menu.slots) {
            if (isInactive(slot.getItem())) {
                slot.set(ItemStack.EMPTY);
                changed = true;
            }
        }
        if (isInactive(menu.getCarried())) {
            menu.setCarried(ItemStack.EMPTY);
            changed = true;
        }
        if (changed) menu.broadcastChanges();
    }

    public static void discardIfInactive(ItemEntity itemEntity) {
        if (isInactive(itemEntity.getItem())) itemEntity.discard();
    }

    public static synchronized void invalidate() {
        activeRun = null;
    }

    private static void removeRun(MinecraftServer server, String token) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            removeFromPlayer(player, stack -> hasRun(stack, token));
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (ItemEntity item : level.getEntities(EntityType.ITEM,
                    entity -> hasRun(entity.getItem(), token))) {
                item.discard();
            }
        }
    }

    private static void removeFromPlayer(ServerPlayer player, StackPredicate predicate) {
        removeFromContainer(player.getInventory(), predicate);
        removeFromContainer(player.getEnderChestInventory(), predicate);

        AbstractContainerMenu menu = player.containerMenu;
        boolean changed = false;
        for (Slot slot : menu.slots) {
            if (predicate.test(slot.getItem())) {
                slot.set(ItemStack.EMPTY);
                changed = true;
            }
        }
        if (predicate.test(menu.getCarried())) {
            menu.setCarried(ItemStack.EMPTY);
            changed = true;
        }
        if (changed) menu.broadcastChanges();
    }

    private static void removeFromContainer(Container container, StackPredicate predicate) {
        boolean changed = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (predicate.test(container.getItem(slot))) {
                container.setItem(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) container.setChanged();
    }

    private static boolean isInactive(ItemStack stack) {
        String token = getRunToken(stack);
        if (token == null) return false;
        synchronized (TemporaryEventItems.class) {
            return activeRun == null || !activeRun.token().equals(token);
        }
    }

    private static boolean hasRun(ItemStack stack, String token) {
        return token.equals(getRunToken(stack));
    }

    private static String getRunToken(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        String token = data.copyTag().getString(RUN_TAG);
        return token.isBlank() ? null : token;
    }

    @FunctionalInterface
    private interface StackPredicate {
        boolean test(ItemStack stack);
    }

    private record ActiveRun(String eventId, String token) {}
}
