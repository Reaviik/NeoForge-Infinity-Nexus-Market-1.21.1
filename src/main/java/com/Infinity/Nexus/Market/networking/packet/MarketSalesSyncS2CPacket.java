package com.Infinity.Nexus.Market.networking.packet;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.screen.market.MarketScreen;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record MarketSalesSyncS2CPacket(List<SaleEntryDTO> sales) implements CustomPacketPayload {

    public static final Type<MarketSalesSyncS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "market_sales_sync"));

    // STREAM_CODEC do pacote
    public static final StreamCodec<RegistryFriendlyByteBuf, MarketSalesSyncS2CPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> ByteBufCodecs.collection(ArrayList::new, SaleEntryDTO.STREAM_CODEC)
                            .encode(buf, new ArrayList<>(packet.sales)),
                    buf -> new MarketSalesSyncS2CPacket(ByteBufCodecs.collection(ArrayList::new, SaleEntryDTO.STREAM_CODEC)
                            .decode(buf))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MarketSalesSyncS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            MarketScreen.setClientSales(packet.sales());
        });
    }

    // DTO de venda
    public static class SaleEntryDTO {
        public final String transactionId;
        public final String seller;
        public final String sellerName;
        public final ItemStack item;
        public final int quantity;
        public final double price;
        public final String date;

        public SaleEntryDTO(String transactionId, String seller, String sellerName,
                            ItemStack item, int quantity, double price, String date) {
            this.transactionId = transactionId;
            this.seller = seller;
            this.sellerName = sellerName;
            this.item = item;
            this.quantity = quantity;
            this.price = price;
            this.date = date;
        }

        // STREAM_CODEC manual (sem limite de parâmetros)
        public static final StreamCodec<RegistryFriendlyByteBuf, SaleEntryDTO> STREAM_CODEC =
                StreamCodec.of(
                        (buf, dto) -> {
                            ByteBufCodecs.STRING_UTF8.encode(buf, dto.transactionId);
                            ByteBufCodecs.STRING_UTF8.encode(buf, dto.seller);
                            ByteBufCodecs.STRING_UTF8.encode(buf, dto.sellerName);
                            ItemStack.STREAM_CODEC.encode(buf, dto.item);
                            ByteBufCodecs.INT.encode(buf, dto.quantity);
                            ByteBufCodecs.DOUBLE.encode(buf, dto.price);
                            ByteBufCodecs.STRING_UTF8.encode(buf, dto.date);
                        },
                        buf -> new SaleEntryDTO(
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf),
                                ItemStack.STREAM_CODEC.decode(buf),
                                ByteBufCodecs.INT.decode(buf),
                                ByteBufCodecs.DOUBLE.decode(buf),
                                ByteBufCodecs.STRING_UTF8.decode(buf)
                        )
                );
    }

    // Novo método para converter MarketItemEntry para SaleEntryDTO
    public static List<SaleEntryDTO> fromMarketItems(List<DatabaseManager.MarketItemEntry> marketItems) {

        List<SaleEntryDTO> result = new ArrayList<>();
        for (DatabaseManager.MarketItemEntry entry : marketItems) {
            // Desserializa o ItemStack do NBT
            ItemStack item = DatabaseManager.deserializeItemStack(entry.itemNbt);

            if (item != null && !item.isEmpty() && item.getCount() > 0) {
                result.add(new SaleEntryDTO(
                        entry.entryId,
                        entry.sellerUUID != null ? entry.sellerUUID : "",
                        entry.sellerName != null ? entry.sellerName : "",
                        item.copy(),
                        entry.quantity,
                        entry.currentPrice,
                        entry.createdAt
                ));
            }
        }
        return result;
    }
}