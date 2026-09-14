/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import model.Employees;
import util.DBContext;

/**
 *
 * @author Hoa Hà
 */
public class EmployeeDAO extends DBContext {

    public List<Employees> getSaleEmployee() {
        List<Employees> list = new ArrayList();
        String sql = "SELECT EmployeeID, UserID, FullName, "
                + "Phone, Position, Status "
                + "FROM Employees "
                + "WHERE Position = 'Sales' "
                + "AND Status = 1";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Employees emp = new Employees();
                emp.setEmployeeID(rs.getInt("EmployeeID"));
                emp.setUserID(rs.getInt("UserID"));
                emp.setFullName(rs.getString("FullName"));
                emp.setPhone(rs.getString("Phone"));
                emp.setPosition(rs.getString("Position"));
                emp.setStatus(rs.getBoolean("Status"));

                list.add(emp);
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

  

}
