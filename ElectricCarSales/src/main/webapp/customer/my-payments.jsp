<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.electriccarsales.model.SalesPreviewKind,com.electriccarsales.service.SalesPreviewService" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<% request.setAttribute("customerTitle", "Thanh toán & Chứng từ"); request.setAttribute("customerEyebrow", "GIAO DỊCH TÀI CHÍNH"); %>
<%@ include file="../common/customer-shell.jspf" %>

<style>
/* CSS Scoped cho my-payments.jsp: Fix tràn viền responsive 375px, 768px, 1366px, 1920px */
.table-wrap {
    width: 100%;
    max-width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
}

#paymentsTable,
.customer-invoice-table {
    width: 100%;
    border-collapse: collapse;
    table-layout: auto;
    white-space: normal !important;
}

#paymentsTable th, #paymentsTable td,
.customer-invoice-table th, .customer-invoice-table td {
    white-space: normal !important;
    word-break: normal;
    overflow-wrap: break-word;
    vertical-align: middle;
    padding: 10px 8px;
    box-sizing: border-box;
}

#paymentsTable thead th,
.customer-invoice-table thead th {
    background: #f8fafc;
    color: #475569;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.5px;
    border-bottom: 2px solid #e2e8f0;
}

.col-pay-time,
#paymentsTable td.col-pay-time-cell,
.col-inv-date,
.customer-invoice-table td.col-inv-date-cell {
    width: 110px;
    min-width: 95px;
    max-width: 125px;
    line-height: 1.35;
    font-size: 11.5px;
    white-space: normal !important;
    overflow-wrap: break-word;
    word-break: normal;
}

.col-pay-id,
#paymentsTable td.col-pay-id-cell,
.col-inv-number,
.customer-invoice-table td.col-inv-number-cell {
    max-width: 135px;
    overflow-wrap: anywhere;
    word-break: break-word;
    font-size: 12.5px;
}

.col-pay-method,
#paymentsTable td.col-pay-method-cell {
    max-width: 180px;
    line-height: 1.35;
    font-size: 12px;
    overflow-wrap: break-word;
    word-break: normal;
}

.col-pay-status,
#paymentsTable td.col-pay-status-cell,
.col-inv-status,
.customer-invoice-table td.col-inv-status-cell {
    max-width: 170px;
    line-height: 1.35;
    white-space: normal !important;
    overflow-wrap: break-word;
}

#paymentsTable .status-badge,
.customer-invoice-table .status-badge {
    white-space: normal !important;
    word-break: normal;
    overflow-wrap: break-word;
    display: inline-flex;
    align-items: center;
    max-width: 100%;
}

.col-pay-actions,
#paymentsTable td.col-pay-actions-cell,
.col-inv-actions,
.customer-invoice-table td.col-inv-actions-cell {
    min-width: 145px;
}

.doc-btn-group {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    align-items: center;
}

.btn-doc {
    white-space: normal !important;
    text-align: center;
    line-height: 1.25;
}

.pay-metric-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 220px), 1fr));
    gap: 16px;
    margin-bottom: 24px;
}

.pay-metric-card {
    min-width: 0;
    overflow-wrap: break-word;
    word-break: normal;
}

.pay-metric-card .button {
    white-space: normal !important;
    text-align: center;
    max-width: 100%;
    height: auto;
    min-height: 38px;
    padding: 8px 14px;
    line-height: 1.35;
    word-break: normal;
}

.order-switch-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 10px 16px;
    margin-bottom: 20px;
    flex-wrap: wrap;
}

.order-switch-bar select {
    max-width: 100%;
    text-overflow: ellipsis;
    min-width: 0;
    flex: 1 1 240px;
}

