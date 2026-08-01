package com.kiras.chaosevents.event;

import net.minecraft.server.MinecraftServer;

/** Temporarily triples the whole server simulation speed from the previous tick rate to 60 TPS. */
public enum AcceleratedTimeEvent implements ChaosEvent {
    INSTANCE;

    public static final float ACCELERATED_TICK_RATE = 60.0F;
    private static final int TIMER_TICKS_PER_SECOND = 60;

    private Float previousTickRate;
    private boolean accelerated;

    @Override
    public String id() {
        return "time_acceleration";
    }

    @Override
    public String displayName() {
        return "Ускорение времени";
    }

    @Override
    public boolean harsh() {
        return true;
    }

    @Override
    public boolean isEligible(MinecraftServer server) {
        return !server.getPlayerList().getPlayers().isEmpty();
    }

    @Override
    public synchronized void start(MinecraftServer server) {
        previousTickRate = server.tickRateManager().tickrate();
        applyAcceleration(server);
    }

    @Override
    public void tick(MinecraftServer server, int elapsedTicks, int remainingTicks) {
        // The vanilla server tick-rate manager accelerates every dimension and normal server mechanic.
    }

    @Override
    public synchronized void stop(MinecraftServer server) {
        restorePreviousRate(server);
        previousTickRate = null;
    }

    @Override
    public synchronized void pause(MinecraftServer server) {
        restorePreviousRate(server);
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

    private void applyAcceleration(MinecraftServer server) {
        server.tickRateManager().setTickRate(ACCELERATED_TICK_RATE);
        accelerated = true;
    }

    private void restorePreviousRate(MinecraftServer server) {
        if (!accelerated || previousTickRate == null) {
            return;
        }
        server.tickRateManager().setTickRate(previousTickRate);
        accelerated = false;
    }
}
