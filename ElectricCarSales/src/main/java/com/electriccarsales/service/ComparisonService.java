package com.electriccarsales.service;

import com.electriccarsales.dao.ComparisonItemDAO;
import com.electriccarsales.dao.ComparisonSessionDAO;
import com.electriccarsales.model.ComparisonItem;
import com.electriccarsales.model.ComparisonSession;

import java.util.List;

public class ComparisonService {
    private ComparisonSessionDAO comparisonSessionDAO;
    private ComparisonItemDAO comparisonItemDAO;

    public ComparisonService (){
    comparisonSessionDAO = new ComparisonSessionDAO();
    comparisonItemDAO = new ComparisonItemDAO();
    }

    public List<ComparisonSession> getAllComparisonSessions(){
        return comparisonSessionDAO.getAll();
    }

    public List<ComparisonItem> getAllComparisonItems(){
        return comparisonItemDAO.getAll();
    }

}
