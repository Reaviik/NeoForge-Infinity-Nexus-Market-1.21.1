package com.Infinity.Nexus.Market.screen;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.screen.buying.BuyingMenu;
import com.Infinity.Nexus.Market.screen.market.MarketMenu;
import com.Infinity.Nexus.Market.screen.seller.VendingMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, InfinityNexusMarket.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<VendingMenu>> VENDING_MENU = registerMenuType("vending_menu", VendingMenu::new);
    public static final DeferredHolder<MenuType<?>, MenuType<BuyingMenu>> BUYING_MENU = registerMenuType("buying_menu", BuyingMenu::new);
    public static final DeferredHolder<MenuType<?>, MenuType<MarketMenu>> MARKET_MENU = registerMenuType("market_menu", MarketMenu::new);

    public static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
