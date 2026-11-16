package com.electriccarsales.service;

import com.electriccarsales.dao.FinanceConnection;
import com.electriccarsales.dao.SchemaTables;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Creates the Order and OrderDetail together, using the vehicle price held in SQL Server. */
public class SalesOrderService {
    public record Choice(int id, String label) {
        public int getId() { return id; }
        public String getLabel() { return label; }
    }
    public record CustomerChoice(int id, String label, String name, String phone, String email, String address) {
        public int getId() { return id; }
        public String getLabel() { return label; }
        public String getName() { return name != null ? name : ""; }
        public String getPhone() { return phone != null ? phone : ""; }
        public String getEmail() { return email != null ? email : ""; }
        public String getAddress() { return address != null ? address : ""; }
    }
    public record CarChoice(int id, String label, BigDecimal originalPrice, BigDecimal salePrice,
                            String promotionName, BigDecimal discountAmount) {
        public int getId() { return id; }
        public String getLabel() { return label; }
        public BigDecimal getOriginalPrice() { return originalPrice; }
        public BigDecimal getSalePrice() { return salePrice; }
        public String getPromotionName() { return promotionName; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public boolean isDiscounted() { return discountAmount != null && discountAmount.signum() > 0; }
    }
    public record Options(List<CustomerChoice> customers, List<CarChoice> cars, List<Choice> partners) {
        public List<CustomerChoice> getCustomers() { return customers; }
        public List<CarChoice> getCars() { return cars; }
        public List<Choice> getPartners() { return partners; }
    }

    public static Map<String, Object> findConsultingStaff(int customerId, Integer sessionUserId) {
        if (customerId > 0) {
            String sql = "SELECT TOP 1 e.EmployeeID, e.EmployeeName, e.Phone, e.Email, u.Username "
                    + "FROM ConsultationRequests cr "
                    + "JOIN Employees e ON e.EmployeeID = cr.EmployeeID "
                    + "JOIN Users u ON u.UserID = e.UserID "
                    + "WHERE cr.CustomerID = ? AND cr.EmployeeID IS NOT NULL "
                    + "ORDER BY cr.RequestDate DESC";
            try (Connection c = FinanceConnection.open();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> map = new LinkedHashMap<>();
                        int empId = rs.getInt("EmployeeID");
                        map.put("employeeId", empId);
                        map.put("employeeCode", "NV" + String.format("%03d", empId));
                        map.put("employeeName", rs.getString("EmployeeName"));
                        map.put("username", rs.getString("Username"));
                        map.put("email", rs.getString("Email") != null ? rs.getString("Email") : "");
                        map.put("phone", rs.getString("Phone") != null ? rs.getString("Phone") : "");
                        map.put("source", "CONSULTATION_ASSIGNED");
                        return map;
                    }
                }
            } catch (SQLException ignored) {}
        }

