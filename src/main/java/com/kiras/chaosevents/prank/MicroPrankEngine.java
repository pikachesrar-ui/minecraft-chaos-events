package com.kiras.chaosevents.prank;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Independent hidden timer for small pranks aimed at one random player.
 */
public final class MicroPrankEngine {

    private static final int TICKS_PER_SECOND = 20;
    private static final int MIN_DELAY_SECONDS = 60;
    private static final int MAX_DELAY_SECONDS = 3 * 60;
    private static final int HELD_ITEM_RETURN_TICKS = 3 * TICKS_PER_SECOND;

    private static final List<PrankType> PRANKS = List.of(PrankType.values());
    private static final List<PendingHeldItem> PENDING_ITEMS = new ArrayList<>();

    private static boolean active;
    private static int ticksUntilNextPrank;
    private static UUID lastTarget;
    private static int consecutiveTargetCount;

    private MicroPrankEngine() {
    }

    public static synchronized void startSession() {
        active = true;
        PENDING_ITEMS.clear();
        lastTarget = null;
        consecutiveTargetCount = 0;
        scheduleNextPrank();
    }

    public static void tick(MinecraftServer server) {
        synchronized (MicroPrankEngine.class) {
            if (!active) {
                return;
            }

            tickPendingItems(server);

            if (ticksUntilNextPrank > 0) {
                ticksUntilNextPrank--;
            }

            if (ticksUntilNextPrank > 0) {
                return;
            }
        }

        triggerRandomPrank(server);

        synchronized (MicroPrankEngine.class) {
            if (active) {
                scheduleNextPrank();
            }
        }
    }

    public static synchronized void stopSession(MinecraftServer server) {
        restoreAllPendingItems(server);
        active = false;
        ticksUntilNextPrank = 0;
        lastTarget = null;
        consecutiveTargetCount = 0;
    }

    public static synchronized void reset() {
        active = false;
        ticksUntilNextPrank = 0;
        PENDING_ITEMS.clear();
        lastTarget = null;
        consecutiveTargetCount = 0;
    }

    public static boolean forceRandomPrank(MinecraftServer server) {
        synchronized (MicroPrankEngine.class) {
            if (!active) {
                return false;
            }
        }
        return triggerRandomPrank(server);
    }

    private static boolean triggerRandomPrank(MinecraftServer server) {
        ServerPlayer target = chooseTarget(server);
        if (target == null) {
            return false;
        }

        PrankType prank = PRANKS.get(ThreadLocalRandom.current().nextInt(PRANKS.size()));
        applyPrank(target, prank);
        return true;
    }

    private static synchronized ServerPlayer chooseTarget(MinecraftServer server) {
        List<ServerPlayer> candidates = new ArrayList<>(server.getPlayerList().getPlayers());
        if (candidates.isEmpty()) {
            return null;
        }

        if (lastTarget != null && consecutiveTargetCount >= 2 && candidates.size() > 1) {
            candidates.removeIf(player -> player.getUUID().equals(lastTarget));
        }

        ServerPlayer selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        if (selected.getUUID().equals(lastTarget)) {
            consecutiveTargetCount++;
        } else {
            lastTarget = selected.getUUID();
            consecutiveTargetCount = 1;
        }
        return selected;
    }

