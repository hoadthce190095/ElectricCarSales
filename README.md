# ElectricCarSales

Dự án SWP391 quản lý cửa hàng ô tô điện. Phần của **Phạm Trần Phúc Tấn** đã được triển khai trên Java 24, Maven WAR, Jakarta Servlet 6, Tomcat 10.1 và SQL Server.

Chức năng gồm quản trị tài khoản/nhân viên, tạo và sao chép vai trò, phân quyền, khuyến mãi, báo cáo kinh doanh, nhật ký và cấu hình hệ thống, trợ lý tư vấn danh mục xe.

Đã bổ sung tổng quan kinh doanh, đối chiếu báo cáo với kỳ trước, lọc tài khoản theo vai trò/trạng thái, lọc nhân viên, xem hiệu lực và tính thử khuyến mãi, đổi mật khẩu cá nhân và bắt đầu cuộc tư vấn mới. Cách tổ chức giao diện tham khảo Shopify, Salesforce và VinFast; xem [nguồn tham khảo và cách áp dụng](docs/THAM_KHAO_THIET_KE.md).

Đọc [hướng dẫn chạy trên IntelliJ](docs/HUONG_DAN_INTELLIJ.md) trước khi triển khai. Mở `ElectricCarSales/pom.xml` dưới dạng Maven project. Trang chủ `/ElectricCarSales/` dùng giao diện VinFast xanh dương theo `main`; không gian quản trị và trợ lý xe ở `/ElectricCarSales/admin.jsp` dùng chung bảng màu và kiểu chữ. Nút **Đăng nhập** trên trang chủ mở `/ElectricCarSales/login.jsp`; mục **Quản trị** mở không gian quản trị.

Chỉ cần chạy `ElectricCarSalesDB.sql` ở thư mục gốc trong SSMS cho cả database mới và database đã có. File này tạo `ElectricCarSalesDB` nếu thiếu, bổ sung schema chung, rồi nạp danh mục VinFast và 30 xe kho DEMO, tiếp theo là bộ báo cáo giả lập gồm 9 đơn cùng thanh toán, trả góp và khuyến mãi. Khi phát hiện đơn hàng, thanh toán hoặc trả góp không thuộc bộ DEMO, script thông báo bằng `PRINT` và bỏ qua phần nạp báo cáo DEMO; schema và danh mục vẫn chạy. Chạy lại giữ dữ liệu hiện có, không xóa database và không thêm bản ghi trùng. Script không tạo tài khoản hay mật khẩu Admin; tự tạo Admin theo hướng dẫn.

Bản trình bày có danh mục VinFast VF 3, VF 5, VF 6, VF 7, VF 8, VF 9, tách phiên bản Eco/Plus, hình xe và nguồn thông số/giá. [Nguồn dữ liệu](docs/NGUON_DU_LIEU_VINFAST.md) ghi chuẩn đo tầm hoạt động và phiên bản áp dụng. Xe tồn kho, khách hàng, giao dịch và khuyến mãi DEMO là dữ liệu mô phỏng để minh họa báo cáo, không phải kết quả kinh doanh VinFast. Xem [kịch bản báo cáo](docs/KICH_BAN_BAO_CAO.md).

Trên máy hiện tại, mở thư mục gốc `D:\SWP391-ElectricCars` trong IntelliJ, chọn **ElectricCarSales - Tomcat 10.1** rồi bấm **Run**. Cấu hình dùng JDK 24, build và deploy **ElectricCarSales:war exploded** lên Tomcat 10.1.60 tại `http://localhost:8080/ElectricCarSales/`. Bấm **Stop** trong IntelliJ để dừng server. Plugin Tomcat cần nhóm tính năng Ultimate; sau khi kích hoạt Trial, khởi động lại IDE và Reload All Maven Projects để nạp plugin và artifact web. Cấu hình Maven **ElectricCarSales - Chạy web** / **ElectricCarSales - Dừng web** được giữ làm cách chạy dự phòng.

