package com.Infinity.Nexus.Market.block.entity;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.block.custom.BaseMachineBlock;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VendingBlockEntity extends AbstractMarketBlockEntity {
    private static final Map<String, TagKey<Item>> TAG_CACHE = new HashMap<>();
    private static final Map<String, Item> ITEM_CACHE = new HashMap<>();
    private static final int MANUAL_SLOT = 0;
    private static final int AUTO_SLOT = 1;
    private static ItemStack RENDER_STACK = ItemStack.EMPTY;

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
            if(progress == maxProgress-1) {
                RENDER_STACK = itemHandler.getStackInSlot(AUTO_SLOT);
                level.setBlock(worldPosition, getBlockState().setValue(BaseMachineBlock.WORK, false).setValue(BaseMachineBlock.PLACED, false), 3);
            }
            return;
        }

        if(itemHandler.getStackInSlot(AUTO_SLOT).isEmpty()) {
            return;
        }

        progress = 0;

        if (pLevel.isClientSide) return;

        if (autoEnabled && autoMinAmount > 0 && autoPrice > 0) {
            progress = 0;
            autoPostSaleOnMarket();
            RENDER_STACK = ItemStack.EMPTY;
            level.setBlock(pPos, pState.setValue(BaseMachineBlock.WORK, true), 3);
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
        boolean isServerItem = owner.equals(InfinityNexusMarket.SERVER_UUID.toString());

        if (!validateSale(player, item, sellerUUID, isServerItem)) {
            return false;
        }

        ItemStack copy = item.copy();
        copy.setCount(1);
        String itemNbt = DatabaseManager.serializeItemStack(copy);

        if (!isServerItem && !validateMinimumPrice(itemNbt, preco)) {
            ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(sellerUUID);
            if (ownerPlayer != null && autoNotify) {
                double minPrice = DatabaseManager.getCurrentPriceForItem(itemNbt) * ModConfigs.minimumPricePercentage;
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
        if (owner.equals(InfinityNexusMarket.SERVER_UUID.toString())) {
            return true;
        }

        // Atualiza o cache se necessário
        DatabaseManager.updateServerItemPriceCache();

        // Verifica se o preço está no cache
        Double serverPrice = DatabaseManager.SERVER_ITEM_PRICE_CACHE.get(itemNbt);
        if (serverPrice == null) {
            // Se não estiver no cache, busca no banco de dados e atualiza o cache
            serverPrice = DatabaseManager.getCurrentPriceForItem(itemNbt);
            DatabaseManager.SERVER_ITEM_PRICE_CACHE.put(itemNbt, serverPrice);
        }

        // Permite apenas preços que sejam pelo menos 50% do preço do servidor
        return price >= (serverPrice * ModConfigs.minimumPricePercentage);
    }

    @Override
    public void setOwner(Player player) {
        if(player.isCreative()){
            owner = InfinityNexusMarket.SERVER_UUID.toString();
            ownerName = "Server";
            return;
        }
        super.setOwner(player);
    }

    public ItemStack getRenderStack() {
        System.out.println(RENDER_STACK);
        return RENDER_STACK;
    }
}