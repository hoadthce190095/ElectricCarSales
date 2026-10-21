<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("customerTitle", "Giỏ hàng"); request.setAttribute("customerEyebrow", "XE ĐIỆN CỦA BẠN"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="page-intro">
    <p>Xe bạn quan tâm sẽ được lưu tại đây để tiện theo dõi. Đơn hàng chính thức được nhân viên xác nhận theo quy trình bán hàng.</p>
    <a class="text-link" href="<%= customerContext %>/index.jsp?view=customer#models">← Tiếp tục khám phá xe</a>
</div>
<div class="commerce-layout">
    <section class="panel cart-panel">
        <div class="panel-heading">
            <div>
                <span class="section-overline">DANH SÁCH ĐÃ CHỌN</span>
                <h2>Xe trong giỏ hàng <span class="muted-count" data-cart-count>(0)</span>
                </h2>
            </div>
            <span class="soft-badge">Lưu trên thiết bị này</span>
        </div>
        <div class="empty-state commerce-empty">
            <div class="empty-illustration car-empty">
                <span>⌁</span>
                <i>✳</i>
            </div>
            <strong>Giỏ hàng đang trống</strong>
            <p>Chọn mẫu xe bạn yêu thích để xem lại tại đây.</p>
            <a class="button button-primary" href="<%= customerContext %>/index.jsp?view=customer#models">Khám phá xe <span>→</span>
            </a>
        </div>
    </section>
    <aside class="panel order-summary">
        <span class="section-overline">TÓM TẮT</span>
        <h2>Thông tin đơn hàng</h2>
        <div class="summary-line">
            <span>Số lượng xe</span>
            <strong data-cart-count>0</strong>
        </div>
        <div class="summary-line">
            <span>Tạm tính</span>
            <strong>—</strong>
        </div>
        <div class="summary-line total-line">
            <span>Tổng cộng</span>
            <strong>—</strong>
        </div>
        <p class="summary-disclaimer">Giá bán và tình trạng xe cần được nhân viên xác nhận trước khi tạo giao dịch.</p>
        <a class="button button-disabled" aria-disabled="true" href="<%= customerContext %>/customer/checkout.jsp">Tiếp tục đặt xe <span>→</span>
        </a>
    </aside>
</div>
<%@ include file="../common/customer-shell-end.jspf" %>
