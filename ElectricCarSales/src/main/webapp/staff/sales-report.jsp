<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<c:if test="${empty reportControllerLoaded}">
    <c:redirect url="/staff/report"/>
</c:if>
<%
    request.setAttribute("staffTitle", "Báo cáo Doanh thu & Giao dịch");
    request.setAttribute("staffActive", "report");
%>
<%@ include file="../common/staff-shell.jspf" %>

<div class="page-heading">
    <div>
        <div class="eyebrow">
            <span class="eyebrow-line"></span>
            SALES &amp; CONSULTATION ANALYTICS
        </div>
        <h1>Báo cáo Doanh thu &amp; Thống kê Giao dịch</h1>
        <p>Tổng hợp số liệu thực tế từ bảng Orders, OrderDetails và ConsultationRequests trong cơ sở dữ liệu.</p>
    </div>
    <div class="heading-actions">
        <a class="button button-secondary" href="${pageContext.request.contextPath}/staff/consultations">Yêu cầu Tư vấn</a>
        <a class="button" href="${pageContext.request.contextPath}/staff/transactions">Quản lý Đơn hàng →</a>
    </div>
</div>

<section class="metric-grid">
    <article class="metric-card">
        <div class="metric-head">
            <span>TỔNG DOANH THU GHI NHẬN</span>
            <span class="metric-icon">₫</span>
        </div>
        <strong><fmt:formatNumber value="${empty totalRevenue ? 0 : totalRevenue}" type="number" groupingUsed="true"/> ₫</strong>
        <div class="metric-foot">Từ các đơn hàng hợp lệ trong CSDL</div>
    </article>
    <article class="metric-card">
        <div class="metric-head">
            <span>SỐ XE TRONG ĐƠN HÀNG</span>
            <span class="metric-icon green">▤</span>
        </div>
        <strong>${empty carsSold ? 0 : carsSold} xe</strong>
        <div class="metric-foot">Tổng số lượng xe giao dịch</div>
    </article>
    <article class="metric-card">
        <div class="metric-head">
            <span>ĐƠN ĐÃ HOÀN TẤT</span>
            <span class="metric-icon blue">✓</span>
        </div>
        <strong>${empty countCompleted ? 0 : countCompleted}</strong>
        <div class="metric-foot">Đã bàn giao xe thành công</div>
    </article>
    <article class="metric-card">
        <div class="metric-head">
            <span>TƯ VẤN CHỜ XỬ LÝ</span>
            <span class="metric-icon amber">✧</span>
        </div>
        <strong>${empty pendingConsultations ? 0 : pendingConsultations}</strong>
        <div class="metric-foot">Yêu cầu tư vấn chưa tiếp nhận</div>
    </article>
</section>

