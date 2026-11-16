<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.electriccarsales.model.SalesPreviewKind,com.electriccarsales.service.SalesPreviewService" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<% request.setAttribute("customerTitle", "Hợp đồng & Chính sách Trả góp"); request.setAttribute("customerEyebrow", "HỒ SƠ GIAO DỊCH"); %>
<%@ include file="../common/customer-shell.jspf" %>
<% request.setAttribute("wordPreviewEnabled", SalesPreviewService.isAvailable(SalesPreviewKind.CONTRACT)); %>
<div class="page-intro">
    <p>Xem hợp đồng mua bán xe của bạn, thực hiện ký hợp đồng điện tử bằng mã OTP (RDS 9.2) và tra cứu chính sách lãi suất tham khảo của các ngân hàng đối tác (RDS UC-12).</p>
    <span class="soft-badge">Xác thực ký hợp đồng qua OTP</span>
</div>

<c:if test="${not empty param.otpSent}">
    <div class="info-note" style="margin-bottom:16px;border-color:#93c5fd;background:#eff6ff;color:#1e3a8a;">
        <span>✓</span>
        <p>Đã phát hành mã OTP 6 chữ số để ký Hợp đồng #<c:out value="${param.otpSent}"/>. Vui lòng kiểm tra email của bạn (hoặc xem trong Console của IntelliJ khi chạy local) và nhập mã OTP vào ô bên dưới.</p>
    </div>
</c:if>
<c:if test="${not empty param.signed}">
    <div class="info-note" style="margin-bottom:16px;border-color:#86efac;background:#f0fdf4;color:#166534;">
        <span>✓</span>
        <p>Hợp đồng #<c:out value="${param.signed}"/> đã được ký xác nhận OTP thành công (Trạng thái: <strong>SIGNED</strong>)! Bạn có thể tiếp tục bước thanh toán 70% còn lại.</p>
    </div>
</c:if>
<c:if test="${not empty contractError}">
    <div class="info-note" style="margin-bottom:16px;border-color:#fca5a5;background:#fef2f2;color:#991b1b;">
        <span>!</span>
        <p><c:out value="${contractError}"/></p>
    </div>
</c:if>

