<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%
    String appContext = request.getContextPath();
    String roleName = session == null ? "" : String.valueOf(session.getAttribute("roleName"));
    if (!("SALES".equalsIgnoreCase(roleName) || "OPERATIONS".equalsIgnoreCase(roleName)
            || "ACCOUNTANT".equalsIgnoreCase(roleName) || "MANAGER".equalsIgnoreCase(roleName)
            || "ADMIN".equalsIgnoreCase(roleName))) {
        response.sendRedirect(appContext + "/login");
        return;
    }
    boolean inventoryPage = Boolean.TRUE.equals(request.getAttribute("inventoryPage"))
            || request.getRequestURI().contains("/staff/inventory")
            || (request.getAttribute("jakarta.servlet.forward.request_uri") != null
                && String.valueOf(request.getAttribute("jakarta.servlet.forward.request_uri")).contains("/staff/inventory"));
    if (inventoryPage && !("OPERATIONS".equalsIgnoreCase(roleName)
            || "MANAGER".equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(roleName))) {
        response.sendError(403);
        return;
    }
%>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#111214">
    <title><%= inventoryPage ? "Quản lý tồn kho" : "Tổng quan nội bộ" %> · VinFast - ElectricCarSale</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="<%= appContext %>/assets/css/staff.css">

    <!-- Nạp staff.js để chạy menu bên trái và các phân hệ dashboard -->
    <script src="<%= appContext %>/assets/js/staff.js?v=sales-order-actions-20261117c" defer></script>
