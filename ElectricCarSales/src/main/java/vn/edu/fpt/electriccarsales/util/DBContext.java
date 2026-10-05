package vn.edu.fpt.electriccarsales.util;

import vn.edu.fpt.electriccarsales.config.Database;

import java.sql.Connection;
import java.sql.SQLException;

public class DBContext implements AutoCloseable {
    protected Connection conn = null;

    public DBContext() {
        try {
            conn = Database.getConnection();
        } catch (SQLException ex) {
            throw new IllegalStateException("Cannot connect to ElectricCarSalesDB", ex);
        }
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
