package com.electriccarsales.dao;

import com.electriccarsales.model.SalesPreviewKind;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/** Reads customer-owned preview data from the existing finance schema. */
public final class SalesPreviewDAO {
    private static final String SQL = """
            SELECT o.OrderID AS "order.id", o.TotalAmount AS "order.totalAmount",
                   o.DepositAmount AS "order.depositAmount",
                   o.TotalAmount-o.DepositAmount AS "order.remainingInitial",
                   o.RemainingAmount AS "payment.currentOutstanding",
                   cu.CustomerName AS "customer.name", cu.Phone AS "customer.phone",
                   cu.Email AS "customer.email", cu.Address AS "customer.address",
                   cu.CustomerID AS "customer.id",
                   ca.CarID AS "car.id", ca.CarCode AS "car.code", ca.VIN AS "car.vin",
                   ca.EngineNumber AS "car.engineNumber", ca.Origin AS "car.origin",
                   ca.Year AS "car.year", cm.ModelName AS "car.model", cv.VersionName AS "car.version",
                   co.ColorName AS "car.color", cl.CarLineName AS "car.line", b.BrandName AS "car.brand",
                   d.SalesPrice AS "order.salesPrice",
                   e.EmployeeName AS "employee.name", e.EmployeeID AS "employee.id", e.Phone AS "employee.phone",
                   i.InvoiceNumber AS "invoice.number", i.InvoiceDate AS "invoice.date",
                   i.TotalAmount AS "invoice.totalAmount", i.TaxAmount AS "invoice.taxAmount",
                   i.SubTotal AS "invoice.subTotal", i.DiscountAmount AS "invoice.discountAmount",
                   ct.ContractNumber AS "contract.number", ct.ContractDate AS "contract.date",
                   ct.ContractAmount AS ContractAmount, ct.Status AS "contract.status",
                   ct.Note AS "contract.terms", p.Note AS "payment.audit",
                   p.PaymentID AS "payment.id", p.PaymentAmount AS "payment.amount",
                   p.PaymentDate AS "payment.depositConfirmedDate", p.ReferenceCode AS "payment.depositReference",
                   pm.MethodName AS "payment.channel", fc.CompanyName AS "finance.partnerName",
                   CASE WHEN om.MethodName='INSTALLMENT' THEN 'INSTALLMENT' ELSE 'FULL_PAYMENT' END AS "payment.mode"
            FROM Orders o
            JOIN Customers cu ON cu.CustomerID=o.CustomerID
            JOIN OrderStatuses os ON os.OrderStatusID=o.OrderStatusID
            JOIN PaymentMethods om ON om.PaymentMethodID=o.PaymentMethodID
            JOIN OrderDetails d ON d.OrderID=o.OrderID
            JOIN Cars ca ON ca.CarID=d.CarID
            JOIN CarModels cm ON cm.ModelID=ca.ModelID
            JOIN CarVersions cv ON cv.VersionID=ca.VersionID
            JOIN CarLines cl ON cl.CarLineID=cm.CarLineID
            JOIN Brands b ON b.BrandID=cl.BrandID
            JOIN Colors co ON co.ColorID=ca.ColorID
            JOIN Payments p ON p.OrderID=o.OrderID
            JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID AND pt.TypeName='DEPOSIT'
            JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID AND ps.StatusID='COMPLETED'
            JOIN PaymentMethods pm ON pm.PaymentMethodID=p.PaymentMethodID
            LEFT JOIN FinancialCompanies fc ON fc.CompanyID=o.CompanyID
            LEFT JOIN Employees e ON e.EmployeeID=o.EmployeeID
            LEFT JOIN Contracts ct ON ct.OrderID=o.OrderID AND ct.CustomerID=o.CustomerID
            LEFT JOIN Invoices i ON i.OrderID=o.OrderID AND i.CustomerID=o.CustomerID
            WHERE o.OrderID=? AND cu.UserID=? AND p.PaymentAmount=o.DepositAmount
                AND (SELECT COUNT(*) FROM Payments p2
                      JOIN PaymentTypes pt2 ON pt2.PaymentTypeID=p2.PaymentTypeID
                      JOIN PaymentStatuses ps2 ON ps2.PaymentStatusID=p2.PaymentStatusID
                      WHERE p2.OrderID=o.OrderID AND pt2.TypeName='DEPOSIT' AND ps2.StatusID='COMPLETED')=1
            """;

