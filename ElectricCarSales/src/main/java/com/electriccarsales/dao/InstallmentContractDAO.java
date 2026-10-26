package com.electriccarsales.dao;

import com.electriccarsales.model.InstallmentContract;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InstallmentContractDAO extends FinanceDAOBase {
    private static final String COLUMNS = "ic.InstallmentContractID, ic.OrderID, ic.CompanyID, ic.PlanID, "
            + "ic.VehiclePrice, ic.DownPayment, ic.LoanAmount, ic.LoanTermMonths, ic.InterestRate, "
            + "ic.TotalInterest, ic.TotalInstallmentAmount, ic.PeriodicPayment, ic.ContractDate, ic.Status";
    private static final String DETAIL_FROM = " FROM InstallmentContracts ic "
            + "JOIN FinancialCompanies fc ON fc.CompanyID = ic.CompanyID "
            + "JOIN InstallmentPlans ip ON ip.PlanID = ic.PlanID "
            + "JOIN Orders o ON o.OrderID = ic.OrderID "
            + "JOIN Customers cu ON cu.CustomerID = o.CustomerID ";

    public InstallmentContractDAO() { super(); }
    public InstallmentContractDAO(Connection transactionConnection) { super(transactionConnection); }

    public InstallmentContract findById(int id) {
        return withConnection(connection -> one(connection, "SELECT " + COLUMNS
                + " FROM InstallmentContracts ic WHERE ic.InstallmentContractID = ?", id));
    }

    public InstallmentContract findByOrderId(int orderId) {
        return withConnection(connection -> one(connection, "SELECT " + COLUMNS
                + " FROM InstallmentContracts ic WHERE ic.OrderID = ?", orderId));
    }

    public boolean existsByOrderId(int orderId) { return findByOrderId(orderId) != null; }

    public boolean orderBelongsToCustomer(int orderId, int customerId) {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM Orders WHERE OrderID = ? AND CustomerID = ?")) {
                statement.setInt(1, orderId);
                statement.setInt(2, customerId);
                try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
            }
        });
    }

    public boolean contractBelongsToCustomer(int contractId, int customerId) {
        return withConnection(connection -> {
            String sql = "SELECT 1 FROM InstallmentContracts ic JOIN Orders o ON o.OrderID = ic.OrderID "
                    + "WHERE ic.InstallmentContractID = ? AND o.CustomerID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, contractId);
                statement.setInt(2, customerId);
                try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
            }
        });
    }

    public List<InstallmentContract> findAll(String status) {
        return withConnection(connection -> {
            String sql = "SELECT " + COLUMNS + " FROM InstallmentContracts ic"
                    + (status == null || status.isBlank() ? "" : " WHERE ic.Status = ?")
                    + " ORDER BY ic.ContractDate DESC, ic.InstallmentContractID DESC";
            return list(connection, sql, status == null || status.isBlank() ? new Object[0] : new Object[]{status});
        });
    }

    public InstallmentContract findDetailsById(int id) {
        return withConnection(connection -> {
            String sql = "SELECT " + COLUMNS + ", fc.CompanyName AS CompanyName, ip.PlanName AS PlanName, "
                    + "cu.CustomerName AS CustomerName" + DETAIL_FROM + "WHERE ic.InstallmentContractID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                try (ResultSet rs = statement.executeQuery()) { return rs.next() ? mapDetails(rs) : null; }
            }
        });
    }

    public List<InstallmentContract> findDetailsByStatus(String status) {
        return withConnection(connection -> {
            String sql = "SELECT " + COLUMNS + ", fc.CompanyName AS CompanyName, ip.PlanName AS PlanName, "
                    + "cu.CustomerName AS CustomerName" + DETAIL_FROM
                    + (status == null || status.isBlank() ? "" : "WHERE ic.Status = ? ")
                    + "ORDER BY ic.ContractDate DESC, ic.InstallmentContractID DESC";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                if (status != null && !status.isBlank()) statement.setString(1, status);
                try (ResultSet rs = statement.executeQuery()) {
                    List<InstallmentContract> result = new ArrayList<>();
                    while (rs.next()) result.add(mapDetails(rs));
                    return result;
                }
            }
        });
    }

    public int insert(InstallmentContract contract) {
        return withConnection(connection -> {
            String sql = "INSERT INTO InstallmentContracts (OrderID, CompanyID, PlanID, VehiclePrice, DownPayment, "
                    + "LoanAmount, LoanTermMonths, InterestRate, TotalInterest, TotalInstallmentAmount, "
                    + "PeriodicPayment, ContractDate, Status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                    + "COALESCE(?, SYSDATETIME()), ?)";
            try (PreparedStatement statement = FinanceJDBC.insertStatement(connection, sql)) {
                bind(statement, contract);
                statement.executeUpdate();
                int id = FinanceJDBC.generatedId(statement);
                contract.setInstallmentContractID(id);
                return id;
            }
        });
    }

    public boolean update(InstallmentContract contract) {
        return withConnection(connection -> {
            String sql = "UPDATE InstallmentContracts SET CompanyID = ?, PlanID = ?, VehiclePrice = ?, "
                    + "DownPayment = ?, LoanAmount = ?, LoanTermMonths = ?, InterestRate = ?, TotalInterest = ?, "
                    + "TotalInstallmentAmount = ?, PeriodicPayment = ?, Status = ? WHERE InstallmentContractID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, contract.getCompanyID()); statement.setInt(2, contract.getPlanID());
                statement.setBigDecimal(3, contract.getVehiclePrice()); statement.setBigDecimal(4, contract.getDownPayment());
                statement.setBigDecimal(5, contract.getLoanAmount()); statement.setInt(6, contract.getLoanTermMonths());
                statement.setBigDecimal(7, contract.getInterestRate()); statement.setBigDecimal(8, contract.getTotalInterest());
                statement.setBigDecimal(9, contract.getTotalInstallmentAmount());
                statement.setBigDecimal(10, contract.getPeriodicPayment()); statement.setString(11, contract.getStatus());
                statement.setInt(12, contract.getInstallmentContractID());
                return statement.executeUpdate() == 1;
            }
        });
    }

    private static void bind(PreparedStatement s, InstallmentContract c) throws SQLException {
        s.setInt(1, c.getOrderID()); s.setInt(2, c.getCompanyID()); s.setInt(3, c.getPlanID());
        s.setBigDecimal(4, c.getVehiclePrice()); s.setBigDecimal(5, c.getDownPayment());
        s.setBigDecimal(6, c.getLoanAmount()); s.setInt(7, c.getLoanTermMonths());
        s.setBigDecimal(8, c.getInterestRate()); s.setBigDecimal(9, c.getTotalInterest());
        s.setBigDecimal(10, c.getTotalInstallmentAmount()); s.setBigDecimal(11, c.getPeriodicPayment());
        s.setTimestamp(12, c.getContractDate()); s.setString(13, c.getStatus());
    }

    private static InstallmentContract one(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    private static List<InstallmentContract> list(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            try (ResultSet rs = statement.executeQuery()) {
                List<InstallmentContract> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        }
    }

    private static InstallmentContract map(ResultSet rs) throws SQLException {
        return new InstallmentContract(rs.getInt("InstallmentContractID"), rs.getInt("OrderID"),
                rs.getInt("CompanyID"), rs.getInt("PlanID"), rs.getBigDecimal("VehiclePrice"),
                rs.getBigDecimal("DownPayment"), rs.getBigDecimal("LoanAmount"), rs.getInt("LoanTermMonths"),
                rs.getBigDecimal("InterestRate"), rs.getBigDecimal("TotalInterest"),
                rs.getBigDecimal("TotalInstallmentAmount"), rs.getBigDecimal("PeriodicPayment"),
                rs.getTimestamp("ContractDate"), rs.getString("Status"));
    }

    private static InstallmentContract mapDetails(ResultSet rs) throws SQLException {
        InstallmentContract contract = map(rs);
        contract.setCompanyName(rs.getString("CompanyName"));
        contract.setPlanName(rs.getString("PlanName"));
        contract.setCustomerName(rs.getString("CustomerName"));
        return contract;
    }
}
