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
            customerService.updateOwnProfile(userId, req.getParameter("customerName"), req.getParameter("phone"),
                    req.getParameter("email"), req.getParameter("address"));
            resp.sendRedirect(req.getContextPath() + "/customer/profile?updated=1");
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            try {
                req.setAttribute("customer", customerService.getOwnProfile(userId));
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
