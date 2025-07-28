package com.Infinity.Nexus.Market.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record TicketItemComponent(String itemId, int count, int price, String sellerName, String entryId) {

    public static final Codec<TicketItemComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("item_id").forGetter(TicketItemComponent::itemId),
            Codec.INT.optionalFieldOf("count", 1).forGetter(TicketItemComponent::count),
            Codec.INT.optionalFieldOf("price", 0).forGetter(TicketItemComponent::price),
            Codec.STRING.optionalFieldOf("display_name", "").forGetter(TicketItemComponent::sellerName),
            Codec.STRING.optionalFieldOf("entry_id", "").forGetter(TicketItemComponent::entryId)
    ).apply(instance, TicketItemComponent::new));

    public ItemStack toItemStack() {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(itemId));
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item, count);
    }

    public static TicketItemComponent fromItemStack(ItemStack stack, int valor, String displayName, String entryId) {
        return new TicketItemComponent(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getCount(),
                valor,
                displayName,
                entryId
        );
    }
}


