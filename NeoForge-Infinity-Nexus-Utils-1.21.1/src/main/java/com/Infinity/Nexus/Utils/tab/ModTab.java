package com.Infinity.Nexus.Utils.tab;

import com.Infinity.Nexus.Utils.InfinityNexusUtils;
import com.Infinity.Nexus.Utils.block.ModBlocksUtils;
import com.Infinity.Nexus.Utils.item.ModItemsUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, InfinityNexusUtils.MOD_ID);
    public static final Supplier<CreativeModeTab> INFINITY_TAB_UTILS = CREATIVE_MODE_TABS.register("infinity_nexus_terraform",
            //Tab Icon
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocksUtils.TERRAFORM.get()))
                    .title(Component.translatable("itemGroup.infinity_nexus_terraform"))
                    .displayItems((pParameters, pOutput) -> {
                        //-------------------------//-------------------------//
                        //Machines
                        pOutput.accept(new ItemStack(ModBlocksUtils.TERRAFORM.get()));
                        //-------------------------//-------------------------//
                        //Upgrades
                        pOutput.accept(new ItemStack(ModItemsUtils.TERRAFORM_RANGE_UPGRADE.get()));
                        pOutput.accept(new ItemStack(ModItemsUtils.TERRAFORM_CLEAR_UPGRADE.get()));
                        pOutput.accept(new ItemStack(ModItemsUtils.TERRAFORM_PLACER_UPGRADE.get()));
                        pOutput.accept(new ItemStack(ModItemsUtils.TERRAFORM_MINER_UPGRADE.get()));

                    })
                    .build());
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
