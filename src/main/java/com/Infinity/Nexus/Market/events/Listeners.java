package com.Infinity.Nexus.Market.events;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.sql.SQLException;

@EventBusSubscriber(modid = InfinityNexusMarket.MOD_ID)
public class Listeners {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
    }
    @SubscribeEvent
    private static void onServerStarted(ServerStartedEvent event) throws SQLException {
        InfinityNexusMarket.serverLevel = event.getServer().getLevel(Level.OVERWORLD);

        InfinityNexusMarket.LOGGER.info("§aInicializando sistema SQLite...");
        DatabaseManager.initialize();
        ModEvents.anunciarTopBalances(event.getServer());
    }
}