package com.Infinity.Nexus.Utils.compat.jade;

import com.Infinity.Nexus.Utils.InfinityNexusUtils;
import com.Infinity.Nexus.Utils.block.entity.TerraformBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum TerraformOwner implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(InfinityNexusUtils.MOD_ID, "terraform_owner");

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (blockAccessor.getBlockEntity() instanceof TerraformBlockEntity terraform) {
            iTooltip.add(Component.translatable("gui.infinity_nexus_terraform.owner").append(terraform.getOwner()));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}