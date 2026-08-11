package com.kiras.chaosevents.registry;

import com.kiras.chaosevents.ChaosEvents;
import com.kiras.chaosevents.config.ChaosConfigCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    public static final Supplier<Item> CONFIG_BIG_BOOK = ITEMS.registerSimpleItem(
            "config_big_book",
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
    );
    public static final Supplier<Item> CONFIG_PRANK_BOOK = ITEMS.registerSimpleItem(
            "config_prank_book",
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
    );
    public static final Supplier<Item> CONFIG_TRIVIA_BOOK = ITEMS.registerSimpleItem(
            "config_trivia_book",
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
    );
    public static final Supplier<Item> CONFIG_SWAP_BOOK = ITEMS.registerSimpleItem(
            "config_swap_book",
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
    );

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    public static Item configBookItem(ChaosConfigCategory category) {
        return switch (category) {
            case BIG -> CONFIG_BIG_BOOK.get();
            case PRANK -> CONFIG_PRANK_BOOK.get();
            case TRIVIA -> CONFIG_TRIVIA_BOOK.get();
            case SWAP -> CONFIG_SWAP_BOOK.get();
        };
    }

    public static ChaosConfigCategory configCategory(ItemStack stack) {
        if (stack.is(CONFIG_BIG_BOOK.get())) return ChaosConfigCategory.BIG;
        if (stack.is(CONFIG_PRANK_BOOK.get())) return ChaosConfigCategory.PRANK;
        if (stack.is(CONFIG_TRIVIA_BOOK.get())) return ChaosConfigCategory.TRIVIA;
        if (stack.is(CONFIG_SWAP_BOOK.get())) return ChaosConfigCategory.SWAP;
        return null;
    }
}
