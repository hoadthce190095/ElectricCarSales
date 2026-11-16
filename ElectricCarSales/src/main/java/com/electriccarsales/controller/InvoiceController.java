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
            if (jsonRequested) FinanceControllerSupport.sendError(req, resp, 401, "Vui lòng đăng nhập lại.");
            else resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        try {
            String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
            List<Invoices> invoices;
            if ("CUSTOMER".equals(role)) {
                Customer customer = new CustomerDAO().findByUserId(user.getUserId());
                if (customer == null) {
                    FinanceControllerSupport.sendError(req, resp, 403, "Bạn không có quyền xem mục này.");
                    return;
                }
                invoices = invoiceService.getCustomerInvoices(customer.getCustomerID());
                req.setAttribute("customerOrders", new com.electriccarsales.dao.order.OrderDAO().findByCustomerId(customer.getCustomerID()));
                req.setAttribute("customerContracts", new com.electriccarsales.dao.ContractsDAO().findByCustomerId(customer.getCustomerID()));
            } else if ("SALES".equals(role)) {
                invoices = invoiceService.getAllInvoices();
            } else if (List.of("ACCOUNTANT", "MANAGER", "ADMIN", "OPERATIONS").contains(role)) {
                invoices = invoiceService.getAllInvoices();
            } else {
                FinanceControllerSupport.sendError(req, resp, 403, "Bạn không có quyền xem mục này.");
                return;
            }
            req.setAttribute("invoices", invoices);

            if (jsonRequested) {
                resp.setContentType("application/json;charset=UTF-8");
                if ("ACCOUNTANT".equals(role)) {
                    resp.getWriter().write(JSON.writeValueAsString(Map.of("invoices", invoices,
                            "csrfToken", PaymentController.ensureCsrf(req))));
                } else {
                    resp.getWriter().write(JSON.writeValueAsString(Map.of("invoices", invoices)));
                }
                return;
            }

            if ("CUSTOMER".equals(role)) {
                req.getRequestDispatcher("/customer/my-invoices.jsp").forward(req, resp);
            } else {
                // Staff: load eligible orders and forward to dedicated JSP
                List<java.util.Map<String, Object>> allOrders = invoiceService.getOrdersForInvoicePage();
                // Phân tách: đủ điều kiện phát hành (SIGNED + PAID + chưa có invoice)
                java.util.List<java.util.Map<String, Object>> eligibleOrders = new java.util.ArrayList<>();
                java.util.List<java.util.Map<String, Object>> issuedOrders = new java.util.ArrayList<>();
                for (java.util.Map<String, Object> row : allOrders) {
                    if (row.get("invoiceId") != null) {
                        issuedOrders.add(row);
                    } else if ("SIGNED".equalsIgnoreCase(String.valueOf(row.get("contractStatus")))
                            && "PAID".equalsIgnoreCase(String.valueOf(row.get("orderStatus")))) {
                        eligibleOrders.add(row);
                    }
                }
                req.setAttribute("allOrders", allOrders);
                req.setAttribute("eligibleOrders", eligibleOrders);
                req.setAttribute("issuedOrders", issuedOrders);
                req.setAttribute("csrfToken", PaymentController.ensureCsrf(req));
                req.getRequestDispatcher("/staff/invoices.jsp").forward(req, resp);
            }
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            log("Could not load invoices", e);
            FinanceControllerSupport.sendError(req, resp, 503, "Không thể tải dữ liệu hóa đơn.");
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
            FinanceControllerSupport.sendError(req, resp, 403, "Phiên thao tác không hợp lệ. Tải lại trang rồi thử lại.");
            return;
        }

        try {
            int orderId = Integer.parseInt(req.getParameter("orderId"));
            String note = req.getParameter("note");
            Integer empId = (Integer) session.getAttribute("employeeId");
            if (empId == null || empId <= 0) {
                FinanceControllerSupport.sendError(req, resp, 403, "Tài khoản kế toán chưa có hồ sơ nhân viên hợp lệ.");
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
                resp.sendRedirect(req.getContextPath() + "/invoice?issued=1");
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            if (jsonRequested) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("application/json;charset=UTF-8");
                JSON.writeValue(resp.getWriter(), Map.of("success", false, "error", ex.getMessage()));
            } else {
                resp.sendRedirect(req.getContextPath() + "/invoice?error=" + java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
    }
}
