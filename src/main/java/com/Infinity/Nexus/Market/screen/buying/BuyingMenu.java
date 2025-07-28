package com.Infinity.Nexus.Market.screen.buying;

import com.Infinity.Nexus.Market.block.ModBlocksMarket;
import com.Infinity.Nexus.Market.block.entity.BuyingBlockEntity;
import com.Infinity.Nexus.Market.itemStackHandler.RestrictedItemStackHandler;
import com.Infinity.Nexus.Market.screen.BaseAbstractContainerMenu;
import com.Infinity.Nexus.Market.screen.ModMenuTypes;
import com.Infinity.Nexus.Market.slots.ResultSlot;
import com.Infinity.Nexus.Market.slots.TicketSlot;
import com.Infinity.Nexus.Market.utils.HasEnergyStorage;
import com.Infinity.Nexus.Market.utils.ModEnergyStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;

public class BuyingMenu extends BaseAbstractContainerMenu implements HasEnergyStorage {
    public final BuyingBlockEntity blockEntity;
    private final Level level;
    private ModEnergyStorage energyStorage;
    private final ContainerData data;
    private static final int slots = 2;

    public BuyingMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, (BuyingBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(9), new RestrictedItemStackHandler(slots));
    }

    public BuyingMenu(int pContainerId, Inventory inv, BuyingBlockEntity entity, ContainerData data, RestrictedItemStackHandler iItemHandler) {
        super(ModMenuTypes.BUYING_MENU.get(), pContainerId, slots);
        checkContainerSize(inv, slots);
        blockEntity = entity;
        energyStorage = (ModEnergyStorage) blockEntity.getEnergyStorage();
        level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new TicketSlot(iItemHandler, 0, 30, 11));
        this.addSlot(new ResultSlot(iItemHandler, 1, 30, 29));


        addDataSlots(data);
    }

    public BuyingBlockEntity getBlockEntity(){
        return blockEntity;
    }
    @Override
    public ModEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public Level getLevel() {
        return level;
    }

    public ContainerData getData() {
        return data;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(this.getBlockEntity().getLevel(), this.getBlockEntity().getBlockPos()),
                pPlayer, ModBlocksMarket.BUYING_MACHINE.get());
    }
}