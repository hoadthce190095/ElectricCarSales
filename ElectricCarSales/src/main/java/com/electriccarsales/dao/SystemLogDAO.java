package com.electriccarsales.dao;

import com.electriccarsales.model.PageResponse;
import com.electriccarsales.model.SystemLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public PageResponse<SystemLog> list(Connection connection, String action, Integer userId,
        LocalDate from, LocalDate to, int page, String targetType, boolean includeHistory, boolean export) throws SQLException {
        int pageSize = export ? 50000 : 20;
        return queryLogs(connection, action, userId, from, to, page, pageSize, targetType, includeHistory);
    }

    public PageResponse<SystemLog> queryLogs(Connection connection, String action, Integer userId,
        LocalDate from, LocalDate to, int page, int pageSize, String targetType, boolean includeHistory) throws SQLException {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;

        String safeAction = action == null ? "" : action.trim();
        String safeTarget = targetType == null ? "" : targetType.trim();
        LocalDate safeFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate safeTo = to != null ? to : LocalDate.now();
        Timestamp fromTs = Timestamp.valueOf(safeFrom.atStartOfDay());
        Timestamp toTs = Timestamp.valueOf(safeTo.plusDays(1).atStartOfDay());

        if (connection != null && SchemaTables.exists(connection, "SystemLogs")) {
            String where = " WHERE (?='' OR l.Action=?) AND (? IS NULL OR l.UserID=?) AND l.CreatedDate>=? AND l.CreatedDate<? AND (?='' OR l.TargetType=?)";
            long total = 0;
            try (PreparedStatement countSt = connection.prepareStatement("SELECT COUNT(*) FROM SystemLogs l" + where)) {
                bindLogs(countSt, safeAction, userId, fromTs, toTs, safeTarget);
                try (ResultSet rs = countSt.executeQuery()) {
                    if (rs.next()) total = rs.getLong(1);
                }
            }

            if (pageSize > 20 && total > 50000) {
                throw new IllegalArgumentException("Nhật ký vượt 50.000 dòng. Thu hẹp bộ lọc trước khi xuất.");
            }

            List<SystemLog> result = new ArrayList<>();
            String sql = "SELECT l.LogID, l.UserID, u.Username, l.Action, l.TargetType, l.TargetID, l.Detail, l.CreatedDate "
                    + "FROM SystemLogs l LEFT JOIN Users u ON u.UserID=l.UserID" + where
                    + " ORDER BY l.LogID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            try (PreparedStatement st = connection.prepareStatement(sql)) {
                bindLogs(st, safeAction, userId, fromTs, toTs, safeTarget);
                st.setInt(9, (page - 1) * pageSize);
                st.setInt(10, pageSize);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        result.add(new SystemLog(
                                rs.getLong("LogID"),
                                rs.getObject("UserID", Integer.class),
                                rs.getString("Username"),
                                rs.getString("Action"),
                                rs.getString("TargetType"),
                                rs.getString("TargetID"),
                                rs.getNString("Detail"),
                                rs.getTimestamp("CreatedDate").toLocalDateTime()
                        ));
                    }
                }
            }
            PageResponse<SystemLog> pageResp = new PageResponse<>(result, page, total, pageSize);
            pageResp.setAvailable(true);
            return pageResp;
        }

        // Khi bảng SystemLogs chưa được cài đặt trong database:
        // Đọc từ bộ nhớ RUNTIME_LOGS và tổng hợp từ OrderStatusHistory nếu includeHistory=true
        List<SystemLog> all = new ArrayList<>();
        synchronized (RUNTIME_LOGS) {
            all.addAll(RUNTIME_LOGS);
        }

        if (includeHistory && connection != null && SchemaTables.exists(connection, "OrderStatusHistory")) {
            try {
                String historySql = "SELECT h.HistoryID, h.OrderID, os.StatusName, h.EmployeeID, u.Username, h.ChangedAt, h.Note "
                        + "FROM OrderStatusHistory h "
                        + "LEFT JOIN OrderStatuses os ON os.OrderStatusID=h.OrderStatusID "
                        + "LEFT JOIN Employees e ON e.EmployeeID=h.EmployeeID "
                        + "LEFT JOIN Users u ON u.UserID=e.UserID "
                        + "WHERE h.ChangedAt>=? AND h.ChangedAt<? ORDER BY h.HistoryID DESC";
                try (PreparedStatement ps = connection.prepareStatement(historySql)) {
                    ps.setTimestamp(1, fromTs);
                    ps.setTimestamp(2, toTs);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            long hid = rs.getLong("HistoryID");
                            int oid = rs.getInt("OrderID");
                            String status = rs.getString("StatusName");
                            Integer empId = rs.getObject("EmployeeID", Integer.class);
                            String username = rs.getString("Username");
                            Timestamp cat = rs.getTimestamp("ChangedAt");
                            String note = rs.getString("Note");
                            LocalDateTime dt = cat != null ? cat.toLocalDateTime() : LocalDateTime.now();
                            all.add(new SystemLog(1000000L + hid, empId, username != null ? username : ("Employee#" + empId),
                                    "ORDER_STATUS_" + (status != null ? status : "UPDATE"),
                                    "ORDER", String.valueOf(oid), note != null ? note : ("Trạng thái đơn: " + status), dt));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        LocalDateTime fromLdt = safeFrom.atStartOfDay();
        LocalDateTime toLdt = safeTo.plusDays(1).atStartOfDay();

        List<SystemLog> filtered = new ArrayList<>();
        for (SystemLog log : all) {
            if (log == null) continue;
            if (!safeAction.isEmpty() && !safeAction.equalsIgnoreCase(log.getAction())) continue;
            if (userId != null && !userId.equals(log.getUserId())) continue;
            if (!safeTarget.isEmpty() && !safeTarget.equalsIgnoreCase(log.getTargetType())) continue;
            LocalDateTime logDate = log.getCreatedDate();
            if (logDate != null && (logDate.isBefore(fromLdt) || logDate.isAfter(toLdt))) continue;
            filtered.add(log);
        }

        filtered.sort((a, b) -> {
            if (a.getCreatedDate() == null && b.getCreatedDate() == null) return 0;
            if (a.getCreatedDate() == null) return 1;
            if (b.getCreatedDate() == null) return -1;
            return b.getCreatedDate().compareTo(a.getCreatedDate());
        });

        long total = filtered.size();
        int fromIdx = Math.min((page - 1) * pageSize, filtered.size());
        int toIdx = Math.min(fromIdx + pageSize, filtered.size());
        List<SystemLog> paged = new ArrayList<>(filtered.subList(fromIdx, toIdx));

        PageResponse<SystemLog> response = new PageResponse<>(paged, page, total, pageSize);
        response.setAvailable(true);
        return response;
    }

    private static void bindLogs(PreparedStatement statement, String action, Integer userId,
        Timestamp from, Timestamp to, String targetType) throws SQLException {
        statement.setString(1, action);
        statement.setString(2, action);
        if (userId == null) {
            statement.setNull(3, Types.INTEGER);
            statement.setNull(4, Types.INTEGER);
        } else {
            statement.setInt(3, userId);
            statement.setInt(4, userId);
        }
        statement.setTimestamp(5, from);
        statement.setTimestamp(6, to);
        statement.setString(7, targetType);
        statement.setString(8, targetType);
    }
}
