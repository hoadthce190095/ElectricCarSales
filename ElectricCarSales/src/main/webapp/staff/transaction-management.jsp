<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Quản lý Giao dịch - VinFast Staff</title>
    <link rel="stylesheet" href="../assets/css/staff.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
</head>
<body style="background: #0f1115; color: #e1e4ea; font-family: 'Be Vietnam Pro', sans-serif;">
    <!-- Top Navigation Bar -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark px-4 py-3 border-bottom border-secondary">
        <a class="navbar-brand fw-bold text-primary" href="${pageContext.request.contextPath}/staff/dashboard.jsp">
            VinFast Staff Portal
        </a>
        <div class="collapse navbar-collapse">
            <ul class="navbar-nav me-auto">
                <li class="nav-item">
                    <a class="nav-link text-white" href="${pageContext.request.contextPath}/staff/dashboard.jsp">Tổng quan</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link text-white" href="${pageContext.request.contextPath}/staff/consultations">Yêu cầu tư vấn</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link active fw-bold text-info" href="${pageContext.request.contextPath}/staff/transactions">Giao dịch bán xe</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link text-white" href="${pageContext.request.contextPath}/staff/report">Báo cáo &amp; Thống kê</a>
                </li>
            </ul>
        </div>
    </nav>

    <div class="container-fluid px-4 mt-4">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <div>
                <h3 class="fw-bold mb-1">Quản Lý Giao Dịch Bán Xe</h3>
                <p class="text-secondary mb-0">Theo dõi, kiểm tra và quản lý toàn bộ các giao dịch mua bán xe trong suốt quy trình bán hàng.</p>
            </div>
            <a href="${pageContext.request.contextPath}/staff/report" class="btn btn-outline-info btn-sm">
                Xem Báo Cáo Tiến Độ Giao Dịch
            </a>
        </div>

        <c:if test="${param.updated eq 'true'}">
            <div class="alert alert-success alert-dismissible fade show" role="alert">
                Cập nhật trạng thái giao dịch thành công!
            </div>
        </c:if>

        <div class="card bg-dark border-secondary">
            <div class="card-body p-0">
                <table class="table table-dark table-hover table-striped mb-0 align-middle">
                    <thead class="table-secondary text-dark">
                        <tr>
                            <th class="ps-3">Mã Đơn (Order ID)</th>
                            <th>Khách Hàng</th>
                            <th>Mẫu Xe Đã Chọn</th>
                            <th>Ngày Tạo</th>
                            <th>Tổng Tiền</th>
                            <th>Trạng Thái</th>
                            <th class="text-center">Hành Động</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${not empty orderList}">
                                <c:forEach var="order" items="${orderList}">
                                    <tr>
                                        <td class="ps-3 fw-bold text-info">#ORD-${order.id}</td>
                                        <td>${empty order.customerName ? '-' : order.customerName}</td>
                                        <td><span class="badge bg-primary">${empty order.vehicleName ? '-' : order.vehicleName}</span></td>
                                        <td class="text-secondary small">${order.orderDate}</td>
                                        <td class="fw-bold text-success">
                                            <fmt:formatNumber value="${order.originalAmount}" type="currency" currencySymbol="VNĐ" maxFractionDigits="0"/>
                                        </td>
                                        <td>
                                            <span class="badge bg-info">${empty order.statusName ? 'Đang xử lý' : order.statusName}</span>
                                        </td>
                                        <td class="text-center">
                                            <a href="${pageContext.request.contextPath}/staff/transaction-detail?id=${order.id}" class="btn btn-sm btn-primary">
                                                Xem Chi Tiết
                                            </a>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td colspan="7" class="text-center py-4 text-secondary">
                                        Hiện chưa có giao dịch nào trong hệ thống.
                                    </td>
                                </tr>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</body>
</html>
