package com.electriccarsales.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Creates SQL Server connections from deployment or local configuration. */
public final class FinanceConnection {
    private FinanceConnection() { }

    public static Connection open() throws SQLException {
        Properties config = loadLocalConfig();
        String url = first(config, "ELECTRIC_CAR_SALES_JDBC_URL", "DB_URL");
        String user = first(config, "ELECTRIC_CAR_SALES_DB_USER", "DB_USER");
        String password = first(config, "ELECTRIC_CAR_SALES_DB_PASSWORD", "DB_PASSWORD");
        if (url == null || url.isBlank()) {
            throw new SQLException("SQL Server chưa được cấu hình. Đặt DB_URL trong db.properties hoặc biến môi trường.");
        }
        if (user == null || user.isBlank()) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, user, password == null ? "" : password);
    }

    private static String first(Properties config, String... keys) {
        for (String key : keys) {
            String value = System.getProperty(key);
            if (value != null && !value.isBlank()) return value.trim();
        }
        for (String key : keys) {
            String value = System.getenv(key);
            if (value != null && !value.isBlank()) return value.trim();
        }
        for (String key : keys) {
            String value = config.getProperty(key);
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    private static Properties loadLocalConfig() {
        Properties config = new Properties();
        try (InputStream input = FinanceConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) config.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể đọc db.properties.", exception);
        }
        return config;
    }
}
