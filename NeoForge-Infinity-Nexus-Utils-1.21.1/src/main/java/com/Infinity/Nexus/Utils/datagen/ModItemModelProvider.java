package com.Infinity.Nexus.Utils.datagen;

import com.Infinity.Nexus.Utils.InfinityNexusUtils;
import com.Infinity.Nexus.Utils.item.ModItemsUtils;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, InfinityNexusUtils.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItemsUtils.TERRAFORM_RANGE_UPGRADE.get());
        basicItem(ModItemsUtils.TERRAFORM_CLEAR_UPGRADE.get());
        basicItem(ModItemsUtils.TERRAFORM_PLACER_UPGRADE.get());
        basicItem(ModItemsUtils.TERRAFORM_MINER_UPGRADE.get());
    }
}
