package com.Infinity.Nexus.Market.events;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.command.MarketCommands;
import com.Infinity.Nexus.Market.command.SQLiteCommands;
import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.utils.BackupManager;
import com.Infinity.Nexus.Market.market.SQLiteManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.command.ConfigCommand;

import java.nio.file.Paths;
import java.util.List;

@EventBusSubscriber(modid = InfinityNexusMarket.MOD_ID)
public class ModEvents {
    private static long lastInflationTick = 0;
    private static long lastBackupTick = 0;
    private static long lastCleanupTick = 0;
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        MarketCommands.register(event.getDispatcher());
        SQLiteCommands.register(event.getDispatcher());
        ConfigCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;
        long gameTime = server.getTickCount();
        // Inflação dinâmica
        if (ModConfigs.inflationEnabled && gameTime - lastInflationTick >= ModConfigs.inflationIntervalMin * 20L * 60L) {
            lastInflationTick = gameTime;
            processarInflacaoDinamica(server);
        }

        // Backup automático
        if (ModConfigs.backupEnabled && gameTime - lastBackupTick >= ModConfigs.backupIntervalMin * 20L * 60L) {
            lastBackupTick = gameTime;
            ServerLevel overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (overworld != null) {
                try {
                    BackupManager.backupAll(overworld, Paths.get("backups"));
                } catch (Exception e) {
                    System.err.println("[Market] Falha ao criar backup: " + e.getMessage());
                }
            }
        }

        // Limpeza automática
        if (ModConfigs.cleanupEnabled && gameTime - lastCleanupTick >= ModConfigs.cleanupIntervalHours * 20L * 60L * 60L) {
            lastCleanupTick = gameTime;
            try {
                int removedSales = SQLiteManager.cleanExpiredSales(ModConfigs.cleanupExpireDays);
                if (removedSales > 0) {
                    InfinityNexusMarket.LOGGER.info("Limpeza automática: " + removedSales + " vendas expiradas removidas");
                }
                
                // Compacta a database periodicamente
                SQLiteManager.compactDatabase();
            } catch (Exception e) {
                InfinityNexusMarket.LOGGER.error("Falha na limpeza automática: " + e.getMessage());
            }
        }
    }

    private static void processarInflacaoDinamica(MinecraftServer server) {
        // Obter todos os itens do servidor
        List<SQLiteManager.MarketItemEntry> serverItems = SQLiteManager.getAllServerItems();

        if (serverItems.isEmpty()) return;

        // Calcular novo preço com inflação
        double inflationRate = ModConfigs.inflationMinMult / 100.0;
        int updatedItems = 0;

        for (SQLiteManager.MarketItemEntry item : serverItems) {
            double newPrice = item.currentPrice * (1 + inflationRate);

            // Atualizar no banco de dados
            ServerLevel level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (level != null) {
                ItemStack itemStack = SQLiteManager.deserializeItemStack(item.itemNbt, level);
                if (!itemStack.isEmpty()) {
                    boolean success = SQLiteManager.addOrUpdateServerItem(
                            item.entryId,
                            itemStack,
                            item.basePrice,
                            newPrice,
                            level
                    );

                    if (success) {
                        updatedItems++;
                    }
                }
            }
        }

        // Notificar jogadores
        if (updatedItems > 0) {
            Component message = Component.literal(
                    String.format("§6[Inflação] §fPreços ajustados em §a%.1f%%§f! (%d itens afetados)",
                            ModConfigs.inflationMinMult, updatedItems)
            );

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.displayClientMessage(message, false);
            }

            InfinityNexusMarket.LOGGER.info("Inflação aplicada: {} itens atualizados", updatedItems);
        }
    }
}
