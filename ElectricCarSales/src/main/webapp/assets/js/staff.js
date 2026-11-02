"use strict";
(() => {
    const app = document.querySelector("[data-staff-app]");
    if (!app) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const content = $("#staff-content");
    const nav = $("#staff-nav");
    const picker = $("#role-picker");
    const names = { sales: "Nhân viên kinh doanh", operations: "Nhân viên vận hành", accountant: "Kế toán", manager: "Quản lý cửa hàng" };
    const initials = { sales: "AN", operations: "MT", accountant: "LH", manager: "TH" };

    const appContext = window.location.pathname.substring(0, window.location.pathname.indexOf('/', 2));

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
                // Menu Tồn kho trỏ thẳng về Servlet
                ["inventory_real", "▦", "Quản lý tồn kho", "", appContext + "/staff/inventory"],
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

    // MẶC ĐỊNH LÀ VÀO TRANG TỔNG QUAN
    let currentRole = "operations";
    let currentView = "dashboard";

    // NẾU URL ĐANG LÀ TRANG TỒN KHO THẬT THÌ ĐỔI MENU SANG TỒN KHO
    if (window.location.pathname.includes('/staff/inventory')) {
        currentView = "inventory_real";
    }

    const labels = {
        "dashboard": "Tổng quan",
        "inventory_real": "Quản lý tồn kho"
    };

    function renderNav() {
        nav.innerHTML = menus[currentRole].map(group => `<div class="nav-group-label">${group.group}</div>${group.items.map(item => {
            const id = item[0], icon = item[1], label = item[2], badge = item[3], customLink = item[4];
            const href = customLink ? customLink : `#${id}`;
            return `<a href="${href}" class="staff-nav-link ${currentView === id ? "active" : ""}" data-view="${id}">
            <span class="staff-nav-icon">${icon}</span>
            <span>${label}</span>${badge ? `<span class="nav-badge">${badge}</span>` : ""}</a>`;
        }).join("")}`).join("");
    }

    function renderDashboard() {
        // Giao diện Tổng quan mẫu (khi ở dashboard)
        return `
        <div class="page-heading">
            <div>
                <div class="eyebrow"><span class="eyebrow-line"></span>TRUNG TÂM VẬN HÀNH</div>
                <h1>Tổng quan vận hành</h1>
                <p>Theo dõi trạng thái xe và các hoạt động kho vận trong ngày.</p>
            </div>
        </div>
        <section class="metric-grid" aria-label="Chỉ số tổng quan">
            <article class="metric-card"><div class="metric-head"><span>Tổng xe trong kho</span><span class="metric-icon">▦</span></div><strong>86</strong><div class="metric-foot"><span class="trend-up">3 địa điểm đang hoạt động</span></div></article>
            <article class="metric-card"><div class="metric-head"><span>Xe có sẵn</span><span class="metric-icon green">✓</span></div><strong>66</strong><div class="metric-foot"><span class="trend-up">Sẵn sàng giao dịch</span></div></article>
            <article class="metric-card"><div class="metric-head"><span>Đang vận chuyển</span><span class="metric-icon blue">⇄</span></div><strong>7</strong><div class="metric-foot"><span class="trend-up">2 lô xe cần cập nhật</span></div></article>
        </section>
        <div class="inline-note mt-4">Vui lòng chọn <b>Quản lý tồn kho</b> ở menu bên trái để xem dữ liệu thực tế từ Database.</div>
        `;
    }

    function render() {
        renderNav();
        const bTitle = document.getElementById("breadcrumb-title");
        if (bTitle) bTitle.textContent = labels[currentView] || "Tổng quan";

        // BẢO VỆ DỮ LIỆU JSP: Nếu đang ở trang Servlet Tồn kho thì KHÔNG render đè
        if (window.location.pathname.includes('/staff/inventory')) {
            return;
        }

        // Nếu ở Dashboard, hiển thị giao diện tổng quan ảo
        if (currentView === "dashboard") {
            content.innerHTML = renderDashboard();
        }
    }

    nav.addEventListener("click", e => {
        const link = e.target.closest("a");
        if (!link) return;
        const href = link.getAttribute("href");
        if (href && !href.startsWith("#")) {
            return; // Cho phép trình duyệt chuyển trang nếu là link Servlet thật
        }
        e.preventDefault();
        const view = link.dataset.view;
        if(view) currentView = view;
        render();
    });

    picker.addEventListener("change", () => {
        currentRole = picker.value;
        currentView = "dashboard";
        render();
    });

    render();
})();