<section class="panel document-panel" style="margin-bottom:24px;">
    <div class="panel-heading">
        <div>
            <span class="section-overline">HỢP ĐỒNG MUA BÁN XE CỦA BẠN (BẢNG CONTRACTS)</span>
            <h2>Danh sách hợp đồng (<c:out value="${not empty contractList ? contractList.size() : 0}"/>)</h2>
        </div>
        <a class="text-link" href="<%= customerContext %>/orders">Xem đơn hàng <span>→</span></a>
    </div>
    <c:choose>
        <c:when test="${not empty contractList}">
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>SỐ HỢP ĐỒNG</th>
                            <th>ĐƠN HÀNG</th>
                            <th>XE &amp; VIN</th>
                            <th>GIÁ TRỊ HỢP ĐỒNG</th>
                            <th>CẤU TRÚC THANH TOÁN &amp; ĐIỀU KHOẢN</th>
                            <th>TRẠNG THÁI</th>
                            <th>KÝ XÁC THỰC OTP (RDS 9.2)</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="ct" items="${contractList}">
                            <tr>
                                <td>
                                    <strong><c:out value="${ct.contractNumber}"/></strong>
                                    <small style="display:block;color:#64748b;"><c:out value="${ct.contractDate}"/></small>
                                </td>
                                <td>#<c:out value="${ct.orderID}"/></td>
                                <td>
                                    <strong><c:out value="${ct.modelName}" default="Xe VinFast"/></strong>
                                    <small style="display:block;color:#64748b;"><c:out value="${ct.versionName}"/> · VIN: <c:out value="${ct.vin}"/></small>
                                </td>
                                <td><strong><fmt:formatNumber value="${ct.contractAmount}" pattern="#,###"/> ₫</strong></td>
                                <td style="font-size:12px;max-width:300px;"><c:out value="${ct.note}"/></td>
                                <td><span class="soft-badge"><c:out value="${ct.status}"/></span></td>
                                <td style="min-width:240px;">
                                    <c:choose>
                                        <c:when test="${ct.status == 'PENDING_SIGNATURE'}">
                                            <div style="font-size:11px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:6px;padding:8px 10px;margin-bottom:8px;line-height:1.5;">
                                                <strong style="display:block;color:#0f172a;margin-bottom:4px;">👤 Thông tin người mua:</strong>
                                                <div>• Họ tên: <strong><c:out value="${ct.customerName}"/></strong></div>
                                                <div>• SĐT: <strong><c:out value="${ct.customerPhone}"/></strong></div>
                                                <c:if test="${not empty ct.customerEmail}"><div>• Email: <c:out value="${ct.customerEmail}"/></div></c:if>
                                                <c:if test="${not empty ct.customerAddress}"><div>• Địa chỉ: <c:out value="${ct.customerAddress}"/></div></c:if>
                                                <small style="color:#64748b;display:block;margin-top:4px;">Nếu thông tin sai, báo Sales sửa qua <a href="<%= customerContext %>/sales-chat">Trò chuyện</a> trước khi ký.</small>
                                            </div>
                                            <c:if test="${ct.status eq 'PENDING_SIGNATURE'}">
                                                <c:url var="previewUrl" value="/customer/sales-document">
                                                    <c:param name="kind" value="contract"/>
                                                    <c:param name="view" value="html"/>
                                                    <c:param name="orderId" value="${ct.orderID}"/>
                                                </c:url>
                                                <p style="margin:4px 0 8px;"><a href="<c:out value='${previewUrl}'/>" style="font-weight:600;">📄 Xem đầy đủ hợp đồng &gt;</a></p>
                                            </c:if>
                                            <form method="post" action="<%= customerContext %>/contract" style="margin-bottom:8px;">
                                                <input type="hidden" name="action" value="requestOtp">
                                                <label><input type="checkbox" name="agreeToSign" value="true" required> Tôi đã kiểm tra thông tin và đồng ý ký HĐ.</label>
                                                <input type="hidden" name="csrfToken" value="<c:out value='${contractCsrf}'/>">
                                                <input type="hidden" name="contractId" value="${ct.contractID}">
                                                <button type="submit" class="button button-secondary" style="padding:5px 10px;font-size:12px;">1. Gửi mã OTP ký HĐ</button>
                                            </form>
                                            <form method="post" action="<%= customerContext %>/contract" style="display:flex;gap:6px;">
                                                <input type="hidden" name="action" value="sign">
                                                <input type="hidden" name="csrfToken" value="<c:out value='${contractCsrf}'/>">
                                                <input type="hidden" name="contractId" value="${ct.contractID}">
                                                <input type="text" name="otp" pattern="[0-9]{6}" maxlength="6" placeholder="Nhập 6 số OTP" required style="width:120px;padding:6px 8px;border:1px solid #cbd5e1;border-radius:6px;font-size:12px;">
                                                <button type="submit" class="button button-primary" style="padding:6px 12px;font-size:12px;">2. Ký HĐ</button>
                                            </form>
                                        </c:when>
                                        <c:when test="${ct.status == 'SIGNED'}">
                                            <div style="display:flex;flex-direction:column;gap:6px;">
                                                <span style="color:#15803d;font-weight:700;font-size:12px;">✓ Đã ký xác thực OTP</span>
                                                <c:url var="viewSignedHtml" value="/customer/sales-document">
                                                    <c:param name="kind" value="contract"/>
                                                    <c:param name="orderId" value="${ct.orderID}"/>
                                                    <c:param name="view" value="html"/>
                                                </c:url>
                                                <c:url var="downloadSignedDoc" value="/customer/sales-document">
                                                    <c:param name="kind" value="contract"/>
                                                    <c:param name="orderId" value="${ct.orderID}"/>
                                                </c:url>
                                                <div style="display:flex;gap:6px;flex-wrap:wrap;">
                                                    <a class="button button-secondary" style="padding:4px 8px;font-size:11px;" href="<c:out value='${viewSignedHtml}'/>">👁️ Xem hợp đồng</a>
                                                    <a class="button button-primary" style="padding:4px 8px;font-size:11px;" href="<c:out value='${downloadSignedDoc}'/>">📥 Tải Word</a>
                                                </div>
                                                <a class="text-link" style="display:block;margin-top:4px;font-size:12px;" href="<%= customerContext %>/orders">Mở đơn hàng để thanh toán 70% còn lại →</a>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <span><c:out value="${ct.status}"/></span>
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
            <div class="document-empty">
                <strong>Chưa có hợp đồng mua bán xe</strong>
                <p>Hợp đồng sẽ được Nhân viên Sales khởi tạo ngay sau khi đơn hàng của bạn hoàn tất xác nhận đặt cọc 30% (DEPOSITED).</p>
                <c:if test="${not empty customerOrders}">
                    <div style="margin-top:16px;text-align:left;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:12px 16px;">
                        <strong style="font-size:13px;color:#0f172a;display:block;margin-bottom:8px;">Trạng thái đơn hàng của bạn:</strong>
                        <c:forEach var="ord" items="${customerOrders}">
                            <div style="font-size:12px;padding:6px 0;border-bottom:1px dashed #e2e8f0;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:6px;">
                                <span><strong>Đơn hàng #${ord.orderID}</strong> · Tổng tiền: <fmt:formatNumber value="${ord.totalAmount}" pattern="#,###"/> ₫ (Đã cọc: <fmt:formatNumber value="${ord.depositAmount}" pattern="#,###"/> ₫)</span>
                                <span style="color:#0369a1;font-weight:600;">
                                    <c:choose>
                                        <c:when test="${ord.orderStatusID eq 3 or ord.depositAmount gt 0}">
                                            ⏳ Đã cọc 30% — Sales đang lập Hợp đồng mua bán điện tử cho bạn
                                        </c:when>
                                        <c:otherwise>
                                            Chưa thanh toán cọc 30% — <a href="<%= customerContext %>/orders">Vào thanh toán cọc</a>
                                        </c:otherwise>
                                    </c:choose>
                                </span>
                            </div>
                        </c:forEach>
                    </div>
                </c:if>
                <a class="text-link" style="display:inline-block;margin-top:12px;" href="<%= customerContext %>/orders">Xem tiến độ đơn hàng <span>→</span></a>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<c:if test="${not empty installmentPlans}">
