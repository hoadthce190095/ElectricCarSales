<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#ececea">
    <title>Xác thực OTP · VinFast ElectricCarSale</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/login.css">
    <style>
        .otp-card { min-height: 600px; }
        .otp-panel { padding-top: 70px; padding-bottom: 72px; }
        .otp-content { width: min(100%, 370px); }
        .otp-form { gap: 13px; }
        .otp-form .field-label { margin: 0 0 -6px 12px; color: #85898e; font-size: 9px; }
        .otp-form .input-wrap input { height: 48px; text-align: center; font-size: 18px; letter-spacing: 8px; padding-left: 20px; }
        .otp-form .submit-button { width: 205px; }
        .otp-hint { margin: 0 0 20px; color: #a0a3a7; font-size: 10px; line-height: 1.8; }
        .otp-test { margin: 0 0 14px; padding: 12px 14px; border: 1px dashed #d7d9d8; border-radius: 9px; background: #f7f8f6; color: #60666a; font-size: 10px; line-height: 1.7; }
        .otp-test strong { display: block; color: #25292c; font-size: 13px; letter-spacing: 2px; }
        .otp-test small { display: block; margin-top: 4px; color: #92969a; font-size: 9px; letter-spacing: 0; }
        .otp-message { display: block; margin: 0 0 12px; }
        .otp-success { border-color: #cde8d7; background: #f4fff7; color: #237044; }
        .otp-content .form-intro { margin-bottom: 18px; }
        @media(max-width:760px) {
            .otp-card { min-height: unset; }
            .otp-panel { padding-top: 62px; }
        }
    </style>
</head>
<body class="login-page">
<main class="login-layout">
    <section class="login-card otp-card" aria-label="Xác thực OTP VinFast ElectricCarSale">
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
        <div class="login-panel otp-panel">
            <a class="back-home" href="${pageContext.request.contextPath}/index.jsp"><span aria-hidden="true">←</span> Trang chủ</a>
            <div class="form-content otp-content">
                <span class="form-eyebrow">BẢO MẬT TÀI KHOẢN</span>
                <h1 id="otp-title">Xác thực OTP</h1>
                <p class="form-intro">Nhập mã xác thực gồm 6 chữ số để kích hoạt tài khoản.</p>
                <c:if test="${not empty success}">
                    <p class="form-message otp-message otp-success" role="status"><c:out value="${success}"/></p>
                </c:if>
                <c:if test="${not empty error}">
                    <p class="form-message otp-message" role="alert"><c:out value="${error}"/></p>
                </c:if>
                <c:if test="${not empty otpForTesting}">
                    <div class="otp-test"><span>OTP dùng để kiểm thử</span><strong><c:out value="${otpForTesting}"/></strong></div>
                </c:if>
                <p class="otp-hint">Mã có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này với người khác.</p>
                <form class="auth-form otp-form" action="${pageContext.request.contextPath}/verify-otp" method="post">
                    <input type="hidden" name="userId" value="<c:out value='${userId}'/>">
                    <label class="field-label" for="otp">Mã xác thực</label>
                    <div class="input-wrap"><input id="otp" name="otp" type="text" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" minlength="6" maxlength="6" placeholder="000000" aria-describedby="otp-help" required autofocus></div>
                    <span id="otp-help" class="sr-only">Nhập chính xác 6 chữ số.</span>
                    <button class="submit-button" type="submit">Xác nhận tài khoản <span aria-hidden="true">→</span></button>
                </form>
                <div class="form-bottom"><span>Đã có tài khoản?</span><a href="${pageContext.request.contextPath}/login" class="text-button">Đăng nhập</a></div>
            </div>
            <footer class="panel-footer"><span>© VinFast ElectricCarSale</span><span>Xác thực an toàn</span></footer>
        </div>
    </section>
</main>
<script>
    const otpInput = document.getElementById('otp');
    if (otpInput) {
        otpInput.addEventListener('input', () => {
            otpInput.value = otpInput.value.replace(/\D/g, '').slice(0, 6);
        });
    }
</script>
</body>
</html>