Nếu muốn chạy bằng script tại thư mục gốc:

```powershell
.\docs\Chay-Web.ps1 -Port 8081
```

Đặt `JAVA_HOME` tới JDK 24 và `CATALINA_HOME` tới Tomcat 10.1 trên máy đang chạy; hoặc truyền `-JavaHome` và `-TomcatHome` cho script. Địa chỉ web là `http://localhost:8081/ElectricCarSales/`; thêm `-OpenBrowser` để tự mở trình duyệt. Lần đầu vào **Quản trị**, chọn **Tạo Admin đầu tiên**, tự đặt mật khẩu 10–128 ký tự rồi đăng nhập. Dừng web bằng `.\docs\Dung-Web.ps1 -Port 8081`; sau khi sửa code, chạy `.\docs\Chay-Web.ps1 -Port 8081 -Restart -Build` để dừng server của script rồi build sạch. Cấu hình SQL Server bằng `DB_URL`, `DB_USER`, `DB_PASSWORD` hoặc `db.properties` riêng trên từng máy theo hướng dẫn.

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
ElectricCarSales/src/main/webapp/index.jsp, login.jsp, admin.jsp
ElectricCarSales/src/main/webapp/assets/css/theme.css, home.css
ElectricCarSales/src/main/webapp/assets/js/home.js
ElectricCarSales/src/main/webapp/assets/admin-dashboard.css
ElectricCarSales/src/main/webapp/assets/admin-common.js, admin-management.js, sales-reports.js, vehicle-catalogue.js, vehicle-assistant.js, admin-dashboard.js
ElectricCarSalesDB.sql
```

Package gốc `com.electriccarsales` và bốn package `controller`, `dao`, `model`, `service` theo sườn `main` của nhóm. Các class dữ liệu giữ constructor/getter/setter. Giao diện là JSP; CSS, JavaScript, API và đường dẫn xuất báo cáo lấy theo context của ứng dụng, không cố định tên thư mục deploy. Dự án giữ Jakarta Servlet/JDBC và WAR chạy Tomcat. Xem [cấu trúc mã nguồn](docs/CAU_TRUC_MA_NGUON.md) và [giải thích luồng code](docs/GIAI_THICH_CODE.md).

Toàn bộ mã nguồn được format với 4 dấu cách, khối điều kiện/vòng lặp có đủ `{ }` và dòng trống giữa các hàm. Có cấu hình dùng lại cho IntelliJ, Maven và Prettier; xem [quy tắc format](docs/FORMAT_CODE.md).

Ngày 17/10/2026 đã chạy build sạch: **92 kiểm thử Java/WAR đạt, 0 lỗi** trên Java 24. Kiểm thử dùng H2 chế độ SQL Server, HTTP trên Tomcat và triển khai WAR ở cả `/ElectricCarSales` và `/showroom`, xác nhận JSP được biên dịch, link assets có phiên bản CSS, đăng nhập, danh mục và tải CSV/XLSX theo context. Cả ba bộ kiểm tra JavaScript đạt, gồm context triển khai, bộ lọc/so sánh trang chủ và phục hồi tải danh mục sau HTTP 503. Bản giao diện xanh dương đã chạy bằng Tomcat native của IntelliJ tại cổng 8080; API trợ lý đọc đủ 10 phiên bản từ SQL Server. Xem [kết quả kiểm tra](docs/KET_QUA_KIEM_TRA.md). WAR loại `db.properties` riêng của máy; bản chia sẻ dùng biến môi trường `DB_*` trong cấu hình chạy Tomcat.

AI dùng chế độ **CATALOG_RULES** chạy từ dữ liệu xe, không gọi mô hình ngôn ngữ bên ngoài. PDF được lưu qua nút **In / Lưu PDF** của trình duyệt; Excel là file XLSX thực. Đăng ký/OTP, thanh toán, bán xe và quản lý kho thuộc phần của các thành viên khác.
