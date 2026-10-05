# Kết quả kiểm tra ngày 05/10/2026

## Nhánh riêng trên mã mới nhất của nhóm

Nhánh `feature/phuc-tan-admin-report-ai` được tạo từ `origin/main` tại commit `031ca5b`. Bổ sung mã/tài nguyên của Tấn, giữ nguyên các lớp Java/JSP/CSS/JS và SQL gốc của nhóm, giữ Maven `groupId` là `com.electriccarsales`. Trang gốc mở `index.jsp`; phần Tấn mở `tan.html`. Filter giới hạn trên năm nhóm URL của phần Tấn.

- JDK 24, Maven `formatter:format clean verify`: **BUILD SUCCESS**, **90 kiểm thử Java đạt, 0 failures, 0 errors**.
- Chạy `node ElectricCarSales/src/test/javascript/frontend-checks.cjs`: **21 kiểm tra đạt**.
- Kiểm thử WAR xác nhận trang gốc vẫn là trang chủ của nhóm, `tan.html` và các assets tải đúng; đăng nhập, quản trị, báo cáo và AI hoạt động với database H2 riêng.
- Kiểm tra API ngoài phạm vi Tấn không bị filter của Tấn áp dụng CSRF; kiểm tra phân quyền và CSRF trên API thuộc phần Tấn vẫn đạt.
- Formatter chỉ xử lý 85 file Java thuộc package `vn.edu.fpt.electriccarsales`; không format mã Java đã có của nhóm.

Các kết quả bên dưới được ghi nhận từ bản cục bộ trước khi tách nhánh. SQL Server thật và các thiết lập IntelliJ cục bộ cần được cấu hình lại trên máy clone nhánh.

## Kết quả bản cục bộ trước khi tách nhánh

Sau khi chuyển cấu trúc theo `Project.zip`:

Đã chỉnh thêm theo yêu cầu code dễ đọc cho sinh viên: quản trị dùng transaction trực tiếp, báo cáo và AI dùng vòng lặp/điều kiện rõ ràng, xuất Excel chuyển sang Apache POI. Tài liệu `GIAI_THICH_CODE.md` mô tả luồng xử lý để đọc và trình bày bài.

- JDK 24, Maven `clean verify`: **BUILD SUCCESS**.
- 87 unit/HTTP tests và 3 kiểm thử WAR: **90 đạt, 0 failures, 0 errors, 0 skipped**.
- 21 kiểm tra JavaScript: kỳ liền trước cùng số ngày (kể cả tháng nhuận), không chia cho 0, tìm tiếng Việt không dấu, giờ Việt Nam/hiệu lực khuyến mãi, quyền Tổng quan và mở báo cáo khi REPORT_MAX_DAYS chỉ bằng 1.
- Có thể chạy lại bằng `node ElectricCarSales/src/test/javascript/frontend-checks.cjs` khi đã có Node.js. Node chỉ dùng kiểm tra, không cần để chạy website.
- Kiểm thử WAR triển khai ứng dụng thực trên Tomcat 10.1: tự phát hiện Servlet/Filter, đọc web.xml, welcome page, JSP và assets; đăng nhập, phân quyền, đọc Users, cấu hình, báo cáo và AI.
- Kiểm tra JSON giữ tên trường sau khi chuyển record thành class JavaBean; kiểm tra danh sách được sao chép và validation không bị bỏ qua khi tạo bằng setter.
- Chạy `vn.edu.fpt.electriccarsales.App` bằng cấu hình cục bộ: kết nối thành công `ElectricCarSalesDB`.
- Script `docs/Tao-Admin.ps1` không có lỗi cú pháp, trỏ tới BootstrapAdmin ở package mới.
- Tạo Admin lần đầu: tùy chọn tắt mặc định, chỉ bật tại loopback, yêu cầu CSRF, mật khẩu do người dùng chọn được băm; từ chối mở lại sau khi có Admin; lỗi ghi log làm rollback việc tạo tài khoản.
- Lọc tài khoản kết hợp từ khóa/vai trò/trạng thái, phân trang 20 dòng, ID không hợp lệ trả 400, không đủ quyền trả 403; cập nhật trùng liên hệ rollback và trả 409.
- Đổi mật khẩu: xác thực mật khẩu hiện tại, kiểm tra mật khẩu mới/xác nhận, ACTIVE và CSRF; đổi session/CSRF, hết hiệu lực phiên khác và phiên trước khi Admin đặt lại mật khẩu; sai 5 lần bị giới hạn, lỗi nhật ký rollback.
- Trợ lý: nhận số người thành số chỗ tối thiểu, ưu tiên tiêu chí người dùng nhập, nhận dòng VF 9 và phiên bản cụ thể, không nhận nhầm VF 90; cuộc trò chuyện mới xóa ngữ cảnh riêng mà giữ đăng nhập và trình duyệt khác.
- Nhãn DEMO trong CSV/XLSX xét cả tồn kho hiện tại, khoản thanh toán/trả góp của đơn ngoài kỳ; không thay đổi công thức tổng tiền hoặc số xe.
- Hai script seed VinFast/DEMO đã nạp vào SQL Server và chạy lại: vẫn 10 mẫu/phiên bản, 30 xe, 9 đơn, 17 Payments, 10 ảnh; 22 AVAILABLE, 2 RESERVED, 6 SOLD.
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
- Kiểm tra bản chạy bằng nút Run của IntelliJ tại cổng **8080**: trang gốc, `tan.html`, `index.jsp`, CSS và 6 script đều trả HTTP 200. API AI đọc SQL Server thật, chế độ CATALOG_RULES, có 10 phiên bản xe.
- Xác nhận **Simple methods in one line** đã bỏ chọn. Tắt **Code Folding → One-line methods**, mở các hàm đã thu gọn trong `ConfigurationRequest.java`; `return value;` và setter hiển thị ở dòng riêng trong IntelliJ.
- WAR được kiểm tra có trang welcome/assets đầy đủ và không chứa `db.properties` riêng của máy.

Các kiểm thử tự động dùng H2 riêng. Bộ dữ liệu VinFast/DEMO được nạp vào database trình bày đang trống trước đó; toàn bộ giao dịch là giả lập, không phải doanh thu VinFast thật. Admin trên SQL Server vẫn cần người dùng tự chọn mật khẩu; kiểm thử đăng nhập/quản trị dùng tài khoản H2 riêng, chưa nhập thay mật khẩu của người dùng. Báo cáo PDF dùng chức năng In/Lưu PDF của trình duyệt; trợ lý dùng chế độ CATALOG_RULES.
