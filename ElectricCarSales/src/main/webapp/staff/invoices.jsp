<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%
    com.electriccarsales.model.SessionUser _u =
        (com.electriccarsales.model.SessionUser) session.getAttribute("user");
    String _role = (_u == null || _u.getRole() == null) ? "" : _u.getRole().trim().toUpperCase();
    request.setAttribute("currentRole", _role);
%>
<!doctype html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Kế toán · Quản lý hóa đơn</title>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/payment-bank-transfer.css">
<style>
  body { font-family: system-ui, sans-serif; margin: 0; background: #f8fafc; color: #1e293b; }
  main { max-width: 1100px; margin: 0 auto; padding: 24px 16px; }
  h1 { font-size: 1.5rem; margin-bottom: 4px; }
  .breadcrumb { margin-bottom: 20px; font-size: .9rem; color: #64748b; }
  .breadcrumb a { color: #2563eb; text-decoration: none; }
  section.panel { background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; margin-bottom: 28px; overflow: hidden; }
  .panel-header { padding: 14px 20px; border-bottom: 1px solid #e2e8f0; display: flex; align-items: center; gap: 10px; }
  .panel-header h2 { margin: 0; font-size: 1.1rem; }
  .badge { font-size: .75rem; background: #e0f2fe; color: #0369a1; border-radius: 999px; padding: 2px 10px; }
  table { width: 100%; border-collapse: collapse; font-size: .9rem; }
  th { background: #f1f5f9; text-align: left; padding: 10px 12px; font-size: .78rem; color: #475569; border-bottom: 1px solid #e2e8f0; }
  td { padding: 10px 12px; border-bottom: 1px solid #f1f5f9; vertical-align: middle; }
  tr:last-child td { border-bottom: none; }
  .status-badge { display: inline-block; font-size: .75rem; padding: 2px 8px; border-radius: 4px; font-weight: 600; }
  .status-PAID { background: #dcfce7; color: #166534; }
  .status-DEPOSITED { background: #fef9c3; color: #854d0e; }
  .status-PROCESSING { background: #dbeafe; color: #1d4ed8; }
  .status-SIGNED { background: #dcfce7; color: #166534; }
  .status-ISSUED { background: #f0fdf4; color: #15803d; }
  .empty-row td { text-align: center; color: #94a3b8; padding: 24px; }
  .btn-issue { background: #2563eb; color: #fff; border: none; padding: 6px 14px; border-radius: 6px; cursor: pointer; font-size: .85rem; }
  .btn-issue:hover { background: #1d4ed8; }
  .btn-link { color: #2563eb; text-decoration: none; font-size: .85rem; }
  .btn-link:hover { text-decoration: underline; }
  .notice { background: #fffbeb; border: 1px solid #fcd34d; border-radius: 6px; padding: 10px 16px; margin-bottom: 20px; font-size: .9rem; color: #78350f; }
  .msg-ok { background: #dcfce7; border: 1px solid #86efac; border-radius: 6px; padding: 10px 16px; margin-bottom: 16px; color: #166534; }
  .msg-err { background: #fee2e2; border: 1px solid #fca5a5; border-radius: 6px; padding: 10px 16px; margin-bottom: 16px; color: #991b1b; }
  label.note-lbl { display: flex; flex-direction: column; gap: 4px; font-size: .82rem; color: #475569; }
  label.note-lbl input { border: 1px solid #cbd5e1; border-radius: 4px; padding: 4px 8px; font-size: .85rem; width: 180px; }
</style>
</head>
<body>
<main>
  <div class="breadcrumb"><a href="<%= request.getContextPath() %>/staff/dashboard">← Trang nhân viên</a></div>
  <h1>Quản lý hóa đơn (Kế toán)</h1>
  <p style="color:#64748b;margin-bottom:20px;">Phát hành hóa đơn chính thức sau khi hợp đồng đã ký (SIGNED) và đơn hàng thanh toán đủ 100%.</p>

  <c:if test="${param.issued eq '1'}">
    <div class="msg-ok">✓ Hóa đơn đã được phát hành thành công.</div>
  </c:if>
  <c:if test="${not empty param.error}">
    <div class="msg-err">⚠ <c:out value="${param.error}"/></div>
  </c:if>

  <%-- Chỉ ACCOUNTANT mới được phát hành hóa đơn --%>
  <c:set var="isAccountant" value="${currentRole eq 'ACCOUNTANT'}"/>

  <%-- ===== PHẦN 1: Đơn đủ điều kiện phát hành hóa đơn ===== --%>
  <section class="panel">
    <div class="panel-header">
      <h2>Đơn đủ điều kiện phát hành hóa đơn</h2>
      <span class="badge"><c:out value="${not empty eligibleOrders ? eligibleOrders.size() : 0}"/> đơn</span>
    </div>
    <c:choose>
      <c:when test="${not empty eligibleOrders}">
        <table>
          <thead>
            <tr>
              <th>Mã đơn</th>
              <th>Khách hàng</th>
              <th>Xe (Model / Phiên bản)</th>
              <th>VIN</th>
              <th>Tổng tiền</th>
              <th>Trạng thái đơn</th>
              <th>Hợp đồng</th>
              <c:if test="${isAccountant}"><th>Thao tác</th></c:if>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="o" items="${eligibleOrders}">
              <tr>
                <td><strong>#<c:out value="${o.orderId}"/></strong></td>
                <td>
                  <c:out value="${o.customerName}" default="—"/>
                  <c:if test="${not empty o.phone}">
                    <br><small style="color:#94a3b8;"><c:out value="${o.phone}"/></small>
                  </c:if>
                </td>
                <td>
                  <c:out value="${o.modelName}" default="—"/>
                  <c:if test="${not empty o.versionName}"> / <c:out value="${o.versionName}"/></c:if>
                </td>
                <td><code><c:out value="${o.vin}" default="—"/></code></td>
                <td>
                  <c:choose>
                    <c:when test="${not empty o.totalAmount}">
                      <fmt:formatNumber value="${o.totalAmount}" pattern="#,###"/> ₫
                    </c:when>
                    <c:otherwise>—</c:otherwise>
                  </c:choose>
                </td>
                <td><span class="status-badge status-${o.orderStatus}"><c:out value="${o.orderStatus}"/></span></td>
                <td>
                  <span class="status-badge status-${o.contractStatus}"><c:out value="${o.contractStatus}" default="—"/></span>
                  <c:if test="${o.contractStatus eq 'SIGNED'}">
                    <br><a class="btn-link" href="<%= request.getContextPath() %>/staff/sales-document?kind=contract&amp;orderId=<c:out value='${o.orderId}'/>">HĐ Word</a>
                  </c:if>
                  <br><a class="btn-link" href="<%= request.getContextPath() %>/staff/sales-document?kind=deposit&amp;orderId=<c:out value='${o.orderId}'/>">Cọc Word</a>
                </td>
                <c:if test="${isAccountant}">
                  <td>
                    <form method="post" action="<%= request.getContextPath() %>/invoice" class="invoice-issue-form" data-order-id="<c:out value='${o.orderId}'/>" data-customer="<c:out value='${o.customerName}'/>">
                      <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
                      <input type="hidden" name="orderId" value="<c:out value='${o.orderId}'/>">
                      <label class="note-lbl">
                        Ghi chú (tùy chọn)
                        <input type="text" name="note" maxlength="200" placeholder="Hóa đơn GTGT xe điện...">
                      </label>
                      <button type="submit" class="btn-issue" style="margin-top:6px;">Phát hành hóa đơn</button>
                    </form>
                  </td>
                </c:if>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </c:when>
      <c:otherwise>
        <table><tbody>
          <tr class="empty-row">
            <td colspan="8">Không có đơn hàng nào đủ điều kiện phát hành hóa đơn hiện tại.<br>
              <small>Yêu cầu: Hợp đồng SIGNED + Đơn hàng PAID + Chưa có hóa đơn.</small>
            </td>
          </tr>
        </tbody></table>
      </c:otherwise>
    </c:choose>
  </section>

  <%-- ===== PHẦN 2: Hóa đơn đã phát hành ===== --%>
  <section class="panel">
    <div class="panel-header">
      <h2>Hóa đơn đã phát hành</h2>
      <span class="badge"><c:out value="${not empty invoices ? invoices.size() : 0}"/> hóa đơn</span>
    </div>
    <c:choose>
      <c:when test="${not empty invoices}">
        <table>
          <thead>
            <tr>
              <th>Số hóa đơn</th>
              <th>Ngày phát hành</th>
              <th>Mã đơn</th>
              <th>Tổng thanh toán</th>
              <th>Trạng thái</th>
              <th>Ghi chú</th>
              <th>Tải xuống</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="inv" items="${invoices}">
              <tr>
                <td><strong><c:out value="${inv.invoiceNumber}"/></strong></td>
                <td><c:out value="${inv.invoiceDate}" default="—"/></td>
                <td>#<c:out value="${inv.orderID}"/></td>
                <td>
                  <c:choose>
                    <c:when test="${not empty inv.totalAmount}">
                      <strong><fmt:formatNumber value="${inv.totalAmount}" pattern="#,###"/> ₫</strong>
                    </c:when>
                    <c:otherwise>—</c:otherwise>
                  </c:choose>
                </td>
                <td><span class="status-badge status-ISSUED"><c:out value="${inv.status}"/></span></td>
                <td><c:out value="${inv.note}" default="—"/></td>
                <td>
                  <a class="btn-link"
                     href="<%= request.getContextPath() %>/staff/sales-document?kind=invoice&amp;orderId=<c:out value='${inv.orderID}'/>">
                    ⬇ Tải Word
                  </a>
                </td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </c:when>
      <c:otherwise>
        <table><tbody>
          <tr class="empty-row">
            <td colspan="7">Chưa có hóa đơn nào được phát hành.</td>
          </tr>
        </tbody></table>
      </c:otherwise>
    </c:choose>
  </section>

  <%-- ===== PHẦN 3: Toàn bộ đơn liên quan (đọc-only, để tham khảo) ===== --%>
  <section class="panel">
    <div class="panel-header">
      <h2>Tất cả đơn hàng liên quan (PAID / DEPOSITED / PROCESSING)</h2>
      <span class="badge"><c:out value="${not empty allOrders ? allOrders.size() : 0}"/> đơn</span>
    </div>
    <c:choose>
      <c:when test="${not empty allOrders}">
        <table>
          <thead>
            <tr>
              <th>Mã đơn</th>
              <th>Khách hàng</th>
              <th>Xe</th>
              <th>VIN</th>
              <th>Tổng tiền</th>
              <th>Còn nợ</th>
              <th>TT Đơn</th>
              <th>Hợp đồng</th>
              <th>Hóa đơn</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="o" items="${allOrders}">
              <tr>
                <td><strong>#<c:out value="${o.orderId}"/></strong></td>
                <td><c:out value="${o.customerName}" default="—"/></td>
                <td>
                  <c:out value="${o.modelName}" default="—"/>
                  <c:if test="${not empty o.versionName}"> <small>/ <c:out value="${o.versionName}"/></small></c:if>
                </td>
                <td><small><code><c:out value="${o.vin}" default="—"/></code></small></td>
                <td>
                  <c:if test="${not empty o.totalAmount}">
                    <fmt:formatNumber value="${o.totalAmount}" pattern="#,###"/> ₫
                  </c:if>
                </td>
                <td>
                  <c:choose>
                    <c:when test="${not empty o.remainingAmount and o.remainingAmount != 0}">
                      <span style="color:#dc2626;">
                        <fmt:formatNumber value="${o.remainingAmount}" pattern="#,###"/> ₫
                      </span>
                    </c:when>
                    <c:otherwise><span style="color:#16a34a;">Đã đủ</span></c:otherwise>
                  </c:choose>
                </td>
                <td><span class="status-badge status-${o.orderStatus}"><c:out value="${o.orderStatus}"/></span></td>
                <td>
                  <span class="status-badge status-${o.contractStatus}"><c:out value="${o.contractStatus}" default="Chưa có"/></span>
                  <c:if test="${o.contractStatus eq 'SIGNED'}">
                    <br><a class="btn-link" href="<%= request.getContextPath() %>/staff/sales-document?kind=contract&amp;orderId=<c:out value='${o.orderId}'/>">HĐ Word</a>
                  </c:if>
                </td>
                <td>
                  <c:choose>
                    <c:when test="${not empty o.invoiceId}">
                      <span class="status-badge status-ISSUED">ĐÃ PHÁT HÀNH</span>
                      <a class="btn-link" href="<%= request.getContextPath() %>/staff/sales-document?kind=invoice&amp;orderId=<c:out value='${o.orderId}'/>">HĐơn Word</a>
                    </c:when>
                    <c:otherwise><span style="color:#94a3b8;">Chưa có</span></c:otherwise>
                  </c:choose>
                  <c:if test="${o.orderStatus eq 'DEPOSITED' or o.orderStatus eq 'PROCESSING' or o.orderStatus eq 'PAID' or o.orderStatus eq 'COMPLETED'}">
                    <br><a class="btn-link" href="<%= request.getContextPath() %>/staff/sales-document?kind=deposit&amp;orderId=<c:out value='${o.orderId}'/>">Cọc Word</a>
                  </c:if>
                </td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </c:when>
      <c:otherwise>
        <table><tbody>
          <tr class="empty-row"><td colspan="9">Không có đơn hàng nào đang hoạt động.</td></tr>
        </tbody></table>
      </c:otherwise>
    </c:choose>
  </section>

</main>

<dialog id="invoice-confirm-dialog" style="border:none;border-radius:12px;padding:0;width:min(480px, calc(100% - 32px));box-shadow:0 25px 60px rgba(15,23,42,0.3);overflow:hidden;">
  <div style="padding:18px 24px;border-bottom:1px solid #e2e8f0;background:#f8fafc;display:flex;align-items:center;justify-content:space-between;">
    <h3 style="margin:0;font-size:16px;font-weight:700;color:#0f172a;">Xác nhận phát hành hóa đơn GTGT</h3>
    <button type="button" id="invoice-confirm-close" style="background:transparent;border:none;font-size:22px;line-height:1;color:#64748b;cursor:pointer;">&times;</button>
  </div>
  <div style="padding:20px 24px;">
    <p style="margin:0;font-size:14px;line-height:1.5;color:#334155;">Bạn có chắc chắn muốn phát hành hóa đơn GTGT chính thức cho đơn hàng này?</p>
    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:12px 14px;margin-top:12px;font-size:13px;display:flex;flex-direction:column;gap:8px;">
      <div style="display:flex;justify-content:space-between;"><span style="color:#64748b;">Mã đơn hàng:</span><strong id="invoice-modal-order-id">—</strong></div>
      <div style="display:flex;justify-content:space-between;"><span style="color:#64748b;">Khách hàng:</span><strong id="invoice-modal-customer">—</strong></div>
    </div>
  </div>
  <div style="display:flex;justify-content:flex-end;gap:10px;padding:16px 24px;border-top:1px solid #e2e8f0;background:#f8fafc;">
    <button type="button" id="invoice-confirm-cancel" style="padding:8px 18px;font-size:13px;font-weight:600;border-radius:7px;border:1px solid #cbd5e1;background:#fff;color:#334155;cursor:pointer;">Không, quay lại</button>
    <button type="button" id="invoice-confirm-submit" style="padding:8px 20px;font-size:13px;font-weight:600;border-radius:7px;border:none;background:#1e3a8a;color:#fff;cursor:pointer;">Có, xác nhận</button>
  </div>
</dialog>

<script>
document.addEventListener("DOMContentLoaded", function() {
  const dialog = document.getElementById("invoice-confirm-dialog");
  const btnClose = document.getElementById("invoice-confirm-close");
  const btnCancel = document.getElementById("invoice-confirm-cancel");
  const btnSubmit = document.getElementById("invoice-confirm-submit");
  const modalOrderId = document.getElementById("invoice-modal-order-id");
  const modalCustomer = document.getElementById("invoice-modal-customer");
  let activeForm = null;

  function closeDialog() {
    if (dialog && typeof dialog.close === "function") dialog.close();
    activeForm = null;
  }

  if (btnClose) btnClose.onclick = closeDialog;
  if (btnCancel) btnCancel.onclick = closeDialog;

  btnSubmit?.addEventListener("click", function() {
    if (!activeForm) return;
    btnSubmit.disabled = true;
    btnCancel.disabled = true;
    btnClose.disabled = true;
    btnSubmit.textContent = "Đang xử lý...";
    activeForm.submit();
  });

  document.querySelectorAll(".invoice-issue-form").forEach(form => {
    form.addEventListener("submit", function(e) {
      e.preventDefault();
      activeForm = form;
      if (modalOrderId) modalOrderId.textContent = "#" + (form.dataset.orderId || "—");
      if (modalCustomer) modalCustomer.textContent = form.dataset.customer || "—";
      btnSubmit.disabled = false;
      btnCancel.disabled = false;
      btnClose.disabled = false;
      btnSubmit.textContent = "Có, xác nhận";
      if (dialog && typeof dialog.showModal === "function") {
        dialog.showModal();
      }
    });
  });
});
</script>
</body>
</html>
