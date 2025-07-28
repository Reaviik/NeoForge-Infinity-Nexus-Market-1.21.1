package com.Infinity.Nexus.Market.itemStackHandler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class RestrictedItemStackHandler extends ItemStackHandler {

    public RestrictedItemStackHandler(int size) {
        super(size);
    }

    // Método para inserção com controle de automação
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate, boolean fromAutomation) {
        if (fromAutomation && !this.isItemValid(slot, stack)) {
            return stack;
        }
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return insertItem(slot, stack, simulate, true);
    }

    // Métodos de extração que você já tinha implementado
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate, boolean fromAutomation) {
        if (fromAutomation && this.isItemValid(slot, this.getStackInSlot(slot))) {
            return ItemStack.EMPTY;
        }
        return super.extractItem(slot, amount, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return extractItem(slot, amount, simulate, true);
    }
}