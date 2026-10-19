package com.electriccarsales.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Resolves the finance-side customer record for an authenticated user. */
public class FinanceCustomerDAO extends FinanceDAOBase {
    public FinanceCustomerDAO() {
        super();
    }

    public Integer findCustomerIdByUserId(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be greater than zero");
        }
        return withConnection(connection -> findCustomerId(connection, userId));
    }

    private static Integer findCustomerId(Connection connection, int userId) throws SQLException {
        String sql = "SELECT CustomerID FROM Customers WHERE UserID = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt("CustomerID") : null;
            }
        }
    }
}
