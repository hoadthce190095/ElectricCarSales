package com.electriccarsales.service;

import com.electriccarsales.DAO.BrandDAO;
import com.electriccarsales.model.Brand;

import java.util.List;

public class BrandService {
    private BrandDAO brandDAO;

    public BrandService() {
        brandDAO = new BrandDAO();
    }

    public List<Brand> getAllBrands() {
        return brandDAO.getAll();
    }
}
