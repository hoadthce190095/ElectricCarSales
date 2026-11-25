package com.electriccarsales.controller;

import com.electriccarsales.dao.FinanceDAOException;
import com.electriccarsales.dao.ContractsDAO;
import com.electriccarsales.dao.InvoiceDAO;
import com.electriccarsales.dao.order.OrderDAO;
import com.electriccarsales.model.Order;
import com.electriccarsales.service.BankTransferService;
import com.electriccarsales.service.OrderExpirationService;
import com.electriccarsales.service.PaymentService;
import com.electriccarsales.service.SePayService;
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
import java.util.List;
import java.util.UUID;

/**
 * Quản lý các yêu cầu thanh toán của khách hàng: tạo giao dịch VietQR SePay + MB Bank,
 * chuyển tiền mặt tại showroom, lịch sử và đối soát công nợ.
 */
@WebServlet("/payment/*")
public class PaymentController extends HttpServlet {
    private PaymentService paymentService;
    private final BankTransferService bankTransferService = new BankTransferService();
    private final OrderExpirationService orderExpirationService = new OrderExpirationService();

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
        String action = action(request);

        Integer customerId = FinanceControllerSupport.customerId(request, response);
        if (customerId == null) return;

        try {
            if ("check".equals(action)) {
                Integer paymentId = FinanceControllerSupport.positiveInt(request.getParameter("paymentId"));
                if (paymentId != null) {
                    orderExpirationService.expireOrderIfOverdueForPayment(paymentId);
                }
                bankTransferService.syncCustomerReportedTransfers(customerId);
                BankTransferService.Transfer transfer = paymentId == null ? null
                        : bankTransferService.checkAndAutoCompleteTransfer(paymentId, customerId);
                if (transfer == null) {
                    FinanceControllerSupport.sendError(request, response, 404, "Không tìm thấy giao dịch của bạn.");
                    return;
                }
                response.setHeader("Cache-Control", "no-store");
                response.setContentType("application/json;charset=UTF-8");
                String transferStatus = transfer.isOrderCancelled() ? "CANCELLED" : transfer.status();
                response.getWriter().write("{\"status\":\"" + transferStatus
                        + "\",\"bankReported\":" + (!transfer.isOrderCancelled() && transfer.bankReported())
                        + ",\"orderStatus\":\"" + (transfer.orderStatus() == null ? "" : transfer.orderStatus()) + "\""
                        + ",\"orderId\":" + transfer.orderId() + "}");
                return;
            }

            String token = ensureCsrf(request);
            request.setAttribute("paymentCsrf", token);
            Integer orderId = FinanceControllerSupport.positiveInt(request.getParameter("orderId"));
            if (orderId != null) {
                orderExpirationService.expireOrderIfOverdue(orderId);
            }

            bankTransferService.syncCustomerReportedTransfers(customerId);

            if ("transfer".equals(action)) {
                Integer requestedPaymentId = FinanceControllerSupport.positiveInt(request.getParameter("paymentId"));
                if (requestedPaymentId != null && orderId == null) {
                    orderExpirationService.expireOrderIfOverdueForPayment(requestedPaymentId);
                }
                String requestedType = request.getParameter("type");
                BankTransferService.Transfer transfer = null;
                if (requestedPaymentId != null) {
                    transfer = bankTransferService.findCustomerTransfer(requestedPaymentId, customerId);
                    if (transfer != null) {
                        orderId = transfer.orderId();
                    }
                } else if (orderId != null) {
                    transfer = bankTransferService.findPendingCustomerTransferByOrder(orderId, customerId, requestedType);
                }

                if (transfer != null && transfer.isOrderCancelled()) {
                    Order cancelledOrder = new OrderDAO().findById(transfer.orderId());
                    request.setAttribute("sepayError", "Đơn hàng đã bị hủy. Bạn không thể tạo hoặc quét mã thanh toán cho đơn này.");
                    if (cancelledOrder != null) {
                        request.setAttribute("cancelReason", cancelledOrder.getCancelReason());
                    }
                    request.setAttribute("orderId", transfer.orderId());
                    request.getRequestDispatcher("/customer/bank-transfer.jsp").forward(request, response);
                    return;
                }

                if (transfer == null && orderId != null) {
                    Order existingOrder = new OrderDAO().findById(orderId);
                    if (existingOrder != null && existingOrder.getCustomerId() == customerId
                            && "CANCELLED".equalsIgnoreCase(existingOrder.getStatus())) {
                        request.setAttribute("sepayError", "Đơn hàng đã bị hủy. Bạn không thể tạo hoặc quét mã thanh toán cho đơn này.");
                        request.setAttribute("cancelReason", existingOrder.getCancelReason());
                        request.setAttribute("orderId", orderId);
                        request.setAttribute("type", requestedType != null ? requestedType : "DEPOSIT");
                        request.getRequestDispatcher("/customer/bank-transfer.jsp").forward(request, response);
                        return;
                    }
                    try {
                        transfer = bankTransferService.createForCustomer(
                                orderId, customerId, requestedType != null ? requestedType : "DEPOSIT", "BANK_TRANSFER");
                    } catch (Exception ex) {
                        request.setAttribute("sepayError", ex.getMessage());
                        request.setAttribute("orderId", orderId);
                        request.setAttribute("type", requestedType != null ? requestedType : "DEPOSIT");
                        request.getRequestDispatcher("/customer/bank-transfer.jsp").forward(request, response);
                        return;
                    }
                } else if (transfer != null && "CASH".equals(transfer.methodName())) {
                    try {
                        transfer = bankTransferService.createForCustomer(
                                transfer.orderId(), customerId, transfer.type(), "BANK_TRANSFER");
                    } catch (Exception ignored) {
                    }
                }

                if (transfer == null) {
                    FinanceControllerSupport.sendError(request, response, HttpServletResponse.SC_NOT_FOUND,
                            "Không tìm thấy yêu cầu chuyển khoản.");
                    return;
                }
                request.setAttribute("transfer", transfer);
                request.setAttribute("orderId", transfer.orderId());
                request.setAttribute("type", transfer.type());
                request.getRequestDispatcher("/customer/bank-transfer.jsp").forward(request, response);
                return;
            }

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

            List<Order> customerOrders = new OrderDAO().findByCustomerId(customerId);
            request.setAttribute("customerOrders", customerOrders);

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
            } else if (orderId == null && customerOrders != null && !customerOrders.isEmpty()) {
                // Mặc định nạp dữ liệu đơn hàng đầu tiên (mới nhất) để hiển thị đầy đủ thông tin thanh toán & công nợ
                int defaultOrderId = customerOrders.get(0).getId();
                request.setAttribute("requestedOrderId", defaultOrderId);
                try {
                    request.setAttribute("orderBalance", paymentService.getOrderBalance(defaultOrderId));
                    request.setAttribute("outstandingDebt", paymentService.getOutstandingDebt(defaultOrderId));
                    request.setAttribute("orderPayments", paymentService.getOrderHistory(defaultOrderId));
                } catch (Exception ex) {
                    getServletContext().log("Could not compute balance for default order #" + defaultOrderId, ex);
                }
            }

