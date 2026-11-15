package com.electriccarsales.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Creates SQL Server connections from deployment or local configuration. */
public final class FinanceConnection {
    private FinanceConnection() { }

    public static Connection open() throws SQLException {
        Properties config = loadLocalConfig();
        String url = first(config, false, "ELECTRIC_CAR_SALES_JDBC_URL", "DB_URL");
        String user = first(config, false, "ELECTRIC_CAR_SALES_DB_USER", "DB_USER");
        String password = first(config, true, "ELECTRIC_CAR_SALES_DB_PASSWORD", "DB_PASSWORD");
        if (url == null || url.isBlank()) {
            throw new SQLException("SQL Server chưa được cấu hình. Đặt biến môi trường DB_URL hoặc DB_URL trong ~/.electriccarsales/db.properties.");
        }
        if (url.startsWith("jdbc:sqlserver:")) {
            try {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            } catch (ClassNotFoundException exception) {
                throw new SQLException("Không tìm thấy SQL Server JDBC Driver.", exception);
            }
        }
        if (user == null || user.isBlank()) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, user, password == null ? "" : password);
    }

    private static String first(Properties config, boolean password, String... keys) {
        for (String key : keys) {
            String value = System.getProperty(key);
            if (value != null && (password || !value.isBlank())) {
                return password ? value : value.trim();
            }
        }
        for (String key : keys) {
            String value = System.getenv(key);
            if (value != null && (password || !value.isBlank())) {
                return password ? value : value.trim();
            }
        }
        for (String key : keys) {
            String value = config.getProperty(key);
            if (value != null && (password || !value.isBlank())) {
                return password ? value : value.trim();
            }
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
        Path userFile = Path.of(System.getProperty("user.home"), ".electriccarsales", "db.properties");
        if (Files.isRegularFile(userFile)) {
            try (InputStream input = Files.newInputStream(userFile)) {
                config.load(input);
            } catch (IOException | IllegalArgumentException exception) {
                throw new IllegalStateException("Không thể đọc cấu hình SQL Server trong thư mục người dùng.", exception);
            }
        }
        return config;
    }
}
