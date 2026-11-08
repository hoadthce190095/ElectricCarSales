<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<c:if test="${empty consultationControllerLoaded}">
    <c:redirect url="/customer/consultation"/>
</c:if>
<%
    request.setAttribute("customerTitle", "Yêu cầu tư vấn & Báo giá");
    request.setAttribute("customerEyebrow", "TƯ VẤN & TIẾP NHẬN NHU CẦU");
%>
<%@ include file="../common/customer-shell.jspf" %>

<c:if test="${param.success eq 'true'}">
    <div class="checkout-notice" style="border-color: #b7eb8f; background: #f6ffed; color: #1f7a4c; margin-bottom: 18px;">
        <span style="background: #23875f; color: #fff;">✓</span>
        <div>
            <strong>Gửi yêu cầu tư vấn thành công!</strong>
            <p>Yêu cầu của bạn đã được lưu vào hệ thống. Nhân viên kinh doanh VinFast sẽ liên hệ hỗ trợ và báo giá sớm nhất.</p>
        </div>
    </div>
</c:if>
<c:if test="${param.error eq 'true'}">
    <div class="checkout-notice" style="border-color: #ffa39e; background: #fff1f0; color: #cf1322; margin-bottom: 18px;">
        <span style="background: #e31837; color: #fff;">!</span>
        <div>
            <strong>Không thể gửi yêu cầu</strong>
            <p>Vui lòng kiểm tra lại thông tin liên hệ và thử lại.</p>
        </div>
    </div>
</c:if>

<div class="checkout-layout" style="margin-bottom: 24px;">
    <section class="panel checkout-panel">
        <div class="panel-heading">
            <div>
                <span class="section-overline">ĐĂNG KÝ NHẬN TƯ VẤN</span>
                <h2>Để lại nhu cầu mua xe của bạn</h2>
            </div>
            <span class="step-indicator">01 <i>/</i> 01</span>
        </div>
        <form class="customer-form" action="${pageContext.request.contextPath}/customer/consultation" method="POST">
            <c:if test="${not empty loggedCustomer}">
                <input type="hidden" name="customerId" value="${loggedCustomer.customerID}">
            </c:if>
            <label>Họ và tên khách hàng
                <input name="fullName" autocomplete="name" value="${not empty loggedCustomer ? loggedCustomer.customerName : ''}" placeholder="Nhập họ và tên của bạn" required>
            </label>
            <div class="form-row">
                <label>Số điện thoại liên hệ
                    <input name="phone" type="tel" autocomplete="tel" value="${not empty loggedCustomer ? loggedCustomer.phone : ''}" placeholder="090 xxx xxxx" required>
                </label>
                <label>Email <span class="optional">Không bắt buộc</span>
                    <input name="email" type="email" autocomplete="email" value="${not empty loggedCustomer ? loggedCustomer.email : ''}" placeholder="khachhang@email.com">
                </label>
            </div>
            <label>Mẫu xe VinFast quan tâm
                <select name="carModel" required>
                    <option value="">-- Chọn mẫu xe điện VinFast --</option>
                    <c:forEach var="cm" items="${carModels}">
                        <option value="${cm.modelId}">${cm.modelName}</option>
                    </c:forEach>
                </select>
            </label>
            <label>Nội dung cần tư vấn
                <textarea name="content" rows="4" placeholder="Chia sẻ phiên bản xe, màu sắc, chính sách trả góp hoặc lịch lái thử bạn mong muốn..." required></textarea>
            </label>
            <label class="consent-row">
                <input type="checkbox" checked required>
                <span>Tôi đồng ý để đội ngũ tư vấn VinFast liên hệ theo thông tin đã cung cấp.</span>
            </label>
            <button class="button button-primary" type="submit">Gửi yêu cầu tư vấn <span>→</span></button>
            <p class="form-footnote">Yêu cầu được lưu trực tiếp vào cơ sở dữ liệu hệ thống để nhân viên kinh doanh tiếp nhận và tạo giao dịch bán xe.</p>
        </form>
    </section>

    <aside class="panel next-steps">
        <span class="section-overline">QUY TRÌNH BÁN HÀNG</span>
        <h2>Các bước tiếp theo</h2>
        <ol class="step-list">
            <li>
                <span>1</span>
                <div>
                    <strong>Tiếp nhận nhu cầu</strong>
                    <p>Yêu cầu tư vấn được ghi nhận với trạng thái <em>Chưa tiếp nhận</em>.</p>
                </div>
            </li>
            <li>
                <span>2</span>
                <div>
                    <strong>Nhân viên tư vấn liên hệ</strong>
                    <p>Nhân viên kinh doanh cập nhật trạng thái <em>Đang xử lý</em> / <em>Đã liên hệ</em> và chốt mẫu xe, màu sắc, phương thức thanh toán.</p>
                </div>
            </li>
            <li>
                <span>3</span>
                <div>
                    <strong>Khởi tạo đơn hàng &amp; giao dịch</strong>
                    <p>Nhân viên lập giao dịch bán xe (Order &amp; OrderDetail) trực tiếp từ yêu cầu tư vấn của bạn.</p>
                </div>
            </li>
        </ol>
        <div class="support-card">
            <span>✳</span>
            <div>
                <strong>Theo dõi đơn hàng</strong>
                <p>Sau khi nhân viên tạo giao dịch, bạn có thể kiểm tra tiến độ tại mục Đơn hàng.</p>
                <a href="${pageContext.request.contextPath}/orders">Xem đơn hàng của tôi →</a>
            </div>
        </div>
    </aside>
