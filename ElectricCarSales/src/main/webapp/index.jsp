<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
boolean customerView = "customer".equals(request.getParameter("view"));
%>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#f7f8fa">
    <meta name="app-context-path" content="<c:out value='${pageContext.request.contextPath}' />">
    <meta name="description" content="Khám phá, tìm kiếm và so sánh các mẫu xe điện VinFast tại ElectricCarSale.">
    <title>VinFast - ElectricCarSale</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>

    <link rel="stylesheet" href="<c:url value='/assets/css/theme.css'><c:param name='v' value='main-blue-20261017' /></c:url>">
    <link rel="stylesheet" href="<c:url value='/assets/css/home.css'><c:param name='v' value='main-blue-20261017' /></c:url>">
    <script src="<c:url value='/assets/js/home.js'><c:param name='v' value='main-blue-20261017' /></c:url>" defer></script>
</head>
<body class="home-page">
<header class="site-header">
    <div class="header-inner">
        <a class="brand" href="<c:url value='/index.jsp' />" aria-label="VinFast ElectricCarSale - Trang chủ">
            <span class="brand-mark" aria-hidden="true">V</span>
            <span class="brand-copy"><strong>VinFast</strong><small>ELECTRIC CAR SALE</small></span>
        </a>
        <button class="mobile-menu-toggle" type="button" aria-expanded="false" aria-controls="primary-nav" aria-label="Mở menu">☰</button>
        <nav class="primary-nav" id="primary-nav" aria-label="Điều hướng chính">
            <a class="nav-link current" href="#models">Mua xe <span aria-hidden="true">⌄</span></a>
            <a class="nav-link" href="#about">Giới thiệu</a>
            <a class="nav-link" href="#contact">Tư vấn</a>
            <a class="nav-link" href="<c:url value='/tan.jsp' />#ai">Trợ lý xe</a>
            <a class="nav-link" href="<c:url value='/tan.jsp'><c:param name='login' value='1' /></c:url>">Quản trị</a>
        </nav>
        <div class="header-actions">
            <a class="header-contact" href="mailto:hotro@electriccarsale.vn?subject=Yeu%20cau%20tu%20van%20xe%20VinFast">
                <span aria-hidden="true">✉</span><span>Liên hệ tư vấn</span>
            </a>
            <% if (customerView) { %>
            <a class="account-link" href="<c:url value='/customer/my-account.jsp' />"><span class="account-icon" aria-hidden="true">KH</span>Tài khoản của tôi</a>
            <% } else { %>
            <a class="account-link" href="<c:url value='/login.jsp' />"><span class="account-icon" aria-hidden="true">♙</span>Đăng nhập</a>
            <% } %>
        </div>
    </div>
</header>

