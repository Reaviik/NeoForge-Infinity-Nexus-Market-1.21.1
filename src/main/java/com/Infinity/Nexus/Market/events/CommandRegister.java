package com.Infinity.Nexus.Market.events;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.command.MarketCommands;
import com.Infinity.Nexus.Market.command.SQLiteCommands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.server.command.ConfigCommand;

@EventBusSubscriber(modid = InfinityNexusMarket.MOD_ID)
public class CommandRegister {
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        new MarketCommands(event.getDispatcher());
        new SQLiteCommands(event.getDispatcher());
        ConfigCommand.register(event.getDispatcher());
    }
}