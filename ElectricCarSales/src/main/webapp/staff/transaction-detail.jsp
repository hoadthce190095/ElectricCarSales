<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<c:if test="${empty orderControllerLoaded}">
    <c:choose>
        <c:when test="${not empty param.id}">
            <c:redirect url="/staff/transaction/detail?id=${param.id}"/>
        </c:when>
        <c:otherwise>
            <c:redirect url="/staff/transactions"/>
        </c:otherwise>
    </c:choose>
</c:if>
<%
    request.setAttribute("staffTitle", "Chi tiết Đơn hàng & Lịch sử Trạng thái");
    request.setAttribute("staffActive", "transactions");
%>
<%@ include file="../common/staff-shell.jspf" %>

<div class="page-heading">
    <div>
        <div class="eyebrow">
            <span class="eyebrow-line"></span>
            ORDER DETAIL &amp; STATUS HISTORY
        </div>
        <h1>
            Chi tiết Đơn hàng
            <c:if test="${not empty order}">#GD-${order.id}</c:if>
        </h1>
        <p>Thông tin chi tiết đơn hàng (Order &amp; OrderDetails) và nhật ký chuyển đổi trạng thái (OrderStatusHistory).</p>
    </div>
    <div class="heading-actions">
        <a class="button button-secondary" href="${pageContext.request.contextPath}/orders">Xem giao diện Khách hàng</a>
        <a class="button" href="${pageContext.request.contextPath}/staff/transactions">← Quay lại Danh sách</a>
    </div>
</div>

<c:if test="${param.statusUpdated eq 'true'}">
    <div class="alert-banner success">
        <span>✓ Đã cập nhật trạng thái đơn hàng và ghi nhận vào bảng OrderStatusHistory thành công!</span>
    </div>
</c:if>
<c:if test="${param.error eq 'true'}">
    <div class="alert-banner danger">
        <span>! Có lỗi xảy ra khi cập nhật trạng thái đơn hàng.</span>
    </div>
</c:if>

