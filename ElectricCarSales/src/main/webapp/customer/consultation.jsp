<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% 
    request.setAttribute("customerTitle", "Yêu cầu tư vấn"); 
    request.setAttribute("customerEyebrow", "DỊCH VỤ TƯ VẤN KHÁCH HÀNG"); 
%>
<%@ include file="../common/customer-shell.jspf" %>

<div class="page-intro split-intro">
    <div>
        <h1 style="color: #fff; font-size: 1.5rem; margin-bottom: 0.25rem;">Gửi Yêu Cầu Tư Vấn Mua Xe</h1>
        <p style="color: #8f96a3;">Điền thông tin và nhu cầu của bạn để chuyên viên tư vấn VinFast liên hệ hỗ trợ sớm nhất.</p>
    </div>
</div>

<% if ("true".equals(request.getParameter("success"))) { %>
<div style="background: rgba(34, 197, 94, 0.15); border: 1px solid #22c55e; color: #4ade80; padding: 1rem; border-radius: 8px; margin-bottom: 1.5rem;">
    ✓ <strong>Thành công!</strong> Yêu cầu tư vấn của bạn đã được gửi. Chuyên viên kinh doanh sẽ liên hệ với bạn trong thời gian sớm nhất.
</div>
<% } %>

<section class="panel" style="max-width: 720px; padding: 2rem; background: #16181c; border: 1px solid #262930; border-radius: 12px;">
    <form action="<%= request.getContextPath() %>/customer/consultation" method="POST">
        <div style="margin-bottom: 1.25rem;">
            <label style="display: block; color: #e1e4ea; font-weight: 500; margin-bottom: 0.5rem;" for="modelSelect">Mẫu xe điện bạn quan tâm</label>
            <select id="modelSelect" name="modelId" style="width: 100%; padding: 0.75rem; background: #0e0f12; border: 1px solid #333742; color: #fff; border-radius: 8px;">
                <option value="1">VinFast VF 3 - Xe đô thị nhỏ gọn</option>
                <option value="2">VinFast VF 5 Plus - Đa dụng phong cách</option>
                <option value="3">VinFast VF 6 - SUV cỡ B hiện đại</option>
                <option value="4">VinFast VF 7 - SUV thể thao cá tính</option>
                <option value="5">VinFast VF 8 - SUV điện đẳng cấp toàn cầu</option>
                <option value="6">VinFast VF 9 - SUV hạng sang 7 chỗ</option>
            </select>
        </div>

        <div style="margin-bottom: 1.25rem;">
            <label style="display: block; color: #e1e4ea; font-weight: 500; margin-bottom: 0.5rem;" for="consultationContent">Nội dung bạn muốn được tư vấn</label>
            <textarea id="consultationContent" name="content" rows="4" required placeholder="Ví dụ: Tôi muốn tư vấn về giá lăn bánh, chính sách trả góp 0% và ưu đãi pin..." style="width: 100%; padding: 0.75rem; background: #0e0f12; border: 1px solid #333742; color: #fff; border-radius: 8px;"></textarea>
        </div>

        <button type="submit" style="background: #2563eb; color: #fff; border: none; padding: 0.85rem 1.75rem; border-radius: 8px; font-weight: 600; cursor: pointer; display: inline-flex; align-items: center; gap: 0.5rem;">
            <span>Gửi yêu cầu ngay</span>
            <span>→</span>
        </button>
    </form>
</section>

<%@ include file="../common/customer-shell-end.jspf" %>
