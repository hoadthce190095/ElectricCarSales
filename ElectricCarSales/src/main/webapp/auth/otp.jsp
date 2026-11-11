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
        .otp-panel { padding-top: 56px; padding-bottom: 56px; }
        .otp-content { width: min(100%, 380px); }
        .otp-form { gap: 12px; }
        .otp-form .field-label { margin: 0 0 -6px 12px; color: #85898e; font-size: 10px; font-weight: 600; }
        .otp-form .input-wrap input.otp-digits { height: 48px; text-align: center; font-size: 18px; letter-spacing: 8px; padding-left: 20px; }
        .otp-form .submit-button { width: 220px; }
        .otp-hint { margin: 0 0 16px; color: #7a7e85; font-size: 11px; line-height: 1.6; }
        .otp-message { display: block; margin: 0 0 12px; }
        .otp-success { border-color: #cde8d7; background: #f4fff7; color: #237044; }
        .otp-content .form-intro { margin-bottom: 16px; }
        @media(max-width:760px) {
            .otp-card { min-height: unset; }
            .otp-panel { padding-top: 48px; }
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
                <c:choose>
                    <c:when test="${otpMode == 'forgot'}">
                        <h1 id="otp-title">Quên mật khẩu</h1>
                        <p class="form-intro">Nhập Email, Số điện thoại hoặc Tên đăng nhập đã đăng ký để nhận mã OTP khôi phục mật khẩu.</p>
                    </c:when>
                    <c:when test="${otpMode == 'reset'}">
                        <h1 id="otp-title">Đặt lại mật khẩu</h1>
                        <p class="form-intro">Nhập mã OTP 6 chữ số và mật khẩu mới của bạn.</p>
                    </c:when>
                    <c:otherwise>
                        <h1 id="otp-title">Xác thực OTP</h1>
                        <p class="form-intro">Nhập mã xác thực gồm 6 chữ số để kích hoạt tài khoản.</p>
                    </c:otherwise>
                </c:choose>

                <c:if test="${not empty notice}">
                    <p class="form-message otp-message otp-success" role="status"><c:out value="${notice}"/></p>
                </c:if>
                <c:if test="${not empty success}">
                    <p class="form-message otp-message otp-success" role="status"><c:out value="${success}"/></p>
                </c:if>
                <c:if test="${not empty error}">
                    <p class="form-message otp-message" role="alert"><c:out value="${error}"/></p>
                </c:if>

                <c:choose>
                    <c:when test="${otpMode == 'forgot'}">
                        <form class="auth-form otp-form" action="${pageContext.request.contextPath}/verify-otp" method="post">
                            <input type="hidden" name="action" value="forgotRequest">
                            <label class="field-label" for="identifier">Email / Số điện thoại / Tên đăng nhập</label>
                            <div class="input-wrap">
                                <input id="identifier" name="identifier" type="text" placeholder="Nhập email hoặc số điện thoại" required autofocus>
                            </div>
                            <button class="submit-button" type="submit">Nhận mã OTP <span aria-hidden="true">→</span></button>
                        </form>
                    </c:when>
                    <c:when test="${otpMode == 'reset'}">
                        <p class="otp-hint">Mã OTP có hiệu lực trong 5 phút (kiểm tra email hoặc Console của IntelliJ khi chạy local).</p>
                        <form class="auth-form otp-form" action="${pageContext.request.contextPath}/verify-otp" method="post">
                            <input type="hidden" name="action" value="resetPassword">
                            <label class="field-label" for="otp">Mã OTP (6 chữ số)</label>
                            <div class="input-wrap">
                                <input id="otp" class="otp-digits" name="otp" type="text" inputmode="numeric" pattern="[0-9]{6}" minlength="6" maxlength="6" placeholder="000000" required autofocus>
                            </div>
                            <label class="field-label" for="newPassword">Mật khẩu mới (8–72 ký tự)</label>
                            <div class="input-wrap">
                                <input id="newPassword" name="newPassword" type="password" minlength="8" maxlength="72" placeholder="Nhập mật khẩu mới" required>
                            </div>
                            <label class="field-label" for="confirmPassword">Nhập lại mật khẩu mới</label>
                            <div class="input-wrap">
                                <input id="confirmPassword" name="confirmPassword" type="password" minlength="8" maxlength="72" placeholder="Xác nhận mật khẩu mới" required>
                            </div>
                            <button class="submit-button" type="submit">Cập nhật mật khẩu <span aria-hidden="true">→</span></button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <p class="otp-hint">Mã có hiệu lực trong 5 phút. Khi chạy local chưa cấu hình SMTP, mã OTP được in tại tab Console của IntelliJ.</p>
                        <c:if test="${not empty email}"><p class="otp-hint">Email đăng ký: <c:out value="${email}"/>.</p></c:if>
                        <form class="auth-form otp-form" action="${pageContext.request.contextPath}/verify-otp" method="post">
                            <input type="hidden" name="userId" value="<c:out value='${not empty userId ? userId : sessionScope.pendingRegistrationUserId}'/>">
                            <label class="field-label" for="otp">Mã xác thực</label>
                            <div class="input-wrap">
                                <input id="otp" class="otp-digits" name="otp" type="text" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" minlength="6" maxlength="6" placeholder="000000" aria-describedby="otp-help" required autofocus>
                            </div>
                            <span id="otp-help" class="sr-only">Nhập chính xác 6 chữ số.</span>
                            <button class="submit-button" type="submit">Xác nhận tài khoản <span aria-hidden="true">→</span></button>
                        </form>
                        <form action="${pageContext.request.contextPath}/verify-otp" method="post" class="otp-form" style="margin-top:8px;">
                            <input type="hidden" name="userId" value="<c:out value='${not empty userId ? userId : sessionScope.pendingRegistrationUserId}'/>">
                            <input type="hidden" name="action" value="resend">
                            <button class="text-button" type="submit">Gửi lại mã xác thực</button>
                        </form>
                    </c:otherwise>
                </c:choose>
                <div class="form-bottom"><span>Đã nhớ mật khẩu?</span><a href="${pageContext.request.contextPath}/login" class="text-button">Đăng nhập</a></div>
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
