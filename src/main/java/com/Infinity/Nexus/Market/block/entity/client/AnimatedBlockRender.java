package com.Infinity.Nexus.Market.block.entity.client;

import com.Infinity.Nexus.Market.block.entity.AbstractMarketBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class AnimatedBlockRender extends GeoBlockRenderer<AbstractMarketBlockEntity> {
    public AnimatedBlockRender(String modelLocation) {
        super(new AnimatedBlockModel(modelLocation));
    }
}