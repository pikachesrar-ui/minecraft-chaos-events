package com.kiras.chaosevents.prank;

import com.kiras.chaosevents.network.ChaosNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Independent hidden timer for twenty-four substantial pranks aimed at one random player. */
public final class MicroPrankEngine {
    private static final int TICKS_PER_SECOND = 20;
    private static final int MIN_DELAY_SECONDS = 60;
    private static final int MAX_DELAY_SECONDS = 3 * 60;
    private static final int HELD_ITEM_RETURN_TICKS = 4 * TICKS_PER_SECOND;
    private static final String PREFIX = "[Микроподлянка] ";

    private static final List<PrankType> PRANKS = List.of(PrankType.values());
    private static final List<PendingHeldItem> PENDING_ITEMS = new ArrayList<>();

    private static boolean active;
    private static int ticksUntilNextPrank;
    private static UUID lastTarget;
    private static int consecutiveTargetCount;

    private MicroPrankEngine() {}

    public static synchronized void startSession() {
        active = true;
        PENDING_ITEMS.clear();
        SafeTntPrank.clear();
        lastTarget = null;
        consecutiveTargetCount = 0;
        scheduleNextPrank();
    }

    public static void tick(MinecraftServer server) {
        synchronized (MicroPrankEngine.class) {
            if (!active) return;
            SafeTntPrank.tick(server);
            tickPendingItems(server);
            if (ticksUntilNextPrank > 0) ticksUntilNextPrank--;
            if (ticksUntilNextPrank > 0) return;
        }

        triggerRandomPrank(server);
        synchronized (MicroPrankEngine.class) {
            if (active) scheduleNextPrank();
        }
    }

    public static synchronized void stopSession(MinecraftServer server) {
        restoreAllPendingItems(server);
        SafeTntPrank.clear();
        active = false;
        ticksUntilNextPrank = 0;
        lastTarget = null;
        consecutiveTargetCount = 0;
    }

    public static synchronized void reset() {
        active = false;
        ticksUntilNextPrank = 0;
        PENDING_ITEMS.clear();
        SafeTntPrank.clear();
        lastTarget = null;
        consecutiveTargetCount = 0;
    }

    public static boolean forceRandomPrank(MinecraftServer server) {
        synchronized (MicroPrankEngine.class) {
            if (!active) return false;
        }
        return triggerRandomPrank(server);
    }

    private static boolean triggerRandomPrank(MinecraftServer server) {
        ServerPlayer target = chooseTarget(server);
        if (target == null) return false;

        PrankType prank = PRANKS.get(ThreadLocalRandom.current().nextInt(PRANKS.size()));
        applyPrank(target, prank);
        announcePrank(server, target, prank);
        return true;
    }

