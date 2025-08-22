package com.Infinity.Nexus.Market.screen;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.networking.ModMessages;
import com.Infinity.Nexus.Market.networking.packet.SendAutoSettingsC2SPacket;
import com.Infinity.Nexus.Market.renderer.EnergyInfoArea;
import com.Infinity.Nexus.Market.renderer.InfoArea;
import com.Infinity.Nexus.Market.utils.MouseUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Optional;

public abstract class AbstractTradeScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    protected static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(InfinityNexusMarket.MOD_ID, "textures/gui/trade_gui.png");

    protected int slotManualX, slotManualY, slotAutoX, slotAutoY, slotSize, spacing;
    protected EditBox priceBox;
    protected Button decrementButton;
    protected Button incrementButton;
    protected Button confirmButton;
    protected EditBox autoAmountBox;
    protected Button autoToggleButton;
    protected boolean autoEnabled;
    protected Button autoNotifyButton;
    protected boolean autoNotify = false;
    protected final String confirmButtonKey;
    protected final String confirmTooltipKey;

    public AbstractTradeScreen(T pMenu, Inventory pPlayerInventory, Component pTitle,
                               String confirmButtonKey, String confirmTooltipKey) {
        super(pMenu, pPlayerInventory, pTitle);
        this.confirmButtonKey = confirmButtonKey;
        this.confirmTooltipKey = confirmTooltipKey;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;

        // Layout base
        slotSize = 18;
        spacing = 4;
        int baseX = this.leftPos + 8;
        int baseY = this.topPos + 10;
        slotManualX = baseX;
        slotManualY = baseY;
        slotAutoX = baseX;
        slotAutoY = slotManualY + 18;

        double savedPrice = getSavedPrice();
        int savedMinAmount = getSavedMinAmount();
        autoEnabled = isAutoEnabled();
        autoNotify = isAutoNotify();

        initPriceBox(savedPrice);
        initDecrementButton();
        initIncrementButton();
        initConfirmButton();
        initAutoAmountBox(savedMinAmount);
        initAutoToggleButton();
        initAutoNotifyButton();
    }

    protected abstract double getSavedPrice();
    protected abstract int getSavedMinAmount();
    protected abstract boolean isAutoEnabled();
    protected abstract boolean isAutoNotify();
    protected abstract void onConfirmAction(double price, BlockPos pos);
    protected abstract void saveSettingsOnClose();

    private void initPriceBox(double savedPrice) {
        priceBox = new EditBox(this.font, slotManualX + slotSize + spacing + 17, slotManualY, 36, 18,
                Component.translatable("gui.infinity_nexus_market.tooltip.price")) {
            @Override
            public void insertText(String text) {
                String filtered = text.replaceAll("[^0-9.]", "");
                super.insertText(filtered);
            }

            @Override
            public void setValue(String value) {
                String filtered = value.replaceAll("[^0-9.]", "");
                super.setValue(filtered);
            }
        };
        priceBox.setValue(String.valueOf(savedPrice));
        priceBox.setResponder(s -> sendAutoSettingsToServer());
        priceBox.setTooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.price")));
        this.addRenderableWidget(priceBox);
    }

    private void initDecrementButton() {
        decrementButton = Button.builder(Component.translatable("gui.infinity_nexus_market.button.decrement"), btn -> {
                    int decrement = hasShiftDown() ? (hasControlDown() ? 1000 : 100) : 1;
                    double current = getCurrentPrice();
                    double newValue = Math.max(0, current - decrement);
                    priceBox.setValue(String.valueOf(newValue));
                    sendAutoSettingsToServer();
                }).bounds(slotManualX + slotSize + spacing + 49 + spacing, slotManualY + 9, 9, 9)
                .tooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.decrement")))
                .build();
        this.addRenderableWidget(decrementButton);
    }

    private void initIncrementButton() {
        incrementButton = Button.builder(Component.translatable("gui.infinity_nexus_market.button.increment"), btn -> {
                    int increment = hasShiftDown() ? (hasControlDown() ? 1000 : 100) : 1;
                    double current = getCurrentPrice();
                    double newValue = current + increment;
                    priceBox.setValue(String.valueOf(newValue));
                    sendAutoSettingsToServer();
                }).bounds(slotManualX + slotSize + spacing + 49 + spacing, slotManualY, 9, 9)
                .tooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.increment")))
                .build();
        this.addRenderableWidget(incrementButton);
    }

    private void initConfirmButton() {
        confirmButton = Button.builder(Component.translatable(confirmButtonKey), btn -> {
                    double price = getCurrentPrice();
                    if (price <= 0) {
                        minecraft.player.displayClientMessage(Component.translatable("gui.infinity_nexus_market.tooltip.invalid_price"), false);
                    } else {
                        onConfirmAction(price, getBlockPos());
                    }
                }).bounds(slotManualX + slotSize + spacing + spacing + spacing + spacing + spacing + 46, slotManualY, 59, 18)
                .tooltip(Tooltip.create(Component.translatable(confirmTooltipKey)))
                .build();
        this.addRenderableWidget(confirmButton);
    }

    private void initAutoAmountBox(int savedMinAmount) {
        autoAmountBox = new EditBox(this.font, slotAutoX + slotSize + spacing + 17, slotAutoY, 36, 18,
                Component.translatable("gui.infinity_nexus_market.tooltip.auto_amount")) {
            @Override
            public void insertText(String text) {
                String filtered = text.replaceAll("[^0-9]", "");
                super.insertText(filtered);
            }

            @Override
            public void setValue(String value) {
                String filtered = value.replaceAll("[^0-9]", "");
                super.setValue(filtered);
            }
        };
        autoAmountBox.setValue(String.valueOf(savedMinAmount));
        autoAmountBox.setResponder(s -> sendAutoSettingsToServer());
        autoAmountBox.setTooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.auto_amount")));
        this.addRenderableWidget(autoAmountBox);
    }

    private void initAutoToggleButton() {
        autoToggleButton = Button.builder(Component.translatable(autoEnabled ? "gui.infinity_nexus_market.button.auto_on" : "gui.infinity_nexus_market.button.auto_off"), btn -> {
                    autoEnabled = !autoEnabled;
                    autoToggleButton.setMessage(Component.translatable(autoEnabled ? "gui.infinity_nexus_market.button.auto_on" : "gui.infinity_nexus_market.button.auto_off"));
                    sendAutoSettingsToServer();
                }).bounds(slotAutoX + slotSize + spacing + 49 + spacing, slotAutoY, 50, 18)
                .tooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.auto_toggle")))
                .build();
        this.addRenderableWidget(autoToggleButton);
    }

    private void initAutoNotifyButton() {
        autoNotifyButton = Button.builder(Component.translatable(autoNotify ? "gui.infinity_nexus_market.button.notify_on" : "gui.infinity_nexus_market.button.notify_off"), btn -> {
                    autoNotify = !autoNotify;
                    autoNotifyButton.setMessage(Component.translatable(autoNotify ? "gui.infinity_nexus_market.button.notify_on" : "gui.infinity_nexus_market.button.notify_off"));
                    sendAutoSettingsToServer();
                }).bounds(slotAutoX + slotSize + spacing + 99 + spacing, slotAutoY, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.auto_notify")))
                .build();
        this.addRenderableWidget(autoNotifyButton);
    }

    private double getCurrentPrice() {
        try {
            return Double.parseDouble(priceBox.getValue().replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void onClose() {
        saveSettingsOnClose();
        super.onClose();
    }


    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        pGuiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 74, 0XFFFFFF);
        pGuiGraphics.drawString(this.font, this.title, 8, -9, 0XFFFFFF);

        InfoArea.draw(pGuiGraphics);
        super.renderLabels(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x + 2, y - 14, 2, 167, 174, 64);
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    protected void sendAutoSettingsToServer() {
        BlockPos pos = getBlockPos();
        ModMessages.sendToServer(new SendAutoSettingsC2SPacket(
                pos,
                priceBox.getValue(),
                autoAmountBox.getValue(),
                autoEnabled,
                autoNotify
        ));
    }

    protected abstract BlockPos getBlockPos();

    private boolean isMouseAboveArea(int pMouseX, int pMouseY, int x, int y, int offsetX, int offsetY, int width, int height) {
        return MouseUtil.isMouseOver(pMouseX, pMouseY, x + offsetX, y + offsetY, width, height);
    }
}