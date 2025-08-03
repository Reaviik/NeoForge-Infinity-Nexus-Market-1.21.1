package com.Infinity.Nexus.Market.component;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.google.common.base.Suppliers;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/*
*
* Credits
* https://github.com/InnovativeOnlineIndustries/Industrial-Foregoing/blob/1.21/src/main/java/com/buuz135/industrial/api/IMachineSettings.java#L25
*
*
*/
public class MarketDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE , InfinityNexusMarket.MOD_ID);

    public static final Supplier<DataComponentType<TicketItemComponent>> TICKET_ITEM = register(
            "ticket_item",
            () -> new TicketItemComponent("minecraft:air", 1, 0, "Unknown", UUID.randomUUID().toString(), true),
            op -> op.persistent(TicketItemComponent.CODEC)
    );

    private static <T> ComponentSupplier<T> register(String name, Supplier<T> defaultVal, UnaryOperator<DataComponentType.Builder<T>> op) {
        var registered = DATA_COMPONENT_TYPES.register(name, () -> op.apply(DataComponentType.builder()).build());
        return new ComponentSupplier<>(registered, defaultVal);
    }

    private static <T>DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static class ComponentSupplier<T> implements Supplier<DataComponentType<T>> {
        private final Supplier<DataComponentType<T>> type;
        private final Supplier<T> defaultSupplier;

        public ComponentSupplier(Supplier<DataComponentType<T>> type, Supplier<T> defaultSupplier) {
            this.type = type;
            this.defaultSupplier = Suppliers.memoize(defaultSupplier::get);
        }

        public T get(ItemStack stack) {
            return stack.getOrDefault(type, defaultSupplier.get());
        }

        @Override
        public DataComponentType<T> get() {
            return type.get();
        }
    }
    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}