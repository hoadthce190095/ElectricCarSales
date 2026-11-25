<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<% request.setAttribute("customerTitle", "Đơn hàng của tôi"); request.setAttribute("customerEyebrow", "GIAO DỊCH CỦA BẠN"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="page-intro split-intro">
    <p>Theo dõi đơn hàng, quản lý thanh toán đặt cọc 30% và thanh toán 70% còn lại.</p>
    <span class="soft-badge">
        <i></i> Dữ liệu cá nhân</span>
</div>
<section class="panel table-panel">
    <div class="panel-heading">
        <div>
            <span class="section-overline">LỊCH SỬ GIAO DỊCH</span>
            <h2>Tất cả đơn hàng</h2>
        </div>
        <label class="search-field">
            <span>⌕</span>
            <input type="search" placeholder="Tìm mã đơn hàng" aria-label="Tìm mã đơn hàng">
        </label>
    </div>
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>MÃ ĐƠN HÀNG</th>
                    <th>NGÀY TẠO</th>
                    <th>MẪU XE</th>
                    <th>TỔNG GIÁ TRỊ</th>
                    <th>CỌC 30%</th>
                    <th>CÒN LẠI 70%</th>
                    <th>ĐÃ THANH TOÁN</th>
                    <th>TRẠNG THÁI</th>
                    <th>THAO TÁC</th>
                </tr>
            </thead>
            <tbody>
                <c:set var="orderList" value="${not empty orders ? orders : customerOrders}"/>
                <c:forEach var="order" items="${orderList}">
                    <tr>
                        <td><strong><c:out value="${order.orderCode}"/></strong><br><small style="color:#64748b;">#<c:out value="${order.id}"/></small></td>
                        <td><c:out value="${order.orderDate}"/></td>
                        <td><c:out value="${order.vehicleName}" default="—"/></td>
                        <td><strong><c:out value="${order.formattedOriginalAmount}"/></strong></td>
                        <td><c:out value="${order.formattedDepositAmount}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${order.installment and not empty order.remainingAmount and order.remainingAmount le 0}">
                                    <span style="color:#15803d;font-weight:700;">0 ₫</span>
                                    <div style="font-size:11px;color:#0369a1;font-weight:600;">(Đã giải ngân 70%)</div>
                                </c:when>
                                <c:otherwise>
                                    <span style="color:#b91c1c;font-weight:700;"><c:out value="${order.formattedRemainingAmount}"/></span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${order.installment and not empty order.remainingAmount and order.remainingAmount le 0}">
                                    <span style="color:#15803d;font-weight:700;"><c:out value="${order.formattedOriginalAmount}"/></span>
                                </c:when>
                                <c:otherwise>
                                    <span style="color:#15803d;font-weight:700;"><c:out value="${order.formattedPaidAmount}"/></span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${order.status eq 'PAID' or order.status eq 'COMPLETED'}">
                                    <span class="status-badge badge-green"><span class="dot"></span> <c:out value="${order.status}"/></span>
                                </c:when>
                                <c:when test="${order.status eq 'DEPOSITED'}">
                                    <span class="status-badge badge-blue"><span class="dot"></span> <c:out value="${order.status}"/></span>
                                </c:when>
                                <c:when test="${order.status eq 'PROCESSING'}">
                                    <span class="status-badge badge-amber"><span class="dot"></span> <c:out value="${order.status}"/></span>
                                </c:when>
                                <c:when test="${order.status eq 'CANCELLED'}">
                                    <span class="status-badge badge-red"><span class="dot"></span> <c:out value="${order.status}"/></span>
                                </c:when>
                                <c:otherwise>
                                    <span class="status-badge badge-amber"><span class="dot"></span> <c:out value="${order.status}"/></span>
                                </c:otherwise>
                            </c:choose>
                            <br><small>PT: <strong><c:out value="${order.paymentMethodName}"/></strong></small>
                            <c:if test="${not empty order.companyName}">
                                <br><small style="color:#0284c7;font-weight:600;">Đối tác: <c:out value="${order.companyName}"/></small>
                            </c:if>
                            <c:if test="${not empty order.contractStatus}">
                                <br><small>HĐ: 
                                    <c:choose>
                                        <c:when test="${order.contractStatus eq 'SIGNED'}">
                                            <span style="color:#15803d;font-weight:700;">SIGNED</span>
                                        </c:when>
                                        <c:when test="${order.contractStatus eq 'CANCELLED'}">
                                            <span style="color:#b91c1c;font-weight:700;">CANCELLED</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span style="color:#b45309;font-weight:700;"><c:out value="${order.contractStatus}"/></span>
                                        </c:otherwise>
                                    </c:choose>
                                </small>
                            </c:if>
                        </td>
                        <td>
                            <div style="margin-bottom:6px;font-size:12px;">
                                <a href="<%= request.getContextPath() %>/payment/detail?orderId=<c:out value='${order.id}'/>">Chi tiết</a>
                                <c:if test="${order.status ne 'CANCELLED'}">
                                    | <a href="<%= request.getContextPath() %>/payment/history?orderId=<c:out value='${order.id}'/>">Thanh toán</a>
                                </c:if>
                                <c:if test="${order.status eq 'DEPOSITED' or order.status eq 'PROCESSING' or order.status eq 'PAID' or order.status eq 'COMPLETED'}">
                                    | <a href="<%= request.getContextPath() %>/customer/sales-document?kind=deposit&amp;orderId=<c:out value='${order.id}'/>&amp;view=html">Biên nhận</a>
                                </c:if>
                                <c:if test="${order.contractStatus eq 'PENDING_SIGNATURE' and order.status ne 'CANCELLED'}">
                                    | <a href="<%= request.getContextPath() %>/contract" style="color:#b45309;font-weight:700;">✍️ Ký HĐ</a>
                                </c:if>
                                <c:if test="${order.contractStatus eq 'SIGNED'}">
                                    | <a href="<%= request.getContextPath() %>/customer/sales-document?kind=contract&amp;orderId=<c:out value='${order.id}'/>&amp;view=html">Hợp đồng</a>
                                </c:if>
                                <c:if test="${order.status eq 'PAID' or order.status eq 'COMPLETED'}">
                                    | <a href="<%= request.getContextPath() %>/payment/history?orderId=<c:out value='${order.id}'/>#invoices" style="color:#15803d;font-weight:600;">Hóa đơn</a>
                                </c:if>
                            </div>

                            <%-- NÚT THANH TOÁN THEO HÌNH THỨC VÀ TIẾN ĐỘ --%>
                            <c:choose>
                                <%-- 0. ĐƠN ĐÃ HỦY: KHÔNG THỂ THANH TOÁN MỚI --%>
                                <c:when test="${order.status eq 'CANCELLED'}">
                                    <div style="color:#991b1b;font-size:12px;font-weight:700;">Đơn hàng đã hủy</div>
                                    <c:if test="${not empty order.cancelReason}">
                                        <div style="color:#7f1d1d;font-size:11.5px;margin-top:2px;">Lý do: <c:out value="${order.cancelReason}"/></div>
                                    </c:if>
                                </c:when>

                                <%-- 1. HÌNH THỨC TRẢ GÓP (INSTALLMENT) --%>
                                <c:when test="${order.installment}">
                                    <c:choose>
                                        <%-- Chưa cọc 30% --%>
                                        <c:when test="${not order.depositPaid and (order.status eq 'PENDING' or order.status eq 'RESERVED')}">
                                            <button type="button" class="button button-primary btn-open-payment-modal"
                                                    data-order-id="${order.id}"
                                                    data-order-code="${order.orderCode}"
                                                    data-amount="${order.depositAmount}"
                                                    data-formatted-amount="${order.formattedDepositAmount}"
                                                    data-type="DEPOSIT"
                                                    data-is-installment="true"
                                                    data-partner="<c:out value='${order.companyName}' default='Đối tác tài chính'/>"
                                                    data-vehicle="${order.vehicleName}">Thanh toán cọc 30%</button>
                                            <button type="button" class="button button-secondary btn-customer-cancel-order"
                                                    data-order-id="${order.id}"
                                                    data-order-code="${order.orderCode}"
                                                    data-vehicle="${order.vehicleName}"
                                                    data-amount="${order.formattedOriginalAmount}"
                                                    style="margin-left:6px;color:#dc2626;border-color:#fca5a5;font-size:12px;padding:6px 12px;">Hủy đơn hàng</button>
                                        </c:when>
                                        <c:when test="${order.remainingAmount gt 0}">
                                            <div style="font-size:12px;color:#92400e;background:#fffbeb;border:1px solid #fde68a;border-radius:6px;padding:6px 8px;line-height:1.4;">
                                                ✓ Đã xác nhận cọc 30%. Kế toán cần đối soát khoản cọc và khách hàng ký hợp đồng trước khi đối tác <strong><c:out value="${order.companyName}" default="Ngân hàng đối tác"/></strong> giải ngân 70%.
                                                <a href="<%= request.getContextPath() %>/contract" style="font-weight:700;text-decoration:underline;color:#92400e;">Xem hợp đồng</a>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div style="font-size:12px;color:#166534;background:#f0fdf4;border:1px solid #bbf7d0;border-radius:6px;padding:6px 8px;line-height:1.4;">
                                                ✓ Đã thanh toán cọc 30%. Đối tác tài chính <strong><c:out value="${order.companyName}" default="Ngân hàng đối tác"/></strong> đã giải ngân 70% sau khi hợp đồng được ký.
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </c:when>

                                <%-- 2. HÌNH THỨC TRẢ THẲNG (FULL PAYMENT) --%>
                                <c:otherwise>
                                    <c:choose>
                                        <%-- Bước 3: Chưa cọc 30% --%>
                                        <c:when test="${not order.depositPaid and (order.status eq 'PENDING' or order.status eq 'RESERVED')}">
                                            <button type="button" class="button button-primary btn-open-payment-modal"
                                                    data-order-id="${order.id}"
                                                    data-order-code="${order.orderCode}"
                                                    data-amount="${order.depositAmount}"
                                                    data-formatted-amount="${order.formattedDepositAmount}"
                                                    data-type="DEPOSIT"
                                                    data-vehicle="${order.vehicleName}">Thanh toán cọc 30%</button>
                                            <button type="button" class="button button-secondary btn-customer-cancel-order"
                                                    data-order-id="${order.id}"
                                                    data-order-code="${order.orderCode}"
                                                    data-vehicle="${order.vehicleName}"
                                                    data-amount="${order.formattedOriginalAmount}"
                                                    style="margin-left:6px;color:#dc2626;border-color:#fca5a5;font-size:12px;padding:6px 12px;">Hủy đơn hàng</button>
                                        </c:when>

                                        <%-- Bước 4 & 5: Đã cọc 30%, còn nợ 70% --%>
                                        <c:when test="${(order.status eq 'DEPOSITED' or order.status eq 'PROCESSING') and order.remainingAmount gt 0}">
                                            <c:choose>
                                                <c:when test="${order.contractStatus eq 'SIGNED'}">
                                                    <button type="button" class="button button-primary btn-open-payment-modal"
                                                            style="background:#0284c7;"
                                                            data-order-id="${order.id}"
                                                            data-order-code="${order.orderCode}"
                                                            data-amount="${order.remainingAmount}"
                                                            data-formatted-amount="${order.formattedRemainingAmount}"
                                                            data-type="REMAINING"
                                                            data-vehicle="${order.vehicleName}">Thanh toán số tiền còn lại (70%)</button>
                                                </c:when>
                                                <c:otherwise>
                                                    <div style="font-size:12px;color:#b45309;background:#fffbeb;border:1px solid #fde68a;border-radius:6px;padding:6px 8px;line-height:1.4;">
                                                        ⏳ Đã cọc 30%. Vui lòng <a href="<%= request.getContextPath() %>/contract" style="font-weight:700;text-decoration:underline;color:#b45309;">Ký hợp đồng bằng OTP</a> để mở chức năng thanh toán 70% còn lại.
                                                    </div>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:when>

                                        <%-- Bước 9: Đã hoàn tất 100% --%>
                                        <c:when test="${order.remainingAmount le 0 or order.status eq 'PAID' or order.status eq 'COMPLETED'}">
                                            <span style="color:#166534;font-weight:700;font-size:12px;">✓ Đã thanh toán đủ 100% (Hoàn tất)</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span style="color:#64748b;font-size:12px;">—</span>
                                        </c:otherwise>
                                    </c:choose>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty orderList}">
                    <tr><td colspan="9"><div class="table-empty"><strong>Chưa có giao dịch</strong>
                        <p>Đơn hàng được Sales Staff lập sẽ xuất hiện tại đây.</p></div></td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
    <div class="table-foot">
        <span>Đang hiển thị <strong><c:out value="${not empty orderCount ? orderCount : (not empty orderList ? orderList.size() : 0)}"/></strong> giao dịch</span>
        <div class="pagination">
            <button disabled aria-label="Trang trước">←</button>
            <span>1</span>
            <button disabled aria-label="Trang tiếp">→</button>
        </div>
    </div>
