package com.electriccarsales.service;

import util.DBContext;
import com.electriccarsales.model.AdministrationLookupsDTO;
import com.electriccarsales.model.EmployeeRequest;
import com.electriccarsales.model.LookupDTO;
import com.electriccarsales.model.PageResponse;
import com.electriccarsales.model.PromotionRequest;
import com.electriccarsales.model.UserRequest;
import com.electriccarsales.model.Employee;
import com.electriccarsales.model.AdminPromotion;
import com.electriccarsales.model.SystemLog;
import com.electriccarsales.model.AdminUser;
import com.electriccarsales.dao.AdministrationDAO;
import com.electriccarsales.dao.SystemLogDAO;
import com.electriccarsales.model.ApiException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdministrationService {
    @FunctionalInterface
    public interface Connections {
        Connection open() throws SQLException;
    }

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
                assertPermission(connection, actor, "MANAGE_USERS");
                validateReferences(connection, input);
                protectAdmin(connection, actor, null, repository.role(connection, input.getRoleId()));
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
        String hash = input.getPassword() == null || input.getPassword().isBlank()
            ? null
            : PasswordHasher.hash(input.getPassword());
        try (Connection connection = connections.open()) {
            beginTransaction(connection);
            try {
                assertPermission(connection, actor, "MANAGE_USERS");
                AdminUser old = repository.findUser(connection, id);
                if (old == null) {
                    throw new ApiException(404, "Không tìm thấy tài khoản.");
                }
                String role = repository.role(connection, input.getRoleId());
                String status = repository.status(connection, input.getStatusId());
                protectAdmin(connection, actor, old, role);
                if ("CUSTOMER".equals(old.getRole())) {
                    if (!"CUSTOMER".equals(role)) {
                        throw new IllegalArgumentException("Tài khoản khách hàng không được chuyển thành nhân viên.");
                    }
                    if (!Set.of("ACTIVE", "INACTIVE", "LOCKED").contains(status == null ? "" : status)) {
                        throw new IllegalArgumentException("Trạng thái không hợp lệ.");
                    }
                } else {
                    validateReferences(connection, input);
                }
                if (actor == id && (!old.getRole().equals(role) || !"ACTIVE".equals(status))) {
                    throw new IllegalArgumentException("Bạn không thể tự khóa hoặc đổi vai trò tài khoản đang dùng.");
                }
                if ("ADMIN".equals(old.getRole()) && "ACTIVE".equals(old.getStatus())
                    && (!"ADMIN".equals(role) || !"ACTIVE".equals(status))
                    && repository.activeAdmins(connection) <= 1) {
                    throw new IllegalArgumentException("Phải giữ ít nhất một Admin đang hoạt động.");
                }
                repository.updateUser(connection, id, input, hash);
                if (!"CUSTOMER".equals(role)) {
                    repository.saveEmployee(connection, id, input.getEmployeeName(), input.getPhone(),
                        input.getEmail());
                } else {
                    try (java.sql.PreparedStatement statement = connection
                        .prepareStatement("UPDATE Customers SET Phone=?,Email=? WHERE UserID=?")) {
                        statement.setString(1, input.getPhone());
                        statement.setString(2, input.getEmail());
                        statement.setInt(3, id);
                        statement.executeUpdate();
                    }
                }
                AdminUser changed = repository.findUser(connection, id);
                logs.record(connection, actor, "USER_UPDATE", "USER", String.valueOf(id),
                    "username=" + old.getUsername() + "; role: " + old.getRole() + " -> " + changed.getRole()
                        + "; status: " + old.getStatus() + " -> " + changed.getStatus() + "; email: " + old.getEmail()
                        + " -> " + changed.getEmail() + "; phone: " + old.getPhone() + " -> " + changed.getPhone()
                        + "; employeeName: " + old.getEmployeeName() + " -> " + changed.getEmployeeName()
                        + "; passwordReset=" + (hash != null));
                connection.commit();
                return changed;
            } catch (SQLException | RuntimeException exception) {
                rollbackTransaction(connection, exception);
                throw exception;
            }
        }
    }

    private void validateReferences(Connection connection, UserRequest input) throws SQLException {
        String role = repository.role(connection, input.getRoleId());
        String status = repository.status(connection, input.getStatusId());
        if (role == null || "CUSTOMER".equals(role)) {
            throw new IllegalArgumentException("Chọn một vai trò nhân viên hợp lệ.");
        }
        if (!Set.of("ACTIVE", "INACTIVE", "LOCKED").contains(status == null ? "" : status)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        }
        if (input.getEmployeeName() == null || input.getEmployeeName().isBlank()) {
            throw new IllegalArgumentException("Nhập họ tên nhân viên.");
        }
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

    public PageResponse<SystemLog> logs(String action, Integer userId, LocalDate from, LocalDate to, int page)
        throws SQLException {
        return logs(action, userId, from, to, page, "", false, null);
    }

    public PageResponse<SystemLog> logs(String action, Integer userId, LocalDate from, LocalDate to, int page,
        String targetType, boolean export, Integer actor) throws SQLException {
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
        if (action.length() > 50) {
            throw new IllegalArgumentException("Tên thao tác quá dài.");
        }
        if (targetType == null) {
            targetType = "";
        }
        if (targetType.length() > 50) {
            throw new IllegalArgumentException("Module tối đa 50 ký tự.");
        }
        try (Connection connection = connections.open()) {
            PageResponse<SystemLog> result = repository.logs(connection, action, userId, from, to, export ? 1 : page,
                export ? 50000 : 20, targetType);
            if (export) {
                logs.record(connection, actor, "LOG_EXPORT", "LOG", null, "from=" + from + "; to=" + to + "; action="
                    + action + "; targetType=" + targetType + "; rows=" + result.getItems().size());
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

    static UserRequest validateUser(UserRequest raw, boolean create) {
        if (raw == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu tài khoản.");
        }
        String username = create ? required(raw.getUsername(), "Tên đăng nhập", 50) : raw.getUsername();
        if (create && !username.matches("[A-Za-z0-9_.-]{3,50}")) {
            throw new IllegalArgumentException("Tên đăng nhập gồm 3–50 chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.");
        }
        if (raw.getRoleId() == null || raw.getRoleId() <= 0 || raw.getStatusId() == null || raw.getStatusId() <= 0) {
            throw new IllegalArgumentException("Chọn vai trò và trạng thái.");
        }
        if (create || raw.getPassword() != null && !raw.getPassword().isBlank()) {
            PasswordHasher.validate(raw.getPassword());
        }
        return new UserRequest(username, raw.getPassword(), email(raw.getEmail()), phone(raw.getPhone()),
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
        if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal("9999999999999999.99")) > 0
            || value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Giá trị giảm giá không hợp lệ, tối đa 2 chữ số thập phân.");
        }
        if ("PERCENTAGE".equals(promotion.getDiscountType()) && value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Phần trăm giảm giá từ 0 đến 100.");
        }
        if (promotion.getStartDate() == null || promotion.getEndDate() == null
            || promotion.getEndDate().isBefore(promotion.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        }
        if (promotion.getStartDate().getYear() < 1 || promotion.getEndDate().getYear() > 9999) {
            throw new IllegalArgumentException("Ngày khuyến mãi phải nằm trong năm 1–9999.");
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
        String email = required(value, "Email", 100);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        return email;
    }

    private static String phone(String value) {
        String phone = required(value, "Số điện thoại", 20);
        if (!phone.matches("\\+?[0-9]{8,15}")) {
            throw new IllegalArgumentException("Số điện thoại gồm 8–15 chữ số, có thể bắt đầu bằng +.");
        }
        return phone;
    }

    private static void validPage(int page) {
        if (page < 1 || page > 1000000) {
            throw new IllegalArgumentException("Trang không hợp lệ.");
        }
    }
}
