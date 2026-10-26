# Kết quả kiểm tra

## Cập nhật ngày 17/10/2026: đồng bộ giao diện VinFast xanh dương theo main

- Trang chủ `index.jsp` dùng bố cục VinFast xanh dương theo `origin/main` tại commit `69deda1`; `preview-main.jsp` forward tới trang chủ. Trang chủ và không gian làm việc `tan.jsp` dùng chung màu, kiểu chữ và biến CSS trong `assets/css/theme.css`. Liên kết đăng nhập đi tới `tan.jsp?login=1`.
- Chạy `mvn clean verify` trên JDK 24: **BUILD SUCCESS**, 88 kiểm thử unit/HTTP và 4 kiểm thử `PackagedWarIT`, tổng **92 đạt, 0 failures, 0 errors**. Kiểm thử WAR xác nhận trang chủ, đường dẫn xem thử, không gian làm việc và assets ở cả `/ElectricCarSales` và `/showroom`, cùng đăng nhập, API và xuất báo cáo.
- Cả ba bộ kiểm tra JavaScript `frontend-checks.cjs`, `admin-report-checks.cjs` và `homepage-checks.cjs` đều đạt. Kiểm tra trang chủ bao gồm tìm kiếm, kết hợp bộ lọc, sắp xếp, đặt lại, so sánh tối đa 3 xe, context triển khai, menu di động và tải lại sau HTTP 503 để hiển thị đủ 10 phiên bản xe.
- Khởi động bằng cấu hình native **ElectricCarSales - Tomcat 10.1** trong IntelliJ: Tomcat **10.1.60**, cổng **8080**, dùng Catalina base do JetBrains quản lý. Trang gốc, `index.jsp`, `preview-main.jsp`, `tan.jsp`, `tan.jsp?login=1` và các assets theme/home/tan đều trả **HTTP 200**.
- API trợ lý đọc SQL Server cục bộ, trả **10 phiên bản xe** trong chế độ **CATALOG_RULES**. Đã xem giao diện desktop của trang chủ, không gian làm việc với sidebar navy và form đăng nhập xanh dương; không gửi form đăng nhập trên SQL Server.

Các mục bên dưới giữ lại kết quả của các lần kiểm tra trước đó; mô tả welcome forward trong lần chuyển JSP đã được thay bằng trang chủ ở bản hiện tại.

## Cập nhật ngày 17/10/2026: JSP và sườn chung của nhóm

- Chuyển toàn bộ Java về `com.electriccarsales` với bốn package `controller`, `dao`, `model`, `service`; khôi phục `Main.java` và `util.DBContext` dùng chung. Đổi các lớp Repository thành DAO và cập nhật import/test.
- Đổi `tan.html` thành `tan.jsp`, welcome `index.jsp` forward tới JSP. JSP dùng UTF-8/JSTL và cung cấp context cho các link API/assets/CSV/XLSX.
- Chạy `formatter:format clean verify formatter:validate`: **BUILD SUCCESS**, 87 kiểm thử unit/HTTP và 4 kiểm thử WAR, tổng **91 đạt, 0 failures, 0 errors, 0 skipped**. Format 84 file Java hợp lệ.
- Kiểm thử Tomcat biên dịch JSP thực ở context gốc, `/ElectricCarSales` và `/showroom`, kiểm tra welcome, trang trực tiếp, assets, đăng nhập, danh mục và xuất CSV/XLSX dưới context đổi tên.
- `node ElectricCarSales/src/test/javascript/frontend-checks.cjs` đạt; kiểm tra thêm URL theo context gốc, context đổi tên và trang lồng thư mục. Các file JavaScript đã qua `node --check`; ba script PowerShell không có lỗi cú pháp.
- Script chạy dùng `JAVA_HOME`/`CATALINA_HOME` hoặc tham số đường dẫn trên từng máy; script tạo Admin dùng FQCN mới. Kiểm thử lần này sử dụng H2, không sửa dữ liệu SQL Server của nhóm.

Các mục bên dưới ghi lại những lần kiểm tra trước đó.

## Cập nhật ngày 08/10/2026: một file SQL chung

- `ElectricCarSalesDB.sql` chứa đủ 46 bảng, danh mục VinFast và dữ liệu báo cáo DEMO; không cần thư mục `database` hay các script SQL riêng.
- Maven `verify` đạt 90 kiểm thử Java ở cả bản cục bộ và nhánh riêng; 21 kiểm tra JavaScript đạt ở bản cục bộ.
- File SQL chung chạy hai lần trên SQL Server cục bộ: số dòng các bảng đang có giữ nguyên, không thêm trùng.
- Đã kiểm tra nhánh bỏ qua dữ liệu báo cáo DEMO khi có giao dịch ngoài bộ DEMO: không phát sinh giao dịch hay rò rỉ transaction. Bản ghi kiểm tra được rollback.
- AIService/ReportService đọc được 10 phiên bản xe, 9 đơn DEMO và 6 xe bán từ SQL Server. Các số liệu này là dữ liệu mô phỏng.

