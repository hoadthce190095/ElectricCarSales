"use strict";
(() => {
    const app = document.querySelector("[data-staff-app]");
    if (!app) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const content = $("#staff-content");
    const nav = $("#staff-nav");
    const names = { sales: "Nhân viên kinh doanh", operations: "Nhân viên vận hành", accountant: "Kế toán", manager: "Quản lý cửa hàng", admin: "Quản trị viên" };

    const appContext = document.body.dataset.contextPath || "";

    const menus = {
        admin: [{ group: "QUẢN TRỊ", items: [["inventory_real", "▦", "Quản lý tồn kho", "", appContext + "/staff/inventory"]] }],
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
                ["inventory_real", "▦", "Quản lý tồn kho", "", appContext + "/staff/inventory"],
                ["transactions", "▤", "Bán hàng & giao dịch"]]
        }, {
            group: "BÁO CÁO", items: [["revenue-report", "▥", "Báo cáo doanh thu"],
                ["vehicle-report", "▧", "Xe bán ra"],
                ["transaction-report", "◷", "Báo cáo giao dịch"],
                ["inventory-report", "▦", "Báo cáo tồn kho"]]
        }]
    };

    // Vai trò chỉ được lấy từ session do JSP đưa xuống, không còn role-picker demo.
    const currentRole = (app.dataset.staffRole || "").trim().toLowerCase();
    if (!menus[currentRole]) {
        window.location.replace(appContext + "/login");
        return;
    }

    const inventoryPage = window.location.pathname.includes("/staff/inventory");
    if (inventoryPage && !["operations", "manager", "admin"].includes(currentRole)) {
        // Server vẫn phải chặn 403; kiểm tra này chỉ giúp UI không hiển thị sai.
        window.location.replace(appContext + "/staff/dashboard");
        return;
    }

    const roleDashboards = {
        sales: ["Tổng quan kinh doanh", "Khách hàng, tư vấn và giao dịch bán xe của nhân viên kinh doanh."],
        operations: ["Tổng quan vận hành", "Theo dõi xe, tồn kho và tiến độ bàn giao."],
        accountant: ["Tổng quan kế toán", "Theo dõi thanh toán, công nợ, trả góp và hóa đơn."],
        manager: ["Tổng quan quản lý", "Theo dõi bán hàng, giao dịch, tồn kho và các báo cáo."],
        admin: ["Không gian quản trị", "Quản lý tồn kho theo quyền Admin đã được RDS cho phép."]
    };

    const navIds = new Set(menus[currentRole].flatMap(group => group.items.map(item => item[0])));
    const hashView = decodeURIComponent(window.location.hash.slice(1));
    let currentView = inventoryPage ? "inventory_real" : (navIds.has(hashView) ? hashView : "dashboard");

    const profileRole = $("#profile-role");
    if (profileRole) profileRole.textContent = names[currentRole];
    const profileName = $("#profile-name");
    const profileAvatar = $("#profile-avatar");
    if (profileAvatar) {
        const initials = (profileName?.textContent || names[currentRole]).trim()
            .split(/\s+/).slice(-2).map(word => word[0] || "").join("").toUpperCase();
        profileAvatar.textContent = initials;
    }

    const allItems = menus[currentRole].flatMap(group => group.items);
    function renderNav() {
        if (!nav) return;
        nav.innerHTML = menus[currentRole].map(group =>
            `<div class="nav-group-label">${group.group}</div>` + group.items.map(item => {
                const [id, icon, label, badge, customLink] = item;
                const href = customLink || `${appContext}/staff/dashboard#${encodeURIComponent(id)}`;
                return `<a href="${href}" class="staff-nav-link ${currentView === id ? "active" : ""}" data-view="${id}">
                    <span class="staff-nav-icon">${icon}</span>
                    <span>${label}</span>${badge ? `<span class="nav-badge">${badge}</span>` : ""}</a>`;
            }).join("")
        ).join("");
    }

    function renderDashboard() {
        const [title, description] = roleDashboards[currentRole];
        const links = allItems.filter(item => item[0] !== "dashboard");
        return `<div class="page-heading"><div>
            <div class="eyebrow"><span class="eyebrow-line"></span>${names[currentRole].toUpperCase()}</div>
            <h1>${title}</h1><p>${description}</p></div></div>
            <section class="metric-grid" aria-label="Các mục nghiệp vụ">
            ${links.map(item => `<a class="metric-card" href="${item[4] || `${appContext}/staff/dashboard#${encodeURIComponent(item[0])}`}">
                <div class="metric-head"><span>${item[2]}</span><span class="metric-icon">${item[1]}</span></div>
                <strong style="font-size:16px;">${item[2]}</strong>
                <div class="metric-foot">Mở chức năng →</div></a>`).join("")}
            </section>`;
    }

    function renderSection(view) {
        const item = allItems.find(row => row[0] === view);
        const title = item ? item[2] : "Tổng quan";
        // Chưa có Servlet/JSP cho các mục dùng hash. Không hiển thị số liệu bịa.
        return `<div class="page-heading"><div>
            <div class="eyebrow"><span class="eyebrow-line"></span>${names[currentRole].toUpperCase()}</div>
            <h1>${title}</h1>
            <p>Mục này đã có trong menu của vai trò ${names[currentRole]}, nhưng phiên bản source hiện tại chưa có trang dữ liệu riêng cho mục này.</p>
            </div></div>
            <div class="inline-note">Nhóm cần kết nối đúng Controller / JSP của nghiệp vụ này theo RDS. Không dùng dữ liệu mẫu làm dữ liệu thật.</div>`;
    }

    function render() {
        renderNav();
        const currentLabel = currentView === "dashboard" ? roleDashboards[currentRole][0] :
            (allItems.find(item => item[0] === currentView)?.[2] || "Tổng quan");
        const breadcrumb = $("#breadcrumb-title");
        if (breadcrumb) breadcrumb.textContent = currentLabel;
        // Trang tồn kho do Servlet truyền dữ liệu sang JSP: không ghi đè nội dung.
        if (inventoryPage || !content) return;
        content.innerHTML = currentView === "dashboard" ? renderDashboard() : renderSection(currentView);
    }

    nav?.addEventListener("click", event => {
        const link = event.target.closest("a");
        if (!link) return;
        if (inventoryPage || link.getAttribute("href")?.includes("/staff/inventory")) return;
        const view = link.dataset.view;
        if (!view || !navIds.has(view)) return;
        event.preventDefault();
        currentView = view;
        window.history.replaceState(null, "", `${appContext}/staff/dashboard#${encodeURIComponent(view)}`);
        render();
    });

    render();
})();
