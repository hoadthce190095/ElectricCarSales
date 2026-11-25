package com.electriccarsales.service;

import com.electriccarsales.dao.AdministrationDAO;
import com.electriccarsales.dao.SystemLogDAO;
import com.electriccarsales.model.*;
import util.DBContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service nghiệp vụ quản trị: kiểm tra dữ liệu và phân quyền cho tài khoản, vai trò, khuyến mãi và nhật ký.
 */
public class AdministrationService {
    private final Connections connections;
    private final AdministrationDAO repository;
    private final SystemLogDAO logs;
    public AdministrationService() {
        this(DBContext::openConnection);
    }

    public AdministrationService(Connections connections) {
        this(connections, new AdministrationDAO(), new SystemLogDAO());
    }

    public AdministrationService(Connections connections, AdministrationDAO repository, SystemLogDAO logs) {
        this.connections = connections;
        this.repository = repository;
        this.logs = logs;
    }

    static UserRequest validateUser(UserRequest raw, boolean create) {
        if (raw == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu tài khoản.");
        }
        String username = create ? required(raw.getUsername(), "Tên đăng nhập", 50) : raw.getUsername();
        if (create && !username.matches("[A-Za-z0-9_]{5,30}")) {
            throw new IllegalArgumentException("Tên đăng nhập gồm 5–30 chữ, số hoặc dấu gạch dưới.");
        }
        if (raw.getRoleId() == null || raw.getRoleId() <= 0 || raw.getStatusId() == null || raw.getStatusId() <= 0) {
            throw new IllegalArgumentException("Chọn vai trò và trạng thái.");
        }
        // Chỉ tạo tài khoản mới được nhập mật khẩu; màn hình chỉnh sửa không nhận password.
        if (create) {
            PasswordHasher.validate(raw.getPassword());
        } else if (raw.getPassword() != null) {
            throw new IllegalArgumentException("Màn hình cập nhật tài khoản không nhận mật khẩu.");
        }
        return new UserRequest(username, raw.getPassword(),
            create || raw.getEmail() != null ? email(raw.getEmail()) : null,
            create || raw.getPhone() != null ? phone(raw.getPhone()) : null,
            raw.getRoleId(), raw.getStatusId(), optional(raw.getEmployeeName(), "Họ tên", 100));
    }

