package com.electriccarsales.controller;

import com.electriccarsales.dao.OrderDetailDAO;
import com.electriccarsales.dao.OrderStatusHistoryDAO;
import com.electriccarsales.model.Order;
import com.electriccarsales.model.OrderDetail;
import com.electriccarsales.model.OrderStatusHistory;
import com.electriccarsales.service.OrderService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "OrderController", urlPatterns = {
    "/orders",
    "/staff/transactions",
    "/staff/transaction-detail",
    "/order/detail"
})
public class OrderController extends HttpServlet {
    private OrderService service = new OrderService();
    private OrderDetailDAO detailDAO = new OrderDetailDAO();
    private OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/staff/transactions".equals(path)) {
            List<Order> list = service.getAllOrders();
            request.setAttribute("orderList", list);
            request.getRequestDispatcher("/staff/transaction-management.jsp").forward(request, response);
        } else if ("/staff/transaction-detail".equals(path) || "/order/detail".equals(path)) {
            try {
                String idStr = request.getParameter("id");
                int orderId = (idStr != null && !idStr.isEmpty()) ? Integer.parseInt(idStr) : 1;
                Order order = service.getOrderById(orderId);
                List<OrderDetail> details = detailDAO.findByOrderId(orderId);
                List<OrderStatusHistory> history = historyDAO.findByOrderId(orderId);

                request.setAttribute("order", order);
                request.setAttribute("orderDetails", details);
                request.setAttribute("statusHistory", history);
                request.getRequestDispatcher("/staff/transaction-detail.jsp").forward(request, response);
            } catch (Exception e) {
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/staff/transactions");
            }
        } else {
            request.setAttribute("orders", service.getAllOrders());
            request.getRequestDispatcher("customer/my-orders.jsp").forward(request, response);
        }
    }
}