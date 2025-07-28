package com.Infinity.Nexus.Market.command;

import com.Infinity.Nexus.Market.market.SQLiteManager;
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
            List<SQLiteManager.MarketItemEntry> playerSales = SQLiteManager.getAllPlayerSales();
            List<SQLiteManager.MarketItemEntry> serverItems = SQLiteManager.getAllServerItems();
            
            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.stats.title"), false);
            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.stats.player_sales", playerSales.size()), false);
            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.stats.server_items", serverItems.size()), false);
            
            // Total de vendas ativas
            long activeSales = playerSales.stream().count();
            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.stats.active_sales", activeSales), false);
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.stats.error", e.getMessage()));
            return 0;
        }
    }
    
    private static int cleanCorruptedData(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            SQLiteManager.cleanCorruptedData();
            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.clean.success"), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.clean.error", e.getMessage()));
            return 0;
        }
    }
} 