<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<c:if test="${empty orderControllerLoaded}">
    <c:redirect url="/staff/transactions"/>
</c:if>
<%
    request.setAttribute("staffTitle", "Quản lý Giao dịch Bán xe");
    request.setAttribute("staffActive", "transactions");
%>
<%@ include file="../common/staff-shell.jspf" %>

<div class="page-heading">
    <div>
        <div class="eyebrow">
            <span class="eyebrow-line"></span>
            SALES ORDER &amp; TRANSACTION MANAGEMENT
        </div>
        <h1>Quản lý Giao dịch &amp; Đơn hàng Bán xe</h1>
        <p>Tạo giao dịch bán xe mới (Orders &amp; OrderDetails), phân bổ xe và cập nhật tiến độ đơn hàng.</p>
    </div>
    <div class="heading-actions">
        <button type="button" class="button" onclick="openCreateOrderModal()">
            <span class="button-icon">+</span> Tạo Giao dịch Bán xe Mới
        </button>
    </div>
</div>

<c:if test="${param.created eq 'true'}">
    <div class="alert-banner success">
        <span>✓ Khởi tạo giao dịch bán xe (Order, OrderDetail &amp; OrderStatusHistory) thành công!</span>
    </div>
</c:if>
<c:if test="${param.statusUpdated eq 'true'}">
    <div class="alert-banner success">
        <span>✓ Đã cập nhật trạng thái đơn hàng và lưu lịch sử trạng thái thành công!</span>
    </div>
</c:if>
<c:if test="${param.error eq 'true'}">
    <div class="alert-banner danger">
        <span>! Có lỗi xảy ra khi xử lý giao dịch. Vui lòng kiểm tra lại thông tin nhập.</span>
    </div>
</c:if>

<section class="metric-grid">
    <article class="metric-card">
        <div class="metric-head">
            <span>TỔNG SỐ ĐƠN HÀNG</span>
            <span class="metric-icon blue">▤</span>
        </div>
        <strong>${empty totalOrdersCount ? 0 : totalOrdersCount}</strong>
        <div class="metric-foot">Tổng giao dịch trên hệ thống</div>
    </article>
    <article class="metric-card">
        <div class="metric-head">
            <span>ĐANG XỬ LÝ / ĐẶT CỌC</span>
            <span class="metric-icon amber">◷</span>
        </div>
        <strong>${empty processingCount ? 0 : processingCount}</strong>
        <div class="metric-foot">Đang trong tiến trình hợp đồng &amp; thanh toán</div>
    </article>
    <article class="metric-card">
        <div class="metric-head">
            <span>ĐÃ HOÀN TẤT BÀN GIAO</span>
            <span class="metric-icon green">✓</span>
        </div>
        <strong>${empty completedCount ? 0 : completedCount}</strong>
        <div class="metric-foot">Đã bàn giao xe cho khách hàng</div>
    </article>
    <article class="metric-card">
        <div class="metric-head">
            <span>DOANH THU ĐƠN HÀNG</span>
            <span class="metric-icon">₫</span>
        </div>
        <strong><fmt:formatNumber value="${empty totalRevenue ? 0 : totalRevenue}" type="number" groupingUsed="true"/> ₫</strong>
        <div class="metric-foot">Tổng giá trị các đơn hàng hợp lệ</div>
    </article>
</section>

