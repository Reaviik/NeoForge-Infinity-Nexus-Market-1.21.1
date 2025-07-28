package com.Infinity.Nexus.Market.events;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.block.entity.BuyingBlockEntity;
import com.Infinity.Nexus.Market.block.entity.ModBlockEntities;
import com.Infinity.Nexus.Market.block.entity.VendingBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;


@EventBusSubscriber(modid = InfinityNexusMarket.MOD_ID)
public class ModBusEvents {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.VENDING_MACHINE_BE.get(), VendingBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.VENDING_MACHINE_BE.get(), VendingBlockEntity::getEnergyStorage);

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.BUYING_MACHINE_BE.get(), BuyingBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.BUYING_MACHINE_BE.get(), BuyingBlockEntity::getEnergyStorage);
    }
}
