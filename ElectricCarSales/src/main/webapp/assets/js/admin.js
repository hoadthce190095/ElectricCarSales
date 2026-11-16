"use strict";

async function loadUsers(filters = state.userFilters, page = state.userPage) {
    const [data, lookup] = await Promise.all([api(`admin/users?${query({ ...filters, page })}`), lookups()]);
    if (state.view !== "users") {
        return;
    }
    const lastPage = Math.max(1, Math.ceil(data.total / data.pageSize));
    if (data.page > lastPage) {
        return loadUsers(filters, lastPage);
    }
    state.userFilters = { ...filters };
    state.userPage = data.page;
    $("#page-actions").replaceChildren(button("+ Tạo tài khoản", () => task(() => editUser()), "button"));
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
        cellTitle(u.employeeName || u.username, `@${u.username} · #${u.userId}`),
        cellTitle(u.email, u.phone),
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
            "Mỗi tài khoản được gắn với một vai trò và một trạng thái truy cập.",
            el("div", {}, [
                searchForm,
                rows.length
                    ? table(["TÀI KHOẢN", "LIÊN HỆ", "VAI TRÒ", "TRẠNG THÁI", "NGÀY TẠO", "THAO TÁC"], rows)
                    : empty("Không có tài khoản phù hợp", "Thử thay đổi từ khóa, vai trò hoặc trạng thái."),
                pager(data.page, data.total, data.pageSize, (p) => task(() => loadUsers(filters, p)))
            ])
        )
    );
}

async function editUser(user) {
    const lookup = await lookups();
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
                    el("strong", { text: user.username })
                ])
                : field("username", "Tên đăng nhập", "text", "", {
                    required: true,
                    minlength: 3,
                    maxlength: 50,
                    help: "3–50 ký tự: chữ, số, dấu chấm, gạch dưới hoặc gạch ngang."
                }),
            user?.role === "CUSTOMER"
                ? null
                : field("employeeName", "Họ tên nhân viên", "text", user?.employeeName || "", {
                    required: true,
                    maxlength: 100
                }),
            field("email", "Email", "email", user?.email || "", { required: true, maxlength: 100 }),
            field("phone", "Số điện thoại", "tel", user?.phone || "", {
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
            user
                ? null
                : field("password", "Mật khẩu khởi tạo", "password", "", {
                      required: true,
                      minlength: 10,
                      maxlength: 128,
                      full: true,
                      autocomplete: "new-password",
                      help: "Tối thiểu 10 ký tự; nên dùng chữ, số và ký tự đặc biệt."
                  })
        ].filter(Boolean),
        async (input) => {
            input.roleId = Number(input.roleId);
            input.statusId = Number(input.statusId);
            if (user) {
                delete input.password;
            } else if (!input.password) {
                delete input.password;
            }
            await api(user ? `admin/users/${user.userId}` : "admin/users", {
                method: user ? "PUT" : "POST",
                body: input
            });
            await (state.view === "employees" ? loadEmployees() : loadUsers());
            toast(user ? "Đã cập nhật tài khoản." : "Đã tạo tài khoản nhân viên.");
        },
        user ? "Lưu thay đổi" : "Tạo tài khoản"
    );
}

async function loadEmployees(filters = state.employeeFilters) {
    const data = await api("admin/employees");
    if (state.view !== "employees") {
        return;
    }
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
        cellTitle(employee.employeeName, `#${employee.employeeId} · @${employee.username}`),
        cellTitle(employee.email, employee.phone),
        pill(employee.role, "neutral"),
        pill(employee.status),
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
                    ? table(["NHÂN VIÊN", "LIÊN HỆ", "VAI TRÒ", "TÀI KHOẢN", "THAO TÁC"], rows)
                    : empty("Không có nhân viên phù hợp", "Thay đổi bộ lọc hoặc tạo tài khoản nhân viên mới.")
            ])
        )
    );
}

