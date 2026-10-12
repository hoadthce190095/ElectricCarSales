package com.electriccarsales.config;

import java.sql.Connection;
import java.sql.DriverManager;

public class Database {
    // Lưu ý: Hãy thay đổi "sa" và "123456" cho đúng với tài khoản SQL Server của bạn
    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=ElectricCarSalesDB;encrypt=true;trustServerCertificate=true;";
    private static final String USER = "sa";
    private static final String PASS = "123456";

    public static Connection getConnection() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (Exception e) {
            System.out.println("Lỗi kết nối DB: " + e.getMessage());
            return null;
        }
    }
}