# Báo cáo tích hợp Phúc Tấn

## A. Tổng quan

- Baseline là nhánh `fix` tại `4f3fd7f`. Source incoming là `ElectricCarSales-phuc-tan-fix-integration.zip`, SHA-256 `2C860A83E7F695155F00570C055BCF43185B2754493B5A0CB3A0812252E14339`, giải nén vào thư mục tạm ngoài repository.
- Đối chiếu 318 đường dẫn trước khi sửa, loại trừ metadata Git/IDE, thư mục backup và đầu ra build. Có 2 file trùng byte, 266 khác BOM hoặc xuống dòng, 24 file cùng tên khác nội dung, 4 chỉ có ở baseline và 22 chỉ có trong ZIP.
- Tích hợp sửa 28 file baseline, thêm 22 file mã nguồn/test và báo cáo này. Trong 296 mục ban đầu phía baseline, 268 mục nằm ngoài danh sách chỉnh sửa tích hợp. Bảy nhóm xung đột được xử lý hoặc ghi điều kiện ở bảng bên dưới.
- Git ban đầu đã có các xóa backup được stage, thay đổi đăng ký, lần chuyển JSP reset mật khẩu, hồ sơ khách hàng, script avatar và các file .class untracked. Đối chiếu lại byte của `RegisterController.java`, `auth/register.jsp`, `auth/reset-password.jsp` và `customer/my-profile.jsp` với bản sao đầu phiên đều trùng. Các mục xóa backup và chuyển JSP được stage trước phiên vẫn có trong index.
- Kiểm tra cuối quan sát thêm ba trạng thái xóa ở root (`ElectricCarSalesCatalogUpgrade.sql`, `Fix-ElectricCarSales-DB.ps1`, `InstallmentFinancePolicyMigration.sql`) và hai DOCX mới xuất hiện. Những đường dẫn đó nằm ngoài thao tác tích hợp.

## B. Ma trận tích hợp

