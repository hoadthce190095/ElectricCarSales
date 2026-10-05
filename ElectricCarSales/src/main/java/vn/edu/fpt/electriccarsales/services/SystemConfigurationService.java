package vn.edu.fpt.electriccarsales.services;

import vn.edu.fpt.electriccarsales.config.Database;
import vn.edu.fpt.electriccarsales.dto.ConfigurationRequest;
import vn.edu.fpt.electriccarsales.entities.Account;
import vn.edu.fpt.electriccarsales.entities.SystemConfiguration;
import vn.edu.fpt.electriccarsales.repositories.AuthenticationRepository;
import vn.edu.fpt.electriccarsales.repositories.SystemConfigurationRepository;
import vn.edu.fpt.electriccarsales.repositories.SystemLogRepository;
import vn.edu.fpt.electriccarsales.util.ApiException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class SystemConfigurationService {
    private static final Map<String, Definition> DEFINITIONS = Map.of("SESSION_TIMEOUT_MINUTES",
        new Definition("Thời gian hết hạn phiên", 5, 120, 30, "Số phút không hoạt động trước khi phải đăng nhập lại."),
        "REPORT_MAX_DAYS", new Definition("Khoảng thời gian báo cáo tối đa", 1, 3660, 3660,
            "Giới hạn số ngày được truy vấn trong một lần báo cáo."));

    private final SystemConfigurationRepository configurationRepository;
    private final AuthenticationRepository authenticationRepository;
    private final SystemLogRepository systemLogRepository;

    public SystemConfigurationService() {
        this(new SystemConfigurationRepository(), new AuthenticationRepository(), new SystemLogRepository());
    }

    public SystemConfigurationService(SystemConfigurationRepository configurationRepository,
        AuthenticationRepository authenticationRepository, SystemLogRepository systemLogRepository) {
        this.configurationRepository = configurationRepository;
        this.authenticationRepository = authenticationRepository;
        this.systemLogRepository = systemLogRepository;
    }

    public static int integer(Connection connection, String key) throws SQLException {
        Definition definition = DEFINITIONS.get(key);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown configuration");
        }
        String value = new SystemConfigurationRepository().findValue(connection, key);
        return value == null ? definition.defaultValue() : validate(definition, value);
    }

    private static int validate(Definition definition, String value) {
        try {
            int number = Integer.parseInt(value);
            if (number < definition.min() || number > definition.max()) {
                throw new NumberFormatException();
            }
            return number;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                "Giá trị phải là số nguyên từ " + definition.min() + " đến " + definition.max() + ".");
        }
    }

    public List<SystemConfiguration> list() throws SQLException {
        try (Connection connection = Database.getConnection()) {
            List<SystemConfiguration> configurations = new ArrayList<>();
            for (String key : new TreeSet<>(DEFINITIONS.keySet())) {
                configurations.add(read(connection, key));
            }
            return configurations;
        }
    }

    private SystemConfiguration read(Connection connection, String key) throws SQLException {
        Definition definition = DEFINITIONS.get(key);
        if (definition == null) {
            throw new ApiException(404, "Cấu hình không tồn tại.");
        }
        SystemConfiguration configuration = configurationRepository.findByKey(connection, key);
        if (configuration == null) {
            configuration = new SystemConfiguration();
            configuration.setKey(key);
            configuration.setValue(String.valueOf(definition.defaultValue()));
        }
        configuration.setName(definition.name());
        configuration.setType("INTEGER");
        configuration.setMin(definition.min());
        configuration.setMax(definition.max());
        configuration.setDescription(definition.description());
        return configuration;
    }

    public SystemConfiguration update(int actorId, String key, ConfigurationRequest input) throws SQLException {
        Definition definition = DEFINITIONS.get(key);
        if (definition == null) {
            throw new ApiException(404, "Cấu hình không tồn tại.");
        }
        if (input == null) {
            throw new IllegalArgumentException("Thiếu giá trị cấu hình.");
        }
        String value = String.valueOf(validate(definition, input.getValue()));
        try (Connection connection = Database.getConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                Account account = authenticationRepository.byId(connection, actorId);
                if (account == null || !"ADMIN".equals(account.getRole()) || !"ACTIVE".equals(account.getStatus())) {
                    throw new ApiException(403, "Chỉ Admin được đổi cấu hình.");
                }
                SystemConfiguration previous = read(connection, key);
                configurationRepository.save(connection, actorId, key, value);
                systemLogRepository.record(connection, actorId, "CONFIGURATION_UPDATE", "CONFIGURATION", key,
                    "before=" + previous.getValue() + "; after=" + value);
                SystemConfiguration result = read(connection, key);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private record Definition(String name, int min, int max, int defaultValue, String description) {
    }
}
