package com.electriccarsales.dao;

import com.electriccarsales.model.AdministrationLookupsDTO;
import com.electriccarsales.model.LookupDTO;
import com.electriccarsales.model.PageResponse;
import com.electriccarsales.model.PromotionRequest;
import com.electriccarsales.model.UserRequest;
import com.electriccarsales.model.Employee;
import com.electriccarsales.model.AdminPromotion;
import com.electriccarsales.model.SystemLog;
import com.electriccarsales.model.AdminUser;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.math.BigDecimal;

public class AdministrationDAO {
    private static final String USERS = "SELECT"
        + " u.UserID,u.Username,u.Email,u.Phone,u.RoleID,r.RoleName,u.StatusID,s.StatusName,u.CreatedDate,e.EmployeeID,e.EmployeeName"
        + " FROM Users u JOIN Roles r ON r.RoleID=u.RoleID JOIN Statuses s ON"
        + " s.StatusID=u.StatusID LEFT JOIN Employees e ON e.UserID=u.UserID ";

    private boolean hasTable(Connection connection, String tableName) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT CASE WHEN OBJECT_ID(?, N'U') IS NOT NULL THEN 1 ELSE 0 END")) {
            ps.setString(1, "dbo." + tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        } catch (SQLException ex) {
            return false;
        }
    }

    public AdministrationLookupsDTO lookups(Connection connection) throws SQLException {
        AdministrationLookupsDTO result = new AdministrationLookupsDTO(lookup(connection, "SELECT RoleID,RoleName FROM Roles ORDER BY RoleID"),
            lookup(connection, "SELECT StatusID,StatusName FROM Statuses ORDER BY StatusID"),
            lookup(connection, "SELECT ModelID,ModelName FROM CarModels ORDER BY ModelName"));
        result.setModelMinPrices(modelMinPrices(connection));
        return result;
    }

    public Map<Integer, BigDecimal> modelMinPrices(Connection connection) throws SQLException {
        Map<Integer, BigDecimal> prices = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT ModelID,MIN(Price) AS MinPrice FROM Cars GROUP BY ModelID");
            ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                BigDecimal price = rows.getBigDecimal("MinPrice");
                if (price != null) {
                    prices.put(rows.getInt("ModelID"), price);
                }
            }
        }
        return prices;
    }

    private List<LookupDTO> lookup(Connection connection, String sql) throws SQLException {
        List<LookupDTO> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.add(new LookupDTO(resultSet.getInt(1), resultSet.getString(2)));
            }
        }
        return result;
    }

    public PageResponse<AdminUser> users(Connection connection, String query, int page) throws SQLException {
        return users(connection, query, page, null, null);
    }

    public PageResponse<AdminUser> users(Connection connection, String query, int page, Integer roleId, Integer statusId)
        throws SQLException {
        String where = " WHERE (u.Username LIKE ? ESCAPE '~' OR u.Email LIKE ? ESCAPE '~' OR"
            + " e.EmployeeName LIKE ? ESCAPE '~')";
        if (roleId != null) {
            where += " AND u.RoleID=?";
        }
        if (statusId != null) {
            where += " AND u.StatusID=?";
        }
        String search = "%" + query.replace("~", "~~").replace("%", "~%").replace("_", "~_").replace("[", "~[") + "%";
        long total;
        try (PreparedStatement statement = connection
            .prepareStatement("SELECT COUNT(*) FROM Users u LEFT JOIN Employees e ON e.UserID=u.UserID" + where)) {
            bindUserFilters(statement, search, roleId, statusId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                total = resultSet.getLong(1);
            }
        }
        List<AdminUser> result = new ArrayList<>();
        try (PreparedStatement statement = connection
            .prepareStatement(USERS + where + " ORDER BY u.UserID DESC OFFSET ? ROWS FETCH NEXT 20 ROWS ONLY")) {
            int nextParameter = bindUserFilters(statement, search, roleId, statusId);
            statement.setInt(nextParameter, (page - 1) * 20);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(user(resultSet));
                }
            }
        }
        return new PageResponse<>(result, page, total, 20);
    }

    private int bindUserFilters(PreparedStatement statement, String search, Integer roleId, Integer statusId)
        throws SQLException {
        statement.setNString(1, search);
        statement.setNString(2, search);
        statement.setNString(3, search);
        int nextParameter = 4;
        if (roleId != null) {
            statement.setInt(nextParameter++, roleId);
        }
        if (statusId != null) {
            statement.setInt(nextParameter++, statusId);
        }
        return nextParameter;
    }

    public AdminUser findUser(Connection connection, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(USERS + " WHERE u.UserID=?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? user(resultSet) : null;
            }
        }
    }

    private AdminUser user(ResultSet resultSet) throws SQLException {
        return new AdminUser(resultSet.getInt("UserID"), resultSet.getString("Username"), resultSet.getString("Email"),
            resultSet.getString("Phone"), resultSet.getInt("RoleID"), resultSet.getString("RoleName"),
            resultSet.getInt("StatusID"), resultSet.getString("StatusName"),
            resultSet.getTimestamp("CreatedDate").toLocalDateTime(), resultSet.getObject("EmployeeID", Integer.class),
            resultSet.getString("EmployeeName"));
    }

    public String role(Connection connection, int id) throws SQLException {
        return name(connection, "SELECT RoleName FROM Roles WHERE RoleID=?", id);
    }

    public String status(Connection connection, int id) throws SQLException {
        return name(connection, "SELECT StatusName FROM Statuses WHERE StatusID=?", id);
    }

    private String name(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    public int activeAdmins(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection
            .prepareStatement("SELECT COUNT(*) FROM Users u JOIN Roles r ON u.RoleID=r.RoleID"
                + " JOIN Statuses s ON u.StatusID=s.StatusID WHERE r.RoleName='ADMIN' AND s.StatusName='ACTIVE'");
            ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    public int createUser(Connection connection, UserRequest input, String hash) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO Users(Username,PasswordHash,Email,Phone,RoleID,StatusID) VALUES(?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, input.getUsername());
            statement.setString(2, hash);
            statement.setString(3, input.getEmail());
            statement.setString(4, input.getPhone());
            statement.setInt(5, input.getRoleId());
            statement.setInt(6, input.getStatusId());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public void updateUser(Connection connection, int id, UserRequest input, String hash) throws SQLException {
        boolean profile = input.getEmail() != null || input.getPhone() != null;
        String sql = "UPDATE Users SET RoleID=?,StatusID=?" + (profile ? ",Email=?,Phone=?" : "") + (hash == null ? "" : ",PasswordHash=?")
            + " WHERE UserID=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, input.getRoleId());
            statement.setInt(2, input.getStatusId());
            int i = 3;
            if (profile) {
                statement.setString(i++, input.getEmail());
                statement.setString(i++, input.getPhone());
            }
            if (hash != null) {
                statement.setString(i++, hash);
            }
            statement.setInt(i, id);
            statement.executeUpdate();
        }
    }

    public void saveEmployee(Connection connection, int userId, String name, String phone, String email)
        throws SQLException {
        try (PreparedStatement statement = connection
            .prepareStatement("UPDATE Employees SET EmployeeName=?,Phone=?,Email=? WHERE UserID=?")) {
            statement.setNString(1, name);
            statement.setString(2, phone);
            statement.setString(3, email);
            statement.setInt(4, userId);
            if (statement.executeUpdate() > 0) {
                return;
            }
        }
        try (PreparedStatement statement = connection
            .prepareStatement("INSERT INTO Employees(UserID,EmployeeName,Phone,Email) VALUES(?,?,?,?)")) {
            statement.setInt(1, userId);
            statement.setNString(2, name);
            statement.setString(3, phone);
            statement.setString(4, email);
            statement.executeUpdate();
        }
    }

    public List<Employee> employees(Connection connection) throws SQLException {
        List<Employee> result = new ArrayList<>();
        try (
            PreparedStatement statement = connection.prepareStatement(
                "SELECT e.EmployeeID,e.UserID,e.EmployeeName,e.Phone,e.Email,u.Username,r.RoleName,s.StatusName"
                    + " FROM Employees e JOIN Users u ON e.UserID=u.UserID JOIN"
                    + " Roles r ON r.RoleID=u.RoleID JOIN Statuses s ON"
                    + " s.StatusID=u.StatusID ORDER BY e.EmployeeName");
            ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.add(new Employee(resultSet.getInt(1), resultSet.getInt(2), resultSet.getString(3),
                    resultSet.getString(4), resultSet.getString(5), resultSet.getString(6), resultSet.getString(7),
                    resultSet.getString(8)));
            }
        }
        return result;
    }

    public List<AdminPromotion> promotions(Connection connection) throws SQLException {
        if (!hasTable(connection, "Promotions")) {
            return List.of();
        }
        List<AdminPromotion> result = new ArrayList<>();
        try (
            PreparedStatement statement = connection
                .prepareStatement("SELECT * FROM Promotions ORDER BY PromotionID DESC");
            ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                List<Integer> ids = new ArrayList<>();
                int id = resultSet.getInt("PromotionID");
                if (hasTable(connection, "PromotionCars")) {
                    try (PreparedStatement links = connection
                        .prepareStatement("SELECT ModelID FROM PromotionCars WHERE PromotionID=? ORDER BY ModelID")) {
                        links.setInt(1, id);
                        try (ResultSet linkResults = links.executeQuery()) {
                            while (linkResults.next()) {
                                ids.add(linkResults.getInt(1));
                            }
                        }
                    }
                }
                result.add(new AdminPromotion(id, resultSet.getNString("PromotionName"), resultSet.getNString("Description"),
                    resultSet.getString("DiscountType"), resultSet.getBigDecimal("DiscountValue"),
                    resultSet.getTimestamp("StartDate").toLocalDateTime(),
                    resultSet.getTimestamp("EndDate").toLocalDateTime(), resultSet.getString("Status"), ids));
            }
        }
        return result;
    }

    public int savePromotion(Connection connection, Integer id, PromotionRequest promotion) throws SQLException {
        if (!hasTable(connection, "Promotions")) {
            throw new com.electriccarsales.model.ApiException(503,
                "Bảng Promotions không có trong schema ElectricCarSalesDB.sql hiện tại.");
        }
        String sql = id == null
            ? "INSERT INTO Promotions(PromotionName,Description,DiscountType,DiscountValue,StartDate,EndDate,Status) VALUES(?,?,?,?,?,?,?)"
            : "UPDATE Promotions SET PromotionName=?,Description=?,DiscountType=?,DiscountValue=?,StartDate=?,EndDate=?,Status=? WHERE PromotionID=?";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setNString(1, promotion.getPromotionName());
            statement.setNString(2, promotion.getDescription());
            statement.setString(3, promotion.getDiscountType());
            statement.setBigDecimal(4, promotion.getDiscountValue());
            statement.setTimestamp(5, Timestamp.valueOf(promotion.getStartDate()));
            statement.setTimestamp(6, Timestamp.valueOf(promotion.getEndDate()));
            statement.setString(7, promotion.getStatus());
            if (id != null) {
                statement.setInt(8, id);
            }
            if (statement.executeUpdate() == 0) {
                throw new com.electriccarsales.model.ApiException(404, "Không tìm thấy khuyến mãi.");
            }
            if (id == null) {
                try (ResultSet resultSet = statement.getGeneratedKeys()) {
                    resultSet.next();
                    id = resultSet.getInt(1);
                }
            }
        }
        if (hasTable(connection, "PromotionCars")) {
            try (PreparedStatement statement = connection
                .prepareStatement("DELETE FROM PromotionCars WHERE PromotionID=?")) {
                statement.setInt(1, id);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection
                .prepareStatement("INSERT INTO PromotionCars(PromotionID,ModelID) VALUES(?,?)")) {
                for (int model : promotion.getModelIds()) {
                    statement.setInt(1, id);
                    statement.setInt(2, model);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        }
        return id;
    }

    public PageResponse<SystemLog> logs(Connection connection, String action, Integer userId, LocalDate from,
        LocalDate to, int page) throws SQLException {
        return logs(connection, action, userId, from, to, page, 20, "");
    }

    public PageResponse<SystemLog> logs(Connection connection, String action, Integer userId, LocalDate from,
        LocalDate to, int page, int pageSize, String targetType) throws SQLException {
        if (!hasTable(connection, "SystemLogs")) {
            List<SystemLog> mem = SystemLogDAO.memoryLogs().stream()
                .filter(l -> (action == null || action.isEmpty() || action.equalsIgnoreCase(l.getAction()))
                    && (userId == null || userId.equals(l.getUserId()))
                    && (targetType == null || targetType.isEmpty() || targetType.equalsIgnoreCase(l.getTargetType()))
                    && (from == null || !l.getCreatedDate().toLocalDate().isBefore(from))
                    && (to == null || !l.getCreatedDate().toLocalDate().isAfter(to)))
                .toList();
            int start = Math.min(mem.size(), Math.max(0, (page - 1) * pageSize));
            int end = Math.min(mem.size(), start + pageSize);
            return new PageResponse<>(mem.subList(start, end), page, mem.size(), pageSize);
        }
        String where = " WHERE (?='' OR l.Action=?) AND (? IS NULL OR l.UserID=?) AND l.CreatedDate>=? AND"
            + " l.CreatedDate<? AND (?='' OR l.TargetType=?)";
        long total;
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM SystemLogs l" + where)) {
            bindLogs(statement, action, userId, from, to);
            statement.setString(7, targetType);
            statement.setString(8, targetType);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                total = resultSet.getLong(1);
            }
        }
        List<SystemLog> result = new ArrayList<>();
        if (pageSize > 20 && total > 50000) {
            throw new IllegalArgumentException("Nhật ký vượt 50.000 dòng. Thu hẹp bộ lọc trước khi xuất.");
        }
        try (PreparedStatement statement = connection
            .prepareStatement("SELECT l.*,u.Username FROM SystemLogs l LEFT JOIN Users u ON u.UserID=l.UserID"
                + where + " ORDER BY l.LogID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY")) {
            bindLogs(statement, action, userId, from, to);
            statement.setString(7, targetType);
            statement.setString(8, targetType);
            statement.setInt(9, (page - 1) * pageSize);
            statement.setInt(10, pageSize);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new SystemLog(resultSet.getLong("LogID"), resultSet.getObject("UserID", Integer.class),
                        resultSet.getString("Username"), resultSet.getString("Action"),
                        resultSet.getString("TargetType"), resultSet.getString("TargetID"),
                        resultSet.getNString("Detail"), resultSet.getTimestamp("CreatedDate").toLocalDateTime()));
                }
            }
        }
        return new PageResponse<>(result, page, total, pageSize);
    }

    private void bindLogs(PreparedStatement statement, String action, Integer userId, LocalDate from, LocalDate to)
        throws SQLException {
        statement.setString(1, action);
        statement.setString(2, action);
        if (userId == null) {
            statement.setNull(3, Types.INTEGER);
            statement.setNull(4, Types.INTEGER);
        } else {
            statement.setInt(3, userId);
            statement.setInt(4, userId);
        }
        statement.setTimestamp(5, Timestamp.valueOf(from.atStartOfDay()));
        statement.setTimestamp(6, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
    }
}
