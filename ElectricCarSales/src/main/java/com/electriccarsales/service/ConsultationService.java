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
        request.setStatus("Pending");
        dao.insert(request);
    }
}