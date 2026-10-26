<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <meta name="theme-color" content="#315b9d" />
        <meta name="app-context-path" content="<c:out value='${pageContext.request.contextPath}' />" />
        <title>VinFast — Không gian quản lý xe điện</title>
        <link rel="stylesheet" href="<c:url value='/assets/css/theme.css'><c:param name='v' value='main-blue-20261017' /></c:url>" />
        <link rel="stylesheet" href="<c:url value='/assets/admin-dashboard.css'><c:param name='v' value='main-blue-20261017' /></c:url>" />
        <script src="<c:url value='/assets/admin-common.js' />" defer></script>
        <script src="<c:url value='/assets/admin-management.js' />" defer></script>
        <script src="<c:url value='/assets/sales-reports.js' />" defer></script>
        <script src="<c:url value='/assets/vehicle-catalogue.js' />" defer></script>
        <script src="<c:url value='/assets/vehicle-assistant.js' />" defer></script>
        <script src="<c:url value='/assets/admin-dashboard.js' />" defer></script>
    </head>
    <body>
        <a class="skip-link" href="#main">Đi tới nội dung chính</a>
        <div class="app-shell">
            <aside class="sidebar" id="sidebar-panel" aria-label="Điều hướng chính">
                <button type="button" id="navigation-close" class="navigation-close icon-button" aria-label="Đóng menu">×</button>
                <a class="brand" href="<c:url value='/index.jsp' />" aria-label="VinFast — Trang chủ">
                    <span class="brand-mark" aria-hidden="true">V</span>
                    <span class="brand-copy">
                        <strong>VinFast</strong>
                        <small>ELECTRIC CAR SALES</small>
                    </span>
                </a>
                <div class="workspace-label">KHÔNG GIAN LÀM VIỆC</div>
                <nav id="navigation">
                    <button class="nav-item" data-view="overview" data-access="report" hidden>
                        <span class="nav-icon">⌂</span>
                        Tổng quan
                    </button>
                    <button class="nav-item" data-view="catalogue">
                        <span class="nav-icon">▱</span>
                        Danh mục xe
                    </button>
                    <button class="nav-item" data-view="users" data-access="admin" hidden>
                        <span class="nav-icon">▤</span>
                        Tài khoản
                    </button>
                    <button class="nav-item" data-view="employees" data-access="admin" hidden>
                        <span class="nav-icon">♙</span>
                        Nhân viên
                    </button>
                    <button class="nav-item" data-view="roles" data-access="admin" hidden>
                        <span class="nav-icon">◇</span>
                        Phân quyền
                    </button>
                    <button class="nav-item" data-view="configurations" data-access="admin" hidden>
                        <span class="nav-icon">⚙</span>
                        Cấu hình hệ thống
                    </button>
                    <button class="nav-item" data-view="promotions" data-access="admin" hidden>
                        <span class="nav-icon">%</span>
                        Khuyến mãi
                    </button>
                    <button class="nav-item" data-view="reports" data-access="report" hidden>
                        <span class="nav-icon">▥</span>
                        Báo cáo
                    </button>
                    <button class="nav-item" data-view="logs" data-access="admin" hidden>
                        <span class="nav-icon">◷</span>
                        Nhật ký hệ thống
                    </button>
                    <button class="nav-item" data-view="ai">
                        <span class="nav-icon">✧</span>
                        Trợ lý xe
                        <span class="nav-new">AI</span>
                    </button>
                </nav>
                <div class="sidebar-bottom">
                    <span class="live-dot"></span>
                    VinFast Electric Cars
                    <small>Quản trị · Báo cáo · Tư vấn AI</small>
                </div>
            </aside>
            <button type="button" id="navigation-backdrop" class="navigation-backdrop" aria-label="Đóng menu" tabindex="-1" hidden></button>
            <div class="main-shell">
                <header class="topbar">
                    <button type="button" id="navigation-toggle" class="navigation-toggle icon-button" aria-label="Mở menu" aria-controls="sidebar-panel" aria-expanded="false">☰</button>
                    <span class="breadcrumb">
                        Showroom
                        <span>/</span>
                        <strong id="breadcrumb-title">Danh mục xe</strong>
                    </span>
                    <div class="topbar-right">
                        <a class="home-link" href="<c:url value='/index.jsp' />">Trang chủ <span aria-hidden="true">↗</span></a>
                        <span class="today" id="today"></span>
                        <button type="button" id="refresh-view" class="icon-button" aria-label="Tải lại màn hình" title="Tải lại màn hình">↻</button>
                        <button id="setup-button" class="button secondary small" hidden>Tạo Admin đầu tiên</button>
                        <button id="session-button" class="session-button">
                            <span class="avatar" id="avatar">G</span>
                            <span id="session-label">Đăng nhập</span>
                            <span>⌄</span>
                        </button>
                    </div>
                </header>
                <main id="main" tabindex="-1">
                    <noscript><p class="notice error">Hãy bật JavaScript trong trình duyệt để đăng nhập và sử dụng các chức năng quản trị, báo cáo, tư vấn xe.</p></noscript>
                    <div class="demo-banner">
                        <span class="pill">BẢN DEMO BÁO CÁO</span>
                        <p>
                            Xe: thông tin VinFast có nguồn tham khảo. Giao dịch, khách hàng và doanh thu: dữ liệu mô
                            phỏng phục vụ bài báo cáo.
                        </p>
                    </div>
                    <div class="page-heading">
                        <div>
                            <p class="eyebrow" id="page-eyebrow">KHÁM PHÁ XE ĐIỆN</p>
                            <h1 id="page-title">Danh mục xe VinFast</h1>
                            <p id="page-description">
                                Khám phá thông số, đối chiếu các phiên bản và hỏi trợ lý tư vấn.
                            </p>
                        </div>
                        <div id="page-actions"></div>
                    </div>
                    <div id="notice" class="notice" role="status" hidden></div>
                    <section id="content" aria-live="polite">
                        <div class="loading">Đang kết nối với hệ thống…</div>
                    </section>
                </main>
                <footer>
                    VINFAST / Electric Cars
                    <span>Quản trị / Báo cáo / Tư vấn AI</span>
                </footer>
            </div>
        </div>
        <dialog id="form-dialog" aria-labelledby="dialog-title">
            <div class="dialog-heading">
                <div>
                    <p class="eyebrow" id="dialog-eyebrow">QUẢN LÝ SHOWROOM</p>
                    <h2 id="dialog-title"></h2>
                </div>
                <button type="button" id="dialog-close" class="icon-button" aria-label="Đóng">×</button>
            </div>
            <form id="dialog-form">
                <div id="dialog-fields" class="form-grid"></div>
                <p id="dialog-error" class="form-error" role="alert" hidden></p>
                <div class="dialog-actions">
                    <button type="button" id="dialog-cancel" class="button secondary">Hủy</button>
                    <button type="submit" id="dialog-submit" class="button">Lưu thay đổi</button>
                </div>
            </form>
        </dialog>
        <div id="toast" class="toast" role="status" hidden></div>
    </body>
</html>
