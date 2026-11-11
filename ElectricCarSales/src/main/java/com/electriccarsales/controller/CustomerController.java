package com.electriccarsales.controller;

import com.electriccarsales.dao.ContractsDAO;
import com.electriccarsales.dao.CustomerDAO;
import com.electriccarsales.dao.order.OrderDAO;
import com.electriccarsales.dao.PaymentDAO;
import com.electriccarsales.model.Customer;
import com.electriccarsales.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/customer/profile")
public class CustomerController extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final CustomerService customerService = new CustomerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        String roleName = req.getSession(false) == null ? "" : String.valueOf(req.getSession(false).getAttribute("roleName"));
        if ("staff".equalsIgnoreCase(req.getParameter("mode"))) {
            if (!List.of("SALES", "MANAGER", "ADMIN").contains(roleName.toUpperCase())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            resp.setContentType("application/json;charset=UTF-8");
            JSON.writeValue(resp.getWriter(), Map.of("customers", customerService.getAllCustomers()));
            return;
        }

        Object id = req.getSession(false) == null ? null : req.getSession(false).getAttribute("userID");
        if (!(id instanceof Integer userId) || userId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if (!"CUSTOMER".equalsIgnoreCase(roleName)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            Customer customer = customerService.getOwnProfile(userId);
            req.setAttribute("customer", customer);
            if (customer != null) {
                req.setAttribute("customerOrders", new OrderDAO().findByCustomerId(customer.getCustomerID()));
                req.setAttribute("customerPayments", new PaymentDAO().findDetailsByCustomerId(customer.getCustomerID()));
                req.setAttribute("customerContracts", new ContractsDAO().findByCustomerId(customer.getCustomerID()));
            }
            req.getRequestDispatcher("/customer/my-account.jsp").forward(req, resp);
        } catch (RuntimeException e) {
            log("Could not load customer profile", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải hồ sơ khách hàng.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        String roleName = req.getSession(false) == null ? "" : String.valueOf(req.getSession(false).getAttribute("roleName"));

        if ("staff".equalsIgnoreCase(req.getParameter("mode"))) {
            if (!List.of("SALES", "MANAGER", "ADMIN").contains(roleName.toUpperCase())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            resp.setContentType("application/json;charset=UTF-8");
            try {
                int customerId = Integer.parseInt(req.getParameter("customerId"));
                Customer target = new CustomerDAO().findById(customerId);
                if (target == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JSON.writeValue(resp.getWriter(), Map.of("success", false, "error", "Không tìm thấy khách hàng."));
                    return;
                }
                boolean ok = customerService.updateOwnProfile(
                        target.getUserID(),
                        req.getParameter("customerName"),
                        req.getParameter("phone"),
                        req.getParameter("email"),
                        req.getParameter("address"));
                JSON.writeValue(resp.getWriter(), Map.of("success", ok,
                        "message", ok ? "Đã cập nhật thông tin khách hàng #" + customerId : "Không thể cập nhật (kiểm tra trùng SĐT/Email)."));
            } catch (RuntimeException ex) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSON.writeValue(resp.getWriter(), Map.of("success", false, "error", ex.getMessage()));
            }
            return;
        }

        Object id = req.getSession(false) == null ? null : req.getSession(false).getAttribute("userID");
        if (!(id instanceof Integer userId) || userId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if (!"CUSTOMER".equalsIgnoreCase(roleName)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            customerService.updateOwnProfile(userId, req.getParameter("customerName"), req.getParameter("phone"),
                    req.getParameter("email"), req.getParameter("address"));
            resp.sendRedirect(req.getContextPath() + "/customer/profile?updated=1");
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            try {
                Customer customer = customerService.getOwnProfile(userId);
                req.setAttribute("customer", customer);
                if (customer != null) {
                    req.setAttribute("customerOrders", new OrderDAO().findByCustomerId(customer.getCustomerID()));
                    req.setAttribute("customerPayments", new PaymentDAO().findDetailsByCustomerId(customer.getCustomerID()));
                    req.setAttribute("customerContracts", new ContractsDAO().findByCustomerId(customer.getCustomerID()));
                }
                req.getRequestDispatcher("/customer/my-account.jsp").forward(req, resp);
            } catch (RuntimeException loadError) {
                log("Could not reload customer profile", loadError);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải hồ sơ khách hàng.");
            }
        } catch (RuntimeException e) {
            log("Could not update customer profile", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể cập nhật hồ sơ khách hàng.");
        }
    }
}
