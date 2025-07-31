package com.Infinity.Nexus.Market.command.response;

import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.utils.BackupManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Backup {
    public static int create(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;
        try {
            BackupManager.backupAll(Paths.get("config/infinity_nexus_market/backups"));
            player.displayClientMessage(Component.translatable("message.infinity_nexus_market.backup_success", ModConfigs.prefix), false);
            return 1;
        } catch (Exception e) {
            player.displayClientMessage(Component.translatable("message.infinity_nexus_market.backup_fail", ModConfigs.prefix, e.getMessage()), false);
            return 0;
        }
    }

    public static int restore(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String backupFileName = StringArgumentType.getString(context, "backup_file");
        Path backupPath = Paths.get("config/infinity_nexus_market/backups", backupFileName);

        try {
            boolean success = BackupManager.restoreBackup(backupPath);
            if (success) {
                player.displayClientMessage(Component.translatable("command.infinity_nexus_market.backup.restore.success", ModConfigs.prefix, backupFileName), false);
                return 1;
            } else {
                player.displayClientMessage(Component.translatable("command.infinity_nexus_market.backup.restore.fail", ModConfigs.prefix, backupFileName), false);
                return 0;
            }
        } catch (Exception e) {
            player.displayClientMessage(Component.translatable("command.infinity_nexus_market.backup.restore.error", ModConfigs.prefix, e.getMessage()),  false);
            return 0;
        }
    }

    public static int list(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        try {
            Path backupDir = Paths.get("config/infinity_nexus_market/backups");

            if (!Files.exists(backupDir)) {
                player.displayClientMessage(Component.translatable("command.infinity_nexus_market.backup.list.none", ModConfigs.prefix), false);
                return 0;
            }

            List<Path> backups = Files.list(backupDir)
                    .filter(p -> p.getFileName().toString().startsWith("market_backup_") &&
                            p.getFileName().toString().endsWith(".zip"))
                    .sorted((a, b) -> {
                        // Ordena por data de modificação (mais recente primeiro)
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .collect(Collectors.toList());

            if (backups.isEmpty()) {
                player.displayClientMessage(Component.translatable("command.infinity_nexus_market.backup.list.none", ModConfigs.prefix), false);
                return 0;
            }

            // Título da lista
            player.displayClientMessage(Component.translatable("command.infinity_nexus_market.backup.list.title", ModConfigs.prefix), false);

            // Lista de backups clicáveis
            for (Path backup : backups) {
                String fileName = backup.getFileName().toString();
                MutableComponent backupLine = Component.literal("§7- §e" + fileName)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.SUGGEST_COMMAND,
                                        "/market backup create restore \"" + fileName + "\"" // Adiciona aspas para lidar com espaços/barras
                                ))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable("command.infinity_nexus_market.backup.list.hover")
                                )));
                player.displayClientMessage(backupLine, false);
            }

            return 1;
        } catch (Exception e) {
            player.displayClientMessage(Component.translatable("command.infinity_nexus_market.backup.list.error", ModConfigs.prefix, e.getMessage()), false);
            return 0;
        }
    }
}