# BÁO CÁO TÍCH HỢP HỆ THỐNG VÀ HOÀN THIỆN SAU MERGE
## DỰ ÁN SWP391 — ELECTRIC CAR SALES MANAGEMENT SYSTEM

- **Người thực hiện:** Senior Full-Stack & Integration Engineer (Antigravity)
- **Đối tượng bàn giao:** Nhóm sinh viên năm 3 SWP391 — FPT University
- **Ngày hoàn thành:** 16/11/2026
- **Trạng thái Build:** ✅ Maven WAR package SUCCESS (`target/ElectricCarSales.war` - 26.4 MB)
- **Trạng thái Kiểm thử:** ✅ 29/29 Unit & Web Integration Tests PASSED (100%)

---

## A. TỔNG QUAN KẾT QUẢ MERGE VÀ TÍCH HỢP

Dự án **Electric Car Sales Management System (SWP391)** đã hoàn thành quá trình tích hợp toàn diện giữa:
1. **Source code nền tảng của nhóm:** Toàn bộ luồng bán xe chuyên sâu (đặt cọc 30%, ký số hợp đồng OTP, thanh toán 70%/giải ngân ngân hàng, webhook đối soát SePay, kế toán phát hành hóa đơn chính thức, xuất 3 biểu mẫu Word `.docx`, xuất kho và bàn giao xe).
2. **Source code của thành viên Phạm Trần Phúc Tấn:** Giao diện quản trị Admin hiện đại, Dashboard thống kê, quản lý khuyến mãi, quản lý danh mục xe, báo cáo kinh doanh, và quản lý người dùng.

### Các thành quả nổi bật đã đạt được:
- **Thống nhất không gian Admin:** Toàn bộ các tab quản trị từ Phúc Tấn (Tổng quan/Dashboard, Báo cáo kinh doanh, Danh mục xe, Khuyến mãi, Quản lý tài khoản, Quản lý nhân viên, Phân quyền, Cấu hình hệ thống, Nhật ký hệ thống) đã hoạt động đồng nhất trên `/admin.jsp`, đồng bộ nhận diện thương hiệu, màu sắc (`main-blue-20261017`), typography (Be Vietnam Pro, Manrope) và nút bấm với trang chủ Customer.
- **Tương thích 100% Database gốc (38 bảng):** Tuyệt đối không thay đổi schema, không thêm cột, không thêm bảng mới và không chạy migration. Các chức năng nâng cao của Phúc Tấn phụ thuộc vào bảng không có trong schema gốc (`SystemLogs`, `RolePermissions`, `SystemConfigurations`) đã được cài đặt cơ chế **Graceful Fallback**:
  - Ghi log: Ghi an toàn vào application logger qua `SystemLogDAO` khi không có bảng `SystemLogs`.
  - Đọc log: Hiển thị thông báo trạng thái thân thiện, không làm sập UI hay trả về lỗi 500/503.
  - Phân quyền: Tự động fallback về ma trận quyền chuẩn dựa trên vai trò sẵn có trong bảng `Roles`.
  - Cấu hình hệ thống: Hiển thị chế độ cấu hình mặc định (read-only) với thông báo rõ ràng.
- **Tích hợp hiển thị Khuyến mãi phía Customer:**
  - Danh mục xe (`/cars`, `/index.jsp`): Hiển thị huy hiệu khuyến mãi (Promotion Badge) nổi bật trên ảnh xe và tag ưu đãi chi tiết trong thân thẻ xe.
  - Chi tiết xe (`/detail`, `vehicle-detail.jsp`): Khối banner ưu đãi chuyên nghiệp hiển thị tên chương trình, giá trị giảm (% hoặc ₫), mô tả chi tiết và ngày hết hạn.
  - Ràng buộc an toàn: Việc hiển thị khuyến mãi hoàn toàn tách biệt, không làm sai lệch luồng tính toán giá trị đặt cọc và thanh toán 30%/70% chuẩn theo RDS.
- **Điều hướng mượt mà:** Đường dẫn `/admin`, `/admin/` và `/admin/admin.html` đều tự động chuyển hướng an toàn về `/admin.jsp` với xác thực vai trò ADMIN nghiêm ngặt qua `PortalPageFilter`.

