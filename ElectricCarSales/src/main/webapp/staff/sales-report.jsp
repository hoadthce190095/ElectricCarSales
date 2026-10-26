<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Báo cáo Bán hàng & Trạng thái Giao dịch - VinFast Staff</title>
    <link rel="stylesheet" href="../assets/css/staff.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
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
                    <a class="nav-link text-white" href="${pageContext.request.contextPath}/staff/transactions">Giao dịch bán xe</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link active fw-bold text-info" href="${pageContext.request.contextPath}/staff/report">Báo cáo &amp; Thống kê</a>
                </li>
            </ul>
        </div>
    </nav>

    <div class="container-fluid px-4 mt-4 mb-5">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <h3 class="fw-bold mb-1">Báo Cáo Bán Hàng &amp; Trạng Thái Giao Dịch</h3>
                <p class="text-secondary mb-0">Hỗ trợ Quản lý và Nhân viên theo dõi kết quả kinh doanh và tiến độ toàn bộ giao dịch.</p>
            </div>
            <div class="btn-group" role="group">
                <button type="button" class="btn btn-primary active" id="tabSalesBtn" onclick="switchTab('sales')">Báo Cáo Doanh Thu</button>
                <button type="button" class="btn btn-outline-info" id="tabStatusBtn" onclick="switchTab('status')">Báo Cáo Trạng Thái Giao Dịch</button>
            </div>
        </div>

        <!-- PHẦN 1: BÁO CÁO DOANH THU (Sales Report) -->
        <div id="salesReportSection">
            <!-- Thẻ tóm tắt chỉ số -->
            <div class="row g-3 mb-4">
                <div class="col-md-4">
                    <div class="card text-white bg-success bg-opacity-25 border border-success p-3 rounded-3">
                        <span class="text-success small text-uppercase fw-bold">Tổng Doanh Thu Bán Xe</span>
                        <h3 class="fw-bold mt-2 text-white">
                            <fmt:formatNumber value="${totalRevenue}" type="currency" currencySymbol="VNĐ" maxFractionDigits="0"/>
                        </h3>
                        <small class="text-success-emphasis">Dữ liệu ghi nhận từ hệ thống</small>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="card text-white bg-info bg-opacity-25 border border-info p-3 rounded-3">
                        <span class="text-info small text-uppercase fw-bold">Số Lượng Xe Đã Bán</span>
                        <h3 class="fw-bold mt-2 text-white">${carsSold} chiếc</h3>
                        <small class="text-info-emphasis">Giao dịch đã xác nhận</small>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="card text-white bg-warning bg-opacity-25 border border-warning p-3 rounded-3">
                        <span class="text-warning small text-uppercase fw-bold">Yêu Cầu Tư Vấn Đang Chờ</span>
                        <h3 class="fw-bold mt-2 text-white">${pendingConsultations} yêu cầu</h3>
                        <small class="text-warning-emphasis">
                            <a href="${pageContext.request.contextPath}/staff/consultations" class="text-warning text-decoration-underline">Xử lý ngay</a>
                        </small>
                    </div>
                </div>
            </div>

            <!-- Biểu đồ doanh thu -->
            <div class="row g-4">
                <div class="col-lg-8">
                    <div class="card bg-dark border-secondary p-3">
                        <h5 class="fw-bold text-white mb-3">Biểu Đồ Doanh Thu &amp; Số Xe Bán Ra</h5>
                        <canvas id="salesChart" height="130"></canvas>
                    </div>
                </div>
                <div class="col-lg-4">
                    <div class="card bg-dark border-secondary p-3">
                        <h5 class="fw-bold text-white mb-3">Doanh Số Theo Dòng Xe</h5>
                        <ul class="list-group list-group-flush bg-dark">
                            <c:choose>
                                <c:when test="${not empty vehicleStats}">
                                    <c:forEach var="v" items="${vehicleStats}">
                                        <li class="list-group-item bg-dark text-white border-secondary d-flex justify-content-between">
                                            <span>${v.modelName}</span>
                                            <strong class="text-info">${v.count} xe (<fmt:formatNumber value="${v.revenue}" type="currency" currencySymbol="VNĐ" maxFractionDigits="0"/>)</strong>
                                        </li>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <li class="list-group-item bg-dark text-secondary border-secondary text-center py-3">
                                        Chưa có dữ liệu theo dòng xe.
                                    </li>
                                </c:otherwise>
                            </c:choose>
                        </ul>
                    </div>
                </div>
            </div>
        </div>

        <!-- PHẦN 2: BÁO CÁO TRẠNG THÁI GIAO DỊCH (Transaction Status Report) -->
        <div id="transactionStatusSection" style="display: none;">
            <div class="row g-3 mb-4">
                <div class="col-md-3">
                    <div class="card bg-dark border-primary p-3">
                        <span class="text-primary small fw-bold">1. ĐẶT CỌC (DEPOSITED)</span>
                        <h3 class="fw-bold text-white mt-1">${countDeposited}</h3>
                        <small class="text-secondary">Đã cọc giữ chỗ xe</small>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card bg-dark border-warning p-3">
                        <span class="text-warning small fw-bold">2. ĐANG XỬ LÝ (PROCESSING)</span>
                        <h3 class="fw-bold text-white mt-1">${countProcessing}</h3>
                        <small class="text-secondary">Đang duyệt hồ sơ / ngân hàng</small>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card bg-dark border-success p-3">
                        <span class="text-success small fw-bold">3. HOÀN TẤT (COMPLETED)</span>
                        <h3 class="fw-bold text-white mt-1">${countCompleted}</h3>
                        <small class="text-secondary">Đã bàn giao xe cho khách</small>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card bg-dark border-danger p-3">
                        <span class="text-danger small fw-bold">4. ĐÃ HỦY (CANCELLED)</span>
                        <h3 class="fw-bold text-white mt-1">${countCancelled}</h3>
                        <small class="text-secondary">Khách rút cọc / hủy</small>
                    </div>
                </div>
            </div>

            <div class="row g-4">
                <div class="col-lg-5">
                    <div class="card bg-dark border-secondary p-3">
                        <h5 class="fw-bold text-white mb-3">Tỷ Lệ Phân Bổ Trạng Thái Giao Dịch</h5>
                        <canvas id="statusDoughnutChart" height="200"></canvas>
                    </div>
                </div>
                <div class="col-lg-7">
                    <div class="card bg-dark border-secondary p-3">
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <h5 class="fw-bold text-white mb-0">Tiến Độ Các Giao Dịch Đang Thực Hiện</h5>
                            <a href="${pageContext.request.contextPath}/staff/transactions" class="text-info text-decoration-none small">
                                Quản lý chi tiết
                            </a>
                        </div>
                        <table class="table table-dark table-hover table-striped align-middle mb-0">
                            <thead class="table-secondary text-dark">
                                <tr>
                                    <th>Mã GD</th>
                                    <th>Khách Hàng</th>
                                    <th>Xe</th>
                                    <th>Trạng Thái</th>
                                    <th>Thao Tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:choose>
                                    <c:when test="${not empty orders}">
                                        <c:forEach var="o" items="${orders}">
                                            <tr>
                                                <td class="text-info fw-bold">#ORD-${o.id}</td>
                                                <td>${empty o.customerName ? '-' : o.customerName}</td>
                                                <td>${empty o.vehicleName ? '-' : o.vehicleName}</td>
                                                <td><span class="badge bg-info">${empty o.statusName ? 'Đang xử lý' : o.statusName}</span></td>
                                                <td><a href="${pageContext.request.contextPath}/staff/transaction-detail?id=${o.id}" class="btn btn-sm btn-outline-info">Chi tiết</a></td>
                                            </tr>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <tr>
                                            <td colspan="5" class="text-center py-3 text-secondary">Hiện chưa có dữ liệu tiến độ giao dịch nào.</td>
                                        </tr>
                                    </c:otherwise>
                                </c:choose>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script>
        function switchTab(tab) {
            const salesSec = document.getElementById('salesReportSection');
            const statusSec = document.getElementById('transactionStatusSection');
            const salesBtn = document.getElementById('tabSalesBtn');
            const statusBtn = document.getElementById('tabStatusBtn');

            if (tab === 'sales') {
                salesSec.style.display = 'block';
                statusSec.style.display = 'none';
                salesBtn.className = 'btn btn-primary active';
                statusBtn.className = 'btn btn-outline-info';
            } else {
                salesSec.style.display = 'none';
                statusSec.style.display = 'block';
                salesBtn.className = 'btn btn-outline-primary';
                statusBtn.className = 'btn btn-info text-dark active';
            }
        }

        // Khởi tạo biểu đồ Doanh thu (Chart.js)
        const ctxSales = document.getElementById('salesChart').getContext('2d');
        new Chart(ctxSales, {
            type: 'bar',
            data: {
                labels: ['Tổng số xe đã bán'],
                datasets: [{
                    label: 'Số lượng xe bán (chiếc)',
                    data: [${carsSold}],
                    backgroundColor: 'rgba(56, 189, 248, 0.7)',
                    borderColor: '#38bdf8',
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                scales: {
                    y: { beginAtZero: true, grid: { color: '#272a30' } },
                    x: { grid: { color: '#272a30' } }
                }
            }
        });

        // Khởi tạo biểu đồ tròn Trạng thái giao dịch
        const ctxStatus = document.getElementById('statusDoughnutChart').getContext('2d');
        new Chart(ctxStatus, {
            type: 'doughnut',
            data: {
                labels: ['Đặt cọc', 'Đang xử lý', 'Hoàn tất bàn giao', 'Đã hủy'],
                datasets: [{
                    data: [${countDeposited}, ${countProcessing}, ${countCompleted}, ${countCancelled}],
                    backgroundColor: ['#22c55e', '#3b82f6', '#06b6d4', '#ef4444']
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { labels: { color: '#fff' } }
                }
            }
        });
    </script>
</body>
</html>
