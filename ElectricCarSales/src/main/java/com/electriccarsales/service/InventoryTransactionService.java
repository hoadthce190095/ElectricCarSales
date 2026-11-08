package com.electriccarsales.service;

import com.electriccarsales.dao.InventoryTransactionDAO;
import com.electriccarsales.model.InventoryTransaction;
import java.util.List;

public class InventoryTransactionService {
    private InventoryTransactionDAO transactionDAO;

    public InventoryTransactionService() {
        this.transactionDAO = new InventoryTransactionDAO();
    }

    public List<InventoryTransaction> getAllTransactions() {
        return transactionDAO.getAllTransactions();
    }

    public boolean addTransaction(InventoryTransaction it) {
        return transactionDAO.addTransaction(it);
    }
}