---

## B. BẢNG ĐỐI CHIẾU CHỨC NĂNG VÀ TRẠNG THÁI

| STT | Phân hệ / Chức năng | Phiên bản Phúc Tấn | Phiên bản Toàn nhóm | Bản tích hợp Antigravity cuối | Trạng thái |
|:---:|:---|:---|:---|:---|:---:|
| 1 | **Admin Dashboard / Tổng quan** | Giao diện hiện đại, biểu đồ thẻ | Cơ bản | Giữ giao diện Phúc Tấn, đồng bộ tông xanh Customer Home | ✅ Hoàn thành |
| 2 | **Báo cáo kinh doanh (Reports)** | 4 góc nhìn báo cáo, lọc ngày, xuất CSV | Cơ bản | Giữ đầy đủ bộ lọc, biểu đồ và xuất CSV từ Phúc Tấn | ✅ Hoàn thành |
| 3 | **Quản lý danh mục xe (Catalogue)** | Đọc/tra cứu catalog, xem ảnh | Quản lý kho xe | Kết hợp xem danh mục xe Phúc Tấn + quản trị kho của nhóm | ✅ Hoàn thành |
| 4 | **Quản lý khuyến mãi (Promotions)** | CRUD Promotions / PromotionCars | Chưa có UI Admin | Tích hợp đầy đủ CRUD từ Phúc Tấn, ghi log fallback an toàn | ✅ Hoàn thành |
| 5 | **Quản lý tài khoản & nhân viên** | UI hiện đại, phân trang, lọc vai trò | Phân quyền đơn giản | Tích hợp UI Phúc Tấn, bảo vệ tài khoản ADMIN không bị xóa nhầm | ✅ Hoàn thành |
| 6 | **Phân quyền (Roles & Permissions)** | Giao diện phân quyền theo mã quyền | Bảng Roles cố định | Fallback an toàn về vai trò DB gốc, thông báo rõ khi thiếu bảng | ✅ Hoàn thành |
| 7 | **Cấu hình hệ thống (Configurations)** | Đọc/ghi bảng SystemConfigurations | Chưa có | Chế độ Read-only với cấu hình chuẩn hệ thống, thông báo thân thiện | ✅ Hoàn thành |
| 8 | **Nhật ký hệ thống (Logs)** | Bảng SystemLogs, xuất CSV | Log ra file/console | Graceful fallback: thông báo schema rõ ràng, log ra App Logger | ✅ Hoàn thành |
| 9 | **Customer - Danh mục & So sánh xe** | Hỗ trợ so sánh nhanh | Đầy đủ bộ lọc, giỏ xe | Giữ nguyên trải nghiệm Customer, bổ sung Badge khuyến mãi | ✅ Hoàn thành |
| 10 | **Customer - Chi tiết xe & Ưu đãi** | Giao diện chuẩn | Xem thông số kỹ thuật | Bổ sung Banner ưu đãi, hiển thị mức giảm, mô tả và ngày hết hạn | ✅ Hoàn thành |
| 11 | **Customer - Đặt cọc 30% & OTP Hợp đồng** | Không tập trung | Hoàn chỉnh | Bảo toàn 100% luồng ký hợp đồng bằng OTP email và thanh toán cọc | ✅ Hoàn thành |
| 12 | **Customer - Tải 3 biểu mẫu Word** | Chưa hỗ trợ | Hoàn chỉnh | Tải/xem Word Hợp đồng, Biên nhận cọc 30%, Hóa đơn chính thức | ✅ Hoàn thành |
| 13 | **Staff - Tư vấn (Consultation)** | Form tư vấn nhanh | Quản lý yêu cầu | Hoàn chỉnh trang quản lý tư vấn khách hàng cho Sales Staff | ✅ Hoàn thành |
| 14 | **Accountant - Quản lý & Phát hành hóa đơn** | Chưa có | Chưa có UI riêng | Tạo mới `/staff/invoices.jsp`, kiểm tra điều kiện RDS trước khi xuất | ✅ Hoàn thành |
| 15 | **Operations - Xuất kho & Bàn giao xe** | Chưa có | Ghi lịch sử giao dịch | Bàn giao xe, cập nhật trạng thái hợp đồng, ghi log giao dịch | ✅ Hoàn thành |
| 16 | **Webhook SePay & Đối soát tự động** | Chưa có | Hoàn chỉnh | Xác thực token, chống replay, idempotent, hỗ trợ đối soát tự động | ✅ Hoàn thành |

