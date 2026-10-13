package com.electriccarsales.dao;

import com.electriccarsales.model.InstallmentPayment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Data access for installment payment history; only verified status codes count as received money. */
public class InstallmentPaymentDAO extends FinanceDAOBase {
    private static final String COLUMNS = "InstallmentPaymentID, ScheduleID, PaymentAmount, PaymentDate, "
            + "PaymentMethodID, PaymentStatusID, ReferenceCode, Note";

    public InstallmentPaymentDAO() { super(); }
    public InstallmentPaymentDAO(Connection transactionConnection) { super(transactionConnection); }

    public InstallmentPayment findById(int paymentId) {
        return withConnection(connection -> one(connection, "SELECT " + COLUMNS
                + " FROM InstallmentPayments WHERE InstallmentPaymentID = ?", paymentId));
    }

    public List<InstallmentPayment> findByScheduleId(int scheduleId) {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM InstallmentPayments WHERE ScheduleID = ? ORDER BY PaymentDate, InstallmentPaymentID",
                scheduleId));
    }

    public List<InstallmentPayment> findByContractId(int contractId) {
        return withConnection(connection -> list(connection, "SELECT ip." + COLUMNS.replace(", ", ", ip.")
                + " FROM InstallmentPayments ip JOIN InstallmentSchedules s ON s.ScheduleID = ip.ScheduleID "
                + "WHERE s.InstallmentContractID = ? ORDER BY ip.PaymentDate, ip.InstallmentPaymentID", contractId));
    }

    public List<InstallmentPayment> findByStatusCode(String statusCode) {
        return withConnection(connection -> list(connection, "SELECT ip." + COLUMNS.replace(", ", ", ip.")
                + " FROM InstallmentPayments ip JOIN PaymentStatuses ps ON ps.PaymentStatusID = ip.PaymentStatusID "
                + "WHERE ps.StatusID = ? ORDER BY ip.PaymentDate DESC, ip.InstallmentPaymentID DESC", statusCode));
    }

    public List<InstallmentPayment> findByReferenceCode(String referenceCode) {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM InstallmentPayments WHERE ReferenceCode = ? "
                + "ORDER BY PaymentDate DESC, InstallmentPaymentID DESC", referenceCode));
    }

    public int insert(InstallmentPayment payment) {
        return withConnection(connection -> {
            String sql = "INSERT INTO InstallmentPayments (ScheduleID, PaymentAmount, PaymentDate, PaymentMethodID, "
                    + "PaymentStatusID, ReferenceCode, Note) VALUES (?, ?, COALESCE(?, SYSDATETIME()), ?, ?, ?, ?)";
            try (PreparedStatement statement = FinanceJDBC.insertStatement(connection, sql)) {
                statement.setInt(1, payment.getScheduleID());
                statement.setBigDecimal(2, payment.getPaymentAmount());
                statement.setTimestamp(3, payment.getPaymentDate());
                statement.setInt(4, payment.getPaymentMethodID());
                statement.setInt(5, payment.getPaymentStatusID());
                statement.setString(6, payment.getReferenceCode());
                statement.setString(7, payment.getNote());
                statement.executeUpdate();
                int id = FinanceJDBC.generatedId(statement);
                payment.setInstallmentPaymentID(id);
                return id;
            }
        });
    }

    public boolean updateStatus(int paymentId, int statusId, String referenceCode) {
        return withConnection(connection -> {
            String sql = "UPDATE InstallmentPayments SET PaymentStatusID = ?, "
                    + "ReferenceCode = COALESCE(?, ReferenceCode) WHERE InstallmentPaymentID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, statusId);
                statement.setString(2, referenceCode);
                statement.setInt(3, paymentId);
                return statement.executeUpdate() == 1;
            }
        });
    }

    /** Guarded status transition lets the service prevent repeated processing of a pending payment. */
    public boolean updateStatusIfCurrent(int paymentId, int expectedStatusId, int newStatusId,
                                         String referenceCode) {
        return withConnection(connection -> {
            String sql = "UPDATE InstallmentPayments SET PaymentStatusID = ?, "
                    + "ReferenceCode = COALESCE(?, ReferenceCode) "
                    + "WHERE InstallmentPaymentID = ? AND PaymentStatusID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, newStatusId);
                statement.setString(2, referenceCode);
                statement.setInt(3, paymentId);
                statement.setInt(4, expectedStatusId);
                return statement.executeUpdate() == 1;
            }
        });
    }

    public java.math.BigDecimal sumSuccessfulByContractId(int contractId) {
        return withConnection(connection -> {
            String sql = "SELECT COALESCE(SUM(ip.PaymentAmount), 0) FROM InstallmentPayments ip "
                    + "JOIN InstallmentSchedules s ON s.ScheduleID = ip.ScheduleID "
                    + "JOIN PaymentStatuses ps ON ps.PaymentStatusID = ip.PaymentStatusID "
                    + "WHERE s.InstallmentContractID = ? AND ps.StatusID = 'COMPLETED'";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, contractId);
                try (ResultSet rs = statement.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
            }
        });
    }

    private static InstallmentPayment one(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    private static List<InstallmentPayment> list(Connection connection, String sql, Object... values)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            try (ResultSet rs = statement.executeQuery()) {
                List<InstallmentPayment> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        }
    }

    private static InstallmentPayment map(ResultSet rs) throws SQLException {
        return new InstallmentPayment(rs.getInt("InstallmentPaymentID"), rs.getInt("ScheduleID"),
                rs.getBigDecimal("PaymentAmount"), rs.getTimestamp("PaymentDate"),
                rs.getInt("PaymentMethodID"), rs.getInt("PaymentStatusID"),
                rs.getString("ReferenceCode"), rs.getString("Note"));
    }
}
