package com.electriccarsales.dao;

import util.DBContext;
import com.electriccarsales.model.ReportFilter;
import com.electriccarsales.model.SaleVehicle;
import com.electriccarsales.model.StatusTotal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only reporting queries compatible with fixed ElectricCarSalesDB.sql schema.
 */
public class ReportDAO {
    @FunctionalInterface
    public interface ConnectionSupplier {
        Connection get() throws SQLException;
    }

    private final ConnectionSupplier connections;

    public ReportDAO() {
        this(DBContext::openConnection);
    }

    public ReportDAO(ConnectionSupplier connections) {
        this.connections = connections;
    }

    private static final String ORDER_JOINS = """
         JOIN OrderStatuses os ON os.OrderStatusID = o.OrderStatusID
         LEFT JOIN Contracts ct ON ct.OrderID = o.OrderID
         LEFT JOIN Invoices inv ON inv.OrderID = o.OrderID
        """;
    private static final String ELIGIBLE = """
         os.StatusName <> 'CANCELLED'
         AND (ct.ContractID IS NULL OR (UPPER(ct.Status) NOT IN ('CANCELLED', 'VOID', 'DRAFT')
              AND ct.CustomerID = o.CustomerID AND ct.ContractAmount = o.TotalAmount))
         AND (inv.InvoiceID IS NULL OR (UPPER(inv.Status) NOT IN ('CANCELLED', 'VOID', 'DRAFT')
              AND inv.CustomerID = o.CustomerID AND inv.TotalAmount = o.TotalAmount))
        """;

    private boolean hasInstallmentTables(Connection connection) throws SQLException {
        return SchemaTables.exists(connection, "InstallmentContracts")
            && SchemaTables.exists(connection, "InstallmentSchedules")
            && SchemaTables.exists(connection, "InstallmentPayments");
    }

    public Snapshot read(ReportFilter filter, LocalDate today) throws SQLException {
        filter.validate();
        try (Connection connection = connections.get()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            try {
                List<OrderRow> orders = orders(connection, filter);
                List<VehicleRow> vehicles = vehicles(connection, filter);
                List<PaymentRow> payments = payments(connection, filter);
                boolean hasInstallments = hasInstallmentTables(connection);
                ExtraTotals extras = extras(connection, filter, today, hasInstallments);
                List<StatusTotal> inventory = inventory(connection, filter);
                boolean containsDemo = hasDemoInventory(connection, filter);
                if (!containsDemo) {
                    containsDemo = hasDemoPayment(connection, filter);
                }
                if (!containsDemo && hasInstallments) {
                    containsDemo = hasDemoInstallmentPayment(connection, filter);
                }
                connection.commit();
                return new Snapshot(orders, vehicles, payments, extras, inventory, containsDemo);
            } catch (SQLException | RuntimeException ex) {
                try {
                    connection.rollback();
                } catch (SQLException rollback) {
                    ex.addSuppressed(rollback);
                }
                throw ex;
            }
        }
    }

