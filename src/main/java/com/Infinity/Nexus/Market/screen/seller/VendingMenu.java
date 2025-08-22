package com.Infinity.Nexus.Market.screen.seller;

import com.Infinity.Nexus.Market.block.ModBlocksMarket;
import com.Infinity.Nexus.Market.block.entity.VendingBlockEntity;
import com.Infinity.Nexus.Market.itemStackHandler.RestrictedItemStackHandler;
import com.Infinity.Nexus.Market.screen.BaseAbstractContainerMenu;
import com.Infinity.Nexus.Market.screen.ModMenuTypes;
import com.Infinity.Nexus.Market.slots.InputSlot;
import com.Infinity.Nexus.Market.slots.ResultSlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;

public class VendingMenu extends BaseAbstractContainerMenu {
    public final VendingBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private static final int slots = 2;

    public VendingMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, (VendingBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(9), new RestrictedItemStackHandler(slots));
    }

    public VendingMenu(int pContainerId, Inventory inv, VendingBlockEntity entity, ContainerData data, RestrictedItemStackHandler iItemHandler) {
        super(ModMenuTypes.VENDING_MENU.get(), pContainerId, slots);
        checkContainerSize(inv, slots);
        blockEntity = entity;
        level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new InputSlot(iItemHandler, 0, 30, 11));
        this.addSlot(new ResultSlot(iItemHandler, 1, 30, 29));


        addDataSlots(data);
    }

    public VendingBlockEntity getBlockEntity(){
        return blockEntity;
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
                pPlayer, ModBlocksMarket.VENDING_MACHINE.get());
    }
}