package com.electriccarsales.controller;

import com.electriccarsales.dao.CarModelDAO;
import com.electriccarsales.dao.CustomerDAO;
import com.electriccarsales.dao.FinancialCompanyDAO;
import com.electriccarsales.model.CarModel;
import com.electriccarsales.model.ConsultationRequest;
import com.electriccarsales.model.Customer;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.ConsultationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@WebServlet(name = "ConsultationController", urlPatterns = {"/consultation"})
public class ConsultationController extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final ConsultationService service = new ConsultationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("user");
        String role = user == null || user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
        boolean jsonRequested = "json".equalsIgnoreCase(request.getParameter("format"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        if (user == null) {
            if (jsonRequested) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                JSON.writeValue(response.getWriter(), Map.of("error", "Vui lòng đăng nhập để xem yêu cầu tư vấn."));
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }
        if (!List.of("CUSTOMER", "SALES", "MANAGER", "ADMIN").contains(role)) {
            if (jsonRequested) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                JSON.writeValue(response.getWriter(), Map.of("error", "Tài khoản không có quyền xem yêu cầu tư vấn."));
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản không có quyền xem yêu cầu tư vấn.");
            }
            return;
        }

        try {
            List<CarModel> models = new CarModelDAO().getAll();
            var companies = new FinancialCompanyDAO().findAll();
            request.setAttribute("carModels", models);
            request.setAttribute("financialCompanies", companies);

            List<ConsultationRequest> consultations = List.of();
            if ("CUSTOMER".equals(role)) {
                Customer customer = new CustomerDAO().findByUserId(user.getUserId());
                if (customer != null) {
                    consultations = service.getCustomerRequests(customer.getCustomerID());
                    request.setAttribute("customer", customer);
                }
            } else if (List.of("SALES", "MANAGER", "ADMIN").contains(role)) {
                consultations = service.getAllRequests();
            }
            request.setAttribute("consultations", consultations);

            if (jsonRequested) {
                response.setContentType("application/json;charset=UTF-8");
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("role", role);
                payload.put("authenticated", user != null);
                payload.put("consultations", consultations);
                payload.put("models", models);
                payload.put("financialCompanies", companies);
                JSON.writeValue(response.getWriter(), payload);
                return;
            }

            if (List.of("SALES", "MANAGER", "ADMIN").contains(role)) {
                response.sendRedirect(request.getContextPath() + "/staff/dashboard#consultations");
                return;
            }
            request.getRequestDispatcher("/customer/checkout.jsp").forward(request, response);
        } catch (RuntimeException ex) {
            log("Consultation GET failed", ex);
            if (jsonRequested) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType("application/json;charset=UTF-8");
                JSON.writeValue(response.getWriter(), Map.of("error", "Không thể tải dữ liệu tư vấn. Vui lòng thử lại sau."));
            } else {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Không thể tải dữ liệu tư vấn.");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        boolean jsonRequested = "json".equalsIgnoreCase(request.getParameter("format"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("user");
        if (user == null) {
            if (jsonRequested) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                JSON.writeValue(response.getWriter(), Map.of("error", "Vui lòng đăng nhập tài khoản khách hàng để gửi yêu cầu tư vấn."));
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }

        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
        String action = request.getParameter("action");
        if (action == null || action.isBlank()) {
            action = "create";
        }

        try {
            if ("update".equalsIgnoreCase(action)) {
                if (!List.of("SALES", "MANAGER", "ADMIN").contains(role)) {
                    sendResult(request, response, jsonRequested, HttpServletResponse.SC_FORBIDDEN, false,
                            "Chỉ nhân viên tư vấn (Sales Staff) mới được cập nhật trạng thái tư vấn.", "/staff/dashboard#consultations");
                    return;
                }
                int requestId = Integer.parseInt(request.getParameter("requestId"));
                String status = request.getParameter("status");
                String note = request.getParameter("note");
                Integer employeeId = (Integer) session.getAttribute("employeeId");
                String staffName = (String) session.getAttribute("displayName");
                if (employeeId == null || employeeId <= 0) {
                    throw new IllegalStateException("Tài khoản Sales chưa liên kết hồ sơ nhân viên.");
                }
                service.updateByStaff(requestId, employeeId, staffName, status, note);
                sendResult(request, response, jsonRequested, HttpServletResponse.SC_OK, true,
                        "Đã cập nhật yêu cầu tư vấn #" + requestId + ".", "/staff/dashboard#consultations");
                return;
            }

            if (!"CUSTOMER".equals(role)) {
                sendResult(request, response, jsonRequested, HttpServletResponse.SC_FORBIDDEN, false,
                        "Chức năng gửi yêu cầu tư vấn dành cho tài khoản Khách hàng.", "/customer/checkout.jsp");
                return;
            }
            Customer customer = new CustomerDAO().findByUserId(user.getUserId());
            if (customer == null) {
                sendResult(request, response, jsonRequested, HttpServletResponse.SC_FORBIDDEN, false,
                        "Tài khoản chưa liên kết hồ sơ khách hàng.", "/customer/checkout.jsp");
                return;
            }
            if (customer.getPhone() == null || customer.getPhone().isBlank()) {
                sendResult(request, response, jsonRequested, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Hãy bổ sung số điện thoại liên hệ trong hồ sơ trước khi gửi yêu cầu tư vấn.", "/customer/my-account.jsp");
                return;
            }

            if ("reply".equalsIgnoreCase(action)) {
                int requestId = Integer.parseInt(request.getParameter("requestId"));
                String note = request.getParameter("message");
                service.appendCustomerNote(requestId, customer.getCustomerID(), note);
                sendResult(request, response, jsonRequested, HttpServletResponse.SC_OK, true,
                        "Đã gửi phản hồi bổ sung cho yêu cầu tư vấn #" + requestId + ".", "/customer/checkout.jsp?updated=1");
                return;
            }

            Integer modelId = parseModelId(request.getParameter("modelId"), request.getParameter("model"));
            String message = request.getParameter("message");
            if (modelId == null) {
                throw new IllegalArgumentException("Vui lòng chọn mẫu xe quan tâm trong danh mục.");
            }
            String paymentPreference = request.getParameter("paymentPreference");
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập nội dung cần tư vấn.");
            }
            String fullContent = message.trim();
            if (paymentPreference != null && !paymentPreference.isBlank()) {
                fullContent = "[Phương án quan tâm: " + paymentPreference.trim() + "] " + fullContent;
            }
            if (fullContent.length() > 1000) {
                throw new IllegalArgumentException("Nội dung yêu cầu vượt quá giới hạn 1.000 ký tự.");
            }

            ConsultationRequest cr = new ConsultationRequest();
            cr.setCustomerId(customer.getCustomerID());
            cr.setStaffId(0);
            cr.setModelId(modelId);
            cr.setContent(fullContent);
            cr.setStatus("PENDING");
            service.createRequest(cr);

            sendResult(request, response, jsonRequested, HttpServletResponse.SC_CREATED, true,
                    "Đã gửi yêu cầu tư vấn #" + cr.getId() + " thành công. Nhân viên Sales sẽ phản hồi sớm.",
                    "/customer/checkout.jsp?submitted=1");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            sendResult(request, response, jsonRequested, HttpServletResponse.SC_BAD_REQUEST, false,
                    ex.getMessage(), "/customer/checkout.jsp");
        } catch (RuntimeException ex) {
            log("Consultation POST failed", ex);
            sendResult(request, response, jsonRequested, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Không thể xử lý yêu cầu tư vấn.", "/customer/checkout.jsp");
        }
    }

    private Integer parseModelId(String modelIdParam, String modelNameParam) {
        if (modelIdParam != null && !modelIdParam.isBlank()) {
            try {
                int id = Integer.parseInt(modelIdParam.trim());
                if (id > 0) return id;
            } catch (NumberFormatException ignored) {
            }
        }
        if (modelNameParam != null && !modelNameParam.isBlank() && !"Chưa xác định".equalsIgnoreCase(modelNameParam.trim())) {
            CarModelDAO modelDAO = new CarModelDAO();
            for (CarModel m : modelDAO.getAll()) {
                if (m.getModelName() != null && (m.getModelName().equalsIgnoreCase(modelNameParam.trim())
                        || m.getModelName().toLowerCase(Locale.ROOT).startsWith(modelNameParam.trim().toLowerCase(Locale.ROOT)))) {
                    return m.getModelID();
                }
            }
        }
        return null;
    }

    private void sendResult(HttpServletRequest req, HttpServletResponse resp, boolean jsonRequested,
                            int status, boolean success, String message, String redirectPath) throws IOException, ServletException {
        if (jsonRequested) {
            resp.setStatus(status);
            resp.setContentType("application/json;charset=UTF-8");
            JSON.writeValue(resp.getWriter(), Map.of("success", success, "message", message));
            return;
        }
        if (success) {
            resp.sendRedirect(req.getContextPath() + redirectPath);
        } else {
            req.setAttribute("error", message);
            doGet(req, resp);
        }
    }
}
