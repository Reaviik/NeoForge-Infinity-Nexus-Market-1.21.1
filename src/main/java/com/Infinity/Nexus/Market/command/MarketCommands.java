package com.Infinity.Nexus.Market.command;

import com.Infinity.Nexus.Market.command.response.Balance;
import com.Infinity.Nexus.Market.utils.LotteryManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MarketCommands {
    public MarketCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("market")
                        .then(Commands.literal("top")
                                .executes(Balance::top))
                        .then(Commands.literal("lottery")
                                .executes(context -> {
                                    if (context.getSource().isPlayer()) {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        LotteryManager.addParticipant(player);
                                        return 1;
                                    } else {
                                        context.getSource().sendFailure(Component.translatable("command.infinity_nexus_market.sqlite.player_only"));
                                        return 0;
                                    }}))
                        .then(Commands.literal("balance")
                                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                                        .executes(Balance::see)  // /market balance <target>
                                        .then(Commands.literal("send")
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                                        .executes(Balance::send)))  // /market balance <target> send <amount>
                                        .then(Commands.literal("request")
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                                        .executes(Balance::request)))  // /market balance <target> request <amount>
                                        .then(Commands.literal("set")
                                                .requires(source -> source.hasPermission(4))
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                                        .executes(Balance::set)))  // /market balance <target> set <amount>
                                        .then(Commands.literal("add")
                                                .requires(source -> source.hasPermission(4))
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                                        .executes(Balance::add)))  // /market balance <target> add <amount>
                                        .then(Commands.literal("remove")
                                                .requires(source -> source.hasPermission(4))
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                                        .executes(Balance::remove)))))  // /market balance <target> remove <amount>

                        // Comandos de limite de vendas (/market salesLimit) - separado do balance
                        .then(Commands.literal("salesLimit")
                                .executes(Balance::getSelfMaxSales)
                                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                                        .executes(Balance::getMaxSales)
                                        .requires(source -> source.hasPermission(4))
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