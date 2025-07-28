package com.Infinity.Nexus.Market.item;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.item.custom.Ticket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItemsMarket {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfinityNexusMarket.MOD_ID);
    public static final DeferredItem<Item> TICKET = ITEMS.register("ticket", () -> new Ticket(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.UNCOMMON)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}