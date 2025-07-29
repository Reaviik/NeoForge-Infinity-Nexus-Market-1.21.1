package com.Infinity.Nexus.Market.networking.packet;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import com.Infinity.Nexus.Market.networking.ModMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record RequestMarketSyncC2SPacket() implements CustomPacketPayload {
    public static final Type<RequestMarketSyncC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "request_market_sync"));

    public static final StreamCodec<FriendlyByteBuf, RequestMarketSyncC2SPacket> STREAM_CODEC =
            StreamCodec.unit(new RequestMarketSyncC2SPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestMarketSyncC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            if (!(player.level() instanceof ServerLevel serverLevel)) return;

            // Busca todos os itens do mercado diretamente do SQLiteManager
            List<DatabaseManager.MarketItemEntry> marketItems = DatabaseManager.getAllMarketItems();

            // Cria e envia o pacote de sincronização
            var syncPacket = new MarketSalesSyncS2CPacket(
                    MarketSalesSyncS2CPacket.fromMarketItems(marketItems, serverLevel)
            );
            ModMessages.sendToPlayer(syncPacket, player);
        });
    }
}