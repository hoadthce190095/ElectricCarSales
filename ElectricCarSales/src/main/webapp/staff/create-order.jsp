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
    .kyc-field input, .kyc-field textarea, .kyc-field select { width:100%; box-sizing:border-box; padding:8px 10px; border:1px solid #cbd5e1; border-radius:6px; font-size:13px; font-family:inherit; }
    .kyc-field input:focus, .kyc-field textarea:focus, .kyc-field select:focus { outline:none; border-color:#0284c7; box-shadow:0 0 0 2px rgba(2,132,199,.2); }
    .kyc-hint { font-size:12px; color:#64748b; margin:0 0 12px; line-height:1.5; }
    .vehicle-select-box { background:#f8fafc; border:1px solid #cbd5e1; border-radius:8px; padding:16px; margin:16px 0; }
    .vehicle-select-grid { display:grid; grid-template-columns:repeat(auto-fit, minmax(200px, 1fr)); gap:12px; }
    .vehicle-price-box { margin-top:12px; background:#ffffff; border:1px solid #e2e8f0; border-radius:8px; padding:14px 16px; }
    .vehicle-price-grid { display:grid; grid-template-columns:repeat(auto-fit, minmax(170px, 1fr)); gap:10px; font-size:13px; }
  </style>
</head>
<body class="finance-review">
<main>
  <h1>Lập đơn hàng bán xe</h1>
  <p><a href="<%= request.getContextPath() %>/staff/dashboard">← Trang nhân viên</a></p>
  <p>Hệ thống tự động đối chiếu tồn kho xe vật lý AVAILABLE, áp dụng khuyến mãi hiệu lực (nếu có), tính tiền cọc 30% và số tiền 70% còn lại.</p>

  <c:if test="${not empty param.created}">
    <p role="status" style="padding:12px;border:1px solid #86efac;background:#f0fdf4;color:#166534;border-radius:7px;">
      ✓ Đã tạo đơn #<c:out value="${param.created}"/>. Khách hàng có thể mở trang Đơn hàng cá nhân để xem và thanh toán cọc 30%.
    </p>
  </c:if>

  <%-- Banner khi lập đơn từ Yêu cầu tư vấn (Sales Chat) --%>
  <c:if test="${not empty consultationRequest}">
    <div style="margin-bottom:16px;padding:14px 16px;background:#eff6ff;border:1px solid #bfdbfe;border-radius:8px;color:#1e3a8a;">
      <div style="font-weight:700;font-size:14px;margin-bottom:4px;display:flex;align-items:center;gap:6px;">
        <span>⚡ Đang lập đơn hàng từ Yêu cầu tư vấn #${consultationRequest.id}</span>
      </div>
      <div style="font-size:13px;color:#334155;line-height:1.6;">
        Khách hàng: <strong><c:out value="${consultationRequest.customerName}"/></strong>
        &nbsp;|&nbsp;
        Mẫu xe quan tâm: <strong><c:out value="${consultationRequest.modelName}" default="—"/></strong>
        <c:if test="${not empty selectedVersionName}"> · Bản: <strong><c:out value="${selectedVersionName}"/></strong></c:if>
        <c:if test="${not empty selectedColorName}"> · Màu: <strong><c:out value="${selectedColorName}"/></strong></c:if>
        <c:if test="${not empty selectedPaymentPref}"> · Phương án: <strong><c:out value="${selectedPaymentPref}"/></strong></c:if>
      </div>
      <div style="font-size:12px;color:#64748b;margin-top:4px;">
        Hệ thống tự động điền thông tin người mua và các lựa chọn xe. Sales Staff có thể kiểm tra và đổi xe khác theo thỏa thuận với khách.
      </div>
    </div>
  </c:if>

  <c:if test="${empty orderOptions.cars}">
    <p role="status" style="padding:12px;border:1px solid #f5c16c;background:#fffbeb;color:#854d0e;border-radius:7px;">
      Chưa có xe vật lý khả dụng trong kho (Cars với trạng thái AVAILABLE chưa gắn đơn). Operations cần nhập xe thực tế trước khi Sales lập đơn.
    </p>
  </c:if>

  <form class="finance-panel" method="post" action="<%= request.getContextPath() %>/orders" id="sales-order-form">
    <input type="hidden" name="csrfToken" value="<c:out value='${paymentCsrf}'/>">

    <label>Khách hàng *
      <select name="customerId" id="sales-customer-select" required style="width:100%;padding:8px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
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

    <%-- KHỐI CHỌN XE PHỤ THUỘC (MẪU XE -> PHIÊN BẢN -> MÀU SẮC -> XE VẬT LÝ / VIN) --%>
    <div class="vehicle-select-box">
      <div style="font-size:14px;font-weight:700;color:#0f172a;margin-bottom:6px;">
        🚗 Lựa chọn xe bán (Phụ thuộc: Mẫu xe → Phiên bản → Màu sắc → Xe vật lý &amp; VIN)
      </div>
      <p style="font-size:12px;color:#64748b;margin:0 0 14px;line-height:1.5;">
        Chỉ hiển thị các mẫu xe, phiên bản và màu sắc thực tế đang có xe vật lý AVAILABLE trong kho và chưa được gán vào đơn hàng khác.
      </p>

      <div class="vehicle-select-grid">
        <div>
          <label style="font-size:12px;color:#475569;display:block;margin-bottom:4px;font-weight:600;">
            1. Chọn Mẫu xe *
          </label>
          <select id="sales-model-select" style="width:100%;box-sizing:border-box;padding:8px 10px;border:1px solid #cbd5e1;border-radius:6px;font-size:13px;" required>
            <option value="">-- Chọn mẫu xe --</option>
          </select>
        </div>

        <div>
          <label style="font-size:12px;color:#475569;display:block;margin-bottom:4px;font-weight:600;">
            2. Chọn Phiên bản xe *
          </label>
          <select id="sales-version-select" style="width:100%;box-sizing:border-box;padding:8px 10px;border:1px solid #cbd5e1;border-radius:6px;font-size:13px;" required>
            <option value="">-- Chọn phiên bản --</option>
          </select>
        </div>

        <div>
          <label style="font-size:12px;color:#475569;display:block;margin-bottom:4px;font-weight:600;">
            3. Chọn Màu sắc xe *
          </label>
          <select id="sales-color-select" style="width:100%;box-sizing:border-box;padding:8px 10px;border:1px solid #cbd5e1;border-radius:6px;font-size:13px;" required>
            <option value="">-- Chọn màu xe --</option>
          </select>
        </div>

        <div>
          <label style="font-size:12px;color:#475569;display:block;margin-bottom:4px;font-weight:600;">
            4. Chọn Xe vật lý / VIN *
          </label>
          <select name="carId" id="sales-car-select" style="width:100%;box-sizing:border-box;padding:8px 10px;border:1px solid #cbd5e1;border-radius:6px;font-size:13px;" required>
            <option value="">-- Chọn xe &amp; VIN --</option>
          </select>
        </div>
      </div>

      <div id="sales-car-stock-alert" style="display:none;margin-top:12px;padding:10px 14px;background:#fffbeb;border:1px solid #fde68a;border-radius:6px;font-size:13px;color:#92400e;"></div>

      <%-- Hộp hiển thị chi tiết giá xe, khuyến mãi, cọc 30% và còn lại 70% --%>
      <div id="sales-price-calc-box" class="vehicle-price-box" style="display:none;">
        <div style="font-size:13px;font-weight:700;color:#1e3a8a;margin-bottom:8px;" id="calc-car-label">Chi tiết giá niêm yết và đặt cọc xe (Giá cố định theo hệ thống)</div>
        <div class="vehicle-price-grid">
          <div><span style="color:#64748b;">Giá niêm yết (cố định):</span> <strong id="calc-orig-price">—</strong></div>
          <div id="calc-promo-wrap" style="display:none;"><span style="color:#64748b;">Chương trình giảm giá:</span> <strong style="color:#b91c1c;" id="calc-promo-name">—</strong></div>
          <div><span style="color:#64748b;" id="calc-sale-price-label">Giá bán thực tế:</span> <strong style="color:#15803d;font-size:14px;" id="calc-sale-price">—</strong></div>
          <div><span style="color:#64748b;">Khách thanh toán cọc 30%:</span> <strong style="color:#d97706;font-size:14px;" id="calc-deposit-price">—</strong></div>
          <div><span style="color:#64748b;">Còn lại 70%:</span> <strong style="color:#2563eb;font-size:14px;" id="calc-remaining-price">—</strong></div>
        </div>
      </div>
    </div>

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
        <button type="submit" disabled>Kho chưa có xe khả dụng</button>
      </c:when>
      <c:otherwise>
        <button type="submit" id="btn-submit-order">Lập đơn hàng (Cọc 30% / Còn 70%) →</button>
      </c:otherwise>
    </c:choose>
  </form>

  <script>
    // 1. Dữ liệu xe AVAILABLE từ database
    const availableCars = [
      <c:forEach var="item" items="${orderOptions.cars}" varStatus="loop">
        {
          id: ${item.id},
          modelId: ${item.modelId},
          modelName: "<c:out value='${item.modelName}'/>",
          versionId: ${item.versionId},
          versionName: "<c:out value='${item.versionName}'/>",
          colorId: ${item.colorId},
          colorName: "<c:out value='${item.colorName}'/>",
          vin: "<c:out value='${item.vin}'/>",
          originalPrice: ${item.originalPrice},
          salePrice: ${item.salePrice},
          discountAmount: ${item.discountAmount},
          discountType: "<c:out value='${item.discountType}'/>",
          discountValue: ${item.discountValue != null ? item.discountValue : 0},
          promotionName: "<c:out value='${item.promotionName}'/>",
          label: "<c:out value='${item.label}'/>"
        }<c:if test="${!loop.last}">,</c:if>
      </c:forEach>
    ];

    const prefillModelId = "<c:out value='${selectedModelId}'/>";
    const prefillVersionName = "<c:out value='${selectedVersionName}'/>";
    const prefillColorName = "<c:out value='${selectedColorName}'/>";
    const prefillPaymentPref = "<c:out value='${selectedPaymentPref}'/>";

    const modelSelect = document.getElementById('sales-model-select');
    const versionSelect = document.getElementById('sales-version-select');
    const colorSelect = document.getElementById('sales-color-select');
    const carSelect = document.getElementById('sales-car-select');
    const stockAlert = document.getElementById('sales-car-stock-alert');
    const priceBox = document.getElementById('sales-price-calc-box');

    const fmtVnd = (num) => new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(Math.round(num)) + ' VND';

    // 2. Khởi tạo danh sách Mẫu xe
    function initModelOptions() {
      const modelMap = new Map();
      availableCars.forEach(car => {
        if (!modelMap.has(car.modelId)) {
          modelMap.set(car.modelId, { id: car.modelId, name: car.modelName, count: 0 });
        }
        modelMap.get(car.modelId).count++;
      });

      if (modelMap.size === 0) {
        modelSelect.innerHTML = '<option value="">Kho chưa có mẫu xe khả dụng</option>';
        versionSelect.innerHTML = '<option value="">--</option>';
        colorSelect.innerHTML = '<option value="">--</option>';
        carSelect.innerHTML = '<option value="">--</option>';
        return;
      }

      modelSelect.innerHTML = '<option value="">-- Chọn mẫu xe có sẵn --</option>' +
        Array.from(modelMap.values()).map(m => 
          `<option value="${m.id}" data-name="${m.name}">${m.name} (${m.count} xe có sẵn)</option>`
        ).join('');
    }

    // 3. Cập nhật Phiên bản theo Mẫu xe
    function updateVersionOptions() {
      const selectedModelId = Number(modelSelect.value || 0);
      stockAlert.style.display = 'none';
      priceBox.style.display = 'none';

      if (!selectedModelId) {
        versionSelect.innerHTML = '<option value="">-- Chọn phiên bản --</option>';
        colorSelect.innerHTML = '<option value="">-- Chọn màu xe --</option>';
        carSelect.innerHTML = '<option value="">-- Chọn xe &amp; VIN --</option>';
        return;
      }

      const versionMap = new Map();
      availableCars.filter(c => c.modelId === selectedModelId).forEach(car => {
        if (!versionMap.has(car.versionId)) {
          versionMap.set(car.versionId, { id: car.versionId, name: car.versionName, count: 0 });
        }
        versionMap.get(car.versionId).count++;
      });

      versionSelect.innerHTML = '<option value="">-- Chọn phiên bản --</option>' +
        Array.from(versionMap.values()).map(v => 
          `<option value="${v.id}" data-name="${v.name}">${v.name} (${v.count} xe có sẵn)</option>`
        ).join('');

      // Tự động chọn nếu chỉ có 1 phiên bản hoặc khớp prefill
      if (versionMap.size === 1) {
        versionSelect.selectedIndex = 1;
      } else if (prefillVersionName) {
        for (let i = 1; i < versionSelect.options.length; i++) {
          if (versionSelect.options[i].dataset.name.toLowerCase().includes(prefillVersionName.toLowerCase())) {
            versionSelect.selectedIndex = i;
            break;
          }
        }
      }

      updateColorOptions();
    }

    // 4. Cập nhật Màu sắc theo Mẫu + Phiên bản
    function updateColorOptions() {
      const selectedModelId = Number(modelSelect.value || 0);
      const selectedVersionId = Number(versionSelect.value || 0);
      priceBox.style.display = 'none';

      if (!selectedModelId || !selectedVersionId) {
        colorSelect.innerHTML = '<option value="">-- Chọn màu xe --</option>';
        carSelect.innerHTML = '<option value="">-- Chọn xe &amp; VIN --</option>';
        return;
      }

      const colorMap = new Map();
      availableCars.filter(c => c.modelId === selectedModelId && c.versionId === selectedVersionId).forEach(car => {
        if (!colorMap.has(car.colorId)) {
          colorMap.set(car.colorId, { id: car.colorId, name: car.colorName, count: 0 });
        }
        colorMap.get(car.colorId).count++;
      });

      colorSelect.innerHTML = '<option value="">-- Chọn màu xe --</option>' +
        Array.from(colorMap.values()).map(col => 
          `<option value="${col.id}" data-name="${col.name}">${col.name} (${col.count} xe có sẵn)</option>`
        ).join('');

      // Tự động chọn nếu chỉ có 1 màu hoặc khớp prefill
      if (colorMap.size === 1) {
        colorSelect.selectedIndex = 1;
      } else if (prefillColorName) {
        for (let i = 1; i < colorSelect.options.length; i++) {
          if (colorSelect.options[i].dataset.name.toLowerCase().includes(prefillColorName.toLowerCase())) {
            colorSelect.selectedIndex = i;
            break;
          }
        }
      }

      updateCarOptions();
    }

    // 5. Cập nhật Xe vật lý (VIN & CarID) theo Mẫu + Phiên bản + Màu
    function updateCarOptions() {
      const selectedModelId = Number(modelSelect.value || 0);
      const selectedVersionId = Number(versionSelect.value || 0);
      const selectedColorId = Number(colorSelect.value || 0);

      if (!selectedModelId || !selectedVersionId || !selectedColorId) {
        carSelect.innerHTML = '<option value="">-- Chọn xe &amp; VIN --</option>';
        priceBox.style.display = 'none';
        return;
      }

      const matchingCars = availableCars.filter(c => 
        c.modelId === selectedModelId && 
        c.versionId === selectedVersionId && 
        c.colorId === selectedColorId
      );

      if (matchingCars.length === 0) {
        carSelect.innerHTML = '<option value="">Hết hàng cho lựa chọn này</option>';
        stockAlert.style.display = 'block';
        stockAlert.innerHTML = '⚠ <strong>Hết hàng:</strong> Không còn xe vật lý khả dụng cho cấu hình đã chọn. Vui lòng chọn màu hoặc phiên bản khác.';
        priceBox.style.display = 'none';
        return;
      }

      stockAlert.style.display = 'none';
      carSelect.innerHTML = matchingCars.map((car, idx) => 
        `<option value="${car.id}" data-idx="${idx}" ${idx === 0 ? 'selected' : ''}>VIN: ${car.vin} (Xe #${car.id})</option>`
      ).join('');

      updatePriceCalculation();
    }

    // 6. Cập nhật bảng tính giá tự động
    function updatePriceCalculation() {
      const selectedCarId = Number(carSelect.value || 0);
      const car = availableCars.find(c => c.id === selectedCarId);

      if (!car) {
        priceBox.style.display = 'none';
        return;
      }

      priceBox.style.display = 'block';
      document.getElementById('calc-car-label').textContent = 
        `${car.modelName} (${car.versionName} - ${car.colorName}) · VIN: ${car.vin}`;

      document.getElementById('calc-orig-price').textContent = fmtVnd(car.originalPrice);
      
      const promoWrap = document.getElementById('calc-promo-wrap');
      const salePriceLabel = document.getElementById('calc-sale-price-label');
      if (car.discountAmount > 0 && car.promotionName) {
        promoWrap.style.display = 'block';
        const discountDesc = String(car.discountType || '').toUpperCase() === 'PERCENTAGE' && Number(car.discountValue) > 0
          ? `Giảm \${car.discountValue}% (-\${fmtVnd(car.discountAmount)})`
          : `-\${fmtVnd(car.discountAmount)}`;
        document.getElementById('calc-promo-name').textContent = 
          `\${car.promotionName} · \${discountDesc}`;
        if (salePriceLabel) salePriceLabel.textContent = 'Giá sau ưu đãi:';
      } else {
        promoWrap.style.display = 'none';
        if (salePriceLabel) salePriceLabel.textContent = 'Giá bán niêm yết:';
      }

      const salePrice = car.salePrice;
      const deposit = Math.round(salePrice * 0.3);
      const remaining = salePrice - deposit;

      document.getElementById('calc-sale-price').textContent = fmtVnd(salePrice);
      document.getElementById('calc-deposit-price').textContent = fmtVnd(deposit);
      document.getElementById('calc-remaining-price').textContent = fmtVnd(remaining);
    }

    modelSelect.addEventListener('change', updateVersionOptions);
    versionSelect.addEventListener('change', updateColorOptions);
    colorSelect.addEventListener('change', updateCarOptions);
    carSelect.addEventListener('change', updatePriceCalculation);

    // Khởi tạo và prefill từ Consultation Request
    initModelOptions();

    if (prefillModelId) {
      for (let i = 0; i < modelSelect.options.length; i++) {
        if (modelSelect.options[i].value === prefillModelId) {
          modelSelect.selectedIndex = i;
          updateVersionOptions();
          break;
        }
      }
    }

    if (prefillPaymentPref && prefillPaymentPref.toLowerCase().includes('trả góp')) {
      const methodSelect = document.getElementById('sales-order-method');
      if (methodSelect) {
        methodSelect.value = 'INSTALLMENT';
      }
    }

    // Phương thức thanh toán & Đối tác tài chính
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

    // Tự động điền KYC khách hàng
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
            customerName.value = data.customer.customerName || customerName.value;
            customerPhone.value = data.customer.phone || customerPhone.value;
            customerEmail.value = data.customer.email || customerEmail.value;
            customerAddress.value = data.customer.address || customerAddress.value;
          }
        }
      } catch (e) {
        console.warn("Realtime customer fetch error", e);
      }
    }

    customerSelect.addEventListener('change', () => populateCustomerInfo(false));
    document.getElementById('btn-fetch-customer').addEventListener('click', () => populateCustomerInfo(true));

    if (customerSelect.value) {
      populateCustomerInfo(false);
    }
  </script>
</main>
</body>
</html>
