package com.electriccarsales.service;

import com.electriccarsales.dao.FinanceConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Creates the Order and OrderDetail together, using the vehicle price held in SQL Server. */
public class SalesOrderService {
    public record Choice(int id, String label) {
        public int getId() { return id; }
        public String getLabel() { return label; }
    }
    public record Options(List<Choice> customers, List<Choice> cars, List<Choice> partners) {
        public List<Choice> getCustomers() { return customers; }
        public List<Choice> getCars() { return cars; }
        public List<Choice> getPartners() { return partners; }
    }

    public Options getOptions() throws SQLException {
        try (Connection c = FinanceConnection.open()) {
            List<Choice> customers = choices(c, "SELECT CustomerID,CustomerName FROM Customers ORDER BY CustomerName");
            List<Choice> cars = choices(c, "SELECT c.CarID,CONCAT(c.CarCode,' - ',m.ModelName,' - ',c.Price,' VND') "
                    + "FROM Cars c JOIN CarStatuses s ON s.CarStatusID=c.CarStatusID "
                    + "JOIN CarModels m ON m.ModelID=c.ModelID "
                    + "WHERE UPPER(LTRIM(RTRIM(s.StatusName))) IN (N'AVAILABLE',N'IN_STOCK',N'FOR_SALE',N'READY_FOR_SALE',N'CÓ SẴN',N'CO SAN') "
                    + "AND NOT EXISTS (SELECT 1 FROM OrderDetails d WHERE d.CarID=c.CarID) ORDER BY c.CarID");
            List<Choice> partners = choices(c, "SELECT CompanyID,CompanyName FROM FinancialCompanies ORDER BY CompanyName");
            return new Options(customers, cars, partners);
        }
    }

