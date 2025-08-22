package com.Infinity.Nexus.Market.item.custom;

import com.Infinity.Nexus.Market.component.MarketDataComponents;
import com.Infinity.Nexus.Market.component.TicketItemComponent;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class Ticket extends Item {
    public Ticket(Item.Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        InteractionHand inverseHand = usedHand == InteractionHand.OFF_HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (!level.isClientSide()) {
            ItemStack itemStack = player.getItemInHand(usedHand);
            ItemStack ticketStack = player.getItemInHand(inverseHand).copy();

            if (itemStack.has(MarketDataComponents.TICKET_ITEM.get())) {
                TicketItemComponent ticket = itemStack.get(MarketDataComponents.TICKET_ITEM.get());
                if(player.isShiftKeyDown()) {
                    itemStack.remove(MarketDataComponents.TICKET_ITEM.get());
                    return InteractionResultHolder.success(itemStack);
                }
                itemStack.set(MarketDataComponents.TICKET_ITEM.get(), TicketItemComponent.fromItemStack(ticket.item_stack_nbt(), ticket.price(), ticket.sellerName(), "", !ticket.randomSeller()));
                return InteractionResultHolder.success(itemStack);
            }

            if (ticketStack.isEmpty()) {
                return InteractionResultHolder.fail(player.getItemInHand(usedHand));
            }

            ticketStack.setCount(1);
            String serializedTicket = DatabaseManager.serializeItemStack(ticketStack);
            itemStack.set(MarketDataComponents.TICKET_ITEM.get(), TicketItemComponent.fromItemStack(serializedTicket, 0, Component.translatable("item.infinity_nexus_market.ticket_anyone").getString(), "", true));
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(MarketDataComponents.TICKET_ITEM.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext tooltip, List<Component> components, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            if (!stack.has(MarketDataComponents.TICKET_ITEM.get())) {
                components.add(Component.translatable("tooltip.infinity_nexus_market.ticket_empty"));
                components.add(Component.translatable("tooltip.infinity_nexus_market.ticket_empty_description"));
                return;
            }

            TicketItemComponent ticket = stack.get(MarketDataComponents.TICKET_ITEM.get());
            ItemStack ticketStack = ticket.toItemStack();
            components.add(Component.translatable("tooltip.infinity_nexus_market.ticket_content", ticketStack.getCount(), ticketStack.getHoverName()));
            components.add(Component.translatable("tooltip.infinity_nexus_market.ticket_price", ticket.price()));
            components.add(Component.translatable("tooltip.infinity_nexus_market.ticket_random_seller", ticket.randomSeller() ? "Yes" : "No"));
            components.add(Component.translatable("tooltip.infinity_nexus_market.ticket_seller", ticket.randomSeller() ? Component.translatable("item.infinity_nexus_market.ticket_anyone").getString() : ticket.sellerName()));

        } else {
            components.add(Component.translatable("tooltip.infinity_nexus_market.pressShift"));
        }

        super.appendHoverText(stack, tooltip, components, flag);
    }

}
