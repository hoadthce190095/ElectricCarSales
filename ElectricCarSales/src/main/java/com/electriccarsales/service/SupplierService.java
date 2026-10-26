package com.electriccarsales.service;

import com.electriccarsales.dao.SupplierDAO;
import com.electriccarsales.model.Supplier;
import java.util.List;

public class SupplierService {
    private SupplierDAO supplierDAO;

    public SupplierService() {
        this.supplierDAO = new SupplierDAO();
    }

    public List<Supplier> getAllSuppliers() {
        return supplierDAO.getAllSuppliers();
    }

    public boolean addSupplier(Supplier s) {

        return supplierDAO.addSupplier(s);
    }
}