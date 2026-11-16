<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#ececea">
    <title>Đăng nhập · VinFast ElectricCarSale</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="<c:url value='/assets/css/login.css'><c:param name='v' value='login-post-20261021' /></c:url>">
    <script src="<c:url value='/assets/js/login.js'><c:param name='v' value='login-admin-setup-20261115' /></c:url>" defer></script>
</head>
<body class="login-page">
<main class="login-layout">
    <section class="login-card" aria-label="Đăng nhập VinFast ElectricCarSale">
        <div class="login-visual">
            <div class="visual-texture" aria-hidden="true"></div>
            <div class="visual-car" aria-hidden="true">
                <span class="car-window"></span><span class="car-body"></span>
                <i class="wheel wheel-left"></i><i class="wheel wheel-right"></i>
                <span class="car-highlight"></span>
            </div>
            <a class="login-brand" href="<%= request.getContextPath() %>/index.jsp" aria-label="Về trang chủ VinFast">
                <span class="brand-mark">V</span>
                <span class="brand-name">VinFast</span>
            </a>
            <div class="visual-caption"><span>Electric Car Sale</span><strong>Hành trình của bạn<br>bắt đầu từ đây.</strong></div>
            <a class="visual-contact" href="mailto:hotro@electriccarsale.vn">hotro@electriccarsale.vn</a>
        </div>

        <div class="login-panel">
            <a class="back-home" href="<%= request.getContextPath() %>/index.jsp"><span aria-hidden="true">←</span> Trang chủ</a>

            <div class="form-content">
                <span class="form-eyebrow">TÀI KHOẢN VINFAST</span>
                <h1 id="login-title">Đăng nhập</h1>
                <p class="form-intro">Nhập tên đăng nhập và mật khẩu để tiếp tục.</p>

                <form id="login-form" class="auth-form" method="post" action="<c:url value='/login' />">
                    <label class="sr-only" for="username">Tên đăng nhập</label>
                    <div class="input-wrap">
                        <input id="username" name="username" type="text" autocomplete="username" placeholder="TÊN ĐĂNG NHẬP" value="<c:out value='${param.username}' />" required maxlength="50">
                        <span class="field-status" aria-hidden="true">✓</span>
                    </div>

                    <label class="sr-only" for="password">Mật khẩu</label>
                    <div class="input-wrap">
                        <input id="password" name="password" type="password" autocomplete="current-password" placeholder="MẬT KHẨU" required maxlength="128">
                        <button class="password-toggle" type="button" aria-label="Hiện mật khẩu" aria-pressed="false" data-password-toggle>Hiện</button>
                    </div>

                    <div class="form-options">
                        <label class="remember-option"><input type="checkbox" name="remember"> <span>Ghi nhớ đăng nhập</span></label>
                        <button type="button" class="text-button" data-forgot-password>Quên mật khẩu?</button>
                    </div>

                    <p class="form-message${empty error and not empty success ? ' success' : ''}" id="form-message" role="status" aria-live="polite" ${empty error and empty success ? 'hidden' : ''}><c:out value="${not empty error ? error : success}" /></p>
                    <button class="submit-button" type="submit">Đăng nhập <span aria-hidden="true">→</span></button>
                </form>

                <c:if test="${localAdminSetup}">
                    <details id="admin-setup" data-url="<c:url value='/api/setup' />"
                             data-auth-url="<c:url value='/api/auth' />" hidden>
                        <summary>Tạo tài khoản Admin đầu tiên</summary>
                        <form class="auth-form" method="post">
                            <label>Tên đăng nhập <input name="username" value="admin" required minlength="3" maxlength="50" autocomplete="username"></label>
                            <label>Mật khẩu <input name="password" type="password" required minlength="10" maxlength="128" autocomplete="new-password"></label>
                            <label>Nhập lại mật khẩu <input name="confirmPassword" type="password" required minlength="10" maxlength="128" autocomplete="new-password"></label>
                            <p data-setup-message role="status"></p>
                            <button class="submit-button" type="submit">Tạo Admin</button>
                        </form>
                    </details>
                </c:if>
                <div class="form-bottom"><span>Bạn chưa có tài khoản?</span><a class="text-button" href="<c:url value='/register' />">Đăng ký</a></div>
                <p class="role-note">Sau khi xác thực, hệ thống sẽ mở giao diện phù hợp với vai trò tài khoản của bạn.</p>
            </div>

            <footer class="panel-footer"><span>© VinFast ElectricCarSale</span><span>Đăng nhập an toàn</span></footer>
        </div>
    </section>
</main>
</body>
</html>
