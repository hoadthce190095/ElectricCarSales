<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("customerTitle", "Hóa đơn"); request.setAttribute("customerEyebrow", "CHỨNG TỪ THANH TOÁN"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="page-intro">
    <p>Tra cứu hóa đơn, tổng giá trị và trạng thái phát hành theo giao dịch của bạn.</p>
</div>
<section class="invoice-overview">
    <div class="invoice-metric">
        <span class="metric-icon">▨</span>
        <span>Tổng số hóa đơn</span>
        <strong>0</strong>
        <small>Được phát hành</small>
    </div>
    <div class="invoice-metric">
        <span class="metric-icon metric-green">✓</span>
        <span>Đã phát hành</span>
        <strong>0</strong>
        <small>Hóa đơn hợp lệ</small>
    </div>
    <div class="invoice-metric">
        <span class="metric-icon metric-amber">◷</span>
        <span>Đang xử lý</span>
        <strong>0</strong>
        <small>Chờ cập nhật</small>
    </div>
</section>
<section class="panel table-panel invoice-list">
    <div class="panel-heading">
        <div>
            <span class="section-overline">LỊCH SỬ HÓA ĐƠN</span>
            <h2>Hóa đơn của tôi</h2>
        </div>
        <label class="search-field">
            <span>⌕</span>
            <input type="search" placeholder="Tìm số hóa đơn" aria-label="Tìm số hóa đơn">
        </label>
    </div>
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>SỐ HÓA ĐƠN</th>
                    <th>NGÀY PHÁT HÀNH</th>
                    <th>MÃ ĐƠN HÀNG</th>
                    <th>TỔNG TIỀN</th>
                    <th>TRẠNG THÁI</th>
                    <th>TÀI LIỆU</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td colspan="6">
                        <div class="table-empty">
                            <span>▨</span>
                            <strong>Chưa có hóa đơn</strong>
                            <p>Hóa đơn của các giao dịch đã phát hành sẽ có thể xem tại đây.</p>
                        </div>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</section>
<%@ include file="../common/customer-shell-end.jspf" %>
