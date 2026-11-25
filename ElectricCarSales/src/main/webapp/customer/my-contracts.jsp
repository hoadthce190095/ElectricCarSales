<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.electriccarsales.model.SalesPreviewKind,com.electriccarsales.service.SalesPreviewService" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<%
    // Tự động chuyển hướng nếu người dùng truy cập trực tiếp file JSP thay vì qua Controller
    if (request.getAttribute("contractList") == null && request.getAttribute("installmentContract") == null) {
        response.sendRedirect(request.getContextPath() + "/contract");
        return;
    }
%>
<% request.setAttribute("customerTitle", "Hợp đồng & Chính sách Trả góp"); request.setAttribute("customerEyebrow", "HỒ SƠ GIAO DỊCH"); %>
<%@ include file="../common/customer-shell.jspf" %>
<% request.setAttribute("wordPreviewEnabled", SalesPreviewService.isAvailable(SalesPreviewKind.CONTRACT)); %>
<div class="page-intro" style="margin-bottom:20px;">
    <div>
        <p style="margin:0 0 6px;font-size:13.5px;color:#334155;line-height:1.5;">
            Xem hợp đồng mua bán xe điện của bạn, kiểm tra thông tin pháp lý, thực hiện <strong>ký hợp đồng điện tử bằng mã OTP</strong> và tra cứu chính sách trả góp ngân hàng đối tác.
        </p>
        <small style="color:#64748b;">Khách hàng chỉ nhận OTP và ký hợp đồng sau khi Kế toán xác nhận đối soát đầy đủ khoản cọc 30%.</small>
    </div>
    <div style="margin-left:auto;display:flex;gap:8px;flex-wrap:wrap;">
        <a class="button button-secondary" href="<%= customerContext %>/orders" style="padding:7px 14px;font-size:12.5px;">Xem đơn hàng <span>→</span></a>
        <a class="button button-primary" href="<%= customerContext %>/payment/history" style="padding:7px 14px;font-size:12.5px;">Quản lý thanh toán <span>→</span></a>
    </div>
</div>

<c:if test="${not empty param.otpSent}">
    <div class="info-note" style="margin-bottom:16px;border-color:#93c5fd;background:#eff6ff;color:#1e3a8a;">
        <span>✓</span>
        <p>Đã phát hành mã OTP 6 chữ số để ký Hợp đồng #<c:out value="${param.otpSent}"/>. Vui lòng kiểm tra email của bạn (hoặc console server local) và nhập mã OTP vào ô bên dưới.</p>
    </div>
</c:if>
<c:if test="${not empty param.signed}">
    <div class="info-note" style="margin-bottom:16px;border-color:#86efac;background:#f0fdf4;color:#166534;">
        <span>✓</span>
        <p>Hợp đồng #<c:out value="${param.signed}"/> đã được ký xác nhận OTP thành công (Trạng thái: <strong>SIGNED</strong>). Với đơn trả góp, đối tác giải ngân 70% sau bước này; với đơn trả thẳng, bạn tiếp tục thanh toán 70% trong mục Thanh toán.</p>
    </div>
</c:if>
<c:if test="${not empty contractError}">
    <div class="info-note" style="margin-bottom:16px;border-color:#fca5a5;background:#fef2f2;color:#991b1b;">
        <span>!</span>
        <p><c:out value="${contractError}"/></p>
    </div>
</c:if>

