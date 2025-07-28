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

public record UpdateAutoSettingsC2SPacket(BlockPos pos, boolean autoEnabled, int autoMinAmount, double autoPrice) implements CustomPacketPayload {
    public static final Type<UpdateAutoSettingsC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "update_vending_auto_settings"));

    public static final StreamCodec<FriendlyByteBuf, UpdateAutoSettingsC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            UpdateAutoSettingsC2SPacket::pos,
            ByteBufCodecs.BOOL,
            UpdateAutoSettingsC2SPacket::autoEnabled,
            ByteBufCodecs.INT,
            UpdateAutoSettingsC2SPacket::autoMinAmount,
            ByteBufCodecs.DOUBLE,
            UpdateAutoSettingsC2SPacket::autoPrice,
            UpdateAutoSettingsC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateAutoSettingsC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            BlockEntity blockEntity = level.getBlockEntity(packet.pos());
            if (blockEntity instanceof VendingBlockEntity vending) {
                vending.setAutoEnabled(packet.autoEnabled());
                vending.setAutoMinAmount(packet.autoMinAmount());
                vending.setAutoPrice(packet.autoPrice());
            } else if (blockEntity instanceof BuyingBlockEntity buying) {
                buying.setAutoEnabled(packet.autoEnabled());
                buying.setAutoMinAmount(packet.autoMinAmount());
                buying.setAutoPrice(packet.autoPrice());
            }
        });
    }
} 