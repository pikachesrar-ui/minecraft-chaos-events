package com.kiras.chaosevents.prank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Server-side helpers for reversible and short-lived prank mechanics. */
final class ExpandedPrankEffects {
    private static final int FAKE_TELEPORT_TICKS = 55;
    private static final int TEMPORARY_BLOCK_TICKS = 90;
    private static final List<PendingTeleport> PENDING_TELEPORTS = new ArrayList<>();
    private static final List<TemporaryBlock> TEMPORARY_BLOCKS = new ArrayList<>();
    private static final List<Item> ORES = List.of(
            Items.COAL, Items.RAW_IRON, Items.RAW_COPPER, Items.RAW_GOLD,
            Items.REDSTONE, Items.LAPIS_LAZULI, Items.QUARTZ, Items.EMERALD
    );

    private ExpandedPrankEffects() {
    }

    static synchronized void tick(MinecraftServer server) {
        Iterator<PendingTeleport> teleportIterator = PENDING_TELEPORTS.iterator();
        while (teleportIterator.hasNext()) {
            PendingTeleport pending = teleportIterator.next();
            pending.ticksRemaining--;
            if (pending.ticksRemaining <= 0) {
                restoreTeleport(server, pending);
                teleportIterator.remove();
            }
        }

        Iterator<TemporaryBlock> blockIterator = TEMPORARY_BLOCKS.iterator();
        while (blockIterator.hasNext()) {
            TemporaryBlock pending = blockIterator.next();
            pending.ticksRemaining--;
            if (pending.ticksRemaining <= 0) {
                restoreBlock(server, pending);
                blockIterator.remove();
            }
        }
    }

    static synchronized void clear(MinecraftServer server) {
        for (PendingTeleport pending : PENDING_TELEPORTS) restoreTeleport(server, pending);
        for (TemporaryBlock pending : TEMPORARY_BLOCKS) restoreBlock(server, pending);
        PENDING_TELEPORTS.clear();
        TEMPORARY_BLOCKS.clear();
    }

    static synchronized void reset() {
        PENDING_TELEPORTS.clear();
        TEMPORARY_BLOCKS.clear();
    }

