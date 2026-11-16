package com.electriccarsales.dao;

import com.electriccarsales.model.Delivery;
import com.electriccarsales.model.OrderStatusHistory;
import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class DeliveryDAO extends DBContext {
    private static final String SELECT_SQL = """
        SELECT d.DeliveryID, d.OrderID, d.CustomerID, d.EmployeeID, d.CarID,
               d.DeliveryDate, d.DeliveryAddress, d.DeliveryStatus, d.Note,
               cu.CustomerName, cu.Phone AS CustomerPhone, e.EmployeeName,
               c.VIN, cm.ModelName, cv.VersionName, inv.InvoiceNumber
        FROM Deliveries d
        JOIN Customers cu ON cu.CustomerID = d.CustomerID
        LEFT JOIN Employees e ON e.EmployeeID = d.EmployeeID
        JOIN Cars c ON c.CarID = d.CarID
        JOIN CarModels cm ON cm.ModelID = c.ModelID
        JOIN CarVersions cv ON cv.VersionID = c.VersionID
        LEFT JOIN Invoices inv ON inv.OrderID = d.OrderID
        """;

    public List<Delivery> getAllDeliveries() {
        List<Delivery> list = new ArrayList<>();
        String sql = SELECT_SQL + " ORDER BY d.DeliveryDate DESC, d.DeliveryID DESC";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tải danh sách bàn giao từ cơ sở dữ liệu.", e);
        }
        return list;
    }

    public List<Delivery> findByCustomerId(int customerId) {
        List<Delivery> list = new ArrayList<>();
        String sql = SELECT_SQL + " WHERE d.CustomerID = ? ORDER BY d.DeliveryDate DESC, d.DeliveryID DESC";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải thông tin bàn giao xe.", e);
        }
        return list;
    }

    public Delivery findByOrderId(int orderId) {
        String sql = SELECT_SQL + " WHERE d.OrderID = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể tải thông tin bàn giao theo đơn hàng.", e);
        }
    }

    public boolean addDelivery(Delivery d) {
        String sql = "INSERT INTO Deliveries (OrderID, CustomerID, EmployeeID, CarID, DeliveryAddress, DeliveryStatus, Note) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, d.getOrderID());
            ps.setInt(2, d.getCustomerID());
            ps.setInt(3, d.getEmployeeID());
            ps.setInt(4, d.getCarID());
            ps.setNString(5, d.getDeliveryAddress());
            ps.setString(6, d.getDeliveryStatus());
            ps.setNString(7, d.getNote());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Lỗi tại addDelivery: " + e.getMessage());
        }
        return false;
    }

    /**
     * Executes vehicle handover & inventory export per RDS UC-14 and 10.3:
     * - Verifies Invoice is ISSUED and Order is PAID (RemainingAmount = 0) (BR5).
     * - Creates Deliveries record (DELIVERED) and InventoryTransactions record (EXPORT).
     * - Updates Car status to DELIVERED and Order status to COMPLETED.
     */
    public Delivery completeHandover(int orderId, Integer employeeId, String deliveryAddress, String note) {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                int customerId;
                Integer carId;
                String orderStatus;
                String invoiceNumber;
                String customerAddress;

                String sql = """
                    SELECT o.CustomerID, o.EmployeeID, od.CarID, os.StatusName AS OrderStatus,
                           inv.InvoiceNumber, ct.Status AS ContractStatus, cu.Address AS CustomerAddress
                    FROM Orders o WITH (UPDLOCK, HOLDLOCK)
                    JOIN OrderStatuses os ON os.OrderStatusID = o.OrderStatusID
                    JOIN Customers cu ON cu.CustomerID = o.CustomerID
                    LEFT JOIN OrderDetails od ON od.OrderID = o.OrderID
                    LEFT JOIN Invoices inv ON inv.OrderID = o.OrderID AND inv.Status='ISSUED'
                    LEFT JOIN Contracts ct ON ct.OrderID = o.OrderID
                    WHERE o.OrderID = ?
                    """;
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId);
                        customerId = rs.getInt("CustomerID");
                        carId = rs.getObject("CarID", Integer.class);
                        orderStatus = rs.getString("OrderStatus");
                        invoiceNumber = rs.getString("InvoiceNumber");
                        customerAddress = rs.getString("CustomerAddress");
                        if (!"SIGNED".equalsIgnoreCase(rs.getString("ContractStatus"))) {
                            throw new IllegalStateException("Hợp đồng chưa được ký.");
                        }
                        if (rs.next()) throw new IllegalStateException("Đơn hàng phải gắn đúng một xe vật lý.");
                    }
                }

                if ("COMPLETED".equalsIgnoreCase(orderStatus)) {
                    throw new IllegalStateException("Đơn hàng #" + orderId + " đã hoàn tất bàn giao xe trước đó.");
                }
                if (!"PAID".equalsIgnoreCase(orderStatus)) {
                    throw new IllegalStateException("Theo RDS (Mục II.10.3 BR5), chỉ được xuất kho và bàn giao xe khi đơn hàng đã thanh toán đủ 100% (PAID). Trạng thái hiện tại: " + orderStatus);
                }
                if (invoiceNumber == null || invoiceNumber.isBlank()) {
                    throw new IllegalStateException("Theo RDS (Mục II.10.3 EX-1 & BR5), bắt buộc phải phát hành hóa đơn (Invoices) trước khi xuất kho bàn giao xe.");
                }
                if (carId == null || carId <= 0) {
                    throw new IllegalStateException("Đơn hàng chưa gắn xe vật lý trong OrderDetails.");
                }

                PaymentDAO.requireExactSettlement(connection, orderId);
                if (employeeId == null || employeeId <= 0) {
                    throw new IllegalStateException("Không tìm thấy hồ sơ nhân viên bàn giao.");
                }
                int respEmp = employeeId;
                String addr = (deliveryAddress != null && !deliveryAddress.isBlank())
                        ? deliveryAddress.trim()
                        : ((customerAddress != null && !customerAddress.isBlank()) ? customerAddress.trim() : "Showroom VinFast Cần Thơ");
                String deliveryNote = (note == null || note.isBlank())
                        ? ("Đã xuất kho và bàn giao xe theo hóa đơn " + invoiceNumber)
                        : note.trim();

                try (PreparedStatement checkDel = connection.prepareStatement("SELECT DeliveryID FROM Deliveries WHERE OrderID = ?")) {
                    checkDel.setInt(1, orderId);
                    try (ResultSet rs = checkDel.executeQuery()) {
                        if (rs.next()) {
                            int delId = rs.getInt(1);
                            try (PreparedStatement upDel = connection.prepareStatement(
                                    "UPDATE Deliveries SET DeliveryStatus = 'DELIVERED', DeliveryAddress = ?, Note = ?, DeliveryDate = SYSDATETIME(), EmployeeID=? WHERE DeliveryID = ? AND DeliveryStatus <> 'DELIVERED'")) {
                                upDel.setNString(1, addr);
                                upDel.setNString(2, deliveryNote);
                                upDel.setInt(3, respEmp);
                                upDel.setInt(4, delId);
                                if (upDel.executeUpdate() != 1) throw new IllegalStateException("Xe đã được bàn giao.");
                            }
                        } else {
                            try (PreparedStatement inDel = connection.prepareStatement(
                                    "INSERT INTO Deliveries (OrderID, CustomerID, EmployeeID, CarID, DeliveryAddress, DeliveryStatus, Note) VALUES (?, ?, ?, ?, ?, 'DELIVERED', ?)")) {
                                inDel.setInt(1, orderId);
                                inDel.setInt(2, customerId);
                                inDel.setInt(3, respEmp);
                                inDel.setInt(4, carId);
                                inDel.setNString(5, addr);
                                inDel.setNString(6, deliveryNote);
                                inDel.executeUpdate();
                            }
                        }
                    }
                }

                // Record EXPORT in InventoryTransactions
                try (PreparedStatement invTx = connection.prepareStatement(
                        "INSERT INTO InventoryTransactions (CarID, EmployeeID, SupplierID, TransactionType, Note) VALUES (?, ?, ?, 'EXPORT', ?)")) {
                    invTx.setInt(1, carId);
                    invTx.setInt(2, respEmp);
                    invTx.setNull(3, Types.INTEGER);
                    invTx.setNString(4, "Xuất kho bàn giao xe cho đơn hàng #" + orderId + " (" + invoiceNumber + ")");
                    invTx.executeUpdate();
                }

                // Update Car status to DELIVERED
                try (PreparedStatement upCar = connection.prepareStatement("""
                        UPDATE Cars
                        SET CarStatusID = (SELECT CarStatusID FROM CarStatuses WHERE UPPER(StatusName) = 'DELIVERED')
                        WHERE CarID = ? AND CarStatusID IN (SELECT CarStatusID FROM CarStatuses WHERE UPPER(StatusName) = 'SOLD')
                        """)) {
                    upCar.setInt(1, carId);
                    if (upCar.executeUpdate() != 1) throw new IllegalStateException("Xe chưa ở trạng thái SOLD.");
                }

                // Update Order status to COMPLETED
                try (PreparedStatement upOrd = connection.prepareStatement("""
                        UPDATE Orders
                        SET OrderStatusID = (SELECT OrderStatusID FROM OrderStatuses WHERE StatusName = 'COMPLETED')
                        WHERE OrderID = ?
                        """)) {
                    upOrd.setInt(1, orderId);
                    upOrd.executeUpdate();
                }

                OrderStatusHistory history = new OrderStatusHistory();
                history.setOrderId(orderId);
                history.setStatus("COMPLETED");
                history.setEmployeeId(respEmp);
                history.setNote("Đã bàn giao xe cho khách hàng tại " + addr + " (COMPLETED).");
                new OrderStatusHistoryDAO().insert(connection, history);

                connection.commit();
                return findByOrderId(orderId);
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); } catch (SQLException rb) { ex.addSuppressed(rb); }
                throw ex;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể hoàn tất bàn giao xe.", e);
        }
    }

    private Delivery map(ResultSet rs) throws SQLException {
        Delivery d = new Delivery(
                rs.getInt("DeliveryID"),
                rs.getInt("OrderID"),
                rs.getInt("CustomerID"),
                rs.getInt("EmployeeID"),
                rs.getInt("CarID"),
                rs.getTimestamp("DeliveryDate"),
                rs.getString("DeliveryAddress"),
                rs.getString("DeliveryStatus"),
                rs.getString("Note")
        );
        d.setCustomerName(rs.getString("CustomerName"));
        d.setCustomerPhone(rs.getString("CustomerPhone"));
        d.setEmployeeName(rs.getString("EmployeeName"));
        d.setVin(rs.getString("VIN"));
        d.setModelName(rs.getString("ModelName"));
        d.setVersionName(rs.getString("VersionName"));
        d.setInvoiceNumber(rs.getString("InvoiceNumber"));
        return d;
    }
}