        if (sessionUserId != null && sessionUserId > 0) {
            String sql = "SELECT e.EmployeeID, e.EmployeeName, e.Phone, e.Email, u.Username "
                    + "FROM Employees e "
                    + "JOIN Users u ON u.UserID = e.UserID "
                    + "JOIN Roles r ON r.RoleID = u.RoleID "
                    + "WHERE u.UserID = ? AND UPPER(r.RoleName) = 'SALES'";
            try (Connection c = FinanceConnection.open();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, sessionUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> map = new LinkedHashMap<>();
                        int empId = rs.getInt("EmployeeID");
                        map.put("employeeId", empId);
                        map.put("employeeCode", "NV" + String.format("%03d", empId));
                        map.put("employeeName", rs.getString("EmployeeName"));
                        map.put("username", rs.getString("Username"));
                        map.put("email", rs.getString("Email") != null ? rs.getString("Email") : "");
                        map.put("phone", rs.getString("Phone") != null ? rs.getString("Phone") : "");
                        map.put("source", "SESSION_NEW_ASSIGNMENT");
                        return map;
                    }
                }
            } catch (SQLException ignored) {}
        }

        return null;
    }

    public Options getOptions() throws SQLException {
        try (Connection c = FinanceConnection.open()) {
            List<CustomerChoice> customers = new ArrayList<>();
            String custSql = "SELECT CustomerID,CustomerName,Phone,Email,Address FROM Customers ORDER BY CustomerName";
            try (PreparedStatement ps = c.prepareStatement(custSql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("CustomerID");
                    String name = rs.getString("CustomerName");
                    String phone = rs.getString("Phone");
                    String email = rs.getString("Email");
                    String address = rs.getString("Address");
                    customers.add(new CustomerChoice(id, name != null ? name : ("Khách hàng #" + id),
                            name, phone, email, address));
                }
            }
            List<CarChoice> cars = carChoices(c);
            List<Choice> partners = choices(c, "SELECT CompanyID,CompanyName FROM FinancialCompanies ORDER BY CompanyID");
            if (partners.isEmpty()) {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("""
                        INSERT INTO FinancialCompanies (CompanyName, Phone, Email, Address, Description)
                        VALUES
                        (N'Shinhan Finance', '19001577', 'support@shinhanfinance.com.vn', N'Tầng 17, Centec Tower, 72-74 Nguyễn Thị Minh Khai, Q.3, TP.HCM', N'Công ty Tài chính Shinhan Việt Nam - Tài trợ mua xe điện VinFast ưu đãi.'),
                        (N'Techcombank', '1800588822', 'call_center@techcombank.com.vn', N'Số 6 Quang Trung, Hoàn Kiếm, Hà Nội', N'Ngân hàng TMCP Kỹ Thương Việt Nam - Gói vay mua ô tô điện giải ngân 70%.'),
                        (N'MB Bank', '1900545426', 'mb247@mbbank.com.vn', N'Số 18 Lê Văn Lương, Cầu Giấy, Hà Nội', N'Ngân hàng TMCP Quân Đội - Đối tác thanh toán VietQR và giải ngân trả góp xe điện.'),
                        (N'VPBank', '1900545415', 'chamsockhachhang@vpbank.com.vn', N'Số 89 Láng Hạ, Đống Đa, Hà Nội', N'Ngân hàng TMCP Việt Nam Thịnh Vượng - Hỗ trợ vay mua xe điện trả góp.')
                    """);
                } catch (Exception ignored) {}
                partners = choices(c, "SELECT CompanyID,CompanyName FROM FinancialCompanies ORDER BY CompanyID");
            }
            return new Options(customers, cars, partners);
        }
    }

    private static List<CarChoice> carChoices(Connection c) throws SQLException {
        String baseSql = "SELECT c.CarID,c.CarCode,m.ModelName,c.Price FROM Cars c "
                + "JOIN CarStatuses s ON s.CarStatusID=c.CarStatusID JOIN CarModels m ON m.ModelID=c.ModelID "
                + "WHERE UPPER(LTRIM(RTRIM(s.StatusName))) IN (N'AVAILABLE',N'IN_STOCK',N'FOR_SALE',N'READY_FOR_SALE',N'CÓ SẴN',N'CO SAN') "
                + "AND NOT EXISTS (SELECT 1 FROM OrderDetails d WHERE d.CarID=c.CarID) ORDER BY c.CarID";
        List<CarChoice> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(baseSql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BigDecimal price = rs.getBigDecimal("Price");
                PromoPrice promo = activePromotion(c, rs.getInt("CarID"));
                BigDecimal discount = promo == null ? BigDecimal.ZERO : promo.discount().min(price).setScale(0, RoundingMode.HALF_UP);
                BigDecimal salePrice = price.subtract(discount);
                String label = rs.getString("CarCode") + " - " + rs.getString("ModelName") + " - "
                        + (discount.signum() > 0 ? salePrice.toPlainString() + " VND (" + promo.name()
                        + ", giá gốc " + price.toPlainString() + " VND)"
                        : price.toPlainString() + " VND");
                result.add(new CarChoice(rs.getInt("CarID"), label, price, salePrice,
                        discount.signum() > 0 ? promo.name() : null, discount));
            }
        }
        return result;
    }

    private record PromoPrice(String name, BigDecimal discount) {}

    private static PromoPrice activePromotion(Connection c, int carId) throws SQLException {
        String sql = "SELECT TOP (1) p.PromotionName,p.DiscountType,p.DiscountValue,c.Price "
                + "FROM Cars c JOIN Promotions p ON p.Status='ACTIVE' "
                + "JOIN PromotionCars pc ON pc.PromotionID=p.PromotionID AND pc.ModelID=c.ModelID "
                + "WHERE c.CarID=? AND p.StartDate<=SYSDATETIME() AND p.EndDate>=SYSDATETIME() "
                + "ORDER BY p.EndDate,p.PromotionID";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, carId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                BigDecimal price = rs.getBigDecimal("Price");
                BigDecimal value = rs.getBigDecimal("DiscountValue");
                BigDecimal discount = "PERCENTAGE".equalsIgnoreCase(rs.getString("DiscountType"))
                        ? price.multiply(value).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP)
                        : value;
                return new PromoPrice(rs.getString("PromotionName"), discount.min(price));
            }
        } catch (SQLException e) {
            if (!SchemaTables.exists(c, "Promotions") || !SchemaTables.exists(c, "PromotionCars")) return null;
            throw e;
        }
    }

    /** Data used by the Sales dashboard's order and deposit views. */
    public Map<String, Object> dashboardData() throws SQLException {
        try (Connection c = FinanceConnection.open()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orders", query(c, """
                SELECT o.OrderID AS id,o.OrderDate AS orderDate,cu.CustomerID AS customerID,
                       cu.CustomerName AS customerName,cu.Phone AS customerPhone,
                       cm.ModelID AS modelID,cm.ModelName AS modelName,cv.VersionName AS versionName,
                       car.VIN AS vin,pm.MethodName AS paymentMethodName,fc.CompanyName AS companyName,
                       o.TotalAmount AS originalAmount,o.DepositAmount AS depositAmount,
                       COALESCE(paid.PaidAmount,0) AS paidAmount,o.RemainingAmount AS remainingAmount,
                       os.StatusName AS status,ct.Status AS contractStatus,
                       inv.InvoiceNumber AS invoiceNumber,d.DeliveryStatus AS deliveryStatus
                FROM Orders o
                JOIN Customers cu ON cu.CustomerID=o.CustomerID
                JOIN OrderStatuses os ON os.OrderStatusID=o.OrderStatusID
                JOIN PaymentMethods pm ON pm.PaymentMethodID=o.PaymentMethodID
                LEFT JOIN FinancialCompanies fc ON fc.CompanyID=o.CompanyID
                LEFT JOIN OrderDetails od ON od.OrderID=o.OrderID
                LEFT JOIN Cars car ON car.CarID=od.CarID
                LEFT JOIN CarModels cm ON cm.ModelID=car.ModelID
                LEFT JOIN CarVersions cv ON cv.VersionID=car.VersionID
                LEFT JOIN Contracts ct ON ct.OrderID=o.OrderID
                LEFT JOIN Invoices inv ON inv.OrderID=o.OrderID
                LEFT JOIN Deliveries d ON d.OrderID=o.OrderID
                LEFT JOIN (
                    SELECT p.OrderID,SUM(p.PaymentAmount) AS PaidAmount
                    FROM Payments p
                    JOIN PaymentStatuses ps ON ps.PaymentStatusID=p.PaymentStatusID
                    WHERE ps.StatusID='COMPLETED'
                    GROUP BY p.OrderID
                ) paid ON paid.OrderID=o.OrderID
                ORDER BY o.OrderDate DESC,o.OrderID DESC
                """));
            data.put("customers", query(c, "SELECT CustomerID AS customerID,CustomerName AS customerName,Phone AS phone,COALESCE(Email,'') AS email,COALESCE(Address,'') AS address FROM Customers ORDER BY CustomerName"));
            List<Map<String, Object>> cars = query(c, """
                SELECT car.CarID AS carID,car.ModelID AS modelID,car.Price AS price,car.VIN AS vin,
                       cm.ModelName AS modelName,cv.VersionName AS versionName,co.ColorName AS colorName
                FROM Cars car
                JOIN CarStatuses cs ON cs.CarStatusID=car.CarStatusID
                JOIN CarModels cm ON cm.ModelID=car.ModelID
                JOIN CarVersions cv ON cv.VersionID=car.VersionID
                JOIN Colors co ON co.ColorID=car.ColorID
                WHERE UPPER(LTRIM(RTRIM(cs.StatusName))) IN (N'AVAILABLE',N'IN_STOCK',N'FOR_SALE',N'READY_FOR_SALE',N'CÓ SẴN',N'CO SAN')
                  AND NOT EXISTS (SELECT 1 FROM OrderDetails od WHERE od.CarID=car.CarID)
                ORDER BY cm.ModelName,cv.VersionName,car.CarID
                """);
            // The Sales dashboard creates orders from this JSON list (not the legacy JSP form).
            // Preserve the original price while making price the effective price used by its preview.
            for (Map<String, Object> car : cars) {
                int carId = ((Number) car.get("carID")).intValue();
                Object rawPrice = car.get("price");
                BigDecimal originalPrice = rawPrice instanceof BigDecimal ? (BigDecimal) rawPrice
                        : (rawPrice instanceof Number ? BigDecimal.valueOf(((Number) rawPrice).doubleValue()) : BigDecimal.ZERO);
                PromoPrice promo = activePromotion(c, carId);
                BigDecimal discount = promo == null || promo.discount() == null ? BigDecimal.ZERO
                        : promo.discount().min(originalPrice).setScale(0, RoundingMode.HALF_UP);
                car.put("originalPrice", originalPrice);
                car.put("discountAmount", discount);
                car.put("promotionName", discount.signum() > 0 ? promo.name() : null);
                car.put("price", originalPrice.subtract(discount));
            }
            data.put("cars", cars);
            data.put("inventorySummary", query(c, """
                SELECT (SELECT COUNT(*) FROM Cars) AS physicalCars,
                       (SELECT COUNT(*) FROM Cars car JOIN CarStatuses cs ON cs.CarStatusID=car.CarStatusID
                        WHERE UPPER(LTRIM(RTRIM(cs.StatusName))) IN (N'AVAILABLE',N'IN_STOCK',N'FOR_SALE',N'READY_FOR_SALE',N'CÓ SẴN',N'CO SAN')) AS availableCars,
                       (SELECT COUNT(*) FROM Cars car JOIN CarStatuses cs ON cs.CarStatusID=car.CarStatusID
                        WHERE UPPER(LTRIM(RTRIM(cs.StatusName))) IN (N'AVAILABLE',N'IN_STOCK',N'FOR_SALE',N'READY_FOR_SALE',N'CÓ SẴN',N'CO SAN')
                          AND NOT EXISTS (SELECT 1 FROM OrderDetails od WHERE od.CarID=car.CarID)) AS availableUnassignedCars
                """));
            List<Map<String, Object>> pms = query(c, "SELECT PaymentMethodID AS paymentMethodID,MethodName AS methodName,Description AS description FROM PaymentMethods WHERE UPPER(MethodName) <> 'CASH' ORDER BY PaymentMethodID");
            if (pms == null || pms.isEmpty()) {
                pms = List.of(
                    Map.of("paymentMethodID", 2, "methodName", "BANK_TRANSFER", "description", "Thanh toán bằng ngân hàng (Chuyển khoản VietQR SePay - MB Bank)"),
                    Map.of("paymentMethodID", 3, "methodName", "INSTALLMENT", "description", "Thanh toán qua nhà tài chính (Trả góp đối tác giải ngân 70%)")
                );
            } else {
                for (Map<String, Object> pm : pms) {
                    Object nameObj = pm.get("methodName") != null ? pm.get("methodName") : pm.get("METHODNAME");
                    String mName = String.valueOf(nameObj).toUpperCase(Locale.ROOT);
                    if ("BANK_TRANSFER".equals(mName)) {
                        pm.put("description", "Thanh toán bằng ngân hàng (Chuyển khoản VietQR SePay - MB Bank)");
                    } else if ("INSTALLMENT".equals(mName)) {
                        pm.put("description", "Thanh toán qua nhà tài chính (Trả góp đối tác giải ngân 70%)");
                    }
                }
            }
            data.put("paymentMethods", pms);

            List<Map<String, Object>> fcs = query(c, "SELECT CompanyID AS companyID,CompanyName AS companyName,Phone AS phone,Email AS email,Address AS address,Description AS description FROM FinancialCompanies ORDER BY CompanyID");
            if (fcs == null || fcs.isEmpty()) {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("""
                        INSERT INTO FinancialCompanies (CompanyName, Phone, Email, Address, Description)
                        VALUES
                        (N'Shinhan Finance', '19001577', 'support@shinhanfinance.com.vn', N'Tầng 17, Centec Tower, 72-74 Nguyễn Thị Minh Khai, Q.3, TP.HCM', N'Công ty Tài chính Shinhan Việt Nam - Tài trợ mua xe điện VinFast ưu đãi.'),
                        (N'Techcombank', '1800588822', 'call_center@techcombank.com.vn', N'Số 6 Quang Trung, Hoàn Kiếm, Hà Nội', N'Ngân hàng TMCP Kỹ Thương Việt Nam - Gói vay mua ô tô điện giải ngân 70%.'),
                        (N'MB Bank', '1900545426', 'mb247@mbbank.com.vn', N'Số 18 Lê Văn Lương, Cầu Giấy, Hà Nội', N'Ngân hàng TMCP Quân Đội - Đối tác thanh toán VietQR và giải ngân trả góp xe điện.'),
                        (N'VPBank', '1900545415', 'chamsockhachhang@vpbank.com.vn', N'Số 89 Láng Hạ, Đống Đa, Hà Nội', N'Ngân hàng TMCP Việt Nam Thịnh Vượng - Hỗ trợ vay mua xe điện trả góp.')
                    """);
                } catch (Exception ignored) {}
                fcs = query(c, "SELECT CompanyID AS companyID,CompanyName AS companyName,Phone AS phone,Email AS email,Address AS address,Description AS description FROM FinancialCompanies ORDER BY CompanyID");
            }
            data.put("financialCompanies", fcs);
            return data;
        }
    }

    public int create(int salesUserId, int customerId, int carId, String paymentMethod, Integer companyId)
            throws SQLException {
        return create(salesUserId, customerId, carId, paymentMethod, companyId, null, null, null, null);
    }

    public int create(int salesUserId, int customerId, int carId, String paymentMethod, Integer companyId,
                      String verifiedName, String verifiedPhone, String verifiedAddress, String salesNote)
            throws SQLException {
        return create(salesUserId, customerId, carId, paymentMethod, companyId, verifiedName, verifiedPhone, null, verifiedAddress, salesNote);
    }

    public int create(int salesUserId, int customerId, int carId, String paymentMethod, Integer companyId,
                      String verifiedName, String verifiedPhone, String verifiedEmail, String verifiedAddress, String salesNote)
            throws SQLException {
        if (salesUserId <= 0 || customerId <= 0 || carId <= 0 || paymentMethod == null) {
            throw new IllegalArgumentException("Thiếu nhân viên, khách hàng, xe hoặc phương thức thanh toán.");
        }
        String method = paymentMethod.trim().toUpperCase(Locale.ROOT);
        if ("CASH".equals(method)) {
            throw new IllegalArgumentException("Đợt thanh toán cọc 30% chỉ hỗ trợ thanh toán bằng ngân hàng hoặc qua nhà tài chính (không áp dụng tiền mặt).");
        }
        if (!List.of("BANK_TRANSFER", "INSTALLMENT").contains(method)) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ (chỉ hỗ trợ Thanh toán bằng ngân hàng hoặc Thanh toán qua nhà tài chính).");
        }
        if ("INSTALLMENT".equals(method)) {
            if (companyId == null || companyId <= 0) {
                throw new IllegalArgumentException("Đơn hàng thanh toán qua nhà tài chính (trả góp) bắt buộc phải chọn nhà tài chính trong danh sách.");
            }
        } else {
            // Thanh toán bằng ngân hàng (BANK_TRANSFER): tự động không gán đối tác tài chính
            companyId = null;
        }
        try (Connection c = FinanceConnection.open()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                int employee = employeeId(c, salesUserId);
                if (employee <= 0) throw new SecurityException("Chỉ Sales Staff được lập đơn hàng.");
                if (!exists(c, "SELECT 1 FROM Customers WHERE CustomerID=?", customerId)) {
                    throw new IllegalArgumentException("Khách hàng không tồn tại.");
                }

                // Kiểm tra validation thông tin người mua
                String cleanName = (verifiedName != null && !verifiedName.isBlank()) ? verifiedName.trim() : null;
                String cleanPhone = (verifiedPhone != null && !verifiedPhone.isBlank()) ? verifiedPhone.trim() : null;
                String cleanEmail = (verifiedEmail != null && !verifiedEmail.isBlank()) ? verifiedEmail.trim() : null;
                String cleanAddress = (verifiedAddress != null && !verifiedAddress.isBlank()) ? verifiedAddress.trim() : null;

                if (cleanName != null && cleanName.length() > 100) {
                    throw new IllegalArgumentException("Họ và tên không được vượt quá 100 ký tự.");
                }
                if (cleanPhone != null) {
                    if (!cleanPhone.matches("^0[0-9]{9}$")) {
                        throw new IllegalArgumentException("Số điện thoại không đúng định dạng (phải bắt đầu bằng số 0 và gồm đúng 10 chữ số, ví dụ: 0912345678).");
                    }
                    try (PreparedStatement checkPhone = c.prepareStatement(
                            "SELECT 1 FROM Customers WHERE Phone=? AND CustomerID<>?")) {
                        checkPhone.setString(1, cleanPhone);
                        checkPhone.setInt(2, customerId);
                        try (ResultSet rs = checkPhone.executeQuery()) {
                            if (rs.next()) {
                                throw new IllegalArgumentException("Số điện thoại này đã được sử dụng bởi khách hàng khác.");
                            }
                        }
                    }
                }
                if (cleanEmail != null) {
                    if (!cleanEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") || cleanEmail.length() > 100) {
                        throw new IllegalArgumentException("Email không đúng định dạng (ví dụ: example@domain.com) hoặc vượt quá 100 ký tự.");
                    }
                }
                if (cleanAddress != null && cleanAddress.length() > 255) {
                    throw new IllegalArgumentException("Địa chỉ không được vượt quá 255 ký tự.");
                }
                if (salesNote != null && salesNote.trim().length() > 200) {
                    throw new IllegalArgumentException("Ghi chú của Sales không được vượt quá 200 ký tự.");
                }

                // Cập nhật thông tin khách hàng phục vụ đơn hàng (không chạm đến Users.Phone / Users.Email xác thực)
                if (cleanName != null || cleanPhone != null || cleanEmail != null || cleanAddress != null) {
                    String updateCust = "UPDATE Customers SET "
                            + "CustomerName = COALESCE(NULLIF(?, ''), CustomerName), "
                            + "Phone = COALESCE(NULLIF(?, ''), Phone), "
                            + "Email = COALESCE(NULLIF(?, ''), Email), "
                            + "Address = COALESCE(NULLIF(?, ''), Address) "
                            + "WHERE CustomerID = ?";
                    try (PreparedStatement ps = c.prepareStatement(updateCust)) {
                        ps.setString(1, cleanName);
                        ps.setString(2, cleanPhone);
                        ps.setString(3, cleanEmail);
                        ps.setString(4, cleanAddress);
                        ps.setInt(5, customerId);
                        ps.executeUpdate();
                    }
                }

                if (companyId != null && !exists(c, "SELECT 1 FROM FinancialCompanies WHERE CompanyID=?", companyId)) {
                    throw new IllegalArgumentException("Đối tác tài chính không tồn tại.");
                }
                BigDecimal originalPrice;
                String status;
                try (PreparedStatement ps = c.prepareStatement("SELECT c.Price,s.StatusName FROM Cars c WITH (UPDLOCK,HOLDLOCK) "
                        + "JOIN CarStatuses s ON s.CarStatusID=c.CarStatusID WHERE c.CarID=?")) {
                    ps.setInt(1, carId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Xe không tồn tại.");
                        originalPrice = rs.getBigDecimal("Price");
                        status = rs.getString("StatusName");
                    }
                }
                if (!isAvailableStatus(status) || exists(c,
                        "SELECT 1 FROM OrderDetails WITH (UPDLOCK,HOLDLOCK) WHERE CarID=?", carId)) {
                    throw new IllegalStateException("Xe đã được bán/đặt hoặc không còn khả dụng.");
                }
                if (originalPrice == null || originalPrice.signum() <= 0) throw new IllegalArgumentException("Giá xe không hợp lệ.");
                try { originalPrice.setScale(0, RoundingMode.UNNECESSARY); }
                catch (ArithmeticException e) { throw new IllegalStateException("Giá xe phải là số nguyên VND."); }
                PromoPrice promotion = activePromotion(c, carId);
                BigDecimal discount = promotion == null ? BigDecimal.ZERO
                        : promotion.discount().min(originalPrice).setScale(0, RoundingMode.HALF_UP);
                BigDecimal price = originalPrice.subtract(discount);
                BigDecimal deposit = price.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
                try { deposit.setScale(0, RoundingMode.UNNECESSARY); }
                catch (ArithmeticException e) { throw new IllegalStateException("30% tiền cọc cần quy tắc làm tròn VND được duyệt."); }
                BigDecimal remaining = price.subtract(deposit);
                int statusId = ensureOrderStatus(c, "PENDING", "Đơn hàng mới tạo");
                int methodId = ensurePaymentMethod(c, method);
                int orderId;
                String orderNote = (salesNote != null && !salesNote.isBlank())
                        ? ("VERIFIED: " + salesNote.trim()) : "VERIFIED_BY_SALES";
                if (promotion != null && discount.signum() > 0) {
                    orderNote += " | KHUYẾN MÃI: " + promotion.name() + " (giá gốc "
                            + originalPrice.toPlainString() + " VND, giảm " + discount.toPlainString()
                            + " VND, giá ưu đãi " + price.toPlainString() + " VND)";
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO Orders(CustomerID,EmployeeID,OrderStatusID,"
                        + "PaymentMethodID,OrderDate,TotalAmount,DepositAmount,RemainingAmount,Note,CompanyID) "
                        + "VALUES(?,?,?,?,SYSDATETIME(),?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, employee);
                    ps.setInt(3, statusId);
                    ps.setInt(4, methodId);
                    ps.setBigDecimal(5, price);
                    ps.setBigDecimal(6, deposit);
                    ps.setBigDecimal(7, remaining);
                    ps.setNString(8, orderNote);
                    if (companyId == null) ps.setNull(9, Types.INTEGER);
                    else ps.setInt(9, companyId);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Cannot obtain OrderID");
                        orderId = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO OrderDetails(OrderID,CarID,SalesPrice) VALUES(?,?,?)")) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, carId);
                    ps.setBigDecimal(3, price);
                    ps.executeUpdate();
                }
                int reservedStatusId = ensureCarStatus(c, "RESERVED", "Xe đã được giữ chỗ cho đơn hàng");
                try (PreparedStatement ps = c.prepareStatement("UPDATE Cars SET CarStatusID=? WHERE CarID=?")) {
                    ps.setInt(1, reservedStatusId);
                    ps.setInt(2, carId);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không thể cập nhật trạng thái giữ chỗ cho xe.");
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO OrderStatusHistory(OrderID,OrderStatusID,EmployeeID,Note) VALUES(?,?,?,?)")) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, statusId);
                    ps.setInt(3, employee);
                    ps.setString(4, "Sales Staff created transaction");
                    ps.executeUpdate();
                }
                c.commit();
                return orderId;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static int employeeId(Connection c, int userId) throws SQLException {
        String sql = "SELECT e.EmployeeID FROM Employees e JOIN Users u ON u.UserID=e.UserID "
                + "JOIN Roles r ON r.RoleID=u.RoleID JOIN Statuses s ON s.StatusID=u.StatusID "
                + "WHERE u.UserID=? AND UPPER(r.RoleName)='SALES' AND UPPER(s.StatusName)='ACTIVE'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private static boolean isAvailableStatus(String status) {
        if (status == null) return false;
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return List.of("AVAILABLE", "IN_STOCK", "FOR_SALE", "READY_FOR_SALE", "CÓ SẴN", "CO SAN").contains(normalized);
    }

    private static List<Choice> choices(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Choice> choices = new ArrayList<>();
            while (rs.next()) choices.add(new Choice(rs.getInt(1), rs.getString(2)));
            return choices;
        }
    }

    private static List<Map<String, Object>> query(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            var metadata = rs.getMetaData();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int index = 1; index <= metadata.getColumnCount(); index++) {
                    String label = metadata.getColumnLabel(index);
                    Object val = rs.getObject(index);
                    row.put(label, val);
                    String lower = label.toLowerCase(Locale.ROOT);
                    if (lower.equals("paymentmethodid")) row.put("paymentMethodID", val);
                    else if (lower.equals("methodname")) row.put("methodName", val);
                    else if (lower.equals("description")) row.put("description", val);
                    else if (lower.equals("companyid")) row.put("companyID", val);
                    else if (lower.equals("companyname")) row.put("companyName", val);
                    else if (lower.equals("carid")) row.put("carID", val);
                    else if (lower.equals("modelid")) row.put("modelID", val);
                    else if (lower.equals("modelname")) row.put("modelName", val);
                    else if (lower.equals("versionname")) row.put("versionName", val);
                    else if (lower.equals("colorname")) row.put("colorName", val);
                }
                rows.add(row);
            }
            return rows;
        }
    }

    private static boolean exists(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static int lookup(Connection c, String sql, String value) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        if (sql.contains("OrderStatuses")) {
            return ensureOrderStatus(c, value, "Trạng thái đơn hàng " + value);
        }
        if (sql.contains("PaymentMethods")) {
            return ensurePaymentMethod(c, value);
        }
        throw new IllegalStateException("Thiếu dữ liệu danh mục: " + value);
    }

    private static int ensureOrderStatus(Connection c, String name, String description) throws SQLException {
        String lookupSql = "SELECT TOP (1) OrderStatusID FROM OrderStatuses WHERE UPPER(LTRIM(RTRIM(StatusName)))=UPPER(LTRIM(RTRIM(?)))";
        try (PreparedStatement ps = c.prepareStatement(lookupSql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        // Thử tìm theo bí danh tương đương trong trường hợp DB dùng tiếng Việt
        if ("PENDING".equalsIgnoreCase(name)) {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT TOP (1) OrderStatusID FROM OrderStatuses WHERE UPPER(StatusName) IN ('PENDING', 'CHỜ XỬ LÝ', 'CHO_XU_LY', 'ĐANG CHỜ', 'MOI_TAO', 'MỚI')")) {
                if (rs.next()) return rs.getInt(1);
            } catch (Exception ignored) {}
        }
        // Tự động khởi tạo bản ghi nếu chưa tồn tại trong danh mục
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO OrderStatuses(StatusName,Description) VALUES(?,?)")) {
            ps.setString(1, name.toUpperCase(Locale.ROOT));
            ps.setNString(2, description);
            ps.executeUpdate();
        } catch (Exception ignored) {}
        try (PreparedStatement ps = c.prepareStatement(lookupSql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT TOP (1) OrderStatusID FROM OrderStatuses ORDER BY OrderStatusID")) {
            if (rs.next()) return rs.getInt(1);
        }
        throw new IllegalStateException("Thiếu dữ liệu danh mục trạng thái đơn hàng: " + name);
    }

    private static int ensurePaymentMethod(Connection c, String method) throws SQLException {
        String lookupSql = "SELECT TOP (1) PaymentMethodID FROM PaymentMethods WHERE UPPER(LTRIM(RTRIM(MethodName)))=UPPER(LTRIM(RTRIM(?)))";
        try (PreparedStatement ps = c.prepareStatement(lookupSql)) {
            ps.setString(1, method);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        String aliasSql = "BANK_TRANSFER".equalsIgnoreCase(method)
                ? "SELECT TOP (1) PaymentMethodID FROM PaymentMethods WHERE UPPER(MethodName) IN ('BANK_TRANSFER', 'BANK', 'CHUYEN_KHOAN', 'CHUYỂN KHOẢN', 'NGAN_HANG')"
                : "SELECT TOP (1) PaymentMethodID FROM PaymentMethods WHERE UPPER(MethodName) IN ('INSTALLMENT', 'TRA_GOP', 'TRẢ GÓP', 'TAI_CHINH')";
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(aliasSql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO PaymentMethods(MethodName, Description) VALUES(?,?)")) {
            ps.setString(1, method.toUpperCase(Locale.ROOT));
            ps.setNString(2, "BANK_TRANSFER".equalsIgnoreCase(method) ? "Thanh toán bằng ngân hàng" : "Thanh toán qua nhà tài chính");
            ps.executeUpdate();
        } catch (Exception ignored) {}
        try (PreparedStatement ps = c.prepareStatement(lookupSql)) {
            ps.setString(1, method);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT TOP (1) PaymentMethodID FROM PaymentMethods ORDER BY PaymentMethodID")) {
            if (rs.next()) return rs.getInt(1);
        }
        throw new IllegalStateException("Thiếu dữ liệu danh mục phương thức thanh toán: " + method);
    }

    private static int ensureCarStatus(Connection c, String name, String description) throws SQLException {
        String lookup = "SELECT TOP (1) CarStatusID FROM CarStatuses WHERE UPPER(LTRIM(RTRIM(StatusName)))=?";
        try (PreparedStatement ps = c.prepareStatement(lookup)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO CarStatuses(StatusName,Description) VALUES(?,?)")) {
            ps.setNString(1, name);
            ps.setNString(2, description);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(lookup)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Không thể khởi tạo trạng thái xe " + name + ".");
                return rs.getInt(1);
            }
        }
    }
}
