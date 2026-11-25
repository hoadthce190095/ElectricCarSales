<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<!doctype html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Accountant · Đối chiếu &amp; Đối soát thanh toán</title>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/payment-bank-transfer.css"></head><body class="finance-review">
<main>
    <h1>Đối chiếu tiền thực nhận &amp; Đối soát giao dịch (Accountant)</h1>
    <p><a href="<%= request.getContextPath() %>/staff/dashboard">← Trang nhân viên (Dashboard)</a></p>
    <p>Chỉ xác nhận sau khi kiểm tra sao kê ngân hàng hoặc chứng từ giải ngân từ đối tác tài chính. Mã tham chiếu và số tiền phải trùng khớp thực tế.</p>
    <c:if test="${param.updated eq '1'}"><p role="status" style="color:#166534;font-weight:700;background:#dcfce7;border:1px solid #86efac;padding:8px 12px;border-radius:6px;">✓ Thao tác đã được lưu vào cơ sở dữ liệu.</p></c:if>

    <c:if test="${not empty lateTransfers}">
    <section class="finance-panel" style="border-color:#fca5a5;background:#fef2f2;">
        <h2 style="color:#991b1b;">⚠️ Giao dịch chuyển khoản phát sinh sau khi đơn đã hủy (<c:out value="${lateTransfers.size()}"/>)</h2>
        <p style="color:#7f1d1d;">Khách hàng đã quét mã QR cũ sau khi đơn bị hủy. Đơn hàng không được tự động kích hoạt lại; Kế toán cần kiểm tra và xử lý hoàn/đối soát thủ công.</p>
        <c:forEach var="lt" items="${lateTransfers}">
            <article style="border-top:1px solid #fecaca;padding:10px 0;">
                <strong>Khoản #<c:out value="${lt.paymentId}"/> · Đơn <c:out value="${lt.orderCode}"/> (#<c:out value="${lt.orderId}"/>) · <c:out value="${lt.customer}"/></strong>
                <p>Số tiền: <strong><c:out value="${lt.formattedReceivedAmount}"/></strong> · Mã tham chiếu: <code><c:out value="${lt.reference}"/></code></p>
                <p style="font-size:12px;color:#7f1d1d;"><c:out value="${lt.note}"/></p>
            </article>
        </c:forEach>
    </section>
    </c:if>

    <section class="finance-panel">
        <h2>Theo dõi công nợ đơn hàng (<c:out value="${openDebts.size()}"/>)</h2>
        <c:if test="${empty openDebts}"><p>Không có đơn hàng còn công nợ.</p></c:if>
        <c:forEach var="debt" items="${openDebts}">
          <article style="border-top:1px solid #e2e8f0;padding:12px 0;">
            <strong>Đơn <c:out value="${debt.orderCode}"/> (#<c:out value="${debt.orderId}"/>) · <c:out value="${debt.customer}"/></strong>
            <p>Tổng: <strong><c:out value="${debt.formattedTotalAmount}"/></strong> · Cọc 30%: <strong><c:out value="${debt.formattedDepositAmount}"/></strong> · Còn nợ: <strong style="color:#b91c1c;"><c:out value="${debt.formattedRemainingAmount}"/></strong></p>
            <p>Phương thức: <strong><c:out value="${debt.paymentMethod}"/></strong> <c:if test="${not empty debt.company}">· Đối tác: <strong style="color:#0284c7;"><c:out value="${debt.company}"/></strong></c:if> · Trạng thái đơn: <c:out value="${debt.orderStatus}"/> · Hợp đồng: <c:out value="${debt.contractStatus}" default="Chưa ký"/></p>
            <div style="margin:6px 0;display:flex;gap:12px;flex-wrap:wrap;font-size:12px;">
              <a href="${pageContext.request.contextPath}/staff/sales-document?kind=contract&amp;orderId=${debt.orderId}&amp;view=html" target="_blank" style="color:#0284c7;font-weight:600;">👁️ Xem Hợp đồng</a>
              <a href="${pageContext.request.contextPath}/staff/sales-document?kind=contract&amp;orderId=${debt.orderId}" style="color:#0284c7;">📥 Tải HĐ (Word)</a>
              <c:if test="${debt.orderStatus eq 'DEPOSITED' or debt.orderStatus eq 'PROCESSING' or debt.orderStatus eq 'PAID' or debt.orderStatus eq 'COMPLETED'}">
                <a href="${pageContext.request.contextPath}/staff/sales-document?kind=deposit&amp;orderId=${debt.orderId}&amp;view=html" target="_blank" style="color:#059669;font-weight:600;">👁️ Xem Biên nhận cọc</a>
                <a href="${pageContext.request.contextPath}/staff/sales-document?kind=deposit&amp;orderId=${debt.orderId}" style="color:#059669;">📥 Tải BN cọc (Word)</a>
              </c:if>
              <c:if test="${debt.orderStatus eq 'PAID' or debt.orderStatus eq 'COMPLETED'}">
                <a href="${pageContext.request.contextPath}/staff/sales-document?kind=invoice&amp;orderId=${debt.orderId}&amp;view=html" target="_blank" style="color:#7c3aed;font-weight:600;">👁️ Xem Hóa đơn</a>
                <a href="${pageContext.request.contextPath}/staff/sales-document?kind=invoice&amp;orderId=${debt.orderId}" style="color:#7c3aed;">📥 Tải HĐơn (Word)</a>
              </c:if>
            </div>
            <c:if test="${debt.paymentMethod eq 'INSTALLMENT' and debt.contractStatus eq 'SIGNED' and (debt.orderStatus eq 'DEPOSITED' or debt.orderStatus eq 'PROCESSING')}">
              <form method="post" action="<%= request.getContextPath() %>/staff/payment-review">
                <input type="hidden" name="csrfToken" value="<c:out value='${csrfTokenForPayment}'/>">
                <input type="hidden" name="action" value="partner">
                <input type="hidden" name="orderId" value="<c:out value='${debt.orderId}'/>">
                <button type="submit">Đồng bộ giải ngân 70% sau khi ký</button>
              </form>
            </c:if>
          </article>
        </c:forEach>
    </section>

    <h2>Giao dịch đang chờ xác nhận (<c:out value="${pendingPayments.size()}"/>)</h2>
    <c:if test="${empty pendingPayments}"><p>Không có khoản PENDING.</p></c:if>
    <c:forEach var="p" items="${pendingPayments}">
      <section class="finance-panel">
        <h3>Khoản #<c:out value="${p.paymentId}"/> · Đơn <c:out value="${p.orderCode}"/> (#<c:out value="${p.orderId}"/>)</h3>
        <p>Khách hàng: <strong><c:out value="${p.customer}"/></strong> — Loại: <strong><c:out value="${p.formattedType}"/></strong> — Kênh: <strong><c:out value="${p.methodName}"/></strong></p>
        <p>Số tiền yêu cầu: <strong style="color:#0284c7;font-size:16px;"><c:out value="${p.formattedAmount}"/></strong></p>
        <p>Mã thanh toán SePay / Nội dung chuyển khoản: <code><c:out value="${p.reference}"/></code></p>
        <p>Trạng thái tự động: <c:choose><c:when test="${p.bankReported}"><strong style="color:#166534;">Đã nhận giao dịch đối soát SePay / ngân hàng.</strong> <small><c:out value="${p.note}"/></small></c:when><c:otherwise>Kiểm tra chứng từ/sao kê thủ công trước khi quyết định.</c:otherwise></c:choose></p>
        <div style="margin:4px 0 8px;font-size:12px;color:#64748b;">
            <span>📄 Chứng từ: </span>
            <a href="${pageContext.request.contextPath}/staff/sales-document?kind=contract&amp;orderId=${p.orderId}&amp;view=html" target="_blank" style="color:#0284c7;font-weight:600;">👁️ HĐ mua bán</a>
            · <a href="${pageContext.request.contextPath}/staff/sales-document?kind=contract&amp;orderId=${p.orderId}" style="color:#0284c7;">📥 HĐ Word</a>
            · <span style="color:#94a3b8;font-style:italic;">Biên nhận cọc: Chưa phát hành (chờ xác nhận tiền)</span>
        </div>
        <form method="post" action="<%= request.getContextPath() %>/staff/payment-review" style="margin-top:12px;background:#f8fafc;padding:12px;border-radius:8px;border:1px solid #e2e8f0;">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfTokenForPayment}'/>">
            <input type="hidden" name="paymentId" value="<c:out value='${p.paymentId}'/>">
            <input type="hidden" name="action" value="confirm">
            <label style="display:block;margin-bottom:8px;">Số tiền thực nhận từ sao kê (VND):
                <input name="receivedAmount" type="number" step="1" min="1" value="<fmt:formatNumber value='${p.amount}' pattern='0' maxFractionDigits='0'/>" required style="padding:6px;width:100%;max-width:280px;margin-top:4px;">
            </label>
            <label style="display:block;margin-bottom:8px;">Mã sao kê hoặc chứng từ giao dịch thực tế:
                <input name="bankTransaction" maxlength="80" pattern="[A-Za-z0-9./_-]{4,80}" value="<c:out value='${p.reference}'/>" required style="padding:6px;width:100%;max-width:280px;margin-top:4px;">
            </label>
            <label style="display:flex;align-items:center;gap:6px;margin:8px 0;"><input type="checkbox" name="checkedStatement" required> Tôi đã đối chiếu sao kê/ngân hàng/chứng từ và khoản này thực sự được nhận đủ.</label>
            <button type="submit" class="button button-primary" style="background:#16a34a;color:#fff;padding:8px 16px;border:none;border-radius:6px;cursor:pointer;">Xác nhận tiền đã nhận</button>
        </form>
        <form method="post" action="<%= request.getContextPath() %>/staff/payment-review" style="margin-top:8px;">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfTokenForPayment}'/>">
            <input type="hidden" name="paymentId" value="<c:out value='${p.paymentId}'/>">
            <input type="hidden" name="action" value="reject">
            <label>Lý do từ chối: <input name="reason" maxlength="200" required style="padding:4px 8px;"></label>
            <button type="submit" class="finance-reject" style="background:#dc2626;color:#fff;padding:4px 10px;border:none;border-radius:6px;cursor:pointer;">Từ chối yêu cầu</button>
        </form>
      </section>
    </c:forEach>

    <h2 id="completed">Khoản đã xác nhận &amp; Đối soát giao dịch (<c:out value="${completedPayments.size()}"/>)</h2>
    <c:if test="${empty completedPayments}"><p>Chưa có khoản đã xác nhận.</p></c:if>
    <c:forEach var="p" items="${completedPayments}">
        <section class="finance-panel">
            <div style="display:flex;justify-content:space-between;align-items:center;">
                <strong>Khoản #<c:out value="${p.paymentId}"/> · Đơn <c:out value="${p.orderCode}"/> (#<c:out value="${p.orderId}"/>) · <c:out value="${p.customer}"/></strong>
                <span class="status-pill" style="background:#dcfce7;color:#166534;border:1px solid #86efac;font-size:11px;font-weight:700;padding:3px 8px;border-radius:4px;">COMPLETED</span>
            </div>
            <p style="margin:6px 0;">Phương thức: <strong><c:out value="${p.methodName}"/></strong> · Loại: <strong><c:out value="${p.formattedType}"/></strong> · Mã thanh toán SePay / Nội dung chuyển khoản: <code><c:out value="${p.reference}"/></code></p>
            <p style="margin:4px 0;">Số tiền thực nhận: <strong style="color:#166534;font-size:15px;"><c:out value="${p.formattedReceivedAmount}"/></strong> · Thời gian: <c:out value="${p.formattedDate}"/></p>

            <div style="margin:6px 0;display:flex;gap:12px;flex-wrap:wrap;font-size:12px;">
                <c:if test="${p.type eq 'DEPOSIT'}">
                    <a href="${pageContext.request.contextPath}/staff/sales-document?kind=deposit&amp;orderId=${p.orderId}&amp;view=html" target="_blank" style="color:#0284c7;font-weight:600;">👁️ Xem biên nhận cọc</a>
                    <a href="${pageContext.request.contextPath}/staff/sales-document?kind=deposit&amp;orderId=${p.orderId}" style="color:#0284c7;">📥 Tải BN cọc (Word)</a>
                </c:if>
                <a href="${pageContext.request.contextPath}/staff/sales-document?kind=contract&amp;orderId=${p.orderId}&amp;view=html" target="_blank" style="color:#475569;font-weight:600;">👁️ Xem HĐ mua bán</a>
                <a href="${pageContext.request.contextPath}/staff/sales-document?kind=contract&amp;orderId=${p.orderId}" style="color:#475569;">📥 Tải HĐ (Word)</a>
                <a href="${pageContext.request.contextPath}/staff/sales-document?kind=invoice&amp;orderId=${p.orderId}&amp;view=html" target="_blank" style="color:#7c3aed;font-weight:600;">👁️ Xem Hóa đơn</a>
                <a href="${pageContext.request.contextPath}/staff/sales-document?kind=invoice&amp;orderId=${p.orderId}" style="color:#7c3aed;">📥 Tải HĐơn (Word)</a>
            </div>
            <c:if test="${not empty p.sePayEvidence}">
                <div style="background:#f0fdf4;border:1px solid #86efac;border-radius:6px;padding:6px 10px;margin:6px 0;font-size:12px;color:#166534;">
                    Mã GD SePay: <strong>#<c:out value="${p.sePayEvidence.eventId}"/></strong>
                    · Cổng: <strong><c:out value="${p.sePayEvidence.gateway}" default="MBBank"/></strong>
                    · Ngày: <c:out value="${p.sePayEvidence.payDate}" default="—"/>
                    · Tiền ngân hàng: <strong style="color:#059669;"><c:out value="${p.formattedReceivedAmount}"/></strong>
                </div>
            </c:if>

            <%-- BƯỚC ĐỐI SOÁT KẾ TOÁN --%>
            <div style="margin-top:10px;padding-top:8px;border-top:1px dashed #cbd5e1;display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:8px;">
                <c:choose>
                    <c:when test="${p.reconciled}">
                        <div style="display:flex;align-items:center;gap:6px;">
                            <span style="background:#dcfce7;color:#166534;border:1px solid #86efac;padding:4px 10px;border-radius:6px;font-weight:700;font-size:12px;">✓ ĐÃ ĐỐI SOÁT</span>
                            <small style="color:#64748b;">(Kế toán #<c:out value="${p.reconciledBy}" default="—"/> · Lúc <c:out value="${p.reconciledAt}" default="—"/>)</small>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div style="display:flex;align-items:center;gap:10px;">
                            <span style="background:#fef3c7;color:#92400e;border:1px solid #fde68a;padding:4px 10px;border-radius:6px;font-weight:700;font-size:12px;">⚠ CHƯA ĐỐI SOÁT</span>
                            <button type="button" class="btn-trigger-reconcile"
                                    data-payment-id="${p.paymentId}"
                                    data-order-code="${p.orderCode}"
                                    data-ref="${p.reference}"
                                    data-amount="${p.formattedReceivedAmount}"
                                    data-type="${p.formattedType}"
                                    data-method="${p.methodName}"
                                    style="background:#059669;color:#fff;border:none;padding:6px 14px;border-radius:6px;font-weight:600;font-size:12px;cursor:pointer;">
                                Xác nhận đã đối soát
                            </button>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>
    </c:forEach>
</main>

<%-- POPUP XÁC NHẬN ĐỐI SOÁT CHO KẾ TOÁN --%>
<div id="accountantReconcileModal" style="display:none;position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(15,23,42,0.6);backdrop-filter:blur(4px);z-index:9999;align-items:center;justify-content:center;">
    <div style="background:#fff;border-radius:12px;max-width:480px;width:92%;padding:22px;box-shadow:0 20px 25px -5px rgba(0,0,0,0.1);position:relative;">
        <h3 style="margin:0 0 12px;font-size:17px;color:#0f172a;">Xác nhận đối soát giao dịch</h3>
        <table style="width:100%;font-size:13px;border-collapse:collapse;margin-bottom:14px;">
            <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:5px 0;color:#64748b;">Mã thanh toán:</td><td style="padding:5px 0;text-align:right;"><strong id="recModalPaymentId">—</strong></td></tr>
            <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:5px 0;color:#64748b;">Mã đơn hàng:</td><td style="padding:5px 0;text-align:right;"><strong id="recModalOrderCode">—</strong></td></tr>
            <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:5px 0;color:#64748b;">Mã tham chiếu:</td><td style="padding:5px 0;text-align:right;"><code id="recModalRef">—</code></td></tr>
            <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:5px 0;color:#64748b;">Loại khoản:</td><td style="padding:5px 0;text-align:right;"><strong id="recModalType">—</strong></td></tr>
            <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:5px 0;color:#64748b;">Phương thức:</td><td style="padding:5px 0;text-align:right;"><span id="recModalMethod">—</span></td></tr>
            <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:5px 0;color:#64748b;">Số tiền thực nhận:</td><td style="padding:5px 0;text-align:right;"><strong id="recModalAmount" style="color:#16a34a;font-size:15px;">—</strong></td></tr>
        </table>
        <div style="background:#f0fdf4;border:1px solid #86efac;border-radius:6px;padding:8px 10px;font-size:12px;color:#166534;margin-bottom:14px;">
            ℹ Bạn đã kiểm tra kỹ mã sao kê hoặc biên nhận thu tiền thực tế? Sau khi xác nhận, giao dịch sẽ được đánh dấu <strong>ĐÃ ĐỐI SOÁT</strong> cố định trong hệ thống.
        </div>
        <form method="post" action="<%= request.getContextPath() %>/staff/payment-review">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfTokenForPayment}'/>">
            <input type="hidden" name="action" value="reconcile">
            <input type="hidden" name="paymentId" id="recModalInputPaymentId" value="">
            <div style="display:flex;justify-content:flex-end;gap:8px;">
                <button type="button" id="btnCancelRecModal" style="padding:7px 14px;border:1px solid #cbd5e1;background:#fff;border-radius:6px;cursor:pointer;">Không</button>
                <button type="submit" style="padding:7px 16px;border:none;background:#059669;color:#fff;border-radius:6px;font-weight:600;cursor:pointer;">Có, Xác nhận đối soát</button>
            </div>
        </form>
    </div>
