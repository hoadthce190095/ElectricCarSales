<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("customerTitle", "Đơn hàng của tôi"); request.setAttribute("customerEyebrow", "GIAO DỊCH CỦA BẠN"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="page-intro split-intro">
    <p>Theo dõi đơn hàng và tiến độ giao dịch thuộc tài khoản của bạn.</p>
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
                    <th>MÃ GIAO DỊCH</th>
                    <th>NGÀY TẠO</th>
                    <th>MẪU XE</th>
                    <th>TỔNG GIÁ TRỊ</th>
                    <th>TRẠNG THÁI</th>
                    <th>
                    </th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td colspan="6">
                        <div class="table-empty">
                            <span>▤</span>
                            <strong>Chưa có giao dịch</strong>
                            <p>Các giao dịch đã được nhân viên ghi nhận sẽ xuất hiện tại đây.</p>
                        </div>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
    <div class="table-foot">
        <span>Đang hiển thị <strong>0</strong> giao dịch</span>
        <div class="pagination">
            <button disabled aria-label="Trang trước">←</button>
            <span>1</span>
            <button disabled aria-label="Trang tiếp">→</button>
        </div>
    </div>
</section>
<%@ include file="../common/customer-shell-end.jspf" %>
