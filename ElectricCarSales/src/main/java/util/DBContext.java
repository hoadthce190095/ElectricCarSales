/*
- Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
- Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import com.electriccarsales.dao.FinanceConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBContext {
    public static Connection openConnection() throws SQLException {
        return FinanceConnection.open();
    }

    // 2. Thêm hàm này để các class DAO cũ của bạn vẫn hoạt động bình thường
    public Connection getConnection() throws SQLException {
        return openConnection();
    }

    public static void main(String[] args) {
        DBContext bd = new DBContext();
    }
}
