<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("customerTitle", "Xác nhận thông tin"); request.setAttribute("customerEyebrow", "BƯỚC TIẾP THEO"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="checkout-notice">
    <span>i</span>
    <div>
        <strong>Đây chưa phải giao dịch mua xe</strong>
        <p>Theo quy trình dự án, nhân viên bán hàng tạo giao dịch và xác nhận thông tin xe. Trang này giúp bạn gửi thông tin quan tâm để được hỗ trợ.</p>
    </div>
</div>
<div class="checkout-layout">
    <section class="panel checkout-panel">
        <div class="panel-heading">
            <div>
                <span class="section-overline">THÔNG TIN LIÊN HỆ</span>
                <h2>Để lại nhu cầu của bạn</h2>
            </div>
            <span class="step-indicator">01 <i>/</i> 01</span>
        </div>
        <form class="customer-form" data-consultation-form>
            <label>Họ và tên<input name="fullName" autocomplete="name" placeholder="Nhập họ tên" required>
            </label>
            <div class="form-row">
                <label>Số điện thoại<input name="phone" type="tel" autocomplete="tel" placeholder="090 xxx xxxx" required>
                </label>
                <label>Email <span class="optional">Không bắt buộc</span>
                    <input name="email" type="email" autocomplete="email" placeholder="ban@email.com">
                </label>
            </div>
            <label>Mẫu xe quan tâm<select name="model">
                    <option value="">Chọn mẫu xe</option>
                    <option>VF 3</option>
                    <option>VF 5</option>
                    <option>VF 6</option>
                    <option>VF 7</option>
                    <option>VF 8</option>
                    <option>VF 9</option>
                    <option>Chưa xác định</option>
                </select>
            </label>
            <label>Nội dung cần tư vấn<textarea name="message" rows="4" placeholder="Chia sẻ nhu cầu hoặc câu hỏi của bạn"></textarea>
            </label>
            <label class="consent-row">
                <input type="checkbox" required>
                <span>Tôi đồng ý để VinFast liên hệ tư vấn theo thông tin đã cung cấp.</span>
            </label>
            <button class="button button-primary" type="submit">Gửi yêu cầu tư vấn <span>→</span>
            </button>
            <p class="form-footnote">Yêu cầu sẽ được tiếp nhận để nhân viên tư vấn hỗ trợ. Chưa có khoản thanh toán nào được thực hiện.</p>
        </form>
    </section>
    <aside class="panel next-steps">
        <span class="section-overline">SAU KHI GỬI</span>
        <h2>Điều gì sẽ diễn ra?</h2>
        <ol class="step-list">
            <li>
                <span>1</span>
                <div>
                    <strong>Tiếp nhận yêu cầu</strong>
                    <p>Thông tin xe và nhu cầu của bạn được gửi đến đội ngũ tư vấn.</p>
                </div>
            </li>
            <li>
                <span>2</span>
                <div>
                    <strong>Nhân viên liên hệ</strong>
                    <p>Nhân viên sẽ trao đổi để xác nhận phiên bản, giá và xe sẵn có.</p>
                </div>
            </li>
            <li>
                <span>3</span>
                <div>
                    <strong>Tạo giao dịch</strong>
                    <p>Giao dịch được lập sau khi hai bên xác nhận thông tin.</p>
                </div>
            </li>
        </ol>
        <div class="support-card">
            <span>✳</span>
            <div>
                <strong>Cần trao đổi ngay?</strong>
                <p>Liên hệ đội ngũ hỗ trợ khách hàng.</p>
                <a href="mailto:hotro@electriccarsale.vn">hotro@electriccarsale.vn</a>
            </div>
        </div>
    </aside>
</div>
<%@ include file="../common/customer-shell-end.jspf" %>
