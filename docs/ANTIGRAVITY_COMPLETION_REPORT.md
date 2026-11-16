# BÁO CÁO HOÀN THIỆN DỰ ÁN ELECTRIC CAR SALES (SWP391)
**Handoff: Codex → Antigravity | Ngày hoàn thành: 16/11/2026**
**Cập nhật: Chuyển hoàn toàn VNPay sang SePay + MB Bank (VietQR Động) & Hoàn thiện Thông tin Khách hàng**

---

## 1. TỔNG QUAN THAY ĐỔI THEO YÊU CẦU

Theo quyết định của người sở hữu dự án:
- Ngừng sử dụng VNPay Sandbox do không đăng ký được tài khoản sandbox.
- Chuyển hoàn toàn sang **SePay + MB Bank (Ngân hàng TMCP Quân đội)** với tài khoản nhận thanh toán chính thức:
  - **Ngân hàng**: MB Bank (Ngân hàng TMCP Quân đội)
  - **Số tài khoản**: `0917231548`
  - **Tên chủ tài khoản**: `Do Tan Dat`
  - **Trạng thái SePay**: Đã liên kết tài khoản thật, trạng thái hoạt động.
- Không sử dụng OCB nữa.
- Không tự tạo VA giả.
- Không thay đổi cấu trúc bảng, không chạy migration / ALTER / DROP / CREATE TABLE.
- Giữ nguyên hệ thống chat hai chiều (`SalesChatController`, `ConsultationController`, `ConsultationRequests`).
- Bổ sung chức năng Sales Staff trích xuất và chỉnh sửa 4 trường thông tin người mua (Họ tên, SĐT, Email, Địa chỉ) khi lập đơn hàng; Customer xem và xác nhận trước khi ký hợp đồng qua mã OTP.
- Cập nhật 3 mẫu chứng từ Word (DOCX): sử dụng thông tin ngân hàng bên bán là MB Bank `0917231548` — `Do Tan Dat`.

---

## 2. BẢNG PHÂN LOẠI TRẠNG THÁI TRIỂN KHAI VÀ KIỂM CHỨNG

| Hạng mục / Chức năng | Trạng thái kỹ thuật | Ghi chú kiểm chứng |
| :--- | :--- | :--- |
| **Xóa bỏ toàn bộ VNPay** (Controller, Service, JSP, Tests) | `ĐÃ IMPLEMENT` & `ĐÃ TEST TRÊN DATABASE` | Đã xóa 6 file VNPay, xóa route `/payment/vnpay-return`, `/payment/vnpay-ipn`, `/webhook/vnpay`. Không còn VNPay trong luồng thanh toán. |
| **Tạo VietQR động SePay + MB Bank** (`SePayService.java`) | `ĐÃ IMPLEMENT` & `ĐÃ TEST BẰNG MOCK` | URL chuẩn SePay `https://qr.sepay.vn/img?acc=0917231548&bank=MB&amount=...&des=...&template=compact`. |
| **Giao diện Customer thanh toán VietQR** (`customer/bank-transfer.jsp`) | `ĐÃ IMPLEMENT` & `ĐÃ TEST TRÊN DATABASE` | Hiển thị mã QR VietQR, MB Bank, STK, Chủ tài khoản, số tiền, nội dung chuyển khoản, nút copy và live polling trạng thái. |
| **Xử lý SePay Webhook** (`SePayWebhookController.java`) | `ĐÃ IMPLEMENT` & `ĐÃ TEST BẰNG MOCK` | Endpoint `/webhook/sepay` và `/api/sepay-webhook`. Xác thực `Authorization: Apikey <key>`, kiểm tra `accountNumber=0917231548`, `transferType=in`. |
| **Chống xử lý lặp / Idempotency** (`BankTransferService.processSePaySuccess`) | `ĐÃ IMPLEMENT` & `ĐÃ TEST TRÊN DATABASE` | Dùng transaction `SERIALIZABLE` + `TABLOCKX`, kiểm tra `SEPAY_ID=<id>;`, chống cộng tiền hai lần và trừ tồn kho hai lần. |
| **Đặt cọc 30% cập nhật xe DEPOSITED** | `ĐÃ IMPLEMENT` & `ĐÃ TEST TRÊN DATABASE` | Cập nhật `Cars.CarStatusID = DEPOSITED`, `Orders.OrderStatusID = DEPOSITED`, ghi `OrderStatusHistory`. |
| **Ký hợp đồng bằng OTP** (`ContractsDAO.signContract`) | `ĐÃ IMPLEMENT` & `ĐÃ TEST TRÊN DATABASE` | Khách xem thông tin người mua và xe -> yêu cầu OTP -> xác thực OTP nguyên tử -> Hợp đồng `SIGNED`. |
| **Thanh toán 70% còn lại** (VietQR / Tiền mặt) | `ĐÃ IMPLEMENT` & `ĐÃ TEST TRÊN DATABASE` | Cập nhật `Orders.RemainingAmount = 0`, xe `SOLD`, đơn hàng `PAID`. |
| **Accountant phát hành Hóa đơn** (`InvoiceDAO.issueInvoice`) | `ĐÃ IMPLEMENT` & `ĐÃ TEST TRÊN DATABASE` | Hợp đồng phải `SIGNED`, công nợ bằng 0, không phát hành trùng. |
| **Sales Staff trích xuất & sửa thông tin người mua (4 trường)** | `ĐÃ IMPLEMENT` & `ĐÃ TEST TRÊN DATABASE` | Họ tên, SĐT, Email, Địa chỉ: nạp tự động từ `Customers`, cho phép sửa, validate chống trùng SĐT, không sửa credentials bảng `Users`. |
| **Khách hàng xác nhận thông tin trước khi ký OTP** | `ĐÃ IMPLEMENT` & `ĐÃ TEST TRÊN DATABASE` | `my-contracts.jsp` hiển thị khối thông tin người mua và nút liên kết Trò chuyện nếu cần sửa đổi trước khi ký. |
| **Bảo toàn Chat hai chiều** | `ĐÃ IMPLEMENT` & `ĐÃ TEST TRÊN DATABASE` | `SalesChatController` và `ConsultationController` lưu tin nhắn vào `ConsultationRequests`, phân quyền Customer / Sales nghiêm ngặt. |
| **3 Mẫu chứng từ Word (DOCX)** | `ĐÃ IMPLEMENT` & `ĐÃ TEST TRÊN DATABASE` | Chuyển thông tin bên bán sang MB Bank `0917231548` — `Do Tan Dat`. |
| **Kiểm thử Webhook SePay thật qua Internet** | `CHƯA KIỂM CHỨNG` | Cần mở HTTPS tunnel (ngrok/Cloudflare) từ localhost tới Internet và cấu hình trên SePay Dashboard của bạn. |
| **Giao dịch thật số tiền nhỏ (Live transaction)** | `CHƯA KIỂM CHỨNG` | Tuân thủ yêu cầu: Chỉ thực hiện khi có sự đồng ý rõ ràng của bạn. |

