
package util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBContext implements AutoCloseable {

    private static final Logger LOG =
            Logger.getLogger(DBContext.class.getName());

    protected Connection conn = null;

    // Constructor cho cac DAO su dung extends DBContext
    public DBContext() {
        try {
            conn = openConnection();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Khong the ket noi SQL Server", e);
            throw new IllegalStateException(
                    "Khong the ket noi SQL Server", e);
        }
    }

    // Ket noi SQL Server
    public static Connection openConnection() throws SQLException {

        String dbURL = System.getenv(
                "ELECTRIC_CAR_SALES_JDBC_URL");

        String username = System.getenv(
                "ELECTRIC_CAR_SALES_DB_USER");

        String password = System.getenv(
                "ELECTRIC_CAR_SALES_DB_PASSWORD");

        if (username == null || username.isBlank()) {
            throw new SQLException(
                    "Chua cau hinh ELECTRIC_CAR_SALES_DB_USER. "
                    + "Hay them tai khoan SQL Server vao Environment variables "
                    + "cua cau hinh chay Tomcat trong IntelliJ.");
        }

        if (password == null || password.isBlank()) {
            throw new SQLException(
                    "Chua cau hinh ELECTRIC_CAR_SALES_DB_PASSWORD. "
                    + "Hay them mat khau SQL Server vao Environment variables "
                    + "cua cau hinh chay Tomcat trong IntelliJ.");
        }

        // Cau hinh mac dinh cho SQL Server local
        if (dbURL == null || dbURL.isBlank()) {
            dbURL = "jdbc:sqlserver://localhost:1433;"
                    + "databaseName=ElectricCarSalesDB;"
                    + "encrypt=true;"
                    + "trustServerCertificate=true;";
        }

        try {
            Class.forName(
                    "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "Khong tim thay SQL Server JDBC Driver", e);
        }

        return DriverManager.getConnection(
                dbURL,
                username,
                password
        );
    }

    // Dong ket noi tu dong khi dung try-with-resources
    @Override
    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    // Kiem tra ket noi SQL Server
    public static void main(String[] args) {

        try (DBContext db = new DBContext()) {

            DatabaseMetaData dm = db.conn.getMetaData();

            System.out.println("Ket noi SQL Server thanh cong!");

            System.out.println(
                    "Driver name: " + dm.getDriverName());

            System.out.println(
                    "Driver version: " + dm.getDriverVersion());

            System.out.println(
                    "Product name: " + dm.getDatabaseProductName());

            System.out.println(
                    "Product version: " + dm.getDatabaseProductVersion());

        } catch (Exception e) {

            System.out.println("Khong ket noi duoc SQL Server!");
            e.printStackTrace();
        }
    }
}
