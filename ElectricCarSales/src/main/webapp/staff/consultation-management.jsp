<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Quản lý Yêu cầu Tư vấn - VinFast Staff</title>
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
                    <a class="nav-link active fw-bold text-info" href="${pageContext.request.contextPath}/staff/consultations">Yêu cầu tư vấn</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link text-white" href="${pageContext.request.contextPath}/staff/transactions">Giao dịch bán xe</a>
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
                <h3 class="fw-bold mb-1">Danh Sách Yêu Cầu Tư Vấn Khách Hàng</h3>
                <p class="text-secondary mb-0">Tiếp nhận, xử lý và cập nhật tiến độ tư vấn cho khách hàng quan tâm xe điện.</p>
            </div>
            <a href="${pageContext.request.contextPath}/customer/consultation" target="_blank" class="btn btn-outline-info btn-sm">
                Mở form gửi tư vấn khách hàng
            </a>
        </div>

        <c:if test="${param.updated eq 'true'}">
            <div class="alert alert-success alert-dismissible fade show" role="alert">
                Cập nhật trạng thái yêu cầu tư vấn thành công!
            </div>
        </c:if>

        <div class="card bg-dark border-secondary">
            <div class="card-body p-0">
                <table class="table table-dark table-hover table-striped mb-0 align-middle">
                    <thead class="table-secondary text-dark">
                        <tr>
                            <th class="ps-3">Mã YC</th>
                            <th>Khách Hàng</th>
                            <th>Số Điện Thoại</th>
                            <th>Mẫu Xe Quan Tâm</th>
                            <th>Nội Dung Yêu Cầu</th>
                            <th>Thời Gian</th>
                            <th>Trạng Thái</th>
                            <th class="text-center">Thao Tác Xử Lý</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${not empty consultationList}">
                                <c:forEach var="c" items="${consultationList}">
                                    <tr>
                                        <td class="ps-3 fw-bold text-info">#YC-${c.id}</td>
                                        <td>${empty c.customerName ? '-' : c.customerName}</td>
                                        <td>${empty c.customerPhone ? '-' : c.customerPhone}</td>
                                        <td><span class="badge bg-primary">${empty c.modelName ? '-' : c.modelName}</span></td>
                                        <td style="max-width: 250px;" class="text-truncate" title="${c.content}">${c.content}</td>
                                        <td class="text-secondary small">${c.createdAt}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${c.status eq '1' || c.status eq 'PENDING'}">
                                                    <span class="badge bg-danger">Chưa tiếp nhận</span>
                                                </c:when>
                                                <c:when test="${c.status eq '2' || c.status eq 'IN_PROGRESS'}">
                                                    <span class="badge bg-warning text-dark">Đang xử lý</span>
                                                </c:when>
                                                <c:when test="${c.status eq '3' || c.status eq 'COMPLETED'}">
                                                    <span class="badge bg-success">Đã liên hệ</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-secondary">${c.statusName}</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="text-center">
                                            <form action="${pageContext.request.contextPath}/staff/consultation/update" method="POST" class="d-inline-flex gap-1">
                                                <input type="hidden" name="requestId" value="${c.id}">
                                                <select name="statusId" class="form-select form-select-sm bg-dark text-white border-secondary" style="width: 130px;">
                                                    <option value="2" ${c.status eq '2' ? 'selected' : ''}>Đang xử lý</option>
                                                    <option value="3" ${c.status eq '3' ? 'selected' : ''}>Đã liên hệ</option>
                                                    <option value="4" ${c.status eq '4' ? 'selected' : ''}>Hủy yêu cầu</option>
                                                </select>
                                                <button type="submit" class="btn btn-sm btn-outline-success">Lưu</button>
                                            </form>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td colspan="8" class="text-center py-4 text-secondary">
                                        Hiện chưa có yêu cầu tư vấn nào từ khách hàng.
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
