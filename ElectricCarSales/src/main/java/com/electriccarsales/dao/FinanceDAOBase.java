package com.electriccarsales.dao;

import java.sql.Connection;
import java.sql.SQLException;

/** Shared connection handling for finance DAOs. A supplied connection remains service-owned. */
abstract class FinanceDAOBase {
    @FunctionalInterface
    protected interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    private final Connection transactionConnection;

    protected FinanceDAOBase() {
        this.transactionConnection = null;
    }

    protected FinanceDAOBase(Connection transactionConnection) {
        this.transactionConnection = transactionConnection;
    }

    protected final boolean hasExternalConnection() {
        return transactionConnection != null;
    }

    protected final <T> T withConnection(SqlWork<T> work) {
        if (transactionConnection != null) {
            try {
                return work.run(transactionConnection);
            } catch (SQLException e) {
                throw new FinanceDAOException("Finance database operation failed.", e);
            }
        }
        try (Connection connection = FinanceConnection.open()) {
            return work.run(connection);
        } catch (SQLException e) {
            throw new FinanceDAOException("Finance database operation failed.", e);
        }
    }
}
