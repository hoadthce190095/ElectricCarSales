"use strict";
const $ = (selector, root = document) => root.querySelector(selector);
const state = {
    user: null,
    permissions: [],
    csrf: "",
    view: "overview",
    viewGeneration: 0,
    loadSequence: 0,
    sessionGeneration: 0,
    lookups: null,
    models: [],
    userFilters: {},
    userPage: 1,
    employeeFilters: {},
    promotionFilters: {},
    logFilters: {},
    logPage: 1,
    report: null,
    reportFilters: {},
    reportComparison: "NONE",
    reportEmployees: [],
    overviewFilters: {},
    catalogueMetadata: null,
    catalogueFilters: {},
    comparedIds: []
};
const money = (value) =>
    value == null
        ? "Chưa có dữ liệu"
        : new Intl.NumberFormat("vi-VN", {
              style: "currency",
              currency: "VND",
              maximumFractionDigits: 0
          }).format(value);
const number = (value) => new Intl.NumberFormat("vi-VN").format(value == null ? 0 : value);
const date = (value) => {
    const parsed = businessDate(value);
    return parsed
        ? parsed.toLocaleString("vi-VN", {
              timeZone: "Asia/Ho_Chi_Minh",
              day: "2-digit",
              month: "2-digit",
              year: "numeric",
              hour: "2-digit",
              minute: "2-digit"
          })
        : "—";
};

function businessDate(value) {
    if (!value) {
        return null;
    }
    const source = String(value);
    const zoned = /T\d\d:\d\d/.test(source) && !/(Z|[+-]\d\d:\d\d)$/.test(source) ? `${source}+07:00` : source;
    const parsed = new Date(zoned);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
}

const roleLabels = {
    ADMIN: "Quản trị viên",
    MANAGER: "Quản lý",
    ACCOUNTANT: "Kế toán",
    SALES: "Tư vấn bán hàng",
    OPERATIONS: "Vận hành",
    CUSTOMER: "Khách hàng"
};
const statusLabels = {
    ACTIVE: "Hoạt động",
    INACTIVE: "Ngừng hoạt động",
    LOCKED: "Đã khóa",
    PENDING: "Chờ xử lý",
    RESERVED: "Giữ chỗ",
    DEPOSITED: "Đã đặt cọc",
    PROCESSING: "Đang xử lý",
    PAID: "Đã thanh toán",
    COMPLETED: "Hoàn tất",
    CANCELLED: "Đã hủy",
    AVAILABLE: "Có sẵn",
    SOLD: "Đã bán",
    DELIVERED: "Đã bàn giao",
    FAILD: "Thất bại",
    FAILED: "Thất bại"
};
const views = {
    overview: ["Tổng quan showroom", "Theo dõi kết quả kinh doanh, giao dịch và tồn kho."],
    catalogue: ["Danh mục xe VinFast", "Tra cứu thông số và đối chiếu các phiên bản xe trong showroom."],
    users: ["Tài khoản", "Quản lý tài khoản, vai trò và quyền truy cập hệ thống."],
    employees: ["Nhân viên", "Thông tin liên hệ của đội ngũ showroom."],
    roles: ["Phân quyền", "Thiết lập những chức năng mỗi vai trò được phép sử dụng."],
    configurations: ["Cấu hình hệ thống", "Điều chỉnh thời gian phiên đăng nhập và phạm vi truy vấn báo cáo."],
    promotions: ["Khuyến mãi", "Tạo ưu đãi và lựa chọn những mẫu xe tham gia chương trình."],
    reports: ["Báo cáo kinh doanh", "Theo dõi doanh thu, xe bán ra và tiến độ giao dịch."],
    logs: ["Nhật ký hệ thống", "Theo dõi hoạt động và truy vết những thay đổi quan trọng."]
};

