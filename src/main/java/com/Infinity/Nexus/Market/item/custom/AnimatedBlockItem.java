package com.Infinity.Nexus.Market.item.custom;

import com.Infinity.Nexus.Market.block.custom.BaseMachineBlock;
import com.Infinity.Nexus.Market.item.custom.client.AnimatedBlockItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.RenderUtil;

import java.util.function.Consumer;


public class AnimatedBlockItem extends BlockItem implements GeoItem {
    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    String filePath;

    public AnimatedBlockItem(Block block, Properties properties, String filePath) {
        super(block, properties);
        this.filePath = filePath;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private AnimatedBlockItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(this.renderer == null) {
                    this.renderer = new AnimatedBlockItemRenderer(filePath);
                }

                return this.renderer;
            }
        });
    }

    protected static final RawAnimation OFF = RawAnimation.begin().thenLoop("animation.model.off");
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, this::deployAnimController));
    }

    protected <E extends GeoAnimatable> PlayState deployAnimController(final AnimationState<E> state) {
        state.getController().setAnimation(OFF);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object itemStack) {
        return RenderUtil.getCurrentTick();
    }
}