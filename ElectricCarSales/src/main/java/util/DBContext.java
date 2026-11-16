/*
- Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
- Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBContext {
    protected Connection conn = null;

    public DBContext() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            conn = openConnection();

            if (conn != null) {
                DatabaseMetaData dm = conn.getMetaData();
                System.out.println("Driver name: " + dm.getDriverName());
                System.out.println("Driver version: " + dm.getDriverVersion());
                System.out.println("Product name: " + dm.getDatabaseProductName());
                System.out.println("Product version: " + dm.getDatabaseProductVersion());
            }

        } catch (SQLException ex) {
            Logger.getLogger(DBContext.class.getName()).log(Level.SEVERE, "Không mở được kết nối SQL Server.", ex);
            System.out.println("Khong ket noi duoc roi em oi!!!!!");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DBContext.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // 1. Thêm hàm STATIC này để sửa dứt điểm lỗi ở AdminSetupController, LoginController...
    public static Connection openConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("Không tìm thấy SQLServerDriver", ex);
        }
        return com.electriccarsales.dao.FinanceConnection.open();
    }

    // 2. Thêm hàm này để các class DAO cũ của bạn vẫn hoạt động bình thường
    public Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = openConnection();
        }
        return conn;
    }

    public static void main(String[] args) {
        DBContext bd = new DBContext();
    }
}
