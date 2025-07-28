package com.Infinity.Nexus.Market.compat.jade;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.block.entity.BuyingBlockEntity;
import com.Infinity.Nexus.Market.block.entity.MarketBlockEntity;
import com.Infinity.Nexus.Market.block.entity.VendingBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum MachineOwner implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "machine_owner");

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (blockAccessor.getBlockEntity() instanceof VendingBlockEntity vending) {
            iTooltip.add(Component.translatable("gui.infinity_nexus_market.owner").append(vending.getOwner()));
        }
        if (blockAccessor.getBlockEntity() instanceof BuyingBlockEntity buying) {
            iTooltip.add(Component.translatable("gui.infinity_nexus_market.owner").append(buying.getOwner()));
        }
        if (blockAccessor.getBlockEntity() instanceof MarketBlockEntity market) {
            iTooltip.add(Component.translatable("gui.infinity_nexus_market.owner").append(market.getOwner()));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}