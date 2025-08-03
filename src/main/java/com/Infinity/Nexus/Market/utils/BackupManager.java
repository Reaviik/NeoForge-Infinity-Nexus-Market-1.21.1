package com.Infinity.Nexus.Market.utils;

import com.Infinity.Nexus.Market.InfinityNexusMarket;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupManager {
    public static void backupAll(Path backupDir) {
        try {
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            // Limpar backups antigos (manter só os 5 mais recentes)
            List<Path> backups = Files.list(backupDir)
                    .filter(p -> p.getFileName().toString().startsWith("market_backup_") &&
                            p.getFileName().toString().endsWith(".zip"))
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .collect(Collectors.toList());

            for (int i = 5; i < backups.size(); i++) {
                try { Files.deleteIfExists(backups.get(i)); } catch (Exception ignored) {}
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss"));
            Path zipPath = backupDir.resolve("market_backup_" + timestamp + ".zip");

            // Caminhos dos arquivos do SQLite (WAL mode)
            Path dbPath = Path.of("config/infinity_nexus_market/market.db");
            Path walPath = Path.of("config/infinity_nexus_market/market.db-wal");
            Path shmPath = Path.of("config/infinity_nexus_market/market.db-shm");

            if (!Files.exists(dbPath)) {
                InfinityNexusMarket.LOGGER.warn("Database SQLite não encontrada para backup: {}", dbPath);
                return;
            }

            try (OutputStream fos = Files.newOutputStream(zipPath);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                // 1. Backup do arquivo principal
                addToZip(zos, dbPath, "market.db");

                // 2. Backup dos arquivos WAL (se existirem)
                if (Files.exists(walPath)) addToZip(zos, walPath, "market.db-wal");
                if (Files.exists(shmPath)) addToZip(zos, shmPath, "market.db-shm");

                // 3. Metadata
                String metadata = String.format(
                        "Backup criado em: %s\n" +
                                "Tipo: SQLite WAL\n" +
                                "Arquivos: %s\n" +
                                "Tamanho total: %d bytes\n" +
                                "Servidor: %s",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                        Files.exists(walPath) ? "market.db + WAL + SHM" : "market.db (non-WAL)",
                        Files.size(dbPath) +
                                (Files.exists(walPath) ? Files.size(walPath) : 0) +
                                (Files.exists(shmPath) ? Files.size(shmPath) : 0),
                        InfinityNexusMarket.serverLevel.getServer().getMotd()
                );
                addTextToZip(zos, "backup_info.txt", metadata);

                InfinityNexusMarket.LOGGER.info("Backup completo criado: {}", zipPath);

            } catch (IOException e) {
                InfinityNexusMarket.LOGGER.error("Erro ao criar backup", e);
            }

        } catch (IOException e) {
            InfinityNexusMarket.LOGGER.error("Erro no sistema de backup", e);
        }
    }

    // Métodos auxiliares
    private static void addToZip(ZipOutputStream zos, Path filePath, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        Files.copy(filePath, zos);
        zos.closeEntry();
    }

    private static void addTextToZip(ZipOutputStream zos, String entryName, String content) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content.getBytes());
        zos.closeEntry();
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