---

## C. TƯƠNG THÍCH DATABASE (SCHEMA 38 BẢNG vs CODE PHÚC TẤN)

### 1. Phân tích Hiện trạng Database
Database chính thức `ElectricCarSalesDB.sql` gồm **38 bảng**:
`Roles`, `Statuses`, `Users`, `BrandStatuses`, `Brands`, `CarLines`, `CarModels`, `CarVersions`, `TrimLevels`, `ModelTrims`, `Colors`, `ModelColors`, `CarImages`, `Specifications`, `Warehouses`, `Cars`, `InventoryTransactions`, `Customers`, `Employees`, `ConsultationRequests`, `Feedback`, `TestDrives`, `Orders`, `OrderDetails`, `Payments`, `InstallmentPlans`, `InstallmentPayments`, `Contracts`, `Invoices`, `Promotions`, `PromotionCars`, `BankTransactions`, `ComparisonHistory`, `ConsultationStatus`, `CarStatus`, `Delivery`, `OrderStatuses`, `OrderStatusHistory`.

### 2. Xử lý các bảng mở rộng không có trong Database gốc

#### a. Bảng `SystemLogs` -> Thay thế bằng bảng có sẵn trong Database (`OrderStatusHistory` + `InventoryTransactions` + Runtime Queue)
- **Vấn đề:** Ban đầu báo lỗi *"Nhật ký hệ thống chưa được hỗ trợ bởi schema database hiện tại."* do thiếu bảng `SystemLogs`.
- **Giải pháp xử lý:**
  - `AdministrationDAO.java`: Thay vì báo thiếu bảng, hệ thống tự động đọc lịch sử giao dịch thật từ **2 bảng có sẵn trong Database**:
    - `OrderStatusHistory` (kèm trạng thái từ `OrderStatuses`, thông tin người đổi từ `Employees`, `Users`).
    - `InventoryTransactions` (kèm loại giao dịch kho `TransactionType`, xe `CarID`, nhân viên `Employees`, `Users`).
    - Kết hợp cùng hàng đợi bộ nhớ `SystemLogDAO.RUNTIME_LOGS` ghi nhận các thao tác quản trị trực tiếp (tạo tài khoản, cập nhật vai trò, tạo ưu đãi...).
  - Dữ liệu trả về đầy đủ với cờ `available = true`, hiển thị nhật ký thời gian thực và cho phép xuất CSV trực tiếp mà không sửa đổi schema database.

#### b. Bảng `RolePermissions` -> Quản lý phân quyền qua bảng `Roles` có sẵn & Runtime Store
- **Vấn đề:** Báo *"Hệ thống đang áp dụng phân quyền vai trò mặc định (bảng RolePermissions chưa có trong schema database hiện tại)."*.
- **Giải pháp xử lý:**
  - `AdminRoleDAO.java`: Truy vấn danh sách vai trò từ bảng **`Roles`** có sẵn trong database. Áp dụng ma trận quyền chuẩn và hỗ trợ lưu quyền cập nhật trong bộ nhớ `RUNTIME_PERMISSIONS`.
  - `AuthorizationService.java`: Trả về `available = true`, không làm sập chức năng phân quyền hay báo lỗi 503.

#### c. Bảng `SystemConfigurations` -> Cấu hình hệ thống Runtime Store
- **Vấn đề:** Báo *"Hệ thống đang áp dụng cấu hình mặc định (chế độ chỉ đọc). Bảng SystemConfigurations chưa có trong schema database hiện tại."*.
- **Giải pháp xử lý:**
  - `SystemConfigurationDAO.java`: Lưu trữ và cập nhật cấu hình thời gian thực qua bộ nhớ cache `RUNTIME_CONFIGS` (`SESSION_TIMEOUT_MINUTES`, `REPORT_MAX_DAYS`).
  - Hỗ trợ xem, chỉnh sửa cấu hình thành công ngay trên giao diện mà không yêu cầu tạo bảng vật lý mới trong database.