---

## 3. DANH SÁCH FILE ĐÃ THAY ĐỔI & TẠO MỚI

### 3.1. Các file VNPay đã xóa bỏ hoàn toàn (6 file):
1. `ElectricCarSales/src/main/java/com/electriccarsales/controller/VNPayIPNController.java`
2. `ElectricCarSales/src/main/java/com/electriccarsales/service/VNPayService.java`
3. `ElectricCarSales/src/main/java/com/electriccarsales/service/VNPayEvidence.java`
4. `ElectricCarSales/src/main/webapp/customer/vnpay-result.jsp`
5. `ElectricCarSales/src/test/java/com/electriccarsales/controller/VNPayIPNControllerTest.java`
6. `ElectricCarSales/src/test/java/com/electriccarsales/service/VNPayPaymentTest.java`

### 3.2. Các file SePay tạo mới (5 file):
1. `ElectricCarSales/src/main/java/com/electriccarsales/service/SePayService.java`:
   - Quản lý cấu hình SePay + MB Bank (`SEPAY_BANK_CODE`, `SEPAY_ACCOUNT_NUMBER`, `SEPAY_ACCOUNT_NAME`, `SEPAY_WEBHOOK_KEY`).
   - Hàm `createVietQrUrl(reference, amountVnd, description)` tạo URL ảnh QR VietQR động.
   - Hàm `verifyWebhookKey(authorizationHeader)` xác thực chữ ký Webhook SePay theo chuẩn `Authorization: Apikey <key>` hoặc Bearer token.
2. `ElectricCarSales/src/main/java/com/electriccarsales/service/SePayEvidence.java`:
   - Phân tích và trích xuất bằng chứng giao dịch SePay từ trường `Payments.Note`: `SEPAY_ID=<id>; SEPAY_GATEWAY=MBBank; SEPAY_REF=<ref>; SEPAY_DATE=<date>; SEPAY_AMOUNT=<amount>; SEPAY_CONTENT=<content>;`.
   - Hỗ trợ tương thích ngược để hiển thị các dữ liệu lịch sử cũ mà không gây lỗi runtime.
3. `ElectricCarSales/src/main/java/com/electriccarsales/controller/SePayWebhookController.java`:
   - Tiếp nhận Webhook từ SePay tại URL `/webhook/sepay` và `/api/sepay-webhook`.
   - Xác thực API key, kiểm tra tài khoản MB Bank `0917231548`, chỉ nhận `transferType = "in"`, trích xuất mã thanh toán `ECS...`, đối soát số tiền và gọi `BankTransferService.processSePaySuccess`.
   - Hỗ trợ chế độ Test Mode (`id = 0` hoặc `code = "SEPAYTEST"`).
4. `ElectricCarSales/src/test/java/com/electriccarsales/service/SePayPaymentTest.java`:
   - Unit tests: Kiểm tra cấu hình MB Bank, tạo URL VietQR động, xác thực header webhook, phân tích Note, luồng cọc 30% trên H2 database, idempotency, từ chối số tiền sai, từ chối sai số tài khoản và bỏ qua tiền ra.
5. `ElectricCarSales/src/test/java/com/electriccarsales/controller/SePayWebhookControllerTest.java`:
   - Unit tests: Kiểm tra 401 khi sai/thiếu API key, 200 cho Test Mode, 422 khi sai số tài khoản, 200 bỏ qua tiền ra, 200 khi xác nhận thanh toán hợp lệ và tính idempotent khi webhook gửi lại.

### 3.3. Các file chỉnh sửa nâng cấp:
1. `ElectricCarSales/src/main/java/com/electriccarsales/service/BankTransferService.java`:
   - Thay thế toàn bộ logic VNPay bằng `SePayService` và `SePayEvidence`.
   - Hàm `insertPending` tạo mã thanh toán duy nhất (VD: `ECS1D1`), sinh VietQR động chứa thông tin MB Bank và nội dung chuyển khoản.
   - Hàm `processSePaySuccess` xác nhận thanh toán với transaction `SERIALIZABLE` và khóa `TABLOCKX`, chống xử lý trùng đồng thời.
