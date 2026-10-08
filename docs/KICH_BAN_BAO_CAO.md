# Kịch bản trình bày phần của Phạm Trần Phúc Tấn

Ứng dụng Java Servlet/JSP chạy trên Tomcat 10.1. Giao diện phần Tấn mở tại `http://localhost:8081/ElectricCarSales/tan.html` khi dùng script cổng 8081; cấu hình IntelliJ của bạn có thể dùng cổng khác. Trang gốc vẫn là `index.jsp` của nhóm.

Danh mục thông số và giá tham khảo lấy từ nguồn công khai của VinFast; xem `NGUON_DU_LIEU_VINFAST.md`. **Toàn bộ khách hàng, nhân viên, tồn kho, đơn hàng, khoản thanh toán, trả góp và khuyến mãi có tiền tố DEMO là dữ liệu giả lập phục vụ bài báo cáo. Các con số bán hàng không phải doanh thu hay kết quả kinh doanh của VinFast.**

## 1. Chuẩn bị lần đầu

1. Trong SQL Server Management Studio, chạy duy nhất `ElectricCarSalesDB.sql` ở thư mục gốc cho cả database mới và database đã có. File này tạo `ElectricCarSalesDB` nếu thiếu, bổ sung schema chung, rồi nạp danh mục VinFast và 30 xe kho DEMO, tiếp theo là bộ báo cáo giả lập gồm 9 đơn cùng thanh toán, trả góp và khuyến mãi. Dữ liệu đã có được giữ nguyên và chạy lại không thêm bản ghi trùng. Nếu phát hiện đơn hàng, thanh toán hoặc trả góp ngoài bộ DEMO, script thông báo bằng `PRINT` và bỏ qua phần nạp báo cáo DEMO; schema và danh mục vẫn chạy. Kịch bản số liệu dưới đây áp dụng khi bộ báo cáo DEMO đã được nạp. Script không tạo tài khoản hay mật khẩu Admin.
2. Chạy `docs/Chay-Web.ps1 -Port 8081` hoặc cấu hình Tomcat IntelliJ (VM option `-Dapp.adminSetup.enabled=true` để bật tạo Admin lần đầu tại localhost).
3. Mở `/ElectricCarSales/tan.html`, chọn **Tạo Admin đầu tiên**, tự đặt mật khẩu rồi đăng nhập. Có thể dùng `docs/Tao-Admin.ps1` thay màn hình này; không có mật khẩu Admin mặc định trong mã nguồn hoặc file ZIP.
4. Trước giờ báo cáo, mở phần Báo cáo, chọn từ đầu tháng cách hiện tại 5 tháng đến hôm nay, nhấn Xem báo cáo.

Ngày giả lập được tính tương đối theo tháng tại **lần nạp đầu tiên**. Chạy lại script không thêm bản ghi trùng và không thay ngày/số tiền của đơn đã nạp. Nếu trình bày vào thời điểm sau đó, chọn khoảng ngày bao phủ 6 tháng của bộ dữ liệu hiện có. Không xóa dữ liệu thật để làm mới demo.

## 2. Giới thiệu cấu trúc code

Mở package `vn.edu.fpt.electriccarsales` trong IntelliJ. Giải thích luồng trình duyệt → Controller → Service → Repository → SQL Server. `entities` chứa dữ liệu của bảng; `dto` chứa dữ liệu nhận/trả API; `security` kiểm tra đăng nhập, phân quyền và CSRF. Các lớp dùng Java thông thường, vòng lặp, điều kiện và JDBC để dễ đọc, dễ giải thích.

Các lớp chính của phần được giao:

| Chức năng | Controller               | Service               | Repository               |
| --------- | ------------------------ | --------------------- | ------------------------ |
| Quản trị  | AdministrationController | AdministrationService | AdministrationRepository |
| Báo cáo   | ReportController         | ReportService         | ReportRepository         |
| Tư vấn xe | AIController             | AIService             | AIRepository             |

## 3. Quản trị tài khoản và phân quyền

Mở Tài khoản: các tài khoản `demo_customer_01`…`06` và `demo_employee_01`…`03` đều **INACTIVE**, không có mật khẩu hợp lệ để đăng nhập. Chúng chỉ làm dữ liệu tham chiếu cho báo cáo. Không dùng những tài khoản này để đăng nhập hoặc đổi thành Admin.

