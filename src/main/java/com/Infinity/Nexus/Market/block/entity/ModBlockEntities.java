package com.Infinity.Nexus.Market.block.entity;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.block.ModBlocksMarket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public  static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, InfinityNexusMarket.MOD_ID);

    public static final Supplier<BlockEntityType<VendingBlockEntity>> VENDING_MACHINE_BE =
            BLOCK_ENTITY.register("vending_machine_block_entity", () -> BlockEntityType.Builder.of(
                    VendingBlockEntity::new, ModBlocksMarket.VENDING_MACHINE.get()).build(null));

    public static final Supplier<BlockEntityType<BuyingBlockEntity>> BUYING_MACHINE_BE =
            BLOCK_ENTITY.register("buying_machine_block_entity", () -> BlockEntityType.Builder.of(
                    BuyingBlockEntity::new, ModBlocksMarket.BUYING_MACHINE.get()).build(null));

    public static final Supplier<BlockEntityType<MarketBlockEntity>> MARKET_MACHINE_BE =
            BLOCK_ENTITY.register("market_machine_block_entity", () -> BlockEntityType.Builder.of(
                    MarketBlockEntity::new, ModBlocksMarket.MARKET_MACHINE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY.register(eventBus);
    }
}