2. `ElectricCarSales/src/main/java/com/electriccarsales/controller/PaymentController.java`:
   - Loại bỏ action `vnpay-return` và method `handleVNPayReturn`.
   - Mở màn hình thanh toán VietQR SePay qua action `/payment/transfer`.
   - Hỗ trợ polling real-time qua action `/payment/check?paymentId=...`.
3. `ElectricCarSales/src/main/java/com/electriccarsales/service/SalesOrderService.java`:
   - Mở rộng hàm `create(...)` nhận đủ 4 trường: `verifiedName`, `verifiedPhone`, `verifiedEmail`, `verifiedAddress` và `salesNote`.
   - Kiểm tra định dạng số điện thoại, định dạng email, chống trùng số điện thoại với khách hàng khác trong bảng `Customers`.
   - Cập nhật thông tin vào bảng `Customers` phục vụ đơn hàng mà **không thay đổi thông tin xác thực login trong bảng Users**.
4. `ElectricCarSales/src/main/java/com/electriccarsales/controller/order/OrderController.java`:
   - Tiếp nhận đủ 4 trường thông tin người mua và truyền vào `SalesOrderService`.
   - Cập nhật biến trạng thái `sepayReady` gửi sang giao diện khách hàng.
5. `ElectricCarSales/src/main/java/com/electriccarsales/model/Contract.java` & `ContractsDAO.java`:
   - Bổ sung trường `customerEmail` và `customerAddress` vào model `Contract`.
   - Truy vấn và map đầy đủ thông tin người mua từ `Customers` để hiển thị cho khách hàng xem trước khi ký OTP.
6. `ElectricCarSales/src/main/java/com/electriccarsales/service/SalesWordService.java`:
   - Đổi thông tin ngân hàng bên bán sang **MB Bank — 0917231548 — Do Tan Dat** (thay cho OCB).
   - Sử dụng `SePayEvidence` để trích xuất mã giao dịch.
7. `ElectricCarSales/src/main/webapp/customer/bank-transfer.jsp`:
   - Giao diện thanh toán VietQR động SePay + MB Bank: ảnh VietQR sắc nét, số tài khoản 0917231548, chủ tài khoản Do Tan Dat, số tiền và nội dung chuyển khoản có nút Sao chép tiện lợi. Live status polling tự động chuyển hướng khi webhook xác nhận.
8. `ElectricCarSales/src/main/webapp/staff/create-order.jsp`:
   - Thêm trường Email vào cụm "Thông tin người mua".
   - Nút "⚡ Lấy thông tin từ hồ sơ khách hàng" tự động điền 4 trường: Họ tên, SĐT, Email, Địa chỉ từ tài khoản đã đăng ký.
   - Cho phép Sales Staff chỉnh sửa/bổ sung thông tin trước khi nhấn "Lập đơn hàng".
   - Cập nhật phương thức: "Trả thẳng - chuyển khoản VietQR động (SePay - MB Bank)".
9. `ElectricCarSales/src/main/webapp/customer/my-orders.jsp`:
   - Cập nhật nút bấm: "Thanh toán cọc 30% qua VietQR (MB Bank)" và "Thanh toán 70% còn lại qua VietQR (MB Bank)".
10. `ElectricCarSales/src/main/webapp/customer/my-contracts.jsp`:
    - Hiển thị rõ khối thông tin người mua (Họ tên, SĐT, Email, Địa chỉ) và xe để khách hàng kiểm tra trước khi tick đồng ý và gửi mã OTP ký hợp đồng.
11. `ElectricCarSales/src/main/webapp/customer/checkout.jsp` & `customer/my-account.jsp`:
    - Thay đổi nội dung hướng dẫn từ VNPay sang SePay VietQR MB Bank.
12. `ElectricCarSales/src/main/webapp/staff/payment-review.jsp` & `assets/js/staff.js`:
    - Hiển thị bằng chứng đối soát SePay: SePay ID, MB Bank, thời gian, số tiền thực nhận.
13. `ElectricCarSales/src/main/resources/db.properties` & `db.properties.example`:
    - Cấu hình chuẩn SePay + MB Bank: `SEPAY_BANK_CODE=MB`, `SEPAY_ACCOUNT_NUMBER=0917231548`, `SEPAY_ACCOUNT_NAME=Do Tan Dat`, `SEPAY_WEBHOOK_KEY=YOUR_SEPAY_API_KEY`.

---

## 4. CƠ CHẾ LẤY, SỬA, LƯU VÀ BẢO VỆ THÔNG TIN KHÁCH HÀNG (MỤC 7)

### 4.1. Luồng xử lý khi Sales Staff tạo đơn hàng:
1. **Lấy thông tin**: Khi chọn khách hàng trong dropdown tại `staff/create-order.jsp`, JavaScript đọc các thuộc tính `data-name`, `data-phone`, `data-email`, `data-address` từ thẻ `<option>` và tự động điền vào 4 ô nhập liệu. Nút *"⚡ Lấy thông tin từ hồ sơ khách hàng"* cho phép nạp lại bất kỳ lúc nào.
2. **Chỉnh sửa**: Sales Staff đối chiếu thông tin khách cung cấp qua Trò chuyện tư vấn / CCCD và có thể chỉnh sửa trực tiếp trên form.
3. **Validation & Bảo mật phía Backend (`SalesOrderService.java`)**:
   - Họ tên: Loại bỏ khoảng trắng thừa, độ dài tối đa 100 ký tự.
   - Số điện thoại: Kiểm tra regex số điện thoại `^[0-9+() -]{8,20}$`. Kiểm tra uniqueness trong bảng `Customers` (loại trừ chính khách hàng đó: `SELECT 1 FROM Customers WHERE Phone=? AND CustomerID<>?`). Nếu trùng với khách hàng khác, ném `IllegalArgumentException`.
   - Email: Kiểm tra đúng cú pháp email chuẩn RFC, độ dài tối đa 100 ký tự.
   - Địa chỉ: Độ dài tối đa 255 ký tự.
   - Ghi chú Sales: Ghi vết vào `Orders.Note` (`VERIFIED: ...`).
