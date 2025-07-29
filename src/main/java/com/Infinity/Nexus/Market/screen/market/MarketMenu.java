package com.Infinity.Nexus.Market.screen.market;

import com.Infinity.Nexus.Market.block.entity.MarketBlockEntity;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import com.Infinity.Nexus.Market.screen.BaseAbstractContainerMenu;
import com.Infinity.Nexus.Market.screen.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;

public class MarketMenu extends BaseAbstractContainerMenu {
    private final Level level;
    private final MarketBlockEntity blockEntity;
    private final ContainerData data;
    private final Player player;

    public MarketMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, (MarketBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(1));
    }

    public MarketMenu(int pContainerId, Inventory inv, MarketBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.MARKET_MENU.get(), pContainerId, 0);
        this.level = inv.player.level();
        this.blockEntity = entity;
        this.data = data;
        this.player = inv.player;
        addDataSlots(data);
    }

    /**
     * Obtém as vendas do mercado diretamente do SQLiteManager
     */
    public List<DatabaseManager.MarketItemEntry> getMarketSales() {
        if (level.isClientSide() || !(level instanceof ServerLevel)) {
            return Collections.emptyList();
        }

        return DatabaseManager.getAllMarketItems();
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
    }
}