function el(tag, attrs = {}, children = []) {
    const node = document.createElement(tag);
    for (const [key, value] of Object.entries(attrs)) {
        if (key === "text") {
            node.textContent = value == null ? "" : String(value);
        } else if (key === "class") {
            node.className = value;
        } else if (key.startsWith("on") && typeof value === "function") {
            node.addEventListener(key.slice(2), value);
        } else if (value !== false && value != null) {
            node.setAttribute(key, value === true ? "" : String(value));
        }
    }
    for (const child of Array.isArray(children) ? children : [children]) {
        if (child != null) {
            node.append(child.nodeType ? child : document.createTextNode(String(child)));
        }
    }
    return node;
}

function button(text, action, cls = "button secondary") {
    return el("button", { type: "button", class: cls, text, onclick: action });
}

function pill(value, kind) {
    const danger = ["LOCKED", "CANCELLED", "FAILED", "FAILD"].includes(value);
    const neutral = ["INACTIVE"].includes(value);
    const warning = ["PENDING", "RESERVED", "PROCESSING", "DEPOSITED"].includes(value);
    return el("span", {
        class: `pill ${kind || (danger ? "danger" : neutral ? "neutral" : warning ? "warning" : "")}`,
        text: statusLabels[value] || roleLabels[value] || value || "—"
    });
}

function cellTitle(title, subtitle) {
    return el("div", {}, [
        el("div", { class: "cell-title", text: title || "—" }),
        subtitle ? el("div", { class: "cell-subtitle", text: subtitle }) : null
    ]);
}

function empty(title = "Chưa có dữ liệu", detail = "Dữ liệu sẽ xuất hiện tại đây khi được ghi nhận trong hệ thống.") {
    return el("div", { class: "empty" }, [
        el("div", { class: "empty-icon", text: "◇" }),
        el("strong", { text: title }),
        el("div", { text: detail })
    ]);
}

function notice(message, error = false) {
    const n = $("#notice");
    n.textContent = message;
    n.className = `notice${error ? " error" : ""}`;
    n.hidden = !message;
}

let toastTimer;
function toast(message) {
    const n = $("#toast");
    n.textContent = message;
    n.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => (n.hidden = true), 4500);
}

// JSP cung cấp context path để link vẫn đúng khi đổi tên WAR hoặc mở trang qua forward.
function appUrl(path) {
    const contextPath = document.querySelector('meta[name="app-context-path"]')?.content || "";
    return `${contextPath.replace(/\/+$/, "")}/${String(path).replace(/^(?:\.\/|\/)+/, "")}`;
}

async function api(path, options = {}) {
    const method = options.method || "GET";
    const headers = { Accept: "application/json", ...(options.headers || {}) };
    if (method !== "GET") {
        headers["Content-Type"] = "application/json";
        headers["X-CSRF-Token"] = state.csrf;
    }
    const response = await fetch(appUrl(`api/${path}`), {
        credentials: "same-origin",
        ...options,
        headers,
        body: options.body == null ? undefined : JSON.stringify(options.body)
    });
    let data;
    try {
        data = await response.json();
    } catch (_) {
        throw new Error("Máy chủ chưa trả về JSON. Kiểm tra URL triển khai và cấu hình Tomcat.");
    }
    if (!response.ok) {
        const changingPassword = path === "auth" && options.body?.action === "changePassword";
        if ([401, 403].includes(response.status) && (path !== "auth" || changingPassword)) {
            if (response.status === 401) {
                state.user = null;
                state.permissions = [];
                state.lookups = null;
                clearPrivateState();
                updateSession();
            }
            try {
                const fresh = await api("auth");
                state.user = fresh.user;
                state.permissions = fresh.permissions || [];
                state.csrf = fresh.csrfToken;
                state.lookups = null;
                updateSession();
            } catch (_) {
                state.user = null;
                state.permissions = [];
                updateSession();
            }
            if (!canView(state.view)) {
                $("#content").replaceChildren(accessNeeded());
            }
        }
        const error = new Error(data.error || `Yêu cầu thất bại (${response.status}).`);
        error.status = response.status;
        throw error;
    }
    return data;
}

function query(values) {
    return new URLSearchParams(Object.entries(values).filter(([, v]) => v !== "" && v != null)).toString();
}

