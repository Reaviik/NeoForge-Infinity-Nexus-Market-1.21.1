package com.Infinity.Nexus.Market.tab;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.block.ModBlocksMarket;
import com.Infinity.Nexus.Market.item.ModItemsMarket;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, InfinityNexusMarket.MOD_ID);
    public static final Supplier<CreativeModeTab> INFINITY_TAB_MARKET = CREATIVE_MODE_TABS.register("infinity_nexus_market",
            //Tab Icon
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocksMarket.VENDING_MACHINE.get()))
                    .title(Component.translatable("itemGroup.infinity_nexus_market"))
                    .displayItems((pParameters, pOutput) -> {
                        //-------------------------//-------------------------//
                        //Machines
                        pOutput.accept(new ItemStack(ModBlocksMarket.VENDING_MACHINE.get()));
                        pOutput.accept(new ItemStack(ModBlocksMarket.BUYING_MACHINE.get()));
                        pOutput.accept(new ItemStack(ModBlocksMarket.MARKET_MACHINE.get()));
                        //-------------------------//-------------------------//
                        //Items
                        pOutput.accept(new ItemStack(ModItemsMarket.TICKET.get()));

                    })
                    .build());
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
