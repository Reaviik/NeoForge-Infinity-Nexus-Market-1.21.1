package com.Infinity.Nexus.Market.screen.buying;

import com.Infinity.Nexus.Market.networking.ModMessages;
import com.Infinity.Nexus.Market.networking.packet.BuyItemC2SPacket;
import com.Infinity.Nexus.Market.screen.AbstractTradeScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BuyingScreen extends AbstractTradeScreen<BuyingMenu> {

    public BuyingScreen(BuyingMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle,
                "gui.infinity_nexus_market.button.buy",
                "gui.infinity_nexus_market.tooltip.buy");
    }

    @Override
    protected double getSavedPrice() {
        return menu.getBlockEntity().getAutoPrice();
    }

    @Override
    protected int getSavedMinAmount() {
        return menu.getBlockEntity().getAutoMinAmount();
    }

    @Override
    protected boolean isAutoEnabled() {
        return menu.getBlockEntity().isAutoEnabled();
    }

    @Override
    protected boolean isAutoNotify() {
        return menu.getBlockEntity().isAutoNotify();
    }

    @Override
    protected void onConfirmAction(double price, BlockPos pos) {
        ModMessages.sendToServer(new BuyItemC2SPacket(pos, price));
    }

    @Override
    protected void saveSettingsOnClose() {
        menu.blockEntity.setAutoEnabled(autoEnabled);
        menu.blockEntity.setAutoMinAmount(Integer.parseInt(autoAmountBox.getValue().replaceAll("[^0-9]", "")));
        menu.blockEntity.setAutoPrice(Double.parseDouble(priceBox.getValue().replaceAll("[^0-9.]+", "")));
        menu.blockEntity.setAutoNotify(autoNotify);
    }

    @Override
    protected BlockPos getBlockPos() {
        return menu.getBlockEntity().getBlockPos();
    }
}