package com.Infinity.Nexus.Market.utils;

import com.Infinity.Nexus.Market.itemStackHandler.RestrictedItemStackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class EnergyUtils {

    public static IEnergyStorage getItemEnergyStorage(ItemStack stack) {
        if(stack.isEmpty()){
            return null;
        }
        try {
            IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (storage == null) {
                return null;
            }
        }catch (Exception ignored){
            return null;
        }

        return stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    public static boolean isItemEnergyStorage(ItemStack stack) {
        return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
    }


    public static @Nullable IEnergyStorage getBlockCapabilityEnergyHandler(Level level, BlockPos pos, Direction direction) {
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
    }

    public static void fillItem(IEnergyStorage energyStorage, RestrictedItemStackHandler itemHandler, int inputItemSlot, int outputItemSlot, int transferAmount) {
        IEnergyStorage inputStorage = getItemEnergyStorage(itemHandler.getStackInSlot(inputItemSlot));
        if(inputStorage == null){
            return;
        }
        if(energyStorage.getEnergyStored() <= 0){
            return;
        }
        transferEnergy(energyStorage, inputStorage, transferAmount);
        if(inputStorage.getEnergyStored() <= inputStorage.getMaxEnergyStored()){
            return;
        }
        if((!itemHandler.getStackInSlot(outputItemSlot).isEmpty())){
            return;
        }
        ItemStackHandlerUtils.insertItem(outputItemSlot, itemHandler.getStackInSlot(inputItemSlot), false, itemHandler);
        ItemStackHandlerUtils.extractItem(inputItemSlot, 1, false, itemHandler);
    }

    public static void drainItem(IEnergyStorage energyStorage, RestrictedItemStackHandler itemHandler, int inputItemSlot, int outputItemSlot, int transferAmount) {
        IEnergyStorage outputStorage = getItemEnergyStorage(itemHandler.getStackInSlot(inputItemSlot));
        if(outputStorage == null){
            return;
        }
        if(energyStorage.getEnergyStored() <= 0){
            return;
        }
        transferEnergy(outputStorage, energyStorage, transferAmount);
        if(outputStorage.getEnergyStored() > 0){
            return;
        }
        if((!itemHandler.getStackInSlot(outputItemSlot).isEmpty())){
            return;
        }
        ItemStackHandlerUtils.insertItem(outputItemSlot, itemHandler.getStackInSlot(inputItemSlot), false, itemHandler);
        ItemStackHandlerUtils.extractItem(inputItemSlot, 1, false, itemHandler);
    }

    /**
     * Transfere energia de um storage para outro
     * @param source Storage de origem
     * @param target Storage de destino
     * @param maxTransfer Máximo a transferir por operação
     * @return Quantidade realmente transferida
     */
    public static int transferEnergy(IEnergyStorage source, IEnergyStorage target, int maxTransfer) {
        if (source == null || target == null || maxTransfer <= 0) return 0;

        // Verifica se ambos podem realizar a operação
        if (!source.canExtract() || !target.canReceive()) return 0;

        int availableEnergy = source.getEnergyStored();
        if (availableEnergy <= 0) return 0;

        int neighborCapacity = target.getMaxEnergyStored() - target.getEnergyStored();
        if (neighborCapacity <= 0) return 0;

        // Calcula quanto podemos transferir
        int transferAmount = Math.min(Math.min(availableEnergy, maxTransfer), neighborCapacity);

        if (transferAmount > 0) {
            int actuallyReceived = target.receiveEnergy(transferAmount, false);
            source.extractEnergy(actuallyReceived, false);
            return actuallyReceived;
        }
        return 0;
    }

    /**
     * Extrai energia de um storage para um consumidor
     * @param source Storage de origem
     * @param amount Quantidade desejada
     * @param simulate Se true, apenas simula a operação
     * @return Quantidade realmente extraída
     */
    public static int extractEnergy(IEnergyStorage source, int amount, boolean simulate) {
        if (source == null || amount <= 0 || !source.canExtract()) return 0;
        return source.extractEnergy(amount, simulate);
    }

    /**
     * Insere energia em um storage
     * @param target Storage de destino
     * @param amount Quantidade desejada
     * @param simulate Se true, apenas simula a operação
     * @return Quantidade realmente inserida
     */
    public static int insertEnergy(IEnergyStorage target, int amount, boolean simulate) {
        if (target == null || amount <= 0 || !target.canReceive()) return 0;
        return target.receiveEnergy(amount, simulate);
    }
}