function editEmployee(employee) {
    modal(
        "Cập nhật nhân viên",
        [
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
            await loadEmployees();
            toast("Đã cập nhật thông tin nhân viên.");
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
    const data = await api("admin/promotions");
    if (state.view !== "promotions") {
        return;
    }
    state.promotionFilters = { ...filters };
    $("#page-actions").replaceChildren(button("+ Tạo khuyến mãi", () => task(() => editPromotion()), "button"));
    const lookup = await lookups();
    if (state.view !== "promotions") {
        return;
    }
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
            button("Tính thử", () => previewPromotion(promotion), "text-button"),
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

function previewPromotion(promotion) {
    const result = el("div", { class: "field full promotion-preview-result", "aria-live": "polite" });
    const priceField = field("basePrice", "Giá xe để tính thử (VND)", "number", "", {
        required: true,
        full: true,
        min: 0,
        step: 1000,
        help: "Nhập giá tham khảo của một mẫu xe thuộc chương trình."
    });
    const input = $("input", priceField);
    input.addEventListener("input", () => {
        if (input.value === "") {
            result.replaceChildren(el("p", { text: "Nhập giá xe để xem mức giảm và giá sau ưu đãi." }));
            return;
        }
        const basePrice = Number(input.value);
        if (!Number.isFinite(basePrice) || basePrice < 0) {
            result.replaceChildren(el("p", { text: "Giá xe phải là số không âm." }));
            return;
        }
        const calculatedDiscount =
            promotion.discountType === "PERCENTAGE"
                ? (basePrice * Number(promotion.discountValue)) / 100
                : Number(promotion.discountValue);
        // Ưu đãi cố định lớn hơn giá xe cũng không tạo ra giá bán âm.
        const discount = Math.min(basePrice, Math.max(0, calculatedDiscount));
        result.replaceChildren(
            el("p", { text: `Mức giảm mô phỏng: ${money(discount)}` }),
            el("strong", { text: `Sau ưu đãi: ${money(basePrice - discount)}` })
        );
    });
    result.append(el("p", { text: "Nhập giá xe để xem mức giảm và giá sau ưu đãi." }));
    modal(
        "Tính thử ưu đãi",
        [
            el("div", { class: "field full" }, [
                el("strong", { text: promotion.promotionName }),
                promotionPill(promotion)
            ]),
            priceField,
            result,
            el("p", {
                class: "field full section-note",
                text: "Đây là phép tính mô phỏng, chưa tính thuế và chi phí lăn bánh. Không tạo giao dịch, không áp dụng ưu đãi vào đơn hàng. Chương trình hết hạn hoặc đã ngừng chỉ được xem để đối chiếu."
            })
        ],
        async () => {},
        "Đóng"
    );
    input.required = false;
}

function localDatetime(value) {
    if (!value) {
        return "";
    }
    return String(value).slice(0, 16);
}

async function editPromotion(promotion) {
    const lookup = await lookups();
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
                min: 0,
                step: "0.01"
            }),
            field("startDate", "Bắt đầu", "datetime-local", localDatetime(promotion?.startDate), {
                required: true
            }),
            field("endDate", "Kết thúc", "datetime-local", localDatetime(promotion?.endDate), {
                required: true
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
                full: true,
                help: "Giữ Ctrl (Windows) để chọn nhiều mẫu xe."
            })
        ],
        async (input, form) => {
            input.discountValue = Number(input.discountValue);
            input.modelIds = [...$("[name=modelIds]", form).selectedOptions].map((o) => Number(o.value));
            if (!input.modelIds.length) {
                throw new Error("Chọn ít nhất một mẫu xe cho chương trình.");
            }
            if (input.discountType === "PERCENTAGE" && input.discountValue > 100) {
                throw new Error("Ưu đãi phần trăm phải nằm trong khoảng 0–100.");
            }
            if (input.endDate < input.startDate) {
                throw new Error("Thời gian kết thúc phải bằng hoặc sau thời gian bắt đầu.");
            }
            await api(promotion ? `admin/promotions/${promotion.promotionId}` : "admin/promotions", {
                method: promotion ? "PUT" : "POST",
                body: input
            });
            await loadPromotions();
            toast("Đã lưu chương trình khuyến mãi.");
        }
    );
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
            await loadPromotions();
            toast("Đã ngừng chương trình khuyến mãi.");
        },
        "Ngừng chương trình"
    );
}

async function loadLogs(filters = {}, page = 1) {
    const data = await api(`admin/logs?${query({ ...filters, page })}`);
    if (state.view !== "logs") {
        return;
    }
    $("#page-actions").replaceChildren(
        el("a", {
            class: "button secondary",
            text: "↓ Xuất CSV",
            href: `./api/admin/logs?${query({ ...filters, format: "csv" })}`
        })
    );
    const form = el(
        "form",
        {
            class: "toolbar",
            onsubmit: (event) => {
                event.preventDefault();
                task(() => loadLogs(values(form)));
            }
        },
        [
            field("from", "Từ ngày", "date", filters.from || ""),
            field("to", "Đến ngày", "date", filters.to || ""),
            field("userId", "Mã người dùng", "number", filters.userId || "", { min: 1 }),
            field("action", "Thao tác", "text", filters.action || ""),
            field("targetType", "Module", "text", filters.targetType || ""),
            el("button", { type: "submit", class: "button secondary", text: "Lọc nhật ký" })
        ]
    );
    const rows = data.items.map((log) => [
        date(log.createdDate),
        cellTitle(log.username || "Khách", log.userId ? `#${log.userId}` : null),
        pill(log.action, "neutral"),
        cellTitle(log.targetType, log.targetId ? `#${log.targetId}` : null),
        el("div", {
            text: log.detail || "—",
            style: "white-space:pre-wrap;min-width:240px;max-width:480px;overflow-wrap:anywhere;"
        })
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
    const data = state.demoMode
        ? {
              permissions: [
                  { code: "MANAGE_USERS", description: "Quản lý tài khoản và nhân viên" },
                  { code: "MANAGE_PROMOTIONS", description: "Quản lý khuyến mãi" },
                  { code: "VIEW_REPORTS", description: "Xem và xuất báo cáo tài chính, kinh doanh" },
                  { code: "VIEW_LOGS", description: "Xem nhật ký hệ thống" }
              ],
              roles: [
                  { roleId: 1, roleName: "CUSTOMER", permissions: [] },
                  { roleId: 2, roleName: "SALES", permissions: [] },
                  { roleId: 3, roleName: "OPERATIONS", permissions: [] },
                  { roleId: 4, roleName: "ACCOUNTANT", permissions: ["VIEW_REPORTS"] },
                  { roleId: 5, roleName: "MANAGER", permissions: ["VIEW_REPORTS"] },
                  {
                      roleId: 6,
                      roleName: "ADMIN",
                      permissions: ["MANAGE_USERS", "MANAGE_PROMOTIONS", "VIEW_REPORTS", "VIEW_LOGS"]
                  }
              ]
          }
        : await api("admin/roles");
    if (state.view !== "roles") {
        return;
    }
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
            await loadRoles();
            toast("Đã tạo vai trò mới.");
        },
        "Tạo vai trò"
    );
}

async function loadConfigurations() {
    const data = await api("admin/configurations");
    if (state.view !== "configurations") {
        return;
    }
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
                        await loadConfigurations();
                        toast("Đã cập nhật cấu hình.");
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
            await loadRoles();
            toast("Đã cập nhật quyền của vai trò.");
        }
    );
}