    /** Data used by the Sales dashboard's order and deposit views. */
    public Map<String, Object> dashboardData() throws SQLException {
        try (Connection c = FinanceConnection.open()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orders", query(c, """
                SELECT o.OrderID AS id,o.OrderDate AS orderDate,cu.CustomerID AS customerID,
                       cu.CustomerName AS customerName,cu.Phone AS customerPhone,
                       cm.ModelID AS modelID,cm.ModelName AS modelName,cv.VersionName AS versionName,
                       car.VIN AS vin,pm.MethodName AS paymentMethodName,fc.CompanyName AS companyName,
                       o.TotalAmount AS originalAmount,o.DepositAmount AS depositAmount,
                       COALESCE(paid.PaidAmount,0) AS paidAmount,o.RemainingAmount AS remainingAmount,
                       os.StatusName AS status,ct.Status AS contractStatus,
                       inv.InvoiceNumber AS invoiceNumber,d.DeliveryStatus AS deliveryStatus
                FROM Orders o
                JOIN Customers cu ON cu.CustomerID=o.CustomerID
                JOIN OrderStatuses os ON os.OrderStatusID=o.OrderStatusID
                JOIN PaymentMethods pm ON pm.PaymentMethodID=o.PaymentMethodID
                LEFT JOIN FinancialCompanies fc ON fc.CompanyID=o.CompanyID
                LEFT JOIN OrderDetails od ON od.OrderID=o.OrderID
                LEFT JOIN Cars car ON car.CarID=od.CarID
                LEFT JOIN CarModels cm ON cm.ModelID=car.ModelID
                LEFT JOIN CarVersions cv ON cv.VersionID=car.VersionID
                LEFT JOIN Contracts ct ON ct.OrderID=o.OrderID
                LEFT JOIN Invoices inv ON inv.OrderID=o.OrderID
                LEFT JOIN Deliveries d ON d.OrderID=o.OrderID
                LEFT JOIN (
                    SELECT p.OrderID,SUM(p.PaymentAmount) AS PaidAmount
                    FROM Payments p
                    JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID
                    WHERE ps.StatusID='COMPLETED'
                    GROUP BY p.OrderID
                ) paid ON paid.OrderID=o.OrderID
                ORDER BY o.OrderDate DESC,o.OrderID DESC
                """));
            data.put("customers", query(c, "SELECT CustomerID AS customerID,CustomerName AS customerName,Phone AS phone FROM Customers ORDER BY CustomerName"));
            data.put("cars", query(c, """
                SELECT car.CarID AS carID,car.ModelID AS modelID,car.Price AS price,car.VIN AS vin,
                       cm.ModelName AS modelName,cv.VersionName AS versionName,co.ColorName AS colorName
                FROM Cars car
                JOIN CarStatuses cs ON cs.CarStatusID=car.CarStatusID
                JOIN CarModels cm ON cm.ModelID=car.ModelID
                JOIN CarVersions cv ON cv.VersionID=car.VersionID
                JOIN Colors co ON co.ColorID=car.ColorID
                WHERE UPPER(LTRIM(RTRIM(cs.StatusName))) IN (N'AVAILABLE',N'IN_STOCK',N'FOR_SALE',N'READY_FOR_SALE',N'CÓ SẴN',N'CO SAN')
                  AND NOT EXISTS (SELECT 1 FROM OrderDetails od WHERE od.CarID=car.CarID)
                ORDER BY cm.ModelName,cv.VersionName,car.CarID
                """));
            data.put("inventorySummary", query(c, """
                SELECT (SELECT COUNT(*) FROM Cars) AS physicalCars,
                       (SELECT COUNT(*) FROM Cars car JOIN CarStatuses cs ON cs.CarStatusID=car.CarStatusID
                        WHERE UPPER(LTRIM(RTRIM(cs.StatusName))) IN (N'AVAILABLE',N'IN_STOCK',N'FOR_SALE',N'READY_FOR_SALE',N'CÓ SẴN',N'CO SAN')) AS availableCars,
                       (SELECT COUNT(*) FROM Cars car JOIN CarStatuses cs ON cs.CarStatusID=car.CarStatusID
                        WHERE UPPER(LTRIM(RTRIM(cs.StatusName))) IN (N'AVAILABLE',N'IN_STOCK',N'FOR_SALE',N'READY_FOR_SALE',N'CÓ SẴN',N'CO SAN')
                          AND NOT EXISTS (SELECT 1 FROM OrderDetails od WHERE od.CarID=car.CarID)) AS availableUnassignedCars
                """));
            data.put("paymentMethods", query(c, "SELECT PaymentMethodID AS paymentMethodID,MethodName AS methodName,Description AS description FROM PaymentMethods ORDER BY MethodName"));
            data.put("financialCompanies", query(c, "SELECT CompanyID AS companyID,CompanyName AS companyName FROM FinancialCompanies ORDER BY CompanyName"));
            return data;
        }
    }

    public int create(int salesUserId, int customerId, int carId, String paymentMethod, Integer companyId)
            throws SQLException {
        if (salesUserId <= 0 || customerId <= 0 || carId <= 0 || paymentMethod == null) {
            throw new IllegalArgumentException("Thiếu nhân viên, khách hàng, xe hoặc phương thức thanh toán.");
        }
        String method = paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (!List.of("CASH", "BANK_TRANSFER", "INSTALLMENT").contains(method)) {
            throw new IllegalArgumentException("Phương thức thanh toán không tồn tại.");
        }
        if ("INSTALLMENT".equals(method) != (companyId != null && companyId > 0)) {
            throw new IllegalArgumentException("Đơn trả góp cần chọn đối tác; trả thẳng không được có đối tác.");
        }
        try (Connection c = FinanceConnection.open()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                int employee = employeeId(c, salesUserId);
                if (employee <= 0) throw new SecurityException("Chỉ Sales Staff được lập đơn hàng.");
                if (!exists(c, "SELECT 1 FROM Customers WHERE CustomerID=?", customerId)) {
                    throw new IllegalArgumentException("Khách hàng không tồn tại.");
                }
                if (companyId != null && !exists(c, "SELECT 1 FROM FinancialCompanies WHERE CompanyID=?", companyId)) {
                    throw new IllegalArgumentException("Đối tác tài chính không tồn tại.");
                }
                BigDecimal price;
                String status;
                try (PreparedStatement ps = c.prepareStatement("SELECT c.Price,s.StatusName FROM Cars c WITH (UPDLOCK,HOLDLOCK) "
                        + "JOIN CarStatuses s ON s.CarStatusID=c.CarStatusID WHERE c.CarID=?")) {
                    ps.setInt(1, carId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Xe không tồn tại.");
                        price = rs.getBigDecimal("Price");
                        status = rs.getString("StatusName");
                    }
                }
                if (!isAvailableStatus(status) || exists(c,
                        "SELECT 1 FROM OrderDetails WITH (UPDLOCK,HOLDLOCK) WHERE CarID=?", carId)) {
                    throw new IllegalStateException("Xe đã được bán/đặt hoặc không còn khả dụng.");
                }
                if (price == null || price.signum() <= 0) throw new IllegalArgumentException("Giá xe không hợp lệ.");
                try { price.setScale(0, RoundingMode.UNNECESSARY); }
                catch (ArithmeticException e) { throw new IllegalStateException("Giá xe phải là số nguyên VND."); }
                BigDecimal deposit = price.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
                try { deposit.setScale(0, RoundingMode.UNNECESSARY); }
                catch (ArithmeticException e) { throw new IllegalStateException("30% tiền cọc cần quy tắc làm tròn VND được duyệt."); }
                BigDecimal remaining = price.subtract(deposit);
                int statusId = lookup(c, "SELECT OrderStatusID FROM OrderStatuses WHERE StatusName=?", "PENDING");
                int methodId = lookup(c, "SELECT PaymentMethodID FROM PaymentMethods WHERE MethodName=?", method);
                int orderId;
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO Orders(CustomerID,EmployeeID,OrderStatusID,"
                        + "PaymentMethodID,OrderDate,TotalAmount,DepositAmount,RemainingAmount,CompanyID) "
                        + "VALUES(?,?,?,?,SYSDATETIME(),?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, employee);
                    ps.setInt(3, statusId);
                    ps.setInt(4, methodId);
                    ps.setBigDecimal(5, price);
                    ps.setBigDecimal(6, deposit);
                    ps.setBigDecimal(7, remaining);
                    if (companyId == null) ps.setNull(8, Types.INTEGER);
                    else ps.setInt(8, companyId);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Cannot obtain OrderID");
                        orderId = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO OrderDetails(OrderID,CarID,SalesPrice) VALUES(?,?,?)")) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, carId);
                    ps.setBigDecimal(3, price);
                    ps.executeUpdate();
                }
                int reservedStatusId = ensureCarStatus(c, "RESERVED", "Xe đã được giữ chỗ cho đơn hàng");
                try (PreparedStatement ps = c.prepareStatement("UPDATE Cars SET CarStatusID=? WHERE CarID=?")) {
                    ps.setInt(1, reservedStatusId);
                    ps.setInt(2, carId);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không thể cập nhật trạng thái giữ chỗ cho xe.");
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO OrderStatusHistory(OrderID,OrderStatusID,EmployeeID,Note) VALUES(?,?,?,?)")) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, statusId);
                    ps.setInt(3, employee);
                    ps.setString(4, "Sales Staff created transaction");
                    ps.executeUpdate();
                }
                c.commit();
                return orderId;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static int employeeId(Connection c, int userId) throws SQLException {
        String sql = "SELECT e.EmployeeID FROM Employees e JOIN Users u ON u.UserID=e.UserID "
                + "JOIN Roles r ON r.RoleID=u.RoleID JOIN Statuses s ON s.StatusID=u.StatusID "
                + "WHERE u.UserID=? AND UPPER(r.RoleName)='SALES' AND UPPER(s.StatusName)='ACTIVE'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private static boolean isAvailableStatus(String status) {
        if (status == null) return false;
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return List.of("AVAILABLE", "IN_STOCK", "FOR_SALE", "READY_FOR_SALE", "CÓ SẴN", "CO SAN").contains(normalized);
    }

    private static List<Choice> choices(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Choice> choices = new ArrayList<>();
            while (rs.next()) choices.add(new Choice(rs.getInt(1), rs.getString(2)));
            return choices;
        }
    }

    private static List<Map<String, Object>> query(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            var metadata = rs.getMetaData();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int index = 1; index <= metadata.getColumnCount(); index++) {
                    row.put(metadata.getColumnLabel(index), rs.getObject(index));
                }
                rows.add(row);
            }
            return rows;
        }
    }

    private static boolean exists(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static int lookup(Connection c, String sql, String value) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Thiếu dữ liệu danh mục: " + value);
                return rs.getInt(1);
            }
        }
    }

    private static int ensureCarStatus(Connection c, String name, String description) throws SQLException {
        String lookup = "SELECT TOP (1) CarStatusID FROM CarStatuses WHERE UPPER(LTRIM(RTRIM(StatusName)))=?";
        try (PreparedStatement ps = c.prepareStatement(lookup)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO CarStatuses(StatusName,Description) VALUES(?,?)")) {
            ps.setNString(1, name);
            ps.setNString(2, description);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(lookup)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Không thể khởi tạo trạng thái xe " + name + ".");
                return rs.getInt(1);
            }
        }
    }
}
