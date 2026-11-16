package com.electriccarsales.controller;

import com.electriccarsales.service.BankTransferService;
import com.electriccarsales.service.SePayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Controller tiếp nhận Webhook biến động số dư tức thời từ SePay (SePay + MB Bank).
 * Endpoint: /webhook/sepay và /api/sepay-webhook
 *
 * Kiểm tra xác thực (Authorization: Apikey <key>), kiểm tra đúng tài khoản MB Bank,
 * chỉ nhận tiền vào (transferType = "in"), kiểm tra số tiền, chống xử lý trùng lặp (idempotent),
 * và cập nhật giao dịch theo giao dịch Serializable.
 */
@WebServlet(urlPatterns = {"/webhook/sepay", "/api/sepay-webhook"})
public class SePayWebhookController extends HttpServlet {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final SePayService sePayService = new SePayService();
    private final BankTransferService bankTransferService = new BankTransferService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":\"active\",\"gateway\":\"SePay MB Bank\",\"accountNumber\":\""
                + sePayService.getAccountNumber() + "\"}");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");

        // 1. Kiểm tra xác thực Webhook (Authorization: Apikey <key>)
        String authHeader = request.getHeader("Authorization");
        if (!sePayService.verifyWebhookKey(authHeader)) {
            logMessage("SePay Webhook: Xác thực không hợp lệ.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"error\":\"Invalid webhook key\"}");
            return;
        }

        // 2. Đọc payload JSON an toàn (giới hạn 32KB chống DoS)
        byte[] body = request.getInputStream().readNBytes(32769);
        if (body.length > 32768) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.getWriter().write("{\"success\":false,\"error\":\"Payload too large\"}");
            return;
        }

        JsonNode data;
        try {
            data = JSON.readTree(body);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"Invalid JSON\"}");
            return;
        }

        if (data == null || !data.isObject()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"Empty payload\"}");
            return;
        }

        // 3. Xử lý webhook Test Mode của SePay Dashboard (Gửi thử dữ liệu)
        long eventId = data.path("id").asLong(-1);
        String code = data.path("code").asText("");
        if (eventId == 0 || "SEPAYTEST".equalsIgnoreCase(code)) {
            logMessage("SePay: Đã nhận webhook kiểm thử (Test Mode), không ghi vào database.");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"success\":true,\"message\":\"SePay test webhook acknowledged\"}");
            return;
        }

        if (eventId <= 0 || !data.path("transferAmount").isNumber()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"Invalid transaction data\"}");
            return;
        }

        // 4. Kiểm tra tài khoản nhận
        String accountNumber = data.path("accountNumber").asText("").trim();
        String expectedAccount = SePayService.getAccountNumber();
        if (!expectedAccount.equals(accountNumber)) {
            logMessage("SePay event " + eventId + ": Sai số tài khoản nhận (" + accountNumber + "), yêu cầu " + expectedAccount);
            response.setStatus(422);
            response.getWriter().write("{\"success\":false,\"error\":\"Wrong bank account\"}");
            return;
        }

        // 5. Chỉ xử lý tiền vào (transferType = "in")
        String transferType = data.path("transferType").asText("").trim();
        if (!"in".equalsIgnoreCase(transferType)) {
            logMessage("SePay event " + eventId + ": Bỏ qua giao dịch tiền ra (transferType=" + transferType + ")");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"success\":true,\"message\":\"Ignored transfer out\"}");
            return;
        }

        BigDecimal transferAmount = data.path("transferAmount").decimalValue();
        if (transferAmount == null || transferAmount.signum() <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"error\":\"Invalid amount\"}");
            return;
        }

        String gateway = data.path("gateway").asText("MBBank");
        String content = data.path("content").asText("");
        String referenceCode = data.path("referenceCode").asText("");
        String transactionDate = data.path("transactionDate").asText("");

        try {
            boolean success = bankTransferService.processSePaySuccess(
                    eventId, gateway, accountNumber, transferType, transferAmount,
                    content, referenceCode, transactionDate);

            if (success) {
                logMessage("SePay: Xác nhận thanh toán thành công cho GD #" + eventId + ", số tiền " + transferAmount);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"success\":true,\"message\":\"Payment confirmed\"}");
            } else {
                logMessage("SePay event " + eventId + ": Không tìm thấy mã thanh toán ECS hoặc đơn hàng PENDING phù hợp.");
                // Trả HTTP 200 để SePay ghi nhận đã tiếp nhận thành công, tránh SePay gửi lại liên tục với giao dịch cá nhân khác
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"success\":true,\"message\":\"No matching pending order found\"}");
            }
        } catch (IllegalStateException e) {
            logMessage("SePay event " + eventId + " lỗi trạng thái / số tiền: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (SQLException e) {
            logMessage("SePay event " + eventId + " lỗi cơ sở dữ liệu: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write("{\"success\":false,\"error\":\"Database error\"}");
        }
    }

    private void logMessage(String message, Throwable... t) {
        try {
            if (getServletContext() != null) {
                if (t.length > 0 && t[0] != null) {
                    getServletContext().log(message, t[0]);
                } else {
                    getServletContext().log(message);
                }
            } else {
                System.out.println(message);
            }
        } catch (Exception ignored) {
        }
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