@media screen and (max-width: 768px) {
    .page-intro {
        flex-direction: column !important;
        align-items: flex-start !important;
        gap: 12px !important;
    }
    .page-intro > div:last-child {
        margin-left: 0 !important;
        width: 100%;
    }
    .page-intro .button {
        flex: 1 1 auto;
        text-align: center;
    }

    .order-switch-bar {
        flex-direction: column !important;
        align-items: stretch !important;
        gap: 8px !important;
        padding: 10px 12px !important;
    }
    .order-switch-bar select {
        width: 100% !important;
        max-width: 100% !important;
    }

    .panel-heading {
        flex-direction: column !important;
        align-items: flex-start !important;
        gap: 12px !important;
    }
    .filter-tabs {
        display: flex;
        flex-wrap: wrap;
        width: 100%;
        gap: 6px;
    }
    .filter-tab {
        flex: 1 1 auto;
        text-align: center;
    }

    #paymentsTable,
    .customer-invoice-table {
        display: block;
        width: 100%;
    }
    #paymentsTable thead,
    .customer-invoice-table thead {
        display: none;
    }
    #paymentsTable tbody,
    .customer-invoice-table tbody {
        display: flex;
        flex-direction: column;
        gap: 14px;
        width: 100%;
    }
    #paymentsTable tbody tr,
    .customer-invoice-table tbody tr {
        display: block;
        background: #fff;
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        padding: 12px 14px;
        box-shadow: 0 1px 3px rgba(0,0,0,0.04);
        box-sizing: border-box;
        width: 100%;
    }
    #paymentsTable tbody td,
    .customer-invoice-table tbody td {
        display: flex;
        flex-wrap: wrap;
        justify-content: space-between;
        align-items: center;
        gap: 6px 10px;
        padding: 8px 0 !important;
        border: none !important;
        border-bottom: 1px dashed #f1f5f9 !important;
        max-width: 100% !important;
        min-width: 0 !important;
        text-align: right;
        box-sizing: border-box;
    }
    #paymentsTable tbody td:last-child,
    .customer-invoice-table tbody td:last-child {
        border-bottom: none !important;
        padding-top: 10px !important;
        justify-content: space-between;
        align-items: flex-start;
    }
    #paymentsTable tbody td::before,
    .customer-invoice-table tbody td::before {
        content: attr(data-label);
        font-weight: 600;
        color: #64748b;
        font-size: 11px;
        text-transform: uppercase;
        letter-spacing: 0.5px;
        text-align: left;
        margin-right: 8px;
        flex-shrink: 0;
    }
    #paymentsTable tbody td > *,
    .customer-invoice-table tbody td > * {
        text-align: right;
        max-width: 100%;
    }

    #paymentsTable tbody td.col-pay-actions-cell,
    .customer-invoice-table tbody td.col-inv-actions-cell,
    #paymentsTable tbody td.col-pay-method-cell,
    #paymentsTable tbody td.col-pay-status-cell {
        flex-direction: column;
        align-items: flex-start;
        text-align: left;
    }
    #paymentsTable tbody td.col-pay-actions-cell::before,
    .customer-invoice-table tbody td.col-inv-actions-cell::before,
    #paymentsTable tbody td.col-pay-method-cell::before,
    #paymentsTable tbody td.col-pay-status-cell::before {
        margin-bottom: 4px;
    }
    #paymentsTable tbody td.col-pay-actions-cell > *,
    .customer-invoice-table tbody td.col-inv-actions-cell > *,
    #paymentsTable tbody td.col-pay-method-cell > *,
    #paymentsTable tbody td.col-pay-status-cell > * {
        text-align: left;
        width: 100%;
    }
    #paymentsTable .doc-btn-group,
    .customer-invoice-table .doc-btn-group {
        justify-content: flex-start;
        width: 100%;
    }

    #paymentsTable tbody td.table-empty-cell,
    .customer-invoice-table tbody td.table-empty-cell {
        display: block !important;
        text-align: center !important;
        border-bottom: none !important;
    }
    #paymentsTable tbody td.table-empty-cell::before,
    .customer-invoice-table tbody td.table-empty-cell::before {
        display: none !important;
    }
}

@media screen and (max-width: 480px) {
    .tx-modal-grid {
        grid-template-columns: 1fr !important;
        gap: 4px 0 !important;
    }
    .pay-metric-grid {
        grid-template-columns: 1fr !important;
        gap: 12px !important;
    }
    .pay-metric-card {
        padding: 14px !important;
    }
    .panel {
        padding: 14px !important;
    }
    .doc-btn-group .btn-doc {
        width: 100%;
        text-align: center;
        justify-content: center;
    }
}
</style>

<div class="page-intro" style="margin-bottom:20px;">
    <div>
        <p style="margin:0 0 6px;font-size:13.5px;color:#334155;line-height:1.5;">
            Quản lý thanh toán đặt cọc 30%, thanh toán 70% còn lại qua <strong>VietQR SePay (MB Bank)</strong>, tra cứu biên nhận giao dịch và <strong>hóa đơn giá trị gia tăng điện tử</strong>.
        </p>
        <small style="color:#64748b;">Hệ thống thanh toán bảo mật · Không tự ý tạo chứng từ khi chưa có giao dịch hợp lệ.</small>
    </div>
    <div style="margin-left:auto;display:flex;gap:8px;flex-wrap:wrap;">
        <a class="button button-secondary" href="<%= customerContext %>/orders" style="padding:7px 14px;font-size:12.5px;">Quản lý đơn hàng <span>→</span></a>
        <a class="button button-primary" href="<%= customerContext %>/contract" style="padding:7px 14px;font-size:12.5px;">Xem hợp đồng <span>→</span></a>
    </div>
</div>

<c:if test="${not empty customerOrders and customerOrders.size() gt 1}">
    <div class="order-switch-bar">
        <label for="orderSelector" style="font-size:12.5px;font-weight:700;color:#1e293b;">📌 Đơn hàng đang xem:</label>
        <select id="orderSelector" onchange="if(this.value) window.location.href='<%= customerContext %>/payment/history?orderId=' + this.value;">
            <c:forEach var="ord" items="${customerOrders}">
                <option value="${ord.id}" ${ord.id == requestedOrderId ? 'selected' : ''}>
                    Đơn hàng #${ord.id} - ${ord.vehicleName != null ? ord.vehicleName : 'Xe điện VinFast'} (<fmt:formatNumber value="${ord.originalAmount}" pattern="#,###"/> ₫) - Trạng thái: ${ord.status}
                </option>
            </c:forEach>
        </select>
    </div>
</c:if>

<!-- =========================================================
     I. THÔNG TIN THANH TOÁN (SECTION II.1)
     ========================================================= -->
