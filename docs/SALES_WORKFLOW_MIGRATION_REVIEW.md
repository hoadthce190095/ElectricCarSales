> **Cập nhật ưu tiên ngày 16/11/2026:** Chủ dự án đã hủy yêu cầu Accountant + Customer cùng duyệt hợp đồng. Schema hiện tại được giữ nguyên; không thực thi migration. Những mô tả về ContractRevisions/ContractDecisions và quy trình duyệt hai bên bên dưới là đề xuất cũ, không còn áp dụng. Luồng đang triển khai là cọc 30% → PENDING_SIGNATURE → Customer đồng ý → OTP đúng user/contract → SIGNED. Xem `SALES_WORKFLOW_STATUS_20261116.md` để biết kết quả hiện tại và các phần chưa hoàn tất.

# Rà soát migration luồng bán xe

Đã đối chiếu docs/SALES_WORKFLOW_IMPLEMENTATION.md với source đang lưu. Các điểm kiểm tra OCB/VA, xác nhận cọc, tổng tiền, ký OTP theo revision, phát hành hóa đơn và bàn giao có trong BankTransferService, SePayWebhookController, ContractsDAO, PaymentDAO, InvoiceDAO và DeliveryDAO. Các tệp vẫn nằm trong working tree hiện tại.

| Phạm vi | Bắt buộc cho chức năng | Rủi ro cần xử lý trước rollout |
|---|---|---|
| SalesConversations, SalesProposals, SalesMessages | Bắt buộc để lưu thành viên, lịch sử chat, đề nghị và quyết định. Chỉ đọc tư vấn hiện có không cần bảng mới. | FK từng cột chưa bảo đảm consultation, khách, nhân viên và order thuộc cùng một giao dịch. Cần kiểm tra trong cùng transaction. Khi từ chối hoặc hết hạn, phải xử lý xe RESERVED mà chỉ trả kho nếu chưa có khoản đã nhận và không có giao dịch đang chờ đối soát. Cần quy tắc cho order cũ. Khóa UNIQUE OrderID hiện chỉ cho một đề nghị mỗi order. |
| ContractRevisions, ContractDecisions | Bắt buộc cho hai bên duyệt cùng một nội dung, OTP và bằng chứng ký. | Snapshot phải bất biến và so khớp hash. Phải ngăn quyết định cũ ảnh hưởng revision tiếp theo. Kiểm tra người duyệt tại server; khóa duy nhất theo vai trò chỉ giữ một quyết định mỗi revision, nên khi cần duyệt lại phải tạo revision mới. Giữ nguyên hợp đồng SIGNED lịch sử; không backfill phê duyệt giả. |
| SePayEvents, PaymentReconciliations | Bắt buộc cho hàng đợi giao dịch lệch và trạng thái đối soát có người thực hiện, thời gian. Hiển thị bằng chứng đã có trong Payments.Note không cần migration. | Rủi ro ghi nhận tiền hai lần khi chuyển nguồn idempotency. Phải kiểm tra và backfill SEPAY_ID, SEPAY_REF, BANKTXN trong lúc tạm dừng đường ghi. Dùng unique index cùng transaction cập nhật payment. Che/tránh lộ số tài khoản, nội dung sao kê và áp dụng thời hạn lưu. Chốt quy trình xử lý/đóng giao dịch lệch trước khi triển khai. |
| SalesDocuments | Bắt buộc cho bản phát hành bất biến và tải lại chính xác bản đã phát hành. Render thử hoặc bản xem trước không cần bảng mới. | Các FK riêng lẻ chưa bảo đảm payment, revision, invoice, order và customer khớp nhau. Chỉ ghi khi đủ điều kiện và kiểm tra hash khi tải. Cần chính sách thu hồi hoặc cấp lại vì UNIQUE OrderID/Kind chỉ cho một bản. Kiểm tra mức tăng backup khi lưu VARBINARY(MAX). |

## Điều kiện trước khi chạy

