package com.electriccarsales.dao;

import com.electriccarsales.model.Car;
import com.electriccarsales.util.VehicleValidator;
import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CarDAO extends DBContext {
    private static final String DETAIL_SQL = """
        SELECT c.CarID, c.CarCode, c.ModelID, c.VersionID, c.ColorID, c.CarStatusID,
               c.Origin, c.Year, c.VIN, c.EngineNumber, c.LotCode, c.Price,
               m.ModelName, v.VersionName, col.ColorName, cs.StatusName
        FROM Cars c
        LEFT JOIN CarModels m ON m.ModelID = c.ModelID
        LEFT JOIN CarVersions v ON v.VersionID = c.VersionID
        LEFT JOIN Colors col ON col.ColorID = c.ColorID
        LEFT JOIN CarStatuses cs ON cs.CarStatusID = c.CarStatusID
        """;

    @SuppressWarnings("SqlResolve")
    public List<Car> getAllCars() {
        List<Car> list = new ArrayList<>();
        String sql = DETAIL_SQL + " ORDER BY c.CarID DESC";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapDetail(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tải danh sách xe vật lý từ cơ sở dữ liệu.", e);
        }
        return list;
    }

    public List<Car> getAvailableCarsWithDetails() {
        List<Car> list = new ArrayList<>();
        String sql = DETAIL_SQL + " WHERE UPPER(LTRIM(RTRIM(cs.StatusName))) IN "
                + "(N'AVAILABLE',N'IN_STOCK',N'FOR_SALE',N'READY_FOR_SALE',N'CÓ SẴN',N'CO SAN') "
                + "AND NOT EXISTS (SELECT 1 FROM OrderDetails od WHERE od.CarID=c.CarID) "
                + "ORDER BY m.ModelName, c.CarID";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapDetail(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tải xe có sẵn từ cơ sở dữ liệu.", e);
        }
        return list;
    }

    public boolean isVinExists(String vin, Integer excludeCarId) {
        if (vin == null || vin.isBlank()) return false;
        String sql = "SELECT 1 FROM Cars WHERE UPPER(LTRIM(RTRIM(VIN))) = ?"
                + (excludeCarId != null && excludeCarId > 0 ? " AND CarID <> ?" : "");
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vin.trim().toUpperCase(Locale.ROOT));
            if (excludeCarId != null && excludeCarId > 0) {
                ps.setInt(2, excludeCarId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể kiểm tra trùng số khung VIN.", e);
        }
    }

    public boolean isEngineNumberExists(String engineNumber, Integer excludeCarId) {
        if (engineNumber == null || engineNumber.isBlank()) return false;
        String sql = "SELECT 1 FROM Cars WHERE UPPER(LTRIM(RTRIM(EngineNumber))) = ?"
                + (excludeCarId != null && excludeCarId > 0 ? " AND CarID <> ?" : "");
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, engineNumber.trim().toUpperCase(Locale.ROOT));
            if (excludeCarId != null && excludeCarId > 0) {
                ps.setInt(2, excludeCarId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể kiểm tra trùng số máy.", e);
        }
    }

    public boolean isCarCodeExists(String carCode, Integer excludeCarId) {
        if (carCode == null || carCode.isBlank()) return false;
        String sql = "SELECT 1 FROM Cars WHERE UPPER(LTRIM(RTRIM(CarCode))) = ?"
                + (excludeCarId != null && excludeCarId > 0 ? " AND CarID <> ?" : "");
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, carCode.trim().toUpperCase(Locale.ROOT));
            if (excludeCarId != null && excludeCarId > 0) {
                ps.setInt(2, excludeCarId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể kiểm tra trùng mã xe.", e);
        }
    }

    public boolean isVersionBelongsToModel(int versionId, int modelId) {
        String sql = "SELECT 1 FROM CarVersions WHERE VersionID = ? AND ModelID = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, versionId);
            ps.setInt(2, modelId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể kiểm tra phiên bản xe theo mẫu xe.", e);
        }
    }

    @SuppressWarnings("SqlResolve")
    public boolean addCar(Car c) {
        VehicleValidator.validateVin(c.getVin());
        VehicleValidator.validateEngineNumber(c.getEngineNumber());
        VehicleValidator.validatePrice(c.getPrice());
        VehicleValidator.validateYear(c.getYear());

        String normVin = VehicleValidator.normalizeVin(c.getVin());
        String normEngine = VehicleValidator.normalizeEngineNumber(c.getEngineNumber());
        c.setVin(normVin);
        c.setEngineNumber(normEngine);

        if (isVinExists(normVin, null)) {
            throw new IllegalArgumentException("VIN này đã tồn tại trong hệ thống.");
        }
        if (isEngineNumberExists(normEngine, null)) {
            throw new IllegalArgumentException("Số máy đã tồn tại trong hệ thống.");
        }
        if (c.getCarCode() != null && !c.getCarCode().isBlank() && isCarCodeExists(c.getCarCode(), null)) {
            throw new IllegalArgumentException("Mã xe " + c.getCarCode() + " đã tồn tại trong hệ thống.");
        }
        if (!isVersionBelongsToModel(c.getVersionID(), c.getModelID())) {
            throw new IllegalArgumentException("Phiên bản xe không thuộc mẫu xe đã chọn.");
        }

        String sql = "INSERT INTO Cars (CarCode, ModelID, VersionID, ColorID, CarStatusID, Origin, Year, VIN, EngineNumber, LotCode, Price) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getCarCode());
            ps.setInt(2, c.getModelID());
            ps.setInt(3, c.getVersionID());
            ps.setInt(4, c.getColorID());
            ps.setInt(5, c.getCarStatusID());
            ps.setNString(6, c.getOrigin());
            ps.setInt(7, c.getYear());
            ps.setString(8, normVin);
            ps.setString(9, normEngine);
            ps.setString(10, c.getLotCode());
            ps.setDouble(11, c.getPrice());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Lỗi tại addCar: " + e.getMessage());
            throw new IllegalStateException("Không thể thêm xe vật lý: " + e.getMessage(), e);
        }
    }

    /**
     * Atomically imports a new physical vehicle into Cars and logs the IMPORT transaction
     * into InventoryTransactions. Rollback entire operation if any error occurs.
     */
    public Car importVehicleInTransaction(Car c, int employeeId, Integer supplierId, String note) {
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Nhân viên thực hiện nhập kho không hợp lệ.");
        }
        VehicleValidator.validateVin(c.getVin());
        VehicleValidator.validateEngineNumber(c.getEngineNumber());
        VehicleValidator.validatePrice(c.getPrice());
        VehicleValidator.validateYear(c.getYear());

        String normVin = VehicleValidator.normalizeVin(c.getVin());
        String normEngine = VehicleValidator.normalizeEngineNumber(c.getEngineNumber());
        c.setVin(normVin);
        c.setEngineNumber(normEngine);

        if (isVinExists(normVin, null)) {
            throw new IllegalArgumentException("VIN này đã tồn tại trong hệ thống.");
        }
        if (isEngineNumberExists(normEngine, null)) {
            throw new IllegalArgumentException("Số máy đã tồn tại trong hệ thống.");
        }
        if (c.getCarCode() != null && !c.getCarCode().isBlank() && isCarCodeExists(c.getCarCode(), null)) {
            throw new IllegalArgumentException("Mã xe " + c.getCarCode() + " đã tồn tại trong hệ thống.");
        }
        if (!isVersionBelongsToModel(c.getVersionID(), c.getModelID())) {
            throw new IllegalArgumentException("Phiên bản xe không thuộc mẫu xe đã chọn.");
        }

        String insertCarSql = """
            INSERT INTO Cars (CarCode, ModelID, VersionID, ColorID, CarStatusID, Origin, Year, VIN, EngineNumber, LotCode, Price)
            VALUES (?, ?, ?, ?, (SELECT TOP 1 CarStatusID FROM CarStatuses WHERE UPPER(StatusName) = 'AVAILABLE'), ?, ?, ?, ?, ?, ?)
            """;
        String insertTxSql = """
            INSERT INTO InventoryTransactions (CarID, EmployeeID, SupplierID, TransactionType, TransactionDate, Note)
            VALUES (?, ?, ?, 'IMPORT', SYSDATETIME(), ?)
            """;

        try (Connection connection = DBContext.openConnection()) {
            connection.setAutoCommit(false);
            int newCarId = 0;
            try {
                // Ensure unique CarCode
                String code = (c.getCarCode() != null && !c.getCarCode().isBlank())
                        ? c.getCarCode().trim().toUpperCase(Locale.ROOT)
                        : ("VF-" + (System.currentTimeMillis() % 1000000));

                try (PreparedStatement ps = connection.prepareStatement(insertCarSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, code);
                    ps.setInt(2, c.getModelID());
                    ps.setInt(3, c.getVersionID());
                    ps.setInt(4, c.getColorID());
                    ps.setNString(5, (c.getOrigin() == null || c.getOrigin().isBlank()) ? "Việt Nam" : c.getOrigin().trim());
                    ps.setInt(6, c.getYear());
                    ps.setString(7, normVin);
                    ps.setString(8, normEngine);
                    ps.setString(9, (c.getLotCode() == null || c.getLotCode().isBlank()) ? "LOT-" + c.getYear() : c.getLotCode().trim());
                    ps.setDouble(10, c.getPrice());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            newCarId = keys.getInt(1);
                        } else {
                            throw new SQLException("Không thể lấy CarID vừa tạo.");
                        }
                    }
                }

                // Insert InventoryTransaction
                try (PreparedStatement psTx = connection.prepareStatement(insertTxSql)) {
                    psTx.setInt(1, newCarId);
                    psTx.setInt(2, employeeId);
                    if (supplierId != null && supplierId > 0) {
                        psTx.setInt(3, supplierId);
                    } else {
                        psTx.setNull(3, Types.INTEGER);
                    }
                    String txNote = (note != null && !note.isBlank())
                            ? note.trim()
                            : ("Nhập kho xe mới VIN " + normVin + " (Loại: IMPORT)");
                    psTx.setNString(4, txNote);
                    psTx.executeUpdate();
                }

                connection.commit();
                c.setCarID(newCarId);
                c.setCarCode(code);
                c.setStatusName("AVAILABLE");
                return c;
            } catch (SQLException | RuntimeException e) {
                try { connection.rollback(); } catch (SQLException rb) { e.addSuppressed(rb); }
                throw new IllegalStateException("Không thể nhập kho xe: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi kết nối cơ sở dữ liệu khi nhập kho xe.", e);
        }
    }

    /**
     * Atomically exports an AVAILABLE vehicle directly from inventory (e.g. transfer, return to supplier).
     * Updates car status and creates an EXPORT transaction in InventoryTransactions.
     * Prevents export if vehicle is already DELIVERED, EXPORTED, SOLD, or bound to an active order.
     */
    public boolean exportVehicleInTransaction(int carId, int employeeId, String reason) {
        if (carId <= 0) {
            throw new IllegalArgumentException("Mã xe xuất kho không hợp lệ.");
        }
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Nhân viên thực hiện xuất kho không hợp lệ.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do xuất kho xe.");
        }

        String checkSql = """
            SELECT c.CarID, c.VIN, cs.StatusName,
                   (SELECT COUNT(1) FROM OrderDetails od
                    JOIN Orders o ON o.OrderID = od.OrderID
                    JOIN OrderStatuses os ON os.OrderStatusID = o.OrderStatusID
                    WHERE od.CarID = c.CarID AND os.StatusName NOT IN ('CANCELLED')) AS ActiveOrders
            FROM Cars c
            LEFT JOIN CarStatuses cs ON cs.CarStatusID = c.CarStatusID
            WHERE c.CarID = ?
            """;

        String updateCarSql = """
            UPDATE Cars
            SET CarStatusID = (SELECT TOP 1 CarStatusID FROM CarStatuses WHERE UPPER(StatusName) IN ('DELIVERED', 'EXPORTED'))
            WHERE CarID = ?
            """;

        String insertTxSql = """
            INSERT INTO InventoryTransactions (CarID, EmployeeID, SupplierID, TransactionType, TransactionDate, Note)
            VALUES (?, ?, NULL, 'EXPORT', SYSDATETIME(), ?)
            """;

        try (Connection connection = DBContext.openConnection()) {
            connection.setAutoCommit(false);
            try {
                String vin = "";
                String statusName = "";
                int activeOrders = 0;

                try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
                    ps.setInt(1, carId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Không tìm thấy xe #" + carId + " trong kho.");
                        }
                        vin = rs.getString("VIN");
                        statusName = rs.getString("StatusName");
                        activeOrders = rs.getInt("ActiveOrders");
                    }
                }

                String upperStatus = statusName == null ? "" : statusName.trim().toUpperCase(Locale.ROOT);
                if (List.of("DELIVERED", "EXPORTED").contains(upperStatus)) {
                    throw new IllegalStateException("Xe VIN " + vin + " đã được xuất kho hoặc bàn giao trước đó.");
                }
                if (List.of("SOLD", "DEPOSITED", "RESERVED").contains(upperStatus) || activeOrders > 0) {
                    throw new IllegalStateException("Xe VIN " + vin + " đang thuộc đơn hàng (trạng thái: " + statusName
                            + "). Vui lòng thực hiện xuất kho qua luồng bàn giao xe (Handover) hoặc xử lý đơn hàng liên quan.");
                }
                if (!List.of("AVAILABLE", "IN_STOCK", "FOR_SALE", "READY_FOR_SALE", "CÓ SẴN").contains(upperStatus)) {
                    throw new IllegalStateException("Chỉ xe ở trạng thái AVAILABLE mới đủ điều kiện xuất kho trực tiếp. Trạng thái hiện tại: " + statusName);
                }

                // Update car status
                try (PreparedStatement psUp = connection.prepareStatement(updateCarSql)) {
                    psUp.setInt(1, carId);
                    if (psUp.executeUpdate() != 1) {
                        throw new SQLException("Không thể cập nhật trạng thái xuất kho cho xe.");
                    }
                }

                // Insert EXPORT transaction
                try (PreparedStatement psTx = connection.prepareStatement(insertTxSql)) {
                    psTx.setInt(1, carId);
                    psTx.setInt(2, employeeId);
                    psTx.setNString(3, "Xuất kho xe VIN " + vin + " (Lý do: " + reason.trim() + ")");
                    psTx.executeUpdate();
                }

                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                try { connection.rollback(); } catch (SQLException rb) { e.addSuppressed(rb); }
                throw new IllegalStateException("Không thể xuất kho xe: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi kết nối cơ sở dữ liệu khi xuất kho xe.", e);
        }
    }

    public boolean updateCarStatus(int carId, String statusName) {
        String sql = """
                UPDATE Cars SET CarStatusID = (SELECT TOP 1 CarStatusID FROM CarStatuses WHERE StatusName = ?)
                WHERE CarID = ? AND NOT EXISTS (SELECT 1 FROM OrderDetails WHERE CarID=Cars.CarID)
                  AND CarStatusID NOT IN (SELECT CarStatusID FROM CarStatuses
                                          WHERE UPPER(StatusName) IN ('RESERVED','DEPOSITED','SOLD','DELIVERED'))
                """;
        if (statusName == null || List.of("RESERVED", "DEPOSITED", "SOLD", "DELIVERED")
                .contains(statusName.strip().toUpperCase(Locale.ROOT))) return false;
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setNString(1, statusName);
            ps.setInt(2, carId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể cập nhật trạng thái xe vật lý.", e);
        }
    }

    private Car mapDetail(ResultSet rs) throws SQLException {
        Car c = new Car(
                rs.getInt("CarID"),
                rs.getString("CarCode"),
                rs.getInt("ModelID"),
                rs.getInt("VersionID"),
                rs.getInt("ColorID"),
                rs.getInt("CarStatusID"),
                rs.getString("Origin"),
                rs.getInt("Year"),
                rs.getString("VIN"),
                rs.getString("EngineNumber"),
                rs.getString("LotCode"),
                rs.getDouble("Price")
        );
        c.setModelName(rs.getString("ModelName"));
        c.setVersionName(rs.getString("VersionName"));
        c.setColorName(rs.getString("ColorName"));
        c.setStatusName(rs.getString("StatusName"));
        return c;
    }
}
