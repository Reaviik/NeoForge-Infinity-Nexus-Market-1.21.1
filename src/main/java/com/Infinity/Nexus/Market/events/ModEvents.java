package com.Infinity.Nexus.Market.events;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.command.MarketCommands;
import com.Infinity.Nexus.Market.command.SQLiteCommands;
import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import com.Infinity.Nexus.Market.utils.BackupManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.command.ConfigCommand;

import java.nio.file.Paths;

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

        // Inflação dinâmica baseada em compras
        if (ModConfigs.inflationEnabled && gameTime - lastInflationTick >= ModConfigs.inflationIntervalMin * 20L * 60L) {
            lastInflationTick = gameTime;
            processarInflacaoDinamica(server);
        }

        // Backup automático
        if (ModConfigs.backupEnabled && gameTime - lastBackupTick >= ModConfigs.backupIntervalMin * 20L * 60L) {
            lastBackupTick = gameTime;
            if (InfinityNexusMarket.serverLevel != null) {
                try {
                    BackupManager.backupAll(Paths.get("backups"));
                } catch (Exception e) {
                    InfinityNexusMarket.LOGGER.error("Falha ao criar backup: " + e.getMessage());
                }
            }
        }

        // Limpeza automática
        if (ModConfigs.cleanupEnabled && gameTime - lastCleanupTick >= ModConfigs.cleanupIntervalHours * 20L * 60L * 60L) {
            lastCleanupTick = gameTime;
            try {
                int removedSales = DatabaseManager.cleanExpiredSales(ModConfigs.cleanupExpireDays);
                if (removedSales > 0) {
                    InfinityNexusMarket.LOGGER.info("Limpeza automática: " + removedSales + " vendas expiradas removidas");
                }

                DatabaseManager.compactDatabase();
            } catch (Exception e) {
                InfinityNexusMarket.LOGGER.error("Falha na limpeza automática: " + e.getMessage());
            }
        }
    }

    private static void processarInflacaoDinamica(MinecraftServer server) {
        // 1. Atualiza os preços baseado nas compras registradas
        DatabaseManager.updateInflationPrices();

        // 2. Notifica os jogadores
        int updatedItems = DatabaseManager.getUpdatedItemsCount();
        if (updatedItems > 0) {
            Component message = Component.literal(
                    String.format("§6[Inflação] §fPreços ajustados! (%d itens afetados)", updatedItems)
            );

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.displayClientMessage(message, false);
            }

            InfinityNexusMarket.LOGGER.info("Inflação aplicada: {} itens atualizados", updatedItems);
        }
    }
}