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
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsultationRequestDAO {
    private static final Logger LOGGER = Logger.getLogger(ConsultationRequestDAO.class.getName());

    @FunctionalInterface
    interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private static final String SELECT_SQL = """
        SELECT r.ConsultationRequestID, r.CustomerID, r.EmployeeID, r.ModelID,
               r.RequestContent, r.RequestDate, COALESCE(s.StatusName, 'PENDING') AS StatusName,
               COALESCE(c.CustomerName, N'Khách hàng') AS CustomerName,
               c.Phone AS CustomerPhone, c.Email AS CustomerEmail,
               m.ModelName, e.EmployeeName
        FROM ConsultationRequests r
        LEFT JOIN ConsultationStatuses s ON s.ConsultationStatusID = r.ConsultationStatusID
        LEFT JOIN Customers c ON c.CustomerID = r.CustomerID
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
            LOGGER.log(Level.SEVERE, "Không thể tải danh sách yêu cầu tư vấn: " + exception.getMessage(), exception);
            throw new IllegalStateException("Không thể tải yêu cầu tư vấn: " + exception.getMessage(), exception);
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
            LOGGER.log(Level.SEVERE, "Không thể tải yêu cầu tư vấn của khách #" + customerId + ": " + exception.getMessage(), exception);
            throw new IllegalStateException("Không thể tải yêu cầu tư vấn của khách hàng: " + exception.getMessage(), exception);
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
            LOGGER.log(Level.SEVERE, "Không thể tải chi tiết yêu cầu tư vấn #" + requestId + ": " + exception.getMessage(), exception);
            throw new IllegalStateException("Không thể tải chi tiết yêu cầu tư vấn: " + exception.getMessage(), exception);
        }
    }

    public int resolveStatusId(Connection connection, String statusName) throws SQLException {
        String target = statusName == null || statusName.isBlank() ? "PENDING" : statusName.trim().toUpperCase(Locale.ROOT);
        // 1. Kiểm tra khớp chính xác StatusName
        String query = "SELECT TOP 1 ConsultationStatusID FROM ConsultationStatuses WHERE UPPER(LTRIM(RTRIM(StatusName))) = UPPER(LTRIM(RTRIM(?)))";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, target);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        // 2. Kiểm tra khớp gần đúng
        String likeQuery = "SELECT TOP 1 ConsultationStatusID FROM ConsultationStatuses WHERE UPPER(StatusName) LIKE ? OR UPPER(StatusName) LIKE 'PEND%' OR UPPER(StatusName) LIKE 'CHO%'";
        try (PreparedStatement ps = connection.prepareStatement(likeQuery)) {
            ps.setString(1, "%" + target + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        // 3. Lấy trạng thái đầu tiên có sẵn
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT TOP 1 ConsultationStatusID FROM ConsultationStatuses ORDER BY ConsultationStatusID ASC")) {
            if (rs.next()) return rs.getInt(1);
        }
        // 4. Bảng rỗng -> tự động chèn các trạng thái chuẩn theo ElectricCarSalesDB.sql
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("INSERT INTO ConsultationStatuses(StatusName) VALUES ('PENDING'), ('IN_PROGRESS'), ('COMPLETED'), ('CANCELLED')");
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Không thể tự động chèn dữ liệu mẫu ConsultationStatuses: " + ex.getMessage());
        }
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT TOP 1 ConsultationStatusID FROM ConsultationStatuses ORDER BY ConsultationStatusID ASC")) {
            if (rs.next()) return rs.getInt(1);
        }
        return 1;
    }

    private Integer validateModelId(Connection connection, Integer modelId) {
        if (modelId == null || modelId <= 0) return null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT ModelID FROM CarModels WHERE ModelID = ?")) {
            ps.setInt(1, modelId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Không thể kiểm tra ModelID " + modelId + ": " + ex.getMessage());
        }
        return null;
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
            INSERT INTO ConsultationRequests(CustomerID, EmployeeID, ModelID, ConsultationStatusID,
                                             RequestContent, RequestDate)
            VALUES(?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = connections.open()) {
            int statusId = resolveStatusId(connection, status);
            Integer safeModelId = validateModelId(connection, request.getModelId());

            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, request.getCustomerId());
                if (request.getStaffId() == 0) {
                    statement.setNull(2, Types.INTEGER);
                } else {
                    statement.setInt(2, request.getStaffId());
                }
                if (safeModelId == null) {
                    statement.setNull(3, Types.INTEGER);
                } else {
                    statement.setInt(3, safeModelId);
                }
                statement.setInt(4, statusId);
                statement.setNString(5, request.getContent());
                statement.setTimestamp(6, date);

                int rowsAffected = statement.executeUpdate();
                if (rowsAffected < 1) {
                    throw new SQLException("Không thể lưu yêu cầu tư vấn vào database (0 hàng bị ảnh hưởng).");
                }
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        request.setId(keys.getInt(1));
                    }
                }
                if (request.getId() <= 0) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "SELECT TOP 1 ConsultationRequestID FROM ConsultationRequests WHERE CustomerID = ? ORDER BY ConsultationRequestID DESC")) {
                        ps.setInt(1, request.getCustomerId());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                request.setId(rs.getInt(1));
                            }
                        }
                    }
                }
                request.setStatus(status);
                request.setCreatedAt(date);
                if (safeModelId != null) {
                    request.setModelId(safeModelId);
                }
            }
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tạo yêu cầu tư vấn trong database: " + exception.getMessage(), exception);
            throw new IllegalStateException("Không thể tạo yêu cầu tư vấn: " + exception.getMessage(), exception);
        }
    }

    public boolean updateRequest(int requestId, Integer employeeId, String newStatus, String appendedMessage) {
        ConsultationRequest current = findById(requestId);
        if (current == null) {
            throw new IllegalArgumentException("Không tìm thấy yêu cầu tư vấn #" + requestId);
        }
        if (employeeId != null && current.getStaffId() != 0 && current.getStaffId() != employeeId) {
            throw new IllegalStateException("Yêu cầu đã được phân công cho nhân viên khác.");
        }
        String status = (newStatus == null || newStatus.isBlank())
                ? current.getStatus()
                : newStatus.trim().toUpperCase(Locale.ROOT);
        if (appendedMessage != null && !appendedMessage.isBlank()
                && ("COMPLETED".equals(current.getStatus()) || "CANCELLED".equals(current.getStatus()))) {
            throw new IllegalStateException("Cuộc trò chuyện đã kết thúc.");
        }
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
                ConsultationStatusID = ?,
                RequestContent = ?
            WHERE ConsultationRequestID = ? AND RequestContent = ?
              AND COALESCE(EmployeeID, 0) = ?
            """;
        try (Connection connection = connections.open()) {
            int newStatusId = resolveStatusId(connection, status);
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                if (staffToSet == null) {
                    ps.setNull(1, Types.INTEGER);
                } else {
                    ps.setInt(1, staffToSet);
                }
                ps.setInt(2, newStatusId);
                ps.setNString(3, updatedContent);
                ps.setInt(4, requestId);
                ps.setNString(5, current.getContent());
                ps.setInt(6, current.getStaffId());
                if (ps.executeUpdate() != 1) {
                    throw new IllegalStateException("Yêu cầu tư vấn vừa thay đổi hoặc không còn tồn tại. Hãy tải lại danh sách.");
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cập nhật yêu cầu tư vấn #" + requestId + ": " + e.getMessage(), e);
            throw new IllegalStateException("Không thể cập nhật yêu cầu tư vấn: " + e.getMessage(), e);
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
