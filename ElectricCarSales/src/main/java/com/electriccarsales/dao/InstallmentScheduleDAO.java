package com.electriccarsales.dao;

import com.electriccarsales.model.InstallmentSchedule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Data access for installment schedule rows. Payment reconciliation belongs to the service layer. */
public class InstallmentScheduleDAO extends FinanceDAOBase {
    private static final String COLUMNS = "ScheduleID, InstallmentContractID, PeriodNumber, DueDate, "
            + "PrincipalAmount, InterestAmount, PaymentAmount, PaidAmount, Status";

    public InstallmentScheduleDAO() { super(); }
    public InstallmentScheduleDAO(Connection transactionConnection) { super(transactionConnection); }

    public List<InstallmentSchedule> findByContractId(int contractId) {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM InstallmentSchedules WHERE InstallmentContractID = ? ORDER BY PeriodNumber, ScheduleID",
                contractId));
    }

    public InstallmentSchedule findById(int scheduleId) {
        return withConnection(connection -> one(connection, "SELECT " + COLUMNS
                + " FROM InstallmentSchedules WHERE ScheduleID = ?", scheduleId));
    }

    public InstallmentSchedule findByContractAndPeriod(int contractId, int periodNumber) {
        return withConnection(connection -> {
            String sql = "SELECT " + COLUMNS + " FROM InstallmentSchedules "
                    + "WHERE InstallmentContractID = ? AND PeriodNumber = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, contractId);
                statement.setInt(2, periodNumber);
                try (ResultSet rs = statement.executeQuery()) { return rs.next() ? map(rs) : null; }
            }
        });
    }

    /** Upcoming means due on/after the supplied date with an unpaid balance. */
    public List<InstallmentSchedule> findUpcoming(java.sql.Date fromDate) {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM InstallmentSchedules WHERE DueDate >= ? AND PaidAmount < PaymentAmount "
                + "ORDER BY DueDate, InstallmentContractID, PeriodNumber", fromDate));
    }

    /** Overdue means due before the supplied date with an unpaid balance. */
    public List<InstallmentSchedule> findOverdue(java.sql.Date asOfDate) {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM InstallmentSchedules WHERE DueDate < ? AND PaidAmount < PaymentAmount "
                + "ORDER BY DueDate, InstallmentContractID, PeriodNumber", asOfDate));
    }

    /** Persists schedules as one transaction when this DAO owns the connection. */
    public List<Integer> insertAll(List<InstallmentSchedule> schedules) {
        if (schedules == null) throw new IllegalArgumentException("schedules must not be null");
        return withConnection(connection -> {
            boolean localTransaction = !hasExternalConnection();
            if (localTransaction) connection.setAutoCommit(false);
            try {
                List<Integer> ids = new ArrayList<>(schedules.size());
                String sql = "INSERT INTO InstallmentSchedules (InstallmentContractID, PeriodNumber, DueDate, "
                        + "PrincipalAmount, InterestAmount, PaymentAmount, PaidAmount, Status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql,
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    for (InstallmentSchedule schedule : schedules) {
                        bind(statement, schedule);
                        statement.executeUpdate();
                        int id = FinanceJDBC.generatedId(statement);
                        schedule.setScheduleID(id);
                        ids.add(id);
                    }
                }
                if (localTransaction) connection.commit();
                return ids;
            } catch (SQLException | RuntimeException e) {
                if (localTransaction) {
                    try { connection.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
                }
                throw e;
            }
        });
    }

    public int insert(InstallmentSchedule schedule) {
        List<Integer> ids = insertAll(List.of(schedule));
        return ids.get(0);
    }

    /** Does not calculate or change PaidAmount; service payment confirmation owns that workflow. */
    public boolean update(InstallmentSchedule schedule) {
        return withConnection(connection -> {
            String sql = "UPDATE InstallmentSchedules SET DueDate = ?, PrincipalAmount = ?, InterestAmount = ?, "
                    + "PaymentAmount = ?, PaidAmount = ?, Status = ? WHERE ScheduleID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setDate(1, schedule.getDueDate());
                statement.setBigDecimal(2, schedule.getPrincipalAmount());
                statement.setBigDecimal(3, schedule.getInterestAmount());
                statement.setBigDecimal(4, schedule.getPaymentAmount());
                statement.setBigDecimal(5, schedule.getPaidAmount());
                statement.setString(6, schedule.getStatus());
                statement.setInt(7, schedule.getScheduleID());
                return statement.executeUpdate() == 1;
            }
        });
    }

    private static void bind(PreparedStatement statement, InstallmentSchedule schedule) throws SQLException {
        statement.setInt(1, schedule.getInstallmentContractID());
        statement.setInt(2, schedule.getPeriodNumber());
        statement.setDate(3, schedule.getDueDate());
        statement.setBigDecimal(4, schedule.getPrincipalAmount());
        statement.setBigDecimal(5, schedule.getInterestAmount());
        statement.setBigDecimal(6, schedule.getPaymentAmount());
        statement.setBigDecimal(7, schedule.getPaidAmount());
        statement.setString(8, schedule.getStatus());
    }

    private static InstallmentSchedule one(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    private static List<InstallmentSchedule> list(Connection connection, String sql, Object... values)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            try (ResultSet rs = statement.executeQuery()) {
                List<InstallmentSchedule> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        }
    }

    private static InstallmentSchedule map(ResultSet rs) throws SQLException {
        return new InstallmentSchedule(rs.getInt("ScheduleID"), rs.getInt("InstallmentContractID"),
                rs.getInt("PeriodNumber"), rs.getDate("DueDate"), rs.getBigDecimal("PrincipalAmount"),
                rs.getBigDecimal("InterestAmount"), rs.getBigDecimal("PaymentAmount"),
                rs.getBigDecimal("PaidAmount"), rs.getString("Status"));
    }
}
