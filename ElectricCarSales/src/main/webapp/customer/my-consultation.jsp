<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<% request.setAttribute("customerTitle", "Tư vấn xe & Trao đổi"); request.setAttribute("customerEyebrow", "HỖ TRỢ KHÁCH HÀNG"); %>
<%@ include file="../common/customer-shell.jspf" %>

<div class="page-intro">
    <p>Quản lý các cuộc trao đổi, nhận tư vấn về mẫu xe điện VinFast và nhận yêu cầu thanh toán đặt cọc từ chuyên viên Sales.</p>
    <span class="soft-badge">Trung tâm tư vấn &amp; trao đổi</span>
</div>

<%-- Thông báo thành công / lỗi --%>
<c:if test="${not empty param.updated}">
    <div class="info-note" style="margin-bottom:16px;border-color:#86efac;background:#f0fdf4;color:#166534;">
        <span>✓</span>
        <p>Phản hồi của bạn đã được gửi thành công đến chuyên viên tư vấn.</p>
    </div>
</c:if>
<c:if test="${not empty param.submitted}">
    <div class="info-note" style="margin-bottom:16px;border-color:#86efac;background:#f0fdf4;color:#166534;">
        <span>✓</span>
        <p>Đã gửi yêu cầu tư vấn thành công! Nhân viên Sales sẽ liên hệ và phản hồi sớm nhất.</p>
    </div>
</c:if>
<c:if test="${not empty error}">
    <div class="info-note" style="margin-bottom:16px;border-color:#fca5a5;background:#fef2f2;color:#991b1b;">
        <span>!</span>
        <p><c:out value="${error}"/></p>
    </div>
</c:if>

<%-- Các liên kết nhanh --%>
<div style="display:flex;gap:12px;flex-wrap:wrap;margin-bottom:20px;align-items:center;">
    <a class="text-link" href="<%= customerContext %>/orders">Xem đơn hàng <span>→</span></a>
    <a class="text-link" href="<%= customerContext %>/contract">Xem hợp đồng <span>→</span></a>
    <a class="text-link" href="<%= customerContext %>/payment">Xem thanh toán <span>→</span></a>
</div>

<%-- Form gửi yêu cầu tư vấn mới (Collapsible) --%>
<details class="panel document-panel" style="margin-bottom:24px;border:1px solid #cbd5e1;border-radius:10px;">
    <summary style="cursor:pointer;font-weight:700;color:#1e40af;padding:14px 18px;background:#f8fafc;border-radius:10px;font-size:14px;">
        ➕ Gửi yêu cầu tư vấn xe mới
    </summary>
    <div style="padding:18px;border-top:1px solid #e2e8f0;background:#fff;">
        <form class="customer-form" method="post" action="<%= customerContext %>/consultation">
            <input type="hidden" name="action" value="create">
            <c:if test="${not empty csrfToken}">
                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
            </c:if>
            <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:14px;margin-bottom:14px;">
                <div>
                    <label style="font-size:12px;font-weight:600;display:block;margin-bottom:4px;">Chọn mẫu xe quan tâm *</label>
                    <select name="modelId" required style="width:100%;padding:8px 10px;border:1px solid #cbd5e1;border-radius:6px;font-size:13px;">
                        <option value="">-- Chọn dòng xe VinFast --</option>
                        <c:forEach var="m" items="${carModels}">
                            <option value="${m.modelID}"><c:out value="${m.modelName}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div>
                    <label style="font-size:12px;font-weight:600;display:block;margin-bottom:4px;">Phương án thanh toán dự kiến</label>
                    <select name="paymentPreference" style="width:100%;padding:8px 10px;border:1px solid #cbd5e1;border-radius:6px;font-size:13px;">
                        <option value="Trả thẳng">Trả thẳng (Chuyển khoản VietQR MB Bank)</option>
                        <option value="Trả góp">Trả góp qua ngân hàng đối tác ( Shinhan, Techcombank, MB Bank, VPBank )</option>
                    </select>
                </div>
            </div>
            <div style="margin-bottom:14px;">
                <label style="font-size:12px;font-weight:600;display:block;margin-bottom:4px;">Nội dung cần tư vấn *</label>
                <textarea name="message" rows="3" required maxlength="800"
                          placeholder="Nhập yêu cầu tư vấn về giá xe, chương trình ưu đãi, phiên bản hoặc phương án vay..."
                          style="width:100%;box-sizing:border-box;padding:10px;border:1px solid #cbd5e1;border-radius:6px;font-size:13px;resize:vertical;"></textarea>
            </div>
            <button type="submit" class="button button-primary" style="font-size:13px;padding:8px 18px;">
                Gửi yêu cầu tư vấn
            </button>
        </form>
    </div>
