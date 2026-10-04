<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!-- TOP BAR -->
<div class="top-bar">
    <div class="site-container top-bar-content">
        <div class="top-slogan">
            ELECTRIC CAR SALES – MẠNH LIỆT TINH THẦN VIỆT
        </div>

        <div class="social-links">
            <a href="#" title="Facebook">f</a>
            <a href="#" title="YouTube">▶</a>
        </div>
    </div>
</div>

<!-- MAIN NAVBAR -->
<header class="main-header">
    <div class="site-container navbar-container">

        <!-- LOGO -->
        <a href="${pageContext.request.contextPath}/" class="brand">
            <div class="brand-logo-placeholder">
                <span>V</span>
            </div>

            <div class="brand-text">
                <div class="brand-name">ELECTRIC CAR SALES</div>
                <div class="brand-subtitle">ĐẠI LÝ Ô TÔ ĐIỆN</div>
            </div>
        </a>

        <!-- NAVIGATION -->
        <nav class="main-navigation">

            <a href="${pageContext.request.contextPath}/"
               class="nav-item active">
                TRANG CHỦ
            </a>

            <a href="${pageContext.request.contextPath}/vehicle/vehicle-list.jsp"
               class="nav-item dropdown-link">
                SẢN PHẨM
                <span class="arrow">⌄</span>
            </a>

            <a href="#" class="nav-item">
                BẢNG GIÁ
            </a>

            <a href="#" class="nav-item dropdown-link">
                TIN TỨC
                <span class="arrow">⌄</span>
            </a>

            <a href="#" class="nav-item">
                GIỚI THIỆU
            </a>

            <a href="${pageContext.request.contextPath}/consultation/consultation.jsp"
               class="nav-item">
                LIÊN HỆ
            </a>

            <a href="${pageContext.request.contextPath}/auth/login.jsp"
               class="dealer-button">
                ĐĂNG NHẬP
            </a>

        </nav>

    </div>
</header>