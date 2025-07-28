package com.Infinity.Nexus.Market.slots;

import com.Infinity.Nexus.Market.itemStackHandler.RestrictedItemStackHandler;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class InputSlot extends RestrictiveSlot {
    public InputSlot(RestrictedItemStackHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition, 64);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return true;
    }
}