</details>

<%-- Danh sách consultation requests & Bố cục 2 cột (Master-Detail) --%>
<c:choose>
    <c:when test="${not empty consultations}">
        <style>
            .consult-chat-wrap { display: grid; grid-template-columns: 340px 1fr; gap: 20px; align-items: start; margin-bottom: 30px; }
            .consult-thread-sidebar { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(15,23,42,0.04); }
            .consult-thread-sidebar-head { padding: 14px 18px; border-bottom: 1px solid #e2e8f0; background: #f8fafc; font-weight: 700; font-size: 14px; color: #1e293b; display: flex; justify-content: space-between; align-items: center; }
            .consult-thread-list { max-height: 720px; overflow-y: auto; }
            .consult-thread-item { padding: 14px 16px; border-bottom: 1px solid #f1f5f9; cursor: pointer; transition: all 0.15s ease; border-left: 4px solid transparent; }
            .consult-thread-item:hover { background: #f8fafc; }
            .consult-thread-item.active { background: #f0f7ff; border-left-color: #2563eb; }
            .consult-detail-container { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(15,23,42,0.04); min-height: 520px; display: flex; flex-direction: column; }
            .consult-detail-head { padding: 16px 20px; border-bottom: 1px solid #e2e8f0; background: #fbfcfd; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
            .consult-chat-flow { flex: 1; padding: 20px; display: flex; flex-direction: column; gap: 12px; overflow-y: auto; max-height: 480px; background: #f8fafc; }
            .consult-reply-box { padding: 16px 20px; border-top: 1px solid #e2e8f0; background: #fff; }
            .consult-vehicle-badge { display: flex; gap: 14px; align-items: center; padding: 12px 16px; background: #f1f5f9; border-radius: 8px; margin-bottom: 14px; }
            @media (max-width: 860px) {
                .consult-chat-wrap { grid-template-columns: 1fr; }
                .consult-thread-item.mobile-hidden { display: none; }
                .consult-mobile-back-btn { display: inline-flex !important; }
            }
            .consult-mobile-back-btn { display: none; }
        </style>

        <div class="consult-chat-wrap">
            <!-- Left Pane: Thread List -->
            <aside class="consult-thread-sidebar" id="consult-sidebar">
                <div class="consult-thread-sidebar-head">
                    <span>Cuộc trò chuyện (${consultations.size()})</span>
                    <span style="font-size:11px;color:#64748b;font-weight:400;">Chọn để xem</span>
                </div>
                <div class="consult-thread-list">
                    <c:forEach var="req" items="${consultations}" varStatus="status">
                        <div class="consult-thread-item ${status.first ? 'active' : ''}" data-target-id="chat-thread-${req.id}">
                            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;">
                                <strong style="font-size:14px;color:#1e3a8a;">
                                    #<c:out value="${req.id}"/> · <c:out value="${req.modelName}" default="Tư vấn chung"/>
                                </strong>
                                <c:choose>
                                    <c:when test="${req.status eq 'PENDING'}">
                                        <span class="soft-badge" style="background:#fef9c3;color:#854d0e;border:1px solid #fde047;font-size:10px;padding:2px 6px;">CHỜ XỬ LÝ</span>
                                    </c:when>
                                    <c:when test="${req.status eq 'IN_PROGRESS'}">
                                        <span class="soft-badge" style="background:#dbeafe;color:#1e40af;border:1px solid #93c5fd;font-size:10px;padding:2px 6px;">ĐANG TƯ VẤN</span>
                                    </c:when>
                                    <c:when test="${req.status eq 'COMPLETED'}">
                                        <span class="soft-badge" style="background:#dcfce7;color:#166534;border:1px solid #86efac;font-size:10px;padding:2px 6px;">HOÀN TẤT</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="soft-badge" style="font-size:10px;padding:2px 6px;"><c:out value="${req.status}"/></span>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                            <div style="font-size:12px;color:#64748b;margin-bottom:4px;">
                                👤 NV: <strong><c:out value="${req.staffName}" default="Đang phân công"/></strong>
                            </div>
                            <div style="font-size:12px;color:#334155;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:280px;">
                                <c:out value="${req.content}"/>
                            </div>
                            <div style="font-size:11px;color:#94a3b8;margin-top:6px;">
                                <fmt:formatDate value="${req.createdAt}" pattern="dd/MM/yyyy HH:mm"/>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </aside>

            <!-- Right Pane: Thread Detail -->
            <section class="consult-detail-container" id="consult-detail-container">
                <c:forEach var="req" items="${consultations}" varStatus="status">
                    <div id="chat-thread-${req.id}" class="consult-thread-body" style="${status.first ? '' : 'display:none;'}">
                        <!-- Header bar -->
                        <div class="consult-detail-head">
                            <div style="display:flex;align-items:center;gap:10px;">
                                <button type="button" class="button button-secondary consult-mobile-back-btn" style="padding:4px 8px;font-size:12px;">← Danh sách</button>
                                <div>
                                    <h2 style="margin:0;font-size:17px;color:#0f172a;">
                                        Yêu cầu #<c:out value="${req.id}"/>: <c:out value="${req.modelName}" default="Tư vấn xe"/>
                                    </h2>
                                    <span style="font-size:12px;color:#64748b;">
                                        Chuyên viên phụ trách: <strong><c:out value="${req.staffName}" default="Đang phân công hỗ trợ"/></strong>
                                    </span>
                                </div>
                            </div>
                            <div>
                                <c:choose>
                                    <c:when test="${req.status eq 'PENDING'}">
                                        <span class="soft-badge" style="background:#fef9c3;color:#854d0e;border:1px solid #fde047;">CHỜ XỬ LÝ</span>
                                    </c:when>
                                    <c:when test="${req.status eq 'IN_PROGRESS'}">
                                        <span class="soft-badge" style="background:#dbeafe;color:#1e40af;border:1px solid #93c5fd;">ĐANG XỬ LÝ</span>
                                    </c:when>
                                    <c:when test="${req.status eq 'COMPLETED'}">
                                        <span class="soft-badge" style="background:#dcfce7;color:#166534;border:1px solid #86efac;">ĐÃ HOÀN TẤT</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="soft-badge"><c:out value="${req.status}"/></span>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>

                        <!-- Vehicle Spec Card (if matching model in catalogue) -->
                        <c:set var="matchedVehicle" value="${null}"/>
                        <c:forEach var="v" items="${catalogVehicles}">
                            <c:if test="${empty matchedVehicle and ((req.modelId ne null and v.modelID eq req.modelId) or (not empty req.modelName and v.modelName eq req.modelName))}">
                                <c:set var="matchedVehicle" value="${v}"/>
                            </c:if>
                        </c:forEach>
                        <c:if test="${not empty matchedVehicle}">
                            <div class="consult-vehicle-badge" style="margin:12px 20px 0 20px;">
                                <c:if test="${not empty matchedVehicle.imageUrl}">
                                    <img src="<c:out value='${matchedVehicle.imageUrl}'/>" alt="${matchedVehicle.modelName}" style="width:100px;height:65px;object-fit:cover;border-radius:6px;background:#fff;">
                                </c:if>
                                <div style="flex:1;">
                                    <div style="display:flex;justify-content:space-between;align-items:center;">
                                        <strong style="font-size:15px;color:#1e3a8a;"><c:out value="${matchedVehicle.modelName}"/> · <c:out value="${matchedVehicle.specificationVersion}"/></strong>
                                        <a href="<%= request.getContextPath() %>/detail?id=${matchedVehicle.modelID}" target="_blank" class="text-link" style="font-size:12px;">Xem chi tiết xe ↗</a>
                                    </div>
                                    <div style="font-size:12px;color:#475569;margin-top:4px;display:flex;gap:14px;flex-wrap:wrap;">
                                        <span>Giá tham khảo: <strong style="color:#0f172a;"><fmt:formatNumber value="${matchedVehicle.priceVnd}" type="currency" currencySymbol="₫"/></strong></span>
                                        <c:if test="${not empty matchedVehicle.batteryKwh}"><span>Pin: <strong>${matchedVehicle.batteryKwh} kWh</strong></span></c:if>
                                        <c:if test="${not empty matchedVehicle.rangeKm}"><span>Quãng đường: <strong>${matchedVehicle.rangeKm} km</strong></span></c:if>
                                        <c:if test="${not empty matchedVehicle.seats}"><span>Chỗ ngồi: <strong>${matchedVehicle.seats} chỗ</strong></span></c:if>
                                    </div>
                                </div>
                            </div>
                        </c:if>

                        <!-- Chat history flow -->
                        <div class="consult-chat-flow">
                            <!-- Match associated order if any -->
                            <c:set var="matchedOrder" value="${null}"/>
                            <c:forEach var="ord" items="${customerOrders}">
                                <c:if test="${empty matchedOrder and (req.content.contains(ord.id.toString()) or (not empty ord.vehicleName and req.modelName ne null and ord.vehicleName.contains(req.modelName)))}">
                                    <c:set var="matchedOrder" value="${ord}"/>
                                </c:if>
                            </c:forEach>

                            <!-- Render message bubbles -->
                            <c:forTokens items="${req.content}" delims="&#10;" var="line">
                                <c:set var="trimmed" value="${line.trim()}"/>
                                <c:if test="${not empty trimmed}">
                                    <c:choose>
                                        <c:when test="${trimmed.startsWith('[Khách hàng]:')}">
                                            <div style="align-self:flex-end;max-width:80%;margin:4px 0;">
                                                <div style="background:#2563eb;color:#fff;border-radius:12px 12px 2px 12px;padding:10px 14px;font-size:13px;word-break:break-word;box-shadow:0 1px 3px rgba(37,99,235,0.2);">
                                                    <small style="opacity:0.8;font-size:11px;display:block;margin-bottom:2px;">Bạn</small>
                                                    <c:out value="${trimmed.substring(13).trim()}"/>
                                                </div>
                                            </div>
                                        </c:when>
                                        <c:when test="${trimmed.startsWith('[') and (trimmed.contains(']:') or trimmed.contains('NV') or trimmed.contains('Sales'))}">
                                            <div style="align-self:flex-start;max-width:80%;margin:4px 0;">
                                                <div style="background:#fff;color:#0f172a;border-radius:12px 12px 12px 2px;padding:10px 14px;font-size:13px;word-break:break-word;border:1px solid #e2e8f0;box-shadow:0 1px 3px rgba(0,0,0,0.03);">
                                                    <small style="color:#2563eb;font-weight:700;font-size:11px;display:block;margin-bottom:2px;">
                                                        👤 <c:out value="${req.staffName}" default="Chuyên viên Sales"/>
                                                    </small>
                                                    <c:out value="${trimmed}"/>
                                                </div>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div style="align-self:flex-end;max-width:80%;margin:4px 0;">
                                                <div style="background:#2563eb;color:#fff;border-radius:12px 12px 2px 12px;padding:10px 14px;font-size:13px;word-break:break-word;box-shadow:0 1px 3px rgba(37,99,235,0.2);">
                                                    <small style="opacity:0.8;font-size:11px;display:block;margin-bottom:2px;">Nội dung yêu cầu ban đầu</small>
                                                    <c:out value="${trimmed}"/>
                                                </div>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </c:if>
                            </c:forTokens>

                            <!-- Interactive Payment Request Card (if order exists or is requested) -->
                            <c:if test="${not empty matchedOrder}">
                                <div style="align-self:stretch;margin:12px 0;padding:16px;border:2px solid #3b82f6;background:#eff6ff;border-radius:10px;box-shadow:0 4px 12px rgba(59,130,246,0.08);">
                                    <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;margin-bottom:10px;">
                                        <div style="display:flex;align-items:center;gap:6px;">
                                            <span style="font-size:18px;">💳</span>
                                            <strong style="color:#1e40af;font-size:15px;letter-spacing:0.3px;">YÊU CẦU THANH TOÁN ĐẶT CỌC</strong>
                                        </div>
                                        <c:choose>
                                            <c:when test="${matchedOrder.status eq 'DEPOSITED' or matchedOrder.status eq 'PAID' or matchedOrder.status eq 'PROCESSING' or matchedOrder.status eq 'DELIVERED'}">
                                                <span style="background:#dcfce7;color:#166534;border:1px solid #86efac;font-weight:700;font-size:12px;padding:4px 10px;border-radius:6px;">✓ ĐÃ THANH TOÁN THÀNH CÔNG</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span style="background:#fef9c3;color:#854d0e;border:1px solid #fde047;font-weight:700;font-size:12px;padding:4px 10px;border-radius:6px;">⏳ CHỜ THANH TOÁN CỌC</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div style="font-size:13px;color:#1e293b;line-height:1.6;margin-bottom:12px;">
                                        <div>Mã đơn hàng: <strong>#<c:out value="${matchedOrder.id}"/></strong> · Xe: <strong><c:out value="${matchedOrder.vehicleName}"/></strong></div>
                                        <div>Tiền đặt cọc 30%: <strong style="color:#b91c1c;font-size:16px;"><fmt:formatNumber value="${matchedOrder.depositAmount}" type="currency" currencySymbol="₫"/></strong> (Tổng giá trị xe: <fmt:formatNumber value="${matchedOrder.originalAmount}" type="currency" currencySymbol="₫"/>)</div>
                                        <div>Phương thức mua: <strong><c:out value="${matchedOrder.paymentMethodName}"/></strong></div>
                                    </div>

                                    <c:choose>
                                        <c:when test="${matchedOrder.status eq 'DEPOSITED' or matchedOrder.status eq 'PAID' or matchedOrder.status eq 'PROCESSING' or matchedOrder.status eq 'DELIVERED'}">
                                            <div style="display:flex;gap:10px;align-items:center;flex-wrap:wrap;border-top:1px solid #bfdbfe;padding-top:10px;">
                                                <span style="font-size:13px;color:#166534;font-weight:600;">Website đã ghi nhận khoản cọc qua SePay + MB Bank.</span>
                                                <a class="button button-secondary" href="<%= customerContext %>/orders" style="font-size:12px;padding:6px 14px;">Xem tiến trình đơn hàng →</a>
                                                <c:if test="${matchedOrder.contractStatus eq 'PENDING_SIGNATURE'}">
                                                    <a class="button button-primary" href="<%= customerContext %>/contract" style="font-size:12px;padding:6px 14px;background:#16a34a;">Ký hợp đồng OTP →</a>
                                                </c:if>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div style="border-top:1px solid #bfdbfe;padding-top:10px;display:flex;gap:10px;align-items:center;flex-wrap:wrap;">
                                                <a class="button button-primary" href="<%= customerContext %>/payment/transfer?orderId=${matchedOrder.id}&type=DEPOSIT"
                                                   style="padding:8px 18px;font-size:13px;font-weight:700;background:#2563eb;color:#fff;border-radius:6px;text-decoration:none;display:inline-block;">
                                                    Thanh toán cọc ngay bằng VietQR (SePay - MB Bank) →
                                                </a>
                                                <span style="font-size:12px;color:#64748b;">Mã QR động tự động điền đúng số tài khoản và số tiền.</span>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>

                                    <%-- Thêm thẻ thanh toán 70% còn lại nếu là Full Payment và đã ký hợp đồng --%>
                                    <c:if test="${matchedOrder.contractStatus eq 'SIGNED' and matchedOrder.paymentMethodName ne 'INSTALLMENT' and matchedOrder.remainingAmount gt 0}">
                                        <div style="margin-top:14px;padding:12px;background:#f0fdf4;border:1px solid #86efac;border-radius:8px;">
                                            <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;">
                                                <div>
                                                    <strong style="color:#166534;font-size:13px;">TIẾP TỤC: THANH TOÁN 70% CÒN LẠI (HỢP ĐỒNG ĐÃ KÝ)</strong>
                                                    <div style="font-size:12px;color:#334155;margin-top:2px;">Số tiền 70% còn lại: <strong style="color:#166534;font-size:15px;"><fmt:formatNumber value="${matchedOrder.remainingAmount}" type="currency" currencySymbol="₫"/></strong></div>
                                                </div>
                                                <a class="button button-primary" href="<%= customerContext %>/payment/transfer?orderId=${matchedOrder.id}&type=REMAINING"
                                                   style="background:#16a34a;padding:8px 16px;font-size:13px;font-weight:700;color:#fff;border-radius:6px;text-decoration:none;">
                                                    Thanh toán 70% còn lại qua VietQR →
                                                </a>
                                            </div>
                                        </div>
                                    </c:if>
                                </div>
                            </c:if>
                        </div>

                        <!-- Reply Form box -->
                        <div class="consult-reply-box">
                            <c:choose>
                                <c:when test="${req.status ne 'COMPLETED' and req.status ne 'CANCELLED'}">
                                    <form method="post" action="<%= customerContext %>/consultation" style="display:flex;gap:10px;align-items:flex-end;">
                                        <input type="hidden" name="action" value="reply">
                                        <input type="hidden" name="requestId" value="${req.id}">
                                        <c:if test="${not empty csrfToken}">
                                            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
                                        </c:if>
                                        <textarea name="message" rows="2" required maxlength="400"
                                                  placeholder="Nhập phản hồi hoặc câu hỏi thêm cho chuyên viên Sales..."
                                                  style="flex:1;padding:10px 12px;border:1px solid #cbd5e1;border-radius:8px;font-size:13px;resize:none;font-family:inherit;"></textarea>
                                        <button type="submit" class="button button-primary" style="padding:10px 18px;font-size:13px;font-weight:600;border-radius:8px;white-space:nowrap;">
                                            Gửi phản hồi ↵
                                        </button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <p style="font-size:13px;color:#94a3b8;margin:0;text-align:center;">
                                        Cuộc trò chuyện này đã kết thúc (<c:out value="${req.status}"/>). Nếu cần hỗ trợ thêm, bạn vui lòng mở yêu cầu tư vấn mới ở trên.
                                    </p>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </c:forEach>
            </section>
        </div>

        <script>
        (function() {
            const threadItems = document.querySelectorAll(".consult-thread-item");
            const threadBodies = document.querySelectorAll(".consult-thread-body");
            const sidebar = document.getElementById("consult-sidebar");
            const detailContainer = document.getElementById("consult-detail-container");

            function activateThread(targetId) {
                threadItems.forEach(item => {
                    const match = item.dataset.targetId === targetId;
                    item.classList.toggle("active", match);
                });
                threadBodies.forEach(body => {
                    body.style.display = (body.id === targetId) ? "flex" : "none";
                    if (body.id === targetId) body.style.flexDirection = "column";
                });
                if (window.innerWidth <= 860) {
                    sidebar.style.display = "none";
                    detailContainer.style.display = "block";
                }
            }

            threadItems.forEach(item => {
                item.addEventListener("click", () => {
                    activateThread(item.dataset.targetId);
                });
            });

            document.querySelectorAll(".consult-mobile-back-btn").forEach(btn => {
                btn.addEventListener("click", () => {
                    sidebar.style.display = "block";
                    detailContainer.style.display = "none";
                });
            });

            // Check URL hash e.g. #chat-thread-123 or #thread-123
            const hash = window.location.hash;
            if (hash) {
                const cleanId = hash.replace("#", "").replace("thread-", "");
                const candidateId = "chat-thread-" + cleanId;
                if (document.getElementById(candidateId)) {
                    activateThread(candidateId);
                }
            }
        })();
        </script>
    </c:when>
    <c:otherwise>
        <div class="panel document-panel" style="padding:40px 20px;text-align:center;">
            <div style="font-size:40px;margin-bottom:12px;">💬</div>
            <h3 style="margin:0 0 8px;font-size:18px;color:#1e293b;">Chưa có yêu cầu tư vấn nào</h3>
            <p style="color:#64748b;font-size:14px;max-width:480px;margin:0 auto 18px;">
                Bạn chưa gửi yêu cầu tư vấn xe điện nào. Hãy bấm nút phía trên hoặc vào danh mục xe để chọn mẫu xe và gửi câu hỏi cho chuyên viên Sales!
            </p>
            <a class="button button-primary" href="<%= customerContext %>/index.jsp#models" style="font-size:13px;padding:8px 20px;">
                Xem danh mục xe VinFast →
            </a>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="../common/customer-shell-end.jspf" %>