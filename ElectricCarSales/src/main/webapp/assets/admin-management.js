"use strict";

async function refreshAdminAfterSave(refresh, message) {
    toast(message);
    try {
        await refresh();
    } catch (error) {
        // Dữ liệu đã lưu thành công; không giữ form mở để người dùng gửi lại lần nữa.
        notice(`${message} Chưa thể làm mới danh sách: ${error.message}`, true);
    }
}

async function loadUsers(filters = state.userFilters, page = state.userPage) {
    if (state.view !== "users" || !canView("users")) {
        return;
    }
    const token = beginViewLoad("users");
    filters = { ...filters };
    const result = await readViewData(token, () =>
        Promise.all([api(`admin/users?${query({ ...filters, page })}`), lookups()])
    );
    if (!isViewCurrent(token) || !canView("users")) {
        return;
    }
    const [data, lookup] = result;
    const lastPage = Math.max(1, Math.ceil(data.total / data.pageSize));
    if (data.page > lastPage) {
        return loadUsers(filters, lastPage);
    }
    notice("");
    state.userFilters = { ...filters };
    state.userPage = data.page;
    $("#page-actions").replaceChildren();
    const searchNode = field("q", "Tìm kiếm tài khoản", "search", filters.q || "", {
        help: "Tên đăng nhập, họ tên hoặc email"
    });
    searchNode.classList.add("search-field");
    const searchForm = el(
        "form",
        {
            class: "toolbar",
            onsubmit: (event) => {
                event.preventDefault();
                task(() => loadUsers(values(searchForm), 1));
            }
        },
        [
            searchNode,
            field("roleId", "Vai trò", "select", filters.roleId || "", {
                choices: [
                    { value: "", label: "Tất cả vai trò" },
                    ...lookup.roles.map((role) => ({
                        value: role.id,
                        label: roleLabels[role.name] || role.name
                    }))
                ]
            }),
            field("statusId", "Trạng thái", "select", filters.statusId || "", {
                choices: [
                    { value: "", label: "Tất cả trạng thái" },
                    ...lookup.statuses.map((status) => ({
                        value: status.id,
                        label: statusLabels[status.name] || status.name
                    }))
                ]
            }),
            el("button", { type: "submit", class: "button secondary", text: "Lọc tài khoản" }),
            button("Xóa bộ lọc", () => task(() => loadUsers({}, 1)), "text-button")
        ]
    );
    const rows = data.items.map((u) => [
        cellTitle(u.username, `UserID #${u.userId}`),
        u.employeeId ? cellTitle(u.employeeName, `EmployeeID #${u.employeeId}`) : el("span", { text: u.role === "CUSTOMER" ? "Khách hàng" : "Chưa có hồ sơ nhân viên" }),
        pill(u.role, "neutral"),
        pill(u.status),
        date(u.createdDate),
        u.role === "ADMIN" && !isAdmin()
            ? pill("Được bảo vệ", "neutral")
            : button("Chỉnh sửa", () => task(() => editUser(u)), "text-button")
    ]);
    $("#content").replaceChildren(
        card(
            "Danh sách tài khoản",
            "Quản lý quyền đăng nhập, vai trò, trạng thái và mật khẩu. Tạo mới và cập nhật hồ sơ tại mục Nhân viên.",
            el("div", {}, [
                searchForm,
                rows.length
                    ? table(["ĐĂNG NHẬP", "HỒ SƠ LIÊN KẾT", "VAI TRÒ", "TRẠNG THÁI", "NGÀY TẠO", "THAO TÁC"], rows)
                    : empty("Không có tài khoản phù hợp", "Thử thay đổi từ khóa, vai trò hoặc trạng thái."),
                pager(data.page, data.total, data.pageSize, (p) => task(() => loadUsers(filters, p)))
            ])
        )
    );
}

