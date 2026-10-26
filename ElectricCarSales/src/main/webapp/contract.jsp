<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% if (request.getAttribute("contractList") == null) { response.sendRedirect(request.getContextPath() + "/contract"); return; }
   request.setAttribute("screenTitle", "Hợp đồng bán xe");
   request.setAttribute("screenDescription", "Theo dõi các hợp đồng đã được ghi nhận trong hệ thống."); %>
<%@ include file="common/data-shell.jspf" %>
<section class="data-panel data-table" aria-label="Danh sách hợp đồng">
    <table>
        <thead><tr><th>Số hợp đồng</th><th>Đơn hàng</th><th>Khách hàng</th><th>Ngày lập</th><th>Giá trị (VND)</th><th>Trạng thái</th><th>Ghi chú</th></tr></thead>
        <tbody>
        <c:forEach items="${contractList}" var="contract">
            <tr>
                <td><c:out value="${contract.contractNumber}" /></td>
                <td><c:out value="${contract.orderID}" /></td>
                <td><c:out value="${contract.customerID}" /></td>
                <td><fmt:formatDate value="${contract.contractDate}" pattern="dd/MM/yyyy" /></td>
                <td><fmt:formatNumber value="${contract.contractAmount}" pattern="#,##0" /></td>
                <td><c:out value="${contract.status}" /></td>
                <td><c:out value="${contract.note}" /></td>
            </tr>
        </c:forEach>
        <c:if test="${empty contractList}"><tr><td colspan="7" class="data-empty">Chưa có hợp đồng.</td></tr></c:if>
        </tbody>
    </table>
</section>
<%@ include file="common/data-shell-end.jspf" %>