</div>

<script>
document.addEventListener("DOMContentLoaded", function() {
    var modal = document.getElementById("accountantReconcileModal");
    var btnCancel = document.getElementById("btnCancelRecModal");
    var recPaymentId = document.getElementById("recModalPaymentId");
    var recOrderCode = document.getElementById("recModalOrderCode");
    var recRef = document.getElementById("recModalRef");
    var recType = document.getElementById("recModalType");
    var recMethod = document.getElementById("recModalMethod");
    var recAmount = document.getElementById("recModalAmount");
    var recInput = document.getElementById("recModalInputPaymentId");

    function closeModal() {
        if (modal) modal.style.display = "none";
    }

    if (btnCancel) btnCancel.addEventListener("click", closeModal);
    if (modal) {
        modal.addEventListener("click", function(e) {
            if (e.target === modal) closeModal();
        });
    }

    document.querySelectorAll(".btn-trigger-reconcile").forEach(function(btn) {
        btn.addEventListener("click", function() {
            var pid = btn.getAttribute("data-payment-id");
            recPaymentId.textContent = "#" + pid;
            recOrderCode.textContent = btn.getAttribute("data-order-code");
            recRef.textContent = btn.getAttribute("data-ref");
            recType.textContent = btn.getAttribute("data-type");
            recMethod.textContent = btn.getAttribute("data-method");
            recAmount.textContent = btn.getAttribute("data-amount");
            recInput.value = pid;
            modal.style.display = "flex";
        });
    });
});
</script>
</body></html>
