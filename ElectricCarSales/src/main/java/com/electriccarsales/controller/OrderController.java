package com.electriccarsales.controller;

import com.electriccarsales.dao.CustomerDAO;
import com.electriccarsales.dao.OrderDetailDAO;
import com.electriccarsales.dao.OrderStatusHistoryDAO;
import com.electriccarsales.model.Customer;
import com.electriccarsales.model.Order;
import com.electriccarsales.model.OrderDetail;
import com.electriccarsales.model.OrderStatus;
import com.electriccarsales.model.OrderStatusHistory;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.ConsultationService;
import com.electriccarsales.service.OrderService;
import com.electriccarsales.service.OrderStatusService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "OrderController", urlPatterns = {
        "/orders",
        "/staff/transactions",
        "/staff/transaction/create",
        "/staff/transaction-detail",
        "/staff/transaction/detail",
        "/order/detail",
        "/order/create"
})
public class OrderController extends HttpServlet {
    private final OrderService service = new OrderService();
    private final OrderStatusService orderStatusService = new OrderStatusService();
    private final ConsultationService consultationService = new ConsultationService();
    private final OrderDetailDAO detailDAO = new OrderDetailDAO();
    private final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        List<OrderStatus> statusList = orderStatusService.getAllStatuses();
        request.setAttribute("statusList", statusList);
        request.setAttribute("orderControllerLoaded", Boolean.TRUE);

