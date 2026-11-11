# BÁO CÁO ĐỐI CHIẾU RDS – SQL – CODE, CHANGELOG & HƯỚNG DẪN TÍCH HỢP THANH TOÁN THẬT (ELECTRICCARSALES)

## PHẦN 1: BẢNG ĐỐI CHIẾU NGHIỆP VỤ RDS – DATABASE (`ElectricCarSalesDB.sql`) – SOURCE CODE

Thứ tự ưu tiên đã áp dụng nghiêm ngặt: **Đúng RDS (`1. RDS Document.docx`) > Giữ nguyên tuyệt đối schema `ElectricCarSalesDB.sql` > Chức năng chính xác & bảo mật > Không làm hỏng phần nhóm đã làm > Sửa tối thiểu theo phong cách nhóm (Dương, Hoa)**.

| STT | Nhóm chức năng / Use Case RDS | Trạng thái ban đầu | Phân tích đối chiếu RDS – SQL – Code | Giải pháp đã hoàn thiện trong bản Patch |
|---|---|---|---|---|
| 1 | **UC-01, UC-02, UC-04**: Đăng ký, Xác thực OTP, Đăng nhập, Quên mật khẩu | **Chưa xong / Lỗi** | `RegisterController` lưu `pendingRegistrationUserId` vào Session nhưng `OtpController` đọc `request.getParameter("userId")` gây `NumberFormatException`. Chưa có nhánh `RESET_PASSWORD` và chưa có fallback khi máy dev chưa cấu hình SMTP. | Sửa `OtpController`, `auth/otp.jsp`, `login.js`, `OtpService`, `OTPVerificationDAO`: tự động lấy `userId` từ Session hoặc email, hỗ trợ gửi lại OTP (`resend`), hoàn thiện luồng `RESET_PASSWORD` và in mã OTP rõ ràng ra Console IntelliJ khi chưa cấu hình SMTP thật. |
| 2 | **UC-05, UC-06, UC-07, UC-28**: Danh mục xe, Tìm kiếm, So sánh xe, AI Tư vấn | **Đã có / Lỗi khi bảng `Cars` rỗng** | `ElectricCarSalesDB.sql` có sẵn dữ liệu `CarBrands`, `CarCategories`, `CarModels`, `CarVersions`, `ModelColors`, nhưng bảng `Cars` (xe vật lý trong kho) và `CarStatuses` chưa có dòng dữ liệu khởi tạo, khiến `AIDAO` và tạo đơn bị trống. Bảng `VehicleTCO` không tồn tại trong SQL. | Thêm cơ chế seed dữ liệu hợp lệ (`INSERT` chuẩn schema) trong `DBContext.ensureCoreSeedData()` cho `CarStatuses`, `FinancialCompanies`, 10 xe mẫu VinFast trong `Cars`, và 6 tài khoản mẫu; bọc `OBJECT_ID('dbo.VehicleTCO')` trong `VehicleTCODAO` và bổ sung fallback `CarVersions.CatalogPrice` trong `AIDAO`. |
| 3 | **UC-09, UC-16, UC-17 & Yêu cầu D (Popup Tư vấn / Chat)** | **Chưa xong / Mâu thuẫn yêu cầu** | **Cảnh báo RDS & SQL**: RDS không có Use Case chat 2 chiều thời gian thực (WebSocket) và `ElectricCarSalesDB.sql` không có bảng `ChatMessages` (chỉ có bảng `ConsultationRequests` với cột `RequestContent NVARCHAR(1000)`). Ngoài ra 4 file nhóm `Consultation*` khai báo sai package `.consultation`. | Không tự thêm bảng trái phép. Đã sửa đúng package, hoàn thiện `ConsultationController`/`Service`/`DAO` và xây dựng **Popup Tư vấn & Dự toán Trả thẳng/Trả góp có nút mở rộng toàn màn hình (`Expand Fullscreen`)** cho khách hàng (`customer-shell-end.jspf`), kết nối trực tiếp với màn hình phản hồi của Sales Staff qua `ConsultationRequests`. |
| 4 | **UC-18, UC-19, UC-10, UC-11**: Tạo đơn hàng, Khách xem đơn, Chọn phương thức trả thẳng/trả góp | **Chưa xong / Lỗi biên dịch** | 8 file `Order*` khai báo sai package `.order`. `OrderController` chỉ mới tạo mock trên bộ nhớ, chưa ghi `Orders`, `OrderDetails`, `OrderStatusHistory` và chưa giữ chỗ xe (`RESERVED`). | Sửa package cho toàn bộ `Order*`; hoàn thiện `OrderService` + `OrderDAO` lưu `Orders`, `OrderDetails`, tự động tính cọc 30% (`DepositAmount`) và 70% còn lại (`RemainingAmount`), khóa trạng thái xe `RESERVED`, ghi `OrderStatusHistory`, hỗ trợ khách hàng đổi phương thức `STRAIGHT` / `INSTALLMENT` + chọn `FinancialCompanies` trước khi ký hợp đồng. |
| 5 | **UC-12, UC-21 & Yêu cầu E**: Đặt cọc 30%, Thanh toán 70%, QR động OCB, Đối soát Kế toán | **Thiếu / Đang hardcode giả lập** | `customer/checkout.jsp` và `customer.js` đang hardcode thông tin ngân hàng giả (`MB Bank 0888686868`) và giả lập kết quả. Bảng `PaymentStatuses` trong SQL có chính tả `'FAILD'`. `Customers` không có cột CCCD. | Xóa toàn bộ giả lập. Xây dựng hệ thống thanh toán thật qua `PaymentController` + `PaymentService` + `PaymentDAO`: sinh **VietQR động OCB (`970448` – STK `0917231548` – `DO TAN DAT`)** + chuỗi chuẩn **EMVCo NAPAS 247 CRC-16**, lấy số tiền thật từ DB (30% cọc hoặc 70% còn lại), sinh mã tham chiếu duy nhất `ECS-ORD{OrderID}-{DEP|REM}-P{PaymentID}` (thay cho CCCD không có trong SQL), hỗ trợ VNPay HMAC-SHA512, Bank Webhook HMAC-SHA256, polling trạng thái thật (`/payment/check-status`) và màn hình Kế toán đối soát xác nhận/từ chối (`/payment/confirm`, `/payment/record-disbursement`). |
| 6 | **Mâu thuẫn nội bộ RDS về Trả góp (`Installment*`)** | **Mâu thuẫn RDS vs SQL** | Một số mục mô tả đầu tài liệu RDS nhắc tới `InstallmentPlans/Contracts/Schedules/Payments/Settlements`, nhưng **RDS UC-11, UC-12, Bảng 21–27, Mục II.5.1, II.6.1, II.7.1–7.3, II.8.1, II.9.1, III.7, EX-08, BR-05** và `ElectricCarSalesDB.sql` quy định rõ: hệ thống chỉ thu cọc 30%, lưu `Orders.CompanyID` (`FinancialCompanies`), còn 70% do ngân hàng giải ngân 1 lần và Kế toán xác nhận. | Tuân thủ đúng quy định chốt của RDS & `ElectricCarSalesDB.sql`: không tạo thêm các bảng `Installment*`. Bọc kiểm tra `OBJECT_ID` trong `InstallmentPlanDAO` / `InstallmentContractDAO` để không lỗi SQL, triển khai đầy đủ chọn công ty tài chính (`FinancialCompanies`), tính toán ước tính trả góp và luồng Kế toán ghi nhận giải ngân 70% (`/payment/record-disbursement`). |
| 7 | **Yêu cầu E.5: Chuyển khoản thử nghiệm 1/1.000 giá trị đơn** | **Cảnh báo mâu thuẫn RDS** | Chuyển 1/1.000 số tiền nhưng đánh dấu đơn đã thanh toán đủ **vi phạm trực tiếp RDS Mục II.6.1 (BR1, BR3), II.7.1 (BR1), II.7.2 (EX-1), II.9.3 (BR3)**. | Hỗ trợ tạo mã QR 1/1.000 (`testScale=1` khi bật `PAYMENT_TEST_MODE=true`) để test chuyển tiền thật trên app ngân hàng, nhưng **ghi nhận trung thực số tiền thực nhận** vào DB kèm cờ `[AMOUNT_MISMATCH]` ở trạng thái `PENDING` để Kế toán kiểm tra, tuyệt đối **không tự động đánh dấu đơn `DEPOSITED` hay `PAID`** khi số tiền thực nhận nhỏ hơn nghĩa vụ theo RDS. |
| 8 | **UC-13, UC-20**: Lập hợp đồng & Ký hợp đồng bằng OTP | **Chưa xong / Chặn nhầm quyền** | `OperationsPageFilter` chặn mọi truy cập `/contract` của `CUSTOMER` (HTTP 403). Chưa có bước gửi & xác thực OTP khi khách ký hợp đồng theo RDS Mục II.8.2. | Mở quyền cho `CUSTOMER` truy cập `/contract`, `/order`, `/invoice`, `/delivery`, `/consultation` của chính mình; hoàn thiện luồng Sales tạo hợp đồng sau khi đơn `DEPOSITED` và Khách hàng ký điện tử 2 bước qua OTP (`SIGN_CONTRACT` -> `SIGNED`). |
| 9 | **UC-14, UC-22**: Kế toán xuất hóa đơn & Khách xem hóa đơn | **Chưa xong** | Chưa kiểm tra chặt điều kiện hợp đồng `SIGNED` và `RemainingAmount = 0` trước khi xuất hóa đơn. | Hoàn thiện `InvoiceController`, `InvoiceService`, `InvoiceDAO` và `customer/my-invoices.jsp`: chỉ cho phép Kế toán xuất hóa đơn khi hợp đồng đã `SIGNED` và đơn đã thanh toán đủ 100% (`RemainingAmount = 0`). |
| 10 | **UC-15, UC-24, UC-25, UC-26**: Quản lý kho, Nhập xe, Xuất kho & Bàn giao xe | **Chưa xong** | Chưa đồng bộ trạng thái `Deliveries`, `InventoryTransactions` (`EXPORT`), `Cars` (`DELIVERED`) và `Orders` (`COMPLETED`). | Hoàn thiện `DeliveryController`, `DeliveryService`, `DeliveryDAO`: khi Operations Staff hoàn tất bàn giao (`DELIVERED`), hệ thống tự động ghi phiếu xuất kho `EXPORT`, chuyển trạng thái xe sang `DELIVERED` và đơn hàng sang `COMPLETED`. |
| 11 | **UC-27, UC-29, UC-30, UC-31**: Quản trị, Khuyến mãi, Báo cáo, Log | **Lỗi thiếu bảng SQL** | Các bảng `Promotions`, `PromotionCars`, `SystemLogs`, `SystemConfigurations`, `RolePermissions` không có trong `ElectricCarSalesDB.sql`. | Bổ sung kiểm tra `OBJECT_ID(...) IS NOT NULL` trong toàn bộ DAO liên quan (`PromotionDAO`, `PromotionCarDAO`, `SystemLogDAO`, `SystemConfigurationDAO`, `AdminRoleDAO`, `AdministrationDAO`, `ReportDAO`) để các màn hình Admin/Manager chạy mượt trên schema cố định mà không văng lỗi SQL. |