async function task(action) {
    const generation = state.viewGeneration;
    try {
        return await action();
    } catch (error) {
        if (generation === state.viewGeneration) {
            notice(error.message, true);
        }
    }
}

// Mỗi lần tải có mã riêng để phản hồi chậm không ghi đè màn hình vừa chọn.
function beginViewLoad(name) {
    return { name, generation: state.viewGeneration, sequence: ++state.loadSequence };
}

function isViewCurrent(token) {
    return (
        state.view === token.name && state.viewGeneration === token.generation && state.loadSequence === token.sequence
    );
}

async function readViewData(token, read) {
    try {
        return await read();
    } catch (error) {
        if (isViewCurrent(token)) {
            throw error;
        }
        return null;
    }
}

function isAdmin() {
    return state.user?.role === "ADMIN";
}

function allowed(code) {
    return isAdmin() || state.permissions.includes(code);
}

function canReport() {
    return allowed("VIEW_REPORTS");
}

function canView(name) {
    if (!Object.hasOwn(views, name)) {
        return false;
    }
    return (
        name === "catalogue" ||
        (["roles", "configurations"].includes(name)
            ? isAdmin()
            : allowed(
                  {
                      overview: "VIEW_REPORTS",
                      users: "MANAGE_USERS",
                      employees: "MANAGE_USERS",
                      promotions: "MANAGE_PROMOTIONS",
                      reports: "VIEW_REPORTS",
                      logs: "VIEW_LOGS"
                  }[name]
              ))
    );
}

function updateSession() {
    $("#session-label").textContent = state.user?.username || "Đăng nhập";
    $("#avatar").textContent = (state.user?.username || "G").slice(0, 1).toUpperCase();
    for (const node of document.querySelectorAll("[data-access]")) {
        node.hidden = !canView(node.dataset.view);
    }
    for (const group of document.querySelectorAll("[data-nav-group]")) {
        group.hidden = ![...group.querySelectorAll("[data-view]")].some((node) => !node.hidden);
    }
}

function accessNeeded() {
    return el("div", { class: "auth-callout" }, [
        el("h2", { text: state.user ? "Quyền truy cập đã thay đổi" : "Đăng nhập để tiếp tục" }),
        el("p", {
            text: state.user
                ? "Tài khoản hiện tại không được phân quyền cho chức năng này. Chọn chức năng khác trong menu."
                : "Chức năng này dành cho tài khoản được phân quyền của showroom."
        }),
        state.user ? null : button("Đăng nhập", login, "button")
    ]);
}

function card(title, subtitle, body) {
    return el("section", { class: "card" }, [
        el("div", { class: "card-heading" }, [
            el("div", {}, [el("h3", { text: title }), subtitle ? el("p", { text: subtitle }) : null])
        ]),
        body
    ]);
}

function table(headers, rows) {
    return el(
        "div",
        { class: "table-wrapper" },
        el("table", {}, [
            el(
                "thead",
                {},
                el(
                    "tr",
                    {},
                    headers.map((h) => el("th", { scope: "col", text: h }))
                )
            ),
            el(
                "tbody",
                {},
                rows.map((row) =>
                    el(
                        "tr",
                        {},
                        row.map((v) => el("td", {}, v?.nodeType ? v : String(v ?? "—")))
                    )
                )
            )
        ])
    );
}

function pager(page, total, pageSize, onPage) {
    const last = Math.max(1, Math.ceil(total / pageSize));
    const prev = button("← Trước", () => onPage(page - 1), "button secondary small");
    prev.disabled = page <= 1;
    const next = button("Sau →", () => onPage(page + 1), "button secondary small");
    next.disabled = page >= last;
    return el("div", { class: "pagination" }, [
        el("span", { text: `${number(total)} bản ghi · Trang ${page}/${last}` }),
        el("div", { class: "pagination-buttons" }, [prev, next])
    ]);
}

