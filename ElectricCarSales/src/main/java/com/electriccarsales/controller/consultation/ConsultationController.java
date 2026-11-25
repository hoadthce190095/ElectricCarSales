package com.electriccarsales.controller;

import com.electriccarsales.dao.CarModelDAO;
import com.electriccarsales.dao.CarVersionDAO;
import com.electriccarsales.dao.ColorDAO;
import com.electriccarsales.dao.CustomerDAO;
import com.electriccarsales.dao.FinancialCompanyDAO;
import com.electriccarsales.dao.ModelColorDAO;
import com.electriccarsales.dao.PromotionDAO;
import com.electriccarsales.dao.order.OrderDAO;
import com.electriccarsales.model.AdminPromotion;
import com.electriccarsales.model.CarModel;
import com.electriccarsales.model.CarVersion;
import com.electriccarsales.model.CatalogVehicle;
import com.electriccarsales.model.Color;
import com.electriccarsales.model.ConsultationRequest;
import com.electriccarsales.model.Customer;
import com.electriccarsales.model.FinancialCompany;
import com.electriccarsales.model.ModelColor;
import com.electriccarsales.model.Order;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.ConsultationService;
import com.electriccarsales.service.VehicleCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import util.DBContext;

@WebServlet(name = "ConsultationController", urlPatterns = {"/consultation"})
public class ConsultationController extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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
                response.setContentType("application/json;charset=UTF-8");
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("role", "GUEST");
                payload.put("authenticated", false);
                payload.put("consultations", List.of());
                List<CarModel> guestModels = List.of();
                try { guestModels = new CarModelDAO().getAll(); } catch (Exception ex) { log("Cannot load car models", ex); }
                List<FinancialCompany> guestCompanies = List.of();
                try { guestCompanies = new FinancialCompanyDAO().findAll(); } catch (Exception ex) { log("Cannot load financial companies", ex); }
                List<CatalogVehicle> guestCatalogVehicles = List.of();
                try { guestCatalogVehicles = new VehicleCatalogService().getCatalogVehicles(); } catch (Exception ex) { log("Cannot load catalog vehicles", ex); }
                populateCatalogPayload(payload, guestModels, guestCatalogVehicles, guestCompanies);
                payload.put("csrfToken", com.electriccarsales.controller.PaymentController.ensureCsrf(request));
                JSON.writeValue(response.getWriter(), payload);
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
            List<CarModel> models = List.of();
            try { models = new CarModelDAO().getAll(); } catch (Exception ex) { log("Cannot load car models", ex); }
            List<FinancialCompany> companies = List.of();
            try { companies = new FinancialCompanyDAO().findAll(); } catch (Exception ex) { log("Cannot load financial companies", ex); }
            request.setAttribute("carModels", models);
            request.setAttribute("financialCompanies", companies);

            List<CatalogVehicle> catalogVehicles = List.of();
            try {
                catalogVehicles = new VehicleCatalogService().getCatalogVehicles();
            } catch (Exception ex) {
                log("Cannot load catalog vehicles for consultation", ex);
            }
            request.setAttribute("catalogVehicles", catalogVehicles);

            List<ConsultationRequest> consultations = List.of();
            List<Order> customerOrders = List.of();
            if ("CUSTOMER".equals(role)) {
                Customer customer = null;
                try {
                    customer = new CustomerDAO().findByUserId(user.getUserId());
                } catch (Exception ex) {
                    log("Cannot find customer for userId=" + user.getUserId(), ex);
                }
                if (customer != null) {
                    try {
                        consultations = service.getCustomerRequests(customer.getCustomerID());
                    } catch (Exception ex) {
                        log("Cannot load customer requests", ex);
                    }
                    request.setAttribute("customer", customer);
                    try {
                        customerOrders = new OrderDAO().findByCustomerId(customer.getCustomerID());
                    } catch (Exception ex) {
                        log("Cannot load customer orders for consultation", ex);
                    }
                }
            } else if (List.of("SALES", "MANAGER", "ADMIN").contains(role)) {
                try {
                    consultations = service.getAllRequests();
                } catch (Exception ex) {
                    log("Cannot load all consultation requests", ex);
                    consultations = List.of();
                }
            }
            request.setAttribute("consultations", consultations);
            request.setAttribute("customerOrders", customerOrders);

            String csrf = com.electriccarsales.controller.PaymentController.ensureCsrf(request);
            request.setAttribute("csrfToken", csrf);

            if (jsonRequested) {
                response.setContentType("application/json;charset=UTF-8");
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("role", role);
                payload.put("authenticated", true);
                payload.put("consultations", consultations);
                populateCatalogPayload(payload, models, catalogVehicles, companies);
                payload.put("csrfToken", csrf);
                JSON.writeValue(response.getWriter(), payload);
                return;
            }

            if (List.of("SALES", "MANAGER", "ADMIN").contains(role)) {
                request.getRequestDispatcher("/staff/consultations.jsp").forward(request, response);
                return;
            }
            if ("checkout".equalsIgnoreCase(request.getParameter("view"))) {
                request.getRequestDispatcher("/customer/checkout.jsp").forward(request, response);
            } else {
                request.getRequestDispatcher("/customer/my-consultation.jsp").forward(request, response);
            }
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

        if (!com.electriccarsales.controller.PaymentController.validCsrf(request)) {
            if (jsonRequested) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                JSON.writeValue(response.getWriter(), Map.of("success", false, "message", "Phiên thao tác không hợp lệ hoặc đã hết hạn. Vui lòng tải lại trang."));
            } else {
                response.sendError(403, "Phiên thao tác không hợp lệ.");
            }
            return;
        }

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
                        "Đã cập nhật yêu cầu tư vấn #" + requestId + ".", "/consultation?updated=1");
                return;
            }

            if (!"CUSTOMER".equals(role)) {
                sendResult(request, response, jsonRequested, HttpServletResponse.SC_FORBIDDEN, false,
                        "Chức năng gửi yêu cầu tư vấn dành cho tài khoản Khách hàng.", "/consultation");
                return;
            }
            Customer customer = new CustomerDAO().findByUserId(user.getUserId());
            if (customer == null) {
                try (Connection conn = DBContext.openConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT Username, Phone, Email FROM Users WHERE UserID = ?")) {
                    ps.setInt(1, user.getUserId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String name = rs.getString("Username");
                            String phone = rs.getString("Phone");
                            String email = rs.getString("Email");
                            if (phone == null || phone.isBlank()) phone = "0900000000";
                            try (PreparedStatement ins = conn.prepareStatement(
                                    "INSERT INTO Customers(UserID, CustomerName, Phone, Email) VALUES(?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                                ins.setInt(1, user.getUserId());
                                ins.setString(2, name != null && !name.isBlank() ? name : "Khách hàng");
                                ins.setString(3, phone);
                                ins.setString(4, email);
                                ins.executeUpdate();
                                customer = new CustomerDAO().findByUserId(user.getUserId());
                            }
                        }
                    }
                } catch (Exception ex) {
                    log("Cannot auto-link customer record for user " + user.getUserId(), ex);
                }
            }
            if (customer == null) {
                sendResult(request, response, jsonRequested, HttpServletResponse.SC_FORBIDDEN, false,
                        "Tài khoản chưa liên kết hồ sơ khách hàng.", "/consultation");
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
                        "Đã gửi phản hồi bổ sung cho yêu cầu tư vấn #" + requestId + ".", "/consultation?updated=1", null);
                return;
            }

            String modelName = request.getParameter("model");
            String versionName = request.getParameter("version");
            String colorName = request.getParameter("color");
            Integer modelId = parseModelId(request.getParameter("modelId"), modelName);
            String message = request.getParameter("message");
            String paymentPreference = request.getParameter("paymentPreference");
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập nội dung cần tư vấn.");
            }

            StringBuilder prefix = new StringBuilder();
            if (modelName != null && !modelName.isBlank() && !"Chưa xác định".equalsIgnoreCase(modelName.trim())) {
                prefix.append("[Mẫu: ").append(modelName.trim());
                if (versionName != null && !versionName.isBlank() && !"Chưa xác định".equalsIgnoreCase(versionName.trim())) {
                    prefix.append(" | Bản: ").append(versionName.trim());
                }
                if (colorName != null && !colorName.isBlank() && !"Chưa xác định".equalsIgnoreCase(colorName.trim())) {
                    prefix.append(" | Màu: ").append(colorName.trim());
                }
                prefix.append("] ");
            }
            if (paymentPreference != null && !paymentPreference.isBlank()) {
                prefix.append("[Phương án: ").append(paymentPreference.trim()).append("] ");
            }

            String fullContent = prefix.toString() + message.trim();
            if (fullContent.length() > 1000) {
                int maxMsg = 1000 - prefix.length();
                if (maxMsg < 10) {
                    throw new IllegalArgumentException("Nội dung yêu cầu vượt quá giới hạn 1.000 ký tự.");
                }
                fullContent = prefix.toString() + message.trim().substring(0, maxMsg);
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
                    "/consultation?submitted=1", cr);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log("Consultation POST failed: " + ex.getMessage(), ex);
            sendResult(request, response, jsonRequested, HttpServletResponse.SC_BAD_REQUEST, false,
                    ex.getMessage(), "/consultation", null);
        } catch (RuntimeException ex) {
            log("Consultation POST runtime error: " + ex.getMessage(), ex);
            sendResult(request, response, jsonRequested, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Không thể xử lý yêu cầu tư vấn: " + ex.getMessage(), "/consultation", null);
        }
    }

    private Integer parseModelId(String modelIdParam, String modelNameParam) {
        CarModelDAO modelDAO = new CarModelDAO();
        List<CarModel> allModels = List.of();
        try {
            allModels = modelDAO.getAll();
        } catch (Exception ex) {
            log("Không thể tải danh sách CarModels: " + ex.getMessage(), ex);
        }

        if (modelIdParam != null && !modelIdParam.isBlank()) {
            try {
                int id = Integer.parseInt(modelIdParam.trim());
                if (id > 0) {
                    for (CarModel m : allModels) {
                        if (m.getModelID() == id) return id;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (modelNameParam != null && !modelNameParam.isBlank() && !"Chưa xác định".equalsIgnoreCase(modelNameParam.trim())) {
            for (CarModel m : allModels) {
                if (m.getModelName() != null && (m.getModelName().equalsIgnoreCase(modelNameParam.trim())
                        || m.getModelName().toLowerCase(Locale.ROOT).startsWith(modelNameParam.trim().toLowerCase(Locale.ROOT))
                        || modelNameParam.trim().toLowerCase(Locale.ROOT).contains(m.getModelName().toLowerCase(Locale.ROOT)))) {
                    return m.getModelID();
                }
            }
        }
        if (!allModels.isEmpty()) {
            return allModels.get(0).getModelID();
        }
        return null;
    }

    private void sendResult(HttpServletRequest req, HttpServletResponse resp, boolean jsonRequested,
                            int status, boolean success, String message, String redirectPath) throws IOException, ServletException {
        sendResult(req, resp, jsonRequested, status, success, message, redirectPath, null);
    }

    private void sendResult(HttpServletRequest req, HttpServletResponse resp, boolean jsonRequested,
                            int status, boolean success, String message, String redirectPath,
                            ConsultationRequest cr) throws IOException, ServletException {
        if (jsonRequested) {
            resp.setStatus(status);
            resp.setContentType("application/json;charset=UTF-8");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("success", success);
            payload.put("message", message);
            if (cr != null && cr.getId() > 0) {
                payload.put("requestId", cr.getId());
                payload.put("consultation", cr);
            }
            JSON.writeValue(resp.getWriter(), payload);
            return;
        }
        if (success) {
            resp.sendRedirect(req.getContextPath() + redirectPath);
        } else {
            req.setAttribute("error", message);
            doGet(req, resp);
        }
    }

    private void populateCatalogPayload(Map<String, Object> payload, List<CarModel> models,
                                        List<CatalogVehicle> catalogVehicles, List<FinancialCompany> companies) {
        payload.put("models", models != null ? models : List.of());

        List<CarVersion> versions = List.of();
        try { versions = new CarVersionDAO().getAll(); } catch (Exception ex) { log("Cannot load versions", ex); }
        payload.put("versions", versions);

        List<Color> colors = List.of();
        try { colors = new ColorDAO().getAll(); } catch (Exception ex) { log("Cannot load colors", ex); }
        payload.put("colors", colors);

        List<ModelColor> modelColors = List.of();
        try { modelColors = new ModelColorDAO().getAll(); } catch (Exception ex) { log("Cannot load model colors", ex); }
        payload.put("modelColors", modelColors);

        Map<Integer, List<AdminPromotion>> activePromos = Map.of();
        try (Connection conn = DBContext.openConnection()) {
            activePromos = PromotionDAO.findAllActiveByModel(conn);
        } catch (Exception ex) {
            log("Cannot load active promotions for consultation", ex);
        }

        Map<Integer, Map<String, Object>> promoByModel = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<AdminPromotion>> entry : activePromos.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                AdminPromotion p = entry.getValue().get(0);
                Map<String, Object> promoMap = new LinkedHashMap<>();
                promoMap.put("id", p.getPromotionId());
                promoMap.put("name", p.getPromotionName());
                promoMap.put("discountType", p.getDiscountType());
                promoMap.put("discountValue", p.getDiscountValue());
                promoMap.put("endDate", p.getEndDate() != null ? p.getEndDate().toLocalDate().toString() : null);
                promoByModel.put(entry.getKey(), promoMap);
            }
        }
        payload.put("promotions", promoByModel);

        Map<Integer, BigDecimal> catalogPricesByVersion = new LinkedHashMap<>();
        Map<Integer, BigDecimal> catalogPricesByModel = new LinkedHashMap<>();
        if (catalogVehicles != null) {
            for (CatalogVehicle cv : catalogVehicles) {
                if (cv.priceVnd() != null && cv.priceVnd().signum() > 0) {
                    if (cv.stockCarID() == null) {
                        catalogPricesByVersion.putIfAbsent(cv.versionID(), cv.priceVnd());
                    }
                    catalogPricesByModel.merge(cv.modelID(), cv.priceVnd(), BigDecimal::min);
                }
            }
        }
        payload.put("catalogPricesByVersion", catalogPricesByVersion);
        payload.put("catalogPricesByModel", catalogPricesByModel);
        payload.put("availableCars", loadAvailableCars(activePromos));
        payload.put("financialCompanies", companies != null ? companies : List.of());
    }

    private List<Map<String, Object>> loadAvailableCars(Map<Integer, List<AdminPromotion>> activePromos) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT c.CarID, c.ModelID, c.VersionID, c.ColorID, c.Price, c.VIN,
                   m.ModelName,
                   COALESCE(v.VersionName, N'Tiêu chuẩn') AS VersionName,
                   COALESCE(col.ColorName, N'Tiêu chuẩn') AS ColorName
            FROM Cars c
            JOIN CarStatuses s ON s.CarStatusID = c.CarStatusID
            JOIN CarModels m ON m.ModelID = c.ModelID
            LEFT JOIN CarVersions v ON v.VersionID = c.VersionID
            LEFT JOIN Colors col ON col.ColorID = c.ColorID
            WHERE UPPER(LTRIM(RTRIM(s.StatusName))) IN (N'AVAILABLE', N'IN_STOCK', N'FOR_SALE', N'READY_FOR_SALE', N'CÓ SẴN', N'CO SAN')
              AND NOT EXISTS (SELECT 1 FROM OrderDetails d WHERE d.CarID = c.CarID)
            ORDER BY m.ModelName, v.VersionName, col.ColorName, c.CarID
            """;
        try (Connection conn = DBContext.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int modelId = rs.getInt("ModelID");
                BigDecimal originalPrice = rs.getBigDecimal("Price");
                if (originalPrice == null) originalPrice = BigDecimal.ZERO;

                BigDecimal discountAmount = BigDecimal.ZERO;
                String promotionName = null;
                String discountType = null;
                BigDecimal discountValue = null;

                List<AdminPromotion> promos = activePromos != null ? activePromos.get(modelId) : null;
                if (promos != null && !promos.isEmpty() && originalPrice.signum() > 0) {
                    AdminPromotion p = promos.get(0);
                    BigDecimal val = p.getDiscountValue();
                    String type = p.getDiscountType();
                    if (val != null && val.signum() > 0) {
                        BigDecimal rawDiscount = "PERCENTAGE".equalsIgnoreCase(type)
                                ? originalPrice.multiply(val).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP)
                                : val.setScale(0, RoundingMode.HALF_UP);
                        discountAmount = rawDiscount.min(originalPrice);
                        if (discountAmount.signum() > 0) {
                            promotionName = p.getPromotionName();
                            discountType = type;
                            discountValue = val;
                        }
                    }
                }

                Map<String, Object> map = new LinkedHashMap<>();
                map.put("carId", rs.getInt("CarID"));
                map.put("modelId", modelId);
                map.put("versionId", rs.getInt("VersionID"));
                map.put("colorId", rs.getInt("ColorID"));
                map.put("originalPrice", originalPrice);
                map.put("discountAmount", discountAmount);
                map.put("discountType", discountType);
                map.put("discountValue", discountValue);
                map.put("promotionName", promotionName);
                map.put("price", originalPrice.subtract(discountAmount));
                map.put("vin", rs.getString("VIN"));
                map.put("modelName", rs.getString("ModelName"));
                map.put("versionName", rs.getString("VersionName"));
                map.put("colorName", rs.getString("ColorName"));
                list.add(map);
            }
        } catch (Exception ex) {
            log("Cannot load available cars for consultation", ex);
        }
        return list;
    }
}