## PHẦN 2: NHẬT KÝ THAY ĐỔI CHI TIẾT (FILE-BY-FILE CHANGELOG)

### 2.1. Khối Cấu hình & Kết nối CSDL
1. **`src/main/java/util/DBContext.java`** (`package util`, class `DBContext`)
   - **Thay đổi**: Bổ sung `getConfig(String key, String defaultValue)` (đọc thứ tự: Biến môi trường hệ thống -> JVM System Property -> `db.properties`) và `ensureCoreSeedData(Connection conn)` tự động seed dữ liệu nền tảng hợp lệ (`Roles`, `OrderStatuses`, `ConsultationStatuses`, `PaymentMethods`, `PaymentTypes`, `PaymentStatuses`, `CarStatuses`, `FinancialCompanies`, 10 xe `Cars` mẫu và 6 tài khoản mẫu cho 6 Role) khi bảng trống.
   - **RDS liên quan**: Toàn bộ Use Case UC-01 đến UC-31.
2. **`src/main/java/com/electriccarsales/dao/FinanceConnection.java`** (`package com.electriccarsales.dao`, class `FinanceConnection`)
   - **Thay đổi**: Khi biến môi trường `ELECTRIC_CAR_SALES_JDBC_URL` chưa cấu hình, tự động gọi `DBContext.openConnection()` để dùng chung cấu hình SQL Server với toàn dự án.
