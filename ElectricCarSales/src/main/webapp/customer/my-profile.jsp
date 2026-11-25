<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
request.setAttribute("customerTitle", "Hồ sơ cá nhân");
request.setAttribute("customerEyebrow", "THÔNG TIN TÀI KHOẢN");
%>
<%@ include file="../common/customer-shell.jspf" %>
<section class="panel profile-panel customer-profile-page" id="profile">
    <div class="panel-heading">
        <div>
            <span class="section-overline">THÔNG TIN CỦA BẠN</span>
            <h2>Hồ sơ cá nhân</h2>
        </div>
        <a class="edit-link" href="#profile-edit-form" data-profile-edit>Chỉnh sửa <span>↗</span></a>
    </div>
    <c:if test="${param.updated == '1'}">
        <div class="info-note" style="margin:16px 0;border-color:#86efac;background:#f0fdf4;color:#166534;">
            <span>✓</span><p>Đã cập nhật hồ sơ cá nhân thành công.</p>
        </div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="info-note" role="alert" style="margin:16px 0;border-color:#fca5a5;background:#fef2f2;color:#991b1b;">
            <span>!</span><p><c:out value="${error}"/></p>
        </div>
    </c:if>
    <div class="profile-summary">
        <div class="profile-avatar" data-avatar-wrap>
            <svg class="avatar-placeholder" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="8" r="4" stroke="currentColor" stroke-width="1.6"/><path d="M4.5 21c.7-4.1 3.3-6.2 7.5-6.2s6.8 2.1 7.5 6.2" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>
            <img data-avatar-image src="<%= customerContext %>/customer/avatar?v=<%= System.currentTimeMillis() %>" alt="Ảnh đại diện" hidden>
        </div>
        <div>
            <strong><c:out value="${customer.customerName}" default="Khách hàng"/></strong>
            <span>Tên tài khoản: <c:out value="${sessionScope.username}"/></span>
        </div>
        <span class="verified-badge">✓ Đã xác thực</span>
    </div>
    <div class="profile-fields" id="profile-display-view">
        <div><span>Họ và tên</span><strong><c:out value="${customer.customerName}" default="Chưa cập nhật"/></strong></div>
        <div><span>Số điện thoại</span><strong><c:out value="${customer.phone}" default="Chưa cập nhật"/></strong></div>
        <div><span>Email</span><strong><c:out value="${customer.email}" default="Chưa cập nhật"/></strong></div>
        <div><span>Địa chỉ</span><strong><c:out value="${customer.address}" default="Chưa cập nhật"/></strong></div>
    </div>
    <form id="profile-edit-form" class="customer-form" method="post" enctype="multipart/form-data" action="<%= customerContext %>/customer/profile?view=profile" hidden style="display:none;margin-top:18px;padding-top:18px;border-top:1px solid #e2e8f0;">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>">
        <label>Họ và tên
            <input name="customerName" type="text" maxlength="100" value="<c:out value='${customer.customerName}'/>" required>
        </label>
        <label>Số điện thoại
            <input name="phone" type="tel" pattern="0[0-9]{9}" maxlength="10" value="<c:out value='${customer.phone}'/>" title="Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng số 0 (ví dụ: 0912345678)" required>
        </label>
        <label>Email đã xác thực OTP
            <input type="email" value="<c:out value='${customer.email}'/>" readonly aria-describedby="profile-email-verified-note">
            <small id="profile-email-verified-note" style="color:#64748b;">Email đã xác thực nên không thể chỉnh sửa.</small>
        </label>
        <label>Địa chỉ bàn giao xe
            <input name="address" type="text" maxlength="255" value="<c:out value='${customer.address}'/>">
        </label>
        <div style="display:flex;gap:10px;align-items:center;">
            <button type="submit" class="button button-primary">Lưu cập nhật hồ sơ →</button>
            <button type="button" class="button button-secondary" id="btn-cancel-profile-edit" style="background:#fff;border:1px solid #cbd5e1;color:#475569;padding:9px 16px;border-radius:6px;font-weight:600;cursor:pointer;">Hủy bỏ</button>
        </div>
    </form>
</section>
<%@ include file="../common/customer-shell-end.jspf" %>