Sau khi chuyển cấu trúc theo `Project.zip`:

Đã chỉnh thêm theo yêu cầu code dễ đọc cho sinh viên: quản trị dùng transaction trực tiếp, báo cáo và AI dùng vòng lặp/điều kiện rõ ràng, xuất Excel chuyển sang Apache POI. Tài liệu `GIAI_THICH_CODE.md` mô tả luồng xử lý để đọc và trình bày bài.

- JDK 24, Maven `clean verify`: **BUILD SUCCESS**.
- 87 unit/HTTP tests và 3 kiểm thử WAR: **90 đạt, 0 failures, 0 errors, 0 skipped**.
- 21 kiểm tra JavaScript: kỳ liền trước cùng số ngày (kể cả tháng nhuận), không chia cho 0, tìm tiếng Việt không dấu, giờ Việt Nam/hiệu lực khuyến mãi, quyền Tổng quan và mở báo cáo khi REPORT_MAX_DAYS chỉ bằng 1.
- Có thể chạy lại bằng `node ElectricCarSales/src/test/javascript/frontend-checks.cjs` khi đã có Node.js. Node chỉ dùng kiểm tra, không cần để chạy website.
- Kiểm thử WAR triển khai ứng dụng thực trên Tomcat 10.1: tự phát hiện Servlet/Filter, đọc web.xml, welcome page, JSP và assets; đăng nhập, phân quyền, đọc Users, cấu hình, báo cáo và AI.
- Kiểm tra JSON giữ tên trường sau khi chuyển record thành class JavaBean; kiểm tra danh sách được sao chép và validation không bị bỏ qua khi tạo bằng setter.
- Chạy `com.electriccarsales.Main` bằng cấu hình cục bộ: kết nối thành công `ElectricCarSalesDB`.
- Script `docs/Tao-Admin.ps1` không có lỗi cú pháp, trỏ tới BootstrapAdmin ở package mới.
- Tạo Admin lần đầu: tùy chọn tắt mặc định, chỉ bật tại loopback, yêu cầu CSRF, mật khẩu do người dùng chọn được băm; từ chối mở lại sau khi có Admin; lỗi ghi log làm rollback việc tạo tài khoản.
- Lọc tài khoản kết hợp từ khóa/vai trò/trạng thái, phân trang 20 dòng, ID không hợp lệ trả 400, không đủ quyền trả 403; cập nhật trùng liên hệ rollback và trả 409.
- Đổi mật khẩu: xác thực mật khẩu hiện tại, kiểm tra mật khẩu mới/xác nhận, ACTIVE và CSRF; đổi session/CSRF, hết hiệu lực phiên khác và phiên trước khi Admin đặt lại mật khẩu; sai 5 lần bị giới hạn, lỗi nhật ký rollback.
- Trợ lý: nhận số người thành số chỗ tối thiểu, ưu tiên tiêu chí người dùng nhập, nhận dòng VF 9 và phiên bản cụ thể, không nhận nhầm VF 90; cuộc trò chuyện mới xóa ngữ cảnh riêng mà giữ đăng nhập và trình duyệt khác.
- Nhãn DEMO trong CSV/XLSX xét cả tồn kho hiện tại, khoản thanh toán/trả góp của đơn ngoài kỳ; không thay đổi công thức tổng tiền hoặc số xe.
- Dữ liệu VinFast/DEMO đã nạp vào SQL Server và chạy lại: vẫn 10 mẫu/phiên bản, 30 xe, 9 đơn, 17 Payments, 10 ảnh; 22 AVAILABLE, 2 RESERVED, 6 SOLD.
- ReportService đọc SQL Server thật cho kỳ 01/05/2026–05/10/2026: 6 xe bán, giá trị bán **4.413.000.000 VND**, tiền thực thu **4.565.900.000 VND**, còn phải thu **1.376.100.000 VND**, 14 thanh toán hoàn tất và 0 đơn không nhất quán. Thu kỳ hạn trả góp được trình bày riêng.
- Xuất báo cáo DEMO từ số liệu SQL Server, đọc lại XLSX bằng Apache POI: đủ 5 sheet, ô số và nhãn dữ liệu mô phỏng; CSV có nhãn ở đầu file.
- Chạy/dừng/khởi động lại Tomcat 10.1.60 bằng `Chay-Web.ps1`/`Dung-Web.ps1` tại cổng 8081; root, JSP, assets và API catalogue trả HTTP 200. Kiểm tra sẵn sàng dùng địa chỉ 127.0.0.1 để tránh chậm phân giải localhost trên máy này.
- Thử giao diện trên trình duyệt: danh mục 10 phiên bản, ảnh VinFast, lọc xe 5 chỗ dưới 700 triệu trả 3 mẫu; so sánh VF 6 Eco/Plus có nguồn và NEDC; chuyển xe đã chọn sang trợ lý trả đúng hai mẫu. API trợ lý từ chối yêu cầu thanh toán.
- Thử bản cập nhật trên Tomcat + SQL Server: hỏi “gia đình 6 người, ngân sách 1,6 tỷ” trả VF 9 Eco/Plus với 7 chỗ và WLTP; hỏi tiếp dùng đúng ngữ cảnh, bắt đầu cuộc trò chuyện mới rồi hỏi “xe đó” không dùng lại mẫu trước đó.
- WAR chia sẻ loại `db.properties`; ZIP không chứa cấu hình DB riêng, `.env`, `.idea`, lịch sử Git hoặc tài nguyên từ dự án cafe.

