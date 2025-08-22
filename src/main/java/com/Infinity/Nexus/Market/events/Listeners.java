package com.Infinity.Nexus.Market.events;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import net.minecraft.client.gui.screens.social.PlayerEntry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.sql.SQLException;

@EventBusSubscriber(modid = InfinityNexusMarket.MOD_ID)
public class Listeners {
    @SubscribeEvent
    private static void onServerStarted(ServerStartedEvent event) throws SQLException {
        InfinityNexusMarket.serverLevel = event.getServer().getLevel(Level.OVERWORLD);
        DatabaseManager.initialize();
        ModEvents.anunciarTopBalances(event.getServer());
    }

    @SubscribeEvent
    private static void onPlayerJoinServer(PlayerEvent.PlayerLoggedInEvent event){
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DatabaseManager.applyFirstAccountAmount(serverPlayer);
        }
    }
}