package com.electriccarsales.dao;

import com.electriccarsales.model.SystemConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class
SystemConfigurationDAO {
    private static final Map<String, SystemConfiguration> RUNTIME_CONFIGS = new ConcurrentHashMap<>();

    public SystemConfiguration findByKey(Connection connection, String key) throws SQLException {
        if (!SchemaTables.exists(connection, "SystemConfigurations")) {
            return RUNTIME_CONFIGS.get(key);
        }
        String sql = "SELECT ConfigValue,UpdatedBy,UpdatedDate FROM SystemConfigurations WHERE ConfigKey=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                Timestamp updatedDate = result.getTimestamp("UpdatedDate");
                return new SystemConfiguration(key, null, result.getString("ConfigValue"), null, 0, 0, null,
                    result.getObject("UpdatedBy", Integer.class),
                    updatedDate == null ? null : updatedDate.toLocalDateTime());
            }
        }
    }

    public String findValue(Connection connection, String key) throws SQLException {
        if (!SchemaTables.exists(connection, "SystemConfigurations")) {
            SystemConfiguration cfg = RUNTIME_CONFIGS.get(key);
            return cfg == null ? null : cfg.getValue();
        }
        try (PreparedStatement statement = connection
            .prepareStatement("SELECT ConfigValue FROM SystemConfigurations WHERE ConfigKey=?")) {
            statement.setString(1, key);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString("ConfigValue") : null;
            }
        }
    }

    public void save(Connection connection, int actorId, String key, String value) throws SQLException {
        if (!SchemaTables.exists(connection, "SystemConfigurations")) {
            SystemConfiguration cfg = new SystemConfiguration(key, null, value, null, 0, 0, null, actorId, LocalDateTime.now());
            RUNTIME_CONFIGS.put(key, cfg);
            return;
        }
        int changed;
        String updateSql = "UPDATE SystemConfigurations SET"
            + " ConfigValue=?,UpdatedBy=?,UpdatedDate=SYSDATETIME() WHERE ConfigKey=?";
        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
            statement.setString(1, value);
            statement.setInt(2, actorId);
            statement.setString(3, key);
            changed = statement.executeUpdate();
        }
        if (changed == 0) {
            String insertSql = "INSERT INTO SystemConfigurations(ConfigKey,ConfigValue,UpdatedBy) VALUES(?,?,?)";
            try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                statement.setString(1, key);
                statement.setString(2, value);
                statement.setInt(3, actorId);
                statement.executeUpdate();
            }
        }
    }
}
