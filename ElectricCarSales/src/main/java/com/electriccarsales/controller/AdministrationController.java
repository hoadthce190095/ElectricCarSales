package com.electriccarsales.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.electriccarsales.model.ConfigurationRequest;
import com.electriccarsales.model.CreateRoleRequest;
import com.electriccarsales.model.EmployeeRequest;
import com.electriccarsales.model.PageResponse;
import com.electriccarsales.model.PromotionRequest;
import com.electriccarsales.model.RoleRequest;
import com.electriccarsales.model.UserRequest;
import com.electriccarsales.model.SystemLog;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.AdministrationService;
import com.electriccarsales.service.AuthorizationService;
import com.electriccarsales.service.SystemConfigurationService;
import com.electriccarsales.model.ApiException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@WebServlet("/api/admin/*")
public class AdministrationController extends JsonServlet {
    private final AdministrationService administrationService;
    private final AuthorizationService authorizationService;
    private final SystemConfigurationService configurationService;

    public AdministrationController() {
        this(new AdministrationService(), new AuthorizationService(), new SystemConfigurationService());
    }

    public AdministrationController(AdministrationService administrationService,
        AuthorizationService authorizationService, SystemConfigurationService configurationService) {
        this.administrationService = administrationService;
        this.authorizationService = authorizationService;
        this.configurationService = configurationService;
    }

    private String[] route(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path == null ? new String[0] : path.substring(1).split("/");
    }

    private int actor(HttpServletRequest request) {
        return ((SessionUser) request.getSession().getAttribute("user")).getUserId();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> showPage(request, response));
    }

    // GET chỉ đọc dữ liệu; POST/PUT/DELETE xử lý thay đổi.
    private void showPage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] path = route(request);
        if (path.length != 1) {
            throw new ApiException(404, "Đường dẫn không tồn tại.");
        }
        if (path[0].equals("logs")) {
            String format = request.getParameter("format");
            boolean export = "csv".equalsIgnoreCase(format);
            if (format != null && !format.isBlank() && !export && !"json".equalsIgnoreCase(format)) {
                throw new ApiException(400, "Định dạng nhật ký phải là json hoặc csv.");
            }
            PageResponse<SystemLog> result = administrationService.logs(request.getParameter("action"),
                request.getParameter("userId") == null || request.getParameter("userId").isBlank()
                    ? null
                    : integer(request.getParameter("userId"), "UserID"),
                date(request, "from"), date(request, "to"), page(request), request.getParameter("targetType"), export,
                actor(request));
            if (export) {
                if (!result.isAvailable()) {
                    throw new ApiException(503, result.getMessage());
                }
                response.setContentType("text/csv;charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment; filename=system-logs.csv");
                var writer = response.getWriter();
                writer.write('\uFEFF');
                csv(writer, "LogID", "Thời gian", "Người dùng", "Thao tác", "Module", "Đối tượng", "Chi tiết");
                for (SystemLog log : result.getItems()) {
                    csv(writer, log.getLogId(), log.getCreatedDate(), log.getUsername(), log.getAction(),
                        log.getTargetType(), log.getTargetId(), log.getDetail());
                }
            } else {
                writeJson(response, result);
            }
            return;
        }
        Object result = switch (path[0]) {
            case "lookups" -> administrationService.lookups();
            case "roles" -> authorizationService.roles();
            case "configurations" -> configurationService.list();
            case "users" -> administrationService.users(request.getParameter("q"), page(request),
                optionalInteger(request, "roleId", "Vai trò"), optionalInteger(request, "statusId", "Trạng thái"));
            case "employees" -> administrationService.employees();
            case "promotions" -> administrationService.promotions();
            case "logs" -> administrationService.logs(request.getParameter("action"),
                request.getParameter("userId") == null || request.getParameter("userId").isBlank()
                    ? null
                    : integer(request.getParameter("userId"), "UserID"),
                date(request, "from"), date(request, "to"), page(request));
            default -> throw new ApiException(404, "Đường dẫn không tồn tại.");
        };
        writeJson(response, result);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> createData(request, response));
    }

    private void createData(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] path = route(request);
        if (path.length != 1) {
            throw new ApiException(404, "Đường dẫn không tồn tại.");
        }
        Object result = switch (path[0]) {
            case "users" -> administrationService.createUser(actor(request), readJson(request, UserRequest.class));
            case "roles" -> authorizationService.createRole(actor(request), readJson(request, CreateRoleRequest.class));
            case "promotions" ->
                administrationService.savePromotion(actor(request), null, readJson(request, PromotionRequest.class));
            default -> throw new ApiException(404, "Đường dẫn không tồn tại.");
        };
        response.setStatus(201);
        writeJson(response, result);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> updateData(request, response));
    }

    private void updateData(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] path = route(request);
        if (path.length != 2) {
            throw new ApiException(404, "Đường dẫn không tồn tại.");
        }
        if (path[0].equals("configurations")) {
            writeJson(response,
                configurationService.update(actor(request), path[1], readJson(request, ConfigurationRequest.class)));
            return;
        }
        int id = integer(path[1], "ID");
        Object result = switch (path[0]) {
            case "users" -> administrationService.updateUser(actor(request), id, readJson(request, UserRequest.class));
            case "roles" -> authorizationService.updateRole(actor(request), id, readJson(request, RoleRequest.class));
            case "employees" ->
                administrationService.updateEmployee(actor(request), id, readJson(request, EmployeeRequest.class));
            case "promotions" ->
                administrationService.savePromotion(actor(request), id, readJson(request, PromotionRequest.class));
            default -> throw new ApiException(404, "Đường dẫn không tồn tại.");
        };
        writeJson(response, result);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> deactivatePromotion(request, response));
    }

    private void deactivatePromotion(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] path = route(request);
        if (path.length != 2 || !path[0].equals("promotions")) {
            throw new ApiException(405, "Không hỗ trợ xóa dữ liệu này.");
        }
        administrationService.deactivatePromotion(actor(request), integer(path[1], "ID"));
        writeJson(response, Map.of("message", "Đã ngừng khuyến mãi."));
    }

    private int page(HttpServletRequest request) {
        return request.getParameter("page") == null ? 1 : integer(request.getParameter("page"), "Trang");
    }

    private Integer optionalInteger(HttpServletRequest request, String name, String field) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        return integer(value.trim(), field);
    }

    private void csv(java.io.Writer writer, Object... values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                writer.write(',');
            }
            String value = values[i] == null ? "" : String.valueOf(values[i]);
            if (value.stripLeading().matches("(?s)^[=+@\\-].*")) {
                value = "'" + value;
            }
            writer.write('"');
            writer.write(value.replace("\"", "\"\""));
            writer.write('"');
        }
        writer.write("\r\n");
    }

    private LocalDate date(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (java.time.format.DateTimeParseException e) {
            throw new ApiException(400, "Ngày " + name + " phải có định dạng YYYY-MM-DD.");
        }
    }
}
