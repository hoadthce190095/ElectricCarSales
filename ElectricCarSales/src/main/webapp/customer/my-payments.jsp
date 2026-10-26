<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<% request.setAttribute("customerTitle", "Thanh toán"); request.setAttribute("customerEyebrow", "THANH TOÁN AN TOÀN"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="page-intro">
    <p>Xem các khoản đặt cọc, thanh toán còn lại và kỳ trả góp được ghi nhận trong giao dịch của bạn.</p>
    <a class="button button-primary" href="<%= request.getContextPath() %>/customer/payment-demo.jsp">Mở demo cổng thanh toán <span>→</span></a>
</div>
<section class="payment-highlight">
    <div>
        <span class="section-overline">TỔNG QUAN THANH TOÁN</span>
        <h2>Các khoản cần thanh toán</h2>
        <p>Số tiền và hạn thanh toán sẽ được hiển thị theo dữ liệu giao dịch đã xác nhận.</p>
    </div>
    <div class="payment-total">
        <span>Số dư cần thanh toán</span>
        <strong><c:choose><c:when test="${not empty outstandingDebt}"><c:out value="${outstandingDebt}"/> ₫</c:when><c:otherwise>—</c:otherwise></c:choose></strong>
        <small><c:choose><c:when test="${not empty orderBalance}">Công nợ của đơn hàng đang xem</c:when><c:otherwise>Chọn đơn hàng để xem công nợ</c:otherwise></c:choose></small>
    </div>
    <div class="highlight-orbit">₫</div>
</section>
<c:if test="${not empty orderBalance}">
    <section class="panel">
        <h2>Đơn hàng #<c:out value="${requestedOrderId}"/></h2>
        <p>Tổng giá trị: <c:out value="${orderBalance.totalAmount}"/> ₫</p>
        <p>Tiền đặt cọc theo đơn: <c:out value="${orderBalance.depositAmount}"/> ₫</p>
        <p>Đã thanh toán thành công: <c:out value="${orderBalance.successfulPayments}"/> ₫</p>
        <p>Công nợ còn lại: <c:out value="${outstandingDebt}"/> ₫</p>
    </section>
</c:if>
<div class="filter-tabs" role="tablist" aria-label="Lọc giao dịch thanh toán">
    <button class="filter-tab active" type="button" role="tab" aria-selected="true">Tất cả <span>0</span>
    </button>
    <button class="filter-tab" type="button" role="tab" aria-selected="false">Đang chờ <span>0</span>
    </button>
    <button class="filter-tab" type="button" role="tab" aria-selected="false">Đã hoàn tất <span>0</span>
    </button>
</div>
<section class="panel table-panel">
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>MÃ THANH TOÁN</th>
                    <th>ĐƠN HÀNG</th>
                    <th>LOẠI KHOẢN</th>
                    <th>NGÀY</th>
                    <th>SỐ TIỀN</th>
                    <th>TRẠNG THÁI</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${not empty paymentHistory}">
                        <c:forEach var="payment" items="${paymentHistory}">
                            <tr>
                                <td><c:out value="${payment.referenceCode}" default="—"/></td>
                                <td><c:out value="${payment.orderID}"/></td>
                                <td><c:out value="${payment.paymentTypeName}" default="—"/></td>
                                <td><c:out value="${payment.paymentDate}" default="—"/></td>
                                <td><c:out value="${payment.paymentAmount}"/> ₫</td>
                                <td><c:out value="${payment.paymentStatusCode}" default="—"/></td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td colspan="6">
                                <div class="table-empty">
                                    <span>◉</span>
                                    <strong>Chưa có khoản thanh toán</strong>
                                    <p>Lịch sử đặt cọc, thanh toán và trả góp sẽ được cập nhật khi phát sinh.</p>
                                </div>
                            </td>
                        </tr>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
</section>
<div class="info-note">
    <span>i</span>
    <p>Trạng thái thanh toán phản ánh dữ liệu hệ thống. Với chuyển khoản hoặc thanh toán tiền mặt, nhân viên có thẩm quyền có thể cần xác minh trước khi giao dịch được xác nhận.</p>
</div>
<c:if test="${paymentTestMode}"><div class="info-note"><span>i</span><p>Chế độ kiểm thử đang bật. Khi tích hợp cổng thanh toán, số tiền gửi cổng sẽ được quy đổi riêng; số tiền nghiệp vụ trong lịch sử vẫn giữ nguyên.</p></div></c:if>
<%@ include file="../common/customer-shell-end.jspf" %>
