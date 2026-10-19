package com.electriccarsales.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class FinanceJDBC {
    private FinanceJDBC() { }

    static int generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("Insert completed without returning an identity key.");
            }
            return keys.getInt(1);
        }
    }

    static PreparedStatement insertStatement(java.sql.Connection connection, String sql) throws SQLException {
        return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }
}
