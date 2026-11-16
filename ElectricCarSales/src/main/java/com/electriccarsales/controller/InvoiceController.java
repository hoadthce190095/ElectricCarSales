package com.electriccarsales.controller;

import com.electriccarsales.dao.CustomerDAO;
import com.electriccarsales.model.Customer;
import com.electriccarsales.model.Invoices;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.InvoiceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@WebServlet("/invoice")
public class InvoiceController extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        boolean jsonRequested = "json".equalsIgnoreCase(req.getParameter("format"))
                || (req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json"));

        HttpSession session = req.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("user");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        try {
            String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
            List<Invoices> invoices;
            if ("CUSTOMER".equals(role)) {
                Customer customer = new CustomerDAO().findByUserId(user.getUserId());
                if (customer == null) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                invoices = invoiceService.getCustomerInvoices(customer.getCustomerID());
            } else if ("SALES".equals(role)) {
                invoices = invoiceService.getAllInvoices();
            } else if (List.of("ACCOUNTANT", "MANAGER", "ADMIN", "OPERATIONS").contains(role)) {
                invoices = invoiceService.getAllInvoices();
            } else {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            req.setAttribute("invoices", invoices);

            if (jsonRequested) {
                resp.setContentType("application/json;charset=UTF-8");
                if ("ACCOUNTANT".equals(role)) {
                    JSON.writeValue(resp.getWriter(), Map.of("invoices", invoices,
                            "csrfToken", PaymentController.ensureCsrf(req)));
                } else {
                    JSON.writeValue(resp.getWriter(), Map.of("invoices", invoices));
                }
                return;
            }

            if ("CUSTOMER".equals(role)) {
                req.getRequestDispatcher("/customer/my-invoices.jsp").forward(req, resp);
            } else {
                resp.sendRedirect(req.getContextPath() + "/staff/dashboard#contracts");
            }
        } catch (RuntimeException e) {
            log("Could not load invoices", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải hóa đơn.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        boolean jsonRequested = "json".equalsIgnoreCase(req.getParameter("format"))
                || (req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json"));

        HttpSession session = req.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("user");
        String role = user == null || user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
        if (!"ACCOUNTANT".equals(role)) {
            if (jsonRequested) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.setContentType("application/json;charset=UTF-8");
                JSON.writeValue(resp.getWriter(), Map.of("success", false,
                        "error", "Theo RDS (Mục 2.3 & II.9.3), chỉ Kế toán (Accountant) mới có thẩm quyền xuất hóa đơn."));
            } else {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Kế toán mới có quyền xuất hóa đơn.");
            }
            return;
        }
        if (!PaymentController.validCsrf(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên thao tác không hợp lệ. Tải lại trang rồi thử lại.");
            return;
        }

        try {
            int orderId = Integer.parseInt(req.getParameter("orderId"));
            String note = req.getParameter("note");
            Integer empId = (Integer) session.getAttribute("employeeId");
            if (empId == null || empId <= 0) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản kế toán chưa có hồ sơ nhân viên hợp lệ.");
                return;
            }
            Invoices issued = invoiceService.issueInvoice(orderId, empId, note);
            if (jsonRequested) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.setContentType("application/json;charset=UTF-8");
                JSON.writeValue(resp.getWriter(), Map.of(
                        "success", true,
                        "message", "Đã phát hành hóa đơn " + issued.getInvoiceNumber() + " cho đơn hàng #" + orderId + ".",
                        "invoice", issued));
            } else {
                resp.sendRedirect(req.getContextPath() + "/staff/dashboard#contracts");
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            if (jsonRequested) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("application/json;charset=UTF-8");
                JSON.writeValue(resp.getWriter(), Map.of("success", false, "error", ex.getMessage()));
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }
}