3. **`src/main/resources/db.properties.example`**
   - **Thay đổi**: Thêm mẫu cấu hình SQL Server kèm các khóa cấu hình VietQR OCB (`OCB`, `970448`, `0917231548`, `DO TAN DAT`), `PAYMENT_TEST_MODE`, `BANK_WEBHOOK_SECRET`, `VNPAY_*`, `MOMO_*`.

### 2.2. Khối Xác thực, OTP & Tài khoản (UC-01 -> UC-04, UC-08)
4. **`src/main/java/com/electriccarsales/dao/OTPVerificationDAO.java`** & **`src/main/java/com/electriccarsales/service/OtpService.java`**
   - **Thay đổi**: Hỗ trợ đầy đủ các loại OTP (`REGISTER`, `RESET_PASSWORD`, `SIGN_CONTRACT`), tự động tìm `OTPTypeID` và `OTPStatusID`, vô hiệu hóa OTP cũ khi tạo mã mới, hỗ trợ fallback in mã OTP ra Console IntelliJ khi chưa cấu hình SMTP.
5. **`src/main/java/com/electriccarsales/controller/RegisterController.java`**, **`OtpController.java`**, **`LoginController.java`**
   - **Thay đổi**: Sửa lỗi mất `userId` khi xác thực OTP đăng ký, thêm action `resend`, thêm luồng quên mật khẩu (`forgot-password` -> OTP -> đặt lại mật khẩu BCrypt).