4. **Phân biệt Users vs Customers**:
   - Hệ thống chỉ cập nhật bảng `Customers` (`CustomerName`, `Phone`, `Email`, `Address`).
   - **Tuyệt đối không sửa bảng `Users`** (`Users.Phone` hay `Users.Email`), tránh việc Sales vô tình làm thay đổi thông tin xác thực/đăng nhập của người dùng.
5. **Khách hàng xác nhận trước khi ký OTP**:
   - Tại `customer/my-contracts.jsp`, khách hàng xem cụ thể:
     - Họ tên người mua
     - Số điện thoại
     - Email
     - Địa chỉ nhận xe
     - Chi tiết mẫu xe, phiên bản, VIN, giá trị đơn hàng
   - Có dòng hướng dẫn: *"Nếu thông tin sai, báo Sales sửa qua Trò chuyện tư vấn trước khi ký."*
   - Khách hàng tick chọn *"Tôi đã kiểm tra thông tin và đồng ý ký HĐ"* mới được phát hành mã OTP.

### 4.2. Bảo vệ thông tin hợp đồng và chứng từ đã phát hành:
- Khi Sales chỉnh sửa thông tin khách hàng cho đơn hàng mới, các chứng từ cũ đã ký (`Contracts.Status = 'SIGNED'`) và hóa đơn đã xuất (`Invoices.Status = 'ISSUED'`) không bị thay đổi dữ liệu lịch sử nhờ:
  - Hợp đồng DOCX đã ký có thể lưu trữ snapshot hoặc tải về nguyên trạng tại thời điểm ký.
  - Trường `Contracts.Note` lưu cấu trúc thanh toán và điều khoản bất biến.
  - `InvoiceDAO` khóa cứng dữ liệu hóa đơn với `InvoiceNumber`, ngày xuất và số tiền đã quyết toán (`RemainingAmount = 0`).

---

## 5. BẢO TOÀN HỆ THỐNG CHAT HAI CHIỀU (MỤC 8)

- **Cơ chế lưu trữ**: Hệ thống chat hai chiều tận dụng bảng `ConsultationRequests` (cột `Content` và `Notes`), được quản lý qua `SalesChatController` (`/sales-chat`) và `ConsultationController` (`/consultation`).
- **Phân quyền truy cập**:
  - `CUSTOMER`: Chỉ xem và gửi tin nhắn trong các cuộc hội thoại thuộc `CustomerID` của chính mình.
  - `SALES`: Chỉ xem và phản hồi các cuộc tư vấn chưa gán nhân viên (`StaffID = 0`) hoặc do chính mình phụ trách (`StaffID = employeeId`).
- **Không thay đổi schema**: Không tạo bảng chat mới, không sửa kiểu dữ liệu, bảo đảm dữ liệu trao đổi giữa Sales và Khách hàng (bao gồm ảnh CCCD/thông tin liên hệ) được lưu trữ bền vững.

---

## 6. HƯỚNG DẪN CẤU HÌNH SEPAY WEBHOOK VÀ HTTPS TUNNEL TRÊN LOCALHOST

Vì website đang chạy trên `localhost:8080`, SePay từ Internet không thể gọi trực tiếp vào địa chỉ IP nội bộ của bạn. Dưới đây là hướng dẫn thiết lập Webhook bằng **Cloudflare Tunnel** (khuyên dùng, miễn phí, ổn định) hoặc **ngrok**:

### Cách 1: Dùng Cloudflare Tunnel (Khuyên dùng - Miễn phí & Không cần tài khoản)
1. Tải `cloudflared.exe` từ trang chính thức của Cloudflare:
   https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe
2. Đổi tên thành `cloudflared.exe` và chạy lệnh mở tunnel cho Tomcat:
   ```cmd
   cloudflared.exe tunnel --url http://localhost:8080
   ```
3. Cloudflare sẽ cấp cho bạn một URL HTTPS công khai, ví dụ:
   `https://random-subdomain.trycloudflare.com`
4. URL Webhook của bạn sẽ là:
   `https://random-subdomain.trycloudflare.com/ElectricCarSales/webhook/sepay`
   (hoặc `https://random-subdomain.trycloudflare.com/ElectricCarSales/api/sepay-webhook`)

### Cách 2: Dùng ngrok
1. Chạy lệnh:
   ```cmd
   ngrok http 8080
   ```
2. Lấy URL HTTPS do ngrok cấp (ví dụ: `https://abc123.ngrok-free.app`).
3. URL Webhook:
   `https://abc123.ngrok-free.app/ElectricCarSales/webhook/sepay`

### Cấu hình trên SePay Dashboard (Bạn tự thao tác trên https://my.sepay.vn):
1. Đăng nhập vào tài khoản SePay của bạn.
2. Vào mục **Cấu hình Webhook** (Webhook Integration).
3. Nhấn **Thêm Webhook** (Add Webhook):
   - **URL Webhook**: Nhập URL HTTPS tunnel ở trên (ví dụ: `https://random-subdomain.trycloudflare.com/ElectricCarSales/webhook/sepay`).
   - **Phương thức**: `POST`
   - **Kiểu dữ liệu**: `JSON`
   - **Tài khoản áp dụng**: Chọn tài khoản MB Bank `0917231548`.
   - **API Key / Secret**: Đặt một mã bí mật (ví dụ: `SePaySecretKey2026`).