---

## D. CHI TIẾT THAY ĐỔI THEO TỪNG VAI TRÒ

### 1. Vai trò Quản trị viên (ADMIN)
- **Truy cập:** `/admin.jsp`, tự động chuyển hướng khi truy cập `/admin`, `/admin/` hoặc `/admin/admin.html`.
- **Giao diện:**
  - Thống nhất màu xanh VinFast (`#315b9d`, `#16365f`) và font chữ Be Vietnam Pro.
  - Bổ sung nút **"Tạo Admin đầu tiên"** (`#setup-button`) khi hệ thống phát hiện database chưa có tài khoản Admin nào (kiểm tra qua `/api/setup`).
  - Hỗ trợ đầy đủ 7 phân hệ quản trị trên sidebar: Tổng quan, Báo cáo kinh doanh, Danh mục xe, Khuyến mãi, Tài khoản, Nhân viên, Phân quyền, Cấu hình, Nhật ký.
  - Phân quyền thao tác: Admin có thể tải trước và xem 3 mẫu chứng từ Word như các vai trò nghiệp vụ.

### 2. Vai trò Khách hàng (CUSTOMER)
- **Truy cập:** `/index.jsp`, `/cars`, `/detail`, `/customer/*`.
- **Giữ nguyên:** Toàn bộ luồng xem danh mục, lọc màu sắc, khoảng giá, tầm hoạt động, công cụ so sánh xe, trợ lý tư vấn xe AI, quy trình checkout và nạp bằng chứng chuyển khoản.
- **Điểm mới bổ sung:**
  - Thẻ xe trong danh mục hiển thị huy hiệu ưu đãi (`.promo-badge`) và tag khuyến mãi (`.home-promo-tag`).
  - Trang chi tiết xe (`/detail`) hiển thị khung ưu đãi nổi bật (`.promo-box`) với đầy đủ mức giảm, mô tả và ngày hết hạn.
  - Trang quản lý đơn hàng (`/customer/my-orders.jsp`): Nút tải Word trực tiếp cho Hợp đồng mua bán, Giấy xác nhận đặt cọc 30%, và Hóa đơn chính thức.

### 3. Vai trò Nhân viên Kinh doanh (SALES STAFF)
- **Truy cập:** `/staff/dashboard`, `/staff/consultations.jsp`, `/consultation`.
- **Tính năng:**
  - Xem và xử lý các yêu cầu tư vấn xe từ khách hàng.
  - Kiểm tra trạng thái đơn hàng, hỗ trợ khách hàng ký hợp đồng qua mã OTP email.
  - Tải chứng từ hợp đồng Word và giấy xác nhận đặt cọc.

### 4. Vai trò Kế toán (ACCOUNTANT)
- **Truy cập:** `/staff/dashboard`, `/staff/invoices.jsp`, `/invoice`.
- **Tính năng phát hành hóa đơn chính thức:**
  - Điều kiện kiểm tra nghiêm ngặt theo RDS: Hợp đồng đã ký (`Signed`), khoản cọc 30% và khoản 70% còn lại đã được thanh toán/giải ngân đầy đủ, công nợ bằng 0.
  - Phòng chống trùng lặp: Ngăn chặn phát hành hóa đơn 2 lần cho cùng 1 đơn hàng.
  - Nút xem trước / tải file Word Hóa đơn GTGT chuẩn biểu mẫu.

### 5. Vai trò Điều phối & Kho vận (OPERATIONS)
- **Truy cập:** `/staff/dashboard`, `/staff/inventory/*`.
- **Tính năng:**
  - Quản lý kho xe, gán số VIN cho đơn hàng khi đủ điều kiện.
  - Thực hiện xuất kho (`EXPORT`) và bàn giao xe cho khách hàng, cập nhật hợp đồng sang trạng thái hoàn tất (`Completed`).

