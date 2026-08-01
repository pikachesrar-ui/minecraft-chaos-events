package com.kiras.chaosevents;

import com.kiras.chaosevents.command.ChaosCommands;
import com.kiras.chaosevents.core.ChaosSessionManager;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(ChaosEvents.MODID)
public final class ChaosEvents {

    public static final String MODID = "chaosevents";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ChaosEvents(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(ChaosCommands::onRegisterCommands);

        LOGGER.info("Chaos Events loaded");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ChaosSessionManager.reset();
        LOGGER.info("Chaos Events: server started; waiting for /chaos start");
    }
}
