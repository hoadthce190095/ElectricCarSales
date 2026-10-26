<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("screenTitle", "Xác thực OTP");
   request.setAttribute("screenDescription", "Sử dụng mã gồm 6 chữ số đã được gửi cho tài khoản đăng ký.");
   request.setAttribute("authScreen", true); %>
<%@ include file="../common/data-shell.jspf" %>
<section class="data-panel" aria-label="Xác thực đăng ký">
    <c:if test="${not empty error}"><p class="data-message data-error" role="alert"><c:out value="${error}" /></p></c:if>
    <c:choose>
        <c:when test="${userId gt 0}">
            <form class="data-form" id="otp-form" method="post" action="<c:url value='/verify-otp' />">
                <input type="hidden" name="userId" value="<c:out value='${userId}' />">
                <label for="otp">Mã OTP<input id="otp" name="otp" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" minlength="6" maxlength="6" required></label>
                <button type="submit">Xác thực</button>
            </form>
        </c:when>
        <c:otherwise><p class="data-message">Không có yêu cầu xác thực hợp lệ. Vui lòng quay lại trang đăng ký.</p></c:otherwise>
    </c:choose>
    <div class="data-footer"><a href="<c:url value='/register' />">Quay lại đăng ký</a><a href="<c:url value='/login' />">Đăng nhập</a></div>
</section>
<%@ include file="../common/data-shell-end.jspf" %>
