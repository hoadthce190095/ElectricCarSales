<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Quản lý Yêu cầu Tư vấn</title>
    <!-- CSS dùng chung của bạn -->
    <link rel="stylesheet" href="../assets/css/staff.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
</head>
<body>
    <div class="container mt-4">
        <h2>Danh sách Yêu cầu Tư vấn từ Khách Hàng</h2>
        <hr>

        <table class="table table-striped table-hover mt-3 border">
            <thead class="table-dark">
                <tr>
                    <th>Mã Yêu Cầu</th>
                    <th>Khách Hàng</th>
                    <th>Số Điện Thoại</th>
                    <th>Mẫu Xe Quan Tâm</th>
                    <th>Trạng Thái</th>
                    <th>Hành Động</th>
                </tr>
            </thead>
            <tbody>
                <!-- Vòng lặp lấy dữ liệu từ DB (hiện tại hiển thị dữ liệu tĩnh để test) -->
                <tr>
                    <td>CST-001</td>
                    <td>Nguyễn Văn A</td>
                    <td>0901234567</td>
                    <td>VinFast VF 8</td>
                    <td><span class="badge bg-danger">Chờ xử lý</span></td>
                    <td>
                        <button class="btn btn-sm btn-primary">Xem</button>
                        <button class="btn btn-sm btn-success">Cập nhật</button>
                    </td>
                </tr>
                <tr>
                    <td>CST-002</td>
                    <td>Lê Thị B</td>
                    <td>0988776655</td>
                    <td>VinFast VF 5</td>
                    <td><span class="badge bg-warning text-dark">Đã liên hệ</span></td>
                    <td>
                        <button class="btn btn-sm btn-primary">Xem</button>
                        <button class="btn btn-sm btn-success">Cập nhật</button>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</body>
</html>