async function editUser(user) {
    const view = state.view;
    const generation = state.viewGeneration;
    const lookup = await lookups();
    if (state.view !== view || state.viewGeneration !== generation || !canView(view)) {
        return;
    }
    const roleChoices = lookup.roles
        .filter(
            (x) =>
                (user?.role === "CUSTOMER" ? x.name === "CUSTOMER" : x.name !== "CUSTOMER") &&
                (isAdmin() || x.name !== "ADMIN")
        )
        .map((x) => ({ value: x.id, label: roleLabels[x.name] || x.name }));
    const statusChoices = lookup.statuses.map((x) => ({
        value: x.id,
        label: statusLabels[x.name] || x.name
    }));
    modal(
        user ? "Chỉnh sửa tài khoản" : "Tạo tài khoản nhân viên",
        [
            user
                ? el("div", { class: "field full" }, [
                      el("span", { text: "Tên đăng nhập" }),
                      el("strong", { text: `${user.username} · UserID #${user.userId}` }),
                      el("small", { text: "Thông tin liên hệ và họ tên được cập nhật tại hồ sơ nhân viên." })
                  ])
                : field("username", "Tên đăng nhập", "text", "", {
                      required: true,
                      minlength: 3,
                      maxlength: 50,
                      help: "3–50 ký tự: chữ, số, dấu chấm, gạch dưới hoặc gạch ngang."
                  }),
            user
                ? null
                : field("employeeName", "Họ tên nhân viên", "text", user?.employeeName || "", {
                      required: true,
                      maxlength: 100
                  }),
            user ? null : field("email", "Email", "email", "", { required: true, maxlength: 100 }),
            user ? null : field("phone", "Số điện thoại", "tel", "", {
                required: true,
                maxlength: 20,
                help: "8–15 chữ số; có thể bắt đầu bằng +."
            }),
            field(
                "roleId",
                "Vai trò",
                "select",
                user?.roleId || roleChoices.find((x) => x.label === roleLabels.SALES)?.value || roleChoices[0]?.value,
                { choices: roleChoices, required: true }
            ),
            field("statusId", "Trạng thái", "select", user?.statusId || statusChoices[0]?.value, {
                choices: statusChoices,
                required: true
            }),
            field("password", user ? "Mật khẩu mới (nếu cần)" : "Mật khẩu khởi tạo", "password", "", {
                required: !user,
                minlength: 10,
                maxlength: 128,
                full: true,
                autocomplete: "new-password",
                help: user
                    ? "Để trống để giữ mật khẩu hiện tại."
                    : "Tối thiểu 10 ký tự; nên dùng chữ, số và ký tự đặc biệt."
            })
        ].filter(Boolean),
        async (input) => {
            input.roleId = Number(input.roleId);
            input.statusId = Number(input.statusId);
            if (!input.password) {
                delete input.password;
            }
            await api(user ? `admin/users/${user.userId}` : "admin/users", {
                method: user ? "PUT" : "POST",
                body: input
            });
            await refreshAdminAfterSave(
                () => (state.view === "employees" ? loadEmployees() : loadUsers()),
                user ? "Đã cập nhật tài khoản." : "Đã tạo tài khoản nhân viên."
            );
        },
        user ? "Lưu quyền đăng nhập" : "Tạo nhân viên và tài khoản"
    );
}

