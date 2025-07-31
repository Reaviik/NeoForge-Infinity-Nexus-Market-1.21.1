package com.Infinity.Nexus.Market.screen.market;

import com.Infinity.Nexus.Market.networking.ModMessages;
import com.Infinity.Nexus.Market.networking.packet.*;
import com.Infinity.Nexus.Market.screen.market.functions.MarketFilterManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MarketScreen extends AbstractContainerScreen<MarketMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("infinity_nexus_market", "textures/gui/market_gui.png");
    private static final UUID SERVER_UUID = UUID.fromString("00000000-0000-0000-0000-00000000c0de");
    private static List<MarketSalesSyncS2CPacket.SaleEntryDTO> clientSales = new ArrayList<>();

    private List<MarketSalesSyncS2CPacket.SaleEntryDTO> sales;
    private MarketSalesSyncS2CPacket.SaleEntryDTO selectedEntry = null;
    private boolean isMouseOverGrid = false;

    // UI elements and state
    private EditBox searchBox;
    private EditBox quantityBox;
    private String searchQuery = "";
    private int quantityToBuy = 1;
    private boolean sortPriceAscending = true;
    private boolean sortQuantityAscending = true;
    private int viewMode = 0;
    private int scrollOffset = 0;
    private String sortType = "price";
    private String floatingMessage = null;
    private long floatingMessageTime = 0;

    // Layout constants
    private static final int GRID_COLUMNS = 9;
    private static final int GRID_ROWS = 8;
    private static final int SLOT_SIZE = 18;
    private static final int BUTTON_WIDTH = 45;
    private static final int BUTTON_HEIGHT = 18;
    private static final long MESSAGE_DURATION = 2000; // ms

    public MarketScreen(MarketMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.sales = new ArrayList<>(clientSales);
    }

    public static void setClientSales(List<MarketSalesSyncS2CPacket.SaleEntryDTO> sales) {
        clientSales = sales;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x + 2, y-14, 2, 167, 174, 64);
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        pGuiGraphics.drawString(this.font, this.title, 8, -9, 0XFFFFFF);
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int gridWidth = GRID_COLUMNS * SLOT_SIZE;
        int gridHeight = GRID_ROWS * SLOT_SIZE;
        int baseX = (this.leftPos + (this.imageWidth - gridWidth) / 2) - 9;
        int baseY = this.topPos + 20;

        initSearchBox(baseX, baseY);
        initQuantityBox(baseX, baseY);
        initSortAndViewButtons(baseX, baseY);
        initActionButtons(baseX, baseY);
        initSalesGrid(baseX, baseY);
    }

    private void initSearchBox(int baseX, int baseY) {
        int buttonsY = baseY + GRID_ROWS * SLOT_SIZE + 5;
        int searchQuantityY = buttonsY - 1;
        boolean wasFocused = searchBox != null && searchBox.isFocused();
        String currentText = searchBox != null ? searchBox.getValue() : searchQuery;

        searchBox = new EditBox(this.font, baseX + 22, searchQuantityY, 73, 18,
                Component.translatable("gui.infinity_nexus_market.search"));
        searchBox.setValue(currentText);
        searchBox.setResponder(text -> {
            searchQuery = text.toLowerCase();
            selectedEntry = null;
        });
        searchBox.setTooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.search")
                .append("\n")
                .append(Component.translatable("gui.infinity_nexus_market.tooltip.search_seller")
                        .withStyle(ChatFormatting.GRAY))));
        this.addRenderableWidget(searchBox);

        if (wasFocused) {
            this.setInitialFocus(searchBox);
            searchBox.setFocused(true);
        }
    }

    private void initQuantityBox(int baseX, int baseY) {
        int buttonsY = baseY + GRID_ROWS * SLOT_SIZE + 5;
        int qtyQuantityY = buttonsY - 1;
        boolean wasFocused = quantityBox != null && quantityBox.isFocused();
        String currentText = quantityBox != null ? quantityBox.getValue() : String.valueOf(quantityToBuy);

        quantityBox = new EditBox(this.font, baseX + 95, qtyQuantityY, 45, 18,
                Component.translatable("gui.infinity_nexus_market.quantity"));

        quantityBox.setTooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.quantity", quantityToBuy)));
        quantityBox.setValue(currentText);
        quantityBox.setFilter(s -> {
            if (s.isEmpty()) return true;
            try {
                int qty = Integer.parseInt(s);
                return qty > 0 && qty <= 64;
            } catch (NumberFormatException e) {
                return false;
            }
        });

        quantityBox.setResponder(text -> {
            if (text.isEmpty()) {
                quantityToBuy = 1;
                return;
            }
            try {
                quantityToBuy = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                quantityToBuy = 1;
            }
        });

        this.addRenderableWidget(quantityBox);

        // Add increment button
        this.addRenderableWidget(Button.builder(
                        Component.literal("+"),
                        btn -> {
                            int amount = 1;
                            if (hasShiftDown()) amount = 100;
                            if (hasControlDown()) amount = 1000;

                            quantityToBuy = Math.min(64, quantityToBuy + amount);
                            quantityBox.setValue(String.valueOf(quantityToBuy));
                        })
                .bounds(baseX + 95 + 45, qtyQuantityY, 9, 9)
                .tooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.increment")))
                .build());

        // Add decrement button
        this.addRenderableWidget(Button.builder(
                        Component.literal("-"),
                        btn -> {
                            int amount = 1;
                            if (hasShiftDown()) amount = 100;
                            if (hasControlDown()) amount = 1000;

                            quantityToBuy = Math.max(1, quantityToBuy - amount);
                            quantityBox.setValue(String.valueOf(quantityToBuy));
                        })
                .bounds(baseX + 95 + 45, qtyQuantityY + 8, 9, 9)
                .tooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.decrement")))
                .build());

        if (wasFocused) {
            this.setInitialFocus(quantityBox);
            quantityBox.setFocused(true);
        }
    }

    private void initSortAndViewButtons(int baseX, int baseY) {
        final int BUTTON_X = baseX + GRID_COLUMNS * SLOT_SIZE + 16;
        final int BUTTON_Y_START = baseY - 10;
        final int BUTTON_SPACING = 18;

        // View mode button
        String[] modeIcons = {"🌐", "👤", "🖥"};
        Component[] modeTooltips = {
                Component.translatable("gui.infinity_nexus_market.mode_name.all"),
                Component.translatable("gui.infinity_nexus_market.mode_name.mine"),
                Component.translatable("gui.infinity_nexus_market.mode_name.server")
        };

        Button viewModeButton = Button.builder(
                        Component.literal(modeIcons[viewMode]),
                        btn -> {
                            viewMode = (viewMode + 1) % 3;
                            selectedEntry = null;
                            this.init();
                        })
                .bounds(BUTTON_X, BUTTON_Y_START, 18, 18)
                .build();
        viewModeButton.setTooltip(Tooltip.create(modeTooltips[viewMode]));
        this.addRenderableWidget(viewModeButton);

        // Sort by price button
        Button sortPriceButton = Button.builder(
                        Component.literal(sortPriceAscending ? "↑$" : "↓$"),
                        btn -> {
                            sortType = "price";
                            sortPriceAscending = !sortPriceAscending;
                            selectedEntry = null;
                            this.init();
                        })
                .bounds(BUTTON_X, BUTTON_Y_START + BUTTON_SPACING, 18, 18)
                .build();
        sortPriceButton.setTooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.sort_price")));
        this.addRenderableWidget(sortPriceButton);

        // Sort by quantity button
        Button sortQuantityButton = Button.builder(
                        Component.literal(sortQuantityAscending ? "↑Q" : "↓Q"),
                        btn -> {
                            sortType = "quantity";
                            sortQuantityAscending = !sortQuantityAscending;
                            selectedEntry = null;
                            this.init();
                        })
                .bounds(BUTTON_X, BUTTON_Y_START + BUTTON_SPACING * 2, 18, 18)
                .build();
        sortQuantityButton.setTooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.sort_quantity")));
        this.addRenderableWidget(sortQuantityButton);

        // Filter button
        Button filterButton = Button.builder(
                        MarketFilterManager.getFilterButtonText(),
                        btn -> {
                            MarketFilterManager.cycleFilter();
                            selectedEntry = null;
                            this.init();
                        })
                .bounds(BUTTON_X, BUTTON_Y_START + BUTTON_SPACING * 3, 18, 18)
                .build();
        filterButton.setTooltip(Tooltip.create(MarketFilterManager.getFilterTooltip()));
        this.addRenderableWidget(filterButton);

        // Scroll up button
        this.addRenderableWidget(Button.builder(
                        Component.literal("▲"),
                        btn -> {
                            int step = hasShiftDown() ? GRID_ROWS * 9 : 9;
                            if (scrollOffset > 0) {
                                scrollOffset = Math.max(0, scrollOffset - step);
                                selectedEntry = null;
                                this.init();
                            }
                        })
                .bounds(BUTTON_X, BUTTON_Y_START + BUTTON_SPACING * 4, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.scroll_up")))
                .build());

        // Scroll down button
        this.addRenderableWidget(Button.builder(
                        Component.literal("▼"),
                        btn -> {
                            int step = hasShiftDown() ? GRID_ROWS * 9 : 9;
                            int maxOffset = Math.max(0, getFilteredSales().size() - (GRID_COLUMNS * GRID_ROWS));
                            if (scrollOffset + (GRID_COLUMNS * GRID_ROWS) < getFilteredSales().size()) {
                                scrollOffset = Math.min(maxOffset, scrollOffset + step);
                                selectedEntry = null;
                                this.init();
                            }
                        })
                .bounds(BUTTON_X, BUTTON_Y_START + BUTTON_SPACING * 5, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.scroll_down")))
                .build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (!isMouseOverGrid) {
            return super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
        }

        double scrollAmount = -scrollDeltaY*9;
        if (hasShiftDown()) {
            scrollAmount *= GRID_ROWS*8;
        }

        int newScrollOffset = scrollOffset + (int)scrollAmount;
        int maxOffset = Math.max(0, getFilteredSales().size() - (GRID_COLUMNS * GRID_ROWS));
        newScrollOffset = Math.max(0, Math.min(maxOffset, newScrollOffset));

        if (newScrollOffset != scrollOffset) {
            scrollOffset = newScrollOffset;
            selectedEntry = null;
            this.init();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
    }

    private void initActionButtons(int baseX, int baseY) {
        int buttonsY = baseY + GRID_ROWS * SLOT_SIZE + 22;
        int buttonsStartX = baseX + (GRID_COLUMNS * SLOT_SIZE - (BUTTON_WIDTH * 2 + 28)) / 2;

        // Buy button
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.infinity_nexus_market.button.buy"),
                        btn -> handleBuyButtonClick())
                .bounds(buttonsStartX, buttonsY, BUTTON_WIDTH+5, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.buy")))
                .build());

        // Remove button
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.infinity_nexus_market.button.remove"),
                        btn -> handleRemoveButtonClick())
                .bounds(buttonsStartX + BUTTON_WIDTH+5, buttonsY, BUTTON_WIDTH+5, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.remove_from_sale")))
                .build());

        // Copy to ticket button
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.infinity_nexus_market.button.copy_to_ticket"),
                        btn -> handleCopyToTicketButtonClick())
                .bounds(buttonsStartX + BUTTON_WIDTH * 2 + 10, buttonsY, 18, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("gui.infinity_nexus_market.tooltip.copy_to_ticket")
                        .append("\n")
                        .append(Component.translatable("gui.infinity_nexus_market.tooltip.copy_to_ticket_seller")
                                .withStyle(ChatFormatting.GRAY))))
                .build());
    }

    private void initSalesGrid(int baseX, int baseY) {
        List<MarketSalesSyncS2CPacket.SaleEntryDTO> filteredSales = getFilteredSales();

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int index = scrollOffset + row * GRID_COLUMNS + col;
                if (index >= filteredSales.size()) continue;

                MarketSalesSyncS2CPacket.SaleEntryDTO entry = filteredSales.get(index);
                int entryX = baseX + 9 + col * SLOT_SIZE;
                int entryY = baseY - 9 + row * SLOT_SIZE;

                Button selectButton = Button.builder(Component.empty(), btn -> {
                            selectedEntry = entry.equals(selectedEntry) ? null : entry;
                            this.init();
                        })
                        .bounds(entryX, entryY, SLOT_SIZE, SLOT_SIZE)
                        .build();
                selectButton.setAlpha(0);
                this.addRenderableWidget(selectButton);
            }
        }
    }

    private void handleBuyButtonClick() {
        if (selectedEntry == null) {
            showFloatingMessage(Component.translatable("gui.infinity_nexus_market.floating.no_item_selected").getString());
            return;
        }

        if (selectedEntry.price <= 0) {
            showFloatingMessage(Component.translatable("gui.infinity_nexus_market.floating.invalid_price").getString());
            return;
        }

        if (quantityToBuy < 1 || quantityToBuy > 64) {
            showFloatingMessage(Component.translatable("gui.infinity_nexus_market.floating.invalid_quantity").getString());
            return;
        }

        ModMessages.sendToServer(new MarketBuyC2SPacket(UUID.fromString(selectedEntry.transactionId), quantityToBuy));
        showFloatingMessage(Component.translatable("gui.infinity_nexus_market.floating.buy_request_sent").getString());
        requestMarketSync();
    }

    private void handleRemoveButtonClick() {
        if (selectedEntry == null) {
            showFloatingMessage(Component.translatable("gui.infinity_nexus_market.floating.no_item_selected").getString());
            return;
        }

        boolean isOwner = menu.getPlayer().getUUID().toString().equals(selectedEntry.seller) || menu.getPlayer().isCreative();
        boolean isOp = menu.getPlayer().hasPermissions(2);

        if (!isOwner && !isOp) {
            showFloatingMessage(Component.translatable("gui.infinity_nexus_market.floating.cant_remove").getString());
            return;
        }

        ModMessages.sendToServer(new MarketRemoveSaleC2SPacket(UUID.fromString(selectedEntry.transactionId)));
        showFloatingMessage(Component.translatable("gui.infinity_nexus_market.floating.remove_request_sent").getString());
        selectedEntry = null;
        requestMarketSync();
    }

    private void handleCopyToTicketButtonClick() {
        if (selectedEntry == null) {
            showFloatingMessage(Component.translatable("gui.infinity_nexus_market.floating.no_item_selected").getString());
            return;
        }

        ModMessages.sendToServer(new CopyToTicketC2SPacket(
                selectedEntry.item.copy(),
                selectedEntry.quantity,
                selectedEntry.price,
                selectedEntry.sellerName,
                selectedEntry.transactionId
        ));
        showFloatingMessage(Component.translatable("gui.infinity_nexus_market.floating.copied_to_ticket").getString());
    }

    private void requestMarketSync() {
        ModMessages.sendToServer(new RequestMarketSyncC2SPacket());
    }

    private List<MarketSalesSyncS2CPacket.SaleEntryDTO> getFilteredSales() {
        return sales.stream()
                .filter(this::filterEntry)
                .sorted((e1, e2) -> {
                    if (sortType.equals("quantity")) {
                        return sortQuantityAscending ?
                                Integer.compare(e1.quantity, e2.quantity) :
                                Integer.compare(e2.quantity, e1.quantity);
                    } else {
                        return sortPriceAscending ?
                                Double.compare(e1.price, e2.price) :
                                Double.compare(e2.price, e1.price);
                    }
                })
                .collect(Collectors.toList());
    }

    private boolean filterEntry(MarketSalesSyncS2CPacket.SaleEntryDTO entry) {
        if (entry.item == null || entry.item.isEmpty()) return false;

        if (!MarketFilterManager.getCurrentFilter().test(entry.item)) {
            return false;
        }

        if (searchQuery.startsWith("#")) {
            String sellerSearch = searchQuery.substring(1).toLowerCase();
            if (entry.seller != null) {
                if (SERVER_UUID.toString().equals(entry.seller)) {
                    return "server".contains(sellerSearch);
                }
                return entry.sellerName != null &&
                        entry.sellerName.toLowerCase().contains(sellerSearch);
            }
            return false;
        }

        String itemName = entry.item.getHoverName().getString().toLowerCase();
        boolean matchItem = itemName.contains(searchQuery);
        boolean matchSeller = entry.sellerName != null &&
                entry.sellerName.toLowerCase().contains(searchQuery);

        boolean searchMatch = matchItem || matchSeller;

        if (viewMode == 1) { // "Meus Itens"
            return searchMatch &&
                    entry.seller != null &&
                    entry.seller.equals(menu.getPlayer().getUUID().toString());
        }
        if (viewMode == 2) { // "Itens do Servidor"
            return searchMatch &&
                    entry.seller != null &&
                    entry.seller.equals(SERVER_UUID.toString());
        }
        return searchMatch; // "Todos"
    }

    private void showFloatingMessage(String message) {
        floatingMessage = message;
        floatingMessageTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        updateSalesFromClientData();
        int gridX = (this.width - GRID_COLUMNS * SLOT_SIZE) / 2;
        int gridY = this.topPos + 11;
        int gridWidth = GRID_COLUMNS * SLOT_SIZE;
        int gridHeight = GRID_ROWS * SLOT_SIZE;

        this.isMouseOverGrid = mouseX >= gridX && mouseX <= gridX + gridWidth &&
                mouseY >= gridY && mouseY <= gridY + gridHeight;

        super.render(guiGraphics, mouseX, mouseY, delta);
        renderSalesGrid(guiGraphics, mouseX, mouseY);
        renderFloatingMessage(guiGraphics);
        renderSelection(guiGraphics);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void updateSalesFromClientData() {
        if (!clientSales.equals(this.sales)) {
            this.sales = new ArrayList<>(clientSales);
            if (selectedEntry != null) {
                selectedEntry = sales.stream()
                        .filter(e -> e.transactionId.equals(selectedEntry.transactionId))
                        .findFirst()
                        .orElse(null);
            }
            this.init();
        }
    }

    private void renderSalesGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<MarketSalesSyncS2CPacket.SaleEntryDTO> filteredSales = getFilteredSales();
        int baseX = this.leftPos + (this.imageWidth - GRID_COLUMNS * SLOT_SIZE) / 2;
        int baseY = this.topPos + 11;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int index = scrollOffset + row * GRID_COLUMNS + col;
                if (index >= filteredSales.size()) continue;

                MarketSalesSyncS2CPacket.SaleEntryDTO entry = filteredSales.get(index);
                if (entry.item == null || entry.item.isEmpty()) continue;

                int entryX = baseX + col * SLOT_SIZE;
                int entryY = baseY + row * SLOT_SIZE;

                guiGraphics.blit(TEXTURE,
                        entryX, entryY,
                        176, 0,
                        SLOT_SIZE, SLOT_SIZE);

                if (SERVER_UUID.toString().equals(entry.seller)) {
                    guiGraphics.fill(entryX+1, entryY+1, entryX + SLOT_SIZE-1, entryY + SLOT_SIZE-1, 0x40FFFF00);
                }

                guiGraphics.renderItem(entry.item, entryX + 1, entryY + 1);

                String quantityText = String.valueOf(Math.min(entry.quantity, 64));
                int textWidth = this.font.width(quantityText);
                int xPos = entryX + 13 - (textWidth - this.font.width("1"));
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 200);
                int color = SERVER_UUID.toString().equals(entry.seller) ? 0x00FFFF00 : 0x00FFFFFF;
                guiGraphics.drawString(this.font, quantityText, xPos, entryY + 9, color, true);
                guiGraphics.pose().popPose();

                if (isMouseOverSlot(mouseX, mouseY, entryX, entryY, SLOT_SIZE)) {
                    renderItemTooltip(guiGraphics, mouseX, mouseY, entry);
                }
            }
        }
    }

    private void renderSelection(GuiGraphics guiGraphics) {
        if (selectedEntry != null) {
            List<MarketSalesSyncS2CPacket.SaleEntryDTO> filteredSales = getFilteredSales();
            int baseX = this.leftPos + (this.imageWidth - GRID_COLUMNS * SLOT_SIZE) / 2;
            int baseY = this.topPos + 11;

            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLUMNS; col++) {
                    int index = scrollOffset + row * GRID_COLUMNS + col;
                    if (index >= filteredSales.size()) continue;

                    MarketSalesSyncS2CPacket.SaleEntryDTO entry = filteredSales.get(index);
                    if (entry.equals(selectedEntry)) {
                        int entryX = baseX + col * SLOT_SIZE;
                        int entryY = baseY + row * SLOT_SIZE;
                        guiGraphics.fill(entryX+1, entryY+1, entryX + SLOT_SIZE-1, entryY + SLOT_SIZE-1, 0x4000FFFF);
                        break;
                    }
                }
            }
        }
    }

    private boolean isMouseOverSlot(int mouseX, int mouseY, int slotX, int slotY, int size) {
        return mouseX >= slotX + 1 && mouseX <= slotX + size - 1 &&
                mouseY >= slotY + 1 && mouseY <= slotY + size - 1;
    }

    private void renderItemTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, MarketSalesSyncS2CPacket.SaleEntryDTO entry) {
        List<Component> tooltip = entry.item.getTooltipLines(
                Item.TooltipContext.of(minecraft.level),
                this.minecraft.player,
                this.minecraft.options.advancedItemTooltips ?
                        TooltipFlag.Default.ADVANCED :
                        TooltipFlag.Default.NORMAL
        );

        tooltip = new ArrayList<>(tooltip);
        tooltip.add(Component.translatable("gui.infinity_nexus_market.tooltip.item_price", "§6"+entry.price));

        if (entry.quantity > 0) {
            tooltip.add(Component.translatable("gui.infinity_nexus_market.tooltip.quantity", "§6"+entry.quantity));
        }

        if (entry.seller != null) {
            String sellerName;
            if (SERVER_UUID.toString().equals(entry.seller)) {
                sellerName = Component.translatable("gui.infinity_nexus_market.server_name")
                        .withStyle(ChatFormatting.YELLOW)
                        .getString();
            } else {
                sellerName = entry.sellerName != null ? entry.sellerName : "Unknown";
            }
            tooltip.add(Component.translatable("gui.infinity_nexus_market.tooltip.seller", "§b"+sellerName));
        }

        if (entry.date != null) {
            tooltip.add(Component.translatable("gui.infinity_nexus_market.tooltip.timestamp", "§6"+ entry.date));
        }

        guiGraphics.renderTooltip(this.font, tooltip, entry.item.getTooltipImage(), mouseX + 1, mouseY - 1);
    }

    private void renderFloatingMessage(GuiGraphics guiGraphics) {
        if (floatingMessage != null && (System.currentTimeMillis() - floatingMessageTime) < MESSAGE_DURATION) {
            int msgWidth = this.font.width(floatingMessage);
            int cx = this.leftPos + (this.imageWidth - msgWidth) / 2;
            int cy = this.topPos + 176;

            guiGraphics.fill(cx - 8, cy - 2, cx + msgWidth + 8, cy + 12, 0xCC222222);
            guiGraphics.drawString(this.font, floatingMessage, cx, cy, 0xFFFFAA00);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            if (keyCode == 256) { // ESC
                this.onClose();
                return true;
            }
            return searchBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        for (var widget : this.renderables) {
            if (widget instanceof AbstractWidget abstractWidget &&
                    abstractWidget.isMouseOver(mouseX, mouseY)) {
                abstractWidget.render(guiGraphics, mouseX, mouseY, 0);
                break;
            }
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        clientSales.clear();
    }
}