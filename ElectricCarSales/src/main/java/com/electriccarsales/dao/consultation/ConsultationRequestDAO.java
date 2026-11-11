package com.electriccarsales.dao;

import com.electriccarsales.model.ConsultationRequest;
import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConsultationRequestDAO {
    @FunctionalInterface
    interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private static final String SELECT_SQL = """
        SELECT r.ConsultationRequestID, r.CustomerID, r.EmployeeID, r.ModelID,
               r.RequestContent, r.RequestDate, s.StatusName,
               c.CustomerName, c.Phone AS CustomerPhone, c.Email AS CustomerEmail,
               m.ModelName, e.EmployeeName
        FROM ConsultationRequests r
        JOIN ConsultationStatuses s ON s.ConsultationStatusID = r.ConsultationStatusID
        JOIN Customers c ON c.CustomerID = r.CustomerID
        LEFT JOIN CarModels m ON m.ModelID = r.ModelID
        LEFT JOIN Employees e ON e.EmployeeID = r.EmployeeID
        """;

    private final ConnectionProvider connections;

    public ConsultationRequestDAO() {
        this(DBContext::openConnection);
    }

    ConsultationRequestDAO(ConnectionProvider connections) {
        this.connections = connections;
    }

    public List<ConsultationRequest> findAll() {
        String sql = SELECT_SQL + " ORDER BY r.RequestDate DESC, r.ConsultationRequestID DESC";
        try (Connection connection = connections.open();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rows = statement.executeQuery()) {
            List<ConsultationRequest> requests = new ArrayList<>();
            while (rows.next()) {
                requests.add(map(rows));
            }
            return requests;
        } catch (SQLException exception) {
            throw new IllegalStateException("Không thể tải yêu cầu tư vấn.", exception);
        }
    }

    public List<ConsultationRequest> findByCustomerId(int customerId) {
        String sql = SELECT_SQL + " WHERE r.CustomerID = ? ORDER BY r.RequestDate DESC, r.ConsultationRequestID DESC";
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            try (ResultSet rows = statement.executeQuery()) {
                List<ConsultationRequest> requests = new ArrayList<>();
                while (rows.next()) {
                    requests.add(map(rows));
                }
                return requests;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Không thể tải yêu cầu tư vấn của khách hàng.", exception);
        }
    }

    public ConsultationRequest findById(int requestId) {
        String sql = SELECT_SQL + " WHERE r.ConsultationRequestID = ?";
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? map(rows) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Không thể tải chi tiết yêu cầu tư vấn.", exception);
        }
    }

    public void insert(ConsultationRequest request) {
        if (request == null || request.getCustomerId() <= 0 || request.getStaffId() < 0
            || (request.getModelId() != null && request.getModelId() <= 0)
            || request.getContent() == null || request.getContent().isBlank() || request.getContent().length() > 1000) {
            throw new IllegalArgumentException("Thông tin yêu cầu tư vấn không hợp lệ.");
        }
        String status = request.getStatus() == null ? "PENDING" : request.getStatus().trim().toUpperCase(Locale.ROOT);
        Timestamp date = request.getCreatedAt() == null ? new Timestamp(System.currentTimeMillis()) : request.getCreatedAt();
        String sql = """
            INSERT INTO ConsultationRequests(CustomerID,EmployeeID,ModelID,ConsultationStatusID,
                                             RequestContent,RequestDate)
            VALUES(?,?,?,(SELECT ConsultationStatusID FROM ConsultationStatuses WHERE StatusName=?),?,?)
            """;
        try (Connection connection = connections.open();
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, request.getCustomerId());
            if (request.getStaffId() == 0) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, request.getStaffId());
            }
            if (request.getModelId() == null) {
                statement.setNull(3, Types.INTEGER);
            } else {
                statement.setInt(3, request.getModelId());
            }
            statement.setString(4, status);
            statement.setNString(5, request.getContent());
            statement.setTimestamp(6, date);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Không tạo được yêu cầu tư vấn.");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    request.setId(keys.getInt(1));
                }
            }
            request.setStatus(status);
            request.setCreatedAt(date);
        } catch (SQLException exception) {
            throw new IllegalStateException("Không thể tạo yêu cầu tư vấn.", exception);
        }
    }

    public boolean updateRequest(int requestId, Integer employeeId, String newStatus, String appendedMessage) {
        ConsultationRequest current = findById(requestId);
        if (current == null) {
            throw new IllegalArgumentException("Không tìm thấy yêu cầu tư vấn #" + requestId);
        }
        String status = (newStatus == null || newStatus.isBlank())
                ? current.getStatus()
                : newStatus.trim().toUpperCase(Locale.ROOT);
        String updatedContent = current.getContent() == null ? "" : current.getContent();
        if (appendedMessage != null && !appendedMessage.isBlank()) {
            String candidate = updatedContent + "\n" + appendedMessage.trim();
            if (candidate.length() > 1000) {
                throw new IllegalArgumentException("Lịch sử yêu cầu đã đạt giới hạn 1.000 ký tự của hệ thống. Hãy gửi một yêu cầu tư vấn mới.");
            }
            updatedContent = candidate;
        }
        Integer staffToSet = (employeeId != null && employeeId > 0)
                ? employeeId
                : (current.getStaffId() > 0 ? current.getStaffId() : null);

        String sql = """
            UPDATE ConsultationRequests
            SET EmployeeID = ?,
                ConsultationStatusID = (SELECT ConsultationStatusID FROM ConsultationStatuses WHERE StatusName = ?),
                RequestContent = ?
            WHERE ConsultationRequestID = ?
            """;
        try (Connection connection = connections.open();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            if (staffToSet == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, staffToSet);
            }
            ps.setString(2, status);
            ps.setNString(3, updatedContent);
            ps.setInt(4, requestId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Yêu cầu tư vấn vừa thay đổi hoặc không còn tồn tại. Hãy tải lại danh sách.");
            }
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể cập nhật yêu cầu tư vấn.", e);
        }
    }

    private ConsultationRequest map(ResultSet rows) throws SQLException {
        ConsultationRequest request = new ConsultationRequest();
        request.setId(rows.getInt("ConsultationRequestID"));
        request.setCustomerId(rows.getInt("CustomerID"));
        request.setStaffId(rows.getInt("EmployeeID"));
        request.setModelId(rows.getObject("ModelID", Integer.class));
        request.setContent(rows.getString("RequestContent"));
        request.setStatus(rows.getString("StatusName"));
        request.setCreatedAt(rows.getTimestamp("RequestDate"));
        request.setCustomerName(rows.getString("CustomerName"));
        request.setCustomerPhone(rows.getString("CustomerPhone"));
        request.setCustomerEmail(rows.getString("CustomerEmail"));
        request.setModelName(rows.getString("ModelName"));
        request.setStaffName(rows.getString("EmployeeName"));
        return request;
    }
}
