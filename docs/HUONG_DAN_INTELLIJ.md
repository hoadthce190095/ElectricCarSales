# Chạy phần Phạm Trần Phúc Tấn trên IntelliJ

Mã nguồn đã có chức năng quản trị tài khoản và nhân viên, phân quyền, khuyến mãi, báo cáo, System Log, cấu hình vận hành và tư vấn xe. Dự án giữ Java Servlet/Maven WAR để ghép với phần của các thành viên khác.

## 1. Mở dự án

**Cấu hình chạy trực tiếp Tomcat trên máy hiện tại:** mở thư mục gốc `D:\SWP391-ElectricCars`, chọn **ElectricCarSales - Tomcat 10.1** ở thanh Run rồi bấm **Run**. Cấu hình trong `.idea/runConfigurations` dùng JDK 24, build artifact **ElectricCarSales:war exploded** rồi deploy lên Tomcat 10.1.60 tại `http://localhost:8080/ElectricCarSales/`. Bấm **Stop** của IntelliJ để dừng Tomcat.

Nếu báo **Required plugin Tomcat Server is disabled**, vào **Settings → Plugins → Installed**, bật **Tomcat and TomEE** và **Jakarta EE: Application Servers**. Khi vừa kích hoạt Trial/Ultimate, khởi động lại IDE để nạp đủ plugin, rồi **Reload All Maven Projects** để tạo artifact web. Nếu Deployment vẫn hiện tên artifact màu đỏ hoặc báo `deployment source ... is not valid`, bỏ mục tham chiếu cũ khỏi Deployment, thêm lại **Artifact → ElectricCarSales:war exploded** từ danh sách vừa đồng bộ và đặt lại context `/ElectricCarSales` trước khi chạy.

**Cách chạy dự phòng:** chọn **ElectricCarSales - Chạy web** để dùng Maven profile `local-web`, build sạch rồi chạy Tomcat bằng script. Chọn **ElectricCarSales - Dừng web** để tắt server của script. Nút Stop của tác vụ Maven chỉ dừng tác vụ đó; Tomcat chạy riêng nên dùng cấu hình Dừng web để tắt. Dừng server của script trước khi chạy Tomcat trực tiếp ở cùng cổng 8080. Máy khác sửa `web.javaHome`, `web.tomcatHome` và `web.port` trong cấu hình Maven theo đường dẫn cài đặt của máy đó.

Các bước dưới đây áp dụng khi import Maven mới hoặc chuyển sang máy khác:

1. Trong IntelliJ chọn **File → Open**, mở `ElectricCarSales/pom.xml`, chọn mở như project Maven.
2. Chọn **Project SDK = JDK 24** và **Language level = 24**. Trong Maven Runner và Maven Importer cũng chọn JDK 24. Máy hiện tại có JDK ở `C:\Program Files\Java\jdk-24\New Folder`; máy khác chọn thư mục JDK tương ứng.
3. Bấm **Reload All Maven Projects**. Package gốc hiện là `com.electriccarsales`, gồm `controller`, `dao`, `model`, `service` theo sườn `main` của nhóm; `util.DBContext` dùng chung nằm ngoài package gốc. Lớp `Main` chỉ kiểm tra kết nối DB; website chạy bằng cấu hình Tomcat.

Nếu mở cả thư mục gốc, nhấp phải `ElectricCarSales/pom.xml` → **Add as Maven Project**. Cấu hình `.idea` đang có của bạn được giữ nguyên; có thể đang chọn JDK khác nên cần chọn lại SDK trong IntelliJ.

Nếu IntelliJ báo `package jakarta.servlet.annotation does not exist`, `package jakarta.servlet.http does not exist` hoặc lỗi `doGet`/`doPost` không override được: nhấp phải `ElectricCarSales/pom.xml` → **Add as Maven Project**, mở cửa sổ **Maven** và bấm **Reload All Maven Projects**, chờ tải dependency và đồng bộ xong rồi chọn **Build → Build Project** (`Ctrl+F9`). POM đã khai báo `jakarta.servlet-api:6.0.0` với scope `provided`; Maven cung cấp thư viện này khi biên dịch, Tomcat 10.1 cung cấp khi chạy. Giữ các import `jakarta`; lỗi override sẽ hết khi IntelliJ nhận được lớp `HttpServlet`. Kiểm tra **External Libraries** có thư viện `jakarta.servlet-api` sau khi reload.

