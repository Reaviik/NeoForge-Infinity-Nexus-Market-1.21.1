package com.Infinity.Nexus.Market.slots;

import com.Infinity.Nexus.Market.item.ModItemsMarket;
import com.Infinity.Nexus.Market.itemStackHandler.RestrictedItemStackHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class TicketSlot extends RestrictiveSlot {

    public TicketSlot(RestrictedItemStackHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition, 64);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return stack.is(ModItemsMarket.TICKET.get());
    }

    @Override
    public boolean mayPickup(Player playerIn) {
        return !this.handler.extractItem(this.getSlotIndex(), 1, true, false).isEmpty();
    }
}