    private static void applyPrank(ServerPlayer player, PrankType prank) {
        switch (prank) {
            case CREEPER_HISS -> sound(player, SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.0F, 1.0F);
            case TNT_FUSE -> sound(player, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 0.85F);
            case ANVIL_CRASH -> sound(player, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.2F, 0.7F);
            case THUNDER_CLAP -> sound(player, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.5F, 1.1F);
            case GHAST_SCREAM -> sound(player, SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 1.0F, 1.0F);
            case ENDERMAN_STARE -> sound(player, SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.0F, 0.8F);
            case PHANTOM_SWOOP -> sound(player, SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 1.0F, 1.2F);
            case WARDEN_HEARTBEAT -> sound(player, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.2F, 0.8F);
            case DARK_BLINK -> effect(player, MobEffects.DARKNESS, 60, 0);
            case BLIND_BLINK -> effect(player, MobEffects.BLINDNESS, 40, 0);
            case NAUSEA_WAVE -> effect(player, MobEffects.CONFUSION, 100, 0);
            case LEVITATION_BUMP -> effect(player, MobEffects.LEVITATION, 18, 1);
            case SLOW_FEET -> effect(player, MobEffects.MOVEMENT_SLOWDOWN, 80, 2);
            case WEAK_HANDS -> effect(player, MobEffects.DIG_SLOWDOWN, 100, 1);
            case GLOW_MARK -> {
                effect(player, MobEffects.GLOWING, 120, 0);
                player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0,
                        player.getZ(), 10, 0.4, 0.8, 0.4, 0.02);
            }
            case HUNGER_NIBBLE -> player.getFoodData().addExhaustion(2.5F);
            case RANDOM_PUSH -> randomPush(player);
            case HOTBAR_ROTATE -> rotateHotbar(player);
            case HELD_ITEM_VANISH -> hideHeldItem(player);
            case FAKE_ITEM_BREAK -> sound(player, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.9F);
            case PORTAL_WHISPER -> sound(player, SoundEvents.PORTAL_TRAVEL, SoundSource.AMBIENT, 0.8F, 1.4F);
            case FIRE_TICKLE -> player.setSecondsOnFire(1);
            case EXPERIENCE_FAKEOUT -> {
                sound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 0.5F);
                player.giveExperiencePoints(1);
            }
            case INVENTORY_JIGGLE -> swapTwoHotbarSlots(player);
        }
    }

    private static void sound(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int dx = random.nextInt(-3, 4);
        int dz = random.nextInt(-3, 4);
        player.serverLevel().playSound(null, player.blockPosition().offset(dx, 0, dz), sound, source, volume, pitch);
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
    }

    private static void randomPush(ServerPlayer player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 push = new Vec3(
                random.nextDouble(-0.8, 0.8),
                random.nextDouble(0.2, 0.55),
                random.nextDouble(-0.8, 0.8)
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

    private static void swapTwoHotbarSlots(ServerPlayer player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int first = random.nextInt(9);
        int second = random.nextInt(9);
        while (second == first) {
            second = random.nextInt(9);
        }

        Inventory inventory = player.getInventory();
        ItemStack firstStack = inventory.getItem(first).copy();
        ItemStack secondStack = inventory.getItem(second).copy();
        inventory.setItem(first, secondStack);
        inventory.setItem(second, firstStack);
        inventory.setChanged();
    }

    private static synchronized void hideHeldItem(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            sound(player, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
            return;
        }

        ItemStack hidden = held.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        PENDING_ITEMS.add(new PendingHeldItem(player.getUUID(), hidden, HELD_ITEM_RETURN_TICKS));
    }

    private static void tickPendingItems(MinecraftServer server) {
        Iterator<PendingHeldItem> iterator = PENDING_ITEMS.iterator();
        while (iterator.hasNext()) {
            PendingHeldItem pending = iterator.next();
            pending.ticksRemaining--;
            if (pending.ticksRemaining <= 0) {
                restoreItem(server, pending);
                iterator.remove();
            }
        }
    }

    private static void restoreAllPendingItems(MinecraftServer server) {
        for (PendingHeldItem pending : PENDING_ITEMS) {
            restoreItem(server, pending);
        }
        PENDING_ITEMS.clear();
    }

    private static void restoreItem(MinecraftServer server, PendingHeldItem pending) {
        ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId);
        if (player == null) {
            return;
        }

        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, pending.stack.copy());
        } else if (!player.getInventory().add(pending.stack.copy())) {
            player.drop(pending.stack.copy(), false);
        }
    }

    private static void scheduleNextPrank() {
        ticksUntilNextPrank = ThreadLocalRandom.current().nextInt(
                MIN_DELAY_SECONDS,
                MAX_DELAY_SECONDS + 1
        ) * TICKS_PER_SECOND;
    }

    public static synchronized int getSecondsUntilNextPrank() {
        if (!active || ticksUntilNextPrank <= 0) {
            return 0;
        }
        return (ticksUntilNextPrank + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
    }

    public static synchronized String getStatusText() {
        if (!active) {
            return "микроподлянки остановлены";
        }
        int totalSeconds = getSecondsUntilNextPrank();
        return String.format("следующая микроподлянка примерно через %d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    public static int getRegisteredPrankCount() {
        return PRANKS.size();
    }

    private enum PrankType {
        CREEPER_HISS,
        TNT_FUSE,
        ANVIL_CRASH,
        THUNDER_CLAP,
        GHAST_SCREAM,
        ENDERMAN_STARE,
        PHANTOM_SWOOP,
        WARDEN_HEARTBEAT,
        DARK_BLINK,
        BLIND_BLINK,
        NAUSEA_WAVE,
        LEVITATION_BUMP,
        SLOW_FEET,
        WEAK_HANDS,
        GLOW_MARK,
        HUNGER_NIBBLE,
        RANDOM_PUSH,
        HOTBAR_ROTATE,
        HELD_ITEM_VANISH,
        FAKE_ITEM_BREAK,
        PORTAL_WHISPER,
        FIRE_TICKLE,
        EXPERIENCE_FAKEOUT,
        INVENTORY_JIGGLE
    }

    private static final class PendingHeldItem {
        private final UUID playerId;
        private final ItemStack stack;
        private int ticksRemaining;

        private PendingHeldItem(UUID playerId, ItemStack stack, int ticksRemaining) {
            this.playerId = playerId;
            this.stack = stack;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
