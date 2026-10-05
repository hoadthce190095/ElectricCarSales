# Cách đọc và giải thích code

## 1. Bắt đầu từ package

`controllers` nhận yêu cầu từ giao diện; `services` kiểm tra nghiệp vụ; `repositories` truy vấn SQL; `entities` chứa dữ liệu nghiệp vụ; `dto` chứa dữ liệu gửi/nhận qua API. Các class dữ liệu dùng thuộc tính `private`, constructor, getter và setter giống dự án mẫu.

Đọc một chức năng theo thứ tự Controller → Service → Repository. Không cần đọc toàn bộ class trước khi theo dõi một chức năng.

## 2. Ví dụ tạo nhân viên

1. Giao diện gửi JSON bằng POST `/api/admin/users`.
2. `AdministrationController.createData()` đọc JSON thành `UserRequest`, lấy ID tài khoản đang đăng nhập và gọi `AdministrationService.createUser()`.
3. Service kiểm tra tên đăng nhập, email, điện thoại, vai trò, trạng thái và mật khẩu. Mật khẩu được băm bởi `PasswordHasher`.
4. Service mở kết nối, bắt đầu transaction, kiểm tra người thao tác có quyền quản lý tài khoản.
5. `AdministrationRepository.createUser()` dùng PreparedStatement để INSERT Users. Service lưu thêm Employees và System Log.
6. Khi mọi bước thành công, `connection.commit()` lưu dữ liệu. Khi có lỗi, `rollbackTransaction()` hủy toàn bộ để không có tài khoản thiếu nhân viên hoặc thiếu nhật ký.
7. Controller trả thông tin tài khoản đã tạo. Response không chứa mật khẩu hoặc hash.

`UserRequest` chứa mật khẩu đầu vào, còn `User` chứa dữ liệu được trả cho giao diện. Đây là lý do dùng hai class riêng.

## 3. Khuyến mãi

`savePromotion()` kiểm tra loại giảm giá, số tiền/phần trăm, ngày bắt đầu/kết thúc và danh sách mẫu xe. Vòng lặp tìm mẫu xe và kiểm tra mã được viết trực tiếp. Repository lưu Promotions và PromotionCars trong cùng transaction.

`deactivatePromotion()` đổi trạng thái thành INACTIVE. Giữ lại bản ghi giúp các đơn hàng cũ và nhật ký vẫn tham chiếu được khuyến mãi.

Giao diện tính trạng thái hiệu lực từ trạng thái lưu trong DB và ngày bắt đầu/kết thúc: chưa bắt đầu, đang áp dụng, đã hết hạn hoặc đã ngừng. Nút tính thử chỉ hiển thị số tiền giảm và giá dự kiến, không tạo đơn hoặc sửa giá xe. Giá sau giảm không âm.

## 4. Báo cáo

`ReportController` làm lần lượt: đọc bộ lọc → kiểm tra định dạng/khoảng ngày → gọi service → ghi log → trả JSON hoặc file.

`ReportRepository` lấy đơn hàng, xe trong đơn và thanh toán riêng. `ReportService` duyệt các danh sách bằng vòng lặp để cộng tổng và gom theo tháng, mẫu xe, nhân viên.

- Tiền thực thu cộng thanh toán thành công theo PaymentDate.
- Giá trị bán hàng và số xe dùng đơn hợp lệ theo OrderDate.
- `Set<Integer>` giữ mã xe đã đếm; một CarID không được tính nhiều lần.
- `BigDecimal` tính tiền trong nghiệp vụ, tránh sai số khi cộng số thập phân bằng double.
- Tồn kho là trạng thái hiện tại; trả góp trình bày riêng để tránh cộng trùng với Payments.

`ReportXlsx` dùng Apache POI: tạo workbook → tạo sheet → tạo row → tạo cell → ghi file. Tiền được xuất thành ô số; dữ liệu tên khách hàng được xuất thành ô văn bản. Các phép tính trước khi xuất vẫn dùng BigDecimal.

Trang Tổng quan dùng cùng API báo cáo, nên tài khoản cần quyền `VIEW_REPORTS`. Khi chọn so sánh, `tan-report.js` tính kỳ liền trước có cùng số ngày rồi gọi API thêm một lần. Hai khoảng không chồng ngày. Nếu giá trị kỳ trước bằng 0, giao diện không chia phần trăm cho 0; nếu API kỳ trước lỗi, báo cáo chính vẫn giữ được.

## 5. Trợ lý xe

`AIController` nhận câu hỏi và lưu ngữ cảnh trong session. `AIService` xử lý theo thứ tự: kiểm tra yêu cầu bị cấm → phân tích ngân sách/số chỗ/tầm hoạt động → đọc danh mục → lọc và trả tư vấn/so sánh/gợi ý.

Chế độ `CATALOG_RULES` sử dụng điều kiện và dữ liệu SQL; không gọi mô hình ngôn ngữ bên ngoài. `AIRepository` chỉ đọc dữ liệu sản phẩm. Câu hỏi thanh toán, hợp đồng, đơn hàng hoặc tài chính bị từ chối trước khi đọc danh mục.

