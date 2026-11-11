package com.electriccarsales.dao;

import com.electriccarsales.model.FinancialCompany;
import com.electriccarsales.model.InstallmentPlan;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InstallmentPlanDAO extends FinanceDAOBase {
    private static final String COLUMNS = "ip.PlanID, ip.CompanyID, ip.PlanName, ip.LoanTermMonths, "
            + "ip.InterestRate, ip.Description, fc.CompanyName AS CompanyName";
    private static final String PLAN_FROM = " FROM InstallmentPlans ip JOIN FinancialCompanies fc ON fc.CompanyID = ip.CompanyID ";

    public InstallmentPlanDAO() { super(); }
    public InstallmentPlanDAO(Connection transactionConnection) { super(transactionConnection); }

    private static boolean hasTable(Connection connection) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT CASE WHEN OBJECT_ID(N'dbo.InstallmentPlans', N'U') IS NOT NULL THEN 1 ELSE 0 END");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) == 1;
        } catch (SQLException ex) {
            return false;
        }
    }

    /**
     * Returns RDS BR9 & BR10 reference installment plans (12/24/36/48/60 months) when InstallmentPlans
     * table is not present in ElectricCarSalesDB.sql.
     */
    private static List<InstallmentPlan> referencePlans(Connection connection) {
        List<FinancialCompany> companies = new FinancialCompanyDAO(connection).findAll();
        List<InstallmentPlan> plans = new ArrayList<>();
        int id = 1;
        Object[][] tiers = {
            {"shinhan", "Shinhan Finance", 12, "7.30"},
            {"shinhan", "Shinhan Finance", 24, "8.20"},
            {"shinhan", "Shinhan Finance", 36, "9.10"},
            {"shinhan", "Shinhan Finance", 48, "10.40"},
            {"shinhan", "Shinhan Finance", 60, "11.80"},
            {"techcombank", "Techcombank", 24, "5.00"},
            {"techcombank", "Techcombank", 36, "5.70"},
            {"techcombank", "Techcombank", 48, "6.30"},
            {"techcombank", "Techcombank", 60, "7.00"},
            {"mb", "MB Bank", 12, "5.00"},
            {"mb", "MB Bank", 24, "5.80"},
            {"mb", "MB Bank", 36, "6.50"},
            {"mb", "MB Bank", 48, "7.40"},
            {"mb", "MB Bank", 60, "8.50"},
            {"vpbank", "VPBank", 36, "6.00"},
            {"vpbank", "VPBank", 48, "7.80"},
            {"vpbank", "VPBank", 60, "9.50"}
        };
        for (Object[] tier : tiers) {
            String key = (String) tier[0];
            String defaultCompanyName = (String) tier[1];
            int months = (Integer) tier[2];
            BigDecimal rate = new BigDecimal((String) tier[3]);
            FinancialCompany matched = companies.stream()
                    .filter(c -> c.getCompanyName() != null && c.getCompanyName().toLowerCase(Locale.ROOT).contains(key))
                    .findFirst().orElse(null);
            int companyId = matched != null ? matched.getCompanyID() : id;
            String companyName = matched != null ? matched.getCompanyName() : defaultCompanyName;
            InstallmentPlan p = new InstallmentPlan(
                    id++,
                    companyName + " - " + months + " tháng",
                    months,
                    rate,
                    "Lãi suất tham khảo " + rate + "%/năm theo RDS BR10 (Cọc 30%, đối tác giải ngân 70%).");
            p.setCompanyID(companyId);
            p.setCompanyName(companyName);
            plans.add(p);
        }
        return plans;
    }

    public List<InstallmentPlan> findAll() {
        return withConnection(connection -> {
            if (!hasTable(connection)) {
                return referencePlans(connection);
            }
            return list(connection, "SELECT " + COLUMNS
                    + PLAN_FROM + "ORDER BY ip.CompanyID, ip.LoanTermMonths, ip.PlanID");
        });
    }

    public InstallmentPlan findById(int planId) {
        return withConnection(connection -> {
            if (!hasTable(connection)) {
                return referencePlans(connection).stream()
                        .filter(p -> p.getPlanID() == planId)
                        .findFirst().orElse(null);
            }
            return one(connection, "SELECT " + COLUMNS + PLAN_FROM + "WHERE ip.PlanID = ?", planId);
        });
    }

    public InstallmentPlan findByName(String planName) {
        return withConnection(connection -> {
            if (!hasTable(connection)) {
                return referencePlans(connection).stream()
                        .filter(p -> p.getPlanName().equalsIgnoreCase(planName))
                        .findFirst().orElse(null);
            }
            try (PreparedStatement statement = connection.prepareStatement("SELECT " + COLUMNS
                    + PLAN_FROM + "WHERE ip.PlanName = ?")) {
                statement.setString(1, planName);
                try (ResultSet rs = statement.executeQuery()) { return rs.next() ? map(rs) : null; }
            }
        });
    }

    public List<InstallmentPlan> findByLoanTermMonths(int months) {
        return withConnection(connection -> {
            if (!hasTable(connection)) {
                return referencePlans(connection).stream()
                        .filter(p -> p.getLoanTermMonths() == months)
                        .toList();
            }
            return list(connection, "SELECT " + COLUMNS
                    + PLAN_FROM + "WHERE ip.LoanTermMonths = ? ORDER BY ip.PlanName", months);
        });
    }

    public int insert(InstallmentPlan plan) {
        return withConnection(connection -> {
            if (!hasTable(connection)) {
                throw new FinanceDAOException("InstallmentPlans table is not present in ElectricCarSalesDB.sql.");
            }
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
            if (!hasTable(connection)) {
                return false;
            }
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