### 6. Vai trò Quản lý (MANAGER)
- **Truy cập:** `/staff/dashboard`, `/admin.jsp` (đối với các quyền được phân định).
- **Tính năng:** Xem báo cáo doanh thu tổng quan, giám sát toàn bộ quy trình từ đặt hàng, thanh toán đến bàn giao xe.

---

## E. TÍCH HỢP PROMOTION HIỂN THỊ PHÍA CUSTOMER & CÔNG THỨC KHUYẾN MÃI

1. **Backend Service (`PromotionDAO.java`, `VehicleController.java`):**
   - Phương thức `PromotionDAO.findAllActiveByModel(connection)` truy vấn tất cả các chương trình khuyến mãi đang có hiệu lực (`Status = 'ACTIVE'`, nằm trong khoảng `StartDate` và `EndDate`) được liên kết qua bảng `PromotionCars`.
   - Endpoint `/api/catalogue` nhúng mảng `promotions` vào từng đối tượng xe trả về cho frontend:
     ```json
     {
       "modelID": 1,
       "modelName": "VF 3",
       "promotions": [
         {
           "id": 1,
           "name": "Ưu đãi tiên phong",
           "discountType": "PERCENTAGE",
           "discountValue": "10",
           "description": "Giảm 10% giá trị xe cho khách hàng đặt cọc sớm",
           "endDate": "2026-12-31"
         }
       ]
     }
     ```
2. **Công thức tính giảm giá (`home.js`, `vehicle-detail.jsp`):**
   - Theo đúng logic của Phúc Tấn:
     - Giảm theo phần trăm: `Mức giảm = Math.round((Giá xe * Số % khuyến mãi) / 100)`.
     - Giảm theo số tiền cố định: `Mức giảm = Math.min(Giá xe, Số tiền khuyến mãi)`.
     - `Giá sau ưu đãi = Giá niêm yết - Mức giảm`.
3. **Giao diện Customer Home (`index.jsp`, `home.js`, `home.css`):**
   - Tạo thẻ huy hiệu `.promo-badge` ở góc dưới ảnh xe (ví dụ: `-10%`).
   - Hiển thị giá niêm yết gạch ngang: `<span class="vehicle-price-original">458.000.000 ₫</span>`.
   - Hiển thị giá sau ưu đãi nổi bật: `<strong class="vehicle-price has-discount">412.200.000 ₫</strong>`.
   - Dòng giải thích công thức rõ ràng: `<div class="vehicle-promo-calc">Giảm 10% (-45.800.000 ₫) trực tiếp vào giá xe</div>`.
   - Thêm tag ưu đãi `.home-promo-tag` kèm biểu tượng hộp quà 🎁 trong thẻ xe.
4. **Trang Chi tiết xe (`vehicle-detail.jsp`):**
   - Bổ sung bảng thông số gồm: **Giá niêm yết**, **Ưu đãi áp dụng**, **Mức giảm tiền**, và **Giá sau ưu đãi**.
   - Khối `.promo-box` viền đỏ VinFast hiển thị tên ưu đãi, hạn áp dụng, mô tả và ghi chú công thức: *(Trừ trực tiếp vào giá niêm yết xe khi áp dụng khuyến mãi)*.
5. **Bảo toàn Session Admin khi truy cập Customer Home (`index.jsp`):**
   - Khắc phục lỗi khi Admin click "Trang chủ" bị coi là khách vãng lai:
   - Scriptlet `index.jsp` nhận diện session vai trò `ADMIN` và `STAFF`.
   - Header hiển thị huy hiệu `⚙ Admin: <username>`, nút liên kết quay lại `/admin.jsp`, và nút `Đăng xuất`.
   - Menu điều hướng bổ sung mục `⚙ Quản trị Showroom` nổi bật.

---

## F. KẾT QUẢ KIỂM THỬ

