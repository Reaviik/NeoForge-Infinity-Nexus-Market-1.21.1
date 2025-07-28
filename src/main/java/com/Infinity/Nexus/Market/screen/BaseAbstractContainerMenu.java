package com.Infinity.Nexus.Market.screen;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class BaseAbstractContainerMenu extends AbstractContainerMenu {
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int VANILLA_SLOT_COUNT = 36;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = 36;
    private static int TE_INVENTORY_SLOT_COUNT;

    protected BaseAbstractContainerMenu(@Nullable MenuType<?> menuType, int containerId, int invSize) {
        super(menuType, containerId);
        TE_INVENTORY_SLOT_COUNT = invSize;
    }

    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = (Slot)this.slots.get(pIndex);
        if (sourceSlot != null && sourceSlot.hasItem()) {
            ItemStack sourceStack = sourceSlot.getItem();
            ItemStack copyOfSourceStack = sourceStack.copy();
            if (pIndex < 36) {
                if (!this.moveItemStackTo(sourceStack, 36, 36 + TE_INVENTORY_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (pIndex >= 36 + TE_INVENTORY_SLOT_COUNT) {
                    System.out.println(Component.translatable("gui.infinity_nexus_market.log.invalid_slot", pIndex).getString());
                    return ItemStack.EMPTY;
                }

                if (!this.moveItemStackTo(sourceStack, 0, 36, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (sourceStack.getCount() == 0) {
                sourceSlot.set(ItemStack.EMPTY);
            } else {
                sourceSlot.setChanged();
            }

            sourceSlot.onTake(playerIn, sourceStack);
            return copyOfSourceStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public boolean stillValid(Player player) {
        return player instanceof ServerPlayer;
    }

    protected void addPlayerInventory(Inventory playerInventory) {
        for(int i = 0; i < 3; ++i) {
            for(int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }

    }

    protected void addPlayerHotbar(Inventory playerInventory) {
        for(int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }

    }
}
