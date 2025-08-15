package com.Infinity.Nexus.Market.command;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SQLiteCommands {
    private static List<String> cachedEntryIds = new ArrayList<>();
    private static long lastCacheUpdate = 0;
    private static final long CACHE_DURATION_MS = 30000;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("market")
                .requires(source -> source.hasPermission(4))
                    .then(Commands.literal("stats")
                            .executes(SQLiteCommands::showStats))
                    .then(Commands.literal("reload")
                            .executes(SQLiteCommands::reloadDatabase))
                    .then(Commands.literal("clean")
                            .executes(SQLiteCommands::cleanCorruptedData))
                    .then(Commands.literal("itemlookup")
                            .then(Commands.argument("entry_id", StringArgumentType.word())
                                    .suggests(ENTRY_ID_SUGGESTIONS)
                                    .executes(SQLiteCommands::itemLookupByEntryId)))
                    .then(Commands.literal("getItemByID")
                            .then(Commands.argument("entry_id", StringArgumentType.word())
                                    .executes(SQLiteCommands::acquireItem)))
                    .then(Commands.literal("remove")
                            .then(Commands.argument("item_or_mod_id", StringArgumentType.string())
                                    .executes(SQLiteCommands::removeItemsByModOrId)))
                    .then(Commands.literal("clearBalances")
                            .executes(SQLiteCommands::clearAllBalances))
        );
    }

    private static int reloadDatabase(CommandContext<CommandSourceStack> commandSourceStackCommandContext) {
        CommandSourceStack source = commandSourceStackCommandContext.getSource();
        if(!source.isPlayer() || !source.getPlayer().isCreative()){
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.console_only", ModConfigs.prefix));
            return 0;
        }
        try {
            DatabaseManager.reload();
            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.reload.success", ModConfigs.prefix), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.reload.error", ModConfigs.prefix, e.getMessage()));
            return 0;
        }
    }

    private static final SuggestionProvider<CommandSourceStack> ENTRY_ID_SUGGESTIONS =
            (context, builder) -> {
                try {
                    updateEntryIdCache();
                    String currentInput = builder.getRemaining().toLowerCase();

                    for (String entryId : cachedEntryIds) {
                        if (entryId.toLowerCase().contains(currentInput)) {
                            DatabaseManager.MarketItemEntry item = DatabaseManager.getMarketItemByEntryId(entryId);
                            if (item != null) {
                                builder.suggest(entryId, Component.literal(item.quantity + "x ").append(item.sellerName).append(" - " + item.currentPrice + "$"));
                            } else {
                                builder.suggest(entryId);
                            }
                        }
                    }
                    return builder.buildFuture();
                } catch (Exception e) {
                    InfinityNexusMarket.LOGGER.error("Erro ao gerar sugestões de entry_id", e);
                    return builder.buildFuture();
                }
            };
    private static int showStats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if(!source.isPlayer() || !source.getPlayer().isCreative()){
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.console_only", ModConfigs.prefix));
            return 0;
        }
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
        if(!source.isPlayer() || !source.getPlayer().isCreative()){
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.console_only", ModConfigs.prefix));
            return 0;
        }
        try {
            DatabaseManager.cleanCorruptedData();
            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.clean.success", ModConfigs.prefix), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.clean.error", ModConfigs.prefix, e.getMessage()));
            return 0;
        }
    }

    private static int itemLookupByEntryId(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if(!source.isPlayer() || !source.getPlayer().isCreative()){
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.console_only", ModConfigs.prefix));
            return 0;
        }
        String entryId = StringArgumentType.getString(context, "entry_id");

        try {
            DatabaseManager.MarketItemEntry entry = DatabaseManager.getMarketItemByEntryId(entryId);

            if (entry != null) {
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.lookup.found.title", ModConfigs.prefix), false);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.lookup.found.entry_id", ModConfigs.prefix, entry.entryId), false);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.lookup.found.acquire")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/market getItemByID " + entry.entryId
                                ))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable("command.infinity_nexus_market.lookup.acquire.hover")
                                ))
                        ), false);

                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.lookup.found.created_at",
                        "§6"+entry.createdAt), false);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.lookup.found.last_updated",
                        "§e"+entry.lastUpdated), false);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.lookup.found.type",
                        "§b"+entry.type), false);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.lookup.found.seller",
                        "§a"+entry.sellerName), false);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.lookup.found.quantity",
                        "§6"+entry.quantity), false);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.lookup.found.price",
                        "§6"+entry.currentPrice), false);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.lookup.found.active",
                        entry.isActive ? "§bYes" : "§cNo"), false);
            } else {
                source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.lookup.not_found", ModConfigs.prefix, entryId));
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.lookup.error", ModConfigs.prefix, e.getMessage()));
            return 0;
        }
    }

    private static int acquireItem(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if(!source.isPlayer() && !source.getPlayer().isCreative()){
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.player_only", ModConfigs.prefix));
            return 0;
        }
        String entryId = StringArgumentType.getString(context, "entry_id");

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.player_only", ModConfigs.prefix));
            return 0;
        }

        try {
            DatabaseManager.MarketItemEntry entry = DatabaseManager.getMarketItemByEntryId(entryId);

            if (entry == null) {
                source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.lookup.not_found", ModConfigs.prefix, entryId));
                return 0;
            }

            if (!entry.isActive) {
                source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.acquire.inactive", ModConfigs.prefix));
                return 0;
            }

            ItemStack itemStack = DatabaseManager.deserializeItemStack(entry.itemNbt);
            if (itemStack.isEmpty()) {
                source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.acquire.invalid_item", ModConfigs.prefix));
                return 0;
            }

            //itemStack.setCount(entry.quantity);

            boolean added = player.getInventory().add(itemStack);
            if (!added) {
                player.drop(itemStack, false);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.acquire.inventory_full", ModConfigs.prefix), false);
            }

            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.acquire.success", ModConfigs.prefix, entry.quantity, itemStack.getDisplayName()), false);

            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.acquire.error", ModConfigs.prefix, e.getMessage()));
            return 0;
        }
    }
    private static void updateEntryIdCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheUpdate > CACHE_DURATION_MS) {
            try {
                List<DatabaseManager.MarketItemEntry> items = DatabaseManager.getAllMarketItems();
                cachedEntryIds = items.stream()
                        .filter(item -> item.isActive && item.entryId != null)
                        .map(item -> item.entryId)
                        .collect(Collectors.toList());
                lastCacheUpdate = currentTime;
                InfinityNexusMarket.LOGGER.debug("Cache de entry_ids atualizado. Total: {}", cachedEntryIds.size());
            } catch (Exception e) {
                InfinityNexusMarket.LOGGER.error("Erro ao atualizar cache de entry_ids", e);
            }
        }
    }
    private static int removeItemsByModOrId(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if(source.isPlayer()){
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.console_only", ModConfigs.prefix));
            return 0;
        }
        String itemId = StringArgumentType.getString(context, "item_or_mod_id");

        try {
            // Verifica se é um ID completo (ex: mekanism:solar_panel) ou apenas namespace (ex: mekanism:)
            boolean isFullId = itemId.contains(":") && itemId.split(":").length == 2;

            int removedCount;
            if (isFullId) {
                // Remove itens com ID exato
                removedCount = DatabaseManager.removeItemsByExactId(itemId);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.remove.success_exact",
                        ModConfigs.prefix, removedCount, itemId), false);
            } else {
                // Remove todos os itens do mod especificado
                String modNamespace = itemId.endsWith(":") ? itemId : itemId + ":";
                removedCount = DatabaseManager.removeItemsByModNamespace(modNamespace);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.remove.success_mod",
                        ModConfigs.prefix, removedCount, modNamespace), false);
            }

            if (removedCount == 0) {
                source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.remove.not_found",
                        ModConfigs.prefix, itemId));
            }

            return removedCount > 0 ? 1 : 0;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.remove.error",
                    ModConfigs.prefix, e.getMessage()));
            return 0;
        }
    }
    private static int clearAllBalances(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if(!source.isPlayer()){
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.console_only", ModConfigs.prefix));
            return 0;
        }
        try {
            int deletedCount = DatabaseManager.clearAllPlayerBalances();
            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.clear_balances.success",
                    ModConfigs.prefix, deletedCount), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.clear_balances.error",
                    ModConfigs.prefix, e.getMessage()));
            return 0;
        }
    }
}