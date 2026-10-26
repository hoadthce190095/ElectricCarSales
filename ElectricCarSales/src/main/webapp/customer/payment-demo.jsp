<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("customerTitle", "Cổng thanh toán điện tử"); request.setAttribute("customerEyebrow", "PAYMENT GATEWAY · DEMO"); %>
<%@ include file="../common/customer-shell.jspf" %>

<section class="gateway-intro-card panel">
    <div class="gateway-intro-heading"><div><span class="section-overline">THANH TOÁN AN TOÀN</span><h2>Liên kết với cổng thanh toán điện tử</h2></div><span class="gateway-demo-pill">DEMO</span></div>
    <p>Mô phỏng thanh toán trả thẳng hoặc trả góp qua Visa, Mastercard, NAPAS/thẻ ghi nợ, ngân hàng trực tuyến và MoMo. Không hiển thị mã QR trên website.</p>
    <p class="gateway-real-note">Giao dịch thật cần adapter và tài khoản nhà cung cấp được cấu hình trên máy chủ. Demo này không kết nối ngân hàng và không thu tiền.</p>
</section>

<div class="paydemo-layout">
    <main class="paydemo-main">
        <section class="panel paydemo-panel">
            <div class="paydemo-heading"><div><span class="section-overline">01 · ĐƠN HÀNG MẪU</span><h2>VinFast VF 8 Plus <small>#EC-2026-001</small></h2></div><span class="paydemo-status" id="order-status">CÒN CÔNG NỢ</span></div>
            <div class="paydemo-summary">
                <label><span>Giá trị đơn hàng</span><div class="paydemo-money"><input id="vehicle-price" type="number" min="10000000" step="1000000" value="1200000000" aria-label="Giá trị đơn hàng"><b>₫</b></div></label>
                <div><span>Đã thanh toán demo</span><strong id="paid-total">0 ₫</strong></div>
                <div><span>Công nợ còn lại</span><strong class="paydemo-debt" id="debt-total">1.200.000.000 ₫</strong></div>
            </div>
            <div class="paydemo-progress"><span id="paid-progress"></span></div>
            <div class="paydemo-tabs" role="tablist" aria-label="Hình thức thanh toán">
                <button class="paydemo-tab is-active" type="button" data-tab="direct" role="tab" aria-selected="true">Trả thẳng</button>
                <button class="paydemo-tab" type="button" data-tab="installment" role="tab" aria-selected="false">Trả góp</button>
            </div>
            <section class="paydemo-tab-panel" id="direct-panel" role="tabpanel">
                <div class="paydemo-fields">
                    <label>Khoản thanh toán<select id="payment-type"><option value="deposit">Đặt cọc 30%</option><option value="remaining">Toàn bộ công nợ còn lại</option><option value="custom">Thanh toán một phần</option></select></label>
                    <label id="custom-amount-wrap" class="is-hidden">Số tiền<div class="paydemo-money"><input id="custom-amount" type="number" min="1000" step="1000" value="10000000"><b>₫</b></div></label>
                </div>
                <div class="paydemo-amount"><span>Số tiền cần thanh toán</span><strong id="due-amount">360.000.000 ₫</strong><small id="due-note">Đặt cọc được tính bằng 30% giá trị đơn.</small></div>
            </section>
            <section class="paydemo-tab-panel is-hidden" id="installment-panel" role="tabpanel" aria-hidden="true">
                <div class="paydemo-fields">
                    <label>Khoản thanh toán<select id="installment-payment-kind"><option value="down-payment">Trả trước</option><option value="monthly">Kỳ trả tiếp theo</option></select></label>
                    <label>Tỷ lệ trả trước<select id="down-payment-rate"><option value="30">30%</option><option value="40">40%</option><option value="50">50%</option></select></label>
                    <label>Thời hạn<select id="installment-term"><option value="12">12 tháng</option><option value="24" selected>24 tháng</option><option value="36">36 tháng</option></select></label>
                    <label>Lãi suất năm (ước tính)<div class="paydemo-money"><input id="annual-rate" type="number" min="0" max="40" step="0.1" value="8.9"><b>%</b></div></label>
                </div>
                <div class="paydemo-metrics"><div><span>Khoản vay dự kiến</span><strong id="loan-amount">840.000.000 ₫</strong></div><div><span>Mỗi tháng</span><strong class="paydemo-accent" id="monthly-payment">36.700.000 ₫</strong></div><div><span>Tổng lãi ước tính</span><strong id="interest-total">40.800.000 ₫</strong></div></div>
                <div class="paydemo-schedule"><table><thead><tr><th>Kỳ</th><th>Đến hạn</th><th>Gốc</th><th>Lãi</th><th>Tổng kỳ</th><th>Trạng thái</th></tr></thead><tbody id="schedule-body"></tbody></table></div>
                <div class="paydemo-settlement"><div><strong>Tất toán trước hạn</strong><small>Dư nợ cộng phí mô phỏng 2%.</small></div><b id="settlement-amount">—</b><button type="button" class="button button-secondary" id="settlement-button">Chọn tất toán</button></div>
            </section>
        </section>

        <section class="panel paydemo-panel"><div class="paydemo-heading"><div><span class="section-overline">LỊCH SỬ TRÊN THIẾT BỊ NÀY</span><h2>Giao dịch demo</h2></div><button class="paydemo-reset" id="reset-demo" type="button">Đặt lại demo</button></div><div class="paydemo-history-empty" id="history-empty">Chưa có giao dịch mô phỏng.</div><div class="paydemo-history" id="history-list"></div></section>
    </main>

    <aside class="paydemo-aside">
        <section class="panel gateway-panel">
            <div class="gateway-panel-heading"><div><span class="section-overline">02 · PHƯƠNG THỨC</span><h2>Cổng thanh toán</h2></div><span class="gateway-state"><i></i> DEMO</span></div>
            <p class="gateway-help">Chọn kênh thanh toán để xem luồng mô phỏng. Không có QR nào được tạo hoặc hiển thị.</p>
            <div class="gateway-options" role="radiogroup" aria-label="Chọn phương thức thanh toán">
                <button type="button" class="gateway-option" data-channel="card" role="radio" aria-checked="false"><span class="gateway-icon card-icon">▰</span><span><b>Thẻ đã liên kết</b><small>Visa · Mastercard · NAPAS</small></span><i></i></button>
                <button type="button" class="gateway-option is-selected" data-channel="momo" role="radio" aria-checked="true"><span class="gateway-icon momo-icon">M</span><span><b>Ví MoMo</b><small>Xác thực qua ứng dụng MoMo</small></span><i></i></button>
                <button type="button" class="gateway-option" data-channel="napas" role="radio" aria-checked="false"><span class="gateway-icon napas-icon">N</span><span><b>NAPAS / thẻ ghi nợ</b><small>Thẻ nội địa</small></span><i></i></button>
                <button type="button" class="gateway-option" data-channel="bank" role="radio" aria-checked="false"><span class="gateway-icon bank-icon">↗</span><span><b>Ngân hàng trực tuyến</b><small>Chuyển tới luồng xác thực của ngân hàng</small></span><i></i></button>
            </div>
            <div class="paydemo-linked-cards is-hidden" id="linked-card-area"><div class="linked-cards-heading"><strong>Thẻ đã liên kết</strong><button id="toggle-card-form" type="button">+ Thêm thẻ demo</button></div><div id="linked-card-list"></div><p id="card-empty" class="card-empty">Chưa có thẻ demo.</p>
                <form id="card-link-form" class="card-link-form is-hidden"><p>Chỉ dùng dữ liệu minh họa. Website không nhận số thẻ hoặc CVV; tích hợp thật sẽ dùng trang token hóa của nhà cung cấp.</p><label>Loại thẻ<select id="card-brand"><option value="Visa">Visa</option><option value="Mastercard">Mastercard</option><option value="NAPAS">NAPAS</option></select></label><label>4 số cuối giả lập<input id="card-last-four" type="text" inputmode="numeric" maxlength="4" autocomplete="off" pattern="[0-9]{4}" placeholder="Ví dụ: 4242" required></label><button type="submit" class="button button-secondary">Liên kết thẻ demo</button></form>
            </div>
            <div class="gateway-selection-note" id="selected-card-note">Luồng MoMo được mô phỏng trong trang này.</div>
            <div class="gateway-total"><span>Số tiền</span><strong id="gateway-amount">360.000.000 ₫</strong></div>
            <button type="button" class="button button-primary gateway-pay-button" id="simulate-payment">Mô phỏng thanh toán <span>→</span></button>
            <div class="gateway-result is-hidden" id="gateway-result" role="status" aria-live="polite"></div>
            <p class="gateway-footnote">Không gọi ngân hàng, không ghi database và không chuyển tiền thật.</p>
        </section>
        <section class="panel gateway-flow"><span class="section-overline">LUỒNG KHI TÍCH HỢP THẬT</span><ol><li><b>1</b><span>Máy chủ tạo phiên thanh toán</span></li><li><b>2</b><span>Khách chọn thẻ, ngân hàng hoặc MoMo</span></li><li><b>3</b><span>Nhà cung cấp xác thực giao dịch</span></li><li><b>4</b><span>Máy chủ xác minh và cập nhật Payments</span></li></ol></section>
    </aside>
</div>
<script src="<%= request.getContextPath() %>/assets/js/payment-demo.js" defer></script>
<%@ include file="../common/customer-shell-end.jspf" %>
