<%--
  Created by IntelliJ IDEA.
  User: Hoa Hà
  Date: 10/14/2026
  Time: 10:21 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh Sách Xe Điện VinFast - ElectricCarSale</title>
    <!-- Google Fonts -->
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary-color: #0068ff;
            --primary-dark: #0052cc;
            --text-main: #111827;
            --text-muted: #4b5563;
            --bg-body: #f7f8fa;
            --card-bg: #ffffff;
            --border-color: #e5e7eb;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Be Vietnam Pro', sans-serif;
            background-color: var(--bg-body);
            color: var(--text-main);
            padding: 40px 20px;
            line-height: 1.5;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
        }

        .page-header {
            text-align: center;
            margin-bottom: 40px;
        }

        .page-header h2 {
            font-size: 32px;
            font-weight: 700;
            color: var(--text-main);
            margin-bottom: 10px;
        }

        .page-header p {
            color: var(--text-muted);
            font-size: 16px;
        }

        .vehicle-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
            gap: 24px;
        }

        .vehicle-card {
            background: var(--card-bg);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 24px;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            transition: transform 0.2s ease, box-shadow 0.2s ease;
        }

        .vehicle-card:hover {
            transform: translateY(-4px);
            box-shadow: 0 10px 24px rgba(0, 0, 0, 0.08);
        }

        .vehicle-tag {
            font-size: 12px;
            font-weight: 700;
            color: var(--primary-color);
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 6px;
            display: block;
        }

        .vehicle-title {
            font-size: 22px;
            font-weight: 700;
            color: var(--text-main);
            margin-bottom: 10px;
        }

        .vehicle-desc {
            color: var(--text-muted);
            font-size: 14px;
            margin-bottom: 20px;
            min-height: 42px;
        }

        .vehicle-specs {
            background: #f9fafb;
            border-radius: 8px;
            padding: 12px;
            margin-bottom: 20px;
            font-size: 13px;
            color: #374151;
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 8px;
        }

        .spec-item {
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .vehicle-actions {
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-top: 1px solid var(--border-color);
            padding-top: 16px;
            margin-top: auto;
        }

        .btn-detail {
            background: var(--primary-color);
            color: white;
            padding: 10px 20px;
            border-radius: 8px;
            text-decoration: none;
            font-size: 14px;
            font-weight: 600;
            transition: background 0.2s;
        }

        .btn-detail:hover {
            background: var(--primary-dark);
        }

        .compare-label {
            font-size: 13px;
            color: var(--text-muted);
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 6px;
            user-select: none;
        }

        .compare-label input {
            width: 16px;
            height: 16px;
            cursor: pointer;
            accent-color: var(--primary-color);
        }

        .empty-state {
            grid-column: 1 / -1;
            text-align: center;
            padding: 60px;
            background: white;
            border-radius: 16px;
            border: 1px solid var(--border-color);
        }

        .empty-state p {
            font-size: 16px;
            color: var(--text-muted);
        }
    </style>
</head>
<body>

<div class="container">
    <div class="page-header">
        <h2>Danh Sách Mẫu Xe Điện VinFast</h2>
        <p>Khám phá thông tin chi tiết, cấu hình và lựa chọn mẫu xe phù hợp với hành trình của bạn.</p>
    </div>

    <div class="vehicle-grid">
        <c:choose>
            <c:when test="${not empty carList}">
                <c:forEach var="car" items="${carList}">
                    <div class="vehicle-card">
                        <div>
                            <span class="vehicle-tag">VinFast Electric Line</span>
                            <h3 class="vehicle-title">${car.modelName}</h3>
                            <p class="vehicle-desc">${car.description}</p>

                            <!-- Khung thông số cơ bản (có thể mở rộng từ specification nếu muốn) -->
                            <div class="vehicle-specs">
                                <div class="spec-item">⚡ <span>Động cơ điện thông minh</span></div>
                                <div class="spec-item">🛡️ <span>Tiêu chuẩn an toàn cao</span></div>
                            </div>
                        </div>

                        <div class="vehicle-actions">
                            <a href="detail?id=${car.modelID}" class="btn-detail">Xem chi tiết</a>
                            <label class="compare-label">
                                <input type="checkbox" name="compare" value="${car.modelID}"> So sánh
                            </label>
                        </div>
                    </div>
                </c:forEach>
            </c:when>
            <c:otherwise>
                <div class="empty-state">
                    <p>Hiện tại chưa có mẫu xe nào trong cơ sở dữ liệu hệ thống.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

</body>
</html>