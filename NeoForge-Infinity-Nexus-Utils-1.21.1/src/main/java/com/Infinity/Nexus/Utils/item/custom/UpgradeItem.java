package com.Infinity.Nexus.Utils.item.custom;

import com.Infinity.Nexus.Utils.component.UtilsDataComponents;
import com.Infinity.Nexus.Utils.config.ModConfigs;
import com.Infinity.Nexus.Utils.item.ModItemsUtils;
import com.Infinity.Nexus.Utils.utils.ModUtilsUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.List;

public class UpgradeItem extends com.Infinity.Nexus.Core.items.custom.UpgradeItem {
    public UpgradeItem(Properties pProperties) {
        super(pProperties);
    }


    @Override
    public void appendHoverText(ItemStack stack, TooltipContext tooltip, List<Component> components, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            if(stack.has(UtilsDataComponents.TERRAFORM_RANGE_CARD_RANGE.get())) {
                int range = stack.getOrDefault(UtilsDataComponents.TERRAFORM_RANGE_CARD_RANGE.get(), ModConfigs.terraformDefaultArea);
                components.add(Component.translatable("item.infinity_nexus_terraform.range_description").append(" §5" + range));
                components.add(Component.translatable("tooltip.infinity_nexus_utils.range_card_mode_change"));
                components.add(ModUtilsUtils.getRangeCardType(stack));
                if (stack.is(ModItemsUtils.TERRAFORM_PLACER_UPGRADE.get())) {
                    boolean randomizer = stack.getOrDefault(UtilsDataComponents.TERRAFORM_PLACER_CARD_RANDOMIZER.get(), false);
                    components.add(Component.translatable(randomizer ? "tooltip.infinity_nexus_utils.placer_card_randomizer_on" : "tooltip.infinity_nexus_utils.placer_card_randomizer_off"));
                }
            } else if (stack.is(ModItemsUtils.TERRAFORM_CLEAR_UPGRADE)) {
                components.add(Component.translatable("item.infinity_nexus_terraform.clear_description"));
            } else if (stack.is(ModItemsUtils.TERRAFORM_PLACER_UPGRADE)) {
                components.add(Component.translatable("item.infinity_nexus_terraform.placer_description"));
                boolean randomizer = stack.getOrDefault(UtilsDataComponents.TERRAFORM_PLACER_CARD_RANDOMIZER.get(), false);
                components.add(Component.translatable(randomizer ? "tooltip.infinity_nexus_utils.placer_card_randomizer_on" : "tooltip.infinity_nexus_utils.placer_card_randomizer_off"));
                components.add(Component.translatable("tooltip.infinity_nexus_utils.placer_card_randomizer_change"));
                components.add(ModUtilsUtils.getPlacerCardType(stack));
                components.add(Component.translatable("tooltip.infinity_nexus_utils.placer_card_type_change"));
            } else if (stack.is(ModItemsUtils.TERRAFORM_MINER_UPGRADE)) {
                components.add(Component.translatable("item.infinity_nexus_terraform.miner_description"));
            } else {
                components.add(Component.translatable("item.infinity_nexus_terraform.range_description").append(" §5" + ModConfigs.terraformDefaultArea));
            }
        } else {
            components.add(Component.translatable("tooltip.infinity_nexus_core.pressShift"));
        }
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        if(stack.is(ModItemsUtils.TERRAFORM_RANGE_UPGRADE.get())) {
            stack.set(UtilsDataComponents.TERRAFORM_RANGE_CARD_RANGE.get(), ModConfigs.terraformDefaultArea);
        }
        super.onCraftedBy(stack, level, player);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if(stack.is(ModItemsUtils.TERRAFORM_PLACER_UPGRADE.get())) {
            if(context.getPlayer().isShiftKeyDown()) {
                boolean current = stack.getOrDefault(UtilsDataComponents.TERRAFORM_PLACER_CARD_RANDOMIZER.get(), false);
                stack.set(UtilsDataComponents.TERRAFORM_PLACER_CARD_RANDOMIZER.get(), !current);
                if(context.getPlayer() instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(Component.translatable(!current ? "tooltip.infinity_nexus_utils.placer_card_randomizer_on" : "tooltip.infinity_nexus_utils.placer_card_randomizer_off"));
                }
            } else {
                int currentType = stack.getOrDefault(UtilsDataComponents.TERRAFORM_PLACER_CARD_TYPE.get(), 0);
                int nextType = (currentType + 1) % 2;
                stack.set(UtilsDataComponents.TERRAFORM_PLACER_CARD_TYPE.get(), nextType);
                if(context.getPlayer() instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(ModUtilsUtils.getPlacerCardType(stack));
                }
            }
            return InteractionResult.SUCCESS;
        }

        if(context.getPlayer().isShiftKeyDown()) {
            return super.useOn(context);
        }
        if(!stack.is(ModItemsUtils.TERRAFORM_RANGE_UPGRADE.get())) {
            return super.useOn(context);
        }
        if(!(context.getPlayer() instanceof ServerPlayer serverPlayer)){
            return super.useOn(context);
        }
        int type = stack.getOrDefault(UtilsDataComponents.TERRAFORM_RANGE_CARD_TYPE.get(), 0);
        int nextType = (type + 1) % 4;
        stack.set(UtilsDataComponents.TERRAFORM_RANGE_CARD_TYPE.get(), nextType);
        serverPlayer.sendSystemMessage(ModUtilsUtils.getRangeCardType(stack));
        return super.useOn(context);
    }


    @Override
    public boolean isFoil(ItemStack pStack) {
        return pStack.has(UtilsDataComponents.TERRAFORM_RANGE_CARD_PERSISTENT.get()) || pStack.isEnchanted();
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.is(ModItemsUtils.TERRAFORM_MINER_UPGRADE.get());
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return stack.is(ModItemsUtils.TERRAFORM_MINER_UPGRADE.get()) ? 15 : 0;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (!stack.is(ModItemsUtils.TERRAFORM_MINER_UPGRADE.get())) return false;
        ItemStack pickaxe = Items.DIAMOND_PICKAXE.getDefaultInstance();
        return pickaxe.supportsEnchantment(enchantment);
    }
}
