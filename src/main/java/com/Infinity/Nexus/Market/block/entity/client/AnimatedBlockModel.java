package com.Infinity.Nexus.Market.block.entity.client;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.block.entity.AbstractMarketBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AnimatedBlockModel extends GeoModel<AbstractMarketBlockEntity> {
    private final String modelLocation;

    public AnimatedBlockModel(String modelLocation) {
        this.modelLocation = modelLocation;
    }

    @Override
    public ResourceLocation getModelResource(AbstractMarketBlockEntity blockEntity) {
        return ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "geo/" + modelLocation + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AbstractMarketBlockEntity blockEntity) {
        return ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "textures/block/" + modelLocation + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractMarketBlockEntity blockEntity) {
        return ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "animations/" + modelLocation + ".animation.json");
    }

    @Override
    public RenderType getRenderType(AbstractMarketBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(getTextureResource(animatable));
    }
}