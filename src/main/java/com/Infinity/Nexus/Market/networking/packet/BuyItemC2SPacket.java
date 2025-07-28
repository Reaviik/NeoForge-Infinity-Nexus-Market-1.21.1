package com.Infinity.Nexus.Market.networking.packet;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.block.entity.BuyingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BuyItemC2SPacket(BlockPos pos, double preco) implements CustomPacketPayload {
    public static final Type<BuyItemC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "buy_item"));

    public static final StreamCodec<FriendlyByteBuf, BuyItemC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            BuyItemC2SPacket::pos,
            ByteBufCodecs.DOUBLE,
            BuyItemC2SPacket::preco,
            BuyItemC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BuyItemC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            Level level = player.level();
            BlockEntity be = level.getBlockEntity(packet.pos());

            if (be instanceof BuyingBlockEntity buyingMachine) {
                buyingMachine.selfBuyInMarket(player);
            }
        });
    }
} 