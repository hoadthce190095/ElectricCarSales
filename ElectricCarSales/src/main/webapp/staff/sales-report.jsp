<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Báo cáo Doanh thu - Quản lý</title>
    <link rel="stylesheet" href="../assets/css/staff.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <!-- Thêm thư viện Chart.js để vẽ biểu đồ -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
    <div class="container mt-4">
        <h2>Báo cáo Doanh thu & Thống kê</h2>
        <hr>

        <!-- Các thẻ tóm tắt (Summary Cards) -->
        <div class="row mb-4">
            <div class="col-md-4">
                <div class="card text-white bg-success mb-3">
                    <div class="card-header">Tổng Doanh Thu Tháng</div>
                    <div class="card-body">
                        <h4 class="card-title">12,500,000,000 VNĐ</h4>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card text-white bg-info mb-3">
                    <div class="card-header">Xe Đã Bán</div>
                    <div class="card-body">
                        <h4 class="card-title">15 chiếc</h4>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card text-white bg-warning mb-3">
                    <div class="card-header">Tư Vấn Đang Chờ</div>
                    <div class="card-body">
                        <h4 class="card-title">8 yêu cầu</h4>
                    </div>
                </div>
            </div>
        </div>

        <!-- Biểu đồ -->
        <div class="row">
            <div class="col-md-8 offset-md-2">
                <canvas id="salesChart"></canvas>
            </div>
        </div>
    </div>

    <script src="../assets/js/report.js"></script>
    <script>
        // Khởi tạo biểu đồ mẫu bằng Chart.js
        const ctx = document.getElementById('salesChart').getContext('2d');
        const salesChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['Tháng 6', 'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10'],
                datasets: [{
                    label: 'Số lượng xe bán ra',
                    data: [5, 8, 12, 10, 15],
                    backgroundColor: 'rgba(54, 162, 235, 0.5)',
                    borderColor: 'rgba(54, 162, 235, 1)',
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                scales: {
                    y: { beginAtZero: true }
                }
            }
        });
    </script>
</body>
</html>