Lọc vai trò CUSTOMER và trạng thái INACTIVE để thấy 6 tài khoản khách hàng mô phỏng; xóa bộ lọc, chọn SALES để xem nhân viên. Màn hình Nhân viên hỗ trợ tìm tên không dấu, vai trò và trạng thái.

Tạo một tài khoản nhân viên mới với dữ liệu do bạn tự nhập. Chọn vai trò SALES, đặt mật khẩu đủ 10 ký tự, lưu. Mở Nhân viên để kiểm tra họ tên và thông tin đã tạo. Đổi trạng thái thành INACTIVE để minh họa khóa tài khoản. Có thể đăng xuất Admin và đăng nhập một tài khoản nhân viên riêng để chứng minh những mục không đủ quyền sẽ bị ẩn hoặc bị API từ chối.

Mở Phân quyền, tạo vai trò mới hoặc điều chỉnh quyền của vai trò dùng thử. Giải thích: hệ thống kiểm tra quyền ở máy chủ; đổi giao diện hoặc gọi URL trực tiếp không tự cấp quyền. Admin cuối cùng không được tự khóa để tránh mất quyền quản trị.

## 4. Khuyến mãi và nhật ký

Mở Khuyến mãi. Bộ dữ liệu có một ưu đãi DEMO đang hoạt động giảm 5% và một chương trình DEMO đã kết thúc. Đây là chương trình bài tập, không phải chính sách của VinFast; ưu đãi không tự thay đổi giá xe hay các đơn đã nạp.

Tạo hoặc sửa khuyến mãi dùng thử, chọn loại giảm tiền hoặc phần trăm, kiểm tra khoảng ngày. Vô hiệu hóa để minh họa giữ lịch sử. Sau đó mở System Log, lọc hành động vừa thực hiện. Nhật ký chỉ được thêm, không có chức năng sửa/xóa lịch sử.

Lọc **Đang áp dụng** hoặc **Đã hết hạn** để phân biệt trạng thái lưu và hiệu lực theo ngày. Chọn **Tính thử**, nhập giá xe để minh họa mức giảm; giải thích phép tính không tạo đơn hàng.

## 5. Báo cáo với số liệu có thể giải thích

Sau khi đăng nhập bằng Admin hoặc tài khoản có VIEW_REPORTS, trang **Tổng quan** hiển thị 4 chỉ số, biểu đồ, giao dịch gần đây và tồn kho. Mở **Báo cáo**, chọn **Kỳ liền trước** rồi xem bảng đối chiếu. Ví dụ 01–10/10 được so với 21–30/09; nếu kỳ trước bằng 0 thì không tính tỷ lệ phần trăm.

Bộ DEMO có 9 đơn: 4 COMPLETED, 2 PAID, 1 DEPOSITED, 1 PROCESSING, 1 CANCELLED. Sáu xe được tính là đã bán, trải qua 6 tháng. Mỗi đơn đã bán có hai khoản thu hoàn tất (đặt cọc và phần còn lại), giúp chứng minh báo cáo không đếm trùng xe khi một đơn có nhiều thanh toán. Các đơn chờ có tiền đặt cọc và khoản thanh toán PENDING; khoản PENDING không tính vào tiền thực thu. Đơn CANCELLED có thể hiện trong danh sách trạng thái nhưng không tính doanh số bán.

| Chỉ tiêu         | Cách giải thích khi báo cáo                                                                                             |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------- |
| Tiền thực thu    | Tổng Payments trạng thái COMPLETED của đơn hợp lệ, theo ngày thanh toán. Có thể gồm tiền đặt cọc của đơn chưa bán xong. |
| Giá trị bán hàng | Tổng giá trị đơn PAID/COMPLETED, theo ngày tạo đơn; không phải lợi nhuận.                                               |
| Số xe đã bán     | Đếm CarID duy nhất trong đơn PAID/COMPLETED.                                                                            |
| Chưa thu         | Giá trị đơn hợp lệ trừ các khoản Payments đã hoàn tất, chặn số âm.                                                      |
| Thu trả góp      | Khoản InstallmentPayments hoàn tất, trình bày riêng và không cộng trùng với tiền showroom đã nhận từ bên tài chính.     |
| Quá hạn trả góp  | Phần còn thiếu của những kỳ đã qua ngày đến hạn; có một kỳ DEMO mới trả một nửa.                                        |

