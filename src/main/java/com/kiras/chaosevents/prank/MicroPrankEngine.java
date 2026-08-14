package com.kiras.chaosevents.prank;

import com.kiras.chaosevents.config.ChaosConfigCategory;
import com.kiras.chaosevents.config.ChaosConfigEntry;
import com.kiras.chaosevents.config.ChaosConfigManager;
import com.kiras.chaosevents.network.ChaosNetwork;
import net.minecraft.core.Holder;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Independent hidden timer for disruptive but nonlethal pranks aimed at one random player. */
public final class MicroPrankEngine {
    private static final int TICKS_PER_SECOND = 20;
    private static final int HELD_ITEM_RETURN_TICKS = 4 * TICKS_PER_SECOND;
    private static final int SCREAMER_DURATION_TICKS = 3 * TICKS_PER_SECOND;
    private static final String PREFIX = "[Микроподлянка] ";

    private static final List<PrankType> PRANKS = List.of(PrankType.values());
    private static final Set<PrankType> USED_PRANKS = EnumSet.noneOf(PrankType.class);
    private static final List<PendingHeldItem> PENDING_ITEMS = new ArrayList<>();

    private static boolean active;
    private static int ticksUntilNextPrank;
    private static UUID lastTarget;
    private static int consecutiveTargetCount;
    private static PrankType lastPrank;

    private MicroPrankEngine() {}

    public static synchronized void startSession() {
        active = true;
        PENDING_ITEMS.clear();
        USED_PRANKS.clear();
        SafeTntPrank.clear();
        ExpandedPrankEffects.reset();
        lastTarget = null;
        consecutiveTargetCount = 0;
        lastPrank = null;
        scheduleNextPrank();
    }

    public static void tick(MinecraftServer server) {
        synchronized (MicroPrankEngine.class) {
            if (!active) return;
            SafeTntPrank.tick(server);
            ExpandedPrankEffects.tick(server);
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
        ExpandedPrankEffects.clear(server);
        active = false;
        ticksUntilNextPrank = 0;
        lastTarget = null;
        consecutiveTargetCount = 0;
        lastPrank = null;
        USED_PRANKS.clear();
    }

    public static synchronized void reset() {
        active = false;
        ticksUntilNextPrank = 0;
        PENDING_ITEMS.clear();
        USED_PRANKS.clear();
        SafeTntPrank.clear();
        ExpandedPrankEffects.reset();
        lastTarget = null;
        consecutiveTargetCount = 0;
        lastPrank = null;
    }

    public static boolean forceRandomPrank(MinecraftServer server) {
        synchronized (MicroPrankEngine.class) {
            if (!active) return false;
        }
        return triggerRandomPrank(server);
    }

    /** Test helper that works even when the normal chaos session is stopped. */
    public static boolean forceScreamer(MinecraftServer server, ServerPlayer preferredTarget) {
        ServerPlayer target = preferredTarget;
        if (target == null || server.getPlayerList().getPlayer(target.getUUID()) == null) {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) return false;
            target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        }

        ChaosNetwork.sendScreamer(target, 0, SCREAMER_DURATION_TICKS);
        return true;
    }

    private static boolean triggerRandomPrank(MinecraftServer server) {
        ServerPlayer target = chooseTarget(server);
        if (target == null) return false;

        PrankType prank = choosePrank();
        if (prank == null) return false;
        applyPrank(target, prank);
        announcePrank(server, target, prank);
        return true;
    }

