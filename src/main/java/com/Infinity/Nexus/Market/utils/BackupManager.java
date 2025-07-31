package com.Infinity.Nexus.Market.utils;

import com.Infinity.Nexus.Market.InfinityNexusMarket;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupManager {
    public static void backupAll(Path backupDir) {
        try {
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }
            
            // Limpar backups antigos (manter só os 5 mais recentes)
            java.util.List<Path> backups = Files.list(backupDir)
                    .filter(p -> p.getFileName().toString().startsWith("market_backup_") && p.getFileName().toString().endsWith(".zip"))
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString())) // mais novo primeiro
                    .toList();
            for (int i = 5; i < backups.size(); i++) {
                try { Files.deleteIfExists(backups.get(i)); } catch (Exception ignored) {}
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy-HH:mm")).replace("-", "_").replace("/", "-").replace(":", "-");
            Path zipPath = backupDir.resolve("market_backup_" + timestamp + ".zip");
            
            // Caminho da database SQLite
            Path dbPath = Path.of("config/infinity_nexus_market/market.db");
            
            if (!Files.exists(dbPath)) {
                InfinityNexusMarket.LOGGER.warn("Database SQLite não encontrada para backup: {}", dbPath);
                return;
            }
            
            try (OutputStream fos = Files.newOutputStream(zipPath);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                
                // Backup da database SQLite
                ZipEntry dbEntry = new ZipEntry("market.db");
                zos.putNextEntry(dbEntry);
                Files.copy(dbPath, zos);
                zos.closeEntry();
                
                // Backup de informações adicionais (metadata)
                ZipEntry metadataEntry = new ZipEntry("backup_info.txt");
                zos.putNextEntry(metadataEntry);
                String metadata = String.format(
                    "Backup criado em: %s\n" +
                    "Database: market.db\n" +
                    "Versão: SQLite\n" +
                    "Tamanho: %d bytes\n" +
                    "Servidor: %s",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    Files.size(dbPath),
                    InfinityNexusMarket.serverLevel.getServer().getMotd()
                );
                zos.write(metadata.getBytes());
                zos.closeEntry();
                
                InfinityNexusMarket.LOGGER.info("Backup da database SQLite criado com sucesso: {}", zipPath);
                
            } catch (IOException e) {
                InfinityNexusMarket.LOGGER.error("Erro ao criar backup da database SQLite", e);
            }
            
        } catch (IOException e) {
            InfinityNexusMarket.LOGGER.error("Erro no sistema de backup", e);
        }
    }
    
    /**
     * Restaura backup da database SQLite
     */
    public static boolean restoreBackup(Path backupZipPath) {
        try {
            Path dbPath = Path.of("config/infinity_nexus_market/market.db");
            Path backupDir = Path.of("config/infinity_nexus_market/backup_restore");
            
            if (!Files.exists(backupZipPath)) {
                InfinityNexusMarket.LOGGER.error("Arquivo de backup não encontrado: {}", backupZipPath);
                return false;
            }
            
            // Criar diretório temporário para extração
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }
            
            // Extrair backup
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(Files.newInputStream(backupZipPath))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("market.db")) {
                        Path extractedDb = backupDir.resolve("market.db");
                        Files.copy(zis, extractedDb, StandardCopyOption.REPLACE_EXISTING);
                        
                        // Fazer backup do arquivo atual antes de restaurar
                        if (Files.exists(dbPath)) {
                            Path currentBackup = backupDir.resolve("market_current_backup.db");
                            Files.copy(dbPath, currentBackup, StandardCopyOption.REPLACE_EXISTING);
                            InfinityNexusMarket.LOGGER.info("Backup do arquivo atual criado: {}", currentBackup);
                        }
                        
                        // Restaurar database
                        Files.copy(extractedDb, dbPath, StandardCopyOption.REPLACE_EXISTING);
                        InfinityNexusMarket.LOGGER.info("Database SQLite restaurada com sucesso de: {}", backupZipPath);
                        
                        // Limpar arquivos temporários
                        Files.deleteIfExists(extractedDb);
                        return true;
                    }
                    zis.closeEntry();
                }
            }
            
            // Limpar diretório temporário
            try {
                Files.walk(backupDir)
                    .sorted((a, b) -> b.compareTo(a)) // deletar arquivos primeiro, depois diretórios
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            InfinityNexusMarket.LOGGER.warn("Não foi possível deletar arquivo temporário: {}", path);
                        }
                    });
            } catch (IOException e) {
                InfinityNexusMarket.LOGGER.warn("Erro ao limpar arquivos temporários", e);
            }
            
        } catch (IOException e) {
            InfinityNexusMarket.LOGGER.error("Erro ao restaurar backup", e);
        }
        
        return false;
    }
} 