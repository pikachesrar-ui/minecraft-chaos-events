package com.kiras.chaosevents;

import com.kiras.chaosevents.command.ChaosCommands;
import com.kiras.chaosevents.core.ChaosSessionManager;
import com.kiras.chaosevents.event.BigEventEngine;
import com.kiras.chaosevents.network.ChaosNetwork;
import com.kiras.chaosevents.prank.MicroPrankEngine;
import com.kiras.chaosevents.registry.ModItems;
import com.kiras.chaosevents.registry.ModSounds;
import com.kiras.chaosevents.spatial.SpatialSwapManager;
import com.kiras.chaosevents.trivia.TriviaEngine;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(ChaosEvents.MODID)
public final class ChaosEvents {
    public static final String MODID = "chaosevents";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ChaosEvents(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModSounds.register(modEventBus);
        modEventBus.addListener(ChaosNetwork::register);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(ChaosCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);

        LOGGER.info("Chaos Events loaded: {} big events, {} micro pranks, {} trivia questions",
                BigEventEngine.getRegisteredEventCount(),
                MicroPrankEngine.getRegisteredPrankCount(),
                TriviaEngine.getQuestionCount());
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

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        MinecraftServer server = event.getPlayer().getServer();
        if (server != null && ChaosSessionManager.isRunning()
                && TriviaEngine.handleChatAnswer(server, event.getPlayer(), event.getRawText())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && player.getServer() != null) {
            SpatialSwapManager.onBlockBroken(player.getServer(), player, event.getState());
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        if (!player.getItemInHand(event.getHand()).is(ModItems.SPATIAL_ANCHOR.get())) {
            return;
        }

        InteractionResult result = SpatialSwapManager.activateAnchor(player.getServer(), player, event.getHand());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private void onServerTick(ServerTickEvent.Post event) {
        ChaosSessionManager.tick(event.getServer());
    }
}