<section class="panel" style="padding:22px;margin-bottom:24px;">
    <div class="panel-heading" style="margin-bottom:18px;border-bottom:1px solid #f1f5f9;padding-bottom:12px;">
        <div>
            <span class="section-overline" style="color:#2563eb;font-weight:700;font-size:11px;">I. THÔNG TIN THANH TOÁN &amp; NGHĨA VỤ TÀI CHÍNH</span>
            <h2 style="font-size:17px;margin:3px 0 0;color:#0f172a;">
                <c:choose>
                    <c:when test="${not empty requestedOrderId}">Đơn hàng #${requestedOrderId} <small style="font-weight:500;color:#64748b;font-size:13px;">· <c:out value="${orderBalance.customerName}" default="Khách hàng"/></small></c:when>
                    <c:otherwise>Tổng quan thanh toán của bạn</c:otherwise>
                </c:choose>
            </h2>
        </div>
        <div>
            <c:choose>
                <c:when test="${not empty orderBalance}">
                    <c:choose>
                        <c:when test="${orderBalance.orderStatus eq 'PAID' or orderBalance.orderStatus eq 'COMPLETED'}">
                            <span class="status-badge badge-green"><span class="dot"></span> ĐÃ THANH TOÁN ĐỦ (100%)</span>
                        </c:when>
                        <c:when test="${orderBalance.orderStatus eq 'DEPOSITED'}">
                            <span class="status-badge badge-blue"><span class="dot"></span> ĐÃ THANH TOÁN CỌC 30%</span>
                        </c:when>
                        <c:when test="${orderBalance.orderStatus eq 'PROCESSING'}">
                            <span class="status-badge badge-amber"><span class="dot"></span> CHỜ THANH TOÁN 70%</span>
                        </c:when>
                        <c:when test="${orderBalance.orderStatus eq 'CANCELLED'}">
                            <span class="status-badge badge-red"><span class="dot"></span> ĐƠN HÀNG ĐÃ HỦY</span>
                        </c:when>
                        <c:otherwise>
                            <span class="status-badge badge-amber"><span class="dot"></span> CHỜ THANH TOÁN</span>
                        </c:otherwise>
                    </c:choose>
                </c:when>
                <c:otherwise>
                    <span class="status-badge badge-neutral"><span class="dot"></span> Chưa có giao dịch</span>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <c:choose>
        <c:when test="${not empty orderBalance}">
            <div class="pay-metric-grid">
                <div class="pay-metric-card accent-blue">
                    <div class="pay-metric-label">
                        <span>Tổng giá trị đơn hàng</span>
                        <span style="font-size:15px;">🚗</span>
                    </div>
                    <div class="pay-metric-val">
                        <fmt:formatNumber value="${orderBalance.totalAmount}" pattern="#,###"/> ₫
                    </div>
                    <div class="pay-metric-sub">
                        Phương thức: <strong><c:out value="${orderBalance.paymentMethodName}" default="Chuyển khoản"/></strong>
                        <c:if test="${not empty orderBalance.companyName}"><br>Đối tác: <c:out value="${orderBalance.companyName}"/></c:if>
                    </div>
                </div>

                <div class="pay-metric-card ${orderBalance.depositPaid gt 0 ? 'accent-green' : 'accent-amber'}">
                    <div class="pay-metric-label">
                        <span>Tiền đặt cọc 30%</span>
                        <span><c:choose><c:when test="${orderBalance.depositPaid gt 0}">✓</c:when><c:otherwise>⏳</c:otherwise></c:choose></span>
                    </div>
                    <div class="pay-metric-val" style="${orderBalance.depositPaid gt 0 ? 'color:#15803d;' : 'color:#b45309;'}">
                        <fmt:formatNumber value="${orderBalance.depositAmount}" pattern="#,###"/> ₫
                    </div>
                    <div class="pay-metric-sub">
                        <c:choose>
                            <c:when test="${orderBalance.depositPaid gt 0}">
                                <span style="color:#15803d;font-weight:600;">✓ Đã thanh toán cọc thành công</span>
                            </c:when>
                            <c:otherwise>
                                <span style="color:#b45309;font-weight:600;">⏳ Chưa hoàn tất thanh toán cọc</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <div class="pay-metric-card ${orderBalance.remainingPaid gt 0 ? 'accent-green' : 'accent-amber'}">
                    <div class="pay-metric-label">
                        <span>Thanh toán còn lại 70%</span>
                        <span><c:choose><c:when test="${orderBalance.remainingPaid gt 0}">✓</c:when><c:otherwise>⚖</c:otherwise></c:choose></span>
                    </div>
                    <div class="pay-metric-val">
                        <fmt:formatNumber value="${orderBalance.totalAmount - orderBalance.depositAmount}" pattern="#,###"/> ₫
                    </div>
                    <div class="pay-metric-sub">
                        <c:choose>
                            <c:when test="${orderBalance.remainingPaid gt 0}">
                                <span style="color:#15803d;font-weight:600;">✓ Đã thanh toán đủ 70%</span>
                            </c:when>
                            <c:when test="${orderBalance.contractStatus eq 'SIGNED'}">
                                <span style="color:#2563eb;font-weight:600;">HĐ đã ký · Sẵn sàng thanh toán</span>
                            </c:when>
                            <c:otherwise>
                                <span>Cần ký hợp đồng trước khi thu 70%</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <div class="pay-metric-card accent-green">
                    <div class="pay-metric-label">
                        <span>Đã ghi nhận thành công</span>
                        <span style="color:#15803d;">💰</span>
                    </div>
                    <div class="pay-metric-val" style="color:#15803d;">
                        <fmt:formatNumber value="${orderBalance.successfulPayments}" pattern="#,###"/> ₫
                    </div>
                    <div class="pay-metric-sub">
                        Tỷ lệ hoàn thành: 
                        <strong>
                            <c:choose>
                                <c:when test="${orderBalance.totalAmount gt 0}">
                                    <fmt:formatNumber value="${(orderBalance.successfulPayments / orderBalance.totalAmount) * 100}" maxFractionDigits="0"/>%
                                </c:when>
                                <c:otherwise>0%</c:otherwise>
                            </c:choose>
                        </strong>
                    </div>
                </div>

                <div class="pay-metric-card ${outstandingDebt le 0 ? 'accent-green' : 'accent-red'}" style="grid-column: span 1 / -1;">
                    <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px;">
                        <div>
                            <div class="pay-metric-label" style="margin:0 0 4px;">
                                <span>SỐ DƯ CÔNG NỢ CÒN LẠI</span>
                            </div>
                            <div class="pay-metric-val" style="font-size:24px;${outstandingDebt le 0 ? 'color:#15803d;' : 'color:#b91c1c;'}">
                                <fmt:formatNumber value="${outstandingDebt}" pattern="#,###"/> ₫
                            </div>
                            <div class="pay-metric-sub" style="margin-top:2px;">
                                <c:choose>
                                    <c:when test="${orderBalance.orderStatus eq 'CANCELLED'}">
                                        <span style="color:#991b1b;font-weight:700;">✕ Đơn hàng đã bị hủy; không thể tạo hoặc tiếp tục thanh toán.</span>
                                        <c:if test="${orderBalance.depositPaid gt 0}">
                                            <br><small style="color:#7f1d1d;">Khoản cọc 30% đã xác nhận không được hoàn lại theo quy định quá hạn.</small>
                                        </c:if>
                                    </c:when>
                                    <c:when test="${outstandingDebt le 0}">
                                        <span style="color:#15803d;font-weight:700;">✓ Bạn đã tất toán 100% nghĩa vụ tài chính cho đơn hàng này.</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span style="color:#b91c1c;font-weight:600;">Số tiền bạn cần hoàn tất trước khi bàn giao xe.</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                        <div style="display:flex;gap:8px;flex-wrap:wrap;">
                            <c:if test="${orderBalance.depositPaid le 0 and orderBalance.orderStatus ne 'CANCELLED'}">
                                <a class="button button-primary" href="<%= customerContext %>/payment/transfer?orderId=${requestedOrderId}&type=DEPOSIT" style="padding:9px 18px;font-size:13px;">
                                    ⚡ Thanh toán cọc 30% ngay (VietQR) →
                                </a>
                            </c:if>
                            <c:if test="${orderBalance.depositPaid gt 0 and outstandingDebt gt 0 and orderBalance.contractStatus eq 'SIGNED' and orderBalance.orderStatus ne 'CANCELLED'}">
                                <a class="button button-primary" href="<%= customerContext %>/payment/transfer?orderId=${requestedOrderId}&type=REMAINING" style="padding:9px 18px;font-size:13px;background:#2563eb;">
                                    ⚡ Thanh toán 70% còn lại (VietQR) →
                                </a>
                            </c:if>
                            <c:if test="${orderBalance.depositPaid gt 0 and outstandingDebt gt 0 and orderBalance.contractStatus ne 'SIGNED' and orderBalance.orderStatus ne 'CANCELLED'}">
                                <a class="button button-warning" href="<%= customerContext %>/contract" style="padding:9px 18px;font-size:13px;background:#f59e0b;color:#fff;border-radius:6px;text-decoration:none;font-weight:600;">
                                    ✍️ Ký hợp đồng OTP để mở khóa thanh toán 70% →
                                </a>
                            </c:if>
                        </div>
                    </div>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div style="text-align:center;padding:32px 16px;background:#f8fafc;border-radius:8px;">
                <span style="font-size:32px;display:block;margin-bottom:8px;">💳</span>
                <strong style="font-size:15px;color:#0f172a;display:block;">Chưa có dữ liệu thanh toán</strong>
                <p style="color:#64748b;font-size:13px;margin:4px 0 16px;">Vui lòng đặt hàng để bắt đầu quy trình đặt cọc 30% và thanh toán xe điện VinFast.</p>
                <a class="button button-primary" href="<%= customerContext %>/index.jsp#models">Khám phá danh mục xe ngay</a>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<!-- =========================================================
     II. LỊCH SỬ GIAO DỊCH & BIÊN LAI (SECTION II.2)
     ========================================================= -->
