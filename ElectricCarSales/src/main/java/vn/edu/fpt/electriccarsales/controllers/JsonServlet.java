package vn.edu.fpt.electriccarsales.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import vn.edu.fpt.electriccarsales.util.ApiException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class JsonServlet extends HttpServlet {
    protected static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final Logger LOG = Logger.getLogger(JsonServlet.class.getName());

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        try {
            super.service(request, response);
        } catch (ApiException e) {
            error(response, e.getStatus(), e.getMessage());
        } catch (IllegalArgumentException e) {
            error(response, 400, e.getMessage() == null ? "Dữ liệu không hợp lệ." : e.getMessage());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Request failed: " + request.getRequestURI(), e);
            error(response, 503, "Không thể xử lý yêu cầu. Kiểm tra kết nối cơ sở dữ liệu và cấu hình máy chủ.");
        }
    }

    protected <T> T readJson(HttpServletRequest request, Class<T> type) throws IOException {
        if (request.getContentLengthLong() > 65536) {
            throw new ApiException(413, "Yêu cầu quá lớn.");
        }
        if (request.getContentType() == null
            || !request.getContentType().toLowerCase().startsWith("application/json")) {
            throw new ApiException(415, "Yêu cầu phải dùng application/json.");
        }
        byte[] bytes = request.getInputStream().readNBytes(65537);
        if (bytes.length > 65536) {
            throw new ApiException(413, "Yêu cầu quá lớn.");
        }
        try {
            T value = JSON.readValue(bytes, type);
            if (value == null) {
                throw new ApiException(400, "Thiếu nội dung yêu cầu.");
            }
            return value;
        } catch (JsonProcessingException e) {
            throw new ApiException(400, "Nội dung JSON không hợp lệ.");
        }
    }

    protected void writeJson(HttpServletResponse response, Object value) throws IOException {
        JSON.writeValue(response.getWriter(), value);
    }

    public static void error(HttpServletResponse response, int status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        JSON.writeValue(response.getWriter(), Map.of("error", message));
    }

    protected int integer(String value, String field) {
        try {
            return Integer.parseInt(value);
        } catch (RuntimeException e) {
            throw new ApiException(400, field + " phải là số nguyên.");
        }
    }

    @FunctionalInterface
    protected interface ThrowingRunnable {
        void run() throws Exception;
    }

    protected void handle(HttpServletRequest request, HttpServletResponse response, ThrowingRunnable action)
        throws IOException {
        try {
            action.run();
        } catch (ApiException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Database request failed", e);
            throw new ApiException(503, "Không thể đọc cơ sở dữ liệu.");
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
