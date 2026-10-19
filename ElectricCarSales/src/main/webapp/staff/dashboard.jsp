<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String appContext = request.getContextPath();
%>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#111214">
    <title>Không gian nội bộ · VinFast - ElectricCarSale</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="<%= appContext %>/assets/css/staff.css">
    <script src="<%= appContext %>/assets/js/staff.js" defer>
    </script>
</head>
<body>
<div class="staff-app" data-staff-app data-context-path="<%= appContext %>">
    <aside class="staff-sidebar" id="staff-sidebar">
        <a class="staff-brand" href="<%= appContext %>/staff/dashboard.jsp" aria-label="VinFast - ElectricCarSale">
            <span class="staff-brand-mark">V</span>
            <span class="staff-brand-copy">
    <strong>VinFast</strong>
    <small>ELECTRIC CAR SALE</small>
    </span>
        </a>
        <div class="workspace-caption">KHÔNG GIAN NỘI BỘ</div>
        <label class="role-picker-label" for="role-picker">Vai trò xem trước</label>
        <select class="role-picker" id="role-picker" aria-label="Chọn vai trò giao diện">
            <option value="sales">Sales Staff</option>
            <option value="operations">Operations Officer</option>
            <option value="accountant">Accountant</option>
            <option value="manager">Manager</option>
        </select>
        <nav class="staff-nav" id="staff-nav" aria-label="Điều hướng nhân viên">
    </nav>

        <div class="workspace-caption" style="margin-top: 12px;">NGHIỆP VỤ</div>
        <div style="padding: 0 12px; display: flex; flex-direction: column; gap: 3px;">
            <a href="<%= appContext %>/staff/consultations" style="color: #e1e4ea; text-decoration: none; font-size: 13px; padding: 6px 10px; border-radius: 6px; display: block;">
                Yêu cầu tư vấn
            </a>
            <a href="<%= appContext %>/staff/transactions" style="color: #e1e4ea; text-decoration: none; font-size: 13px; padding: 6px 10px; border-radius: 6px; display: block;">
                Quản lý giao dịch
            </a>
            <a href="<%= appContext %>/staff/report" style="color: #e1e4ea; text-decoration: none; font-size: 13px; padding: 6px 10px; border-radius: 6px; display: block;">
                Báo cáo &amp; Thống kê
            </a>
        </div>

        <div class="sidebar-support" style="margin-top: 15px;">
            <span class="support-orbit">✳</span>
            <strong>Trung tâm hỗ trợ</strong>
            <p>Liên hệ quản trị viên khi bạn cần trợ giúp về quyền truy cập.</p>
            <a href="mailto:support@electriccarsale.vn">Gửi yêu cầu <span>↗</span>
    </a>
        </div>
        <div class="sidebar-foot">
    <i>
    </i>
    <span>VinFast ElectricCarSale</span>
    <small>NỘI BỘ</small>
    </div>
    </aside>

    <div class="staff-main">
        <header class="staff-topbar">
            <button class="mobile-menu" type="button" aria-label="Mở menu" aria-expanded="false" data-menu-toggle>☰</button>
            <div class="breadcrumb">
    <span>VinFast</span>
    <i>/</i>
    <strong id="breadcrumb-title">Tổng quan công việc</strong>
    </div>
            <div class="topbar-actions">
                <span class="date-label" id="today-label">
    </span>
                <button class="topbar-icon" type="button" aria-label="Thông báo" data-toast="Bạn đã xem các thông báo mới nhất.">♧<b>
    </b>
    </button>
                <span class="topbar-divider">
    </span>
                <button class="staff-profile" type="button" data-toast="Thông tin tài khoản sẽ được kết nối khi có backend.">
                    <span class="profile-avatar" id="profile-avatar">AN</span>
                    <span class="profile-copy">
    <strong id="profile-name">An Nguyễn</strong>
    <small id="profile-role">Nhân viên kinh doanh</small>
    </span>
                    <span class="profile-chevron">⌄</span>
                </button>
            </div>
        </header>

        <!-- Thanh liên kết nhanh đến các phân hệ nghiệp vụ -->
        <div class="staff-quick-nav" style="background: #181b20; border-bottom: 1px solid #2d3139; padding: 10px 24px; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; font-size: 13px;">
            <div style="display: flex; align-items: center; gap: 8px;">
                <span style="color: #9aa0a6;">Truy cập nhanh nghiệp vụ:</span>
            </div>
            <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                <a href="<%= appContext %>/staff/consultations" class="btn-quick-nav" style="color: #4dabf7; text-decoration: none; padding: 5px 12px; background: rgba(77, 171, 247, 0.12); border: 1px solid rgba(77, 171, 247, 0.35); border-radius: 6px; font-weight: 500;">
                    Yêu cầu tư vấn
                </a>
                <a href="<%= appContext %>/staff/transactions" class="btn-quick-nav" style="color: #69db7c; text-decoration: none; padding: 5px 12px; background: rgba(105, 219, 124, 0.12); border: 1px solid rgba(105, 219, 124, 0.35); border-radius: 6px; font-weight: 500;">
                    Quản lý giao dịch
                </a>
                <a href="<%= appContext %>/staff/report" class="btn-quick-nav" style="color: #da77f2; text-decoration: none; padding: 5px 12px; background: rgba(218, 119, 242, 0.12); border: 1px solid rgba(218, 119, 242, 0.35); border-radius: 6px; font-weight: 500;">
                    Báo cáo doanh thu &amp; giao dịch
                </a>
            </div>
        </div>

        <main class="staff-content" id="staff-content" tabindex="-1" aria-live="polite">
    </main>
        <footer class="staff-footer">
    <span>© 2026 VinFast - ElectricCarSale</span>
    <span>Không gian làm việc nội bộ</span>
    </footer>
    </div>
</div>
<div class="toast" id="staff-toast" role="status" aria-live="polite">
    </div>
</body>
</html>