<section class="panel document-panel" style="margin-bottom:24px;">
    <div class="panel-heading" style="padding:18px 22px;border-bottom:1px solid #f1f5f9;">
        <div>
            <span class="section-overline" style="color:#2563eb;font-weight:700;font-size:11px;">HỢP ĐỒNG MUA BÁN XE CỦA BẠN (BẢNG CONTRACTS)</span>
            <h2 style="font-size:17px;margin:3px 0 0;color:#0f172a;">Danh sách hợp đồng (<c:out value="${not empty contractList ? contractList.size() : 0}"/>)</h2>
        </div>
        <div>
            <a class="text-link" href="<%= customerContext %>/orders" style="font-size:12px;font-weight:600;">Xem đơn hàng <span>→</span></a>
        </div>
    </div>

    <c:choose>
        <c:when test="${not empty contractList}">
            <div class="table-wrap">
                <table class="customer-contract-table">
                    <thead>
                        <tr>
                            <th>SỐ HỢP ĐỒNG</th>
                            <th>ĐƠN HÀNG</th>
                            <th>XE &amp; VIN</th>
                            <th>GIÁ TRỊ HỢP ĐỒNG</th>
                            <th>CẤU TRÚC THANH TOÁN &amp; ĐIỀU KHOẢN</th>
                            <th>TRẠNG THÁI</th>
                            <th>THAO TÁC &amp; KÝ OTP</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="ct" items="${contractList}">
                            <tr>
                                <td>
                                    <strong style="color:#0f172a;font-size:13px;"><c:out value="${ct.contractNumber}"/></strong>
                                    <small style="display:block;color:#64748b;font-size:11px;margin-top:2px;"><c:out value="${ct.contractDate}"/></small>
                                </td>
                                <td>
                                    <a href="<%= customerContext %>/orders" style="font-weight:700;color:#2563eb;text-decoration:none;">
                                        #<c:out value="${ct.orderID}"/>
                                    </a>
                                </td>
                                <td>
                                    <strong style="color:#0f172a;"><c:out value="${ct.modelName}" default="Xe VinFast"/></strong>
                                    <small style="display:block;color:#64748b;font-size:11.5px;"><c:out value="${ct.versionName}"/> · VIN: <c:out value="${ct.vin}" default="Đang gán VIN"/></small>
                                </td>
                                <td>
                                    <strong style="color:#0f172a;font-size:14px;"><fmt:formatNumber value="${ct.contractAmount}" pattern="#,##0"/> VND</strong>
                                </td>
                                <td class="contract-terms-cell">
                                    <c:out value="${ct.note}"/>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${ct.status eq 'SIGNED'}">
                                            <span class="status-badge badge-green"><span class="dot"></span> ĐÃ KÝ OTP</span>
                                        </c:when>
                                        <c:when test="${ct.status eq 'PENDING_SIGNATURE'}">
                                            <span class="status-badge badge-amber"><span class="dot"></span> CHỜ KÝ OTP</span>
                                        </c:when>
                                        <c:when test="${ct.status eq 'CANCELLED'}">
                                            <span class="status-badge badge-red"><span class="dot"></span> ĐÃ HỦY</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="status-badge badge-neutral"><span class="dot"></span> <c:out value="${ct.status}"/></span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="contract-actions-cell">
                                    <c:choose>
                                        <c:when test="${ct.status == 'PENDING_SIGNATURE'}">
                                            <c:url var="previewHtmlUrl" value="/customer/sales-document">
                                                <c:param name="kind" value="contract"/>
                                                <c:param name="view" value="html"/>
                                                <c:param name="orderId" value="${ct.orderID}"/>
                                            </c:url>
                                            <c:url var="downloadDocxUrl" value="/customer/sales-document">
                                                <c:param name="kind" value="contract"/>
                                                <c:param name="orderId" value="${ct.orderID}"/>
                                            </c:url>

                                            <!-- Nút xem hợp đồng nổi bật -->
                                            <div class="doc-btn-group" style="margin-bottom:10px;">
                                                <a class="btn-doc btn-doc-primary" href="<c:out value='${previewHtmlUrl}'/>">👁️ Xem hợp đồng</a>
                                                <a class="btn-doc btn-doc-outline" href="<c:out value='${downloadDocxUrl}'/>">📥 Tải Word</a>
                                            </div>

                                            <div style="font-size:11px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:6px;padding:8px 10px;margin-bottom:8px;line-height:1.5;">
                                                <strong style="display:block;color:#0f172a;margin-bottom:4px;">👤 Thông tin bên mua (Kiểm tra trước khi ký):</strong>
                                                <div>• Họ tên: <strong><c:out value="${ct.customerName}"/></strong></div>
                                                <div>• SĐT: <strong><c:out value="${ct.customerPhone}"/></strong></div>
                                                <c:if test="${not empty ct.customerEmail}"><div>• Email: <c:out value="${ct.customerEmail}"/></div></c:if>
                                                <c:if test="${not empty ct.customerAddress}"><div>• Địa chỉ: <c:out value="${ct.customerAddress}"/></div></c:if>
                                                <small style="color:#64748b;display:block;margin-top:4px;">Nếu thông tin chưa chính xác, vui lòng liên hệ Sales trước khi ký.</small>
                                            </div>

                                            <c:choose>
                                                <c:when test="${ct.depositReconciled}">
                                            <!-- Form ký hợp đồng bằng OTP -->
                                            <form method="post" action="<%= customerContext %>/contract" class="form-request-contract-otp" style="margin-bottom:8px;" data-contract-id="${ct.contractID}" data-contract-num="${ct.contractNumber}" data-amount="<fmt:formatNumber value='${ct.contractAmount}' pattern='#,##0'/> VND">
                                                <input type="hidden" name="action" value="requestOtp">
                                                <label style="display:flex;align-items:center;gap:6px;font-size:11.5px;color:#1e293b;cursor:pointer;">
                                                    <input type="checkbox" name="agreeToSign" value="true" required> 
                                                    Tôi đã kiểm tra kỹ thông tin và đồng ý ký HĐ.
                                                </label>
                                                <input type="hidden" name="csrfToken" value="<c:out value='${contractCsrf}'/>">
                                                <input type="hidden" name="contractId" value="${ct.contractID}">
                                                <button type="submit" class="button button-secondary" style="padding:5px 10px;font-size:11.5px;margin-top:4px;">
                                                    1. Gửi mã OTP ký HĐ
                                                </button>
                                            </form>
                                            <form method="post" action="<%= customerContext %>/contract" class="form-sign-contract-otp" style="display:flex;gap:6px;align-items:center;" data-contract-id="${ct.contractID}" data-contract-num="${ct.contractNumber}">
                                                <input type="hidden" name="action" value="sign">
                                                <input type="hidden" name="csrfToken" value="<c:out value='${contractCsrf}'/>">
                                                <input type="hidden" name="contractId" value="${ct.contractID}">
                                                <input type="text" name="otp" pattern="[0-9]{6}" maxlength="6" placeholder="Nhập 6 số OTP" required style="width:115px;padding:5px 8px;border:1px solid #cbd5e1;border-radius:6px;font-size:12px;">
                                                <button type="submit" class="button button-primary" style="padding:5px 12px;font-size:11.5px;">
                                                    2. Ký HĐ
                                                </button>
                                            </form>
                                                </c:when>
                                                <c:otherwise>
                                                    <div style="border:1px solid #fde68a;background:#fffbeb;color:#92400e;border-radius:6px;padding:9px 11px;font-size:12px;line-height:1.5;">
                                                        Chức năng ký hợp đồng sẽ mở sau khi Kế toán xác nhận đối soát đầy đủ khoản cọc 30%.
                                                    </div>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:when>

                                        <c:when test="${ct.status == 'SIGNED'}">
                                            <c:url var="viewSignedHtml" value="/customer/sales-document">
                                                <c:param name="kind" value="contract"/>
                                                <c:param name="orderId" value="${ct.orderID}"/>
                                                <c:param name="view" value="html"/>
                                            </c:url>
                                            <c:url var="downloadSignedDoc" value="/customer/sales-document">
                                                <c:param name="kind" value="contract"/>
                                                <c:param name="orderId" value="${ct.orderID}"/>
                                            </c:url>
                                            <div style="display:flex;flex-direction:column;gap:6px;">
                                                <span style="color:#15803d;font-weight:700;font-size:12px;display:flex;align-items:center;gap:4px;">
                                                    ✓ Đã ký xác thực OTP thành công
                                                </span>
                                                <div class="doc-btn-group">
                                                    <a class="btn-doc btn-doc-primary" href="<c:out value='${viewSignedHtml}'/>">👁️ Xem hợp đồng</a>
                                                    <a class="btn-doc btn-doc-outline" href="<c:out value='${downloadSignedDoc}'/>">📥 Tải Word</a>
                                                </div>
                                                <c:choose>
                                                    <c:when test="${ct.paymentMethodName eq 'INSTALLMENT'}">
                                                        <small style="color:#166534;">Khoản 70% được ghi nhận khi đại lý nhận được tiền từ đối tác.</small>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <a class="text-link" style="display:block;margin-top:4px;font-size:12px;font-weight:600;" href="<%= customerContext %>/payment/history?orderId=${ct.orderID}">
                                                            Chuyển tới thanh toán 70% còn lại →
                                                        </a>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </c:when>

                                        <c:when test="${ct.status == 'CANCELLED'}">
                                            <span class="status-badge badge-red"><span class="dot"></span> Hợp đồng đã hủy</span>
                                        </c:when>

                                        <c:otherwise>
                                            <span class="status-badge badge-neutral"><c:out value="${ct.status}"/></span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:otherwise>
            <div class="document-empty" style="text-align:center;padding:36px 16px;">
                <span style="font-size:32px;display:block;margin-bottom:8px;">📑</span>
                <strong style="font-size:15px;color:#0f172a;display:block;">Chưa có hợp đồng mua bán xe</strong>
                <p style="color:#64748b;font-size:13px;max-width:520px;margin:6px auto 16px;line-height:1.6;">
                    Theo quy trình nghiệp vụ: Hợp đồng mua bán chỉ được chuyên viên Sales khởi tạo sau khi đơn hàng của bạn đã hoàn tất đặt cọc 30% (DEPOSITED).
                </p>
                <c:if test="${not empty customerOrders}">
                    <div style="max-width:620px;margin:0 auto;text-align:left;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:14px 18px;">
                        <strong style="font-size:12.5px;color:#0f172a;display:block;margin-bottom:8px;text-transform:uppercase;">Tiến độ các đơn hàng hiện tại của bạn:</strong>
                                <c:forEach var="ord" items="${customerOrders}">
                            <div style="font-size:12px;padding:8px 0;border-bottom:1px dashed #e2e8f0;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;">
                                <span>
                                    <strong>Đơn hàng #<c:out value="${ord.id}"/></strong> 
                                    · Tổng tiền: <fmt:formatNumber value="${ord.originalAmount}" pattern="#,##0"/> VND 
                                    (Cọc 30%: <fmt:formatNumber value="${ord.depositAmount}" pattern="#,##0"/> VND)
                                </span>
                                <span>
                                    <c:choose>
                                        <c:when test="${ord.status eq 'CANCELLED'}">
                                            <span class="status-badge badge-red"><span class="dot"></span> Đã hủy<c:if test="${not empty ord.cancelReason}"> · <c:out value="${ord.cancelReason}"/></c:if></span>
                                        </c:when>
                                        <c:when test="${ord.depositPaid}">
                                            <span class="status-badge badge-blue"><span class="dot"></span> Đã cọc 30% · Chờ Sales tạo hợp đồng</span>
                                        </c:when>
                                        <c:otherwise>
                                            <a class="btn-doc btn-doc-warning" href="<%= customerContext %>/payment/transfer?orderId=${ord.id}&type=DEPOSIT">
                                                Thanh toán cọc 30% ngay →
                                            </a>
                                        </c:otherwise>
                                    </c:choose>
                                </span>
                            </div>
                        </c:forEach>
                    </div>
                </c:if>
                <div style="margin-top:16px;">
                    <a class="text-link" href="<%= customerContext %>/orders">Xem tiến độ tất cả đơn hàng <span>→</span></a>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<c:if test="${financialCompanies ne null}">
