package com.electriccarsales.dao;

import com.electriccarsales.model.InstallmentPlan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InstallmentPlanDAO extends FinanceDAOBase {
    private static final String COLUMNS = "ip.PlanID, ip.CompanyID, ip.PlanName, ip.LoanTermMonths, "
            + "ip.InterestRate, ip.Description, fc.CompanyName AS CompanyName";
    private static final String PLAN_FROM = " FROM InstallmentPlans ip JOIN FinancialCompanies fc ON fc.CompanyID = ip.CompanyID ";

    public InstallmentPlanDAO() { super(); }
    public InstallmentPlanDAO(Connection transactionConnection) { super(transactionConnection); }

    public List<InstallmentPlan> findAll() {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + PLAN_FROM + "ORDER BY ip.CompanyID, ip.LoanTermMonths, ip.PlanID"));
    }

    public InstallmentPlan findById(int planId) {
        return withConnection(connection -> one(connection, "SELECT " + COLUMNS
                + PLAN_FROM + "WHERE ip.PlanID = ?", planId));
    }

    public InstallmentPlan findByName(String planName) {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT " + COLUMNS
                    + PLAN_FROM + "WHERE ip.PlanName = ?")) {
                statement.setString(1, planName);
                try (ResultSet rs = statement.executeQuery()) { return rs.next() ? map(rs) : null; }
            }
        });
    }

    public List<InstallmentPlan> findByLoanTermMonths(int months) {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + PLAN_FROM + "WHERE ip.LoanTermMonths = ? ORDER BY ip.PlanName", months));
    }

    public int insert(InstallmentPlan plan) {
        return withConnection(connection -> {
            String sql = "INSERT INTO InstallmentPlans (CompanyID, PlanName, LoanTermMonths, InterestRate, Description) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement statement = FinanceJDBC.insertStatement(connection, sql)) {
                statement.setInt(1, plan.getCompanyID());
                statement.setString(2, plan.getPlanName());
                statement.setInt(3, plan.getLoanTermMonths());
                statement.setBigDecimal(4, plan.getInterestRate());
                statement.setString(5, plan.getDescription());
                statement.executeUpdate();
                int id = FinanceJDBC.generatedId(statement);
                plan.setPlanID(id);
                return id;
            }
        });
    }

    public boolean update(InstallmentPlan plan) {
        return withConnection(connection -> {
            String sql = "UPDATE InstallmentPlans SET CompanyID = ?, PlanName = ?, LoanTermMonths = ?, InterestRate = ?, Description = ? WHERE PlanID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, plan.getCompanyID());
                statement.setString(2, plan.getPlanName());
                statement.setInt(3, plan.getLoanTermMonths());
                statement.setBigDecimal(4, plan.getInterestRate());
                statement.setString(5, plan.getDescription());
                statement.setInt(6, plan.getPlanID());
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
        InstallmentPlan plan = new InstallmentPlan(rs.getInt("PlanID"), rs.getString("PlanName"),
                rs.getInt("LoanTermMonths"), rs.getBigDecimal("InterestRate"), rs.getString("Description"));
        plan.setCompanyID(rs.getInt("CompanyID"));
        plan.setCompanyName(rs.getString("CompanyName"));
        return plan;
    }
}
