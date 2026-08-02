package com.kiras.chaosevents.prank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Visual TNT prank with a manual non-destructive blast.
 * It cannot destroy blocks, damage entities or apply knockback.
 */
final class SafeTntPrank {
    private static final int MANUAL_FUSE_TICKS = 55;
    private static final int VANILLA_FUSE_SAFETY_TICKS = 20 * 60 * 60;
    private static final List<PendingTnt> PENDING = new ArrayList<>();

    private SafeTntPrank() {
    }

    static void spawnBehind(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        PrimedTnt tnt = EntityType.TNT.create(level);
        if (tnt == null) {
            return;
        }

        Vec3 behind = player.position().subtract(player.getLookAngle().scale(2.5));
        tnt.moveTo(behind.x, behind.y + 0.2, behind.z);
        tnt.setFuse(VANILLA_FUSE_SAFETY_TICKS);
        level.addFreshEntity(tnt);
        PENDING.add(new PendingTnt(tnt, MANUAL_FUSE_TICKS));

        level.playSound(null, tnt.blockPosition(), SoundEvents.TNT_PRIMED,
                SoundSource.BLOCKS, 1.1F, 0.85F);
    }

    static void tick(MinecraftServer server) {
        Iterator<PendingTnt> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            PendingTnt pending = iterator.next();
            if (pending.tnt.isRemoved()) {
                iterator.remove();
                continue;
            }

            pending.ticksRemaining--;
            if (pending.ticksRemaining <= 0) {
                detonate(pending);
                iterator.remove();
            }
        }
    }

    static void clear() {
        for (PendingTnt pending : PENDING) {
            if (!pending.tnt.isRemoved()) {
                pending.tnt.discard();
            }
        }
        PENDING.clear();
    }

    private static void detonate(PendingTnt pending) {
        PrimedTnt tnt = pending.tnt;
        if (!(tnt.level() instanceof ServerLevel level)) {
            tnt.discard();
            return;
        }

        double x = tnt.getX();
        double y = tnt.getY();
        double z = tnt.getZ();
        tnt.discard();

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z,
                1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS, 0.9F, 1.15F);
    }

    private static final class PendingTnt {
        private final PrimedTnt tnt;
        private int ticksRemaining;

        private PendingTnt(PrimedTnt tnt, int ticksRemaining) {
            this.tnt = tnt;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
