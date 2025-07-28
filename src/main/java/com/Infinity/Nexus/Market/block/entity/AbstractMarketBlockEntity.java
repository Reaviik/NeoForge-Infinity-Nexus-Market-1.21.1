package com.Infinity.Nexus.Market.block.entity;

import com.Infinity.Nexus.Market.itemStackHandler.RestrictedItemStackHandler;
import com.Infinity.Nexus.Market.utils.ModEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class AbstractMarketBlockEntity extends BlockEntity implements MenuProvider {
    protected String owner;
    protected String ownerName;
    protected boolean autoEnabled = false;
    protected int autoMinAmount = 0;
    protected double autoPrice = 0.0;
    protected boolean autoNotify = false;
    protected final ContainerData data;
    protected final RestrictedItemStackHandler itemHandler;
    protected final ModEnergyStorage ENERGY_STORAGE;

    public AbstractMarketBlockEntity(BlockEntityType<?> type, BlockPos pPos, BlockState pBlockState, int slots) {
        super(type, pPos, pBlockState);
        this.ENERGY_STORAGE = createEnergyStorage();
        this.itemHandler = createItemHandler(slots);
        this.data = createContainerData();
    }

    protected abstract RestrictedItemStackHandler createItemHandler(int slots);
    protected abstract ContainerData createContainerData();

    private ModEnergyStorage createEnergyStorage() {
        return new ModEnergyStorage(getEnergyCapacity(), getEnergyTransfer()) {
            @Override
            public void onEnergyChanged() {
                setChanged();
                if (level != null) {
                    level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 4);
                }
            }
        };
    }

    protected abstract int getEnergyCapacity();
    protected abstract int getEnergyTransfer();

    public IItemHandler getItemHandler(Direction direction) {
        return itemHandler;
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction direction) {
        return ENERGY_STORAGE;
    }

    public IEnergyStorage getEnergyStorage() {
        return ENERGY_STORAGE;
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider registries) {
        super.saveAdditional(pTag, registries);
        pTag.put("inventory", itemHandler.serializeNBT(registries));
        pTag.putInt("energy", ENERGY_STORAGE.getEnergyStored());
        pTag.putString("owner", owner == null ? "" : owner);
        pTag.putString("ownerName", ownerName == null ? "" : ownerName);
        pTag.putBoolean("autoEnabled", autoEnabled);
        pTag.putInt("autoMinAmount", autoMinAmount);
        pTag.putDouble("autoPrice", autoPrice);
        pTag.putBoolean("autoNotify", autoNotify);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider registries) {
        super.loadAdditional(pTag, registries);
        itemHandler.deserializeNBT(registries, pTag.getCompound("inventory"));
        ENERGY_STORAGE.setEnergy(pTag.getInt("energy"));
        if (pTag.getString("owner").equals("")) {
            owner = null;
        } else {
            owner = pTag.getString("owner");
        }
        if (pTag.getString("ownerName").equals("")) {
            ownerName = null;
        } else {
            ownerName = pTag.getString("ownerName");
        }
        autoEnabled = pTag.getBoolean("autoEnabled");
        autoMinAmount = pTag.getInt("autoMinAmount");
        autoPrice = pTag.getDouble("autoPrice");
        autoNotify = pTag.getBoolean("autoNotify");
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
        ownerName = player.getName().getString();
        setChanged();
    }

    public String getOwner() {
        if (owner == null) {
            return "§4❎";
        }
        Player player = level.getPlayerByUUID(UUID.fromString(owner));
        Component displayName = player == null ? Component.empty() : player.getDisplayName();
        return "§e" + displayName.getString();
    }

    // Common auto-config methods
    public void setAutoEnabled(boolean enabled) {
        this.autoEnabled = enabled;
        setChanged();
    }

    public void setAutoMinAmount(int amount) {
        this.autoMinAmount = amount;
        setChanged();
    }

    public void setAutoPrice(double price) {
        this.autoPrice = price;
        setChanged();
    }

    public void setAutoNotify(boolean notify) {
        this.autoNotify = notify;
        setChanged();
    }

    public void toggleWork(boolean work) {
        autoEnabled = work;
        setChanged();
    }

    public boolean isAutoEnabled() {
        return autoEnabled;
    }

    public int getAutoMinAmount() {
        return autoMinAmount;
    }

    public double getAutoPrice() {
        return autoPrice;
    }

    public boolean isAutoNotify() {
        return autoNotify;
    }
}