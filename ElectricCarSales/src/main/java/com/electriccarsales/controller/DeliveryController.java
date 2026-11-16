package com.electriccarsales.controller;

import com.electriccarsales.dao.CarDAO;
import com.electriccarsales.dao.CarModelDAO;
import com.electriccarsales.dao.CarStatusDAO;
import com.electriccarsales.dao.CarVersionDAO;
import com.electriccarsales.dao.ColorDAO;
import com.electriccarsales.dao.InventoryTransactionDAO;
import com.electriccarsales.dao.SupplierDAO;
import com.electriccarsales.model.Car;
import com.electriccarsales.model.Delivery;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.CarInventoryService;
import com.electriccarsales.service.DeliveryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import util.DBContext;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@WebServlet(name = "DeliveryController", urlPatterns = {"/delivery"})
public class DeliveryController extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
    private DeliveryService deliveryService;
    private CarInventoryService carInventoryService;

    @Override
    public void init() throws ServletException {
        deliveryService = new DeliveryService();
        carInventoryService = new CarInventoryService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        boolean jsonRequested = "json".equalsIgnoreCase(request.getParameter("format"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        try {
            List<Delivery> deliveries = deliveryService.getAllDeliveries();
            request.setAttribute("deliveryList", deliveries);

            if (jsonRequested) {
                response.setContentType("application/json;charset=UTF-8");
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("deliveries", deliveries);
                payload.put("csrfToken", PaymentController.ensureCsrf(request));
                payload.put("cars", new CarDAO().getAllCars());
                payload.put("carStatuses", new CarStatusDAO().getAllCarStatuses());
                payload.put("inventoryTransactions", new InventoryTransactionDAO().getAllTransactions());
                payload.put("suppliers", new SupplierDAO().getAllSuppliers());
                payload.put("models", new CarModelDAO().getAll());
                payload.put("versions", new CarVersionDAO().getAll());
                payload.put("colors", new ColorDAO().getAll());
                JSON.writeValue(response.getWriter(), payload);
                return;
            }

            response.sendRedirect(request.getContextPath() + "/staff/dashboard#handover");
        } catch (RuntimeException ex) {
            log("Could not load inventory/delivery data", ex);
            if (jsonRequested) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType("application/json;charset=UTF-8");
                JSON.writeValue(response.getWriter(), Map.of("error",
                        "Không thể tải dữ liệu kho/bàn giao từ cơ sở dữ liệu. Vui lòng kiểm tra cấu hình SQL Server."));
            } else {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "Không thể tải dữ liệu kho/bàn giao từ cơ sở dữ liệu.");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        boolean jsonRequested = "json".equalsIgnoreCase(request.getParameter("format"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("user");
        String role = user == null || user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
        String action = request.getParameter("action");
        if (action == null || action.isBlank()) {
            action = "handover";
        }

        try {
            if ("importCar".equalsIgnoreCase(action)) {
                if (!List.of("OPERATIONS", "ADMIN").contains(role)) {
                    sendResponse(request, response, jsonRequested, HttpServletResponse.SC_FORBIDDEN, false,
                            "Theo RDS (UC-09, 10.2), chỉ Operations Officer hoặc Admin mới được nhập xe vào kho.");
                    return;
                }
                int modelId = Integer.parseInt(request.getParameter("modelId"));
                int versionId = Integer.parseInt(request.getParameter("versionId"));
                int colorId = Integer.parseInt(request.getParameter("colorId"));
                String vin = request.getParameter("vin");
                String engineNo = request.getParameter("engineNumber");
                String carCode = request.getParameter("carCode");
                int year = Integer.parseInt(request.getParameter("year"));
                double price = Double.parseDouble(request.getParameter("price"));
                String origin = request.getParameter("origin");
                String lotCode = request.getParameter("lotCode");
                Integer supplierId = FinanceControllerSupport.positiveInt(request.getParameter("supplierId"));
                String note = request.getParameter("note");
                int employeeId = resolveEmployeeId(session, user);

                Car car = new Car(0,
                        (carCode == null || carCode.isBlank()) ? ("VF-" + (System.currentTimeMillis() % 1000000)) : carCode.trim(),
                        modelId, versionId, colorId, 1,
                        (origin == null || origin.isBlank()) ? "Việt Nam" : origin.trim(),
                        year, vin, engineNo,
                        (lotCode == null || lotCode.isBlank()) ? ("LOT-" + year) : lotCode.trim(),
                        price);

                Car imported = carInventoryService.importVehicle(car, employeeId, supplierId, note);
                sendResponse(request, response, jsonRequested, HttpServletResponse.SC_CREATED, true,
                        "Đã nhập kho xe mới VIN " + imported.getVin() + " thành công (Trạng thái: AVAILABLE).");
                return;
            }

            if ("exportCar".equalsIgnoreCase(action)) {
                if (!List.of("OPERATIONS", "ADMIN").contains(role)) {
                    sendResponse(request, response, jsonRequested, HttpServletResponse.SC_FORBIDDEN, false,
                            "Theo RDS (UC-09, 10.3), chỉ Operations Officer hoặc Admin mới được xuất kho xe.");
                    return;
                }
                int carId = Integer.parseInt(request.getParameter("carId"));
                String exportReason = request.getParameter("exportReason");
                int employeeId = resolveEmployeeId(session, user);

                carInventoryService.exportVehicle(carId, employeeId, exportReason);
                sendResponse(request, response, jsonRequested, HttpServletResponse.SC_OK, true,
                        "Đã xuất kho xe #" + carId + " thành công (Ghi nhận giao dịch EXPORT trong kho).");
                return;
            }

            if ("updateCarStatus".equalsIgnoreCase(action)) {
                if (!List.of("OPERATIONS", "SALES", "ADMIN", "MANAGER").contains(role)) {
                    sendResponse(request, response, jsonRequested, HttpServletResponse.SC_FORBIDDEN, false,
                            "Bạn không có quyền cập nhật trạng thái xe.");
                    return;
                }
                int carId = Integer.parseInt(request.getParameter("carId"));
                String statusName = request.getParameter("statusName");
                if (!carInventoryService.updateCarStatus(carId, statusName)) {
                    throw new IllegalStateException("Xe không đủ điều kiện cập nhật trạng thái kho.");
                }
                sendResponse(request, response, jsonRequested, HttpServletResponse.SC_OK, true,
                        "Đã cập nhật trạng thái xe #" + carId + " sang " + statusName + ".");
                return;
            }

            // Default: handover
            if (!"OPERATIONS".equals(role)) {
                sendResponse(request, response, jsonRequested, HttpServletResponse.SC_FORBIDDEN, false,
                        "Chỉ Operations Officer có quyền xác nhận bàn giao xe.");
                return;
            }
            if (!PaymentController.validCsrf(request)) {
                sendResponse(request, response, jsonRequested, HttpServletResponse.SC_FORBIDDEN, false,
                        "Phiên thao tác không hợp lệ.");
                return;
            }
            int employeeId = resolveEmployeeId(session, user);
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            String address = request.getParameter("deliveryAddress");
            String note = request.getParameter("note");
            Delivery delivered = deliveryService.completeHandover(orderId, employeeId, address, note);
            sendResponse(request, response, jsonRequested, HttpServletResponse.SC_OK, true,
                    "Đã xác nhận xuất kho và bàn giao xe VIN " + delivered.getVin()
                    + " cho đơn hàng #" + orderId + ". Đơn hàng đã hoàn tất (COMPLETED).");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            sendResponse(request, response, jsonRequested, HttpServletResponse.SC_BAD_REQUEST, false, ex.getMessage());
        } catch (RuntimeException ex) {
            log("Delivery POST failed", ex);
            sendResponse(request, response, jsonRequested, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Không thể xử lý nghiệp vụ kho/bàn giao: " + ex.getMessage());
        }
    }

    private int resolveEmployeeId(HttpSession session, SessionUser user) {
        Integer empId = session == null ? null : (Integer) session.getAttribute("employeeId");
        if (empId != null && empId > 0) return empId;
        if (user != null && user.getUserId() > 0) {
            String sql = "SELECT EmployeeID FROM Employees WHERE UserID = ?";
            try (Connection conn = DBContext.openConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, user.getUserId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        if (session != null) session.setAttribute("employeeId", id);
                        return id;
                    }
                }
            } catch (SQLException ignored) {}
        }
        if (user != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
            try (Connection conn = DBContext.openConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT TOP 1 EmployeeID FROM Employees ORDER BY EmployeeID");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    if (session != null) session.setAttribute("employeeId", id);
                    return id;
                }
            } catch (SQLException ignored) {}
        }
        throw new IllegalStateException("Không tìm thấy thông tin nhân viên hợp lệ trong phiên đăng nhập.");
    }

    private void sendResponse(HttpServletRequest req, HttpServletResponse resp, boolean jsonRequested,
                              int status, boolean success, String message) throws IOException {
        if (jsonRequested) {
            resp.setStatus(status);
            resp.setContentType("application/json;charset=UTF-8");
            JSON.writeValue(resp.getWriter(), Map.of("success", success, "message", message));
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/staff/dashboard#handover");
    }
}
