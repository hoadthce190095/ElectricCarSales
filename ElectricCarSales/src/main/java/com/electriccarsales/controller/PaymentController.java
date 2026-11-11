package com.electriccarsales.controller;

import com.electriccarsales.dao.FinanceDAOException;
import com.electriccarsales.service.PaymentService;
import com.electriccarsales.service.BankTransferService;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.UUID;
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
    private final BankTransferService bankTransferService = new BankTransferService();

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
            if ("qr".equals(action) || "check".equals(action)) {
                Integer paymentId = FinanceControllerSupport.positiveInt(request.getParameter("paymentId"));
                BankTransferService.Transfer transfer = paymentId == null ? null
                        : bankTransferService.findCustomerTransfer(paymentId, customerId);
                if (transfer == null) {
                    FinanceControllerSupport.sendError(request, response, 404, "Không tìm thấy giao dịch của bạn.");
                    return;
                }
                response.setHeader("Cache-Control", "no-store");
                if ("check".equals(action)) {
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":\"" + transfer.status() + "\"}");
                } else {
                    request.setAttribute("transfer", transfer);
                    request.getRequestDispatcher("/customer/bank-transfer.jsp").forward(request, response);
                }
                return;
            }
            String token = ensureCsrf(request);
            request.setAttribute("paymentCsrf", token);
            Integer orderId = FinanceControllerSupport.positiveInt(request.getParameter("orderId"));
            if ("status".equals(action)) {
                Integer paymentId = FinanceControllerSupport.positiveInt(request.getParameter("paymentId"));
                if (paymentId != null) {
                    var payment = paymentService.getPayment(paymentId);
                    if (payment == null || !paymentService.orderBelongsToCustomer(payment.getOrderID(), customerId)) {
                        FinanceControllerSupport.sendError(request, response, HttpServletResponse.SC_NOT_FOUND,
                                "Không tìm thấy giao dịch thanh toán.");
                        return;
                    }
                    request.setAttribute("selectedPayment", payment);
                    orderId = payment.getOrderID();
                }
            }
            if ("detail".equals(action) || "status".equals(action) || ("history".equals(action) && orderId != null)) {
                if (orderId == null) {
                    FinanceControllerSupport.sendError(request, response, HttpServletResponse.SC_BAD_REQUEST,
                            "Thiếu mã giao dịch hoặc đơn hàng hợp lệ.");
                    return;
                }
                if (!paymentService.orderBelongsToCustomer(orderId, customerId)) {
                    FinanceControllerSupport.sendError(request, response, HttpServletResponse.SC_NOT_FOUND,
                            "Không tìm thấy đơn hàng.");
                    return;
                }
                request.setAttribute("orderBalance", paymentService.getOrderBalance(orderId));
                request.setAttribute("outstandingDebt", paymentService.getOutstandingDebt(orderId));
                request.setAttribute("orderPayments", paymentService.getOrderHistory(orderId));
                request.setAttribute("requestedOrderId", orderId);
            } else if (!"history".equals(action) && !"".equals(action)) {
                FinanceControllerSupport.sendError(request, response, HttpServletResponse.SC_NOT_FOUND,
                        "Chức năng thanh toán không tồn tại.");
                return;
            }

            request.setAttribute("paymentHistory", paymentService.getCustomerPaymentHistory(customerId));
            request.setAttribute("paymentTestMode", paymentService.isTestMode());
            request.getRequestDispatcher("/customer/my-payments.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Cannot read bank transfer", e);
            FinanceControllerSupport.sendError(request, response, 503, "Không thể truy cập dữ liệu chuyển khoản.");
        } catch (IllegalArgumentException e) {
            FinanceControllerSupport.sendError(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    "Thông tin thanh toán không hợp lệ.");
        } catch (IllegalStateException e) {
            getServletContext().log("Order payment balance is inconsistent.", e);
            FinanceControllerSupport.sendError(request, response, HttpServletResponse.SC_CONFLICT,
                    "Dữ liệu công nợ chưa nhất quán; vui lòng liên hệ nhân viên hỗ trợ.");
        } catch (FinanceDAOException e) {
            getServletContext().log("Payment query failed.", e);
            FinanceControllerSupport.sendError(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Chưa thể tải thông tin thanh toán. Vui lòng thử lại sau.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = action(request);
        if ("callback".equals(action)) {
            FinanceControllerSupport.sendError(request, response, HttpServletResponse.SC_NOT_IMPLEMENTED,
                    "Chưa cấu hình adapter cổng để xác minh thông báo giao dịch.");
            return;
        }
        Integer customerId = FinanceControllerSupport.customerId(request, response);
        if (customerId == null) return;
        if ("confirm".equals(action)) {
            FinanceControllerSupport.sendError(request, response, HttpServletResponse.SC_NOT_IMPLEMENTED,
                    "Không thể xác nhận thanh toán từ trình duyệt khách hàng.");
            return;
        }
        if (!"create".equals(action)) {
            FinanceControllerSupport.sendError(request, response, HttpServletResponse.SC_NOT_FOUND,
                    "Chức năng thanh toán không tồn tại.");
            return;
        }
        Integer orderId = FinanceControllerSupport.positiveInt(request.getParameter("orderId"));
        if (orderId == null) {
            FinanceControllerSupport.sendError(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    "Thiếu mã đơn hàng hợp lệ.");
            return;
        }
        if (!validCsrf(request)) {
            FinanceControllerSupport.sendError(request, response, 403, "Phiên xác nhận không hợp lệ. Mở lại trang thanh toán.");
            return;
        }
        try {
            BankTransferService.Transfer transfer = bankTransferService.createForCustomer(
                    orderId, customerId, request.getParameter("type"));
            request.setAttribute("transfer", transfer);
            request.getRequestDispatcher("/customer/bank-transfer.jsp").forward(request, response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            FinanceControllerSupport.sendError(request, response, 409, e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Bank transfer creation failed", e);
            FinanceControllerSupport.sendError(request, response, 503, "Không thể tạo yêu cầu chuyển khoản.");
        }
    }

    static String ensureCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Object token = session.getAttribute("csrfToken");
        if (!(token instanceof String value) || value.isBlank()) {
            String freshToken = UUID.randomUUID().toString();
            session.setAttribute("csrfToken", freshToken);
            return freshToken;
        }
        return (String) token;
    }

    static boolean validCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute("csrfToken");
        String actual = request.getParameter("csrfToken");
        return expected != null && actual != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String action(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.equals("/")) return "";
        return path.substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}
