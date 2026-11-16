package com.electriccarsales.controller;

import com.electriccarsales.dao.CustomerDAO;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.ConsultationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/** Conversation view backed by existing consultation records; no schema changes. */
@WebServlet("/sales-chat")
public class SalesChatController extends HttpServlet {
    private final ConsultationService service = new ConsultationService();

    private SessionUser authorize(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var session = req.getSession(false);
        var user = session == null ? null : (SessionUser) session.getAttribute("user");
        if (user == null) { resp.sendRedirect(req.getContextPath() + "/login"); return null; }
        if (!"CUSTOMER".equalsIgnoreCase(user.getRole()) && !"SALES".equalsIgnoreCase(user.getRole())) {
            resp.sendError(403); return null;
        }
        resp.setHeader("Cache-Control", "no-store");
        return user;
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        var user = authorize(req, resp);
        if (user == null) return;
        boolean customer = "CUSTOMER".equalsIgnoreCase(user.getRole());
        try {
            if (customer) {
                var owner = new CustomerDAO().findByUserId(user.getUserId());
                if (owner == null) { resp.sendError(403); return; }
                req.setAttribute("threads", service.getCustomerRequests(owner.getCustomerID()));
            } else {
                var employee = (Integer) req.getSession().getAttribute("employeeId");
                if (employee == null || employee <= 0) { resp.sendError(403); return; }
                req.setAttribute("threads", service.getAllRequests().stream()
                        .filter(t -> t.getStaffId() == 0 || t.getStaffId() == employee).toList());
            }
            req.setAttribute("isCustomer", customer);
            req.setAttribute("chatCsrf", PaymentController.ensureCsrf(req));
            req.getRequestDispatcher("/WEB-INF/sales-chat.jsp").forward(req, resp);
        } catch (RuntimeException ex) {
            log("Cannot load sales chat", ex);
            resp.sendError(503, "Không thể tải cuộc trò chuyện.");
        }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        var user = authorize(req, resp);
        if (user == null) return;
        if (!PaymentController.validCsrf(req)) { resp.sendError(403); return; }
        try {
            int id = Integer.parseInt(req.getParameter("requestId"));
            String message = req.getParameter("message");
            if (message == null || message.isBlank() || message.length() > 800)
                throw new IllegalArgumentException("Nhập tin nhắn từ 1 đến 800 ký tự.");
            if ("CUSTOMER".equalsIgnoreCase(user.getRole())) {
                var owner = new CustomerDAO().findByUserId(user.getUserId());
                if (owner == null) { resp.sendError(403); return; }
                service.appendCustomerNote(id, owner.getCustomerID(), message);
            } else {
                Integer employee = (Integer) req.getSession().getAttribute("employeeId");
                if (employee == null || employee <= 0) { resp.sendError(403); return; }
                var thread = service.getRequest(id);
                if (thread == null || (thread.getStaffId() != 0 && thread.getStaffId() != employee)) {
                    resp.sendError(403); return;
                }
                if ("COMPLETED".equals(thread.getStatus()) || "CANCELLED".equals(thread.getStatus()))
                    throw new IllegalStateException("Cuộc trò chuyện đã kết thúc.");
                service.updateByStaff(id, employee, "Sales", "IN_PROGRESS", message);
            }
            resp.sendRedirect(req.getContextPath() + "/sales-chat#thread-" + id);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            req.setAttribute("chatError", ex.getMessage());
            doGet(req, resp);
        }
    }
}
