package com.electriccarsales.controller.order;

import com.electriccarsales.dao.FinanceCustomerDAO;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.SalesOrderService;
import com.electriccarsales.service.order.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

@WebServlet(name = "OrderController", urlPatterns = {"/orders", "/order"})
public class OrderController extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final java.util.Set<String> STAFF_ROLES =
            java.util.Set.of("SALES", "ACCOUNTANT", "OPERATIONS", "MANAGER", "ADMIN");
    private final OrderService service = new OrderService();
    private final SalesOrderService salesOrders = new SalesOrderService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        boolean jsonRequested = jsonRequested(request);
        HttpSession session = request.getSession(false);
        Object account = session == null ? null : session.getAttribute("user");
        if (!(account instanceof SessionUser user)) {
            respondError(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Vui lòng đăng nhập.");
            return;
        }
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase(java.util.Locale.ROOT);
        if (STAFF_ROLES.contains(role)) {
            try {
                if (jsonRequested) {
                    Map<String, Object> payload = salesOrders.dashboardData();
                    if ("SALES".equals(role)) {
                        payload.put("csrfToken", ensureCsrf(session));
                    } else {
                        payload.remove("customers");
                        payload.remove("cars");
                        payload.remove("paymentMethods");
                        payload.remove("financialCompanies");
                    }
                    response.setContentType("application/json;charset=UTF-8");
                    JSON.writeValue(response.getWriter(), payload);
                    return;
                }
                if (!"SALES".equals(role)) {
                    response.sendRedirect(request.getContextPath() + "/staff/dashboard#transactions");
                    return;
                }
                String csrfToken = ensureCsrf(session);
                request.setAttribute("orderOptions", salesOrders.getOptions());
                request.setAttribute("paymentCsrf", csrfToken);
                request.getRequestDispatcher("/staff/create-order.jsp").forward(request, response);
            } catch (SQLException e) {
                getServletContext().log("Cannot load Sales order data", e);
                respondError(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "Không thể tải đơn hàng và xe sẵn có.");
            } catch (RuntimeException e) {
                getServletContext().log("Cannot prepare Sales order data", e);
                respondError(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "Không thể tải dữ liệu đơn hàng. Kiểm tra kết nối Database và cấu hình schema.");
            }
            return;
        }
        if (!"CUSTOMER".equalsIgnoreCase(user.getRole())) {
            respondError(request, response, HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền xem đơn hàng.");
            return;
        }
        if (jsonRequested) {
            respondError(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "API danh sách này chỉ dành cho Sales; Customer dùng trang đơn hàng cá nhân.");
            return;
        }
        try {
            Integer customerId = new FinanceCustomerDAO().findCustomerIdByUserId(user.getUserId());
            if (customerId == null) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            request.setAttribute("orders", service.getCustomerOrders(customerId));
            String csrfToken = ensureCsrf(session);
            request.setAttribute("paymentCsrf", csrfToken);
            request.getRequestDispatcher("/customer/my-orders.jsp").forward(request, response);
        } catch (RuntimeException ex) {
            getServletContext().log("Cannot load customer orders", ex);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Không thể tải đơn hàng của bạn.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object account = session == null ? null : session.getAttribute("user");
        if (!(account instanceof SessionUser user) || !"SALES".equalsIgnoreCase(user.getRole())) {
            respondError(request, response, HttpServletResponse.SC_FORBIDDEN, "Chỉ Sales Staff được lập đơn hàng.");
            return;
        }
        Object expected = session.getAttribute("csrfToken");
        String actual = request.getParameter("csrfToken");
        if (!(expected instanceof String token) || actual == null || !MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
            respondError(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "Phiên thao tác không hợp lệ. Tải lại danh sách đơn hàng.");
            return;
        }
        try {
            int customer = Integer.parseInt(request.getParameter("customerId"));
            int car = Integer.parseInt(request.getParameter("carId"));
            String partnerValue = request.getParameter("companyId");
            Integer partner = partnerValue == null || partnerValue.isBlank() ? null : Integer.parseInt(partnerValue);
            int orderId = salesOrders.create(user.getUserId(), customer, car, request.getParameter("method"), partner);
            if (jsonRequested(request)) {
                response.setStatus(HttpServletResponse.SC_CREATED);
                response.setContentType("application/json;charset=UTF-8");
                JSON.writeValue(response.getWriter(), Map.of(
                        "success", true, "orderId", orderId, "message", "Đã tạo đơn hàng #" + orderId + "."));
                return;
            }
            response.sendRedirect(request.getContextPath() + "/orders?created=" + orderId);
        } catch (SQLException e) {
            getServletContext().log("Sales order creation failed", e);
            respondError(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Lỗi Database khi tạo đơn.");
        } catch (SecurityException e) {
            respondError(request, response, HttpServletResponse.SC_FORBIDDEN, "Tài khoản chưa có quyền lập đơn.");
        } catch (NumberFormatException e) {
            respondError(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    "Nhập mã khách hàng, xe hoặc ngân hàng hợp lệ.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            respondError(request, response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        }
    }

    private static boolean jsonRequested(HttpServletRequest request) {
        return "json".equalsIgnoreCase(request.getParameter("format"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));
    }

    private static String ensureCsrf(HttpSession session) {
        Object current = session.getAttribute("csrfToken");
        if (current instanceof String value && !value.isBlank()) return value;
        String value = UUID.randomUUID().toString();
        session.setAttribute("csrfToken", value);
        return value;
    }

    private static void respondError(HttpServletRequest request, HttpServletResponse response, int status, String message)
            throws IOException {
        if (jsonRequested(request)) {
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            JSON.writeValue(response.getWriter(), Map.of("success", false, "error", message));
        } else {
            response.sendError(status, message);
        }
    }
}