<section class="panel document-panel" style="margin-bottom:24px;">
    <div class="panel-heading" style="padding:18px 22px;border-bottom:1px solid #f1f5f9;">
        <div>
            <span class="section-overline" style="color:#059669;font-weight:700;font-size:11px;">THAM KHẢO ĐỐI TÁC TÀI CHÍNH</span>
            <h2 style="font-size:17px;margin:3px 0 0;color:#0f172a;">Đối tác tài chính</h2>
        </div>
    </div>
    <div style="padding:16px 22px;">
        <p style="font-size:13px;color:#475569;margin:0 0 14px;line-height:1.6;">
            Khi chọn mua trả góp, khách hàng đóng <strong>30% tiền đặt cọc</strong>; Kế toán cần xác nhận đối soát khoản cọc trước khi khách ký hợp đồng OTP. Sau khi ký, đối tác tài chính chuyển khoản giải ngân <strong>70% còn lại</strong> cho đại lý. Các kỳ trả góp hàng tháng được khách hàng thanh toán trực tiếp với ngân hàng đối tác.
        </p>
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>ĐƠN VỊ TÀI CHÍNH</th>
                        <th>ĐIỆN THOẠI</th>
                        <th>EMAIL</th>
                        <th>ĐỊA CHỈ</th>
                        <th>GHI CHÚ CHÍNH SÁCH</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="plan" items="${financialCompanies}">
                        <tr>
                            <td><strong><c:out value="${plan.companyName}"/></strong></td>
                            <td><c:out value="${plan.phone}"/></td>
                            <td><c:out value="${plan.email}"/></td>
                            <td><strong style="color:#15803d;"><c:out value="${plan.address}"/></strong></td>
                            <td style="color:#64748b;font-size:12px;"><c:out value="${plan.description}" default="—"/></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</section>
</c:if>

<%@ include file="../common/customer-shell-end.jspf" %>
