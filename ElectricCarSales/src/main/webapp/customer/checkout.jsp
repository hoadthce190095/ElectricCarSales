<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<% request.setAttribute("customerTitle", "Yêu cầu tư vấn & Đặt xe"); request.setAttribute("customerEyebrow", "TƯ VẤN BÁN HÀNG"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="checkout-notice">
    <span>i</span>
    <div>
        <strong>Quy trình Tư vấn &amp; Lập đơn hàng theo RDS</strong>
        <p>Khách hàng gửi yêu cầu tư vấn mẫu xe và phương án thanh toán (Trả thẳng hoặc Trả góp qua Ngân hàng đối tác). Sales Staff sẽ tiếp nhận, phản hồi tư vấn và tạo giao dịch đơn hàng chính thức trên hệ thống.</p>
    </div>
</div>
<c:if test="${param.submitted == '1' or param.updated == '1'}">
    <div class="info-note" style="margin-bottom:16px;border-color:#86efac;background:#f0fdf4;color:#166534;">
        <span>✓</span>
        <p>Đã ghi nhận thông tin tư vấn vào hệ thống (Bảng ConsultationRequests). Sales Staff sẽ phản hồi và hỗ trợ lập đơn hàng.</p>
    </div>
</c:if>
<c:if test="${not empty error}">
    <div class="info-note" style="margin-bottom:16px;border-color:#fca5a5;background:#fef2f2;color:#991b1b;">
        <span>!</span>
        <p><c:out value="${error}"/></p>
    </div>
</c:if>
<div class="checkout-layout">
    <section class="panel checkout-panel">
        <div class="panel-heading">
            <div>
                <span class="section-overline">GỬI YÊU CẦU TƯ VẤN</span>
                <h2>Để lại nhu cầu mua xe của bạn</h2>
            </div>
            <button type="button" class="button button-secondary" data-open-consult-popup>⛶ Mở Popup Tính giá Toàn màn hình</button>
        </div>
        <form class="customer-form" method="post" action="<%= customerContext %>/consultation">
            <input type="hidden" name="action" value="create">
            <div class="form-row">
                <label>Mẫu xe quan tâm
                    <select name="modelId" required>
                        <option value="">-- Chọn mẫu xe VinFast --</option>
                        <c:forEach var="m" items="${carModels}">
                            <option value="${m.modelID}"><c:out value="${m.modelName}"/></option>
                        </c:forEach>
                    </select>
                </label>
                <label>Phương án thanh toán dự kiến
                    <select name="paymentPreference">
                        <option value="Trả thẳng (Đặt cọc 30% + Thanh toán 70%)">Trả thẳng (Cọc 30% + Trả 70%)</option>
                        <option value="Trả góp qua Techcombank (Cọc 30% + NH giải ngân 70%)">Trả góp qua Techcombank</option>
                        <option value="Trả góp qua MB Bank (Cọc 30% + NH giải ngân 70%)">Trả góp qua MB Bank</option>
                        <option value="Trả góp qua Shinhan Finance (Cọc 30% + NH giải ngân 70%)">Trả góp qua Shinhan Finance</option>
                        <option value="Trả góp qua VPBank (Cọc 30% + NH giải ngân 70%)">Trả góp qua VPBank</option>
                    </select>
                </label>
            </div>
            <label>Nội dung cần tư vấn (màu sắc, phiên bản, thời gian nhận xe, yêu cầu lập đơn...)
                <textarea name="message" rows="4" maxlength="900" placeholder="Ví dụ: Tôi muốn mua VF 7 Plus màu Đỏ, nhờ nhân viên Sales kiểm tra xe sẵn có và lập đơn hàng giúp tôi..." required></textarea>
            </label>
            <label class="consent-row">
                <input type="checkbox" required checked>
                <span>Tôi đồng ý để Sales Staff liên hệ tư vấn và lập đơn hàng theo thông tin đã cung cấp.</span>
            </label>
            <button class="button button-primary" type="submit">Gửi yêu cầu tư vấn <span>→</span></button>
        </form>
    </section>
    <aside class="panel next-steps">
        <span class="section-overline">QUY TRÌNH CHUẨN RDS</span>
        <h2>Các bước mua xe</h2>
        <ol class="step-list">
            <li>
                <span>1</span>
                <div>
                    <strong>Tư vấn &amp; Lập đơn (Sales Staff)</strong>
                    <p>Sales Staff tiếp nhận yêu cầu, chọn xe vật lý có sẵn (AVAILABLE) và tạo đơn hàng.</p>
                </div>
            </li>
            <li>
                <span>2</span>
                <div>
                    <strong>Đặt cọc 30% (VietQR OCB)</strong>
                    <p>Khách hàng quét mã VietQR OCB động đúng 30% giá trị đơn. Kế toán xác nhận tiền vào tài khoản.</p>
                </div>
            </li>
            <li>
                <span>3</span>
                <div>
                    <strong>Ký Hợp đồng OTP &amp; Thanh toán 70%</strong>
                    <p>Khách hàng ký hợp đồng bằng OTP, thanh toán 70% còn lại (hoặc ngân hàng đối tác giải ngân 70%), nhận Hóa đơn &amp; Bàn giao xe.</p>
                </div>
            </li>
        </ol>
    </aside>
</div>

<section class="panel table-panel" style="margin-top:24px;">
    <div class="panel-heading">
        <div>
            <span class="section-overline">THEO DÕI YÊU CẦU TƯ VẤN</span>
            <h2>Lịch sử trao đổi tư vấn với Sales Staff</h2>
        </div>
    </div>
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>MÃ YC</th>
                    <th>NGÀY GỬI</th>
                    <th>MẪU XE</th>
                    <th>NV PHỤ TRÁCH</th>
                    <th>TRẠNG THÁI</th>
                    <th>NỘI DUNG TRAO ĐỔI &amp; PHẢN HỒI</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${not empty consultations}">
                        <c:forEach var="cr" items="${consultations}">
                            <tr>
                                <td><strong>#<c:out value="${cr.id}"/></strong></td>
                                <td><c:out value="${cr.createdAt}"/></td>
                                <td><c:out value="${cr.modelName}" default="Tư vấn chung"/></td>
                                <td><c:out value="${cr.staffName}" default="Chờ phân công"/></td>
                                <td><span class="soft-badge"><c:out value="${cr.status}"/></span></td>
                                <td style="min-width:320px;">
                                    <div style="white-space:pre-line;font-size:13px;margin-bottom:8px;"><c:out value="${cr.content}"/></div>
                                    <c:if test="${cr.status == 'PENDING' or cr.status == 'IN_PROGRESS'}">
                                        <form method="post" action="<%= customerContext %>/consultation" style="display:flex;gap:6px;">
                                            <input type="hidden" name="action" value="reply">
                                            <input type="hidden" name="requestId" value="${cr.id}">
                                            <input type="text" name="message" maxlength="300" placeholder="Gửi phản hồi bổ sung cho Sales..." required style="flex:1;padding:6px 10px;border:1px solid #cbd5e1;border-radius:6px;font-size:12px;">
                                            <button type="submit" class="button button-secondary" style="padding:6px 12px;font-size:12px;">Gửi</button>
                                        </form>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td colspan="6">
                                <div class="table-empty">
                                    <span>✧</span>
                                    <strong>Chưa có yêu cầu tư vấn nào</strong>
                                    <p>Hãy gửi yêu cầu tư vấn ở biểu mẫu phía trên để bắt đầu.</p>
                                </div>
                            </td>
                        </tr>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
</section>
<%@ include file="../common/customer-shell-end.jspf" %>
