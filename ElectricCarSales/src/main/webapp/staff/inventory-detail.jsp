<%--
  Created by IntelliJ IDEA.
  User: ADMIN
  Date: 10/28/2026
  Time: 4:40 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%
    String appContext = request.getContextPath();
%>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#111214">
    <title>Chi tiết tồn kho · VinFast - ElectricCarSale</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link
        href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&family=Manrope:wght@400;500;600;700;800&display=swap"
        rel="stylesheet">
    <link rel="stylesheet" href="<%= appContext %>/assets/css/staff.css">

    <!-- LƯU Ý: Vẫn giữ staff.js để các nút menu/sidebar hoạt động -->
    <script src="<%= appContext %>/assets/js/staff.js?v=logout-role-2" defer></script>
</head>
<body data-context-path="<%= appContext %>">
<%
    String roleName = session == null ? "" : String.valueOf(session.getAttribute("roleName"));
    if (!("OPERATIONS".equalsIgnoreCase(roleName) || "MANAGER".equalsIgnoreCase(roleName)
            || "ADMIN".equalsIgnoreCase(roleName))) {
        response.sendError(403);
        return;
    }
%>
<div class="staff-app" data-staff-app data-staff-role="<c:out value='${sessionScope.roleName}' />">
    <!-- BỘ KHUNG SIDEBAR BÊN TRÁI -->
    <aside class="staff-sidebar" id="staff-sidebar">
        <a class="staff-brand" href="<%= appContext %>/staff/inventory">
            <span class="staff-brand-mark">V</span>
            <span class="staff-brand-copy">
                <strong>VinFast</strong>
                <small>ELECTRIC CAR SALE</small>
            </span>
        </a>
        <div class="workspace-caption">KHÔNG GIAN NỘI BỘ</div>
        <nav class="staff-nav" id="staff-nav"></nav>
        <div class="sidebar-foot">
            <i></i><span>VinFast ElectricCarSale</span><small>NỘI BỘ</small>
        </div>
    </aside>

    <!-- BỘ KHUNG MAIN CONTENT -->
    <div class="staff-main">
        <header class="staff-topbar">
            <button class="mobile-menu" type="button" data-menu-toggle>☰</button>
            <div class="breadcrumb">
                <span>VinFast</span> <i>/</i> <span>Quản lý tồn kho</span> <i>/</i>
                <strong>Chi tiết lô xe</strong>
            </div>
            <div class="topbar-actions">
                <form method="post" action="<%= appContext %>/logout" style="margin:0">
                    <button class="staff-logout-btn" type="submit">Đăng xuất</button>
                </form>
                <div class="staff-profile">
                    <span class="profile-avatar" id="profile-avatar">NV</span>
                    <span class="profile-copy">
                        <strong id="profile-name"><c:out value="${empty sessionScope.displayName ? sessionScope.username : sessionScope.displayName}" default="Nhân viên" /></strong>
                        <small id="profile-role">Nhân viên</small>
                    </span>
                </div>
            </div>
        </header>

        <!-- NỘI DUNG BẢNG CHI TIẾT XE VẬT LÝ -->
        <main class="staff-content p-4" style="overflow-y: auto;">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h4 class="fw-bold">Chi tiết kho: ${selectedLocation}</h4>
                <!-- Đổi thành class nút bấm của staff.css để đồng bộ giao diện -->
                <a href="<%= appContext %>/staff/inventory" class="button button-secondary"
                   style="text-decoration: none;">
                    &larr; Trở về tổng quan
                </a>
            </div>

            <div class="table-responsive bg-white rounded shadow-sm">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                    <tr>
                        <th>Số khung (VIN)</th>
                        <th>Số máy</th>
                        <th>Màu sắc</th>
                        <th>Trạng thái xe</th>
                        <th>Giá bán</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:choose>
                        <c:when test="${not empty carDetails}">
                            <c:forEach var="car" items="${carDetails}">
                                <tr>
                                    <td class="fw-bold text-primary">${car.vin}</td>
                                    <td>${car.engineNumber}</td>
                                    <td>${car.color}</td>
                                    <td>
                                        <!-- Tự động tô màu nút theo trạng thái sử dụng class status-pill -->
                                        <c:choose>
                                            <c:when test="${car.status == 'AVAILABLE' || car.status == 'Có sẵn'}">
                                                <span class="status-pill success"><c:out value="${car.status}" /></span>
                                            </c:when>
                                            <c:when test="${car.status == 'RESERVED' || car.status == 'Đã giữ chỗ'}">
                                                <span class="status-pill warning"><c:out value="${car.status}" /></span>
                                            </c:when>
                                            <c:when test="${car.status == 'DEPOSITED' || car.status == 'Đã đặt cọc'}">
                                                <span class="status-pill warning"><c:out value="${car.status}" /></span>
                                            </c:when>
                                            <c:when test="${car.status == 'SOLD' || car.status == 'Đã bán'}">
                                                <span class="status-pill info"><c:out value="${car.status}" /></span>
                                            </c:when>
                                            <c:when test="${car.status == 'DELIVERED' || car.status == 'Đã bàn giao' || car.status == 'EXPORTED'}">
                                                <span class="status-pill info"><c:out value="${car.status}" /></span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="status-pill danger"><c:out value="${car.status}" /></span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <!-- Format giá tiền theo chuẩn 1,290,000,000 -->
                                    <td><fmt:formatNumber value="${car.price}" pattern="#,###"/> VNĐ</td>
                                </tr>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td colspan="5" style="text-align: center; padding: 20px;">
                                    Không tìm thấy xe vật lý nào trong kho này.
                                </td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                    </tbody>
                </table>
            </div>
        </main>
    </div>
</div>
</body>
</html>