</div>

<section class="panel table-panel">
    <div class="panel-heading">
        <div>
            <span class="section-overline">DANH SÁCH ĐÃ GỬI</span>
            <h2>Lịch sử yêu cầu tư vấn</h2>
        </div>
        <label class="search-field">
            <input type="search" id="consultSearchInput" placeholder="Tìm theo mẫu xe, nội dung..." aria-label="Tìm yêu cầu tư vấn">
        </label>
    </div>
    <div class="table-wrap">
        <table id="customerConsultTable">
            <thead>
                <tr>
                    <th>MÃ YÊU CẦU</th>
                    <th>KHÁCH HÀNG</th>
                    <th>MẪU XE QUAN TÂM</th>
                    <th>NỘI DUNG TƯ VẤN</th>
                    <th>NGÀY GỬI</th>
                    <th>NHÂN VIÊN PHỤ TRÁCH</th>
                    <th>TRẠNG THÁI</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${not empty consultations}">
                        <c:forEach var="item" items="${consultations}">
                            <tr>
                                <td><strong>#YC-${item.id}</strong></td>
                                <td>
                                    <strong>${empty item.customerName ? 'Khách hàng' : item.customerName}</strong>
                                    <c:if test="${not empty item.customerPhone}">
                                        <div style="font-size: 12px; color: #6c727f;">${item.customerPhone}</div>
                                    </c:if>
                                </td>
                                <td><strong>${empty item.modelName ? 'Chưa chọn mẫu' : item.modelName}</strong></td>
                                <td style="max-width: 320px; white-space: normal;">${item.content}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty item.createdAt}">
                                            <fmt:formatDate value="${item.createdAt}" pattern="dd/MM/yyyy HH:mm"/>
                                        </c:when>
                                        <c:otherwise>-</c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${empty item.employeeName ? 'Chờ phân công' : item.employeeName}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${item.statusId == 1}">
                                            <span class="status-pill warning">${empty item.statusName ? 'Chưa tiếp nhận' : item.statusName}</span>
                                        </c:when>
                                        <c:when test="${item.statusId == 2}">
                                            <span class="status-pill info">${empty item.statusName ? 'Đang xử lý' : item.statusName}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="status-pill success">${empty item.statusName ? 'Đã liên hệ' : item.statusName}</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td colspan="7" style="text-align: center; padding: 2.5rem; color: #8f96a3;">
                                Chưa có yêu cầu tư vấn nào được ghi nhận trong hệ thống.
                            </td>
                        </tr>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
    <div class="table-foot">
        <span>Đang hiển thị <strong>${empty consultations ? 0 : consultations.size()}</strong> yêu cầu tư vấn</span>
    </div>
</section>

<script>
    document.getElementById('consultSearchInput')?.addEventListener('input', function () {
        const kw = this.value.trim().toLowerCase();
        document.querySelectorAll('#customerConsultTable tbody tr').forEach(tr => {
            if (tr.querySelectorAll('td').length <= 1) return;
            tr.style.display = tr.textContent.toLowerCase().includes(kw) ? '' : 'none';
        });
    });
</script>
<%@ include file="../common/customer-shell-end.jspf" %>
