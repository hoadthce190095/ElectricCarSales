<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%
request.setAttribute("customerTitle", "Tài khoản của tôi");
request.setAttribute("customerEyebrow", "CHÀO MỪNG TRỞ LẠI");
%>
<%@ include file="../common/customer-shell.jspf" %>
<section class="welcome-banner">
    <div class="welcome-copy">
        <span class="welcome-kicker">VINFAST ELECTRIC CAR SALE</span>
        <h2>Mỗi hành trình xanh<br>đều bắt đầu từ bạn.</h2>
        <p>Theo dõi thông tin xe, đơn hàng, chuyển khoản MB Bank và hợp đồng của bạn tại một nơi.</p>
        <div style="display:flex;gap:10px;flex-wrap:wrap;">
            <a class="button button-light" href="<%= customerContext %>/index.jsp?view=customer#models">Khám phá xe <span>↗</span></a>
            <button type="button" class="button button-secondary" data-open-consult-popup style="background:rgba(255,255,255,0.18);color:#fff;border-color:rgba(255,255,255,0.35);">✧ Mở popup tư vấn và báo giá</button>
        </div>
    </div>
    <div class="welcome-art" aria-hidden="true">
        <div class="art-glow"></div>
        <div class="art-ring ring-one"></div>
        <div class="art-ring ring-two"></div>
        <div class="art-car">
            <span class="car-window"></span>
            <span class="car-body"></span>
            <i class="wheel wheel-left"></i>
            <i class="wheel wheel-right"></i>
            <span class="car-light"></span>
        </div>
        <span class="art-spark spark-one">✳</span>
        <span class="art-spark spark-two">✦</span>
        <span class="art-label">PURE<br>ELECTRIC</span>
    </div>
</section>
<div class="section-heading">
    <div>
        <span class="section-overline">TỔNG QUAN</span>
        <h2>Không gian của bạn</h2>
    </div>
    <span class="updated-note"><i></i> Tài khoản khách hàng</span>
</div>
<section class="summary-grid" aria-label="Tóm tắt tài khoản">
    <a class="summary-card" href="<%= customerContext %>/orders">
        <span class="summary-icon icon-orders">▤</span>
        <span class="summary-label">Đơn hàng</span>
        <strong><c:out value="${not empty customerOrders ? customerOrders.size() : 0}"/></strong>
        <span class="summary-foot">Đơn hàng của bạn <b>↗</b></span>
    </a>
    <a class="summary-card" href="<%= customerContext %>/payment/history">
        <span class="summary-icon icon-payments">◉</span>
        <span class="summary-label">Thanh toán</span>
        <strong><c:out value="${not empty customerPayments ? customerPayments.size() : 0}"/></strong>
        <span class="summary-foot">Lịch sử chuyển khoản <b>↗</b></span>
    </a>
    <a class="summary-card" href="<%= customerContext %>/contract">
        <span class="summary-icon icon-contracts">▧</span>
        <span class="summary-label">Hợp đồng</span>
        <strong><c:out value="${not empty customerContracts ? customerContracts.size() : 0}"/></strong>
        <span class="summary-foot">Hợp đồng &amp; Ký OTP <b>↗</b></span>
    </a>
</section>
<section aria-label="Truy cập nhanh các mục tài khoản">
    <div class="section-heading" style="margin-top:24px;">
        <div><span class="section-overline">CÁC MỤC TÀI KHOẢN</span><h2>Truy cập nhanh</h2></div>
    </div>
    <div class="summary-grid">
        <a class="summary-card" href="<%= customerContext %>/customer/my-invoices.jsp"><span class="summary-icon icon-contracts">▨</span><span class="summary-label">Hóa đơn</span><strong>Mở hóa đơn</strong><span class="summary-foot">Hóa đơn mua xe <b>↗</b></span></a>
        <a class="summary-card" href="<%= customerContext %>/customer/ai-assistant.jsp"><span class="summary-icon icon-payments">✧</span><span class="summary-label">Trợ lý xe</span><strong>Tư vấn xe</strong><span class="summary-foot">Tìm xe phù hợp <b>↗</b></span></a>
        <a class="summary-card" href="<%= customerContext %>/customer/profile?view=profile"><span class="summary-icon icon-contracts">◎</span><span class="summary-label">Hồ sơ cá nhân</span><strong>Thông tin của bạn</strong><span class="summary-foot">Xem và chỉnh sửa <b>↗</b></span></a>
        <a class="summary-card" href="<%= customerContext %>/index.jsp#models"><span class="summary-icon icon-orders">↗</span><span class="summary-label">Khám phá xe</span><strong>Danh mục VinFast</strong><span class="summary-foot">Chọn mẫu xe <b>↗</b></span></a>
    </div>