<c:set var="completedCount" value="0"/>
<c:set var="pendingCount" value="0"/>
<c:set var="failedCount" value="0"/>
<c:forEach var="p" items="${paymentHistory}">
    <c:choose>
        <c:when test="${p.orderStatus eq 'CANCELLED' or p.paymentStatusCode eq 'CANCELLED' or p.paymentStatusCode eq 'FAILD' or p.paymentStatusCode eq 'FAILED'}"><c:set var="failedCount" value="${failedCount + 1}"/></c:when>
        <c:when test="${p.paymentFinalized}"><c:set var="completedCount" value="${completedCount + 1}"/></c:when>
        <c:when test="${p.paymentStatusCode eq 'PENDING' or p.paymentStatusCode eq 'COMPLETED'}"><c:set var="pendingCount" value="${pendingCount + 1}"/></c:when>
        <c:otherwise><c:set var="failedCount" value="${failedCount + 1}"/></c:otherwise>
    </c:choose>
</c:forEach>

<section class="panel table-panel" style="margin-bottom:28px;">
    <div class="panel-heading" style="padding:18px 22px;border-bottom:1px solid #f1f5f9;">
        <div>
            <span class="section-overline" style="color:#059669;font-weight:700;font-size:11px;">II. LỊCH SỬ GIAO DỊCH &amp; BIÊN LAI THU TIỀN</span>
            <h2 style="font-size:17px;margin:3px 0 0;color:#0f172a;">Lịch sử thanh toán &amp; Biên lai</h2>
        </div>
        <div class="filter-tabs" role="tablist" style="margin:0;">
            <button class="filter-tab active" type="button" onclick="filterPayments('ALL', this)">Tất cả <span>${not empty paymentHistory ? paymentHistory.size() : 0}</span></button>
            <button class="filter-tab" type="button" onclick="filterPayments('COMPLETED', this)">Đã xác nhận <span>${completedCount}</span></button>
            <button class="filter-tab" type="button" onclick="filterPayments('PENDING', this)">Chờ đối soát <span>${pendingCount}</span></button>
            <c:if test="${failedCount gt 0}">
                <button class="filter-tab" type="button" onclick="filterPayments('FAILD', this)">Thất bại/Hủy <span>${failedCount}</span></button>
            </c:if>
        </div>
    </div>

    <div class="table-wrap">
        <table id="paymentsTable">
            <thead>
                <tr>
                    <th class="col-pay-id">MÃ THANH TOÁN</th>
                    <th class="col-pay-order">ĐƠN HÀNG</th>
                    <th class="col-pay-type">LOẠI KHOẢN</th>
                    <th class="col-pay-method">PHƯƠNG THỨC</th>
                    <th class="col-pay-amount">SỐ TIỀN</th>
                    <th class="col-pay-time">THỜI GIAN</th>
                    <th class="col-pay-status">TRẠNG THÁI</th>
                    <th class="col-pay-actions">CHỨNG TỪ / BIÊN LAI</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${not empty paymentHistory}">
                        <c:forEach var="payment" items="${paymentHistory}">
                            <tr data-status="${payment.orderStatus eq 'CANCELLED' ? 'CANCELLED' : (payment.paymentFinalized ? 'COMPLETED' : (payment.paymentStatusCode eq 'COMPLETED' or payment.paymentStatusCode eq 'PENDING' ? 'PENDING' : payment.paymentStatusCode))}">
                                <td class="col-pay-id-cell" data-label="Mã thanh toán">
                                    <strong><c:out value="${payment.referenceCode}" default="#${payment.paymentID}"/></strong>
                                    <small style="display:block;color:#94a3b8;font-size:10px;">ID: #${payment.paymentID}</small>
                                </td>
                                <td class="col-pay-order-cell" data-label="Đơn hàng">
                                    <a href="<%= customerContext %>/payment/history?orderId=${payment.orderID}" style="font-weight:700;color:#2563eb;text-decoration:none;">
                                        #<c:out value="${payment.orderID}"/>
                                    </a>
                                </td>
                                <td class="col-pay-type-cell" data-label="Loại khoản">
                                    <c:choose>
                                        <c:when test="${payment.paymentTypeName eq 'DEPOSIT'}">
                                            <span style="font-weight:600;color:#0f172a;">Đặt cọc 30%</span>
                                        </c:when>
                                        <c:when test="${payment.paymentTypeName eq 'REMAINING'}">
                                            <span style="font-weight:600;color:#0f172a;">Thanh toán 70%</span>
                                        </c:when>
                                        <c:otherwise>
                                            <c:out value="${payment.paymentTypeName}" default="Thanh toán"/>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="col-pay-method-cell" data-label="Phương thức">
                                    <c:choose>
                                        <c:when test="${payment.paymentMethodName eq 'BANK_TRANSFER'}">
                                            <span>Chuyển khoản VietQR (MB Bank)</span>
                                        </c:when>
                                        <c:when test="${payment.paymentMethodName eq 'CASH'}">
                                            <span>Tiền mặt tại showroom</span>
                                        </c:when>
                                        <c:when test="${payment.paymentMethodName eq 'INSTALLMENT'}">
                                            <span>Giải ngân đối tác trả góp</span>
                                        </c:when>
                                        <c:otherwise>
                                            <c:out value="${payment.paymentMethodName}" default="Chuyển khoản"/>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="col-pay-amount-cell" data-label="Số tiền">
                                    <strong style="color:#0f172a;"><fmt:formatNumber value="${payment.paymentAmount}" pattern="#,###"/> ₫</strong>
                                </td>
                                <td class="col-pay-time-cell" data-label="Thời gian" style="font-size:12px;color:#64748b;">
                                    <c:out value="${payment.paymentDate}" default="—"/>
                                </td>
                                <td class="col-pay-status-cell" data-label="Trạng thái">
                                    <c:choose>
                                        <c:when test="${payment.orderStatus eq 'CANCELLED'}">
                                            <c:choose>
                                                <c:when test="${payment.paymentStatusCode eq 'COMPLETED' and payment.paymentTypeName eq 'DEPOSIT'}">
                                                    <span class="status-badge badge-red"><span class="dot"></span> Đơn đã hủy · Mất cọc 30%</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="status-badge badge-red"><span class="dot"></span> Đơn hàng đã hủy</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:when>
                                        <c:when test="${payment.partnerDisbursement}">
                                            <span class="status-badge badge-green"><span class="dot"></span> Đối tác đã giải ngân</span>
                                        </c:when>
                                        <c:when test="${payment.accountantConfirmed}">
                                            <span class="status-badge badge-green"><span class="dot"></span> Đã đối soát</span>
                                        </c:when>
                                        <c:when test="${payment.paymentStatusCode eq 'COMPLETED'}">
                                            <span class="status-badge badge-amber"><span class="dot"></span> Đã nhận tiền · chờ Kế toán đối soát</span>
                                        </c:when>
                                        <c:when test="${payment.paymentStatusCode eq 'PENDING'}">
                                            <span class="status-badge badge-amber"><span class="dot"></span> Chờ đối soát</span>
                                        </c:when>
                                        <c:when test="${payment.paymentStatusCode eq 'FAILD' or payment.paymentStatusCode eq 'FAILED'}">
                                            <span class="status-badge badge-red"><span class="dot"></span> Thất bại</span>
                                        </c:when>
                                        <c:when test="${payment.paymentStatusCode eq 'CANCELLED'}">
                                            <span class="status-badge badge-red"><span class="dot"></span> Đã hủy</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="status-badge badge-neutral"><c:out value="${payment.paymentStatusCode}"/></span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="col-pay-actions-cell" data-label="Chứng từ / Biên lai">
                                    <c:choose>
                                        <c:when test="${payment.orderStatus eq 'CANCELLED'}">
                                            <small style="color:#991b1b;font-style:italic;">Đơn đã hủy</small>
                                        </c:when>
                                        <c:when test="${payment.paymentFinalized}">
                                            <div class="doc-btn-group">
                                                <c:if test="${payment.paymentTypeName eq 'DEPOSIT'}">
                                                    <c:url var="depositHtml" value="/customer/sales-document">
                                                        <c:param name="kind" value="deposit"/>
                                                        <c:param name="orderId" value="${payment.orderID}"/>
                                                        <c:param name="view" value="html"/>
                                                    </c:url>
                                                    <c:url var="depositDocx" value="/customer/sales-document">
                                                        <c:param name="kind" value="deposit"/>
                                                        <c:param name="orderId" value="${payment.orderID}"/>
                                                    </c:url>
                                                    <a class="btn-doc btn-doc-primary" href="<c:out value='${depositHtml}'/>">👁️ Xem biên nhận</a>
                                                    <a class="btn-doc btn-doc-outline" href="<c:out value='${depositDocx}'/>" title="Tải file Word .docx">📥 Word</a>
                                                </c:if>
                                                <c:if test="${payment.paymentTypeName ne 'DEPOSIT'}">
                                                    <button class="btn-doc btn-doc-outline" type="button" 
                                                            onclick="openTxModal('${payment.referenceCode}', '${payment.orderID}', '${payment.paymentTypeName}', '${payment.paymentMethodName}', '${payment.paymentAmount}', '${payment.paymentDate}', '${payment.paymentStatusCode}', '<c:out value="${payment.note}" escapeXml="true"/>')">
                                                        👁️ Chi tiết giao dịch
                                                    </button>
                                                </c:if>
                                            </div>
                                        </c:when>
                                        <c:when test="${payment.paymentStatusCode eq 'COMPLETED'}">
                                            <small style="color:#b45309;">Đã nhận tiền · chờ Kế toán đối soát</small>
                                        </c:when>
                                        <c:when test="${payment.paymentStatusCode eq 'PENDING'}">
                                            <c:choose>
                                                <c:when test="${payment.paymentMethodName eq 'BANK_TRANSFER'}">
                                                    <a class="btn-doc btn-doc-warning" href="<%= customerContext %>/payment/transfer?orderId=${payment.orderID}&paymentId=${payment.paymentID}&type=${payment.paymentTypeName}">
                                                        Quét mã VietQR →
                                                    </a>
                                                </c:when>
                                                <c:otherwise>
                                                    <small style="color:#b45309;">Chờ nhân viên đối soát</small>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:when>
                                        <c:otherwise>
                                            <small style="color:#94a3b8;font-style:italic;">Giao dịch chưa hoàn tất</small>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td colspan="8" class="table-empty-cell">
                                <div class="table-empty" style="padding:32px 16px;">
                                    <span>◉</span>
                                    <strong>Chưa có khoản thanh toán nào</strong>
                                    <p>Khi bạn thực hiện đặt cọc hoặc thanh toán tiền mua xe, lịch sử và biên nhận sẽ được hiển thị đầy đủ tại đây.</p>
                                </div>
                            </td>
                        </tr>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
