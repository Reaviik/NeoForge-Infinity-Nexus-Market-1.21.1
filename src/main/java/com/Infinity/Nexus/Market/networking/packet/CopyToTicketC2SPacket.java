package com.Infinity.Nexus.Market.networking.packet;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.component.MarketDataComponents;
import com.Infinity.Nexus.Market.component.TicketItemComponent;
import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.item.ModItemsMarket;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CopyToTicketC2SPacket(ItemStack item, int quantity, double price, String sellerName, String entryId)
        implements CustomPacketPayload {

    public static final Type<CopyToTicketC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "copy_to_ticket"));

    // Codec seguro (não quebra se sellerName for null)
    public static final StreamCodec<RegistryFriendlyByteBuf, CopyToTicketC2SPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        ItemStack.STREAM_CODEC.encode(buf, packet.item());
                        ByteBufCodecs.INT.encode(buf, 1);
                        ByteBufCodecs.DOUBLE.encode(buf, packet.price());
                        ByteBufCodecs.STRING_UTF8.encode(buf, packet.sellerName() == null ? "" : packet.sellerName());
                        ByteBufCodecs.STRING_UTF8.encode(buf, packet.entryId() == null ? "" : packet.entryId());
                    },
                    buf -> new CopyToTicketC2SPacket(
                            ItemStack.STREAM_CODEC.decode(buf),
                            ByteBufCodecs.INT.decode(buf),
                            ByteBufCodecs.DOUBLE.decode(buf),
                            ByteBufCodecs.STRING_UTF8.decode(buf),
                            ByteBufCodecs.STRING_UTF8.decode(buf)
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CopyToTicketC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            // Procura o primeiro ticket sem componente
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(ModItemsMarket.TICKET.get()) && !stack.has(MarketDataComponents.TICKET_ITEM.get())) {
                    // Adiciona o componente
                    stack.set(MarketDataComponents.TICKET_ITEM.get(),
                            new TicketItemComponent(
                                    DatabaseManager.serializeItemStack(packet.item()),
                                    packet.quantity(),
                                    (int) packet.price(),
                                    packet.sellerName() == null ? "" : packet.sellerName(),
                                    packet.entryId() == null ? "" : packet.entryId(),
                                    false
                            )
                    );
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "message.infinity_nexus_market.copied_to_ticket",
                                    ModConfigs.prefix
                            ), false
                    );
                    return;
                }
            }

            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.infinity_nexus_market.no_empty_ticket",
                            ModConfigs.prefix
                    ), false
            );
        });
    }
}