async function loadEmployees(filters = state.employeeFilters) {
    if (state.view !== "employees" || !canView("employees")) {
        return;
    }
    const token = beginViewLoad("employees");
    filters = { ...filters };
    const data = await readViewData(token, () => api("admin/employees"));
    if (!isViewCurrent(token) || !canView("employees")) {
        return;
    }
    notice("");
    state.employeeFilters = { ...filters };
    $("#page-actions").replaceChildren(button("+ Thêm nhân viên", () => task(() => editUser()), "button"));
    const keyword = searchText(filters.q);
    const items = data.filter((employee) => {
        const text = searchText([employee.employeeName, employee.username, employee.email, employee.phone].join(" "));
        return (
            (!keyword || text.includes(keyword)) &&
            (!filters.role || employee.role === filters.role) &&
            (!filters.status || employee.status === filters.status)
        );
    });
    const roles = [...new Set(data.map((employee) => employee.role))];
    const statuses = [...new Set(data.map((employee) => employee.status))];
    const searchNode = field("q", "Tìm nhân viên", "search", filters.q || "", {
        help: "Họ tên, tài khoản, email hoặc điện thoại"
    });
    searchNode.classList.add("search-field");
    const form = el(
        "form",
        {
            class: "toolbar",
            onsubmit: (event) => {
                event.preventDefault();
                task(() => loadEmployees(values(form)));
            }
        },
        [
            searchNode,
            field("role", "Vai trò", "select", filters.role || "", {
                choices: [
                    { value: "", label: "Tất cả" },
                    ...roles.map((role) => ({ value: role, label: roleLabels[role] || role }))
                ]
            }),
            field("status", "Trạng thái", "select", filters.status || "", {
                choices: [
                    { value: "", label: "Tất cả" },
                    ...statuses.map((status) => ({ value: status, label: statusLabels[status] || status }))
                ]
            }),
            el("button", { type: "submit", class: "button secondary", text: "Lọc nhân viên" }),
            button("Xóa bộ lọc", () => task(() => loadEmployees({})), "text-button")
        ]
    );
    const rows = items.map((employee) => [
        cellTitle(employee.employeeName, `EmployeeID #${employee.employeeId}`),
        cellTitle(employee.email, employee.phone),
        cellTitle(`@${employee.username}`, `UserID #${employee.userId}`),
        employee.role === "ADMIN" && !isAdmin()
            ? pill("Được bảo vệ", "neutral")
            : button("Chỉnh sửa", () => editEmployee(employee), "text-button")
    ]);
    $("#content").replaceChildren(
        card(
            "Đội ngũ showroom",
            `${number(items.length)}/${number(data.length)} nhân viên · Tạo nhân viên mới cùng với tài khoản đăng nhập.`,
            el("div", {}, [
                form,
                rows.length
                    ? table(["NHÂN VIÊN", "LIÊN HỆ", "TÀI KHOẢN LIÊN KẾT", "THAO TÁC"], rows)
                    : empty("Không có nhân viên phù hợp", "Thay đổi bộ lọc hoặc tạo tài khoản nhân viên mới.")
            ])
        )
    );
}

function editEmployee(employee) {
    modal(
        "Cập nhật nhân viên",
        [
            el("p", { class: "field full section-note", text: `EmployeeID #${employee.employeeId} liên kết UserID #${employee.userId} · @${employee.username}. Email và số điện thoại được đồng bộ với tài khoản.` }),
            field("employeeName", "Họ và tên", "text", employee.employeeName, {
                required: true,
                full: true,
                maxlength: 100
            }),
            field("email", "Email", "email", employee.email || "", { required: true, maxlength: 100 }),
            field("phone", "Số điện thoại", "tel", employee.phone || "", {
                required: true,
                maxlength: 20
            })
        ],
        async (input) => {
            await api(`admin/employees/${employee.employeeId}`, { method: "PUT", body: input });
            await refreshAdminAfterSave(loadEmployees, "Đã cập nhật thông tin nhân viên.");
        }
    );
}

function promotionPhase(promotion, now = new Date()) {
    if (promotion.status !== "ACTIVE") {
        return "INACTIVE";
    }
    // DATETIME2 trong database lưu giờ Việt Nam, nên phải gắn múi giờ trước khi so sánh.
    const start = businessDate(promotion.startDate);
    const end = businessDate(promotion.endDate);
    if (!start || !end) {
        return "UNKNOWN";
    }
    if (now < start) {
        return "SCHEDULED";
    }
    if (now > end) {
        return "EXPIRED";
    }
    return "RUNNING";
}

function promotionPill(promotion) {
    const phase = promotionPhase(promotion);
    const labels = {
        SCHEDULED: "Sắp diễn ra",
        RUNNING: "Đang áp dụng",
        EXPIRED: "Đã hết hạn",
        INACTIVE: "Đã ngừng",
        UNKNOWN: "Chưa rõ thời gian"
    };
    return pill(labels[phase], phase === "RUNNING" ? "" : phase === "SCHEDULED" ? "warning" : "neutral");
}

