package com.Infinity.Nexus.Utils.block.entity;

import com.Infinity.Nexus.Utils.InfinityNexusUtils;
import com.Infinity.Nexus.Utils.block.ModBlocksUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public  static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, InfinityNexusUtils.MOD_ID);

    public static final Supplier<BlockEntityType<TerraformBlockEntity>> TERRAFORM_BE =
            BLOCK_ENTITY.register("terraform_block_entity", () -> BlockEntityType.Builder.of(
                    TerraformBlockEntity::new, ModBlocksUtils.TERRAFORM.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY.register(eventBus);
    }
}
