package com.Infinity.Nexus.Market.block.entity;

import com.Infinity.Nexus.Market.block.custom.BaseMachineBlock;
import com.Infinity.Nexus.Market.component.MarketDataComponents;
import com.Infinity.Nexus.Market.component.TicketItemComponent;
import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.item.ModItemsMarket;
import com.Infinity.Nexus.Market.itemStackHandler.RestrictedItemStackHandler;
import com.Infinity.Nexus.Market.screen.buying.BuyingMenu;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import com.Infinity.Nexus.Market.utils.ItemStackHandlerUtils;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BuyingBlockEntity extends AbstractMarketBlockEntity{
    private static final int TICKET_SLOT = 0;
    private static final int AUTO_SLOT = 1;
    private DatabaseManager.MarketItemEntry lastFoundEntry = null;

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
        //System.out.println("tick:" + progress + "/" + maxProgress);

        if(progress < maxProgress) {
            progress++;
            if(progress == maxProgress-1) {
                level.setBlock(worldPosition, getBlockState().setValue(BaseMachineBlock.WORK, false).setValue(BaseMachineBlock.PLACED, false), 3);
            }
            return;
        }

        progress = 0;

        if (!autoEnabled) {
            return;
        }

        //if(getEnergyStored() < ModConfigs.buyingTicksPerOperation){
        //    return;
        //}

        if (owner == null || ownerName == null) {
            return;
        }

        if (itemHandler.getStackInSlot(TICKET_SLOT).isEmpty()) {
            return;
        }

        if (itemHandler.getStackInSlot(AUTO_SLOT).getCount() >= 64) {
            return;
        }

        TicketItemComponent itemComponent = isValidTicket();

        if (itemComponent == null) {
            return;
        }

        if (!slotAcceptItem(itemComponent.toItemStack())) {
            return;
        }


        if (!ownerHasBalance(itemComponent.price())) {
            return;
        }

        processAutoBuy(itemComponent);
        progress = 0;
        level.setBlock(pPos, pState.setValue(BaseMachineBlock.WORK, true), 3);
    }

    private boolean slotAcceptItem(@Nullable ItemStack itemStack) {
        return itemStack != null && !itemStack.isEmpty() &&
                ItemStackHandlerUtils.canInsertItemAndAmountIntoOutputSlot(
                        itemStack.getItem(),
                        itemStack.getCount(),
                        AUTO_SLOT,
                        itemHandler
                );
    }

    private boolean ownerHasBalance(double price) {
        return owner != null && DatabaseManager.getPlayerBalance(owner) >= price;
    }

    private TicketItemComponent isValidTicket() {
        ItemStack ticket = itemHandler.getStackInSlot(TICKET_SLOT);
        if (ticket.isEmpty() || !ticket.has(MarketDataComponents.TICKET_ITEM.get())) {
            return null;
        }
        TicketItemComponent itemComponent = ticket.get(MarketDataComponents.TICKET_ITEM.get());
        return (itemComponent != null && itemComponent.price() > 0 && !itemComponent.toItemStack().isEmpty())
                ? itemComponent
                : null;
    }

    public void selfBuyInMarket(ServerPlayer player) {
        if (player == null) return;
        executeBuyTransaction(player, isValidTicket(), false);

    }

    public void processAutoBuy(TicketItemComponent itemComponent) {
        if (owner == null) return;

        //extractEnergy(ModConfigs.buyingEnergyPerOperation, false);
        executeBuyTransaction(null, itemComponent, true);
    }

    /**
     * Executes a buy transaction for the specified player.
     * @param player The player executing the transaction
     * @param itemComponent The ticket item component with purchase details
     * @param auto Whether this is an automatic transaction initiated by the machine
     */
    private void executeBuyTransaction(ServerPlayer player, TicketItemComponent itemComponent, boolean auto) {
        if (itemComponent == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // For auto transactions, use owner UUID instead of player
        String transactionPlayerUUID = auto ? owner : (player != null ? player.getUUID().toString() : null);
        if (transactionPlayerUUID == null) return;

        if (!validateTransaction(player, itemComponent, auto)) {
            return;
        }

        double price = autoPrice != 0 ? autoPrice : itemComponent.price();
        int quantity = autoMinAmount != 0 ? autoMinAmount : itemComponent.count();
        quantity = calculateAvailableQuantity(quantity);

        DatabaseManager.MarketItemEntry entry = findMarketEntry(itemComponent, price);

        if (entry == null) {
            if (autoNotify && player != null) {
                player.displayClientMessage(Component.translatable("message.infinity_nexus_market.item_not_found", ModConfigs.prefix), false);
            }
            return;
        }


        if (entry.sellerUUID.equals(transactionPlayerUUID)) {
            if (autoNotify && player != null) {
                player.displayClientMessage(Component.translatable("message.infinity_nexus_market.cant_buy_own_item", ModConfigs.prefix), false);
            }
            return;
        }

        ItemStack saleItem = DatabaseManager.deserializeItemStack(entry.itemNbt);
        int saleQuantity = entry.quantity;
        quantity = Math.min(quantity, saleQuantity);

        double totalCost = price * quantity;
        if (!processPayment(transactionPlayerUUID, player, entry.sellerUUID, totalCost, quantity, saleQuantity, auto)) {
            return;
        }

        updateMarketEntry(entry, quantity, saleItem, saleQuantity);
        deliverItems(saleItem, quantity);
        notifyOwnerIfNeeded(serverLevel, saleItem, quantity, totalCost);
    }

    private boolean validateTransaction(ServerPlayer player, TicketItemComponent itemComponent, boolean auto) {
        if (itemComponent.price() <= 0 || itemComponent.toItemStack().isEmpty()) {
            if (!auto && autoNotify) {
                player.displayClientMessage(
                        Component.translatable("message.infinity_nexus_market.invalid_ticket", ModConfigs.prefix), false);
            }
            return false;
        }
        return true;
    }

    private int calculateAvailableQuantity(int desiredQuantity) {
        int spaceInSlot = itemHandler.getStackInSlot(AUTO_SLOT).isEmpty()
                ? 64
                : 64 - itemHandler.getStackInSlot(AUTO_SLOT).getCount();
        return Math.min(desiredQuantity, spaceInSlot);
    }

    private DatabaseManager.MarketItemEntry findMarketEntry(TicketItemComponent itemComponent, double price) {
        String serializedItem = DatabaseManager.serializeItemStack(itemComponent.toItemStack());

        if (lastFoundEntry != null && lastFoundEntry.itemNbt.equals(serializedItem)) {
            DatabaseManager.MarketItemEntry refreshedEntry = DatabaseManager.getMarketItemByEntryId(lastFoundEntry.entryId);
            if (refreshedEntry != null && refreshedEntry.isActive && refreshedEntry.currentPrice <= price) {
                return refreshedEntry;
            }
            lastFoundEntry = null;
        }

        DatabaseManager.MarketItemEntry entry = DatabaseManager.getMarketItemByStackAndPrice(
                itemComponent.toItemStack(),
                price,
                itemComponent.sellerName(),
                itemComponent.randomSeller(),
                ownerName
        );

        if (entry != null) {
            lastFoundEntry = entry;
        }

        return entry;
    }

    private boolean processPayment(String buyerUUID, ServerPlayer player, String sellerUUID, double totalCost, int quantity, int saleQuantity, boolean auto) {
        double buyerBalance = DatabaseManager.getPlayerBalance(buyerUUID);

        if (buyerBalance < totalCost) {
            if (!auto && player != null && autoNotify) {
                player.displayClientMessage(Component.translatable("message.infinity_nexus_market.insufficient_balance", ModConfigs.prefix), false);
            }
            return false;
        }

        // Update buyer balance
        String buyerName = player != null ? player.getName().getString() : DatabaseManager.getSellerNameByEntryUUID(buyerUUID);
        DatabaseManager.setPlayerBalance(buyerUUID, buyerName, buyerBalance - totalCost);
        DatabaseManager.incrementPlayerStats(buyerUUID, totalCost, 0.0, 0, 1);

        return true;
    }

    private void updateMarketEntry(DatabaseManager.MarketItemEntry entry, int quantity,
                                   ItemStack saleItem, int saleQuantity) {
        if ("player".equals(entry.type)) {
            if (quantity >= saleQuantity) {
                DatabaseManager.removeMarketItem(entry.entryId);
                DatabaseManager.incrementPlayerStats(
                        entry.sellerUUID,
                        0.0, entry.currentPrice * quantity, 1, 0
                );
            } else {
                ItemStack updatedItem = saleItem.copy();
                updatedItem.setCount(saleQuantity - quantity);

                DatabaseManager.addOrUpdateMarketItem(
                        entry.entryId,
                        "player",
                        entry.sellerUUID,
                        entry.sellerName,
                        updatedItem,
                        saleQuantity - quantity,
                        entry.basePrice,
                        entry.currentPrice
                );
            }
        }else{
            DatabaseManager.addPlayerBalance(SERVER_UUID.toString(), "Server", entry.currentPrice * quantity);
        }

        DatabaseManager.addSalesHistory(
                entry.itemNbt,
                quantity,
                entry.currentPrice,
                entry.sellerUUID,
                entry.sellerName
        );
    }

    private void deliverItems(ItemStack saleItem, int quantity) {
        synchronized (this) {
            ItemStack deliveredItem = saleItem.copy();
            deliveredItem.setCount(quantity);
            postItemInSlot(deliveredItem);
        }
    }

    public void postItemInSlot(ItemStack stack) {
        ItemStackHandlerUtils.insertItem(AUTO_SLOT, stack, false, itemHandler);
    }

    private void notifyOwnerIfNeeded(ServerLevel serverLevel, ItemStack item, int quantity, double cost) {
        if (!autoNotify || owner == null) return;

        Player ownerPlayer = serverLevel.getPlayerByUUID(UUID.fromString(owner));
        if (ownerPlayer == null) {
            return;
        }

        ownerPlayer.displayClientMessage(createTransactionMessage(item, quantity, cost), false);
    }

    private Component createTransactionMessage(ItemStack item, int quantity, double cost) {
        Component itemComponentMsg = ComponentUtils.wrapInSquareBrackets(item.getHoverName())
                .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                                new HoverEvent.ItemStackInfo(item))));
        Component priceComponent = Component.translatable("gui.infinity_nexus_market.tooltip.price_value",
                String.format("%.2f", cost)).withStyle(ChatFormatting.GOLD);

        return Component.translatable(
                "message.infinity_nexus_market.buy_success_item",
                ModConfigs.prefix,
                quantity,
                itemComponentMsg,
                priceComponent
        );
    }

    public ItemStack getRenderStack() {
        if (itemHandler.getStackInSlot(AUTO_SLOT).isEmpty()) return ItemStack.EMPTY;
        return itemHandler.getStackInSlot(AUTO_SLOT);
    }
}