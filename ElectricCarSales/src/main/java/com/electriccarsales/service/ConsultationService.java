package com.electriccarsales.service;

import com.electriccarsales.dao.ConsultationRequestDAO;
import com.electriccarsales.model.ConsultationRequest;
import java.util.List;

public class ConsultationService {
    private ConsultationRequestDAO dao = new ConsultationRequestDAO();

    public List<ConsultationRequest> getAllRequests() {
        return dao.findAll();
    }

    public void createRequest(ConsultationRequest request) {
        dao.insert(request);
    }

    public void updateRequestStatus(int requestId, int statusId) {
        dao.updateStatus(requestId, statusId);
    }
}