    private static synchronized ServerPlayer chooseTarget(MinecraftServer server) {
        List<ServerPlayer> candidates = new ArrayList<>(server.getPlayerList().getPlayers());
        if (candidates.isEmpty()) return null;
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
            case SCREAMER_RAGE -> {
                ChaosNetwork.sendScreamer(player, 0, 24);
                effect(player, MobEffects.DARKNESS, 80, 0);
            }
            case SCREAMER_VOID -> {
                ChaosNetwork.sendScreamer(player, 1, 32);
                effect(player, MobEffects.CONFUSION, 100, 0);
            }
            case TNT_BEHIND -> SafeTntPrank.spawnBehind(player);
            case CREEPER_AMBUSH -> spawnNear(player, EntityType.CREEPER, 2, 3);
            case SILVERFISH_SWARM -> spawnNear(player, EntityType.SILVERFISH, 5, 4);
            case LIGHTNING_STRIKE -> strikeLightning(player);
            case RANDOM_TELEPORT -> randomTeleport(player, 18);
            case VIOLENT_LAUNCH -> player.setDeltaMovement(player.getDeltaMovement().add(
                    ThreadLocalRandom.current().nextDouble(-1.2, 1.2), 1.75,
                    ThreadLocalRandom.current().nextDouble(-1.2, 1.2)));
            case DOWNWARD_SLAM -> player.setDeltaMovement(player.getDeltaMovement().add(0.0, -2.2, 0.0));
            case LEVITATION_DROP -> {
                effect(player, MobEffects.LEVITATION, 50, 3);
                effect(player, MobEffects.SLOW_FALLING, 140, 0);
            }
            case HELD_ITEM_VANISH -> hideHeldItem(player);
            case HOTBAR_SHUFFLE -> shuffleHotbar(player);
            case HAND_SWAP -> swapHands(player);
            case HUNGER_CRASH -> {
                player.getFoodData().addExhaustion(16.0F);
                effect(player, MobEffects.HUNGER, 180, 3);
            }
            case XP_DRAIN -> {
                player.giveExperiencePoints(-7);
                sound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 0.35F);
            }
            case FIRE_BURST -> {
                player.igniteForSeconds(6.0F);
                player.serverLevel().sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0,
                        player.getZ(), 45, 0.8, 1.0, 0.8, 0.1);
            }
            case DARKNESS_NAUSEA -> {
                effect(player, MobEffects.DARKNESS, 180, 0);
                effect(player, MobEffects.CONFUSION, 180, 1);
            }
            case WITHER_TOUCH -> {
                effect(player, MobEffects.WITHER, 100, 1);
                effect(player, MobEffects.WEAKNESS, 180, 1);
            }
            case SLOW_TRAP -> {
                effect(player, MobEffects.MOVEMENT_SLOWDOWN, 200, 4);
                effect(player, MobEffects.DIG_SLOWDOWN, 200, 3);
            }
            case PHANTOM_ATTACK -> spawnNear(player, EntityType.PHANTOM, 2, 7);
            case ZOMBIE_RING -> spawnNear(player, EntityType.ZOMBIE, 4, 5);
            case ENDERMAN_VISIT -> {
                spawnNear(player, EntityType.ENDERMAN, 2, 5);
                sound(player, SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.2F, 0.65F);
            }
            case INVENTORY_JIGGLE -> {
                rotateHotbar(player);
                swapHands(player);
            }
            case CREEPER_PANIC -> {
                sound(player, SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.5F, 0.75F);
                effect(player, MobEffects.BLINDNESS, 55, 0);
                player.setDeltaMovement(player.getDeltaMovement().add(0.0, 0.8, 0.0));
            }
        }
    }

    private static void announcePrank(MinecraftServer server, ServerPlayer target, PrankType prank) {
        Component message = Component.literal(
                PREFIX + "Игрок " + target.getGameProfile().getName()
                        + " получил подлянку «" + prank.displayName + "»: " + prank.description + "."
        );
        server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(message));
    }

    private static void sound(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        player.serverLevel().playSound(null, player.blockPosition().offset(random.nextInt(-3, 4), 0, random.nextInt(-3, 4)),
                sound, source, volume, pitch);
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
    }

    private static void strikeLightning(ServerPlayer player) {
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(player.serverLevel());
        if (lightning == null) return;
        lightning.moveTo(player.getX(), player.getY(), player.getZ());
        player.serverLevel().addFreshEntity(lightning);
    }

    private static void spawnNear(ServerPlayer player, EntityType<? extends Mob> type, int count, int radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ServerLevel level = player.serverLevel();
        BlockPos base = player.blockPosition();
        for (int i = 0; i < count; i++) {
            Mob mob = type.create(level);
            if (mob == null) continue;
            mob.moveTo(base.getX() + 0.5 + random.nextInt(-radius, radius + 1),
                    base.getY() + 1.0,
                    base.getZ() + 0.5 + random.nextInt(-radius, radius + 1),
                    random.nextFloat() * 360.0F, 0.0F);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
        }
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

    private static void rotateHotbar(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        ItemStack last = inventory.getItem(8).copy();
        for (int slot = 8; slot > 0; slot--) inventory.setItem(slot, inventory.getItem(slot - 1).copy());
        inventory.setItem(0, last);
        inventory.setChanged();
    }

    private static void swapHands(ServerPlayer player) {
        ItemStack main = player.getMainHandItem().copy();
        ItemStack off = player.getOffhandItem().copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, off);
        player.setItemInHand(InteractionHand.OFF_HAND, main);
    }

    private static synchronized void hideHeldItem(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            sound(player, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
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
        for (PendingHeldItem pending : PENDING_ITEMS) restoreItem(server, pending);
        PENDING_ITEMS.clear();
    }

    private static void restoreItem(MinecraftServer server, PendingHeldItem pending) {
        ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId);
        if (player == null) return;
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, pending.stack.copy());
        } else if (!player.getInventory().add(pending.stack.copy())) {
            player.drop(pending.stack.copy(), false);
        }
    }

    private static void scheduleNextPrank() {
        ticksUntilNextPrank = ThreadLocalRandom.current().nextInt(MIN_DELAY_SECONDS, MAX_DELAY_SECONDS + 1)
                * TICKS_PER_SECOND;
    }

    public static synchronized int getSecondsUntilNextPrank() {
        if (!active || ticksUntilNextPrank <= 0) return 0;
        return (ticksUntilNextPrank + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
    }

    public static synchronized String getStatusText() {
        if (!active) return "микроподлянки остановлены";
        int totalSeconds = getSecondsUntilNextPrank();
        return String.format("следующая микроподлянка примерно через %d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    public static int getRegisteredPrankCount() { return PRANKS.size(); }

    private enum PrankType {
        SCREAMER_RAGE("Яростный скример", "на экране внезапно появился скример"),
        SCREAMER_VOID("Скример Бездны", "Бездна резко захватила экран"),
        TNT_BEHIND("TNT за спиной", "за спиной появился безопасный TNT"),
        CREEPER_AMBUSH("Засада криперов", "рядом появились два крипера"),
        SILVERFISH_SWARM("Рой чешуйниц", "вокруг появился рой чешуйниц"),
        LIGHTNING_STRIKE("Удар молнии", "в игрока ударила молния"),
        RANDOM_TELEPORT("Случайный телепорт", "игрока переместило в случайную точку"),
        VIOLENT_LAUNCH("Сильный толчок", "игрока резко подбросило"),
        DOWNWARD_SLAM("Удар вниз", "игрока резко потянуло к земле"),
        LEVITATION_DROP("Левитационный сбой", "игрок взлетел и начал падать"),
        HELD_ITEM_VANISH("Исчезнувший предмет", "предмет в руке временно пропал"),
        HOTBAR_SHUFFLE("Перемешанный хотбар", "слоты хотбара перемешались"),
        HAND_SWAP("Обмен рук", "предметы в руках поменялись местами"),
        HUNGER_CRASH("Приступ голода", "сытость резко уменьшилась"),
        XP_DRAIN("Кража опыта", "часть опыта исчезла"),
        FIRE_BURST("Вспышка огня", "игрок внезапно загорелся"),
        DARKNESS_NAUSEA("Тьма и тошнота", "игрок потерял ориентацию"),
        WITHER_TOUCH("Касание иссушения", "на игрока наложилось иссушение"),
        SLOW_TRAP("Ловушка замедления", "движение и добыча сильно замедлились"),
        PHANTOM_ATTACK("Атака фантомов", "рядом появились фантомы"),
        ZOMBIE_RING("Кольцо зомби", "игрока окружили зомби"),
        ENDERMAN_VISIT("Визит эндерменов", "рядом появились эндермены"),
        INVENTORY_JIGGLE("Инвентарная встряска", "хотбар и руки перемешались"),
        CREEPER_PANIC("Крипер-паника", "раздалось шипение и экран ослеп");

        private final String displayName;
        private final String description;

        PrankType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
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
