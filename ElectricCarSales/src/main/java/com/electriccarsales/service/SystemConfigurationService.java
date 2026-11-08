package com.electriccarsales.service;

import util.DBContext;
import com.electriccarsales.model.ConfigurationRequest;
import com.electriccarsales.model.Account;
import com.electriccarsales.model.SystemConfiguration;
import com.electriccarsales.dao.AuthenticationDAO;
import com.electriccarsales.dao.SystemConfigurationDAO;
import com.electriccarsales.dao.SystemLogDAO;
import com.electriccarsales.model.ApiException;

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

    private final SystemConfigurationDAO configurationDAO;
    private final AuthenticationDAO authenticationDAO;
    private final SystemLogDAO systemLogDAO;

    public SystemConfigurationService() {
        this(new SystemConfigurationDAO(), new AuthenticationDAO(), new SystemLogDAO());
    }

    public SystemConfigurationService(SystemConfigurationDAO configurationDAO, AuthenticationDAO authenticationDAO,
        SystemLogDAO systemLogDAO) {
        this.configurationDAO = configurationDAO;
        this.authenticationDAO = authenticationDAO;
        this.systemLogDAO = systemLogDAO;
    }

    public static int integer(Connection connection, String key) throws SQLException {
        Definition definition = DEFINITIONS.get(key);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown configuration");
        }
        String value = new SystemConfigurationDAO().findValue(connection, key);
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
        try (Connection connection = DBContext.openConnection()) {
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
        SystemConfiguration configuration = configurationDAO.findByKey(connection, key);
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
        try (Connection connection = DBContext.openConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                Account account = authenticationDAO.byId(connection, actorId);
                if (account == null || !"ADMIN".equals(account.getRole()) || !"ACTIVE".equals(account.getStatus())) {
                    throw new ApiException(403, "Chỉ Admin được đổi cấu hình.");
                }
                SystemConfiguration previous = read(connection, key);
                configurationDAO.save(connection, actorId, key, value);
                systemLogDAO.record(connection, actorId, "CONFIGURATION_UPDATE", "CONFIGURATION", key,
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
