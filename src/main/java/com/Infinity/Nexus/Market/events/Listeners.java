package com.Infinity.Nexus.Market.events;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = InfinityNexusMarket.MOD_ID)
public class Listeners {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
    }
    @SubscribeEvent
    private static void onServerStarted(ServerStartedEvent event) {
        System.out.println("onServerStarting");
        InfinityNexusMarket.serverLevel = event.getServer().getLevel(Level.OVERWORLD);
    }
}