<c:choose>
    <c:when test="${not empty order}">
        <section class="detail-grid" style="margin-bottom: 16px;">
            <article class="detail-card">
                <span>KHÁCH HÀNG SỞ HỮU ĐƠN HÀNG</span>
                <strong style="font-size: 16px; margin-top: 6px;">${empty order.customerName ? 'Khách hàng' : order.customerName}</strong>
                <div style="margin-top: 6px; font-size: 12.5px; color: #585a60;">
                    <div>Mã KH: <strong>#${order.customerId}</strong> · SĐT: <strong>${empty order.customerPhone ? '-' : order.customerPhone}</strong></div>
                    <c:if test="${not empty order.customerEmail}">
                        <div style="margin-top: 2px;">Email: ${order.customerEmail}</div>
                    </c:if>
                </div>
            </article>

            <article class="detail-card">
                <span>THÔNG TIN GIAO DỊCH &amp; THANH TOÁN</span>
                <strong style="font-size: 16px; margin-top: 6px;">${empty order.paymentMethodName ? 'Thanh toán tiêu chuẩn' : order.paymentMethodName}</strong>
                <div style="margin-top: 6px; font-size: 12.5px; color: #585a60;">
                    <div>NV phụ trách: <strong>${empty order.employeeName ? 'Chưa phân công' : order.employeeName}</strong></div>
                    <div style="margin-top: 2px;">
                        Ngày tạo:
                        <c:choose>
                            <c:when test="${not empty order.createdAt}">
                                <fmt:formatDate value="${order.createdAt}" pattern="dd/MM/yyyy HH:mm"/>
                            </c:when>
                            <c:otherwise>-</c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </article>

            <article class="detail-card">
                <span>TỔNG GIÁ TRỊ ĐƠN HÀNG &amp; TRẠNG THÁI</span>
                <strong style="font-size: 19px; color: #e31837; margin-top: 6px;">
                    <fmt:formatNumber value="${order.originalAmount}" type="number" groupingUsed="true"/> ₫
                </strong>
                <div style="margin-top: 8px;">
                    <c:choose>
                        <c:when test="${order.statusId == 6}">
                            <span class="status-pill success">${empty order.statusName ? 'Đã hoàn tất' : order.statusName}</span>
                        </c:when>
                        <c:when test="${order.statusId == 7}">
                            <span class="status-pill danger">${empty order.statusName ? 'Đã hủy' : order.statusName}</span>
                        </c:when>
                        <c:when test="${order.statusId == 2 || order.statusId == 3}">
                            <span class="status-pill warning">${empty order.statusName ? 'Đang xử lý' : order.statusName}</span>
                        </c:when>
                        <c:otherwise>
                            <span class="status-pill info">${empty order.statusName ? 'Đang xử lý' : order.statusName}</span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </article>
        </section>

        <div class="dashboard-grid">
            <div style="display: flex; flex-direction: column; gap: 14px;">
                <section class="panel table-panel">
                    <div class="panel-head">
                        <div>
                            <h2 class="panel-title">Chi tiết Xe trong Đơn hàng (OrderDetails)</h2>
                            <p class="panel-subtitle">Thông tin dòng xe, màu sắc và đơn giá được liên kết trực tiếp với đơn hàng #GD-${order.id}</p>
                        </div>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead>
                                <tr>
                                    <th>MÃ CHI TIẾT</th>
                                    <th>MÃ XE (CAR ID)</th>
                                    <th>DÒNG XE &amp; PHIÊN BẢN</th>
                                    <th>MÀU SẮC</th>
                                    <th>SỐ LƯỢNG</th>
                                    <th>ĐƠN GIÁ</th>
                                    <th>THÀNH TIỀN</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:choose>
                                    <c:when test="${not empty details}">
                                        <c:forEach var="d" items="${details}">
                                            <tr>
                                                <td><span class="cell-primary">#OD-${d.id}</span></td>
                                                <td><span class="cell-primary">#CAR-${d.carId}</span></td>
                                                <td><span class="cell-primary">${empty d.vehicleName ? order.vehicleName : d.vehicleName}</span></td>
                                                <td>${empty d.vehicleColor ? order.vehicleColor : d.vehicleColor}</td>
                                                <td>${d.quantity}</td>
                                                <td><fmt:formatNumber value="${d.unitPrice}" type="number" groupingUsed="true"/> ₫</td>
                                                <td>
                                                    <span class="cell-primary" style="color: #e31837;">
                                                        <fmt:formatNumber value="${d.subtotal}" type="number" groupingUsed="true"/> ₫
                                                    </span>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <tr>
                                            <td colspan="7" class="table-empty">Chưa có bản ghi OrderDetails cho đơn hàng này.</td>
                                        </tr>
                                    </c:otherwise>
                                </c:choose>
                            </tbody>
                        </table>
                    </div>
                </section>

                <section class="panel" style="padding: 18px;">
                    <h2 class="panel-title">Cập nhật Trạng thái Đơn hàng (OrderStatusController)</h2>
                    <p class="panel-subtitle" style="margin-bottom: 14px;">Khi thay đổi trạng thái, hệ thống tự động cập nhật bảng Orders và thêm bản ghi mới vào OrderStatusHistory.</p>
                    <form action="${pageContext.request.contextPath}/staff/order-status/update" method="POST" style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
                        <input type="hidden" name="orderId" value="${order.id}">
                        <select name="statusId" style="height: 38px; min-width: 260px; padding: 0 12px; border: 1px solid #dcdde1; border-radius: 6px; font-size: 13px;" required>
                            <c:forEach var="st" items="${statusList}">
                                <option value="${st.statusId}" ${order.statusId == st.statusId ? 'selected' : ''}>
                                    ${st.statusId} - ${st.description}
                                </option>
                            </c:forEach>
                        </select>
                        <button type="submit" class="button">Cập nhật &amp; Ghi lịch sử trạng thái</button>
                    </form>
                </section>
            </div>

            <aside class="panel">
                <div class="panel-head">
                    <div>
                        <h2 class="panel-title">Lịch sử Trạng thái (OrderStatusHistory)</h2>
                        <p class="panel-subtitle">Theo dõi toàn bộ quá trình xử lý đơn hàng</p>
                    </div>
                </div>
                <div class="activity-list" style="padding: 0 18px 16px;">
                    <c:choose>
                        <c:when test="${not empty history}">
                            <c:forEach var="h" items="${history}">
                                <div class="activity-row" style="padding: 12px 0;">
                                    <span class="activity-marker ${h.statusId == 6 ? 'green' : (h.statusId == 7 ? '' : 'amber')}" style="width: 24px; height: 24px; font-size: 11px;">
                                        ${h.statusId == 6 ? '✓' : '◷'}
                                    </span>
                                    <div class="activity-copy">
                                        <strong style="font-size: 13px; color: #191a1e;">
                                            ${empty h.statusName ? h.status : h.statusName}
                                        </strong>
                                        <span style="font-size: 12px; color: #585a60; margin-top: 3px;">
                                            Cập nhật bởi: <strong>${empty h.changedByName ? 'Hệ thống / Nhân viên' : h.changedByName}</strong>
                                        </span>
                                        <span style="font-size: 11.5px; color: #888b92; margin-top: 2px;">
                                            Thời gian:
                                            <c:choose>
                                                <c:when test="${not empty h.changedAt}">
                                                    <fmt:formatDate value="${h.changedAt}" pattern="dd/MM/yyyy HH:mm:ss"/>
                                                </c:when>
                                                <c:otherwise>-</c:otherwise>
                                            </c:choose>
                                        </span>
                                    </div>
                                </div>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <div class="table-empty">Chưa có lịch sử trạng thái nào được ghi nhận.</div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </aside>
        </div>
    </c:when>
    <c:otherwise>
        <section class="panel" style="padding: 40px; text-align: center;">
            <div class="empty-icon">!</div>
            <strong style="font-size: 15px;">Không tìm thấy đơn hàng yêu cầu</strong>
            <p style="color: #70737b; font-size: 13px; margin: 8px 0 16px;">Đơn hàng này không tồn tại trong cơ sở dữ liệu hoặc đường dẫn không hợp lệ.</p>
            <a class="button" href="${pageContext.request.contextPath}/staff/transactions">Quay lại Danh sách Đơn hàng</a>
        </section>
    </c:otherwise>
</c:choose>

<%@ include file="../common/staff-shell-end.jspf" %>