            request.setAttribute("customerInvoices", new InvoiceDAO().findByCustomerId(customerId));
            request.setAttribute("customerContracts", new ContractsDAO().findByCustomerId(customerId));
            request.setAttribute("paymentHistory", paymentService.getCustomerPaymentHistory(customerId));
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

        String method = request.getParameter("method") == null ? "BANK_TRANSFER" : request.getParameter("method");
        String type = request.getParameter("type");

        if ("BANK_TRANSFER".equals(method) && !SePayService.isReady()) {
            boolean jsonRequested = "json".equalsIgnoreCase(request.getParameter("format"))
                    || (request.getHeader("Accept") != null && request.getHeader("Accept").toLowerCase(java.util.Locale.ROOT).contains("application/json"));
            String err = SePayService.configStatus();
            if (jsonRequested) {
                FinanceControllerSupport.sendError(request, response, 409, err);
                return;
            }
            request.setAttribute("sepayError", err);
            request.setAttribute("orderId", orderId);
            request.setAttribute("type", type);
            request.setAttribute("paymentCsrf", ensureCsrf(request));
            request.getRequestDispatcher("/customer/bank-transfer.jsp").forward(request, response);
            return;
        }

        try {
            orderExpirationService.expireOrderIfOverdue(orderId);
            BankTransferService.Transfer transfer = bankTransferService.createForCustomer(
                    orderId, customerId, type, method);
            if ("CASH".equals(transfer.methodName())) {
                response.sendRedirect(request.getContextPath() + "/payment/detail?orderId=" + transfer.orderId());
                return;
            }
            response.sendRedirect(request.getContextPath() + "/payment/transfer?orderId=" + transfer.orderId()
                    + "&type=" + transfer.type());
        } catch (IllegalArgumentException | IllegalStateException e) {
            boolean jsonRequested = "json".equalsIgnoreCase(request.getParameter("format"))
                    || (request.getHeader("Accept") != null && request.getHeader("Accept").toLowerCase(java.util.Locale.ROOT).contains("application/json"));
            if (jsonRequested) {
                FinanceControllerSupport.sendError(request, response, 409, e.getMessage());
            } else {
                request.setAttribute("sepayError", e.getMessage());
                request.setAttribute("orderId", orderId);
                request.setAttribute("type", type);
                request.setAttribute("paymentCsrf", ensureCsrf(request));
                request.getRequestDispatcher("/customer/bank-transfer.jsp").forward(request, response);
            }
        } catch (SQLException e) {
            getServletContext().log("Bank transfer creation failed", e);
            FinanceControllerSupport.sendError(request, response, 503, "Không thể tạo yêu cầu chuyển khoản.");
        }
    }

    public static String ensureCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Object token = session.getAttribute("csrfToken");
        if (!(token instanceof String value) || value.isBlank()) {
            String freshToken = UUID.randomUUID().toString();
            session.setAttribute("csrfToken", freshToken);
            return freshToken;
        }
        return (String) token;
    }

    public static boolean validCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute("csrfToken");
        String actual = request.getParameter("csrfToken");
        if (actual == null) actual = request.getHeader("X-CSRF-Token");
        return expected != null && actual != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String action(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.equals("/")) return "";
        return path.substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}
