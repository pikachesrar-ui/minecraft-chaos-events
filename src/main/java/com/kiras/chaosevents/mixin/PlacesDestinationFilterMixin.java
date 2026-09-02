package com.kiras.chaosevents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Removes the deliberately bland Places warp tunnel from Chaos-managed random slips. */
@Mixin(targets = "com.kiras.chaosevents.integration.PlacesRealitySlipManager", remap = false)
public abstract class PlacesDestinationFilterMixin {
    private static final String DISABLED_DESTINATION = "WARP_TUNNEL";

    @Redirect(
            method = "chooseDestination",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z")
    )
    private static boolean chaos$skipWarpTunnel(List<Object> destinations, Object destination) {
        if (destination instanceof Enum<?> enumValue
                && DISABLED_DESTINATION.equals(enumValue.name())) {
            return false;
        }
        return destinations.add(destination);
    }
}
