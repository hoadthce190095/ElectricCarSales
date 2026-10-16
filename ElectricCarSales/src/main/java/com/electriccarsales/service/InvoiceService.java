package com.electriccarsales.service;

import com.electriccarsales.dao.InvoiceDAO;
import com.electriccarsales.model.Invoices;

import java.util.List;

public class InvoiceService {
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();

    public List<Invoices> getCustomerInvoices(int customerId) {
        if (customerId <= 0) throw new IllegalArgumentException("Khách hàng không hợp lệ.");
        return invoiceDAO.findByCustomerId(customerId);
    }

    public List<Invoices> getAllInvoices() {
        return invoiceDAO.findAll();
    }

    public Invoices getCustomerInvoice(int invoiceId, int customerId) {
        if (invoiceId <= 0 || customerId <= 0) throw new IllegalArgumentException("Hóa đơn không hợp lệ.");
        return invoiceDAO.findByIdForCustomer(invoiceId, customerId);
    }
}
