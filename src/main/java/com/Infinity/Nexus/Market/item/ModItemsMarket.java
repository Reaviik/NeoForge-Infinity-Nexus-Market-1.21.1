package com.Infinity.Nexus.Market.item;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.block.ModBlocksMarket;
import com.Infinity.Nexus.Market.item.custom.Ticket;
import com.Infinity.Nexus.Market.item.custom.AnimatedBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItemsMarket {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfinityNexusMarket.MOD_ID);
    public static final DeferredItem<Item> TICKET = ITEMS.register("ticket", () -> new Ticket(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> VENDING_MACHINE = ITEMS.register("vending_machine", () -> new AnimatedBlockItem(ModBlocksMarket.VENDING_MACHINE.get(), new Item.Properties(), "vending_machine"));
    public static final DeferredItem<Item> BUYING_MACHINE = ITEMS.register("buying_machine", () -> new AnimatedBlockItem(ModBlocksMarket.BUYING_MACHINE.get(), new Item.Properties(), "buying_machine"));
    public static final DeferredItem<Item> MARKET_MACHINE = ITEMS.register("market_machine", () -> new AnimatedBlockItem(ModBlocksMarket.MARKET_MACHINE.get(), new Item.Properties(), "market_machine"));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}