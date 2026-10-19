<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<% request.setAttribute("customerTitle", "Đơn hàng của tôi"); request.setAttribute("customerEyebrow", "GIAO DỊCH CỦA BẠN"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="page-intro split-intro">
    <p>Theo dõi đơn hàng và tiến độ giao dịch thuộc tài khoản của bạn.</p>
    <span class="soft-badge">
        <i></i> Dữ liệu cá nhân</span>
</div>
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
                    <th>
                    </th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${not empty orders}">
                        <c:forEach var="o" items="${orders}">
                            <tr>
                                <td><strong>#GD-${o.id}</strong></td>
                                <td>${o.createdAt}</td>
                                <td>${empty o.vehicleName ? 'VinFast VF 8 Plus' : o.vehicleName}</td>
                                <td><fmt:formatNumber value="${o.originalAmount}" type="currency" currencySymbol="₫"/></td>
                                <td><span class="status-pill ${o.status eq '6' ? 'success' : 'info'}">${empty o.statusName ? 'Đang xử lý' : o.statusName}</span></td>
                                <td>
                                    <a href="${pageContext.request.contextPath}/order/detail?id=${o.id}" class="text-button" style="color: #0071e3; font-weight: 600; text-decoration: none;">Xem chi tiết →</a>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td><strong style="color: #0071e3;">#GD-00842</strong></td>
                            <td>19/10/2026</td>
                            <td>VinFast VF 8 Plus</td>
                            <td>1.299.000.000 ₫</td>
                            <td><span class="status-pill success" style="background: rgba(46, 204, 113, 0.15); color: #2ecc71; padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 600;">Đang xử lý</span></td>
                            <td>
                                <a href="${pageContext.request.contextPath}/order/detail?id=1" class="text-button" style="color: #0071e3; font-weight: 600; text-decoration: none;">Xem chi tiết →</a>
                            </td>
                        </tr>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
    <div class="table-foot">
        <span>Đang hiển thị <strong>0</strong> giao dịch</span>
        <div class="pagination">
            <button disabled aria-label="Trang trước">←</button>
            <span>1</span>
            <button disabled aria-label="Trang tiếp">→</button>
        </div>
    </div>
</section>
<%@ include file="../common/customer-shell-end.jspf" %>
