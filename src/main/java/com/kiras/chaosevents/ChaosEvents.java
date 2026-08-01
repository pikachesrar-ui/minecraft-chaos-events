package com.kiras.chaosevents;

import com.kiras.chaosevents.command.ChaosCommands;
import com.kiras.chaosevents.core.ChaosSessionManager;
import com.kiras.chaosevents.event.BigEventEngine;
import com.kiras.chaosevents.prank.MicroPrankEngine;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(ChaosEvents.MODID)
public final class ChaosEvents {

    public static final String MODID = "chaosevents";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ChaosEvents(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(ChaosCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);

        LOGGER.info("Chaos Events loaded: {} big events, {} micro pranks",
                BigEventEngine.getRegisteredEventCount(),
                MicroPrankEngine.getRegisteredPrankCount());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ChaosSessionManager.reset();
        LOGGER.info("Chaos Events: server started; waiting for /chaos start");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ChaosSessionManager.shutdown(event.getServer());
        LOGGER.info("Chaos Events: server stopping; active mechanics cleaned up");
    }

    private void onServerTick(ServerTickEvent.Post event) {
        ChaosSessionManager.tick(event.getServer());
    }
}
