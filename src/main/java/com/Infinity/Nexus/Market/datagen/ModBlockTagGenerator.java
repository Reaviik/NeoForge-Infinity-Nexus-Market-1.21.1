package com.Infinity.Nexus.Market.datagen;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.block.ModBlocksMarket;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagGenerator extends BlockTagsProvider {
    public ModBlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, InfinityNexusMarket.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {

        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .add(ModBlocksMarket.VENDING_MACHINE.get())
            .add(ModBlocksMarket.BUYING_MACHINE.get());


        this.tag(BlockTags.NEEDS_STONE_TOOL)
            .add(ModBlocksMarket.VENDING_MACHINE.get())
            .add(ModBlocksMarket.BUYING_MACHINE.get());


    }

    @Override
    public String getName() {
        return Component.translatable("datagen.infinity_nexus_market.block_tags").getString();
    }
}