async function loadPromotions(filters = state.promotionFilters) {
    if (state.view !== "promotions" || !canView("promotions")) {
        return;
    }
    const token = beginViewLoad("promotions");
    filters = { ...filters };
    const result = await readViewData(token, () => Promise.all([api("admin/promotions"), lookups()]));
    if (!isViewCurrent(token) || !canView("promotions")) {
        return;
    }
    const [data, lookup] = result;
    notice("");
    state.promotionFilters = { ...filters };
    $("#page-actions").replaceChildren(button("+ Tạo khuyến mãi", () => task(() => editPromotion()), "button"));
    const names = new Map(lookup.models.map((m) => [m.id, m.name]));
    const keyword = searchText(filters.q);
    const items = data.filter((promotion) => {
        const text = searchText(`${promotion.promotionName} ${promotion.description || ""}`);
        return (!keyword || text.includes(keyword)) && (!filters.phase || promotionPhase(promotion) === filters.phase);
    });
    const searchNode = field("q", "Tìm chương trình", "search", filters.q || "");
    searchNode.classList.add("search-field");
    const form = el(
        "form",
        {
            class: "toolbar",
            onsubmit: (event) => {
                event.preventDefault();
                task(() => loadPromotions(values(form)));
            }
        },
        [
            searchNode,
            field("phase", "Hiệu lực", "select", filters.phase || "", {
                choices: [
                    { value: "", label: "Tất cả chương trình" },
                    { value: "SCHEDULED", label: "Sắp diễn ra" },
                    { value: "RUNNING", label: "Đang áp dụng" },
                    { value: "EXPIRED", label: "Đã hết hạn" },
                    { value: "INACTIVE", label: "Đã ngừng" }
                ]
            }),
            el("button", { type: "submit", class: "button secondary", text: "Lọc chương trình" }),
            button("Xóa bộ lọc", () => task(() => loadPromotions({})), "text-button")
        ]
    );
    const rows = items.map((promotion) => [
        cellTitle(promotion.promotionName, promotion.description),
        promotion.discountType === "PERCENTAGE"
            ? `${number(promotion.discountValue)}%`
            : money(promotion.discountValue),
        cellTitle(date(promotion.startDate), `Đến ${date(promotion.endDate)}`),
        el("div", {
            text: (promotion.modelIds || []).map((id) => names.get(id) || `#${id}`).join(", ") || "Chưa chọn mẫu xe"
        }),
        promotionPill(promotion),
        el("div", { class: "actions" }, [
            button("Sửa", () => task(() => editPromotion(promotion)), "text-button"),
            promotion.status === "ACTIVE"
                ? button("Ngừng", () => deactivatePromotion(promotion), "text-button danger")
                : null
        ])
    ]);
    $("#content").replaceChildren(
        card(
            "Chương trình khuyến mãi",
            `${number(items.length)}/${number(data.length)} chương trình · Hiệu lực đối chiếu theo giờ Việt Nam khi tải dữ liệu.`,
            el("div", {}, [
                form,
                rows.length
                    ? table(["CHƯƠNG TRÌNH", "ƯU ĐÃI", "THỜI GIAN", "MẪU XE ÁP DỤNG", "HIỆU LỰC", "THAO TÁC"], rows)
                    : empty("Không có chương trình phù hợp", "Thử đổi bộ lọc hoặc tạo chương trình mới.")
            ])
        )
    );
}

function localDatetime(value) {
    if (!value) {
        return "";
    }
    return String(value).slice(0, 16);
}

// Match LocalDateTime.plusMonths: preserve the time and clamp to the target month's last day.
function promotionEndLimit(value) {
    const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/.exec(String(value));
    if (!match) return "";
    const [year, month, day, hour, minute] = match.slice(1).map(Number);
    const days = (y, m) => [31, y % 4 === 0 && (y % 100 !== 0 || y % 400 === 0) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][m - 1];
    if (year < 1 || month < 1 || month > 12 || day < 1 || day > days(year, month) || hour > 23 || minute > 59) return "";
    const index = year * 12 + month - 1 + 3;
    const targetYear = Math.floor(index / 12);
    const targetMonth = index % 12 + 1;
    if (targetYear > 9999) return "9999-12-31T23:59";
    return `${String(targetYear).padStart(4, "0")}-${String(targetMonth).padStart(2, "0")}-${String(Math.min(day, days(targetYear, targetMonth))).padStart(2, "0")}T${match[4]}:${match[5]}`;
}

