
package com.electriccarsales.controller;

import com.electriccarsales.dao.FinanceConnection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet("/webhook/sepay")
public class SePayWebhookController extends HttpServlet {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Pattern PAYMENT_CODE = Pattern.compile(
        "(?<![A-Z0-9])ECS\\d+[DR]\\d+(?![A-Z0-9])"
    );

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws IOException {

        String apiKey = System.getenv("SEPAY_WEBHOOK_KEY");
        String mbAccount = System.getenv("ECS_MB_ACCOUNT");

        if (apiKey == null || apiKey.isBlank()
            || mbAccount == null || mbAccount.isBlank()) {
            response.sendError(503, "Webhook chưa cấu hình.");
            return;
        }

        String authorization = request.getHeader("Authorization");
        String expected = "Apikey " + apiKey;

        if (authorization == null
            || !MessageDigest.isEqual(
            authorization.getBytes(StandardCharsets.UTF_8),
            expected.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(401, "Invalid webhook key");
            return;
        }

        byte[] body = request.getInputStream().readNBytes(16385);
        if (body.length > 16384) {
            response.sendError(413);
            return;
        }

        JsonNode data;
        try {
            data = JSON.readTree(body);
        } catch (IOException e) {
            response.sendError(400, "Invalid JSON");
            return;
        }


        // Chỉ xử lý dữ liệu Gửi thử của SePay.
        // Không ghi nhận thanh toán vào Database.
        if (data != null
            && data.isObject()
            && data.path("id").asLong(-1) == 0
            && "SEPAYTEST".equals(data.path("code").asText())) {

            getServletContext().log(
                "SePay TEST received - no payment recorded"
            );

            success(response);
            return;
        }

        if (data == null || !data.isObject()
            || data.path("id").asLong(0) <= 0
            || !data.path("transferAmount").isNumber()) {
            response.sendError(400, "Invalid transaction");
            return;
        }

        String account = data.path("accountNumber").asText();
        if (!mbAccount.equals(account)) {
            response.sendError(422, "Wrong bank account");
            return;
        }

        if (!"in".equalsIgnoreCase(
            data.path("transferType").asText())) {
            success(response);
            return;
        }

        BigDecimal received = data.path("transferAmount")
            .decimalValue();

        if (received.signum() <= 0) {
            response.sendError(400, "Invalid amount");
            return;
        }

        long eventId = data.path("id").asLong();
        String content = data.path("content").asText("")
            .toUpperCase(Locale.ROOT);

        Matcher matcher = PAYMENT_CODE.matcher(content);

        if (!matcher.find()) {
            getServletContext().log(
                "SePay event " + eventId
                    + ": không tìm thấy mã thanh toán ECS."
            );
            success(response);
            return;
        }

        String paymentReference = matcher.group();
        String eventTag = "SEPAY_ID=" + eventId + ";";

        try (Connection connection = FinanceConnection.open()) {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(
                Connection.TRANSACTION_SERIALIZABLE
            );

            try {
                // Khóa khi đối soát để tránh xử lý trùng.
                try (Statement lock = connection.createStatement()) {
                    lock.executeQuery(
                        "SELECT COUNT(*) FROM Payments "
                            + "WITH (TABLOCKX, HOLDLOCK)"
                    ).close();
                }

                boolean processed = false;

                try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM Payments "
                        + "WHERE CHARINDEX(?, COALESCE(Note,'')) > 0")) {
                    ps.setString(1, eventTag);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        processed = rs.getInt(1) > 0;
                    }
                }

                if (!processed) {
                    String sql =
                        "SELECT p.PaymentID, p.PaymentAmount, "
                            + "p.Note, ps.StatusID "
                            + "FROM Payments p "
                            + "JOIN PaymentStatuses ps "
                            + "ON p.PaymentStatusID = ps.PaymentStatusID "
                            + "JOIN PaymentMethods pm "
                            + "ON p.PaymentMethodID = pm.PaymentMethodID "
                            + "WHERE p.ReferenceCode = ? "
                            + "AND pm.MethodName = 'BANK_TRANSFER'";

                    try (PreparedStatement ps =
                             connection.prepareStatement(sql)) {

                        ps.setString(1, paymentReference);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()
                                && "PENDING".equals(rs.getString("StatusID"))
                                && received.compareTo(
                                rs.getBigDecimal("PaymentAmount")) == 0) {

                                int paymentId = rs.getInt("PaymentID");
                                String oldNote = rs.getString("Note");

                                String note = (oldNote == null ? "" : oldNote + " ")
                                    + eventTag
                                    + " SEPAY_RECEIVED="
                                    + received.toPlainString() + ";";

                                if (note.length() > 500) {
                                    throw new SQLException(
                                        "Payment note too long");
                                }

                                try (PreparedStatement update =
                                         connection.prepareStatement(
                                             "UPDATE Payments SET Note=? "
                                                 + "WHERE PaymentID=?")) {
                                    update.setNString(1, note);
                                    update.setInt(2, paymentId);
                                    update.executeUpdate();
                                }

                                getServletContext().log(
                                    "SePay: đã đối soát đúng khoản #"
                                        + paymentId
                                        + ", chờ Accountant xác nhận."
                                );
                            } else {
                                getServletContext().log(
                                    "SePay event " + eventId
                                        + ": mã hoặc số tiền không khớp PENDING."
                                );
                            }
                        }
                    }
                }

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            getServletContext().log("SePay database error", e);
            response.sendError(503, "Database unavailable");
            return;
        }

        success(response);
    }

    private static void success(HttpServletResponse response)
        throws IOException {
        response.setStatus(201);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":true}");
    }
}
