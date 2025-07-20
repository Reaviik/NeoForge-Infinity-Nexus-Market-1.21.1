package com.Infinity.Nexus.Utils.item;

import com.Infinity.Nexus.Utils.InfinityNexusUtils;
import com.Infinity.Nexus.Utils.item.custom.UpgradeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItemsUtils {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfinityNexusUtils.MOD_ID);

    public static final DeferredItem<Item> TERRAFORM_RANGE_UPGRADE = ITEMS.register("terraform_range_upgrade",() -> new UpgradeItem(new Item.Properties().rarity(Rarity.COMMON).stacksTo(1)));
    public static final DeferredItem<Item> TERRAFORM_CLEAR_UPGRADE = ITEMS.register("terraform_clear_upgrade",() -> new UpgradeItem(new Item.Properties().rarity(Rarity.COMMON).stacksTo(1)));
    public static final DeferredItem<Item> TERRAFORM_PLACER_UPGRADE = ITEMS.register("terraform_placer_upgrade",() -> new UpgradeItem(new Item.Properties().rarity(Rarity.COMMON).stacksTo(1)));
    public static final DeferredItem<Item> TERRAFORM_MINER_UPGRADE = ITEMS.register("terraform_miner_upgrade",() -> new UpgradeItem(new Item.Properties().rarity(Rarity.COMMON).stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}