6. **`src/main/java/com/electriccarsales/controller/CustomerController.java`**
   - **Thay đổi**: Bổ sung hiển thị & tìm kiếm danh sách khách hàng cho Sales Staff / Manager / Admin và cập nhật hồ sơ cho Customer (UC-08).
7. **`src/main/java/com/electriccarsales/controller/OperationsPageFilter.java`** & **`PortalPageFilter.java`**
   - **Thay đổi**: Mở quyền cho `CUSTOMER` gọi `/contract`, `/order`, `/invoice`, `/delivery`, `/consultation` và tự động điều hướng khi truy cập trực tiếp URL `.jsp` sang Controller tương ứng để luôn nạp dữ liệu thật từ DB.

### 2.3. Khối Tư vấn, Đơn hàng & Trạng thái (UC-09, UC-10, UC-11, UC-16 -> UC-19)
8. **`src/main/java/com/electriccarsales/model/ConsultationRequest.java`**, **`ConsultationStatus.java`**, **`dao/ConsultationRequestDAO.java`**, **`dao/ConsultationStatusDAO.java`**, **`service/ConsultationService.java`**, **`controller/ConsultationController.java`**
   - **Thay đổi**: Sửa lỗi sai `package`, triển khai đầy đủ tạo yêu cầu tư vấn từ khách hàng, cập nhật trạng thái và ghi chú phản hồi từ Sales Staff.
9. **`src/main/java/com/electriccarsales/model/Order.java`**, **`OrderStatusHistory.java`**, **`Car.java`**, **`dao/OrderDAO.java`**, **`dao/OrderDetailDAO.java`**, **`dao/OrderStatusDAO.java`**, **`dao/OrderStatusHistoryDAO.java`**, **`dao/CarDAO.java`**, **`service/OrderService.java`**, **`service/OrderStatusService.java`**, **`controller/OrderController.java`**, **`controller/OrderStatusController.java`**
   - **Thay đổi**: Sửa lỗi sai `package`, hoàn thiện tạo đơn hàng thật trong SQL Server (`Orders` + `OrderDetails`), tính chính xác `DepositAmount = 30%`, `RemainingAmount = 70%`, giữ chỗ xe (`RESERVED`), chọn phương thức `STRAIGHT`/`INSTALLMENT` và công ty tài chính (`CompanyID`), ghi nhận lịch sử chuyển trạng thái (`OrderStatusHistory`).