function promotionCents(value) {
    const match = /^(\d+)(?:\.(\d+))?$/.exec(String(value));
    if (!match || (match[2] || "").replace(/0+$/, "").length > 2) return null;
    return BigInt(match[1]) * 100n + BigInt(((match[2] || "") + "00").slice(0, 2));
}

function promotionValidation(input, modelMinPrices = {}) {
    const errors = {};
    const value = promotionCents(input.discountValue);
    if (value === null || value <= 0n || value > 999999999999999999n) {
        errors.discountValue = "Nhập mức giảm lớn hơn 0, tối đa 2 chữ số thập phân.";
    } else if (input.discountType === "PERCENTAGE" && value > 10000n) {
        errors.discountValue = "Phần trăm giảm giá không được vượt quá 100%.";
    } else if (input.discountType === "FIXED") {
        for (const id of input.modelIds || []) {
            const price = modelMinPrices[id] == null ? null : promotionCents(modelMinPrices[id]);
            if (price !== null && value > price) {
                errors.discountValue = `Mức giảm không được vượt giá xe thấp nhất của mẫu đã chọn (${money(modelMinPrices[id])}).`;
                break;
            }
        }
    }
    const limit = promotionEndLimit(input.startDate);
    if (!limit) errors.startDate = "Nhập ngày và giờ bắt đầu hợp lệ.";
    if (!promotionEndLimit(input.endDate)) errors.endDate = "Nhập ngày và giờ kết thúc hợp lệ.";
    else if (limit && input.endDate <= input.startDate) errors.endDate = "Ngày kết thúc phải sau ngày bắt đầu.";
    else if (limit && input.endDate > limit) errors.endDate = "Thời gian khuyến mãi tối đa 3 tháng lịch kể từ ngày bắt đầu.";
    if (!input.modelIds?.length) errors.modelIds = "Chọn ít nhất một mẫu xe áp dụng.";
    return errors;
}

function configurePromotionForm(form, lookup) {
    const names = ["discountValue", "discountType", "startDate", "endDate", "modelIds"];
    const controls = Object.fromEntries(names.map((name) => [name, $(`[name=${name}]`, form)]));
    const messages = {};
    for (const name of ["discountValue", "startDate", "endDate", "modelIds"]) {
        const message = el("small", { class: "form-error", id: `promotion-${name}-error`, "aria-live": "polite", hidden: true });
        controls[name].parentElement.append(message);
        controls[name].setAttribute("aria-describedby", message.id);
        messages[name] = message;
    }
    const check = () => {
        const input = Object.fromEntries(names.map((name) => [name, controls[name].value]));
        input.modelIds = [...controls.modelIds.selectedOptions].map((option) => Number(option.value));
        const errors = promotionValidation(input, lookup.modelMinPrices);
        controls.discountValue.min = "0.01";
        const selectedPrices = input.modelIds.map((id) => lookup.modelMinPrices?.[id]).filter((price) => price != null);
        controls.discountValue.max = input.discountType === "PERCENTAGE" ? "100" : selectedPrices.length ? String(Math.min(...selectedPrices.map(Number))) : "9999999999999999.99";
        controls.endDate.min = controls.startDate.value;
        controls.endDate.max = promotionEndLimit(controls.startDate.value) || "9999-12-31T23:59";
        for (const [name, message] of Object.entries(messages)) {
            message.textContent = errors[name] || "";
            message.hidden = !errors[name];
            controls[name].setCustomValidity(errors[name] || "");
            controls[name].setAttribute("aria-invalid", String(Boolean(errors[name])));
        }
        return errors;
    };
    for (const control of Object.values(controls)) {
        control.addEventListener("input", check);
        control.addEventListener("change", check);
    }
    check();
    return check;
}

