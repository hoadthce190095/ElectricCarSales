"use strict";
(() => {
    const app = document.querySelector("[data-staff-app]");
    if (!app) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const content = $("#staff-content");
    const nav = $("#staff-nav");
    const toastEl = $("#staff-toast");
    const names = {
        sales: "Nhân viên kinh doanh (Sales Staff)",
        operations: "Nhân viên vận hành (Operations Officer)",
        accountant: "Kế toán (Accountant)",
        manager: "Quản lý cửa hàng (Manager)",
        admin: "Quản trị viên (Admin)"
    };

    const appContext = document.body.dataset.contextPath || "";
    let toastTimer;

    function notify(msg) {
        if (!toastEl) {
            alert(msg);
            return;
        }
        toastEl.textContent = msg;
        toastEl.classList.add("visible");
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => toastEl.classList.remove("visible"), 4200);
    }

    const fmtMoney = (v) => new Intl.NumberFormat("vi-VN").format(Math.round(Number(v) || 0)) + " ₫";
    const esc = (s) => String(s ?? "—")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");

    const menus = {
        admin: [{ group: "QUẢN TRỊ", items: [["inventory_real", "▦", "Quản lý tồn kho", "", appContext + "/staff/inventory"]] }],
        sales: [{
            group: "CÔNG VIỆC", items: [
                ["dashboard", "⌂", "Tổng quan"],
                ["consultations", "✧", "Yêu cầu tư vấn"],
                ["customers", "♙", "Khách hàng"]
            ]
        }, {
            group: "BÁN HÀNG", items: [
                ["transactions", "▤", "Giao dịch & Lập đơn"],
                ["deposits", "◉", "Theo dõi Đặt cọc 30%"],
                ["contracts", "▧", "Hợp đồng mua bán"],
                ["handover", "↗", "Phối hợp Bàn giao xe"]
            ]
        }],
        operations: [{
            group: "VẬN HÀNH", items: [
                ["dashboard", "⌂", "Tổng quan"],
                ["inventory_real", "▦", "Quản lý tồn kho", "", appContext + "/staff/inventory"],
                ["vehicle-movements", "⇄", "Nhập / Xuất kho xe"],
                ["vehicle-status", "◷", "Trạng thái xe vật lý"],
                ["handover", "↗", "Xác nhận Bàn giao xe"]
            ]
        }],
        accountant: [{
            group: "TÀI CHÍNH & ĐỐI SOÁT", items: [
                ["dashboard", "⌂", "Tổng quan"],
                ["payments", "◉", "Xác nhận Thanh toán"],
                ["debts", "◷", "Quản lý Công nợ & Giải ngân 70%"],
                ["installments", "▤", "Đối tác Trả góp"],
                ["contracts", "▧", "Hợp đồng & Xuất Hóa đơn"]
            ]
        }],
        manager: [{
            group: "QUẢN LÝ", items: [
                ["dashboard", "⌂", "Tổng quan"],
                ["inventory_real", "▦", "Quản lý tồn kho", "", appContext + "/staff/inventory"],
                ["transactions", "▤", "Bán hàng & Giao dịch"]
            ]
        }, {
            group: "BÁO CÁO KINH DOANH", items: [
                ["revenue-report", "▥", "Báo cáo doanh thu"],
                ["vehicle-report", "▧", "Báo cáo xe bán ra"],
                ["transaction-report", "◷", "Báo cáo giao dịch"],
                ["inventory-report", "▦", "Báo cáo tồn kho"]
            ]
        }]
    };

    const currentRole = (app.dataset.staffRole || "").trim().toLowerCase();
    if (!menus[currentRole]) {
        window.location.replace(appContext + "/login");
        return;
    }

    const inventoryPage = window.location.pathname.includes("/staff/inventory");
    if (inventoryPage && !["operations", "manager", "admin"].includes(currentRole)) {
        window.location.replace(appContext + "/staff/dashboard");
        return;
    }

    const roleDashboards = {
        sales: ["Tổng quan kinh doanh", "Tiếp nhận tư vấn, quản lý khách hàng, lập đơn hàng (cọc 30% / trả 70%), tạo hợp đồng và phối hợp bàn giao xe theo RDS."],
        operations: ["Tổng quan vận hành", "Quản lý tồn kho, nhập/xuất xe vật lý, cập nhật trạng thái xe và xác nhận bàn giao xe sau khi có hóa đơn."],
        accountant: ["Tổng quan kế toán", "Đối soát sao kê ngân hàng, xác nhận đặt cọc 30%, xác nhận thanh toán/giải ngân 70%, theo dõi công nợ và phát hành hóa đơn."],
        manager: ["Tổng quan quản lý", "Giám sát toàn bộ giao dịch bán xe, tồn kho và xuất báo cáo doanh thu / xe bán ra theo thời gian thực."],
        admin: ["Không gian quản trị", "Quản lý tồn kho theo quyền Admin đã được RDS cho phép."]
    };

    const navIds = new Set(menus[currentRole].flatMap(group => group.items.map(item => item[0])));
    const hashView = decodeURIComponent(window.location.hash.slice(1));
    let currentView = inventoryPage ? "inventory_real" : (navIds.has(hashView) ? hashView : "dashboard");
    let prefillOrderCustomer = null;

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

    async function fetchJson(url, options = {}) {
        const res = await fetch(appContext + url, {
            credentials: "same-origin",
            headers: { "Accept": "application/json", ...(options.headers || {}) },
            ...options
        });
        const contentType = res.headers.get("content-type") || "";
        const body = await res.text();
        let data;
        if (contentType.toLowerCase().includes("application/json")) {
            try { data = body ? JSON.parse(body) : {}; }
            catch (_) { throw new Error("Máy chủ trả dữ liệu JSON không hợp lệ. Vui lòng tải lại trang."); }
        } else {
            const redirectedToLogin = res.redirected && /\/login(?:[?#]|$)/i.test(res.url);
            if (res.status === 401 || redirectedToLogin) {
                throw new Error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            }
            const detail = body && !/^\s*</.test(body) ? body.slice(0, 240) : "";
            throw new Error(detail || `Máy chủ không trả JSON (HTTP ${res.status}). Vui lòng kiểm tra quyền truy cập hoặc tải lại trang.`);
        }
        if (!res.ok) {
            throw new Error(data.error || data.message || `Lỗi máy chủ (${res.status})`);
        }
        return data;
    }

    async function postForm(url, formDataObj) {
        const params = new URLSearchParams();
        params.set("format", "json");
        for (const [k, v] of Object.entries(formDataObj)) {
            if (v !== undefined && v !== null) params.set(k, String(v));
        }
        return fetchJson(url, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            body: params.toString()
        });
    }

    function renderDashboard() {
        const [title, description] = roleDashboards[currentRole];
        const links = allItems.filter(item => item[0] !== "dashboard");
        return `<div class="page-heading"><div>
            <div class="eyebrow"><span class="eyebrow-line"></span>${names[currentRole].toUpperCase()}</div>
            <h1>${title}</h1><p>${description}</p></div></div>
            <section class="metric-grid" aria-label="Các mục nghiệp vụ">
            ${links.map(item => `<a class="metric-card" href="${item[4] || `${appContext}/staff/dashboard#${encodeURIComponent(item[0])}`}" data-view="${item[0]}">
                <div class="metric-head"><span>${item[2]}</span><span class="metric-icon">${item[1]}</span></div>
                <strong style="font-size:16px;">${item[2]}</strong>
                <div class="metric-foot">Mở chức năng →</div></a>`).join("")}
            </section>
            <div class="inline-note" style="margin-top:20px;">
                <strong>Quy trình chuẩn RDS:</strong> Tư vấn (ConsultationRequests) → Sales lập đơn (Orders + OrderDetails, giữ chỗ xe RESERVED) → Khách xem đơn &amp; thanh toán chuyển khoản VietQR (SePay - MB Bank) đặt cọc 30% → Kế toán xác nhận cọc (DEPOSITED) → Sales lập Hợp đồng &amp; Khách ký OTP (SIGNED) → Thanh toán 70% còn lại (hoặc Ngân hàng đối tác giải ngân 70%) → Kế toán xác nhận đủ 100% (PAID) &amp; Xuất Hóa đơn (ISSUED) → Vận hành/Sales xác nhận Xuất kho Bàn giao xe (DELIVERED / COMPLETED).
            </div>`;
    }

    async function loadConsultationsView() {
        content.innerHTML = `<p>Đang tải danh sách yêu cầu tư vấn từ bảng ConsultationRequests...</p>`;
        const data = await fetchJson("/consultation?format=json");
        const list = data.consultations || [];
        content.innerHTML = `
            <div class="page-heading"><div>
                <div class="eyebrow"><span class="eyebrow-line"></span>UC-06 · QUẢN LÝ TƯ VẤN</div>
                <h1>Yêu cầu tư vấn khách hàng (${list.length})</h1>
                <a href="${appContext}/sales-chat">Mở trang trò chuyện</a>
                <p>Tiếp nhận yêu cầu tư vấn, ghi phản hồi tư vấn giá/phương án trả thẳng - trả góp và chuyển sang lập đơn hàng.</p>
            </div></div>
            <div class="table-responsive bg-white rounded shadow-sm border" style="padding:16px;background:#fff;border-radius:12px;border:1px solid #e2e8f0;">
                <table class="table" style="width:100%;border-collapse:collapse;font-size:13px;">
                    <thead>
                        <tr style="border-bottom:2px solid #e2e8f0;text-align:left;">
                            <th style="padding:10px;">MÃ YC</th>
                            <th style="padding:10px;">KHÁCH HÀNG</th>
                            <th style="padding:10px;">MẪU XE QUAN TÂM</th>
                            <th style="padding:10px;">TRẠNG THÁI</th>
                            <th style="padding:10px;">NỘI DUNG &amp; PHẢN HỒI</th>
                            <th style="padding:10px;">XỬ LÝ &amp; LẬP ĐƠN</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length ? list.map(c => `
                            <tr style="border-bottom:1px solid #f1f5f9;vertical-align:top;">
                                <td style="padding:10px;"><strong>#${c.id}</strong><br><small>${esc(c.createdAt)}</small></td>
                                <td style="padding:10px;">
                                    <strong>${esc(c.customerName)}</strong> (KH #${c.customerId})<br>
                                    <small>SĐT: ${esc(c.customerPhone)} · ${esc(c.customerEmail)}</small>
                                </td>
                                <td style="padding:10px;"><strong>${esc(c.modelName || "Tư vấn chung")}</strong></td>
                                <td style="padding:10px;">
                                    <span class="status-pill">${esc(c.status)}</span><br>
                                    <small>NV: ${esc(c.staffName || "Chưa phân công")}</small>
                                </td>
                                <td style="padding:10px;max-width:340px;white-space:pre-line;">${esc(c.content)}</td>
                                <td style="padding:10px;min-width:260px;">
                                    <form class="staff-consult-form" data-req-id="${c.id}" style="display:flex;flex-direction:column;gap:6px;">
                                        <select name="status" style="padding:6px;border:1px solid #cbd5e1;border-radius:6px;">
                                            <option value="PENDING" ${c.status === "PENDING" ? "selected" : ""}>PENDING</option>
                                            <option value="IN_PROGRESS" ${c.status === "IN_PROGRESS" ? "selected" : ""}>IN_PROGRESS (Đang tư vấn)</option>
                                            <option value="COMPLETED" ${c.status === "COMPLETED" ? "selected" : ""}>COMPLETED (Đã tư vấn xong)</option>
                                            <option value="CANCELLED" ${c.status === "CANCELLED" ? "selected" : ""}>CANCELLED</option>
                                        </select>
                                        <input type="text" name="note" maxlength="300" placeholder="Nhập phản hồi tư vấn giá / trả góp..." style="padding:6px 8px;border:1px solid #cbd5e1;border-radius:6px;">
                                        <div style="display:flex;gap:6px;">
                                            <button type="submit" class="button button-primary" style="padding:6px 10px;font-size:12px;">Lưu phản hồi</button>
                                            <button type="button" class="button button-secondary btn-jump-create-order" data-customer-id="${c.customerId}" data-model-id="${c.modelId || ""}" style="padding:6px 10px;font-size:12px;">+ Lập đơn hàng →</button>
                                        </div>
                                    </form>
                                </td>
                            </tr>
                        `).join("") : `<tr><td colspan="6" style="padding:24px;text-align:center;">Chưa có yêu cầu tư vấn nào.</td></tr>`}
                    </tbody>
                </table>
            </div>
        `;
        content.querySelectorAll(".staff-consult-form").forEach(form => {
            form.addEventListener("submit", async (e) => {
                e.preventDefault();
                try {
                    const res = await postForm("/consultation", {
                        action: "update",
                        requestId: form.dataset.reqId,
                        status: form.status.value,
                        note: form.note.value
                    });
                    notify(res.message || "Đã cập nhật yêu cầu tư vấn.");
                    await loadConsultationsView();
                } catch (err) {
                    notify(err.message);
                }
            });
        });
        content.querySelectorAll(".btn-jump-create-order").forEach(btn => {
            btn.addEventListener("click", () => {
                prefillOrderCustomer = {
                    customerId: Number(btn.dataset.customerId),
                    modelId: Number(btn.dataset.modelId) || null
                };
                currentView = "transactions";
                window.history.replaceState(null, "", `${appContext}/staff/dashboard#transactions`);
                render();
            });
        });
    }

    async function loadCustomersView() {
        content.innerHTML = `<p>Đang tải danh sách khách hàng (Bảng Customers)...</p>`;
        const data = await fetchJson("/customer/profile?mode=staff");
        const customers = data.customers || [];
        content.innerHTML = `
            <div class="page-heading"><div>
                <div class="eyebrow"><span class="eyebrow-line"></span>UC-15 · QUẢN LÝ KHÁCH HÀNG</div>
                <h1>Danh sách khách hàng (${customers.length})</h1>
                <p>Xem và cập nhật thông tin liên hệ của khách hàng phục vụ lập đơn và bàn giao xe.</p>
            </div></div>
            <div style="padding:16px;background:#fff;border-radius:12px;border:1px solid #e2e8f0;overflow-x:auto;">
                <table style="width:100%;border-collapse:collapse;font-size:13px;">
                    <thead>
                        <tr style="border-bottom:2px solid #e2e8f0;text-align:left;">
                            <th style="padding:10px;">MÃ KH</th>
                            <th style="padding:10px;">HỌ VÀ TÊN</th>
                            <th style="padding:10px;">SỐ ĐIỆN THOẠI</th>
                            <th style="padding:10px;">EMAIL</th>
                            <th style="padding:10px;">ĐỊA CHỈ</th>
                            <th style="padding:10px;">THAO TÁC</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${customers.map(c => `
                            <tr style="border-bottom:1px solid #f1f5f9;">
                                <td style="padding:10px;"><strong>#${c.customerID}</strong></td>
                                <td style="padding:10px;"><input id="cname-${c.customerID}" value="${esc(c.customerName)}" style="padding:6px;border:1px solid #cbd5e1;border-radius:6px;width:100%;"></td>
                                <td style="padding:10px;"><input id="cphone-${c.customerID}" value="${esc(c.phone)}" style="padding:6px;border:1px solid #cbd5e1;border-radius:6px;width:120px;"></td>
                                <td style="padding:10px;"><input id="cemail-${c.customerID}" value="${esc(c.email || "")}" style="padding:6px;border:1px solid #cbd5e1;border-radius:6px;width:180px;"></td>
                                <td style="padding:10px;"><input id="caddr-${c.customerID}" value="${esc(c.address || "")}" style="padding:6px;border:1px solid #cbd5e1;border-radius:6px;width:180px;"></td>
                                <td style="padding:10px;">
                                    <button type="button" class="button button-primary btn-save-cust" data-id="${c.customerID}" style="padding:6px 10px;font-size:12px;">Lưu</button>
                                </td>
                            </tr>
                        `).join("")}
                    </tbody>
                </table>
            </div>
        `;
        content.querySelectorAll(".btn-save-cust").forEach(btn => {
            btn.addEventListener("click", async () => {
                const id = btn.dataset.id;
                try {
                    const res = await postForm("/customer/profile?mode=staff", {
                        customerId: id,
                        customerName: document.getElementById(`cname-${id}`).value,
                        phone: document.getElementById(`cphone-${id}`).value,
                        email: document.getElementById(`cemail-${id}`).value,
                        address: document.getElementById(`caddr-${id}`).value
                    });
                    notify(res.message || "Đã cập nhật khách hàng.");
                } catch (err) {
                    notify(err.message);
                }
            });
        });
    }

    async function loadTransactionsAndDepositsView(onlyDeposits = false) {
        content.innerHTML = `<p>Đang tải dữ liệu đơn hàng, xe có sẵn và phương thức thanh toán...</p>`;
        const data = await fetchJson("/orders?format=json");
        const orders = data.orders || [];
        const customers = data.customers || [];
        const cars = data.cars || [];
        const inventory = data.inventorySummary?.[0] || {};
        const methods = data.paymentMethods || [];
        const companies = data.financialCompanies || [];
        const canCreateOrder = ["sales", "admin"].includes(currentRole);

        content.innerHTML = `
            <div class="page-heading"><div>
                <div class="eyebrow"><span class="eyebrow-line"></span>UC-08 &amp; UC-10 · GIAO DỊCH BÁN XE &amp; ĐẶT CỌC 30%</div>
                <h1>${onlyDeposits ? "Theo dõi Đặt cọc 30% (Deposits)" : "Quản lý Đơn hàng & Lập giao dịch bán xe"}</h1>
                <p>Hệ thống tự động tính 30% tiền đặt cọc và 70% số tiền còn lại từ giá xe vật lý trong kho (Cars.Price).</p>
            </div></div>

            ${canCreateOrder && !onlyDeposits ? `
            <section style="padding:18px;background:#fff;border-radius:12px;border:1px solid #cbd5e1;margin-bottom:20px;">
                <h3 style="margin:0 0 12px;font-size:16px;">+ Lập đơn hàng bán xe mới (Sales Staff - UC-08)</h3>
                <form id="staff-create-order-form" style="display:flex;flex-direction:column;gap:14px;">
                    <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(240px, 1fr));gap:12px;align-items:end;">
                        <label style="font-size:12px;font-weight:600;">Khách hàng *
                            <select name="customerId" id="so-customer" required style="width:100%;padding:8px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                                <option value="">-- Chọn khách hàng --</option>
                                ${customers.map(c => `<option value="${c.customerID}" ${prefillOrderCustomer?.customerId === c.customerID ? "selected" : ""}>#${c.customerID} · ${esc(c.customerName)} (${esc(c.phone)})</option>`).join("")}
                            </select>
                        </label>
                        <label style="font-size:12px;font-weight:600;">Xe vật lý có sẵn (AVAILABLE) *
                            <select name="carId" id="so-car" required style="width:100%;padding:8px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                                <option value="">${cars.length ? "-- Chọn xe có sẵn trong kho --" : "-- Kho chưa có xe vật lý khả dụng --"}</option>
                                ${cars.map(car => `<option value="${car.carID}" data-price="${car.price}" ${prefillOrderCustomer?.modelId === car.modelID ? "selected" : ""}>${esc(car.modelName)} (${esc(car.versionName)} - ${esc(car.colorName)}) · VIN: ${esc(car.vin)} · ${fmtMoney(car.price)}</option>`).join("")}
                            </select>
                        </label>
                        <label style="font-size:12px;font-weight:600;">Phương thức thanh toán *
                            <select name="paymentMethodId" id="so-method" required style="width:100%;padding:8px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                                ${methods.map(m => `<option value="${m.paymentMethodID}" data-name="${m.methodName}">${esc(m.methodName)} — ${esc(m.description)}</option>`).join("")}
                            </select>
                        </label>
                        <label id="so-company-wrap" style="font-size:12px;font-weight:600;display:none;">Ngân hàng đối tác (Bắt buộc khi Trả góp) *
                            <select name="companyId" id="so-company" style="width:100%;padding:8px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                                <option value="">-- Chọn đối tác tài chính --</option>
                                ${companies.map(fc => `<option value="${fc.companyID}">${esc(fc.companyName)}</option>`).join("")}
                            </select>
                        </label>
                    </div>

                    <!-- KHỐI THÔNG TIN NGƯỜI MUA XE (XÁC MINH THEO CHAT/HỒ SƠ) -->
                    <div style="background:#f8fafc;border:1px solid #cbd5e1;border-radius:8px;padding:14px;">
                        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;flex-wrap:wrap;gap:8px;">
                            <strong style="font-size:13px;color:#0f172a;">Thông tin người mua xe (Dùng lập Hợp đồng OTP &amp; Xuất Hóa đơn)</strong>
                            <button type="button" id="so-btn-autofill" class="button button-secondary" style="padding:4px 10px;font-size:12px;">⚡ Lấy thông tin từ hồ sơ khách hàng</button>
                        </div>
                        <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(220px, 1fr));gap:10px;">
                            <label style="font-size:12px;font-weight:600;">Họ và tên người mua *
                                <input type="text" name="customerName" id="so-cust-name" required maxlength="100" placeholder="Họ và tên khách hàng" style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                            </label>
                            <label style="font-size:12px;font-weight:600;">Số điện thoại liên lạc chính thức *
                                <input type="tel" name="customerPhone" id="so-cust-phone" required maxlength="20" placeholder="Ví dụ: 0912345678" style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                            </label>
                            <label style="font-size:12px;font-weight:600;">Email nhận chứng từ &amp; thông báo
                                <input type="email" name="customerEmail" id="so-cust-email" maxlength="100" placeholder="Ví dụ: customer@example.com" style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                            </label>
                            <label style="font-size:12px;font-weight:600;">Địa chỉ bàn giao xe / thường trú *
                                <input type="text" name="customerAddress" id="so-cust-address" required maxlength="255" placeholder="Địa chỉ thường trú / nhận xe" style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                            </label>
                        </div>
                        <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:10px;">
                            <label style="font-size:12px;font-weight:600;">Ghi chú xác minh từ Sales Staff (nguồn chat / thỏa thuận)
                                <input type="text" name="salesNote" id="so-sales-note" maxlength="200" placeholder="Đã xác nhận CCCD/thông tin qua chat..." style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                            </label>
                            <label style="font-size:12px;font-weight:600;">Ghi chú đơn hàng (nội bộ)
                                <input type="text" name="note" id="so-order-note" maxlength="300" placeholder="Ghi chú giao dịch..." style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                            </label>
                        </div>
                    </div>

                    <div style="display:flex;justify-content:flex-end;">
                        <button type="submit" class="button button-primary" style="padding:10px 24px;font-size:14px;font-weight:600;">Tạo đơn hàng (Cọc 30% / Còn 70%) →</button>
                    </div>
                </form>
                ${!cars.length ? `<div role="status" style="margin-top:10px;padding:10px 12px;border:1px solid #f5c16c;background:#fffbeb;color:#854d0e;border-radius:7px;font-size:12px;">
                    ${Number(inventory.physicalCars || 0) === 0
                        ? "Danh sách Cars hiện không có xe vật lý nào. Vui lòng nhờ Operations nhập xe thật (VIN, phiên bản, màu, giá và trạng thái AVAILABLE); hệ thống không tự tạo xe tồn kho giả."
                        : Number(inventory.availableCars || 0) === 0
                            ? "Kho có xe nhưng hiện không xe nào ở trạng thái AVAILABLE. Operations cần kiểm tra trạng thái tồn kho."
                            : "Có xe AVAILABLE nhưng tất cả đã được gắn vào đơn hàng. Vui lòng kiểm tra tồn kho/đơn hàng với Operations."}
                </div>` : ""}
                <div id="so-calc-preview" style="margin-top:10px;font-size:13px;color:#1e3a8a;background:#eff6ff;padding:8px 12px;border-radius:6px;">
                    Chọn xe để xem tự động tính toán Tổng tiền, Đặt cọc 30% và 70% còn lại theo RDS.
                </div>
            </section>
            ` : ""}

            <div style="padding:16px;background:#fff;border-radius:12px;border:1px solid #e2e8f0;overflow-x:auto;">
                <table style="width:100%;border-collapse:collapse;font-size:13px;">
                    <thead>
                        <tr style="border-bottom:2px solid #e2e8f0;text-align:left;">
                            <th style="padding:10px;">MÃ ĐƠN</th>
                            <th style="padding:10px;">KHÁCH HÀNG</th>
                            <th style="padding:10px;">XE &amp; VIN</th>
                            <th style="padding:10px;">PHƯƠNG THỨC</th>
                            <th style="padding:10px;">TỔNG TIỀN / CỌC 30% / CÒN NỢ</th>
                            <th style="padding:10px;">TIẾN ĐỘ RDS</th>
                            <th style="padding:10px;">THAO TÁC</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${orders.length ? orders.map(o => `
                            <tr style="border-bottom:1px solid #f1f5f9;vertical-align:top;">
                                <td style="padding:10px;"><strong>#${o.id}</strong><br><small>${esc(o.orderDate)}</small></td>
                                <td style="padding:10px;"><strong>${esc(o.customerName)}</strong><br><small>${esc(o.customerPhone)}</small></td>
                                <td style="padding:10px;">
                                    <strong>${esc(o.modelName || "—")}</strong><br>
                                    <small>${esc(o.versionName)} · VIN: ${esc(o.vin)}</small>
                                </td>
                                <td style="padding:10px;">
                                    <strong>${esc(o.paymentMethodName)}</strong>
                                    ${o.companyName ? `<br><small style="color:#1456db;">NH: ${esc(o.companyName)}</small>` : ""}
                                </td>
                                <td style="padding:10px;">
                                    Tổng: <strong>${fmtMoney(o.originalAmount)}</strong><br>
                                    Cọc 30%: <strong>${fmtMoney(o.depositAmount)}</strong><br>
                                    Đã thu: <span style="color:#15803d;font-weight:700;">${fmtMoney(o.paidAmount)}</span><br>
                                    Còn lại: <span style="color:#b91c1c;font-weight:700;">${fmtMoney(o.remainingAmount)}</span>
                                </td>
                                <td style="padding:10px;">
                                    <span class="status-pill">${esc(o.status)}</span><br>
                                    <small>HĐ: <strong>${esc(o.contractStatus || "Chưa lập")}</strong></small><br>
                                    <small>HĐơn: <strong>${esc(o.invoiceNumber || "Chưa xuất")}</strong></small><br>
                                    <small>Giao xe: <strong>${esc(o.deliveryStatus || "Chưa giao")}</strong></small>
                                </td>
                                <td style="padding:10px;min-width:210px;">
                                    <div style="display:flex;flex-direction:column;gap:6px;">
                                        ${o.status === "DEPOSITED" && !o.contractStatus && currentRole === "sales" ? `
                                            <button type="button" class="button button-primary btn-quick-contract" data-order-id="${o.id}" style="padding:6px 10px;font-size:12px;">+ Lập Hợp đồng (RDS 9.1)</button>
                                        ` : ""}
                                        ${o.status === "PAID" && o.invoiceNumber && o.deliveryStatus !== "DELIVERED" && currentRole === "operations" ? `
                                            <button type="button" class="button button-primary btn-quick-handover" data-order-id="${o.id}" style="padding:6px 10px;font-size:12px;">✓ Xác nhận Bàn giao xe</button>
                                        ` : ""}
                                        ${(o.status === "PENDING" || o.status === "RESERVED") && ["sales", "admin"].includes(currentRole) ? `
                                            <button type="button" class="button button-secondary btn-send-payment-chat" data-order-id="${o.id}" data-customer-id="${o.customerId}" data-model="${esc(o.modelName || "")}" data-deposit="${o.depositAmount}" style="padding:5px 10px;font-size:12px;color:#1e40af;">✉ Gửi YC cọc qua chat</button>
                                            <button type="button" class="button button-secondary btn-cancel-order" data-order-id="${o.id}" style="padding:5px 10px;font-size:12px;color:#b91c1c;">Hủy đơn hàng</button>
                                        ` : ""}
                                    </div>
                                </td>
                            </tr>
                        `).join("") : `<tr><td colspan="7" style="padding:24px;text-align:center;">Chưa có đơn hàng nào.</td></tr>`}
                    </tbody>
                </table>
            </div>
        `;
        prefillOrderCustomer = null;

        const customerSelect = document.getElementById("so-customer");
        const carSelect = document.getElementById("so-car");
        const methodSelect = document.getElementById("so-method");
        const companyWrap = document.getElementById("so-company-wrap");
        const preview = document.getElementById("so-calc-preview");
        const btnAutofill = document.getElementById("so-btn-autofill");

        async function autofillCustomerInfo(force = false) {
            const selectedId = Number(customerSelect?.value);
            if (!selectedId) {
                if (force) notify("Vui lòng chọn khách hàng trước khi lấy thông tin.");
                return;
            }
            const nameInput = document.getElementById("so-cust-name");
            const phoneInput = document.getElementById("so-cust-phone");
            const emailInput = document.getElementById("so-cust-email");
            const addrInput = document.getElementById("so-cust-address");

            // 1. Thử điền nhanh từ mảng cache sẵn trong payload
            const found = customers.find(c => Number(c.customerID) === selectedId);
            if (found) {
                if (nameInput) nameInput.value = found.customerName || "";
                if (phoneInput) phoneInput.value = found.phone || "";
                if (emailInput) emailInput.value = found.email || "";
                if (addrInput) addrInput.value = found.address || "";
            }

            // 2. Gọi API backend lấy dữ liệu mới nhất từ database
            try {
                const res = await fetchJson(`/customer/profile?mode=staff&customerId=${selectedId}`);
                if (res && res.customer) {
                    const c = res.customer;
                    if (nameInput) nameInput.value = c.customerName || "";
                    if (phoneInput) phoneInput.value = c.phone || "";
                    if (emailInput) emailInput.value = c.email || "";
                    if (addrInput) addrInput.value = c.address || "";
                    if (force) notify(`Đã lấy thông tin mới nhất từ database của khách #${selectedId}.`);
                }
            } catch (err) {
                console.warn("Could not fetch realtime customer profile", err);
            }
        }

        customerSelect?.addEventListener("change", () => autofillCustomerInfo(false));
        btnAutofill?.addEventListener("click", () => autofillCustomerInfo(true));
        if (customerSelect?.value) autofillCustomerInfo(false);

        function updateOrderPreview() {
            if (!carSelect || !preview) return;
            const price = Number(carSelect.selectedOptions[0]?.dataset?.price || 0);
            const mName = methodSelect?.selectedOptions[0]?.dataset?.name || "";
            if (companyWrap) {
                companyWrap.style.display = mName === "INSTALLMENT" ? "block" : "none";
            }
            if (price > 0) {
                const dep = Math.round(price * 0.30);
                const rem = price - dep;
                preview.innerHTML = `Tổng giá trị xe: <strong>${fmtMoney(price)}</strong> · Tiền đặt cọc 30%: <strong>${fmtMoney(dep)}</strong> · Số tiền 70% còn lại: <strong>${fmtMoney(rem)}</strong> (${mName === "INSTALLMENT" ? "Ngân hàng đối tác giải ngân trực tiếp 70%" : "Khách hàng thanh toán trực tiếp 70%"}).`;
            }
        }
        carSelect?.addEventListener("change", updateOrderPreview);
        methodSelect?.addEventListener("change", updateOrderPreview);
        updateOrderPreview();

        document.getElementById("staff-create-order-form")?.addEventListener("submit", async (e) => {
            e.preventDefault();
            const f = e.currentTarget;
            try {
                const res = await postForm("/orders", {
                    action: "create",
                    customerId: f.customerId.value,
                    carId: f.carId.value,
                    method: f.paymentMethodId.selectedOptions[0]?.dataset?.name,
                    companyId: f.companyId?.value || "",
                    customerName: f.customerName?.value || "",
                    customerPhone: f.customerPhone?.value || "",
                    customerEmail: f.customerEmail?.value || "",
                    customerAddress: f.customerAddress?.value || "",
                    salesNote: f.salesNote?.value || "",
                    note: f.note.value,
                    csrfToken: data.csrfToken
                });
                notify(res.message || "Đã tạo đơn hàng thành công.");
                await loadTransactionsAndDepositsView(onlyDeposits);
            } catch (err) {
                notify(err.message);
            }
        });

        content.querySelectorAll(".btn-quick-contract").forEach(btn => {
            btn.addEventListener("click", async () => {
                try {
                    const res = await postForm("/contract", {
                        action: "create",
                        csrfToken: data.csrfToken,
                        orderId: btn.dataset.orderId,
                        note: "Hợp đồng mua bán xe điện VinFast theo quy chuẩn RDS."
                    });
                    notify(res.message);
                    await loadTransactionsAndDepositsView(onlyDeposits);
                } catch (err) {
                    notify(err.message);
                }
            });
        });

        content.querySelectorAll(".btn-quick-handover").forEach(btn => {
            btn.addEventListener("click", async () => {
                try {
                    const res = await postForm("/delivery", {
                        action: "handover",
                        csrfToken: data.csrfToken,
                        orderId: btn.dataset.orderId,
                        deliveryAddress: "Showroom VinFast Cần Thơ",
                        note: "Đã kiểm tra hóa đơn và xuất kho bàn giao xe."
                    });
                    notify(res.message);
                    await loadTransactionsAndDepositsView(onlyDeposits);
                } catch (err) {
                    notify(err.message);
                }
            });
        });

        content.querySelectorAll(".btn-send-payment-chat").forEach(btn => {
            btn.addEventListener("click", async () => {
                const orderId = btn.dataset.orderId;
                const dep = fmtMoney(Number(btn.dataset.deposit || 0));
                const model = btn.dataset.model || "xe";
                try {
                    const consultData = await fetchJson("/consultation?format=json");
                    const customerConsult = (consultData.consultations || []).find(c => Number(c.customerId) === Number(btn.dataset.customerId));
                    if (!customerConsult) {
                        notify(`Khách hàng #${btn.dataset.customerId} chưa có yêu cầu tư vấn trên hệ thống. Khách có thể thanh toán trực tiếp tại mục Đơn hàng.`);
                        return;
                    }
                    const noteText = `[YÊU CẦU THANH TOÁN ĐẶT CỌC]: Đơn hàng #${orderId} (${model}). Số tiền cọc 30%: ${dep}. Quý khách vui lòng kiểm tra và thanh toán đặt cọc qua VietQR (SePay - MB Bank).`;
                    const res = await postForm("/consultation", {
                        action: "update",
                        requestId: customerConsult.id,
                        status: customerConsult.status === "PENDING" ? "IN_PROGRESS" : customerConsult.status,
                        note: noteText
                    });
                    notify(res.message || `Đã gửi yêu cầu cọc đơn #${orderId} vào hội thoại #${customerConsult.id}.`);
                } catch (err) {
                    notify(err.message || "Không thể gửi yêu cầu cọc qua chat.");
                }
            });
        });

        content.querySelectorAll(".btn-cancel-order").forEach(btn => {
            btn.addEventListener("click", async () => {
                try {
                    const res = await postForm("/order-status", {
                        orderId: btn.dataset.orderId,
                        status: "CANCELLED",
                        note: "Nhân viên hủy đơn chưa đặt cọc."
                    });
                    notify(res.message);
                    await loadTransactionsAndDepositsView(onlyDeposits);
                } catch (err) {
                    notify(err.message);
                }
            });
        });
    }

    async function loadAccountantPaymentsAndDebtsView(mode = "payments") {
        content.innerHTML = `<p>Đang tải giao dịch chờ đối soát và công nợ từ cơ sở dữ liệu...</p>`;
        const data = await fetchJson("/staff/payment-review?format=json");
        const pending = data.pendingPayments || [];
        const debts = data.openDebts || [];
        const csrf = esc(data.csrfToken || "");
        const debtsFirst = mode === "debts";
        const renderDebts = () => `
            <section id="accountant-debts" style="padding:16px;background:#fff;border:1px solid #e2e8f0;border-radius:12px;margin:16px 0;">
                <h2 style="margin:0 0 10px;">Theo dõi công nợ đơn hàng (${debts.length})</h2>
                ${debts.length ? debts.map(d => `
                    <article style="padding:12px 0;border-top:1px solid #e2e8f0;">
                        <strong>Đơn #${d.orderId} · ${esc(d.customer)}</strong>
                        <p>Tổng ${fmtMoney(d.totalAmount)} · Cọc ${fmtMoney(d.depositAmount)} · Còn nợ <strong>${fmtMoney(d.remainingAmount)}</strong></p>
                        <p>Phương thức ${esc(d.paymentMethod)} ${d.company ? `· Đối tác ${esc(d.company)}` : ""} · Đơn ${esc(d.orderStatus)} · Hợp đồng ${esc(d.contractStatus || "Chưa ký")}</p>
                        ${d.paymentMethod === "INSTALLMENT" && d.contractStatus === "SIGNED" && ["DEPOSITED", "PROCESSING"].includes(d.orderStatus) ? `
                            <form class="accountant-review-form" style="display:inline-flex;" data-action-label="Đã ghi nhận yêu cầu giải ngân 70% cho đơn #${d.orderId}.">
                                <input type="hidden" name="csrfToken" value="${csrf}"><input type="hidden" name="format" value="json">
                                <input type="hidden" name="action" value="partner"><input type="hidden" name="orderId" value="${d.orderId}">
                                <button type="submit" class="button button-primary">Ghi nhận chờ giải ngân 70%</button>
                            </form>` : ""}
                    </article>`).join("") : `<p>Không có đơn hàng còn công nợ.</p>`}
            </section>`;
        const renderPending = () => `
            <section id="accountant-pending">
                <h2>Giao dịch đang chờ xác nhận (${pending.length})</h2>
                ${pending.length ? pending.map(p => `
                    <article style="padding:16px;background:#fff;border:1px solid #e2e8f0;border-radius:12px;margin:12px 0;">
                        <h3 style="margin:0 0 8px;">Khoản #${p.paymentId} · Đơn #${p.orderId}</h3>
                        <p>Khách hàng ${esc(p.customer)} · Loại ${esc(p.type)} · ${esc(p.methodName)}</p>
                        <p>Số tiền yêu cầu <strong>${fmtMoney(p.amount)}</strong></p>
                        <p>Nội dung đối chiếu <strong>${esc(p.reference)}</strong></p>
                        ${p.note ? `<p>Ghi chú: ${esc(p.note)}</p>` : ""}
                        <form class="accountant-review-form" style="display:flex;flex-wrap:wrap;gap:10px;align-items:end;" data-action-label="Đã xác nhận khoản #${p.paymentId}.">
                            <input type="hidden" name="csrfToken" value="${csrf}"><input type="hidden" name="format" value="json">
                            <input type="hidden" name="paymentId" value="${p.paymentId}"><input type="hidden" name="action" value="confirm">
                            <label style="display:grid;gap:4px;">Số tiền thực nhận (VND)<input name="receivedAmount" type="number" min="1" step="1" required></label>
                            <label style="display:grid;gap:4px;">Mã sao kê hoặc chứng từ<input name="bankTransaction" maxlength="80" pattern="[A-Za-z0-9./_-]{4,80}" required></label>
                            <label style="display:flex;gap:6px;align-items:center;"><input name="checkedStatement" type="checkbox" required>Đã đối chiếu chứng từ thực nhận</label>
                            <button type="submit" class="button button-primary">Xác nhận đã nhận tiền</button>
                        </form>
                        <form class="accountant-review-form" style="display:flex;gap:8px;margin-top:10px;align-items:end;" data-action-label="Đã từ chối khoản #${p.paymentId}.">
                            <input type="hidden" name="csrfToken" value="${csrf}"><input type="hidden" name="format" value="json">
                            <input type="hidden" name="paymentId" value="${p.paymentId}"><input type="hidden" name="action" value="reject">
                            <label style="display:grid;gap:4px;">Lý do từ chối<input name="reason" maxlength="200" required></label>
                            <button type="submit" class="button button-secondary">Từ chối</button>
                        </form>
                    </article>`).join("") : `<p>Không có khoản PENDING.</p>`}
            </section>`;
        content.innerHTML = `
            <div class="page-heading"><div>
                <div class="eyebrow"><span class="eyebrow-line"></span>UC-10–12 · KẾ TOÁN ĐỐI SOÁT</div>
                <h1>${debtsFirst ? "Quản lý công nợ & giải ngân 70%" : "Xác nhận thanh toán"}</h1>
                <p>Chỉ xác nhận sau khi đối chiếu sao kê ngân hàng hoặc chứng từ giải ngân thật. Thao tác được lưu vào database.</p>
            </div></div>
            <div class="inline-note">Trang kế toán chạy trong dashboard. Giải ngân 70% chỉ được tạo chờ xác nhận sau khi hợp đồng đã ký; không tự kết nối/xác nhận với ngân hàng.</div>
            ${debtsFirst ? renderDebts() + renderPending() : renderPending() + renderDebts()}
            <section>
                <h2>Khoản đã xác nhận (${(data.completedPayments || []).length})</h2>
                ${(data.completedPayments || []).map(p => `
                    <article style="padding:14px;background:#fff;border:1px solid #e2e8f0;border-radius:10px;margin:10px 0;">
                        <div style="display:flex;justify-content:space-between;align-items:center;">
                            <strong>Khoản #${esc(p.paymentId)} · Đơn #${esc(p.orderId)} · ${fmtMoney(p.amount)}</strong>
                            <span class="status-pill" style="background:#dcfce7;color:#166534;border:1px solid #86efac;font-size:11px;font-weight:700;">COMPLETED</span>
                        </div>
                        <p style="margin:4px 0;">Phương thức: <strong>${esc(p.methodName)}</strong> · Loại: <strong>${esc(p.type)}</strong> · Mã đối chiếu: <code>${esc(p.reference)}</code></p>
                        ${p.type === "DEPOSIT" ? `<p style="margin:4px 0;"><a href="${appContext}/staff/sales-document?kind=deposit&amp;orderId=${p.orderId}" class="text-link" style="font-size:12px;">📥 Tải biên nhận cọc (Word)</a></p>` : ""}
                        ${p.sePayEvidence ? `
                            <div style="background:#f0fdf4;border:1px solid #86efac;border-radius:6px;padding:8px 12px;margin-top:6px;font-size:12px;color:#166534;">
                                <strong>Bằng chứng giao dịch SePay (MB Bank · 0917231548 · Do Tan Dat):</strong><br>
                                Mã GD SePay: <strong>#${esc(p.sePayEvidence.eventId || p.sePayEvidence.transactionNo)}</strong>
                                · Cổng: <strong>${esc(p.sePayEvidence.gateway || p.sePayEvidence.bankCode || "MBBank")}</strong>
                                · Thời gian: <strong>${esc(p.sePayEvidence.payDate || p.sePayEvidence.transactionDate || "—")}</strong><br>
                                Số tiền thực nhận ngân hàng (Chỉ đọc): <strong style="font-size:13px;color:#059669;">${fmtMoney(p.sePayEvidence.received)}</strong>
                            </div>
                        ` : ""}
                        <div style="margin-top:6px;font-size:11px;color:#64748b;">
                            Trạng thái đối soát: Tự động ghi nhận qua Webhook SePay. <em>(Lưu cờ [Đã đối chiếu] độc lập: BLOCKED do schema DB chưa có cột Reconciled/ReconciledBy/ReconciledAt)</em>
                        </div>
                    </article>`).join("") || "<p>Chưa có khoản đã xác nhận.</p>"}
            </section>
        `;
        content.querySelectorAll(".accountant-review-form").forEach(form => form.addEventListener("submit", async event => {
            event.preventDefault();
            if (!form.reportValidity()) return;
            const button = form.querySelector('button[type="submit"]');
            if (!button || button.disabled) return;
            button.disabled = true;
            try {
                const values = Object.fromEntries(new FormData(form).entries());
                const result = await postForm("/staff/payment-review", values);
                notify(result.message || form.dataset.actionLabel || "Đã lưu.");
                await loadAccountantPaymentsAndDebtsView(mode);
            } catch (err) {
                notify(err.message || "Không thể lưu thao tác kế toán.");
                button.disabled = false;
            }
        }));
    }

    async function loadContractsAndInvoicesView() {
        content.innerHTML = `<p>Đang tải danh sách hợp đồng, đơn hàng và hóa đơn...</p>`;
        const [contractData, invoiceData, orderData] = await Promise.all([
            fetchJson("/contract?format=json"),
            fetchJson("/invoice?format=json"),
            fetchJson("/orders?format=json")
        ]);
        const contracts = contractData.contracts || [];
        const invoices = invoiceData.invoices || [];
        const orders = orderData.orders || [];
        const canCreateContract = ["sales", "accountant", "admin"].includes(currentRole);
        const canIssueInvoice = currentRole === "accountant";
        const invoiceCsrfToken = invoiceData.csrfToken || "";

        const ordersReadyForContract = orders.filter(o => currentRole === "sales" && o.status === "DEPOSITED" && !o.contractStatus);
        const ordersReadyForInvoice = orders.filter(o => o.status === "PAID" && o.contractStatus === "SIGNED" && !o.invoiceNumber);

        content.innerHTML = `
            <div class="page-heading"><div>
                <div class="eyebrow"><span class="eyebrow-line"></span>UC-13 · HỢP ĐỒNG MUA BÁN &amp; HÓA ĐƠN GTGT</div>
                <h1>Quản lý Hợp đồng (${contracts.length}) &amp; Hóa đơn (${invoices.length})</h1>
                <p>Lập hợp đồng sau khi khách đặt cọc 30% (DEPOSITED), theo dõi trạng thái khách ký OTP (SIGNED), và phát hành hóa đơn sau khi thu đủ 100% (PAID).</p>
            </div></div>

            ${canCreateContract && ordersReadyForContract.length ? `
            <section style="padding:16px;background:#eff6ff;border:1px solid #bfdbfe;border-radius:12px;margin-bottom:18px;">
                <h3 style="margin:0 0 8px;font-size:15px;">Đơn hàng đã đặt cọc 30% chờ lập Hợp đồng (${ordersReadyForContract.length})</h3>
                <div style="display:flex;gap:10px;flex-wrap:wrap;">
                    ${ordersReadyForContract.map(o => `
                        <button type="button" class="button button-primary btn-create-contract" data-order-id="${o.id}">
                            + Lập Hợp đồng cho Đơn #${o.id} (${esc(o.customerName)} · ${esc(o.modelName)})
                        </button>
                    `).join("")}
                </div>
            </section>
            ` : ""}

            ${canIssueInvoice && ordersReadyForInvoice.length ? `
            <section style="padding:16px;background:#f0fdf4;border:1px solid #bbf7d0;border-radius:12px;margin-bottom:18px;">
                <h3 style="margin:0 0 8px;font-size:15px;">Đơn hàng đã ký HĐ &amp; Thanh toán đủ 100% chờ Xuất Hóa đơn (${ordersReadyForInvoice.length})</h3>
                <div style="display:flex;gap:10px;flex-wrap:wrap;">
                    ${ordersReadyForInvoice.map(o => `
                        <button type="button" class="button button-primary btn-issue-invoice" data-order-id="${o.id}">
                            ✓ Phát hành Hóa đơn cho Đơn #${o.id} (${esc(o.customerName)} · ${fmtMoney(o.originalAmount)})
                        </button>
                    `).join("")}
                </div>
            </section>
            ` : ""}

            <h3 style="margin:12px 0 8px;">1. Danh sách Hợp đồng (Bảng Contracts)</h3>
            <div style="padding:16px;background:#fff;border-radius:12px;border:1px solid #e2e8f0;overflow-x:auto;margin-bottom:22px;">
                <table style="width:100%;border-collapse:collapse;font-size:13px;">
                    <thead>
                        <tr style="border-bottom:2px solid #e2e8f0;text-align:left;">
                            <th style="padding:10px;">SỐ HỢP ĐỒNG</th>
                            <th style="padding:10px;">ĐƠN HÀNG</th>
                            <th style="padding:10px;">KHÁCH HÀNG</th>
                            <th style="padding:10px;">XE &amp; VIN</th>
                            <th style="padding:10px;">GIÁ TRỊ HĐ</th>
                            <th style="padding:10px;">TRẠNG THÁI KÝ OTP</th>
                            <th style="padding:10px;">ĐIỀU KHOẢN THANH TOÁN</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${contracts.length ? contracts.map(c => `
                            <tr style="border-bottom:1px solid #f1f5f9;">
                                <td style="padding:10px;"><strong>${esc(c.contractNumber)}</strong><br><small>${esc(c.contractDate)}</small></td>
                                <td style="padding:10px;">#${c.orderID}</td>
                                <td style="padding:10px;"><strong>${esc(c.customerName)}</strong><br><small>${esc(c.customerPhone)}</small></td>
                                <td style="padding:10px;">${esc(c.modelName)}<br><small>VIN: ${esc(c.vin)}</small></td>
                                <td style="padding:10px;"><strong>${fmtMoney(c.contractAmount)}</strong></td>
                                <td style="padding:10px;"><span class="status-pill">${esc(c.status)}</span></td>
                                <td style="padding:10px;max-width:300px;font-size:12px;">${esc(c.note)}</td>
                            </tr>
                        `).join("") : `<tr><td colspan="7" style="padding:20px;text-align:center;">Chưa có hợp đồng nào.</td></tr>`}
                    </tbody>
                </table>
            </div>

            <h3 style="margin:12px 0 8px;">2. Danh sách Hóa đơn đã phát hành (Bảng Invoices)</h3>
            <div style="padding:16px;background:#fff;border-radius:12px;border:1px solid #e2e8f0;overflow-x:auto;">
                <table style="width:100%;border-collapse:collapse;font-size:13px;">
                    <thead>
                        <tr style="border-bottom:2px solid #e2e8f0;text-align:left;">
                            <th style="padding:10px;">SỐ HÓA ĐƠN</th>
                            <th style="padding:10px;">ĐƠN HÀNG</th>
                            <th style="padding:10px;">NGÀY PHÁT HÀNH</th>
                            <th style="padding:10px;">TỔNG TIỀN</th>
                            <th style="padding:10px;">TRẠNG THÁI</th>
                            <th style="padding:10px;">GHI CHÚ</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${invoices.length ? invoices.map(inv => `
                            <tr style="border-bottom:1px solid #f1f5f9;">
                                <td style="padding:10px;"><strong>${esc(inv.invoiceNumber)}</strong>
                                ${currentRole === "accountant" && inv.status === "ISSUED" ? `<a href="${appContext}/staff/sales-document?kind=invoice&amp;orderId=${inv.orderID}">Tải Word</a>` : ""}</td>
                                <td style="padding:10px;">#${inv.orderID}</td>
                                <td style="padding:10px;">${esc(inv.invoiceDate)}</td>
                                <td style="padding:10px;"><strong>${fmtMoney(inv.totalAmount)}</strong></td>
                                <td style="padding:10px;"><span class="status-pill">${esc(inv.status)}</span></td>
                                <td style="padding:10px;">${esc(inv.note)}</td>
                            </tr>
                        `).join("") : `<tr><td colspan="6" style="padding:20px;text-align:center;">Chưa có hóa đơn nào được phát hành.</td></tr>`}
                    </tbody>
                </table>
            </div>
        `;

        content.querySelectorAll(".btn-create-contract").forEach(btn => {
            btn.addEventListener("click", async () => {
                try {
                    const res = await postForm("/contract", {
                        action: "create",
                        csrfToken: contractData.csrfToken,
                        orderId: btn.dataset.orderId
                    });
                    notify(res.message);
                    await loadContractsAndInvoicesView();
                } catch (err) {
                    notify(err.message);
                }
            });
        });

        content.querySelectorAll(".btn-issue-invoice").forEach(btn => {
            btn.addEventListener("click", async () => {
                try {
                    const res = await postForm("/invoice", {
                        orderId: btn.dataset.orderId,
                        csrfToken: invoiceCsrfToken,
                        note: "Kế toán phát hành hóa đơn chính thức sau khi thu đủ 100%."
                    });
                    notify(res.message);
                    await loadContractsAndInvoicesView();
                } catch (err) {
                    notify(err.message);
                }
            });
        });
    }

    async function loadInstallmentsPartnerView() {
        content.innerHTML = `<p>Đang tải danh sách đối tác tài chính (Bảng FinancialCompanies)...</p>`;
        const data = await fetchJson("/contract?format=json");
        const companies = data.financialCompanies || [];
        const plans = data.installmentPlans || [];
        content.innerHTML = `
            <div class="page-heading"><div>
                <div class="eyebrow"><span class="eyebrow-line"></span>UC-12 · ĐỐI TÁC TÀI CHÍNH &amp; CHÍNH SÁCH TRẢ GÓP</div>
                <h1>Danh mục Ngân hàng / Công ty Tài chính Đối tác (${companies.length})</h1>
                <p>Theo RDS (UC-12, BR2a, EX-08): Khách mua trả góp đóng 30% cọc trên hệ thống; 70% còn lại do ngân hàng đối tác giải ngân trực tiếp cho đại lý.</p>
            </div></div>
            <div style="padding:16px;background:#fff;border-radius:12px;border:1px solid #e2e8f0;margin-bottom:20px;">
                <table style="width:100%;border-collapse:collapse;font-size:13px;">
                    <thead>
                        <tr style="border-bottom:2px solid #e2e8f0;text-align:left;">
                            <th style="padding:10px;">MÃ ĐỐI TÁC</th>
                            <th style="padding:10px;">TÊN ĐƠN VỊ TÀI CHÍNH</th>
                            <th style="padding:10px;">HOTLINE &amp; EMAIL</th>
                            <th style="padding:10px;">ĐỊA CHỈ</th>
                            <th style="padding:10px;">CHÍNH SÁCH THAM KHẢO</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${companies.map(fc => `
                            <tr style="border-bottom:1px solid #f1f5f9;">
                                <td style="padding:10px;"><strong>#${fc.companyID}</strong></td>
                                <td style="padding:10px;"><strong>${esc(fc.companyName)}</strong></td>
                                <td style="padding:10px;">${esc(fc.phone)}<br><small>${esc(fc.email)}</small></td>
                                <td style="padding:10px;">${esc(fc.address)}</td>
                                <td style="padding:10px;">${esc(fc.description)}</td>
                            </tr>
                        `).join("")}
                    </tbody>
                </table>
            </div>
            <div style="padding:16px;background:#fff;border-radius:12px;border:1px solid #e2e8f0;">
                <h3 style="margin:0 0 10px;">Bảng lãi suất tham khảo theo kỳ hạn (RDS BR9–BR10)</h3>
                <table style="width:100%;border-collapse:collapse;font-size:13px;">
                    <thead>
                        <tr style="border-bottom:2px solid #e2e8f0;text-align:left;">
                            <th style="padding:10px;">ĐƠN VỊ</th>
                            <th style="padding:10px;">GÓI VAY</th>
                            <th style="padding:10px;">KỲ HẠN</th>
                            <th style="padding:10px;">LÃI SUẤT NĂM</th>
                            <th style="padding:10px;">MÔ TẢ</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${plans.map(p => `
                            <tr style="border-bottom:1px solid #f1f5f9;">
                                <td style="padding:10px;"><strong>${esc(p.companyName)}</strong></td>
                                <td style="padding:10px;">${esc(p.planName)}</td>
                                <td style="padding:10px;">${p.loanTermMonths} tháng</td>
                                <td style="padding:10px;"><strong>${p.interestRate}%/năm</strong></td>
                                <td style="padding:10px;">${esc(p.description)}</td>
                            </tr>
                        `).join("")}
                    </tbody>
                </table>
            </div>
        `;
    }

    async function loadHandoverAndInventoryView(subMode = "handover") {
        content.innerHTML = `<p>Đang tải dữ liệu kho xe, giao dịch nhập/xuất và bàn giao xe...</p>`;
        const [delData, ordData] = await Promise.all([
            fetchJson("/delivery?format=json"),
            fetchJson("/orders?format=json")
        ]);
        const deliveries = delData.deliveries || [];
        const cars = delData.cars || [];
        const carStatuses = delData.carStatuses || [];
        const invTxs = delData.inventoryTransactions || [];
        const models = delData.models || [];
        const versions = delData.versions || [];
        const colors = delData.colors || [];
        const orders = ordData.orders || [];
        const readyOrders = orders.filter(o => currentRole === "operations" && o.contractStatus === "SIGNED" && o.status === "PAID" && o.invoiceNumber && o.deliveryStatus !== "DELIVERED");

        if (subMode === "vehicle-movements") {
            content.innerHTML = `
                <div class="page-heading"><div>
                    <div class="eyebrow"><span class="eyebrow-line"></span>UC-09 · NHẬP / XUẤT KHO XE VẬT LÝ (10.2 &amp; 10.3)</div>
                    <h1>Quản lý Nhập / Xuất kho xe (${invTxs.length} giao dịch)</h1>
                    <p>Nhập xe vật lý mới vào bảng Cars (AVAILABLE) và theo dõi lịch sử nhập/xuất trong InventoryTransactions.</p>
                </div></div>

                <section style="padding:16px;background:#fff;border-radius:12px;border:1px solid #cbd5e1;margin-bottom:20px;">
                    <h3 style="margin:0 0 12px;font-size:15px;">+ Nhập kho xe vật lý mới (Import Vehicle - RDS 10.2)</h3>
                    <form id="import-car-form" style="display:grid;grid-template-columns:repeat(auto-fit, minmax(180px, 1fr));gap:10px;align-items:end;">
                        <label style="font-size:12px;font-weight:600;">Mẫu xe *
                            <select name="modelId" required style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                                ${models.map(m => `<option value="${m.modelID}">${esc(m.modelName)}</option>`).join("")}
                            </select>
                        </label>
                        <label style="font-size:12px;font-weight:600;">Phiên bản *
                            <select name="versionId" required style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                                ${versions.map(v => `<option value="${v.versionID}">${esc(v.versionName)}</option>`).join("")}
                            </select>
                        </label>
                        <label style="font-size:12px;font-weight:600;">Màu sắc *
                            <select name="colorId" required style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                                ${colors.map(c => `<option value="${c.colorID}">${esc(c.colorName)}</option>`).join("")}
                            </select>
                        </label>
                        <label style="font-size:12px;font-weight:600;">Số khung VIN (Đúng 17 ký tự) *
                            <input type="text" name="vin" minlength="17" maxlength="17" placeholder="VD: VNF2026EV00000999" required style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                        </label>
                        <label style="font-size:12px;font-weight:600;">Số máy *
                            <input type="text" name="engineNumber" placeholder="VD: VNF-ENG-2026-999" required style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                        </label>
                        <label style="font-size:12px;font-weight:600;">Giá bán (VND) *
                            <input type="number" name="price" min="100000000" step="1000000" value="898000000" required style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                        </label>
                        <input type="hidden" name="year" value="2026">
                        <button type="submit" class="button button-primary" style="padding:9px;">+ Nhập kho xe (IMPORT) →</button>
                    </form>
                </section>

                <div style="padding:16px;background:#fff;border-radius:12px;border:1px solid #e2e8f0;overflow-x:auto;">
                    <table style="width:100%;border-collapse:collapse;font-size:13px;">
                        <thead>
                            <tr style="border-bottom:2px solid #e2e8f0;text-align:left;">
                                <th style="padding:10px;">MÃ GD KHO</th>
                                <th style="padding:10px;">MÃ XE (CAR ID)</th>
                                <th style="padding:10px;">LOẠI GIAO DỊCH</th>
                                <th style="padding:10px;">NGÀY THỰC HIỆN</th>
                                <th style="padding:10px;">GHI CHÚ</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${invTxs.length ? invTxs.map(t => `
                                <tr style="border-bottom:1px solid #f1f5f9;">
                                    <td style="padding:10px;"><strong>#${t.inventoryTransactionID}</strong></td>
                                    <td style="padding:10px;">Car #${t.carID}</td>
                                    <td style="padding:10px;"><span class="status-pill">${esc(t.transactionType)}</span></td>
                                    <td style="padding:10px;">${esc(t.transactionDate)}</td>
                                    <td style="padding:10px;">${esc(t.note)}</td>
                                </tr>
                            `).join("") : `<tr><td colspan="5" style="padding:20px;text-align:center;">Chưa có giao dịch nhập/xuất kho.</td></tr>`}
                        </tbody>
                    </table>
                </div>
            `;
            document.getElementById("import-car-form")?.addEventListener("submit", async (e) => {
                e.preventDefault();
                const f = e.currentTarget;
                try {
                    const res = await postForm("/delivery", {
                        action: "importCar",
                        modelId: f.modelId.value,
                        versionId: f.versionId.value,
                        colorId: f.colorId.value,
                        vin: f.vin.value,
                        engineNumber: f.engineNumber.value,
                        price: f.price.value,
                        year: f.year.value
                    });
                    notify(res.message);
                    await loadHandoverAndInventoryView("vehicle-movements");
                } catch (err) {
                    notify(err.message);
                }
            });
            return;
        }

        if (subMode === "vehicle-status") {
            content.innerHTML = `
                <div class="page-heading"><div>
                    <div class="eyebrow"><span class="eyebrow-line"></span>UC-09 · TRẠNG THÁI XE VẬT LÝ (10.4)</div>
                    <h1>Danh sách Xe vật lý trong kho (${cars.length} xe)</h1>
                    <p>Quản lý trạng thái từng xe vật lý theo số khung VIN (AVAILABLE → RESERVED → DEPOSITED → SOLD → DELIVERED).</p>
                </div></div>
                <div style="padding:16px;background:#fff;border-radius:12px;border:1px solid #e2e8f0;overflow-x:auto;">
                    <table style="width:100%;border-collapse:collapse;font-size:13px;">
                        <thead>
                            <tr style="border-bottom:2px solid #e2e8f0;text-align:left;">
                                <th style="padding:10px;">CAR ID</th>
                                <th style="padding:10px;">MẪU XE &amp; PHIÊN BẢN</th>
                                <th style="padding:10px;">MÀU SẮC</th>
                                <th style="padding:10px;">SỐ KHUNG (VIN)</th>
                                <th style="padding:10px;">GIÁ BÁN</th>
                                <th style="padding:10px;">TRẠNG THÁI HIỆN TẠI</th>
                                <th style="padding:10px;">CẬP NHẬT TRẠNG THÁI</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${cars.length ? cars.map(c => `
                                <tr style="border-bottom:1px solid #f1f5f9;">
                                    <td style="padding:10px;"><strong>#${c.carID}</strong> (${esc(c.carCode)})</td>
                                    <td style="padding:10px;"><strong>${esc(c.modelName)}</strong><br><small>${esc(c.versionName)}</small></td>
                                    <td style="padding:10px;">${esc(c.colorName)}</td>
                                    <td style="padding:10px;"><code>${esc(c.vin)}</code></td>
                                    <td style="padding:10px;">${fmtMoney(c.price)}</td>
                                    <td style="padding:10px;"><span class="status-pill">${esc(c.statusName)}</span></td>
                                    <td style="padding:10px;">
                                        <div style="display:flex;gap:6px;">
                                            <select id="cstatus-${c.carID}" style="padding:5px;border:1px solid #cbd5e1;border-radius:6px;">
                                                ${carStatuses.map(s => `<option value="${s.statusName}" ${s.statusName === c.statusName ? "selected" : ""}>${esc(s.statusName)}</option>`).join("")}
                                            </select>
                                            <button type="button" class="button button-secondary btn-update-car-status" data-car-id="${c.carID}" style="padding:5px 10px;font-size:12px;">Lưu</button>
                                        </div>
                                    </td>
                                </tr>
                            `).join("") : `<tr><td colspan="7" style="padding:24px;text-align:center;color:#64748b;">Chưa có xe vật lý trong kho. Operations cần nhập xe thật có VIN để trạng thái và tồn kho được hiển thị tại đây.</td></tr>`}
                        </tbody>
                    </table>
                </div>
            `;
            content.querySelectorAll(".btn-update-car-status").forEach(btn => {
                btn.addEventListener("click", async () => {
                    const id = btn.dataset.carId;
                    const st = document.getElementById(`cstatus-${id}`).value;
                    try {
                        const res = await postForm("/delivery", {
                            action: "updateCarStatus",
                            carId: id,
                            statusName: st
                        });
                        notify(res.message);
                        await loadHandoverAndInventoryView("vehicle-status");
                    } catch (err) {
                        notify(err.message);
                    }
                });
            });
            return;
        }

        // Default: handover
        content.innerHTML = `
            <div class="page-heading"><div>
                <div class="eyebrow"><span class="eyebrow-line"></span>UC-14 &amp; 10.3 · XUẤT KHO &amp; BÀN GIAO XE</div>
                <h1>Quản lý Bàn giao xe (${deliveries.length} biên bản bàn giao)</h1>
                <p>Theo RDS (10.3 BR5): Xe chỉ được xuất kho bàn giao sau khi đơn hàng đã thanh toán đủ 100% (PAID) và đã phát hành Hóa đơn (ISSUED).</p>
            </div></div>

            ${readyOrders.length ? `
            <section style="padding:16px;background:#f0fdf4;border:1px solid #bbf7d0;border-radius:12px;margin-bottom:20px;">
                <h3 style="margin:0 0 10px;font-size:15px;">Đơn hàng đủ điều kiện Xuất kho &amp; Bàn giao xe ngay (${readyOrders.length})</h3>
                ${readyOrders.map(o => `
                    <form class="handover-confirm-form" data-order-id="${o.id}" style="display:flex;gap:10px;flex-wrap:wrap;align-items:center;padding:10px;background:#fff;border-radius:8px;border:1px solid #dcfce7;margin-bottom:8px;">
                        <div style="flex:1;min-width:220px;">
                            <strong>Đơn #${o.id} · ${esc(o.customerName)}</strong> (${esc(o.customerPhone)})<br>
                            <small>${esc(o.modelName)} · VIN: ${esc(o.vin)} · Hóa đơn: <strong>${esc(o.invoiceNumber)}</strong></small>
                        </div>
                        <input type="text" name="deliveryAddress" placeholder="Địa chỉ bàn giao xe..." value="Showroom VinFast Cần Thơ" required style="padding:7px 10px;border:1px solid #cbd5e1;border-radius:6px;min-width:220px;">
                        <input type="text" name="note" placeholder="Ghi chú biên bản bàn giao..." style="padding:7px 10px;border:1px solid #cbd5e1;border-radius:6px;min-width:200px;">
                        <button type="submit" class="button button-primary" style="padding:8px 14px;">✓ Xác nhận Xuất kho &amp; Bàn giao xe →</button>
                    </form>
                `).join("")}
            </section>
            ` : ""}

            <div style="padding:16px;background:#fff;border-radius:12px;border:1px solid #e2e8f0;overflow-x:auto;">
                <table style="width:100%;border-collapse:collapse;font-size:13px;">
                    <thead>
                        <tr style="border-bottom:2px solid #e2e8f0;text-align:left;">
                            <th style="padding:10px;">MÃ BÀN GIAO</th>
                            <th style="padding:10px;">ĐƠN HÀNG &amp; HÓA ĐƠN</th>
                            <th style="padding:10px;">KHÁCH HÀNG</th>
                            <th style="padding:10px;">XE &amp; VIN</th>
                            <th style="padding:10px;">ĐỊA CHỈ &amp; NGÀY GIAO</th>
                            <th style="padding:10px;">TRẠNG THÁI</th>
                            <th style="padding:10px;">GHI CHÚ</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${deliveries.length ? deliveries.map(d => `
                            <tr style="border-bottom:1px solid #f1f5f9;">
                                <td style="padding:10px;"><strong>#${d.deliveryID}</strong></td>
                                <td style="padding:10px;">Đơn #${d.orderID}<br><small>HĐơn: ${esc(d.invoiceNumber)}</small></td>
                                <td style="padding:10px;"><strong>${esc(d.customerName)}</strong><br><small>${esc(d.customerPhone)}</small></td>
                                <td style="padding:10px;">${esc(d.modelName)}<br><small>VIN: ${esc(d.vin)}</small></td>
                                <td style="padding:10px;">${esc(d.deliveryAddress)}<br><small>${esc(d.deliveryDate)}</small></td>
                                <td style="padding:10px;"><span class="status-pill">${esc(d.deliveryStatus)}</span></td>
                                <td style="padding:10px;">${esc(d.note)}</td>
                            </tr>
                        `).join("") : `<tr><td colspan="7" style="padding:20px;text-align:center;">Chưa có biên bản bàn giao xe nào.</td></tr>`}
                    </tbody>
                </table>
            </div>
        `;
        content.querySelectorAll(".handover-confirm-form").forEach(form => {
            form.addEventListener("submit", async (e) => {
                e.preventDefault();
                try {
                    const res = await postForm("/delivery", {
                        action: "handover",
                        csrfToken: delData.csrfToken,
                        orderId: form.dataset.orderId,
                        deliveryAddress: form.deliveryAddress.value,
                        note: form.note.value
                    });
                    notify(res.message);
                    await loadHandoverAndInventoryView("handover");
                } catch (err) {
                    notify(err.message);
                }
            });
        });
    }

    async function loadManagerReportView(reportType) {
        content.innerHTML = `<p>Đang tổng hợp báo cáo từ cơ sở dữ liệu (/api/reports)...</p>`;
        const today = new Date().toISOString().slice(0, 10);
        const fromDefault = "2026-01-01";
        const report = await fetchJson(`/api/reports?from=${fromDefault}&to=${today}`);
        const sum = report.summary || {};
        const sales = report.sales || [];
        const inv = report.inventoryByStatus || [];

        content.innerHTML = `
            <div class="page-heading">
                <div>
                    <div class="eyebrow"><span class="eyebrow-line"></span>UC-18 · BÁO CÁO QUẢN LÝ (MANAGER)</div>
                    <h1>Báo cáo Doanh thu, Bán hàng &amp; Tồn kho</h1>
                    <p>Kỳ báo cáo: ${fromDefault} đến ${today}. Số liệu được tổng hợp trực tiếp từ Orders, Payments, Contracts, Invoices và Cars.</p>
                </div>
                <div style="display:flex;gap:8px;">
                    <a class="button button-secondary" href="${appContext}/api/reports?from=${fromDefault}&to=${today}&format=csv">Xuất CSV ↗</a>
                    <a class="button button-primary" href="${appContext}/api/reports?from=${fromDefault}&to=${today}&format=xlsx">Xuất Excel (.xlsx) ↗</a>
                </div>
            </div>

            <section class="metric-grid" style="margin-bottom:20px;">
                <div class="metric-card">
                    <div class="metric-head"><span>Tổng doanh thu ghi nhận</span><span>₫</span></div>
                    <strong>${fmtMoney(sum.totalRevenue)}</strong>
                    <div class="metric-foot">Tổng giá trị giao dịch: ${fmtMoney(sum.totalSalesAmount)}</div>
                </div>
                <div class="metric-card">
                    <div class="metric-head"><span>Thực thu (COMPLETED)</span><span>✓</span></div>
                    <strong style="color:#15803d;">${fmtMoney(sum.totalPaidAmount)}</strong>
                    <div class="metric-foot">Số giao dịch thanh toán: ${sum.completedPayments || 0}</div>
                </div>
                <div class="metric-card">
                    <div class="metric-head"><span>Công nợ còn phải thu</span><span>◷</span></div>
                    <strong style="color:#b91c1c;">${fmtMoney(sum.totalOutstandingAmount)}</strong>
                    <div class="metric-foot">Hợp đồng trả góp: ${sum.totalInstallmentContracts || 0}</div>
                </div>
                <div class="metric-card">
                    <div class="metric-head"><span>Tổng xe bán ra</span><span>▤</span></div>
                    <strong>${sum.totalVehiclesSold || 0} xe</strong>
                    <div class="metric-foot">Hợp đồng hợp lệ: ${sum.totalContracts || 0}</div>
                </div>
            </section>

            <h3 style="margin:14px 0 8px;">Chi tiết Giao dịch Bán hàng (${sales.length})</h3>
            <div style="padding:16px;background:#fff;border-radius:12px;border:1px solid #e2e8f0;overflow-x:auto;margin-bottom:20px;">
                <table style="width:100%;border-collapse:collapse;font-size:13px;">
                    <thead>
                        <tr style="border-bottom:2px solid #e2e8f0;text-align:left;">
                            <th style="padding:10px;">ĐƠN HÀNG</th>
                            <th style="padding:10px;">KHÁCH HÀNG</th>
                            <th style="padding:10px;">NHÂN VIÊN</th>
                            <th style="padding:10px;">TỔNG TIỀN</th>
                            <th style="padding:10px;">ĐÃ THU</th>
                            <th style="padding:10px;">CÒN NỢ</th>
                            <th style="padding:10px;">TRẠNG THÁI</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${sales.length ? sales.map(s => `
                            <tr style="border-bottom:1px solid #f1f5f9;">
                                <td style="padding:10px;"><strong>#${s.orderId}</strong><br><small>${esc(s.orderDate)}</small></td>
                                <td style="padding:10px;">${esc(s.customerName)}</td>
                                <td style="padding:10px;">${esc(s.employeeName || "—")}</td>
                                <td style="padding:10px;">${fmtMoney(s.totalAmount)}</td>
                                <td style="padding:10px;color:#15803d;font-weight:700;">${fmtMoney(s.paidAmount)}</td>
                                <td style="padding:10px;color:#b91c1c;font-weight:700;">${fmtMoney(s.remainingAmount)}</td>
                                <td style="padding:10px;">
                                    <span class="status-pill">${esc(s.salesStatus)}</span>
                                    ${s.contractNumber ? `<br><small>HĐ: ${esc(s.contractNumber)}</small>` : ""}
                                    ${s.invoiceNumber ? `<br><small>HĐơn: ${esc(s.invoiceNumber)}</small>` : ""}
                                </td>
                            </tr>
                        `).join("") : `<tr><td colspan="7" style="padding:20px;text-align:center;">Chưa có dữ liệu giao dịch trong kỳ.</td></tr>`}
                    </tbody>
                </table>
            </div>

            <h3 style="margin:14px 0 8px;">Phân bổ Tồn kho theo Trạng thái xe (Bảng Cars)</h3>
            <div style="padding:16px;background:#fff;border-radius:12px;border:1px solid #e2e8f0;overflow-x:auto;">
                <table style="width:100%;border-collapse:collapse;font-size:13px;">
                    <thead>
                        <tr style="border-bottom:2px solid #e2e8f0;text-align:left;">
                            <th style="padding:10px;">TRẠNG THÁI XE</th>
                            <th style="padding:10px;">SỐ LƯỢNG XE</th>
                            <th style="padding:10px;">TỔNG GIÁ TRỊ NIÊM YẾT</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${inv.map(row => `
                            <tr style="border-bottom:1px solid #f1f5f9;">
                                <td style="padding:10px;"><strong>${esc(row.status)}</strong></td>
                                <td style="padding:10px;">${row.total} xe</td>
                                <td style="padding:10px;">${fmtMoney(row.amount)}</td>
                            </tr>
                        `).join("")}
                    </tbody>
                </table>
            </div>
        `;
    }

    async function renderSection(view) {
        try {
            switch (view) {
                case "consultations":
                    await loadConsultationsView();
                    break;
                case "customers":
                    await loadCustomersView();
                    break;
                case "transactions":
                    await loadTransactionsAndDepositsView(false);
                    break;
                case "deposits":
                    await loadTransactionsAndDepositsView(true);
                    break;
                case "payments":
                    await loadAccountantPaymentsAndDebtsView("payments");
                    break;
                case "debts":
                    await loadAccountantPaymentsAndDebtsView("debts");
                    break;
                case "installments":
                    await loadInstallmentsPartnerView();
                    break;
                case "contracts":
                    await loadContractsAndInvoicesView();
                    break;
                case "handover":
                    await loadHandoverAndInventoryView("handover");
                    break;
                case "vehicle-movements":
                    await loadHandoverAndInventoryView("vehicle-movements");
                    break;
                case "vehicle-status":
                    await loadHandoverAndInventoryView("vehicle-status");
                    break;
                case "revenue-report":
                case "vehicle-report":
                case "transaction-report":
                case "inventory-report":
                    await loadManagerReportView(view);
                    break;
                default:
                    content.innerHTML = renderDashboard();
            }
        } catch (err) {
            content.innerHTML = `<div class="inline-note" style="border-color:#fca5a5;background:#fef2f2;color:#991b1b;">
                Không thể tải dữ liệu mục này: ${esc(err.message)}
            </div>`;
        }
    }

    function render() {
        renderNav();
        const currentLabel = currentView === "dashboard" ? roleDashboards[currentRole][0] :
            (allItems.find(item => item[0] === currentView)?.[2] || "Tổng quan");
        const breadcrumb = $("#breadcrumb-title");
        if (breadcrumb) breadcrumb.textContent = currentLabel;
        if (inventoryPage || !content) return;
        if (currentView === "dashboard") {
            content.innerHTML = renderDashboard();
            content.querySelectorAll("[data-view]").forEach(link => {
                link.addEventListener("click", (e) => {
                    const v = link.dataset.view;
                    if (v && v !== "inventory_real" && navIds.has(v)) {
                        e.preventDefault();
                        currentView = v;
                        window.history.replaceState(null, "", `${appContext}/staff/dashboard#${encodeURIComponent(v)}`);
                        render();
                    }
                });
            });
        } else {
            renderSection(currentView);
        }
    }

    nav?.addEventListener("click", event => {
        const link = event.target.closest("a");
        if (!link) return;
        if (link.getAttribute("href")?.includes("/staff/inventory")) return;
        const view = link.dataset.view;
        if (!view || !navIds.has(view)) return;
        if (inventoryPage) {
            window.location.href = `${appContext}/staff/dashboard#${encodeURIComponent(view)}`;
            return;
        }
        event.preventDefault();
        currentView = view;
        window.history.replaceState(null, "", `${appContext}/staff/dashboard#${encodeURIComponent(view)}`);
        render();
    });

    render();
})();
