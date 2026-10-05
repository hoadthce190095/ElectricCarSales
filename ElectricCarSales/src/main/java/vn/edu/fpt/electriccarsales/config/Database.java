package vn.edu.fpt.electriccarsales.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A fresh connection per operation; never share JDBC connections between requests.
 */
public final class Database {
    private static final Properties LOCAL = load();

    private Database() {
    }

    private static Properties load() {
        Properties result = new Properties();
        try (InputStream input = Database.class.getResourceAsStream("/db.properties")) {
            if (input != null) {
                result.load(input);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read db.properties", e);
        }
        return result;
    }

    private static String setting(String key, String fallback) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        if (value == null) {
            value = LOCAL.getProperty(key, fallback);
        }
        return value;
    }

    public static Connection getConnection() throws SQLException {
        String url = setting("DB_URL",
            "jdbc:sqlserver://localhost:1433;databaseName=ElectricCarSalesDB;encrypt=true;trustServerCertificate=true");
        String user = setting("DB_USER", "sa");
        String password = setting("DB_PASSWORD", "");
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQL Server JDBC driver missing", e);
        }
        return DriverManager.getConnection(url, user, password);
    }
}