1. Chia rollout theo nhóm phụ thuộc và thử trên bản sao SQL Server. DDL đề xuất hiện tạo bảng vô điều kiện trong một transaction, không dùng như script chạy lặp. Xác minh đúng database, các FK và các thiết lập SET cho filtered index.
2. Quyết định chính sách chuyển tiếp cho giao dịch cũ, khóa nghiệp vụ khi chuyển sổ đối soát và chụp backup đã kiểm tra khôi phục.
3. Chỉ bật từng tính năng sau khi schema, dữ liệu và code tương ứng đã qua kiểm thử. Khi cần quay lui, tắt luồng mới và giữ lại dữ liệu phát sinh để đối soát.

Chưa thực thi migration. File SQL đề xuất vẫn giữ nguyên.

## Phần ghép vào schema hiện tại

- Màn hình kế toán đọc mã sự kiện, mã sao kê, thời điểm và số tiền từ định dạng đã được webhook ghi. Các trường chỉ phục vụ tra cứu. Không suy diễn trạng thái đã đối soát từ trạng thái đã xác nhận thanh toán.
- Khách đăng nhập có thể tải bản xem trước DOCX thuộc đơn của mình. Biên nhận cần đúng một khoản DEPOSIT đã nhận đủ 30%. Hợp đồng phải PENDING_SIGNATURE, khớp tổng tiền và đã cọc. Hóa đơn phải ISSUED, hợp đồng SIGNED và sổ thanh toán đủ hai khoản. Không tạo hồ sơ phát hành hay cập nhật DB khi tải.
- Chat giao dịch, ghi quyết định đề nghị, duyệt hai bên, hàng đợi giao dịch lệch và lưu đối soát vẫn cần các nhóm bảng nói trên. Không dùng session hoặc cột Note để giả lập các trạng thái đó.

### Cấu hình bản xem trước Word

Đặt ECS_WORD_PREVIEW_DIR trong JVM hoặc môi trường Tomcat, trỏ vào thư mục riêng do quản trị viên kiểm soát. Các liên kết xuất hiện khi có tệp tương ứng. Chỉ đặt vào thư mục này bản DOCX đã được nhóm rà soát và chuyển dữ liệu demo thành trường động. Ba mẫu gốc vẫn là tài liệu nguồn, chưa được đăng ký làm mẫu tải. Không dùng bản xem trước làm mẫu phê duyệt hoặc bản phát hành.

| Tệp cấu hình | Trường bắt buộc ngoài nhãn xem trước |
|---|---|
| deposit-preview.docx | order.id, payment.id, payment.amount, customer.name |
| contract-preview.docx | order.id, contract.number, order.totalAmount, customer.name |
| invoice-preview.docx | order.id, invoice.number, invoice.totalAmount, customer.name |

Dùng trường dạng {{tên.trường}}. Đặt {{preview.label}} thành dòng nhìn thấy trong nội dung chính. Bổ sung tùy mẫu các trường được liệt kê trong câu SELECT của SalesPreviewDAO, như car.vin, car.model, payment.depositReference hoặc finance.partnerName. Chỉ đặt trường khi dữ liệu có mặt cho giao dịch tương ứng. Các trường trống hoặc chưa ánh xạ khiến việc tạo bản xem trước dừng. Không đặt dữ liệu demo, ảnh ký hoặc thông tin pháp nhân chưa được xác minh vào bản dùng để thử với khách. Mẫu hỗ trợ đoạn, bảng và header/footer; để ô nội dung trong các cấu trúc này thay vì text box.

Endpoint tải là GET /customer/word-preview?kind=deposit|contract|invoice&orderId=… . Kiểm tra bằng một đơn thuộc khách, phiên khác và đơn chưa đủ điều kiện. Trước khi triển khai mẫu chính thức, cần render để kiểm tra vị trí nhãn, bố cục và trường Word trên từng trang.

### Kiểm tra tích hợp

Maven offline đã biên dịch và đóng gói WAR. JUnit chạy 33/33 test thành công qua Tomcat loopback và H2 dùng một lần. Bao gồm tải biên nhận DOCX qua WAR, chặn phiên chưa đăng nhập, từ chối đơn không phù hợp, kiểm tra quyền sở hữu và tổng tiền cho cả ba loại, cùng kiểm tra render trường Word và parse SePay. Jasper biên dịch bốn JSP đã sửa với 0 lỗi; JavaScript qua kiểm tra cú pháp. Chưa chạy trên SQL Server/SePay thật và chưa render ba mẫu nguồn thành chứng từ xuất.