| Module | Baseline và thay đổi trong ZIP | Kết quả trong workspace | Kiểm chứng |
| --- | --- | --- | --- |
| Admin và phân quyền trang | Dashboard HTML cũ có tổng quan, danh mục, quản trị, báo cáo và AI. ZIP có dashboard JSP, bộ lọc, modal, trạng thái tải và điều hướng mới. ZIP yêu cầu đổi redirect nhưng trang JSP ở ngoài `/admin/*`. | `admin.jsp` và bundle `admin-*`, `sales-reports.js`, `vehicle-catalogue.js` là dashboard chính. `PortalPageFilter` bảo vệ cả JSP và URL cũ bằng ADMIN đã xác thực. URL cũ chuyển tiếp hash/query, riêng `#ai` mở trang AI. Dashboard staff tiếp tục đi theo route hiện có. | Jasper và Node pass. Kiểm thử 302 khách, 403 SALES, 200 ADMIN đã có trong WAR IT, chưa chạy qua HTTP. |
| Authentication / Profile | ZIP nới kiểm tra mật khẩu, dùng vị trí JSP reset cũ và thiếu trang profile đang viết. B có modal bootstrap Admin trong dashboard. | Giữ validation đăng ký hiện có. `OtpController` forward tới JSP đã chuyển trong workspace qua cùng kiểm tra phiên OTP. Bootstrap đầu tiên nằm ở trang login, chỉ hiển thị khi máy chủ bật tính năng, truy cập loopback và endpoint xác nhận chưa có Admin. POST bootstrap vẫn dùng CSRF. Hồ sơ và avatar giữ route đang có. | Jasper pass cho login, reset, profile. Hai test bootstrap chạy trực tiếp pass. Chưa gửi OTP qua SMTP. |
| AI / danh mục khách | ZIP thêm UI chat, form tiêu chí, so sánh 2–4 xe, kết nối lại và reset. | Một `customer-ai.js` xử lý các form mới, dùng `/api/auth` lấy CSRF và `/api/ai` cho hội thoại. Trang AI dành cho khách vãng lai có header riêng, khách đăng nhập giữ customer shell. Chỉ đường dẫn AI được mở cho khách vãng lai. `AIDAO` lấy giá phiên bản và màu tham chiếu bằng JOIN tương thích H2, giữ điều kiện xe vật lý khả dụng. | Node pass cho chat, follow-up, CSRF, retry/reset và an toàn nội dung. Java HTTP/concurrency bị chặn. |
| Reporting | ZIP dùng chung bộ dò bảng tùy chọn, thêm dashboard, lọc kỳ, so sánh và export. | `ReportDAO` dò nhóm bảng installment một lần mỗi snapshot. Báo cáo dùng `Payments` hoàn tất cho tiền thực thu, `Orders` cho doanh số và tính công nợ riêng. `Contracts`, `Invoices`, `Deliveries` có UNIQUE `OrderID` trong SQL cơ sở, khoản thu được tổng hợp theo đơn. Bộ lọc và CSV/XLSX dùng chung API hiện có. | Test H2 pass cho 30/70, pending, canceled và tránh nhân tổng tiền khi một đơn có nhiều khoản. Node pass. Download thực tế chưa xác minh. |
| System Log, Roles, Config | SOURCE A có luồng ghi log trong app; SOURCE B bổ sung ghi vào bảng SystemLogs. SQL cơ sở chốt không có bảng audit tổng quát. | Khi bảng SystemLogs tồn tại ngoài schema cơ sở, log ghi theo transaction. Khi thiếu bảng, code ghi dấu sự kiện vào application logger, không giữ bản ghi giả trong bộ nhớ và không làm rollback thao tác nghiệp vụ. API JSON trả available=false cùng thông báo schema; dashboard hiển thị trạng thái chưa hỗ trợ và export CSV trả HTTP 503. Không ánh xạ Payments, OrderStatusHistory hay InventoryTransactions thành log quản trị vì chúng có nghiệp vụ riêng. | Đã bổ sung test H2 cho bootstrap không cần SystemLogs và phản hồi DAO unavailable; test Java biên dịch bằng javac nhưng chưa chạy. Node kiểm tra dashboard pass. Chưa kiểm thử HTTP API/CSV và logging thực tế của Tomcat. |
| Kết nối / WAR | ZIP đọc cấu hình ngoài WAR và thêm loại trừ SMTP. | `FinanceConnection` dùng JVM properties rồi biến môi trường trước file cấu hình, và hỗ trợ `~/.electriccarsales/db.properties` cùng cách đọc classpath có sẵn. `DBContext` dùng chung đường kết nối. WAR loại trừ `db.properties` và `smtp.properties`. | Compile/package pass. Danh sách nội dung WAR xác nhận loại trừ cả hai file. Chưa kết nối SQL Server thật. |
| Khuyến mãi / SQL | ZIP có dashboard ghi dữ liệu và migration gộp cả installment. File SQL người dùng chỉ định ở repository root có Promotions và PromotionCars; bản SQL nằm trong Maven project chưa có hai bảng này. | Dashboard gọi API hiện có. Lưu cần Promotions và PromotionCars; việc thiếu SystemLogs không chặn thao tác, nhưng audit DB không bền vững và được ghi vào application logger. Chi tiết xe hiển thị ưu đãi ACTIVE đúng mẫu và thời gian khi database được tạo từ SQL root. Giá, invoice, thanh toán tiếp tục dùng luồng hiện có. Không chạy migration. | H2 xác nhận lọc theo mẫu, thời hạn, trạng thái và BigDecimal. JSP chi tiết compile. Admin HTTP chưa xác minh. |

## C. Đóng góp sáu thành viên

| Thành viên | Luồng đối chiếu | Xác minh còn thiếu |
| --- | --- | --- |
| Đỗ Tấn Đạt | Payment, đặt cọc 30%, phần còn lại 70%, đối tác tài chính, công nợ và xác nhận. H2 đã kiểm tra cách báo cáo đọc khoản thu. | Thanh toán thật, callback, đối soát và chống xác nhận trùng trong môi trường chạy. |
| Võ Nguyễn Thái Dương | Login, register, OTP, reset, profile, avatar và hóa đơn. Login ADMIN đổi route; controller OTP đồng bộ vị trí view với lần chuyển file đang có. | OTP/SMTP, cập nhật avatar, đăng ký, login từng role, logout và hóa đơn qua giao diện. |
| Đặng Thị Hà Hoa | Danh mục, mẫu/phiên bản/màu/ảnh, chi tiết, so sánh và khuyến mãi. Chi tiết xe có vùng ưu đãi theo dữ liệu. | Search/filter/compare, ảnh và UX trên trình duyệt. |
| Trần Võ Anh Kiệt | Consultation, Order, OrderDetails, tình trạng đơn và lịch sử trạng thái giữ route/service baseline. | Tạo yêu cầu tư vấn và tạo/cập nhật đơn từ UI tới SQL Server. |
| Phan Trần Hiếu Nhân | Tồn kho, VIN, supplier, import/export, bàn giao, hợp đồng giữ luồng baseline. AI chỉ đọc danh mục và tình trạng có thể bán. | Import/export, kiểm kê, điều kiện bàn giao và hợp đồng trên SQL Server. |
| Phạm Trần Phúc Tấn | Dashboard, users, employees, roles, quyền, cấu hình, log, khuyến mãi, reports và AI đã nối vào ứng dụng. | Hồi quy HTTP, driver SQL Server, kiểm tra hiển thị và các chức năng đòi schema tùy chọn. |