</head>
<body data-context-path="<%= appContext %>">
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
                <strong id="breadcrumb-title"><%= inventoryPage ? "Quản lý tồn kho" : "Tổng quan" %></strong>
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
                        <strong id="profile-name"><c:out value="${empty sessionScope.displayName ? sessionScope.username : sessionScope.displayName}" default="Nhân viên" /></strong>
                        <small id="profile-role">Nhân viên</small>
                    </span>
                    <span class="profile-chevron">⌄</span>
                </div>
            </div>
        </header>

        <%-- Dashboard sẽ được staff.js dựng theo vai trò khi ở dashboard. Khi ở Servlet /staff/inventory, hiển thị giao diện tồn kho hoàn chỉnh --%>
        <main class="staff-content p-4" id="staff-content" tabindex="-1" aria-live="polite" style="overflow-y: auto;">
          <% if (!inventoryPage) { %>
            <% if ("ACCOUNTANT".equalsIgnoreCase(roleName) || "MANAGER".equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(roleName)) { %>
              <p><a href="<%= appContext %>/staff/dashboard#payments">Đối chiếu thanh toán thật →</a> | <a href="<%= appContext %>/invoice">Quản lý &amp; Phát hành hóa đơn (Invoices) →</a></p>
            <% } %>
            <% if ("SALES".equalsIgnoreCase(roleName) || "MANAGER".equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(roleName)) { %>
              <p><a href="<%= appContext %>/orders">Lập đơn bán xe →</a> | <a href="<%= appContext %>/consultation">Quản lý Yêu cầu tư vấn →</a></p>
            <% } %>
          <% } %>

          <% if (inventoryPage) { %>
            <div class="page-heading" style="margin-bottom:20px;">
                <div>
                    <div class="eyebrow"><span class="eyebrow-line"></span>RDS 9.1 &amp; 10.5 · QUẢN LÝ TỒN KHO XE VẬT LÝ</div>
                    <h1 style="font-size:24px;margin:4px 0 8px;">Quản lý tồn kho xe điện VinFast</h1>
                    <p style="color:#64748b;font-size:14px;margin:0;">Theo dõi chi tiết từng xe vật lý theo số khung VIN, số máy, màu sắc, phiên bản và trạng thái thực tế trong kho.</p>
                </div>
            </div>

            <c:if test="${not empty inventoryLoadError}">
                <div role="alert" style="padding:14px 18px;background:#fef2f2;border:1px solid #fca5a5;border-radius:10px;color:#991b1b;margin-bottom:20px;">
                    <strong>⚠ Lỗi cơ sở dữ liệu:</strong> <c:out value="${inventoryLoadError}" />
                </div>
            </c:if>

            <!-- KPI THỐNG KÊ TỒN KHO THEO THỜI GIAN THỰC -->
            <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(180px, 1fr));gap:14px;margin-bottom:24px;">
                <div style="background:#fff;padding:16px;border-radius:12px;border:1px solid #e2e8f0;box-shadow:0 1px 3px rgba(0,0,0,0.05);">
                    <div style="font-size:12px;color:#64748b;font-weight:600;text-transform:uppercase;">Tổng xe trong kho</div>
                    <div style="font-size:26px;font-weight:700;color:#0f172a;margin-top:4px;">
                        <c:out value="${inventoryStats.totalCars}" default="0" />
                    </div>
                    <small style="color:#94a3b8;">Tổng số xe vật lý ghi nhận</small>
                </div>
                <div style="background:#fff;padding:16px;border-radius:12px;border:1px solid #bbf7d0;background:#f0fdf4;box-shadow:0 1px 3px rgba(0,0,0,0.05);">
                    <div style="font-size:12px;color:#166534;font-weight:600;text-transform:uppercase;">Xe có sẵn (AVAILABLE)</div>
                    <div style="font-size:26px;font-weight:700;color:#15803d;margin-top:4px;">
                        <c:out value="${inventoryStats.availableCars}" default="0" />
                    </div>
                    <small style="color:#166534;">Khả dụng để bán / tạo đơn</small>
                </div>
                <div style="background:#fff;padding:16px;border-radius:12px;border:1px solid #fef08a;background:#fefce8;box-shadow:0 1px 3px rgba(0,0,0,0.05);">
                    <div style="font-size:12px;color:#854d0e;font-weight:600;text-transform:uppercase;">Đang giữ chỗ (RESERVED)</div>
                    <div style="font-size:26px;font-weight:700;color:#a16207;margin-top:4px;">
                        <c:out value="${inventoryStats.reservedCars}" default="0" />
                    </div>
                    <small style="color:#854d0e;">Đã gắn đơn hàng chờ cọc</small>
                </div>
                <div style="background:#fff;padding:16px;border-radius:12px;border:1px solid #bae6fd;background:#f0f9ff;box-shadow:0 1px 3px rgba(0,0,0,0.05);">
                    <div style="font-size:12px;color:#075985;font-weight:600;text-transform:uppercase;">Đã đặt cọc (DEPOSITED)</div>
                    <div style="font-size:26px;font-weight:700;color:#0284c7;margin-top:4px;">
                        <c:out value="${inventoryStats.depositedCars}" default="0" />
                    </div>
                    <small style="color:#075985;">Đã cọc 30% chờ ký HĐ</small>
                </div>
                <div style="background:#fff;padding:16px;border-radius:12px;border:1px solid #e2e8f0;background:#f8fafc;box-shadow:0 1px 3px rgba(0,0,0,0.05);">
                    <div style="font-size:12px;color:#475569;font-weight:600;text-transform:uppercase;">Đã bán / Bàn giao</div>
                    <div style="font-size:26px;font-weight:700;color:#334155;margin-top:4px;">
                        <c:out value="${inventoryStats.soldDeliveredCars}" default="0" />
                    </div>
                    <small style="color:#64748b;">SOLD / DELIVERED</small>
                </div>
            </div>

            <!-- THANH TÌM KIẾM VÀ LỌC TRẠNG THÁI (RDS 9.1.3 & 9.1.4) -->
            <section style="background:#fff;padding:16px;border-radius:12px;border:1px solid #cbd5e1;margin-bottom:20px;">
                <form method="get" action="<%= appContext %>/staff/inventory" style="display:flex;gap:12px;flex-wrap:wrap;align-items:center;">
                    <div style="flex:1;min-width:240px;">
                        <input type="text" name="q" value="<c:out value='${searchKeyword}' />" placeholder="Tìm theo số khung VIN, số máy, mã xe, tên xe..."
                               style="width:100%;padding:9px 12px;border:1px solid #cbd5e1;border-radius:8px;font-size:13px;box-sizing:border-box;">
                    </div>
                    <div style="min-width:200px;">
                        <select name="status" style="width:100%;padding:9px 12px;border:1px solid #cbd5e1;border-radius:8px;font-size:13px;box-sizing:border-box;">
                            <option value="">-- Tất cả trạng thái --</option>
                            <option value="AVAILABLE" ${selectedStatus eq 'AVAILABLE' ? 'selected' : ''}>AVAILABLE (Có sẵn)</option>
                            <option value="RESERVED" ${selectedStatus eq 'RESERVED' ? 'selected' : ''}>RESERVED (Đang giữ chỗ)</option>
                            <option value="DEPOSITED" ${selectedStatus eq 'DEPOSITED' ? 'selected' : ''}>DEPOSITED (Đã đặt cọc)</option>
                            <option value="SOLD" ${selectedStatus eq 'SOLD' ? 'selected' : ''}>SOLD (Đã thanh toán đủ)</option>
                            <option value="DELIVERED" ${selectedStatus eq 'DELIVERED' ? 'selected' : ''}>DELIVERED (Đã bàn giao/xuất)</option>
                        </select>
                    </div>
                    <button type="submit" class="button button-primary" style="padding:9px 18px;font-size:13px;">
                        🔍 Tìm kiếm / Lọc
                    </button>
                    <c:if test="${not empty searchKeyword or not empty selectedStatus}">
                        <a href="<%= appContext %>/staff/inventory" class="button button-secondary" style="padding:9px 14px;font-size:13px;text-decoration:none;">
                            ✕ Xóa bộ lọc
                        </a>
                    </c:if>
                </form>
            </section>

            <!-- DANH SÁCH XE VẬT LÝ TRONG KHO (RDS 9.1 & 10.4) -->
            <div style="background:#fff;border-radius:12px;border:1px solid #e2e8f0;overflow-x:auto;box-shadow:0 1px 3px rgba(0,0,0,0.05);margin-bottom:24px;">
                <table style="width:100%;border-collapse:collapse;font-size:13px;text-align:left;">
                    <thead>
                    <tr style="background:#f8fafc;border-bottom:2px solid #e2e8f0;color:#475569;font-size:12px;text-transform:uppercase;">
                        <th style="padding:12px 14px;">MÃ XE</th>
                        <th style="padding:12px 14px;">MẪU XE &amp; PHIÊN BẢN</th>
                        <th style="padding:12px 14px;">MÀU SẮC</th>
                        <th style="padding:12px 14px;">SỐ KHUNG (VIN)</th>
                        <th style="padding:12px 14px;">SỐ MÁY</th>
                        <th style="padding:12px 14px;">GIÁ BÁN</th>
                        <th style="padding:12px 14px;">NGÀY NHẬP / NCC</th>
                        <th style="padding:12px 14px;">TRẠNG THÁI</th>
                        <th style="padding:12px 14px;">THAO TÁC</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:choose>
                        <c:when test="${not empty inventoryCars}">
                            <c:forEach var="car" items="${inventoryCars}">
                                <tr style="border-bottom:1px solid #f1f5f9;">
                                    <td style="padding:12px 14px;">
                                        <strong><c:out value="${car.carCode}" /></strong>
                                        <br><small style="color:#94a3b8;">ID: #${car.carID}</small>
                                    </td>
                                    <td style="padding:12px 14px;">
                                        <strong><c:out value="${car.modelName}" default="Chưa có mẫu" /></strong>
                                        <br><small style="color:#64748b;"><c:out value="${car.versionName}" default="Tiêu chuẩn" /></small>
                                    </td>
                                    <td style="padding:12px 14px;">
                                        <c:out value="${car.colorName}" default="Tiêu chuẩn" />
                                    </td>
                                    <td style="padding:12px 14px;">
                                        <code style="background:#f1f5f9;padding:3px 6px;border-radius:4px;font-weight:600;color:#0f172a;"><c:out value="${car.vin}" /></code>
                                    </td>
                                    <td style="padding:12px 14px;">
                                        <code style="background:#f8fafc;padding:3px 6px;border-radius:4px;color:#334155;"><c:out value="${car.engineNumber}" /></code>
                                    </td>
                                    <td style="padding:12px 14px;font-weight:600;">
                                        <fmt:formatNumber value="${car.price}" pattern="#,###" /> VNĐ
                                    </td>
                                    <td style="padding:12px 14px;color:#64748b;">
                                        <c:choose>
                                            <c:when test="${not empty car.importDate}">
                                                <fmt:formatDate value="${car.importDate}" pattern="dd/MM/yyyy HH:mm" />
                                            </c:when>
                                            <c:otherwise>
                                                <span>Kho ban đầu</span>
                                            </c:otherwise>
                                        </c:choose>
                                        <c:if test="${not empty car.supplierName}">
                                            <br><small style="color:#94a3b8;">NCC: <c:out value="${car.supplierName}" /></small>
                                        </c:if>
                                    </td>
                                    <td style="padding:12px 14px;">
                                        <c:choose>
                                            <c:when test="${car.statusName eq 'AVAILABLE' or car.statusName eq 'Có sẵn'}">
                                                <span class="status-pill" style="background:#dcfce7;color:#166534;border:1px solid #bbf7d0;padding:4px 8px;border-radius:999px;font-weight:600;font-size:11px;">AVAILABLE</span>
                                            </c:when>
                                            <c:when test="${car.statusName eq 'RESERVED' or car.statusName eq 'Đã giữ chỗ'}">
                                                <span class="status-pill" style="background:#fef9c3;color:#854d0e;border:1px solid #fef08a;padding:4px 8px;border-radius:999px;font-weight:600;font-size:11px;">RESERVED</span>
                                            </c:when>
                                            <c:when test="${car.statusName eq 'DEPOSITED' or car.statusName eq 'Đã đặt cọc'}">
                                                <span class="status-pill" style="background:#e0f2fe;color:#075985;border:1px solid #bae6fd;padding:4px 8px;border-radius:999px;font-weight:600;font-size:11px;">DEPOSITED</span>
                                            </c:when>
                                            <c:when test="${car.statusName eq 'SOLD' or car.statusName eq 'Đã bán'}">
                                                <span class="status-pill" style="background:#f3e8ff;color:#6b21a8;border:1px solid #e9d5ff;padding:4px 8px;border-radius:999px;font-weight:600;font-size:11px;">SOLD</span>
                                            </c:when>
                                            <c:when test="${car.statusName eq 'DELIVERED' or car.statusName eq 'Đã bàn giao' or car.statusName eq 'EXPORTED'}">
                                                <span class="status-pill" style="background:#f1f5f9;color:#475569;border:1px solid #e2e8f0;padding:4px 8px;border-radius:999px;font-weight:600;font-size:11px;">DELIVERED</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="status-pill" style="background:#f1f5f9;color:#334155;border:1px solid #cbd5e1;padding:4px 8px;border-radius:999px;font-size:11px;"><c:out value="${car.statusName}" /></span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td style="padding:12px 14px;">
                                        <a href="<%= appContext %>/staff/inventory/detail?modelId=${car.modelID}" style="color:#0284c7;font-weight:600;text-decoration:none;">
                                            Chi tiết lô &rarr;
                                        </a>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td colspan="9" style="text-align:center;padding:40px 20px;color:#64748b;">
                                    <div style="font-size:16px;font-weight:600;color:#334155;margin-bottom:6px;">Chưa có dữ liệu tồn kho</div>
                                    <p style="margin:0 0 14px;">Không tìm thấy xe vật lý nào trong kho phù hợp với điều kiện tìm kiếm hoặc lọc hiện tại.</p>
                                    <a href="<%= appContext %>/staff/dashboard#vehicle-movements" class="button button-primary" style="text-decoration:none;font-size:12px;padding:6px 12px;">
                                        + Nhập kho xe mới (IMPORT)
                                    </a>
                                </td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                    </tbody>
                </table>
            </div>

            <!-- BẢNG TỔNG QUAN THEO MẪU XE (RDS 10.5) -->
            <c:if test="${not empty inventoryList}">
                <div style="margin-top:20px;background:#fff;border-radius:12px;border:1px solid #e2e8f0;padding:16px;box-shadow:0 1px 3px rgba(0,0,0,0.05);">
                    <h3 style="font-size:15px;margin:0 0 12px;color:#0f172a;">Tổng quan số lượng tồn kho theo mẫu xe &amp; màu sắc (RDS 10.5)</h3>
                    <div style="overflow-x:auto;">
                        <table style="width:100%;border-collapse:collapse;font-size:13px;text-align:left;">
                            <thead>
                            <tr style="background:#f8fafc;border-bottom:2px solid #e2e8f0;color:#64748b;font-size:12px;">
                                <th style="padding:10px 12px;">MẪU XE</th>
                                <th style="padding:10px 12px;">MÀU SẮC</th>
                                <th style="padding:10px 12px;">VỊ TRÍ</th>
                                <th style="padding:10px 12px;">TRẠNG THÁI</th>
                                <th style="padding:10px 12px;">SỐ LƯỢNG</th>
                                <th style="padding:10px 12px;">XEM CHI TIẾT</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="item" items="${inventoryList}">
                                <tr style="border-bottom:1px solid #f1f5f9;">
                                    <td style="padding:10px 12px;font-weight:600;">${item.modelName}</td>
                                    <td style="padding:10px 12px;">${item.color}</td>
                                    <td style="padding:10px 12px;color:#64748b;">${item.location}</td>
                                    <td style="padding:10px 12px;">
                                        <span class="status-pill" style="font-size:11px;padding:2px 8px;border-radius:4px;background:#f1f5f9;">
                                            <c:out value="${item.statusName}" />
                                        </span>
                                    </td>
                                    <td style="padding:10px 12px;font-weight:700;">${item.quantity}</td>
                                    <td style="padding:10px 12px;">
                                        <a href="<%= appContext %>/staff/inventory/detail?modelId=${item.modelId}&location=${item.location}" style="color:#0284c7;text-decoration:none;font-weight:600;">
                                            Chi tiết lô xe &rarr;
                                        </a>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </c:if>
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
