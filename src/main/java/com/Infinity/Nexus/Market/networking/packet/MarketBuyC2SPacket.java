package com.Infinity.Nexus.Market.networking.packet;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.market.SQLiteManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record MarketBuyC2SPacket(UUID transactionId, int quantity) implements CustomPacketPayload {
    public static final Type<MarketBuyC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "market_buy"));

    public static final StreamCodec<FriendlyByteBuf, MarketBuyC2SPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
            MarketBuyC2SPacket::transactionId,
            ByteBufCodecs.INT,
            MarketBuyC2SPacket::quantity,
            MarketBuyC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MarketBuyC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            if (!(player.level() instanceof ServerLevel serverLevel)) return;

            // Busca o item no market
            SQLiteManager.MarketItemEntry marketEntry = SQLiteManager.getAllMarketItems().stream()
                    .filter(e -> e.entryId.equals(packet.transactionId().toString()))
                    .findFirst()
                    .orElse(null);

            if (marketEntry == null) {
                player.displayClientMessage(Component.translatable("message.infinity_nexus_market.sale_not_found"), false);
                return;
            }

            // Desserializa o ItemStack
            ItemStack itemStack = SQLiteManager.deserializeItemStack(marketEntry.itemNbt, serverLevel);
            if (itemStack.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.infinity_nexus_market.invalid_item"), false);
                return;
            }

            // Verifica espaço no inventário usando canPlaceItem() em todos os slots
            ItemStack testStack = itemStack.copy();
            testStack.setCount(packet.quantity());
            boolean canPlace = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().canPlaceItem(i, testStack)) {
                    canPlace = true;
                    break;
                }
            }

            if (!canPlace) {
                player.displayClientMessage(Component.translatable("message.infinity_nexus_market.inventory_full"), false);
                return;
            }

            // Restante da lógica...
            UUID SERVER_UUID = UUID.fromString("00000000-0000-0000-0000-00000000c0de");
            boolean isServerItem = "server".equals(marketEntry.type);

            int availableQuantity = isServerItem ? Integer.MAX_VALUE : marketEntry.quantity;
            int quantityToBuy = Math.min(packet.quantity(), availableQuantity);
            double cost = marketEntry.currentPrice * quantityToBuy;

            if (SQLiteManager.getPlayerBalance(player.getUUID().toString()) < cost) {
                player.displayClientMessage(Component.translatable("message.infinity_nexus_market.insufficient_balance"), false);
                return;
            }

            processTransaction(player, marketEntry, quantityToBuy, cost, isServerItem);

            if (!isServerItem) {
                updatePlayerSale(marketEntry, quantityToBuy, serverLevel);
            }

            deliverItem(player, itemStack, quantityToBuy);

            if (isServerItem) {
                SQLiteManager.addSalesHistory(
                        marketEntry.itemNbt,
                        quantityToBuy,
                        marketEntry.currentPrice,
                        SERVER_UUID.toString(),
                        "Server"
                );
            }

            sendPurchaseMessage(player, itemStack, quantityToBuy, cost);
        });
    }

    private static void processTransaction(ServerPlayer player, SQLiteManager.MarketItemEntry entry, int quantity, double cost, boolean isServerItem) {
        // Subtrai saldo do comprador
        SQLiteManager.setPlayerBalance(
                player.getUUID().toString(),
                player.getName().getString(),
                SQLiteManager.getPlayerBalance(player.getUUID().toString()) - cost
        );

        // Atualiza estatísticas do comprador
        SQLiteManager.incrementPlayerStats(
                player.getUUID().toString(),
                cost,    // total gasto
                0.0,     // total ganho
                0,       // total vendas
                1        // total compras
        );

        // Se não for item do servidor, adiciona saldo ao vendedor
        if (!isServerItem && entry.sellerUUID != null) {
            SQLiteManager.setPlayerBalance(
                    entry.sellerUUID,
                    entry.sellerName != null ? entry.sellerName : "Unknown",
                    SQLiteManager.getPlayerBalance(entry.sellerUUID) + cost
            );

            // Atualiza estatísticas do vendedor
            SQLiteManager.incrementPlayerStats(
                    entry.sellerUUID,
                    0.0,     // total gasto
                    cost,    // total ganho
                    1,       // total vendas
                    0        // total compras
            );
        }
    }

    private static void updatePlayerSale(SQLiteManager.MarketItemEntry entry,
                                         int quantityBought, ServerLevel serverLevel) {
        if (quantityBought >= entry.quantity) {
            // Remove completamente se comprou toda a quantidade
            SQLiteManager.removeMarketItem(entry.entryId);
        } else {
            entry.quantity -= quantityBought;
            SQLiteManager.addOrUpdateMarketItem(
                    entry.entryId,
                    "player",
                    entry.sellerUUID,
                    entry.sellerName,
                    SQLiteManager.deserializeItemStack(entry.itemNbt, serverLevel),
                    entry.quantity,
                    entry.basePrice,
                    entry.currentPrice,
                    serverLevel
            );
        }
    }

    private static void deliverItem(ServerPlayer player, ItemStack itemStack, int quantity) {
        ItemStack toDeliver = itemStack.copy();
        toDeliver.setCount(quantity);
        if (!player.getInventory().add(toDeliver)) {
            player.drop(toDeliver, false);
        }
    }

    private static void sendPurchaseMessage(ServerPlayer player, ItemStack item, int quantity, double cost) {
        Component itemComponent = ComponentUtils.wrapInSquareBrackets(item.getHoverName())
                .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                                new HoverEvent.ItemStackInfo(item))));
        Component priceComponent = Component.literal(String.format("%.2f", cost))
                .withStyle(ChatFormatting.GOLD);

        Component msg = Component.translatable(
                quantity > 1 ? "message.infinity_nexus_market.buy_success_multiple"
                        : "message.infinity_nexus_market.buy_success_single",
                ModConfigs.prefix,
                quantity,
                itemComponent,
                priceComponent
        );
        player.displayClientMessage(msg, false);
    }
}