<section class="panel table-panel">
    <div class="panel-head">
        <div>
            <h2 class="panel-title">Danh sách Đơn hàng Bán xe (Orders)</h2>
            <p class="panel-subtitle">Dữ liệu kết nối trực tiếp từ bảng Orders, OrderDetails, OrderStatuses và OrderStatusHistory</p>
        </div>
    </div>

    <form class="toolbar" action="${pageContext.request.contextPath}/staff/transactions" method="GET">
        <input type="search" name="q" value="${searchKeyword}" placeholder="Tìm mã GD, tên khách hàng, SĐT, mẫu xe...">
        <select name="status">
            <option value="">-- Tất cả trạng thái đơn hàng --</option>
            <c:forEach var="st" items="${statusList}">
                <option value="${st.statusId}" ${selectedStatus eq st.statusCode || selectedStatus eq ''.concat(st.statusId) ? 'selected' : ''}>
                    ${st.description}
                </option>
            </c:forEach>
        </select>
        <button type="submit" class="button">Lọc đơn hàng</button>
        <c:if test="${not empty selectedStatus or not empty searchKeyword}">
            <a href="${pageContext.request.contextPath}/staff/transactions" class="button button-secondary">Xóa bộ lọc</a>
        </c:if>
    </form>

    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>MÃ ĐƠN / NGÀY TẠO</th>
                    <th>KHÁCH HÀNG</th>
                    <th>MẪU XE &amp; CHI TIẾT</th>
                    <th>GIÁ TRỊ ĐƠN HÀNG</th>
                    <th>TRẠNG THÁI HIỆN TẠI</th>
                    <th>CẬP NHẬT TRẠNG THÁI NHANH</th>
                    <th>CHI TIẾT</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${not empty orders}">
                        <c:forEach var="o" items="${orders}">
                            <tr>
                                <td>
                                    <span class="cell-primary">#GD-${o.id}</span>
                                    <span class="cell-secondary">
                                        <c:choose>
                                            <c:when test="${not empty o.createdAt}">
                                                <fmt:formatDate value="${o.createdAt}" pattern="dd/MM/yyyy HH:mm"/>
                                            </c:when>
                                            <c:otherwise>-</c:otherwise>
                                        </c:choose>
                                    </span>
                                </td>
                                <td>
                                    <span class="cell-primary">${empty o.customerName ? 'Khách hàng' : o.customerName}</span>
                                    <span class="cell-secondary">
                                        SĐT: ${empty o.customerPhone ? '-' : o.customerPhone}
                                        <c:if test="${not empty o.employeeName}"> · NV: ${o.employeeName}</c:if>
                                    </span>
                                </td>
                                <td>
                                    <span class="cell-primary">${empty o.vehicleName ? 'Xe VinFast' : o.vehicleName}</span>
                                    <span class="cell-secondary">
                                        Màu: ${empty o.vehicleColor ? 'Tiêu chuẩn' : o.vehicleColor}
                                        <c:if test="${not empty o.paymentMethodName}"> · ${o.paymentMethodName}</c:if>
                                    </span>
                                </td>
                                <td>
                                    <span class="cell-primary" style="color: #e31837;">
                                        <fmt:formatNumber value="${o.originalAmount}" type="number" groupingUsed="true"/> ₫
                                    </span>
                                    <span class="cell-secondary">Mã xe (CarID): #${o.carId}</span>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${o.statusId == 6}">
                                            <span class="status-pill success">${empty o.statusName ? 'Đã hoàn tất' : o.statusName}</span>
                                        </c:when>
                                        <c:when test="${o.statusId == 7}">
                                            <span class="status-pill danger">${empty o.statusName ? 'Đã hủy' : o.statusName}</span>
                                        </c:when>
                                        <c:when test="${o.statusId == 2 || o.statusId == 3}">
                                            <span class="status-pill warning">${empty o.statusName ? 'Đang xử lý' : o.statusName}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="status-pill info">${empty o.statusName ? 'Đang xử lý' : o.statusName}</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <form action="${pageContext.request.contextPath}/staff/order-status/update" method="POST" style="display: flex; align-items: center; gap: 6px;">
                                        <input type="hidden" name="orderId" value="${o.id}">
                                        <input type="hidden" name="redirectTo" value="list">
                                        <select name="statusId" style="height: 34px; padding: 0 8px; border: 1px solid #dcdde1; border-radius: 5px; font-size: 12px;">
                                            <c:forEach var="st" items="${statusList}">
                                                <option value="${st.statusId}" ${o.statusId == st.statusId ? 'selected' : ''}>
                                                    ${st.description}
                                                </option>
                                            </c:forEach>
                                        </select>
                                        <button type="submit" class="button button-secondary" style="min-height: 34px; padding: 0 10px;">Lưu</button>
                                    </form>
                                </td>
                                <td>
                                    <a href="${pageContext.request.contextPath}/staff/transaction/detail?id=${o.id}" class="button button-secondary" style="min-height: 34px;">
                                        Chi tiết →
                                    </a>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td colspan="7" class="table-empty">
                                <div class="empty-icon">▤</div>
                                <strong>Chưa có giao dịch bán xe nào trong cơ sở dữ liệu</strong>
                                <div style="margin-top: 6px;">Nhấn nút <strong>+ Tạo Giao dịch Bán xe Mới</strong> để khởi tạo đơn hàng đầu tiên.</div>
                            </td>
                        </tr>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
    <div class="table-foot">
        <span>Đang hiển thị <strong>${empty orders ? 0 : orders.size()}</strong> / <strong>${empty totalOrdersCount ? 0 : totalOrdersCount}</strong> đơn hàng</span>
        <span>Bảng dữ liệu: Orders · OrderDetails · OrderStatusHistory</span>
    </div>
