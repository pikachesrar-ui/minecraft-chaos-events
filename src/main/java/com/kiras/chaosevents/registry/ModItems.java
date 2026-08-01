package com.kiras.chaosevents.registry;

import com.kiras.chaosevents.ChaosEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChaosEvents.MODID);

    public static final Supplier<Item> SPATIAL_ANCHOR = ITEMS.registerSimpleItem(
            "spatial_anchor",
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
    );

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
