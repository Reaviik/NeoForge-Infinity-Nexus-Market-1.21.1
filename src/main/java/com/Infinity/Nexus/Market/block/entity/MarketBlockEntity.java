package com.Infinity.Nexus.Market.block.entity;

import com.Infinity.Nexus.Market.itemStackHandler.RestrictedItemStackHandler;
import com.Infinity.Nexus.Market.networking.ModMessages;
import com.Infinity.Nexus.Market.networking.packet.MarketSalesSyncS2CPacket;
import com.Infinity.Nexus.Market.screen.market.MarketMenu;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class MarketBlockEntity extends AbstractMarketBlockEntity {
    protected final ContainerData data;
    private String owner;

    public MarketBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.MARKET_MACHINE_BE.get(), pPos, pBlockState, 0);
        this.data = createContainerData();
    }

    @Override
    protected RestrictedItemStackHandler createItemHandler(int slots) {
        return new RestrictedItemStackHandler(slots);
    }

    protected ContainerData createContainerData() {
        return new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> owner == null ? 0 : 1;
                    default -> throw new IllegalArgumentException();
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> owner = pValue == 1 ? "" : null;
                    default -> throw new IllegalArgumentException();
                }
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
    }

    @Override
    protected int getEnergyCapacity() {
        return 0;
    }

    @Override
    protected int getEnergyTransfer() {
        return 0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infinity_nexus_market.market_machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        if (pPlayer instanceof ServerPlayer serverPlayer && level instanceof ServerLevel) {
            // Obtém as vendas diretamente do SQLiteManager
            List<DatabaseManager.MarketItemEntry> marketItems = DatabaseManager.getAllMarketItems();

            // Converte para DTOs e envia para o cliente
            var packet = new MarketSalesSyncS2CPacket(
                    MarketSalesSyncS2CPacket.fromMarketItems(marketItems)
            );
            ModMessages.sendToPlayer(packet, serverPlayer);
        }
        return new MarketMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider registries) {
        super.saveAdditional(pTag, registries);
        pTag.putString("owner", owner == null ? "" : owner);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider registries) {
        super.loadAdditional(pTag, registries);
        if (pTag.getString("owner").equals("")) {
            owner = null;
        } else {
            owner = pTag.getString("owner");
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithFullMetadata(registries);
    }

    public void setOwner(Player player) {
        owner = player.getStringUUID();
        setChanged();
    }

    public String getOwner() {
        if (owner == null) {
            return Component.translatable("gui.infinity_nexus_market.no_owner").getString();
        }
        Player player = level.getPlayerByUUID(UUID.fromString(owner));
        Component displayName = player == null ? Component.empty() : player.getDisplayName();
        return Component.translatable("gui.infinity_nexus_market.owner_format", displayName.getString()).getString();
    }
}