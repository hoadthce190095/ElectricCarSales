<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("customerTitle", "Hợp đồng & tài liệu"); request.setAttribute("customerEyebrow", "HỒ SƠ GIAO DỊCH"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="page-intro">
    <p>Hợp đồng và tài liệu liên quan đến các giao dịch của bạn sẽ được lưu trữ tại đây.</p>
    <span class="soft-badge">Chỉ xem tài liệu được cấp quyền</span>
</div>
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
<%@ include file="../common/customer-shell-end.jspf" %>