## D. Thay đổi xuyên module

- Trang chủ vẫn dẫn tới login và AI. Login của CUSTOMER giữ đường về trang chủ. SALES, OPERATIONS, ACCOUNTANT và MANAGER tiếp tục dùng staff dashboard. Admin cũ có đường chuyển tiếp và dashboard mới dùng lại session, quyền và CSRF của ứng dụng.
- View AI dành cho CUSTOMER tái sử dụng menu hồ sơ/đơn hàng/thanh toán. Hành vi AI giới hạn ở catalogue, không mở quyền đến payment, contract hoặc công nợ.
- Đối chiếu mục thanh toán trong RDS và script cơ sở cho thấy `Orders.CompanyID` là tham chiếu đối tác tài chính, với 30% do khách đặt cọc và 70% còn lại do khách hoặc đối tác thanh toán. RDS cũng có đoạn cũ mô tả lịch góp. Không kích hoạt thu góp hàng tháng. Nếu hệ thống đã có bảng installment cũ, dữ liệu báo cáo tùy chọn của baseline vẫn hiển thị riêng, không cộng thêm vào tiền thực thu của website.

## E. Database

| Đối tượng | Trạng thái và quyết định |
| --- | --- |
| SQL được người dùng chỉ định ở repository root | Có 38 CREATE TABLE, gồm 35 bảng nghiệp vụ và VehicleTCO, Promotions, PromotionCars. Không có SystemLogs, RolePermissions hoặc SystemConfigurations. File không được thực thi trong lượt tích hợp. |
| SQL đặt trong Maven project | Có 35 CREATE TABLE; thiếu VehicleTCO, Promotions và PromotionCars so với file SQL root. Hai bản SQL đang không đồng nhất; cần triển khai theo đúng file SQL đã được nhóm chốt. |
| Promotions, PromotionCars, VehicleTCO | Đã có trong file SQL root người dùng chỉ định; chưa có trong bản SQL con của Maven project. Mã kiểm tra sự tồn tại bảng trước khi dùng. Không cần migration thêm nếu database đích được tạo theo SQL root. |
| SystemLogs | Không có trong SQL root hoặc bản SQL Maven. Chỉ hỗ trợ ghi DB nếu database có bảng này; code không tạo bảng hoặc tự chạy migration. Khi thiếu, API nhật ký báo unavailable; fallback application logger không phải audit DB bền vững và không giả lập kết quả truy vấn. |
| RolePermissions, SystemConfigurations | Không có trong SQL cơ sở; chức năng bền vững phụ thuộc migration riêng đã được review/phê duyệt. |
| VehicleTCO | Có trong SQL root người dùng chỉ định, chưa có trong SQL đặt ở Maven project. Không chạy migration ZIP. |
| InstallmentPlans/Contracts/Schedules/Payments/Settlements | Migration ZIP có thêm các bảng này, cột và ràng buộc tài chính. Cần review riêng vì có cấu trúc phục vụ lịch thu góp định kỳ. SQL cơ sở đã có Orders.CompanyID. |

Đã tham chiếu RDS cho nghiệp vụ và SDS cho quan hệ thiết kế. Fixture H2 chỉ tải định nghĩa bảng và fixture kiểm thử trong bộ nhớ. Không chạy SQL root, batch reset, trigger, demo hay migration trên SQL Server.

## F. Kiểm thử

