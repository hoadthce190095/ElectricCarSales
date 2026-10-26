package com.electriccarsales.dao.consultation;

import com.electriccarsales.model.consultation.ConsultationRequest;
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

    private final ConnectionProvider connections;

    public ConsultationRequestDAO() {
        this(DBContext::openConnection);
    }

    ConsultationRequestDAO(ConnectionProvider connections) {
        this.connections = connections;
    }

    public List<ConsultationRequest> findAll() {
        String sql = """
            SELECT r.ConsultationRequestID,r.CustomerID,r.EmployeeID,r.ModelID,
                   r.RequestContent,r.RequestDate,s.StatusName
            FROM ConsultationRequests r
            JOIN ConsultationStatuses s ON s.ConsultationStatusID=r.ConsultationStatusID
            ORDER BY r.RequestDate DESC,r.ConsultationRequestID DESC
            """;
        try (Connection connection = connections.open();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rows = statement.executeQuery()) {
            List<ConsultationRequest> requests = new ArrayList<>();
            while (rows.next()) {
                ConsultationRequest request = new ConsultationRequest();
                request.setId(rows.getInt("ConsultationRequestID"));
                request.setCustomerId(rows.getInt("CustomerID"));
                request.setStaffId(rows.getInt("EmployeeID"));
                request.setModelId(rows.getObject("ModelID", Integer.class));
                request.setContent(rows.getString("RequestContent"));
                request.setStatus(rows.getString("StatusName"));
                request.setCreatedAt(rows.getTimestamp("RequestDate"));
                requests.add(request);
            }
            return requests;
        } catch (SQLException exception) {
            throw new IllegalStateException("Không thể tải yêu cầu tư vấn.", exception);
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
            statement.setString(5, request.getContent());
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
}
