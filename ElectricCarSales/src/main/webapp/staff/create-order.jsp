<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Sales Staff · Lập đơn bán xe</title>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/payment-bank-transfer.css"></head>
<body class="finance-review"><main>
  <h1>Lập đơn hàng bán xe</h1>
  <p><a href="<%= request.getContextPath() %>/staff/dashboard">← Trang nhân viên</a></p>
  <p>Số tiền bán được lấy từ Cars.Price trong Database; hệ thống tự tính cọc 30% và còn lại 70%.</p>
  <c:if test="${not empty param.created}"><p role="status">Đã tạo đơn #<c:out value="${param.created}"/>. Khách hàng có thể mở trang Đơn hàng để xem.</p></c:if>
  <c:if test="${empty orderOptions.cars}">
    <p role="status" style="padding:12px;border:1px solid #f5c16c;background:#fffbeb;color:#854d0e;border-radius:7px;">
      Chưa có xe vật lý khả dụng trong Cars. Danh mục mẫu xe/phiên bản chưa phải tồn kho; Operations cần nhập xe bằng VIN và số máy thực tế trước khi Sales lập đơn.
    </p>
  </c:if>
  <form class="finance-panel" method="post" action="<%= request.getContextPath() %>/orders">
    <input type="hidden" name="csrfToken" value="<c:out value='${paymentCsrf}'/>">
    <label>Khách hàng <select name="customerId" required><option value="">Chọn khách hàng</option>
      <c:forEach var="item" items="${orderOptions.customers}"><option value="<c:out value='${item.id}'/>"><c:out value="${item.label}"/> (#<c:out value="${item.id}"/>)</option></c:forEach>
    </select></label>
    <label>Xe còn sẵn <select name="carId" required><option value="">${empty orderOptions.cars ? 'Kho chưa có xe khả dụng' : 'Chọn xe'}</option>
      <c:forEach var="item" items="${orderOptions.cars}"><option value="<c:out value='${item.id}'/>"><c:out value="${item.label}"/></option></c:forEach>
    </select></label>
    <label>Phương thức đơn hàng <select name="method" id="sales-order-method" required>
      <option value="BANK_TRANSFER">Trả thẳng - chuyển khoản</option><option value="CASH">Trả thẳng - tiền mặt</option>
      <option value="INSTALLMENT">Trả góp - đối tác giải ngân 70%</option>
    </select></label>
    <label>Đối tác tài chính (chỉ khi trả góp) <select name="companyId" id="sales-order-partner">
      <option value="">Không chọn (trả thẳng)</option>
      <c:forEach var="item" items="${orderOptions.partners}"><option value="<c:out value='${item.id}'/>"><c:out value="${item.label}"/></option></c:forEach>
    </select></label>
    <c:choose>
      <c:when test="${empty orderOptions.cars}"><button type="submit" disabled>Lập đơn hàng</button></c:when>
      <c:otherwise><button type="submit">Lập đơn hàng</button></c:otherwise>
    </c:choose>
  </form>
  <script>const method = document.getElementById('sales-order-method'); const partner = document.getElementById('sales-order-partner');
  function update() { partner.required = method.value === 'INSTALLMENT'; if (!partner.required) partner.value = ''; }
  method.addEventListener('change', update); update();</script>
</main></body></html>
