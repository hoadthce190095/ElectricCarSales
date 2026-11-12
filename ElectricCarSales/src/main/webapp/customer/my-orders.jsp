<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("customerTitle", "Đơn hàng của tôi"); request.setAttribute("customerEyebrow", "GIAO DỊCH CỦA BẠN"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="page-intro split-intro">
    <p>Theo dõi đơn hàng và tiến độ giao dịch thuộc tài khoản của bạn.</p>
    <span class="soft-badge">
        <i></i> Dữ liệu cá nhân</span>
</div>
<c:if test="${param.cancelled == '1'}">
    <div class="info-note" style="margin-bottom:16px;border-color:#86efac;background:#f0fdf4;color:#166534;">
        <span>✓</span>
        <p>Đã hủy đơn hàng thành công.</p>
    </div>
</c:if>
<section class="panel table-panel">
    <div class="panel-heading">
        <div>
            <span class="section-overline">LỊCH SỬ GIAO DỊCH</span>
            <h2>Tất cả đơn hàng</h2>
        </div>
        <label class="search-field">
            <span>⌕</span>
            <input type="search" placeholder="Tìm mã đơn hàng" aria-label="Tìm mã đơn hàng">
        </label>
    </div>
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>MÃ GIAO DỊCH</th>
                    <th>NGÀY TẠO</th>
                    <th>MẪU XE</th>
                    <th>TỔNG GIÁ TRỊ</th>
                    <th>TRẠNG THÁI</th>
                    <th>THAO TÁC</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="order" items="${orders}">
                    <tr>
                        <td>#<c:out value="${order.id}"/></td>
                        <td><c:out value="${order.orderDate}"/></td>
                        <td><c:out value="${order.vehicleName}" default="—"/></td>
                        <td><c:out value="${order.originalAmount}"/> VND</td>
                        <td><span class="soft-badge"><c:out value="${order.status}"/></span></td>
                        <td>
                            <div style="display:flex;gap:6px;align-items:center;flex-wrap:wrap;">
                                <a href="<%= request.getContextPath() %>/payment/detail?orderId=<c:out value='${order.id}'/>" class="button button-secondary" style="padding:4px 8px;font-size:12px;">Công nợ</a>
                                <c:if test="${order.status eq 'PENDING' or order.status eq 'RESERVED'}">
                                    <form method="post" action="<%= request.getContextPath() %>/payment/create" style="display:inline;">
                                        <input type="hidden" name="csrfToken" value="<c:out value='${paymentCsrf}'/>">
                                        <input type="hidden" name="orderId" value="<c:out value='${order.id}'/>">
                                        <input type="hidden" name="type" value="DEPOSIT">
                                        <button type="submit" class="button button-primary" style="padding:4px 8px;font-size:12px;">QR cọc 30%</button>
                                    </form>
                                </c:if>
                                <c:if test="${order.status eq 'PENDING'}">
                                    <form method="post" action="<%= request.getContextPath() %>/order-status" style="display:inline;" onsubmit="return confirm('Bạn có chắc chắn muốn hủy đơn hàng này?');">
                                        <input type="hidden" name="orderId" value="<c:out value='${order.id}'/>">
                                        <input type="hidden" name="status" value="CANCELLED">
                                        <button type="submit" class="button button-secondary" style="padding:4px 8px;font-size:12px;color:#b91c1c;">Hủy đơn</button>
                                    </form>
                                </c:if>
                                <c:if test="${(order.status eq 'DEPOSITED' or order.status eq 'PROCESSING') and order.remainingAmount gt 0}">
                                    <form method="post" action="<%= request.getContextPath() %>/payment/create" style="display:inline;">
                                        <input type="hidden" name="csrfToken" value="<c:out value='${paymentCsrf}'/>">
                                        <input type="hidden" name="orderId" value="<c:out value='${order.id}'/>">
                                        <input type="hidden" name="type" value="REMAINING">
                                        <button type="submit" class="button button-primary" style="padding:4px 8px;font-size:12px;">QR 70% (chỉ trả thẳng)</button>
                                    </form>
                                </c:if>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty orders}">
                    <tr><td colspan="6"><div class="table-empty"><strong>Chưa có giao dịch</strong>
                        <p>Đơn hàng được Sales Staff lập sẽ xuất hiện tại đây.</p></div></td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
    <div class="table-foot">
        <span>Đang hiển thị <strong><c:out value="${orders.size()}"/></strong> giao dịch</span>
        <div class="pagination">
            <button disabled aria-label="Trang trước">←</button>
            <span>1</span>
            <button disabled aria-label="Trang tiếp">→</button>
        </div>
    </div>
</section>
<%@ include file="../common/customer-shell-end.jspf" %>
