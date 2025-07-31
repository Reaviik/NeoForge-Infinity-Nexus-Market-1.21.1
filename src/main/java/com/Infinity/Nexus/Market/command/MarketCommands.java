package com.Infinity.Nexus.Market.command;

import com.Infinity.Nexus.Market.command.response.Backup;
import com.Infinity.Nexus.Market.command.response.Balance;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;

public class MarketCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("market")
                        // Comandos de saldo (/market balance)
                        .then(Commands.literal("balance")
                                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                                        .executes(Balance::see)  // /market balance <target>
                                        .requires(source -> source.hasPermission(4))
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                                        .executes(Balance::set)))  // /market balance <target> set <amount>
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                                        .executes(Balance::add)))  // /market balance <target> add <amount>
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                                        .executes(Balance::remove)))))  // /market balance <target> remove <amount>

                        // Comandos de limite de vendas (/market salesLimit) - separado do balance
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.literal("salesLimit")
                                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                                        .executes(Balance::getMaxSales)  // /market salesLimit <target>
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(Balance::setMaxSales)))  // /market salesLimit <target> set <amount>
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(Balance::addMaxSales)))  // /market salesLimit <target> add <amount>
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(Balance::removeMaxSales)))))  // /market salesLimit <target> remove <amount>
        );
    }
}