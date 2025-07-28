package com.Infinity.Nexus.Market.command.response;

import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.market.SQLiteManager;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class Balance {
    public static int see(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");

            for (var profile : targets) {
                double currentBalance = SQLiteManager.getPlayerBalance(profile.getId().toString());

                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.balance",
                        ModConfigs.prefix,
                        profile.getName(),
                        currentBalance), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.balance.error", e.getMessage()));
            return 0;
        }
    }

    public static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            for (var profile : targets) {
                double currentBalance = SQLiteManager.getPlayerBalance(profile.getId().toString());
                SQLiteManager.setPlayerBalance(profile.getId().toString(), profile.getName(), currentBalance + amount);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.add.success",
                        ModConfigs.prefix,
                        amount,
                        profile.getName(),
                        (currentBalance + amount)), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.add.error", e.getMessage()));
            return 0;
        }
    }

    public static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            for (var profile : targets) {
                double currentBalance = SQLiteManager.getPlayerBalance(profile.getId().toString());
                if (currentBalance >= amount) {
                    SQLiteManager.setPlayerBalance(profile.getId().toString(), profile.getName(), currentBalance - amount);
                    source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.remove.success",
                            amount,
                            profile.getName(),
                            (currentBalance - amount)), false);
                } else {
                    source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.remove.insufficient",
                            profile.getName(),
                            currentBalance));
                }
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.remove.error", e.getMessage()));
            return 0;
        }
    }

    public static int set(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            for (var profile : targets) {
                SQLiteManager.setPlayerBalance(profile.getId().toString(), profile.getName(), amount);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.set.success",
                        profile.getName(),
                        amount), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.set.error", e.getMessage()));
            return 0;
        }
    }

    // Novos métodos para gerenciar o limite de vendas
    public static int setMaxSales(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");
            int maxSales = IntegerArgumentType.getInteger(context, "maxSales");

            for (var profile : targets) {
                SQLiteManager.setPlayerMaxSales(profile.getId().toString(), maxSales);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.set_max_sales.success",
                        profile.getName(),
                        maxSales), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.set_max_sales.error", e.getMessage()));
            return 0;
        }
    }

    public static int getMaxSales(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");

            for (var profile : targets) {
                int maxSales = SQLiteManager.getPlayerMaxSales(profile.getId().toString());
                int currentSales = SQLiteManager.getPlayerCurrentSalesCount(profile.getId().toString());
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.get_max_sales",
                        profile.getName(),
                        currentSales,
                        maxSales), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.get_max_sales.error", e.getMessage()));
            return 0;
        }
    }

    public static int removeMaxSales(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");
            int amountToRemove = IntegerArgumentType.getInteger(context, "amount");

            for (var profile : targets) {
                int currentMax = SQLiteManager.getPlayerMaxSales(profile.getId().toString());
                int newMax = Math.max(0, currentMax - amountToRemove); // Garante que não fique negativo

                SQLiteManager.setPlayerMaxSales(profile.getId().toString(), newMax);

                source.sendSuccess(() -> Component.translatable(
                        "command.infinity_nexus_market.sqlite.balance.remove_max_sales.success",
                        amountToRemove,
                        profile.getName(),
                        newMax
                ), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable(
                    "command.infinity_nexus_market.sqlite.balance.remove_max_sales.error",
                    e.getMessage()
            ));
            return 0;
        }
    }


    public static int addMaxSales(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");
            int amountToAdd = IntegerArgumentType.getInteger(context, "amount");

            for (var profile : targets) {
                int currentMax = SQLiteManager.getPlayerMaxSales(profile.getId().toString());
                int newMax = currentMax + amountToAdd;

                SQLiteManager.setPlayerMaxSales(profile.getId().toString(), newMax);

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
                    e.getMessage()
            ));
            return 0;
        }
    }
}