function field(name, label, type = "text", value = "", options = {}) {
    const input =
        type === "select"
            ? el(
                  "select",
                  { name, required: options.required || false, multiple: options.multiple || false },
                  (options.choices || []).map((o) =>
                      el("option", {
                          value: o.value,
                          text: o.label,
                          selected: options.multiple
                              ? (value || []).map(String).includes(String(o.value))
                              : String(value) === String(o.value)
                      })
                  )
              )
            : type === "textarea"
              ? el("textarea", { name, rows: 3, text: value || "", maxlength: options.maxlength || 500 })
              : el("input", {
                    name,
                    type,
                    value: value ?? "",
                    required: options.required || false,
                    min: options.min,
                    max: options.max,
                    step: options.step,
                    maxlength: options.maxlength,
                    minlength: options.minlength,
                    autocomplete: options.autocomplete || "off"
                });
    return el("label", { class: `field${options.full ? " full" : ""}` }, [
        el("span", { text: label }),
        input,
        options.help ? el("small", { text: options.help }) : null
    ]);
}

function values(form) {
    return Object.fromEntries(new FormData(form));
}

function searchText(value) {
    // Bỏ dấu khi tìm tại trình duyệt để "Tan" vẫn tìm được tên "Tấn".
    return String(value || "")
        .trim()
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/đ/g, "d");
}

function applySession(result) {
    state.user = result.user;
    state.permissions = result.permissions || [];
    state.csrf = result.csrfToken;
    state.lookups = null;
    updateSession();
}

function clearPrivateState() {
    state.sessionGeneration++;
    state.viewGeneration++;
    state.overviewFilters = {};
    state.report = null;
    state.reportEmployees = [];
    state.reportFilters = {};
    state.reportComparison = "NONE";
    state.userFilters = {};
    state.userPage = 1;
    state.employeeFilters = {};
    state.promotionFilters = {};
    state.logFilters = {};
    state.logPage = 1;
}

