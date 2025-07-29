package com.Infinity.Nexus.Market.command;

import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SQLiteCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("sqlite")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("stats")
                    .executes(SQLiteCommands::showStats))
                .then(Commands.literal("clean")
                    .executes(SQLiteCommands::cleanCorruptedData))
        );
    }
    
    private static int showStats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            // Estatísticas básicas da database
            List<DatabaseManager.MarketItemEntry> playerSales = DatabaseManager.getAllPlayerSales();
            List<DatabaseManager.MarketItemEntry> serverItems = DatabaseManager.getAllServerItems();
            
            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.stats.title"), false);
            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.stats.player_sales", ModConfigs.prefix, playerSales.size()), false);
            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.stats.server_items", ModConfigs.prefix, serverItems.size()), false);
            
            // Total de vendas ativas
            long activeSales = playerSales.stream().count();
            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.stats.active_sales", ModConfigs.prefix, activeSales), false);
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.stats.error", ModConfigs.prefix, e.getMessage()));
            return 0;
        }
    }
    
    private static int cleanCorruptedData(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            DatabaseManager.cleanCorruptedData();
            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.clean.success", ModConfigs.prefix), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.clean.error", ModConfigs.prefix, e.getMessage()));
            return 0;
        }
    }
} 