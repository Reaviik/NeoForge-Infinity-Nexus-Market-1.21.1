package com.Infinity.Nexus.Market.compat;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.block.ModBlocksMarket;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;


@JeiPlugin
public class JEIModPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(new ItemStack(ModBlocksMarket.VENDING_MACHINE.get()), Component.translatable("infinity_nexus_market.vending_jei_information"));
        registration.addItemStackInfo(new ItemStack(ModBlocksMarket.BUYING_MACHINE.get()), Component.translatable("infinity_nexus_market.buying_jei_information"));
    }
}
