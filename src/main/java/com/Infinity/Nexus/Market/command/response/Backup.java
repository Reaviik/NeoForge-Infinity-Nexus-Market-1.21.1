package com.Infinity.Nexus.Market.command.response;

import com.Infinity.Nexus.Market.utils.BackupManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Backup {
    public static int create(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;
        ServerLevel level = player.serverLevel();
        try {
            BackupManager.backupAll(level, Paths.get("backups"));
            player.displayClientMessage(Component.translatable("message.infinity_nexus_market.backup_success"), false);
            return 1;
        } catch (Exception e) {
            player.displayClientMessage(Component.translatable("message.infinity_nexus_market.backup_fail", e.getMessage()), false);
            return 0;
        }
    }
    
    public static int restore(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;
        
        String backupFileName = StringArgumentType.getString(context, "backup_file");
        Path backupPath = Paths.get("backups", backupFileName);
        
        try {
            boolean success = BackupManager.restoreBackup(backupPath);
            if (success) {
                player.displayClientMessage(Component.translatable("command.infinity_nexus_market.backup.restore.success", backupFileName), false);
                return 1;
            } else {
                player.displayClientMessage(Component.translatable("command.infinity_nexus_market.backup.restore.fail", backupFileName), false);
                return 0;
            }
        } catch (Exception e) {
            player.displayClientMessage(Component.translatable("command.infinity_nexus_market.backup.restore.error", e.getMessage()), false);
            return 0;
        }
    }
}
