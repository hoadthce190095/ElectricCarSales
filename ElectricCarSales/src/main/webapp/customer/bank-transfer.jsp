<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<% request.setAttribute("customerTitle", "Thanh toán VietQR · SePay + MB Bank"); request.setAttribute("customerEyebrow", "CỔNG CHUYỂN KHOẢN VIETQR · MB BANK"); %>
<%@ include file="../common/customer-shell.jspf" %>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/payment-bank-transfer.css">

<c:choose>
    <c:when test="${not empty transfer and not empty transfer.paymentUrl}">
        <section class="panel bank-transfer-panel" data-payment-id="<c:out value='${transfer.paymentId}'/>"
                 data-check-url="<%= request.getContextPath() %>/payment/check?paymentId=<c:out value='${transfer.paymentId}'/>"
                 data-success-url="<%= request.getContextPath() %>/payment/detail?orderId=<c:out value='${transfer.orderId}'/>">
            <div class="transfer-heading">
                <div>
                    <span class="section-overline">ĐƠN HÀNG #<c:out value="${transfer.orderId}"/></span>
                    <h2>Thanh toán chuyển khoản <c:out value="${transfer.type eq 'DEPOSIT' ? 'đặt cọc 30%' : '70% còn lại'}"/></h2>
                </div>
                <span class="transfer-badge">Mã thanh toán #<c:out value="${transfer.paymentId}"/></span>
            </div>

            <div class="info-note" style="border-color: #93c5fd; background: #eff6ff; color: #1e40af; margin: 16px 0;">
                <span>ℹ</span>
                <div>
                    <strong>Chuyển khoản VietQR tự động qua SePay + MB Bank</strong>
                    <p>Quý khách mở ứng dụng MB Bank hoặc bất kỳ ứng dụng ngân hàng nào (Vietcombank, BIDV, Techcombank, MoMo...) và quét mã VietQR bên dưới. Số tài khoản, số tiền và nội dung chuyển khoản được tự động điền chính xác. Giao dịch sẽ được SePay tự động ghi nhận trong 1-3 giây.</p>
                </div>
            </div>

            <div style="display: flex; gap: 32px; align-items: flex-start; justify-content: center; flex-wrap: wrap; margin: 24px 0;">
                <%-- KHỐI HIỂN THỊ MÃ VIETQR ĐỘNG TỪ SEPAY --%>
                <div style="text-align: center; max-width: 320px; width: 100%; background: #ffffff; padding: 16px; border-radius: 12px; border: 1px solid #e2e8f0; box-shadow: 0 4px 12px rgba(0,0,0,0.06);">
                    <img src="<c:out value='${transfer.paymentUrl}'/>" alt="Mã VietQR Chuyển khoản MB Bank SePay"
                         id="sepay-vietqr-image"
                         style="width: 100%; max-width: 280px; height: auto; border-radius: 8px; display: block; margin: 0 auto;">
                    <p style="margin: 10px 0 0; font-size: 0.85rem; color: #475569; font-weight: 500;">
                        Quét mã bằng ứng dụng ngân hàng để thanh toán nhanh
                    </p>
                </div>

                <%-- KHỐI THÔNG TIN CHUYỂN KHOẢN CHI TIẾT --%>
                <div style="flex: 1; min-width: 290px; max-width: 500px;">
                    <div class="transfer-summary" style="margin-bottom: 16px;">
                        <span>Số tiền cần thanh toán</span>
                        <strong><fmt:formatNumber value="${transfer.amount}" pattern="#,##0"/> VND</strong>
                        <small>Vui lòng chuyển khoản đúng số tiền để hệ thống tự động xác nhận.</small>
                    </div>

                    <dl class="bank-transfer-details">
                        <dt>Ngân hàng nhận</dt>
                        <dd><strong><c:out value="${transfer.bankName}"/></strong></dd>

                        <dt>Số tài khoản</dt>
                        <dd style="display: flex; justify-content: space-between; align-items: center; gap: 8px;">
                            <strong id="bank-account" style="font-size: 1.1rem; color: #0f172a;"><c:out value="${transfer.accountNumber}"/></strong>
                            <button type="button" class="button button-secondary" data-copy="bank-account" style="padding: 3px 8px; font-size: 11px;">Sao chép</button>
                        </dd>

                        <dt>Chủ tài khoản</dt>
                        <dd style="display: flex; justify-content: space-between; align-items: center; gap: 8px;">
                            <strong id="bank-name"><c:out value="${transfer.accountName}"/></strong>
                            <button type="button" class="button button-secondary" data-copy="bank-name" style="padding: 3px 8px; font-size: 11px;">Sao chép</button>
                        </dd>

                        <dt>Nội dung chuyển khoản</dt>
                        <dd style="display: flex; justify-content: space-between; align-items: center; gap: 8px;">
                            <strong id="bank-reference" style="font-size: 1.15rem; color: #0284c7; letter-spacing: 0.5px;"><c:out value="${transfer.reference}"/></strong>
                            <button type="button" class="button button-secondary" data-copy="bank-reference" style="padding: 3px 8px; font-size: 11px;">Sao chép</button>
                        </dd>

                        <dt>Khoản thanh toán</dt>
                        <dd><c:out value="${transfer.type eq 'DEPOSIT' ? 'Đặt cọc 30% giá trị hợp đồng' : 'Thanh toán 70% còn lại'}"/></dd>

                        <dt>Trạng thái yêu cầu</dt>
                        <dd><span class="soft-badge"><c:out value="${transfer.status}"/></span></dd>
                    </dl>

                    <%-- KHỐI TRẠNG THÁI REAL-TIME POLLING --%>
                    <div id="bank-transfer-state" class="transfer-state-box"
                         style="margin-top: 16px; padding: 12px; border-radius: 8px; background: #f0fdf4; border: 1px solid #bbf7d0; color: #166534; font-size: 0.9rem; text-align: center;">
                        ⏳ Đang chờ giao dịch chuyển khoản... Hệ thống sẽ tự động chuyển hướng khi nhận được tiền.
                    </div>
                    <div style="margin-top: 10px; display: flex; justify-content: center;">
                        <button type="button" id="btn-recheck-transfer" class="button button-primary" style="font-size: 13px; padding: 7px 16px; width: 100%; border-radius: 6px;">
                            ⚡ Tôi đã chuyển khoản (Kiểm tra giao dịch ngay)
                        </button>
                    </div>
                </div>
            </div>

            <div style="background: #fffbeb; border: 1px solid #fef3c7; border-radius: 8px; padding: 14px; margin: 20px 0; font-size: 0.88rem; color: #92400e; line-height: 1.6;">
                <strong>⚠️ Lưu ý quan trọng khi chuyển khoản:</strong>
                <ul style="margin: 6px 0 0 18px; padding: 0;">
                    <li>Giữ nguyên nội dung chuyển khoản là mã <strong><c:out value="${transfer.reference}"/></strong> để hệ thống tự động đối chiếu webhook SePay.</li>
                    <li>Không tự ý sửa số tiền thanh toán; số tiền phải đúng từng đồng theo yêu cầu.</li>
                    <li>Sau khi chuyển khoản thành công, vui lòng giữ màn hình này từ 1-5 giây để hệ thống tự động cập nhật đơn hàng.</li>
                </ul>
            </div>

            <div style="margin-top: 24px; padding-top: 16px; border-top: 1px dashed #e5e7eb; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
                <a class="button button-secondary" href="<%= request.getContextPath() %>/payment?orderId=<c:out value='${transfer.orderId}'/>">Quay lại trang thanh toán</a>
                <a class="button button-secondary" href="<%= request.getContextPath() %>/payment/detail?orderId=<c:out value='${transfer.orderId}'/>">Xem chi tiết đơn &amp; công nợ</a>
            </div>
        </section>
        <script src="<%= request.getContextPath() %>/assets/js/payment-bank-transfer.js" defer></script>
    </c:when>
    <c:otherwise>
        <c:set var="resolvedOrderId" value="${not empty orderId ? orderId : (not empty transfer ? transfer.orderId : '')}"/>
        <c:set var="resolvedType" value="${not empty type ? type : (not empty transfer ? transfer.type : 'DEPOSIT')}"/>
        <section class="panel bank-transfer-panel">
            <div class="transfer-heading">
                <div>
                    <span class="section-overline">ĐƠN HÀNG #<c:out value="${resolvedOrderId}"/></span>
                    <h2>Thông báo thanh toán</h2>
                </div>
            </div>
            <div class="info-note" style="border-color:#fca5a5;background:#fef2f2;color:#991b1b;margin:16px 0;">
                <span>!</span>
                <div>
                    <strong><c:out value="${not empty sepayError ? sepayError : 'Cổng thanh toán SePay + MB Bank chưa sẵn sàng.'}"/></strong>
                    <c:if test="${not empty cancelReason}">
                        <p style="margin:6px 0 0;font-size:13px;">Lý do hủy đơn: <strong><c:out value="${cancelReason}"/></strong></p>
                    </c:if>
                </div>
            </div>
            <p>Hệ thống chưa thể mở yêu cầu chuyển khoản lúc này. Quý khách có thể quay lại trang thanh toán để thử lại hoặc xem thông tin đơn hàng.</p>

            <c:if test="${not empty resolvedOrderId}">
                <div style="margin: 24px 0; display:flex; gap:12px; align-items:center; flex-wrap: wrap;">
                    <a class="button button-primary" href="<%= request.getContextPath() %>/payment?orderId=<c:out value='${resolvedOrderId}'/>">Quay lại trang thanh toán</a>
                    <a class="button button-secondary" href="<%= request.getContextPath() %>/payment/detail?orderId=<c:out value='${resolvedOrderId}'/>">Xem chi tiết đơn hàng</a>
                    <a class="button button-secondary" href="<%= request.getContextPath() %>/orders">Quay lại danh sách đơn hàng</a>
                </div>
            </c:if>
            <c:if test="${empty resolvedOrderId}">
                <p><a class="button button-primary" href="<%= request.getContextPath() %>/orders">Quay lại danh sách đơn hàng</a></p>
            </c:if>
        </section>
    </c:otherwise>
</c:choose>

<%@ include file="../common/customer-shell-end.jspf" %>
