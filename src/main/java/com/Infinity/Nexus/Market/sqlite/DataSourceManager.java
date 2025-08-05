package com.Infinity.Nexus.Market.sqlite;

import com.Infinity.Nexus.Market.config.ModConfigs;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DataSourceManager {
    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://"+ ModConfigs.dbIp + ":" + ModConfigs.dbPort  +"/"+ ModConfigs.dbName + "?useSSL=false&serverTimezone=UTC");
        config.setUsername(ModConfigs.dbUsername);
        config.setPassword(ModConfigs.dbPassword);
        config.setMaximumPoolSize(10); // ajuste conforme necessidade
        config.setConnectionTimeout(30000); // 30 segundos
        config.setIdleTimeout(600000); // 10 minutos
        config.setMaxLifetime(1800000); // 30 minutos

        dataSource = new HikariDataSource(config);
    }
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}