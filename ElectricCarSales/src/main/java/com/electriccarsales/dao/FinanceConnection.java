package com.electriccarsales.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Creates SQL Server connections from deployment configuration. */
public final class FinanceConnection {
    private FinanceConnection() { }

    public static Connection open() throws SQLException {
        String url = System.getenv("ELECTRIC_CAR_SALES_JDBC_URL");
        String user = System.getenv("ELECTRIC_CAR_SALES_DB_USER");
        String password = System.getenv("ELECTRIC_CAR_SALES_DB_PASSWORD");
        if (url == null || url.isBlank()) {
            throw new SQLException("Missing ELECTRIC_CAR_SALES_JDBC_URL configuration.");
        }
        if (user == null || user.isBlank()) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, user, password == null ? "" : password);
    }
}