### 2.4. Khối Thanh toán Thật, VietQR OCB, Webhook & Đối soát Kế toán (UC-12, UC-21 & Yêu cầu E)
10. **`src/main/java/com/electriccarsales/dao/PaymentDAO.java`**
    - **Thay đổi**: Thêm các hàm giao dịch ACID (`createOrReusePendingPayment`, `findByReferenceCode`, `markCustomerTransferred`, `confirmPaymentTransaction`, `recordBankDisbursementByAccountant`, `calculateOrderBalance`, `getPaymentsByCustomerId`). Tự động xử lý chính tả `'FAILD'` trong bảng `PaymentStatuses` của SQL gốc. Chặn ghi nhận trùng giao dịch (`Idempotency`) và kiểm tra số tiền thực nhận so với nghĩa vụ đơn hàng.
11. **`src/main/java/com/electriccarsales/service/PaymentService.java`**
    - **Thay đổi**:
      - Tạo phiên thanh toán VietQR động cho tài khoản **OCB (`970448`) – STK `0917231548` – Chủ TK `DO TAN DAT`**.
      - Sinh chuỗi **EMVCo NAPAS 247 QR Payload kèm mã kiểm tra CRC-16/CCITT-FALSE** và URL ảnh VietQR chuẩn.
      - Sinh mã tham chiếu duy nhất `ECS-ORD{OrderID}-{DEP|REM}-P{PaymentID}` lưu vào `Payments.ReferenceCode` và nội dung chuyển khoản `ECS ORD{OrderID} {COC30|TT70} P{PaymentID} {SDT}`.
      - Tích hợp tạo URL thanh toán & kiểm tra chữ ký **VNPay v2.1.0 (HMAC-SHA512)** và chữ ký **MoMo / Bank Webhook (HMAC-SHA256)**.
      - Hỗ trợ chế độ test chuyển khoản thật 1/1.000 (`testScale=1` khi `PAYMENT_TEST_MODE=true`) kèm cảnh báo và chặn tự động đánh dấu đơn đủ tiền khi số tiền thực nhận nhỏ hơn nghĩa vụ RDS.
12. **`src/main/java/com/electriccarsales/controller/PaymentController.java`**
    - **Thay đổi**: Triển khai đầy đủ các endpoint:
      - `GET /payment/checkout`: Tạo/lấy phiên thanh toán từ DB và mở giao diện QR động.
      - `GET /payment/check-status`: JSON API để trang thanh toán tự động kiểm tra trạng thái thật từ server mỗi 5 giây và tự động chuyển trang khi giao dịch đã được xác nhận `SUCCESS`.
      - `POST /payment/notify-transfer`: Khách hàng gửi mã giao dịch ngân hàng sau khi chuyển khoản -> lưu trạng thái `PENDING` chờ đối soát (tuyệt đối không tự lên `SUCCESS`).
      - `GET /payment/vnpay-checkout` & `GET /payment/vnpay-return`: Chuyển hướng và xác minh chữ ký trả về từ VNPay.
      - `POST /payment/webhook/bank`: Nhận Webhook tự động từ Casso/SePay/Ngân hàng, xác thực `BANK_WEBHOOK_SECRET`, bóc tách mã `ECS-ORD...`, kiểm tra số tiền thực nhận và tự động cập nhật `SUCCESS` nếu khớp 100% nghĩa vụ hoặc gắn cờ `[AMOUNT_MISMATCH]` nếu chuyển thiếu (ví dụ chuyển 1/1.000).
      - `POST /payment/confirm` & `POST /payment/record-disbursement`: Kế toán đối soát sao kê ngân hàng và xác nhận/từ chối giao dịch hoặc ghi nhận giải ngân 70% từ ngân hàng đối tác.

