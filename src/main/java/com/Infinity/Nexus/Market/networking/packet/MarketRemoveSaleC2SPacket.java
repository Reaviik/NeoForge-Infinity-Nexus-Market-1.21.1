package com.Infinity.Nexus.Market.networking.packet;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.market.SQLiteManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record MarketRemoveSaleC2SPacket(UUID transactionId) implements CustomPacketPayload {
    public static final Type<MarketRemoveSaleC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "market_remove_sale"));

    public static final StreamCodec<FriendlyByteBuf, MarketRemoveSaleC2SPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
            MarketRemoveSaleC2SPacket::transactionId,
            MarketRemoveSaleC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MarketRemoveSaleC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            if (!(player.level() instanceof ServerLevel serverLevel)) return;

            // Sempre usar a Overworld para operações de market
            ServerLevel overworld = serverLevel.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (overworld == null) {
                player.displayClientMessage(Component.translatable("message.infinity_nexus_market.overworld_not_found"), false);
                return;
            }

            // Busca a venda diretamente no SQLiteManager
            SQLiteManager.MarketItemEntry saleEntry = SQLiteManager.getAllPlayerSales().stream()
                    .filter(e -> e.entryId.equals(packet.transactionId().toString()))
                    .findFirst()
                    .orElse(null);

            if (saleEntry == null) {
                player.displayClientMessage(Component.translatable("message.infinity_nexus_market.sale_not_found"), false);
                return;
            }

            // Verifica se o jogador é o vendedor
            if (!player.getUUID().toString().equals(saleEntry.sellerUUID)) {
                player.displayClientMessage(Component.translatable("message.infinity_nexus_market.not_seller"), false);
                return;
            }

            // Remove a venda
            boolean removed = SQLiteManager.removeMarketItem(saleEntry.entryId);
            if (!removed) {
                player.displayClientMessage(Component.translatable("message.infinity_nexus_market.sale_remove_failed"), false);
                return;
            }

            // Devolve o item ao jogador
            ItemStack item = SQLiteManager.deserializeItemStack(saleEntry.itemNbt, overworld);
            if (!item.isEmpty()) {
                player.getInventory().placeItemBackInInventory(item.copy());
            }

            player.displayClientMessage(Component.translatable("message.infinity_nexus_market.sale_removed"), false);
        });
    }
}