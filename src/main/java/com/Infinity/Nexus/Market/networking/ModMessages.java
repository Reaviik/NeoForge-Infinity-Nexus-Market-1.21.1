package com.Infinity.Nexus.Market.networking;

import com.Infinity.Nexus.Market.networking.packet.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModMessages {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1")
                .versioned("1.0")
                .optional();

        registrar.playToServer(
                ToggleWorkC2SPacket.TYPE,
                ToggleWorkC2SPacket.STREAM_CODEC,
                ToggleWorkC2SPacket::handle
        );
        registrar.playToServer(
                UpdateAutoSettingsC2SPacket.TYPE,
                UpdateAutoSettingsC2SPacket.STREAM_CODEC,
                UpdateAutoSettingsC2SPacket::handle
        );
        registrar.playToServer(
                SendAutoSettingsC2SPacket.TYPE,
                SendAutoSettingsC2SPacket.STREAM_CODEC,
                SendAutoSettingsC2SPacket::handle
        );
        registrar.playToServer(
                SellItemC2SPacket.TYPE,
                SellItemC2SPacket.STREAM_CODEC,
                SellItemC2SPacket::handle
        );
        registrar.playToServer(
                BuyItemC2SPacket.TYPE,
                BuyItemC2SPacket.STREAM_CODEC,
                BuyItemC2SPacket::handle
        );
        registrar.playToServer(
                MarketBuyC2SPacket.TYPE,
                MarketBuyC2SPacket.STREAM_CODEC,
                MarketBuyC2SPacket::handle
        );
        registrar.playToServer(
                MarketRemoveSaleC2SPacket.TYPE,
                MarketRemoveSaleC2SPacket.STREAM_CODEC,
                MarketRemoveSaleC2SPacket::handle
        );
        registrar.playToServer(
                CopyToTicketC2SPacket.TYPE,
                CopyToTicketC2SPacket.STREAM_CODEC,
                CopyToTicketC2SPacket::handle
        );
        registrar.playToServer(
                RequestMarketSyncC2SPacket.TYPE,
                RequestMarketSyncC2SPacket.STREAM_CODEC,
                RequestMarketSyncC2SPacket::handle
        );
        registrar.playToClient(
                MarketSalesSyncS2CPacket.TYPE,
                MarketSalesSyncS2CPacket.STREAM_CODEC,
                MarketSalesSyncS2CPacket::handle
        );
    }


    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}