/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import model.OrderDetail;
import util.DBContext;

/**
 *
 * @author Hoa Hà
 */
public class OrderDetailDAO extends DBContext{
    public List<OrderDetail> getOrderDetailsByOrderID(int orderID) {

    List<OrderDetail> list = new ArrayList<>();

    String sql = "SELECT "
            + "od.OrderDetailID, "
            + "od.OrderID, "
            + "od.CarID, "
            + "od.Quantity, "
            + "od.UnitPrice, "
            + "od.SubTotal, "
            + "c.VIN, "
            + "b.BrandName, "
            + "cm.ModelName, "
            + "c.Color, "
            + "c.ManufacturingYear, "
            + "cm.BatteryCapacity, "
            + "cm.RangeKM "
            + "FROM OrderDetails od "
            + "JOIN Cars c "
            + "ON od.CarID = c.CarID "
            + "JOIN CarModels cm "
            + "ON c.ModelID = cm.ModelID "
            + "JOIN Brands b "
            + "ON cm.BrandID = b.BrandID "
            + "WHERE od.OrderID = ?";

    try {

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, orderID);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            OrderDetail detail = new OrderDetail();

            detail.setOrderDetailID(
                    rs.getInt("OrderDetailID"));

            detail.setOrderID(
                    rs.getInt("OrderID"));

            detail.setCarID(
                    rs.getInt("CarID"));

            detail.setQuantity(
                    rs.getInt("Quantity"));

            detail.setUnitPrice(
                    rs.getDouble("UnitPrice"));

            detail.setSubTotal(
                    rs.getDouble("SubTotal"));

            // Car information
            detail.setVin(
                    rs.getString("VIN"));

            detail.setBrandName(
                    rs.getString("BrandName"));

            detail.setModelName(
                    rs.getString("ModelName"));

            detail.setColor(
                    rs.getString("Color"));

            detail.setManufacturingYear(
                    rs.getInt("ManufacturingYear"));

            detail.setBatteryCapacity(
                    rs.getDouble("BatteryCapacity"));

            detail.setRangeKM(
                    rs.getInt("RangeKM"));

            list.add(detail);
        }

        rs.close();
        ps.close();

    } catch (Exception e) {
        e.printStackTrace();
    }

    return list;
}
}
