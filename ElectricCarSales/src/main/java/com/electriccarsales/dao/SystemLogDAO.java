package com.electriccarsales.dao;

import com.electriccarsales.model.SystemLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes audit rows when the optional SystemLogs table is installed.
 * When absent, records into in-memory queue and application logger.
 */
public final class SystemLogDAO {
    private static final Logger LOGGER = Logger.getLogger(SystemLogDAO.class.getName());
    public static final List<SystemLog> RUNTIME_LOGS = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicLong LOG_ID_GEN = new AtomicLong(500000);

    public void recordRequired(Connection connection, Integer actorId, String action, String targetType,
        String targetId, String detail) throws SQLException {
        record(connection, actorId, action, targetType, targetId, detail);
    }

    public void record(Connection connection, Integer actorId, String action, String targetType, String targetId,
        String detail) throws SQLException {
        if (SchemaTables.exists(connection, "SystemLogs")) {
            insert(connection, actorId, action, targetType, targetId, detail);
            return;
        }
        logToApplication(actorId, action, targetType, targetId, detail);
    }

    private void logToApplication(Integer actorId, String action, String targetType, String targetId, String detail) {
        LOGGER.log(Level.INFO, "[AUDIT_NOT_IN_DATABASE] user={0} action={1} target={2}:{3} detail={4}",
            new Object[]{actorId, action, targetType, targetId, detail});
        long id = LOG_ID_GEN.incrementAndGet();
        SystemLog log = new SystemLog(
            id, actorId, actorId == null ? "Hệ thống" : "Tài khoản #" + actorId,
            action, targetType, targetId, detail, java.time.LocalDateTime.now()
        );
        RUNTIME_LOGS.add(0, log);
        if (RUNTIME_LOGS.size() > 1000) {
            RUNTIME_LOGS.remove(RUNTIME_LOGS.size() - 1);
        }
    }

    private void insert(Connection connection, Integer actorId, String action, String targetType,
        String targetId, String detail) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO SystemLogs(UserID,Action,TargetType,TargetID,Detail) VALUES(?,?,?,?,?)")) {
            if (actorId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, actorId);
            }
            statement.setString(2, action);
            statement.setString(3, targetType);
            statement.setString(4, targetId);
            statement.setNString(5, detail);
            statement.executeUpdate();
        }
    }
}
