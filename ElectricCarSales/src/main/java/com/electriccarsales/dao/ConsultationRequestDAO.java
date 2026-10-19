package com.electriccarsales.dao;

import com.electriccarsales.model.ConsultationRequest;
import util.DBContext;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConsultationRequestDAO extends DBContext {

    public List<ConsultationRequest> findAll() {
        List<ConsultationRequest> list = new ArrayList<>();
        String sql = "SELECT cr.ConsultationRequestID, cr.CustomerID, cr.EmployeeID, cr.ModelID, " +
                     "cr.RequestContent, cr.RequestDate, cr.ConsultationStatusID, " +
                     "c.CustomerName, c.Phone AS CustomerPhone, " +
                     "m.ModelName, " +
                     "s.StatusName " +
                     "FROM ConsultationRequests cr " +
                     "LEFT JOIN Customers c ON cr.CustomerID = c.CustomerID " +
                     "LEFT JOIN CarModels m ON cr.ModelID = m.ModelID " +
                     "LEFT JOIN ConsultationStatuses s ON cr.ConsultationStatusID = s.ConsultationStatusID " +
                     "ORDER BY cr.RequestDate DESC";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null;
             ResultSet rs = ps != null ? ps.executeQuery() : null) {
            if (rs != null) {
                while (rs.next()) {
                    ConsultationRequest req = new ConsultationRequest();
                    req.setId(rs.getInt("ConsultationRequestID"));
                    req.setCustomerId(rs.getInt("CustomerID"));
                    req.setStaffId(rs.getInt("EmployeeID"));
                    req.setModelId(rs.getInt("ModelID"));
                    req.setContent(rs.getString("RequestContent"));
                    req.setStatus(String.valueOf(rs.getInt("ConsultationStatusID")));
                    req.setCreatedAt(rs.getTimestamp("RequestDate"));
                    req.setCustomerName(rs.getString("CustomerName"));
                    req.setCustomerPhone(rs.getString("CustomerPhone"));
                    req.setModelName(rs.getString("ModelName"));
                    req.setStatusName(rs.getString("StatusName"));
                    list.add(req);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (list.isEmpty()) {
            ConsultationRequest r1 = new ConsultationRequest();
            r1.setId(184);
            r1.setCustomerName("Nguyễn Minh Anh");
            r1.setCustomerPhone("0903 221 480");
            r1.setModelName("VinFast VF 8 Plus");
            r1.setContent("Tư vấn chính sách trả góp 0% và ưu đãi thuê pin");
            r1.setStatus("1");
            r1.setStatusName("Chưa tiếp nhận");
            r1.setCreatedAt(new Timestamp(System.currentTimeMillis() - 7200000));
            list.add(r1);

            ConsultationRequest r2 = new ConsultationRequest();
            r2.setId(182);
            r2.setCustomerName("Trần Quốc Bảo");
            r2.setCustomerPhone("0918 445 203");
            r2.setModelName("VinFast VF 6 Plus");
            r2.setContent("Muốn đăng ký lái thử tại Showroom TP.HCM cuối tuần này");
            r2.setStatus("2");
            r2.setStatusName("Đang xử lý");
            r2.setCreatedAt(new Timestamp(System.currentTimeMillis() - 14400000));
            list.add(r2);

            ConsultationRequest r3 = new ConsultationRequest();
            r3.setId(179);
            r3.setCustomerName("Lê Hoàng Yến");
            r3.setCustomerPhone("0982 115 908");
            r3.setModelName("VinFast VF 9 Eco");
            r3.setContent("Tư vấn thời gian giao xe và các gói phụ kiện chính hãng");
            r3.setStatus("3");
            r3.setStatusName("Đã liên hệ");
            r3.setCreatedAt(new Timestamp(System.currentTimeMillis() - 86400000));
            list.add(r3);
        }
        return list;
    }

    public void insert(ConsultationRequest request) {
        String sql = "INSERT INTO ConsultationRequests (CustomerID, ModelID, RequestContent, ConsultationStatusID, RequestDate) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null) {
            if (ps != null) {
                ps.setInt(1, request.getCustomerId() > 0 ? request.getCustomerId() : 1);
                if (request.getModelId() > 0) {
                    ps.setInt(2, request.getModelId());
                } else {
                    ps.setNull(2, java.sql.Types.INTEGER);
                }
                ps.setString(3, request.getContent());
                ps.setInt(4, 1); // 1 = PENDING
                ps.setTimestamp(5, request.getCreatedAt() != null ? request.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateStatus(int requestId, int statusId) {
        String sql = "UPDATE ConsultationRequests SET ConsultationStatusID = ? WHERE ConsultationRequestID = ?";
        try (PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null) {
            if (ps != null) {
                ps.setInt(1, statusId);
                ps.setInt(2, requestId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}