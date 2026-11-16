> **Cập nhật ưu tiên ngày 16/11/2026:** Chủ dự án đã hủy yêu cầu Accountant + Customer cùng duyệt hợp đồng. Schema hiện tại được giữ nguyên; không thực thi migration. Những mô tả về ContractRevisions/ContractDecisions và quy trình duyệt hai bên bên dưới là đề xuất cũ, không còn áp dụng. Luồng đang triển khai là cọc 30% → PENDING_SIGNATURE → Customer đồng ý → OTP đúng user/contract → SIGNED. Xem `SALES_WORKFLOW_STATUS_20261116.md` để biết kết quả hiện tại và các phần chưa hoàn tất.

# Triển khai luồng bán xe và thanh toán

## Audit và phụ thuộc

| Nghiệp vụ | API và dữ liệu | Thay đổi |
|---|---|---|
| Đơn và kho | /orders → SalesOrderService → Orders, OrderDetails, Cars | Luồng hiện hữu khóa xe khi lập đơn và đưa về RESERVED. Xe bị loại khỏi danh sách có thể bán ngay khi giữ chỗ. Xác nhận cọc chuyển xe sang DEPOSITED, không thay số lượng vật lý và không ghi EXPORT. Chưa có đề nghị qua chat hay quyết định chấp nhận/từ chối. |
| Thanh toán | /payment/create, /payment/check, /webhook/sepay, /staff/payment-review → BankTransferService | QR mới dùng VA OCB gắn với payment. Giữ định dạng mã ECS. Xác nhận cọc cập nhật Payment, Order, Car và lịch sử trong cùng transaction. Kế toán dùng chứng từ cho tiền mặt và giải ngân. Danh sách khoản thành công chỉ đọc. |
| Công nợ | Payments, Orders, FinancialCompanies | Khoản 70% yêu cầu SIGNED. Đơn trả góp chỉ ghi nhận 70% từ đối tác. Đơn trả thẳng dùng tiền mặt hoặc QR. |
| Hợp đồng | /contract → ContractsService, ContractsDAO và OTPVerifications | Sales chỉ tạo nháp khi cọc thực nhận đủ 30%. Backend yêu cầu cùng một phiên bản được hai bên duyệt. Tiêu thụ OTP, ghi bằng chứng và đặt SIGNED cùng transaction. Chưa có màn hình tạo phiên bản, duyệt hoặc từ chối. |
| Hóa đơn và bàn giao | /invoice, /delivery → InvoiceDAO, DeliveryDAO, PaymentDAO | Kiểm tra 30%, 70% và tổng tiền thực nhận, công nợ, Signed và invoice ISSUED. Xe chỉ chuyển từ SOLD sang DELIVERED trong transaction có EXPORT. |
| JSON kế toán | /contract?format=json gồm dữ liệu đối tác, /invoice, /orders và /staff/payment-review | Sửa nhánh hết phiên trả redirect HTML và nhánh lỗi HTML. Serialize hợp đồng/hóa đơn trước khi ghi body. Bỏ truy vấn đối tác lặp trong ContractController. Chưa xác nhận từng nguyên nhân trên Tomcat/SQL Server thật. |

Duyệt của Accountant trước khi ký là phần mở rộng so với RDS 9.2. RDS mô tả Accountant xác nhận cọc. Yêu cầu mới chuyển giao dịch OCB khớp sang SePay và dành thao tác xác nhận bàn giao cho Operations. Schema gốc không có registry sự kiện SePay, bản duyệt, hội thoại giao dịch hay chứng từ bất biến. SQL seed dùng FAILD.

## Cấu hình Tomcat và kiểm thử SePay

1. Giữ SEPAY_WEBHOOK_KEY trong cấu hình Tomcat. Webhook dùng Authorization: Apikey … . Kiểm tra ECS_OCB_ACCOUNT, ECS_OCB_ACCOUNT_NAME và khai báo ECS_OCB_VA là VA chính thức. Giữ ECS_OCB_VA_VERIFIED=false trước khi xác minh. ECS_MB_ACCOUNT chỉ dành cho webhook của khoản MB lịch sử đang chờ.
2. Liên kết OCB và tạo VA qua SePay. Trong môi trường thử nghiệm, so sánh VA quét từ QR, gateway OCB, accountNumber của tài khoản chính, subAccount của VA và referenceCode trên sao kê. Nếu cấu trúc webhook khác, giữ cờ false để đối chiếu lại. Bật cờ verified trên môi trường cần sử dụng chỉ sau khi đã kiểm tra.
3. Trỏ SePay đến /<context>/webhook/sepay. Mã hiện tại là ECS{OrderID}{D|R}{PaymentID}. Có thể cấu hình tiền tố ECS và hậu tố chữ số/chữ cái dài 3–21 ký tự. Giữ webhook sự kiện thiếu mã để có thể phát hiện khoản cần xử lý.
4. Kiểm tra /payment/check?paymentId=… bằng phiên của chính khách hàng. Đối chiếu Payment, Order, History và Cars. Replay cùng event không làm tăng số tiền. Lỗi SQL trả 503 để SePay retry.
5. Dấu vết giao dịch khớp hiện lưu trong Payments.Note. Giao dịch không khớp cần xem log/SePay. Chưa có hàng đợi sai lệch và chức năng lưu người/thời điểm đối chiếu. Không đặt cờ verified trong môi trường triển khai khi còn thiếu VA hoặc chưa kiểm thử nguồn giao dịch.

