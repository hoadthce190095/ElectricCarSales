<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Chi Tiết Giao Dịch - VinFast</title>
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

    <div class="container-fluid px-4 mt-4 mb-5">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <div>
                <a href="${pageContext.request.contextPath}/staff/transactions" class="text-decoration-none text-info small">Danh sách giao dịch (Staff)</a> &nbsp;|&nbsp; <a href="${pageContext.request.contextPath}/customer/my-orders.jsp" class="text-decoration-none text-info small">Đơn hàng của tôi (Khách hàng)</a>
                <h3 class="fw-bold mt-1">Chi Tiết Giao Dịch Bán Xe #GD-${empty order ? '-' : order.id}</h3>
            </div>
            <div>
                <span class="badge bg-success fs-6 px-3 py-2">
                    ${empty order.statusName ? 'Đang xử lý' : order.statusName}
                </span>
            </div>
        </div>

        <c:if test="${param.updated eq 'true'}">
            <div class="alert alert-success alert-dismissible fade show" role="alert">
                Đã cập nhật trạng thái giao dịch và ghi nhận vào lịch sử thành công!
            </div>
        </c:if>

        <div class="row g-4">
            <!-- Cột 1: Thông tin khách hàng & Xe -->
            <div class="col-lg-6">
                <!-- Thông tin khách hàng -->
                <div class="card bg-dark border-secondary mb-4">
                    <div class="card-header bg-secondary text-white fw-bold">
                        1. Thông Tin Khách Hàng (Customer Information)
                    </div>
                    <div class="card-body">
                        <table class="table table-dark table-borderless mb-0">
                            <tr>
                                <td class="text-secondary" style="width: 35%;">Họ và tên:</td>
                                <td class="fw-bold text-white">${empty order.customerName ? '-' : order.customerName}</td>
                            </tr>
                            <tr>
                                <td class="text-secondary">Số điện thoại:</td>
                                <td class="text-info">${empty order.customerPhone ? '-' : order.customerPhone}</td>
                            </tr>
                            <tr>
                                <td class="text-secondary">Email:</td>
                                <td>${empty order.customerEmail ? '-' : order.customerEmail}</td>
                            </tr>
                            <tr>
                                <td class="text-secondary">Địa chỉ nhận xe:</td>
                                <td>${empty order.customerAddress ? '-' : order.customerAddress}</td>
                            </tr>
                        </table>
                    </div>
                </div>

                <!-- Thông tin xe đã chọn -->
                <div class="card bg-dark border-secondary">
                    <div class="card-header bg-secondary text-white fw-bold">
                        2. Thông Tin Xe Đã Chọn (Selected Vehicle)
                    </div>
                    <div class="card-body">
                        <table class="table table-dark table-borderless mb-0">
                            <tr>
                                <td class="text-secondary" style="width: 35%;">Dòng xe / Mẫu xe:</td>
                                <td class="fw-bold text-warning">${empty order.vehicleName ? '-' : order.vehicleName}</td>
                            </tr>
                            <tr>
                                <td class="text-secondary">Số khung (VIN):</td>
                                <td><code>${empty order.vinNumber ? '-' : order.vinNumber}</code></td>
                            </tr>
                            <tr>
                                <td class="text-secondary">Giá niêm yết:</td>
                                <td class="fw-bold text-success fs-5">
                                    <fmt:formatNumber value="${order.originalAmount}" type="currency" currencySymbol="VNĐ" maxFractionDigits="0"/>
                                </td>
                            </tr>
                            <tr>
                                <td class="text-secondary">Ghi chú đơn hàng:</td>
                                <td class="text-light">${empty order.note ? '-' : order.note}</td>
                            </tr>
                        </table>
                    </div>
                </div>
            </div>

            <!-- Cột 2: Thanh toán & Trạng thái -->
            <div class="col-lg-6">
                <!-- Thanh toán & Tài chính -->
                <div class="card bg-dark border-secondary mb-4">
                    <div class="card-header bg-secondary text-white fw-bold">
                        3. Thanh Toán &amp; Tài Chính (Payment &amp; Financials)
                    </div>
                    <div class="card-body">
                        <table class="table table-dark table-borderless mb-0">
                            <tr>
                                <td class="text-secondary" style="width: 40%;">Phương thức thanh toán:</td>
                                <td><span class="badge bg-info text-dark">${empty order.paymentMethod ? '-' : order.paymentMethod}</span></td>
                            </tr>
                            <tr>
                                <td class="text-secondary">Ngày tạo giao dịch:</td>
                                <td>${empty order.orderDate ? '-' : order.orderDate}</td>
                            </tr>
                            <tr>
                                <td class="text-secondary">Đã đặt cọc:</td>
                                <td class="fw-bold text-info">
                                    <fmt:formatNumber value="${order.depositAmount}" type="currency" currencySymbol="VNĐ" maxFractionDigits="0"/>
                                </td>
                            </tr>
                            <tr>
                                <td class="text-secondary">Số tiền còn lại:</td>
                                <td class="fw-bold text-danger">
                                    <fmt:formatNumber value="${order.remainingAmount}" type="currency" currencySymbol="VNĐ" maxFractionDigits="0"/>
                                </td>
                            </tr>
                        </table>
                    </div>
                </div>

                <!-- Cập nhật trạng thái giao dịch -->
                <div class="card bg-dark border-secondary">
                    <div class="card-header bg-secondary text-white fw-bold d-flex justify-content-between align-items-center">
                        <span>4. Trạng Thái &amp; Quy Trình Giao Dịch</span>
                    </div>
                    <div class="card-body">
                        <form action="${pageContext.request.contextPath}/staff/order/update-status" method="POST" class="mb-4">
                            <input type="hidden" name="orderId" value="${order.id}">
                            <label class="form-label text-secondary small fw-bold">CẬP NHẬT TIẾN ĐỘ GIAO DỊCH (Dành cho Nhân viên / Quản lý):</label>
                            <div class="input-group">
                                <select name="statusId" class="form-select bg-dark text-white border-secondary">
                                    <option value="1">1. Mới tạo (PENDING)</option>
                                    <option value="2">2. Đã giữ chỗ xe (RESERVED)</option>
                                    <option value="3">3. Đã đặt cọc (DEPOSITED)</option>
                                    <option value="4" selected>4. Đang xử lý hồ sơ (PROCESSING)</option>
                                    <option value="5">5. Đã thanh toán đủ (PAID)</option>
                                    <option value="6">6. Đã bàn giao xe (COMPLETED)</option>
                                    <option value="7">7. Đã hủy giao dịch (CANCELLED)</option>
                                </select>
                                <button type="submit" class="btn btn-primary px-3">Cập nhật ngay</button>
                            </div>
                        </form>

                        <h6 class="fw-bold text-secondary mb-2">Lịch sử thay đổi trạng thái (Status History):</h6>
                        <ul class="list-group list-group-flush bg-dark">
                            <c:choose>
                                <c:when test="${not empty statusHistory}">
                                    <c:forEach var="h" items="${statusHistory}">
                                        <li class="list-group-item bg-dark text-white border-secondary d-flex justify-content-between">
                                            <span>Trạng thái: <strong>${h.status}</strong></span>
                                            <span class="text-secondary small">${h.changedAt}</span>
                                        </li>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <li class="list-group-item bg-dark text-secondary border-secondary">
                                        Chưa có bản ghi lịch sử trạng thái nào.
                                    </li>
                                </c:otherwise>
                            </c:choose>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