Nếu câu hỏi ghi “gia đình 6 người”, service dùng 6 làm số chỗ tối thiểu. Số chỗ nhập trong form hoặc ghi rõ trong câu hỏi được ưu tiên. Hỏi dòng “VF 9” trả các phiên bản trong dòng, còn hỏi “VF 9 Plus” chỉ lấy phiên bản Plus. Các tên xe phải khớp ranh giới từ để tránh nhận nhầm VF 90 thành VF 9.

Nút **Cuộc trò chuyện mới** gửi POST `/api/ai?action=reset` kèm CSRF. Controller xóa ngữ cảnh xe trong session và tạo mã cuộc trò chuyện mới; các câu như “xe đó” sẽ không dùng lại lựa chọn cũ. Đăng nhập và cuộc tư vấn trên trình duyệt khác không bị thay đổi.

## 6. Các lớp hỗ trợ cần biết

| Class / thành phần    | Mục đích                                                                    |
| --------------------- | --------------------------------------------------------------------------- |
| SecurityFilter        | Kiểm tra phiên, trạng thái tài khoản, quyền và CSRF trước Controller        |
| SessionUser           | Giữ ID, tên đăng nhập, vai trò trong session                                |
| AuthorizationService  | Quản lý vai trò và danh sách quyền                                          |
| PasswordHasher        | Băm/kiểm tra mật khẩu PBKDF2                                                |
| PasswordChangeService | Kiểm tra mật khẩu cũ, cập nhật mật khẩu mới và ghi nhật ký cùng transaction |
| PasswordSession       | Nhận biết mật khẩu trong DB đã thay đổi để hết hiệu lực phiên đăng nhập cũ  |
| JsonServlet           | Đọc/ghi JSON và trả lỗi HTTP chung                                          |
| Database              | Mở kết nối từ cấu hình DB\_\*                                               |
| App                   | Kiểm tra kết nối DB; Tomcat khởi động website                               |
| BootstrapAdmin        | Tạo Admin đầu tiên bằng mật khẩu tự nhập trên máy                           |

## 7. Đổi mật khẩu và lọc tài khoản

Đổi mật khẩu đi qua `AuthenticationController` → `PasswordChangeService` → `AuthenticationRepository`. Service kiểm tra tài khoản ACTIVE, mật khẩu hiện tại, mật khẩu mới 10–128 ký tự, xác nhận và điều kiện khác mật khẩu cũ. Cập nhật bằng PreparedStatement, ghi `PASSWORD_CHANGE` rồi commit; ghi log lỗi thì rollback. Không ghi mật khẩu vào log hoặc JSON trả về.

Sau khi đổi, controller tạo session và CSRF mới. Session giữ dấu vân tay của hash trong DB. Mỗi yêu cầu được bảo vệ kiểm tra lại dấu vân tay; phiên từ trình duyệt khác hoặc phiên trước khi Admin đặt lại mật khẩu sẽ hết hiệu lực.

Danh sách tài khoản gửi `q`, `roleId`, `statusId`, `page`. Service kiểm tra vai trò/trạng thái tồn tại; repository ghép điều kiện cố định và dùng tham số PreparedStatement. Câu đếm tổng và câu lấy dữ liệu dùng cùng bộ lọc để phân trang 20 dòng đúng kết quả. Không chọn bộ lọc nghĩa là xem tất cả trong phạm vi quyền quản lý.

## 8. Các file giao diện

`tan.html` tải script bằng `defer`, theo thứ tự dưới đây. Các file dùng JavaScript thông thường với hàm và biến dùng chung, không cần npm hoặc framework.

| File             | Nội dung                                                         |
| ---------------- | ---------------------------------------------------------------- |
| tan-common.js    | Gọi API, session/CSRF, thông báo, form đăng nhập và đổi mật khẩu |
| tan-admin.js     | Tài khoản, nhân viên, vai trò, khuyến mãi, nhật ký, cấu hình     |
| tan-report.js    | Tổng quan, bộ lọc báo cáo, so sánh kỳ và xuất file               |
| tan-catalogue.js | Danh mục, chi tiết và so sánh xe                                 |
| tan-ai.js        | Gửi câu hỏi và hiển thị tư vấn                                   |
| tan.js           | Gắn sự kiện và khởi động trang                                   |

## 9. Kiểm tra khi chỉnh bài

Chạy `mvn clean verify` trong `ElectricCarSales`. Các test dùng database H2 riêng và Tomcat để kiểm tra nghiệp vụ, API và WAR. Sau đó chạy cấu hình **Tomcat 10.1.60** trong IntelliJ, mở `/ElectricCarSales/tan.html` và thử với dữ liệu thật của nhóm.

Nếu sửa tên thuộc tính DTO, cần kiểm tra file `assets/tan-*.js` của chức năng tương ứng: giao diện sử dụng tên trường JSON từ getter. Nếu sửa cách tính báo cáo, đối chiếu ReportServiceTest trước khi thay đổi để giữ quy tắc không đếm trùng xe và tiền.