Nếu xuất hiện **Project JDK is not defined**, bấm **Setup SDK → 24**. Máy này đã có OpenJDK 24. Sau khi sửa POM, nhấp phải `ElectricCarSales/pom.xml` → **Maven → Sync Project** để IntelliJ nhận lại module và thư viện. POM của bản hoàn chỉnh còn có Jackson cho JSON, Apache POI cho Excel, JUnit/H2/Tomcat nhúng cho kiểm thử; dùng đầy đủ file này khi mở project.

## 2. Chuẩn bị SQL Server

Kết nối SQL Server bằng SSMS và chạy duy nhất `ElectricCarSalesDB.sql` ở thư mục gốc. File này chứa schema chung, danh mục VinFast và dữ liệu DEMO cho báo cáo:

- **Chưa có ElectricCarSalesDB:** script tạo database và các bảng chung.
- **Đã có ElectricCarSalesDB:** cùng script bổ sung các bảng còn thiếu, giữ database và dữ liệu hiện có.

Schema chung khai báo Suppliers trước InventoryTransactions để tạo khóa ngoại đúng thứ tự, đồng thời có RolePermissions, SystemLogs và SystemConfigurations cho phần của Tấn. Trigger bảo vệ SystemLogs khỏi UPDATE/DELETE. Script không tạo tài khoản hay mật khẩu Admin; tự tạo Admin theo mục 4.

**Dữ liệu có trong cùng file SQL:** sau schema, script nạp danh mục VinFast và 30 xe kho DEMO, rồi bộ báo cáo giả lập gồm 9 đơn cùng thanh toán, trả góp và khuyến mãi vào `ElectricCarSalesDB`. Danh mục có thông số/giá từ nguồn VinFast; xe kho và mọi giao dịch DEMO đều giả lập. Nếu đã có đơn hàng, thanh toán hoặc trả góp ngoài bộ DEMO, script thông báo bằng `PRINT` và bỏ qua phần nạp báo cáo DEMO; schema và danh mục vẫn chạy. Chạy lại chỉ bổ sung bản ghi còn thiếu, không thêm trùng, không xóa hay ghi đè dữ liệu đã có. Đọc `docs/NGUON_DU_LIEU_VINFAST.md` và `docs/KICH_BAN_BAO_CAO.md` để biết nguồn và cách giải thích số liệu.

SQL Server phải cho phép kết nối JDBC/TCP tới instance và cổng bạn cấu hình. Ví dụ máy dùng SQLEXPRESS có thể có cổng khác 1433; xem SQL Server Configuration Manager để lấy cổng đang dùng. Không cần đổi tên instance nếu đã cấu hình đúng URL.

## 3. Cấu hình kết nối

Trong cấu hình chạy Tomcat và cấu hình chạy BootstrapAdmin của IntelliJ, đặt các biến môi trường:

```properties
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=ElectricCarSalesDB;encrypt=true;trustServerCertificate=true
DB_USER=YOUR_SQL_LOGIN
DB_PASSWORD=YOUR_SQL_PASSWORD
```

Thay cổng, tên đăng nhập và mật khẩu theo SQL Server của bạn. URL trên phù hợp cho phát triển cục bộ với chứng chỉ tự ký. Khi triển khai máy chủ có chứng chỉ hợp lệ, dùng cấu hình xác minh chứng chỉ phù hợp.

Cách khác: sao chép `src/main/resources/db.properties.example` thành `db.properties`, điền thông tin thực tế. IntelliJ có thể dùng file này trong module resources khi chạy artifact exploded. Maven loại file riêng này khỏi WAR đóng gói; khi chạy WAR chia sẻ, đặt biến môi trường `DB_*` cho Tomcat. Không gửi file có mật khẩu cùng mã nguồn.

