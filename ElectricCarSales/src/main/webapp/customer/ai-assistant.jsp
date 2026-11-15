<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="app-context-path" content="<c:out value='${pageContext.request.contextPath}' />">
    <meta name="theme-color" content="#315b9d">
    <title>Trợ lý chọn xe · VinFast ElectricCarSale</title>
    <link rel="stylesheet" href="<c:url value='/assets/css/theme.css' />">
    <link rel="stylesheet" href="<c:url value='/assets/css/customer-ai.css'><c:param name='v' value='customer-ai-20261028' /></c:url>">
    <script src="<c:url value='/assets/js/customer-ai.js'><c:param name='v' value='customer-ai-20261028' /></c:url>" defer></script>
</head>
<body class="customer-ai-page">
    <a class="ai-skip-link" href="#ai-main">Đi tới nội dung tư vấn</a>
    <header class="ai-site-header">
        <a class="ai-brand" href="<c:url value='/index.jsp' />" aria-label="VinFast ElectricCarSale - Trang chủ">
            <span class="ai-brand-mark" aria-hidden="true">V</span>
            <span><strong>VinFast</strong><small>ELECTRIC CAR SALE</small></span>
        </a>
        <nav aria-label="Điều hướng">
            <a href="<c:url value='/index.jsp#models' />">Danh mục xe <span aria-hidden="true">↗</span></a>
            <a class="ai-account-link" id="ai-account-link" href="<c:url value='/login' />">Đăng nhập</a>
        </nav>
    </header>
    <main id="ai-main" class="ai-main" data-customer-ai>
        <div class="ai-page-heading">
            <div>
                <span class="ai-eyebrow">VINFAST · TRỢ LÝ CHỌN XE</span>
                <h1>Chiếc xe phù hợp,<br>bắt đầu từ nhu cầu của bạn.</h1>
                <p>Khám phá thông số, nhận gợi ý và đặt các mẫu xe cạnh nhau.</p>
            </div>
            <div class="ai-heading-note"><span aria-hidden="true">✧</span><p>Tư vấn sản phẩm<br><strong>Dành cho mọi khách hàng</strong></p></div>
        </div>
        <div id="ai-status" class="ai-status" role="status" aria-live="polite">Đang kết nối danh mục xe…</div>
        <button type="button" id="ai-reconnect" class="ai-button ai-button-secondary" hidden>Thử kết nối lại</button>
        <noscript><p class="ai-status ai-status-error">Hãy bật JavaScript để gửi câu hỏi tới trợ lý xe.</p></noscript>
        <div class="ai-layout">
            <section class="ai-panel ai-chat-panel" aria-label="Tư vấn xe">
                <div class="ai-chat-header">
                    <span class="ai-avatar" aria-hidden="true">✧</span>
                    <div><h2>Trợ lý VinFast</h2><p id="ai-catalogue-status">Đang đọc danh mục showroom</p></div>
                    <button type="button" id="ai-reset" class="ai-text-button" disabled>Cuộc trò chuyện mới</button>
                </div>
                <div id="ai-messages" class="ai-chat-body" role="log" aria-label="Hội thoại tư vấn xe" aria-live="polite" aria-relevant="additions text" aria-busy="true"></div>
                <div class="ai-chat-compose">
                    <div class="ai-prompts" aria-label="Câu hỏi gợi ý">
                        <button type="button" data-ai-prompt="Gợi ý xe 5 chỗ dưới 800 triệu">Xe dưới 800 triệu</button>
                        <button type="button" data-ai-prompt="So sánh VF 5 và VF 6">So sánh VF 5 và VF 6</button>
                        <button type="button" data-ai-prompt="Xe nào có tầm hoạt động xa?">Xe đi xa</button>
                    </div>
                    <form id="ai-question-form">
                        <label class="ai-sr-only" for="ai-question">Câu hỏi cho trợ lý xe</label>
                        <textarea id="ai-question" name="message" rows="2" maxlength="2000" placeholder="Ví dụ: Tôi cần xe đi làm, ngân sách khoảng 700 triệu…" required></textarea>
                        <button id="ai-send" class="ai-button" type="submit" disabled>Gửi <span aria-hidden="true">↑</span></button>
                    </form>
                    <p class="ai-compose-note">Enter để gửi · Shift + Enter để xuống dòng. Tư vấn theo dữ liệu danh mục xe.</p>
                </div>
            </section>
            <aside class="ai-aside">
                <section class="ai-panel ai-criteria-panel">
                    <span class="ai-eyebrow">NHU CẦU CỦA BẠN</span>
                    <h2>Thu hẹp lựa chọn</h2>
                    <p>Điền tiêu chí quan trọng với bạn để nhận gợi ý phù hợp.</p>
                    <form id="ai-needs-form">
                        <label>Ngân sách tối đa <span>(triệu đồng)</span><input name="budget" type="number" min="0.1" step="0.1" placeholder="Ví dụ: 800"></label>
                        <label>Tầm hoạt động tối thiểu <span>(km)</span><input name="minRange" type="number" min="1" step="1" placeholder="Ví dụ: 350"></label>
                        <label>Số chỗ tối thiểu<select name="minSeats"><option value="">Không giới hạn</option><option value="4">4 chỗ</option><option value="5">5 chỗ</option><option value="7">7 chỗ</option></select></label>
                        <button type="submit" class="ai-button" disabled>Nhận gợi ý xe <span aria-hidden="true">↗</span></button>
                    </form>
                </section>
                <section class="ai-panel ai-compare-panel">
                    <span class="ai-eyebrow">ĐẶT XE CẠNH NHAU</span>
                    <h2>So sánh thông số</h2>
                    <form id="ai-compare-form">
                        <fieldset><legend>Chọn từ 2 đến 4 mẫu xe</legend><div id="ai-model-choices" class="ai-model-choices"><p>Đang tải mẫu xe…</p></div></fieldset>
                        <p id="ai-selected-count" class="ai-selected-count" aria-live="polite">0/4 mẫu xe đã chọn</p>
                        <button type="submit" class="ai-button ai-button-secondary" disabled>So sánh đã chọn</button>
                    </form>
                </section>
                <p class="ai-product-note">Giá và thông số là thông tin tham khảo. Nhân viên showroom sẽ xác nhận tình trạng xe và điều kiện giao dịch.</p>
            </aside>
        </div>
    </main>
    <footer class="ai-site-footer"><span>VinFast · ElectricCarSale</span><a href="mailto:hotro@electriccarsale.vn">Liên hệ showroom <span aria-hidden="true">↗</span></a></footer>
</body>
</html>
