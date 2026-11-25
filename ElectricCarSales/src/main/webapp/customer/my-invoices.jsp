<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    // Theo yêu cầu I: Loại bỏ trang Hóa đơn riêng, chuyển hướng về trang Thanh toán tích hợp hóa đơn
    response.sendRedirect(request.getContextPath() + "/payment/history#invoices");
    return;
%>
<div class="page-intro">
    <p>Tra cứu hóa đơn chính thức được Kế toán phát hành sau khi đơn hàng hoàn tất thanh toán 100% giá trị xe.</p>
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
                                <td><fmt:formatNumber value="${inv.subTotal}" pattern="#,##0"/> VND</td>
                                <td><strong><fmt:formatNumber value="${inv.totalAmount}" pattern="#,##0"/> VND</strong></td>
                                <td><span class="soft-badge"><c:out value="${inv.status}"/></span></td>
                                <td>
                                    <c:out value="${inv.note}" default="—"/>
                                    <c:if test="${inv.status eq 'ISSUED'}">
                                        <c:url var="previewHtmlUrl" value="/customer/sales-document">
                                            <c:param name="kind" value="invoice"/>
                                            <c:param name="orderId" value="${inv.orderID}"/>
                                            <c:param name="view" value="html"/>
                                        </c:url>
                                        <c:url var="downloadWordUrl" value="/customer/sales-document">
                                            <c:param name="kind" value="invoice"/>
                                            <c:param name="orderId" value="${inv.orderID}"/>
                                        </c:url>
                                        <div style="margin-top:6px;display:flex;gap:8px;">
                                            <a class="button button-secondary" style="padding:4px 8px;font-size:11px;" href="<c:out value='${previewHtmlUrl}'/>">👁️ Xem hóa đơn</a>
                                            <a class="button button-primary" style="padding:4px 8px;font-size:11px;" href="<c:out value='${downloadWordUrl}'/>">📥 Tải Word</a>
                                        </div>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td colspan="7">
                                <div class="table-empty">
                                    <span>▨</span>
                                    <strong>Chưa có hóa đơn nào được phát hành</strong>
                                    <p>Theo quy trình chuẩn: Hóa đơn GTGT chỉ được Kế toán phát hành sau khi Hợp đồng đã ký (SIGNED) và đơn hàng hoàn tất 100% nghĩa vụ thanh toán (công nợ = 0).</p>
                                    <c:if test="${not empty customerOrders}">
                                        <div style="margin-top:16px;text-align:left;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:12px 16px;">
                                            <strong style="font-size:13px;color:#0f172a;display:block;margin-bottom:8px;">Tiến độ các đơn hàng hiện có của bạn:</strong>
                                            <c:forEach var="ord" items="${customerOrders}">
                                                <div style="font-size:12px;padding:6px 0;border-bottom:1px dashed #e2e8f0;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:6px;">
                                                    <span><strong>Đơn hàng #${ord.orderID}</strong> · Tổng: <fmt:formatNumber value="${ord.totalAmount}" pattern="#,##0"/> VND (Còn thiếu: <fmt:formatNumber value="${ord.remainingAmount}" pattern="#,##0"/> VND)</span>
                                                    <span style="color:#b45309;font-weight:600;">
                                                        <c:choose>
                                                            <c:when test="${ord.remainingAmount le 0}">
                                                                ✓ Đã hoàn tất 100% tiền — Đang chờ Kế toán phát hành Hóa đơn
                                                            </c:when>
                                                            <c:when test="${ord.depositAmount gt 0 and ord.orderStatusID eq 3}">
                                                                Đã cọc 30% — Cần hoàn tất ký Hợp đồng và thanh toán 70% còn lại
                                                            </c:when>
                                                            <c:otherwise>
                                                                Chưa hoàn tất thanh toán — <a href="<%= customerContext %>/orders">Xem đơn hàng</a>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </span>
                                                </div>
                                            </c:forEach>
                                        </div>
                                    </c:if>
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
