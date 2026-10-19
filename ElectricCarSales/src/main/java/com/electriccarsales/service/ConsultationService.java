package com.electriccarsales.service.consultation;

import com.electriccarsales.dao.consultation.ConsultationRequestDAO;
import com.electriccarsales.model.consultation.ConsultationRequest;
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