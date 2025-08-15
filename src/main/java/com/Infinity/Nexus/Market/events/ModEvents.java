package com.Infinity.Nexus.Market.events;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.command.MarketCommands;
import com.Infinity.Nexus.Market.command.SQLiteCommands;
import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import com.Infinity.Nexus.Market.utils.LotteryManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.command.ConfigCommand;

import java.util.*;
import java.util.stream.Collectors;

import static com.Infinity.Nexus.Market.command.response.Balance.formatBalance;

@EventBusSubscriber(modid = InfinityNexusMarket.MOD_ID)
public class ModEvents {

    private static long lastInflationTick = 0;
    private static long lastCleanupTick = 0;
    private static long lastTopBalanceTick = 0;
    public static Map<String, Double> previousTopPositions = new HashMap<>();
    public static Map<String, Double> previousTopBalances = new HashMap<>();
    private static long lastLotteryCheck = 0;
    private static final long LOTTERY_CHECK_INTERVAL = 15 * 60 * 20;
    private static boolean lotteryFired = false;
    public static final boolean isDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
    private static final UUID SERVER_UUID = UUID.fromString("00000000-0000-0000-0000-00000000c0de");

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
            processDynamicInflation(server);
        }

        // Limpeza automática
        if (ModConfigs.cleanupEnabled && gameTime - lastCleanupTick >= ModConfigs.cleanupIntervalHours * 20L * 60L * 60L) {
            lastCleanupTick = gameTime;
            try {
                int removedSales = DatabaseManager.cleanExpiredSales(ModConfigs.cleanupExpireDays);
                if (removedSales > 0) {
                    InfinityNexusMarket.LOGGER.info(Component.translatable("command.infinity_nexus_market.events.cleanup.removed", removedSales).getString());
                }

                DatabaseManager.compactDatabase();
            } catch (Exception e) {
                InfinityNexusMarket.LOGGER.error(Component.translatable("command.infinity_nexus_market.events.cleanup.failed", e.getMessage()).getString());
            }
        }

        // Verifica se é hora do sorteio (Sábado 17:50)
        if (isDay && !lotteryFired && ModConfigs.lotteryEnabled) {
            lastLotteryCheck = gameTime;
            Calendar cal = Calendar.getInstance();

            if (cal.get(Calendar.HOUR_OF_DAY) == 17 && cal.get(Calendar.MINUTE) > 50 && cal.get(Calendar.MINUTE) < 59) {
                distributeLottery(server);
                return;
            }
            if (cal.get(Calendar.HOUR_OF_DAY) > 8 && cal.get(Calendar.HOUR_OF_DAY) < 17 && gameTime - lastLotteryCheck >= LOTTERY_CHECK_INTERVAL) {
                loteryAnnouncement(server);
                return;
            }
        }

        // Anúncio do top 10 de saldos a cada 5 minutos
        if(ModConfigs.topBalanceInterval > 0) {
            if (gameTime - lastTopBalanceTick >= ModConfigs.topBalanceInterval * 20L * 60L) {
                lastTopBalanceTick = gameTime;
                anunciarTopBalances(server);
            }
        }
    }



    private static void processDynamicInflation(MinecraftServer server) {
        DatabaseManager.updateInflationPrices();
        int updatedItems = DatabaseManager.getUpdatedItemsCount();
        if (updatedItems > 0) {
            Component message = Component.translatable(
                    "command.infinity_nexus_market.events.inflation.adjusted",
                    updatedItems
            ).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.displayClientMessage(message, false);
            }

            InfinityNexusMarket.LOGGER.info(Component.translatable("command.infinity_nexus_market.events.inflation.applied", updatedItems).getString());
        }
    }

    public static void anunciarTopBalances(MinecraftServer server) {
        try {
            Map<String, Double> allBalances = DatabaseManager.getAllPlayerBalances();

            // Ordena do maior para o menor e pega os top 10
            List<Map.Entry<String, Double>> top10 = allBalances.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(10)
                    .toList();

            if (top10.isEmpty()) {
                return;
            }

            // Cria mapa das novas posições
            Map<String, Double> currentTopPositions = new HashMap<>();
            Map<String, Double> currentTopBalances = new HashMap<>();
            for (int i = 0; i < top10.size(); i++) {
                currentTopBalances.put(top10.get(i).getKey(), top10.get(i).getValue());
                currentTopPositions.put(top10.get(i).getKey(), (double) (i + 1));
            }

            // Verifica se houve mudanças
            boolean hasChanges = !currentTopPositions.equals(previousTopPositions);

            // Se não houve mudanças e já temos um ranking anterior, não faz nada
            if (!hasChanges && !previousTopPositions.isEmpty()) {
                return;
            }

            // Cria a mensagem do top 10
            Component header = Component.translatable("command.infinity_nexus_market.events.top_balances.header")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.displayClientMessage(header, false);

                for (int i = 0; i < top10.size(); i++) {
                    Map.Entry<String, Double> entry = top10.get(i);
                    String playerId = entry.getKey();

                    double balance = entry.getValue();
                    String formattedBalance = formatBalance(balance);

                    String rankColor = switch (i) {
                        case 0 -> "§6§l";
                        case 1 -> "§7§l";
                        case 2 -> "§c§l";
                        case 3 -> "§a§l";
                        case 4 -> "§b§l";
                        case 5 -> "§9§l";
                        case 6 -> "§5§l";
                        case 7 -> "§d§l";
                        case 8 -> "§e§l";
                        case 9 -> "§f§l";
                        default -> "§f";
                    };

                    // Verifica mudança de posição
                    String positionChange = "";
                    if (!previousTopPositions.isEmpty()) {
                        Double previousPosition = previousTopPositions.get(playerId);
                        if (previousPosition != null) {
                            int currentPosition = i + 1;
                            if (currentPosition < previousPosition) {
                                // Subiu no ranking
                                positionChange = " §a↑";
                            } else if (currentPosition > previousPosition) {
                                // Desceu no ranking
                                positionChange = " §c↓";
                            }
                            // Se igual, não mostra nada
                        } else {
                            // Novo no top 10
                            positionChange = " §a↑";
                        }
                    }

                    Component message = Component.literal(
                            String.format("%s%d. §b%s §f- %s%s",
                                    rankColor, i+1, playerId, formattedBalance, positionChange)
                    );
                    player.displayClientMessage(message, false);
                }
            }

            // Atualiza o ranking anterior para o próximo ciclo
            previousTopPositions = currentTopPositions;
            previousTopBalances = currentTopBalances;

            InfinityNexusMarket.LOGGER.info(Component.translatable("command.infinity_nexus_market.events.top_balances.announced").getString());
        } catch (Exception e) {
            InfinityNexusMarket.LOGGER.error(Component.translatable("command.infinity_nexus_market.events.top_balances.failed", e.getMessage()).getString());
        }
    }
    private static void loteryAnnouncement(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers(); // Obtenha a lista de jogadores online>
        for (ServerPlayer player : players) {
            player.displayClientMessage(Component.literal("=== LOTERIA ==="), false);
            player.displayClientMessage(Component.translatable("command.infinity_nexus_market.events.lottery.announcement"), false);
            player.displayClientMessage(Component.translatable("command.infinity_nexus_market.events.lottery.announcement_jackpot", formatBalance(DatabaseManager.getPlayerBalance(SERVER_UUID.toString()))), false);
            player.displayClientMessage(Component.literal("==============="), false);
        }
    }
    private static void distributeLottery(MinecraftServer server) {
        List<String> participants = LotteryManager.getParticipants();
        if (participants.isEmpty()) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("command.infinity_nexus_market.events.lottery.no_participants")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    false);
            return;
        }

        // Pega o saldo do servidor
        double serverBalance = DatabaseManager.getPlayerBalance(SERVER_UUID.toString());
        if (serverBalance <= 0) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("command.infinity_nexus_market.events.lottery.insufficient_funds")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    false);
            return;
        }

        // Sorteia 5 vencedores
        Collections.shuffle(participants);
        List<String> winners = participants.size() > 5 ?
                participants.subList(0, 5) :
                new ArrayList<>(participants);

        // Percentuais de distribuição
        double[] percentages = {0.50, 0.25, 0.15, 0.10, 0.05};

        // Distribui os prêmios
        for (int i = 0; i < winners.size(); i++) {
            String winnerId = winners.get(i);
            double amount = serverBalance * percentages[i];

            ServerPlayer winner = server.getPlayerList().getPlayerByName(winnerId);
            if (winner != null) {
                DatabaseManager.addPlayerBalance(winnerId.toString(), winner.getName().getString(), amount);
                winner.sendSystemMessage(Component.translatable(
                                "command.infinity_nexus_market.events.lottery.winner_message",
                                amount, i+1)
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }
        }

        // Remove o dinheiro do servidor
        DatabaseManager.setPlayerBalance(SERVER_UUID.toString(), "Server", 0);

        // Limpa os participantes
        LotteryManager.clearParticipants();

        // Anuncia os vencedores
        MutableComponent announcement = Component.translatable("command.infinity_nexus_market.events.lottery.results_header")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

        for (int i = 0; i < winners.size(); i++) {
            String winnerId = winners.get(i);
            if (winnerId != null) {
                announcement.append("\n").append(Component.literal(
                        String.format("§e%dº §b%s §f- §a%.2f§f",
                                i+1, winnerId, serverBalance * percentages[i])));
            }
        }

        server.getPlayerList().broadcastSystemMessage(announcement, false);
    }
}