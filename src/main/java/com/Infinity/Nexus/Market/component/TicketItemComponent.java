package com.Infinity.Nexus.Market.component;

import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

public record TicketItemComponent(String item_stack_nbt, int count, int price, String sellerName, String entryId, boolean randomSeller) {

    public static final Codec<TicketItemComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("item_stack_nbt").forGetter(TicketItemComponent::item_stack_nbt),
            Codec.INT.optionalFieldOf("count", 1).forGetter(TicketItemComponent::count),
            Codec.INT.optionalFieldOf("price", 0).forGetter(TicketItemComponent::price),
            Codec.STRING.optionalFieldOf("display_name", "Unknown").forGetter(TicketItemComponent::sellerName),
            Codec.STRING.optionalFieldOf("entry_id", "").forGetter(TicketItemComponent::entryId),
            Codec.BOOL.optionalFieldOf("random_seller", false).forGetter(TicketItemComponent::randomSeller)
    ).apply(instance, TicketItemComponent::new));

    public ItemStack toItemStack() {
        ItemStack stack = DatabaseManager.deserializeItemStack(item_stack_nbt);
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        return stack;
    }

    public static TicketItemComponent fromItemStack(String itemId, int valor, String displayName, String entryId, boolean randomSeller) {
        return new TicketItemComponent(
                itemId,
                1,
                valor,
                displayName,
                entryId,
                randomSeller
        );
    }
}


