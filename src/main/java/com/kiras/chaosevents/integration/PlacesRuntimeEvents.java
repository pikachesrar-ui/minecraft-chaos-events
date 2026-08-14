package com.kiras.chaosevents.integration;

import com.kiras.chaosevents.ChaosEvents;
import com.kiras.chaosevents.core.ChaosSessionManager;
import com.kiras.chaosevents.event.AcceleratedTimeEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Keeps the optional Places bridge aligned with the public Chaos Events session state. */
@EventBusSubscriber(modid = ChaosEvents.MODID)
public final class PlacesRuntimeEvents {
    private static ChaosSessionManager.State lastState = ChaosSessionManager.State.STOPPED;

    private PlacesRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        PlacesRealitySlipManager.reset();
        lastState = ChaosSessionManager.State.STOPPED;
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PlacesRealitySlipManager.stopSession(event.getServer());
        lastState = ChaosSessionManager.State.STOPPED;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ChaosSessionManager.State state = ChaosSessionManager.getState();

        if (state == ChaosSessionManager.State.STOPPED) {
            if (lastState != ChaosSessionManager.State.STOPPED) {
                PlacesRealitySlipManager.stopSession(event.getServer());
            }
            lastState = state;
            return;
        }

        if (lastState == ChaosSessionManager.State.STOPPED) {
            PlacesRealitySlipManager.startSession();
        }

        if (state == ChaosSessionManager.State.RUNNING
                && AcceleratedTimeEvent.INSTANCE.shouldTickAuxiliarySystems()) {
            PlacesRealitySlipManager.tick(event.getServer());
        } else {
            // Returning a player after 5-10 real minutes is a safety mechanic, not a chaos timer.
            PlacesRealitySlipManager.tickPendingReturns(event.getServer());
        }

        lastState = state;
    }
}
