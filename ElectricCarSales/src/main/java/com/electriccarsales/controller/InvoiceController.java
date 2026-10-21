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
public class InvoiceController extends HttpServlet {
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        var session = req.getSession(false);
        Object id = session == null ? null : session.getAttribute("userID");
        Object role = session == null ? null : session.getAttribute("roleName");
        if (!(id instanceof Integer userId) || !(role instanceof String roleName)) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if (!"ACCOUNTANT".equalsIgnoreCase(roleName)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            int orderId = Integer.parseInt(req.getParameter("orderId"));
            invoiceService.issueForPaidOrder(orderId, userId);
            resp.sendRedirect(req.getContextPath() + "/invoice?issued=1");
        } catch (IllegalArgumentException e) {
            req.getSession().setAttribute("invoiceError", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/invoice");
        } catch (RuntimeException e) {
            log("Invoice issue failed", e);
            req.getSession().setAttribute("invoiceError", "Không thể phát hành hóa đơn lúc này.");
            resp.sendRedirect(req.getContextPath() + "/invoice");
        }
    }

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
            req.setAttribute("invoiceRole", role);
            if ("1".equals(req.getParameter("issued"))) req.setAttribute("success", "Hóa đơn đã được phát hành.");
            if (session != null && session.getAttribute("invoiceError") != null) {
                req.setAttribute("error", session.getAttribute("invoiceError"));
                session.removeAttribute("invoiceError");
            }
            if ("CUSTOMER".equals(role)) {
                Customer customer = new CustomerDAO().findByUserId(userId);
                if (customer == null) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                req.setAttribute("invoices", invoiceService.getCustomerInvoices(customer.getCustomerID()));
            } else if ("SALES".equals(role)) {
                req.setAttribute("invoices", invoiceService.getEmployeeInvoices(userId));
            } else if ("ACCOUNTANT".equals(role) || "MANAGER".equals(role)) {
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