Thứ tự cấu hình: JVM property `-D...` → biến môi trường → file `db.properties` → mặc định. Mặc định không có mật khẩu SQL Server.

**Cấu hình cục bộ đã kiểm tra trên máy này:** JDBC kết nối được tới `localhost:1433`, database `ElectricCarSalesDB`, bằng **Windows Authentication** với URL:

```properties
jdbc:sqlserver://localhost:1433;databaseName=ElectricCarSalesDB;encrypt=true;trustServerCertificate=true;integratedSecurity=true;authenticationScheme=NativeAuthentication
```

Windows Authentication đã được kiểm tra với cấu hình cục bộ. File `ElectricCarSales/src/main/resources/db.properties` là cấu hình riêng mà bạn có thể đổi sang SQL login; bản ZIP/WAR chia sẻ không chứa file này. Để dùng Windows Authentication, trong cấu hình chạy Tomcat thêm **VM options**:

```text
-Djava.library.path=D:\SWP391-ElectricCars\tool\jdbc-auth
```

Thư mục trên chứa `mssql-jdbc_auth-13.4.0.x64.dll`, tải từ artifact Microsoft trên Maven Central, đã đối chiếu checksum và xác minh chữ ký **Microsoft Corporation** hợp lệ. Driver dùng DLL để xác thực bằng tài khoản Windows của tiến trình Java; có thể chỉ định thư mục DLL qua `java.library.path` theo [hướng dẫn Microsoft Learn về JDBC integrated authentication](https://learn.microsoft.com/en-us/sql/connect/jdbc/building-the-connection-url?view=sql-server-ver17#connecting-with-integrated-authentication-on-windows).

DLL và file cấu hình riêng của máy này không được đưa vào ZIP. Máy khác dùng SQL login theo ví dụ `DB_USER`/`DB_PASSWORD` ở trên, hoặc cấu hình Windows Authentication bằng tài khoản Windows có quyền trên SQL Server của mình, với DLL đúng phiên bản driver và kiến trúc JVM. Kiểm tra kết nối này chỉ xác nhận JDBC truy cập được database; các bước kiểm tra nghiệp vụ trên SQL Server vẫn thực hiện theo mục 9.

## 4. Tạo Admin đầu tiên

**Tự đặt ngay trên web:** chạy `docs/Chay-Web.ps1`, mở `http://localhost:8081/ElectricCarSales/tan.jsp`, chọn **Tạo Admin đầu tiên** và nhập mật khẩu hai lần. Sau đó đăng nhập bằng tài khoản vừa tạo. Màn hình chỉ được bật ở máy chủ cục bộ, yêu cầu CSRF, không còn dùng được khi có bất kỳ Admin nào và không trả mật khẩu/hash ra API. Không cần gửi mật khẩu trong chat.

Nếu chạy bằng cấu hình Tomcat của IntelliJ, thêm VM option `-Dapp.adminSetup.enabled=true` để bật màn hình này trên localhost khi DB chưa có Admin. Mặc định trong WAR là tắt. Khi đã tạo Admin, có thể bỏ option. Công cụ PowerShell bên dưới vẫn dùng được nếu bạn thích đặt mật khẩu trong Terminal.

**Tự đặt mật khẩu trên máy này:** trong tab **Terminal** của IntelliJ, dùng PowerShell tại thư mục `D:\SWP391-ElectricCars` và chạy:

```powershell
.\docs\Tao-Admin.ps1
```

Script hỏi mật khẩu hai lần với nội dung được che; nhập 10–128 ký tự. Tên đăng nhập mặc định là `admin`; có thể đổi bằng `-Username ten_dang_nhap`. Script dùng lớp `BootstrapAdmin` từ bản WAR đã build, cấu hình DB cục bộ hoặc biến `DB_*` hiện có, và DLL Windows Authentication khi có trong `tool/jdbc-auth`. Mật khẩu chỉ được truyền tạm qua môi trường tiến trình Java, không được in hay ghi vào file; các biến môi trường trước đó được khôi phục sau khi chạy. Khi thấy `Created initial Admin: admin`, mở giao diện đăng nhập tại `http://localhost:8080/ElectricCarSales/tan.jsp` nếu chạy IntelliJ, hoặc `http://localhost:8081/ElectricCarSales/tan.jsp` nếu chạy script, và dùng mật khẩu vừa tự nhập. Không cần gửi mật khẩu trong chat.

Nếu chưa có bản build, chạy Maven `verify` trước. Script tìm JDK 24 từ `JAVA_HOME`; máy khác có thể chỉ định `-JavaHome 'C:\duong-dan\jdk-24'`. Nếu PowerShell báo script bị chặn, có thể chạy một lần bằng `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\docs\Tao-Admin.ps1`; lệnh này không đổi execution policy được lưu trên máy.

**Cách chạy trực tiếp lớp Java trong IntelliJ:**

Sau khi chạy schema chung, mở `com.electriccarsales.service.BootstrapAdmin` và chạy `main` từ IntelliJ với các biến DB ở trên, thêm:

```properties
ADMIN_USERNAME=admin
ADMIN_PASSWORD=YOUR_INITIAL_PASSWORD
```

Mật khẩu phải dài 10–128 ký tự. Công cụ tạo tài khoản ADMIN/ACTIVE, băm PBKDF2 và ghi nhật ký; không có mật khẩu đăng nhập mặc định trong dự án. Công cụ từ chối nếu đã có Admin để tránh ghi đè tài khoản.

Nếu database đã có Admin từ code cũ, đối chiếu định dạng PasswordHash. Bản này dùng `pbkdf2-sha256$iterations$salt$hash`; mật khẩu dạng plain text hoặc thuật toán khác không đăng nhập được qua lớp tích hợp này. Ghép `PasswordHasher` với AuthenticationService của thành viên phụ trách tài khoản hoặc dùng quy trình đặt lại mật khẩu của nhóm. Không xóa Admin để xử lý lỗi đăng nhập; chạy schema chung không đặt lại mật khẩu.

## 5. Build và chạy Tomcat

Trong cửa sổ Maven của IntelliJ, chạy **Lifecycle → verify**, hoặc terminal trong `ElectricCarSales`:

```powershell
mvn clean verify
```

File đầu ra là `ElectricCarSales/target/ElectricCarSales.war`.

**Chạy bằng script:** đặt `JAVA_HOME` tới JDK 24, `CATALINA_HOME` tới Tomcat 10.1 của máy đang chạy, rồi từ thư mục gốc chạy `.\docs\Chay-Web.ps1`. Script dùng các đường dẫn môi trường đó, deploy `target/ElectricCarSales`, chỉ nghe `127.0.0.1` và kiểm tra web trả HTTP 200 trước khi thông báo. Script mặc định dùng cổng **8081**, còn Tomcat trong IntelliJ dùng **8080**. Có thể truyền `-TomcatHome`, `-JavaHome` và `-Port 8082` nếu cần một cổng khác. Khi đổi cổng, truyền cùng `-Port` cho script chạy và dừng. Khi sửa code, chạy `.\docs\Dung-Web.ps1`, rồi `.\docs\Chay-Web.ps1 -Build`. Nếu tự chọn cùng cổng cho cả hai cách chạy, dừng một server trước khi chạy server còn lại.

**IntelliJ Ultimate:** sau khi import Maven, cấu hình chạy như sau:

1. Mở **Run → Edit Configurations**, chọn cấu hình **Tomcat Server → Local** hiện có hoặc tạo mới. Chọn Tomcat **10.1**, JRE **24**.
2. Trong tab **Deployment**, bấm **+ → Artifact → ElectricCarSales:war exploded**. Đặt **Application context** là `/ElectricCarSales`.
3. Nếu danh sách đang có artifact `unnamed` chỉ chứa module output ở gốc, thiếu `WEB-INF/classes`, `WEB-INF/lib` và tài nguyên web, bỏ mục `unnamed` khỏi **Deployment**. Dùng artifact `ElectricCarSales:war exploded` do Maven import tạo để có Servlet, dependency, `web.xml`, `tan.jsp` và assets trong đúng cấu trúc WAR.
4. Trong **Before launch**, thêm hoặc chọn **Build Artifacts → ElectricCarSales:war exploded**; bỏ tác vụ build `unnamed` cũ. Giữ tác vụ **Build/Make** nếu đã có.
5. Đặt biến DB trong cấu hình chạy, bấm **Apply**, rồi **Run**. Nếu chưa thấy artifact `ElectricCarSales:war exploded`, trở lại cửa sổ **Maven**, bấm **Reload All Maven Projects** và chờ đồng bộ xong trước khi chọn lại.

**IntelliJ Community:** build WAR như trên, sao chép WAR vào thư mục `webapps` của Tomcat 10.1. Đặt JAVA_HOME vào JDK 24 và các biến DB trong môi trường chạy Tomcat; khởi động bằng `bin/catalina.bat run` trong terminal của bạn. Tránh chạy đồng thời hai Tomcat cùng cổng 8080. Không dùng Tomcat 9 vì dự án sử dụng Jakarta Servlet.

Mở [trang chủ](http://localhost:8080/ElectricCarSales/). Welcome file `index.jsp` hiển thị giao diện VinFast xanh dương theo `main`, gồm banner, danh mục, bộ lọc và so sánh tối đa 3 xe. Nút **Quản trị** và **Trợ lý xe** dẫn sang [không gian làm việc](http://localhost:8080/ElectricCarSales/tan.jsp); nút **Đăng nhập** mở form qua `tan.jsp?login=1`. Cả hai trang dùng chung bảng màu và kiểu chữ từ `assets/css/theme.css`. Đường dẫn xem thử cũ `preview-main.jsp` forward tới trang chủ này.

## 6. Các chức năng

| Chức năng        | Hành vi                                                                                                                                                             |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Danh mục VinFast | Xem hình, thông số, giá niêm yết tham khảo, chuẩn tầm hoạt động và nguồn; tìm/lọc, xem chi tiết, so sánh và chuyển sang tư vấn.                                     |
| Tài khoản        | Tạo nhân viên, sửa liên hệ, đổi vai trò, ACTIVE/INACTIVE/LOCKED, đặt mật khẩu mới. Không xóa tài khoản có dữ liệu liên quan.                                        |
| Nhân viên        | Tạo cùng tài khoản, sửa họ tên/email/điện thoại, đồng bộ với Users.                                                                                                 |
| Phân quyền       | Tạo vai trò, sao chép quyền, cấp/thu hồi MANAGE_USERS, MANAGE_PROMOTIONS, VIEW_REPORTS, VIEW_LOGS.                                                                  |
| Khuyến mãi       | Tạo/sửa ưu đãi theo phần trăm hoặc số tiền, khoảng ngày, danh sách mẫu xe; ngừng hoạt động để giữ lịch sử.                                                          |
| Báo cáo          | Ngày/tháng/quý/năm, nhân viên, mẫu xe; thu tiền, giá trị xe bán, số xe, trạng thái, tồn kho, công nợ; xuất XLSX/CSV và In/Lưu PDF. API hỗ trợ thêm paymentMethodId. |
| System Log       | Ghi đăng nhập, thay đổi tài khoản/quyền/khuyến mãi/cấu hình, xem/xuất báo cáo; lọc ngày/người dùng/thao tác/module, xuất toàn bộ kết quả CSV tối đa 50.000 dòng.    |
| Cấu hình         | SESSION_TIMEOUT_MINUTES 5–120 phút; REPORT_MAX_DAYS 1–3660 ngày. Có hiệu lực ở yêu cầu kế tiếp.                                                                     |
| Trợ lý xe        | Hỏi thông số, so sánh 2–8 mẫu, phân tích nhu cầu và gợi ý theo ngân sách/số chỗ/tầm hoạt động; giữ ngữ cảnh trong phiên.                                            |

ADMIN có toàn bộ quyền và chỉ Admin được quản lý vai trò/cấu hình. Mặc định MANAGER và ACCOUNTANT có VIEW_REPORTS. Các vai trò nhân viên khác được Admin cấp quyền khi cần. CUSTOMER không được cấp quyền nội bộ. Quyền và trạng thái được đọc lại từ database ở mỗi yêu cầu; khóa tài khoản có tác dụng với phiên đang mở. Quyền ADMIN và ít nhất một Admin đang hoạt động được bảo vệ. Schema hiện tại gắn mỗi user với một RoleID.

Các thay đổi quản trị và bản ghi nhật ký nằm cùng transaction: nếu ghi log thất bại thì dữ liệu quản trị rollback. Đăng xuất vẫn hủy phiên khi database lỗi và hiển thị hạn chế ghi log. API ghi dữ liệu yêu cầu `X-CSRF-Token` lấy từ GET `/api/auth`.

## 7. Đối chiếu số liệu báo cáo

Doanh thu thực thu là tổng Payments thành công theo PaymentDate, loại giao dịch hủy/chứng từ không hợp lệ. Đây là khoản đã nhận, chưa phải chính sách doanh thu kế toán được nhóm xác định. Giá trị xe đã bán tính từ giao dịch PAID/COMPLETED theo OrderDate; mỗi CarID chỉ đếm một lần. Một đơn có nhiều xe/thanh toán không nhân đôi số tiền.

InstallmentPayments hiển thị riêng vì schema chưa xác định quan hệ ghi trùng với Payments. Tồn kho là trạng thái hiện tại, không phải số dư cuối kỳ lịch sử. Chi tiết cách tính và giao dịch chưa nhất quán xuất hiện trên giao diện và file Excel. Kiểm tra các mục này khi ghép Payment/Order/Inventory của nhóm.

Excel là XLSX có ô số và chuỗi văn bản, CSV chặn chuỗi có thể trở thành công thức. PDF hiện sử dụng **In / Lưu PDF** của trình duyệt.

## 8. Giới hạn tích hợp

Trợ lý chạy chế độ **CATALOG_RULES**, xử lý tiếng Việt và trả lời từ dữ liệu catalogue; không gọi ChatGPT/OpenAI hoặc LLM bên ngoài và không cần khóa API. Chỉ truy vấn bảng xe/hãng/dòng/thông số/phiên bản/màu/trạng thái sản phẩm. Giá lấy từ xe có thể bán, thông số thiếu được thông báo. Yêu cầu tài chính, thanh toán, giữ chỗ, đơn hàng, hợp đồng hoặc xác nhận mua xe bị từ chối; trợ lý không gọi PaymentService.

Đăng nhập hiện là lớp tích hợp để chạy các màn hình của Tấn; đăng ký/OTP, gửi email thông tin tài khoản, dữ liệu phòng ban và nghiệp vụ các thành viên khác cần ghép với module của nhóm. ZIP không có cấu hình dịch vụ email hay cột phòng ban. Không có tính năng gửi email tự động. Bộ DEMO trong file SQL chung phục vụ trình bày; phần nạp báo cáo được bỏ qua khi có giao dịch ngoài bộ DEMO. Đây không phải dữ liệu bán hàng thực tế. Promotions dùng chung với phần catalogue của Hoa.

## 9. Kiểm tra trước khi nộp

Ngày 17/10/2026, chạy `mvn clean verify` trên JDK 24: **92 kiểm thử đạt, 0 failures, 0 errors** (88 unit/HTTP và 4 kiểm thử WAR). Kiểm thử bao gồm nghiệp vụ/H2 chế độ MSSQLServer, HTTP chạy Tomcat 10.1 và triển khai chính file WAR. Kiểm thử WAR sử dụng web.xml và tự phát hiện Servlet/Filter annotations, xác nhận trang chủ/JSP/assets ở `/ElectricCarSales` và `/showroom`, đăng nhập, báo cáo, màn hình setup bị tắt mặc định và trợ lý xe. Có kiểm tra tạo Admin lần đầu/CSRF/rollback và nhãn dữ liệu DEMO trong CSV/XLSX. Cả ba bộ kiểm tra JavaScript trong `docs/FORMAT_CODE.md` đều đạt, gồm tìm/lọc/so sánh trên trang chủ và tải lại danh mục sau HTTP 503.

Trên máy này, ElectricCarSalesDB đã có RolePermissions, SystemLogs và SystemConfigurations, 10 phiên bản VinFast, 30 xe kho DEMO và 9 đơn giả lập. Bộ dữ liệu mẫu đã được kiểm tra nạp lại: giữ số dòng, không thêm trùng. Báo cáo qua ReportService trên SQL Server đã khớp 6 xe bán, giá trị bán 4.413.000.000 VND và thu tiền 4.565.900.000 VND. Bản hiện tại đã chạy bằng cấu hình native **ElectricCarSales - Tomcat 10.1** trong IntelliJ, Tomcat 10.1.60 tại `http://localhost:8080/ElectricCarSales/`: trang chủ, không gian làm việc, form đăng nhập và assets trả HTTP 200; API trợ lý đọc 10 phiên bản từ SQL Server. Đã xem giao diện desktop, chưa gửi form đăng nhập trên SQL Server. Lần kiểm tra script trước đó dùng cổng 8081; script chạy/dừng chỉ quản lý server của nó. Cần bạn tự đặt mật khẩu Admin theo mục 4 để dùng các màn hình quản trị trên DB này. Số liệu giả lập đã được đối chiếu, chưa xác nhận chính sách kế toán của dữ liệu bán hàng thật.

Sau khi cấu hình SQL Server, kiểm tra:

1. Đăng nhập Admin; tạo nhân viên và vai trò mới; cấp VIEW_REPORTS cho vai trò, thử bằng tài khoản mới.
2. Khóa tài khoản đang đăng nhập ở trình duyệt khác; xác nhận yêu cầu tiếp theo bị từ chối. Giữ Admin hoạt động.
3. Tạo/sửa/ngừng khuyến mãi cho mẫu xe thật; xem nhật ký trước/sau.
4. Xem báo cáo khoảng ngày có dữ liệu, đối chiếu tiền/xe với dữ liệu nguồn; mở XLSX bằng Excel và lưu PDF.
5. Đổi REPORT_MAX_DAYS, kiểm tra khoảng vượt giới hạn bị từ chối; khôi phục giá trị mong muốn.
6. Hỏi thông số xe thật, so sánh hai mẫu, gợi ý theo ngân sách; yêu cầu thanh toán hoặc tạo hợp đồng phải bị từ chối.

Lỗi HTTP 404 tại `/ElectricCarSales/tan.jsp`: kiểm tra tab **Deployment** dùng `ElectricCarSales:war exploded`, context `/ElectricCarSales`, tác vụ **Before launch** build đúng artifact và log Tomcat báo triển khai thành công. Artifact `unnamed` chỉ chứa lớp Java ở gốc không có trang web hoặc cấu trúc WAR đầy đủ nên có thể khiến Tomcat khởi động nhưng URL vẫn trả 404.

Lỗi HTTP 503: kiểm tra DB_URL/DB_USER/DB_PASSWORD, TCP/cổng SQL Server và các bảng từ `ElectricCarSalesDB.sql`. Lỗi 401: đăng nhập hoặc kiểm tra ACTIVE/mật khẩu. Lỗi 403: quyền vai trò hoặc CSRF; tải lại trang để lấy phiên mới. Nếu cổng 8080 đã dùng, chọn cổng khác và thay URL trình duyệt tương ứng.
