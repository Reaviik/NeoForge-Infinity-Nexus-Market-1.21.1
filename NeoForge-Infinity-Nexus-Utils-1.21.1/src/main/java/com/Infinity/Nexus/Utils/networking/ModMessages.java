package com.Infinity.Nexus.Utils.networking;

import com.Infinity.Nexus.Utils.networking.packet.AreaVisibilityS2CPacket;
import com.Infinity.Nexus.Utils.networking.packet.ShowTerraformAreaC2SPacket;
import com.Infinity.Nexus.Utils.networking.packet.ToggleAreaC2SPacket;
import com.Infinity.Nexus.Utils.networking.packet.ToggleTerraformingC2SPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModMessages {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1")
                .versioned("1.0")
                .optional();

        // Registrar pacotes C2S
        registrar.playToServer(
                ToggleAreaC2SPacket.TYPE,
                ToggleAreaC2SPacket.STREAM_CODEC,
                ToggleAreaC2SPacket::handle
        );

        registrar.playToServer(
                ShowTerraformAreaC2SPacket.TYPE,
                ShowTerraformAreaC2SPacket.STREAM_CODEC,
                ShowTerraformAreaC2SPacket::handle
        );

        registrar.playToServer(
                ToggleTerraformingC2SPacket.TYPE,
                ToggleTerraformingC2SPacket.STREAM_CODEC,
                ToggleTerraformingC2SPacket::handle
        );

        // Registrar pacote de movimentação da área
        registrar.playToServer(
                com.Infinity.Nexus.Utils.networking.packet.MoveTerraformAreaC2SPacket.TYPE,
                com.Infinity.Nexus.Utils.networking.packet.MoveTerraformAreaC2SPacket.STREAM_CODEC,
                com.Infinity.Nexus.Utils.networking.packet.MoveTerraformAreaC2SPacket::handle
        );

        // Registrar pacote para setar tamanho manualmente
        registrar.playToServer(
                com.Infinity.Nexus.Utils.networking.packet.SetTerraformAreaSizeC2SPacket.TYPE,
                com.Infinity.Nexus.Utils.networking.packet.SetTerraformAreaSizeC2SPacket.STREAM_CODEC,
                com.Infinity.Nexus.Utils.networking.packet.SetTerraformAreaSizeC2SPacket::handle
        );

        // Registrar pacotes S2C (se necessário)
        registrar.playToClient(
                AreaVisibilityS2CPacket.TYPE,
                AreaVisibilityS2CPacket.STREAM_CODEC,
                AreaVisibilityS2CPacket::handle
        );
    }


    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}