</section>

<!-- =========================================================
     III. HÓA ĐƠN GIÁ TRỊ GIA TĂNG (SECTION II.3 & I)
     ========================================================= -->
<div id="invoices"></div>
<section class="panel table-panel invoice-list" style="margin-bottom:28px;">
    <div class="panel-heading" style="padding:18px 22px;border-bottom:1px solid #f1f5f9;">
        <div>
            <span class="section-overline" style="color:#7c3aed;font-weight:700;font-size:11px;">III. HÓA ĐƠN GIÁ TRỊ GIA TĂNG (BẢNG INVOICES)</span>
            <h2 style="font-size:17px;margin:3px 0 0;color:#0f172a;">Hóa đơn điện tử chính thức</h2>
            <p style="margin:4px 0 0;font-size:12px;color:#64748b;">
                Hóa đơn GTGT chính thức do Kế toán phát hành sau khi Hợp đồng đã ký xác thực OTP và đơn hàng đã thanh toán 100%.
            </p>
        </div>
        <div>
            <span class="status-badge ${not empty customerInvoices ? 'badge-green' : 'badge-neutral'}">
                <span class="dot"></span> <c:out value="${not empty customerInvoices ? customerInvoices.size() : 0}"/> hóa đơn đã phát hành
            </span>
        </div>
    </div>

    <div class="table-wrap">
        <table class="customer-invoice-table">
            <thead>
                <tr>
                    <th class="col-inv-number">SỐ HÓA ĐƠN</th>
                    <th class="col-inv-date">NGÀY PHÁT HÀNH</th>
                    <th class="col-inv-order">ĐƠN HÀNG</th>
                    <th class="col-inv-subtotal">TẠM TÍNH</th>
                    <th class="col-inv-tax">THUẾ GTGT</th>
                    <th class="col-inv-total">TỔNG THANH TOÁN</th>
                    <th class="col-inv-status">TRẠNG THÁI</th>
                    <th class="col-inv-actions">THAO TÁC</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${not empty customerInvoices}">
                        <c:forEach var="inv" items="${customerInvoices}">
                            <tr>
                                <td class="col-inv-number-cell" data-label="Số hóa đơn">
                                    <strong style="color:#0f172a;"><c:out value="${inv.invoiceNumber}"/></strong>
                                    <small style="display:block;color:#64748b;font-size:10px;">ID: #${inv.invoiceID}</small>
                                </td>
                                <td class="col-inv-date-cell" data-label="Ngày phát hành" style="font-size:12.5px;color:#475569;">
                                    <c:out value="${inv.invoiceDate}"/>
                                </td>
                                <td class="col-inv-order-cell" data-label="Đơn hàng">
                                    <a href="<%= customerContext %>/payment/history?orderId=${inv.orderID}" style="font-weight:700;color:#2563eb;text-decoration:none;">
                                        #<c:out value="${inv.orderID}"/>
                                    </a>
                                </td>
                                <td class="col-inv-subtotal-cell" data-label="Tạm tính" style="font-size:12.5px;">
                                    <fmt:formatNumber value="${inv.subTotal}" pattern="#,###"/> ₫
                                </td>
                                <td class="col-inv-tax-cell" data-label="Thuế GTGT" style="font-size:12.5px;color:#64748b;">
                                    <fmt:formatNumber value="${inv.taxAmount}" pattern="#,###"/> ₫
                                </td>
                                <td class="col-inv-total-cell" data-label="Tổng thanh toán">
                                    <strong style="color:#15803d;font-size:13.5px;"><fmt:formatNumber value="${inv.totalAmount}" pattern="#,###"/> ₫</strong>
                                </td>
                                <td class="col-inv-status-cell" data-label="Trạng thái">
                                    <span class="status-badge badge-green"><span class="dot"></span> ĐÃ PHÁT HÀNH</span>
                                </td>
                                <td class="col-inv-actions-cell" data-label="Thao tác">
                                    <c:url var="invoiceHtml" value="/customer/sales-document">
                                        <c:param name="kind" value="invoice"/>
                                        <c:param name="orderId" value="${inv.orderID}"/>
                                        <c:param name="view" value="html"/>
                                    </c:url>
                                    <c:url var="invoiceDocx" value="/customer/sales-document">
                                        <c:param name="kind" value="invoice"/>
                                        <c:param name="orderId" value="${inv.orderID}"/>
                                    </c:url>
                                    <div class="doc-btn-group">
                                        <a class="btn-doc btn-doc-success" href="<c:out value='${invoiceHtml}'/>">👁️ Xem hóa đơn</a>
                                        <a class="btn-doc btn-doc-outline" href="<c:out value='${invoiceDocx}'/>" title="Tải file Word .docx">📥 Word</a>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td colspan="8" class="table-empty-cell">
                                <div class="table-empty" style="padding:28px 16px;">
                                    <span style="font-size:28px;color:#94a3b8;">📄</span>
                                    <strong style="color:#0f172a;font-size:14px;">Chưa phát hành hóa đơn</strong>
                                    <p style="max-width:560px;margin:6px auto 14px;color:#64748b;font-size:12.5px;line-height:1.6;">
                                        Theo quy định quản lý tài chính: Hóa đơn điện tử chỉ được Kế toán duyệt phát hành sau khi đơn hàng đã hoàn tất đặt cọc 30%, Hợp đồng mua bán đã được ký xác thực bằng mã OTP và công nợ 70% còn lại đã được quyết toán đủ (số dư công nợ = 0).
                                    </p>
                                    <c:if test="${not empty customerOrders}">
                                        <div style="max-width:620px;margin:0 auto;text-align:left;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:12px 16px;">
                                            <strong style="font-size:12px;color:#0f172a;display:block;margin-bottom:8px;text-transform:uppercase;">Tiến độ phát hành hóa đơn theo đơn hàng:</strong>
                                            <c:forEach var="ord" items="${customerOrders}">
                                                <div style="font-size:12px;padding:6px 0;border-bottom:1px dashed #e2e8f0;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;">
                                                    <span>
                                                        <strong>Đơn hàng #${ord.id}</strong> 
                                                        (${ord.vehicleName != null ? ord.vehicleName : 'Xe điện VinFast'}) · 
                                                        Tổng: <fmt:formatNumber value="${ord.originalAmount}" pattern="#,###"/> ₫
                                                    </span>
                                                    <span>
                                                        <c:choose>
                                                            <c:when test="${ord.status eq 'CANCELLED'}">
                                                                <span class="status-badge badge-red"><span class="dot"></span> Đơn hàng đã hủy</span>
                                                            </c:when>
                                                            <c:when test="${ord.remainingAmount le 0 and ord.contractStatus eq 'SIGNED'}">
                                                                <span class="status-badge badge-amber"><span class="dot"></span> Đã đủ điều kiện · Đang chờ Kế toán phát hành</span>
                                                            </c:when>
                                                            <c:when test="${ord.contractStatus ne 'SIGNED' and ord.depositAmount gt 0}">
                                                                <span class="status-badge badge-neutral"><span class="dot"></span> Chưa ký hợp đồng OTP</span>
                                                            </c:when>
                                                            <c:when test="${ord.remainingAmount gt 0}">
                                                                <span class="status-badge badge-neutral"><span class="dot"></span> Còn nợ <fmt:formatNumber value="${ord.remainingAmount}" pattern="#,###"/> ₫</span>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <span class="status-badge badge-neutral"><span class="dot"></span> Chưa thanh toán cọc</span>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </span>
                                                </div>
                                            </c:forEach>
                                        </div>
                                    </c:if>
                                </div>
                            </td>
                        </tr>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
