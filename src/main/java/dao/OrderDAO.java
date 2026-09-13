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
import model.Orders;
import util.DBContext;

/**
 *
 * @author Hoa Hà
 */
public class OrderDAO extends DBContext {

    public int createOrder(String customerName, String phone,
            String email, String address, int carID) {
        int orderID = -1;

        try {
            conn.setAutoCommit(false);

            //Find or create customer
            int customerID = -1;

            String findCustomerSQL = "SELECT CustomerID "
                    + "FROM Customers "
                    + "WHERE Phone = ?";

            PreparedStatement ps = conn.prepareStatement(findCustomerSQL);
            ps.setString(1, phone);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Customer already exists
                customerID = rs.getInt("CustomerID");
            } else {
                // Customer does not exist
                String insertCustomerSQL = "INSERT INTO Customers "
                        + "(UserID, FullName, Phone, Email, Address) "
                        + "VALUES (NULL, ?, ?, ?, ?)";

                PreparedStatement psInsertCustomer = conn.prepareStatement(insertCustomerSQL, PreparedStatement.RETURN_GENERATED_KEYS);

                psInsertCustomer.setString(1, customerName);
                psInsertCustomer.setString(2, phone);
                psInsertCustomer.setString(3, email);
                psInsertCustomer.setString(4, address);

                psInsertCustomer.executeUpdate();
                ResultSet rsNewCustomer = psInsertCustomer.getGeneratedKeys();

                if (rsNewCustomer.next()) {
                    customerID = rsNewCustomer.getInt(1);
                }

                rsNewCustomer.close();
                psInsertCustomer.close();
            }
            ps.close();
            rs.close();

            //check customer ID 
            if (customerID == -1) {
                conn.rollback();
                conn.setAutoCommit(true);
                return -1;
            }

            //Check selected car
            String checkCarSQL = "SELECT c.CarID, c.CarStatus, cm.Price "
                    + "FROM Cars c "
                    + "JOIN CarModels cm "
                    + "ON c.ModelID = cm.ModelID "
                    + "WHERE c.CarID = ?";

            PreparedStatement psCar = conn.prepareStatement(checkCarSQL);
            psCar.setInt(1, carID);
            ResultSet rsCar = psCar.executeQuery();
            if (!rsCar.next()) {
                conn.rollback();
                conn.setAutoCommit(true);
                return -1;
            }

            String carStatus = rsCar.getString("CarStatus");
            double price = rsCar.getDouble("Price");

            rsCar.close();
            psCar.close();

            // Car must be AVAILABLE
            if (!"AVAILABLE".equalsIgnoreCase(carStatus)) {
                conn.rollback();
                conn.setAutoCommit(true);
                return -1;
            }

            //Update car status
            String updateSQL = "UPDATE Cars "
                    + "SET CarStatus = 'SOLD' "
                    + "WHERE CarID = ? "
                    + "AND CarStatus = 'AVAILABLE'";

            PreparedStatement psUpdate = conn.prepareStatement(updateSQL);
            psUpdate.setInt(1, carID);
            int affectedRows = psUpdate.executeUpdate();
            psUpdate.close();

            if (affectedRows == 0) {
                conn.rollback();
                conn.setAutoCommit(true);
                return -1;
            }

            // CREATE ORDER
            String inserOrderSQL = " INSERT INTO Orders "
                    + "(CustomerID, EmployeeID, OrderStatus, TotalAmount, Note) "
                    + "VALUES (?, NULL, 'PENDING', ?, ?)";

            PreparedStatement psOrder = conn.prepareStatement(inserOrderSQL,
                    PreparedStatement.RETURN_GENERATED_KEYS);
            psOrder.setInt(1, customerID);
            psOrder.setDouble(2, price);
            psOrder.setString(3, "Order from website");
            psOrder.executeUpdate();

            ResultSet rsOrder = psOrder.getGeneratedKeys();
            if (rsOrder.next()) {
                orderID = rsOrder.getInt(1);
            }

            rsOrder.close();
            psOrder.close();

            //Check orderID
            if (orderID == -1) {
                conn.rollback();
                conn.setAutoCommit(true);
                return -1;
            }

            // Create OrderDetail
            String inserDetailSQL = "INSERT INTO OrderDetails "
                    + "(OrderID, CarID, Quantity, UnitPrice) "
                    + "VALUES (?, ?, ?, ?)";

            PreparedStatement psDetail = conn.prepareStatement(inserDetailSQL);

            psDetail.setInt(1, orderID);
            psDetail.setInt(2, carID);
            psDetail.setInt(3, 1);
            psDetail.setDouble(4, price);

            psDetail.executeUpdate();
            psDetail.close();

            // Comit transaction
            conn.commit();
            conn.setAutoCommit(true);

        } catch (Exception e) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (Exception ex) {
                ex.printStackTrace();

            }
            e.printStackTrace();
            orderID = -1;
        }

        return orderID;
    }

    public List<Orders> getAllOrders() {
        List<Orders> list = new ArrayList<>();

        String sql = "SELECT "
                + "o.OrderID, "
                + "o.CustomerID, "
                + "c.FullName, "
                + "c.Phone, "
                + "c.Email, "
                + "c.Address, "
                + "o.OrderDate, "
                + "o.OrderStatus, "
                + "o.TotalAmount, "
                + "o.Note "
                + "FROM Orders o "
                + "JOIN Customers c "
                + "ON o.CustomerID = c.CustomerID "
                + "ORDER BY o.OrderID ASC";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Orders order = new Orders();

                order.setOrderID(rs.getInt("OrderID"));
                order.setCustomerID(rs.getInt("CustomerID"));
                order.setCustomerName(rs.getString("FullName"));
                order.setPhone(rs.getString("Phone"));
                order.setEmail(rs.getString("Email"));
                order.setAddress(rs.getString("Address"));
                order.setOrderDate(rs.getTimestamp("OrderDate"));
                order.setOrderStatus(rs.getString("OrderStatus"));
                order.setTotalAmount(rs.getDouble("TotalAmount"));
                order.setNote(rs.getString("Note"));

                list.add(order);
            }

            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public Orders getOrderByID(int orderID) {
        Orders order = null;

        String sql = "SELECT "
                + "o.OrderID, "
                + "o.CustomerID, "
                + "c.FullName, "
                + "c.Phone, "
                + "c.Email, "
                + "c.Address, "
                + "o.OrderDate, "
                + "o.OrderStatus, "
                + "o.TotalAmount, "
                + "o.Note "
                + "FROM Orders o "
                + "JOIN Customers c "
                + "ON o.CustomerID = c.CustomerID "
                + "WHERE o.OrderID = ?";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, orderID);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                order = new Orders();

                order.setOrderID(rs.getInt("OrderID"));
                order.setCustomerID(rs.getInt("CustomerID"));
                order.setCustomerName(rs.getString("FullName"));
                order.setPhone(rs.getString("Phone"));
                order.setEmail(rs.getString("Email"));
                order.setAddress(rs.getString("Address"));
                order.setOrderDate(rs.getTimestamp("OrderDate"));
                order.setOrderStatus(rs.getString("OrderStatus"));
                order.setTotalAmount(rs.getDouble("TotalAmount"));
                order.setNote(rs.getString("Note"));

            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return order;
    }

    public List<OrderDetail> getOrderDetailByID(int orderID) {
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