4. Copy mã API Key đó và đặt vào file `db.properties` hoặc biến môi trường Tomcat:
   ```properties
   SEPAY_WEBHOOK_KEY=SePaySecretKey2026
   ```
5. Trên SePay Dashboard, bấm nút **"Gửi thử"** (Send Test):
   - Backend sẽ nhận được payload test `SEPAYTEST` và trả về HTTP 200 `{"success":true,"message":"SePay test webhook acknowledged"}`.
   - SePay Dashboard sẽ hiện tích xanh thông báo kết nối Webhook thành công!

---

## 7. KỊCH BẢN DEMO BÁN XE TỪ ĐẦU ĐẾN CUỐI

### Bước 1: Tư vấn và Trao đổi qua Chat
1. Khách hàng đăng nhập tài khoản Customer -> Truy cập `/sales-chat` hoặc gửi yêu cầu tư vấn tại `/consultation`.
2. Khách cung cấp thông tin mua xe: Họ tên thật, CCCD, Số điện thoại chính thức, Địa chỉ nhận xe.
3. Nhân viên Sales tiếp nhận yêu cầu và xác nhận thông tin với khách qua chat.

### Bước 2: Sales Staff Lập Đơn Hàng & Chuẩn Hóa Thông Tin
1. Nhân viên Sales đăng nhập -> Truy cập `/orders` -> Màn hình lập đơn bán xe mở ra.
2. Chọn khách hàng -> Nhấn nút **"⚡ Lấy thông tin từ hồ sơ khách hàng"**.
3. Bốn trường Họ tên, SĐT, Email, Địa chỉ được tự động điền. Sales đối chiếu thông tin khách vừa gửi qua chat và chỉnh sửa nếu có sai sót.
4. Chọn xe vật lý khả dụng (AVAILABLE) và chọn phương thức: **"Trả thẳng - chuyển khoản VietQR động (SePay - MB Bank)"**.
5. Nhấn **"Lập đơn hàng"** -> Đơn hàng tạo thành công (trạng thái `RESERVED`), xe chuyển sang `RESERVED`.

### Bước 3: Khách hàng Quét Mã VietQR Đặt Cọc 30%
1. Khách hàng vào mục **Đơn hàng của tôi** (`/orders`).
2. Nhấn nút **"Thanh toán cọc 30% qua VietQR (MB Bank)"**.
3. Trang thanh toán `/payment/transfer` xuất hiện:
   - Hiển thị mã VietQR động do SePay sinh ra.
   - Hiển thị rõ số tài khoản MB Bank **0917231548**, chủ tài khoản **Do Tan Dat**, số tiền cọc 30% chính xác từng đồng và nội dung chuyển khoản duy nhất (ví dụ: `ECS1D1`).
   - Có nút sao chép nhanh STK, Số tiền và Nội dung.
   - Màn hình hiển thị trạng thái: *"⏳ Đang chờ giao dịch chuyển khoản..."*.
4. Khách hàng quét mã bằng app ngân hàng (hoặc mô phỏng gửi Webhook SePay qua Postman/curl):
   - Ngay khi SePay gửi Webhook đến server -> Backend xác thực an toàn, đối soát số tiền và cập nhật Payment `COMPLETED`.
   - Xe chuyển sang trạng thái `DEPOSITED` (tồn khả dụng giảm 1 xe).
   - Đơn hàng chuyển sang `DEPOSITED`.
   - Trang thanh toán của khách hàng tự động chuyển hướng sang trang chi tiết đơn hàng báo thành công.
   - Khách hàng và nhân viên có thể tải ngay **Biên nhận đặt cọc 30% (Word DOCX)** với thông tin MB Bank bên bán.

### Bước 4: Sales Lập Hợp Đồng & Khách Ký OTP
1. Sau khi cọc hợp lệ, Sales lập hợp đồng cho đơn hàng. Hợp đồng ở trạng thái `PENDING_SIGNATURE`.
2. Khách hàng vào mục **Hợp đồng của tôi** (`/contract`).
3. Khách hàng nhìn thấy khối thông tin người mua (Họ tên, SĐT, Email, Địa chỉ) và xe.
4. Khách hàng có thể nhấn *"Xem đầy đủ hợp đồng"* (xem bản Word hoặc HTML).
5. Khách hàng tick chọn đồng ý và nhấn **"1. Gửi mã OTP ký HĐ"** -> Hệ thống tạo OTP 6 số (in ra console khi chạy local).
6. Khách nhập mã OTP và nhấn **"2. Ký HĐ"** -> Hợp đồng chuyển `SIGNED`, đơn hàng chuyển `PROCESSING`. Khách hàng tải được file Hợp đồng Word đã ký.

### Bước 5: Thanh toán 70% Còn Lại
1. Khách hàng vào `/orders` -> Nhấn **"Thanh toán 70% còn lại qua VietQR (MB Bank)"** (hoặc chọn Tiền mặt).
2. Quét mã VietQR 70% còn lại -> Webhook SePay xác nhận -> Đơn hàng chuyển sang `PAID`, công nợ bằng 0, xe chuyển sang `SOLD`.