</section>

<!-- Transaction Details Modal -->
<div id="txDetailModal" style="display:none;position:fixed;inset:0;background:rgba(0,0,0,0.5);z-index:9999;align-items:center;justify-content:center;padding:16px;">
    <div style="background:#fff;border-radius:12px;max-width:500px;width:100%;box-shadow:0 20px 25px -5px rgba(0,0,0,0.1);overflow:hidden;">
        <div style="padding:16px 20px;background:#f8fafc;border-bottom:1px solid #e2e8f0;display:flex;justify-content:space-between;align-items:center;">
            <h3 style="margin:0;font-size:16px;color:#0f172a;">Chi tiết giao dịch thanh toán</h3>
            <button onclick="closeTxModal()" type="button" style="background:none;border:none;font-size:20px;cursor:pointer;color:#64748b;">&times;</button>
        </div>
        <div style="padding:20px;font-size:13px;line-height:1.8;" id="txModalBody">
            <!-- Populated dynamically by JS -->
        </div>
        <div style="padding:12px 20px;background:#f8fafc;border-top:1px solid #e2e8f0;text-align:right;">
            <button onclick="closeTxModal()" type="button" class="button button-light" style="padding:6px 14px;font-size:12.5px;">Đóng</button>
        </div>
    </div>
</div>

