package com.electriccarsales.controller;

import com.electriccarsales.model.Customer;
import com.electriccarsales.service.CustomerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/customer/profile")
public class CustomerController extends HttpServlet {
    private final CustomerService customerService = new CustomerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object id = req.getSession(false) == null ? null : req.getSession(false).getAttribute("userID");
        if (!(id instanceof Integer userId) || userId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if (!"CUSTOMER".equals(String.valueOf(req.getSession(false).getAttribute("roleName")))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            Customer customer = customerService.getOwnProfile(userId);
            if (customer == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy hồ sơ khách hàng.");
                return;
            }
            req.setAttribute("profileControllerRequest", Boolean.TRUE);
            if ("1".equals(req.getParameter("updated"))) req.setAttribute("success", "Hồ sơ đã được cập nhật.");
            req.setAttribute("customer", customer);
            req.getRequestDispatcher("/customer/my-account.jsp").forward(req, resp);
        } catch (RuntimeException e) {
            log("Could not load customer profile", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải hồ sơ khách hàng.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Object id = req.getSession(false) == null ? null : req.getSession(false).getAttribute("userID");
        if (!(id instanceof Integer userId) || userId <= 0) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if (!"CUSTOMER".equals(String.valueOf(req.getSession(false).getAttribute("roleName")))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            boolean updated = customerService.updateOwnProfile(userId, req.getParameter("customerName"), req.getParameter("phone"),
                    req.getParameter("email"), req.getParameter("address"));
            if (!updated) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Không thể cập nhật hồ sơ khách hàng.");
                return;
            }
            Customer persisted = customerService.getOwnProfile(userId);
            if (persisted != null) {
                req.getSession(false).setAttribute("email", persisted.getEmail());
                req.getSession(false).setAttribute("phone", persisted.getPhone());
            }
            resp.sendRedirect(req.getContextPath() + "/customer/profile?updated=1");
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            try {
                req.setAttribute("customer", customerService.getOwnProfile(userId));
                req.setAttribute("profileControllerRequest", Boolean.TRUE);
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
