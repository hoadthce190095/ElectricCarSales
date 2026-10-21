package com.electriccarsales.service;

import com.electriccarsales.dao.SpecificationDAO;
import com.electriccarsales.model.Specification;

import java.util.List;

public class SpecificationService {
    private SpecificationDAO specificationDAO;

    public SpecificationService() {
        specificationDAO = new SpecificationDAO();
    }

 public List<Specification> getSpecification() {
        return specificationDAO.getAll();
 }
}
