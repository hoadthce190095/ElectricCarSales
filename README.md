# ElectricCarSales

Group project for SWP391 - Electric Car Sales System.

## Phần của Phạm Trần Phúc Tấn

Nhánh `feature/phuc-tan-admin-report-ai` bổ sung Administration, Reporting và AI Assistant trên Java 24, Jakarta Servlet 6, JDBC/SQL Server và Tomcat 10.1. Mã Java của phần này nằm trong package `vn.edu.fpt.electriccarsales`.

- Quản trị tài khoản/nhân viên, vai trò/quyền, khuyến mãi, System Log và cấu hình vận hành.
- Báo cáo doanh số, tiền thực thu, trạng thái giao dịch, tồn kho và công nợ; xuất CSV/XLSX và In/Lưu PDF.
- Tư vấn xe, thông số, so sánh và gợi ý từ catalogue VinFast bằng luật Java (`CATALOG_RULES`). Trợ lý chỉ đọc dữ liệu sản phẩm; không xử lý thanh toán hoặc tạo hợp đồng.
- Đăng nhập, đổi mật khẩu, CSRF và tạo Admin đầu tiên cục bộ để chạy các màn hình trên.

Trang riêng: [localhost:8080/ElectricCarSales/tan.html](http://localhost:8080/ElectricCarSales/tan.html). Nếu dùng script cổng 8081, mở [localhost:8081/ElectricCarSales/tan.html](http://localhost:8081/ElectricCarSales/tan.html). Trang gốc `/ElectricCarSales/` vẫn dùng `index.jsp` của nhóm.

## Cài đặt và kiểm tra

1. Mở `ElectricCarSales/pom.xml` trong IntelliJ, chọn JDK 24 và tải lại Maven.
2. Chuẩn bị SQL Server theo [hướng dẫn IntelliJ](docs/HUONG_DAN_INTELLIJ.md). Chưa có DB thì chạy `database/00-create-database.sql`; sau đó chạy `database/01-tan-modules.sql`. Hai script `02` và `03` dành cho catalogue và báo cáo DEMO tùy chọn.
3. Điền kết nối riêng qua biến môi trường `DB_URL`, `DB_USER`, `DB_PASSWORD`, hoặc sao chép `db.properties.example` thành `db.properties`. File riêng này được bỏ qua bởi Git và loại khỏi WAR.
4. Build tại thư mục gốc:

```powershell
mvn -f ElectricCarSales/pom.xml clean verify
node ElectricCarSales/src/test/javascript/frontend-checks.cjs
```

Nhánh này đã chạy `formatter:format clean verify`: **90 kiểm thử Java đạt, 0 lỗi**; **21 kiểm tra JavaScript đạt**. Kiểm thử WAR xác nhận trang chủ của nhóm, trang `tan.html`, assets/API của Tấn và phạm vi filter. Xem chi tiết trong [KET_QUA_KIEM_TRA.md](docs/KET_QUA_KIEM_TRA.md). Node chỉ dùng kiểm tra/format; website chạy bằng Java và Tomcat. Tạo Admin đầu tiên theo hướng dẫn và tự đặt mật khẩu trên máy.

Xem thêm [cấu trúc mã nguồn](docs/CAU_TRUC_MA_NGUON.md), [giải thích code](docs/GIAI_THICH_CODE.md), [kịch bản báo cáo](docs/KICH_BAN_BAO_CAO.md), [nguồn VinFast](docs/NGUON_DU_LIEU_VINFAST.md) và [quy tắc format](docs/FORMAT_CODE.md). Giao dịch, tồn kho và số liệu có nhãn DEMO phục vụ bài tập; không phải kết quả kinh doanh của VinFast.

## Phạm vi nhánh và ghép với nhóm

Push phần này lên `feature/phuc-tan-admin-report-ai`; nhánh `main` chỉ thay đổi khi nhóm chủ động merge. Các lớp Java/JSP/CSS/JS, file SQL gốc và trang chủ đã có trên `main` được giữ nguyên. POM giữ `groupId` của nhóm và bổ sung thư viện/plugin cần cho phần của Tấn.

`SecurityFilter` chỉ áp dụng cho `/api/auth`, `/api/setup`, `/api/ai`, `/api/reports` và `/api/admin/*`. Khi ghép các module, thống nhất đăng nhập/session, định dạng mật khẩu, quyền vai trò và schema Users/Roles/xe/giao dịch với các thành viên. Báo cáo đọc dữ liệu Order/Payment/Inventory chung; không thực hiện nghiệp vụ bán hàng hoặc thanh toán thay module của nhóm.
