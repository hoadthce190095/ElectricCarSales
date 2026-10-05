package vn.edu.fpt.electriccarsales.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Audit writes participate in the same transaction as the business change. No update/delete API.
 */
public final class SystemLogRepository {
    public void record(Connection connection, Integer actorId, String action, String targetType, String targetId,
        String detail) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO SystemLogs(UserID,Action,TargetType,TargetID,Detail)" + " VALUES(?,?,?,?,?)")) {
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
