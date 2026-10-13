<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("customerTitle", "Demo thanh toán"); request.setAttribute("customerEyebrow", "MÔ PHỎNG NGHIỆP VỤ PAYMENT & FINANCE"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="demo-banner" role="note">
    <span class="demo-banner-icon">!</span>
    <div><strong>Chế độ trình diễn — không phát sinh giao dịch thật</strong>
        <p>QR bên dưới chỉ chứa nội dung giả lập. Không quét để chuyển tiền. Khi tích hợp ngân hàng/cổng thanh toán, tài khoản nhận tiền sẽ được cấu hình ở phía máy chủ.</p>
    </div>
    <span class="demo-mode">DEMO</span>
</div>

<div class="demo-grid">
    <section class="demo-main-column">
        <section class="panel demo-panel">
            <div class="demo-panel-heading">
                <div><span class="section-overline">01 · CHỌN KHOẢN THANH TOÁN</span><h2>Đơn hàng demo <span class="demo-order-id">#EC-2026-001</span></h2></div>
                <span class="demo-status" id="demo-order-status">CÒN CÔNG NỢ</span>
            </div>
            <div class="demo-order-summary">
                <div><span>Mẫu xe</span><strong>VinFast VF 8 Plus</strong></div>
                <label><span>Giá trị đơn hàng</span><div class="demo-money-input"><input id="vehicle-price" type="number" min="10000000" step="1000000" value="1200000000" inputmode="numeric" aria-label="Giá trị đơn hàng"><b>₫</b></div></label>
                <div><span>Đã thanh toán (demo)</span><strong id="paid-total">0 ₫</strong></div>
                <div><span>Công nợ còn lại</span><strong class="demo-debt" id="debt-total">1.200.000.000 ₫</strong></div>
            </div>
            <div class="demo-progress"><span id="paid-progress"></span></div>

            <div class="demo-tabs" role="tablist" aria-label="Hình thức thanh toán">
                <button type="button" class="demo-tab is-active" data-tab="direct" role="tab" aria-selected="true">Trả thẳng</button>
                <button type="button" class="demo-tab" data-tab="installment" role="tab" aria-selected="false">Trả góp</button>
            </div>

            <div class="demo-tab-content" id="direct-panel" role="tabpanel">
                <div class="demo-fields">
                    <label>Loại khoản
                        <select id="payment-type"><option value="deposit">Đặt cọc 30%</option><option value="remaining">Thanh toán toàn bộ công nợ</option><option value="custom">Thanh toán một phần</option></select>
                    </label>
                    <label id="custom-amount-wrap" class="is-hidden">Số tiền thanh toán
                        <div class="demo-money-input"><input id="custom-amount" type="number" min="1000" step="1000" value="10000000"><b>₫</b></div>
                    </label>
                    <label>Phương thức
                        <select id="payment-method"><option value="BANK_TRANSFER">Chuyển khoản ngân hàng (QR demo)</option><option value="CASH">Tiền mặt tại showroom (demo)</option></select>
                    </label>
                </div>
                <div class="demo-amount-callout"><span>Số tiền cần thanh toán</span><strong id="due-amount">360.000.000 ₫</strong><small id="due-note">Tiền cọc được tính bằng 30% giá trị đơn.</small></div>
            </div>

            <div class="demo-tab-content is-hidden" id="installment-panel" role="tabpanel" aria-hidden="true">
                <div class="demo-fields">
                    <label>Khoản thanh toán
                        <select id="installment-payment-kind"><option value="down-payment">Trả trước</option><option value="monthly">Thanh toán kỳ tiếp theo</option></select>
                    </label>
                    <label>Trả trước
                        <select id="down-payment-rate"><option value="30">30% · 360.000.000 ₫</option><option value="40">40%</option><option value="50">50%</option></select>
                    </label>
                    <label>Thời hạn vay
                        <select id="installment-term"><option value="12">12 tháng</option><option value="24" selected>24 tháng</option><option value="36">36 tháng</option></select>
                    </label>
                    <label>Lãi suất năm (ước tính)
                        <div class="demo-money-input"><input id="annual-rate" type="number" min="0" max="40" step="0.1" value="8.9"><b>%</b></div>
                    </label>
                </div>
                <div class="installment-results">
                    <div><span>Khoản vay dự kiến</span><strong id="loan-amount">840.000.000 ₫</strong></div>
                    <div><span>Trả góp mỗi tháng</span><strong class="demo-highlight" id="monthly-payment">36.700.000 ₫</strong></div>
                    <div><span>Tổng lãi ước tính</span><strong id="interest-total">40.800.000 ₫</strong></div>
                </div>
                <div class="schedule-wrap"><table class="demo-schedule"><thead><tr><th>Kỳ</th><th>Ngày đến hạn</th><th>Gốc</th><th>Lãi</th><th>Tổng kỳ</th><th>Trạng thái</th></tr></thead><tbody id="schedule-body"></tbody></table></div>
                <div class="demo-settlement"><div><strong>Tất toán trước hạn (ước tính)</strong><span id="settlement-note">Dư nợ gốc + phí tất toán mô phỏng 2%.</span></div><strong id="settlement-amount">—</strong><button class="button button-secondary" type="button" id="settlement-button">Tạo QR tất toán</button></div>
            </div>
        </section>

        <section class="panel demo-history-panel">
            <div class="demo-panel-heading"><div><span class="section-overline">02 · LỊCH SỬ TRÊN THIẾT BỊ NÀY</span><h2>Giao dịch demo</h2></div><button class="demo-reset" id="reset-demo" type="button">Đặt lại demo</button></div>
            <div class="demo-history-empty" id="history-empty">Chưa có giao dịch. Tạo một QR và chọn “Mô phỏng đã thanh toán” để xem lịch sử cập nhật.</div>
            <div class="demo-history-list" id="history-list"></div>
        </section>
    </section>

    <aside class="demo-side-column">
        <section class="panel qr-panel">
            <div class="qr-heading"><span class="section-overline">03 · THÔNG TIN NHẬN TIỀN</span><span class="qr-live"><i></i> ĐANG MÔ PHỎNG</span></div>
            <div class="qr-frame"><div id="demo-qr" aria-label="QR nội dung thanh toán demo"></div><div id="qr-fallback" class="qr-fallback is-hidden">Mã QR đang tải. Nội dung demo: <span id="qr-text-fallback"></span></div></div>
            <strong class="qr-caption" id="qr-caption">QR demo cho khoản đặt cọc</strong>
            <div class="recipient-card">
                <div><span>Ngân hàng</span><strong>NGÂN HÀNG MÔ PHỎNG</strong></div>
                <div><span>Chủ tài khoản</span><strong>ElectricCarSale Demo</strong></div>
                <div><span>Số tài khoản</span><strong>DEMO-000000</strong></div>
                <div><span>Số tiền</span><strong id="qr-amount">360.000.000 ₫</strong></div>
                <div><span>Nội dung</span><strong class="qr-reference" id="qr-reference">DEMO EC-2026-001 DATCOC</strong></div>
            </div>
            <button class="button button-primary qr-confirm" id="simulate-payment" type="button">Mô phỏng đã thanh toán <span>→</span></button>
            <p class="qr-disclaimer">Nút trên chỉ đổi trạng thái dữ liệu trong trình duyệt. Hệ thống không gọi ngân hàng, không nhận tiền và không ghi vào cơ sở dữ liệu.</p>
        </section>
        <section class="panel demo-flow-card">
            <span class="section-overline">LUỒNG NGHIỆP VỤ</span>
            <ol><li><b>1</b><span>Tính số tiền từ công nợ đơn hàng</span></li><li><b>2</b><span>Tạo mã tham chiếu và QR demo</span></li><li><b>3</b><span>Cổng thanh toán xác thực giao dịch</span></li><li><b>4</b><span>Máy chủ cập nhật Payments và công nợ</span></li></ol>
            <p>Ở bản demo, bước 3–4 được thay bằng nút mô phỏng. Khi làm thật, tiền về tài khoản doanh nghiệp cấu hình với ngân hàng/cổng thanh toán.</p>
        </section>
    </aside>
</div>

<script src="https://cdn.jsdelivr.net/npm/qrcodejs@1.0.0/qrcode.min.js" defer></script>
<script src="<%= request.getContextPath() %>/assets/js/payment-demo.js" defer></script>
<%@ include file="../common/customer-shell-end.jspf" %>
