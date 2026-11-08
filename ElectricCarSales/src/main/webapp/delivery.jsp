<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% if (request.getAttribute("deliveryList") == null) { response.sendRedirect(request.getContextPath() + "/delivery"); return; }
   request.setAttribute("screenTitle", "Giao xe");
   request.setAttribute("screenDescription", "Theo dõi lịch và trạng thái bàn giao xe cho khách hàng."); %>
<%@ include file="common/data-shell.jspf" %>
<section class="data-panel data-table" aria-label="Danh sách giao xe">
    <table>
        <thead><tr><th>Mã giao xe</th><th>Đơn hàng</th><th>Khách hàng</th><th>Xe</th><th>Ngày giao</th><th>Địa chỉ</th><th>Trạng thái</th></tr></thead>
        <tbody>
        <c:forEach items="${deliveryList}" var="delivery">
            <tr>
                <td><c:out value="${delivery.deliveryID}" /></td>
                <td><c:out value="${delivery.orderID}" /></td>
                <td><c:out value="${delivery.customerID}" /></td>
                <td><c:out value="${delivery.carID}" /></td>
                <td><fmt:formatDate value="${delivery.deliveryDate}" pattern="dd/MM/yyyy" /></td>
                <td><c:out value="${delivery.deliveryAddress}" /></td>
                <td><c:out value="${delivery.deliveryStatus}" /></td>
            </tr>
        </c:forEach>
        <c:if test="${empty deliveryList}"><tr><td colspan="7" class="data-empty">Chưa có lịch giao xe.</td></tr></c:if>
        </tbody>
    </table>
</section>
<%@ include file="common/data-shell-end.jspf" %>
