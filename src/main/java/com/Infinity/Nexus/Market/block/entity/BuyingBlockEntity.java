package com.Infinity.Nexus.Market.block.entity;

import com.Infinity.Nexus.Market.component.MarketDataComponents;
import com.Infinity.Nexus.Market.component.TicketItemComponent;
import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.item.ModItemsMarket;
import com.Infinity.Nexus.Market.itemStackHandler.RestrictedItemStackHandler;
import com.Infinity.Nexus.Market.screen.buying.BuyingMenu;
import com.Infinity.Nexus.Market.utils.ItemStackHandlerUtils;
import com.Infinity.Nexus.Market.market.SQLiteManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BuyingBlockEntity extends AbstractMarketBlockEntity {
    private static final int TICKET_SLOT = 0;
    private static final int AUTO_SLOT = 1;
    private int progress = 0;
    private int maxProgress = ModConfigs.buyingTicksPerOperation;

    public BuyingBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.BUYING_MACHINE_BE.get(), pPos, pBlockState, 2);
    }

    @Override
    protected RestrictedItemStackHandler createItemHandler(int slots) {
        return new RestrictedItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                setChanged();
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                }
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return switch (slot) {
                    case 0 -> stack.is(ModItemsMarket.TICKET.get());
                    case 1 -> true;
                    default -> false;
                };
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate, boolean fromAutomation) {
                if (slot == 1) {
                    return super.extractItem(slot, amount, simulate, false);
                }
                return super.extractItem(slot, amount, simulate, fromAutomation);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot == 0) {
                    return stack;
                }
                return super.insertItem(slot, stack, simulate);
            }
        };
    }

    @Override
    protected ContainerData createContainerData() {
        return new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> owner == null ? 0 : 1;
                    case 3 -> ownerName == null ? 0 : 1;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> progress = pValue;
                    case 1 -> maxProgress = pValue;
                    case 2 -> owner = pValue == 1 ? "" : null;
                    case 3 -> ownerName = pValue == 1 ? "" : null;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    @Override
    protected int getEnergyCapacity() {
        return ModConfigs.buyingEnergyCapacity;
    }

    @Override
    protected int getEnergyTransfer() {
        return ModConfigs.buyingEnergyTransfer;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infinity_nexus_market.buying_machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new BuyingMenu(pContainerId, pPlayerInventory, this, this.data, this.itemHandler);
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        if (pLevel.isClientSide) return;

        if (autoEnabled) {
            autoBuyInMarket();
        }

        if(owner == null || ownerName == null) {
            return;
        }

        if (itemHandler.getStackInSlot(TICKET_SLOT).isEmpty()) {
            return;
        }

        TicketItemComponent itemComponent = isValidTicket();
        if(itemComponent == null){
            return;
        }

        if(!ownerHasBalance(itemComponent.price())){
            return;
        }

        if(!slotAcceptItem(itemComponent.toItemStack())){
            return;
        }

        processAutoBuy(itemComponent);
    }

    private boolean slotAcceptItem(ItemStack itemStack) {
        return ItemStackHandlerUtils.canInsertItemAndAmountIntoOutputSlot(itemStack.getItem(), itemStack.getCount(), AUTO_SLOT, itemHandler);
    }

    private boolean ownerHasBalance(int price) {
        return SQLiteManager.getPlayerBalance(owner) >= price;
    }

    private TicketItemComponent isValidTicket() {
        ItemStack ticket = itemHandler.getStackInSlot(TICKET_SLOT);
        if (!ticket.has(MarketDataComponents.TICKET_ITEM.get())) {
            return null;
        }
        TicketItemComponent itemComponent = ticket.get(MarketDataComponents.TICKET_ITEM.get());

        if(itemComponent.price() <= 0 || itemComponent.toItemStack().isEmpty()){
            return null;
        }
        return itemComponent;
    }

    public void autoBuyInMarket() {
    }

    public void selfBuyInMarket(ServerPlayer player) {
        if(player == null){
            return;
        }
        executeBuyTransaction(player, isValidTicket());
    }

    public void processAutoBuy(TicketItemComponent itemComponent) {
        Player player = level.getPlayerByUUID(UUID.fromString(owner));
        if(player == null){
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        executeBuyTransaction(serverPlayer, itemComponent);
    }

    private void executeBuyTransaction(ServerPlayer player, TicketItemComponent itemComponent) {
        if (level == null) {
            return;
        }

        List<SQLiteManager.MarketItemEntry> marketItems = SQLiteManager.getAllMarketItems();

        for (SQLiteManager.MarketItemEntry entry : marketItems) {
            ItemStack saleItem = SQLiteManager.deserializeItemStack(entry.itemNbt, (ServerLevel) level);
            int saleQuantity = entry.quantity;
            double salePrice = entry.currentPrice;

            double price = autoPrice != 0 ? autoPrice : itemComponent.price();
            int quantity = autoMinAmount != 0 ? autoMinAmount : itemComponent.count();

            boolean sameItem = ItemStack.isSameItemSameComponents(saleItem, itemComponent.toItemStack());
            boolean samePrice = salePrice == price;

            if (sameItem && samePrice) {
                // Verifica se a quantidade disponível é suficiente
                if (saleQuantity < quantity) {
                    quantity = saleQuantity; // Ajusta para comprar apenas o disponível
                }

                // Verifica saldo do jogador
                double totalCost = price * quantity;
                if (SQLiteManager.getPlayerBalance(player.getUUID().toString()) < totalCost) {
                    player.displayClientMessage(Component.translatable("message.infinity_nexus_market.insufficient_balance"), false);
                    return;
                }

                // Atualiza saldo do jogador
                SQLiteManager.setPlayerBalance(
                        player.getUUID().toString(),
                        player.getName().getString(),
                        SQLiteManager.getPlayerBalance(player.getUUID().toString()) - totalCost
                );

                // Atualiza estatísticas
                SQLiteManager.incrementPlayerStats(
                        player.getUUID().toString(),
                        totalCost,  // total gasto
                        0.0,        // total ganho
                        0,          // total vendas
                        1           // total compras
                );

                // Atualiza ou remove a entrada do mercado
                if ("player".equals(entry.type)) {
                    if (quantity >= saleQuantity) {
                        SQLiteManager.removeMarketItem(entry.entryId);
                        SQLiteManager.incrementPlayerStats(
                                entry.sellerUUID,
                                0.0,        // total gasto
                                totalCost,  // total ganho
                                1,          // total vendas
                                0           // total compras
                        );
                        System.out.println("MarketBuyC2SPacket: Removendo item do mercado: " + entry.entryId);
                    } else {
                        // Atualiza a quantidade restante
                        ItemStack updatedItem = saleItem.copy();
                        updatedItem.setCount(saleQuantity - quantity);

                        SQLiteManager.addOrUpdateMarketItem(
                                entry.entryId,
                                "player",
                                entry.sellerUUID,
                                entry.sellerName,
                                updatedItem,
                                saleQuantity - quantity,
                                entry.basePrice,
                                entry.currentPrice,
                                (ServerLevel) level
                        );
                        System.out.println("MarketBuyC2SPacket: Atualizando item do mercado: " + entry.entryId);
                    }
                }

                // Entrega os itens
                ItemStack deliveredItem = saleItem.copy();
                deliveredItem.setCount(quantity);
                postItemInSlot(deliveredItem);

                // Notifica o jogador
                notifyOwnerIfNeeded((ServerLevel) level, saleItem, quantity, totalCost);
                return;
            }
        }
    }

    public void postItemInSlot(ItemStack stack) {
        ItemStackHandlerUtils.insertItem(AUTO_SLOT, stack, false, itemHandler);
    }

    private void notifyOwnerIfNeeded(ServerLevel serverLevel, ItemStack item, int quantity, double cost) {
        if (autoNotify) {
            Player ownerPlayer = serverLevel.getPlayerByUUID(UUID.fromString(owner));
            if (ownerPlayer != null) {
                Component itemComponentMsg = ComponentUtils.wrapInSquareBrackets(item.getHoverName())
                        .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                                        new HoverEvent.ItemStackInfo(item))));
                Component priceComponent = Component.translatable("gui.infinity_nexus_market.tooltip.price_value",
                        String.format("%.2f", cost)).withStyle(ChatFormatting.GOLD);
                Component msg = Component.translatable(
                        "message.infinity_nexus_market.buy_success_item",
                        ModConfigs.prefix,
                        quantity,
                        itemComponentMsg,
                        priceComponent
                );
                ownerPlayer.displayClientMessage(msg, false);
            }
        }
    }
}