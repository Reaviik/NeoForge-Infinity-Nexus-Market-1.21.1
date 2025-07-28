package com.Infinity.Nexus.Market.item.custom;

import com.Infinity.Nexus.Market.component.MarketDataComponents;
import com.Infinity.Nexus.Market.component.TicketItemComponent;
import com.Infinity.Nexus.Market.market.SQLiteManager;
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
        if (!level.isClientSide() && player.isShiftKeyDown()) {
            ItemStack itemStack = player.getItemInHand(usedHand);

            // Remover
            if (itemStack.has(MarketDataComponents.TICKET_ITEM.get())) {
                itemStack.remove(MarketDataComponents.TICKET_ITEM.get());
            }

            // Adicionar novo
            itemStack.set(MarketDataComponents.TICKET_ITEM.get(),
                    TicketItemComponent.fromItemStack(player.getItemInHand(inverseHand), 0, Component.translatable("item.infinity_nexus_market.ticket_anyone").getString(), "")
            );
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
                return;
            }

            TicketItemComponent ticket = stack.get(MarketDataComponents.TICKET_ITEM.get());
            ItemStack ticketStack = ticket.toItemStack();
            components.add(Component.translatable("tooltip.infinity_nexus_market.ticket_content",ticketStack.getCount(),ticketStack.getHoverName()));
            components.add(Component.translatable("tooltip.infinity_nexus_market.ticket_price", ticket.price()));
            String sellerName = ticket.sellerName();
            if ((sellerName == null || sellerName.isEmpty()) && ticket.entryId() != null && !ticket.entryId().isEmpty()) {
                // Busca o nome do vendedor na database market
                sellerName = SQLiteManager.getSellerNameByEntryId(ticket.entryId());
                if (sellerName == null || sellerName.isEmpty()) sellerName = "?";
            }
            components.add(Component.translatable("tooltip.infinity_nexus_market.ticket_seller", sellerName));

        } else {
            components.add(Component.translatable("tooltip.infinity_nexus_market.pressShift"));
        }

        super.appendHoverText(stack, tooltip, components, flag);
    }

}
