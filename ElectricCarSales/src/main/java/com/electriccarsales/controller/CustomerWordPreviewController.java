package com.electriccarsales.controller;

import com.electriccarsales.model.SalesPreviewKind;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.SalesPreviewService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

@WebServlet("/customer/word-preview")
public final class CustomerWordPreviewController extends HttpServlet {
    private final SalesPreviewService previews = new SalesPreviewService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var session = request.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof SessionUser user)
                || !"CUSTOMER".equalsIgnoreCase(user.getRole())) {
            response.sendError(403);
            return;
        }
        Integer orderId = FinanceControllerSupport.positiveInt(request.getParameter("orderId"));
        String requestedKind = request.getParameter("kind");
        if (orderId == null || requestedKind == null) {
            response.sendError(400);
            return;
        }
        SalesPreviewKind kind;
        try {
            kind = SalesPreviewKind.valueOf(requestedKind.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalidKind) {
            response.sendError(400);
            return;
        }

        byte[] docx;
        try {
            docx = previews.render(kind, orderId, user.getUserId());
        } catch (IOException | SQLException | RuntimeException unavailable) {
            log("Word preview unavailable for " + kind, unavailable);
            response.sendError(503, "Chưa thể tạo bản xem trước Word.");
            return;
        }
        if (docx == null) {
            response.sendError(404, "Không có chứng từ phù hợp cho đơn hàng này.");
            return;
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Cache-Control", "private, no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Disposition", "attachment; filename=" + orderId + "-" + kind.filename());
        response.setContentLength(docx.length);
        response.getOutputStream().write(docx);
    }
}
