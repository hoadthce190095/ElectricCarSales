package com.electriccarsales.dao;

import util.DBContext;
import java.sql.Connection;
import java.sql.SQLException;

/** Finance uses exactly the same database configuration as other DAOs. */
public final class FinanceConnection {
    private FinanceConnection() { }

    public static Connection open() throws SQLException {
        return DBContext.openConnection();
    }
}
