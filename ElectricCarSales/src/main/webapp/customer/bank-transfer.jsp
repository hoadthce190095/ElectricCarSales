<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<% request.setAttribute("customerTitle", "Chuyển khoản MB Bank"); request.setAttribute("customerEyebrow", "THANH TOÁN CHUYỂN KHOẢN · CHỜ ĐỐI CHIẾU"); %>
<%@ include file="../common/customer-shell.jspf" %>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/payment-bank-transfer.css">
<section class="panel bank-transfer-panel" data-payment-id="<c:out value='${transfer.paymentId}'/>"
         data-check-url="<%= request.getContextPath() %>/payment/check?paymentId=<c:out value='${transfer.paymentId}'/>"
         data-success-url="<%= request.getContextPath() %>/payment/detail?orderId=<c:out value='${transfer.orderId}'/>">
    <div class="transfer-heading">
        <div><span class="section-overline">ĐƠN HÀNG #<c:out value="${transfer.orderId}"/></span>
            <h2>Chuyển khoản <c:out value="${transfer.type eq 'DEPOSIT' ? 'đặt cọc 30%' : '70% còn lại'}"/></h2></div>
        <span class="transfer-badge">Mã thanh toán <c:out value="${transfer.paymentId}"/></span>
    </div>
    <div class="transfer-summary">
        <span>Số tiền cần chuyển</span>
        <strong><c:out value="${transfer.amount}"/> ₫</strong>
        <small>Vui lòng chuyển đúng số tiền và giữ nguyên nội dung bên dưới.</small>
    </div>
    <dl class="bank-transfer-details">
        <dt>Ngân hàng</dt><dd><c:out value="${transfer.bankName}"/></dd>
        <dt>Chủ tài khoản</dt><dd><c:out value="${transfer.accountName}"/></dd>
        <dt>Số tài khoản</dt><dd><span id="bank-account"><c:out value="${transfer.accountNumber}"/></span>
            <button class="copy-button" type="button" data-copy="bank-account">Sao chép</button></dd>
        <dt>Số tiền</dt><dd><span id="bank-amount"><c:out value="${transfer.amount}"/></span> VND
            <button class="copy-button" type="button" data-copy="bank-amount">Sao chép</button></dd>
        <dt>Nội dung chuyển khoản</dt><dd><span id="bank-reference"><c:out value="${transfer.reference}"/></span>
            <button class="copy-button" type="button" data-copy="bank-reference">Sao chép</button></dd>
    </dl>
    <div class="info-note"><span>i</span><p>Hãy tự mở ứng dụng MB Bank để chuyển khoản và kiểm tra tên người nhận hiển thị trước khi xác nhận. Tên chủ tài khoản đang dùng theo thông tin đã cung cấp; hệ thống không xác minh tên với ngân hàng. Website không yêu cầu mật khẩu, thông tin thẻ hoặc OTP ngân hàng. Bấm sao chép chỉ giúp điền thông tin, không xác nhận thanh toán.</p></div>
    <p id="bank-transfer-state" class="transfer-state" role="status">
        <c:choose>
            <c:when test="${transfer.status eq 'COMPLETED'}">Đã được kế toán xác nhận.</c:when>
            <c:when test="${transfer.status eq 'FAILD' or transfer.status eq 'CANCELLED'}">Giao dịch bị từ chối hoặc hủy.</c:when>
            <c:when test="${transfer.bankReported}">Đã nhận thông tin giao dịch, chờ kế toán xác nhận.</c:when>
            <c:otherwise>Chờ chuyển khoản hoặc ngân hàng ghi nhận giao dịch.</c:otherwise>
        </c:choose>
    </p>
    <p><a class="button button-primary" href="<%= request.getContextPath() %>/payment/detail?orderId=<c:out value='${transfer.orderId}'/>">Xem lịch sử và công nợ</a></p>
</section>
<script src="<%= request.getContextPath() %>/assets/js/payment-bank-transfer.js" defer></script>
<%@ include file="../common/customer-shell-end.jspf" %>