async function editPromotion(promotion) {
    const generation = state.viewGeneration;
    const lookup = await lookups();
    if (state.view !== "promotions" || state.viewGeneration !== generation || !canView("promotions")) {
        return;
    }
    modal(
        promotion ? "Chỉnh sửa khuyến mãi" : "Tạo chương trình khuyến mãi",
        [
            field("promotionName", "Tên chương trình", "text", promotion?.promotionName || "", {
                required: true,
                full: true,
                maxlength: 150
            }),
            field("description", "Mô tả", "textarea", promotion?.description || "", { full: true }),
            field("discountType", "Hình thức ưu đãi", "select", promotion?.discountType || "PERCENTAGE", {
                choices: [
                    { value: "PERCENTAGE", label: "Giảm theo phần trăm (%)" },
                    { value: "FIXED", label: "Giảm số tiền (VND)" }
                ],
                required: true
            }),
            field("discountValue", "Giá trị ưu đãi", "number", promotion?.discountValue ?? "", {
                required: true,
                min: "0.01",
                step: "0.01"
            }),
            field("startDate", "Bắt đầu", "datetime-local", localDatetime(promotion?.startDate), {
                required: true
            }),
            field("endDate", "Kết thúc", "datetime-local", localDatetime(promotion?.endDate), {
                required: true,
                help: "Sau ngày bắt đầu, tối đa 3 tháng lịch; ngày cuối tháng được giữ trong tháng đích."
            }),
            field("status", "Trạng thái", "select", promotion?.status || "ACTIVE", {
                required: true,
                choices: [
                    { value: "ACTIVE", label: "Hoạt động" },
                    { value: "INACTIVE", label: "Ngừng hoạt động" }
                ]
            }),
            field("modelIds", "Mẫu xe áp dụng", "select", promotion?.modelIds || [], {
                choices: lookup.models.map((m) => ({ value: m.id, label: m.name })),
                multiple: true,
                required: true,
                full: true,
                help: "Giữ Ctrl (Windows) để chọn nhiều mẫu xe."
            })
        ],
        async (input, form) => {
            input.modelIds = [...$("[name=modelIds]", form).selectedOptions].map((o) => Number(o.value));
            const errors = promotionValidation(input, lookup.modelMinPrices);
            if (Object.keys(errors).length) {
                throw new Error(Object.values(errors)[0]);
            }
            try {
                await api(promotion ? `admin/promotions/${promotion.promotionId}` : "admin/promotions", {
                    method: promotion ? "PUT" : "POST",
                    body: input
                });
            } catch (error) {
                if (/giảm giá|giá trị|thập phân/i.test(error.message)) {
                    const message = $("#promotion-discountValue-error", form);
                    message.textContent = error.message;
                    message.hidden = false;
                    $("[name=discountValue]", form).setAttribute("aria-invalid", "true");
                }
                throw error;
            }
            await refreshAdminAfterSave(loadPromotions, "Đã lưu chương trình khuyến mãi.");
        }
    );
    configurePromotionForm($("#dialog-form"), lookup);
}

function deactivatePromotion(promotion) {
    modal(
        "Ngừng chương trình",
        [
            el("p", {
                class: "field full",
                text: `Ngừng chương trình “${promotion.promotionName}”? Thông tin chương trình được giữ lại để theo dõi.`
            })
        ],
        async () => {
            await api(`admin/promotions/${promotion.promotionId}`, { method: "DELETE" });
            await refreshAdminAfterSave(loadPromotions, "Đã ngừng chương trình khuyến mãi.");
        },
        "Ngừng chương trình"
    );
}