    public static PromotionRequest validatePromotion(PromotionRequest promotion) {
        if (promotion == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu khuyến mãi.");
        }
        String name = required(promotion.getPromotionName(), "Tên khuyến mãi", 150),
            description = optional(promotion.getDescription(), "Mô tả", 500);
        if (!Set.of("PERCENTAGE", "FIXED")
            .contains(promotion.getDiscountType() == null ? "" : promotion.getDiscountType())) {
            throw new IllegalArgumentException("Loại giảm giá phải là PERCENTAGE hoặc FIXED.");
        }
        BigDecimal value = promotion.getDiscountValue();
        if (value == null || value.signum() <= 0 || value.compareTo(new BigDecimal("9999999999999999.99")) > 0
            || value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Giá trị giảm giá phải lớn hơn 0, tối đa 2 chữ số thập phân.");
        }
        if ("PERCENTAGE".equals(promotion.getDiscountType()) && value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Phần trăm giảm giá phải lớn hơn 0 và không vượt quá 100.");
        }
        if (promotion.getStartDate() == null || promotion.getEndDate() == null
            || !promotion.getEndDate().isAfter(promotion.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu.");
        }
        if (promotion.getStartDate().getYear() < 1 || promotion.getEndDate().getYear() > 9999) {
            throw new IllegalArgumentException("Ngày khuyến mãi phải nằm trong năm 1–9999.");
        }
        if (promotion.getEndDate().isAfter(promotion.getStartDate().plusMonths(3))) {
            throw new IllegalArgumentException("Ngày kết thúc không được vượt quá 3 tháng lịch kể từ ngày bắt đầu.");
        }
        if (!Set.of("ACTIVE", "INACTIVE").contains(promotion.getStatus() == null ? "" : promotion.getStatus())) {
            throw new IllegalArgumentException("Trạng thái khuyến mãi không hợp lệ.");
        }
        List<Integer> ids = promotion.getModelIds();
        if (ids == null || ids.isEmpty() || ids.size() > 100 || new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("Chọn từ 1 đến 100 mẫu xe, không trùng nhau.");
        }
        for (Integer modelId : ids) {
            if (modelId == null || modelId <= 0) {
                throw new IllegalArgumentException("Chọn từ 1 đến 100 mẫu xe, không trùng nhau.");
            }
        }
        return new PromotionRequest(name, description, promotion.getDiscountType(), value, promotion.getStartDate(),
            promotion.getEndDate(), promotion.getStatus(), List.copyOf(ids));
    }

    private static String required(String value, String name, int max) {
        String result = optional(value, name, max);
        if (result == null || result.isBlank()) {
            throw new IllegalArgumentException(name + " không được trống.");
        }
        return result;
    }

    private static String optional(String value, String name, int max) {
        if (value == null) {
            return null;
        }
        value = value.trim();
        if (value.length() > max) {
            throw new IllegalArgumentException(name + " tối đa " + max + " ký tự.");
        }
        return value.isEmpty() ? null : value;
    }

    private static String email(String value) {
        String email = required(value, "Email", 30);
        if (!email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        return email;
    }

    private static String phone(String value) {
        String phone = required(value, "Số điện thoại", 20);
        if (!phone.matches("0[0-9]{9}")) {
            throw new IllegalArgumentException("Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.");
        }
        return phone;
    }

    private static void validPage(int page) {
        if (page < 1 || page > 1000000) {
            throw new IllegalArgumentException("Trang không hợp lệ.");
        }
    }

    public AdministrationLookupsDTO lookups() throws SQLException {
        try (Connection connection = connections.open()) {
            return repository.lookups(connection);
        }
    }

    public PageResponse<AdminUser> users(String query, int page) throws SQLException {
        return users(query, page, null, null);
    }

    public PageResponse<AdminUser> users(String query, int page, Integer roleId, Integer statusId) throws SQLException {
        if (query == null) {
            query = "";
        }
        if (query.length() > 100) {
            throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");
        }
        validPage(page);
        try (Connection connection = connections.open()) {
            if (roleId != null && (roleId <= 0 || repository.role(connection, roleId) == null)) {
                throw new IllegalArgumentException("Vai trò lọc không tồn tại.");
            }
            if (statusId != null && (statusId <= 0 || repository.status(connection, statusId) == null)) {
                throw new IllegalArgumentException("Trạng thái lọc không tồn tại.");
            }
            return repository.users(connection, query, page, roleId, statusId);
        }
    }

    public List<Employee> employees() throws SQLException {
        try (Connection connection = connections.open()) {
            return repository.employees(connection);
        }
    }

    public List<AdminPromotion> promotions() throws SQLException {
        try (Connection connection = connections.open()) {
            return repository.promotions(connection);
        }
    }

    public AdminUser createUser(int actor, UserRequest raw) throws SQLException {
        UserRequest input = validateUser(raw, true);
        String hash = PasswordHasher.hash(input.getPassword());
        try (Connection connection = connections.open()) {
            beginTransaction(connection);
            try {
                requireAccountAdmin(connection, actor);
                String role = repository.role(connection, input.getRoleId());
                if (role == null || "CUSTOMER".equals(role)) {
                    throw new IllegalArgumentException("Chọn một vai trò nhân viên hợp lệ.");
                }
                validateStatus(repository.status(connection, input.getStatusId()));
                required(input.getEmployeeName(), "Họ tên nhân viên", 100);
                int id = repository.createUser(connection, input, hash);
                repository.saveEmployee(connection, id, input.getEmployeeName(), input.getPhone(), input.getEmail());
                AdminUser created = repository.findUser(connection, id);
                logs.record(connection, actor, "USER_CREATE", "USER", String.valueOf(id), "Tạo nhân viên "
                    + created.getUsername() + "; role=" + created.getRole() + "; status=" + created.getStatus());
                connection.commit();
                return created;
            } catch (SQLException | RuntimeException exception) {
                rollbackTransaction(connection, exception);
                throw exception;
            }
        }
    }

    public AdminUser updateUser(int actor, int id, UserRequest raw) throws SQLException {
        UserRequest input = validateUser(raw, false);
        boolean profile = input.getEmail() != null || input.getPhone() != null || input.getEmployeeName() != null;
        try (Connection connection = connections.open()) {
            beginTransaction(connection);
            try {
                requireAccountAdmin(connection, actor);
                AdminUser old = existingAccount(connection, id);
                String role = existingRole(connection, input.getRoleId());
                String status = repository.status(connection, input.getStatusId());
                validateStatus(status);
                protectAccountTransition(connection, actor, old, role, status);
                repository.updateUser(connection, id, input);
                if (profile && "CUSTOMER".equals(role)) {
                    repository.updateCustomerProfile(connection, id, input);
                } else if (profile && old.getEmployeeId() != null) {
                    repository.saveEmployee(connection, id, input.getEmployeeName(), input.getPhone(),
                        input.getEmail());
                } else if (input.getEmployeeName() != null) {
                    String newPhone = input.getPhone() == null ? old.getPhone() : input.getPhone();
                    String newEmail = input.getEmail() == null ? old.getEmail() : input.getEmail();
                    repository.saveEmployee(connection, id, input.getEmployeeName(), newPhone, newEmail);
                }
                AdminUser changed = repository.findUser(connection, id);
                logs.record(connection, actor, "USER_UPDATE", "USER", String.valueOf(id),
                    "username=" + old.getUsername() + "; before=" + accountDetails(old)
                        + "; after=" + accountDetails(changed));
                connection.commit();
                return changed;
            } catch (SQLException | RuntimeException exception) {
                rollbackTransaction(connection, exception);
                throw exception;
            }
        }
    }

    public AdminUser assignUserRole(int actor, int targetUserId, int roleId) throws SQLException {
        try (Connection connection = connections.open()) {
            beginTransaction(connection);
            try {
                requireAccountAdmin(connection, actor);
                AdminUser old = existingAccount(connection, targetUserId);
                String role = existingRole(connection, roleId);
                protectAccountTransition(connection, actor, old, role, old.getStatus());
                repository.assignUserRole(connection, targetUserId, roleId);
                logs.record(connection, actor, "USER_ROLE_ASSIGN", "USER", String.valueOf(targetUserId),
                    "username=" + old.getUsername() + "; before=" + old.getRole() + " (#" + old.getRoleId()
                        + "); after=" + role + " (#" + roleId + ")");
                AdminUser result = repository.findUser(connection, targetUserId);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                rollbackTransaction(connection, exception);
                throw exception;
            }
        }
    }

    private void requireAccountAdmin(Connection connection, int actorId) throws SQLException {
        AdminUser actor = repository.findUser(connection, actorId);
        if (actor == null || !"ADMIN".equals(actor.getRole()) || !"ACTIVE".equals(actor.getStatus())) {
            throw new ApiException(403, "Chỉ Admin được quản trị tài khoản.");
        }
    }

    private AdminUser existingAccount(Connection connection, int id) throws SQLException {
        AdminUser account = repository.findUser(connection, id);
        if (account == null) {
            throw new ApiException(404, "EX-4: Không tìm thấy tài khoản đã chọn.");
        }
        return account;
    }

    private String existingRole(Connection connection, int roleId) throws SQLException {
        String role = repository.role(connection, roleId);
        if (role == null) {
            throw new ApiException(404, "EX-3: Không tìm thấy vai trò đã chọn.");
        }
        return role;
    }

    private void validateStatus(String status) {
        if (!Set.of("ACTIVE", "INACTIVE", "LOCKED").contains(status == null ? "" : status)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        }
    }

    private void protectAccountTransition(Connection connection, int actor, AdminUser old, String role, String status)
        throws SQLException {
        if ("CUSTOMER".equals(old.getRole()) != "CUSTOMER".equals(role)) {
            throw new IllegalArgumentException("Tài khoản khách hàng và tài khoản nội bộ dùng hồ sơ riêng.");
        }
        if (actor == old.getUserId() && (!old.getRole().equals(role) || !"ACTIVE".equals(status))) {
            throw new IllegalArgumentException("Bạn không thể tự khóa hoặc đổi vai trò tài khoản đang dùng.");
        }
        if ("ADMIN".equals(old.getRole()) && "ACTIVE".equals(old.getStatus())
            && (!"ADMIN".equals(role) || !"ACTIVE".equals(status)) && repository.activeAdmins(connection) <= 1) {
            throw new IllegalArgumentException("Phải giữ ít nhất một Admin đang hoạt động.");
        }
    }

    private String accountDetails(AdminUser user) {
        return "{name=" + user.getEmployeeName() + ", email=" + user.getEmail() + ", phone=" + user.getPhone()
            + ", role=" + user.getRole() + ", status=" + user.getStatus() + "}";
    }

    public Employee updateEmployee(int actor, int id, EmployeeRequest raw) throws SQLException {
        if (raw == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu nhân viên.");
        }
        final EmployeeRequest valid = new EmployeeRequest(required(raw.getEmployeeName(), "Họ tên", 100),
            phone(raw.getPhone()), email(raw.getEmail()));
        try (Connection connection = connections.open()) {
            beginTransaction(connection);
            try {
                assertPermission(connection, actor, "MANAGE_USERS");
                Employee old = findEmployee(connection, id);
                protectAdmin(connection, actor, repository.findUser(connection, old.getUserId()), old.getRole());
                repository.saveEmployee(connection, old.getUserId(), valid.getEmployeeName(), valid.getPhone(),
                    valid.getEmail());
                try (java.sql.PreparedStatement statement = connection
                    .prepareStatement("UPDATE Users SET Phone=?,Email=? WHERE UserID=?")) {
                    statement.setString(1, valid.getPhone());
                    statement.setString(2, valid.getEmail());
                    statement.setInt(3, old.getUserId());
                    statement.executeUpdate();
                }
                logs.record(connection, actor, "EMPLOYEE_UPDATE", "EMPLOYEE", String.valueOf(id),
                    "name: " + old.getEmployeeName() + " -> " + valid.getEmployeeName() + "; phone: " + old.getPhone()
                        + " -> " + valid.getPhone() + "; email: " + old.getEmail() + " -> " + valid.getEmail());
                connection.commit();
                return new Employee(id, old.getUserId(), valid.getEmployeeName(), valid.getPhone(), valid.getEmail(),
                    old.getUsername(), old.getRole(), old.getStatus());
            } catch (SQLException | RuntimeException exception) {
                rollbackTransaction(connection, exception);
                throw exception;
            }
        }
    }

    public AdminPromotion savePromotion(int actor, Integer id, PromotionRequest raw) throws SQLException {
        PromotionRequest input = validatePromotion(raw);
        try (Connection connection = connections.open()) {
            beginTransaction(connection);
            try {
                assertPermission(connection, actor, "MANAGE_PROMOTIONS");
                AdminPromotion old = null;
                if (id != null) {
                    old = findPromotion(connection, id);
                }
                Set<Integer> validModels = new HashSet<>();
                for (LookupDTO model : repository.lookups(connection).getModels()) {
                    validModels.add(model.getId());
                }
                if (!validModels.containsAll(input.getModelIds())) {
                    throw new IllegalArgumentException("Có mẫu xe không tồn tại.");
                }
                if ("FIXED".equals(input.getDiscountType())) {
                    var prices = repository.modelMinPrices(connection);
                    for (Integer modelId : input.getModelIds()) {
                        BigDecimal minimum = prices.get(modelId);
                        if (minimum != null && input.getDiscountValue().compareTo(minimum) > 0) {
                            throw new IllegalArgumentException("Giá trị giảm giá không được vượt giá xe thấp nhất của mẫu áp dụng ("
                                + minimum.toPlainString() + " VND).");
                        }
                    }
                }
                int key = repository.savePromotion(connection, id, input);
                logs.record(connection, actor, id == null ? "PROMOTION_CREATE" : "PROMOTION_UPDATE", "PROMOTION",
                    String.valueOf(key), "before=" + old + "; after=" + input);
                connection.commit();
                return new AdminPromotion(key, input.getPromotionName(), input.getDescription(), input.getDiscountType(),
                    input.getDiscountValue(), input.getStartDate(), input.getEndDate(), input.getStatus(),
                    input.getModelIds());
            } catch (SQLException | RuntimeException exception) {
                rollbackTransaction(connection, exception);
                throw exception;
            }
        }
    }

    public void deactivatePromotion(int actor, int id) throws SQLException {
        try (Connection connection = connections.open()) {
            beginTransaction(connection);
            try {
                assertPermission(connection, actor, "MANAGE_PROMOTIONS");
                try (java.sql.PreparedStatement statement = connection
                    .prepareStatement("UPDATE Promotions SET Status='INACTIVE' WHERE" + " PromotionID=?")) {
                    statement.setInt(1, id);
                    if (statement.executeUpdate() == 0) {
                        throw new ApiException(404, "Không tìm thấy khuyến mãi.");
                    }
                }
                logs.record(connection, actor, "PROMOTION_DEACTIVATE", "PROMOTION", String.valueOf(id),
                    "status -> INACTIVE");
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollbackTransaction(connection, exception);
                throw exception;
            }
        }
    }

    // Đọc hoặc xuất CSV nhật ký: chuẩn hóa LOGIN -> LOGIN_SUCCESS, kiểm tra source (SYSTEM/ALL) và ghi LOG_EXPORT.
    public PageResponse<SystemLog> logs(String action, Integer userId, LocalDate from, LocalDate to, int page,
        String targetType, String source, boolean export, Integer actor) throws SQLException {
        validPage(page);
        if (from == null) {
            from = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(30);
        }
        if (to == null) {
            to = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        }
        if (from.getYear() < 1 || to.isBefore(from) || to.isAfter(LocalDate.of(9998, 12, 31))) {
            throw new IllegalArgumentException("Khoảng ngày không hợp lệ.");
        }
        if (action == null) {
            action = "";
        }
        if ("LOGIN".equalsIgnoreCase(action)) {
            action = "LOGIN_SUCCESS";
        }
        if (action.length() > 50) {
            throw new IllegalArgumentException("Tên thao tác quá dài.");
        }
        if (targetType == null) {
            targetType = "";
        }
        if (targetType.length() > 50) {
            throw new IllegalArgumentException("Module tối đa 50 ký tự.");
        }
        boolean includeHistory = "ALL".equalsIgnoreCase(source);
        if (source != null && !source.isBlank() && !includeHistory && !"SYSTEM".equalsIgnoreCase(source)) {
            throw new IllegalArgumentException("Nguồn nhật ký không hợp lệ.");
        }
        try (Connection connection = connections.open()) {
            PageResponse<SystemLog> result = logs.list(connection, action, userId, from, to, export ? 1 : page,
                targetType, includeHistory, export);
            if (export) {
                logs.record(connection, actor, "LOG_EXPORT", "LOG", null, "from=" + from + "; to=" + to + "; action="
                    + action + "; targetType=" + targetType + "; source=" + (includeHistory ? "ALL" : "SYSTEM")
                    + "; rows=" + result.getItems().size());
            }
            return result;
        }
    }

    private void assertPermission(Connection connection, int id, String permission) throws SQLException {
        AdminUser actor = repository.findUser(connection, id);
        if (actor == null || !"ACTIVE".equals(actor.getStatus())
            || !AuthorizationService.permissions(connection, actor.getRoleId(), actor.getRole()).contains(permission)) {
            throw new ApiException(403, "Bạn không có quyền sử dụng chức năng này.");
        }
    }

    private void protectAdmin(Connection connection, int actorId, AdminUser target, String newRole) throws SQLException {
        AdminUser actor = repository.findUser(connection, actorId);
        if (!"ADMIN".equals(actor.getRole())
            && ("ADMIN".equals(newRole) || target != null && "ADMIN".equals(target.getRole()))) {
            throw new ApiException(403, "Chỉ Admin được quản lý tài khoản và vai trò Admin.");
        }
    }

    // Lưu dữ liệu và nhật ký cùng một transaction: lỗi ở bước nào thì hủy toàn bộ.
    private void beginTransaction(Connection connection) throws SQLException {
        connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        connection.setAutoCommit(false);
    }

    private void rollbackTransaction(Connection connection, Exception exception) throws SQLException {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            exception.addSuppressed(rollbackError);
        }
        if (exception instanceof SQLException sqlError) {
            boolean duplicate = sqlError.getErrorCode() == 2601 || sqlError.getErrorCode() == 2627
                || "23505".equals(sqlError.getSQLState());
            if (duplicate) {
                throw new ApiException(409, "Tên đăng nhập, email hoặc số điện thoại đã được sử dụng.");
            }
        }
    }

    private Employee findEmployee(Connection connection, int employeeId) throws SQLException {
        for (Employee employee : repository.employees(connection)) {
            if (employee.getEmployeeId() == employeeId) {
                return employee;
            }
        }
        throw new ApiException(404, "Không tìm thấy nhân viên.");
    }

    private AdminPromotion findPromotion(Connection connection, int promotionId) throws SQLException {
        for (AdminPromotion promotion : repository.promotions(connection)) {
            if (promotion.getPromotionId() == promotionId) {
                return promotion;
            }
        }
        throw new ApiException(404, "Không tìm thấy khuyến mãi.");
    }

    @FunctionalInterface
    public interface Connections {
        Connection open() throws SQLException;
    }
}
