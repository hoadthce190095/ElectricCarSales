# Cấu trúc phần Phạm Trần Phúc Tấn

Package gốc là `com.electriccarsales`, thống nhất với nhánh `main` của nhóm. Chỉ dùng bốn package nghiệp vụ, cùng `Main.java` và lớp `util.DBContext` dùng chung đã có trong sườn:

```text
src/main/java/
  com/electriccarsales/
    controller/   Servlet, JsonServlet, SecurityFilter
    dao/          Các lớp JDBC *DAO và AICatalogue
    model/        Dữ liệu nghiệp vụ, request/response, SessionUser, ApiException
    service/      Nghiệp vụ, mật khẩu/phiên, xuất báo cáo, BootstrapAdmin
    Main.java
  util/
    DBContext.java
src/main/webapp/
  index.jsp       Trang chủ VinFast xanh dương theo main
  login.jsp       Đăng nhập từ trang chủ
  admin.jsp       Quản trị, báo cáo và tra cứu danh mục
  customer/ai-assistant.jsp  Trợ lý tư vấn xe công khai cho khách hàng
  assets/         CSS, JavaScript và dữ liệu danh mục bổ sung
  WEB-INF/web.xml
```

`model` gộp các đối tượng dữ liệu bảng và dữ liệu nhận/trả API. `SecurityFilter` thuộc `controller`; `PasswordHasher`, `PasswordSession`, `BootstrapAdmin`, `ReportCsv`, `ReportXlsx` thuộc `service`. Không tạo thêm các package `dto`, `entities`, `security`, `config` hay `util` bên trong `com.electriccarsales`.

`util.DBContext` giữ constructor không tham số, trường `protected conn` và `getConnection()` để tương thích với DAO của nhóm. `openConnection()` tạo kết nối mới cho mỗi thao tác; cấu hình lấy từ Java system properties, biến môi trường `DB_*`, rồi file riêng `/db.properties`. `Main` kiểm tra kết nối; website chạy trên Tomcat 10.1, Java 24 và Jakarta Servlet 6.

## Luồng xử lý

```text
admin.jsp + assets/admin-dashboard.js và các script chức năng
    → controller.SecurityFilter: phiên, trạng thái, quyền, CSRF
    → controller: đọc request và trả JSON / file
    → service: kiểm tra dữ liệu, nghiệp vụ, transaction
    → dao: PreparedStatement và ánh xạ model
    → SQL Server
```

Controller và service nhận dependency qua constructor; constructor không tham số tạo dependency mặc định cho Servlet container. Thay đổi nghiệp vụ và System Log được ghi cùng transaction. Các class dữ liệu dùng thuộc tính private, constructor, getter/setter. Tên JSON và URL API giữ nguyên sau khi chuyển package.

## Class theo chức năng

| Chức năng                        | Controller               | Service                                     | DAO / đối tượng                                                                               |
| -------------------------------- | ------------------------ | ------------------------------------------- | --------------------------------------------------------------------------------------------- |
| Tài khoản, nhân viên, khuyến mãi | AdministrationController | AdministrationService                       | AdministrationDAO; AdminUser, Employee, AdminPromotion                                                  |
| Vai trò, phân quyền              | AdministrationController | AuthorizationService                        | AdminRoleDAO; AdminRole, PermissionDTO, CreateRoleRequest, RoleRequest                                  |
| Nhật ký                          | AdministrationController | AdministrationService                       | SystemLogDAO, AdministrationDAO; SystemLog, PageResponse                                      |
| Cấu hình                         | AdministrationController | SystemConfigurationService                  | SystemConfigurationDAO; SystemConfiguration, ConfigurationRequest                             |
| Đăng nhập, đổi mật khẩu          | AuthenticationController | AuthorizationService, PasswordChangeService | AuthenticationDAO; Account, LoginRequest, SessionUser                                         |
| Admin lần đầu cục bộ             | AdminSetupController     | AdminSetupService, BootstrapAdmin           | AdminSetupRequest, PasswordHasher, SystemLogDAO                                               |
| Báo cáo                          | ReportController         | ReportService, ReportCsv, ReportXlsx        | ReportDAO; ReportFilter, ReportData, ReportSummary, SaleReport                                |
| Tư vấn xe                        | AIController             | AIService                                   | AIDAO, AICatalogue; VehicleModel, VehicleSpecifications, AIRequest, AIResponse, AIConstraints |