### 2.5. Khối Hợp đồng, Hóa đơn, Kho & Bàn giao, Báo cáo/Admin (UC-13 -> UC-15, UC-20, UC-22 -> UC-31)
13. **`src/main/java/com/electriccarsales/model/Contract.java`**, **`dao/ContractsDAO.java`**, **`service/ContractsService.java`**, **`controller/ContractController.java`**
    - **Thay đổi**: Hoàn thiện lập hợp đồng (khi đơn đã `DEPOSITED`) và ký hợp đồng bằng OTP (`request-sign-otp` -> `sign-with-otp`).
14. **`src/main/java/com/electriccarsales/dao/InvoiceDAO.java`**, **`service/InvoiceService.java`**, **`controller/InvoiceController.java`**
    - **Thay đổi**: Hoàn thiện xuất hóa đơn (`ISSUED`) khi hợp đồng đã `SIGNED` và `RemainingAmount = 0`.
15. **`src/main/java/com/electriccarsales/model/Delivery.java`**, **`dao/DeliveryDAO.java`**, **`service/DeliveryService.java`**, **`controller/DeliveryController.java`**, **`controller/InstallmentController.java`**
    - **Thay đổi**: Hoàn thiện nhập kho (`IMPORT`), xuất kho (`EXPORT`), cập nhật bàn giao xe (`DELIVERED`) và đóng đơn hàng (`COMPLETED`).
16. **`src/main/java/com/electriccarsales/dao/FinancialCompanyDAO.java`**, **`AIDAO.java`**, **`PromotionDAO.java`**, **`PromotionCarDAO.java`**, **`VehicleTCODAO.java`**, **`SystemConfigurationDAO.java`**, **`SystemLogDAO.java`**, **`AdminRoleDAO.java`**, **`AdministrationDAO.java`**, **`ReportDAO.java`**, **`InstallmentPlanDAO.java`**, **`InstallmentContractDAO.java`**
    - **Thay đổi**: Đồng bộ câu lệnh SQL với đúng các bảng/cột có thật trong `ElectricCarSalesDB.sql`.

### 2.6. Khối Giao diện JSP & JavaScript
17. **`src/main/webapp/customer/checkout.jsp`**: Giao diện thanh toán VietQR động OCB (`0917231548` - `DO TAN DAT`), hiển thị số tiền thật từ DB (Cọc 30% hoặc Còn lại 70%), nút bật/tắt chế độ kiểm thử 1/1.000 (kèm cảnh báo RDS), tự động polling `/payment/check-status` mỗi 5 giây, form báo đã chuyển khoản (`PENDING`), và nút thanh toán VNPay.
18. **`src/main/webapp/customer/my-orders.jsp`**, **`my-payments.jsp`**, **`my-contracts.jsp`**, **`my-invoices.jsp`**, **`my-account.jsp`**: Hiển thị dữ liệu thật từ SQL Server cho khách hàng, chọn phương thức trả thẳng/trả góp, ký hợp đồng bằng OTP, theo dõi lịch sử giao dịch và hóa đơn.
19. **`src/main/webapp/common/customer-shell.jspf`**, **`customer-shell-end.jspf`**, **`assets/js/customer.js`**: Bổ sung **Popup Tư vấn & Dự toán Trả thẳng/Trả góp có nút mở rộng toàn màn hình (`Mở rộng toàn màn hình`)**, bỏ toàn bộ dữ liệu mock trong `customer.js`.
20. **`src/main/webapp/assets/js/staff.js`**, **`auth/otp.jsp`**, **`assets/js/login.js`**, **`index.jsp`**, **`vehicle-detail.jsp`**: Kết nối bảng điều khiển của Sales Staff, Accountant (xác nhận cọc 30%, xác nhận 70%, ghi nhận giải ngân, xuất hóa đơn) và Operations Staff (nhập kho, bàn giao xe) với các endpoint thật trên server.

## PHẦN 3: HƯỚNG DẪN CẤU HÌNH, CHẠY VÀ KIỂM THỬ TRONG INTELLIJ IDEA

