package com.electriccarsales.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import util.DBContext;
import com.electriccarsales.model.LoginRequest;
import com.electriccarsales.model.Account;
import com.electriccarsales.dao.AuthenticationDAO;
import com.electriccarsales.dao.SystemLogDAO;
import com.electriccarsales.service.PasswordHasher;
import com.electriccarsales.service.PasswordSession;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.AuthorizationService;
import com.electriccarsales.service.PasswordChangeService;
import com.electriccarsales.model.ApiException;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/api/auth")
public class AuthenticationController extends JsonServlet {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationController.class.getName());
    private static final ConcurrentHashMap<String, Attempts> ATTEMPTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Attempts> PASSWORD_ATTEMPTS = new ConcurrentHashMap<>();
    private static final String DUMMY_PASSWORD_HASH = PasswordHasher.hash("invalid-account-password");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthenticationDAO authenticationDAO;
    private final SystemLogDAO systemLogDAO;
    private final PasswordChangeService passwordChangeService;

    public AuthenticationController() {
        this(new AuthenticationDAO(), new SystemLogDAO());
    }

    public AuthenticationController(AuthenticationDAO authenticationDAO, SystemLogDAO systemLogDAO) {
        this.authenticationDAO = authenticationDAO;
        this.systemLogDAO = systemLogDAO;
        this.passwordChangeService = new PasswordChangeService(authenticationDAO, systemLogDAO);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeJson(response, state(request));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, () -> {
            LoginRequest input = readJson(request, LoginRequest.class);
            if ("logout".equals(input.getAction())) {
                logout(request, response);
                return;
            }
            if ("changePassword".equals(input.getAction())) {
                changePassword(request, response, input);
                return;
            }
            if (!"login".equals(input.getAction())) {
                throw new ApiException(400, "Thao tác đăng nhập không hợp lệ.");
            }
            login(request, response, input);
        });
    }

    private void login(HttpServletRequest request, HttpServletResponse response, LoginRequest input)
        throws SQLException, IOException {
        if (input.getUsername() == null || input.getUsername().isBlank() || input.getUsername().length() > 50
            || input.getPassword() == null || input.getPassword().length() > 128) {
            throw new ApiException(400, "Nhập tên đăng nhập và mật khẩu hợp lệ.");
        }

        String remoteAddress = request.getRemoteAddr();
        long now = System.currentTimeMillis();
        checkAttempts(ATTEMPTS, remoteAddress, now);

        try (Connection connection = DBContext.openConnection()) {
            Account account = authenticationDAO.byUsername(connection, input.getUsername().trim());
            String passwordHash = account == null ? DUMMY_PASSWORD_HASH : account.getPasswordHash();
            boolean passwordValid = PasswordHasher.verify(input.getPassword(), passwordHash);
            if (!passwordValid || account == null || !"ACTIVE".equals(account.getStatus())) {
                recordFailedAttempt(ATTEMPTS, remoteAddress, now);
                systemLogDAO.record(connection, account == null ? null : account.getUserId(), "LOGIN_FAILED", "USER",
                    account == null ? null : String.valueOf(account.getUserId()), "Đăng nhập không thành công");
                throw new ApiException(401, "Tên đăng nhập, mật khẩu hoặc trạng thái tài khoản không hợp lệ.");
            }

            systemLogDAO.record(connection, account.getUserId(), "LOGIN", "USER", String.valueOf(account.getUserId()),
                "Đăng nhập thành công");
            List<String> permissions = AuthorizationService.permissions(connection, account.getRoleId(),
                account.getRole());
            ATTEMPTS.remove(remoteAddress);
            createLoginSession(request, account, permissions, connection);
            writeJson(response, state(request));
        }
    }

    private void changePassword(HttpServletRequest request, HttpServletResponse response, LoginRequest input)
        throws SQLException, IOException {
        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("user");
        if (user == null) {
            throw new ApiException(401, "Bạn cần đăng nhập để đổi mật khẩu.");
        }
        String attemptKey = request.getRemoteAddr() + ":" + user.getUserId();
        long now = System.currentTimeMillis();
        checkAttempts(PASSWORD_ATTEMPTS, attemptKey, now);
        try {
            PasswordChangeService.ChangeResult changed = passwordChangeService.changePassword(user.getUserId(),
                session.getAttribute(PasswordSession.ATTRIBUTE), input);
            PASSWORD_ATTEMPTS.remove(attemptKey);
            try (Connection connection = DBContext.openConnection()) {
                createLoginSession(request, changed.account(), changed.permissions(), connection);
            }
            Map<String, Object> result = state(request);
            result.put("message", "Đã đổi mật khẩu. Các phiên đăng nhập khác đã hết hiệu lực.");
            writeJson(response, result);
        } catch (PasswordChangeService.InvalidCurrentPasswordException exception) {
            recordFailedAttempt(PASSWORD_ATTEMPTS, attemptKey, now);
            throw exception;
        } catch (ApiException exception) {
            if (exception.getStatus() == 401) {
                session.invalidate();
            }
            throw exception;
        }
    }

    static void createLoginSession(HttpServletRequest request, Account account, List<String> permissions,
        Connection connection) throws SQLException {
        String email;
        String phone;
        Integer employeeId;
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT u.Email,u.Phone,e.EmployeeID FROM Users u LEFT JOIN Employees e ON e.UserID=u.UserID"
                + " WHERE u.UserID=?")) {
            statement.setInt(1, account.getUserId());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new ApiException(401, "Tài khoản không còn tồn tại.");
                }
                email = result.getString("Email");
                phone = result.getString("Phone");
                employeeId = result.getObject("EmployeeID", Integer.class);
            }
        }
        HttpSession previous = request.getSession(false);
        if (previous != null) {
            previous.invalidate();
        }
        HttpSession session = request.getSession();
        session.setAttribute("user", new SessionUser(account.getUserId(), account.getUsername(), account.getRole()));
        session.setAttribute(PasswordSession.ATTRIBUTE, PasswordSession.fingerprint(account.getPasswordHash()));
        session.setAttribute("userID", account.getUserId());
        session.setAttribute("username", account.getUsername());
        session.setAttribute("roleID", account.getRoleId());
        session.setAttribute("roleName", account.getRole());
        session.setAttribute("email", email);
        session.setAttribute("phone", phone);
        if (employeeId != null && employeeId > 0) {
            session.setAttribute("employeeId", employeeId);
            session.setAttribute("role", account.getRole());
        }
        request.setAttribute("permissions", permissions);
    }

    private void checkAttempts(ConcurrentHashMap<String, Attempts> attemptsByKey, String key, long now) {
        attemptsByKey.entrySet().removeIf(entry -> entry.getValue().until() < now);
        Attempts attempts = attemptsByKey.get(key);
        if (attempts != null && attempts.count() >= 5) {
            throw new ApiException(429, "Xác thực sai nhiều lần. Thử lại sau 5 phút.");
        }
    }

    private void recordFailedAttempt(ConcurrentHashMap<String, Attempts> attemptsByKey, String key, long now) {
        if (attemptsByKey.size() < 10000 || attemptsByKey.containsKey(key)) {
            attemptsByKey.compute(key,
                (attemptKey, previous) -> new Attempts(previous == null ? 1 : previous.count() + 1,
                    previous == null ? now + 300000 : previous.until()));
        }
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser user = (SessionUser) request.getSession().getAttribute("user");
        request.getSession().invalidate();
        String auditWarning = null;
        if (user != null) {
            try (Connection connection = DBContext.openConnection()) {
                systemLogDAO.record(connection, user.getUserId(), "LOGOUT", "USER", String.valueOf(user.getUserId()),
                    "Đăng xuất");
            } catch (SQLException exception) {
                LOGGER.log(Level.WARNING, "Logout audit unavailable", exception);
                auditWarning = "Đã đăng xuất; chưa thể ghi nhật ký do cơ sở dữ liệu không khả dụng.";
            }
        }
        Map<String, Object> result = state(request);
        if (auditWarning != null) {
            result.put("warning", auditWarning);
        }
        writeJson(response, result);
    }

    private Map<String, Object> state(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Object timeout = request.getAttribute("sessionTimeoutMinutes");
        session.setMaxInactiveInterval(timeout instanceof Integer minutes ? minutes * 60 : 1800);
        String token = (String) session.getAttribute("csrfToken");
        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute("csrfToken", token);
        }
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("user", session.getAttribute("user"));
        state.put("csrfToken", token);
        state.put("permissions",
            session.getAttribute("user") == null ? List.of() : request.getAttribute("permissions"));
        return state;
    }

    private record Attempts(int count, long until) {
    }
}
