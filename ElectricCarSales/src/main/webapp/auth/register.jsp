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
        .server-message { display:block; margin:0 0 12px; }
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
                <form id="register-form" class="auth-form register-form" action="${pageContext.request.contextPath}/register" method="post">
                    <label class="field-label" for="name">Họ và tên</label>
                    <div class="input-wrap"><input id="name" name="name" type="text" placeholder="HỌ VÀ TÊN" value="<c:out value='${name}'/>" autocomplete="name" maxlength="100" required></div>
                    <div class="field-row">
                        <div><label class="field-label" for="username">Tên đăng nhập</label><div class="input-wrap"><input id="username" name="username" type="text" placeholder="TÊN ĐĂNG NHẬP" value="<c:out value='${username}'/>" autocomplete="username" minlength="5" maxlength="50" pattern="[A-Za-z0-9_]+" required></div></div>
                        <div><label class="field-label" for="phone">Số điện thoại</label><div class="input-wrap"><input id="phone" name="phone" type="tel" placeholder="SỐ ĐIỆN THOẠI" value="<c:out value='${phone}'/>" autocomplete="tel" pattern="0[0-9]{9,10}" maxlength="11" required></div></div>
                    </div>
                    <label class="field-label" for="email">Email</label>
                    <div class="input-wrap"><input id="email" name="email" type="email" placeholder="EMAIL" value="<c:out value='${email}'/>" autocomplete="email" maxlength="100" required></div>
                    <label class="field-label" for="address">Địa chỉ (không bắt buộc)</label>
                    <div class="input-wrap"><input id="address" name="address" type="text" placeholder="ĐỊA CHỈ" value="<c:out value='${address}'/>" autocomplete="street-address" maxlength="255"></div>
                    <div class="field-row">
                        <div><label class="field-label" for="password">Mật khẩu</label><div class="input-wrap"><input id="password" name="password" type="password" placeholder="TỐI THIỂU 8 KÝ TỰ" autocomplete="new-password" minlength="8" maxlength="72" required></div></div>
                        <div><label class="field-label" for="confirmPassword">Nhập lại mật khẩu</label><div class="input-wrap"><input id="confirmPassword" name="confirmPassword" type="password" placeholder="NHẬP LẠI MẬT KHẨU" autocomplete="new-password" minlength="8" maxlength="72" required></div></div>
                    </div>
                    <button class="submit-button" type="submit">Đăng ký tài khoản <span aria-hidden="true">→</span></button>
                </form>
                <div class="form-bottom"><span>Đã có tài khoản?</span><a href="${pageContext.request.contextPath}/login" class="text-button">Đăng nhập</a></div>
            </div>
            <footer class="panel-footer"><span>© VinFast ElectricCarSale</span><span>Đăng ký an toàn</span></footer>
        </div>
    </section>
</main>
</body>
</html>