<script>
function filterPayments(status, btn) {
    document.querySelectorAll('.filter-tabs .filter-tab').forEach(function(b) { b.classList.remove('active'); });
    btn.classList.add('active');
    var rows = document.querySelectorAll('#paymentsTable tbody tr[data-status]');
    rows.forEach(function(r) {
        if (status === 'ALL') {
            r.style.display = '';
        } else if (status === 'FAILD') {
            var st = r.getAttribute('data-status');
            r.style.display = (st === 'FAILD' || st === 'FAILED' || st === 'CANCELLED') ? '' : 'none';
        } else {
            r.style.display = (r.getAttribute('data-status') === status) ? '' : 'none';
        }
    });
}

function openTxModal(ref, orderId, type, method, amount, date, status, note) {
    var modal = document.getElementById('txDetailModal');
    var body = document.getElementById('txModalBody');
    var fmtAmount = Number(amount).toLocaleString('vi-VN') + ' ₫';
    body.innerHTML = 
        '<div class="tx-modal-grid" style="display:grid;grid-template-columns:130px 1fr;gap:8px 12px;">' +
        '  <span style="color:#64748b;">Mã thanh toán:</span><strong>' + (ref || '—') + '</strong>' +
        '  <span style="color:#64748b;">Mã đơn hàng:</span><span>#' + orderId + '</span>' +
        '  <span style="color:#64748b;">Loại thanh toán:</span><span>' + (type === 'DEPOSIT' ? 'Đặt cọc 30%' : (type === 'REMAINING' ? 'Thanh toán 70%' : type)) + '</span>' +
        '  <span style="color:#64748b;">Phương thức:</span><span>' + (method === 'BANK_TRANSFER' ? 'VietQR MB Bank (0917231548)' : method) + '</span>' +
        '  <span style="color:#64748b;">Số tiền:</span><strong style="color:#15803d;font-size:15px;">' + fmtAmount + '</strong>' +
        '  <span style="color:#64748b;">Thời gian:</span><span>' + (date || '—') + '</span>' +
        '  <span style="color:#64748b;">Trạng thái:</span><span style="color:#15803d;font-weight:700;">' + status + '</span>' +
        '  <span style="color:#64748b;">Ghi chú đối soát:</span><span style="word-break:break-all;font-size:12px;color:#334155;">' + (note || 'Đã đối soát khớp hệ thống') + '</span>' +
        '</div>';
    modal.style.display = 'flex';
}

function closeTxModal() {
    document.getElementById('txDetailModal').style.display = 'none';
}
</script>

<%@ include file="../common/customer-shell-end.jspf" %>
