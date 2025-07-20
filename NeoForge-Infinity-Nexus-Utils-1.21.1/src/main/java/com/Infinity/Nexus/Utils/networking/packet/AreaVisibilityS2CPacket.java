package com.Infinity.Nexus.Utils.networking.packet;

import com.Infinity.Nexus.Core.utils.GetResourceLocation;
import com.Infinity.Nexus.Utils.InfinityNexusUtils;
import com.Infinity.Nexus.Utils.block.entity.TerraformBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AreaVisibilityS2CPacket(BlockPos pos, boolean showArea) implements CustomPacketPayload {
    public static final Type<AreaVisibilityS2CPacket> TYPE =
            new Type<>(GetResourceLocation.withNamespaceAndPath(InfinityNexusUtils.MOD_ID, "area_visibility"));

    public static final StreamCodec<FriendlyByteBuf, AreaVisibilityS2CPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            AreaVisibilityS2CPacket::pos,
            ByteBufCodecs.BOOL,
            AreaVisibilityS2CPacket::showArea,
            AreaVisibilityS2CPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AreaVisibilityS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(packet.pos()) instanceof TerraformBlockEntity blockEntity) {
                blockEntity.setShowArea(packet.showArea());
            }
        });
    }
}