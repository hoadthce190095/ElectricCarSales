# Cấu trúc phần Phạm Trần Phúc Tấn

Package của phần Tấn là `vn.edu.fpt.electriccarsales`, theo cách đặt tên `vn.edu.fpt.cafemanagement` trong mẫu `Project.zip`. Các package đã có của nhóm được giữ nguyên. Tên class dùng PascalCase; thuộc tính và phương thức dùng camelCase. Các đối tượng dữ liệu là class Java thông thường, thuộc tính private, constructor không tham số và đầy đủ, getter/setter. Mã thụt đầu dòng bốn dấu cách.

Mẫu cafe chạy Spring Boot. Bài xe điện giữ Jakarta Servlet 6, JDBC, SQL Server và Tomcat 10.1 đang dùng, nên không thêm annotation Spring hoặc JPA vào class. `App` kiểm tra kết nối database; cấu hình Tomcat trong IntelliJ khởi động website.

## Luồng xử lý

```text
tan.html + assets/tan.js
    → SecurityFilter: phiên, trạng thái, quyền, CSRF
    → controllers: đọc request và trả JSON / file
    → services: kiểm tra dữ liệu, nghiệp vụ, transaction
    → repositories: PreparedStatement và ánh xạ entities / dto
    → SQL Server
```

Controller và service nhận dependency qua constructor. Constructor không tham số tạo dependency mặc định để Servlet container có thể khởi tạo. Các lớp Repository dùng PreparedStatement; thay đổi nghiệp vụ và System Log ghi cùng transaction.

Trang riêng là `/ElectricCarSales/tan.html`; trang gốc vẫn mở `index.jsp` của nhóm. `SecurityFilter` chỉ áp dụng cho `/api/auth`, `/api/setup`, `/api/ai`, `/api/reports` và `/api/admin/*`. Khi ghép module, thống nhất session, PasswordHash, quyền vai trò và các bảng dùng chung với nhóm.

Các luồng chính dùng vòng lặp `for`, điều kiện `if` và phương thức riêng cho từng bước. Phần quản trị viết `try/catch`, `commit/rollback` trực tiếp để theo dõi thứ tự lưu tài khoản, nhân viên và nhật ký. Phần báo cáo có chú thích tiếng Việt giải thích tiền thực thu, giá trị xe bán và cách tránh đếm trùng. Xuất XLSX dùng Apache POI như mẫu; không tự ghép cấu trúc ZIP/XML của Excel.

Đọc [giải thích luồng code](GIAI_THICH_CODE.md) để theo dõi từng chức năng.

## Class theo chức năng

| Chức năng                        | Controller               | Service                    | Repository / đối tượng                                                                               |
| -------------------------------- | ------------------------ | -------------------------- | ---------------------------------------------------------------------------------------------------- |
| Tài khoản, nhân viên, khuyến mãi | AdministrationController | AdministrationService      | AdministrationRepository; User, Employee, Promotion                                                  |
| Vai trò, phân quyền              | AdministrationController | AuthorizationService       | RoleRepository; Role, PermissionDTO, CreateRoleRequest, RoleRequest                                  |
| System Log                       | AdministrationController | AdministrationService      | SystemLogRepository, AdministrationRepository; SystemLog, PageResponse                               |
| Cấu hình                         | AdministrationController | SystemConfigurationService | SystemConfigurationRepository; SystemConfiguration, ConfigurationRequest                             |
| Đăng nhập tích hợp               | AuthenticationController | AuthorizationService       | AuthenticationRepository; Account, LoginRequest, SessionUser, PasswordHasher                         |
| Admin lần đầu cục bộ             | AdminSetupController     | AdminSetupService          | AdminSetupRequest, PasswordHasher, SystemLogRepository                                               |
| Báo cáo                          | ReportController         | ReportService              | ReportRepository; ReportFilter, ReportData, ReportSummary, SaleReport; ReportCsv, ReportXlsx         |
| Tư vấn xe                        | AIController             | AIService                  | AIRepository, AICatalogue; VehicleModel, VehicleSpecifications, AIRequest, AIResponse, AIConstraints |

`entities` chứa dữ liệu nghiệp vụ. `dto` chứa request, response, bộ lọc và các dòng tổng hợp. `security` quản lý phiên/mật khẩu/quyền truy cập. `config.Database` mở kết nối; `util.BootstrapAdmin` tạo Admin đầu tiên theo mật khẩu tự nhập trên máy.

## API và giới hạn nghiệp vụ

| URL                              | Chức năng                                                                                    |
| -------------------------------- | -------------------------------------------------------------------------------------------- |
| `/api/auth`                      | Trạng thái phiên, đăng nhập, đăng xuất và CSRF token                                         |
| `/api/setup`                     | Tạo Admin đầu tiên nếu server bật tùy chọn và yêu cầu đến từ localhost; khóa khi đã có Admin |
| `/api/admin/lookups`             | Danh sách vai trò, trạng thái, mẫu xe                                                        |
| `/api/admin/users`, `/employees` | Xem/tạo/sửa tài khoản nhân viên và thông tin nhân viên                                       |
| `/api/admin/roles`               | Xem/tạo/sao chép vai trò, cấp/thu hồi quyền                                                  |
| `/api/admin/promotions`          | Tạo/sửa/ngừng khuyến mãi                                                                     |
| `/api/admin/logs`                | Tra cứu và xuất nhật ký                                                                      |
| `/api/admin/configurations`      | Xem và cập nhật cấu hình                                                                     |
| `/api/reports`                   | Báo cáo JSON, CSV hoặc XLSX                                                                  |
| `/api/ai`                        | Danh mục, tư vấn thông số, so sánh, phân tích nhu cầu, gợi ý xe                              |

Tên JSON giữ nguyên để giao diện hiện tại hoạt động sau khi đổi package/class. Mật khẩu chỉ nhận qua request; không trả password/hash trong dữ liệu giao diện. Admin, quyền nội bộ và lịch sử dữ liệu được bảo vệ.

AI chạy `CATALOG_RULES` từ dữ liệu danh mục xe. AI không gọi PaymentService, không truy vấn bảng tài chính, không thanh toán/tạo đơn hàng/tạo hợp đồng/xác nhận mua xe. Những module nghiệp vụ đó thuộc các thành viên khác.

Không sao chép database, thông tin đăng nhập, `.env`, lịch sử Git hoặc tài nguyên quán cafe từ ZIP mẫu vào bài xe điện.
