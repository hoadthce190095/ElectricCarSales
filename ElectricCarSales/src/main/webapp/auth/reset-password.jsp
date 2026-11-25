<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#ececea">
    <title>Đặt lại mật khẩu · VinFast ElectricCarSale</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/login.css">
    <style>
        .reset-card { min-height: 600px; }
        .reset-panel { padding-top: 56px; padding-bottom: 56px; }
        .reset-content { width: min(100%, 380px); }
        .reset-form { gap: 12px; }
        .reset-form .field-label { margin: 0 0 -6px 12px; color: #85898e; font-size: 10px; font-weight: 600; }
        .reset-message { display: block; margin: 0 0 12px; }
        @media(max-width:760px) { .reset-card { min-height: unset; } .reset-panel { padding-top: 48px; } }
    </style>
</head>
<body class="login-page">
<main class="login-layout">
    <section class="login-card reset-card" aria-label="Đặt lại mật khẩu">
        <div class="login-visual">
            <div class="visual-texture" aria-hidden="true"></div>
            <div class="visual-car" aria-hidden="true">
                <span class="car-window"></span><span class="car-body"></span>
                <i class="wheel wheel-left"></i><i class="wheel wheel-right"></i><span class="car-highlight"></span>
            </div>
            <a class="login-brand" href="${pageContext.request.contextPath}/index.jsp" aria-label="Về trang chủ VinFast">
                <span class="brand-mark">V</span><span class="brand-name">VinFast</span>
            </a>
            <div class="visual-caption"><span>Electric Car Sale</span><strong>Bảo mật tài khoản<br>của bạn.</strong></div>
            <a class="visual-contact" href="mailto:hotro@electriccarsale.vn">hotro@electriccarsale.vn</a>
        </div>
        <div class="login-panel reset-panel">
            <a class="back-home" href="${pageContext.request.contextPath}/index.jsp"><span aria-hidden="true">←</span> Trang chủ</a>
            <div class="form-content reset-content">
                <span class="form-eyebrow">BẢO MẬT TÀI KHOẢN</span>
                <h1>Đặt lại mật khẩu</h1>
                <p class="form-intro">OTP đã được xác thực. Hãy tạo mật khẩu mới cho tài khoản.</p>
                <c:if test="${not empty error}">
                    <p class="form-message reset-message" role="alert"><c:out value="${error}"/></p>
                </c:if>
                <form class="auth-form reset-form" action="${pageContext.request.contextPath}/verify-otp" method="post">
                    <input type="hidden" name="action" value="resetPassword">
                    <label class="field-label" for="newPassword">Mật khẩu mới (8–30 ký tự, gồm chữ hoa, số và ký tự đặc biệt)</label>
                    <div class="input-wrap">
                        <input id="newPassword" name="newPassword" type="password" minlength="8" maxlength="30" autocomplete="new-password" placeholder="Nhập mật khẩu mới" required autofocus>
                    </div>
                    <label class="field-label" for="confirmPassword">Nhập lại mật khẩu mới</label>
                    <div class="input-wrap">
                        <input id="confirmPassword" name="confirmPassword" type="password" minlength="8" maxlength="30" autocomplete="new-password" placeholder="Xác nhận mật khẩu mới" required>
                    </div>
                    <button class="submit-button" type="submit">Cập nhật mật khẩu <span aria-hidden="true">→</span></button>
                </form>
                <div class="form-bottom"><span>Đã nhớ mật khẩu?</span><a href="${pageContext.request.contextPath}/login" class="text-button">Đăng nhập</a></div>
            </div>
            <footer class="panel-footer"><span>© VinFast ElectricCarSale</span><span>Xác thực an toàn</span></footer>
        </div>
    </section>
</main>
</body>
</html>
