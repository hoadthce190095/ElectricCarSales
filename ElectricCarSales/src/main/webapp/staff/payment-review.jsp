<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Accountant · Đối chiếu thanh toán</title>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/payment-bank-transfer.css"></head><body class="finance-review">
<main>
    <h1>Đối chiếu tiền thực nhận (Accountant)</h1>
    <p><a href="<%= request.getContextPath() %>/staff/dashboard">← Trang nhân viên</a></p>
    <p>Chỉ xác nhận sau khi kiểm tra sao kê OCB hoặc chứng từ giải ngân từ đối tác tài chính. Mã tham chiếu và số tiền phải trùng khớp thực tế.</p>
    <c:if test="${param.updated eq '1'}"><p role="status">Thao tác đã được lưu vào cơ sở dữ liệu.</p></c:if>
    <section class="finance-panel"><h2>Theo dõi công nợ đơn hàng</h2>
        <c:if test="${empty openDebts}"><p>Không có đơn hàng còn công nợ.</p></c:if>
        <c:forEach var="debt" items="${openDebts}">
          <article style="border-top:1px solid #e2e8f0;padding:12px 0;">
            <strong>Đơn #<c:out value="${debt.orderId}"/> · <c:out value="${debt.customer}"/></strong>
            <p>Tổng: <c:out value="${debt.totalAmount}"/> VND · Cọc: <c:out value="${debt.depositAmount}"/> VND · Còn nợ: <strong><c:out value="${debt.remainingAmount}"/> VND</strong></p>
            <p>Phương thức: <c:out value="${debt.paymentMethod}"/> <c:if test="${not empty debt.company}">· Đối tác: <c:out value="${debt.company}"/></c:if> · Trạng thái đơn: <c:out value="${debt.orderStatus}"/> · Hợp đồng: <c:out value="${debt.contractStatus}" default="Chưa ký"/></p>
            <c:if test="${debt.paymentMethod eq 'INSTALLMENT' and debt.contractStatus eq 'SIGNED' and (debt.orderStatus eq 'DEPOSITED' or debt.orderStatus eq 'PROCESSING')}">
              <form method="post" action="<%= request.getContextPath() %>/staff/payment-review">
                <input type="hidden" name="csrfToken" value="<c:out value='${csrfTokenForPayment}'/>">
                <input type="hidden" name="action" value="partner">
                <input type="hidden" name="orderId" value="<c:out value='${debt.orderId}'/>">
                <button type="submit">Ghi nhận chờ giải ngân 70%</button>
              </form>
            </c:if>
          </article>
        </c:forEach>
    </section>
    <h2>Giao dịch đang chờ xác nhận</h2>
    <c:if test="${empty pendingPayments}"><p>Không có khoản PENDING.</p></c:if>
    <c:forEach var="p" items="${pendingPayments}">
      <section class="finance-panel">
        <h3>Khoản #<c:out value="${p.paymentId}"/> · Đơn #<c:out value="${p.orderId}"/></h3>
        <p>Khách hàng: <c:out value="${p.customer}"/> — Loại: <c:out value="${p.type}"/></p>
        <p>Số tiền yêu cầu: <strong><c:out value="${p.amount}"/> VND</strong></p>
        <p>Nội dung đối chiếu: <strong><c:out value="${p.reference}"/></strong></p>
        <form method="post" action="<%= request.getContextPath() %>/staff/payment-review">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfTokenForPayment}'/>">
            <input type="hidden" name="paymentId" value="<c:out value='${p.paymentId}'/>">
            <input type="hidden" name="action" value="confirm">
            <label>Số tiền thực nhận từ sao kê (VND) <input name="receivedAmount" type="number" step="1" min="1" required></label>
            <label>Mã giao dịch thật trên sao kê <input name="bankTransaction" maxlength="80" pattern="[A-Za-z0-9./_-]{4,80}" required></label>
            <label><input type="checkbox" name="checkedStatement" required> Tôi đã đối chiếu sao kê/ngân hàng và khoản này thực sự được nhận đủ.</label>
            <button type="submit">Xác nhận tiền đã nhận</button>
        </form>
        <form method="post" action="<%= request.getContextPath() %>/staff/payment-review">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfTokenForPayment}'/>">
            <input type="hidden" name="paymentId" value="<c:out value='${p.paymentId}'/>">
            <input type="hidden" name="action" value="reject">
            <label>Lý do từ chối <input name="reason" maxlength="200" required></label>
            <button type="submit" class="finance-reject">Từ chối yêu cầu</button>
        </form>
      </section>
    </c:forEach>
</main></body></html>