</section>
<div class="dashboard-grid">
    <section class="panel recent-panel">
        <div class="panel-heading">
            <div>
                <span class="section-overline">CẬP NHẬT MỚI NHẤT</span>
                <h2>Đơn hàng gần đây</h2>
            </div>
            <a class="text-link" href="<%= customerContext %>/orders">Xem tất cả <span>→</span></a>
        </div>
        <c:choose>
            <c:when test="${not empty customerOrders}">
                <div class="table-wrap">
                    <table>
                        <thead>
                            <tr>
                                <th>MÃ ĐƠN</th>
                                <th>MẪU XE</th>
                                <th>TỔNG TIỀN</th>
                                <th>CÒN LẠI</th>
                                <th>TRẠNG THÁI</th>
                                <th>THAO TÁC</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="ord" items="${customerOrders}" end="4">
                                <tr>
                                    <td><strong>#<c:out value="${ord.id}"/></strong></td>
                                    <td><c:out value="${ord.modelName}" default="Xe VinFast"/> <small style="display:block;color:#64748b;"><c:out value="${ord.versionName}"/></small></td>
                                    <td><fmt:formatNumber value="${ord.originalAmount}" pattern="#,###"/> ₫</td>
                                    <td><fmt:formatNumber value="${ord.remainingAmount}" pattern="#,###"/> ₫</td>
                                    <td><span class="soft-badge"><c:out value="${ord.status}"/></span></td>
                                    <td>
                                        <a class="text-link" href="<%= customerContext %>/orders">Đơn hàng &amp; thanh toán ↗</a>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="empty-state">
                    <div class="empty-illustration">
                        <span>▤</span>
                        <i>✳</i>
                    </div>
                    <strong>Chưa có đơn hàng nào</strong>
                    <p>Gửi yêu cầu tư vấn để Sales Staff hỗ trợ lập đơn hàng cho bạn.</p>
                    <a href="<%= customerContext %>/consultation" class="text-link">Gửi yêu cầu tư vấn <span>→</span></a>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
    <section class="panel profile-panel" id="profile">
        <div class="panel-heading">
            <div>
                <span class="section-overline">THÔNG TIN CỦA BẠN</span>
                <h2>Hồ sơ cá nhân</h2>
            </div>
            <a class="edit-link" href="#profile" data-profile-edit>Chỉnh sửa <span>↗</span></a>
        </div>
        <c:if test="${param.updated == '1'}">
            <div class="info-note" style="margin-bottom:12px;border-color:#86efac;background:#f0fdf4;color:#166534;">
                <span>✓</span><p>Đã cập nhật hồ sơ cá nhân thành công.</p>
            </div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="info-note" style="margin-bottom:12px;border-color:#fca5a5;background:#fef2f2;color:#991b1b;">
                <span>!</span><p><c:out value="${error}"/></p>
            </div>
        </c:if>
        <div class="profile-summary">
            <div class="profile-avatar" data-avatar-wrap>
                <svg class="avatar-placeholder" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="8" r="4" stroke="currentColor" stroke-width="1.6"/><path d="M4.5 21c.7-4.1 3.3-6.2 7.5-6.2s6.8 2.1 7.5 6.2" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>
                <img data-avatar-image src="<%= customerContext %>/customer/avatar?v=<%= System.currentTimeMillis() %>" alt="Ảnh đại diện" hidden>
            </div>
            <div>
                <strong><c:out value="${customer.customerName}" default="Khách hàng" /></strong>
                <span>Tài khoản: <c:out value="${sessionScope.username}" /></span>
            </div>
            <span class="verified-badge">✓ Đã xác thực</span>
        </div>
        <div class="profile-fields">
            <div>
                <span>Họ và tên</span>
                <strong><c:out value="${customer.customerName}" default="Chưa cập nhật" /></strong>
            </div>
            <div>
                <span>Số điện thoại</span>
                <strong><c:out value="${customer.phone}" default="Chưa cập nhật" /></strong>
            </div>
            <div>
                <span>Email</span>
                <strong><c:out value="${customer.email}" default="Chưa cập nhật" /></strong>
            </div>
            <div>
                <span>Địa chỉ</span>
                <strong><c:out value="${customer.address}" default="Chưa cập nhật" /></strong>
            </div>
        </div>
        <form id="profile-edit-form" class="customer-form" method="post" enctype="multipart/form-data" action="<%= customerContext %>/customer/profile" hidden style="margin-top:14px;padding-top:14px;border-top:1px solid #e2e8f0;">
            <label>Họ và tên
                <input name="customerName" type="text" maxlength="100" value="<c:out value='${customer.customerName}'/>" required>
            </label>
            <label>Số điện thoại
                <input name="phone" type="tel" pattern="0[0-9]{9,10}" maxlength="11" value="<c:out value='${customer.phone}'/>" required>
            </label>
            <label>Email
                <input name="email" type="email" maxlength="100" value="<c:out value='${customer.email}'/>" required>
            </label>
            <label>Địa chỉ bàn giao xe
                <input name="address" type="text" maxlength="255" value="<c:out value='${customer.address}'/>">
            </label>
            <label>Ảnh đại diện (JPEG, PNG hoặc WebP · tối đa 2 MB)
                <input name="avatar" type="file" accept="image/jpeg,image/png,image/webp">
            </label>
            <button type="submit" class="button button-primary">Lưu cập nhật hồ sơ →</button>
        </form>
    </section>
</div>
<%@ include file="../common/customer-shell-end.jspf" %>