Tham khảo [kết nối OCB](https://docs.sepay.vn/ket-noi-ocb-api.html), [webhook](https://developer.sepay.vn/vi/sepay-webhooks/tich-hop-webhook), [mã thanh toán](https://developer.sepay.vn/vi/sepay-webhooks/cau-hinh-ma-thanh-toan) và [VietQR](https://developer.sepay.vn/vi/tien-ich-khac/tao-qr-code).

## Migration và hoàn tác

SalesWorkflowMigration.proposal.sql là bản review, chưa được áp dụng. Chạy thử trên bản sao SQL Server sau khi nhóm xác nhận quy trình.

- SalesConversations dùng cùng ConsultationRequests và thành viên đã phân công. SalesProposals lưu quyết định, người quyết định, lý do, thời gian và phương thức cọc. SalesMessages hỗ trợ tin nhắn người dùng và sự kiện backend. Ứng dụng cần khóa cả đề nghị, order và xe, bảo vệ quyền thành viên và chặn tạo payment trước khi chấp nhận. Cần chính sách chuyển tiếp cho đơn cũ.
- ContractRevisions lưu bản DOCX bất biến và SHA-256. ContractDecisions lưu quyết định theo revision và vai trò. Khi sửa, tạo revision mới và làm mất hiệu lực màn hình duyệt cũ. Không chuyển lịch sử thành bản đã duyệt. Cần triển khai tạo revision, xem bản đầy đủ và ghi hai quyết định có kiểm tra quyền.
- SePayEvents dùng EventID và khóa duy nhất cho mã sao kê đã gắn payment. PaymentReconciliations lưu riêng người, ngày và mã chứng từ đối chiếu. Khi đổi đường ghi, dừng đầu vào, đối chiếu SEPAY_ID/BANKTXN lịch sử và backfill có kiểm tra trùng trước khi bật lại.
- SalesDocuments lưu mỗi loại chứng từ một lần trên một order. Luồng download phải kiểm tra quyền sở hữu bằng server, dùng dữ liệu bất biến và không nhận đường dẫn file từ người dùng.
- Hoàn tác bằng tắt phần code gọi schema mới và giữ bảng cùng dữ liệu để đối soát. Trên DB thử nghiệm rỗng, có thể tháo sau khi xác nhận không có dữ liệu, theo thứ tự SalesDocuments, PaymentReconciliations, SePayEvents, ContractDecisions, ContractRevisions, SalesMessages, SalesProposals, SalesConversations. Với dữ liệu thật, giữ bảng hoặc phục hồi bản sao có đối soát các giao dịch sau thời điểm sao lưu.

## Mẫu Word và điều kiện phát hành

Thư mục đề xuất cho bản mẫu được duyệt là WEB-INF/templates/sales hoặc thư mục ngoài webroot do Tomcat quản lý. Chưa đóng gói hoặc triển khai endpoint xem/tải DOCX/PDF.

| Mẫu | Nguồn thông tin và trạng thái |
|---|---|
| Mau_Hop_Dong_Mua_Ban_O_To_Dien_ECS.docx | Dùng các nhóm trường contract, order, payment, employee, customer, car, finance, seller, handover, warranty, signature. Lấy từ Contracts, Orders, Payments, Employees, Customers, OrderDetails, Cars, FinancialCompanies và cấu hình pháp nhân. Chọn mục 4A hoặc 4B, giữ phần còn lại; lưu bản đã duyệt theo revision. Nháp sau cọc và bản ký sau OTP hợp lệ. |
| ElectricCarSales_BienNhan_Coc_30.docx | BC-DEMO-0001, ORDER-DEMO-0001, ngày, số tiền, danh tính, xe và mã sao kê là nội dung mẫu cố định. Chuẩn hóa thành trường; lấy dữ liệu từ DEPOSIT COMPLETED, đơn và xe. Mẫu hiện có câu xác nhận bởi kế toán cần nhóm duyệt cách thể hiện khi SePay xác thực. |
| ElectricCarSales_HoaDon_ThanhToan_100.docx | HD-DEMO-0001, thông tin khách và xe, số hợp đồng, hai khoản đã thu và xác nhận tất toán là dữ liệu demo. Ánh xạ Invoices, Contracts, Payments và Orders. Xuất sau khi invoice ISSUED, đủ 100% và Signed. |

Nhóm cần xác nhận thông tin pháp nhân, MST, người đại diện, các khoản phí và tài liệu bảo hành. Khi tích hợp POI, kiểm tra trường qua nhiều runs, đoạn, bảng, header/footer và xem kết quả render toàn bộ trang. Các tài liệu mẫu không cung cấp dữ liệu giao dịch thật.

## Kiểm thử tích hợp còn lại

- SQL Server tách biệt: xác nhận cọc chính xác, replay, sai VA, sai mã, sai tiền, lỗi giữa transaction, hai khách cùng VIN, tái tạo payment, hủy sau cọc, đủ số tiền và không có EXPORT ở bước cọc.
- Kiểm tra tài khoản khách khác, cash, giải ngân đúng đối tác, khi thiếu 70%, khi thiếu chữ ký, khi thiếu hóa đơn; Operations bàn giao sau khi đủ điều kiện và chỉ có một EXPORT.
- Tomcat: kiểm tra JSON với phiên hợp lệ và hết hạn, giao diện thanh toán, popup tư vấn, danh mục, kho và báo cáo. Sau khi triển khai phần còn thiếu, kiểm tra chấp nhận/từ chối đề nghị, đổi bản duyệt, OTP sai/hết hạn/dùng lại và quyền tải chứng từ.


## Danh sách thay đổi trong workspace

| Nhóm | Tệp và chức năng |
|---|---|
| Thanh toán và kế toán | BankTransferService.java tạo và xác nhận khoản 30%/70%, chuyển trạng thái xe. PaymentController.java nhận kênh và giữ kiểm tra quyền khách. SePayWebhookController.java xác thực sự kiện, định tuyến OCB/MB và tránh xác nhận lặp. AccountantPaymentController.java trả hàng đợi và danh sách khoản thành công. db.properties.example mô tả VA/config. |
| Hợp đồng | ContractController.java kiểm tra CSRF và quyền tạo, trả JSON chuẩn. ContractsService.java và ContractsDAO.java kiểm tra cọc, bản duyệt và tiêu thụ OTP cùng transaction ký. OtpService.java gửi OTP ký qua SMTP, không xuất mã ký ra console. |
| Hóa đơn, bàn giao, tồn kho | PaymentDAO.java kiểm tra tổng từng khoản. InvoiceDAO.java kiểm tra đủ điều kiện hóa đơn và xe SOLD. DeliveryDAO.java và DeliveryController.java kiểm tra Signed, ISSUED, vai trò, tồn kho, lịch sử EXPORT. CarDAO.java ngăn cập nhật trạng thái thủ công của xe đã gắn đơn. |
| Điều hướng và phiên | OrderController.java cấp CSRF cho Operations. OrderDAO.java và Order.java đưa hình thức mua và trạng thái hợp đồng lên trang đơn khách. InvoiceController.java, OperationsPageFilter.java và PortalPageFilter.java giữ định dạng JSON cho lỗi tài chính/xác thực. |
| JSP và JavaScript | bank-transfer.jsp và payment-bank-transfer.js hiển thị QR/polling. my-orders.jsp, my-account.jsp, checkout.jsp cập nhật kênh chi trả. my-contracts.jsp mang CSRF khi yêu cầu OTP và ký. staff.js và payment-review.jsp phân biệt xác nhận SePay và chứng từ. |
| Test | Mới OcbPaymentTest.java và SaleSettlementTest.java. TestApplication.java chuẩn hóa tên schema SQL Server khi tạo fixture H2. |
| Tài liệu | Mới SALES_WORKFLOW_IMPLEMENTATION.md và SalesWorkflowMigration.proposal.sql. |

Đối chiếu thêm SDS trong workspace cho kiến trúc MVC, trạng thái xe vật lý, Orders.CompanyID và OTPVerifications.Purpose. Mục đích OTP ký nối thêm mã revision trong giới hạn VARCHAR(50) của schema hiện tại. Bản DOCX RDS, SDS và ba mẫu nguồn được đọc để đối chiếu.

## Kết quả kiểm tra trên máy

- Maven offline test-compile thành công. Maven offline package với skipTests tạo WAR thành công. Lệnh verify dừng trong Surefire vì metadata cache dùng repository ID khác.
- Chạy trực tiếp JUnit với Tomcat loopback và H2 dùng một lần. 27/27 test đạt, gồm 5 test mới. Jasper biên dịch 6 JSP đã sửa với 0 lỗi. Hai tệp JavaScript qua kiểm tra cú pháp Node.
- Chưa chạy SePay thật, SQL Server, SMTP hoặc kiểm thử cạnh tranh SQL Server. Migration chưa được thực thi. Kết quả H2 không xác minh cú pháp khóa/giao dịch đặc thù SQL Server hoặc toàn bộ luồng mới.
