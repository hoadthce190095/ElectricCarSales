# Chạy phần Phạm Trần Phúc Tấn trên IntelliJ

Mã nguồn đã có chức năng quản trị tài khoản và nhân viên, phân quyền, khuyến mãi, báo cáo, System Log, cấu hình vận hành và tư vấn xe. Dự án giữ Java Servlet/Maven WAR để ghép với phần của các thành viên khác.

## 1. Mở dự án

1. Trong IntelliJ chọn **File → Open**, mở `ElectricCarSales/pom.xml`, chọn mở như project Maven.
2. Chọn **Project SDK = JDK 24** và **Language level = 24**. Trong Maven Runner và Maven Importer cũng chọn JDK 24. Chọn đường dẫn JDK đã cài trên máy của bạn.
3. Bấm **Reload All Maven Projects**. Package gốc hiện là `com.electriccarsales`, gồm `controller`, `dao`, `model`, `service` theo sườn `main` của nhóm; `util.DBContext` dùng chung nằm ngoài package gốc. Lớp `Main` chỉ kiểm tra kết nối DB; website chạy bằng cấu hình Tomcat.

Nếu mở cả thư mục gốc, nhấp phải `ElectricCarSales/pom.xml` → **Add as Maven Project**. Kiểm tra Project SDK và Maven Runner sau khi import.

Nếu IntelliJ báo `package jakarta.servlet.annotation does not exist`, `package jakarta.servlet.http does not exist` hoặc lỗi `doGet`/`doPost` không override được: nhấp phải `ElectricCarSales/pom.xml` → **Add as Maven Project**, mở cửa sổ **Maven** và bấm **Reload All Maven Projects**, chờ tải dependency và đồng bộ xong rồi chọn **Build → Build Project** (`Ctrl+F9`). POM đã khai báo `jakarta.servlet-api:6.0.0` với scope `provided`; Maven cung cấp thư viện này khi biên dịch, Tomcat 10.1 cung cấp khi chạy. Giữ các import `jakarta`; lỗi override sẽ hết khi IntelliJ nhận được lớp `HttpServlet`. Kiểm tra **External Libraries** có thư viện `jakarta.servlet-api` sau khi reload.

Nếu xuất hiện **Project JDK is not defined**, bấm **Setup SDK → 24**. Sau khi sửa POM, nhấp phải `ElectricCarSales/pom.xml` → **Maven → Sync Project** để IntelliJ nhận lại module và thư viện. POM của bản hoàn chỉnh còn có Jackson cho JSON, Apache POI cho Excel, JUnit/H2/Tomcat nhúng cho kiểm thử; dùng đầy đủ file này khi mở project.

## 2. Chuẩn bị SQL Server

Trên cùng một instance SQL Server, mở các file ở thư mục gốc trong SSMS dưới dạng UTF-8 rồi lần lượt chạy:

1. `ElectricCarSalesDB.sql` tạo database nếu thiếu, bổ sung bảng dùng chung và nạp các mục danh mục VinFast còn thiếu.
2. `ElectricCarSalesFeatureMigration.sql` bổ sung bảng quản trị, phân quyền, khuyến mãi, nhật ký, cấu hình và trả góp để dùng báo cáo.

Hai file dùng được cho database mới và database đã có. Báo cáo trên database mới sẽ có số liệu khi nhóm tạo đơn và ghi nhận giao dịch. Khi chạy qua `sqlcmd`, dùng `-b -f 65001`.

## 3. Cấu hình kết nối

Từ thư mục gốc, chạy PowerShell:

```powershell
.\Setup-LocalConfig.ps1
```

Mở `%USERPROFILE%\.electriccarsales\db.properties` và điền thông tin của instance SQL Server trên máy bạn.

```properties
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=ElectricCarSalesDB;encrypt=true;trustServerCertificate=true
DB_USER=YOUR_SQL_LOGIN
DB_PASSWORD=YOUR_SQL_PASSWORD
```

Sửa cổng theo instance và kiểm tra SQL Server đã bật TCP/IP. SQL Server Express có thể dùng cổng khác 1433. URL trên dùng cho phát triển cục bộ với chứng chỉ tự ký.

