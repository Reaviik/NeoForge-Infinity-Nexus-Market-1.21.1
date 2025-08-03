package com.Infinity.Nexus.Market.item.custom.client;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.item.custom.AnimatedBlockItem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AnimatedBlockItemModel extends GeoModel<AnimatedBlockItem> {
    private final String modelLocation;

    public AnimatedBlockItemModel(String modelLocation) {
        this.modelLocation = modelLocation;
    }


    @Override
    public ResourceLocation getModelResource(AnimatedBlockItem animatedBlockItem) {
        return ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "geo/" + modelLocation + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AnimatedBlockItem animatedBlockItem) {
        return ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "textures/block/" + modelLocation + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(AnimatedBlockItem animatedBlockItem) {
        return ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "animations/" + modelLocation + ".animation.json");
    }

    @Override
    public RenderType getRenderType(AnimatedBlockItem animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(getTextureResource(animatable));
    }
}