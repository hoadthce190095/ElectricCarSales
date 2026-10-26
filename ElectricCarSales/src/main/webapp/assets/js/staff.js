"use strict";
(() => {
    const app = document.querySelector("[data-staff-app]");
    if (!app) return;
    const contextPath = app.dataset.contextPath || "";
    const $ = (selector, root = document) => root.querySelector(selector);
    const content = $("#staff-content");
    const nav = $("#staff-nav");
    const picker = $("#role-picker");
    const names = { sales: "Nhân viên kinh doanh", operations: "Nhân viên vận hành", accountant: "Kế toán", manager: "Quản lý cửa hàng" };
    const initials = { sales: "AN", operations: "MT", accountant: "LH", manager: "TH" };
    const menus = {
        sales: [{
            group: "CÔNG VIỆC", items: [["dashboard", "⌂", "Tổng quan"],
            ["customers", "♙", "Khách hàng"],
            ["consultations", "✧", "Yêu cầu tư vấn", 3]]
        }, {
            group: "BÁN HÀNG", items: [["transactions", "▤", "Giao dịch bán xe"],
            ["deposits", "◉", "Đặt cọc"],
            ["contracts", "▧", "Hợp đồng"],
            ["handover", "↗", "Bàn giao xe"]]
        }],
        operations: [{
            group: "VẬN HÀNH", items: [["dashboard", "⌂", "Tổng quan"],
            ["inventory", "▦", "Quản lý tồn kho"],
            ["vehicle-movements", "⇄", "Nhập / xuất xe"],
            ["vehicle-status", "◷", "Trạng thái xe"],
            ["handover", "↗", "Bàn giao xe"]]
        }],
        accountant: [{
            group: "TÀI CHÍNH", items: [["dashboard", "⌂", "Tổng quan"],
            ["payments", "◉", "Thanh toán"],
            ["installments", "▤", "Trả góp"],
            ["debts", "◷", "Theo dõi công nợ"],
            ["contracts", "▧", "Hợp đồng & hóa đơn"]]
        }],
        manager: [{
            group: "QUẢN LÝ", items: [["dashboard", "⌂", "Tổng quan"],
            ["inventory", "▦", "Tổng quan tồn kho"],
            ["transactions", "▤", "Bán hàng & giao dịch"]]
        }, {
            group: "BÁO CÁO", items: [["revenue-report", "▥", "Báo cáo doanh thu"],
            ["vehicle-report", "▧", "Xe bán ra"],
            ["transaction-report", "◷", "Báo cáo giao dịch"],
            ["inventory-report", "▦", "Báo cáo tồn kho"]]
        }]
    };
    const labels = Object.fromEntries(Object.values(menus).flatMap(groups => groups.flatMap(group => group.items.map(item => [item[0], item[2]]))));
    const demoRows = {
        customers: [["KH-24081", "Nguyễn Minh Anh", "0903 221 480", "VF 8 Plus", "Đang tư vấn", "success"],
        ["KH-24076", "Trần Quốc Bảo", "0918 445 203", "VF 6 Plus", "Chờ liên hệ", "warning"],
        ["KH-24062", "Lê Hoàng Yến", "0982 115 908", "VF 9 Eco", "Đang giao dịch", "info"],
        ["KH-24051", "Phạm Đức Long", "0908 681 229", "VF 7 Plus", "Đã đặt cọc", "success"]],
        consultations: [["YC-0184", "Nguyễn Minh Anh", "VF 8 Plus", "Hôm nay · 09:20", "Chưa tiếp nhận", "warning"],
        ["YC-0182", "Trần Quốc Bảo", "VF 6 Plus", "Hôm nay · 08:45", "Đang xử lý", "info"],
        ["YC-0179", "Lê Hoàng Yến", "VF 9 Eco", "Hôm qua · 16:10", "Đã liên hệ", "success"],
        ["YC-0176", "Võ Thanh Hà", "VF 7 Plus", "Hôm qua · 14:32", "Chưa tiếp nhận", "warning"]],
        transactions: [["GD-00842", "Nguyễn Minh Anh", "VF 8 Plus", "1.299.000.000 ₫", "Đặt cọc", "success"],
        ["GD-00839", "Lê Hoàng Yến", "VF 9 Eco", "1.499.000.000 ₫", "Chờ hợp đồng", "warning"],
        ["GD-00831", "Trần Quốc Bảo", "VF 6 Plus", "765.000.000 ₫", "Đang xử lý", "info"],
        ["GD-00825", "Phạm Đức Long", "VF 7 Plus", "999.000.000 ₫", "Hoàn tất", "success"]],
        deposits: [["DC-00481", "GD-00842 · Nguyễn Minh Anh", "VF 8 Plus", "50.000.000 ₫", "Đã xác nhận", "success"],
        ["DC-00479", "GD-00839 · Lê Hoàng Yến", "VF 9 Eco", "30.000.000 ₫", "Chờ xác nhận", "warning"],
        ["DC-00472", "GD-00831 · Trần Quốc Bảo", "VF 6 Plus", "20.000.000 ₫", "Đã xác nhận", "success"]],
        contracts: [["HD-00321", "Nguyễn Minh Anh", "VF 8 Plus", "02/10/2026", "Chờ ký", "warning"],
        ["HD-00318", "Lê Hoàng Yến", "VF 9 Eco", "30/09/2026", "Đang hoàn thiện", "info"],
        ["HD-00309", "Phạm Đức Long", "VF 7 Plus", "28/09/2026", "Đã ký", "success"]],
        handover: [["BG-00119", "Phạm Đức Long", "VF 7 Plus · Trắng", "Đã thanh toán", "Sẵn sàng", "success"],
        ["BG-00116", "Nguyễn Minh Anh", "VF 8 Plus · Đen", "Đang hoàn tất hồ sơ", "Đang chuẩn bị", "warning"],
        ["BG-00111", "Trần Quốc Bảo", "VF 6 Plus · Xanh", "Chờ thanh toán", "Chưa sẵn sàng", "danger"]],
        inventory: [["VF 3 Plus", "Đỏ · Trắng · Xanh", "Showroom TP.HCM", "12", "Có sẵn", "success"],
        ["VF 6 Plus", "Đen · Trắng", "Kho Bình Dương", "8", "Có sẵn", "success"],
        ["VF 7 Plus", "Đỏ · Đen · Trắng", "Showroom Hà Nội", "5", "Đã đặt cọc", "warning"],
        ["VF 8 Plus", "Đen · Xanh", "Kho Bình Dương", "3", "Có sẵn", "success"],
        ["VF 9 Eco", "Trắng · Đen", "Showroom TP.HCM", "2", "Sắp hết", "danger"]],
        "vehicle-movements": [["NX-00217", "VF 8 Plus · 5 xe", "Kho Bình Dương → Showroom HCM", "11/10/2026", "Đang vận chuyển", "info"],
        ["NX-00214", "VF 6 Plus · 3 xe", "Nhà máy → Kho Bình Dương", "10/10/2026", "Đã nhập kho", "success"],
        ["NX-00209", "VF 7 Plus · 2 xe", "Showroom HN → Khách hàng", "09/10/2026", "Đã xuất", "success"]],
        "vehicle-status": [["VF8-24-07142", "VF 8 Plus · Đen", "Kho Bình Dương", "Có sẵn", "success"],
        ["VF7-24-03185", "VF 7 Plus · Trắng", "Showroom Hà Nội", "Đã đặt cọc", "warning"],
        ["VF6-24-10224", "VF 6 Plus · Xanh", "Showroom TP.HCM", "Đã bàn giao", "info"]],
        payments: [["TT-01982", "GD-00842 · Nguyễn Minh Anh", "Chuyển khoản", "50.000.000 ₫", "Chờ xác minh", "warning"],
        ["TT-01978", "GD-00839 · Lê Hoàng Yến", "Thẻ tín dụng", "30.000.000 ₫", "Đã xác nhận", "success"],
        ["TT-01961", "GD-00831 · Trần Quốc Bảo", "Chuyển khoản", "20.000.000 ₫", "Chênh lệch", "danger"]],
        installments: [["TG-00124", "Lê Hoàng Yến · VF 9 Eco", "Trả trước 30%", "24 tháng", "Đang hoạt động", "success"],
        ["TG-00119", "Trần Quốc Bảo · VF 6 Plus", "Trả trước 40%", "18 tháng", "Chờ duyệt", "warning"]],
        debts: [["CN-00084", "Lê Hoàng Yến", "GD-00839 · VF 9 Eco", "1.049.300.000 ₫", "15/11/2026", "Trong hạn", "success"],
        ["CN-00079", "Trần Quốc Bảo", "GD-00831 · VF 6 Plus", "459.000.000 ₫", "08/10/2026", "Quá hạn", "danger"]],
        "revenue-report": [["Tháng 10/2026", "36 xe", "42.850.000.000 ₫", "+12,4%", "Đang cập nhật", "info"],
        ["Tháng 09/2026", "41 xe", "46.220.000.000 ₫", "+8,1%", "Đã chốt", "success"],
        ["Tháng 08/2026", "34 xe", "38.600.000.000 ₫", "+4,6%", "Đã chốt", "success"]],
        "vehicle-report": [["VF 6", "14 xe", "10.710.000.000 ₫", "Sales Team 1", "Tăng 8%", "success"],
        ["VF 8", "11 xe", "14.289.000.000 ₫", "Sales Team 2", "Tăng 12%", "success"],
        ["VF 7", "7 xe", "6.993.000.000 ₫", "Sales Team 1", "Giảm 2%", "warning"],
        ["VF 9", "4 xe", "5.996.000.000 ₫", "Sales Team 3", "Tăng 5%", "success"]],
        "transaction-report": [["Đặt cọc", "18 giao dịch", "50%", "21.500.000.000 ₫", "success"],
        ["Hợp đồng", "9 giao dịch", "25%", "10.275.000.000 ₫", "info"],
        ["Thanh toán", "6 giao dịch", "17%", "7.150.000.000 ₫", "warning"],
        ["Bàn giao", "3 giao dịch", "8%", "3.925.000.000 ₫", "success"]],
        "inventory-report": [["Showroom TP.HCM", "26 xe", "18 có sẵn", "5 đã cọc", "Ổn định", "success"],
        ["Kho Bình Dương", "42 xe", "36 có sẵn", "4 đã cọc", "Ổn định", "success"],
        ["Showroom Hà Nội", "18 xe", "12 có sẵn", "4 đã cọc", "Cần theo dõi", "warning"]]
    };
    const titles = {
        customers: ["Khách hàng", "Danh sách khách hàng và mối quan tâm được phân công cho bạn.", ["MÃ KHÁCH HÀNG", "KHÁCH HÀNG", "LIÊN HỆ", "QUAN TÂM", "TRẠNG THÁI"]],
        consultations: ["Yêu cầu tư vấn", "Tiếp nhận và theo dõi những yêu cầu tư vấn sản phẩm.", ["MÃ YÊU CẦU", "KHÁCH HÀNG", "MẪU XE", "THỜI GIAN", "TRẠNG THÁI"]],
        transactions: ["Giao dịch bán xe", "Theo dõi tiến độ giao dịch từ tư vấn đến bàn giao.", ["MÃ GIAO DỊCH", "KHÁCH HÀNG", "MẪU XE", "GIÁ TRỊ", "TIẾN ĐỘ"]],
        deposits: ["Đặt cọc", "Theo dõi khoản đặt cọc và tình trạng xác nhận.", ["MÃ ĐẶT CỌC", "GIAO DỊCH", "MẪU XE", "SỐ TIỀN", "TRẠNG THÁI"]],
        contracts: ["Hợp đồng & hóa đơn", "Hồ sơ hợp đồng liên kết với các giao dịch bán xe.", ["MÃ HỒ SƠ", "KHÁCH HÀNG", "MẪU XE", "CẬP NHẬT", "TRẠNG THÁI"]],
        handover: ["Bàn giao xe", "Kiểm tra điều kiện và theo dõi lịch bàn giao xe.", ["MÃ BÀN GIAO", "KHÁCH HÀNG", "XE", "ĐIỀU KIỆN", "TRẠNG THÁI"]],
        inventory: ["Quản lý tồn kho", "Theo dõi số lượng xe theo mẫu, màu sắc và địa điểm.", ["MẪU XE", "MÀU SẮC", "VỊ TRÍ", "SỐ LƯỢNG", "TRẠNG THÁI"]],
        "vehicle-movements": ["Nhập / xuất xe", "Theo dõi các lô xe nhập kho, xuất kho và điều chuyển.", ["MÃ PHIẾU", "LÔ XE", "LỘ TRÌNH", "NGÀY", "TRẠNG THÁI"]],
        "vehicle-status": ["Trạng thái xe", "Tra cứu trạng thái hiện tại của từng xe trong hệ thống.", ["SỐ KHUNG", "XE", "VỊ TRÍ", "TRẠNG THÁI"]],
        payments: ["Thanh toán", "Kiểm tra chứng từ và xác minh khoản tiền thực nhận.", ["MÃ THANH TOÁN", "GIAO DỊCH", "PHƯƠNG THỨC", "SỐ TIỀN", "TRẠNG THÁI"]],
        installments: ["Trả góp", "Theo dõi kế hoạch trả góp và tiến độ thanh toán.", ["MÃ KẾ HOẠCH", "KHÁCH HÀNG", "ĐIỀU KHOẢN", "KỲ HẠN", "TRẠNG THÁI"]],
        debts: ["Theo dõi công nợ", "Theo dõi khoản còn phải thu và thời hạn thanh toán.", ["MÃ CÔNG NỢ", "KHÁCH HÀNG", "GIAO DỊCH", "CÒN PHẢI THU", "ĐẾN HẠN", "TRẠNG THÁI"]],
        "revenue-report": ["Báo cáo doanh thu", "Tổng hợp doanh thu theo kỳ và tình hình thực hiện.", ["KỲ BÁO CÁO", "XE ĐÃ BÁN", "DOANH THU", "THAY ĐỔI", "TRẠNG THÁI"]],
        "vehicle-report": ["Báo cáo xe bán ra", "Phân tích số lượng xe bán theo mẫu xe và đội ngũ kinh doanh.", ["MẪU XE", "SỐ LƯỢNG", "DOANH THU", "NHÂN VIÊN", "SO VỚI KỲ TRƯỚC"]],
        "transaction-report": ["Báo cáo giao dịch", "Theo dõi số lượng giao dịch qua từng giai đoạn xử lý.", ["GIAI ĐOẠN", "SỐ LƯỢNG", "TỶ TRỌNG", "GIÁ TRỊ", "TRẠNG THÁI"]],
        "inventory-report": ["Báo cáo tồn kho", "Phân bố lượng xe theo địa điểm và tình trạng sẵn sàng.", ["ĐỊA ĐIỂM", "TỔNG XE", "CÓ SẴN", "ĐÃ ĐẶT CỌC", "GHI CHÚ"]]
    };
    let currentRole = "sales", currentView = "dashboard", toastTimer;
    const roleDashboard = {
        sales: {
            title: "Tổng quan công việc", kicker: "KHÔNG GIAN KINH DOANH", intro: "Chào buổi sáng, An. Đây là những việc đang cần bạn theo dõi hôm nay.", metrics: [["Khách hàng phụ trách", "128", "+8 khách hàng tháng này", "♙", ""],
            ["Yêu cầu cần xử lý", "12", "3 yêu cầu mới hôm nay", "✧", "amber"],
            ["Giao dịch đang mở", "18", "5 giao dịch chờ cập nhật", "▤", "blue"],
            ["Doanh số tháng này", "8,45 tỷ ₫", "+12,4% so với tháng trước", "↗", "green"]], tasks: [["Yêu cầu tư vấn mới", "3 yêu cầu chưa tiếp nhận", "✧", ""],
            ["Hợp đồng chờ ký", "4 hồ sơ cần theo dõi", "▧", "amber"],
            ["Lịch bàn giao hôm nay", "2 xe · 14:00, 16:30", "↗", "green"]], caption: "Giao dịch gần đây", rows: "transactions"
        },
        operations: {
            title: "Tổng quan vận hành", kicker: "TRUNG TÂM VẬN HÀNH", intro: "Theo dõi trạng thái xe và các hoạt động kho vận trong ngày.", metrics: [["Tổng xe trong kho", "86", "3 địa điểm đang hoạt động", "▦", ""],
            ["Xe có sẵn", "66", "Sẵn sàng giao dịch", "✓", "green"],
            ["Đang vận chuyển", "7", "2 lô xe cần cập nhật", "⇄", "blue"],
            ["Cảnh báo tồn thấp", "3", "Cần theo dõi bổ sung", "!", "amber"]], tasks: [["Lô xe sắp đến", "VF 8 Plus · 5 xe · 15:30", "⇄", ""],
            ["Xe chờ cập nhật vị trí", "4 xe chưa xác nhận vị trí", "▦", "amber"],
            ["Lịch bàn giao hôm nay", "2 xe cần chuẩn bị", "↗", "green"]], caption: "Trạng thái xe mới cập nhật", rows: "inventory"
        },
        accountant: {
            title: "Tổng quan tài chính", kicker: "TRUNG TÂM TÀI CHÍNH", intro: "Các khoản thanh toán và hồ sơ tài chính đang cần được rà soát.", metrics: [["Thanh toán chờ xác minh", "8", "2 khoản mới hôm nay", "◉", "amber"],
            ["Giá trị đã thu tháng này", "12,68 tỷ ₫", "+9,2% so với tháng trước", "↗", "green"],
            ["Hồ sơ trả góp", "24", "3 hồ sơ chờ duyệt", "▤", "blue"],
            ["Khoản công nợ quá hạn", "2", "Cần kiểm tra trong hôm nay", "!", "amber"]], tasks: [["Thanh toán cần xác minh", "8 khoản · 1 có chênh lệch", "◉", "amber"],
            ["Hồ sơ trả góp mới", "3 hồ sơ chờ duyệt", "▤", ""],
            ["Công nợ đến hạn", "5 khoản trong 7 ngày tới", "◷", "green"]], caption: "Thanh toán gần đây", rows: "payments"
        },
        manager: {
            title: "Tổng quan kinh doanh", kicker: "ĐIỀU HÀNH SHOWROOM", intro: "Bức tranh hoạt động kinh doanh và tồn kho trong tháng 10/2026.", metrics: [["Doanh thu tháng này", "42,85 tỷ ₫", "+12,4% so với tháng trước", "↗", "green"],
            ["Xe đã bán", "36 xe", "Mục tiêu tháng: 48 xe", "▤", "blue"],
            ["Giao dịch đang mở", "27", "4 giao dịch có chênh lệch", "◷", "amber"],
            ["Tổng xe tồn kho", "86 xe", "66 xe sẵn sàng bán", "▦", ""]], tasks: [["Giao dịch cần xem xét", "4 khoản thanh toán chênh lệch", "◷", "amber"],
            ["Cảnh báo tồn kho thấp", "3 mẫu xe cần theo dõi", "▦", ""],
            ["Tiến độ bàn giao", "9 xe đang hoàn tất hồ sơ", "↗", "green"]], caption: "Giao dịch bán hàng", rows: "transactions"
        }
    };
    const money = value => value;
    function esc(value) {
        const entities = { "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;" };
        return String(value).replace(/[&<>"']/g, character => entities[character]);
    }
    function renderNav() {
        nav.innerHTML = menus[currentRole].map(group => `<div class="nav-group-label">${group.group}</div>${group.items.map(([id, icon, label, badge]) => `<a href="#${id}" class="staff-nav-link ${currentView === id ? "active" : ""}" data-view="${id}">
            <span class="staff-nav-icon">${icon}</span>
            <span>${label}</span>${badge ? `<span class="nav-badge">${badge}</span>` : ""}</a>`).join("")}`).join("");
    }
    function heading(title, kicker, intro, action = "") {
        return `<div class="page-heading">
            <div>
            <div class="eyebrow">
            <span class="eyebrow-line">
            </span>${kicker}</div>
            <h1>${title}</h1>
            <p>${intro}</p>
            </div>
            <div class="heading-actions">${action}</div>
            </div>`;
    }
    function note() {
        return `<div class="demo-note">
            <span>ⓘ</span>
            <div>
            <b>Bản xem trước giao diện</b> · Số liệu và hồ sơ trên trang là dữ liệu minh họa, chưa kết nối cơ sở dữ liệu.</div>
            </div>`;
    }
    function metricCards(metrics) {
        return `<section class="metric-grid" aria-label="Chỉ số tổng quan">${metrics.map(([label, value, foot, icon, color]) => `<article class="metric-card">
            <div class="metric-head">
            <span>${label}</span>
            <span class="metric-icon ${color}">${icon}</span>
            </div>
            <strong>${value}</strong>
            <div class="metric-foot">
            <span class="trend-up">${foot.split(" ")[0]}</span>
            <span>${foot.split(" ").slice(1).join(" ")}</span>
            </div>
            </article>`).join("")}</section>`;
    }
    function miniTable(key) {
        const meta = titles[key], rows = (demoRows[key] || []).slice(0, 4);
        return `<section class="panel table-panel">
            <div class="panel-head">
            <div>
            <h2 class="panel-title">${roleDashboard[currentRole].caption}</h2>
            <p class="panel-subtitle">Cập nhật hoạt động mới nhất</p>
            </div>
            ${key === "transactions" ? `<a href="${contextPath}/staff/transactions" class="text-button" style="text-decoration:none;">Xem tất cả →</a>` : `<button class="text-button" data-view="${key}">Xem tất cả →</button>`}
            </div>
            <div class="table-wrap">
            <table>
            <thead>
            <tr>${meta[2].map(x => `<th>${x}</th>`).join("")}</tr>
            </thead>
            <tbody>${rows.map(row => `<tr>${row.slice(0, -2).map((cell, i) => `<td>${i === 0 || i === 1 ? `<span class="cell-primary">${cell}</span>` : esc(cell)}</td>`).join("")}<td>
            <span class="status-pill ${row.at(-1)}">${row[row.length - 2]}</span>
            </td>
            </tr>`).join("")}</tbody>
            </table>
            </div>
            </section>`;
    }
    function renderDashboard() {
        const d = roleDashboard[currentRole];
        const bars = [38, 55, 48, 73, 61, 88, 67, 95, 72, 84, 62, 100];
        return `${heading(d.title, d.kicker, d.intro, `<span class="live-indicator">
            <i>
            </i> Cập nhật trực tiếp</span>${currentRole === "manager" ? `<a href="${contextPath}/staff/transactions" class="button button-secondary" style="text-decoration:none;">Xem giao dịch →</a>` : `<a href="${contextPath}/staff/transactions" class="button" style="text-decoration:none;"><span class="button-icon">＋</span> Quản lý giao dịch</a>`}`)}${note()}${metricCards(d.metrics)}<div class="dashboard-grid">
            <section class="panel">
            <div class="panel-head">
            <div>
            <h2 class="panel-title">${currentRole === "manager" ? "Doanh thu bán xe" : "Hiệu quả hoạt động"}</h2>
            <p class="panel-subtitle">Tổng hợp 12 tháng gần nhất</p>
            </div>
            <div class="chart-legend">
            <span>
            <i class="legend-dot">
            </i>${currentRole === "accountant" ? "Đã thu" : "Kỳ này"}</span>
            <span>
            <i class="legend-dot gray">
            </i>Kỳ trước</span>
            <select class="chart-select" aria-label="Kỳ báo cáo">
            <option>12 tháng</option>
            <option>6 tháng</option>
            <option>30 ngày</option>
            </select>
            </div>
            </div>
            <div class="revenue-body">
            <div class="chart-area">
            <div class="chart-ylabels">
            <span>50 tỷ</span>
            <span>35 tỷ</span>
            <span>20 tỷ</span>
            <span>0</span>
            </div>
            <div class="chart-plot">${bars.map((v, i) => `<span class="chart-column secondary" style="--bar2:${Math.max(20, v - 13)}%">
            </span>
            <span class="chart-column" style="--bar:${v}%" title="${["T11", "T12", "T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10"][i]}">
            </span>`).join("")}</div>
            <div class="chart-xlabels">${["T11", "T12", "T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10"].map(x => `<span>${x}</span>`).join("")}</div>
            </div>
            </div>
            </section>
            <section class="panel task-panel">
            <div class="panel-head">
            <div>
            <h2 class="panel-title">Cần bạn chú ý</h2>
            <p class="panel-subtitle">Các công việc ưu tiên hôm nay</p>
            </div>
            <span class="status-pill warning">${d.tasks.length} việc</span>
            </div>
            <div class="task-list">${d.tasks.map(([title, detail, icon, color]) => {
                let taskLink = "";
                if (title.includes("tư vấn")) taskLink = `${contextPath}/staff/consultations`;
                else if (title.includes("Giao dịch") || title.includes("Hợp đồng")) taskLink = `${contextPath}/staff/transactions`;
                return `<div class="task-row" ${taskLink ? `onclick="window.location.href='${taskLink}'" style="cursor:pointer;"` : ""}>
            <span class="task-icon ${color}">${icon}</span>
            <div class="task-copy">
            <strong>${title}</strong>
            <span>${detail}</span>
            </div>
            <span class="task-count">→</span>
            </div>`;
            }).join("")}</div>
            </section>
            </div>
            <div class="dashboard-lower">${miniTable(d.rows)}<section class="panel">
            <div class="panel-head">
            <div>
            <h2 class="panel-title">${currentRole === "manager" ? "Tình hình tồn kho" : "Hoạt động gần đây"}</h2>
            <p class="panel-subtitle">${currentRole === "manager" ? "Xe theo trạng thái tại các địa điểm" : "Cập nhật trong hệ thống"}</p>
            </div>${currentRole === "manager" ? `<button class="text-button" data-view="inventory">Chi tiết →</button>` : ""}</div>${currentRole === "manager" ? `<div class="inventory-bars">${[["Có sẵn", 66, 77],
            ["Đã đặt cọc", 12, 14],
            ["Đã bán", 5, 6],
            ["Đã bàn giao", 3, 4]].map(([name, n, pct]) => `<div class="inventory-row">
            <span>${name}</span>
            <div class="inventory-track">
            <div class="inventory-fill" style="width:${pct}%">
            </div>
            </div>
            <strong>${n} xe</strong>
            </div>`).join("")}</div>` : `<div class="activity-list">
            <div class="activity-row">
            <span class="activity-marker">✧</span>
            <div class="activity-copy">
            <strong>Yêu cầu tư vấn mới được gửi</strong>
            <span>Khách hàng quan tâm VF 8 Plus · 12 phút trước</span>
            </div>
            </div>
            <div class="activity-row">
            <span class="activity-marker green">✓</span>
            <div class="activity-copy">
            <strong>Giao dịch GD-00842 được cập nhật</strong>
            <span>Đã xác nhận đặt cọc · 48 phút trước</span>
            </div>
            </div>
            <div class="activity-row">
            <span class="activity-marker amber">◷</span>
            <div class="activity-copy">
            <strong>Hồ sơ hợp đồng cần theo dõi</strong>
            <span>HD-00321 chờ khách hàng ký · 2 giờ trước</span>
            </div>
            </div>
            </div>`}</section>
            </div>`;
    }
    function renderTable(key) {
        const [title, intro, columns] = titles[key];
        const rows = demoRows[key] || [];
        const hasStatuses = !(["transaction-report", "vehicle-report"].includes(key));
        const addable = ["customers", "consultations", "transactions", "vehicle-movements"].includes(key) && currentRole !== "manager";
        let action = addable ? `<button class="button" data-toast="Biểu mẫu sẽ được kết nối khi backend được triển khai.">
            <span class="button-icon">＋</span>${key === "transactions" ? "Tạo giao dịch" : "Thêm mới"}</button>` : `<button class="button button-secondary" data-toast="Báo cáo minh họa sẵn sàng để xuất khi backend được kết nối.">↓ Xuất báo cáo</button>`;
        if (key === "consultations") {
            action = `<a href="${contextPath}/staff/consultations" class="button" style="text-decoration:none;">Mở Quản lý Tư vấn</a>`;
        } else if (key === "transactions") {
            action = `<a href="${contextPath}/staff/transactions" class="button" style="text-decoration:none;">Mở Quản lý Giao dịch</a>`;
        } else if (key === "revenue-report" || key === "transaction-report") {
            action = `<a href="${contextPath}/staff/report" class="button button-secondary" style="text-decoration:none;">Xem Báo cáo & Thống kê</a>`;
        }
        let rowAction = `<button class="text-button" data-toast="Chi tiết bản ghi sẽ hiển thị khi kết nối backend.">Chi tiết</button>`;
        if (key === "transactions") {
            rowAction = `<a href="${contextPath}/staff/transaction-detail?id=1" class="text-button" style="text-decoration:none; color: #0071e3; font-weight:600;">Chi tiết</a>`;
        } else if (key === "consultations") {
            rowAction = `<a href="${contextPath}/staff/consultations" class="text-button" style="text-decoration:none; color: #0071e3; font-weight:600;">Xử lý</a>`;
        } else if (key === "revenue-report" || key === "transaction-report") {
            rowAction = `<a href="${contextPath}/staff/report" class="text-button" style="text-decoration:none; color: #0071e3; font-weight:600;">Xem chi tiết</a>`;
        }
        const cells = rows.map(row => `<tr>${row.map((cell, i) => {
            if (hasStatuses && i === row.length - 2) return `<td>
            <span class="status-pill ${row.at(-1)}">${esc(cell)}</span>
            </td>`;
            if (hasStatuses && i === row.length - 1) return "";
            if (key === "transaction-report" && i === row.length - 1) return `<td>
            <span class="status-pill ${row[i]}">Theo dõi</span>
            </td>`;
            if (key === "vehicle-report" && i === row.length - 2) return `<td>
            <span class="status-pill ${row.at(-1)}">${esc(cell)}</span>
            </td>`;
            if (key === "vehicle-report" && i === row.length - 1) return "";
            return `<td>${i === 0 || i === 1 ? `<span class="cell-primary">${esc(cell)}</span>` : esc(cell)}</td>`;
        }).join("")}<td>
            ${rowAction}
            </td>
            </tr>`).join("");
        return `${heading(title, currentRole === "manager" ? "BÁO CÁO & GIÁM SÁT" : "NGHIỆP VỤ NỘI BỘ", intro, action)}${note()}<section class="panel table-panel">
            <div class="panel-head">
            <div>
            <h2 class="panel-title">${title}</h2>
            <p class="panel-subtitle">${rows.length} bản ghi minh họa</p>
            </div>
            </div>
            <div class="toolbar">
            <input type="search" data-table-search placeholder="Tìm theo mã, khách hàng hoặc mẫu xe" aria-label="Tìm kiếm bảng">
            <select data-status-filter aria-label="Lọc trạng thái">
            <option value="">Tất cả trạng thái</option>
            <option>Đang xử lý</option>
            <option>Chờ xác nhận</option>
            <option>Đã xác nhận</option>
            <option>Có sẵn</option>
            </select>
            <button class="button button-secondary" data-apply-filter>Lọc kết quả</button>
            </div>
            <div class="table-wrap">
            <table>
            <thead>
            <tr>${columns.map(x => `<th>${x}</th>`).join("")}<th>THAO TÁC</th>
            </tr>
            </thead>
            <tbody data-table-body>${cells}</tbody>
            </table>
            </div>
            <div class="table-empty" data-empty hidden>
            <div class="empty-icon">⌕</div>Không tìm thấy kết quả phù hợp.</div>
            <div class="table-foot">
            <span>Hiển thị <strong data-row-count>${rows.length}</strong> trên ${rows.length} bản ghi minh họa</span>
            <span>Trang 1 / 1　‹　<span class="status-pill info">1</span>　›</span>
            </div>
            </section>${currentRole === "manager" ? `<div class="inline-note">Manager có quyền xem và giám sát dữ liệu theo phạm vi được cấp. Thao tác cập nhật do nhóm nghiệp vụ phụ trách thực hiện.</div>` : ""}`;
    }
    function renderInventoryOverview() {
        return `${heading("Tổng quan tồn kho", "QUẢN LÝ TỒN KHO", "Theo dõi xe theo mẫu, trạng thái và địa điểm lưu kho.", `<button class="button button-secondary" data-view="inventory-report">Xem báo cáo tồn kho →</button>`)}${note()}${metricCards([["Tổng xe trong hệ thống", "86 xe", "Tại 3 địa điểm", "▦", ""],
        ["Có sẵn", "66 xe", "Sẵn sàng giao dịch", "✓", "green"],
        ["Đã đặt cọc", "12 xe", "Chờ hoàn tất giao dịch", "◉", "amber"],
        ["Cảnh báo tồn thấp", "3 mẫu", "Cần theo dõi bổ sung", "!", ""]])}<div class="dashboard-lower">${miniTable("inventory")}<section class="panel">
            <div class="panel-head">
            <div>
            <h2 class="panel-title">Phân bố xe theo địa điểm</h2>
            <p class="panel-subtitle">Tổng số xe trong kho và showroom</p>
            </div>
            </div>
            <div class="inventory-bars">${[["Kho Bình Dương", 42, 92],
            ["Showroom TP.HCM", 26, 64],
            ["Showroom Hà Nội", 18, 43]].map(([name, n, pct]) => `<div class="inventory-row">
            <span>${name}</span>
            <div class="inventory-track">
            <div class="inventory-fill" style="width:${pct}%">
            </div>
            </div>
            <strong>${n} xe</strong>
            </div>`).join("")}</div>
            </section>
            </div>
            <div class="inline-note">Màn hình Manager tập trung giám sát. Thêm, sửa, xóa danh mục xe thuộc phạm vi Admin hoặc quyền cụ thể được cấp.</div>`;
    }
    function render() {
        renderNav();
        $("#breadcrumb-title").textContent = labels[currentView] || roleDashboard[currentRole].title;
        $("#profile-name").textContent = ({ sales: "An Nguyễn", operations: "Minh Trần", accountant: "Linh Hoàng", manager: "Tuấn Hoàng" })[currentRole];
        $("#profile-role").textContent = names[currentRole];
        $("#profile-avatar").textContent = initials[currentRole];
        content.innerHTML = currentView === "dashboard" ? renderDashboard() : currentRole === "manager" && currentView === "inventory" ? renderInventoryOverview() : renderTable(currentView);
        content.scrollTop = 0;
    }
    function toast(message) {
        const box = $("#staff-toast");
        box.textContent = message;
        box.classList.add("visible");
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => box.classList.remove("visible"), 2800);
    }
    nav.addEventListener("click", e => {
        const link = e.target.closest("[data-view]");
        if (!link) return;
        e.preventDefault();
        currentView = link.dataset.view;
        render();
        $("#staff-sidebar").classList.remove("open");
        $("[data-menu-toggle]").setAttribute("aria-expanded", "false");
    });
    content.addEventListener("click", e => {
        const target = e.target.closest("[data-view]");
        if (target) {
            currentView = target.dataset.view;
            render();
            return;
        } const toastTarget = e.target.closest("[data-toast]");
        if (toastTarget) toast(toastTarget.dataset.toast);
    });
    document.addEventListener("click", e => {
        const target = e.target.closest("[data-toast]");
        if (target && !content.contains(target)) toast(target.dataset.toast);
    });
    picker.addEventListener("change", () => {
        currentRole = picker.value;
        currentView = "dashboard";
        render();
    });
    $("[data-menu-toggle]").addEventListener("click", e => {
        const open = $("#staff-sidebar").classList.toggle("open");
        e.currentTarget.setAttribute("aria-expanded", String(open));
    });
    function applyFilters() {
        const query = ($("[data-table-search]")?.value || "").trim().toLocaleLowerCase("vi");
        const status = $("[data-status-filter]")?.value || "";
        let visible = 0;
        $$rows().forEach(row => {
            const show = row.textContent.toLocaleLowerCase("vi").includes(query) && (!status || row.textContent.includes(status));
            row.hidden = !show;
            if (show) visible++;
        });
        const empty = $("[data-empty]");
        if (empty) empty.hidden = visible > 0;
        const count = $("[data-row-count]");
        if (count) count.textContent = String(visible);
    }
    content.addEventListener("input", e => {
        if (e.target.matches("[data-table-search]")) applyFilters();
    });
    content.addEventListener("click", e => {
        if (e.target.closest("[data-apply-filter]")) {
            applyFilters();
            toast("Đã áp dụng bộ lọc.");
        }
    });
    content.addEventListener("change", e => {
        if (e.target.matches("[data-status-filter]")) applyFilters();
    });
    content.addEventListener("change", e => {
        if (e.target.matches(".chart-select")) toast(`Đã chọn kỳ báo cáo: ${e.target.value}.`);
    });
    function $$rows() {
        return Array.from(content.querySelectorAll("[data-table-body] tr"));
    }
    $("#today-label").textContent = new Intl.DateTimeFormat("vi-VN", { weekday: "short", day: "2-digit", month: "2-digit", year: "numeric", timeZone: "Asia/Ho_Chi_Minh" }).format(new Date());
    render();
})();
