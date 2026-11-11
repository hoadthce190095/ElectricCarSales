package com.electriccarsales.controller.order;

import com.electriccarsales.service.order.OrderService;
import com.electriccarsales.service.SalesOrderService;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.ServletException;
import com.electriccarsales.dao.FinanceCustomerDAO;
import com.electriccarsales.model.SessionUser;
import java.util.UUID;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "OrderController", urlPatterns = {"/orders"})
public class OrderController extends HttpServlet {
    private OrderService service = new OrderService();
    private final SalesOrderService salesOrders = new SalesOrderService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Object account = session == null ? null : session.getAttribute("user");
        if (!(account instanceof SessionUser user)) {
            response.sendError(403);
            return;
        }
        if ("SALES".equalsIgnoreCase(user.getRole())) {
            try {
                request.setAttribute("orderOptions", salesOrders.getOptions());
                if (session.getAttribute("csrfToken") == null) session.setAttribute("csrfToken", UUID.randomUUID().toString());
                request.setAttribute("paymentCsrf", session.getAttribute("csrfToken"));
                request.getRequestDispatcher("/staff/create-order.jsp").forward(request, response);
            } catch (SQLException e) {
                getServletContext().log("Cannot load Sales order options", e);
                response.sendError(503);
            }
            return;
        }
        if (!"CUSTOMER".equalsIgnoreCase(user.getRole())) {
            response.sendError(403);
            return;
        }
        try {
            Integer customerId = new FinanceCustomerDAO().findCustomerIdByUserId(user.getUserId());
            if (customerId == null) {
                response.sendError(403);
                return;
            }
            request.setAttribute("orders", service.getCustomerOrders(customerId));
            if (session.getAttribute("csrfToken") == null) {
                session.setAttribute("csrfToken", UUID.randomUUID().toString());
            }
            request.setAttribute("paymentCsrf", session.getAttribute("csrfToken"));
            request.getRequestDispatcher("/customer/my-orders.jsp").forward(request, response);
        } catch (RuntimeException ex) {
            getServletContext().log("Cannot load customer orders", ex);
            response.sendError(503, "Không thể tải đơn hàng của bạn.");
        }
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object account = session == null ? null : session.getAttribute("user");
        if (!(account instanceof SessionUser user) || !"SALES".equalsIgnoreCase(user.getRole())) {
            response.sendError(403);
            return;
        }
        Object expected = session.getAttribute("csrfToken");
        String actual = request.getParameter("csrfToken");
        if (!(expected instanceof String token) || actual == null ||
                !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(403, "Phiên thao tác không hợp lệ.");
            return;
        }
        try {
            int customer = Integer.parseInt(request.getParameter("customerId"));
            int car = Integer.parseInt(request.getParameter("carId"));
            String partnerValue = request.getParameter("companyId");
            Integer partner = partnerValue == null || partnerValue.isBlank() ? null : Integer.parseInt(partnerValue);
            int orderId = salesOrders.create(user.getUserId(), customer, car, request.getParameter("method"), partner);
            response.sendRedirect(request.getContextPath() + "/orders?created=" + orderId);
        } catch (SQLException e) {
            getServletContext().log("Sales order creation failed", e);
            response.sendError(503, "Lỗi Database khi tạo đơn.");
        } catch (SecurityException e) {
            response.sendError(403);
        } catch (NumberFormatException e) {
            response.sendError(400, "Nhập mã khách hàng, xe hoặc ngân hàng hợp lệ.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            response.sendError(409, e.getMessage());
        }
    }

}