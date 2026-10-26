"use strict";
const $ = (selector, root = document) => root.querySelector(selector);
const state = {
    user: null,
    permissions: [],
    csrf: "",
    view: "catalogue",
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
    aiHistory: [],
    catalogueMetadata: null,
    catalogueFilters: {},
    comparedIds: [],
    cataloguePrompt: null
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
    overview: ["Tổng quan showroom", "Nhìn nhanh kết quả kinh doanh và mở những chức năng thường dùng."],
    catalogue: ["Danh mục xe VinFast", "Khám phá thông số, đối chiếu các phiên bản và hỏi trợ lý tư vấn."],
    users: ["Tài khoản", "Quản lý tài khoản, vai trò và quyền truy cập hệ thống."],
    employees: ["Nhân viên", "Thông tin liên hệ của đội ngũ showroom."],
    roles: ["Phân quyền", "Thiết lập những chức năng mỗi vai trò được phép sử dụng."],
    configurations: ["Cấu hình hệ thống", "Điều chỉnh thời gian phiên đăng nhập và phạm vi truy vấn báo cáo."],
    promotions: ["Khuyến mãi", "Tạo ưu đãi và lựa chọn những mẫu xe tham gia chương trình."],
    reports: ["Báo cáo kinh doanh", "Theo dõi doanh thu, xe bán ra và tiến độ giao dịch."],
    logs: ["Nhật ký hệ thống", "Theo dõi hoạt động và truy vết những thay đổi quan trọng."],
    ai: ["Trợ lý xe", "Tìm hiểu, so sánh và chọn chiếc xe phù hợp với bạn."]
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
    return (
        ["catalogue", "ai"].includes(name) ||
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
    state.aiHistory = [];
    if (typeof resetAIState === "function") {
        resetAIState();
    }
    state.cataloguePrompt = null;
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
                const result = await api("auth", { method: "POST", body: { action: "logout" } });
                applySession(result);
                clearPrivateState();
                await showView("catalogue");
                toast("Đã đăng xuất.");
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
                minlength: 10,
                maxlength: 128,
                autocomplete: "new-password",
                help: "10–128 ký tự và khác mật khẩu hiện tại."
            }),
            field("confirmPassword", "Nhập lại mật khẩu mới", "password", "", {
                required: true,
                full: true,
                minlength: 10,
                maxlength: 128,
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
            // Server tạo session mới, nên ngữ cảnh tư vấn cũ cũng đã hết hiệu lực.
            state.sessionGeneration++;
            resetAIState();
            if (state.view === "ai") {
                await loadAI();
            }
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

async function showView(name, options = {}) {
    name = Object.hasOwn(views, name) ? name : "catalogue";
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
    $("#page-eyebrow").textContent = ["catalogue", "ai"].includes(name) ? "KHÁM PHÁ XE ĐIỆN" : "VẬN HÀNH SHOWROOM";
    for (const nav of document.querySelectorAll("[data-view]")) {
        nav.classList.toggle("active", nav.dataset.view === name);
        if (nav.dataset.view === name) {
            nav.setAttribute("aria-current", "page");
        } else {
            nav.removeAttribute("aria-current");
        }
    }
    if (!["catalogue", "ai"].includes(name) && (!state.user || !canView(name))) {
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
                logs: loadLogs,
                ai: loadAI
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