Sau khi format toàn bộ project:

- 85 file Java được format; `formatter:validate` xác nhận cả 85 file đúng cấu hình và không cần sửa thêm.
- 10 file giao diện/kiểm tra JavaScript và 9 file Markdown được kiểm tra bằng Prettier; XML, 5 file SQL và 3 script PowerShell cũng đã được trình bày lại.
- Java và JavaScript giữ nguyên cây cú pháp sau format, ngoại trừ việc thêm khối `{ }` quanh các câu lệnh ngắn. Giá trị chuỗi được giữ nguyên.
- SQL giữ nguyên token thực thi và chuỗi dữ liệu; chỉ chuẩn hóa khoảng trắng của chú thích. PowerShell không có lỗi cú pháp và giữ nguyên 1.374 token.
- Chạy lại `mvn clean verify formatter:validate`: **BUILD SUCCESS**, **90 kiểm thử đạt, 0 failures, 0 errors, 0 skipped**. Chạy lại **21 kiểm tra JavaScript**, tất cả đạt.
- Có `.editorconfig`, `.prettierrc.json` và `ElectricCarSales/config/java-formatter.xml` để dùng tiếp; xem `FORMAT_CODE.md`.
- Kiểm tra đúng mẫu getter/setter người dùng cung cấp: đọc cây cú pháp của 85 file Java, có 514 câu lệnh `return` và 521 getter/setter trong mã chính. Toàn bộ `return` bắt đầu ở dòng riêng; thân getter/setter và dấu `}` được xuống dòng. Không có lỗi cú pháp. Cấu hình format đã bổ sung quy tắc giữ getter/setter trên nhiều dòng.

Sửa lỗi chạy trong IntelliJ ngày 05/10/2026:

- POM hiện tại thiếu các thư viện của mã nguồn hoàn chỉnh. Đã khôi phục dependency Jackson, Apache POI, driver SQL Server 13.4.0 và cấu hình compiler/kiểm thử/WAR từ bản đã kiểm tra; lưu riêng POM trước khi sửa trong công cụ cục bộ.
- Chạy lại `mvn clean verify`: **BUILD SUCCESS**, **90 kiểm thử đạt, 0 lỗi**; **21 kiểm tra JavaScript đạt**.
- Đồng bộ Maven trong IntelliJ, đặt **Project SDK = 24** để hết thông báo **Project JDK is not defined**. Chạy cấu hình **Tomcat 10.1.60**, **JRE 24**, deploy **ElectricCarSales:war exploded**, context **/ElectricCarSales**.
- Kiểm tra bản chạy bằng nút Run của IntelliJ tại cổng **8080**: trang gốc, `tan.jsp`, `index.jsp`, CSS và 6 script đều trả HTTP 200. API AI đọc SQL Server thật, chế độ CATALOG_RULES, có 10 phiên bản xe.
- Xác nhận **Simple methods in one line** đã bỏ chọn. Tắt **Code Folding → One-line methods**, mở các hàm đã thu gọn trong `ConfigurationRequest.java`; `return value;` và setter hiển thị ở dòng riêng trong IntelliJ.
- WAR được kiểm tra có trang welcome/assets đầy đủ và không chứa `db.properties` riêng của máy.

Các kiểm thử tự động dùng H2 riêng. Bộ dữ liệu VinFast/DEMO được nạp vào database trình bày đang trống trước đó; toàn bộ giao dịch là giả lập, không phải doanh thu VinFast thật. Admin trên SQL Server vẫn cần người dùng tự chọn mật khẩu; kiểm thử đăng nhập/quản trị dùng tài khoản H2 riêng, chưa nhập thay mật khẩu của người dùng. Báo cáo PDF dùng chức năng In/Lưu PDF của trình duyệt; trợ lý dùng chế độ CATALOG_RULES.
