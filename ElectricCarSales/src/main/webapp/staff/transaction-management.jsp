<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Quản lý Giao dịch - Electric Car Sales</title>
    <!-- Giả sử bạn đang dùng file CSS chung cho staff -->
    <link rel="stylesheet" href="../assets/css/staff.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
</head>
<body>
    <div class="container mt-4">
        <h2>Quản lý Giao dịch Bán Xe</h2>
        <hr>
        <!-- Bảng hiển thị danh sách giao dịch -->
        <table class="table table-bordered table-hover mt-3">
            <thead class="table-dark">
                <tr>
                    <th>Mã Đơn (Order ID)</th>
                    <th>Khách Hàng</th>
                    <th>Ngày Tạo</th>
                    <th>Tổng Tiền</th>
                    <th>Trạng Thái</th>
                    <th>Hành Động</th>
                </tr>
            </thead>
            <tbody>
                <!-- Dùng JSTL để lặp qua danh sách orders truyền từ OrderController -->
                <c:forEach var="order" items="${orderList}">
                    <tr>
                        <td>${order.id}</td>
                        <td>${order.customerName}</td>
                        <td>${order.orderDate}</td>
                        <td>${order.totalAmount} VNĐ</td>
                        <td>
                            <span class="badge bg-info">${order.status}</span>
                        </td>
                        <td>
                            <a href="${pageContext.request.contextPath}/order/detail?id=${order.id}" class="btn btn-sm btn-primary">Chi tiết</a>
                            <button class="btn btn-sm btn-warning" onclick="updateStatus(${order.id})">Cập nhật</button>
                        </td>
                    </tr>
                </c:forEach>

                <!-- Dữ liệu mẫu (xóa đi khi đã kết nối Servlet thành công) -->
                <tr>
                    <td>ORD-001</td>
                    <td>Trần Võ Anh Kiệt</td>
                    <td>2026-10-15</td>
                    <td>1,200,000,000 VNĐ</td>
                    <td><span class="badge bg-warning">Đang xử lý</span></td>
                    <td>
                        <a href="#" class="btn btn-sm btn-primary">Chi tiết</a>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <script src="../assets/js/staff.js"></script>
    <script>
        function updateStatus(orderId) {
            // Hàm xử lý gọi API hoặc chuyển trang để cập nhật trạng thái
            alert("Mở modal cập nhật trạng thái cho đơn hàng: " + orderId);
        }
    </script>
</body>
</html>
