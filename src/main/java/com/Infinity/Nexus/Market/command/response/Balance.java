package com.Infinity.Nexus.Market.command.response;

import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.events.ModEvents;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Balance {
    public static int see(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");

            // Obter o ranking global atualizado
            Map<String, Double> allBalances = DatabaseManager.getAllPlayerBalances();

            // Ordenar o ranking global
            List<Map.Entry<String, Double>> sortedGlobalBalances = allBalances.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                    .toList();

            for (var profile : targets) {
                String playerName = profile.getName();
                double currentBalance = DatabaseManager.getPlayerBalance(profile.getId().toString());

                // Buscar posição no ranking GLOBAL
                int rankPosition = IntStream.range(0, sortedGlobalBalances.size())
                        .filter(i -> sortedGlobalBalances.get(i).getKey().equalsIgnoreCase(playerName))
                        .findFirst()
                        .orElse(-1) + 1;

                source.sendSuccess(() ->
                        Component.translatable(
                                "command.infinity_nexus_market.sqlite.balance.balance",
                                ModConfigs.prefix,
                                "§b" + playerName,
                                formatBalance(currentBalance),
                                "§7" + rankPosition
                        ), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable(
                    "command.infinity_nexus_market.sqlite.balance.balance.error",
                    e.getMessage()
            ));
            return 0;
        }
    }



    public static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        if (source.isPlayer() && !source.getPlayer().hasPermissions(4)) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.no_permission", ModConfigs.prefix));
            return 0;
        }
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            for (var profile : targets) {
                String targetUUID = profile.getId().toString();
                String targetName = profile.getName();

                // Verifica se é o servidor (case-insensitive)
                if (targetName.equalsIgnoreCase("server")) {
                    targetUUID = DatabaseManager.SERVER_UUID.toString();
                    targetName = "Server"; // Padroniza o nome
                }

                double currentBalance = DatabaseManager.getPlayerBalance(targetUUID);
                DatabaseManager.addPlayerBalance(targetUUID, targetName, amount);

                String finalTargetName = targetName;
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.add.success",
                        ModConfigs.prefix,
                        amount,
                        finalTargetName,
                        formatBalance(currentBalance + amount)), false);

                // Notifica apenas jogadores reais (não aplicável ao servidor)
                if (!targetUUID.equals(DatabaseManager.SERVER_UUID.toString())) {
                    Player targetPlayer = source.getServer().getPlayerList().getPlayer(profile.getId());
                    if (targetPlayer != null) {
                        targetPlayer.sendSystemMessage(Component.translatable("command.infinity_nexus_market.sqlite.balance.add.success_receiver",
                                ModConfigs.prefix,
                                formatBalance(amount),
                                formatBalance(currentBalance + amount)));
                    }
                }
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.add.error", ModConfigs.prefix, e.getMessage()));
            return 0;
        }
    }

    public static int remove(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.isPlayer() && !source.getPlayer().hasPermissions(4)) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.no_permission", ModConfigs.prefix));
            return 0;
        }
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            for (var profile : targets) {
                String targetUUID = profile.getId().toString();
                String targetName = profile.getName();

                if (targetName.equalsIgnoreCase("server")) {
                    targetUUID = DatabaseManager.SERVER_UUID.toString();
                    targetName = "Server";
                }

                double currentBalance = DatabaseManager.getPlayerBalance(targetUUID);
                if (currentBalance >= amount) {
                    DatabaseManager.setPlayerBalance(targetUUID, targetName, currentBalance - amount);
                    String finalTargetName = targetName;
                    source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.remove.success",
                            ModConfigs.prefix,
                            amount,
                            finalTargetName,
                            formatBalance(currentBalance - amount)), false);

                    // Notifica apenas jogadores reais
                    if (!targetUUID.equals(DatabaseManager.SERVER_UUID.toString())) {
                        Player targetPlayer = source.getServer().getPlayerList().getPlayer(profile.getId());
                        if (targetPlayer != null) {
                            targetPlayer.sendSystemMessage(Component.translatable("command.infinity_nexus_market.sqlite.balance.remove.success_removed",
                                    ModConfigs.prefix,
                                    formatBalance(amount),
                                    targetName,
                                    formatBalance(currentBalance - amount)));
                        }
                    }
                } else {
                    source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.remove.insufficient",
                            ModConfigs.prefix,
                            targetName,
                            currentBalance));
                }
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.remove.error", ModConfigs.prefix, e.getMessage()));
            return 0;
        }
    }

    public static int set(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.isPlayer() && !source.getPlayer().hasPermissions(4)) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.no_permission", ModConfigs.prefix));
            return 0;
        }
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            for (var profile : targets) {
                String targetUUID = profile.getId().toString();
                String targetName = profile.getName();

                if (targetName.equalsIgnoreCase("server")) {
                    targetUUID = DatabaseManager.SERVER_UUID.toString();
                    targetName = "Server";
                }

                DatabaseManager.setPlayerBalance(targetUUID, targetName, amount);
                String finalTargetName = targetName;
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.set.success",
                        ModConfigs.prefix,
                        finalTargetName,
                        formatBalance(amount)), false);

                // Notifica apenas jogadores reais
                if (!targetUUID.equals(DatabaseManager.SERVER_UUID.toString())) {
                    Player targetPlayer = source.getServer().getPlayerList().getPlayer(profile.getId());
                    if (targetPlayer != null) {
                        targetPlayer.sendSystemMessage(Component.translatable("command.infinity_nexus_market.sqlite.balance.set.success_set",
                                ModConfigs.prefix,
                                amount,
                                targetName,
                                formatBalance(amount)));
                    }
                }
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.set.error", ModConfigs.prefix, e.getMessage()));
            return 0;
        }
    }
    // Comando para enviar dinheiro de um jogador para outro
    public static int send(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player senderPlayer = source.getPlayerOrException();
        var targets = GameProfileArgument.getGameProfiles(context, "target");
        double amount = DoubleArgumentType.getDouble(context, "amount");

        if (amount <= 0) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.send.invalid_amount", ModConfigs.prefix, amount));
            return 0;
        }

        for (var profile : targets) {
            if (profile.getId().equals(senderPlayer.getUUID())) {
                source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.send.cannot_send_to_self", ModConfigs.prefix));
                continue;
            }

            double senderBalance = DatabaseManager.getPlayerBalance(senderPlayer.getUUID().toString());
            if (senderBalance < amount) {
                source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.send.insufficient_funds",
                        ModConfigs.prefix,
                        amount,
                        senderPlayer.getName().getString()));
                continue;
            }

            double receiverBalance = DatabaseManager.getPlayerBalance(profile.getId().toString());

            // Deduzir do remetente
            DatabaseManager.setPlayerBalance(senderPlayer.getUUID().toString(), senderPlayer.getName().getString(), senderBalance - amount);
            // Adicionar ao destinatário
            DatabaseManager.setPlayerBalance(profile.getId().toString(), profile.getName(), receiverBalance + amount);

            // Notificar ambos os jogadores
            Player receiverPlayer = source.getServer().getPlayerList().getPlayer(profile.getId());
            if (receiverPlayer != null) {
                receiverPlayer.sendSystemMessage(Component.translatable("command.infinity_nexus_market.sqlite.balance.send.received",
                        ModConfigs.prefix,
                        amount,
                        senderPlayer.getName().getString()));
            }

            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.send.success",
                    ModConfigs.prefix,
                    amount,
                    profile.getName()), false);
        }
        return 1;
    }

    // Comando para solicitar pagamento de outro jogador
    public static int request(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player requesterPlayer = source.getPlayerOrException();
        var targets = GameProfileArgument.getGameProfiles(context, "target");
        double amount = DoubleArgumentType.getDouble(context, "amount");

        if (amount <= 0) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.request.invalid_amount", ModConfigs.prefix, amount));
            return 0;
        }

        for (var profile : targets) {
            if (profile.getId().equals(requesterPlayer.getUUID())) {
                source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.request.cannot_request_from_self"));
                continue;
            }

            Player targetPlayer = source.getServer().getPlayerList().getPlayer(profile.getId());
            if (targetPlayer != null) {
                // Criar os botões "Pagar" e "Recusar"
                Component payButton = Component.translatable("command.infinity_nexus_market.sqlite.balance.request.pay_button")
                        .withStyle(style -> style
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/market balance " + requesterPlayer.getName().getString() + " send " + amount))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("command.infinity_nexus_market.sqlite.balance.request.pay_button_hover")))
                        );

                // Enviar mensagem ao alvo com os botões
                targetPlayer.sendSystemMessage(
                        Component.translatable("command.infinity_nexus_market.sqlite.balance.request.received",
                                        ModConfigs.prefix,
                                        amount,
                                        requesterPlayer.getName().getString())
                                .append(Component.literal(" "))
                                .append(payButton)
                );
            }

            // Notificar o solicitante que a solicitação foi enviada
            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.request.sent",
                    ModConfigs.prefix,
                    amount,
                    profile.getName()), false);
        }
        return 1;
    }

    // Novos métodos para gerir o limite de vendas
    public static int setMaxSales(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");
            int maxSales = IntegerArgumentType.getInteger(context, "maxSales");

            for (var profile : targets) {
                DatabaseManager.setPlayerMaxSales(profile.getId().toString(), maxSales);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.set_max_sales.success",
                        ModConfigs.prefix,
                        profile.getName(),
                        maxSales), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.set_max_sales.error",
                    ModConfigs.prefix));
            return 0;
        }
    }

    public static int getMaxSales(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");

            for (var profile : targets) {
                int maxSales = DatabaseManager.getPlayerMaxSales(profile.getId().toString());
                int currentSales = DatabaseManager.getPlayerCurrentSalesCount(profile.getId().toString());
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.get_max_sales",
                        ModConfigs.prefix,
                        profile.getName(),
                        currentSales,
                        maxSales), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.get_max_sales.error",
                    ModConfigs.prefix));
            return 0;
        }
    }
    public static int getSelfMaxSales(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
                var profile = context.getSource().getPlayer();
                int maxSales = DatabaseManager.getPlayerMaxSales(profile.getStringUUID());
                int currentSales = DatabaseManager.getPlayerCurrentSalesCount(profile.getStringUUID());
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.get_max_sales",
                        ModConfigs.prefix,
                        profile.getName(),
                        currentSales,
                        maxSales), false);
                return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.get_max_sales.error",
                    ModConfigs.prefix));
            return 0;
        }
    }

    public static int removeMaxSales(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if(source.isPlayer() && !source.getPlayer().hasPermissions(4)){
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.no_permission", ModConfigs.prefix));
            return 0;
        }
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");
            int amountToRemove = IntegerArgumentType.getInteger(context, "amount");

            for (var profile : targets) {
                int currentMax = DatabaseManager.getPlayerMaxSales(profile.getId().toString());
                int newMax = Math.max(0, currentMax - amountToRemove); // Garante que não fique negativo

                DatabaseManager.setPlayerMaxSales(profile.getId().toString(), newMax);

                source.sendSuccess(() -> Component.translatable(
                        "command.infinity_nexus_market.sqlite.balance.remove_max_sales.success",
                        ModConfigs.prefix,
                        amountToRemove,
                        profile.getName(),
                        newMax
                ), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable(
                    "command.infinity_nexus_market.sqlite.balance.remove_max_sales.error",
                    ModConfigs.prefix,
                    e.getMessage()
            ));
            return 0;
        }
    }


    public static int addMaxSales(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if(source.isPlayer() && !source.getPlayer().hasPermissions(4)){
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.no_permission", ModConfigs.prefix));
            return 0;
        }
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");
            int amountToAdd = IntegerArgumentType.getInteger(context, "amount");

            for (var profile : targets) {
                int currentMax = DatabaseManager.getPlayerMaxSales(profile.getId().toString());
                int newMax = currentMax + amountToAdd;

                DatabaseManager.setPlayerMaxSales(profile.getId().toString(), newMax);

                source.sendSuccess(() -> Component.translatable(
                        "command.infinity_nexus_market.sqlite.balance.add_max_sales.success",
                        amountToAdd,
                        profile.getName(),
                        newMax
                ), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable(
                    "command.infinity_nexus_market.sqlite.balance.add_max_sales.error",
                    ModConfigs.prefix,
                    e.getMessage()
            ));
            return 0;
        }
    }

    public static int top(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            // Verifica se temos dados salvos
            if (ModEvents.previousTopBalances.isEmpty()) {
                source.sendFailure(Component.translatable("command.infinity_nexus_market.no_ranking", ModConfigs.prefix));
                return 0;
            }

            // Obtém os saldos dos jogadores no top
            Map<String, Double> balances = ModEvents.previousTopBalances;

            // Ordena pelo saldo (decrescente)
            List<Map.Entry<String, Double>> sortedBalances = balances.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(10)
                    .collect(Collectors.toList());

            source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.events.top_balances.header"), false);

            String[] rankColors = {
                    "§6§l", // 1º - Ouro
                    "§7§l", // 2º - Prata
                    "§c§l", // 3º - Bronze
                    "§a§l",   // 4º - Verde
                    "§b§l",   // 5º - Ciano
                    "§9§l",   // 6º - Azul
                    "§5§l",   // 7º - Roxo
                    "§d§l",   // 8º - Rosa
                    "§e§l",   // 9º - Amarelo
                    "§f§l"    // 10º - Branco
            };


            for (int i = 0; i < sortedBalances.size(); i++) {
                Map.Entry<String, Double> entry = sortedBalances.get(i);
                String playerId = entry.getKey();

                double balance = entry.getValue();
                String formattedBalance = formatBalance(balance);

                String rankColor = rankColors[i];
                Double previousPosition = ModEvents.previousTopPositions.get(playerId);

                // Determina a seta de mudança
                String arrow = "";
                if (previousPosition != null) {
                    int currentPosition = i + 1;
                    if (currentPosition < previousPosition) {
                        arrow = " §a↑"; // Subiu
                    } else if (currentPosition > previousPosition) {
                        arrow = " §c↓"; // Desceu
                    }
                } else {
                    arrow = " §a↑"; // Novo no top
                }

                int finalI = i;
                String finalArrow = arrow;
                source.sendSuccess(() -> Component.literal(
                        String.format("%s%d. §b%s §f- %s%s",
                                rankColor, finalI +1, playerId, formattedBalance, finalArrow)
                ), false);
            }

            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cErro ao exibir o ranking: " + e.getMessage()));
            return 0;
        }
    }

    public static String formatBalance(double balance) {
        if (balance < 1000) {
            // Corta na segunda casa decimal sem arredondar
            double truncated = Math.floor(balance * 100) / 100;
            // Se for número inteiro, mostra sem decimais, senão mostra 2 casas
            return truncated == (long)truncated ?
                    String.format("§6%d", (long)truncated) :
                    String.format("§6%.2f", truncated);
        } else if (balance < 1_000_000) {
            return String.format("§6%.1fk", Math.floor(balance / 1000 * 10) / 10);
        } else if (balance < 1_000_000_000) {
            return String.format("§6%.1fm", Math.floor(balance / 1_000_000 * 10) / 10);
        } else if (balance < 1_000_000_000_000L) {
            return String.format("§6%.1fml", Math.floor(balance / 1_000_000_000 * 10) / 10);
        } else if (balance < 1_000_000_000_000_000L) {
            return String.format("§6%.1fb", Math.floor(balance / 1_000_000_000_000L * 10) / 10);
        } else {
            return String.format("§6%.1ft", Math.floor(balance / 1_000_000_000_000_000L * 10) / 10);
        }
    }
}