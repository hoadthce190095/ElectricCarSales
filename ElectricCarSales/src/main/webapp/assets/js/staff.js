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

    const fmtMoney = (v) => new Intl.NumberFormat("vi-VN").format(Math.round(Number(v) || 0)) + " VNĐ";
    const esc = (s) => String(s ?? "—")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");

    function showAppConfirmModal({
        title = "Xác nhận thao tác",
        message = "Bạn có chắc chắn muốn thực hiện thao tác này?",
        details = [],
        confirmText = "Có, xác nhận",
        cancelText = "Không, quay lại",
        isDanger = false,
        onConfirm = null
    }) {
        if (typeof window.showAppConfirmModal === "function") {
            return window.showAppConfirmModal({ title, message, details, confirmText, cancelText, isDanger, onConfirm });
        }
        return new Promise((resolve) => {
            let dialog = document.getElementById("app-confirm-dialog");
            if (!dialog) {
                dialog = document.createElement("dialog");
                dialog.id = "app-confirm-dialog";
                dialog.className = "app-confirm-dialog";
                document.body.appendChild(dialog);
            }

            let detailsHtml = "";
            if (Array.isArray(details) && details.length > 0) {
                detailsHtml = `
                    <div class="app-confirm-details" style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:12px 14px;margin-top:12px;font-size:13px;display:flex;flex-direction:column;gap:8px;">
                        ${details.map(d => `
                            <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;">
                                <span style="color:#64748b;font-weight:500;">${esc(d.label)}:</span>
                                <span style="color:#0f172a;font-weight:600;text-align:right;">${esc(d.value)}</span>
                            </div>
                        `).join("")}
                    </div>
                `;
            }

            dialog.innerHTML = `
                <div style="padding:18px 24px;border-bottom:1px solid #e2e8f0;background:#f8fafc;display:flex;align-items:center;justify-content:space-between;">
                    <h3 style="margin:0;font-size:16px;font-weight:700;color:#0f172a;">${esc(title)}</h3>
                    <button type="button" class="app-confirm-close" aria-label="Đóng" style="background:transparent;border:none;font-size:22px;line-height:1;color:#64748b;cursor:pointer;padding:2px 6px;border-radius:6px;">&times;</button>
                </div>
                <div style="padding:20px 24px;">
                    <p style="margin:0;font-size:14px;line-height:1.5;color:#334155;">${message}</p>
                    ${detailsHtml}
                </div>
                <div style="display:flex;justify-content:flex-end;gap:10px;padding:16px 24px;border-top:1px solid #e2e8f0;background:#f8fafc;">
                    <button type="button" class="app-confirm-btn-cancel button button-secondary" style="padding:8px 18px;font-size:13px;font-weight:600;border-radius:7px;border:1px solid #cbd5e1;background:#fff;color:#334155;cursor:pointer;">${cancelText}</button>
                    <button type="button" class="app-confirm-btn-confirm button button-primary" style="padding:8px 20px;font-size:13px;font-weight:600;border-radius:7px;border:none;background:${isDanger ? '#dc2626' : '#1e3a8a'};color:#fff;cursor:pointer;">${confirmText}</button>
                </div>
            `;

            const btnCancel = dialog.querySelector(".app-confirm-btn-cancel");
            const btnConfirm = dialog.querySelector(".app-confirm-btn-confirm");
            const btnClose = dialog.querySelector(".app-confirm-close");

            const closeDialog = (val) => {
                try { dialog.close(); } catch (_) { dialog.style.display = "none"; }
                resolve(val);
            };

            btnCancel.onclick = () => closeDialog(false);
            btnClose.onclick = () => closeDialog(false);
            dialog.oncancel = (e) => {
                e.preventDefault();
                closeDialog(false);
            };

            btnConfirm.onclick = async () => {
                btnConfirm.disabled = true;
                btnCancel.disabled = true;
                btnClose.disabled = true;
                const originalText = btnConfirm.textContent;
                btnConfirm.textContent = "Đang xử lý...";

                if (typeof onConfirm === "function") {
                    try {
                        await onConfirm();
                        closeDialog(true);
                    } catch (err) {
                        btnConfirm.disabled = false;
                        btnCancel.disabled = false;
                        btnClose.disabled = false;
                        btnConfirm.textContent = originalText;
                        closeDialog(false);
                    }
                } else {
                    closeDialog(true);
                }
            };

            if (typeof dialog.showModal === "function") {
                dialog.showModal();
            } else {
                dialog.style.display = "block";
            }
        });
    }

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
                const confirmed = await showAppConfirmModal({
                    title: "Xác nhận cập nhật yêu cầu tư vấn",
                    message: "Bạn có chắc chắn muốn lưu cập nhật trạng thái và ghi chú cho yêu cầu tư vấn này?",
                    details: [
                        { label: "Mã yêu cầu", value: `#${form.dataset.reqId}` },
                        { label: "Trạng thái mới", value: form.status.value },
                        { label: "Ghi chú/Phản hồi", value: form.note.value || "—" }
                    ],
                    confirmText: "Có, xác nhận",
                    cancelText: "Không, quay lại"
                });
                if (!confirmed) return;
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

        function renderRows(list) {
            if (!list.length) {
                return `<tr><td colspan="6" style="padding:24px;text-align:center;color:#64748b;">Không tìm thấy khách hàng nào phù hợp.</td></tr>`;
            }
            return list.map(c => `
                <tr style="border-bottom:1px solid #f1f5f9;">
                    <td style="padding:10px;"><strong>#${c.customerID}</strong></td>
                    <td style="padding:10px;"><strong>${esc(c.customerName)}</strong></td>
                    <td style="padding:10px;">${esc(c.phone)}</td>
                    <td style="padding:10px;">${esc(c.email || "—")}</td>
                    <td style="padding:10px;">${esc(c.address || "—")}</td>
                    <td style="padding:10px;">
                        <button type="button" class="button button-secondary btn-view-cust" data-id="${c.customerID}" style="padding:6px 12px;font-size:12px;">Xem chi tiết</button>
                    </td>
                </tr>
            `).join("");
        }

        content.innerHTML = `
            <div class="page-heading"><div>
                <div class="eyebrow"><span class="eyebrow-line"></span>UC-15 · QUẢN LÝ KHÁCH HÀNG (CHẾ ĐỘ XEM)</div>
                <h1>Danh sách khách hàng (${customers.length})</h1>
                <p>Xem và tra cứu thông tin khách hàng phục vụ tư vấn và lập đơn hàng (Nhân viên Sales chỉ có quyền xem).</p>
            </div></div>
            <div style="margin-bottom:14px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px;">
                <input id="cust-search" type="search" placeholder="🔍 Tìm kiếm khách hàng theo tên, SĐT, email, địa chỉ..." style="padding:8px 14px;border:1px solid #cbd5e1;border-radius:8px;font-size:13px;width:360px;max-width:100%;">
                <span id="cust-count-pill" style="font-size:12px;color:#64748b;font-weight:600;">Hiển thị ${customers.length} khách hàng</span>
            </div>
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
                    <tbody id="cust-tbody">
                        ${renderRows(customers)}
                    </tbody>
                </table>
            </div>
            <dialog id="cust-detail-dialog" style="border:none;border-radius:12px;padding:0;width:min(480px, calc(100% - 32px));box-shadow:0 25px 60px rgba(15,23,42,0.3);overflow:hidden;">
                <div style="padding:18px 24px;border-bottom:1px solid #e2e8f0;background:#f8fafc;display:flex;align-items:center;justify-content:space-between;">
                    <h3 style="margin:0;font-size:16px;font-weight:700;color:#0f172a;">Chi tiết thông tin khách hàng</h3>
                    <button type="button" id="cust-detail-close" style="background:transparent;border:none;font-size:22px;line-height:1;color:#64748b;cursor:pointer;">&times;</button>
                </div>
                <div id="cust-detail-body" style="padding:20px 24px;font-size:14px;display:flex;flex-direction:column;gap:12px;"></div>
                <div style="display:flex;justify-content:flex-end;padding:14px 24px;border-top:1px solid #e2e8f0;background:#f8fafc;">
                    <button type="button" id="cust-detail-ok" class="button button-primary" style="padding:8px 20px;">Đóng</button>
                </div>
            </dialog>
        `;

        const searchInput = document.getElementById("cust-search");
        const tbody = document.getElementById("cust-tbody");
        const countPill = document.getElementById("cust-count-pill");
        const detailDialog = document.getElementById("cust-detail-dialog");
        const detailBody = document.getElementById("cust-detail-body");
        const detailClose = document.getElementById("cust-detail-close");
        const detailOk = document.getElementById("cust-detail-ok");

        const closeDetail = () => { if (detailDialog && typeof detailDialog.close === "function") detailDialog.close(); };
        if (detailClose) detailClose.onclick = closeDetail;
        if (detailOk) detailOk.onclick = closeDetail;

        function attachDetailListeners() {
            content.querySelectorAll(".btn-view-cust").forEach(btn => {
                btn.addEventListener("click", () => {
                    const c = customers.find(x => x.customerID === Number(btn.dataset.id));
                    if (!c || !detailBody) return;
                    detailBody.innerHTML = `
                        <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:14px;display:grid;gap:10px;">
                            <div style="display:flex;justify-content:space-between;"><span style="color:#64748b;">Mã khách hàng:</span><strong>#${c.customerID}</strong></div>
                            <div style="display:flex;justify-content:space-between;"><span style="color:#64748b;">Họ và tên:</span><strong>${esc(c.customerName)}</strong></div>
                            <div style="display:flex;justify-content:space-between;"><span style="color:#64748b;">Số điện thoại:</span><strong>${esc(c.phone)}</strong></div>
                            <div style="display:flex;justify-content:space-between;"><span style="color:#64748b;">Email:</span><strong>${esc(c.email || "—")}</strong></div>
                            <div style="display:flex;justify-content:space-between;"><span style="color:#64748b;">Địa chỉ thường trú:</span><strong>${esc(c.address || "—")}</strong></div>
                            <div style="display:flex;justify-content:space-between;border-top:1px dashed #cbd5e1;padding-top:8px;"><span style="color:#64748b;">Tài khoản UserID:</span><strong>#${c.userID || "—"}</strong></div>
                        </div>
                    `;
                    if (detailDialog && typeof detailDialog.showModal === "function") {
                        detailDialog.showModal();
                    }
                });
            });
        }

        searchInput?.addEventListener("input", () => {
            const query = (searchInput.value || "").toLowerCase().trim();
            const filtered = customers.filter(c => {
                if (!query) return true;
                return String(c.customerID).includes(query)
                    || (c.customerName || "").toLowerCase().includes(query)
                    || (c.phone || "").toLowerCase().includes(query)
                    || (c.email || "").toLowerCase().includes(query)
                    || (c.address || "").toLowerCase().includes(query);
            });
            if (tbody) tbody.innerHTML = renderRows(filtered);
            if (countPill) countPill.textContent = `Hiển thị ${filtered.length} / ${customers.length} khách hàng`;
            attachDetailListeners();
        });

        attachDetailListeners();
    }

    async function loadTransactionsAndDepositsView(onlyDeposits = false) {
        content.innerHTML = `<p>Đang tải dữ liệu đơn hàng, xe có sẵn và phương thức thanh toán...</p>`;
        const data = await fetchJson("/orders?format=json");
        const orders = data.orders || [];
        const customers = data.customers || [];
        const cars = data.cars || [];
        const inventory = data.inventorySummary?.[0] || {};
        let rawMethods = (data.paymentMethods || []).filter(m => {
            const name = (m.methodName || m.MethodName || "").toUpperCase();
            return name !== "CASH";
        });
        if (!Array.isArray(rawMethods) || rawMethods.length === 0) {
            rawMethods = [
                { paymentMethodID: 2, methodName: "BANK_TRANSFER", description: "Thanh toán bằng ngân hàng (Chuyển khoản VietQR SePay - MB Bank)" },
                { paymentMethodID: 3, methodName: "INSTALLMENT", description: "Thanh toán qua nhà tài chính (Trả góp đối tác giải ngân 70%)" }
            ];
        }
        const methods = rawMethods.map(m => ({
            id: m.paymentMethodID ?? m.paymentMethodId ?? m.PaymentMethodID ?? 0,
            name: (m.methodName || m.MethodName || "").toUpperCase(),
            description: m.description || m.Description || m.methodName || m.MethodName || ""
        }));

        let rawCompanies = data.financialCompanies || [];
        const companies = rawCompanies.map(fc => ({
            id: fc.companyID ?? fc.companyId ?? fc.CompanyID ?? 0,
            name: fc.companyName || fc.CompanyName || ""
        }));
        const canCreateOrder = ["sales", "admin"].includes(currentRole);

        content.innerHTML = `
            <div class="page-heading"><div>
                <div class="eyebrow"><span class="eyebrow-line"></span>UC-08 &amp; UC-10 · GIAO DỊCH BÁN XE &amp; ĐẶT CỌC 30%</div>
                <h1>${onlyDeposits ? "Theo dõi Đặt cọc 30% (Deposits)" : "Quản lý Đơn hàng & Lập giao dịch bán xe"}</h1>
                <p>Hệ thống tự áp dụng khuyến mãi đang hiệu lực (nếu có), sau đó tính 30% tiền đặt cọc và số tiền còn lại từ giá xe trong kho.</p>
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
                                ${cars.map(car => `<option value="${car.carID}" data-price="${car.price}" data-original-price="${car.originalPrice ?? car.price}" data-discount="${car.discountAmount || 0}" data-promotion="${esc(car.promotionName || "")}" ${prefillOrderCustomer?.modelId === car.modelID ? "selected" : ""}>${esc(car.modelName)} (${esc(car.versionName)} - ${esc(car.colorName)}) · VIN: ${esc(car.vin)} · ${car.promotionName ? `${fmtMoney(car.price)} · ƯU ĐÃI ${esc(car.promotionName)} (giá gốc ${fmtMoney(car.originalPrice)})` : fmtMoney(car.price)}</option>`).join("")}
                            </select>
                        </label>
                        <label style="font-size:12px;font-weight:600;">Phương thức thanh toán (Đặt cọc 30%) *
                            <select name="paymentMethodId" id="so-method" required style="width:100%;padding:8px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                                ${methods.map(m => `<option value="${m.id}" data-name="${m.name}" ${m.name === "BANK_TRANSFER" ? "selected" : ""}>${m.name === "BANK_TRANSFER" ? "Thanh toán bằng ngân hàng (Chuyển khoản VietQR SePay - MB Bank)" : "Thanh toán qua nhà tài chính (Trả góp đối tác giải ngân 70%)"}</option>`).join("")}
                            </select>
                        </label>
                        <label id="so-company-wrap" style="font-size:12px;font-weight:600;display:none;">Chọn nhà tài chính cho trả góp *
                            <select name="companyId" id="so-company" style="width:100%;padding:8px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                                <option value="">-- Chọn nhà tài chính từ database --</option>
                                ${companies.map(fc => `<option value="${fc.id}">${esc(fc.name)}</option>`).join("")}
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
                                <input type="tel" name="customerPhone" id="so-cust-phone" required pattern="0[0-9]{9}" maxlength="10" placeholder="Ví dụ: 0912345678" title="Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng số 0 (ví dụ: 0912345678)" style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                            </label>
                            <label style="font-size:12px;font-weight:600;">Email nhận chứng từ &amp; thông báo
                                <input type="email" name="customerEmail" id="so-cust-email" maxlength="100" pattern="^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$" placeholder="Ví dụ: customer@example.com" title="Email phải có định dạng hợp lệ (ví dụ: customer@example.com)" style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                            </label>
                            <label style="font-size:12px;font-weight:600;">Địa chỉ bàn giao xe / thường trú *
                                <input type="text" name="customerAddress" id="so-cust-address" required maxlength="255" placeholder="Địa chỉ thường trú / nhận xe" style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                            </label>
                        </div>
                        <div style="margin-top:12px;background:#f1f5f9;border:1px solid #cbd5e1;border-radius:8px;padding:12px 14px;">
                            <div style="font-size:12px;font-weight:700;color:#1e3a8a;margin-bottom:8px;text-transform:uppercase;letter-spacing:0.5px;">
                                Thông tin nhân viên đang tư vấn
                            </div>
                            <div id="so-consulting-staff-box" style="font-size:13px;">
                                <div style="color:#64748b;font-style:italic;">Đang tải thông tin nhân viên tư vấn...</div>
                            </div>
                        </div>
                        <div style="margin-top:10px;">
                            <label style="font-size:12px;font-weight:600;">Ghi chú đơn hàng (nội bộ)
                            <input type="text" name="note" id="so-order-note" maxlength="200" placeholder="Ghi chú thêm; thông tin khuyến mãi sẽ tự động được ghi vào đơn..." style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
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
                                    ${(o.paymentMethodName === "INSTALLMENT" || o.companyName) && (Number(o.remainingAmount) === 0 || Number(o.paidAmount) >= Number(o.depositAmount)) ? `
                                        Đã thu: <span style="color:#15803d;font-weight:700;">${fmtMoney(o.originalAmount)}</span><br>
                                        Còn lại: <span style="color:#15803d;font-weight:700;">0 ₫</span><br>
                                        <small style="color:#15803d;font-weight:600;">✓ NH ${esc(o.companyName || "đối tác")} giải ngân 70%</small>
                                    ` : `
                                        Đã thu: <span style="color:#15803d;font-weight:700;">${fmtMoney(o.paidAmount)}</span><br>
                                        Còn lại: <span style="color:#b91c1c;font-weight:700;">${fmtMoney(o.remainingAmount)}</span>
                                    `}
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
                                        ${(o.status === "PENDING" || o.status === "RESERVED") && ["sales", "manager", "admin"].includes(currentRole) ? `
                                            ${Number(o.paidAmount || 0) <= 0 ? `
                                                <button type="button" class="button button-secondary btn-cancel-order" data-order-id="${o.id}" data-model="${esc(o.modelName || '')}" data-customer="${esc(o.customerName || '')}" data-vin="${esc(o.vin || '')}" style="padding:5px 10px;font-size:12px;color:#dc2626;border:1px solid #fca5a5;background:#fff5f5;font-weight:600;">✕ Hủy đơn hàng</button>
                                            ` : ""}
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

        function renderConsultingStaff(staff) {
            const box = document.getElementById("so-consulting-staff-box");
            if (!box) return;
            if (staff && (staff.fullName || staff.employeeName || staff.username)) {
                box.innerHTML = `
                    <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(180px, 1fr));gap:8px;">
                        <div><span style="color:#64748b;">Họ và tên:</span> <strong>${esc(staff.fullName || staff.employeeName || "—")}</strong></div>
                        <div><span style="color:#64748b;">Username:</span> <strong>${esc(staff.username || "—")}</strong></div>
                        <div><span style="color:#64748b;">Mã nhân viên:</span> <strong>${esc(staff.employeeCode || ("EMP" + (staff.employeeId || staff.employeeID || "")))}</strong></div>
                        <div><span style="color:#64748b;">Email:</span> <strong>${esc(staff.email || "—")}</strong></div>
                        <div><span style="color:#64748b;">Số điện thoại:</span> <strong>${esc(staff.phone || "—")}</strong></div>
                    </div>
                `;
            } else {
                box.innerHTML = `<div style="color:#94a3b8;font-style:italic;">Chưa có nhân viên phụ trách tư vấn</div>`;
            }
        }

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

            // 2. Gọi API backend lấy dữ liệu mới nhất từ database và thông tin nhân viên tư vấn
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
                if (res && res.consultingStaff) {
                    renderConsultingStaff(res.consultingStaff);
                } else {
                    renderConsultingStaff(data.currentEmployee);
                }
            } catch (err) {
                console.warn("Could not fetch realtime customer profile", err);
                renderConsultingStaff(data.currentEmployee);
            }
        }

        customerSelect?.addEventListener("change", () => autofillCustomerInfo(false));
        btnAutofill?.addEventListener("click", () => autofillCustomerInfo(true));
        if (customerSelect?.value) autofillCustomerInfo(false);

        function updateOrderPreview() {
            if (!carSelect || !preview) return;
            const selectedCar = carSelect.selectedOptions[0];
            const price = Number(selectedCar?.dataset?.price || 0);
            const originalPrice = Number(selectedCar?.dataset?.originalPrice || price);
            const discount = Number(selectedCar?.dataset?.discount || 0);
            const promotionName = selectedCar?.dataset?.promotion || "";
            const mName = methodSelect?.selectedOptions[0]?.dataset?.name || "";
            if (companyWrap) {
                const isInstallment = (mName === "INSTALLMENT");
                companyWrap.style.display = isInstallment ? "block" : "none";
                const companySelect = document.getElementById("so-company");
                if (companySelect) {
                    companySelect.required = isInstallment;
                    if (!isInstallment) companySelect.value = "";
                }
            }
            if (price > 0) {
                const dep = Math.round(price * 0.30);
                const rem = price - dep;
                preview.innerHTML = `${promotionName ? `<span style="color:#b91c1c;font-weight:700;">🎁 Khuyến mãi ${esc(promotionName)}: giá gốc <del>${fmtMoney(originalPrice)}</del>, giảm ${fmtMoney(discount)}.</span><br>` : ""}Tổng giá trị xe: <strong>${fmtMoney(price)}</strong> · Tiền đặt cọc 30%: <strong>${fmtMoney(dep)}</strong> · Số tiền 70% còn lại: <strong>${fmtMoney(rem)}</strong> ${mName === "INSTALLMENT" ? "<span style='color:#15803d;font-weight:600;'>(Khách hàng thanh toán 30% cọc qua ngân hàng; 70% còn lại do nhà tài chính giải ngân cho showroom sau khi cọc)</span>" : "(Khách hàng thanh toán 30% cọc và 70% còn lại bằng chuyển khoản ngân hàng)"}.`;
            }
        }
        carSelect?.addEventListener("change", updateOrderPreview);
        methodSelect?.addEventListener("change", updateOrderPreview);
        updateOrderPreview();
        renderConsultingStaff(data.currentEmployee);

        document.getElementById("staff-create-order-form")?.addEventListener("submit", async (e) => {
            e.preventDefault();
            const f = e.currentTarget;

            const phoneVal = (f.customerPhone?.value || "").trim();
            if (!/^0[0-9]{9}$/.test(phoneVal)) {
                notify("Số điện thoại người mua không đúng định dạng. Phải bắt đầu bằng số 0 và có đúng 10 chữ số (ví dụ: 0912345678).");
                f.customerPhone?.focus();
                return;
            }
            const emailVal = (f.customerEmail?.value || "").trim();
            if (emailVal && !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(emailVal)) {
                notify("Email người mua không đúng định dạng (ví dụ: customer@example.com).");
                f.customerEmail?.focus();
                return;
            }

            const chosenMethod = f.paymentMethodId.selectedOptions[0]?.dataset?.name || "";
            const companyIdVal = (chosenMethod === "INSTALLMENT") ? (f.companyId?.value || "") : "";
            if (chosenMethod === "INSTALLMENT" && !companyIdVal) {
                notify("Đơn hàng thanh toán qua nhà tài chính (trả góp) bắt buộc phải chọn nhà tài chính trong danh sách.");
                f.companyId?.focus();
                return;
            }

            const selectedCar = carSelect?.selectedOptions[0];
            const price = Number(selectedCar?.dataset?.price || 0);
            const originalPrice = Number(selectedCar?.dataset?.originalPrice || price);
            const discount = Number(selectedCar?.dataset?.discount || 0);
            const promotionName = selectedCar?.dataset?.promotion || "";
            const dep = Math.round(price * 0.30);
            const rem = price - dep;
            const modalDetails = [
                { label: "Khách hàng", value: f.customerName?.value || customerSelect?.selectedOptions[0]?.text },
                { label: "Xe vật lý", value: carSelect?.selectedOptions[0]?.text },
                { label: "Phương thức thanh toán", value: f.paymentMethodId.selectedOptions[0]?.text },
                ...(promotionName ? [
                    { label: "Khuyến mãi", value: `${promotionName} · Giá gốc ${fmtMoney(originalPrice)} · Giảm ${fmtMoney(discount)}` }
                ] : []),
                { label: "Tổng giá xe", value: fmtMoney(price) },
                { label: "Tiền đặt cọc 30%", value: fmtMoney(dep) },
                { label: "Số tiền 70% còn lại", value: fmtMoney(rem) }
            ];
            if (chosenMethod === "INSTALLMENT") {
                modalDetails.push({ label: "Quy tắc giải ngân", value: "Khách chỉ cọc 30%. 70% còn lại mặc định do đối tác tài chính giải ngân." });
            }

            const confirmed = await showAppConfirmModal({
                title: "Xác nhận tạo đơn hàng bán xe",
                message: "Bạn có chắc chắn muốn lập đơn hàng bán xe mới cho khách hàng này? Sau khi tạo, xe sẽ được giữ chỗ (RESERVED) và khách hàng cần đặt cọc 30%.",
                details: modalDetails,
                confirmText: "Có, xác nhận",
                cancelText: "Không, quay lại"
            });
            if (!confirmed) return;
            try {
                const res = await postForm("/orders", {
                    action: "create",
                    customerId: f.customerId.value,
                    carId: f.carId.value,
                    method: chosenMethod,
                    companyId: companyIdVal,
                    customerName: f.customerName?.value || "",
                    customerPhone: f.customerPhone?.value || "",
                    customerEmail: f.customerEmail?.value || "",
                    customerAddress: f.customerAddress?.value || "",
                    salesNote: f.note.value,
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
                const confirmed = await showAppConfirmModal({
                    title: "Xác nhận lập hợp đồng mua bán",
                    message: "Bạn có chắc chắn muốn lập hợp đồng mua bán xe điện cho đơn hàng này theo quy chuẩn RDS?",
                    details: [
                        { label: "Mã đơn hàng", value: `#${btn.dataset.orderId}` },
                        { label: "Quy chuẩn", value: "Hợp đồng mua bán xe điện VinFast (RDS 9.1)" }
                    ],
                    confirmText: "Có, xác nhận",
                    cancelText: "Không, quay lại"
                });
                if (!confirmed) return;
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
                const confirmed = await showAppConfirmModal({
                    title: "Xác nhận bàn giao xe",
                    message: "Bạn có chắc chắn muốn xác nhận xuất kho và hoàn tất bàn giao xe cho đơn hàng này?",
                    details: [
                        { label: "Mã đơn hàng", value: `#${btn.dataset.orderId}` },
                        { label: "Địa điểm bàn giao", value: "Showroom VinFast Cần Thơ" }
                    ],
                    confirmText: "Có, xác nhận",
                    cancelText: "Không, quay lại"
                });
                if (!confirmed) return;
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

        content.querySelectorAll(".btn-cancel-order").forEach(btn => {
            btn.addEventListener("click", async () => {
                const orderId = btn.dataset.orderId;
                const model = btn.dataset.model || "Xe điện";
                const customer = btn.dataset.customer || "Khách hàng";
                const vin = btn.dataset.vin || "";

                const confirmed = await showAppConfirmModal({
                    title: "Xác nhận hủy đơn hàng",
                    message: `Bạn có chắc chắn muốn hủy đơn hàng <strong>#${esc(orderId)}</strong> của khách hàng <strong>${esc(customer)}</strong>?<br><br><span style="color:#b91c1c;font-size:13px;line-height:1.5;display:block;">⚠️ Sau khi hủy, xe vật lý (<strong>${esc(model)}</strong>${vin ? ' · VIN: ' + esc(vin) : ''}) sẽ được tự động giải phóng về trạng thái có sẵn (AVAILABLE) để bán cho khách hàng khác. Thao tác này không thể hoàn tác.</span>`,
                    details: [
                        { label: "Mã đơn hàng", value: `#${orderId}` },
                        { label: "Khách hàng", value: customer },
                        { label: "Dòng xe / Mẫu xe", value: model },
                        ...(vin ? [{ label: "Số khung VIN", value: vin }] : [])
                    ],
                    confirmText: "Có, hủy đơn hàng",
                    cancelText: "Không, quay lại",
                    isDanger: true
                });

                if (!confirmed) return;

                try {
                    const res = await postForm("/order-status", {
                        orderId: orderId,
                        status: "CANCELLED",
                        note: "Sales Staff hủy đơn trước khi thanh toán cọc"
                    });
                    notify(res.message || `Đã hủy thành công đơn hàng #${orderId}.`);
                    await loadTransactionsAndDepositsView(onlyDeposits);
                } catch (err) {
                    notify(err.message || "Không thể hủy đơn hàng.");
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
                        <h3 style="margin:0 0 8px;">Khoản #${p.paymentId} · Đơn #${p.orderId} (Mã đơn: <strong>${esc(p.orderCode || ('ECS' + p.orderId))}</strong>)</h3>
                        <p>Khách hàng: <strong>${esc(p.customer)}</strong> · Loại: <strong>${esc(p.formattedType || (p.type === 'DEPOSIT' ? 'DEPOSIT_30' : 'REMAINING_70'))}</strong> · Phương thức: <strong>${esc(p.methodName)}</strong></p>
                        <p>Số tiền yêu cầu: <strong style="color:#1e40af;font-size:15px;">${fmtMoney(p.amount)}</strong></p>
                        <p>Mã tham chiếu / Đối chiếu: <code>${esc(p.reference)}</code></p>
                        ${p.note ? `<p style="font-size:12px;color:#64748b;">Ghi chú: ${esc(p.note)}</p>` : ""}
                        <form class="accountant-review-form" style="display:flex;flex-wrap:wrap;gap:10px;align-items:end;margin-top:12px;" data-action-label="Đã xác nhận khoản #${p.paymentId}.">
                            <input type="hidden" name="csrfToken" value="${csrf}"><input type="hidden" name="format" value="json">
                            <input type="hidden" name="paymentId" value="${p.paymentId}"><input type="hidden" name="action" value="confirm">
                            <label style="display:grid;gap:4px;font-size:13px;">Số tiền thực nhận (VND)
                                <input name="receivedAmount" type="number" min="1" step="1" value="${Math.round(Number(p.amount) || 0)}" required style="font-weight:600;">
                            </label>
                            <label style="display:grid;gap:4px;font-size:13px;">Mã sao kê hoặc chứng từ
                                <input name="bankTransaction" maxlength="80" pattern="[A-Za-z0-9./_-]{4,80}" value="${esc(p.reference || '')}" required style="font-weight:600;">
                            </label>
                            <label style="display:flex;gap:6px;align-items:center;font-size:13px;">
                                <input name="checkedStatement" type="checkbox" checked required>Đã đối chiếu chứng từ thực nhận
                            </label>
                            <button type="submit" class="button button-primary">Xác nhận đã nhận tiền</button>
                        </form>
                        <form class="accountant-review-form" style="display:flex;gap:8px;margin-top:10px;align-items:end;" data-action-label="Đã từ chối khoản #${p.paymentId}.">
                            <input type="hidden" name="csrfToken" value="${csrf}"><input type="hidden" name="format" value="json">
                            <input type="hidden" name="paymentId" value="${p.paymentId}"><input type="hidden" name="action" value="reject">
                            <label style="display:grid;gap:4px;font-size:13px;">Lý do từ chối<input name="reason" maxlength="200" required></label>
                            <button type="submit" class="button button-secondary">Từ chối</button>
                        </form>
                    </article>`).join("") : `<p>Không có khoản PENDING.</p>`}
            </section>`;
        content.innerHTML = `
            <div class="page-heading"><div>
                <div class="eyebrow"><span class="eyebrow-line"></span>UC-10–12 · KẾ TOÁN ĐỐI SOÁT</div>
                <h1>${debtsFirst ? "Quản lý công nợ & giải ngân 70%" : "Xác nhận & Đối soát thanh toán"}</h1>
                <p>Chỉ xác nhận sau khi đối chiếu sao kê ngân hàng hoặc chứng từ giải ngân thật. Thao tác được lưu vào database.</p>
            </div></div>
            <div class="inline-note">Trang kế toán chạy trong dashboard. Giải ngân 70% chỉ được tạo chờ xác nhận sau khi hợp đồng đã ký; không tự kết nối/xác nhận với ngân hàng.</div>
            ${debtsFirst ? renderDebts() + renderPending() : renderPending() + renderDebts()}
            <section id="accountant-completed">
                <h2>Khoản đã xác nhận &amp; Đối soát (${(data.completedPayments || []).length})</h2>
                ${(data.completedPayments || []).map(p => `
                    <article style="padding:14px;background:#fff;border:1px solid #e2e8f0;border-radius:10px;margin:10px 0;border-left:4px solid ${p.reconciled ? '#15803d' : '#f59e0b'};">
                        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;">
                            <div>
                                <strong style="font-size:15px;">Khoản #${esc(p.paymentId)} · Đơn <code>${esc(p.orderCode || ('ECS' + p.orderId))}</code></strong>
                                <span style="margin-left:8px;font-size:14px;color:#1e40af;font-weight:700;">${fmtMoney(p.receivedAmount || p.amount)}</span>
                            </div>
                            <div style="display:flex;gap:8px;align-items:center;">
                                <span class="status-pill" style="background:#dcfce7;color:#166534;border:1px solid #86efac;font-size:11px;font-weight:700;">COMPLETED</span>
                                ${p.reconciled ? `
                                    <span class="status-pill" style="background:#dbeafe;color:#1e40af;border:1px solid #93c5fd;font-size:11px;font-weight:700;">✓ ĐÃ ĐỐI SOÁT</span>
                                ` : `
                                    <span class="status-pill" style="background:#fef3c7;color:#92400e;border:1px solid #fcd34d;font-size:11px;font-weight:700;">⚠ CHƯA ĐỐI SOÁT</span>
                                `}
                            </div>
                        </div>
                        <p style="margin:6px 0;font-size:13px;">
                            Phương thức: <strong>${esc(p.methodName)}</strong> · 
                            Loại: <strong>${esc(p.formattedType || (p.type === 'DEPOSIT' ? 'DEPOSIT_30' : 'REMAINING_70'))}</strong> · 
                            Mã tham chiếu: <code>${esc(p.reference)}</code> ·
                            Thời gian: <small>${esc(p.formattedDate || '—')}</small>
                        </p>
                        ${p.type === "DEPOSIT" ? `<p style="margin:4px 0;"><a href="${appContext}/staff/sales-document?kind=deposit&amp;orderId=${p.orderId}" class="text-link" style="font-size:12px;">📥 Tải biên nhận cọc (Word)</a></p>` : ""}
                        ${p.sePayEvidence ? `
                            <div style="background:#f0fdf4;border:1px solid #86efac;border-radius:6px;padding:8px 12px;margin-top:6px;font-size:12px;color:#166534;">
                                <strong>Bằng chứng giao dịch SePay (MB Bank · 0917231548 · Do Tan Dat):</strong><br>
                                Mã GD SePay: <strong>#${esc(p.sePayEvidence.eventId || p.sePayEvidence.transactionNo)}</strong>
                                · Cổng: <strong>${esc(p.sePayEvidence.gateway || p.sePayEvidence.bankCode || "MBBank")}</strong>
                                · Thời gian: <strong>${esc(p.sePayEvidence.payDate || p.sePayEvidence.transactionDate || "—")}</strong><br>
                                Số tiền thực nhận ngân hàng: <strong style="font-size:13px;color:#059669;">${fmtMoney(p.sePayEvidence.received)}</strong>
                            </div>
                        ` : ""}
                        <div style="margin-top:8px;padding-top:8px;border-top:1px dashed #e2e8f0;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;">
                            <div style="font-size:12px;color:#64748b;">
                                ${p.reconciled ? `
                                    <span style="color:#15803d;font-weight:600;">✓ Đã đối soát bởi Nhân viên #${esc(p.reconciledBy || 'Kế toán')}</span> vào lúc <small>${esc(p.reconciledAt || '—')}</small>
                                ` : `
                                    <span style="color:#b45309;font-weight:600;">⚠ Giao dịch đã ghi nhận thành công nhưng chưa hoàn tất đối soát sổ sách.</span>
                                `}
                            </div>
                            <div>
                                ${!p.reconciled ? `
                                    <button type="button" class="button button-primary btn-reconcile-trigger" 
                                            data-payment-id="${p.paymentId}"
                                            data-order-code="${esc(p.orderCode || ('ECS' + p.orderId))}"
                                            data-amount="${fmtMoney(p.receivedAmount || p.amount)}"
                                            data-reference="${esc(p.reference)}"
                                            data-type="${esc(p.formattedType || p.type)}"
                                            style="padding:6px 14px;font-size:12px;background:#1e40af;border-color:#1e40af;cursor:pointer;">
                                        🔍 Xác nhận đã đối soát
                                    </button>
                                ` : `
                                    <span style="font-size:12px;color:#15803d;font-weight:600;">Đã khóa đối soát</span>
                                `}
                            </div>
                        </div>
                    </article>`).join("") || "<p>Chưa có khoản đã xác nhận.</p>"}
            </section>
        `;

        function showReconcileModal({ paymentId, orderCode, amount, ref, type }) {
            let modal = document.getElementById("accountant-reconcile-modal");
            if (!modal) {
                modal = document.createElement("div");
                modal.id = "accountant-reconcile-modal";
                modal.style.position = "fixed";
                modal.style.top = "0";
                modal.style.left = "0";
                modal.style.width = "100%";
                modal.style.height = "100%";
                modal.style.backgroundColor = "rgba(15, 23, 42, 0.65)";
                modal.style.backdropFilter = "blur(4px)";
                modal.style.display = "flex";
                modal.style.alignItems = "center";
                modal.style.justifyContent = "center";
                modal.style.zIndex = "9999";
                document.body.appendChild(modal);
            }
            modal.innerHTML = `
                <div style="background:#fff;border-radius:14px;max-width:480px;width:92%;box-shadow:0 20px 25px -5px rgba(0,0,0,0.25);overflow:hidden;">
                    <div style="padding:16px 20px;background:#1e40af;color:#fff;display:flex;justify-content:space-between;align-items:center;">
                        <h3 style="margin:0;font-size:16px;font-weight:600;">🔍 Xác nhận đối soát giao dịch</h3>
                        <button type="button" id="btn-modal-close" style="background:none;border:none;color:#fff;font-size:22px;cursor:pointer;line-height:1;">&times;</button>
                    </div>
                    <div style="padding:20px;">
                        <p style="margin:0 0 14px;color:#334155;font-size:14px;line-height:1.5;">
                            Vui lòng kiểm tra kỹ thông tin đối chiếu với tài khoản ngân hàng / quỹ tiền mặt trước khi xác nhận:
                        </p>
                        <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:14px;margin-bottom:16px;display:grid;gap:8px;font-size:13px;">
                            <div style="display:flex;justify-content:space-between;"><span style="color:#64748b;">Mã thanh toán:</span><strong>#${esc(paymentId)}</strong></div>
                            <div style="display:flex;justify-content:space-between;"><span style="color:#64748b;">Mã đơn hàng:</span><strong>${esc(orderCode)}</strong></div>
                            <div style="display:flex;justify-content:space-between;"><span style="color:#64748b;">Loại thanh toán:</span><strong>${esc(type)}</strong></div>
                            <div style="display:flex;justify-content:space-between;"><span style="color:#64748b;">Mã tham chiếu:</span><code>${esc(ref)}</code></div>
                            <div style="display:flex;justify-content:space-between;border-top:1px dashed #cbd5e1;padding-top:8px;">
                                <span style="color:#64748b;font-weight:600;">Số tiền thực nhận:</span>
                                <strong style="color:#059669;font-size:15px;">${esc(amount)}</strong>
                            </div>
                        </div>
                        <div style="background:#eff6ff;border:1px solid #bfdbfe;border-radius:6px;padding:10px 12px;font-size:12px;color:#1e40af;margin-bottom:20px;">
                            ℹ️ Khi chọn <strong>"Có, xác nhận đối soát"</strong>, hệ thống sẽ kiểm tra trạng thái, ghi nhận đối soát vào database và khóa giao dịch. Không thể đối soát lại cùng giao dịch.
                        </div>
                        <div style="display:flex;justify-content:flex-end;gap:10px;">
                            <button type="button" id="btn-modal-cancel" class="button button-secondary" style="padding:8px 16px;">Không, quay lại</button>
                            <button type="button" id="btn-modal-confirm" class="button button-primary" style="background:#1e40af;border-color:#1e40af;padding:8px 16px;">Có, xác nhận</button>
                        </div>
                    </div>
                </div>
            `;
            modal.style.display = "flex";

            const closeModal = () => { modal.style.display = "none"; };
            modal.querySelector("#btn-modal-close").addEventListener("click", closeModal);
            modal.querySelector("#btn-modal-cancel").addEventListener("click", closeModal);

            const confirmBtn = modal.querySelector("#btn-modal-confirm");
            confirmBtn.addEventListener("click", async () => {
                confirmBtn.disabled = true;
                confirmBtn.textContent = "Đang xử lý...";
                try {
                    const res = await postForm("/staff/payment-review", {
                        action: "reconcile",
                        paymentId: paymentId,
                        csrfToken: csrf,
                        format: "json"
                    });
                    closeModal();
                    notify(res.message || "Đã đối soát giao dịch thành công.");
                    await loadAccountantPaymentsAndDebtsView(mode);
                } catch (err) {
                    confirmBtn.disabled = false;
                    confirmBtn.textContent = "Có, xác nhận";
                    notify(err.message || "Không thể thực hiện đối soát.");
                }
            });
        }

        content.querySelectorAll(".btn-reconcile-trigger").forEach(btn => {
            btn.addEventListener("click", () => {
                showReconcileModal({
                    paymentId: btn.dataset.paymentId,
                    orderCode: btn.dataset.orderCode,
                    amount: btn.dataset.amount,
                    ref: btn.dataset.reference,
                    type: btn.dataset.type
                });
            });
        });

        content.querySelectorAll(".accountant-review-form").forEach(form => form.addEventListener("submit", async event => {
            event.preventDefault();
            if (!form.reportValidity()) return;
            const button = form.querySelector('button[type="submit"]');
            if (!button || button.disabled) return;
            const values = Object.fromEntries(new FormData(form).entries());
            const action = values.action;
            let confirmTitle = "Xác nhận thao tác kế toán";
            let confirmMsg = "Bạn có chắc chắn muốn thực hiện thao tác kế toán này?";
            let details = [];
            let isDanger = false;
            if (action === "confirm") {
                confirmTitle = "Xác nhận đã nhận tiền";
                confirmMsg = "Bạn có chắc chắn đã kiểm tra sao kê ngân hàng và xác nhận nhận đủ khoản thanh toán này?";
                details = [
                    { label: "Mã thanh toán", value: `#${values.paymentId}` },
                    { label: "Số tiền thực nhận", value: fmtMoney(values.receivedAmount) },
                    { label: "Mã sao kê / chứng từ", value: values.bankTransaction || "—" }
                ];
            } else if (action === "reject") {
                confirmTitle = "Xác nhận từ chối khoản thanh toán";
                confirmMsg = "Bạn có chắc chắn muốn từ chối khoản thanh toán này?";
                details = [
                    { label: "Mã thanh toán", value: `#${values.paymentId}` },
                    { label: "Lý do từ chối", value: values.reason || "—" }
                ];
                isDanger = true;
            } else if (action === "partner") {
                confirmTitle = "Xác nhận giải ngân 70% từ đối tác";
                confirmMsg = "Bạn có chắc chắn muốn ghi nhận chờ giải ngân 70% còn lại cho đơn hàng này?";
                details = [
                    { label: "Mã đơn hàng", value: `#${values.orderId}` }
                ];
            }

            const confirmed = await showAppConfirmModal({
                title: confirmTitle,
                message: confirmMsg,
                details: details,
                confirmText: "Có, xác nhận",
                cancelText: "Không, quay lại",
                isDanger: isDanger
            });
            if (!confirmed) return;

            button.disabled = true;
            try {
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
                const confirmed = await showAppConfirmModal({
                    title: "Xác nhận lập hợp đồng mua bán",
                    message: "Bạn có chắc chắn muốn lập hợp đồng mua bán cho đơn hàng này?",
                    details: [
                        { label: "Mã đơn hàng", value: `#${btn.dataset.orderId}` }
                    ],
                    confirmText: "Có, xác nhận",
                    cancelText: "Không, quay lại"
                });
                if (!confirmed) return;
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
                const confirmed = await showAppConfirmModal({
                    title: "Xác nhận phát hành hóa đơn GTGT",
                    message: "Bạn có chắc chắn muốn phát hành hóa đơn GTGT chính thức cho đơn hàng này?",
                    details: [
                        { label: "Mã đơn hàng", value: `#${btn.dataset.orderId}` },
                        { label: "Nghiệp vụ", value: "Xuất hóa đơn GTGT theo quy chuẩn RDS (PAID)" }
                    ],
                    confirmText: "Có, xác nhận",
                    cancelText: "Không, quay lại"
                });
                if (!confirmed) return;
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
        const suppliers = delData.suppliers || [];
        const availableCarsList = cars.filter(c => ["AVAILABLE", "CÓ SẴN", "IN_STOCK"].includes((c.statusName || "").trim().toUpperCase()));
        const orders = ordData.orders || [];
        const readyOrders = orders.filter(o => currentRole === "operations" && o.contractStatus === "SIGNED" && o.status === "PAID" && o.invoiceNumber && o.deliveryStatus !== "DELIVERED");

        if (subMode === "vehicle-movements") {
            content.innerHTML = `
                <div class="page-heading"><div>
                    <div class="eyebrow"><span class="eyebrow-line"></span>UC-09 · NHẬP / XUẤT KHO XE VẬT LÝ (RDS 10.2 &amp; 10.3)</div>
                    <h1>Quản lý Nhập / Xuất kho xe (${invTxs.length} giao dịch)</h1>
                    <p>Nhập xe vật lý mới vào bảng Cars (AVAILABLE) và xuất kho xe kèm theo dõi lịch sử trong InventoryTransactions.</p>
                </div></div>

                <!-- FORM NHẬP KHO XE MỚI (RDS 10.2) -->
                <section style="padding:16px;background:#fff;border-radius:12px;border:1px solid #cbd5e1;margin-bottom:20px;">
                    <h3 style="margin:0 0 12px;font-size:15px;color:#0f172a;">+ Nhập kho xe vật lý mới (Import Vehicle - RDS 10.2)</h3>
                    <form id="import-car-form" style="display:grid;grid-template-columns:repeat(auto-fit, minmax(180px, 1fr));gap:12px;align-items:end;">
                        <label style="font-size:12px;font-weight:600;">Mẫu xe *
                            <select name="modelId" id="import-model-id" required style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                                ${models.map(m => `<option value="${m.modelID}">${esc(m.modelName)}</option>`).join("")}
                            </select>
                        </label>
                        <label style="font-size:12px;font-weight:600;">Phiên bản *
                            <select name="versionId" id="import-version-id" required style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                                ${versions.map(v => `<option value="${v.versionID}" data-model="${v.modelID}">${esc(v.versionName)}</option>`).join("")}
                            </select>
                        </label>
                        <label style="font-size:12px;font-weight:600;">Màu sắc *
                            <select name="colorId" required style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                                ${colors.map(c => `<option value="${c.colorID}">${esc(c.colorName)}</option>`).join("")}
                            </select>
                        </label>
                        <label style="font-size:12px;font-weight:600;">Nhà cung cấp
                            <select name="supplierId" style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                                <option value="">-- Chọn nhà cung cấp (nếu có) --</option>
                                ${suppliers.map(s => `<option value="${s.supplierID}">${esc(s.supplierName)}</option>`).join("")}
                            </select>
                        </label>
                        <label style="font-size:12px;font-weight:600;">Số khung VIN (Đúng 17 ký tự) *
                            <input type="text" name="vin" id="import-vin" minlength="17" maxlength="17" placeholder="VD: VF82026VN00000123" required style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;text-transform:uppercase;">
                            <div id="vin-error" style="color:#dc2626;font-size:11px;margin-top:3px;display:none;font-weight:500;"></div>
                        </label>
                        <label style="font-size:12px;font-weight:600;">Số máy (5-50 ký tự) *
                            <input type="text" name="engineNumber" id="import-engine" minlength="5" maxlength="50" placeholder="VD: VF-ENG-2026-001" required style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;text-transform:uppercase;">
                            <div id="engine-error" style="color:#dc2626;font-size:11px;margin-top:3px;display:none;font-weight:500;"></div>
                        </label>
                        <label style="font-size:12px;font-weight:600;">Giá bán (VND) *
                            <input type="number" name="price" id="import-price" min="100000" step="any" value="898000000" required style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                        </label>
                        <input type="hidden" name="year" value="2026">
                        <button type="submit" id="btn-submit-import" class="button button-primary" style="padding:9px;">+ Nhập kho xe (IMPORT) →</button>
                    </form>
                </section>

                <!-- FORM XUẤT KHO XE TRỰC TIẾP (RDS 10.3 & 9.3) -->
                <section style="padding:16px;background:#fff;border-radius:12px;border:1px solid #cbd5e1;margin-bottom:20px;">
                    <h3 style="margin:0 0 8px;font-size:15px;color:#0f172a;">- Xuất kho xe vật lý trực tiếp (Export Vehicle - RDS 10.3)</h3>
                    <p style="font-size:12px;color:#64748b;margin:0 0 12px;">Xuất xe vật lý trực tiếp khỏi kho (chuyển kho/showroom, trả nhà cung cấp, kiểm định...). Chỉ xe ở trạng thái AVAILABLE mới được xuất trực tiếp.</p>
                    <form id="export-car-form" style="display:grid;grid-template-columns:repeat(auto-fit, minmax(220px, 1fr));gap:12px;align-items:end;">
                        <label style="font-size:12px;font-weight:600;">Chọn xe khả dụng trong kho (AVAILABLE) *
                            <select name="carId" id="export-car-id" required style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                                <option value="">-- Chọn xe khả dụng (${availableCarsList.length} xe) --</option>
                                ${availableCarsList.map(c => `<option value="${c.carID}">#${c.carID} · ${esc(c.modelName)} (VIN: ${esc(c.vin)}) - ${fmtMoney(c.price)}</option>`).join("")}
                            </select>
                        </label>
                        <label style="font-size:12px;font-weight:600;">Lý do xuất kho xe *
                            <input type="text" name="exportReason" id="export-reason" placeholder="VD: Xuất chuyển showroom Cần Thơ, trả NCC..." required style="width:100%;padding:7px;border:1px solid #cbd5e1;border-radius:6px;margin-top:4px;">
                            <div id="export-reason-error" style="color:#dc2626;font-size:11px;margin-top:3px;display:none;font-weight:500;"></div>
                        </label>
                        <button type="submit" id="btn-submit-export" class="button button-secondary" style="padding:9px;color:#dc2626;border-color:#fca5a5;">- Xuất kho xe (EXPORT) →</button>
                    </form>
                </section>

                <div style="padding:16px;background:#fff;border-radius:12px;border:1px solid #e2e8f0;overflow-x:auto;">
                    <h3 style="margin:0 0 12px;font-size:15px;">Lịch sử giao dịch Nhập / Xuất kho (InventoryTransactions)</h3>
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
                                    <td style="padding:10px;">
                                        <span class="status-pill" style="${t.transactionType === 'IMPORT' ? 'background:#dcfce7;color:#166534;border:1px solid #bbf7d0;' : 'background:#fee2e2;color:#991b1b;border:1px solid #fca5a5;'}padding:3px 8px;border-radius:4px;font-weight:600;font-size:11px;">
                                            ${esc(t.transactionType)}
                                        </span>
                                    </td>
                                    <td style="padding:10px;">${esc(t.transactionDate)}</td>
                                    <td style="padding:10px;">${esc(t.note)}</td>
                                </tr>
                            `).join("") : `<tr><td colspan="5" style="padding:20px;text-align:center;">Chưa có giao dịch nhập/xuất kho.</td></tr>`}
                        </tbody>
                    </table>
                </div>
            `;

            // Dynamic filter for versions dropdown based on selected model
            const importModelSelect = document.getElementById("import-model-id");
            const importVersionSelect = document.getElementById("import-version-id");
            function filterVersionsByModel() {
                if (!importModelSelect || !importVersionSelect) return;
                const selectedModelId = parseInt(importModelSelect.value, 10);
                const matchingVersions = versions.filter(v => v.modelID === selectedModelId);
                const listToRender = matchingVersions.length ? matchingVersions : versions;
                importVersionSelect.innerHTML = listToRender.map(v => `<option value="${v.versionID}">${esc(v.versionName)}</option>`).join("");
            }
            importModelSelect?.addEventListener("change", filterVersionsByModel);
            filterVersionsByModel();

            // Client-side validation helpers
            const VIN_REGEX = /^[A-HJ-NPR-Z0-9]{17}$/;
            const ENGINE_REGEX = /^[A-Za-z0-9-]{5,50}$/;

            function validateVinField(rawVal) {
                if (!rawVal || !rawVal.trim()) {
                    return "Vui lòng nhập số khung VIN.";
                }
                const trimmed = rawVal.trim();
                if (trimmed.length !== 17) {
                    return "VIN phải có đúng 17 ký tự.";
                }
                const normalized = trimmed.toUpperCase();
                if (!VIN_REGEX.test(normalized)) {
                    return "VIN chỉ được chứa chữ cái hợp lệ và chữ số, không chứa I, O, Q.";
                }
                return "";
            }

            function validateEngineField(rawVal) {
                if (!rawVal || !rawVal.trim()) {
                    return "Vui lòng nhập số máy.";
                }
                const trimmed = rawVal.trim();
                if (trimmed.length < 5 || trimmed.length > 50) {
                    return "Số máy phải có từ 5 đến 50 ký tự.";
                }
                if (!ENGINE_REGEX.test(trimmed)) {
                    return "Số máy chỉ được chứa chữ cái Latin, chữ số và dấu gạch ngang (-).";
                }
                return "";
            }

            const vinInput = document.getElementById("import-vin");
            const vinError = document.getElementById("vin-error");
            const engineInput = document.getElementById("import-engine");
            const engineError = document.getElementById("engine-error");

            function checkVinInput() {
                if (!vinInput || !vinError) return true;
                const err = validateVinField(vinInput.value);
                if (err) {
                    vinError.textContent = err;
                    vinError.style.display = "block";
                    vinInput.style.borderColor = "#dc2626";
                    return false;
                } else {
                    vinError.textContent = "";
                    vinError.style.display = "none";
                    vinInput.style.borderColor = "#cbd5e1";
                    return true;
                }
            }

            function checkEngineInput() {
                if (!engineInput || !engineError) return true;
                const err = validateEngineField(engineInput.value);
                if (err) {
                    engineError.textContent = err;
                    engineError.style.display = "block";
                    engineInput.style.borderColor = "#dc2626";
                    return false;
                } else {
                    engineError.textContent = "";
                    engineError.style.display = "none";
                    engineInput.style.borderColor = "#cbd5e1";
                    return true;
                }
            }

            vinInput?.addEventListener("input", () => {
                vinInput.value = vinInput.value.toUpperCase();
                checkVinInput();
            });
            vinInput?.addEventListener("blur", checkVinInput);

            engineInput?.addEventListener("input", () => {
                engineInput.value = engineInput.value.toUpperCase();
                checkEngineInput();
            });
            engineInput?.addEventListener("blur", checkEngineInput);

            // Import Form Submit with Confirmation Modal (Section VII)
            const importForm = document.getElementById("import-car-form");
            importForm?.addEventListener("submit", async (e) => {
                e.preventDefault();
                const isVinValid = checkVinInput();
                const isEngineValid = checkEngineInput();
                if (!isVinValid || !isEngineValid) {
                    if (!isVinValid && vinInput) vinInput.focus();
                    else if (!isEngineValid && engineInput) engineInput.focus();
                    return;
                }

                const f = e.currentTarget;
                const priceVal = parseFloat(f.price.value);
                if (isNaN(priceVal) || priceVal < 100000) {
                    notify("Giá bán xe phải lớn hơn hoặc bằng 100.000 VNĐ.");
                    f.price.focus();
                    return;
                }
                const normVin = f.vin.value.trim().toUpperCase();
                const normEngine = f.engineNumber.value.trim().toUpperCase();
                const supplierText = f.supplierId.selectedOptions[0]?.text || "Không chọn";

                const confirmed = await showAppConfirmModal({
                    title: "Xác nhận nhập kho xe vật lý",
                    message: "Bạn có chắc chắn muốn nhập xe này vào kho?",
                    details: [
                        { label: "Mã xe", value: "(Tự sinh theo mẫu xe)" },
                        { label: "Tên xe", value: f.modelId.selectedOptions[0]?.text + " - " + f.versionId.selectedOptions[0]?.text },
                        { label: "VIN", value: normVin },
                        { label: "Số máy", value: normEngine },
                        { label: "Nhà cung cấp", value: supplierText },
                        { label: "Giá bán", value: fmtMoney(f.price.value) }
                    ],
                    confirmText: "Có, xác nhận nhập xe",
                    cancelText: "Không, quay lại"
                });
                if (!confirmed) return;

                const submitBtn = document.getElementById("btn-submit-import");
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.textContent = "Đang xử lý nhập kho...";
                }

                try {
                    const res = await postForm("/delivery", {
                        action: "importCar",
                        modelId: f.modelId.value,
                        versionId: f.versionId.value,
                        colorId: f.colorId.value,
                        supplierId: f.supplierId.value,
                        vin: normVin,
                        engineNumber: normEngine,
                        price: f.price.value,
                        year: f.year.value
                    });
                    notify(res.message);
                    await loadHandoverAndInventoryView("vehicle-movements");
                } catch (err) {
                    notify(err.message);
                    if (submitBtn) {
                        submitBtn.disabled = false;
                        submitBtn.textContent = "+ Nhập kho xe (IMPORT) →";
                    }
                }
            });

            // Export Form Submit with Confirmation Modal (Section VI & VII)
            const exportForm = document.getElementById("export-car-form");
            exportForm?.addEventListener("submit", async (e) => {
                e.preventDefault();
                const ef = e.currentTarget;
                const carId = ef.carId.value;
                const reason = ef.exportReason.value.trim();
                const reasonErrDiv = document.getElementById("export-reason-error");

                if (!carId) {
                    notify("Vui lòng chọn xe khả dụng cần xuất kho.");
                    return;
                }
                if (!reason) {
                    if (reasonErrDiv) {
                        reasonErrDiv.textContent = "Vui lòng nhập lý do xuất kho.";
                        reasonErrDiv.style.display = "block";
                    }
                    ef.exportReason.focus();
                    return;
                }
                if (reasonErrDiv) reasonErrDiv.style.display = "none";

                const selectedCar = availableCarsList.find(c => c.carID == carId);
                const confirmed = await showAppConfirmModal({
                    title: "Xác nhận xuất kho xe vật lý",
                    message: "Bạn có chắc chắn muốn xuất xe này khỏi kho?",
                    details: [
                        { label: "Mã xe", value: `Car #${carId}` },
                        { label: "Mẫu xe", value: selectedCar ? selectedCar.modelName : `Car #${carId}` },
                        { label: "VIN", value: selectedCar ? selectedCar.vin : "" },
                        { label: "Số máy", value: selectedCar ? selectedCar.engineNumber : "" },
                        { label: "Trạng thái", value: selectedCar ? selectedCar.statusName : "AVAILABLE" },
                        { label: "Lý do xuất kho", value: reason }
                    ],
                    confirmText: "Có, xác nhận xuất kho",
                    cancelText: "Không, quay lại"
                });
                if (!confirmed) return;

                const submitExportBtn = document.getElementById("btn-submit-export");
                if (submitExportBtn) {
                    submitExportBtn.disabled = true;
                    submitExportBtn.textContent = "Đang xử lý xuất kho...";
                }

                try {
                    const res = await postForm("/delivery", {
                        action: "exportCar",
                        carId: carId,
                        exportReason: reason
                    });
                    notify(res.message);
                    await loadHandoverAndInventoryView("vehicle-movements");
                } catch (err) {
                    notify(err.message);
                    if (submitExportBtn) {
                        submitExportBtn.disabled = false;
                        submitExportBtn.textContent = "- Xuất kho xe (EXPORT) →";
                    }
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
                    const confirmed = await showAppConfirmModal({
                        title: "Xác nhận thay đổi trạng thái xe",
                        message: "Bạn có chắc chắn muốn cập nhật trạng thái của xe vật lý này trong kho?",
                        details: [
                            { label: "Mã xe", value: `Car #${id}` },
                            { label: "Trạng thái mới", value: st }
                        ],
                        confirmText: "Có, xác nhận",
                        cancelText: "Không, quay lại"
                    });
                    if (!confirmed) return;
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
                const confirmed = await showAppConfirmModal({
                    title: "Xác nhận xuất kho & bàn giao xe",
                    message: "Bạn có chắc chắn muốn xác nhận xuất kho và hoàn tất biên bản bàn giao xe cho khách hàng?",
                    details: [
                        { label: "Mã đơn hàng", value: `#${form.dataset.orderId}` },
                        { label: "Địa chỉ bàn giao", value: form.deliveryAddress.value }
                    ],
                    confirmText: "Có, xác nhận",
                    cancelText: "Không, quay lại"
                });
                if (!confirmed) return;
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

    async function loadManagerReportView(reportType, selectedFrom, selectedTo) {
        const today = new Date();
        const todayValue = [today.getFullYear(), String(today.getMonth() + 1).padStart(2, "0"),
            String(today.getDate()).padStart(2, "0")].join("-");
        const fromDefault = `${today.getFullYear()}-01-01`;
        const from = selectedFrom || fromDefault;
        const to = selectedTo || todayValue;
        const reports = {
            "revenue-report": { title: "Báo cáo doanh thu", description: "Theo dõi tiền đã thu, giá trị bán hàng và công nợ theo kỳ." },
            "vehicle-report": { title: "Báo cáo xe bán ra", description: "Thống kê số xe bán theo mẫu và danh sách xe trong các đơn hợp lệ." },
            "transaction-report": { title: "Báo cáo giao dịch", description: "Đối chiếu trạng thái đơn hàng, thanh toán và từng giao dịch bán." },
            "inventory-report": { title: "Báo cáo tồn kho", description: "Ảnh chụp tồn kho hiện tại theo trạng thái xe và giá niêm yết." }
        };
        const config = reports[reportType] || reports["revenue-report"];
        content.innerHTML = `<p>Đang tổng hợp dữ liệu báo cáo từ hệ thống bán hàng...</p>`;
        const params = new URLSearchParams({ from, to });
        const report = await fetchJson(`/api/reports?${params.toString()}`);
        const sum = report.summary || {};
        const monthly = report.monthly || [];
        const models = report.models || [];
        const orders = report.sales || [];
        const inventory = report.inventory || [];
        const orderStatuses = report.orderStatuses || [];
        const paymentStatuses = report.paymentStatuses || [];
        const validOrders = orders.filter(order => order.eligible);
        const validSoldOrders = validOrders.filter(order => ["PAID", "COMPLETED"].includes(order.status));
        const soldVehicles = validSoldOrders.flatMap(order => (order.vehicles || []).map(vehicle => ({ ...vehicle, order })));
        const totalInventory = inventory.reduce((total, row) => total + Number(row.count || 0), 0);
        const inventoryValue = inventory.reduce((total, row) => total + Number(row.amount || 0), 0);
        const maxMonthlyRevenue = Math.max(1, ...monthly.map(row => Number(row.revenue || 0)));
        const money = value => fmtMoney(value || 0);
        const table = (headers, rows, emptyText) => `
            <div style="padding:14px 16px;background:#fff;border-radius:12px;border:1px solid #e2e8f0;overflow-x:auto;margin-bottom:20px;">
                <table style="width:100%;border-collapse:collapse;font-size:13px;">
                    <thead><tr style="border-bottom:2px solid #e2e8f0;text-align:left;">${headers.map(h => `<th style="padding:10px;">${h}</th>`).join("")}</tr></thead>
                    <tbody>${rows.length ? rows.join("") : `<tr><td colspan="${headers.length}" style="padding:22px;text-align:center;color:#64748b;">${emptyText}</td></tr>`}</tbody>
                </table>
            </div>`;
        const statusRows = (rows, label) => rows.map(row => `<tr style="border-bottom:1px solid #f1f5f9;">
            <td style="padding:10px;"><strong>${esc(row.status)}</strong></td><td style="padding:10px;">${row.count}</td>
            <td style="padding:10px;">${label === "order" || label === "inventory" || label === "payment" ? money(row.amount) : "—"}</td></tr>`);

        let body = "";
        if (reportType === "revenue-report") {
            body = `<section class="metric-grid" style="margin-bottom:20px;">
                <div class="metric-card"><div class="metric-head"><span>Tiền thực thu</span><span>PAYMENTS</span></div><strong>${money(sum.revenue)}</strong><div class="metric-foot">${sum.completedPayments || 0} thanh toán hoàn tất</div></div>
                <div class="metric-card"><div class="metric-head"><span>Giá trị bán hàng</span><span>ORDERS</span></div><strong>${money(sum.salesAmount)}</strong><div class="metric-foot">Đơn PAID/COMPLETED hợp lệ</div></div>
                <div class="metric-card"><div class="metric-head"><span>Công nợ còn lại</span><span>◷</span></div><strong style="color:#b91c1c;">${money(sum.outstandingAmount)}</strong><div class="metric-foot">Theo đơn hợp lệ trong kỳ</div></div>
                <div class="metric-card"><div class="metric-head"><span>Giảm giá / thuế</span><span>VNĐ</span></div><strong>${money(sum.discountAmount)}</strong><div class="metric-foot">Thuế: ${money(sum.taxAmount)}</div></div>
            </section>
            <h3>Doanh thu và bán hàng theo tháng</h3>
            <div style="background:#fff;border:1px solid #e2e8f0;border-radius:12px;padding:18px;margin-bottom:18px;">
                ${monthly.length ? monthly.map(row => `<div style="display:grid;grid-template-columns:90px 1fr 150px;gap:12px;align-items:center;margin:10px 0;font-size:13px;">
                    <strong>${esc(row.month)}</strong><div style="height:12px;background:#f1f5f9;border-radius:8px;overflow:hidden;"><div title="${money(row.revenue)}" style="width:${Math.max(2, Number(row.revenue || 0) / maxMonthlyRevenue * 100)}%;height:100%;background:#e11d48;border-radius:8px;"></div></div><span>${money(row.revenue)}</span></div>`).join("") : `<p style="color:#64748b;">Chưa có dữ liệu doanh thu trong kỳ.</p>`}
            </div>
            <h3>Chi tiết theo tháng</h3>${table(["Tháng", "Tiền thực thu", "Giá trị bán", "Xe bán", "Số đơn"], monthly.map(row => `<tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:10px;">${esc(row.month)}</td><td style="padding:10px;">${money(row.revenue)}</td><td style="padding:10px;">${money(row.salesAmount)}</td><td style="padding:10px;">${row.vehiclesSold}</td><td style="padding:10px;">${row.orders}</td></tr>`), "Chưa có số liệu trong kỳ.")}`;
        } else if (reportType === "vehicle-report") {
            body = `<section class="metric-grid" style="margin-bottom:20px;">
                <div class="metric-card"><div class="metric-head"><span>Tổng xe đã bán</span><span>XE</span></div><strong>${sum.vehiclesSold || 0}</strong><div class="metric-foot">Đơn PAID/COMPLETED hợp lệ</div></div>
                <div class="metric-card"><div class="metric-head"><span>Mẫu xe có phát sinh</span><span>▧</span></div><strong>${models.length}</strong><div class="metric-foot">Trong kỳ đã chọn</div></div>
                <div class="metric-card"><div class="metric-head"><span>Đơn bán hợp lệ</span><span>▤</span></div><strong>${validSoldOrders.length}</strong><div class="metric-foot">Không tính đơn hủy/chứng từ sai</div></div>
                <div class="metric-card"><div class="metric-head"><span>Giá trị xe theo chi tiết đơn</span><span>VNĐ</span></div><strong>${money(models.reduce((total, row) => total + Number(row.salesAmount || 0), 0))}</strong><div class="metric-foot">Từ OrderDetails.SalesPrice</div></div>
            </section><h3>Số xe và giá trị bán theo mẫu</h3>
            ${table(["Mẫu xe", "Số xe", "Giá trị bán"], models.map(row => `<tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:10px;"><strong>${esc(row.modelName)}</strong></td><td style="padding:10px;">${row.vehiclesSold}</td><td style="padding:10px;">${money(row.salesAmount)}</td></tr>`), "Chưa có xe bán trong kỳ.")}
            <h3>Danh sách xe trong đơn hợp lệ (${soldVehicles.length})</h3>
            ${table(["Mã đơn / ngày", "Mẫu xe", "Phiên bản", "VIN", "Giá bán", "Khách hàng"], soldVehicles.map(item => `<tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:10px;">#${item.order.orderId}<br><small>${esc(item.order.orderDate)}</small></td><td style="padding:10px;">${esc(item.modelName)}</td><td style="padding:10px;">${esc(item.versionName)}</td><td style="padding:10px;">${esc(item.vin)}</td><td style="padding:10px;">${money(item.salesPrice)}</td><td style="padding:10px;">${esc(item.order.customerName)}</td></tr>`), "Chưa có xe bán trong kỳ.")}`;
        } else if (reportType === "transaction-report") {
            body = `<section class="metric-grid" style="margin-bottom:20px;">
                <div class="metric-card"><div class="metric-head"><span>Tổng giao dịch</span><span>ORDERS</span></div><strong>${sum.totalOrders || 0}</strong><div class="metric-foot">Theo ngày tạo đơn</div></div>
                <div class="metric-card"><div class="metric-head"><span>Đơn hoàn tất</span><span>✓</span></div><strong>${sum.completedOrders || 0}</strong><div class="metric-foot">Đơn PAID, COMPLETED hoặc đã bàn giao xe</div></div>
                <div class="metric-card"><div class="metric-head"><span>Đơn đã hủy</span><span>×</span></div><strong>${sum.cancelledOrders || 0}</strong><div class="metric-foot">Trạng thái CANCELLED</div></div>
                <div class="metric-card"><div class="metric-head"><span>Chứng từ cần rà soát</span><span>!</span></div><strong>${sum.inconsistentOrders || 0}</strong><div class="metric-foot">Hợp đồng/hóa đơn không hợp lệ</div></div>
            </section><h3>Phân loại trạng thái đơn hàng</h3>
            ${table(["Trạng thái đơn", "Số đơn", "Giá trị đơn"], statusRows(orderStatuses, "order"), "Chưa có đơn hàng trong kỳ.")}
            <h3>Phân loại thanh toán</h3>
            ${table(["Trạng thái thanh toán", "Số giao dịch", "Số tiền"], statusRows(paymentStatuses, "payment"), "Chưa có thanh toán trong kỳ.")}
            <h3>Chi tiết giao dịch (${orders.length})</h3>
            ${table(["Đơn hàng", "Khách hàng", "Nhân viên", "Tổng tiền", "Đã thu", "Còn nợ", "Trạng thái / chứng từ"], orders.map(row => `<tr style="border-bottom:1px solid #f1f5f9;"><td style="padding:10px;"><strong>#${row.orderId}</strong><br><small>${esc(row.orderDate)}</small></td><td style="padding:10px;">${esc(row.customerName)}</td><td style="padding:10px;">${esc(row.employeeName || "—")}</td><td style="padding:10px;">${money(row.totalAmount)}</td><td style="padding:10px;color:#15803d;">${money(row.paidAmount)}</td><td style="padding:10px;color:#b91c1c;">${money(row.remainingAmount)}</td><td style="padding:10px;"><span class="status-pill">${esc(row.status)}</span>${row.eligible ? "" : "<br><small>Cần rà soát chứng từ</small>"}${row.contractNumber ? `<br><small>HĐ: ${esc(row.contractNumber)}</small>` : ""}${row.invoiceNumber ? `<br><small>Hóa đơn: ${esc(row.invoiceNumber)}</small>` : ""}</td></tr>`), "Chưa có giao dịch trong kỳ.")}`;
        } else {
            body = `<section class="metric-grid" style="margin-bottom:20px;">
                <div class="metric-card"><div class="metric-head"><span>Tổng xe vật lý</span><span>XE</span></div><strong>${totalInventory}</strong><div class="metric-foot">Đang ghi nhận trong Cars</div></div>
                <div class="metric-card"><div class="metric-head"><span>Xe có sẵn</span><span>AVAILABLE</span></div><strong>${(inventory.find(row => row.status === "AVAILABLE") || {}).count || 0}</strong><div class="metric-foot">Khả dụng để bán</div></div>
                <div class="metric-card"><div class="metric-head"><span>Xe đã bán</span><span>SOLD</span></div><strong>${(inventory.find(row => row.status === "SOLD") || {}).count || 0}</strong><div class="metric-foot">Theo trạng thái xe hiện tại</div></div>
                <div class="metric-card"><div class="metric-head"><span>Giá niêm yết tồn kho</span><span>VNĐ</span></div><strong>${money(inventoryValue)}</strong><div class="metric-foot">Tổng giá theo trạng thái hiện tại</div></div>
            </section><h3>Tồn kho theo trạng thái xe</h3>
            ${table(["Trạng thái xe", "Số lượng", "Tổng giá trị niêm yết"], statusRows(inventory, "inventory"), "Chưa có dữ liệu xe trong kho.")}
            <p style="color:#64748b;font-size:12px;">Báo cáo tồn kho là ảnh chụp trạng thái hiện tại; khoảng ngày chỉ áp dụng cho các luồng giao dịch.</p>`;
        }

        const exportParams = new URLSearchParams({ from, to, format: "xlsx" });
        content.innerHTML = `
            <div class="page-heading" style="align-items:flex-start;gap:18px;">
                <div><div class="eyebrow"><span class="eyebrow-line"></span>UC-18 · BÁO CÁO QUẢN LÝ</div>
                    <h1>${config.title}</h1><p>${config.description} Dữ liệu tổng hợp trực tiếp từ các luồng giao dịch trong hệ thống.</p></div>
                <a class="button button-primary" href="${appContext}/api/reports?${exportParams.toString()}">Xuất Excel (.xlsx) ↗</a>
            </div>
            <form id="manager-report-filter" style="display:flex;align-items:end;gap:12px;flex-wrap:wrap;background:#fff;padding:14px 16px;border:1px solid #e2e8f0;border-radius:12px;margin-bottom:20px;">
                <label style="display:grid;gap:5px;font-size:12px;color:#475569;">Từ ngày<input name="from" type="date" value="${esc(from)}" required></label>
                <label style="display:grid;gap:5px;font-size:12px;color:#475569;">Đến ngày<input name="to" type="date" value="${esc(to)}" required></label>
                <button class="button button-secondary" type="submit">Xem báo cáo</button>
                <span style="font-size:12px;color:#64748b;">Kỳ báo cáo: ${esc(from)} – ${esc(to)}</span>
            </form>
            ${body}`;
        $("#manager-report-filter")?.addEventListener("submit", event => {
            event.preventDefault();
            const form = event.currentTarget;
            if (form.elements.from.value > form.elements.to.value) {
                notify("Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc.");
                return;
            }
            loadManagerReportView(reportType, form.elements.from.value, form.elements.to.value);
        });
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