## JSP và đường dẫn

Trang gốc dùng welcome `index.jsp`, hiển thị trang chủ VinFast xanh dương; nút đăng nhập công khai mở `login.jsp`. `admin.jsp` giữ các màn hình quản trị, báo cáo và tra cứu danh mục; liên kết `admin.jsp?login=1` mở form đăng nhập quản trị khi chưa có phiên. Trợ lý đặt tại `customer/ai-assistant.jsp`, dùng `assets/js/customer-ai.js` gọi `/api/auth` lấy CSRF rồi gửi câu hỏi tới `/api/ai`. Khách chưa đăng nhập cũng tư vấn được. Trang Admin không tải module hội thoại.

`assets/css/theme.css` chứa màu và kiểu chữ dùng chung, được tải trước `assets/css/home.css` ở trang chủ và `assets/admin-dashboard.css` ở không gian làm việc. `assets/js/home.js` xử lý tìm kiếm, bộ lọc, sắp xếp và so sánh tối đa 3 xe từ `assets/vinfast-catalogue.json`.

JSP dùng JSTL `c:url` cho CSS, JavaScript và link về trang chính. Meta `app-context-path` cung cấp context cho JavaScript; các script quản trị dùng hàm `appUrl()` khi gọi API, đọc JSON danh mục và tải CSV/XLSX. Vì vậy deploy ở `/ElectricCarSales`, `/showroom` hoặc context gốc vẫn tạo URL đúng.

Khi ghép với trang JSP của nhóm, thêm link bằng `<c:url value="/admin.jsp" />`; tránh đường dẫn `../` phụ thuộc độ sâu của trang. Sau khi đổi package phải chạy `mvn clean verify` để loại class cũ khỏi WAR.

## API

| URL                                        | Chức năng                                                          |
| ------------------------------------------ | ------------------------------------------------------------------ |
| `/api/auth`                                | Trạng thái phiên, đăng nhập, đăng xuất, đổi mật khẩu và CSRF token |
| `/api/setup`                               | Tạo Admin đầu tiên khi được bật và truy cập từ localhost           |
| `/api/admin/lookups`                       | Vai trò, trạng thái, mẫu xe                                        |
| `/api/admin/users`, `/api/admin/employees` | Quản lý tài khoản nhân viên và nhân viên                           |
| `/api/admin/roles`                         | Xem/tạo/sao chép vai trò, cấp/thu hồi quyền                        |
| `/api/admin/promotions`                    | Tạo/sửa/ngừng khuyến mãi                                           |
| `/api/admin/logs`                          | Tra cứu và xuất nhật ký                                            |
| `/api/admin/configurations`                | Xem và cập nhật cấu hình                                           |
| `/api/reports`                             | Báo cáo JSON, CSV hoặc XLSX                                        |
| `/api/ai`                                  | Danh mục, tư vấn thông số, so sánh, phân tích nhu cầu, gợi ý xe    |

AI hiện dùng `CATALOG_RULES` từ danh mục xe và không huấn luyện mô hình, nên các lớp AI nằm trong bốn package trên. Nếu bổ sung huấn luyện sau này, gom phần huấn luyện vào một package `ai`. Các chức năng xe, hợp đồng, thanh toán và kho của nhóm tiếp tục dùng các lớp riêng trên `main`.

Đọc [giải thích luồng code](GIAI_THICH_CODE.md) và [hướng dẫn IntelliJ](HUONG_DAN_INTELLIJ.md) để chạy thử.
