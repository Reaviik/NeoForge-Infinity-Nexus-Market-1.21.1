package com.Infinity.Nexus.Market.networking.packet;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.block.entity.BuyingBlockEntity;
import com.Infinity.Nexus.Market.block.entity.VendingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleWorkC2SPacket(BlockPos pos, boolean work) implements CustomPacketPayload {
    public static final Type<ToggleWorkC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "toggle_area"));

    public static final StreamCodec<FriendlyByteBuf, ToggleWorkC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ToggleWorkC2SPacket::pos,
            ByteBufCodecs.BOOL,
            ToggleWorkC2SPacket::work,
            ToggleWorkC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleWorkC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof VendingBlockEntity vending) {
                vending.toggleWork(packet.work());
            } else if (blockEntity instanceof BuyingBlockEntity buying) {
                buying.toggleWork(packet.work());
            }
        });
    }
}