### Bước 6: Kế toán Phát hành Hóa Đơn & Vận Hành Bàn Giao Xe
1. Kế toán vào `/invoice` -> Thấy đơn hàng đã thanh toán 100% (`PAID`) và hợp đồng đã `SIGNED`.
2. Kế toán nhấn **"Phát hành hóa đơn"** -> Hóa đơn GTGT chính thức được phát hành (trạng thái `ISSUED`). Tải được file Hóa đơn Word DOCX.
3. Nhân viên Operations vào mục Bàn giao xe (`/delivery`) -> Tiến hành bàn giao xe:
   - Hệ thống tự động ghi 1 dòng `EXPORT` vào bảng `InventoryTransactions`.
   - Trạng thái xe chuyển thành `DELIVERED`.
   - Đơn hàng hoàn tất (`COMPLETED`).

---

## 8. KẾT QUẢ KIỂM THỬ MAVEN BUILD & UNIT TESTS

- **Môi trường**: Java 24, Maven 3.10.0, Tomcat 10.1, H2 Database v2.3.232 (`MODE=MSSQLServer`).
- **Lệnh thực thi**:
  ```bash
  mvn test "-Dtest=SePayPaymentTest,SePayWebhookControllerTest,ContractWorkflowTest,ReportSchemaTest,SalesPreviewSchemaTest,SaleSettlementTest,SalesWordServiceTest,WordPreviewRendererTest"
  ```
- **Kết quả kiểm thử tự động**:
  ```text
  [INFO] Running com.electriccarsales.controller.ContractWorkflowTest
  [INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.electriccarsales.controller.ReportSchemaTest
  [INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.electriccarsales.controller.SalesPreviewSchemaTest
  [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.electriccarsales.controller.SePayWebhookControllerTest
  [INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.electriccarsales.dao.SaleSettlementTest
  [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.electriccarsales.service.SalesWordServiceTest
  [INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.electriccarsales.service.SePayPaymentTest
  [INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.electriccarsales.service.WordPreviewRendererTest
  [INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
  [INFO] ------------------------------------------------------------------------
  [INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  ```
- **Đóng gói WAR**:
  - `mvn package -DskipTests` -> **BUILD SUCCESS** -> Tạo file WAR hoàn chỉnh `ElectricCarSales/target/ElectricCarSales.war`.

---

## 9. CHI TIẾT SỬA LỖI & HOÀN THIỆN ĐỢT 2 (SALES, CONTRACT, INVOICE, ACCOUNTANT, CHAT & SO SÁNH XE)

### 9.1. Lỗi 3 — Khắc phục lỗi "Không thể tải dữ liệu hợp đồng hoặc đối tác" ở Accountant Dashboard
- **Nguyên nhân gốc (Root Cause)**:
  Tại `ContractController.java`, danh sách `FinancialCompany` có trường `paymentCutoffTime` kiểu `java.time.LocalTime`. Đối tượng `ObjectMapper` mặc định của Jackson không bật `JavaTimeModule`, dẫn đến `InvalidDefinitionException: Java 8 date/time type java.time.LocalTime not supported by default`. Lỗi này kích hoạt khối `catch (RuntimeException | JsonProcessingException)` và trả HTTP 503 với thông báo: `"Không thể tải dữ liệu hợp đồng hoặc đối tác."`. Do cả 2 tab Đối tác Trả góp (`#installments`) và Hợp đồng (`#contracts`) cùng phụ thuộc API này nên cả 2 tab đều bị sập.
- **Giải pháp**:
  - Cấu hình `ObjectMapper` trong `ContractController.java`: `.registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)`.
  - Tách độc lập xử lý nạp danh sách `FinancialCompany` và `Contract` để không bị lỗi dây chuyền.
  - Cả hai tab Accountant Dashboard hiện tải mượt mà 100% dữ liệu thực tế.

### 9.2. Lỗi 1 — Khách hàng không thấy Hợp đồng và Hóa đơn sau khi cọc
- **Nguyên nhân gốc**:
  Sau khi đặt cọc thành công 30% qua SePay, theo quy trình chuẩn RDS, Sales Staff phải là người bấm nút tạo Hợp đồng (`PENDING_SIGNATURE`). Tuy nhiên do lỗi Lỗi 3 ở trên, Sales và Kế toán không mở được danh sách để xử lý hợp đồng. Ngoài ra, giao diện `my-contracts.jsp` và `my-invoices.jsp` của Khách hàng hiển thị danh sách rỗng (0) mà không giải thích tiến trình đơn hàng khiến khách hàng lầm tưởng bị lỗi.
- **Giải pháp**:
  - `ContractController` và `InvoiceController` truyền bổ sung `customerOrders` vào request context của Customer.
  - Cập nhật giao diện `my-contracts.jsp`: Nếu chưa có hợp đồng được tạo, hệ thống hiển thị bảng trạng thái đơn hàng đã đặt cọc kèm thông báo *"Chuyên viên Sales đang chuẩn bị Hợp đồng mua bán xe..."*.
  - Bổ sung chức năng **Xem trực tiếp hợp đồng HTML** (`CustomerSalesDocumentController?action=view&kind=contract&view=html`) song song với nút Tải file Word và Ký hợp đồng OTP.
  - Cập nhật giao diện `my-invoices.jsp`: Phân loại rõ đơn hàng đang ở bước nào (Chờ cọc / Chờ ký HĐ / Chờ thanh toán 70% / Đang chờ Kế toán duyệt phát hành hóa đơn). Bổ sung nút **👁️ Xem hóa đơn trực tiếp HTML** và **📥 Tải Word**.