window.showAppConfirmModal = function({
    title = "Xác nhận thao tác",
    message = "Bạn có chắc chắn muốn thực hiện thao tác này?",
    details = [],
    confirmText = "Có, xác nhận",
    cancelText = "Không, quay lại",
    isDanger = false,
    onConfirm = null
}) {
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
                            <span style="color:#64748b;font-weight:500;">${d.label}:</span>
                            <span style="color:#0f172a;font-weight:600;text-align:right;">${d.value}</span>
                        </div>
                    `).join("")}
                </div>
            `;
        }

        dialog.innerHTML = `
            <div style="padding:18px 24px;border-bottom:1px solid #e2e8f0;background:#f8fafc;display:flex;align-items:center;justify-content:space-between;">
                <h3 style="margin:0;font-size:16px;font-weight:700;color:#0f172a;">${title}</h3>
                <button type="button" class="app-confirm-close" aria-label="Đóng" style="background:transparent;border:none;font-size:22px;line-height:1;color:#64748b;cursor:pointer;padding:2px 6px;border-radius:6px;">&times;</button>
            </div>
            <div style="padding:20px 24px;">
                <p style="margin:0;font-size:14px;line-height:1.5;color:#334155;">${message}</p>
                ${detailsHtml}
            </div>
            <div style="display:flex;justify-content:flex-end;gap:10px;padding:16px 24px;border-top:1px solid #e2e8f0;background:#f8fafc;">
                <button type="button" class="app-confirm-btn-cancel button secondary" style="padding:8px 18px;font-size:13px;font-weight:600;border-radius:7px;border:1px solid #cbd5e1;background:#fff;color:#334155;cursor:pointer;">${cancelText}</button>
                <button type="button" class="app-confirm-btn-confirm button" style="padding:8px 20px;font-size:13px;font-weight:600;border-radius:7px;border:none;background:${isDanger ? '#dc2626' : '#1e3a8a'};color:#fff;cursor:pointer;">${confirmText}</button>
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
};

let modalSequence = 0;
function modal(title, fields, onSubmit, submitLabel = "Lưu thay đổi") {
    const dialog = $("#form-dialog");
    const sequence = ++modalSequence;
    let pending = false;
    dialog.dataset.pending = "false";
    $("#dialog-submit").disabled = false;
    $("#dialog-close").disabled = false;
    $("#dialog-cancel").disabled = false;
    $("#dialog-title").textContent = title;
    $("#dialog-fields").replaceChildren(...fields);
    $("#dialog-error").hidden = true;
    $("#dialog-submit").textContent = submitLabel;
    $("#dialog-form").onsubmit = async (event) => {
        event.preventDefault();
        if (pending || sequence !== modalSequence) {
            return;
        }
        pending = true;
        dialog.dataset.pending = "true";
        const form = event.currentTarget;
        const input = values(form);
        const controls = [...dialog.querySelectorAll("button, input, select, textarea")];
        const disabled = controls.map((control) => control.disabled);
        controls.forEach((control) => {
            control.disabled = true;
        });
        $("#dialog-error").hidden = true;
        try {
            await onSubmit(input, form);
            if (sequence === modalSequence) {
                dialog.close();
            }
        } catch (error) {
            if (sequence === modalSequence) {
                $("#dialog-error").textContent = error.message;
                $("#dialog-error").hidden = false;
            }
        } finally {
            pending = false;
            if (sequence === modalSequence) {
                dialog.dataset.pending = "false";
                controls.forEach((control, index) => {
                    control.disabled = disabled[index];
                });
            }
        }
    };
    if (!dialog.open) {
        dialog.showModal();
    }
}

function login() {
    if (state.user) {
        modal(
            "Tài khoản của bạn",
            [
                el("div", { class: "field full account-summary" }, [
                    el("strong", { text: state.user.username }),
                    el("p", { text: roleLabels[state.user.role] || state.user.role }),
                    button(
                        "Đổi mật khẩu",
                        () => {
                            $("#form-dialog").close();
                            changePassword();
                        },
                        "button secondary"
                    )
                ])
            ],
            async () => {
                const confirmed = await window.showAppConfirmModal({
                    title: "Xác nhận đăng xuất",
                    message: "Bạn có chắc chắn muốn đăng xuất không?",
                    confirmText: "Đăng xuất",
                    cancelText: "Hủy",
                    isDanger: true
                });
                if (!confirmed) return;
                const result = await api("auth", { method: "POST", body: { action: "logout" } });
                applySession(result);
                clearPrivateState();
                location.assign(result.redirect || appUrl("/index.jsp"));
            },
            "Đăng xuất"
        );
        return;
    }
    modal(
        "Đăng nhập vào showroom",
        [
            field("username", "Tên đăng nhập", "text", "", {
                required: true,
                autocomplete: "username",
                full: true,
                maxlength: 50
            }),
            field("password", "Mật khẩu", "password", "", {
                required: true,
                maxlength: 128,
                autocomplete: "current-password",
                full: true
            })
        ],
        async (input) => {
            const result = await api("auth", { method: "POST", body: { action: "login", ...input } });
            applySession(result);
            clearPrivateState();
            await showView(canReport() ? "overview" : canView("users") ? "users" : "catalogue");
            toast(`Xin chào ${state.user.username}.`);
        },
        "Đăng nhập"
    );
}

function changePassword() {
    modal(
        "Đổi mật khẩu cá nhân",
        [
            field("currentPassword", "Mật khẩu hiện tại", "password", "", {
                required: true,
                full: true,
                maxlength: 128,
                autocomplete: "current-password"
            }),
            field("newPassword", "Mật khẩu mới", "password", "", {
                required: true,
                full: true,
                minlength: 8,
                maxlength: 30,
                autocomplete: "new-password",
                help: "8–30 ký tự và khác mật khẩu hiện tại."
            }),
            field("confirmPassword", "Nhập lại mật khẩu mới", "password", "", {
                required: true,
                full: true,
                minlength: 8,
                maxlength: 30,
                autocomplete: "new-password"
            }),
            el("p", {
                class: "field full section-note",
                text: "Bạn tiếp tục dùng phiên hiện tại sau khi đổi. Những phiên khác đang dùng mật khẩu cũ sẽ cần đăng nhập lại."
            })
        ],
        async (input) => {
            if (input.newPassword !== input.confirmPassword) {
                throw new Error("Hai lần nhập mật khẩu mới chưa giống nhau.");
            }
            if (input.newPassword === input.currentPassword) {
                throw new Error("Mật khẩu mới phải khác mật khẩu hiện tại.");
            }
            const result = await api("auth", {
                method: "POST",
                body: { action: "changePassword", ...input }
            });
            applySession(result);
            state.sessionGeneration++;
            toast(result.message || "Đã đổi mật khẩu cá nhân.");
        },
        "Đổi mật khẩu"
    );
}

async function lookups() {
    if (!state.lookups) {
        state.lookups = await api("admin/lookups");
    }
    return state.lookups;
}

function viewFromLocation() {
    const name = window.location.hash.replace(/^#\/?/, "");
    return Object.hasOwn(views, name) ? name : null;
}

function defaultView() {
    if (!state.user || canReport()) {
        return "overview";
    }
    return canView("users") ? "users" : "catalogue";
}

async function showView(name, options = {}) {
    name = Object.hasOwn(views, name) ? name : defaultView();
    const generation = ++state.viewGeneration;
    state.view = name;
    if (options.history !== false && window.location.hash !== `#${name}`) {
        window.history.pushState(null, "", `#${name}`);
    }
    if (typeof closeNavigation === "function") {
        closeNavigation();
    }
    const heading = views[name];
    document.title = `${heading[0]} · VinFast ElectricCarSale`;
    $("#page-title").textContent = heading[0];
    $("#breadcrumb-title").textContent = heading[0];
    $("#page-description").textContent = heading[1];
    $("#page-actions").replaceChildren();
    $("#main").setAttribute("aria-busy", "false");
    if (options.focus) {
        $("#main").focus({ preventScroll: true });
    }
    notice("");
    $("#page-eyebrow").textContent = name === "catalogue" ? "DANH MỤC SHOWROOM" : "VẬN HÀNH SHOWROOM";
    for (const nav of document.querySelectorAll("[data-view]")) {
        nav.classList.toggle("active", nav.dataset.view === name);
        if (nav.dataset.view === name) {
            nav.setAttribute("aria-current", "page");
        } else {
            nav.removeAttribute("aria-current");
        }
    }
    if (name !== "catalogue" && (!state.user || !canView(name))) {
        $("#content").replaceChildren(accessNeeded());
        return;
    }
    $("#content").replaceChildren(el("div", { class: "loading", text: "Đang tải dữ liệu…" }));
    $("#main").setAttribute("aria-busy", "true");
    await task(() =>
        (
            ({
                overview: loadOverview,
                catalogue: loadCatalogue,
                users: loadUsers,
                employees: loadEmployees,
                roles: loadRoles,
                configurations: loadConfigurations,
                promotions: loadPromotions,
                reports: loadReports,
                logs: loadLogs
            })[name] || loadCatalogue
        )()
    );
    if (generation !== state.viewGeneration) {
        return;
    }
    $("#main").setAttribute("aria-busy", "false");
    if ($("#content > .loading") && $("#notice").textContent) {
        $("#content").replaceChildren(
            el("div", { class: "load-error" }, [
                empty("Chưa thể tải dữ liệu", "Kiểm tra kết nối, sau đó thử tải lại màn hình này."),
                button("Thử tải lại", () => showView(name, { history: false }), "button secondary")
            ])
        );
    }
}

// Global logout confirmation for Admin portal
document.addEventListener("submit", async function(e) {
    const form = e.target;
    if (!form || !form.action || !form.action.includes("/logout")) return;
    if (form.dataset.confirmedLogout === "true") return;
    e.preventDefault();
    e.stopPropagation();

    const confirmed = await window.showAppConfirmModal({
        title: "Xác nhận đăng xuất",
        message: "Bạn có chắc chắn muốn đăng xuất không?",
        confirmText: "Đăng xuất",
        cancelText: "Hủy",
        isDanger: true
    });
    if (confirmed) {
        form.dataset.confirmedLogout = "true";
        form.submit();
    }
}, true);
