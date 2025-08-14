package com.Infinity.Nexus.Market.sqlite;

import com.Infinity.Nexus.Market.config.ModConfigs;
import com.Infinity.Nexus.Market.InfinityNexusMarket;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DataSourceManager {
    private static HikariDataSource dataSource;
    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized) return;

        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://"+ ModConfigs.dbIp + ":" + ModConfigs.dbPort  +"/"+ ModConfigs.dbName + "?useSSL=false&serverTimezone=UTC");
            config.setUsername(ModConfigs.dbUsername);
            config.setPassword(ModConfigs.dbPassword);
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setInitializationFailTimeout(5000); // Tempo para falhar rápido se não conectar

            // Configurações adicionais para melhor resiliência
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);

            // Testa a conexão imediatamente
            if (!isConnectionValid()) {
                throw new SQLException("Initial connection test failed");
            }

            initialized = true;
            InfinityNexusMarket.LOGGER.info("MySQL DataSource initialized successfully");
        } catch (Exception e) {
            InfinityNexusMarket.LOGGER.error("Failed to initialize MySQL DataSource", e);
            if (dataSource != null) {
                dataSource.close();
                dataSource = null;
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized || dataSource == null || dataSource.isClosed()) {
            initialize(); // Tenta reinicializar se necessário
        }

        if (dataSource == null) {
            throw new SQLException("DataSource is not available");
        }

        return dataSource.getConnection();
    }

    public static boolean isConnectionValid() {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }

        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (SQLException e) {
            InfinityNexusMarket.LOGGER.warn("Connection validation failed", e);
            return false;
        }
    }
}