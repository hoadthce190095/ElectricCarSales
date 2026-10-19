# Tham khảo giao diện và chức năng

Ngày tham khảo: 05/10/2026; đồng bộ giao diện theo nhánh `main` ngày 17/10/2026. Project vẫn sử dụng Java Servlet, JDBC và JavaScript thuần. Các nguồn dưới đây dùng để học cách tổ chức chức năng cho bài SWP391.

## Giao diện chung theo main của nhóm

Trang chủ `index.jsp` lấy bố cục VinFast xanh dương từ `origin/main`, commit `69deda1`: header trắng, banner navy, bộ lọc bên trái, lưới xe và thanh so sánh. `preview-main.jsp` forward tới trang chủ để không duy trì hai bản giao diện. Trang chủ so sánh tối đa 3 xe; chức năng so sánh trong không gian làm việc `tan.jsp` vẫn hỗ trợ 2–4 xe.

`assets/css/theme.css` định nghĩa bảng màu và kiểu chữ chung cho trang chủ và `tan.jsp`: xanh dương `#315b9d`, navy `#16365f`, nền sáng `#f7f8fa`, chữ `#17233a`, font Be Vietnam Pro và Manrope. CSS riêng giữ bố cục tương ứng cho danh mục công khai và các màn hình quản trị/báo cáo. Logo VinFast và liên kết **Trang chủ** nối hai phần; nút **Đăng nhập** trên trang chủ đi tới form hiện có tại `tan.jsp?login=1`.

## Shopify: tổng quan và so sánh báo cáo

Nguồn: [Using the Analytics overview dashboard](https://help.shopify.com/en/manual/reports-and-analytics/shopify-reports/overview-dashboard/using-the-overview-dashboard).

Tài liệu hướng dẫn hiển thị các chỉ số dưới dạng thẻ số, biểu đồ, lựa chọn khoảng ngày và so sánh hai kỳ. Project áp dụng vào trang **Tổng quan** và **Báo cáo**:

- Tổng quan hiển thị doanh thu thực thu, giá trị xe đã bán, số xe và giao dịch trong 6 tháng gần nhất, biểu đồ tháng, trạng thái, giao dịch gần đây và tồn kho.
- Báo cáo có bộ lọc theo ngày, mẫu xe, nhân viên; có lựa chọn nhanh tháng, quý, năm hoặc 6 tháng.
- Chọn **Kỳ liền trước** để đối chiếu 4 chỉ số theo cùng số ngày, cùng mẫu xe và nhân viên. Ví dụ kỳ 01–10/10 được so với 21–30/09, không so với cả tháng 09.
- Kỳ trước bằng 0 thì giao diện ghi rõ chưa thể tính tỷ lệ, tránh hiển thị phần trăm vô hạn. Khi không tải được kỳ trước, báo cáo hiện tại vẫn được giữ.

Doanh thu thực thu và giá trị xe đã bán là hai chỉ số khác nhau trong nghiệp vụ của project. Không lấy định nghĩa doanh thu của Shopify để thay thế công thức của showroom.

## Salesforce: danh sách và bộ lọc

Nguồn: [Work with Account List Views](https://trailhead.salesforce.com/content/learn/modules/accounts-list-view-step-by-step/work-with-account-list-views).

Tài liệu trình bày danh sách có thể lọc theo tiêu chí và phản ánh dữ liệu mới sau khi thay đổi. Project áp dụng:

- Tài khoản có từ khóa, vai trò và trạng thái. Lọc và phân trang tại database; giữ bộ lọc khi đổi trang hoặc sửa tài khoản.
- Nhân viên có từ khóa, vai trò và trạng thái. Danh sách nhân viên của showroom được tìm tại trình duyệt, hỗ trợ từ khóa tiếng Việt có dấu hoặc không dấu.
- Khuyến mãi có từ khóa và hiệu lực thực tế: sắp diễn ra, đang áp dụng, hết hạn hoặc đã ngừng. Hiệu lực đối chiếu giờ Việt Nam mỗi lần tải danh sách.
- Nhật ký giữ bộ lọc, xuất CSV và không có thao tác sửa/xóa nội dung đã ghi.

## VinFast: so sánh sản phẩm

Nguồn: [So sánh xe VinFast](https://shop.vinfastauto.com/vn_vi/so-sanh-xe-vinfast).

Trang chính thức cho phép lựa chọn nhiều mẫu xe để đối chiếu các nhóm thông số. Project sử dụng bảng so sánh 2–4 mẫu, có pin, quãng đường, chuẩn đo, số chỗ, công suất, kích thước, giá tham khảo và nguồn. Trường chưa xác định được ghi rõ thay vì tự tạo số liệu. Giá và chính sách thực tế phải đọc tại liên kết nguồn; doanh số và tồn kho trong bài là dữ liệu mô phỏng.

## Cách tổ chức JavaScript để dễ trình bày bài

Các file được tải tuần tự bằng `defer`, sử dụng cùng các hàm dùng chung; không cần cài framework hoặc npm để chạy:

| File               | Trách nhiệm                                                                              |
| ------------------ | ---------------------------------------------------------------------------------------- |
| `tan-common.js`    | Trạng thái trang, gọi API, kiểm tra quyền, bảng, form, dialog, đăng nhập và đổi mật khẩu |
| `tan-admin.js`     | Tài khoản, nhân viên, vai trò, cấu hình, khuyến mãi và nhật ký                           |
| `tan-report.js`    | Tổng quan, báo cáo, biểu đồ, ngày và phép tính so sánh kỳ                                |
| `tan-catalogue.js` | Danh mục, nguồn VinFast, chi tiết xe và bảng so sánh                                     |
| `tan-ai.js`        | Hội thoại, gợi ý nhu cầu và cuộc trò chuyện mới                                          |
| `tan.js`           | Khởi động trang và tạo Admin lần đầu khi được bật                                        |

Phép tính thử khuyến mãi chỉ minh họa giá giảm, giới hạn mức giảm không vượt quá giá xe; không ghi vào đơn hàng. Đổi mật khẩu dùng xác nhận mật khẩu hiện tại, cập nhật phiên và CSRF từ máy chủ. Quyền truy cập luôn được kiểm tra lại ở phía Java.