    public Map<String, String> find(SalesPreviewKind kind, int orderId, int userId) throws SQLException {
        return find(kind, orderId, userId, false);
    }

    public Map<String, String> find(SalesPreviewKind kind, int orderId, int userId, boolean includeSigned) throws SQLException {
        String condition = switch (kind) {
            case DEPOSIT -> "";
            case CONTRACT -> includeSigned
                    ? " AND ct.Status IN ('PENDING_SIGNATURE','SIGNED') AND os.StatusName IN ('DEPOSITED','PROCESSING','PAID','COMPLETED')"
                    : " AND ct.Status='PENDING_SIGNATURE' AND os.StatusName IN ('DEPOSITED','PROCESSING')";
            case INVOICE -> " AND i.Status='ISSUED' AND ct.Status='SIGNED' AND i.TotalAmount=o.TotalAmount";
        };

        try (var connection = FinanceConnection.open()) {
            connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                Map<String, String> fields;
                try (var query = connection.prepareStatement(SQL + condition)) {
                    query.setInt(1, orderId);
                    query.setInt(2, userId);
                    try (var rows = query.executeQuery()) {
                        fields = read(rows, kind);
                    }
                }
                if (!fields.isEmpty() && kind == SalesPreviewKind.INVOICE) {
                    try {
                        PaymentDAO.requireExactSettlement(connection, orderId);
                    } catch (IllegalStateException invalidSettlement) {
                        fields = Map.of();
                    }
                }
                if (!fields.isEmpty() && includeSigned) {
                    try (var query = connection.prepareStatement("""
                            SELECT p.ReferenceCode,p.PaymentDate,pm.MethodName FROM Payments p
                            JOIN PaymentTypes pt ON pt.PaymentTypeID=p.PaymentTypeID
                            JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID
                            JOIN PaymentMethods pm ON pm.PaymentMethodID=p.PaymentMethodID
                            WHERE p.OrderID=? AND pt.TypeName='REMAINING' AND ps.StatusID='COMPLETED'
                            """)) {
                        query.setInt(1, orderId);
                        try (var rows = query.executeQuery()) {
                            if (rows.next()) {
                                fields.put("payment.remainingReference", rows.getString(1));
                                fields.put("payment.remainingDate", rows.getString(2));
                                fields.put("payment.remainingChannel", rows.getString(3));
                            }
                        }
                    }
                    try (var query = connection.prepareStatement("""
                            SELECT v.OTPVerificationID,v.VerifiedDate FROM OTPVerifications v
                            JOIN Contracts c ON c.OrderID=?
                            JOIN Customers cu ON cu.CustomerID=c.CustomerID AND cu.UserID=v.UserID
                            WHERE v.Purpose=CONCAT('SIGN_CONTRACT_ID:',c.ContractID)
                              AND v.Status='VERIFIED' AND v.VerifiedDate IS NOT NULL
                            ORDER BY v.OTPVerificationID DESC
                            """)) {
                        query.setInt(1, orderId);
                        try (var rows = query.executeQuery()) {
                            if (rows.next()) {
                                fields.put("signature.verificationReference", "OTPVerification #"+rows.getInt(1));
                                fields.put("signature.signedAt", rows.getString(2));
                            }
                        }
                    }
                }
                connection.commit();
                return fields;
            } catch (SQLException | RuntimeException failure) {
                try { connection.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
                throw failure;
            }
        }
    }

    private Map<String, String> read(ResultSet rows, SalesPreviewKind kind) throws SQLException {
        if (!rows.next()) return Map.of();
        BigDecimal total = rows.getBigDecimal("order.totalAmount");
        BigDecimal deposit = total.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
        if (deposit.signum() <= 0 || deposit.compareTo(rows.getBigDecimal("order.depositAmount")) != 0) {
            return Map.of();
        }
        BigDecimal contractAmt = rows.getBigDecimal("ContractAmount");
        if (kind != SalesPreviewKind.DEPOSIT && (contractAmt == null || total.compareTo(contractAmt) != 0)) {
            return Map.of();
        }

        var fields = new HashMap<String, String>();
        var metadata = rows.getMetaData();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            String key = metadata.getColumnLabel(i);
            String value = rows.getString(i);
            if (key.contains(".") && value != null) fields.put(key, value);
        }
        if (rows.next()) return Map.of();
        return fields;
    }
}
