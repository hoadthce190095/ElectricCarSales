package com.electriccarsales.service;

import util.DBContext;
import com.electriccarsales.model.CreateRoleRequest;
import com.electriccarsales.model.PermissionDTO;
import com.electriccarsales.model.RoleRequest;
import com.electriccarsales.model.RolesResponse;
import com.electriccarsales.model.Account;
import com.electriccarsales.model.Role;
import com.electriccarsales.dao.AuthenticationDAO;
import com.electriccarsales.dao.RoleDAO;
import com.electriccarsales.dao.SystemLogDAO;
import com.electriccarsales.model.ApiException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AuthorizationService {
    private static final List<PermissionDTO> DEFINITIONS = List.of(
        new PermissionDTO("MANAGE_USERS", "Quản lý tài khoản và nhân viên"),
        new PermissionDTO("MANAGE_PROMOTIONS", "Quản lý khuyến mãi"),
        new PermissionDTO("VIEW_REPORTS", "Xem và xuất báo cáo tài chính, kinh doanh"),
        new PermissionDTO("VIEW_LOGS", "Xem nhật ký hệ thống"));

    private final RoleDAO roleDAO;
    private final AuthenticationDAO authenticationDAO;
    private final SystemLogDAO systemLogDAO;

    public AuthorizationService() {
        this(new RoleDAO(), new AuthenticationDAO(), new SystemLogDAO());
    }

    public AuthorizationService(RoleDAO roleDAO, AuthenticationDAO authenticationDAO, SystemLogDAO systemLogDAO) {
        this.roleDAO = roleDAO;
        this.authenticationDAO = authenticationDAO;
        this.systemLogDAO = systemLogDAO;
    }

    public static List<String> permissions(Connection connection, int roleId, String role) throws SQLException {
        if ("ADMIN".equals(role)) {
            return DEFINITIONS.stream().map(PermissionDTO::getCode).toList();
        }
        return new RoleDAO().findPermissions(connection, roleId);
    }

    public RolesResponse roles() throws SQLException {
        try (Connection connection = DBContext.openConnection()) {
            List<Role> roles = roleDAO.findAll(connection);
            for (Role role : roles) {
                role.setPermissions(permissions(connection, role.getRoleId(), role.getRoleName()));
            }
            List<PermissionDTO> definitions = DEFINITIONS.stream()
                .map(permission -> new PermissionDTO(permission.getCode(), permission.getDescription())).toList();
            return new RolesResponse(roles, definitions);
        }
    }

    public Role createRole(int actorId, CreateRoleRequest input) throws SQLException {
        if (input == null || input.getRoleName() == null || !input.getRoleName().matches("[A-Z][A-Z0-9_]{2,19}")) {
            throw new IllegalArgumentException("Tên vai trò gồm 3–20 chữ IN HOA, số hoặc gạch dưới, bắt đầu bằng chữ.");
        }
        if (Set.of("ADMIN", "CUSTOMER").contains(input.getRoleName())) {
            throw new IllegalArgumentException("Vai trò này được bảo vệ.");
        }

        try (Connection connection = DBContext.openConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                requireAdmin(connection, actorId, "Chỉ Admin được tạo vai trò.");
                List<String> permissionList = input.getPermissions();
                if (input.getCloneRoleId() != null) {
                    String sourceRole = roleDAO.findRoleName(connection, input.getCloneRoleId());
                    if (sourceRole == null) {
                        throw new ApiException(404, "Vai trò gốc không tồn tại.");
                    }
                    if ("ADMIN".equals(sourceRole)) {
                        throw new IllegalArgumentException("Không thể sao chép vai trò Admin.");
                    }
                    permissionList = permissions(connection, input.getCloneRoleId(), sourceRole);
                }
                validatePermissions(permissionList);
                int roleId = roleDAO.create(connection, input.getRoleName());
                roleDAO.addPermissions(connection, roleId, permissionList);
                systemLogDAO.record(connection, actorId, "ROLE_CREATE", "ROLE", String.valueOf(roleId),
                    "role=" + input.getRoleName() + "; permissions=" + permissionList + "; cloneRoleId="
                        + input.getCloneRoleId());
                connection.commit();
                return new Role(roleId, input.getRoleName(), List.copyOf(permissionList));
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                if (exception instanceof SQLException sqlException && (sqlException.getErrorCode() == 2601
                    || sqlException.getErrorCode() == 2627 || "23505".equals(sqlException.getSQLState()))) {
                    throw new ApiException(409, "Tên vai trò đã tồn tại.");
                }
                throw exception;
            }
        }
    }

    public Role updateRole(int actorId, int roleId, RoleRequest input) throws SQLException {
        if (input == null) {
            throw new IllegalArgumentException("Danh sách quyền không hợp lệ.");
        }
        validatePermissions(input.getPermissions());

        try (Connection connection = DBContext.openConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                requireAdmin(connection, actorId, "Chỉ Admin được thay đổi phân quyền.");
                String roleName = roleDAO.findRoleName(connection, roleId);
                if (roleName == null) {
                    throw new ApiException(404, "Không tìm thấy vai trò.");
                }
                if ("ADMIN".equals(roleName)) {
                    throw new IllegalArgumentException("Quyền Admin được bảo vệ để tránh mất quyền quản trị.");
                }
                if ("CUSTOMER".equals(roleName) && !input.getPermissions().isEmpty()) {
                    throw new IllegalArgumentException("Không thể cấp quyền nội bộ cho khách hàng.");
                }

                List<String> previousPermissions = permissions(connection, roleId, roleName);
                roleDAO.replacePermissions(connection, roleId, input.getPermissions());
                systemLogDAO.record(connection, actorId, "ROLE_PERMISSIONS_UPDATE", "ROLE", String.valueOf(roleId),
                    "role=" + roleName + "; before=" + previousPermissions + "; after=" + input.getPermissions());
                connection.commit();
                return new Role(roleId, roleName, List.copyOf(input.getPermissions()));
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void requireAdmin(Connection connection, int actorId, String message) throws SQLException {
        Account account = authenticationDAO.byId(connection, actorId);
        if (account == null || !"ADMIN".equals(account.getRole()) || !"ACTIVE".equals(account.getStatus())) {
            throw new ApiException(403, message);
        }
    }

    private void validatePermissions(List<String> permissions) {
        if (permissions == null || permissions.stream().anyMatch(Objects::isNull)
            || new HashSet<>(permissions).size() != permissions.size()
            || !DEFINITIONS.stream().map(PermissionDTO::getCode).toList().containsAll(permissions)) {
            throw new IllegalArgumentException("Danh sách quyền không hợp lệ.");
        }
    }
}
