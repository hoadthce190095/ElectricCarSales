<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("screenTitle", "Đăng ký tài khoản");
   request.setAttribute("screenDescription", "Nhập thông tin của bạn để đăng ký tài khoản khách hàng.");
   request.setAttribute("authScreen", true); %>
<%@ include file="../common/data-shell.jspf" %>
<section class="data-panel" aria-label="Thông tin đăng ký">
    <p class="data-message" role="status">Kênh gửi OTP chưa được cấu hình. Hệ thống hiện chưa thể hoàn tất đăng ký; vui lòng liên hệ quản trị viên để được hỗ trợ.</p>
    <c:if test="${not empty error}"><p class="data-message data-error" role="alert"><c:out value="${error}" /></p></c:if>
    <form class="data-form" id="register-form" method="post" action="<c:url value='/register' />">
        <label for="name">Họ và tên<input id="name" name="name" autocomplete="name" maxlength="100" value="<c:out value='${name}' />" required></label>
        <label for="username">Tên đăng nhập<input id="username" name="username" autocomplete="username" minlength="5" maxlength="50" pattern="[A-Za-z0-9_]{5,50}" value="<c:out value='${username}' />" required></label>
        <label for="phone">Số điện thoại<input id="phone" name="phone" type="tel" autocomplete="tel" pattern="0[0-9]{9,10}" maxlength="11" value="<c:out value='${phone}' />" required></label>
        <label for="email">Email<input id="email" name="email" type="email" autocomplete="email" maxlength="100" value="<c:out value='${email}' />" required></label>
        <label for="address">Địa chỉ<textarea id="address" name="address" autocomplete="street-address" maxlength="255" rows="2"><c:out value="${address}" /></textarea></label>
        <label for="password">Mật khẩu<input id="password" name="password" type="password" autocomplete="new-password" minlength="8" maxlength="72" required></label>
        <label for="confirmPassword">Nhập lại mật khẩu<input id="confirmPassword" name="confirmPassword" type="password" autocomplete="new-password" minlength="8" maxlength="72" required></label>
        <button type="submit">Kiểm tra thông tin đăng ký</button>
    </form>
    <div class="data-footer"><a href="<c:url value='/login' />">Đã có tài khoản? Đăng nhập</a><a href="mailto:hotro@electriccarsale.vn">Liên hệ hỗ trợ</a></div>
</section>
<%@ include file="../common/data-shell-end.jspf" %>
