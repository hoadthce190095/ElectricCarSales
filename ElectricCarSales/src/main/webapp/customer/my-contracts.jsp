<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<% request.setAttribute("customerTitle", "Hợp đồng & tài liệu"); request.setAttribute("customerEyebrow", "HỒ SƠ GIAO DỊCH"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="page-intro">
    <p>Hợp đồng và tài liệu liên quan đến các giao dịch của bạn sẽ được lưu trữ tại đây.</p>
    <span class="soft-badge">Chỉ xem tài liệu được cấp quyền</span>
</div>
<c:choose>
<c:when test="${not empty installmentContract}">
<section class="panel document-panel">
    <div class="panel-heading">
        <div>
            <span class="section-overline">HỢP ĐỒNG TRẢ GÓP</span>
            <h2>Hợp đồng #<c:out value="${installmentContract.installmentContractID}"/></h2>
        </div>
        <span class="soft-badge"><c:out value="${installmentContract.status}"/></span>
    </div>
    <p>Đơn hàng #<c:out value="${installmentContract.orderID}"/> · <c:out value="${installmentContract.companyName}" default="Đơn vị tài chính"/> · <c:out value="${installmentContract.planName}" default="Gói trả góp"/></p>
    <p>Giá xe: <c:out value="${installmentContract.vehiclePrice}"/> ₫ · Trả trước: <c:out value="${installmentContract.downPayment}"/> ₫</p>
    <p>Số tiền vay: <c:out value="${installmentContract.loanAmount}"/> ₫ · Lãi: <c:out value="${installmentContract.totalInterest}"/> ₫</p>
    <c:if test="${not empty installmentCompany.paymentDueDay}">
        <p>Ngày đến hạn: trước <c:out value="${installmentCompany.paymentCutoffTime}"/> ngày <c:out value="${installmentCompany.paymentDueDay}"/> hằng tháng · Phí trễ hạn: <c:out value="${installmentCompany.lateFeeDailyPercent}"/>% số tiền kỳ/tháng trễ.</p>
    </c:if>
    <p>Tổng nghĩa vụ trả góp: <c:out value="${installmentContract.totalInstallmentAmount}"/> ₫ · Còn nợ: <c:out value="${outstandingDebt}"/> ₫</p>
    <h3>Lịch thanh toán</h3>
    <div class="table-wrap"><table>
        <thead><tr><th>KỲ</th><th>HẠN THANH TOÁN</th><th>SỐ TIỀN</th><th>ĐÃ TRẢ</th><th>TRẠNG THÁI</th></tr></thead>
        <tbody>
        <c:forEach var="schedule" items="${installmentSchedule}">
            <tr><td><c:out value="${schedule.periodNumber}"/></td><td><c:out value="${schedule.dueDate}"/></td><td><c:out value="${schedule.paymentAmount}"/> ₫</td><td><c:out value="${schedule.paidAmount}"/> ₫</td><td><c:out value="${schedule.status}"/></td></tr>
        </c:forEach>
        <c:if test="${empty installmentSchedule}"><tr><td colspan="5">Chưa có lịch thanh toán.</td></tr></c:if>
        </tbody>
    </table></div>
    <c:if test="${settlementQuoteUnavailable}"><p class="info-note">Chưa thể lập báo giá tất toán: hệ thống chưa có dữ liệu gốc, lãi phát sinh và phí được xác nhận bởi đơn vị tài chính.</p></c:if>
    <c:if test="${not empty settlementQuote}"><p>Gốc còn lại: <c:out value="${settlementQuote.principalRemaining}"/> ₫ · Lãi phát sinh: <c:out value="${settlementQuote.accruedInterest}"/> ₫ · Phí: <c:out value="${settlementQuote.settlementFee}"/> ₫ · Tổng tất toán: <c:out value="${settlementQuote.settlementAmount}"/> ₫</p></c:if>
    <c:if test="${not empty settlement}"><p>Đã ghi nhận tất toán ngày <c:out value="${settlement.settlementDate}"/> · <c:out value="${settlement.settlementAmount}"/> ₫ · Mã tham chiếu <c:out value="${settlement.referenceCode}" default="—"/></p></c:if>
</section>
</c:when>
<c:otherwise>
<c:if test="${not empty installmentPlans}">
<section class="panel document-panel">
    <div class="panel-heading"><div><span class="section-overline">KẾ HOẠCH TÀI CHÍNH</span><h2>Các gói trả góp</h2></div></div>
    <div class="table-wrap"><table>
        <thead><tr><th>ĐƠN VỊ</th><th>GÓI</th><th>KỲ HẠN</th><th>LÃI SUẤT NĂM</th><th>MÔ TẢ</th></tr></thead>
        <tbody><c:forEach var="plan" items="${installmentPlans}">
            <tr><td><c:out value="${plan.companyName}"/></td><td><c:out value="${plan.planName}"/></td><td><c:out value="${plan.loanTermMonths}"/> tháng</td><td><c:out value="${plan.interestRate}"/>%</td><td><c:out value="${plan.description}" default="—"/></td></tr>
        </c:forEach></tbody>
    </table></div>
    <h3>Đơn vị tài chính</h3>
    <ul><c:forEach var="company" items="${financialCompanies}"><li><c:out value="${company.companyName}"/> · Hạn trước <c:out value="${company.paymentCutoffTime}"/> ngày <c:out value="${company.paymentDueDay}"/> hằng tháng · Trễ hạn <c:out value="${company.lateFeeDailyPercent}"/>%/ngày · Phí tất toán: <c:choose><c:when test="${not empty company.earlySettlementFeePercent}"><c:out value="${company.earlySettlementFeePercent}"/>% gốc còn lại</c:when><c:otherwise>chưa được đơn vị tài chính xác nhận</c:otherwise></c:choose></li></c:forEach></ul>
</section>
</c:if>
<section class="panel document-panel">
    <div class="panel-heading">
        <div>
            <span class="section-overline">TÀI LIỆU CỦA BẠN</span>
            <h2>Hợp đồng</h2>
        </div>
        <button class="filter-select" type="button" data-document-filter>☷ <span>Tất cả tài liệu</span>⌄</button>
    </div>
    <div class="document-empty">
        <div class="document-stack">
            <span class="doc-sheet sheet-back"></span>
            <span class="doc-sheet sheet-front">
                <b>V</b>
                <i></i>
                <i></i>
                <i></i>
            </span>
            <span class="doc-lock">⌑</span>
        </div>
        <strong>Chưa có hợp đồng hoặc tài liệu</strong>
        <p>Tài liệu sẽ xuất hiện khi được tạo và liên kết với giao dịch của bạn.</p>
        <a class="text-link" href="<%= customerContext %>/customer/my-orders.jsp">Xem đơn hàng <span>→</span>
        </a>
    </div>
</section>
</c:otherwise>
</c:choose>
<%@ include file="../common/customer-shell-end.jspf" %>