| Kiểm thử | Kết quả |
| --- | --- |
| Kiểm thử Maven đã ghi nhận ở vòng tích hợp trước | Báo cáo vòng trước ghi nhận compile/package và các test H2/Jasper như mô tả bên dưới; các test HTTP bị giới hạn quyền loopback. |
| Điều chỉnh SystemLogs trong lượt hiện tại | Lệnh mvn -f ElectricCarSales/pom.xml clean verify chưa chạy được: máy hiện tại không có Maven hoặc Maven Wrapper trong PATH/project. Không tuyên bố Java build/test mới pass. |
| Node.js hiện tại | 3/3 bộ test JavaScript pass: admin-management, admin-report, customer-ai. |
| Kiểm tra hiện tại | git diff --check vẫn báo trailing whitespace ở file SQL gốc đã thay đổi trước lượt này; không tự format SQL để tránh làm nhiễu thay đổi sẵn có. |
| `mvn failsafe:integration-test failsafe:verify` trên WAR trước bước hiển thị ưu đãi | 7 lỗi môi trường cùng nguyên nhân loopback tại khởi tạo HTTP client. WAR đã được Tomcat nhúng nạp trước bước này. |
| Jasper `JspC -compile` | 0 lỗi cho `admin.jsp`, `customer/ai-assistant.jsp`, `auth/reset-password.jsp`, `customer/my-profile.jsp`, `index.jsp`, `login.jsp`, `vehicle-detail.jsp`. |
| Node.js và Git | 3/3 bộ test JavaScript pass. Kiểm tra cú pháp JS và `git diff --check` pass. |

Khi chạy mvn clean verify, đã sao lưu 604 file target hiện có vào thư mục temp trước lệnh; lệnh dừng ngay vì Maven không được cài/PATH, do đó không có thao tác clean nào xảy ra. Node test chạy trên workspace. Các luồng SQL Server, UI trình duyệt, CSRF qua HTTP, kiểm thử đủ role, SMTP, SePay và quy trình tài chính/tồn kho chưa được xác minh đầu cuối ở lượt này.

## G. Điều kiện còn mở

- Cần chạy đủ Surefire/Failsafe ở môi trường cho phép kết nối localhost. Xác minh bằng Tomcat + SQL Server và tài khoản thử nghiệm riêng cho từng vai trò theo ma trận ở trên.
- Với database chỉ có SQL cơ sở, nhật ký SystemLogs bền vững không khả dụng; application logger chỉ là fallback vận hành. Các thay đổi phân quyền/cấu hình, khuyến mãi và một số thao tác Admin còn cần các bảng tương ứng. Nhóm cần duyệt từng phần DDL; không áp dụng migration gộp.
- Chưa có kết quả test trình duyệt cho dashboard, mobile navigation, modal và trang AI trong layout khách hàng. Chưa xác minh CSV/XLSX qua HTTP.
- `Setup-LocalConfig.ps1` và SQL gộp trong ZIP không được nhập. Phần cấu hình chạy dùng chung trình đọc JDBC và `db.properties.example`. Phần đọc TCO hiện hữu vẫn phụ thuộc schema.

## H. Build và chạy

1. Dùng JDK 24, Maven và Tomcat 10.1. Từ repository chạy `mvn -f ElectricCarSales/pom.xml clean verify`. Chạy từng file `ElectricCarSales/src/test/javascript/*-checks.cjs` bằng Node.js.
2. Cấu hình kết nối tới database đã được chuẩn bị bằng `DB_URL`, `DB_USER`, `DB_PASSWORD` hoặc sao chép mẫu `ElectricCarSales/src/main/resources/db.properties.example` tới `~/.electriccarsales/db.properties` của tài khoản chạy Tomcat. Cấu hình email và cổng thanh toán qua các thiết lập máy chủ hiện dùng.
3. Deploy `ElectricCarSales/target/ElectricCarSales.war`, mở `/<context>/` rồi đăng nhập theo vai trò. Địa chỉ dashboard Admin là `/<context>/admin.jsp` và địa chỉ AI là `/<context>/customer/ai-assistant.jsp`.
4. Bootstrap Admin đầu tiên không còn yêu cầu bảng SystemLogs. Bật -Dapp.adminSetup.enabled=true trên JVM Tomcat, truy cập trang login từ loopback và dùng form Admin đầu tiên. Khi schema không có SystemLogs, sự kiện bootstrap chỉ được ghi vào application logger; lịch sử audit bền vững cần schema audit được phê duyệt.