</section>

<!-- Modal Khởi tạo Giao dịch Bán xe Mới -->
<div class="staff-modal-backdrop" id="createOrderModal">
    <div class="staff-modal">
        <div class="staff-modal-head">
            <h3>Khởi tạo Giao dịch Bán xe Mới (Order &amp; OrderDetail)</h3>
            <button type="button" class="button button-secondary" onclick="closeCreateOrderModal()" style="min-height: 30px; padding: 0 10px;">✕</button>
        </div>
        <form action="${pageContext.request.contextPath}/order/create" method="POST">
            <div class="staff-modal-body">
                <input type="hidden" name="customerId" value="${param.customerId}">
                <div class="staff-form-grid">
                    <div class="staff-field">
                        <label>Họ và tên khách hàng *</label>
                        <input type="text" name="customerName" value="${param.customerName}" placeholder="Nhập họ tên khách hàng" required>
                    </div>
                    <div class="staff-field">
                        <label>Số điện thoại khách hàng *</label>
                        <input type="tel" name="customerPhone" value="${param.customerPhone}" placeholder="090 xxx xxxx" required>
                    </div>
                    <div class="staff-field">
                        <label>Email khách hàng</label>
                        <input type="email" name="customerEmail" value="${param.customerEmail}" placeholder="khachhang@email.com">
                    </div>
                    <div class="staff-field">
                        <label>Nhân viên phụ trách</label>
                        <select name="employeeId">
                            <c:choose>
                                <c:when test="${not empty employees}">
                                    <c:forEach var="emp" items="${employees}">
                                        <option value="${emp.employeeId}">${emp.employeeName}</option>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <option value="">Tự động gán nhân viên kinh doanh</option>
                                </c:otherwise>
                            </c:choose>
                        </select>
                    </div>

                    <div class="staff-field" style="grid-column: span 2;">
                        <label>Chọn mẫu xe / Xe trong kho *</label>
                        <select name="carId" id="carSelect" onchange="syncPriceFromCar()" required>
                            <option value="">-- Chọn xe hoặc mẫu xe VinFast --</option>
                            <c:if test="${not empty availableCars}">
                                <optgroup label="Xe có sẵn trong kho (Cars)">
                                    <c:forEach var="car" items="${availableCars}">
                                        <option value="${car.carId}" data-price="${car.price}" data-model="${car.modelName}" data-color="${car.colorName}">
                                            [Kho #${car.carId}] ${car.modelName} - Màu ${car.colorName} (<fmt:formatNumber value="${car.price}" type="number" groupingUsed="true"/> ₫)
                                        </option>
                                    </c:forEach>
                                </optgroup>
                            </c:if>
                            <optgroup label="Chọn theo dòng xe (Tự động phân bổ xe mới từ CarModels)">
                                <c:forEach var="cm" items="${carModelsWithPrices}">
                                    <option value="${cm.modelId}" data-price="${cm.price}" data-model="${cm.modelName}" ${param.modelId eq ''.concat(cm.modelId) ? 'selected' : ''}>
                                        ${cm.modelName} — Giá niêm yết: <fmt:formatNumber value="${cm.price}" type="number" groupingUsed="true"/> ₫
                                    </option>
                                </c:forEach>
                            </optgroup>
                        </select>
                        <input type="hidden" name="vehicleName" id="vehicleNameInput" value="">
                    </div>

                    <div class="staff-field">
                        <label>Màu ngoại thất</label>
                        <select name="vehicleColor" id="vehicleColorSelect">
                            <option value="Trắng">Trắng Pearl</option>
                            <option value="Đen">Đen Jet Black</option>
                            <option value="Đỏ">Đỏ Crimson Red</option>
                            <option value="Xanh">Xanh VinFast Blue</option>
                            <option value="Xám">Xám Neptune Grey</option>
                        </select>
                    </div>

                    <div class="staff-field">
                        <label>Phương thức thanh toán *</label>
                        <select name="paymentMethodId" required>
                            <c:choose>
                                <c:when test="${not empty paymentMethods}">
                                    <c:forEach var="pm" items="${paymentMethods}">
                                        <option value="${pm.paymentMethodId}">${pm.methodName}</option>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <option value="1">Thanh toán trả thẳng</option>
                                    <option value="2">Mua xe trả góp</option>
                                    <option value="3">Chuyển khoản ngân hàng</option>
                                </c:otherwise>
                            </c:choose>
                        </select>
                    </div>

                    <div class="staff-field">
                        <label>Đơn giá giao dịch (VNĐ) *</label>
                        <input type="number" name="amount" id="orderAmountInput" step="1000" min="1000000" placeholder="Ví dụ: 850000000" required>
                    </div>

                    <div class="staff-field">
                        <label>Trạng thái khởi tạo *</label>
                        <select name="statusId" required>
                            <c:forEach var="st" items="${statusList}">
                                <option value="${st.statusId}" ${st.statusId == 1 ? 'selected' : ''}>
                                    ${st.description}
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                </div>
            </div>
            <div class="staff-modal-foot">
                <button type="button" class="button button-secondary" onclick="closeCreateOrderModal()">Hủy bỏ</button>
                <button type="submit" class="button">Lưu &amp; Tạo Đơn hàng</button>
            </div>
        </form>
    </div>
</div>

<script>
    function openCreateOrderModal() {
        document.getElementById('createOrderModal').classList.add('open');
        syncPriceFromCar();
    }
    function closeCreateOrderModal() {
        document.getElementById('createOrderModal').classList.remove('open');
    }
    function syncPriceFromCar() {
        const select = document.getElementById('carSelect');
        if (!select || select.selectedIndex < 0) return;
        const opt = select.options[select.selectedIndex];
        const price = opt.getAttribute('data-price');
        const model = opt.getAttribute('data-model');
        const color = opt.getAttribute('data-color');
        if (price) {
            document.getElementById('orderAmountInput').value = Math.round(parseFloat(price));
        }
        if (model) {
            document.getElementById('vehicleNameInput').value = model;
        }
        if (color) {
            const colorSel = document.getElementById('vehicleColorSelect');
            for (let i = 0; i < colorSel.options.length; i++) {
                if (colorSel.options[i].value === color) {
                    colorSel.selectedIndex = i;
                    break;
                }
            }
        }
    }
    <c:if test="${not empty param.createFromConsult}">
    window.addEventListener('DOMContentLoaded', function () {
        openCreateOrderModal();
    });
    </c:if>
</script>

<%@ include file="../common/staff-shell-end.jspf" %>