### Bước 1: Khởi tạo Database trên SQL Server
1. Mở **SQL Server Management Studio (SSMS)**.
2. Mở file `ElectricCarSalesDB.sql` (giữ nguyên 100% file gốc) và nhấn **Execute (F5)** để tạo database `ElectricCarSalesDB`.
3. Khi ứng dụng khởi động lần đầu, `DBContext.ensureCoreSeedData()` sẽ tự động `INSERT` các dữ liệu danh mục còn thiếu (`CarStatuses`, `FinancialCompanies`, 10 xe mẫu VinFast trong `Cars`, và 6 tài khoản mẫu) mà không sửa bất kỳ cấu trúc bảng nào.

### Bước 2: Giải nén bản vá & Cấu hình trong IntelliJ
1. Giải nén file `ElectricCarSales_Patch_Completed.zip` đè trực tiếp vào thư mục gốc của project `ElectricCarSales` (hoặc copy thư mục `src` trong ZIP đè vào `src` của project).
2. Mở project trong **IntelliJ IDEA Ultimate**, chọn **JDK 17+**.
3. Cập nhật thông tin kết nối SQL Server trong `src/main/resources/db.properties` (hoặc copy từ `db.properties.example`):
   ```properties
   db.url=jdbc:sqlserver://localhost:1433;databaseName=ElectricCarSalesDB;encrypt=true;trustServerCertificate=true
   db.username=sa
   db.password=MatKhauSQLCuaBan

   # Cấu hình tài khoản nhận tiền thật VietQR OCB
   VIETQR_BANK_ID=OCB
   VIETQR_BANK_BIN=970448
   VIETQR_ACCOUNT_NO=0917231548
   VIETQR_ACCOUNT_NAME=DO TAN DAT

   # Cho phép hiển thị tùy chọn test chuyển khoản 1/1.000 (vẫn giữ nguyên quy tắc kế toán RDS)
   PAYMENT_TEST_MODE=true
   ```
4. Cấu hình **Tomcat Server 10.1.x (Local)** trong IntelliJ -> Deploy artifact `ElectricCarSales:war exploded` -> Nhấn **Run**.

### Bước 3: Danh sách tài khoản mẫu (Tự động khởi tạo sẵn)
Mật khẩu chung cho cả 6 tài khoản mẫu: **`Password@123`**
- **Customer**: `customer1`
- **Sales Staff**: `sales1`
- **Accountant (Kế toán)**: `accountant1`
- **Operations Staff (Kho & Bàn giao)**: `ops1`
- **Manager**: `manager1`
- **Admin**: `admin`

### Bước 4: Kịch bản kiểm thử End-to-End theo đúng quy trình RDS
1. **Tư vấn & Dự toán**: Đăng nhập `customer1` -> Mở **Popup Tư vấn & Dự toán** (góc phải dưới) -> Nhấn **Mở rộng toàn màn hình** -> Gửi yêu cầu tư vấn -> Đăng nhập `sales1` vào `/consultation` phản hồi.
2. **Tạo đơn hàng**: `sales1` vào `/order` -> Tạo đơn cho `customer1` -> Hệ thống tự động tính `DepositAmount = 30%`, `RemainingAmount = 70%`, khóa xe `RESERVED`.
3. **Đặt cọc 30% bằng VietQR OCB**: `customer1` vào `/order` -> Nhấn **Đặt cọc 30%** -> Hệ thống mở `/payment/checkout` với mã QR động của **OCB – STK `0917231548` – `DO TAN DAT`**, đúng số tiền 30% và nội dung `ECS ORD{OrderID} COC30 P{PaymentID} ...`.
4. **Kế toán xác nhận cọc (hoặc Webhook tự động)**: `accountant1` vào `/payment/manage` xác nhận phiếu thu -> Đơn chuyển sang `DEPOSITED`, màn hình khách hàng tự động chuyển sang hoàn tất.
5. **Hợp đồng & Ký OTP**: `sales1` lập hợp đồng tại `/contract` -> `customer1` vào `/contract` nhận mã OTP (xem trong Console IntelliJ nếu chưa cấu hình SMTP) và ký điện tử -> Hợp đồng chuyển sang `SIGNED`.
6. **Thanh toán 70% còn lại / Giải ngân ngân hàng**:
   - Đơn trả thẳng (`STRAIGHT`): `customer1` quét VietQR OCB 70% còn lại -> `accountant1` (hoặc Webhook) xác nhận.
   - Đơn trả góp (`INSTALLMENT`): `accountant1` vào `/payment/manage` ghi nhận giải ngân 70% từ công ty tài chính (`/payment/record-disbursement`).
   - Đơn hàng chuyển sang `PAID` (`RemainingAmount = 0`).
