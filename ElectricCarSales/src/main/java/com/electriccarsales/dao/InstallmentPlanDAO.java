package com.electriccarsales.dao;

import com.electriccarsales.model.InstallmentPlan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InstallmentPlanDAO extends FinanceDAOBase {
    private static final String COLUMNS = "PlanID, PlanName, LoanTermMonths, InterestRate, Description";

    public InstallmentPlanDAO() { super(); }
    public InstallmentPlanDAO(Connection transactionConnection) { super(transactionConnection); }

    public List<InstallmentPlan> findAll() {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM InstallmentPlans ORDER BY LoanTermMonths, PlanID"));
    }

    public InstallmentPlan findById(int planId) {
        return withConnection(connection -> one(connection, "SELECT " + COLUMNS
                + " FROM InstallmentPlans WHERE PlanID = ?", planId));
    }

    public InstallmentPlan findByName(String planName) {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT " + COLUMNS
                    + " FROM InstallmentPlans WHERE PlanName = ?")) {
                statement.setString(1, planName);
                try (ResultSet rs = statement.executeQuery()) { return rs.next() ? map(rs) : null; }
            }
        });
    }

    public List<InstallmentPlan> findByLoanTermMonths(int months) {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM InstallmentPlans WHERE LoanTermMonths = ? ORDER BY PlanName", months));
    }

    public int insert(InstallmentPlan plan) {
        return withConnection(connection -> {
            String sql = "INSERT INTO InstallmentPlans (PlanName, LoanTermMonths, InterestRate, Description) VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement = FinanceJDBC.insertStatement(connection, sql)) {
                statement.setString(1, plan.getPlanName());
                statement.setInt(2, plan.getLoanTermMonths());
                statement.setBigDecimal(3, plan.getInterestRate());
                statement.setString(4, plan.getDescription());
                statement.executeUpdate();
                int id = FinanceJDBC.generatedId(statement);
                plan.setPlanID(id);
                return id;
            }
        });
    }

    public boolean update(InstallmentPlan plan) {
        return withConnection(connection -> {
            String sql = "UPDATE InstallmentPlans SET PlanName = ?, LoanTermMonths = ?, InterestRate = ?, Description = ? WHERE PlanID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, plan.getPlanName());
                statement.setInt(2, plan.getLoanTermMonths());
                statement.setBigDecimal(3, plan.getInterestRate());
                statement.setString(4, plan.getDescription());
                statement.setInt(5, plan.getPlanID());
                return statement.executeUpdate() == 1;
            }
        });
    }

    private static InstallmentPlan one(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    private static List<InstallmentPlan> list(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            try (ResultSet rs = statement.executeQuery()) {
                List<InstallmentPlan> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        }
    }

    private static InstallmentPlan map(ResultSet rs) throws SQLException {
        return new InstallmentPlan(rs.getInt("PlanID"), rs.getString("PlanName"),
                rs.getInt("LoanTermMonths"), rs.getBigDecimal("InterestRate"), rs.getString("Description"));
    }
}
