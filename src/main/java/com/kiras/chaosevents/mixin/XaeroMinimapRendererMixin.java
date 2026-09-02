package com.kiras.chaosevents.mixin;

import com.kiras.chaosevents.integration.PlacesRealitySlipManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional hard fallback for Xaero's Minimap 26.x.
 *
 * <p>The normal integration also applies Xaero's no-minimap effect while the local player is in a
 * Places dimension. Some Xaero builds still render the HUD despite a client-local effect, so this
 * mixin cancels the minimap renderer itself whenever the current dimension namespace is
 * {@code places}. {@link Pseudo} keeps Chaos Events compatible with installations that do not have
 * Xaero's Minimap.</p>
 */
@Pseudo
@Mixin(targets = "xaero.hud.minimap.module.MinimapRenderer", remap = false)
public abstract class XaeroMinimapRendererMixin {

    @Inject(
            method = "render(Lxaero/hud/minimap/module/MinimapSession;Lxaero/hud/render/module/ModuleRenderContext;Lnet/minecraft/client/gui/GuiGraphics;F)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void chaosevents$hideMinimapInPlaces(
            @Coerce Object session,
            @Coerce Object context,
            GuiGraphics guiGraphics,
            float partialTicks,
            CallbackInfo callbackInfo
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && PlacesRealitySlipManager.isPlacesDimension(minecraft.player.level())) {
            callbackInfo.cancel();
        }
    }
}