7. **Xuất hóa đơn & Bàn giao xe**: `accountant1` vào `/invoice` xuất hóa đơn (`ISSUED`) -> `ops1` vào `/delivery` hoàn tất bàn giao (`DELIVERED`) -> Đơn hàng chuyển sang `COMPLETED`, kho ghi phiếu `EXPORT`.

## PHẦN 4: GIAI ĐOẠN 2 – HƯỚNG DẪN KẾT NỐI THANH TOÁN THẬT TỪNG BƯỚC (OCB VIETQR, WEBHOOK, VNPAY, MOMO)

### 1. Chuyển khoản ngân hàng VietQR OCB (`0917231548` – `DO TAN DAT`)
- Đã hoạt động thực tế 100% qua chuẩn **EMVCo NAPAS 247 CRC-16** (BIN OCB `970448`, STK `0917231548`, Chủ TK `DO TAN DAT`). Khi quét bằng bất kỳ ứng dụng ngân hàng nào tại Việt Nam, app ngân hàng đều tự động điền chính xác người nhận `DO TAN DAT`, số tiền của đơn hàng và nội dung chuyển khoản chứa mã tham chiếu `ECS ORD...`.

### 2. Tự động hóa xác nhận giao dịch OCB 24/7 qua Webhook (SePay / Casso)
Để hệ thống tự động chuyển `SUCCESS` ngay khi tài khoản OCB `0917231548` nhận được tiền thật:
1. Đăng ký tài khoản tại **SePay.vn** hoặc **Casso.vn** và liên kết tài khoản **OCB `0917231548` (`DO TAN DAT`)**.
2. Chạy `ngrok http 8080` trên máy chạy IntelliJ để lấy địa chỉ HTTPS public.
3. Cấu hình Webhook URL trên SePay/Casso trỏ về:
   `https://<domain-ngrok>/payment/webhook/bank`
4. Khai báo `BANK_WEBHOOK_SECRET` trong `db.properties` trùng với Secret Header trên SePay/Casso.
5. Khi khách chuyển khoản đúng số tiền và nội dung `ECS ORD...`, `POST /payment/webhook/bank` sẽ tự động xác thực chữ ký/secret, đối chiếu số tiền thực nhận, chống trùng giao dịch (`bankTransactionId`) và cập nhật `Payments` -> `SUCCESS` + `Orders` -> `DEPOSITED`/`PAID`.

### 3. Kết nối cổng thanh toán VNPay (Thẻ ATM NAPAS / Visa / Mastercard)
1. Đăng ký tài khoản tại `https://sandbox.vnpayment.vn/devreg/` (hoặc VNPay Merchant thật).
2. Điền `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_PAY_URL`, `VNPAY_RETURN_URL` vào `db.properties`.
3. `PaymentService` đã tích hợp sẵn thuật toán ký số `HMAC-SHA512` chuẩn VNPay v2.1.0 tại `/payment/vnpay-checkout` và `/payment/vnpay-return`.

### 4. Kiểm thử chuyển tiền thật bằng 1/1.000 giá trị đơn hàng
- Khi bật `PAYMENT_TEST_MODE=true`, khách hàng có thể chọn **"Chế độ kiểm thử 1/1.000"** trên trang `/payment/checkout` để tạo mã QR OCB với số tiền bằng `1/1.000` nghĩa vụ (ví dụ `45.353.000 đ` -> `45.353 đ`).
- **Bảo vệ quy tắc kế toán RDS**: Hệ thống ghi nhận trung thực số tiền thực nhận (`45.353 đ`) kèm nhãn `[AMOUNT_MISMATCH]` ở trạng thái `PENDING`, tuyệt đối không tự động đánh dấu đơn hàng đã thanh toán đủ khi số tiền thực nhận nhỏ hơn nghĩa vụ trong RDS.
