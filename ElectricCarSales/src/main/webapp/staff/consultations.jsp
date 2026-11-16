<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Sales Staff · Yêu cầu tư vấn</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/payment-bank-transfer.css">
    <style>
        .consult-card { border:1px solid #e2e8f0; border-radius:8px; padding:16px; margin-bottom:16px; background:#fff; }
        .consult-card h3 { margin:0 0 8px; font-size:15px; }
        .status-badge { display:inline-block; padding:2px 8px; border-radius:4px; font-size:12px; font-weight:600; }
        .status-PENDING { background:#fef9c3; color:#854d0e; }
        .status-IN_PROGRESS { background:#dbeafe; color:#1e40af; }
        .status-COMPLETED { background:#dcfce7; color:#166534; }
        .status-CANCELLED { background:#fee2e2; color:#991b1b; }
        .content-box { background:#f8fafc; border-radius:6px; padding:10px; font-size:13px; white-space:pre-wrap; word-break:break-word; margin:8px 0; }
        .update-form { margin-top:12px; padding-top:12px; border-top:1px dashed #e2e8f0; }
        .update-form label { display:block; font-size:12px; color:#475569; margin-bottom:4px; }
        .update-form select, .update-form textarea { width:100%; box-sizing:border-box; padding:7px 10px; border:1px solid #cbd5e1; border-radius:6px; font-size:13px; margin-bottom:10px; }
        .update-form textarea { resize:vertical; height:80px; }
        .update-form button { background:#1e3a8a; color:#fff; border:none; border-radius:6px; padding:7px 18px; font-size:13px; cursor:pointer; }
        .update-form button:hover { background:#1e40af; }
        .blocked-notice { background:#fffbeb; border:1px solid #fbbf24; border-radius:6px; padding:12px 16px; margin-bottom:20px; color:#78350f; font-size:13px; }
    </style>
</head>
<body class="finance-review">
<main>
    <h1>Quản lý Yêu cầu Tư vấn · Sales Staff</h1>
    <p><a href="<%= request.getContextPath() %>/staff/dashboard">← Trang nhân viên</a></p>

    <%-- Thông báo BLOCKED chat --%>
    <div class="blocked-notice">
        ⚠ <strong>Lưu ý kỹ thuật:</strong> Hệ thống tư vấn hiện tại là <strong>một chiều</strong>.
        Chat thời gian thực chưa được hỗ trợ do giới hạn database schema
        (ConsultationRequests chỉ lưu 1 request + nội dung append, không có bảng ChatMessages).
        Nhân viên cập nhật trạng thái và ghi chú bên dưới; khách hàng xem kết quả tại trang "Tư vấn xe".
    </div>

    <c:if test="${param.updated eq '1'}">
        <p role="status" style="color:#166534;background:#dcfce7;padding:8px 12px;border-radius:6px;margin-bottom:16px;">
            ✓ Đã cập nhật yêu cầu tư vấn thành công.
        </p>
    </c:if>

    <section class="finance-panel">
        <h2>Danh sách yêu cầu tư vấn (<c:out value="${not empty consultations ? consultations.size() : 0}"/>)</h2>

        <c:if test="${empty consultations}">
            <p>Chưa có yêu cầu tư vấn nào trong hệ thống.</p>
        </c:if>

        <c:forEach var="req" items="${consultations}">
            <article class="consult-card">
                <h3>
                    Yêu cầu #<c:out value="${req.id}"/>
                    &nbsp;
                    <span class="status-badge status-<c:out value='${req.status}'/>">
                        <c:choose>
                            <c:when test="${req.status == 'PENDING'}">CHỜ XỬ LÝ</c:when>
                            <c:when test="${req.status == 'IN_PROGRESS'}">ĐANG XỬ LÝ</c:when>
                            <c:when test="${req.status == 'COMPLETED'}">ĐÃ HOÀN TẤT</c:when>
                            <c:when test="${req.status == 'CANCELLED'}">ĐÃ HỦY</c:when>
                            <c:otherwise><c:out value="${req.status}"/></c:otherwise>
                        </c:choose>
                    </span>
                </h3>

                <table style="font-size:13px;border-collapse:collapse;width:100%;margin-bottom:4px;">
                    <tr>
                        <td style="color:#64748b;padding:2px 10px 2px 0;width:120px;">Khách hàng</td>
                        <td><strong><c:out value="${req.customerName}" default="—"/></strong>
                            <c:if test="${not empty req.customerPhone}"> · <c:out value="${req.customerPhone}"/></c:if>
                        </td>
                    </tr>
                    <tr>
                        <td style="color:#64748b;padding:2px 10px 2px 0;">Mẫu xe</td>
                        <td><c:out value="${req.modelName}" default="Chưa xác định"/></td>
                    </tr>
                    <tr>
                        <td style="color:#64748b;padding:2px 10px 2px 0;">Ngày gửi</td>
                        <td><c:out value="${req.createdAt}"/></td>
                    </tr>
                    <tr>
                        <td style="color:#64748b;padding:2px 10px 2px 0;">Nhân viên</td>
                        <td><c:out value="${req.staffName}" default="Chưa phân công"/></td>
                    </tr>
                </table>

                <div>
                    <small style="text-transform:uppercase;font-size:11px;color:#94a3b8;letter-spacing:.5px;">Nội dung yêu cầu &amp; lịch sử</small>
                    <div class="content-box"><c:out value="${req.content}"/></div>
                </div>

                <%-- Nút tắt lập đơn hàng cho khách hàng từ yêu cầu tư vấn --%>
                <div style="margin: 10px 0;">
                    <a style="font-size:12px;font-weight:600;padding:6px 14px;background:#0284c7;color:#fff;text-decoration:none;border-radius:6px;display:inline-flex;align-items:center;gap:6px;"
                       href="<%= request.getContextPath() %>/orders?customerId=<c:out value='${req.customerId}'/>">
                        <span>⚡ Lập đơn hàng cho khách này</span>
                        <span aria-hidden="true">→</span>
                    </a>
                </div>

                <%-- Form cập nhật trạng thái và ghi chú --%>
                <div class="update-form">
                    <form method="post" action="<%= request.getContextPath() %>/consultation">
                        <input type="hidden" name="action" value="update">
                        <input type="hidden" name="requestId" value="${req.id}">
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">

                        <label for="status_${req.id}">Trạng thái</label>
                        <select id="status_${req.id}" name="status">
                            <option value="PENDING"      <c:if test="${req.status == 'PENDING'}">selected</c:if>>PENDING — Chờ xử lý</option>
                            <option value="IN_PROGRESS"  <c:if test="${req.status == 'IN_PROGRESS'}">selected</c:if>>IN_PROGRESS — Đang xử lý</option>
                            <option value="COMPLETED"    <c:if test="${req.status == 'COMPLETED'}">selected</c:if>>COMPLETED — Đã hoàn tất</option>
                            <option value="CANCELLED"    <c:if test="${req.status == 'CANCELLED'}">selected</c:if>>CANCELLED — Đã hủy</option>
                        </select>

                        <label for="note_${req.id}">Ghi chú phản hồi (sẽ được nối vào nội dung)</label>
                        <textarea id="note_${req.id}" name="note" placeholder="Nhập ghi chú hoặc phản hồi cho khách hàng..."></textarea>

                        <button type="submit">Cập nhật</button>
                    </form>
                </div>
            </article>
        </c:forEach>
    </section>
</main>
</body>
</html>
