<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1.0">

    <title>Electric Car Sales</title>

    <!-- Bootstrap -->
    <link
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
        rel="stylesheet">

    <!-- Common CSS -->
    <link
        rel="stylesheet"
        href="${pageContext.request.contextPath}/assets/css/style.css">

    <!-- Home CSS -->
    <link
        rel="stylesheet"
        href="${pageContext.request.contextPath}/assets/css/home.css">
</head>

<body>

<jsp:include page="/common/navbar.jsp"/>

<!-- SEARCH BAR AREA -->
<section class="search-strip">
    <div class="site-container search-container">

        <form class="top-search" action="#" method="get">

            <input
                type="text"
                name="keyword"
                placeholder="Tìm kiếm ...">

            <button type="submit">
                🔍
            </button>

        </form>

    </div>
</section>


<!-- MAIN CONTENT -->
<main class="site-container main-layout">

    <!-- ================= LEFT CONTENT ================= -->
    <section class="home-content">

        <!-- HERO CAROUSEL -->
        <div id="homeCarousel"
             class="carousel slide hero-carousel"
             data-bs-ride="carousel">

            <div class="carousel-inner">

                <!-- SLIDE 1 -->
                <div class="carousel-item active">

                    <div class="hero-placeholder">

                        <div class="hero-text">
                            <div class="hero-title">
                                ELECTRIC CAR
                            </div>

                            <div class="hero-subtitle">
                                GIẢI PHÁP DI CHUYỂN XANH
                            </div>

                            <div class="hero-price">
                                CÔNG NGHỆ – AN TOÀN – HIỆN ĐẠI
                            </div>
                        </div>

                    </div>

                </div>


                <!-- SLIDE 2 -->
                <div class="carousel-item">

                    <div class="hero-placeholder hero-placeholder-2">

                        <div class="hero-text">
                            <div class="hero-title">
                                Ô TÔ ĐIỆN
                            </div>

                            <div class="hero-subtitle">
                                LỰA CHỌN CHO TƯƠNG LAI
                            </div>

                        </div>

                    </div>

                </div>


                <!-- SLIDE 3 -->
                <div class="carousel-item">

                    <div class="hero-placeholder hero-placeholder-3">

                        <div class="hero-text">
                            <div class="hero-title">
                                ELECTRIC CAR SALES
                            </div>

                            <div class="hero-subtitle">
                                KHÁM PHÁ DÒNG XE CỦA BẠN
                            </div>

                        </div>

                    </div>

                </div>

            </div>


            <!-- PREVIOUS -->
            <button class="carousel-control-prev"
                    type="button"
                    data-bs-target="#homeCarousel"
                    data-bs-slide="prev">

                <span class="carousel-control-prev-icon"></span>

            </button>


            <!-- NEXT -->
            <button class="carousel-control-next"
                    type="button"
                    data-bs-target="#homeCarousel"
                    data-bs-slide="next">

                <span class="carousel-control-next-icon"></span>

            </button>


            <!-- INDICATORS -->
            <div class="carousel-indicators">

                <button type="button"
                        data-bs-target="#homeCarousel"
                        data-bs-slide-to="0"
                        class="active"></button>

                <button type="button"
                        data-bs-target="#homeCarousel"
                        data-bs-slide-to="1"></button>

                <button type="button"
                        data-bs-target="#homeCarousel"
                        data-bs-slide-to="2"></button>

            </div>

        </div>


        <!-- ================= INTRO ================= -->

        <section class="home-section">

            <div class="section-heading">

                <span></span>

                <h2>ELECTRIC CAR SALES</h2>

                <span></span>

            </div>


            <div class="quick-services">

                <a href="#" class="quick-service">

                    <div class="service-icon">
                        ₫
                    </div>

                    <div class="service-title">
                        BẢNG BÁO GIÁ
                    </div>

                </a>


                <a href="#" class="quick-service">

                    <div class="service-icon">
                        ◎
                    </div>

                    <div class="service-title">
                        ĐĂNG KÝ LÁI THỬ
                    </div>

                </a>


                <a href="#" class="quick-service">

                    <div class="service-icon">
                        🎁
                    </div>

                    <div class="service-title">
                        ƯU ĐÃI &amp; KHUYẾN MÃI
                    </div>

                </a>


                <a href="${pageContext.request.contextPath}/consultation/consultation.jsp"
                   class="quick-service">

                    <div class="service-icon">
                        ✉
                    </div>

                    <div class="service-title">
                        THÔNG TIN LIÊN HỆ
                    </div>

                </a>

            </div>

        </section>


        <!-- ================= VEHICLES ================= -->

        <section class="home-section">

            <div class="section-heading">

                <span></span>

                <h2>Ô TÔ ĐIỆN</h2>

                <span></span>

            </div>


            <div class="vehicle-grid">


                <!-- VEHICLE 1 -->
                <a href="${pageContext.request.contextPath}/vehicle/vehicle-detail.jsp"
                   class="vehicle-card">

                    <div class="vehicle-image">
                        <div class="image-placeholder">
                            XE
                        </div>
                    </div>

                    <h3>MODEL EV 01</h3>

                    <p>Ô tô điện 4 chỗ</p>

                </a>


                <!-- VEHICLE 2 -->
                <a href="${pageContext.request.contextPath}/vehicle/vehicle-detail.jsp"
                   class="vehicle-card">

                    <div class="vehicle-image">
                        <div class="image-placeholder">
                            XE
                        </div>
                    </div>

                    <h3>MODEL EV 02</h3>

                    <p>Ô tô điện 5 chỗ</p>

                </a>


                <!-- VEHICLE 3 -->
                <a href="${pageContext.request.contextPath}/vehicle/vehicle-detail.jsp"
                   class="vehicle-card">

                    <div class="vehicle-image">
                        <div class="image-placeholder">
                            XE
                        </div>
                    </div>

                    <h3>MODEL EV 03</h3>

                    <p>Ô tô điện 5 chỗ</p>

                </a>


                <!-- VEHICLE 4 -->
                <a href="${pageContext.request.contextPath}/vehicle/vehicle-detail.jsp"
                   class="vehicle-card">

                    <div class="vehicle-image">
                        <div class="image-placeholder">
                            XE
                        </div>
                    </div>

                    <h3>MODEL EV 04</h3>

                    <p>Ô tô điện SUV</p>

                </a>


                <!-- VEHICLE 5 -->
                <a href="${pageContext.request.contextPath}/vehicle/vehicle-detail.jsp"
                   class="vehicle-card">

                    <div class="vehicle-image">
                        <div class="image-placeholder">
                            XE
                        </div>
                    </div>

                    <h3>MODEL EV 05</h3>

                    <p>Ô tô điện SUV</p>

                </a>


                <!-- VEHICLE 6 -->
                <a href="${pageContext.request.contextPath}/vehicle/vehicle-detail.jsp"
                   class="vehicle-card">

                    <div class="vehicle-image">
                        <div class="image-placeholder">
                            XE
                        </div>
                    </div>

                    <h3>MODEL EV 06</h3>

                    <p>Ô tô điện 7 chỗ</p>

                </a>


                <!-- VEHICLE 7 -->
                <a href="${pageContext.request.contextPath}/vehicle/vehicle-detail.jsp"
                   class="vehicle-card">

                    <div class="vehicle-image">
                        <div class="image-placeholder">
                            XE
                        </div>
                    </div>

                    <h3>MODEL EV 07</h3>

                    <p>Ô tô điện cao cấp</p>

                </a>


                <!-- VEHICLE 8 -->
                <a href="${pageContext.request.contextPath}/vehicle/vehicle-detail.jsp"
                   class="vehicle-card">

                    <div class="vehicle-image">
                        <div class="image-placeholder">
                            XE
                        </div>
                    </div>

                    <h3>MODEL EV 08</h3>

                    <p>Ô tô điện SUV</p>

                </a>

            </div>

        </section>

    </section>


    <!-- ================= RIGHT SIDEBAR ================= -->

    <aside class="home-sidebar">


        <!-- SEARCH -->
        <div class="sidebar-block">

            <div class="sidebar-title">
                TÌM KIẾM
            </div>

            <form class="sidebar-search">

                <input type="text"
                       placeholder="Search ...">

                <button type="submit">
                    🔍
                </button>

            </form>

        </div>


        <!-- HOTLINE -->
        <div class="sidebar-block">

            <div class="sidebar-title">
                HOTLINE TƯ VẤN
            </div>

            <div class="hotline-box">

                <div class="hotline-number">
                    0900000000
                </div>

                <div class="hotline-title">
                    PHÒNG KINH DOANH
                </div>

                <p>
                    Báo giá nhanh, đăng ký lái thử,
                    hỗ trợ mua xe trả góp
                </p>

            </div>

        </div>


        <!-- CONTACT -->
        <div class="sidebar-block">

            <div class="sidebar-title">
                THÔNG TIN LIÊN HỆ
            </div>

            <div class="contact-box">

                <ul>

                    <li>
                        Địa chỉ: Đại lý Electric Car Sales
                    </li>

                    <li>
                        Số điện thoại:
                        0900000000
                    </li>

                    <li>
                        Email:
                        contact@electriccarsales.com
                    </li>

                </ul>

            </div>

        </div>


        <!-- QUICK REGISTER -->
        <div class="sidebar-block">

            <div class="sidebar-title">
                ĐĂNG KÝ NHANH
            </div>

            <form class="quick-register">

                <input
                    type="text"
                    placeholder="Họ và Tên Quý khách (*)">

                <input
                    type="tel"
                    placeholder="Số điện thoại liên hệ (*)">

                <input
                    type="text"
                    placeholder="Lời nhắn">

                <button type="submit">
                    ĐĂNG KÝ HỖ TRỢ NHANH
                </button>

            </form>

        </div>

    </aside>

</main>


<!-- FLOATING CONTACT BUTTONS -->

<div class="floating-contact">

    <a href="#" class="floating-button zalo">
        Z
    </a>

    <a href="tel:0900000000"
       class="floating-button phone">
        ☎
    </a>

    <a href="#" class="floating-button share">
        ↗
    </a>

</div>


<!-- Bootstrap JS -->
<script
    src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js">
</script>

</body>
</html>