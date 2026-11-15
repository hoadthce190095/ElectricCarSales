package com.electriccarsales.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/** Finds feature tables that may be absent from older database schemas. */
final class SchemaTables {
    private SchemaTables() {
    }

    static boolean exists(Connection connection, String tableName) throws SQLException {
        if (connection == null) {
            return false;
        }
        String schema = connection.getSchema();
        String sql = """
            SELECT 1
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_TYPE = 'BASE TABLE' AND UPPER(TABLE_NAME) = ?
              AND UPPER(TABLE_SCHEMA) IN (?, 'DBO')
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName.toUpperCase(Locale.ROOT));
            statement.setString(2, schema == null ? null : schema.toUpperCase(Locale.ROOT));
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }
}