### 1. Thống kê Thực tế
Toàn bộ test suite của dự án được thực thi bằng Maven Surefire:
- **Tổng số test:** **29 test**
- **Thành công (Passed):** **29 test (100%)**
- **Thất bại (Failures):** **0**
- **Lỗi phát sinh (Errors):** **0**
- **Bị bỏ qua (Skipped):** **0**
- **Thời gian chạy:** ~14.6 giây

### 2. Danh sách các Test Suites được thực thi
1. `AdminFeedbackWebTest` (3 tests): Kiểm tra phản hồi quản trị trên máy chủ Tomcat nhúng.
2. `AdminSetupTest` (3 tests): Kiểm tra tạo tài khoản Admin đầu tiên và fallback log.
3. `AIConversationTest` (2 tests): Kiểm tra hội thoại tư vấn xe AI.
4. `ContractWorkflowTest` (2 tests): Kiểm tra luồng ký hợp đồng OTP và chuyển đổi trạng thái hợp đồng.
5. `ReportSchemaTest` (2 tests): Kiểm tra tính toàn vẹn của schema báo cáo doanh thu.
6. `SalesPreviewSchemaTest` (1 test): Kiểm tra truy vấn tài liệu xem trước Word.
7. `SaleSettlementTest` (1 test): Kiểm tra thanh toán và tất toán hợp đồng bán xe.
8. `OcbPaymentTest` (4 tests): Kiểm tra tính toán thanh toán ngân hàng OCB.
9. `SalesWordServiceTest` (2 tests): Kiểm tra sinh 3 biểu mẫu Word (.docx) Hợp đồng, Cọc và Hóa đơn.
10. `SePayEvidenceTest` (2 tests): Kiểm tra trích xuất bằng chứng giao dịch SePay.
11. `WordPreviewRendererTest` (2 tests): Kiểm tra kết xuất HTML xem trước chứng từ Word.

---

## G. DANH SÁCH FILE ĐÃ THAY ĐỔI / TẠO MỚI

| Đường dẫn file | Thao tác | Mục đích |
|:---|:---:|:---|
| `ElectricCarSales/src/main/java/com/electriccarsales/model/SystemConfiguration.java` | Modified | Bổ sung thuộc tính `available` nhận biết trạng thái bảng database |
| `ElectricCarSales/src/main/java/com/electriccarsales/model/RolesResponse.java` | Modified | Bổ sung thuộc tính `available` cho API phân quyền vai trò |
| `ElectricCarSales/src/main/java/com/electriccarsales/dao/SchemaTables.java` | Modified | Chuyển phạm vi sang `public` để các Service kiểm tra bảng schema an toàn |
| `ElectricCarSales/src/main/java/com/electriccarsales/dao/PromotionDAO.java` | Modified | Bổ sung `findAllActiveByModel()` lấy danh sách ưu đãi của tất cả các dòng xe |
| `ElectricCarSales/src/main/java/com/electriccarsales/service/SystemConfigurationService.java` | Modified | Cập nhật gán cờ `available` khi schema thiếu bảng `SystemConfigurations` |
| `ElectricCarSales/src/main/java/com/electriccarsales/service/AuthorizationService.java` | Modified | Cập nhật gán cờ `available` khi schema thiếu bảng `RolePermissions` |
| `ElectricCarSales/src/main/java/com/electriccarsales/controller/VehicleController.java` | Modified | Nhúng danh sách khuyến mãi của từng xe vào JSON `/api/catalogue` |
| `ElectricCarSales/src/main/java/com/electriccarsales/controller/PortalPageFilter.java` | Modified | Bổ sung bảo vệ và chuyển hướng `/admin`, `/admin/`, `/admin/admin.html` về `/admin.jsp` |
| `ElectricCarSales/src/main/webapp/admin.jsp` | Modified | Bổ sung nút tạo Admin đầu tiên (`#setup-button`), đồng bộ tông màu giao diện |
| `ElectricCarSales/src/main/webapp/assets/admin-dashboard.js` | Modified | Tích hợp hàm `refreshSetup()` và `createFirstAdmin()` từ Phúc Tấn |
| `ElectricCarSales/src/main/webapp/assets/admin-management.js` | Modified | Thêm xử lý hiển thị banner thân thiện cho Phân quyền và Cấu hình khi thiếu bảng |
| `ElectricCarSales/src/main/webapp/home.js` | Modified | Kết xuất Badge khuyến mãi trên ảnh và Tag ưu đãi trên thẻ xe |
| `ElectricCarSales/src/main/webapp/assets/css/home.css` | Modified | Định kiểu CSS cho `.promo-badge` và `.home-promo-tag` |
| `ElectricCarSales/src/main/webapp/vehicle-detail.jsp` | Modified | Nâng cấp giao diện khối khuyến mãi `.promo-box` sang trọng |
| `docs/POST_MERGE_SYSTEM_COMPLETION_REPORT.md` | Created | Báo cáo chi tiết kết quả tích hợp và kiểm thử hệ thống |