    private List<OrderRow> orders(Connection connection, ReportFilter filter) throws SQLException {
        String sql = """
            SELECT o.OrderID, o.OrderDate, o.EmployeeID, os.StatusName, o.TotalAmount,
                   cu.CustomerName, e.EmployeeName, ct.ContractNumber, inv.InvoiceNumber,
                   d.DeliveryStatus, COALESCE(pp.PaidAmount, 0) PaidAmount,
                   COALESCE(inv.DiscountAmount, 0) DiscountAmount,
                   COALESCE(inv.TaxAmount, 0) TaxAmount,
                   CASE WHEN
            """ + ELIGIBLE + " THEN 1 ELSE 0 END Eligible FROM Orders o " + ORDER_JOINS + """
             JOIN Customers cu ON cu.CustomerID = o.CustomerID
             LEFT JOIN Employees e ON e.EmployeeID = o.EmployeeID
             LEFT JOIN Deliveries d ON d.OrderID = o.OrderID
             LEFT JOIN (SELECT p.OrderID, SUM(p.PaymentAmount) PaidAmount
                        FROM Payments p JOIN PaymentStatuses ps ON ps.PaymentStatusID = p.PaymentStatusID
                        WHERE ps.StatusID = 'COMPLETED' GROUP BY p.OrderID) pp ON pp.OrderID = o.OrderID
             WHERE o.OrderDate >= ? AND o.OrderDate < ?
            """ + filters(filter) + " ORDER BY o.OrderDate, o.OrderID";
        List<OrderRow> rows = new ArrayList<>();
        try (PreparedStatement statement = dated(connection, sql, filter)) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int employeeId = rs.getInt("EmployeeID");
                    Integer employee = rs.wasNull() ? null : employeeId;
                    rows.add(new OrderRow(rs.getInt("OrderID"), rs.getTimestamp("OrderDate").toLocalDateTime(),
                        employee, rs.getString("EmployeeName"), rs.getString("CustomerName"),
                        rs.getString("StatusName"), rs.getBigDecimal("TotalAmount"), rs.getBigDecimal("PaidAmount"),
                        rs.getString("ContractNumber"), rs.getString("InvoiceNumber"), rs.getString("DeliveryStatus"),
                        rs.getBigDecimal("DiscountAmount"), rs.getBigDecimal("TaxAmount"), rs.getBoolean("Eligible")));
                }
            }
        }
        return rows;
    }

    private List<VehicleRow> vehicles(Connection connection, ReportFilter filter) throws SQLException {
        String sql = """
            SELECT od.OrderID, c.CarID, c.VIN, cm.ModelID, cm.ModelName, cv.VersionName, od.SalesPrice
            FROM OrderDetails od JOIN Orders o ON o.OrderID = od.OrderID
            JOIN Cars c ON c.CarID = od.CarID JOIN CarModels cm ON cm.ModelID = c.ModelID
            JOIN Carversions cv ON cv.VersionID = c.VersionID
            WHERE o.OrderDate >= ? AND o.OrderDate < ?
            """ + filters(filter) + " ORDER BY od.OrderID, od.OrderDetailID";
        List<VehicleRow> rows = new ArrayList<>();
        try (PreparedStatement statement = dated(connection, sql, filter); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new VehicleRow(rs.getInt("OrderID"),
                    new SaleVehicle(rs.getInt("CarID"), rs.getString("VIN"), rs.getInt("ModelID"),
                        rs.getString("ModelName"), rs.getString("VersionName"), rs.getBigDecimal("SalesPrice"))));
            }
        }
        return rows;
    }

    private List<PaymentRow> payments(Connection connection, ReportFilter filter) throws SQLException {
        String sql = """
            SELECT p.PaymentDate, ps.StatusID, p.PaymentAmount, CASE WHEN
            """ + ELIGIBLE + " THEN 1 ELSE 0 END Eligible FROM Payments p JOIN Orders o ON o.OrderID = p.OrderID "
            + ORDER_JOINS + """
                 JOIN PaymentStatuses ps ON ps.PaymentStatusID = p.PaymentStatusID
                 WHERE p.PaymentDate >= ? AND p.PaymentDate < ?
                """ + filters(filter);
        List<PaymentRow> rows = new ArrayList<>();
        try (PreparedStatement statement = dated(connection, sql, filter); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new PaymentRow(rs.getTimestamp("PaymentDate").toLocalDateTime(), rs.getString("StatusID"),
                    rs.getBigDecimal("PaymentAmount"), rs.getBoolean("Eligible")));
            }
        }
        return rows;
    }

    private ExtraTotals extras(Connection connection, ReportFilter filter, LocalDate today,
                               boolean hasInstallments) throws SQLException {
        if (!hasInstallments) {
            // In ElectricCarSalesDB.sql, installment orders use Orders.CompanyID or PaymentMethods.MethodName = 'INSTALLMENT'
            String countSql = "SELECT COUNT(*) Total FROM Orders o JOIN PaymentMethods pm ON pm.PaymentMethodID = o.PaymentMethodID "
                + ORDER_JOINS + " WHERE o.OrderDate >= ? AND o.OrderDate < ? "
                + "AND (o.CompanyID IS NOT NULL OR pm.MethodName = 'INSTALLMENT') AND " + ELIGIBLE + filters(filter);
            long contracts = 0;
            try (PreparedStatement statement = dated(connection, countSql, filter);
                 ResultSet rs = statement.executeQuery()) {
                if (rs.next()) contracts = rs.getLong("Total");
            }
            return new ExtraTotals(BigDecimal.ZERO, contracts, BigDecimal.ZERO);
        }
        String receiptSql = """
            SELECT COALESCE(SUM(ip.PaymentAmount), 0) Amount
            FROM InstallmentPayments ip JOIN PaymentStatuses ps ON ps.PaymentStatusID = ip.PaymentStatusID
            JOIN InstallmentSchedules sch ON sch.ScheduleID = ip.ScheduleID
            JOIN InstallmentContracts ic ON ic.InstallmentContractID = sch.InstallmentContractID
            JOIN Orders o ON o.OrderID = ic.OrderID
            """ + ORDER_JOINS + " WHERE ip.PaymentDate >= ? AND ip.PaymentDate < ? AND ps.StatusID ="
            + " 'COMPLETED' AND UPPER(ic.Status) NOT IN ('CANCELLED', 'VOID', 'DRAFT') AND " + ELIGIBLE
            + filters(filter);
        BigDecimal receipts;
        try (PreparedStatement statement = dated(connection, receiptSql, filter);
            ResultSet rs = statement.executeQuery()) {
            rs.next();
            receipts = rs.getBigDecimal("Amount");
        }
        String contractsSql = "SELECT COUNT(*) Total FROM InstallmentContracts ic JOIN Orders o ON o.OrderID ="
            + " ic.OrderID " + ORDER_JOINS + " WHERE o.OrderDate >= ? AND o.OrderDate < ? "
            + "AND UPPER(ic.Status) NOT IN ('CANCELLED', 'VOID', 'DRAFT') AND " + ELIGIBLE + filters(filter);
        long contracts;
        try (PreparedStatement statement = dated(connection, contractsSql, filter);
            ResultSet rs = statement.executeQuery()) {
            rs.next();
            contracts = rs.getLong("Total");
        }
        String overdueSql = """
            SELECT COALESCE(SUM(CASE WHEN sch.PaymentAmount > COALESCE(paid.Amount, 0)
                 THEN sch.PaymentAmount - COALESCE(paid.Amount, 0) ELSE 0 END), 0) Amount
            FROM InstallmentSchedules sch
            JOIN InstallmentContracts ic ON ic.InstallmentContractID = sch.InstallmentContractID
            JOIN Orders o ON o.OrderID = ic.OrderID
            """ + ORDER_JOINS + """
             LEFT JOIN (SELECT ip.ScheduleID, SUM(ip.PaymentAmount) Amount FROM InstallmentPayments ip
                 JOIN PaymentStatuses ps ON ps.PaymentStatusID = ip.PaymentStatusID
                 WHERE ps.StatusID = 'COMPLETED' GROUP BY ip.ScheduleID) paid ON paid.ScheduleID = sch.ScheduleID
             WHERE o.OrderDate >= ? AND o.OrderDate < ?
            """ + filters(filter) + " AND sch.DueDate < ? AND UPPER(sch.Status) NOT IN ('CANCELLED', 'VOID') "
            + "AND UPPER(ic.Status) NOT IN ('CANCELLED', 'VOID', 'DRAFT') AND " + ELIGIBLE;
        BigDecimal overdue;
        try (PreparedStatement statement = dated(connection, overdueSql, filter)) {
            statement.setDate(3 + filterCount(filter), java.sql.Date.valueOf(today));
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                overdue = rs.getBigDecimal("Amount");
            }
        }
        return new ExtraTotals(receipts, contracts, overdue);
    }

    private List<StatusTotal> inventory(Connection connection, ReportFilter filter) throws SQLException {
        String sql = """
            SELECT cs.StatusName, COUNT(*) Total, COALESCE(SUM(c.Price), 0) Amount
            FROM Cars c JOIN CarStatuses cs ON cs.CarStatusID = c.CarStatusID
            """ + (filter.getModelId() == null ? "" : " WHERE c.ModelID = ? ")
            + " GROUP BY cs.StatusName ORDER BY cs.StatusName";
        List<StatusTotal> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (filter.getModelId() != null) {
                statement.setInt(1, filter.getModelId());
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(
                        new StatusTotal(rs.getString("StatusName"), rs.getLong("Total"), rs.getBigDecimal("Amount")));
                }
            }
        }
        return rows;
    }

    private boolean hasDemoInventory(Connection connection, ReportFilter filter) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Cars c JOIN CarStatuses cs ON cs.CarStatusID=c.CarStatusID"
            + " WHERE c.VIN LIKE 'DEMO%'" + (filter.getModelId() == null ? "" : " AND c.ModelID=?");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (filter.getModelId() != null) {
                statement.setInt(1, filter.getModelId());
            }
            statement.setQueryTimeout(30);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1) > 0;
            }
        }
    }

    private boolean hasDemoPayment(Connection connection, ReportFilter filter) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Payments p JOIN Orders o ON o.OrderID=p.OrderID"
            + " JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID"
            + " WHERE p.PaymentDate>=? AND p.PaymentDate<?" + filters(filter) + demoOrderCondition();
        try (PreparedStatement statement = dated(connection, sql, filter);
            ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getLong(1) > 0;
        }
    }

    private boolean hasDemoInstallmentPayment(Connection connection, ReportFilter filter) throws SQLException {
        String sql = "SELECT COUNT(*) FROM InstallmentPayments ip JOIN PaymentStatuses ps ON"
            + " ps.PaymentStatusID=ip.PaymentStatusID JOIN InstallmentSchedules sch ON"
            + " sch.ScheduleID=ip.ScheduleID JOIN InstallmentContracts ic ON"
            + " ic.InstallmentContractID=sch.InstallmentContractID JOIN Orders o ON o.OrderID=ic.OrderID"
            + ORDER_JOINS + " WHERE ip.PaymentDate>=? AND ip.PaymentDate<? AND"
            + " ps.StatusID='COMPLETED' AND UPPER(ic.Status) NOT IN ('CANCELLED','VOID','DRAFT') AND " + ELIGIBLE
            + filters(filter) + demoOrderCondition();
        try (PreparedStatement statement = dated(connection, sql, filter);
            ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getLong(1) > 0;
        }
    }

    private String demoOrderCondition() {
        return " AND EXISTS (SELECT 1 FROM OrderDetails odd JOIN Cars cd ON cd.CarID=odd.CarID"
            + " WHERE odd.OrderID=o.OrderID AND cd.VIN LIKE 'DEMO%')";
    }

    private static String filters(ReportFilter filter) {
        return (filter.getEmployeeId() == null ? "" : " AND o.EmployeeID = ?")
            + (filter.getModelId() == null
                ? ""
                : " AND EXISTS (SELECT 1 FROM OrderDetails odf JOIN Cars cf ON cf.CarID ="
                    + " odf.CarID WHERE odf.OrderID = o.OrderID AND cf.ModelID = ?)")
            + (filter.getPaymentMethodId() == null ? "" : " AND o.PaymentMethodID = ?");
    }

    private static int filterCount(ReportFilter filter) {
        return (filter.getEmployeeId() == null ? 0 : 1) + (filter.getModelId() == null ? 0 : 1)
            + (filter.getPaymentMethodId() == null ? 0 : 1);
    }

    private static PreparedStatement dated(Connection connection, String sql, ReportFilter filter) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            statement.setTimestamp(1, Timestamp.valueOf(filter.getFrom().atStartOfDay()));
            statement.setTimestamp(2, Timestamp.valueOf(filter.getTo().plusDays(1).atStartOfDay()));
            int index = 3;
            if (filter.getEmployeeId() != null) {
                statement.setInt(index++, filter.getEmployeeId());
            }
            if (filter.getModelId() != null) {
                statement.setInt(index++, filter.getModelId());
            }
            if (filter.getPaymentMethodId() != null) {
                statement.setInt(index, filter.getPaymentMethodId());
            }
            statement.setQueryTimeout(30);
            return statement;
        } catch (SQLException ex) {
            statement.close();
            throw ex;
        }
    }

    public static class Snapshot {
        private List<OrderRow> orders;
        private List<VehicleRow> vehicles;
        private List<PaymentRow> payments;
        private ExtraTotals extras;
        private List<StatusTotal> inventory;
        private boolean containsDemo;

        public Snapshot() {
        }

        public Snapshot(List<OrderRow> orders, List<VehicleRow> vehicles, List<PaymentRow> payments, ExtraTotals extras,
            List<StatusTotal> inventory) {
            this(orders, vehicles, payments, extras, inventory, false);
        }

        public Snapshot(List<OrderRow> orders, List<VehicleRow> vehicles, List<PaymentRow> payments, ExtraTotals extras,
            List<StatusTotal> inventory, boolean containsDemo) {
            this.orders = orders;
            this.vehicles = vehicles;
            this.payments = payments;
            this.extras = extras;
            this.inventory = inventory;
            this.containsDemo = containsDemo;
        }

        public List<OrderRow> getOrders() { return orders; }
        public void setOrders(List<OrderRow> orders) { this.orders = orders; }
        public List<VehicleRow> getVehicles() { return vehicles; }
        public void setVehicles(List<VehicleRow> vehicles) { this.vehicles = vehicles; }
        public List<PaymentRow> getPayments() { return payments; }
        public void setPayments(List<PaymentRow> payments) { this.payments = payments; }
        public ExtraTotals getExtras() { return extras; }
        public void setExtras(ExtraTotals extras) { this.extras = extras; }
        public List<StatusTotal> getInventory() { return inventory; }
        public void setInventory(List<StatusTotal> inventory) { this.inventory = inventory; }
        public boolean isContainsDemo() { return containsDemo; }
        public void setContainsDemo(boolean containsDemo) { this.containsDemo = containsDemo; }
    }

    public static class OrderRow {
        private int orderId;
        private LocalDateTime orderDate;
        private Integer employeeId;
        private String employeeName;
        private String customerName;
        private String status;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private String contractNumber;
        private String invoiceNumber;
        private String deliveryStatus;
        private BigDecimal discountAmount;
        private BigDecimal taxAmount;
        private boolean eligible;

        public OrderRow() {
        }

        public OrderRow(int orderId, LocalDateTime orderDate, Integer employeeId, String employeeName,
            String customerName, String status, BigDecimal totalAmount, BigDecimal paidAmount, String contractNumber,
            String invoiceNumber, String deliveryStatus, BigDecimal discountAmount, BigDecimal taxAmount,
            boolean eligible) {
            this.orderId = orderId;
            this.orderDate = orderDate;
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.customerName = customerName;
            this.status = status;
            this.totalAmount = totalAmount;
            this.paidAmount = paidAmount;
            this.contractNumber = contractNumber;
            this.invoiceNumber = invoiceNumber;
            this.deliveryStatus = deliveryStatus;
            this.discountAmount = discountAmount;
            this.taxAmount = taxAmount;
            this.eligible = eligible;
        }

        public int getOrderId() { return orderId; }
        public void setOrderId(int orderId) { this.orderId = orderId; }
        public LocalDateTime getOrderDate() { return orderDate; }
        public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public BigDecimal getPaidAmount() { return paidAmount; }
        public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
        public String getContractNumber() { return contractNumber; }
        public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }
        public String getInvoiceNumber() { return invoiceNumber; }
        public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
        public String getDeliveryStatus() { return deliveryStatus; }
        public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
        public BigDecimal getTaxAmount() { return taxAmount; }
        public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
        public boolean isEligible() { return eligible; }
        public void setEligible(boolean eligible) { this.eligible = eligible; }
    }

    public static class VehicleRow {
        private int orderId;
        private SaleVehicle vehicle;

        public VehicleRow() {
        }

        public VehicleRow(int orderId, SaleVehicle vehicle) {
            this.orderId = orderId;
            this.vehicle = vehicle;
        }

        public int getOrderId() { return orderId; }
        public void setOrderId(int orderId) { this.orderId = orderId; }
        public SaleVehicle getVehicle() { return vehicle; }
        public void setVehicle(SaleVehicle vehicle) { this.vehicle = vehicle; }
    }

    public static class PaymentRow {
        private LocalDateTime paymentDate;
        private String status;
        private BigDecimal amount;
        private boolean eligible;

        public PaymentRow() {
        }

        public PaymentRow(LocalDateTime paymentDate, String status, BigDecimal amount, boolean eligible) {
            this.paymentDate = paymentDate;
            this.status = status;
            this.amount = amount;
            this.eligible = eligible;
        }

        public LocalDateTime getPaymentDate() { return paymentDate; }
        public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public boolean isEligible() { return eligible; }
        public void setEligible(boolean eligible) { this.eligible = eligible; }
    }

    public static class ExtraTotals {
        private BigDecimal installmentReceipts;
        private long installmentContracts;
        private BigDecimal overdueAmount;

        public ExtraTotals() {
        }

        public ExtraTotals(BigDecimal installmentReceipts, long installmentContracts, BigDecimal overdueAmount) {
            this.installmentReceipts = installmentReceipts;
            this.installmentContracts = installmentContracts;
            this.overdueAmount = overdueAmount;
        }

        public BigDecimal getInstallmentReceipts() { return installmentReceipts; }
        public void setInstallmentReceipts(BigDecimal installmentReceipts) { this.installmentReceipts = installmentReceipts; }
        public long getInstallmentContracts() { return installmentContracts; }
        public void setInstallmentContracts(long installmentContracts) { this.installmentContracts = installmentContracts; }
        public BigDecimal getOverdueAmount() { return overdueAmount; }
        public void setOverdueAmount(BigDecimal overdueAmount) { this.overdueAmount = overdueAmount; }
    }
}