</section>

<%-- CONFIRMATION MODAL THANH TOÁN ĐƠN HÀNG --%>
<div id="paymentConfirmationModal" class="modal-overlay" style="display:none;position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(15,23,42,0.6);backdrop-filter:blur(4px);z-index:9999;align-items:center;justify-content:center;">
    <div class="modal-container" style="background:#fff;border-radius:14px;max-width:520px;width:92%;padding:24px;box-shadow:0 20px 25px -5px rgba(0,0,0,0.1),0 8px 10px -6px rgba(0,0,0,0.1);position:relative;">
        <div style="display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #e2e8f0;padding-bottom:12px;margin-bottom:16px;">
            <h3 id="modalPaymentTitle" style="margin:0;font-size:18px;color:#0f172a;font-weight:700;">Xác nhận thanh toán đơn hàng</h3>
            <button type="button" id="btnCancelModalX" style="background:none;border:none;font-size:20px;cursor:pointer;color:#64748b;">✕</button>
        </div>
        <div id="modalInstallmentNotice" style="display:none;background:#f0fdf4;border:1px solid #bbf7d0;border-radius:8px;padding:10px 12px;font-size:12px;color:#166534;margin-bottom:14px;line-height:1.4;">
            🏦 <strong>Đơn hàng trả góp:</strong> Sau khi Kế toán đối soát tiền cọc 30% và Quý khách ký hợp đồng, đối tác tài chính mới giải ngân 70% còn lại cho showroom.
        </div>
        <div style="margin-bottom:16px;">
            <table style="width:100%;font-size:14px;border-collapse:collapse;">
                <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;">Mã đơn hàng:</td><td style="padding:6px 0;text-align:right;"><strong id="modalOrderCode" style="color:#0284c7;">—</strong></td></tr>
                <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;">Mẫu xe:</td><td style="padding:6px 0;text-align:right;"><span id="modalVehicleName">—</span></td></tr>
                <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;">Khoản thanh toán:</td><td style="padding:6px 0;text-align:right;"><strong id="modalPaymentType">—</strong></td></tr>
                <tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:6px 0;color:#64748b;">Số tiền cần thanh toán:</td><td style="padding:6px 0;text-align:right;"><strong id="modalPaymentAmount" style="color:#16a34a;font-size:16px;">—</strong></td></tr>
            </table>
        </div>
        <form id="paymentConfirmForm" method="post" action="<%= request.getContextPath() %>/payment/create">
            <input type="hidden" name="csrfToken" value="<c:out value='${paymentCsrf}'/>">
            <input type="hidden" name="orderId" id="modalFormOrderId" value="">
            <input type="hidden" name="type" id="modalFormType" value="">

            <div style="margin-bottom:16px;">
                <label style="display:block;font-size:13px;font-weight:600;margin-bottom:6px;color:#334155;">Chọn phương thức thanh toán:</label>
                <div style="display:grid;gap:8px;">
                    <label style="display:flex;align-items:center;gap:10px;padding:10px;border:1px solid #cbd5e1;border-radius:8px;cursor:pointer;background:#f8fafc;">
                        <input type="radio" name="method" value="BANK_TRANSFER" checked>
                        <div>
                            <strong>Chuyển khoản VietQR tự động qua SePay + MB Bank</strong>
                            <div style="font-size:12px;color:#64748b;">Quét mã QR MB Bank, hệ thống tự động ghi nhận ngay lập tức</div>
                        </div>
                    </label>
                </div>
            </div>

            <div style="background:#eff6ff;border:1px solid #bfdbfe;border-radius:8px;padding:10px;font-size:12px;color:#1e40af;margin-bottom:16px;">
                ℹ <em>Lưu ý: Thao tác xác nhận này sẽ khởi tạo giao dịch thanh toán hợp lệ. Giao dịch chỉ được ghi nhận thành công và trừ công nợ sau khi nhận được tiền thực tế (qua SePay Webhook hoặc xác nhận đối soát từ Kế toán).</em>
            </div>

            <div style="display:flex;justify-content:flex-end;gap:10px;">
                <button type="button" id="btnCancelModal" class="button button-secondary" style="padding:8px 16px;">Không, quay lại</button>
                <button type="submit" id="btnConfirmPaymentSubmit" class="button button-primary" style="padding:8px 20px;">Có, xác nhận</button>
            </div>
        </form>
    </div>
