# ElectricCarSales

Dự án SWP391 quản lý cửa hàng ô tô điện. Phần của **Phạm Trần Phúc Tấn** đã được triển khai trên Java 24, Maven WAR, Jakarta Servlet 6, Tomcat 10.1 và SQL Server.

Chức năng gồm quản trị tài khoản/nhân viên, tạo và sao chép vai trò, phân quyền, khuyến mãi, báo cáo kinh doanh, nhật ký và cấu hình hệ thống, trợ lý tư vấn danh mục xe.

Đã bổ sung tổng quan kinh doanh, đối chiếu báo cáo với kỳ trước, lọc tài khoản theo vai trò/trạng thái, lọc nhân viên, quản lý hiệu lực khuyến mãi, đổi mật khẩu cá nhân và bắt đầu cuộc tư vấn mới. Cách tổ chức giao diện tham khảo Shopify, Salesforce và VinFast; xem [nguồn tham khảo và cách áp dụng](docs/THAM_KHAO_THIET_KE.md).

## Chạy trên máy của thành viên

1. Mở `ElectricCarSales/pom.xml` dưới dạng Maven project với **JDK 24**. Dùng **Tomcat 10.1** để triển khai file `ElectricCarSales/target/ElectricCarSales.war` sau khi chạy `mvn -f ElectricCarSales/pom.xml clean verify`.
2. Trong SSMS, chạy [ElectricCarSalesDB.sql](ElectricCarSalesDB.sql) rồi [ElectricCarSalesFeatureMigration.sql](ElectricCarSalesFeatureMigration.sql) ở thư mục gốc. Chạy cả hai với database mới hoặc database đã có. Hai file bổ sung schema và các mục danh mục còn thiếu, giữ nguyên dữ liệu kinh doanh. Báo cáo đọc dữ liệu từ các giao dịch hiện có trên từng database.
3. Chạy `.\Setup-LocalConfig.ps1` rồi sửa `%USERPROFILE%\.electriccarsales\db.properties` theo tên máy, cổng, tài khoản và mật khẩu SQL Server của bạn. Có thể đặt `DB_URL`, `DB_USER`, `DB_PASSWORD` trong môi trường của Tomcat. Khởi động lại Tomcat sau khi đổi cấu hình.
4. Mở `/ElectricCarSales/`. Nút **Trợ lý xe** mở `/ElectricCarSales/customer/ai-assistant.jsp` và khách có thể tư vấn xe ngay. Đăng nhập bằng tài khoản có quyền để mở **Quản trị** ở `/ElectricCarSales/admin.jsp`. Nếu database chưa có Admin, xem cách tạo tài khoản đầu tiên trong [hướng dẫn chạy trên IntelliJ](docs/HUONG_DAN_INTELLIJ.md#4-tạo-admin-đầu-tiên).

Mở các file SQL dưới dạng UTF-8. Nếu dùng `sqlcmd`, thêm `-b -f 65001` để báo lỗi và giữ đúng dấu tiếng Việt. Xem [nguồn dữ liệu danh mục VinFast](docs/NGUON_DU_LIEU_VINFAST.md).

Trên Windows, có thể khởi động Tomcat từ thư mục gốc bằng PowerShell.

```powershell
.\docs\Chay-Web.ps1 -Port 8081
```

Đặt `JAVA_HOME` tới JDK 24 và `CATALINA_HOME` tới Tomcat 10.1 trên máy đang chạy; hoặc truyền `-JavaHome` và `-TomcatHome` cho script. Địa chỉ web là `http://localhost:8081/ElectricCarSales/`; thêm `-OpenBrowser` để tự mở trình duyệt. Dừng web bằng `.\docs\Dung-Web.ps1 -Port 8081`; sau khi sửa code, chạy `.\docs\Chay-Web.ps1 -Port 8081 -Restart -Build` để dừng server của script rồi build sạch. Cấu hình SQL Server bằng `DB_URL`, `DB_USER`, `DB_PASSWORD` hoặc `db.properties` riêng trên từng máy theo hướng dẫn.

```text
ElectricCarSales/src/main/java/
  com/electriccarsales/
    controller/   Các Servlet và SecurityFilter
    dao/          AdministrationDAO, AuthenticationDAO, AdminRoleDAO, SystemConfigurationDAO,
                  SystemLogDAO, ReportDAO, AIDAO, AICatalogue
    model/        Dữ liệu nghiệp vụ, request/response, SessionUser, ApiException
    service/      Nghiệp vụ quản trị, phân quyền, báo cáo, tư vấn xe;
                  PasswordHasher, PasswordSession, BootstrapAdmin, ReportCsv, ReportXlsx
    Main.java     Kiểm tra kết nối DB
  util/
    DBContext.java  Kết nối SQL Server dùng chung theo sườn của nhóm
ElectricCarSales/src/main/webapp/index.jsp, admin.jsp, customer/ai-assistant.jsp
ElectricCarSales/src/main/webapp/assets/css/theme.css, home.css
ElectricCarSales/src/main/webapp/assets/js/home.js
ElectricCarSales/src/main/webapp/assets/admin-common.js, admin-management.js, sales-reports.js
ElectricCarSales/src/main/webapp/assets/js/customer-ai.js
ElectricCarSalesDB.sql
```

Package gốc `com.electriccarsales` và bốn package `controller`, `dao`, `model`, `service` theo sườn `main` của nhóm. Các class dữ liệu giữ constructor/getter/setter. Giao diện là JSP; CSS, JavaScript, API và đường dẫn xuất báo cáo lấy theo context của ứng dụng, không cố định tên thư mục deploy. Dự án giữ Jakarta Servlet/JDBC và WAR chạy Tomcat. Xem [cấu trúc mã nguồn](docs/CAU_TRUC_MA_NGUON.md) và [giải thích luồng code](docs/GIAI_THICH_CODE.md).

Toàn bộ mã nguồn được format với 4 dấu cách, khối điều kiện/vòng lặp có đủ `{ }` và dòng trống giữa các hàm. Có cấu hình dùng lại cho IntelliJ, Maven và Prettier; xem [quy tắc format](docs/FORMAT_CODE.md).

Chạy `mvn -f ElectricCarSales/pom.xml clean verify` để kiểm tra WAR và các luồng HTTP. Sau khi triển khai, thử mở Quản trị và Trợ lý xe bằng cấu hình SQL Server của máy chạy.

AI dùng chế độ **CATALOG_RULES** chạy từ dữ liệu xe, không gọi mô hình ngôn ngữ bên ngoài. PDF được lưu qua nút **In / Lưu PDF** của trình duyệt; Excel là file XLSX thực. Đăng ký/OTP, thanh toán, bán xe và quản lý kho thuộc phần của các thành viên khác.
