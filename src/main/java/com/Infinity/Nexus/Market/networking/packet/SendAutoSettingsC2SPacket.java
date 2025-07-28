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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SendAutoSettingsC2SPacket(BlockPos pos, String price, String minAmount, boolean autoEnabled, boolean autoNotify) implements CustomPacketPayload {
    public static final Type<SendAutoSettingsC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "send_auto_settings"));

    public static final StreamCodec<FriendlyByteBuf, SendAutoSettingsC2SPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SendAutoSettingsC2SPacket::pos,
            ByteBufCodecs.STRING_UTF8,
            SendAutoSettingsC2SPacket::price,
            ByteBufCodecs.STRING_UTF8,
            SendAutoSettingsC2SPacket::minAmount,
            ByteBufCodecs.BOOL,
            SendAutoSettingsC2SPacket::autoEnabled,
            ByteBufCodecs.BOOL,
            SendAutoSettingsC2SPacket::autoNotify,
            SendAutoSettingsC2SPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SendAutoSettingsC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;
            Level level = player.level();
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (be instanceof VendingBlockEntity vending) {
                double price = 0;
                int minAmount = 0;
                try { price = Double.parseDouble(packet.price()); } catch (Exception ignored) {}
                try { minAmount = Integer.parseInt(packet.minAmount()); } catch (Exception ignored) {}
                vending.setAutoPrice(price);
                vending.setAutoMinAmount(minAmount);
                vending.setAutoEnabled(packet.autoEnabled());
                vending.setAutoNotify(packet.autoNotify());
            } else if (be instanceof BuyingBlockEntity buying) {
                double price = 0;
                int minAmount = 0;
                try { price = Double.parseDouble(packet.price()); } catch (Exception ignored) {}
                try { minAmount = Integer.parseInt(packet.minAmount()); } catch (Exception ignored) {}
                buying.setAutoPrice(price);
                buying.setAutoMinAmount(minAmount);
                buying.setAutoEnabled(packet.autoEnabled());
                buying.setAutoNotify(packet.autoNotify());
            }
        });
    }
} 