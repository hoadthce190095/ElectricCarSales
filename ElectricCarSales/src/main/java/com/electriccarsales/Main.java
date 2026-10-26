package com.electriccarsales;

import util.DBContext;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Checks the database configuration; Tomcat starts the web application.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws SQLException {
        try (Connection connection = DBContext.openConnection()) {
            System.out.println("Database connected: " + connection.getCatalog());
            System.out.println("Deploy ElectricCarSales:war exploded on Tomcat 10.1.");
            System.out.println("Open http://localhost:8080/ElectricCarSales/admin.jsp");
        }
    }
}
