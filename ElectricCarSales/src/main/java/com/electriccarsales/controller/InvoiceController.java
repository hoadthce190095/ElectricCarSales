package com.electriccarsales.controller;

import com.electriccarsales.dao.CustomerDAO;
import com.electriccarsales.model.Customer;
import com.electriccarsales.service.InvoiceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Locale;

@WebServlet("/invoice")
public class InvoiceController extends HttpServlet{
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        var session = req.getSession(false);
        Object id = session == null ? null : session.getAttribute("userID");
        Object roleAttribute = session == null ? null : session.getAttribute("roleName");
        if (!(id instanceof Integer userId) || !(roleAttribute instanceof String roleName)) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        try {
            String role = roleName.toUpperCase(Locale.ROOT);
            if ("CUSTOMER".equals(role)) {
                Customer customer = new CustomerDAO().findByUserId(userId);
                if (customer == null) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                req.setAttribute("invoices", invoiceService.getCustomerInvoices(customer.getCustomerID()));
            } else if ("ACCOUNTANT".equals(role) || "ADMIN".equals(role)) {
                req.setAttribute("invoices", invoiceService.getAllInvoices());
            } else {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            req.getRequestDispatcher("/customer/my-invoices.jsp").forward(req, resp);
        } catch (RuntimeException e) {
            log("Could not load invoices", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải hóa đơn.");
        }
    }
}