async function loadLogs(filters = state.logFilters, page = state.logPage) {
    if (state.view !== "logs" || !canView("logs")) {
        return;
    }
    const token = beginViewLoad("logs");
    filters = { ...filters };
    if (filters.from && filters.to && filters.from > filters.to) {
        throw new Error("Ngày bắt đầu phải bằng hoặc trước ngày kết thúc.");
    }
    const data = await readViewData(token, () => api(`admin/logs?${query({ ...filters, page })}`));
    if (!isViewCurrent(token) || !canView("logs")) {
        return;
    }
    const lastPage = Math.max(1, Math.ceil(data.total / data.pageSize));
    if (data.page > lastPage) {
        return loadLogs(filters, lastPage);
    }
    notice("");
    state.logFilters = { ...filters };
    state.logPage = data.page;
    $("#page-actions").replaceChildren(
        button("Làm mới", () => task(() => loadLogs()), "button secondary"),
        el("a", {
            class: "button secondary",
            text: "↓ Xuất CSV",
            href: appUrl(`api/admin/logs?${query({ ...filters, format: "csv" })}`)
        })
    );
    const form = el(
        "form",
        {
            class: "toolbar",
            onsubmit: (event) => {
                event.preventDefault();
                task(() => loadLogs(values(form), 1));
            }
        },
        [
            field("from", "Từ ngày", "date", filters.from || ""),
            field("to", "Đến ngày", "date", filters.to || ""),
            field("userId", "Mã người dùng", "number", filters.userId || "", { min: 1, step: 1 }),
            field("action", "Thao tác", "text", filters.action || ""),
            field("targetType", "Module", "text", filters.targetType || ""),
            el("button", { type: "submit", class: "button secondary", text: "Lọc nhật ký" }),
            button("Xóa bộ lọc", () => task(() => loadLogs({}, 1)), "text-button")
        ]
    );
    const rows = data.items.map((log) => [
        date(log.createdDate),
        cellTitle(log.username || "Khách", log.userId ? `#${log.userId}` : null),
        pill(log.action, "neutral"),
        cellTitle(log.targetType, log.targetId ? `#${log.targetId}` : null),
        el("details", {}, [
            el("summary", { text: `Xem nhật ký #${log.logId}` }),
            el("p", {
                text: log.detail || "Không có nội dung chi tiết.",
                style: "white-space:pre-wrap;min-width:240px;max-width:480px;overflow-wrap:anywhere;"
            })
        ])
    ]);
    $("#content").replaceChildren(
        card(
            "Nhật ký hoạt động",
            "Nhật ký được lưu để truy vết. Nội dung không thể sửa hoặc xóa từ giao diện.",
            el("div", {}, [
                form,
                rows.length
                    ? table(["THỜI GIAN", "NGƯỜI THỰC HIỆN", "THAO TÁC", "ĐỐI TƯỢNG", "CHI TIẾT"], rows)
                    : empty("Không có nhật ký phù hợp", "Thử thay đổi thời gian hoặc bộ lọc."),
                pager(data.page, data.total, data.pageSize, (p) => task(() => loadLogs(filters, p)))
            ])
        )
    );
}

// Report and AI views use their dedicated, read-only business projections.
async function loadRoles() {
    if (state.view !== "roles" || !canView("roles")) {
        return;
    }
    const token = beginViewLoad("roles");
    const data = await readViewData(token, () => api("admin/roles"));
    if (!isViewCurrent(token) || !canView("roles")) {
        return;
    }
    notice("");
    $("#page-actions").replaceChildren(button("+ Tạo vai trò", () => createRole(data), "button"));
    const descriptions = new Map(data.permissions.map((p) => [p.code, p.description]));
    const cards = data.roles.map((role) =>
        el("article", { class: "role-card" }, [
            el("h3", {}, [
                el("span", { text: roleLabels[role.roleName] || role.roleName }),
                pill(role.roleName, "neutral")
            ]),
            el("p", {
                text: role.permissions.length
                    ? role.permissions.map((p) => descriptions.get(p) || p).join(" · ")
                    : "Chưa được cấp quyền quản trị hoặc báo cáo."
            }),
            ["ADMIN", "CUSTOMER"].includes(role.roleName)
                ? el("p", {
                      text:
                          role.roleName === "ADMIN"
                              ? "Vai trò quản trị mặc định được bảo vệ để duy trì quyền truy cập."
                              : "Khách hàng sử dụng danh mục và trợ lý xe; không được cấp quyền nội bộ."
                  })
                : button("Thiết lập quyền", () => editRole(role, data.permissions), "button secondary small")
        ])
    );
    $("#content").replaceChildren(
        card(
            "Vai trò & quyền truy cập",
            "Thay đổi quyền được áp dụng cho tài khoản của vai trò trong những yêu cầu tiếp theo.",
            el("div", { class: "card-body role-grid" }, cards)
        )
    );
}

