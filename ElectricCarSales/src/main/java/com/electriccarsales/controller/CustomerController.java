package com.electriccarsales.controller;

import com.electriccarsales.dao.ContractsDAO;
import com.electriccarsales.dao.CustomerDAO;
import com.electriccarsales.dao.OrderDAO;
import com.electriccarsales.dao.PaymentDAO;
import com.electriccarsales.model.Customer;
import com.electriccarsales.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.Part;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/customer/profile", "/customer/avatar"})
@MultipartConfig(maxFileSize = 2 * 1024 * 1024, maxRequestSize = 2 * 1024 * 1024)
public class CustomerController extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final CustomerService customerService = new CustomerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        if ("/customer/avatar".equals(req.getServletPath())) {
            Object avatarUserId = req.getSession(false) == null ? null : req.getSession(false).getAttribute("userID");
            if (!(avatarUserId instanceof Integer userId) || userId <= 0
                    || !"CUSTOMER".equalsIgnoreCase(String.valueOf(req.getSession(false).getAttribute("roleName")))) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            CustomerDAO.Avatar avatar;
            try {
                avatar = new CustomerDAO().findAvatarByUserId(userId);
            } catch (RuntimeException avatarError) {
                log("Customer avatar is not available", avatarError);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (avatar == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            resp.setContentType(avatar.contentType());
            resp.setHeader("Cache-Control", "private, no-store");
            resp.setContentLength(avatar.data().length);
            resp.getOutputStream().write(avatar.data());
            return;
        }
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
            String profileView = "profile".equals(req.getParameter("view"))
                    ? "/customer/my-profile.jsp" : "/customer/my-account.jsp";
            req.getRequestDispatcher(profileView).forward(req, resp);
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
            Part avatar = req.getPart("avatar");
            byte[] avatarData = null;
            String avatarType = null;
            if (avatar != null && avatar.getSize() > 0) {
                avatarData = readAvatar(avatar);
                avatarType = avatarContentType(avatarData);
                if (avatarType == null) throw new IllegalArgumentException("Ảnh đại diện chỉ nhận JPEG, PNG hoặc WebP (tối đa 2 MB).");
            }
            String customerName = req.getParameter("customerName");
            String phone = req.getParameter("phone");
            String email = req.getParameter("email");
            boolean updated = customerService.updateOwnProfile(userId, customerName, phone, email,
                    req.getParameter("address"));
            if (!updated) throw new IllegalArgumentException("Không thể cập nhật thông tin hồ sơ. Vui lòng kiểm tra lại dữ liệu.");
            if (avatarData != null && !new CustomerDAO().updateAvatar(userId, avatarData, avatarType)) {
                throw new IllegalArgumentException("Không tìm thấy hồ sơ để cập nhật ảnh đại diện.");
            }
            req.getSession(false).setAttribute("displayName", customerName.trim());
            req.getSession(false).setAttribute("phone", phone.trim());
            req.getSession(false).setAttribute("email", email.trim());
            String returnView = "profile".equals(req.getParameter("view")) ? "?view=profile" : "";
            resp.sendRedirect(req.getContextPath() + "/customer/profile" + returnView + (returnView.isEmpty() ? "?updated=1" : "&updated=1"));
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
                String profileView = "profile".equals(req.getParameter("view"))
                        ? "/customer/my-profile.jsp" : "/customer/my-account.jsp";
                req.getRequestDispatcher(profileView).forward(req, resp);
            } catch (RuntimeException loadError) {
                log("Could not reload customer profile", loadError);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải hồ sơ khách hàng.");
            }
        } catch (RuntimeException e) {
            log("Could not update customer profile", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể cập nhật hồ sơ khách hàng.");
        }
    }

    private static byte[] readAvatar(Part part) throws IOException {
        if (part.getSize() > 2L * 1024 * 1024) {
            throw new IllegalArgumentException("Ảnh đại diện không được vượt quá 2 MB.");
        }
        try (InputStream input = part.getInputStream()) {
            return input.readAllBytes();
        }
    }

    private static String avatarContentType(byte[] image) {
        if (image.length >= 3 && (image[0] & 0xff) == 0xff && (image[1] & 0xff) == 0xd8 && (image[2] & 0xff) == 0xff) return "image/jpeg";
        if (image.length >= 8 && image[0] == (byte) 0x89 && image[1] == 0x50 && image[2] == 0x4e && image[3] == 0x47
                && image[4] == 0x0d && image[5] == 0x0a && image[6] == 0x1a && image[7] == 0x0a) return "image/png";
        if (image.length >= 12 && image[0] == 'R' && image[1] == 'I' && image[2] == 'F' && image[3] == 'F'
                && image[8] == 'W' && image[9] == 'E' && image[10] == 'B' && image[11] == 'P') return "image/webp";
        return null;
    }
}
