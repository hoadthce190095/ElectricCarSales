package com.electriccarsales.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import util.DBContext;
import com.electriccarsales.model.AdminSetupRequest;
import com.electriccarsales.service.AdminSetupService;
import com.electriccarsales.model.ApiException;

import java.io.IOException;
import java.sql.Connection;
import java.util.Map;

@WebServlet("/api/setup")
public class AdminSetupController extends JsonServlet {
    private final AdminSetupService service = new AdminSetupService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            boolean available = false;
            if (allowedHere(request)) {
                try (Connection connection = DBContext.openConnection()) {
                    available = service.needsAdmin(connection);
                }
            }
            writeJson(response, Map.of("available", available, "username", "admin"));
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            if (!allowedHere(request)) {
                throw new ApiException(403, "Tạo Admin lần đầu chỉ được bật trên máy chạy web.");
            }
            AdminSetupRequest input = readJson(request, AdminSetupRequest.class);
            if (input.getPassword() == null || !input.getPassword().equals(input.getConfirmPassword())) {
                throw new ApiException(400, "Hai mật khẩu không trùng nhau.");
            }
            try (Connection connection = DBContext.openConnection()) {
                service.create(connection, input.getUsername(), input.getPassword(),
                    "Tạo Admin đầu tiên bằng màn hình cục bộ");
            }
            response.setStatus(201);
            writeJson(response,
                Map.of("created", true, "message", "Đã tạo Admin. Đăng nhập bằng mật khẩu bạn vừa đặt."));
        });
    }

    static boolean allowedHere(HttpServletRequest request) {
        // Mặc định tắt. Không tin X-Forwarded-For hoặc hostname do người gửi tự đặt.
        if (!Boolean.parseBoolean(System.getProperty("app.adminSetup.enabled", "false"))) {
            return false;
        }
        String address = request.getRemoteAddr();
        return "127.0.0.1".equals(address) || "::1".equals(address) || "0:0:0:0:0:0:0:1".equals(address);
    }
}
