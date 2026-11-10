<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
request.setAttribute("customerTitle", "Tài khoản của tôi");
request.setAttribute("customerEyebrow", "CHÀO MỪNG TRỞ LẠI");
%>
<%@ include file="../common/customer-shell.jspf" %>
<section class="welcome-banner">
    <div class="welcome-copy">
        <span class="welcome-kicker">VINFAST ELECTRIC CAR SALE</span>
        <h2>Mỗi hành trình xanh<br>đều bắt đầu từ bạn.</h2>
        <p>Theo dõi thông tin xe, đơn hàng và các giao dịch của bạn tại một nơi.</p>
        <a class="button button-light" href="<%= customerContext %>/index.jsp?view=customer#models">Khám phá xe <span>↗</span>
        </a>
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
    <span class="updated-note">
        <i></i> Tài khoản khách hàng</span>
</div>
<section class="summary-grid" aria-label="Tóm tắt tài khoản">
    <a class="summary-card" href="<%= customerContext %>/customer/my-orders.jsp">
        <span class="summary-icon icon-orders">▤</span>
        <span class="summary-label">Đơn hàng</span>
        <strong>—</strong>
        <span class="summary-foot">Đơn hàng của bạn <b>↗</b>
        </span>
    </a>
    <a class="summary-card" href="<%= customerContext %>/customer/my-payments.jsp">
        <span class="summary-icon icon-payments">◉</span>
        <span class="summary-label">Thanh toán</span>
        <strong>—</strong>
        <span class="summary-foot">Lịch sử giao dịch <b>↗</b>
        </span>
    </a>
    <a class="summary-card" href="<%= customerContext %>/customer/my-contracts.jsp">
        <span class="summary-icon icon-contracts">▧</span>
        <span class="summary-label">Hợp đồng</span>
        <strong>—</strong>
        <span class="summary-foot">Tài liệu của bạn <b>↗</b>
        </span>
    </a>
</section>
<div class="dashboard-grid">
    <section class="panel recent-panel">
        <div class="panel-heading">
            <div>
                <span class="section-overline">CẬP NHẬT MỚI NHẤT</span>
                <h2>Đơn hàng gần đây</h2>
            </div>
            <a class="text-link" href="<%= customerContext %>/customer/my-orders.jsp">Xem tất cả <span>→</span>
            </a>
        </div>
        <div class="empty-state">
            <div class="empty-illustration">
                <span>▤</span>
                <i>✳</i>
            </div>
            <strong>Chưa có đơn hàng nào</strong>
            <p>Đơn hàng và trạng thái giao dịch sẽ xuất hiện tại đây sau khi được ghi nhận.</p>
            <a href="<%= customerContext %>/index.jsp?view=customer#models" class="text-link">Tìm hiểu các mẫu xe <span>→</span>
            </a>
        </div>
    </section>
    <section class="panel profile-panel" id="profile">
        <div class="panel-heading">
            <div>
                <span class="section-overline">THÔNG TIN CỦA BẠN</span>
                <h2>Hồ sơ cá nhân</h2>
            </div>
            <a class="edit-link" href="#profile" data-profile-edit>Chỉnh sửa <span>↗</span>
            </a>
        </div>
        <div class="profile-summary">
            <div class="profile-avatar">KH</div>
            <div>
<strong><c:out value="${customer.customerName}" default="Khách hàng" /></strong>                <span>Tài khoản: <c:out value="${sessionScope.username}" /></span>
            </div>
            <span class="verified-badge">✓ Đã xác thực</span>
        </div>
        <div class="profile-fields">
            <div>
                <span>Họ và tên</span>
<strong><c:out value="${customer.customerName}" default="Chưa cập nhật" /></strong>            </div>
            <div>
                <span>Số điện thoại</span>
<strong><c:out value="${customer.phone}" default="Chưa cập nhật" /></strong>            </div>
            <div>
                <span>Email</span>
<strong><c:out value="${customer.email}" default="Chưa cập nhật" /></strong>            </div>
        </div>
        <p class="profile-note">Thông tin sẽ được tải từ tài khoản của bạn khi kết nối hệ thống.</p>
    </section>
</div>
<section class="journey-strip">
    <div class="journey-icon">✳</div>
    <div>
        <strong>Sẵn sàng cho hành trình mới?</strong>
        <span>Khám phá các mẫu xe điện và tìm chiếc xe phù hợp với bạn.</span>
    </div>
    <a href="<%= customerContext %>/index.jsp?view=customer#models">Khám phá xe <span>→</span>
    </a>
</section>
<%@ include file="../common/customer-shell-end.jspf" %>
