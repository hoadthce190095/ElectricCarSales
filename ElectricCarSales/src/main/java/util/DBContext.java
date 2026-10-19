package util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Shared database configuration for DAOs and services. Each operation opens its own JDBC
 * connection; existing group DAOs can also use the instance connection.
 */
public class DBContext implements AutoCloseable {
    private static final Properties LOCAL = load();
    protected Connection conn = null;

    public DBContext() {
        try {
            conn = openConnection();
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot connect to ElectricCarSalesDB", exception);
        }
    }

    private static Properties load() {
        Properties result = new Properties();
        try (InputStream input = DBContext.class.getResourceAsStream("/db.properties")) {
            if (input != null) {
                result.load(input);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read db.properties", exception);
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

    public static Connection openConnection() throws SQLException {
        String url = setting("DB_URL",
            "jdbc:sqlserver://localhost:1433;databaseName=ElectricCarSalesDB;encrypt=true;trustServerCertificate=true");
        String user = setting("DB_USER", "sa");
        String password = setting("DB_PASSWORD", "");
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("SQL Server JDBC driver missing", exception);
        }
        return DriverManager.getConnection(url, user, password);
    }

    public Connection getConnection() {
        return conn;
    }

    @Override
    public void close() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }
}
