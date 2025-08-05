package com.Infinity.Nexus.Market;

import com.Infinity.Nexus.Market.block.ModBlocksMarket;
import com.Infinity.Nexus.Market.block.entity.ModBlockEntities;
import com.Infinity.Nexus.Market.block.entity.client.AnimatedBlockRender;
import com.Infinity.Nexus.Market.component.MarketDataComponents;
import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.item.ModItemsMarket;
import com.Infinity.Nexus.Market.networking.ModMessages;
import com.Infinity.Nexus.Market.screen.ModMenuTypes;
import com.Infinity.Nexus.Market.screen.buying.BuyingScreen;
import com.Infinity.Nexus.Market.screen.market.MarketScreen;
import com.Infinity.Nexus.Market.screen.seller.VendingScreen;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import com.Infinity.Nexus.Market.tab.ModTab;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.slf4j.Logger;

import java.sql.SQLException;

@Mod(InfinityNexusMarket.MOD_ID)
public class InfinityNexusMarket {
    long time = System.currentTimeMillis();
    public static final String MOD_ID = "infinity_nexus_market";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static ServerLevel serverLevel;

    public InfinityNexusMarket(IEventBus modEventBus, ModContainer modContainer){

        ModTab.register(modEventBus);

        ModItemsMarket.register(modEventBus);
        ModBlocksMarket.register(modEventBus);

        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        MarketDataComponents.register(modEventBus);

        modEventBus.register(ModMessages.class);
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::registerScreens);

        modContainer.registerConfig(ModConfig.Type.SERVER, ModConfigs.SPEC);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.VENDING_MENU.get(), VendingScreen::new);
        event.register(ModMenuTypes.BUYING_MENU.get(), BuyingScreen::new);
        event.register(ModMenuTypes.MARKET_MENU.get(), MarketScreen::new);
    }

    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            BlockEntityRenderers.register(ModBlockEntities.VENDING_MACHINE_BE.get(), context -> new AnimatedBlockRender("vending_machine"));
            BlockEntityRenderers.register(ModBlockEntities.BUYING_MACHINE_BE.get(), context -> new AnimatedBlockRender("buying_machine"));
            BlockEntityRenderers.register(ModBlockEntities.MARKET_MACHINE_BE.get(), context -> new AnimatedBlockRender("market_machine"));
        }
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("   §4_____§5_   __§9__________§3_   ______§b_______  __");
        LOGGER.info("  §4/_  _§5/ | / §9/ ____/  _§3/ | / /  _§b/_  __| \\/ /");
        LOGGER.info("   §4/ /§5/  |/ §9/ /_   / /§3/  |/ // /  §b/ /   \\  / ");
        LOGGER.info(" §4_/ /§5/ /|  §9/ __/ _/ /§3/ /|  // /  §b/ /    / /  ");
        LOGGER.info("§4/___§5/_/ |_§9/_/   /___§3/_/ |_/___/ §b/_/    /_/   ");
        LOGGER.info("§b             Infinty Nexus Utils");
        LOGGER.info("§f[§bMarket§f]: §cTempo de carregamento: {} ms", System.currentTimeMillis() - time);
    }
}
