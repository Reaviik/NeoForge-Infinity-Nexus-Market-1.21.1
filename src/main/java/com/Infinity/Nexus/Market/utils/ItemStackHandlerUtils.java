package com.Infinity.Nexus.Market.utils;

import com.Infinity.Nexus.Market.itemStackHandler.RestrictedItemStackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class ItemStackHandlerUtils {

    public static boolean canInsertItemAndAmountIntoOutputSlot(Item item, int count, int outputSlot, RestrictedItemStackHandler itemHandler) {
        boolean itemValid = itemHandler.getStackInSlot(outputSlot).isEmpty() || itemHandler.getStackInSlot(outputSlot).getItem() == item;
        boolean countValid = (itemHandler.getStackInSlot(outputSlot).getCount() + count) <= itemHandler.getStackInSlot(outputSlot).getMaxStackSize();
        return itemHandler.getStackInSlot(outputSlot).isEmpty() || (itemValid && countValid);
    }

    public static void setStackInSlot(int slot, ItemStack stack, RestrictedItemStackHandler itemHandler) {
        itemHandler.setStackInSlot(slot, stack);
    }

    public static void extractItem(int slot, int count, boolean simulate, RestrictedItemStackHandler itemHandler) {
        itemHandler.extractItem(slot, count, simulate, false);
    }

    public static void insertItem(int slot, ItemStack stack, boolean simulate, RestrictedItemStackHandler itemHandler) {
        itemHandler.insertItem(slot, stack, simulate, false);
    }

    public static void shrinkItem(int slot, int count, RestrictedItemStackHandler itemHandler) {
        itemHandler.getStackInSlot(slot).shrink(count);
    }

    public static ItemStack getStackInSlot(int slot, RestrictedItemStackHandler itemHandler) {
        return itemHandler.getStackInSlot(slot);
    }

    public static @Nullable IItemHandler getBlockCapabilityItemHandler(Level level, BlockPos pos, Direction direction) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
    }
}
