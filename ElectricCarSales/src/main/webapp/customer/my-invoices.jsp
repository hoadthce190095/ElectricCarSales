<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<% request.setAttribute("customerTitle", "Hóa đơn"); request.setAttribute("customerEyebrow", "CHỨNG TỪ THANH TOÁN"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="page-intro">
    <p>Tra cứu hóa đơn, tổng giá trị và trạng thái phát hành theo giao dịch của bạn.</p>
</div>
<c:if test="${not empty success}"><p role="status"><c:out value="${success}"/></p></c:if>
<c:if test="${not empty error}"><p role="alert"><c:out value="${error}"/></p></c:if>
<c:if test="${invoiceRole eq 'ACCOUNTANT'}">
    <section class="panel">
        <div class="panel-heading"><div><span class="section-overline">KẾ TOÁN</span><h2>Phát hành hóa đơn</h2></div></div>
        <p>Nhập mã đơn hàng. Hệ thống kiểm tra hợp đồng đã ký, trạng thái thanh toán và chống phát hành trùng.</p>
        <form action="${pageContext.request.contextPath}/invoice" method="post">
            <label>Mã đơn hàng <input type="number" min="1" name="orderId" required></label>
            <button class="edit-link" type="submit">Kiểm tra và phát hành</button>
        </form>
    </section>
</c:if>
<section class="invoice-overview">
    <div class="invoice-metric">
        <span class="metric-icon">▨</span>
        <span>Tổng số hóa đơn</span>
        <strong><c:out value="${fn:length(invoices)}"/></strong>
        <small>Được phát hành</small>
    </div>
</section>
<section class="panel table-panel invoice-list">
    <div class="panel-heading">
        <div>
            <span class="section-overline">LỊCH SỬ HÓA ĐƠN</span>
            <h2>Danh sách hóa đơn</h2>
        </div>
        <label class="search-field">
            <span>⌕</span>
            <input type="search" placeholder="Tìm số hóa đơn" aria-label="Tìm số hóa đơn">
        </label>
    </div>
    <div class="table-wrap">
        <table>
            <thead>
            <tr>
                <th>SỐ HÓA ĐƠN</th>
                <th>NGÀY PHÁT HÀNH</th>
                <th>MÃ ĐƠN HÀNG</th>
                <th>TỔNG TIỀN</th>
                <th>TRẠNG THÁI</th>
                <th>TÀI LIỆU</th>
            </tr>
            </thead>
            <tbody>
            <c:choose>
                <c:when test="${empty invoices}">
                    <tr><td colspan="6"><div class="table-empty"><span>▨</span><strong>Chưa có hóa đơn</strong><p>Hóa đơn của các giao dịch đã phát hành sẽ có thể xem tại đây.</p></div></td></tr>
                </c:when>
                <c:otherwise>
                    <c:forEach var="invoice" items="${invoices}">
                        <tr>
                            <td><c:out value="${invoice.invoiceNumber}"/></td>
                            <td><c:out value="${invoice.invoiceDate}"/></td>
                            <td><c:out value="${invoice.orderID}"/></td>
                            <td><c:out value="${invoice.totalAmount}"/></td>
                            <td><c:out value="${invoice.status}"/></td>
                            <td>—</td>
                        </tr>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
            </tbody>
        </table>
    </div>
</section>
<%@ include file="../common/customer-shell-end.jspf" %>