        if ("/staff/transactions".equals(path)) {
            List<Order> allOrders = service.getAllOrders();
            String statusFilter = request.getParameter("status");
            String keyword = request.getParameter("q");
            List<Order> filteredOrders = new ArrayList<>();

            int depositedCount = 0;
            int processingCount = 0;
            int completedCount = 0;
            BigDecimal totalRevenue = BigDecimal.ZERO;

            for (Order o : allOrders) {
                String st = o.getStatus();
                if ("2".equals(st) || "3".equals(st)) {
                    depositedCount++;
                } else if ("1".equals(st) || "4".equals(st) || "5".equals(st)) {
                    processingCount++;
                } else if ("6".equals(st)) {
                    completedCount++;
                }

                if (!"7".equals(st) && o.getOriginalAmount() != null) {
                    totalRevenue = totalRevenue.add(o.getOriginalAmount());
                }

                boolean matchStatus = (statusFilter == null || statusFilter.isBlank() || statusFilter.equals(st));
                boolean matchKeyword = true;
                if (keyword != null && !keyword.isBlank()) {
                    String kw = keyword.trim().toLowerCase();
                    String idStr = String.valueOf(o.getId());
                    String cName = o.getCustomerName() != null ? o.getCustomerName().toLowerCase() : "";
                    String cPhone = o.getCustomerPhone() != null ? o.getCustomerPhone().toLowerCase() : "";
                    String vName = o.getVehicleName() != null ? o.getVehicleName().toLowerCase() : "";
                    String vin = o.getVinNumber() != null ? o.getVinNumber().toLowerCase() : "";
                    matchKeyword = idStr.contains(kw) || cName.contains(kw) || cPhone.contains(kw) || vName.contains(kw) || vin.contains(kw);
                }
                if (matchStatus && matchKeyword) {
                    filteredOrders.add(o);
                }
            }

            request.setAttribute("orders", filteredOrders);
            request.setAttribute("orderList", filteredOrders);
            request.setAttribute("totalOrdersCount", allOrders.size());
            request.setAttribute("totalOrderCount", allOrders.size());
            request.setAttribute("depositedCount", depositedCount);
            request.setAttribute("processingCount", processingCount);
            request.setAttribute("completedCount", completedCount);
            request.setAttribute("totalRevenue", totalRevenue);
            request.setAttribute("selectedStatus", statusFilter);
            request.setAttribute("searchKeyword", keyword);
            request.setAttribute("availableCars", service.getAvailableCarsForSale());
            request.setAttribute("carModelsWithPrices", service.getCarModelsWithPrices());
            request.setAttribute("carModels", service.getCarModelsWithPrices());
            request.setAttribute("paymentMethods", service.getPaymentMethods());
            request.setAttribute("employees", service.getEmployees());
            request.setAttribute("employeeList", service.getEmployees());
            request.setAttribute("customerList", loadCustomersSafely());
            request.getRequestDispatcher("/staff/transaction-management.jsp").forward(request, response);
        } else if ("/staff/transaction-detail".equals(path) || "/staff/transaction/detail".equals(path) || "/order/detail".equals(path)) {
            String idParam = request.getParameter("id");
            int orderId = parseInt(idParam, 0);
            Order order = orderId > 0 ? service.getOrderById(orderId) : null;
            if (order == null) {
                List<Order> all = service.getAllOrders();
                if (all != null && !all.isEmpty()) {
                    order = all.get(0);
                    orderId = order.getId();
                }
            }
            List<OrderDetail> details = orderId > 0 ? detailDAO.findByOrderId(orderId) : Collections.emptyList();
            List<OrderStatusHistory> history = orderId > 0 ? historyDAO.findByOrderId(orderId) : Collections.emptyList();

            request.setAttribute("order", order);
            request.setAttribute("details", details);
            request.setAttribute("orderDetails", details);
            request.setAttribute("history", history);
            request.setAttribute("statusHistory", history);
            request.setAttribute("employees", service.getEmployees());
            request.setAttribute("employeeList", service.getEmployees());
            request.getRequestDispatcher("/staff/transaction-detail.jsp").forward(request, response);
        } else if ("/orders".equals(path)) {
            List<Order> myOrders = resolveCustomerOrders(request);
            request.setAttribute("orders", myOrders);
            request.setAttribute("myOrders", myOrders);
            request.getRequestDispatcher("/customer/my-orders.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String path = request.getServletPath();

        if ("/staff/transactions".equals(path) || "/staff/transaction/create".equals(path) || "/order/create".equals(path)) {
            try {
                int customerId = parseInt(request.getParameter("customerId"), 0);
                String customerName = request.getParameter("customerName");
                String customerPhone = request.getParameter("customerPhone");
                String customerEmail = request.getParameter("customerEmail");
                int carId = parseInt(request.getParameter("carId"), 0);
                int modelId = parseInt(request.getParameter("modelId"), 0);
                String vehicleName = request.getParameter("vehicleName");
                String vehicleColor = request.getParameter("vehicleColor");
                int paymentMethodId = parseInt(request.getParameter("paymentMethodId"), 1);
                int statusId = parseInt(request.getParameter("statusId"), 0);
                Integer employeeId = parseInt(request.getParameter("employeeId"), 0) > 0
                        ? parseInt(request.getParameter("employeeId"), 0)
                        : null;

                String amtStr = request.getParameter("amount");
                if (amtStr == null || amtStr.isBlank()) {
                    amtStr = request.getParameter("totalAmount");
                }
                BigDecimal totalAmount = parseBigDecimal(amtStr);
                BigDecimal depositAmount = parseBigDecimal(request.getParameter("depositAmount"));
                String note = request.getParameter("note");
                int consultationId = parseInt(request.getParameter("consultationId"), 0);

                Order order = new Order();
                order.setCustomerId(customerId);
                order.setCustomerName(customerName);
                order.setCustomerPhone(customerPhone);
                order.setCustomerEmail(customerEmail);
                order.setPaymentMethodId(paymentMethodId);
                order.setVehicleName(vehicleName);
                order.setVehicleColor(vehicleColor);
                if (statusId > 0) {
                    order.setOrderStatusId(statusId);
                    order.setStatus(String.valueOf(statusId));
                }
                order.setOriginalAmount(totalAmount);
                order.setDepositAmount(depositAmount);
                order.setNote(note);

                int newOrderId = service.createSalesTransaction(order, carId, modelId, totalAmount, employeeId);
                if (consultationId > 0) {
                    consultationService.updateRequestStatus(consultationId, 3, employeeId); // 3 = COMPLETED
                }
                if (newOrderId > 0) {
                    response.sendRedirect(request.getContextPath() + "/staff/transactions?created=true&newOrderId=" + newOrderId);
                } else {
                    response.sendRedirect(request.getContextPath() + "/staff/transactions?error=true");
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/staff/transactions?error=true");
            }
        }
    }

    private List<Order> resolveCustomerOrders(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") instanceof SessionUser sessionUser) {
            try (CustomerDAO customerDAO = new CustomerDAO()) {
                Customer customer = customerDAO.findByUserId(sessionUser.getUserId());
                if (customer != null) {
                    return service.getOrdersByCustomerId(customer.getCustomerID());
                }
            } catch (Exception ignored) {
            }
        }
        return service.getAllOrders();
    }

    private List<Customer> loadCustomersSafely() {
        try (CustomerDAO customerDAO = new CustomerDAO()) {
            return customerDAO.findAll();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private int parseInt(String val, int fallback) {
        if (val == null || val.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private BigDecimal parseBigDecimal(String val) {
        if (val == null || val.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            String cleaned = val.replaceAll("[^0-9.]", "");
            return cleaned.isEmpty() ? BigDecimal.ZERO : new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