    static synchronized void fakeTeleport(ServerPlayer player, boolean driftAfterReturn) {
        PENDING_TELEPORTS.removeIf(pending -> pending.playerId.equals(player.getUUID()));
        StoredPosition original = StoredPosition.capture(player);
        ServerLevel level = player.serverLevel();
        double targetY = Math.min(level.getMaxBuildHeight() - 6.0,
                Math.max(player.getY() + 34.0, level.getSeaLevel() + 36.0));
        player.teleportTo(level, player.getX(), targetY, player.getZ(), Set.<RelativeMovement>of(),
                player.getYRot(), 88.0F);
        effect(player, MobEffects.SLOW_FALLING, 180, 0);
        effect(player, MobEffects.DAMAGE_RESISTANCE, 180, 4);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.2F, 0.55F);
        PENDING_TELEPORTS.add(new PendingTeleport(player.getUUID(), original, driftAfterReturn, FAKE_TELEPORT_TICKS));
    }

    static void hauntedContainers(ServerPlayer player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 7; i++) {
            BlockPos pos = player.blockPosition().offset(random.nextInt(-9, 10), random.nextInt(-2, 4), random.nextInt(-9, 10));
            SoundEvent sound = random.nextBoolean() ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE;
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.25F, random.nextFloat(0.55F, 1.25F));
        }
    }

    static synchronized void cobwebSnare(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos base = player.blockPosition();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int placed = 0;
        for (int attempt = 0; attempt < 10 && placed < 4; attempt++) {
            BlockPos pos = base.offset(random.nextInt(-2, 3), random.nextInt(0, 2), random.nextInt(-2, 3));
            BlockState original = level.getBlockState(pos);
            if (!original.isAir()) continue;
            BlockState web = Blocks.COBWEB.defaultBlockState();
            if (level.setBlockAndUpdate(pos, web)) {
                TEMPORARY_BLOCKS.add(new TemporaryBlock(level.dimension(), pos.immutable(), original, web,
                        TEMPORARY_BLOCK_TICKS));
                placed++;
            }
        }
    }

    static void dropHeldItem(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return;
        ItemStack dropped = held.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.drop(dropped, false);
    }

    static void giveWaterBucket(ServerPlayer player) {
        giveOrDrop(player, new ItemStack(Items.WATER_BUCKET));
    }

    static void giveRandomOre(ServerPlayer player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Item item = ORES.get(random.nextInt(ORES.size()));
        giveOrDrop(player, new ItemStack(item, random.nextInt(1, 5)));
    }

    static void repairHeldItem(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!held.isDamageableItem()) return;
        held.setDamageValue(Math.max(0, held.getDamageValue() - Math.max(1, held.getMaxDamage() / 3)));
    }

    static void damageHeldItem(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!held.isDamageableItem()) return;
        held.setDamageValue(Math.min(held.getMaxDamage() - 1,
                held.getDamageValue() + Math.max(1, held.getMaxDamage() / 5)));
    }

    static void forceMount(ServerPlayer player) {
        if (player.isPassenger()) return;
        ServerLevel level = player.serverLevel();
        Horse horse = EntityType.HORSE.create(level);
        if (horse == null) return;
        horse.moveTo(player.getX() + 1.0, player.getY(), player.getZ() + 1.0, player.getYRot(), 0.0F);
        horse.setPersistenceRequired();
        level.addFreshEntity(horse);
        player.startRiding(horse, true);
    }

    static void rainbowSheep(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Sheep sheep = EntityType.SHEEP.create(level);
        if (sheep == null) return;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        sheep.setColor(DyeColor.byId(random.nextInt(16)));
        sheep.moveTo(player.getX() + random.nextDouble(-3.0, 3.0), player.getY(),
                player.getZ() + random.nextDouble(-3.0, 3.0), random.nextFloat() * 360.0F, 0.0F);
        sheep.setPersistenceRequired();
        level.addFreshEntity(sheep);
    }

    static void experienceBurst(ServerPlayer player) {
        ExperienceOrb.award(player.serverLevel(), player.position().add(0.0, 1.0, 0.0),
                ThreadLocalRandom.current().nextInt(8, 31));
    }

    static void flingNearbyEntities(ServerPlayer player) {
        for (Entity entity : nearbyEntities(player, 12.0)) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(
                    ThreadLocalRandom.current().nextDouble(-0.9, 0.9),
                    ThreadLocalRandom.current().nextDouble(1.1, 1.9),
                    ThreadLocalRandom.current().nextDouble(-0.9, 0.9)
            ));
        }
    }

    static void warpNearbyEntities(ServerPlayer player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Entity entity : nearbyEntities(player, 14.0)) {
            entity.teleportTo(player.getX() + random.nextDouble(-3.0, 3.0),
                    player.getY() + random.nextDouble(0.0, 2.0),
                    player.getZ() + random.nextDouble(-3.0, 3.0));
        }
    }

    static void invisiblePlayer(ServerPlayer player) {
        effect(player, MobEffects.INVISIBILITY, 20 * 20, 0);
    }

    static void onePunch(ServerPlayer player) {
        effect(player, MobEffects.DAMAGE_BOOST, 8 * 20, 9);
    }

    static void totalHeal(ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(10.0F);
        effect(player, MobEffects.REGENERATION, 8 * 20, 1);
    }

    private static List<Entity> nearbyEntities(ServerPlayer player, double radius) {
        AABB box = player.getBoundingBox().inflate(radius);
        return player.serverLevel().getEntities(player, box,
                entity -> entity.isAlive() && !(entity instanceof ServerPlayer));
    }

    private static void restoreTeleport(MinecraftServer server, PendingTeleport pending) {
        ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId);
        if (player == null) return;
        ServerLevel target = server.getLevel(pending.original.dimension);
        if (target == null) return;
        StoredPosition pos = pending.original;
        player.teleportTo(target, pos.x, pos.y, pos.z, Set.<RelativeMovement>of(), pos.yaw, pos.pitch);
        if (pending.driftAfterReturn) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            player.randomTeleport(pos.x + random.nextInt(-12, 13), pos.y + random.nextInt(-2, 4),
                    pos.z + random.nextInt(-12, 13), true);
        }
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0F, 1.25F);
    }

    private static void restoreBlock(MinecraftServer server, TemporaryBlock pending) {
        ServerLevel level = server.getLevel(pending.dimension);
        if (level == null) return;
        if (level.getBlockState(pending.pos).equals(pending.placed)) {
            level.setBlockAndUpdate(pending.pos, pending.original);
        }
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack.copy())) player.drop(stack.copy(), false);
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
    }

    private record StoredPosition(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
        private static StoredPosition capture(ServerPlayer player) {
            return new StoredPosition(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot());
        }
    }

    private static final class PendingTeleport {
        private final UUID playerId;
        private final StoredPosition original;
        private final boolean driftAfterReturn;
        private int ticksRemaining;

        private PendingTeleport(UUID playerId, StoredPosition original, boolean driftAfterReturn, int ticksRemaining) {
            this.playerId = playerId;
            this.original = original;
            this.driftAfterReturn = driftAfterReturn;
            this.ticksRemaining = ticksRemaining;
        }
    }

    private static final class TemporaryBlock {
        private final ResourceKey<Level> dimension;
        private final BlockPos pos;
        private final BlockState original;
        private final BlockState placed;
        private int ticksRemaining;

        private TemporaryBlock(ResourceKey<Level> dimension, BlockPos pos, BlockState original,
                               BlockState placed, int ticksRemaining) {
            this.dimension = dimension;
            this.pos = pos;
            this.original = original;
            this.placed = placed;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