<main>
    <section class="showcase" aria-label="Khám phá xe điện VinFast">
        <div class="showcase-heading">
            <div><span class="eyebrow">VINFAST ELECTRIC CAR SALE</span><h1>Chọn xe điện cho<br class="desktop-break"> hành trình của bạn</h1></div>
            <p>Khám phá các mẫu xe, xem thông số và tìm lựa chọn phù hợp với nhu cầu của bạn.</p>
        </div>
        <div class="banner-track" id="banner-track" aria-label="Một số dòng xe nổi bật">
            <a class="feature-banner banner-city" href="#models" data-banner="0">
                <span class="banner-copy"><small>NHỎ GỌN MỖI NGÀY</small><strong>Di chuyển linh hoạt<br>trong phố</strong><span class="banner-link">Khám phá VF 3 <b>↗</b></span></span>
                <img alt="VinFast VF 3" loading="eager">
                <span class="banner-shade" aria-hidden="true"></span>
            </a>
            <a class="feature-banner banner-family" href="#models" data-banner="1">
                <span class="banner-copy"><small>KHÔNG GIAN CHO CẢ NHÀ</small><strong>Thoải mái trên<br>mọi cung đường</strong><span class="banner-link">Khám phá VF 7 <b>↗</b></span></span>
                <img alt="VinFast VF 7" loading="lazy">
                <span class="banner-shade" aria-hidden="true"></span>
            </a>
            <a class="feature-banner banner-long" href="#models" data-banner="2">
                <span class="banner-copy"><small>SẴN SÀNG ĐI XA</small><strong>Khám phá<br>một hành trình mới</strong><span class="banner-link">Khám phá VF 8 <b>↗</b></span></span>
                <img alt="VinFast VF 8" loading="lazy">
                <span class="banner-shade" aria-hidden="true"></span>
            </a>
        </div>
    </section>

    <section class="catalog-layout" id="models" aria-label="Danh mục xe điện VinFast">
        <aside class="filter-panel" aria-label="Bộ lọc xe">
            <div class="filter-heading"><div><span class="eyebrow">TÌM XE PHÙ HỢP</span><h2>Bộ lọc</h2></div><button class="reset-filters" id="reset-filters" type="button">Đặt lại</button></div>
            <label class="filter-label" for="model-filter">Hãng xe, dòng xe</label>
            <div class="select-wrap"><span aria-hidden="true">▦</span><select id="model-filter" aria-label="Lọc theo dòng xe"><option value="">Tất cả dòng xe</option></select><b aria-hidden="true">⌄</b></div>

            <label class="filter-label" for="price-filter">Mức giá tham khảo</label>
            <div class="select-wrap"><span aria-hidden="true">₫</span><select id="price-filter"><option value="">Tất cả mức giá</option><option value="500">Dưới 500 triệu</option><option value="800">Dưới 800 triệu</option><option value="1000">Dưới 1 tỷ</option><option value="1500">Dưới 1,5 tỷ</option></select><b aria-hidden="true">⌄</b></div>

            <label class="filter-label" for="range-filter">Quãng đường công bố</label>
            <div class="select-wrap"><span aria-hidden="true">↗</span><select id="range-filter"><option value="">Mọi mức quãng đường</option><option value="300">Từ 300 km</option><option value="400">Từ 400 km</option><option value="500">Từ 500 km</option></select><b aria-hidden="true">⌄</b></div>

            <label class="filter-label" for="color-filter">Màu sắc</label>
            <div class="select-wrap"><span aria-hidden="true">◉</span><select id="color-filter"><option value="">Tất cả màu sắc</option></select><b aria-hidden="true">⌄</b></div>

            <div class="filter-note"><span aria-hidden="true">i</span><p>Giá và quãng đường là thông tin tham khảo theo cấu hình được ghi trong danh mục.</p></div>
            <a class="filter-contact" href="mailto:hotro@electriccarsale.vn?subject=Tu%20van%20chon%20xe%20VinFast">Cần tư vấn chọn xe? <span>Liên hệ với chúng tôi ↗</span></a>
        </aside>

        <div class="catalog-main">
            <form class="search-form" id="search-form" role="search">
                <label class="sr-only" for="vehicle-search">Tìm kiếm xe</label>
                <span class="search-icon" aria-hidden="true">⌕</span>
                <input id="vehicle-search" type="search" placeholder="Tìm theo mẫu xe hoặc từ khóa..." autocomplete="off">
                <button type="submit">Tìm kiếm</button>
            </form>

            <div class="catalog-title-row">
                <div><span class="eyebrow blue-eyebrow">DANH MỤC XE ĐIỆN</span><h2>Khám phá các mẫu xe VinFast</h2><p id="result-summary" aria-live="polite">Đang tải danh mục xe...</p></div>
                <label class="sort-control" for="sort-filter"><span>Sắp xếp</span><select id="sort-filter"><option value="recommended">Nổi bật</option><option value="price-asc">Giá thấp đến cao</option><option value="price-desc">Giá cao đến thấp</option><option value="range-desc">Quãng đường xa nhất</option></select></label>
            </div>

            <div class="active-filters" id="active-filters" aria-live="polite"></div>
            <div class="vehicle-grid" id="vehicle-grid" aria-live="polite" aria-busy="true">
                <div class="loading-card"><span class="loading-orbit"></span><span>Đang tải danh sách xe...</span></div>
            </div>
            <div class="empty-state" id="empty-state" hidden><span aria-hidden="true">⌕</span><h3>Chưa tìm thấy mẫu xe phù hợp</h3><p>Thử thay đổi từ khóa hoặc bộ lọc để xem thêm lựa chọn.</p><button type="button" id="empty-reset">Xóa bộ lọc</button></div>
            <p class="catalog-disclaimer" id="catalog-disclaimer"></p>
        </div>
    </section>

    <section class="about-strip" id="about">
        <div class="about-icon" aria-hidden="true">⚡</div><div><span class="eyebrow">VINFAST · ELECTRIC CAR SALE</span><h2>Thông tin xe rõ ràng, lựa chọn dễ dàng</h2><p>Tìm hiểu mẫu xe, cấu hình và thông số trước khi quyết định. Đội ngũ tư vấn sẵn sàng hỗ trợ khi bạn cần.</p></div>
        <a href="mailto:hotro@electriccarsale.vn?subject=Yeu%20cau%20tu%20van%20xe%20VinFast" id="contact">Gửi yêu cầu tư vấn <span>↗</span></a>
    </section>
</main>

<div class="compare-dock" id="compare-dock" hidden>
    <span><b id="compare-count">0</b> mẫu xe đã chọn để so sánh</span><button id="compare-open" type="button">So sánh xe <span>→</span></button><button class="compare-clear" id="compare-clear" type="button" aria-label="Xóa các xe đã chọn">×</button>
</div>
<dialog class="compare-dialog" id="compare-dialog" aria-labelledby="compare-title">
    <div class="dialog-heading"><div><span class="eyebrow">ĐỐI CHIẾU THÔNG SỐ</span><h2 id="compare-title">So sánh xe</h2></div><button type="button" class="dialog-close" id="compare-close" aria-label="Đóng">×</button></div>
    <div class="compare-table-wrap" id="compare-table-wrap"></div>
</dialog>
<footer class="site-footer"><a class="footer-brand" href="<c:url value='/index.jsp' />">VinFast - ElectricCarSale</a><span>© <span id="copyright-year">2026</span> · Thông tin sản phẩm mang tính tham khảo.</span><a href="mailto:hotro@electriccarsale.vn">hotro@electriccarsale.vn</a></footer>
</body>
</html>
