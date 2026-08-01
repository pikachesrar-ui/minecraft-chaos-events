package com.kiras.chaosevents.registry;

import com.kiras.chaosevents.ChaosEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public final class ModSounds {
    public static final int SCREAMER_SLOT_COUNT = 10;

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(
            BuiltInRegistries.SOUND_EVENT,
            ChaosEvents.MODID
    );

    private static final List<Supplier<SoundEvent>> SCREAMERS = IntStream
            .rangeClosed(1, SCREAMER_SLOT_COUNT)
            .mapToObj(ModSounds::registerScreamer)
            .toList();

    private ModSounds() {
    }

    private static Supplier<SoundEvent> registerScreamer(int slot) {
        String eventName = "screamer_" + slot;
        return SOUND_EVENTS.register(
                eventName,
                () -> SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(ChaosEvents.MODID, eventName)
                )
        );
    }

    public static SoundEvent screamer(int slot) {
        int safeSlot = Math.max(1, Math.min(SCREAMER_SLOT_COUNT, slot));
        return SCREAMERS.get(safeSlot - 1).get();
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
