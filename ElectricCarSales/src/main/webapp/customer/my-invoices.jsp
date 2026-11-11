<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<% request.setAttribute("customerTitle", "Hóa đơn"); request.setAttribute("customerEyebrow", "CHỨNG TỪ THANH TOÁN"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="page-intro">
    <p>Tra cứu hóa đơn chính thức được Kế toán phát hành sau khi đơn hàng hoàn tất thanh toán 100% giá trị xe (RDS 9.3).</p>
</div>
<section class="invoice-overview">
    <div class="invoice-metric">
        <span class="metric-icon">▨</span>
        <span>Tổng số hóa đơn</span>
        <strong><c:out value="${not empty invoices ? invoices.size() : 0}"/></strong>
        <small>Thuộc tài khoản của bạn</small>
    </div>
    <div class="invoice-metric">
        <span class="metric-icon metric-green">✓</span>
        <span>Đã phát hành (ISSUED)</span>
        <strong><c:out value="${not empty invoices ? invoices.size() : 0}"/></strong>
        <small>Đủ điều kiện bàn giao xe</small>
    </div>
    <div class="invoice-metric">
        <span class="metric-icon metric-amber">◷</span>
        <span>Trạng thái thanh toán</span>
        <strong>100%</strong>
        <small>Đã hoàn tất nghĩa vụ</small>
    </div>
</section>
<section class="panel table-panel invoice-list">
    <div class="panel-heading">
        <div>
            <span class="section-overline">LỊCH SỬ HÓA ĐƠN (BẢNG INVOICES)</span>
            <h2>Hóa đơn của tôi</h2>
        </div>
    </div>
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>SỐ HÓA ĐƠN</th>
                    <th>NGÀY PHÁT HÀNH</th>
                    <th>MÃ ĐƠN HÀNG</th>
                    <th>TẠM TÍNH</th>
                    <th>TỔNG THANH TOÁN</th>
                    <th>TRẠNG THÁI</th>
                    <th>GHI CHÚ</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${not empty invoices}">
                        <c:forEach var="inv" items="${invoices}">
                            <tr>
                                <td><strong><c:out value="${inv.invoiceNumber}"/></strong></td>
                                <td><c:out value="${inv.invoiceDate}"/></td>
                                <td>#<c:out value="${inv.orderID}"/></td>
                                <td><fmt:formatNumber value="${inv.subTotal}" pattern="#,###"/> ₫</td>
                                <td><strong><fmt:formatNumber value="${inv.totalAmount}" pattern="#,###"/> ₫</strong></td>
                                <td><span class="soft-badge"><c:out value="${inv.status}"/></span></td>
                                <td><c:out value="${inv.note}" default="—"/></td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td colspan="7">
                                <div class="table-empty">
                                    <span>▨</span>
                                    <strong>Chưa có hóa đơn</strong>
                                    <p>Hóa đơn sẽ được Kế toán phát hành ngay sau khi hợp đồng đã ký (SIGNED) và đơn hàng thanh toán đủ 100% (RemainingAmount = 0).</p>
                                </div>
                            </td>
                        </tr>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
</section>
<%@ include file="../common/customer-shell-end.jspf" %>
