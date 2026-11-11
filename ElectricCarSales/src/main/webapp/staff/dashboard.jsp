<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    String appContext = request.getContextPath();
%>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#111214">
    <title>Quản lý tồn kho · VinFast - ElectricCarSale</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="<%= appContext %>/assets/css/staff.css">

    <!-- Vẫn nạp staff.js để chạy menu bên trái, nhưng cần xóa đoạn code ghi đè thẻ <main> trong file JS này -->
    <script src="<%= appContext %>/assets/js/staff.js?v=staff-workflow-filter-fix-20261111" defer></script>
</head>
<body data-context-path="<%= appContext %>">
<%
    String roleName = session == null ? "" : String.valueOf(session.getAttribute("roleName"));
    if (!("SALES".equalsIgnoreCase(roleName) || "OPERATIONS".equalsIgnoreCase(roleName)
            || "ACCOUNTANT".equalsIgnoreCase(roleName) || "MANAGER".equalsIgnoreCase(roleName)
            || "ADMIN".equalsIgnoreCase(roleName))) {
        response.sendRedirect(appContext + "/login");
        return;
    }
    boolean inventoryPage = request.getRequestURI().contains("/staff/inventory");
    if (inventoryPage && !("OPERATIONS".equalsIgnoreCase(roleName)
            || "MANAGER".equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(roleName))) {
        response.sendError(403);
        return;
    }
%>
<div class="staff-app" data-staff-app data-staff-role="<c:out value='${sessionScope.roleName}' />">
    <!-- BỘ KHUNG SIDEBAR BÊN TRÁI -->
    <aside class="staff-sidebar" id="staff-sidebar">
        <a class="staff-brand" href="<%= appContext %>/staff/dashboard" aria-label="VinFast - ElectricCarSale">
            <span class="staff-brand-mark">V</span>
            <span class="staff-brand-copy">
                <strong>VinFast</strong>
                <small>ELECTRIC CAR SALE</small>
            </span>
        </a>
        <div class="workspace-caption">KHÔNG GIAN NỘI BỘ</div>
        <nav class="staff-nav" id="staff-nav" aria-label="Điều hướng nhân viên"></nav>
        <div class="sidebar-support">
            <span class="support-orbit">✳</span>
            <strong>Trung tâm hỗ trợ</strong>
            <p>Liên hệ quản trị viên khi bạn cần trợ giúp về quyền truy cập.</p>
            <a href="mailto:support@electriccarsale.vn">Gửi yêu cầu <span>↗</span></a>
        </div>
        <div class="sidebar-foot">
            <i></i>
            <span>VinFast ElectricCarSales</span>
            <small>NỘI BỘ</small>
        </div>
    </aside>

    <!-- BỘ KHUNG GIAO DIỆN CHÍNH -->
    <div class="staff-main">
        <header class="staff-topbar">
            <button class="mobile-menu" type="button" aria-label="Mở menu" aria-expanded="false" data-menu-toggle>☰</button>
            <div class="breadcrumb">
                <span>VinFast</span>
                <i>/</i>
                <strong id="breadcrumb-title">Tổng quan</strong>
            </div>
            <div class="topbar-actions">
                <span class="date-label" id="today-label"></span>
                <button class="topbar-icon" type="button" aria-label="Thông báo">♧<b></b></button>
                <form method="post" action="<%= appContext %>/logout" style="margin:0">
                    <button class="staff-logout-btn" type="submit">Đăng xuất</button>
                </form>
                <span class="topbar-divider"></span>
                <div class="staff-profile">
                    <span class="profile-avatar" id="profile-avatar">NV</span>
                    <span class="profile-copy">
                        <!-- Tên và Role đã được cập nhật chuẩn xác -->
                        <strong id="profile-name"><c:out value="${empty sessionScope.displayName ? sessionScope.username : sessionScope.displayName}" default="Nhân viên" /></strong>
                        <small id="profile-role">Nhân viên</small>
                    </span>
                    <span class="profile-chevron">⌄</span>
                </div>
            </div>
        </header>

        <%-- Dashboard sẽ được staff.js dựng theo vai trò. Chỉ hiển thị bảng khi Servlet tồn kho được gọi. --%>
        <main class="staff-content p-4" id="staff-content" tabindex="-1" aria-live="polite" style="overflow-y: auto;">
          <% if ("ACCOUNTANT".equalsIgnoreCase(roleName)) { %>
            <p><a href="<%= appContext %>/staff/dashboard#payments">Đối chiếu thanh toán thật →</a></p>
          <% } %>
          <% if ("SALES".equalsIgnoreCase(roleName)) { %>
            <p><a href="<%= appContext %>/orders">Lập đơn bán xe →</a></p>
          <% } %>
          <% if (inventoryPage) { %>
            <div class="mb-4">
                <h3 class="fw-bold">Quản lý tồn kho</h3>
                <p class="text-muted">Theo dõi số xe vật lý theo mẫu, màu, trạng thái và số lượng; xe đã gắn đơn được tính là RESERVED.</p>
            </div>

            <c:if test="${not empty inventoryLoadError}">
                <div role="alert" class="alert alert-danger"><c:out value="${inventoryLoadError}" /></div>
            </c:if>

            <div class="table-responsive bg-white rounded shadow-sm border">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                    <tr>
                        <th>MẪU XE</th>
                        <th>MÀU SẮC</th>
                        <th>VỊ TRÍ</th>
                        <th>SỐ LƯỢNG</th>
                        <th>TRẠNG THÁI</th>
                        <th>THAO TÁC</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:choose>
                        <c:when test="${not empty inventoryList}">
                            <c:forEach var="item" items="${inventoryList}">
                                <tr>
                                    <td class="fw-bold">${item.modelName}</td>
                                    <td>${item.color}</td>
                                    <td>${item.location}</td>
                                    <td>${item.quantity}</td>
                                    <td><span class="badge ${item.statusName eq 'AVAILABLE' ? 'bg-success' : item.statusName eq 'RESERVED' ? 'bg-warning text-dark' : 'bg-secondary'}"><c:out value="${item.statusName}" /></span></td>
                                    <td>
                                        <!-- URL truyền tham số chính xác sang Servlet xem chi tiết -->
                                        <a href="<%= appContext %>/staff/inventory/detail?modelId=${item.modelId}&location=${item.location}" class="text-primary fw-bold text-decoration-none">
                                            Chi tiết &rarr;
                                        </a>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td colspan="6" class="text-center text-muted py-5">
                                    Chưa có xe vật lý trong bảng Cars. Hãy dùng “Nhập / Xuất kho xe” để ghi nhận xe thật có VIN, phiên bản, màu, giá và trạng thái; danh sách khách hàng/Sales sẽ lấy cùng xe AVAILABLE chưa gắn đơn.
                                </td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                    </tbody>
                </table>
            </div>
          <% } %>
        </main>

        <footer class="staff-footer">
            <span>© 2026 VinFast - ElectricCarSales</span>
            <span>Không gian làm việc nội bộ</span>
        </footer>
    </div>
</div>
<div class="toast" id="staff-toast" role="status" aria-live="polite"></div>
</body>
</html>
