<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Sales Staff · Lập đơn bán xe</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/payment-bank-transfer.css">
  <style>
    .kyc-box { background:#f8fafc; border:1px solid #cbd5e1; border-radius:8px; padding:16px; margin:16px 0; }
    .kyc-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:10px; flex-wrap:wrap; gap:8px; }
    .kyc-title { color:#0f172a; font-size:14px; font-weight:700; }
    .btn-fetch { background:#0284c7; color:#fff; border:none; padding:6px 14px; border-radius:6px; font-size:12px; font-weight:600; cursor:pointer; transition:background .2s; }
    .btn-fetch:hover { background:#0369a1; }
    .kyc-grid { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
    @media (max-width: 640px) { .kyc-grid { grid-template-columns:1fr; } }
    .kyc-field { margin-bottom:8px; }
    .kyc-field label { font-size:12px; color:#475569; display:block; margin-bottom:4px; font-weight:600; }
    .kyc-field input, .kyc-field textarea { width:100%; box-sizing:border-box; padding:8px 10px; border:1px solid #cbd5e1; border-radius:6px; font-size:13px; font-family:inherit; }
    .kyc-field input:focus, .kyc-field textarea:focus { outline:none; border-color:#0284c7; box-shadow:0 0 0 2px rgba(2,132,199,.2); }
    .kyc-hint { font-size:12px; color:#64748b; margin:0 0 12px; line-height:1.5; }
  </style>
</head>
<body class="finance-review">
<main>
  <h1>Lập đơn hàng bán xe</h1>
  <p><a href="<%= request.getContextPath() %>/staff/dashboard">← Trang nhân viên</a></p>
  <p>Hệ thống lấy giá xe từ Database, tự áp dụng khuyến mãi còn hiệu lực (nếu có), rồi tính cọc 30% và số tiền còn lại.</p>

  <c:if test="${not empty param.created}">
    <p role="status" style="padding:12px;border:1px solid #86efac;background:#f0fdf4;color:#166534;border-radius:7px;">
      ✓ Đã tạo đơn #<c:out value="${param.created}"/>. Khách hàng có thể mở trang Đơn hàng cá nhân để xem và thanh toán cọc 30%.
    </p>
  </c:if>

  <c:if test="${empty orderOptions.cars}">
    <p role="status" style="padding:12px;border:1px solid #f5c16c;background:#fffbeb;color:#854d0e;border-radius:7px;">
      Chưa có xe vật lý khả dụng trong Cars. Danh mục mẫu xe/phiên bản chưa phải tồn kho; Operations cần nhập xe bằng VIN và số máy thực tế trước khi Sales lập đơn.
    </p>
  </c:if>

  <form class="finance-panel" method="post" action="<%= request.getContextPath() %>/orders">
    <input type="hidden" name="csrfToken" value="<c:out value='${paymentCsrf}'/>">

    <label>Khách hàng
      <select name="customerId" id="sales-customer-select" required>
        <option value="">-- Chọn khách hàng --</option>
        <c:forEach var="item" items="${orderOptions.customers}">
          <option value="<c:out value='${item.id}'/>"
                  data-name="<c:out value='${item.name}'/>"
                  data-phone="<c:out value='${item.phone}'/>"
                  data-email="<c:out value='${item.email}'/>"
                  data-address="<c:out value='${item.address}'/>"
                  <c:if test="${selectedCustomerId eq item.id}">selected</c:if>>
            <c:out value="${item.label}"/> (#<c:out value="${item.id}"/>)
          </option>
        </c:forEach>
      </select>
    </label>

    <%-- KHỐI XÁC MINH & BỔ SUNG THÔNG TIN KHÁCH HÀNG (KYC) --%>
    <div class="kyc-box">
      <div class="kyc-header">
        <span class="kyc-title">🛡️ Thông tin người mua (Dùng cho Hợp đồng, Hóa đơn &amp; Bàn giao xe)</span>
        <button type="button" class="btn-fetch" id="btn-fetch-customer" title="Tự động điền thông tin sẵn có từ hồ sơ khách hàng đã đăng ký">
          ⚡ Lấy thông tin từ hồ sơ khách hàng
        </button>
      </div>
      <p class="kyc-hint">
        Sales Staff trao đổi với khách hàng qua Trò chuyện tư vấn / điện thoại và xác nhận lại 4 trường thông tin chuẩn theo giấy tờ pháp lý.
        Dữ liệu mặc định lấy từ hồ sơ khách hàng đã đăng ký, Sales có thể bổ sung / chỉnh sửa nếu khách cập nhật thông tin mới.
      </p>

      <div class="kyc-grid">
        <div class="kyc-field">
          <label for="customerName">Họ và tên khách hàng (chuẩn theo CCCD/CMND) *</label>
          <input type="text" name="customerName" id="customerName" required maxlength="100" placeholder="Ví dụ: Nguyễn Văn A">
        </div>
        <div class="kyc-field">
          <label for="customerPhone">Số điện thoại liên lạc chính thức *</label>
          <input type="tel" name="customerPhone" id="customerPhone" required pattern="0[0-9]{9}" maxlength="10" placeholder="Ví dụ: 0912345678" title="Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng số 0 (ví dụ: 0912345678)">
        </div>
      </div>

      <div class="kyc-grid">
        <div class="kyc-field">
          <label for="customerEmail">Email nhận chứng từ &amp; thông báo</label>
          <input type="email" name="customerEmail" id="customerEmail" maxlength="100" pattern="^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$" placeholder="Ví dụ: nguyenvana@example.com" title="Email phải có định dạng hợp lệ (ví dụ: nguyenvana@example.com)">
        </div>
        <div class="kyc-field">
          <label for="customerAddress">Địa chỉ bàn giao xe / thường trú (để lập hợp đồng &amp; xuất hóa đơn) *</label>
          <input type="text" name="customerAddress" id="customerAddress" required maxlength="255" placeholder="Ví dụ: Số 123 Đường Hai Bà Trưng, Quận Ninh Kiều, Cần Thơ">
        </div>
      </div>

      <div class="kyc-field" style="margin-bottom:0;grid-column:1/-1;background:#f8fafc;padding:12px 14px;border:1px solid #cbd5e1;border-radius:8px;">
        <label style="font-size:12px;font-weight:700;color:#1e3a8a;margin-bottom:8px;display:block;text-transform:uppercase;letter-spacing:0.5px;">Thông tin nhân viên đang tư vấn</label>
        <c:choose>
          <c:when test="${not empty consultingStaff}">
            <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(200px, 1fr));gap:8px;font-size:13px;">
              <div><span style="color:#64748b;">Họ và tên:</span> <strong><c:out value="${consultingStaff.employeeName}"/></strong></div>
              <div><span style="color:#64748b;">Username:</span> <strong><c:out value="${consultingStaff.username}"/></strong></div>
              <div><span style="color:#64748b;">Mã nhân viên:</span> <strong><c:out value="${consultingStaff.employeeCode}"/></strong></div>
              <div><span style="color:#64748b;">Email:</span> <strong><c:out value="${consultingStaff.email}" default="—"/></strong></div>
              <div><span style="color:#64748b;">Số điện thoại:</span> <strong><c:out value="${consultingStaff.phone}" default="—"/></strong></div>
            </div>
          </c:when>
          <c:otherwise>
            <div style="color:#94a3b8;font-size:13px;font-style:italic;">Chưa có nhân viên phụ trách tư vấn</div>
          </c:otherwise>
        </c:choose>
      </div>
    </div>

    <label>Xe còn sẵn
      <select name="carId" required>
        <option value="">${empty orderOptions.cars ? 'Kho chưa có xe khả dụng' : 'Chọn xe'}</option>
        <c:forEach var="item" items="${orderOptions.cars}">
          <option value="<c:out value='${item.id}'/>" data-promotion="<c:out value='${item.promotionName}'/>"
                  data-original-price="<c:out value='${item.originalPrice}'/>" data-sale-price="<c:out value='${item.salePrice}'/>">
            <c:out value="${item.label}"/>
          </option>
        </c:forEach>
      </select>
    </label>

    <p role="note" style="margin:6px 0 14px;padding:10px 12px;border:1px solid #fed7aa;background:#fff7ed;color:#9a3412;border-radius:7px;font-size:13px;">
      Khuyến mãi đang hiệu lực được tự động trừ vào giá đơn hàng và ghi rõ tên chương trình, giá gốc, mức giảm trong ghi chú đơn.
    </p>

    <label>Ghi chú cho đơn hàng (không bắt buộc)
      <textarea name="salesNote" maxlength="200" rows="2" placeholder="Ghi chú thêm của Sales; thông tin khuyến mãi sẽ được hệ thống tự động bổ sung."></textarea>
    </label>

    <label>Phương thức thanh toán (Đặt cọc 30%) *
      <select name="method" id="sales-order-method" required>
        <option value="BANK_TRANSFER">Thanh toán bằng ngân hàng (Chuyển khoản VietQR SePay - MB Bank)</option>
        <option value="INSTALLMENT">Thanh toán qua nhà tài chính (Trả góp - Nhà tài chính giải ngân 70%)</option>
      </select>
    </label>

    <div id="sales-order-partner-wrap" style="display:none;margin-top:10px;">
      <label>Chọn nhà tài chính cho trả góp *
        <select name="companyId" id="sales-order-partner" style="width:100%;padding:8px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
          <option value="">-- Chọn nhà tài chính từ database --</option>
          <c:forEach var="item" items="${orderOptions.partners}">
            <option value="<c:out value='${item.id}'/>"><c:out value="${item.label}"/></option>
          </c:forEach>
        </select>
      </label>
    </div>

    <c:choose>
      <c:when test="${empty orderOptions.cars}">
        <button type="submit" disabled>Lập đơn hàng</button>
      </c:when>
      <c:otherwise>
        <button type="submit">Lập đơn hàng</button>
      </c:otherwise>
    </c:choose>
  </form>

  <script>
    const method = document.getElementById('sales-order-method');
    const partner = document.getElementById('sales-order-partner');
    const partnerWrap = document.getElementById('sales-order-partner-wrap');
    function updatePartner() {
      const isInstallment = method.value === 'INSTALLMENT';
      if (partnerWrap) partnerWrap.style.display = isInstallment ? 'block' : 'none';
      partner.required = isInstallment;
      if (!isInstallment) partner.value = '';
    }
    method.addEventListener('change', updatePartner);
    updatePartner();

    // Logic "Lấy thông tin từ người dùng" và "Bổ sung / xác minh thông tin"
    const customerSelect = document.getElementById('sales-customer-select');
    const customerName = document.getElementById('customerName');
    const customerPhone = document.getElementById('customerPhone');
    const customerEmail = document.getElementById('customerEmail');
    const customerAddress = document.getElementById('customerAddress');

    async function populateCustomerInfo(force = false) {
      const selectedOption = customerSelect.options[customerSelect.selectedIndex];
      if (!selectedOption || !selectedOption.value) {
        if (force) alert("Vui lòng chọn khách hàng trước khi lấy thông tin.");
        customerName.value = '';
        customerPhone.value = '';
        customerEmail.value = '';
        customerAddress.value = '';
        return;
      }
      customerName.value = selectedOption.dataset.name || '';
      customerPhone.value = selectedOption.dataset.phone || '';
      customerEmail.value = selectedOption.dataset.email || '';
      customerAddress.value = selectedOption.dataset.address || '';

      try {
        const resp = await fetch('<%= request.getContextPath() %>/customer/profile?mode=staff&customerId=' + selectedOption.value);
        if (resp.ok) {
          const data = await resp.json();
          if (data && data.customer) {
            customerName.value = data.customer.customerName || '';
            customerPhone.value = data.customer.phone || '';
            customerEmail.value = data.customer.email || '';
            customerAddress.value = data.customer.address || '';
          }
        }
      } catch (e) {
        console.warn("Realtime customer fetch error", e);
      }
    }

    customerSelect.addEventListener('change', () => populateCustomerInfo(false));
    document.getElementById('btn-fetch-customer').addEventListener('click', () => populateCustomerInfo(true));

    // Tự động điền nếu đã có khách hàng được chọn sẵn từ URL
    if (customerSelect.value) {
      populateCustomerInfo(false);
    }
  </script>
</main>
</body>
</html>
