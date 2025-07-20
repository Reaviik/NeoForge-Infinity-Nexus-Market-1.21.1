package com.Infinity.Nexus.Utils.utils;

import com.Infinity.Nexus.Utils.component.UtilsDataComponents;
import com.Infinity.Nexus.Utils.config.ModConfigs;
import com.Infinity.Nexus.Utils.item.ModItemsUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ModUtilsUtils {
    public static int getUpgradeCount(ItemStackHandler itemHandler, int[] upgradeSlots, Item upgrade) {
        int count = 0;
        for (int upgradeSlot : upgradeSlots) {
            ItemStack stack = itemHandler.getStackInSlot(upgradeSlot);
            if (stack.getItem() == upgrade) {
                count += stack.getCount();
            }
        }
        return Math.min(count, 4);
    }

    public static void damageRangeCard(ItemStackHandler handler, int[] upgradeSlots) {
        for (int slot : upgradeSlots) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(ModItemsUtils.TERRAFORM_RANGE_UPGRADE.get()) && stack.has(UtilsDataComponents.TERRAFORM_RANGE_CARD_RANGE.get())) {
                int range = stack.getOrDefault(UtilsDataComponents.TERRAFORM_RANGE_CARD_RANGE.get(), ModConfigs.terraformDefaultArea);
                if (!ModConfigs.terraformCostCardExtraDurability) {
                    stack.set(UtilsDataComponents.TERRAFORM_RANGE_CARD_RANGE.get(), Math.max(ModConfigs.terraformDefaultArea, range - 1));
                } else {
                    stack.set(UtilsDataComponents.TERRAFORM_RANGE_CARD_RANGE.get(), Math.max(0, range - 1));
                }
                break;
            }
        }
    }
    
    public static Component getRangeCardType(ItemStack itemStack) {
        int value = itemStack.getOrDefault(UtilsDataComponents.TERRAFORM_RANGE_CARD_TYPE.get(), 0);
        return switch (value) {
            case 1 -> Component.translatable("tooltip.infinity_nexus_utils.range_card_type_skip_be");
            case 2 -> Component.translatable("tooltip.infinity_nexus_utils.range_card_type_skip_fluid");
            case 3 -> Component.translatable("tooltip.infinity_nexus_utils.range_card_type_fluids_only");
            default -> Component.translatable("tooltip.infinity_nexus_utils.range_card_type_all");
        };
    }
    public static Component getPlacerCardType(ItemStack itemStack) {
        int value = itemStack.getOrDefault(UtilsDataComponents.TERRAFORM_PLACER_CARD_TYPE.get(), 0);
        return switch (value) {
            case 1 -> Component.translatable("tooltip.infinity_nexus_utils.placer_card_type_wall");
            default -> Component.translatable("tooltip.infinity_nexus_utils.placer_card_type_all");
        };
    }
}
