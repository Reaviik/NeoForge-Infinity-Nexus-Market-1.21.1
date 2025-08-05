package com.Infinity.Nexus.Market.block.entity;

import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.itemStackHandler.RestrictedItemStackHandler;
import com.Infinity.Nexus.Market.screen.seller.VendingMenu;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import com.Infinity.Nexus.Market.utils.ItemStackHandlerUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VendingBlockEntity extends AbstractMarketBlockEntity {
    private static final UUID SERVER_UUID = UUID.fromString("00000000-0000-0000-0000-00000000c0de");
    private static final Map<String, TagKey<Item>> TAG_CACHE = new HashMap<>();
    private static final Map<String, Item> ITEM_CACHE = new HashMap<>();
    private static final int MANUAL_SLOT = 0;
    private static final int AUTO_SLOT = 1;
    private static final String LOG_PREFIX = "[VendingMachine] ";
    private static final boolean DEBUG_MODE = false;

    private int progress = 0;
    private int maxProgress = ModConfigs.vendingTicksPerOperation;

    public VendingBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.VENDING_MACHINE_BE.get(), pPos, pBlockState, 2);
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
                    case 0, 1 -> true;
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
        return ModConfigs.vendingEnergyCapacity;
    }

    @Override
    protected int getEnergyTransfer() {
        return ModConfigs.vendingEnergyTransfer;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infinity_nexus_market.vending_machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new VendingMenu(pContainerId, pPlayerInventory, this, this.data, this.itemHandler);
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {

        if(progress < maxProgress) {
            progress++;
            return;
        }
        if (pLevel.isClientSide) return;
        //if (getEnergyStored() < ModConfigs.vendingEnergyPerOperation) return;

        if (autoEnabled && autoMinAmount > 0 && autoPrice > 0) {
            //extractEnergy(ModConfigs.vendingEnergyPerOperation, false);
            progress = 0;
            autoPostSaleOnMarket();
        }
    }

    public void autoPostSaleOnMarket() {
        if (autoMinAmount > 0) {
            postSaleOnMarketBase(null, AUTO_SLOT, autoPrice, autoNotify);
        }
    }

    private boolean isBlacklisted(ItemStack stack) {
        for (String entry : ModConfigs.vendingItemBlacklist) {
            if (entry.startsWith("#")) {
                TagKey<Item> tagKey = TAG_CACHE.computeIfAbsent(entry,
                        k -> TagKey.create(Registries.ITEM, ResourceLocation.parse(entry.substring(1))));

                if (stack.is(tagKey)) {
                    return true;
                }
            } else {
                Item itemBL = ITEM_CACHE.computeIfAbsent(entry,
                        k -> BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(entry)));

                if (itemBL != null && stack.is(itemBL)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void postManualSaleToMarket(Player player, double price) {
        ItemStack item = itemHandler.getStackInSlot(MANUAL_SLOT).copy();
        boolean success = postSaleOnMarketBase(player, MANUAL_SLOT, price, false);

        if (success) {
            notifySaleSuccess(player, item, item.getCount(), price);
        } else if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("message.infinity_nexus_market.sell_fail", ModConfigs.prefix), false);
        }
    }

    private boolean postSaleOnMarketBase(@Nullable Player player, int slot, double preco, boolean notificar) {
        if (owner == null || !(level instanceof ServerLevel)) {
            return false;
        }

        ItemStack item = itemHandler.getStackInSlot(slot);
        UUID sellerUUID = UUID.fromString(owner);
        boolean isServerItem = owner.equals(SERVER_UUID.toString());

        if (!validateSale(player, item, sellerUUID, isServerItem)) {
            return false;
        }

        ItemStack copy = item.copy();
        copy.setCount(1);
        String itemNbt = DatabaseManager.serializeItemStack(copy);

        if (!isServerItem && !validateMinimumPrice(itemNbt, preco)) {
            ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(sellerUUID);
            if (ownerPlayer != null && autoNotify) {
                double minPrice = DatabaseManager.getCurrentPriceForItem(itemNbt) * 0.5;
                ownerPlayer.displayClientMessage(
                        Component.translatable("message.infinity_nexus_market.price_too_low",
                                        ModConfigs.prefix,
                                        minPrice,
                                        "§b" + item.getHoverName().getString() + "§r")
                                .withStyle(ChatFormatting.YELLOW),
                        false);
            }
            DatabaseManager.addPlayerBalance(sellerUUID.toString(), ownerName, preco);
            completeSaleTransaction(slot, item.getCount());
            return true;
        }

        String sellerName = resolveSellerName(sellerUUID, isServerItem);
        if (sellerName == null) return false;

        processSale(item, preco, sellerUUID, sellerName, isServerItem);

        if (notificar && !isServerItem) {
            notifyOwnerIfOnline(sellerUUID, item, item.getCount(), preco);
        }

        completeSaleTransaction(slot, item.getCount());
        return true;
    }

    private boolean validateSale(Player player, ItemStack item, UUID sellerUUID, boolean isServerItem) {
        if (item.isEmpty()) return false;
        if (isBlacklisted(item)) {
            notifyPlayer(player, "message.infinity_nexus_market.blacklist_item");
            return false;
        }
        if (!isServerItem && !DatabaseManager.canPlayerAddMoreSales(sellerUUID.toString())) {
            notifySaleLimitReached(player, sellerUUID.toString());
            return false;
        }
        return true;
    }

    private String resolveSellerName(UUID sellerUUID, boolean isServerItem) {
        if (ownerName != null && !ownerName.isEmpty()) {
            return ownerName;
        }
        return isServerItem ? "Server" : DatabaseManager.getSellerNameByEntryUUID(sellerUUID.toString());
    }

    private void processSale(ItemStack item, double preco, UUID sellerUUID, String sellerName,
                             boolean isServerItem) {
        if (isServerItem) {
            DatabaseManager.addOrUpdateServerItem(
                    UUID.randomUUID().toString(),
                    item.copy(),
                    preco,
                    preco
            );
        } else {
            DatabaseManager.addPlayerSale(
                    sellerUUID.toString(),
                    sellerName,
                    item.copy(),
                    item.getCount(),
                    preco
            );
        }
    }

    private void notifyOwnerIfOnline(UUID sellerUUID, ItemStack item, int quantidade, double preco) {
        if (level.getServer() == null) return;

        ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(sellerUUID);
        if (ownerPlayer != null) {
            notifySaleSuccess(ownerPlayer, item, quantidade, preco);
        }
    }

    private void completeSaleTransaction(int slot, int quantidade) {
        ItemStackHandlerUtils.extractItem(slot, quantidade, false, itemHandler);
        setChanged();

        if (!level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    private void notifySaleSuccess(Player player, ItemStack item, int quantidade, double preco) {
        if (!(player instanceof ServerPlayer sp)) return;

        Component itemComponent = ComponentUtils.wrapInSquareBrackets(item.getHoverName())
                .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                                new HoverEvent.ItemStackInfo(item))));
        Component priceComponent = Component.translatable("gui.infinity_nexus_market.tooltip.price_value",
                String.format("%.2f", preco)).withStyle(ChatFormatting.GOLD);
        Component msg = Component.translatable(
                "message.infinity_nexus_market.sell_success_item",
                ModConfigs.prefix, quantidade, itemComponent, priceComponent);
        sp.displayClientMessage(msg, false);
    }

    private void notifyPlayer(Player player, String messageKey) {
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable(messageKey, ModConfigs.prefix), true);
        }
    }

    private void notifySaleLimitReached(Player player, String sellerUUID) {
        if (player instanceof ServerPlayer sp) {
            int currentSales = DatabaseManager.getPlayerCurrentSalesCount(sellerUUID);
            int maxSales = DatabaseManager.getPlayerMaxSales(sellerUUID);
            Component msg = Component.translatable(
                    "message.infinity_nexus_market.sell_limit_reached",
                    ModConfigs.prefix, currentSales, maxSales
            ).withStyle(ChatFormatting.RED);
            sp.displayClientMessage(msg, false);
        }
    }
    private boolean validateMinimumPrice(String itemNbt, double price) {
        // Não aplicar para itens do servidor
        if (owner.equals(SERVER_UUID.toString())) {
            return true;
        }
        double serverPrice = DatabaseManager.getCurrentPriceForItem(itemNbt);

        // Permite apenas preços que sejam pelo menos 50% do preço do servidor
        return price >= (serverPrice * 0.5);
    }

    @Override
    public void setOwner(Player player) {
        if(player.isCreative()){
            owner = SERVER_UUID.toString();
            ownerName = "Server";
            return;
        }
        super.setOwner(player);
    }
}