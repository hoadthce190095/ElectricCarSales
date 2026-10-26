package com.electriccarsales.dao;

import com.electriccarsales.model.FinancialCompany;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FinancialCompanyDAO extends FinanceDAOBase {
    private static final String COLUMNS = "CompanyID, CompanyName, Phone, Email, Address, Description, "
            + "PaymentDueDay, PaymentCutoffTime, LateFeeDailyPercent, EarlySettlementFeePercent";

    public FinancialCompanyDAO() { super(); }
    public FinancialCompanyDAO(Connection transactionConnection) { super(transactionConnection); }

    public List<FinancialCompany> findAll() {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM FinancialCompanies ORDER BY CompanyName"));
    }

    public FinancialCompany findById(int companyId) {
        return withConnection(connection -> one(connection, "SELECT " + COLUMNS
                + " FROM FinancialCompanies WHERE CompanyID = ?", companyId));
    }

    public FinancialCompany findByName(String companyName) {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT " + COLUMNS
                    + " FROM FinancialCompanies WHERE CompanyName = ?")) {
                statement.setString(1, companyName);
                try (ResultSet rs = statement.executeQuery()) { return rs.next() ? map(rs) : null; }
            }
        });
    }

    public List<FinancialCompany> search(String query) {
        return withConnection(connection -> list(connection, "SELECT " + COLUMNS
                + " FROM FinancialCompanies WHERE CompanyName LIKE ? OR Phone LIKE ? OR Email LIKE ? "
                + "ORDER BY CompanyName", like(query), like(query), like(query)));
    }

    public int insert(FinancialCompany company) {
        return withConnection(connection -> {
            String sql = "INSERT INTO FinancialCompanies (CompanyName, Phone, Email, Address, Description, "
                    + "PaymentDueDay, PaymentCutoffTime, LateFeeDailyPercent, EarlySettlementFeePercent) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = FinanceJDBC.insertStatement(connection, sql)) {
                bind(statement, company);
                statement.executeUpdate();
                int id = FinanceJDBC.generatedId(statement);
                company.setCompanyID(id);
                return id;
            }
        });
    }

    public boolean update(FinancialCompany company) {
        return withConnection(connection -> {
            String sql = "UPDATE FinancialCompanies SET CompanyName = ?, Phone = ?, Email = ?, Address = ?, Description = ?, "
                    + "PaymentDueDay = ?, PaymentCutoffTime = ?, LateFeeDailyPercent = ?, "
                    + "EarlySettlementFeePercent = ? WHERE CompanyID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, company);
                statement.setInt(10, company.getCompanyID());
                return statement.executeUpdate() == 1;
            }
        });
    }

    private static String like(String value) { return "%" + (value == null ? "" : value.trim()) + "%"; }

    private static void bind(PreparedStatement statement, FinancialCompany company) throws SQLException {
        statement.setString(1, company.getCompanyName());
        statement.setString(2, company.getPhone());
        statement.setString(3, company.getEmail());
        statement.setString(4, company.getAddress());
        statement.setString(5, company.getDescription());
        if (company.getPaymentDueDay() == null) statement.setNull(6, java.sql.Types.TINYINT);
        else statement.setInt(6, company.getPaymentDueDay());
        if (company.getPaymentCutoffTime() == null) statement.setNull(7, java.sql.Types.TIME);
        else statement.setTime(7, java.sql.Time.valueOf(company.getPaymentCutoffTime()));
        statement.setBigDecimal(8, company.getLateFeeDailyPercent());
        statement.setBigDecimal(9, company.getEarlySettlementFeePercent());
    }

    private static FinancialCompany one(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    private static List<FinancialCompany> list(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            try (ResultSet rs = statement.executeQuery()) {
                List<FinancialCompany> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        }
    }

    private static FinancialCompany map(ResultSet rs) throws SQLException {
        FinancialCompany company = new FinancialCompany(rs.getInt("CompanyID"), rs.getString("CompanyName"),
                rs.getString("Phone"), rs.getString("Email"), rs.getString("Address"), rs.getString("Description"));
        int dueDay = rs.getInt("PaymentDueDay");
        company.setPaymentDueDay(rs.wasNull() ? null : dueDay);
        java.sql.Time cutoff = rs.getTime("PaymentCutoffTime");
        company.setPaymentCutoffTime(cutoff == null ? null : cutoff.toLocalTime());
        company.setLateFeeDailyPercent(rs.getBigDecimal("LateFeeDailyPercent"));
        company.setEarlySettlementFeePercent(rs.getBigDecimal("EarlySettlementFeePercent"));
        return company;
    }
}
