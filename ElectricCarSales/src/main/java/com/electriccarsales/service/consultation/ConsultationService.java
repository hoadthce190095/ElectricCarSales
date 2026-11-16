package com.electriccarsales.service;

import com.electriccarsales.dao.ConsultationRequestDAO;
import com.electriccarsales.model.ConsultationRequest;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ConsultationService {
    private static final Set<String> VALID_STATUSES = Set.of("PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED");
    private final ConsultationRequestDAO dao = new ConsultationRequestDAO();

    public List<ConsultationRequest> getAllRequests() {
        return dao.findAll();
    }

    public List<ConsultationRequest> getCustomerRequests(int customerId) {
        if (customerId <= 0) throw new IllegalArgumentException("Mã khách hàng không hợp lệ.");
        return dao.findByCustomerId(customerId);
    }

    public ConsultationRequest getRequest(int requestId) {
        if (requestId <= 0) throw new IllegalArgumentException("Mã yêu cầu tư vấn không hợp lệ.");
        return dao.findById(requestId);
    }

    public void createRequest(ConsultationRequest request) {
        if (request == null) throw new IllegalArgumentException("Yêu cầu tư vấn không được để trống.");
        request.setStatus("PENDING");
        dao.insert(request);
    }

    public boolean updateByStaff(int requestId, Integer employeeId, String staffName, String newStatus, String responseNote) {
        ConsultationRequest assigned = dao.findById(requestId);
        if (assigned == null || employeeId == null || employeeId <= 0
                || (assigned.getStaffId() != 0 && assigned.getStaffId() != employeeId)) {
            throw new IllegalArgumentException("Bạn không được phân công cuộc trò chuyện này.");
        }
        String normalizedStatus = newStatus == null || newStatus.isBlank()
                ? "IN_PROGRESS"
                : newStatus.trim().toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Trạng thái tư vấn không hợp lệ: " + normalizedStatus);
        }
        String formattedNote = null;
        if (responseNote != null && !responseNote.isBlank()) {
            String label = (staffName == null || staffName.isBlank()) ? "NV Tư vấn" : staffName.trim();
            formattedNote = "[" + label + "]: " + responseNote.trim();
        }
        return dao.updateRequest(requestId, employeeId, normalizedStatus, formattedNote);
    }

    public boolean appendCustomerNote(int requestId, int customerId, String customerNote) {
        if (customerNote == null || customerNote.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung phản hồi.");
        }
        ConsultationRequest current = dao.findById(requestId);
        if (current == null || current.getCustomerId() != customerId) {
            throw new IllegalArgumentException("Không tìm thấy yêu cầu tư vấn của bạn.");
        }
        if ("CANCELLED".equalsIgnoreCase(current.getStatus()) || "COMPLETED".equalsIgnoreCase(current.getStatus())) {
            throw new IllegalStateException("Yêu cầu tư vấn này đã kết thúc. Vui lòng gửi yêu cầu tư vấn mới.");
        }
        return dao.updateRequest(requestId, null, current.getStatus(), "[Khách hàng]: " + customerNote.trim());
    }
}