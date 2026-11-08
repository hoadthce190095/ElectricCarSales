package com.electriccarsales.controller;

import com.electriccarsales.model.ApiException;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple Servlet helper for the existing interactive Admin/Reports/AI screens.
 * Regular features use form parameters + JSP. No Jackson or Spring involved.
 */
public abstract class JsonServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(JsonServlet.class.getName());
    private static final Set<String> SECRETS = Set.of(
        "password", "passwordHash", "currentPassword", "newPassword", "confirmPassword");
    protected static final Gson JSON = new GsonBuilder().serializeNulls()
        .addSerializationExclusionStrategy(new ExclusionStrategy() {
            @Override public boolean shouldSkipField(FieldAttributes field) {
                return SECRETS.contains(field.getName());
            }
            @Override public boolean shouldSkipClass(Class<?> clazz) { return false; }
        })
        .registerTypeAdapter(LocalDate.class, stringAdapter(LocalDate::parse))
        .registerTypeAdapter(LocalDateTime.class, stringAdapter(LocalDateTime::parse))
        .registerTypeAdapter(Instant.class, stringAdapter(Instant::parse))
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create();

    private static <T> TypeAdapter<T> stringAdapter(java.util.function.Function<String,T> parser) {
        return new TypeAdapter<>() {
            @Override public void write(JsonWriter out, T value) throws IOException {
                if (value == null) out.nullValue(); else out.value(value.toString());
            }
            @Override public T read(JsonReader in) throws IOException {
                if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                    in.nextNull(); return null;
                }
                return parser.apply(in.nextString());
            }
        };
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        try {
            super.service(request, response);
        } catch (ApiException e) {
            error(response, e.getStatus(), e.getMessage());
        } catch (IllegalArgumentException e) {
            error(response, 400, e.getMessage() == null ? "Du lieu khong hop le" : e.getMessage());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Request failed: " + request.getRequestURI(), e);
            error(response, 503, "Khong the xu ly. Kiem tra database va server.");
        }
    }

    protected <T> T readJson(HttpServletRequest request, Class<T> type) throws IOException {
        if (request.getContentLengthLong() > 65536) throw new ApiException(413, "Yeu cau qua lon.");
        if (request.getContentType() == null
            || !request.getContentType().toLowerCase().startsWith("application/json")) {
            throw new ApiException(415, "Yeu cau phai la application/json.");
        }
        try (InputStream stream = request.getInputStream()) {
            byte[] bytes = stream.readNBytes(65537);
            if (bytes.length > 65536) throw new ApiException(413, "Yeu cau qua lon.");
            T value = JSON.fromJson(new String(bytes, StandardCharsets.UTF_8), type);
            if (value == null) throw new ApiException(400, "Thieu noi dung yeu cau.");
            return value;
        } catch (JsonParseException e) {
            throw new ApiException(400, "Noi dung JSON khong hop le.");
        }
    }

    protected void writeJson(HttpServletResponse response, Object value) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        JSON.toJson(value, response.getWriter());
    }

    public static void error(HttpServletResponse response, int status, String message) throws IOException {
        if (response.isCommitted()) return;
        response.resetBuffer();
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        JSON.toJson(Map.of("error", message == null ? "Error" : message), response.getWriter());
    }

    protected int integer(String value, String field) {
        try { return Integer.parseInt(value); }
        catch (RuntimeException e) { throw new ApiException(400, field + " phai la so nguyen."); }
    }

    @FunctionalInterface protected interface ThrowingRunnable { void run() throws Exception; }

    protected void handle(HttpServletRequest request, HttpServletResponse response, ThrowingRunnable action)
        throws IOException {
        try { action.run(); }
        catch (ApiException | IllegalArgumentException e) { throw e; }
        catch (SQLException e) {
            LOG.log(Level.SEVERE, "Database request failed", e);
            throw new ApiException(503, "Khong the doc co so du lieu.");
        } catch (Exception e) { throw new IOException(e); }
    }
}
