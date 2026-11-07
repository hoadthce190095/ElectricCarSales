package com.electriccarsales.service;

import com.electriccarsales.dao.ComparisonItemDAO;
import com.electriccarsales.dao.ComparisonSessionDAO;
import com.electriccarsales.model.ComparisonItem;
import com.electriccarsales.model.ComparisonSession;

import java.util.List;

public class ComparisonService {
    private final ComparisonSessionDAO sessionDAO = new ComparisonSessionDAO();
    private final ComparisonItemDAO itemDAO = new ComparisonItemDAO();

    public List<ComparisonSession> getAllComparisonSessions() {
        return sessionDAO.getAll();
    }

    public List<ComparisonItem> getAllComparisonItems() {
        return itemDAO.getAll();
    }
}