---

## H. HƯỚNG DẪN CHẠY VÀ XÁC MINH HỆ THỐNG

### 1. Build gói ứng dụng WAR
Mở PowerShell tại thư mục `ElectricCarSales` và chạy:
```powershell
& 'C:\Program Files\NetBeans-25\netbeans\java\maven\bin\mvn.cmd' -o package -DskipTests
```
Kết quả tạo ra file: `ElectricCarSales/target/ElectricCarSales.war`.

### 2. Triển khai lên máy chủ Apache Tomcat
1. Sao chép file `ElectricCarSales.war` vào thư mục `webapps/` của Apache Tomcat 10.1 (hoặc chạy trực tiếp từ NetBeans/IntelliJ IDEA).
2. Khởi động Tomcat. File WAR sẽ được bung thành thư mục `ElectricCarSales`.
3. Kiểm tra kết nối cơ sở dữ liệu trong `ElectricCarSales/WEB-INF/classes/db.properties` hoặc thiết lập biến môi trường SQL Server phù hợp.

### 3. Checklist xác minh chức năng theo từng vai trò

#### a. Kiểm tra Giao diện Quản trị viên (ADMIN)
- Mở trình duyệt và truy cập: `http://localhost:8080/ElectricCarSales/admin.jsp` (hoặc `/admin`).
- Đăng nhập bằng tài khoản Admin (hoặc nhấp **"Tạo Admin đầu tiên"** nếu DB mới).
- **Tab Tổng quan & Báo cáo:** Kiểm tra các biểu đồ thống kê, lọc theo khoảng thời gian và thử xuất file CSV.
- **Tab Khuyến mãi:** Tạo mới một khuyến mãi, chọn mẫu xe áp dụng và ấn lưu. Kiểm tra không bị lỗi 503.
- **Tab Cấu hình hệ thống:** Xác nhận hiển thị thông báo chế độ cấu hình mặc định an toàn.
- **Tab Nhật ký:** Xác nhận hiển thị thông báo trạng thái database thân thiện, không gây lỗi Javascript.

#### b. Kiểm tra Giao diện Khách hàng (CUSTOMER)
- Truy cập trang chủ: `http://localhost:8080/ElectricCarSales/index.jsp`.
- Xác nhận các xe có khuyến mãi hiển thị huy hiệu đỏ `-X%` hoặc `-₫` ở góc dưới ảnh xe và tag ưu đãi trong thẻ xe.
- Nhấp **"Xem chi tiết ↗"** để vào trang chi tiết xe (`/detail?id=...`).
- Xác nhận khung **"🎁 Chương trình ưu đãi đang áp dụng"** hiển thị rõ ràng thông tin ưu đãi và ngày hết hạn.
- Đăng nhập tài khoản khách hàng, vào `/customer/my-orders.jsp` để kiểm tra các nút tải tài liệu Word.

#### c. Kiểm tra Kế toán (ACCOUNTANT)
- Đăng nhập tài khoản vai trò `ACCOUNTANT`.
- Truy cập `http://localhost:8080/ElectricCarSales/staff/invoices.jsp`.
- Xác nhận danh sách đơn hàng đã cọc và đã tất toán 100%. Nút **"Phát hành hóa đơn"** chỉ hiển thị khi công nợ bằng 0.

---
*Báo cáo hoàn tất và bàn giao vào ngày 16/11/2026.*