function createRole(data, source) {
    const checks = data.permissions.map((p) =>
        el("label", { class: "checkbox-row" }, [
            el("input", {
                type: "checkbox",
                name: "permissions",
                value: p.code,
                checked: source?.permissions.includes(p.code) || false
            }),
            el("span", { text: p.description })
        ])
    );
    modal(
        source ? `Sao chép vai trò ${source.roleName}` : "Tạo vai trò mới",
        [
            field("roleName", "Tên vai trò", "text", "", {
                required: true,
                minlength: 3,
                maxlength: 20,
                full: true,
                help: "3–20 chữ IN HOA, số hoặc gạch dưới; bắt đầu bằng chữ."
            }),
            field("cloneRoleId", "Sao chép quyền từ vai trò", "select", source?.roleId || "", {
                full: true,
                choices: [
                    { value: "", label: "Thiết lập quyền bên dưới" },
                    ...data.roles
                        .filter((r) => !["ADMIN", "CUSTOMER"].includes(r.roleName))
                        .map((r) => ({ value: r.roleId, label: r.roleName }))
                ],
                help: "Nếu chọn vai trò gốc, hệ thống sao chép toàn bộ quyền từ vai trò đó."
            }),
            el("div", { class: "field full checkbox-list" }, checks)
        ],
        async (input, form) => {
            await api("admin/roles", {
                method: "POST",
                body: {
                    roleName: input.roleName,
                    permissions: [...form.querySelectorAll("input[name=permissions]:checked")].map((n) => n.value),
                    ...(input.cloneRoleId ? { cloneRoleId: Number(input.cloneRoleId) } : {})
                }
            });
            state.lookups = null;
            await refreshAdminAfterSave(loadRoles, "Đã tạo vai trò mới.");
        },
        "Tạo vai trò"
    );
}

async function loadConfigurations() {
    if (state.view !== "configurations" || !canView("configurations")) {
        return;
    }
    const token = beginViewLoad("configurations");
    const data = await readViewData(token, () => api("admin/configurations"));
    if (!isViewCurrent(token) || !canView("configurations")) {
        return;
    }
    notice("");
    const rows = data.map((c) => [
        cellTitle(c.name, c.description),
        `${c.value} ${c.key === "SESSION_TIMEOUT_MINUTES" ? "phút" : "ngày"}`,
        `${c.min}–${c.max}`,
        date(c.updatedDate),
        button(
            "Chỉnh sửa",
            () => {
                modal(
                    c.name,
                    [
                        field("value", "Giá trị mới", "number", c.value, {
                            required: true,
                            min: c.min,
                            max: c.max,
                            step: 1,
                            full: true
                        })
                    ],
                    async (input) => {
                        await api(`admin/configurations/${c.key}`, {
                            method: "PUT",
                            body: { value: input.value }
                        });
                        await refreshAdminAfterSave(loadConfigurations, "Đã cập nhật cấu hình.");
                    }
                );
            },
            "text-button"
        )
    ]);
    $("#content").replaceChildren(
        card(
            "Cấu hình vận hành",
            "Thay đổi có hiệu lực ở yêu cầu tiếp theo và được ghi nhật ký.",
            table(["CẤU HÌNH", "GIÁ TRỊ", "PHẠM VI", "CẬP NHẬT", "THAO TÁC"], rows)
        )
    );
}

function editRole(role, permissions) {
    const checks = permissions.map((p) =>
        el("label", { class: "checkbox-row" }, [
            el("input", {
                type: "checkbox",
                name: "permissions",
                value: p.code,
                checked: role.permissions.includes(p.code)
            }),
            el("span", { text: p.description || p.code })
        ])
    );
    modal(
        `Phân quyền: ${roleLabels[role.roleName] || role.roleName}`,
        [el("div", { class: "field full checkbox-list" }, checks)],
        async (_, form) => {
            await api(`admin/roles/${role.roleId}`, {
                method: "PUT",
                body: {
                    permissions: [...form.querySelectorAll("input[name=permissions]:checked")].map((n) => n.value)
                }
            });
            await refreshAdminAfterSave(loadRoles, "Đã cập nhật quyền của vai trò.");
        }
    );
}
