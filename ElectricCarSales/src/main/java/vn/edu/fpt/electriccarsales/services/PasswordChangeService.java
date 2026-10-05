package vn.edu.fpt.electriccarsales.services;

import vn.edu.fpt.electriccarsales.config.Database;
import vn.edu.fpt.electriccarsales.dto.LoginRequest;
import vn.edu.fpt.electriccarsales.entities.Account;
import vn.edu.fpt.electriccarsales.repositories.AuthenticationRepository;
import vn.edu.fpt.electriccarsales.repositories.SystemLogRepository;
import vn.edu.fpt.electriccarsales.security.PasswordHasher;
import vn.edu.fpt.electriccarsales.security.PasswordSession;
import vn.edu.fpt.electriccarsales.util.ApiException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class PasswordChangeService {
    private final AuthenticationRepository accounts;
    private final SystemLogRepository logs;

    public PasswordChangeService(AuthenticationRepository accounts, SystemLogRepository logs) {
        this.accounts = accounts;
        this.logs = logs;
    }

    public ChangeResult changePassword(int userId, Object savedFingerprint, LoginRequest input) throws SQLException {
        validate(input);
        try (Connection connection = Database.getConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                Account account = accounts.byId(connection, userId);
                if (account == null || !"ACTIVE".equals(account.getStatus())
                    || !PasswordSession.matches(savedFingerprint, account.getPasswordHash())) {
                    throw new ApiException(401, "Phiên đăng nhập đã hết hiệu lực. Đăng nhập lại.");
                }
                if (!PasswordHasher.verify(input.getCurrentPassword(), account.getPasswordHash())) {
                    throw new InvalidCurrentPasswordException();
                }
                String newHash = PasswordHasher.hash(input.getNewPassword());
                if (!accounts.changePassword(connection, userId, account.getPasswordHash(), newHash)) {
                    throw new ApiException(409, "Tài khoản vừa thay đổi. Đăng nhập lại rồi thử lại.");
                }
                List<String> permissions = AuthorizationService.permissions(connection, account.getRoleId(),
                    account.getRole());

                // Mật khẩu và nhật ký phải cùng thành công; nếu ghi nhật ký lỗi thì hoàn tác.
                logs.record(connection, userId, "PASSWORD_CHANGE", "USER", String.valueOf(userId),
                    "Người dùng tự đổi mật khẩu");
                connection.commit();
                account.setPasswordHash(newHash);
                return new ChangeResult(account, permissions);
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    exception.addSuppressed(rollbackError);
                }
                throw exception;
            }
        }
    }

    private void validate(LoginRequest input) {
        if (input.getCurrentPassword() == null || input.getCurrentPassword().isEmpty()
            || input.getCurrentPassword().length() > 128) {
            throw new IllegalArgumentException("Nhập mật khẩu hiện tại hợp lệ.");
        }
        PasswordHasher.validate(input.getNewPassword());
        if (!input.getNewPassword().equals(input.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }
        if (input.getNewPassword().equals(input.getCurrentPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
    }

    public static class InvalidCurrentPasswordException extends ApiException {
        public InvalidCurrentPasswordException() {
            super(400, "Mật khẩu hiện tại không đúng.");
        }
    }

    // Kết quả nội bộ để controller tạo session mới, không serialize Account ra JSON.
    public record ChangeResult(Account account, List<String> permissions) {
    }
}
