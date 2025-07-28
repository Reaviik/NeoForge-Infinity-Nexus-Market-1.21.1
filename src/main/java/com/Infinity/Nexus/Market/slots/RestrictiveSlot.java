package com.Infinity.Nexus.Market.slots;

import com.Infinity.Nexus.Market.itemStackHandler.RestrictedItemStackHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class RestrictiveSlot extends SlotItemHandler {
    RestrictedItemStackHandler handler;
    int stackSize;

    public RestrictiveSlot(RestrictedItemStackHandler itemHandler, int index, int xPosition, int yPosition, int stackSize) {
        super(itemHandler, index, xPosition, yPosition);
        handler = itemHandler;
        this.stackSize = stackSize;
    }


    @Override
    public boolean mayPickup(Player playerIn) {
        return !this.handler.extractItem(this.getSlotIndex(), 1, true, false).isEmpty();
    }

    @Override
    @Nonnull
    public ItemStack remove(int amount) {
        return this.handler.extractItem(this.getSlotIndex(), amount, false, false);
    }

    @Override
    public int getMaxStackSize() {
        return stackSize;
    }
}