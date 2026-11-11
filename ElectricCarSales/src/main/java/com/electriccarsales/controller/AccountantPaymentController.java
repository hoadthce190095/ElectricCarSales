package com.electriccarsales.controller;

import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.BankTransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Accountant-only manual bank statement review. */
@WebServlet("/staff/payment-review")
public class AccountantPaymentController extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final BankTransferService service = new BankTransferService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (accountant(request, response) == null) return;
        boolean jsonRequested = wantsJson(request);
        try {
            var pendingPayments = service.pendingPayments();
            var openDebts = service.openDebts();
            String csrfToken = PaymentController.ensureCsrf(request);
            if (jsonRequested) {
                response.setContentType("application/json;charset=UTF-8");
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("pendingPayments", pendingPayments);
                payload.put("openDebts", openDebts);
                payload.put("csrfToken", csrfToken);
                JSON.writeValue(response.getWriter(), payload);
                return;
            }
            request.setAttribute("pendingPayments", pendingPayments);
            request.setAttribute("openDebts", openDebts);
            request.setAttribute("csrfTokenForPayment", csrfToken);
            request.getRequestDispatcher("/staff/payment-review.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Cannot list pending payments", e);
            writeError(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Không thể tải dữ liệu công nợ/thanh toán từ cơ sở dữ liệu.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean jsonRequested = wantsJson(request);
        SessionUser user = accountant(request, response);
        if (user == null) return;
        if (!PaymentController.validCsrf(request)) {
            writeError(request, response, HttpServletResponse.SC_FORBIDDEN, "Phiên thao tác không hợp lệ.");
            return;
        }
        try {
            boolean confirm = "confirm".equals(request.getParameter("action"));
            if (!confirm && !"reject".equals(request.getParameter("action"))
                    && !"partner".equals(request.getParameter("action"))) {
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
            Integer orderId = FinanceControllerSupport.positiveInt(request.getParameter("orderId"));
            if ("partner".equals(request.getParameter("action"))) {
                if (orderId == null) throw new IllegalArgumentException("Thiếu mã đơn trả góp.");
                service.createPartnerDisbursement(orderId);
            } else {
                Integer paymentId = FinanceControllerSupport.positiveInt(request.getParameter("paymentId"));
                if (paymentId == null) throw new IllegalArgumentException("Thiếu mã thanh toán.");
                BigDecimal received = null;
                if (confirm) {
                    if (!"on".equals(request.getParameter("checkedStatement"))) {
                        throw new IllegalArgumentException("Phải đối chiếu với sao kê OCB/đối tác trước khi xác nhận.");
                    }
                    try { received = new BigDecimal(request.getParameter("receivedAmount")); }
                    catch (NumberFormatException | NullPointerException e) {
                        throw new IllegalArgumentException("Số tiền thực nhận không hợp lệ.");
                    }
                }
                service.decide(paymentId, user.getUserId(), confirm, request.getParameter("bankTransaction"),
                        received, request.getParameter("reason"));
            }
            if (jsonRequested) {
                response.setContentType("application/json;charset=UTF-8");
                JSON.writeValue(response.getWriter(), Map.of("success", true, "message", "Đã lưu nghiệp vụ kế toán."));
            } else {
                String dashboardSection = "partner".equals(request.getParameter("action"))
                        ? "debts" : "payments";
                response.sendRedirect(request.getContextPath() + "/staff/dashboard#" + dashboardSection);
            }
        } catch (SQLException e) {
            getServletContext().log("Accountant payment review failed", e);
            writeError(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Không thể ghi nhận thanh toán vào cơ sở dữ liệu.");
        } catch (SecurityException e) {
            writeError(request, response, HttpServletResponse.SC_FORBIDDEN, "Tài khoản chưa có quyền Accountant.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            writeError(request, response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        }
    }

    private static boolean wantsJson(HttpServletRequest request) {
        return "json".equalsIgnoreCase(request.getParameter("format"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));
    }

    private static void writeError(HttpServletRequest request, HttpServletResponse response,
                                   int status, String message) throws IOException {
        if (wantsJson(request)) {
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            JSON.writeValue(response.getWriter(), Map.of("success", false, "error", message));
        } else {
            response.sendError(status, message);
        }
    }

    private static SessionUser accountant(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        if (value instanceof SessionUser user && "ACCOUNTANT".equalsIgnoreCase(user.getRole())) return user;
        response.sendError(403, "Chỉ Accountant được đối chiếu và xác nhận tiền.");
        return null;
    }
}
