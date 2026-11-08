<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<c:if test="${empty consultationControllerLoaded}">
    <c:redirect url="/staff/consultations"/>
</c:if>
<%
    request.setAttribute("staffTitle", "Quản lý Yêu cầu Tư vấn");
    request.setAttribute("staffActive", "consultations");
%>
<%@ include file="../common/staff-shell.jspf" %>

<div class="page-heading">
    <div>
        <div class="eyebrow">
            <span class="eyebrow-line"></span>
            CONSULTATION &amp; SALES ORDER
        </div>
        <h1>Tiếp nhận &amp; Quản lý Yêu cầu Tư vấn</h1>
        <p>Theo dõi nhu cầu khách hàng từ CSDL, cập nhật tiến độ tư vấn và khởi tạo giao dịch bán xe.</p>
    </div>
    <div class="heading-actions">
        <a class="button button-secondary" href="${pageContext.request.contextPath}/staff/transactions">Danh sách Đơn hàng →</a>
    </div>
</div>

<c:if test="${param.updated eq 'true'}">
    <div class="alert-banner success">
        <span>✓ Đã cập nhật trạng thái yêu cầu tư vấn trong cơ sở dữ liệu thành công.</span>
    </div>
</c:if>
<c:if test="${param.error eq 'true'}">
    <div class="alert-banner danger">
        <span>! Có lỗi xảy ra khi cập nhật trạng thái yêu cầu tư vấn.</span>
    </div>
</c:if>

<section class="metric-grid">
    <article class="metric-card">
        <div class="metric-head">
            <span>TỔNG YÊU CẦU TƯ VẤN</span>
            <span class="metric-icon blue">✧</span>
        </div>
        <strong>${empty totalCount ? 0 : totalCount}</strong>
        <div class="metric-foot">Toàn bộ yêu cầu lưu trong hệ thống</div>
    </article>
    <article class="metric-card">
        <div class="metric-head">
            <span>CHƯA TIẾP NHẬN</span>
            <span class="metric-icon amber">◷</span>
        </div>
        <strong>${empty pendingCount ? 0 : pendingCount}</strong>
        <div class="metric-foot">Cần nhân viên kinh doanh liên hệ</div>
    </article>
    <article class="metric-card">
        <div class="metric-head">
            <span>ĐANG XỬ LÝ</span>
            <span class="metric-icon">▤</span>
        </div>
        <strong>${empty inProgressCount ? 0 : inProgressCount}</strong>
        <div class="metric-foot">Đang trao đổi báo giá &amp; phiên bản xe</div>
    </article>
    <article class="metric-card">
        <div class="metric-head">
            <span>ĐÃ LIÊN HỆ</span>
            <span class="metric-icon green">✓</span>
        </div>
        <strong>${empty completedCount ? 0 : completedCount}</strong>
        <div class="metric-foot">Sẵn sàng tạo giao dịch bán xe</div>
    </article>
</section>

