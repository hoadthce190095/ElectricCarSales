<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Xác thực OTP</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
</head>

<body class="auth-page">
<main class="auth-card">
    <a class="brand" href="${pageContext.request.contextPath}/index.jsp">⚡ ELECTRIC CAR SALES</a>
    <p class="eyebrow">BẢO MẬT TÀI KHOẢN</p>
    <h1>Xác thực OTP</h1>
    <c:if test="${not empty success}">
        <div class="alert success">
            <c:out value="${success}" />
        </div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert error">
            <c:out value="${error}" />
        </div>
    </c:if>
    <c:if test="${not empty otpForTesting}">
        <div class="test-otp"><strong>OTP dùng để kiểm thử:
            <c:out value="${otpForTesting}" />
        </strong><br><small>Chỉ hiển thị tạm thời khi test local. Cần tích hợp email/SMS trước khi triển
            khai thật.</small></div>
    </c:if>
    <p class="muted">Nhập mã OTP 6 chữ số. Mã có hiệu lực trong 5 phút.</p>
    <form action="${pageContext.request.contextPath}/verify-otp" method="post"><input type="hidden"
                                                                                      name="userId" value="${userId}">
        <label for="otp">Mã xác thực</label><input id="otp" name="otp" inputmode="numeric"
                                                   pattern="[0-9]{6}" minlength="6" maxlength="6" placeholder="000000" required>
        <button type="submit">Xác nhận tài khoản</button>
    </form>
    <p class="auth-footer"><a href="${pageContext.request.contextPath}/login">Quay lại đăng nhập</a></p>
</main>
</body>

</html>