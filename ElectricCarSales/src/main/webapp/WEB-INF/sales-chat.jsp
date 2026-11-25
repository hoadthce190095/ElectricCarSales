<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Trò chuyện tư vấn khách hàng · ElectricCarSales</title>
    <style>
        :root {
            --sc-primary: #102a56;
            --sc-accent: #0284c7;
            --sc-accent-hover: #0369a1;
            --sc-bg: #f1f5f9;
            --sc-card: #ffffff;
            --sc-border: #cbd5e1;
            --sc-text: #0f172a;
            --sc-muted: #64748b;
            --sc-bubble-customer: #eff6ff;
            --sc-bubble-customer-border: #bfdbfe;
            --sc-bubble-staff: #f0fdf4;
            --sc-bubble-staff-border: #bbf7d0;
        }
        body {
            font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            font-size: 14.5px;
            line-height: 1.5;
            background: var(--sc-bg);
            color: var(--sc-text);
            margin: 0;
            padding: 0;
        }
        .sc-shell {
            max-width: 980px;
            margin: 24px auto;
            padding: 0 16px;
        }
        .sc-topbar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
            flex-wrap: wrap;
            gap: 12px;
        }
        .sc-back-link {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            color: var(--sc-accent);
            text-decoration: none;
            font-weight: 600;
            font-size: 13.5px;
            padding: 6px 12px;
            background: #fff;
            border: 1px solid var(--sc-border);
            border-radius: 6px;
            transition: all .2s;
        }
        .sc-back-link:hover {
            background: #f8fafc;
            border-color: #94a3b8;
        }
        .sc-header-title {
            margin: 0;
            font-size: 22px;
            font-weight: 700;
            color: var(--sc-primary);
        }
        .sc-header-desc {
            font-size: 13px;
            color: var(--sc-muted);
            margin: 4px 0 0;
        }
        .sc-error-banner {
            background: #fef2f2;
            border: 1px solid #fecaca;
            color: #991b1b;
            padding: 10px 14px;
            border-radius: 8px;
            font-size: 13.5px;
            font-weight: 600;
            margin-bottom: 16px;
        }
        .sc-thread-card {
            background: var(--sc-card);
            border: 1px solid var(--sc-border);
            border-radius: 12px;
            margin-bottom: 24px;
            box-shadow: 0 2px 8px rgba(15, 23, 42, 0.05);
            overflow: hidden;
        }
        .sc-thread-header {
            background: #f8fafc;
            border-bottom: 1px solid #e2e8f0;
            padding: 16px 20px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-wrap: wrap;
            gap: 12px;
        }
        .sc-thread-info {
            display: flex;
            flex-direction: column;
            gap: 4px;
        }
        .sc-thread-id {
            font-size: 16px;
            font-weight: 700;
            color: var(--sc-primary);
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .sc-thread-meta {
            font-size: 12.5px;
            color: var(--sc-muted);
        }
        .sc-status-pill {
            display: inline-block;
            font-size: 11px;
            font-weight: 700;
            padding: 3px 8px;
            border-radius: 999px;
            text-transform: uppercase;
            letter-spacing: .4px;
        }
        .sc-status-PENDING { background: #fef9c3; color: #854d0e; }
        .sc-status-IN_PROGRESS { background: #dbeafe; color: #1e40af; }
        .sc-status-COMPLETED { background: #dcfce7; color: #166534; }
        .sc-status-CANCELLED { background: #fee2e2; color: #991b1b; }

        /* Nút lập đơn hàng cho Sales Staff */
        .sc-staff-actions {
            display: flex;
            gap: 8px;
            flex-wrap: wrap;
        }
        .sc-btn-order {
            background: #0284c7;
            color: #ffffff !important;
            border: none;
            border-radius: 6px;
            padding: 7px 14px;
            font-size: 12.5px;
            font-weight: 600;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: background .2s;
            cursor: pointer;
        }
        .sc-btn-order:hover {
            background: #0369a1;
        }
        .sc-btn-secondary {
            background: #ffffff;
            color: #334155 !important;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            padding: 6px 12px;
            font-size: 12.5px;
            font-weight: 600;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            transition: all .2s;
        }
        .sc-btn-secondary:hover {
            background: #f8fafc;
            border-color: #94a3b8;
        }

        /* Thẻ tóm tắt yêu cầu xe ban đầu */
        .sc-inquiry-box {
            background: #f8fafc;
            border-bottom: 1px solid #e2e8f0;
            padding: 14px 20px;
        }
        .sc-inquiry-title {
            font-size: 12px;
            font-weight: 700;
            color: #475569;
            text-transform: uppercase;
            letter-spacing: .5px;
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            gap: 6px;
        }
        .sc-spec-grid {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin-bottom: 8px;
        }
        .sc-spec-tag {
            background: #ffffff;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            padding: 4px 10px;
            font-size: 12px;
            display: inline-flex;
            gap: 4px;
        }
        .sc-spec-tag strong {
            color: var(--sc-primary);
        }

        /* Khung hiển thị tin nhắn */
        .sc-chat-area {
            padding: 20px;
            display: flex;
            flex-direction: column;
            gap: 14px;
            max-height: 480px;
            overflow-y: auto;
            background: #ffffff;
        }
        .sc-bubble {
            max-width: 82%;
            padding: 12px 16px;
            border-radius: 12px;
            font-size: 14px;
            line-height: 1.6;
            word-break: break-word;
            display: flex;
            flex-direction: column;
            gap: 4px;
        }
        .sc-bubble-author {
            font-size: 11.5px;
            font-weight: 700;
            display: flex;
            align-items: center;
            gap: 6px;
        }
        .sc-bubble-customer {
            align-self: flex-start;
            background: var(--sc-bubble-customer);
            border: 1px solid var(--sc-bubble-customer-border);
            color: #1e3a8a;
            border-bottom-left-radius: 4px;
        }
        .sc-bubble-customer .sc-bubble-author {
            color: #1d4ed8;
        }
        .sc-bubble-staff {
            align-self: flex-end;
            background: var(--sc-bubble-staff);
            border: 1px solid var(--sc-bubble-staff-border);
            color: #14532d;
            border-bottom-right-radius: 4px;
        }
        .sc-bubble-staff .sc-bubble-author {
            color: #15803d;
        }
        .sc-bubble-system {
            align-self: center;
            background: #f1f5f9;
            border: 1px solid #e2e8f0;
            color: #475569;
            font-size: 12.5px;
            max-width: 90%;
            text-align: center;
        }

        /* Khung nhập tin nhắn */
        .sc-reply-box {
            background: #f8fafc;
            border-top: 1px solid #e2e8f0;
            padding: 16px 20px;
        }
        .sc-reply-form {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }
        .sc-reply-textarea {
            width: 100%;
            box-sizing: border-box;
            padding: 10px 12px;
            border: 1px solid #cbd5e1;
            border-radius: 8px;
            font-size: 13.5px;
            font-family: inherit;
            resize: vertical;
            min-height: 75px;
        }
        .sc-reply-textarea:focus {
            outline: none;
            border-color: var(--sc-accent);
            box-shadow: 0 0 0 2px rgba(2, 132, 199, 0.2);
        }
        .sc-reply-foot {
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-wrap: wrap;
            gap: 10px;
        }
        .sc-char-counter {
            font-size: 12px;
            color: var(--sc-muted);
        }
        .sc-btn-send {
            background: var(--sc-primary);
            color: #ffffff;
            border: none;
            border-radius: 6px;
            padding: 9px 20px;
            font-size: 13.5px;
            font-weight: 600;
            cursor: pointer;
            transition: background .2s;
        }
        .sc-btn-send:hover {
            background: #1e3a8a;
        }
        .sc-closed-notice {
            padding: 12px 16px;
            background: #f8fafc;
            border-top: 1px solid #e2e8f0;
            color: var(--sc-muted);
            font-size: 13px;
            text-align: center;
        }
    </style>
</head>
<body>
<main class="sc-shell">
    <div class="sc-topbar">
        <div>
            <h1 class="sc-header-title">Trò chuyện tư vấn xe điện</h1>
            <p class="sc-header-desc">
                Trao đổi trực tiếp giữa Khách hàng và Nhân viên tư vấn (Sales Staff) về mẫu xe, màu sắc và phương án thanh toán.
            </p>
        </div>
        <div style="display:flex;gap:8px;align-items:center;">
            <a class="sc-back-link" href="${pageContext.request.contextPath}/${isCustomer ? 'consultation' : 'staff/dashboard'}">
                ← Quay lại ${isCustomer ? 'Tư vấn xe' : 'Trang nhân viên'}
            </a>
            <button type="button" class="sc-back-link" onclick="window.location.reload();" title="Làm mới để kiểm tra tin nhắn mới">
                🔄 Tải lại
            </button>
        </div>
    </div>

    <c:if test="${not empty chatError}">
        <div class="sc-error-banner" role="alert">
            ⚠ <c:out value="${chatError}"/>
        </div>
    </c:if>

    <c:if test="${empty threads}">
        <div style="background:#fff;border:1px solid #cbd5e1;border-radius:12px;padding:32px;text-align:center;color:#64748b;">
            <p style="font-size:16px;margin:0 0 8px;">Chưa có cuộc trò chuyện tư vấn nào.</p>
            <p style="font-size:13px;margin:0;">
                <c:choose>
                    <c:when test="${isCustomer}">
                        Hãy mở hộp thoại "Tư vấn &amp; Báo giá" ở trang chủ hoặc chi tiết xe để bắt đầu!
                    </c:when>
                    <c:otherwise>
                        Chưa có khách hàng nào gửi yêu cầu hoặc phân công cho bạn.
                    </c:otherwise>
                </c:choose>
            </p>
        </div>
    </c:if>

    <c:forEach var="thread" items="${threads}">
        <article class="sc-thread-card" id="thread-${thread.id}">
            <%-- Header của Thread --%>
            <div class="sc-thread-header">
                <div class="sc-thread-info">
                    <div class="sc-thread-id">
                        <span>#${thread.id} · <c:out value="${thread.modelName}" default="Tư vấn xe VinFast"/></span>
                        <span class="sc-status-pill sc-status-${thread.status}">
                            <c:choose>
                                <c:when test="${thread.status eq 'PENDING'}">CHỜ XỬ LÝ</c:when>
                                <c:when test="${thread.status eq 'IN_PROGRESS'}">ĐANG TƯ VẤN</c:when>
                                <c:when test="${thread.status eq 'COMPLETED'}">HOÀN TẤT</c:when>
                                <c:when test="${thread.status eq 'CANCELLED'}">ĐÃ HỦY</c:when>
                                <c:otherwise><c:out value="${thread.status}"/></c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="sc-thread-meta">
                        Khách hàng: <strong><c:out value="${thread.customerName}"/></strong>
                        <c:if test="${not empty thread.customerPhone}"> · SĐT: <c:out value="${thread.customerPhone}"/></c:if>
                        &nbsp;|&nbsp;
                        Phụ trách: <strong><c:out value="${not empty thread.staffName ? thread.staffName : 'Chưa phân công'}"/></strong>
                    </div>
                </div>

                <%-- Nút LẬP ĐƠN HÀNG cho Sales Staff --%>
                <c:if test="${!isCustomer}">
                    <div class="sc-staff-actions">
                        <a class="sc-btn-order"
                           href="${pageContext.request.contextPath}/orders?customerId=${thread.customerId}&modelId=${thread.modelId}&requestId=${thread.id}"
                           title="Mở form lập đơn và điền sẵn thông tin khách hàng, mẫu xe từ yêu cầu tư vấn">
                            ⚡ Lập đơn hàng theo tư vấn
                        </a>
                        <a class="sc-btn-secondary"
                           href="${pageContext.request.contextPath}/orders?customerId=${thread.customerId}"
                           title="Lập đơn mới hoặc đổi mẫu xe khác cho khách hàng này">
                            + Chọn xe khác
                        </a>
                        <c:if test="${thread.status ne 'COMPLETED' and thread.status ne 'CANCELLED'}">
                            <button type="button" class="sc-btn-order" style="background:#16a34a;"
                                    onclick="updateChatStatus(${thread.id}, 'COMPLETED')">
                                ✓ Completed
                            </button>
                            <button type="button" class="sc-btn-order" style="background:#dc2626;"
                                    onclick="updateChatStatus(${thread.id}, 'CANCELLED')">
                                ✕ Cancel
                            </button>
                        </c:if>
                    </div>
                </c:if>
            </div>

            <%-- Khối tóm tắt thông số yêu cầu ban đầu --%>
            <div class="sc-inquiry-box">
                <div class="sc-inquiry-title">
                    <span>📋 Thông tin xe &amp; phương án quan tâm:</span>
                </div>
                <div class="sc-spec-grid">
                    <div class="sc-spec-tag"><span>Mẫu xe:</span> <strong><c:out value="${thread.modelName}" default="—"/></strong></div>
                    <c:if test="${not empty thread.versionInfo}">
                        <div class="sc-spec-tag"><span>Phiên bản:</span> <strong><c:out value="${thread.versionInfo}"/></strong></div>
                    </c:if>
                    <c:if test="${not empty thread.colorInfo}">
                        <div class="sc-spec-tag"><span>Màu sắc:</span> <strong><c:out value="${thread.colorInfo}"/></strong></div>
                    </c:if>
                    <c:if test="${not empty thread.paymentPreferenceInfo}">
                        <div class="sc-spec-tag"><span>Phương án:</span> <strong><c:out value="${thread.paymentPreferenceInfo}"/></strong></div>
                    </c:if>
                    <div class="sc-spec-tag"><span>Ngày gửi:</span> <c:out value="${thread.createdAt}"/></div>
                </div>
            </div>

            <%-- Khung hiển thị các tin nhắn hội thoại --%>
            <div class="sc-chat-area">
                <%-- Parse các dòng hội thoại --%>
                <c:set var="rawLines" value="${fn:split(thread.content, '&#10;')}" />
                <c:forEach var="line" items="${rawLines}" varStatus="status">
                    <c:set var="trimmedLine" value="${fn:trim(line)}" />
                    <c:if test="${not empty trimmedLine}">
                        <c:choose>
                            <%-- Phản hồi từ Khách hàng --%>
                            <c:when test="${fn:startsWith(trimmedLine, '[Khách hàng]:')}">
                                <div class="sc-bubble sc-bubble-customer">
                                    <div class="sc-bubble-author">
                                        <span>👤</span>
                                        <span><c:out value="${thread.customerName}" default="Khách hàng"/></span>
                                    </div>
                                    <div><c:out value="${fn:trim(fn:substringAfter(trimmedLine, '[Khách hàng]:'))}"/></div>
                                </div>
                            </c:when>

                            <%-- Phản hồi từ Sales Staff --%>
                            <c:when test="${fn:startsWith(trimmedLine, '[Sales]:') or fn:startsWith(trimmedLine, '[NV ')}">
                                <c:set var="staffMsg" value="${fn:substringAfter(trimmedLine, ']: ')}" />
                                <c:if test="${empty staffMsg}">
                                    <c:set var="staffMsg" value="${fn:substringAfter(trimmedLine, ']:')}" />
                                </c:if>
                                <div class="sc-bubble sc-bubble-staff">
                                    <div class="sc-bubble-author">
                                        <span>💼</span>
                                        <span><c:out value="${not empty thread.staffName ? thread.staffName : 'Nhân viên tư vấn'}"/></span>
                                    </div>
                                    <div><c:out value="${fn:trim(staffMsg)}"/></div>
                                </div>
                            </c:when>

                            <%-- Dòng đầu tiên (Yêu cầu tư vấn ban đầu của khách) --%>
                            <c:when test="${status.first}">
                                <div class="sc-bubble sc-bubble-customer">
                                    <div class="sc-bubble-author">
                                        <span>👤</span>
                                        <span>Yêu cầu ban đầu từ <c:out value="${thread.customerName}" default="Khách hàng"/></span>
                                    </div>
                                    <div>
                                        <%-- Hiển thị phần nội dung đã lọc bớt các tag cấu trúc --%>
                                        <c:out value="${thread.cleanMessage}"/>
                                    </div>
                                </div>
                            </c:when>

                            <%-- Dòng ghi chú khác --%>
                            <c:otherwise>
                                <div class="sc-bubble sc-bubble-system">
                                    <c:out value="${trimmedLine}"/>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </c:if>
                </c:forEach>
            </div>

            <%-- Khung gửi tin nhắn phản hồi --%>
            <c:choose>
                <c:when test="${thread.status eq 'COMPLETED' or thread.status eq 'CANCELLED'}">
                    <div class="sc-closed-notice">
                        Cuộc trò chuyện này đã kết thúc. Vui lòng mở yêu cầu tư vấn mới nếu cần hỗ trợ thêm.
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="sc-reply-box">
                        <form method="post" action="${pageContext.request.contextPath}/sales-chat" class="sc-reply-form" onsubmit="handleChatSubmit(this)">
                            <input type="hidden" name="csrfToken" value="<c:out value='${chatCsrf}'/>">
                            <input type="hidden" name="requestId" value="${thread.id}">
                            
                            <textarea id="message-${thread.id}" name="message" class="sc-reply-textarea" maxlength="800"
                                      placeholder="${isCustomer ? 'Nhập phản hồi hoặc câu hỏi thêm cho Sales Staff...' : 'Nhập phản hồi, thông tin tồn kho hoặc tư vấn cho khách hàng...'}"
                                      required oninput="updateCharCount(this, ${thread.id})"></textarea>

                            <div class="sc-reply-foot">
                                <div class="sc-char-counter">
                                    <span id="char-count-${thread.id}">0</span> / 800 ký tự (Tin nhắn) · Tổng lịch sử: ${fn:length(thread.content)} / 1.000 ký tự
                                    <c:if test="${fn:length(thread.content) > 900}">
                                        <span style="color:#b91c1c;font-weight:600;">(Gần đầy giới hạn 1.000 ký tự)</span>
                                    </c:if>
                                </div>
                                <button type="submit" class="sc-btn-send">Gửi phản hồi →</button>
                            </div>
                        </form>
                    </div>
                </c:otherwise>
            </c:choose>
        </article>
    </c:forEach>
</main>

<script>
    function updateCharCount(textarea, id) {
        const countSpan = document.getElementById('char-count-' + id);
        if (countSpan) {
            countSpan.textContent = textarea.value.length;
        }
    }

    function handleChatSubmit(form) {
        const btn = form.querySelector('button[type="submit"]');
        if (btn) {
            btn.disabled = true;
            btn.textContent = "Đang gửi...";
        }
    }

    async function updateChatStatus(requestId, status) {
        let note = "";
        if (status === "COMPLETED") {
            if (!confirm("Xác nhận đã tư vấn xong cho yêu cầu #" + requestId + "?")) return;
        } else if (status === "CANCELLED") {
            const reason = prompt("Vui lòng ghi rõ lý do hủy yêu cầu tư vấn #" + requestId + ":");
            if (reason === null) return;
            note = reason.trim();
            if (!note) {
                alert("Vui lòng ghi rõ lý do hủy yêu cầu tư vấn.");
                return;
            }
            if (!confirm("Xác nhận hủy yêu cầu tư vấn #" + requestId + " với lý do: " + note + "?")) return;
        }
        try {
            const body = new URLSearchParams({
                action: "update",
                format: "json",
                requestId: String(requestId),
                status: status,
                note: note,
                csrfToken: "<c:out value='${chatCsrf}'/>"
            });
            const res = await fetch("${pageContext.request.contextPath}/consultation", {
                method: "POST",
                credentials: "same-origin",
                headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8", "Accept": "application/json" },
                body: body.toString()
            });
            const data = await res.json();
            if (!res.ok || !data.success) {
                alert(data.message || data.error || "Không thể cập nhật trạng thái.");
                return;
            }
            window.location.reload();
        } catch (err) {
            alert("Lỗi kết nối máy chủ khi cập nhật trạng thái.");
        }
    }
</script>
</body>
</html>