Ứng dụng ưu tiên JVM property, rồi biến môi trường, rồi file trong thư mục người dùng. Có thể đặt trực tiếp `DB_URL`, `DB_USER`, `DB_PASSWORD` trong cấu hình chạy Tomcat. Maven loại file chứa thông tin kết nối ra khỏi WAR.

Nếu dùng Windows Authentication, thêm `integratedSecurity=true;authenticationScheme=NativeAuthentication` vào URL, bỏ `DB_USER` và `DB_PASSWORD`, rồi trỏ `-Djava.library.path` tới thư mục chứa DLL xác thực tương ứng với bản Microsoft JDBC Driver và kiến trúc JVM. Xem [hướng dẫn JDBC của Microsoft](https://learn.microsoft.com/en-us/sql/connect/jdbc/building-the-connection-url?view=sql-server-ver17#connecting-with-integrated-authentication-on-windows).

## 4. Tạo Admin đầu tiên

**Tự đặt ngay trên web:** chạy `docs/Chay-Web.ps1`, mở `http://localhost:8081/ElectricCarSales/admin.jsp`, chọn **Tạo Admin đầu tiên** và nhập mật khẩu hai lần. Sau đó đăng nhập bằng tài khoản vừa tạo. Màn hình chỉ được bật ở máy chủ cục bộ, yêu cầu CSRF, không còn dùng được khi có bất kỳ Admin nào và không trả mật khẩu/hash ra API. Không cần gửi mật khẩu trong chat.

Nếu chạy bằng cấu hình Tomcat của IntelliJ, thêm VM option `-Dapp.adminSetup.enabled=true` để bật màn hình này trên localhost khi DB chưa có Admin. Mặc định trong WAR là tắt. Khi đã tạo Admin, có thể bỏ option. Công cụ PowerShell bên dưới vẫn dùng được nếu bạn thích đặt mật khẩu trong Terminal.

**Tạo Admin bằng PowerShell**

Từ thư mục gốc của repository, chạy lệnh sau.

```powershell
.\docs\Tao-Admin.ps1
```

Script hỏi mật khẩu hai lần với nội dung được che; nhập 10–128 ký tự. Tên đăng nhập mặc định là `admin`; có thể đổi bằng `-Username ten_dang_nhap`. Script dùng lớp `BootstrapAdmin` từ bản WAR đã build, cấu hình DB cục bộ hoặc biến `DB_*` hiện có, và DLL Windows Authentication khi có trong `tool/jdbc-auth`. Mật khẩu chỉ được truyền tạm qua môi trường tiến trình Java, không được in hay ghi vào file; các biến môi trường trước đó được khôi phục sau khi chạy. Khi thấy `Created initial Admin: admin`, mở giao diện đăng nhập tại `http://localhost:8080/ElectricCarSales/admin.jsp` nếu chạy IntelliJ, hoặc `http://localhost:8081/ElectricCarSales/admin.jsp` nếu chạy script, và dùng mật khẩu vừa tự nhập. Không cần gửi mật khẩu trong chat.

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
3. Nếu danh sách đang có artifact `unnamed` chỉ chứa module output ở gốc, thiếu `WEB-INF/classes`, `WEB-INF/lib` và tài nguyên web, bỏ mục `unnamed` khỏi **Deployment**. Dùng artifact `ElectricCarSales:war exploded` do Maven import tạo để có Servlet, dependency, `web.xml`, `admin.jsp` và assets trong đúng cấu trúc WAR.
4. Trong **Before launch**, thêm hoặc chọn **Build Artifacts → ElectricCarSales:war exploded**; bỏ tác vụ build `unnamed` cũ. Giữ tác vụ **Build/Make** nếu đã có.
5. Đặt biến DB trong cấu hình chạy, bấm **Apply**, rồi **Run**. Nếu chưa thấy artifact `ElectricCarSales:war exploded`, trở lại cửa sổ **Maven**, bấm **Reload All Maven Projects** và chờ đồng bộ xong trước khi chọn lại.

**IntelliJ Community:** build WAR như trên, sao chép WAR vào thư mục `webapps` của Tomcat 10.1. Đặt JAVA_HOME vào JDK 24 và các biến DB trong môi trường chạy Tomcat; khởi động bằng `bin/catalina.bat run` trong terminal của bạn. Tránh chạy đồng thời hai Tomcat cùng cổng 8080. Không dùng Tomcat 9 vì dự án sử dụng Jakarta Servlet.

Mở [trang chủ](http://localhost:8080/ElectricCarSales/). Chọn **Trợ lý xe** để mở `customer/ai-assistant.jsp`. Chọn **Đăng nhập** để mở `login.jsp`. Quản trị viên sau đăng nhập tới `admin.jsp`.

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

Quản trị, khuyến mãi và báo cáo dùng chung schema với các phần khác của nhóm. Số liệu báo cáo phụ thuộc vào giao dịch đã ghi nhận.

## 9. Kiểm tra trước khi nộp

Ngày 17/10/2026, chạy `mvn clean verify` trên JDK 24: **92 kiểm thử đạt, 0 failures, 0 errors** (88 unit/HTTP và 4 kiểm thử WAR). Kiểm thử bao gồm nghiệp vụ/H2 chế độ MSSQLServer, HTTP chạy Tomcat 10.1 và triển khai chính file WAR. Kiểm thử WAR sử dụng web.xml và tự phát hiện Servlet/Filter annotations, xác nhận trang chủ/JSP/assets ở `/ElectricCarSales` và `/showroom`, đăng nhập, báo cáo, màn hình setup bị tắt mặc định và trợ lý xe. Có kiểm tra tạo Admin lần đầu/CSRF/rollback và nhãn dữ liệu DEMO trong CSV/XLSX. Cả ba bộ kiểm tra JavaScript trong `docs/FORMAT_CODE.md` đều đạt, gồm tìm/lọc/so sánh trên trang chủ và tải lại danh mục sau HTTP 503.

Với dữ liệu SQL Server tại máy chạy, đăng nhập Admin rồi mở báo cáo và đối chiếu bộ lọc kỳ với dữ liệu giao dịch. Mở Trợ lý xe để kiểm tra danh mục và gửi một câu hỏi thử.

Sau khi cấu hình SQL Server, kiểm tra:

1. Đăng nhập Admin; tạo nhân viên và vai trò mới; cấp VIEW_REPORTS cho vai trò, thử bằng tài khoản mới.
2. Khóa tài khoản đang đăng nhập ở trình duyệt khác; xác nhận yêu cầu tiếp theo bị từ chối. Giữ Admin hoạt động.
3. Tạo/sửa/ngừng khuyến mãi cho mẫu xe thật; xem nhật ký trước/sau.
4. Xem báo cáo khoảng ngày có dữ liệu, đối chiếu tiền/xe với dữ liệu nguồn; mở XLSX bằng Excel và lưu PDF.
5. Đổi REPORT_MAX_DAYS, kiểm tra khoảng vượt giới hạn bị từ chối; khôi phục giá trị mong muốn.
6. Hỏi thông số xe thật, so sánh hai mẫu, gợi ý theo ngân sách; yêu cầu thanh toán hoặc tạo hợp đồng phải bị từ chối.

Lỗi HTTP 404 tại `/ElectricCarSales/admin.jsp`: kiểm tra tab **Deployment** dùng `ElectricCarSales:war exploded`, context `/ElectricCarSales`, tác vụ **Before launch** build đúng artifact và log Tomcat báo triển khai thành công. Artifact `unnamed` chỉ chứa lớp Java ở gốc không có trang web hoặc cấu trúc WAR đầy đủ nên có thể khiến Tomcat khởi động nhưng URL vẫn trả 404.

Lỗi HTTP 503: kiểm tra DB_URL/DB_USER/DB_PASSWORD, TCP/cổng SQL Server và các bảng từ `ElectricCarSalesDB.sql` và `ElectricCarSalesFeatureMigration.sql`. Lỗi 401: đăng nhập hoặc kiểm tra ACTIVE/mật khẩu. Lỗi 403: quyền vai trò hoặc CSRF; tải lại trang để lấy phiên mới. Nếu cổng 8080 đã dùng, chọn cổng khác và thay URL trình duyệt tương ứng.
