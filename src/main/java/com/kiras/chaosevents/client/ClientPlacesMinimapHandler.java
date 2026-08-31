package com.kiras.chaosevents.client;

import com.kiras.chaosevents.ChaosEvents;
import com.kiras.chaosevents.integration.PlacesRealitySlipManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ClientTickEvent;

/** Client-side fallback for Xaero's Minimap when the map mod is not installed on the server. */
@EventBusSubscriber(modid = ChaosEvents.MODID, value = Dist.CLIENT)
public final class ClientPlacesMinimapHandler {
    private static final ResourceKey<MobEffect> XAERO_NO_MINIMAP = ResourceKey.create(
            Registries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath("xaerominimap", "no_minimap")
    );
    private static final int REFRESH_THRESHOLD_TICKS = 20;
    private static final int EFFECT_DURATION_TICKS = 60;

    private static boolean appliedByChaosEvents;

    private ClientPlacesMinimapHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            appliedByChaosEvents = false;
            return;
        }

        Holder<MobEffect> noMinimap = BuiltInRegistries.MOB_EFFECT.get(XAERO_NO_MINIMAP).orElse(null);
        if (noMinimap == null) {
            appliedByChaosEvents = false;
            return;
        }

        if (PlacesRealitySlipManager.isPlacesDimension(player.level())) {
            MobEffectInstance current = player.getEffect(noMinimap);
            if (current == null || current.getDuration() <= REFRESH_THRESHOLD_TICKS) {
                // Keep this effect local and invisible in the vanilla HUD. Xaero only needs the
                // presence of its registered effect in order to suppress minimap rendering.
                player.addEffect(new MobEffectInstance(noMinimap, EFFECT_DURATION_TICKS, 0,
                        true, false, false));
            }
            appliedByChaosEvents = true;
        } else if (appliedByChaosEvents) {
            player.removeEffect(noMinimap);
            appliedByChaosEvents = false;
        }
    }
}