<section class="panel table-panel">
    <div class="panel-head">
        <div>
            <h2 class="panel-title">Danh sách Yêu cầu Tư vấn Khách hàng</h2>
            <p class="panel-subtitle">Dữ liệu đồng bộ trực tiếp từ bảng ConsultationRequests và ConsultationStatuses</p>
        </div>
    </div>

    <form class="toolbar" action="${pageContext.request.contextPath}/staff/consultations" method="GET">
        <input type="search" name="q" value="${searchKeyword}" placeholder="Tìm mã YC, tên khách hàng, SĐT, mẫu xe...">
        <select name="status">
            <option value="">-- Tất cả trạng thái --</option>
            <c:forEach var="st" items="${statusList}">
                <option value="${st.statusId}" ${selectedStatus eq st.statusCode || selectedStatus eq ''.concat(st.statusId) ? 'selected' : ''}>
                    ${st.description}
                </option>
            </c:forEach>
        </select>
        <button type="submit" class="button">Lọc dữ liệu</button>
        <c:if test="${not empty selectedStatus or not empty searchKeyword}">
            <a href="${pageContext.request.contextPath}/staff/consultations" class="button button-secondary">Xóa bộ lọc</a>
        </c:if>
    </form>

    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>MÃ YC / NGÀY GỬI</th>
                    <th>KHÁCH HÀNG</th>
                    <th>MẪU XE QUAN TÂM</th>
                    <th>NỘI DUNG TƯ VẤN</th>
                    <th>TRẠNG THÁI</th>
                    <th>CẬP NHẬT TRẠNG THÁI</th>
                    <th>TẠO ĐƠN HÀNG</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${not empty consultationList}">
                        <c:forEach var="item" items="${consultationList}">
                            <tr>
                                <td>
                                    <span class="cell-primary">#YC-${item.id}</span>
                                    <span class="cell-secondary">
                                        <c:choose>
                                            <c:when test="${not empty item.createdAt}">
                                                <fmt:formatDate value="${item.createdAt}" pattern="dd/MM/yyyy HH:mm"/>
                                            </c:when>
                                            <c:otherwise>-</c:otherwise>
                                        </c:choose>
                                    </span>
                                </td>
                                <td>
                                    <span class="cell-primary">${empty item.customerName ? 'Khách hàng' : item.customerName}</span>
                                    <span class="cell-secondary">
                                        SĐT: ${empty item.customerPhone ? '-' : item.customerPhone}
                                        <c:if test="${not empty item.customerEmail}"> · ${item.customerEmail}</c:if>
                                    </span>
                                </td>
                                <td>
                                    <span class="cell-primary">${empty item.modelName ? 'Chưa chọn mẫu' : item.modelName}</span>
                                    <span class="cell-secondary">NV: ${empty item.employeeName ? 'Chưa phân công' : item.employeeName}</span>
                                </td>
                                <td style="max-width: 280px;">
                                    ${item.content}
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${item.statusId == 1}">
                                            <span class="status-pill warning">${empty item.statusName ? 'Chưa tiếp nhận' : item.statusName}</span>
                                        </c:when>
                                        <c:when test="${item.statusId == 2}">
                                            <span class="status-pill info">${empty item.statusName ? 'Đang xử lý' : item.statusName}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="status-pill success">${empty item.statusName ? 'Đã liên hệ' : item.statusName}</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <form action="${pageContext.request.contextPath}/staff/consultation/update" method="POST" style="display: flex; align-items: center; gap: 6px;">
                                        <input type="hidden" name="requestId" value="${item.id}">
                                        <select name="statusId" style="height: 34px; padding: 0 8px; border: 1px solid #dcdde1; border-radius: 5px; font-size: 12px;">
                                            <c:forEach var="st" items="${statusList}">
                                                <option value="${st.statusId}" ${item.statusId == st.statusId ? 'selected' : ''}>
                                                    ${st.description}
                                                </option>
                                            </c:forEach>
                                        </select>
                                        <button type="submit" class="button button-secondary" style="min-height: 34px; padding: 0 10px;">Lưu</button>
                                    </form>
                                </td>
                                <td>
                                    <a class="button" href="${pageContext.request.contextPath}/staff/transactions?createFromConsult=${item.id}&customerId=${item.customerId}&customerName=${item.customerName}&customerPhone=${item.customerPhone}&customerEmail=${item.customerEmail}&modelId=${item.modelId}">
                                        + Tạo đơn hàng
                                    </a>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td colspan="7" class="table-empty">
                                <div class="empty-icon">✧</div>
                                <strong>Chưa có yêu cầu tư vấn nào phù hợp</strong>
                                <div style="margin-top: 4px;">Khi khách hàng gửi form tư vấn, dữ liệu sẽ hiển thị tại đây.</div>
                            </td>
                        </tr>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
    <div class="table-foot">
        <span>Đang hiển thị <strong>${empty consultationList ? 0 : consultationList.size()}</strong> / <strong>${empty totalCount ? 0 : totalCount}</strong> yêu cầu tư vấn</span>
        <span>Bảng dữ liệu: ConsultationRequests</span>
    </div>
</section>

<%@ include file="../common/staff-shell-end.jspf" %>