<div class="dashboard-grid" style="margin-bottom: 16px;">
    <section class="panel table-panel">
        <div class="panel-head">
            <div>
                <h2 class="panel-title">Thống kê Doanh thu theo Dòng xe</h2>
                <p class="panel-subtitle">Tổng hợp từ bảng Orders &amp; OrderDetails</p>
            </div>
        </div>
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>DÒNG XE VINFAST</th>
                        <th>SỐ LƯỢNG ĐƠN</th>
                        <th>DOANH THU GHI NHẬN</th>
                    </tr>
                </thead>
                <tbody>
                    <c:choose>
                        <c:when test="${not empty vehicleStats}">
                            <c:forEach var="vs" items="${vehicleStats}">
                                <tr>
                                    <td><span class="cell-primary">${vs.modelName}</span></td>
                                    <td><span class="status-pill info">${vs.count} xe</span></td>
                                    <td>
                                        <span class="cell-primary" style="color: #e31837;">
                                            <fmt:formatNumber value="${vs.revenue}" type="number" groupingUsed="true"/> ₫
                                        </span>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td colspan="3" class="table-empty">Chưa có dữ liệu giao dịch theo dòng xe.</td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>
    </section>

    <aside class="panel">
        <div class="panel-head">
            <div>
                <h2 class="panel-title">Phân bổ Trạng thái Đơn hàng</h2>
                <p class="panel-subtitle">Theo bảng OrderStatuses</p>
            </div>
        </div>
        <div class="task-list" style="padding: 0 18px 14px;">
            <div class="task-row">
                <span class="task-icon amber">◷</span>
                <div class="task-copy">
                    <strong style="font-size: 13px;">Chờ đặt cọc</strong>
                    <span style="font-size: 11.5px;">Đơn hàng mới tạo chờ khách đặt cọc</span>
                </div>
                <span class="task-count" style="font-size: 12px; width: 26px; height: 26px;">${empty countPending ? 0 : countPending}</span>
            </div>
            <div class="task-row">
                <span class="task-icon">◉</span>
                <div class="task-copy">
                    <strong style="font-size: 13px;">Đã đặt cọc / Chờ hợp đồng</strong>
                    <span style="font-size: 11.5px;">Đang hoàn thiện thủ tục hợp đồng mua bán</span>
                </div>
                <span class="task-count" style="font-size: 12px; width: 26px; height: 26px;">${empty countDeposited ? 0 : countDeposited}</span>
            </div>
            <div class="task-row">
                <span class="task-icon">▤</span>
                <div class="task-copy">
                    <strong style="font-size: 13px;">Chờ thanh toán / Chờ bàn giao</strong>
                    <span style="font-size: 11.5px;">Đang tất toán và chuẩn bị xe bàn giao</span>
                </div>
                <span class="task-count" style="font-size: 12px; width: 26px; height: 26px;">${empty countProcessing ? 0 : countProcessing}</span>
            </div>
            <div class="task-row">
                <span class="task-icon green">✓</span>
                <div class="task-copy">
                    <strong style="font-size: 13px;">Đã hoàn tất</strong>
                    <span style="font-size: 11.5px;">Đã bàn giao xe và hoàn thành hồ sơ</span>
                </div>
                <span class="task-count" style="font-size: 12px; width: 26px; height: 26px;">${empty countCompleted ? 0 : countCompleted}</span>
            </div>
            <div class="task-row">
                <span class="task-icon">✕</span>
                <div class="task-copy">
                    <strong style="font-size: 13px;">Đã hủy</strong>
                    <span style="font-size: 11.5px;">Giao dịch đã hủy bỏ</span>
                </div>
                <span class="task-count" style="font-size: 12px; width: 26px; height: 26px;">${empty countCancelled ? 0 : countCancelled}</span>
            </div>
        </div>
    </aside>
</div>

<section class="panel table-panel">
    <div class="panel-head">
        <div>
            <h2 class="panel-title">Danh sách Giao dịch Ghi nhận trong Báo cáo</h2>
            <p class="panel-subtitle">Chi tiết các đơn hàng từ CSDL</p>
        </div>
    </div>
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>MÃ GIAO DỊCH</th>
                    <th>NGÀY TẠO</th>
                    <th>KHÁCH HÀNG</th>
                    <th>DÒNG XE</th>
                    <th>NHÂN VIÊN PHỤ TRÁCH</th>
                    <th>GIÁ TRỊ</th>
                    <th>TRẠNG THÁI</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${not empty orders}">
                        <c:forEach var="o" items="${orders}">
                            <tr>
                                <td><a href="${pageContext.request.contextPath}/staff/transaction/detail?id=${o.id}" class="cell-primary" style="color: #e31837;">#GD-${o.id}</a></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty o.createdAt}">
                                            <fmt:formatDate value="${o.createdAt}" pattern="dd/MM/yyyy HH:mm"/>
                                        </c:when>
                                        <c:otherwise>-</c:otherwise>
                                    </c:choose>
                                </td>
                                <td><span class="cell-primary">${empty o.customerName ? '-' : o.customerName}</span></td>
                                <td>${empty o.vehicleName ? '-' : o.vehicleName}</td>
                                <td>${empty o.employeeName ? '-' : o.employeeName}</td>
                                <td><strong><fmt:formatNumber value="${o.originalAmount}" type="number" groupingUsed="true"/> ₫</strong></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${o.statusId == 6}">
                                            <span class="status-pill success">${empty o.statusName ? 'Đã hoàn tất' : o.statusName}</span>
                                        </c:when>
                                        <c:when test="${o.statusId == 7}">
                                            <span class="status-pill danger">${empty o.statusName ? 'Đã hủy' : o.statusName}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="status-pill info">${empty o.statusName ? 'Đang xử lý' : o.statusName}</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td colspan="7" class="table-empty">Chưa có giao dịch nào trong cơ sở dữ liệu.</td>
                        </tr>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
</section>

<%@ include file="../common/staff-shell-end.jspf" %>
