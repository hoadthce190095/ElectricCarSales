package com.electriccarsales.dao;

import com.electriccarsales.model.SystemLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists audit records in the same transaction as the business change.
 * Standalone events can use the application log on older databases.
 */
public final class SystemLogDAO {
    private static final Logger LOGGER = Logger.getLogger(SystemLogDAO.class.getName());
    private static final AtomicLong SEQ = new AtomicLong(1);
    private static final List<SystemLog> MEMORY_LOGS = new CopyOnWriteArrayList<>();

    public static List<SystemLog> memoryLogs() {
        List<SystemLog> copy = new ArrayList<>(MEMORY_LOGS);
        Collections.reverse(copy);
        return copy;
    }

    public void record(Connection connection, Integer actorId, String action, String targetType, String targetId,
        String detail) throws SQLException {
        if (SchemaTables.exists(connection, "SystemLogs")) {
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
            return;
        }
        if (connection != null && !connection.getAutoCommit()) {
            throw new SQLException("Không thể ghi nhật ký giao dịch. Hãy cập nhật schema quản trị.");
        }

        String username = null;
        if (connection != null && actorId != null) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT Username FROM Users WHERE UserID = ?")) {
                ps.setInt(1, actorId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) username = rs.getString(1);
                }
            } catch (SQLException ignored) {
            }
        }
        SystemLog entry = new SystemLog(SEQ.getAndIncrement(), actorId, username, action, targetType, targetId,
                detail, LocalDateTime.now());
        if (MEMORY_LOGS.size() > 2000) {
            MEMORY_LOGS.remove(0);
        }
        MEMORY_LOGS.add(entry);
        LOGGER.log(Level.INFO, "[AUDIT] user={0} action={1} target={2}:{3} detail={4}",
                new Object[]{actorId, action, targetType, targetId, detail});
    }
}
