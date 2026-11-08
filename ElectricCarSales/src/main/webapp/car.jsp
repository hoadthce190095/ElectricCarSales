<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% if (request.getAttribute("carList") == null) { response.sendRedirect(request.getContextPath() + "/car"); return; }
   request.setAttribute("screenTitle", "Kho xe");
   request.setAttribute("screenDescription", "Danh sách xe hiện có và trạng thái kho."); %>
<%@ include file="common/data-shell.jspf" %>
<section class="data-panel data-table" aria-label="Danh sách xe">
    <table>
        <thead><tr><th>Mã xe</th><th>Mẫu xe / Phiên bản</th><th>Năm</th><th>VIN</th><th>Trạng thái</th><th>Giá (VND)</th></tr></thead>
        <tbody>
        <c:forEach items="${carList}" var="car">
            <tr>
                <td><c:out value="${car.carCode}" /></td>
                <td><c:out value="${car.modelID}" /> / <c:out value="${car.versionID}" /></td>
                <td><c:out value="${car.year}" /></td>
                <td><c:out value="${car.vin}" /></td>
                <td><c:forEach items="${statusList}" var="status"><c:if test="${status.carStatusID eq car.carStatusID}"><c:out value="${status.statusName}" /></c:if></c:forEach></td>
                <td><fmt:formatNumber value="${car.price}" pattern="#,##0" /></td>
            </tr>
        </c:forEach>
        <c:if test="${empty carList}"><tr><td colspan="6" class="data-empty">Chưa có xe trong kho.</td></tr></c:if>
        </tbody>
    </table>
</section>
<%@ include file="common/data-shell-end.jspf" %>