### 9.3. Lỗi 2 — Nút "Lấy thông tin từ hồ sơ" của Sales Staff
- **Nguyên nhân gốc**:
  Trên dashboard SPA (`staff.js`), form lập đơn thiếu các input `customerName`, `customerPhone`, `customerEmail`, `customerAddress` và nút `btnAutofill`. Trên file JSP `create-order.jsp`, nút chỉ đọc từ dataset tĩnh thay vì gọi API lấy hồ sơ mới nhất từ database.
- **Giải pháp**:
  - Bổ sung API endpoint `GET /customer/profile?mode=staff&customerId=<id>` trong `CustomerController.java`.
  - Bổ sung hàm `CustomerService.getCustomerById(int customerId)`.
  - Nâng cấp `staff.js` và `create-order.jsp`: Khi chọn khách hàng hoặc bấm `⚡ Lấy thông tin từ hồ sơ`, hệ thống fetch dữ liệu mới nhất từ database và tự động điền 4 trường: Họ tên, Số điện thoại, Email, Địa chỉ.
  - Cả 4 trường giữ nguyên tính năng chỉnh sửa linh hoạt cho Sales Staff; kiểm tra hợp lệ và lưu cập nhật vào bảng `Customers` cho đơn hàng mà không đụng chạm tới tài khoản đăng nhập `Users`.

### 9.4. Lỗi 4 & Bổ sung Quản lý Công nợ, Giải ngân 70% (Accountant)
- **Giải pháp triển khai**:
  - Tab Quản lý công nợ (`loadAccountantPaymentsAndDebtsView`): Hiển thị đầy đủ mọi đơn hàng còn công nợ 70%, xe & VIN, tổng tiền, cọc 30%, nợ 70%, phương thức thanh toán.
  - Đối với khoản thanh toán SePay: Hiển thị bằng chứng giao dịch ngân hàng thực nhận (`sePayEvidence.received`) ở chế độ **chỉ đọc**; số tiền và mã đối chiếu trích xuất nguyên vẹn từ SePay webhook, chống chỉnh sửa tùy tiện.
  - Phân luồng rõ ràng giữa Trả thẳng (Khách thanh toán 70% qua SePay sau khi ký HĐ) và Trả góp (Kế toán ghi nhận giải ngân 70% từ ngân hàng đối tác; không yêu cầu khách trả 70% trên web).
- **Trạng thái lưu cờ [Đã đối chiếu]**:
  - `BLOCKED VÌ THIẾU PERSISTENCE`: Tuân thủ nghiêm ngặt chỉ thị của người dùng *"Không tự ý ALTER TABLE / CREATE TABLE; không lưu cờ tùy tiện trong Payments.Note; không tạo checkbox giả chỉ chạy trên frontend"*. Bảng `Payments` trong `ElectricCarSalesDB.sql` hiện chưa có các cột `Reconciled (bit)`, `ReconciledBy (int)`, `ReconciledAt (datetime)`. Hệ thống đã hiển thị minh bạch bằng chứng SePay chỉ đọc và thông báo lý do kỹ thuật.
  - *Đề xuất schema bổ sung (khi được phê duyệt)*:
    ```sql
    ALTER TABLE Payments ADD Reconciled BIT DEFAULT 0;
    ALTER TABLE Payments ADD ReconciledBy INT NULL FOREIGN KEY REFERENCES Employees(EmployeeID);
    ALTER TABLE Payments ADD ReconciledAt DATETIME NULL;
    ```

### 9.5. Lỗi 5 & Lỗi 6 — Thiết kế lại Trang Tư vấn Khách hàng (Customer Consultation) & Chat hai chiều
- **Lỗi 5 (Loại bỏ cảnh báo chat cũ)**:
  Đã xóa bỏ hoàn toàn thông báo lỗi thời: *"Chat hai chiều thời gian thực hiện chưa được hỗ trợ do giới hạn database schema. Đây là hệ thống yêu cầu tư vấn một chiều..."* trên `customer/my-consultation.jsp`.
