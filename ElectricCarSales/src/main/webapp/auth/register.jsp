<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng ký | Electric Car Sales</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
</head>

<body class="auth-page">
<main class="auth-card wide">
    <a class="brand" href="${pageContext.request.contextPath}/index.jsp">⚡ ELECTRIC CAR SALES</a>
    <p class="eyebrow">TÀI KHOẢN KHÁCH HÀNG</p>
    <h1>Tạo tài khoản</h1>
    <p class="muted">Nhập thông tin bên dưới để đăng ký thành viên.</p>
    <c:if test="${not empty error}">
        <div class="alert error">
            <c:out value="${error}" />
        </div>
    </c:if>
    <form action="${pageContext.request.contextPath}/register" method="post">
        <label for="name">Họ và tên</label><input id="name" name="name" value="${name}" maxlength="100" autocomplete="name" required>
        <div class="form-grid">
            <div><label for="username">Tên đăng nhập</label><input id="username" name="username" value="${username}" minlength="5" maxlength="50" pattern="[A-Za-z0-9_]+" autocomplete="username" required><small>5-50 ký tự, chữ/số/gạch dưới.</small></div>
            <div><label for="phone">Số điện thoại</label><input id="phone" name="phone" value="${phone}" type="tel" pattern="0[0-9]{9,10}" maxlength="11" autocomplete="tel" required></div>
        </div>
        <label for="email">Email</label><input id="email" name="email" value="${email}" type="email" maxlength="100" autocomplete="email" required>
        <label for="address">Địa chỉ (không bắt buộc)</label><input id="address" name="address" value="${address}" maxlength="255" autocomplete="street-address">
        <div class="form-grid">
            <div><label for="password">Mật khẩu</label><input id="password" name="password" type="password" minlength="8" maxlength="72" autocomplete="new-password" required></div>
            <div><label for="confirmPassword">Nhập lại mật khẩu</label><input id="confirmPassword" name="confirmPassword" type="password" minlength="8" maxlength="72" autocomplete="new-password" required></div>
        </div>
        <button type="submit">Đăng ký tài khoản</button></form>
    <p class="auth-footer">Đã có tài khoản? <a href="${pageContext.request.contextPath}/login">Đăng nhập</a></p>
    <a class="back-link" href="${pageContext.request.contextPath}/index.jsp">← Quay lại trang chủ</a></main>
</body>

</html>