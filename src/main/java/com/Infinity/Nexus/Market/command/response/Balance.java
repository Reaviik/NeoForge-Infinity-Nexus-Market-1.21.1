package com.Infinity.Nexus.Market.command.response;

import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.sqlite.DatabaseManager;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class Balance {
    public static int see(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");

            for (var profile : targets) {
                double currentBalance = DatabaseManager.getPlayerBalance(profile.getId().toString());

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
                double currentBalance = DatabaseManager.getPlayerBalance(profile.getId().toString());
                DatabaseManager.setPlayerBalance(profile.getId().toString(), profile.getName(), currentBalance + amount);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.add.success",
                        ModConfigs.prefix,
                        amount,
                        profile.getName(),
                        (currentBalance + amount)), false);
                Player target = source.getServer().getPlayerList().getPlayer(profile.getId());
                if (target != null) {
                    target.sendSystemMessage(Component.translatable("command.infinity_nexus_market.sqlite.balance.add.success_receiver",
                            ModConfigs.prefix,
                            amount,
                            (currentBalance + amount)));
                }
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.add.error", ModConfigs.prefix, e.getMessage()));
            return 0;
        }
    }

    public static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            for (var profile : targets) {
                double currentBalance = DatabaseManager.getPlayerBalance(profile.getId().toString());
                if (currentBalance >= amount) {
                    DatabaseManager.setPlayerBalance(profile.getId().toString(), profile.getName(), currentBalance - amount);
                    source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.remove.success",
                            ModConfigs.prefix,
                            amount,
                            profile.getName(),
                            (currentBalance - amount)), false);
                    Player target = source.getServer().getPlayerList().getPlayer(profile.getId());
                    if (target != null) {
                        target.sendSystemMessage(Component.translatable("command.infinity_nexus_market.sqlite.balance.remove.success_removed",
                                ModConfigs.prefix,
                                amount,
                                profile.getName(),
                                (currentBalance - amount)));
                    }
                } else {
                    source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.remove.insufficient",
                            ModConfigs.prefix,
                            profile.getName(),
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
        try {
            var targets = GameProfileArgument.getGameProfiles(context, "target");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            for (var profile : targets) {
                DatabaseManager.setPlayerBalance(profile.getId().toString(), profile.getName(), amount);
                source.sendSuccess(() -> Component.translatable("command.infinity_nexus_market.sqlite.balance.set.success",
                        ModConfigs.prefix,
                        profile.getName(),
                        amount), false);
                Player target = source.getServer().getPlayerList().getPlayer(profile.getId());
                if (target != null) {
                    target.sendSystemMessage(Component.translatable("command.infinity_nexus_market.sqlite.balance.set.success_set",
                            ModConfigs.prefix,
                            amount,
                            profile.getName(),
                            (amount)));
                }
            }
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.balance.set.error", ModConfigs.prefix, e.getMessage()));
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
                DatabaseManager.setPlayerMaxSales(profile.getId().toString(), maxSales);
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
                int maxSales = DatabaseManager.getPlayerMaxSales(profile.getId().toString());
                int currentSales = DatabaseManager.getPlayerCurrentSalesCount(profile.getId().toString());
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
}