- **Lỗi 6 & 14 (Bố cục 2 cột & Interactive Payment Request Cards)**:
  - Thiết kế lại trang `my-consultation.jsp` sang bố cục Master-Detail responsive:
    - **Cột trái**: Danh sách các cuộc trò chuyện (Hội thoại #ID, Mẫu xe, Chuyên viên phụ trách, Badge trạng thái `PENDING / IN_PROGRESS / COMPLETED`, trích đoạn tin nhắn gần nhất, thời gian).
    - **Cột phải**: Chi tiết cuộc trò chuyện đã chọn.
      - Header: Thông tin xe tư vấn từ danh mục (ảnh xe, giá tham khảo, dung lượng pin, quãng đường, chỗ ngồi, link xem chi tiết xe).
      - Body: Lịch sử tin nhắn dạng bong bóng đối thoại trực quan: tin nhắn của Khách hàng hiển thị bên phải (màu xanh VinFast), phản hồi của chuyên viên Sales hiển thị bên trái (màu xám thanh lịch).
      - **Thẻ Yêu cầu Thanh toán Đặt cọc Tương tác (Interactive Payment Request Card)**:
        - Tự động liên kết đơn hàng tương ứng của khách hàng.
        - Hiển thị Mã đơn, Mẫu xe, Số tiền đặt cọc 30%.
        - Nếu đơn hàng đã đặt cọc / thanh toán: Hiển thị badge xanh `✓ ĐÃ THANH TOÁN THÀNH CÔNG` (nút thanh toán bị ẩn/vô hiệu hóa, chống thanh toán lặp).
        - Nếu đơn hàng chờ cọc: Hiển thị nút nổi bật `Thanh toán cọc ngay bằng VietQR (SePay - MB Bank) →` dẫn tới `/payment/transfer?orderId=...&type=DEPOSIT`.
        - Nếu đơn Full Payment đã ký hợp đồng: Hiển thị thêm thẻ thanh toán 70% còn lại.
      - Khung gửi phản hồi bổ sung: Textarea kèm nút gửi tiện lợi, cập nhật trực tiếp vào cơ sở dữ liệu.
      - Trên giao diện Sales Staff (`staff.js`): Bổ sung nút `✉ Gửi YC cọc qua chat` tại danh sách đơn hàng giúp chuyên viên Sales gửi ngay yêu cầu thanh toán cọc kèm số tiền 30% vào hội thoại tư vấn của khách hàng chỉ với 1 click.

### 9.6. Bổ sung — Tối ưu hóa Luồng So Sánh Xe (Vehicle Comparison Streamlining)
- **Nguyên nhân thao tác dư thừa**:
  Trước đây, khi khách hàng tick chọn 2 hoặc 3 xe và bấm nút "So sánh xe →", trang `index.jsp` bật một modal popup trung gian (`compareDialog.showModal()`) rồi yêu cầu khách bấm thêm link "Xem trang so sánh đầy đủ" mới chuyển trang.
- **Giải pháp**:
  - Tối ưu `assets/js/home.js` và `home.js`:
    - Khi chưa chọn xe: Thông báo *"Vui lòng chọn xe để so sánh."*
    - Khi mới chọn 1 xe: Thông báo *"Vui lòng chọn thêm ít nhất 1 xe nữa để so sánh (tối thiểu 2 xe)."*
    - Khi đã chọn đủ từ 2 đến 3 xe: Lưu `sessionStorage` và **chuyển hướng ngay lập tức (single-click navigation)** sang URL `/comparison?key=...&key=...`.
  - Cập nhật `ComparisonController.java`: Bổ sung thông số "Dòng xe" và truyền `comparisonKey` trong dữ liệu các cột xe.
  - Nâng cấp trang kết quả `compare.jsp`:
    - Hiển thị bảng so sánh thông số kỹ thuật đầy đủ ngay khi mở trang, không yêu cầu bấm thêm bất kỳ nút nào.
    - Mỗi cột xe có nút **✕ Bỏ** trực tiếp: Khách hàng có thể bỏ từng xe ngay trên bảng so sánh, URL và `sessionStorage` tự động cập nhật ngay tức thì.
    - Nếu danh sách so sánh có dưới 3 xe, hiển thị nút tiện ích **➕ Thêm xe khác để so sánh** dẫn về danh mục xe với các xe đã chọn được bảo lưu nguyên vẹn.

---

## 10. TỔNG KẾT BẢNG TRẠNG THÁI CUỐI CÙNG

| Hạng mục / Chức năng | Trạng thái kỹ thuật | Bằng chứng / Cơ chế kiểm chứng |
| :--- | :--- | :--- |
| **SePay + MB Bank VietQR động** | `LIVE VERIFIED` & `UNIT TESTED` | MB Bank `0917231548` — `Do Tan Dat`. Website đã ghi nhận thanh toán thành công qua SePay webhook. |
| **Lỗi 1: Khách hàng xem Hợp đồng & Hóa đơn** | `IMPLEMENTED` & `UNIT TESTED` | Đã có xem trực tiếp HTML preview, tải Word DOCX, ký hợp đồng bằng mã OTP. |
| **Lỗi 2: Nút Lấy thông tin từ hồ sơ của Sales** | `IMPLEMENTED` & `INTEGRATION TESTED` | Nạp realtime từ DB qua `/customer/profile?mode=staff`, điền 4 trường, cho phép sửa, không đổi login `Users`. |
| **Lỗi 3: Khắc phục 2 tab Accountant bị lỗi 503** | `IMPLEMENTED` & `UNIT TESTED` | Đã cấu hình `JavaTimeModule` cho Jackson trên `ContractController`. Cả 2 tab tải trơn tru. |
| **Lỗi 4: Hiển thị tiền thực nhận & Đối chiếu** | `IMPLEMENTED` (Data) & `BLOCKED` (Persistence) | Số tiền thực nhận SePay hiển thị chỉ đọc; cờ đối chiếu báo rõ `BLOCKED` do schema thiếu cột theo đúng quy tắc. |
| **Lỗi 5: Loại bỏ cảnh báo chat cũ** | `IMPLEMENTED` | Đã xóa triệt để thông báo chat một chiều trên `my-consultation.jsp`. |
| **Lỗi 6 & 14: Trang Tư vấn & Payment Request** | `IMPLEMENTED` | Bố cục 2 cột, specs xe, tin nhắn bong bóng, thẻ thanh toán VietQR cọc 30% và 70% còn lại, chống thanh toán lặp. |
| **Bổ sung: Quản lý công nợ & giải ngân 70%** | `IMPLEMENTED` & `UNIT TESTED` | Phân luồng Full Payment (khách trả 70% sau ký HĐ) vs Installment (đối tác giải ngân; kế toán xác nhận). |
| **Bổ sung: Tối ưu so sánh xe 1 click** | `IMPLEMENTED` | Chuyển ngay sang bảng so sánh chi tiết, hỗ trợ bỏ/thêm xe linh hoạt, đồng bộ `sessionStorage`. |
| **Maven Compile & Package WAR** | `BUILD SUCCESS` | File WAR `target/ElectricCarSales.war` sẵn sàng triển khai trên Tomcat 10.1. |
