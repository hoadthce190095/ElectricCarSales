# Nguồn dữ liệu xe VinFast

Ngày kiểm tra: **05/10/2026**. Thông tin xe lấy từ website và brochure chính thức VinFast, phục vụ bài SWP391. Dữ liệu chỉ là bản chụp tại ngày kiểm tra; khi mua xe phải kiểm tra chính sách đang áp dụng với VinFast.

## Giá và thông số đã nhập

Giá trong bảng là MSRP **kèm pin và VAT**, theo [chính sách giá 15/07/2026](https://vinfastauto.com/vn_vi/node/11885) ([PDF chính thức](https://static-cms-prod.vinfastauto.com/202607015_thong-bao-chinh-sach-gia-ban-cac-dong-xe-vinfast-thang-07.2026_0.pdf)), với cấu hình nêu trong file JSON. Giá này chưa gồm phí lăn bánh, bộ sạc tại nhà, màu nâng cao; không trừ ưu đãi theo điều kiện khách hàng. Không lấy mức giá khuyến mại của đại lý làm giá niêm yết.

| Mẫu trong hệ thống | Giá tham khảo (VND) | Pin khả dụng (kWh) | Quãng đường (km và chuẩn) | Công suất (kW) | Ghế |
| ------------------ | ------------------: | -----------------: | ------------------------- | -------------: | --: |
| VF 3               |         285.000.000 |              18.64 | 215 NEDC                  |             30 |   4 |
| VF 5 Plus          |         496.000.000 |              37.23 | 326 NEDC                  |            100 |   5 |
| VF 6 Eco           |         646.000.000 |               59.6 | 485 NEDC                  |            130 |   5 |
| VF 6 Plus          |         699.000.000 |               59.6 | 460 NEDC                  |            150 |   5 |
| VF 7 Eco           |         740.000.000 |               59.6 | 440 NEDC                  |            130 |   5 |
| VF 7 Plus          |         830.000.000 |                 70 | 500.5 NEDC                |            150 |   5 |
| VF 8 Eco           |         898.000.000 |               87.7 | 635 NEDC                  |            150 |   5 |
| VF 8 Plus          |       1.079.000.000 |               87.7 | 475 NEDC                  |            300 |   5 |
| VF 9 Eco           |       1.348.000.000 |                123 | 626 WLTP                  |            300 |   7 |
| VF 9 Plus          |       1.529.000.000 |                123 | 602 WLTP                  |            300 |   7 |

VF 3 chọn **Eco Tiêu chuẩn 2**. VF 6 Plus chọn **Tiêu chuẩn 2**. VF 7 Eco chọn **Tiêu chuẩn 2**; VF 7 Plus chọn **FWD, trần thép, Tiêu chuẩn 2**. VF 8 chọn pin CATL bản VN; Eco FWD và Plus AWD. VF 9 Plus chọn trần thép 7 chỗ, không gồm tùy chọn ghế cơ trưởng 6 chỗ.

## Nguồn theo từng dòng

| Dòng | Trang chính thức dùng để đối chiếu                                                                                                                                             |
| ---- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| VF 3 | [Thông số VF 3](https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf3)                                                                                                             |
| VF 5 | [Thông số VF 5](https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf5)                                                                                                             |
| VF 6 | [Thông số VF 6](https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf6)                                                                                                             |
| VF 7 | [Thông số VF 7](https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf7), [brochure 10/04/2026](https://static-cms-prod.vinfastauto.com/statics/shared/10042026-Brochur-%20VF-7.pdf) |
| VF 8 | [Thông số VF 8](https://vinfastauto.com/vn_vi/dat-coc-xe-vf8), [brochure 03/02/2026](https://storage.googleapis.com/vinfast-data-01/brochure/VF8_Brochure_03022026.pdf)        |
| VF 9 | [Thông số VF 9](https://vinfastauto.com/vn_vi/dat-coc-xe-vf9), [brochure từ trang chính thức](https://storage.googleapis.com/vinfast-data-01/brochure/VF%209_%20Brochure.pdf)  |

Ảnh minh họa được liên kết trực tiếp từ CDN `static-cms-prod.vinfastauto.com`, lấy từ trang sản phẩm trên. Không lưu ảnh của bên thứ ba vào dự án. Các URL ảnh đã trả HTTP 200 khi kiểm tra. File `ElectricCarSales/src/main/webapp/assets/vinfast-catalogue.json` chứa đường dẫn ảnh, nguồn giá, nguồn thông số và ngày kiểm tra cho từng mẫu.

## Cách đọc thông số

- NEDC và WLTP là hai quy trình thử khác nhau. Số km công bố không phải cam kết quãng đường thực tế; không so sánh trực tiếp hai số thuộc hai chuẩn khác nhau.
- VF 7 Plus trong bài là FWD 150 kW. Plus AWD là cấu hình khác, không lấy công suất 260 kW của AWD ghép với giá bản FWD.
- VF 8 có khác biệt giữa tài liệu ở các thời điểm. Trang sản phẩm ngày kiểm tra ghi Eco 635 km NEDC, Plus 475 km NEDC; brochure tháng 02/2026 ghi Eco 562 km NEDC và Plus 457 km WLTP. Danh mục dùng **range của trang sản phẩm ngày kiểm tra**, giữ ghi chú chênh lệch trong JSON; không quy đổi hoặc ghép hai chuẩn.
- VF 9 có dòng nhãn đơn vị trên website chưa rõ. Brochure ghi rõ **402 hp / 300 kW**, nên cột `MaxPower` lưu 300 kW. Kích thước rộng 2254 mm theo brochure cần kiểm tra cách đo có gương khi đối chiếu mẫu khác.
- Mục sạc nhanh ghi rõ khoảng **10-70% pin**. Cột `ChargingTime` để NULL vì schema không tách sạc nhanh và sạc đầy; thời gian sạc nhanh được giữ trong mô tả và JSON. Không lấy 25/31/35 phút làm thời gian sạc từ 0-100%.
- Mức tiêu thụ điện, tốc độ tối đa hoặc số túi khí chưa đủ thông tin theo cấu hình cụ thể được để NULL. Riêng VF 9 có tốc độ tối đa 200 km/h được xác thực từ brochure.
- Màu lấy từ bộ chọn màu trên trang sản phẩm. Ba xe kho mẫu mỗi phiên bản dùng màu tiêu chuẩn; bảng giá màu nâng cao không được tự động cộng vào giá xe tiêu chuẩn.

## Phân biệt dữ liệu hãng và dữ liệu báo cáo

Tên xe, kích thước, pin, công suất, số ghế, quãng đường công bố, màu và ảnh là thông tin sản phẩm có nguồn VinFast. **30 xe kho có mã `DEMO-...`, VIN bắt đầu `DEMO`, số động cơ, mã lô và năm ghi trong kho là dữ liệu giả lập phục vụ học tập.** Đây không phải VIN hay tồn kho thật của hãng.

Khách hàng, nhân viên, đơn hàng, hóa đơn, thanh toán, trả góp và khuyến mãi trong phần báo cáo DEMO của `ElectricCarSalesDB.sql` là dữ liệu giả lập của showroom trong bài. Không được trình bày các số liệu này là doanh số thật của VinFast.

## Chạy script

Trong SQL Server, chạy duy nhất `ElectricCarSalesDB.sql` ở thư mục gốc cho cả database mới và database đã có. Script thực hiện theo thứ tự:

1. Tạo `ElectricCarSalesDB` nếu thiếu và bổ sung schema chung của cả nhóm.
2. Nạp danh mục VinFast cùng 30 xe kho DEMO vào database đó.
3. Nạp bộ báo cáo giả lập gồm 9 đơn cùng thanh toán, trả góp và khuyến mãi. Nếu đã có đơn hàng, thanh toán hoặc trả góp ngoài bộ DEMO, phần này được bỏ qua và thông báo bằng `PRINT`; schema và danh mục vẫn chạy.

Script chỉ thêm bản ghi còn thiếu; chạy lại không thêm trùng mã xe hoặc giao dịch, không sửa hay xóa dữ liệu hiện có. Script không tạo tài khoản hay mật khẩu Admin. Mỗi variant Eco/Plus có `ModelID` riêng vì bảng `Specifications` chỉ cho một dòng thông số trên một `ModelID`.
