package com.electriccarsales.dao;

import com.electriccarsales.model.InstallmentSettlement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Data access for the settlement record supported by the current schema. */
public class InstallmentSettlementDAO extends FinanceDAOBase {
    private static final String COLUMNS = "SettlementID, InstallmentContractID, SettlementDate, "
            + "PrincipalRemaining, AccruedInterest, SettlementFee, SettlementAmount, PaymentMethodID, "
            + "EmployeeID, ReferenceCode, Note";

    public InstallmentSettlementDAO() { super(); }
    public InstallmentSettlementDAO(Connection transactionConnection) { super(transactionConnection); }

    public InstallmentSettlement findById(int settlementId) {
        return withConnection(connection -> one(connection, "SELECT " + COLUMNS
                + " FROM InstallmentSettlements WHERE SettlementID = ?", settlementId));
    }

    public InstallmentSettlement findByContractId(int contractId) {
        return withConnection(connection -> one(connection, "SELECT " + COLUMNS
                + " FROM InstallmentSettlements WHERE InstallmentContractID = ?", contractId));
    }

    public List<InstallmentSettlement> findAll() {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM InstallmentSettlements ORDER BY SettlementDate DESC, SettlementID DESC"));
    }

    public List<InstallmentSettlement> findByEmployeeId(int employeeId) {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM InstallmentSettlements WHERE EmployeeID = ? "
                + "ORDER BY SettlementDate DESC, SettlementID DESC", employeeId));
    }

    public boolean existsByContractId(int contractId) {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM InstallmentSettlements WHERE InstallmentContractID = ?")) {
                statement.setInt(1, contractId);
                try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
            }
        });
    }

    public int insert(InstallmentSettlement settlement) {
        return withConnection(connection -> {
            String sql = "INSERT INTO InstallmentSettlements (InstallmentContractID, SettlementDate, "
                    + "PrincipalRemaining, AccruedInterest, SettlementFee, SettlementAmount, PaymentMethodID, "
                    + "EmployeeID, ReferenceCode, Note) VALUES (?, COALESCE(?, SYSDATETIME()), ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = FinanceJDBC.insertStatement(connection, sql)) {
                statement.setInt(1, settlement.getInstallmentContractID());
                statement.setTimestamp(2, settlement.getSettlementDate());
                statement.setBigDecimal(3, settlement.getPrincipalRemaining());
                statement.setBigDecimal(4, settlement.getAccruedInterest());
                statement.setBigDecimal(5, settlement.getSettlementFee());
                statement.setBigDecimal(6, settlement.getSettlementAmount());
                statement.setInt(7, settlement.getPaymentMethodID());
                statement.setInt(8, settlement.getEmployeeID());
                statement.setString(9, settlement.getReferenceCode());
                statement.setString(10, settlement.getNote());
                statement.executeUpdate();
                int id = FinanceJDBC.generatedId(statement);
                settlement.setSettlementID(id);
                return id;
            }
        });
    }

    /** The schema stores no settlement status; this updates only persisted payment reference information. */
    public boolean updateReferenceAndNote(int settlementId, String referenceCode, String note) {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE InstallmentSettlements SET ReferenceCode = ?, Note = ? WHERE SettlementID = ?")) {
                statement.setString(1, referenceCode);
                statement.setString(2, note);
                statement.setInt(3, settlementId);
                return statement.executeUpdate() == 1;
            }
        });
    }

    private static InstallmentSettlement one(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    private static List<InstallmentSettlement> list(Connection connection, String sql, Object... values)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            try (ResultSet rs = statement.executeQuery()) {
                List<InstallmentSettlement> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        }
    }

    private static InstallmentSettlement map(ResultSet rs) throws SQLException {
        return new InstallmentSettlement(rs.getInt("SettlementID"), rs.getInt("InstallmentContractID"),
                rs.getTimestamp("SettlementDate"), rs.getBigDecimal("PrincipalRemaining"),
                rs.getBigDecimal("AccruedInterest"), rs.getBigDecimal("SettlementFee"),
                rs.getBigDecimal("SettlementAmount"), rs.getInt("PaymentMethodID"),
                rs.getInt("EmployeeID"), rs.getString("ReferenceCode"), rs.getString("Note"));
    }
}
