<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<% request.setAttribute("customerTitle", "Chuyển khoản OCB"); request.setAttribute("customerEyebrow", "VIETQR · CHỜ ĐỐI CHIẾU"); %>
<%@ include file="../common/customer-shell.jspf" %>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/payment-bank-transfer.css">
<section class="panel bank-transfer-panel" data-payment-id="<c:out value='${transfer.paymentId}'/>"
         data-check-url="<%= request.getContextPath() %>/payment/check?paymentId=<c:out value='${transfer.paymentId}'/>"
         data-success-url="<%= request.getContextPath() %>/payment/detail?orderId=<c:out value='${transfer.orderId}'/>">
    <h2>Chuyển khoản <c:out value="${transfer.type eq 'DEPOSIT' ? 'đặt cọc 30%' : '70% còn lại'}"/></h2>
    <p>Mã đơn hàng: <strong>#<c:out value="${transfer.orderId}"/></strong> — Khoản thanh toán #<c:out value="${transfer.paymentId}"/></p>
    <div class="bank-transfer-layout">
        <div class="bank-transfer-qr">
            <c:choose>
                <c:when test="${transfer.status eq 'PENDING'}">
                    <img alt="Mã VietQR chuyển khoản vào tài khoản OCB" src="<c:out value='${transfer.qrUrl}'/>" loading="eager">
                    <small>QR được tạo theo chính số tiền của đơn hàng. Vui lòng kiểm tra lại thông tin trên ứng dụng ngân hàng.</small>
                </c:when>
                <c:otherwise><p>QR đã ngừng sử dụng vì khoản thanh toán không còn PENDING.</p></c:otherwise>
            </c:choose>
        </div>
        <dl class="bank-transfer-details">
            <dt>Ngân hàng</dt><dd>OCB (Ngân hàng Phương Đông)</dd>
            <dt>Số tài khoản</dt><dd>0917231548</dd>
            <dt>Chủ tài khoản</dt><dd>Do Tan Dat</dd>
            <dt>Số tiền phải chuyển</dt><dd class="bank-transfer-amount"><c:out value="${transfer.amount}"/> VND</dd>
            <dt>Nội dung chuyển khoản</dt><dd class="bank-transfer-ref"><c:out value="${transfer.reference}"/></dd>
        </dl>
    </div>
    <div class="info-note"><span>i</span><p>Quét QR <strong>không đồng nghĩa đã thanh toán</strong>. Kế toán phải kiểm tra tiền thực nhận qua sao kê OCB trước khi xác nhận. Không chuyển ít hơn số tiền hiển thị để thử nghiệm nếu muốn hoàn tất đơn.</p></div>
    <p id="bank-transfer-state" role="status">Trạng thái máy chủ: <strong><c:out value="${transfer.status}"/></strong></p>
    <p><a class="button button-primary" href="<%= request.getContextPath() %>/payment/detail?orderId=<c:out value='${transfer.orderId}'/>">Xem lịch sử và công nợ</a></p>
</section>
<script src="<%= request.getContextPath() %>/assets/js/payment-bank-transfer.js" defer></script>
<%@ include file="../common/customer-shell-end.jspf" %>