Đơn VF 7 Eco dùng kịch bản trả góp: bên tài chính giả lập chuyển phần giá xe còn lại cho showroom, sau đó người mua trả theo lịch riêng 12 tháng, lãi suất giả lập 0%. Lịch này phục vụ kiểm tra cách tính, không phải gói vay thực tế. Kỳ đầu đã trả đủ, kỳ thứ hai trả một nửa, những kỳ còn lại chưa trả. Các số tiền kỳ cuối điều chỉnh phần lẻ để tổng gốc bằng đúng số tiền vay.

Đổi bộ lọc dòng xe hoặc nhân viên để chứng minh dữ liệu thay đổi. Xem biểu đồ theo tháng, bảng doanh số theo mẫu xe, doanh số theo nhân viên, trạng thái đơn/thanh toán và tồn kho. Tồn kho là ảnh chụp trạng thái hiện tại nên không phụ thuộc khoảng ngày chọn của doanh số.

Xuất CSV để mở bằng Excel, xuất XLSX để xem các sheet, rồi dùng In/PDF để lưu bản trình bày. Đối chiếu một đơn trong bảng chi tiết với khoản đặt cọc/phần còn lại trong database. Khi báo cáo, luôn nhắc đây là dữ liệu mô phỏng.

## 6. Trợ lý tư vấn sản phẩm

Thử các câu:

- “Gia đình 5 người, ngân sách 700 triệu, muốn xe đi làm hằng ngày.”
- “So sánh VF 6 Eco và VF 6 Plus.”
- “VF 9 có mấy chỗ ngồi, quãng đường tham khảo bao nhiêu?”
- “Thanh toán giúp tôi” hoặc “Tạo hợp đồng mua xe” để minh họa giới hạn nghiệp vụ.

Trợ lý đọc danh mục sản phẩm để gợi ý xe, trả lời thông số và so sánh. Bản hiện tại dùng luật Java và dữ liệu trong SQL Server, không gọi mô hình AI bên ngoài. Trợ lý không thực hiện thanh toán, không tạo hợp đồng/đơn hàng, không đọc báo cáo tài chính hay xác nhận mua xe. Quãng đường công bố có chuẩn đo khác nhau; đối chiếu ghi chú/nguồn của từng mẫu trước khi so sánh.

Thử “Gia đình 6 người, ngân sách 1,6 tỷ” để gợi ý dòng 7 chỗ. Hỏi tiếp “thông số xe đó”, rồi nhấn **Cuộc trò chuyện mới** và hỏi lại để minh họa ngữ cảnh được xóa.

## 7. Mật khẩu cá nhân

Chọn tên tài khoản ở góc trên → **Đổi mật khẩu**. Dùng tài khoản thử do bạn tự tạo, nhập mật khẩu hiện tại và mật khẩu mới hai lần. Phiên hiện tại tiếp tục đăng nhập với CSRF mới; trình duyệt khác phải đăng nhập lại. System Log có `PASSWORD_CHANGE` nhưng không có mật khẩu. Khi trình bày, tránh để lộ mật khẩu trên màn hình.

## 8. Câu trả lời ngắn cho phần hỏi đáp

- **Vì sao Jakarta bị báo thiếu package?** Project cần dependency `jakarta.servlet-api` và Tomcat 10.1; tải lại Maven trong IntelliJ và chạy artifact WAR đúng cấu hình.
- **Có cần Internet khi chạy?** Backend/AI theo luật không cần API bên ngoài. Hình/đường dẫn nguồn xe được lấy từ website VinFast có thể cần Internet khi xem.
- **Mật khẩu lưu thế nào?** PBKDF2 với salt riêng; không lưu mật khẩu thô. Tài khoản DEMO không có hash đăng nhập hợp lệ.
- **Phân quyền ở đâu?** Filter kiểm tra session, trạng thái tài khoản và quyền ở server trước khi xử lý API.
- **Báo cáo có phải số liệu VinFast thật?** Không. Thông tin sản phẩm có nguồn VinFast; giao dịch và số liệu kinh doanh đều giả lập cho bài tập.
- **Phần chưa tích hợp của nhóm?** Phần Tấn chạy ở `tan.html`, bên cạnh trang chủ và mã đã có của nhóm. Báo cáo đọc các bảng chung; bán hàng/thanh toán/giao xe vẫn do module của thành viên phụ trách. Khi merge cần thống nhất đăng nhập/session, quyền và schema dữ liệu.