<section class="panel document-panel">
    <div class="panel-heading">
        <div>
            <span class="section-overline">THAM KHẢO ĐỐI TÁC TÀI CHÍNH (RDS UC-12 &amp; BR9–BR10)</span>
            <h2>Bảng lãi suất và kỳ hạn trả góp tham khảo</h2>
        </div>
    </div>
    <p style="font-size:13px;color:#475569;margin-bottom:12px;">
        Theo RDS (BR2a &amp; EX-08): Khi chọn mua trả góp, khách hàng đóng <strong>30% tiền đặt cọc</strong> qua hệ thống/đại lý; phần <strong>70% còn lại</strong> do ngân hàng đối tác giải ngân một lần trực tiếp cho đại lý. Các kỳ trả góp hàng tháng được khách hàng thanh toán trực tiếp với ngân hàng đối tác.
    </p>
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>ĐƠN VỊ TÀI CHÍNH</th>
                    <th>GÓI THAM KHẢO</th>
                    <th>KỲ HẠN VAY</th>
                    <th>LÃI SUẤT NĂM (BR10)</th>
                    <th>GHI CHÚ CHÍNH SÁCH</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="plan" items="${installmentPlans}">
                    <tr>
                        <td><strong><c:out value="${plan.companyName}"/></strong></td>
                        <td><c:out value="${plan.planName}"/></td>
                        <td><c:out value="${plan.loanTermMonths}"/> tháng</td>
                        <td><strong><c:out value="${plan.interestRate}"/>%/năm</strong></td>
                        <td><c:out value="${plan.description}" default="—"/></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</section>
</c:if>
<%@ include file="../common/customer-shell-end.jspf" %>
