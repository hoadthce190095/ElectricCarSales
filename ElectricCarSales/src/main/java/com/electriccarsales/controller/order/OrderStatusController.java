package com.electriccarsales.controller;

import com.electriccarsales.dao.CustomerDAO;
import com.electriccarsales.model.Customer;
import com.electriccarsales.model.Order;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.order.OrderService;
import com.electriccarsales.service.OrderStatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebServlet(name = "OrderStatusController", urlPatterns = {"/order-status"})
public class OrderStatusController extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final OrderStatusService service = new OrderStatusService();
    private final OrderService orderService = new OrderService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!com.electriccarsales.controller.PaymentController.validCsrf(request)) {
            response.sendError(403, "Phiên thao tác không hợp lệ.");
            return;
        }
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        boolean jsonRequested = "json".equalsIgnoreCase(request.getParameter("format"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
        Integer employeeId = (Integer) session.getAttribute("employeeId");
        if (employeeId == null && !"CUSTOMER".equals(role)) {
            try (var c = com.electriccarsales.dao.FinanceConnection.open();
                 var ps = c.prepareStatement("SELECT EmployeeID FROM Employees WHERE UserID = ?")) {
                ps.setInt(1, user.getUserId());
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        employeeId = rs.getInt(1);
                        session.setAttribute("employeeId", employeeId);
                    }
                }
            } catch (Exception ignored) {}
        }

        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            String status = request.getParameter("status");
            String note = request.getParameter("note");

            if ("CUSTOMER".equals(role)) {
                Customer customer = new CustomerDAO().findByUserId(user.getUserId());
                Order order = orderService.getOrderById(orderId);
                if (customer == null || order == null || order.getCustomerId() != customer.getCustomerID()) {
                    sendResponse(request, response, jsonRequested, HttpServletResponse.SC_FORBIDDEN, false,
                            "Bạn không có quyền thao tác trên đơn hàng này.", "/orders");
                    return;
                }
                if (!"CANCELLED".equalsIgnoreCase(status) || !"PENDING".equalsIgnoreCase(order.getStatus())) {
                    sendResponse(request, response, jsonRequested, HttpServletResponse.SC_BAD_REQUEST, false,
                            "Khách hàng chỉ có thể hủy đơn hàng khi đơn đang ở trạng thái chờ đặt cọc (PENDING).", "/orders");
                    return;
                }
                service.updateOrderStatus(orderId, "CANCELLED", null, "Khách hàng yêu cầu hủy đơn khi chưa đặt cọc.");
                sendResponse(request, response, jsonRequested, HttpServletResponse.SC_OK, true,
                        "Đã hủy đơn hàng #" + orderId + ".", "/orders?cancelled=1");
                return;
            }

            if (!List.of("SALES", "OPERATIONS", "MANAGER", "ADMIN").contains(role)) {
                sendResponse(request, response, jsonRequested, HttpServletResponse.SC_FORBIDDEN, false,
                        "Bạn không có quyền cập nhật trạng thái đơn hàng.", "/staff/dashboard#transactions");
                return;
            }

            if ("CANCELLED".equalsIgnoreCase(status)) {
                Order order = orderService.getOrderById(orderId);
                if (order == null) {
                    sendResponse(request, response, jsonRequested, HttpServletResponse.SC_NOT_FOUND, false,
                            "Không tìm thấy đơn hàng #" + orderId + ".", "/staff/dashboard#transactions");
                    return;
                }
                if (order.getPaidAmount().signum() > 0
                        || (!"PENDING".equalsIgnoreCase(order.getStatus()) && !"RESERVED".equalsIgnoreCase(order.getStatus()))) {
                    sendResponse(request, response, jsonRequested, HttpServletResponse.SC_BAD_REQUEST, false,
                            "Chỉ có thể hủy đơn PENDING/RESERVED chưa thanh toán. Trạng thái hiện tại: " + order.getStatus(),
                            "/staff/dashboard#transactions");
                    return;
                }
                if (note == null || note.isBlank()) {
                    sendResponse(request, response, jsonRequested, HttpServletResponse.SC_BAD_REQUEST, false,
                            "Vui lòng nhập rõ lý do hủy đơn hàng.",
                            "/staff/dashboard#transactions");
                    return;
                }
                String cancelNote = note.trim();
                service.updateOrderStatus(orderId, "CANCELLED", employeeId, cancelNote);
                sendResponse(request, response, jsonRequested, HttpServletResponse.SC_OK, true,
                        "Đã hủy đơn hàng #" + orderId + " thành công và giải phóng xe về kho khả dụng.", "/staff/dashboard#transactions");
                return;
            }

            service.updateOrderStatus(orderId, status, employeeId, note);
            sendResponse(request, response, jsonRequested, HttpServletResponse.SC_OK, true,
                    "Đã cập nhật trạng thái đơn hàng #" + orderId + " sang " + status + ".", "/staff/dashboard#transactions");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            sendResponse(request, response, jsonRequested, HttpServletResponse.SC_BAD_REQUEST, false,
                    ex.getMessage(), "CUSTOMER".equals(role) ? "/orders" : "/staff/dashboard#transactions");
        } catch (RuntimeException ex) {
            log("Update order status failed", ex);
            sendResponse(request, response, jsonRequested, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Không thể cập nhật trạng thái đơn hàng.", "/orders");
        }
    }

    private void sendResponse(HttpServletRequest req, HttpServletResponse resp, boolean jsonRequested,
                              int httpStatus, boolean success, String message, String redirectPath) throws IOException {
        if (jsonRequested) {
            resp.setStatus(httpStatus);
            resp.setContentType("application/json;charset=UTF-8");
            JSON.writeValue(resp.getWriter(), Map.of("success", success, "message", message));
            return;
        }
        resp.sendRedirect(req.getContextPath() + redirectPath);
    }
}
