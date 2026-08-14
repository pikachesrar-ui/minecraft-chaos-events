package com.kiras.chaosevents.event;

import com.kiras.chaosevents.integration.PlacesRealitySlipManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Runs the server world at 200 TPS while allowing players and their ridden
 * entities to tick only once per ten server ticks. This keeps player movement,
 * cooldowns and survival mechanics near normal 20 TPS while the rest of the
 * loaded world advances ten times faster.
 */
public enum AcceleratedTimeEvent implements ChaosEvent {
    INSTANCE;

    public static final int WORLD_SPEED_MULTIPLIER = 10;
    public static final float ACCELERATED_TICK_RATE = 20.0F * WORLD_SPEED_MULTIPLIER;
    private static final int TIMER_TICKS_PER_SECOND = (int) ACCELERATED_TICK_RATE;

    private Float previousTickRate;
    private boolean accelerated;
    private int acceleratedTickIndex;
    private boolean normalSpeedTick;

    @Override
    public String id() {
        return "time_acceleration";
    }

    @Override
    public String displayName() {
        return "Ускорение мира";
    }

    @Override
    public boolean harsh() {
        return true;
    }

    @Override
    public boolean isEligible(MinecraftServer server) {
        return BigEventPlayerPolicy.hasEligiblePlayer(server);
    }

    @Override
    public synchronized void start(MinecraftServer server) {
        previousTickRate = server.tickRateManager().tickrate();
        applyAcceleration(server);
    }

    @Override
    public void tick(MinecraftServer server, int elapsedTicks, int remainingTicks) {
        // Vanilla's tick-rate manager advances blocks, block entities, weather,
        // time, redstone and ordinary entities at the accelerated server rate.
    }

    @Override
    public synchronized void stop(MinecraftServer server) {
        restorePreviousRate(server);
        previousTickRate = null;
        resetTickGate();
    }

    @Override
    public synchronized void pause(MinecraftServer server) {
        restorePreviousRate(server);
        resetTickGate();
    }

    @Override
    public synchronized void resume(MinecraftServer server) {
        if (previousTickRate != null) {
            applyAcceleration(server);
        }
    }

    @Override
    public int timerTicksPerSecond() {
        return TIMER_TICKS_PER_SECOND;
    }

    /** Called at the start of each server tick before entities are processed. */
    public synchronized void beginServerTick() {
        if (!accelerated) {
            normalSpeedTick = true;
            return;
        }

        acceleratedTickIndex = (acceleratedTickIndex + 1) % WORLD_SPEED_MULTIPLIER;
        normalSpeedTick = acceleratedTickIndex == 0;
    }

    /**
     * Cancels nine out of ten server-side ticks for players. A vehicle carrying
     * a player is gated as well, otherwise riding a boat, horse or minecart
     * would move the player at ten times normal speed.
     */
    public synchronized boolean shouldCancelNormalSpeedEntityTick(Entity entity) {
        if (!accelerated || entity.level().isClientSide) {
            return false;
        }

        boolean protectedEntity = entity instanceof ServerPlayer
                || carriesServerPlayer(entity)
                || PlacesRealitySlipManager.isPlacesDimension(entity.level());
        return protectedEntity && !normalSpeedTick;
    }

    /** Keeps Chaos Events' own prank and trivia delays in real time. */
    public synchronized boolean shouldTickAuxiliarySystems() {
        return !accelerated || normalSpeedTick;
    }

    public synchronized boolean isAccelerated() {
        return accelerated;
    }

    private void applyAcceleration(MinecraftServer server) {
        server.tickRateManager().setTickRate(ACCELERATED_TICK_RATE);
        accelerated = true;
        resetTickGate();
    }

    private void restorePreviousRate(MinecraftServer server) {
        if (!accelerated || previousTickRate == null) {
            return;
        }
        server.tickRateManager().setTickRate(previousTickRate);
        accelerated = false;
    }

    private void resetTickGate() {
        acceleratedTickIndex = 0;
        normalSpeedTick = true;
    }

    private static boolean carriesServerPlayer(Entity entity) {
        for (Entity passenger : entity.getPassengers()) {
            if (passenger instanceof ServerPlayer || carriesServerPlayer(passenger)) {
                return true;
            }
        }
        return false;
    }
}
