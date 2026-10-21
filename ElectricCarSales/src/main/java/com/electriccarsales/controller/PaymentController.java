package com.electriccarsales.controller;

import com.electriccarsales.dao.FinanceDAOException;
import com.electriccarsales.service.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Payment history and balance endpoints. Checkout and callbacks require a configured provider adapter. */
@WebServlet("/payment/*")
public class PaymentController extends HttpServlet {
    private PaymentService paymentService;

    public PaymentController() {
        this(new PaymentService());
    }

    PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public void init() {
        if (paymentService == null) paymentService = new PaymentService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer customerId = FinanceControllerSupport.customerId(request, response);
        if (customerId == null) return;

        try {
            String action = action(request);
            Integer orderId = FinanceControllerSupport.positiveInt(request.getParameter("orderId"));
            if ("status".equals(action)) {
                Integer paymentId = FinanceControllerSupport.positiveInt(request.getParameter("paymentId"));
                if (paymentId != null) {
                    var payment = paymentService.getPayment(paymentId);
                    if (payment == null || !paymentService.orderBelongsToCustomer(payment.getOrderID(), customerId)) {
                        FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                                "Không tìm thấy giao dịch thanh toán.");
                        return;
                    }
                    request.setAttribute("selectedPayment", payment);
                    orderId = payment.getOrderID();
                }
            }
            if ("detail".equals(action) || "status".equals(action) || ("history".equals(action) && orderId != null)) {
                if (orderId == null) {
                    FinanceControllerSupport.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                            "Thiếu mã giao dịch hoặc đơn hàng hợp lệ.");
                    return;
                }
                if (!paymentService.orderBelongsToCustomer(orderId, customerId)) {
                    FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                            "Không tìm thấy đơn hàng.");
                    return;
                }
                request.setAttribute("orderBalance", paymentService.getOrderBalance(orderId));
                request.setAttribute("outstandingDebt", paymentService.getOutstandingDebt(orderId));
                request.setAttribute("orderPayments", paymentService.getOrderHistory(orderId));
                request.setAttribute("requestedOrderId", orderId);
            } else if (!"history".equals(action) && !"".equals(action)) {
                FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                        "Chức năng thanh toán không tồn tại.");
                return;
            }

            request.setAttribute("paymentHistory", paymentService.getCustomerPaymentHistory(customerId));
            request.setAttribute("paymentTestMode", paymentService.isTestMode());
            request.getRequestDispatcher("/customer/my-payments.jsp").forward(request, response);
        } catch (IllegalArgumentException e) {
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Thông tin thanh toán không hợp lệ.");
        } catch (IllegalStateException e) {
            getServletContext().log("Order payment balance is inconsistent.", e);
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_CONFLICT,
                    "Dữ liệu công nợ chưa nhất quán; vui lòng liên hệ nhân viên hỗ trợ.");
        } catch (FinanceDAOException e) {
            getServletContext().log("Payment query failed.", e);
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Chưa thể tải thông tin thanh toán. Vui lòng thử lại sau.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = action(request);
        if ("callback".equals(action)) {
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_IMPLEMENTED,
                    "Chưa cấu hình adapter cổng để xác minh thông báo giao dịch.");
            return;
        }
        Integer customerId = FinanceControllerSupport.customerId(request, response);
        if (customerId == null) return;
        if ("confirm".equals(action)) {
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_IMPLEMENTED,
                    "Không thể xác nhận thanh toán từ trình duyệt khách hàng.");
            return;
        }
        if (!"create".equals(action)) {
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                    "Chức năng thanh toán không tồn tại.");
            return;
        }
        Integer orderId = FinanceControllerSupport.positiveInt(request.getParameter("orderId"));
        if (orderId == null) {
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Thiếu mã đơn hàng hợp lệ.");
            return;
        }
        try {
            if (!paymentService.orderBelongsToCustomer(orderId, customerId)) {
                FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                        "Không tìm thấy đơn hàng.");
                return;
            }
            // Do not create a pending row until provider and provider-specific amount rules are configured.
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Chưa cấu hình cổng thanh toán điện tử; chưa tạo yêu cầu hoặc ghi nhận giao dịch.");
        } catch (FinanceDAOException e) {
            getServletContext().log("Payment authorization query failed.", e);
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Chưa thể xác minh đơn hàng. Vui lòng thử lại sau.");
        }
    }

    private static String action(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.equals("/")) return "";
        return path.substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}
