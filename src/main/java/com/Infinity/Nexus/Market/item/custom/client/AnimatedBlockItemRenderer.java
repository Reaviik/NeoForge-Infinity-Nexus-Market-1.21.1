package com.Infinity.Nexus.Market.item.custom.client;

import com.Infinity.Nexus.Market.item.custom.AnimatedBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class AnimatedBlockItemRenderer extends GeoItemRenderer<AnimatedBlockItem>{
    public AnimatedBlockItemRenderer(String modelLocation) {
        super(new AnimatedBlockItemModel(modelLocation));
    }
}
