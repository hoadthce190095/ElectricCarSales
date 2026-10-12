<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Quản lý Nhà cung cấp</title>
    <style>
        body { font-family: Arial, sans-serif; padding: 20px; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
        th { background-color: #f4f4f4; }
    </style>
</head>
<body>

<h2>Danh sách Nhà cung cấp</h2>

<table>
    <thead>
    <tr>
        <th>ID</th>
        <th>Tên nhà cung cấp</th>
        <th>Số điện thoại</th>
        <th>Email</th>
        <th>Địa chỉ</th>
        <th>Ghi chú</th>
    </tr>
    </thead>
    <tbody>

    <c:forEach var="s" items="${supplierList}">
        <tr>
            <td>${s.supplierID}</td>
            <td>${s.supplierName}</td>
            <td>${s.phone}</td>
            <td>${s.email}</td>
            <td>${s.address}</td>
            <td>${s.description}</td>
        </tr>
    </c:forEach>
    </tbody>
</table>

</body>
</html>