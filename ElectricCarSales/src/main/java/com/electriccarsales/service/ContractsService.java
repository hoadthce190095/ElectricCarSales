package com.electriccarsales.service;

import com.electriccarsales.dao.ContractsDAO;
import com.electriccarsales.model.Contract;
import java.util.List;

public class ContractsService {
    private ContractsDAO contractsDAO;

    public ContractsService() {
        this.contractsDAO = new ContractsDAO();
    }

    public List<Contract> getAllContracts() {
        return contractsDAO.getAllContracts();
    }

    public boolean addContract(Contract c) {
        return contractsDAO.addContract(c);
    }
}