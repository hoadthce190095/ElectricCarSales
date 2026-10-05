package vn.edu.fpt.electriccarsales;

import vn.edu.fpt.electriccarsales.config.Database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Checks the database configuration; Tomcat starts the web application.
 */
public final class App {
    private App() {
    }

    public static void main(String[] args) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            System.out.println("Database connected: " + connection.getCatalog());
            System.out.println("Deploy ElectricCarSales:war exploded on Tomcat 10.1.");
            System.out.println("Open http://localhost:8080/ElectricCarSales/tan.html");
        }
    }
}