    private static synchronized PrankType choosePrank() {
        List<PrankType> enabled = PRANKS.stream()
                .filter(prank -> ChaosConfigManager.isEnabled(ChaosConfigCategory.PRANK, prank.id()))
                .toList();
        if (enabled.isEmpty()) return null;

        USED_PRANKS.removeIf(prank -> !enabled.contains(prank));
        if (enabled.size() == 1) {
            lastPrank = enabled.getFirst();
            USED_PRANKS.add(lastPrank);
            return lastPrank;
        }

        boolean newCycle = enabled.stream().allMatch(USED_PRANKS::contains);
        if (newCycle) USED_PRANKS.clear();

        List<PrankType> candidates = enabled.stream()
                .filter(prank -> !USED_PRANKS.contains(prank))
                .filter(prank -> !newCycle || prank != lastPrank)
                .toList();
        if (candidates.isEmpty()) {
            candidates = enabled.stream().filter(prank -> !USED_PRANKS.contains(prank)).toList();
        }

        PrankType selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        USED_PRANKS.add(selected);
        lastPrank = selected;
        return selected;
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
                ChaosNetwork.sendScreamer(player, 0, SCREAMER_DURATION_TICKS);
                effect(player, MobEffects.DARKNESS, 80, 0);
            }
            case SCREAMER_VOID -> {
                ChaosNetwork.sendScreamer(player, 1, SCREAMER_DURATION_TICKS);
                effect(player, MobEffects.CONFUSION, 100, 0);
            }
            case TNT_BEHIND -> SafeTntPrank.spawnBehind(player);
            case RANDOM_TELEPORT -> randomTeleport(player, 18);
            case LEVITATION_DROP -> {
                effect(player, MobEffects.LEVITATION, 50, 3);
                effect(player, MobEffects.SLOW_FALLING, 160, 0);
                effect(player, MobEffects.DAMAGE_RESISTANCE, 160, 4);
            }
            case HELD_ITEM_VANISH -> hideHeldItem(player);
            case HOTBAR_SHUFFLE -> shuffleHotbar(player);
            case HAND_SWAP -> swapHands(player);
            case HUNGER_CRASH -> {
                int food = player.getFoodData().getFoodLevel();
                player.getFoodData().setFoodLevel(Math.max(6, food - 8));
                player.getFoodData().setSaturation(0.0F);
                effect(player, MobEffects.WEAKNESS, 120, 0);
            }
            case XP_DRAIN -> {
                player.giveExperiencePoints(-7);
                sound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 0.35F);
            }
            case DARKNESS_NAUSEA -> {
                effect(player, MobEffects.DARKNESS, 180, 0);
                effect(player, MobEffects.CONFUSION, 180, 1);
            }
            case SLOW_TRAP -> {
                effect(player, MobEffects.MOVEMENT_SLOWDOWN, 200, 4);
                effect(player, MobEffects.DIG_SLOWDOWN, 200, 3);
            }
            case INVENTORY_JIGGLE -> {
                rotateHotbar(player);
                swapHands(player);
            }
            case CREEPER_PANIC -> {
                sound(player, SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.5F, 0.75F);
                effect(player, MobEffects.BLINDNESS, 55, 0);
                effect(player, MobEffects.CONFUSION, 70, 0);
            }
            case WITHER_BURST -> effect(
                    player,
                    MobEffects.WITHER,
                    ThreadLocalRandom.current().nextInt(5, 11) * TICKS_PER_SECOND,
                    0
            );
            case FAKE_TELEPORT -> ExpandedPrankEffects.fakeTeleport(player, false);
            case FAKE_FAKE_TELEPORT -> ExpandedPrankEffects.fakeTeleport(player, true);
            case HAUNTED_CHESTS -> ExpandedPrankEffects.hauntedContainers(player);
            case COBWEB_SNARE -> ExpandedPrankEffects.cobwebSnare(player);
            case DROP_HAND_ITEM -> ExpandedPrankEffects.dropHeldItem(player);
            case MLG_GIFT -> ExpandedPrankEffects.giveWaterBucket(player);
            case RANDOM_ORE_GIFT -> ExpandedPrankEffects.giveRandomOre(player);
            case HELD_ITEM_REPAIR -> ExpandedPrankEffects.repairHeldItem(player);
            case HELD_ITEM_RUST -> ExpandedPrankEffects.damageHeldItem(player);
            case FORCED_MOUNT -> ExpandedPrankEffects.forceMount(player);
            case RAINBOW_SHEEP_VISIT -> ExpandedPrankEffects.rainbowSheep(player);
            case XP_BURST -> ExpandedPrankEffects.experienceBurst(player);
            case ENTITY_FLING -> ExpandedPrankEffects.flingNearbyEntities(player);
            case INVISIBLE_PLAYER -> ExpandedPrankEffects.invisiblePlayer(player);
            case TOTAL_HEAL -> ExpandedPrankEffects.totalHeal(player);
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
        player.serverLevel().playSound(null,
                player.blockPosition().offset(random.nextInt(-3, 4), 0, random.nextInt(-3, 4)),
                sound, source, volume, pitch);
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
    }

    private static void randomTeleport(ServerPlayer player, int radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double originX = player.getX();
        double originY = player.getY();
        double originZ = player.getZ();

        grantTeleportSafety(player);

        for (int attempt = 0; attempt < 32; attempt++) {
            int offsetX = random.nextInt(-radius, radius + 1);
            int offsetZ = random.nextInt(-radius, radius + 1);
            if (Math.abs(offsetX) < 4 && Math.abs(offsetZ) < 4) continue;

            double targetY = Math.max(player.serverLevel().getMinBuildHeight() + 1,
                    Math.min(player.serverLevel().getMaxBuildHeight() - 2,
                            originY + random.nextInt(-8, 13)));
            if (player.randomTeleport(originX + offsetX + 0.5, targetY,
                    originZ + offsetZ + 0.5, true)) {
                sound(player, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                return;
            }
        }

        ServerLevel level = player.serverLevel();
        int targetX = (int) Math.floor(originX) + random.nextInt(-radius, radius + 1);
        int targetZ = (int) Math.floor(originZ) + random.nextInt(-radius, radius + 1);
        int targetY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
        if (targetY > level.getMinBuildHeight() && targetY < level.getMaxBuildHeight() - 1) {
            player.teleportTo(targetX + 0.5, targetY, targetZ + 0.5);
            sound(player, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void grantTeleportSafety(ServerPlayer player) {
        effect(player, MobEffects.DAMAGE_RESISTANCE, 8 * TICKS_PER_SECOND, 4);
        effect(player, MobEffects.FIRE_RESISTANCE, 8 * TICKS_PER_SECOND, 0);
        effect(player, MobEffects.SLOW_FALLING, 8 * TICKS_PER_SECOND, 0);
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
        int minSeconds = ChaosConfigManager.getMinIntervalSeconds(ChaosConfigCategory.PRANK);
        int maxSeconds = ChaosConfigManager.getMaxIntervalSeconds(ChaosConfigCategory.PRANK);
        ticksUntilNextPrank = ThreadLocalRandom.current().nextInt(minSeconds, maxSeconds + 1)
                * TICKS_PER_SECOND;
    }

    public static synchronized int getSecondsUntilNextPrank() {
        if (!active || ticksUntilNextPrank <= 0) return 0;
        return (ticksUntilNextPrank + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
    }

    public static synchronized String getStatusText() {
        if (!active) return "микроподлянки остановлены";
        int totalSeconds = getSecondsUntilNextPrank();
        return String.format("следующая микроподлянка примерно через %d:%02d",
                totalSeconds / 60, totalSeconds % 60);
    }

    public static int getRegisteredPrankCount() { return PRANKS.size(); }

    public static List<ChaosConfigEntry> getConfigEntries() {
        return PRANKS.stream()
                .map(prank -> new ChaosConfigEntry(prank.id(), prank.displayName, prank.description))
                .toList();
    }

    private enum PrankType {
        SCREAMER_RAGE("Яростный скример", "на экране внезапно появился скример"),
        SCREAMER_VOID("Скример Бездны", "Бездна резко захватила экран"),
        TNT_BEHIND("TNT за спиной", "за спиной появился полностью безопасный TNT"),
        RANDOM_TELEPORT("Случайный телепорт", "игрока переместило в случайную безопасную точку"),
        LEVITATION_DROP("Левитационный сбой", "игрок взлетел с защитой от падения"),
        HELD_ITEM_VANISH("Исчезнувший предмет", "предмет в руке временно пропал"),
        HOTBAR_SHUFFLE("Перемешанный хотбар", "слоты хотбара перемешались"),
        HAND_SWAP("Обмен рук", "предметы в руках поменялись местами"),
        HUNGER_CRASH("Приступ голода", "сытость уменьшилась, но не до уровня голодания"),
        XP_DRAIN("Кража опыта", "часть опыта исчезла"),
        DARKNESS_NAUSEA("Тьма и тошнота", "игрок потерял ориентацию"),
        SLOW_TRAP("Ловушка замедления", "движение и добыча сильно замедлились"),
        INVENTORY_JIGGLE("Инвентарная встряска", "хотбар и руки перемешались"),
        CREEPER_PANIC("Крипер-паника", "раздалось безопасное шипение и экран ослеп"),
        WITHER_BURST("Приступ иссушения", "игрок получил иссушение на 5–10 секунд"),
        FAKE_TELEPORT("Фальшивый телепорт", "игрок на несколько секунд оказался в невозможном месте"),
        FAKE_FAKE_TELEPORT("Двойной обман", "возвращение после телепорта оказалось неточным"),
        HAUNTED_CHESTS("Голодные сундуки", "вокруг начали хлопать невидимые сундуки"),
        COBWEB_SNARE("Паутинный капкан", "рядом возникла временная паутина"),
        DROP_HAND_ITEM("Слабая хватка", "предмет выпал из основной руки"),
        MLG_GIFT("Ведро спасения", "игрок неожиданно получил ведро воды"),
        RANDOM_ORE_GIFT("Случайная руда", "игрок получил немного случайного сырья"),
        HELD_ITEM_REPAIR("Чудесный ремонт", "предмет в руке частично восстановился"),
        HELD_ITEM_RUST("Внезапная ржавчина", "предмет в руке потерял часть прочности"),
        FORCED_MOUNT("Незапланированная поездка", "игрок оказался верхом на лошади"),
        RAINBOW_SHEEP_VISIT("Радужный гость", "рядом появилась разноцветная овца"),
        XP_BURST("Всплеск опыта", "вокруг игрока появились сферы опыта"),
        ENTITY_FLING("Подброс окружения", "ближайшие сущности разлетелись вверх"),
        INVISIBLE_PLAYER("Исчезновение", "игрок временно стал невидимым"),
        TOTAL_HEAL("Неожиданная помощь", "здоровье и голод полностью восстановились");

        private final String displayName;
        private final String description;

        PrankType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        private String id() {
            return name().toLowerCase(Locale.ROOT);
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
