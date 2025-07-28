package com.Infinity.Nexus.Market.screen.market.functions;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

import java.util.function.Predicate;

public class MarketFilterManager {
    public enum FilterType {
        ALL("All", stack -> true),
        ARMOR("Armor", stack -> stack.is(Tags.Items.ARMORS)),
        WEAPON("Weapon", stack -> stack.is(Tags.Items.MELEE_WEAPON_TOOLS)),
        TOOL("Tool", stack -> stack.is(Tags.Items.TOOLS)),
        BLOCK("Block", stack -> stack.getItem() instanceof BlockItem),
        FOOD("Food", stack -> stack.has(DataComponents.FOOD)),
        POTION("Potion", stack -> stack.is(Tags.Items.POTIONS)),
        ENCHANTED("Enchants", stack -> stack.getItem() instanceof EnchantedBookItem);

        private final String name;
        private final Predicate<ItemStack> predicate;

        FilterType(String name, Predicate<ItemStack> predicate) {
            this.name = name;
            this.predicate = predicate;
        }

        public Component getDisplayName() {
            return Component.translatable("gui.infinity_nexus_market.filter." + name);
        }

        public boolean test(ItemStack stack) {
            return predicate.test(stack);
        }

        public FilterType next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static FilterType currentFilter = FilterType.ALL;

    public static FilterType getCurrentFilter() {
        return currentFilter;
    }

    public static void cycleFilter() {
        currentFilter = currentFilter.next();
    }

    public static Component getFilterButtonText() {
        return currentFilter.getDisplayName();
    }

    public static Component getFilterTooltip() {
        return Component.translatable("gui.infinity_nexus_market.tooltip.filter", currentFilter.name);
    }
}