</div>

<script>
document.addEventListener("DOMContentLoaded", function() {
    var modal = document.getElementById("paymentConfirmationModal");
    var modalTitle = document.getElementById("modalPaymentTitle");
    var modalOrderCode = document.getElementById("modalOrderCode");
    var modalVehicleName = document.getElementById("modalVehicleName");
    var modalPaymentType = document.getElementById("modalPaymentType");
    var modalPaymentAmount = document.getElementById("modalPaymentAmount");
    var modalFormOrderId = document.getElementById("modalFormOrderId");
    var modalFormType = document.getElementById("modalFormType");
    var btnCancelModal = document.getElementById("btnCancelModal");
    var btnCancelModalX = document.getElementById("btnCancelModalX");
    var paymentConfirmForm = document.getElementById("paymentConfirmForm");

    function closeModal() {
        if (modal) modal.style.display = "none";
    }

    if (btnCancelModal) btnCancelModal.addEventListener("click", closeModal);
    if (btnCancelModalX) btnCancelModalX.addEventListener("click", closeModal);
    if (modal) {
        modal.addEventListener("click", function(e) {
            if (e.target === modal) closeModal();
        });
    }

    if (paymentConfirmForm) {
        paymentConfirmForm.addEventListener("submit", function() {
            var submitBtn = document.getElementById("btnConfirmPaymentSubmit");
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = "Đang xử lý...";
            }
            if (btnCancelModal) btnCancelModal.disabled = true;
            if (btnCancelModalX) btnCancelModalX.disabled = true;
        });
    }

    document.querySelectorAll(".btn-open-payment-modal").forEach(function(btn) {
        btn.addEventListener("click", function() {
            var orderId = btn.getAttribute("data-order-id");
            var orderCode = btn.getAttribute("data-order-code") || ("ECS" + orderId);
            var formattedAmount = btn.getAttribute("data-formatted-amount") || "";
            var type = btn.getAttribute("data-type") || "DEPOSIT";
            var vehicle = btn.getAttribute("data-vehicle") || "—";

            modalOrderCode.textContent = orderCode + " (#" + orderId + ")";
            modalVehicleName.textContent = vehicle;
            modalPaymentAmount.textContent = formattedAmount;
            modalFormOrderId.value = orderId;
            modalFormType.value = type;

            var isInst = btn.getAttribute("data-is-installment") === "true";
            var partner = btn.getAttribute("data-partner") || "Nhà tài chính đối tác";
            var instNotice = document.getElementById("modalInstallmentNotice");
            if (instNotice) {
                if (isInst) {
                    instNotice.style.display = "block";
                    instNotice.innerHTML = "🏦 <strong>Đơn hàng trả góp:</strong> Sau khi Kế toán đối soát tiền cọc 30% và Quý khách ký hợp đồng, đối tác tài chính <strong>" + partner + "</strong> mới giải ngân 70% còn lại cho showroom.";
                } else {
                    instNotice.style.display = "none";
                }
            }

            if (type === "DEPOSIT") {
                modalTitle.textContent = "Xác nhận đặt cọc 30% · " + orderCode;
                modalPaymentType.textContent = "Đặt cọc 30% giá trị hợp đồng";
            } else {
                modalTitle.textContent = "Xác nhận thanh toán 70% còn lại · " + orderCode;
                modalPaymentType.textContent = "Thanh toán 70% số tiền còn lại";
            }

            modal.style.display = "flex";
        });
    });

    document.querySelectorAll(".btn-customer-cancel-order").forEach(function(btn) {
        btn.addEventListener("click", async function() {
            var orderId = btn.getAttribute("data-order-id");
            var orderCode = btn.getAttribute("data-order-code") || ("ECS" + orderId);
            var vehicle = btn.getAttribute("data-vehicle") || "—";
            var amount = btn.getAttribute("data-amount") || "—";
            var confirmed = false;
            if (typeof window.showAppConfirmModal === "function") {
                confirmed = await window.showAppConfirmModal({
                    title: "Xác nhận hủy đơn hàng",
                    message: "Bạn có chắc chắn muốn hủy đơn hàng này không? Xe đang giữ chỗ sẽ được giải phóng lại kho để khách hàng khác có thể đặt.",
                    details: [
                        { label: "Mã đơn hàng", value: orderCode + " (#" + orderId + ")" },
                        { label: "Mẫu xe", value: vehicle },
                        { label: "Tổng tiền", value: amount }
                    ],
                    confirmText: "Có, xác nhận",
                    cancelText: "Không, quay lại",
                    isDanger: true
                });
            } else {
                confirmed = window.confirm("Bạn có chắc chắn muốn hủy đơn hàng " + orderCode + " (" + vehicle + ")?");
            }
            if (!confirmed) return;
            try {
                var params = new URLSearchParams();
                params.set("orderId", orderId);
                params.set("status", "CANCELLED");
                params.set("format", "json");
                var res = await fetch("<%= request.getContextPath() %>/order-status", {
                    method: "POST",
                    credentials: "same-origin",
                    headers: { "X-CSRF-Token": "<c:out value='${sessionScope.csrfToken}'/>", "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8", "Accept": "application/json" },
                    body: params.toString()
                });
                var data = await res.json();
                if (data.success) {
                    alert(data.message || "Đã hủy đơn hàng thành công.");
                    window.location.reload();
                } else {
                    alert(data.message || data.error || "Không thể hủy đơn hàng.");
                }
            } catch (err) {
                alert("Lỗi kết nối máy chủ khi thực hiện hủy đơn.");
            }
        });
    });
});
</script>

<%@ include file="../common/customer-shell-end.jspf" %>
