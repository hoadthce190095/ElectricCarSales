<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#ececea">
    <title>Đăng ký · VinFast ElectricCarSale</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/login.css">
    <style>
        .register-card { min-height: 680px; }
        .register-panel { padding-top: 52px; padding-bottom: 54px; }
        .register-content { width: min(100%, 370px); }
        .register-form { gap: 9px; }
        .register-form .input-wrap input { height: 39px; font-size: 10px; letter-spacing: .35px; }
        .register-form .input-wrap input::placeholder { letter-spacing: .7px; }
        .register-form .submit-button { width: 190px; }
        .register-form .field-row { display:grid; grid-template-columns:1fr 1fr; gap:10px; }
        .register-form .field-label { margin: 1px 0 -6px 12px; color:#85898e; font-size:9px; }
        .register-form .input-wrap input:invalid:not(:placeholder-shown) { border-color:#e7aab4; }
        .server-message { display:block; margin:0 0 10px; padding:0; border:0; background:transparent; color:#c62828; font-size:11px; line-height:1.4; }
        .client-message { display:block; margin:0 0 10px; color:#c62828; font-size:11px; line-height:1.4; }
        .client-message[hidden] { display:none; }
        .success-message { border-color:#cde8d7; background:#f4fff7; color:#237044; }
        .register-content .form-intro { margin-bottom:15px; }
        @media(max-width:760px) {
            .register-card { min-height:unset; }
            .register-panel { padding-top:62px; }
        }
        @media(max-width:390px) {
            .register-form .field-row { grid-template-columns:1fr; }
        }
    </style>
</head>
<body class="login-page">
<main class="login-layout">
    <section class="login-card register-card" aria-label="Đăng ký tài khoản VinFast ElectricCarSale">
        <div class="login-visual">
            <div class="visual-texture" aria-hidden="true"></div>
            <div class="visual-car" aria-hidden="true">
                <span class="car-window"></span><span class="car-body"></span>
                <i class="wheel wheel-left"></i><i class="wheel wheel-right"></i><span class="car-highlight"></span>
            </div>
            <a class="login-brand" href="${pageContext.request.contextPath}/index.jsp" aria-label="Về trang chủ VinFast">
                <span class="brand-mark">V</span><span class="brand-name">VinFast</span>
            </a>
            <div class="visual-caption"><span>Electric Car Sale</span><strong>Khởi đầu hành trình<br>cùng bạn.</strong></div>
            <a class="visual-contact" href="mailto:hotro@electriccarsale.vn">hotro@electriccarsale.vn</a>
        </div>
        <div class="login-panel register-panel">
            <a class="back-home" href="${pageContext.request.contextPath}/index.jsp"><span aria-hidden="true">←</span> Trang chủ</a>
            <div class="form-content register-content">
                <span class="form-eyebrow">TÀI KHOẢN VINFAST</span>
                <h1 id="register-title">Tạo tài khoản</h1>
                <p class="form-intro">Đăng ký tài khoản khách hàng để bắt đầu hành trình của bạn.</p>
                <c:if test="${not empty error}">
                    <p class="form-message server-message" role="alert"><c:out value="${error}"/></p>
                </c:if>
                <p id="register-validation" class="client-message" role="alert" aria-live="polite" hidden></p>
                <form id="register-form" class="auth-form register-form" action="${pageContext.request.contextPath}/register" method="post" novalidate>
                    <label class="field-label" for="name">Họ và tên</label>
                    <div class="input-wrap"><input id="name" name="name" type="text" placeholder="HỌ VÀ TÊN" value="<c:out value='${name}'/>" autocomplete="name" maxlength="100" required></div>
                    <div class="field-row">
                        <div><label class="field-label" for="username">Tên đăng nhập</label><div class="input-wrap"><input id="username" name="username" type="text" placeholder="TÊN ĐĂNG NHẬP" value="<c:out value='${username}'/>" autocomplete="username" minlength="5" maxlength="30" pattern="[A-Za-z0-9_]+" required></div></div>
                        <div><label class="field-label" for="phone">Số điện thoại</label><div class="input-wrap"><input id="phone" name="phone" type="tel" placeholder="SỐ ĐIỆN THOẠI" value="<c:out value='${phone}'/>" autocomplete="tel" pattern="0[0-9]{9}" maxlength="10" title="Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số (ví dụ: 0912345678)" required></div></div>
                    </div>
                    <label class="field-label" for="email">Email</label>
                    <div class="input-wrap"><input id="email" name="email" type="email" placeholder="EMAIL" value="<c:out value='${email}'/>" autocomplete="email" maxlength="30" pattern="^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$" title="Email phải có định dạng hợp lệ (ví dụ: example@domain.com)" required></div>
                    <label class="field-label" for="address">Địa chỉ (không bắt buộc)</label>
                    <div class="input-wrap"><input id="address" name="address" type="text" placeholder="ĐỊA CHỈ" value="<c:out value='${address}'/>" autocomplete="street-address" maxlength="255"></div>
                    <div class="field-row">
                        <div><label class="field-label" for="password">Mật khẩu</label><div class="input-wrap"><input id="password" name="password" type="password" placeholder="TỐI THIỂU 8 KÝ TỰ" autocomplete="new-password" minlength="8" maxlength="30" pattern="(?=.*[0-9])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,30}" title="Tối thiểu 8 ký tự, gồm 1 số, 1 chữ in hoa và 1 ký tự đặc biệt." required></div></div>
                        <div><label class="field-label" for="confirmPassword">Nhập lại mật khẩu</label><div class="input-wrap"><input id="confirmPassword" name="confirmPassword" type="password" placeholder="NHẬP LẠI MẬT KHẨU" autocomplete="new-password" minlength="8" maxlength="30" required></div></div>
                    </div>
                    <button class="submit-button" type="submit">Đăng ký tài khoản <span aria-hidden="true">→</span></button>
                </form>
                <div class="form-bottom"><span>Đã có tài khoản?</span><a href="${pageContext.request.contextPath}/login" class="text-button">Đăng nhập</a></div>
            </div>
            <footer class="panel-footer"><span>© VinFast ElectricCarSale</span><span>Đăng ký an toàn</span></footer>
        </div>
    </section>
</main>
<script>
(() => {
    const form = document.getElementById('register-form');
    const message = document.getElementById('register-validation');
    const password = document.getElementById('password');
    const confirmPassword = document.getElementById('confirmPassword');

    function validateForm(event) {
        confirmPassword.setCustomValidity(
            confirmPassword.value && confirmPassword.value !== password.value
                ? 'Mật khẩu nhập lại không khớp.' : ''
        );
        if (form.checkValidity()) {
            message.hidden = true;
            message.textContent = '';
            return true;
        }
        if (event) event.preventDefault();
        const invalid = form.querySelector(':invalid');
        let text = 'Vui lòng nhập đầy đủ và kiểm tra lại thông tin.';
        if (invalid === password && password.validity.patternMismatch) {
            text = 'Mật khẩu cần tối thiểu 8 ký tự, gồm 1 số, 1 chữ in hoa và 1 ký tự đặc biệt.';
        } else if (invalid === confirmPassword && confirmPassword.validity.customError) {
            text = 'Mật khẩu nhập lại không khớp.';
        } else if (invalid && invalid.validity.valueMissing) {
            text = 'Vui lòng điền đầy đủ thông tin bắt buộc.';
        } else if (invalid && invalid.validity.typeMismatch) {
            text = 'Email không đúng định dạng.';
        } else if (invalid && invalid.validity.patternMismatch) {
            text = invalid.id === 'phone'
                ? 'Số điện thoại phải bắt đầu bằng 0 và có đúng 10 chữ số (ví dụ: 0912345678).'
                : 'Tên đăng nhập phải có 5–30 ký tự, chỉ gồm chữ, số và dấu gạch dưới.';
        } else if (invalid && invalid.validity.tooShort) {
            text = 'Tên đăng nhập cần có ít nhất 5 ký tự.';
        }
        message.textContent = text;
        message.hidden = false;
        if (event && event.type === 'submit') invalid.focus();
        return false;
    }

    form.addEventListener('submit', validateForm);
    [password, confirmPassword].forEach(input => input.addEventListener('input', () => {
        confirmPassword.setCustomValidity(
            confirmPassword.value && confirmPassword.value !== password.value
                ? 'Mật khẩu nhập lại không khớp.' : ''
        );
        if (!message.hidden) validateForm();
    }));
    form.addEventListener('input', event => {
        if (event.target !== password && event.target !== confirmPassword && !message.hidden) {
            validateForm();
        }
    });
})();
</script>
</body>
</html>
