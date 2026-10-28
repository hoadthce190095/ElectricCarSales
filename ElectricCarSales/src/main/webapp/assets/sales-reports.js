"use strict";

function isoToday() {
    const parts = new Intl.DateTimeFormat("en-CA", {
        timeZone: "Asia/Ho_Chi_Minh",
        year: "numeric",
        month: "2-digit",
        day: "2-digit"
    }).formatToParts(new Date());
    const value = (type) => parts.find((p) => p.type === type).value;
    return `${value("year")}-${value("month")}-${value("day")}`;
}

function lastSixMonths() {
    const today = isoToday();
    const firstMonth = new Date(Date.UTC(Number(today.slice(0, 4)), Number(today.slice(5, 7)) - 6, 1));
    return { from: firstMonth.toISOString().slice(0, 10), to: today };
}

function calendarDate(value) {
    const parts = String(value || "").split("-");
    return parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : "—";
}

function previousPeriod(period) {
    const start = new Date(`${period.from}T00:00:00Z`);
    const end = new Date(`${period.to}T00:00:00Z`);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || start > end) {
        return null;
    }
    const dayMilliseconds = 24 * 60 * 60 * 1000;
    const days = Math.round((end - start) / dayMilliseconds) + 1;
    // Hai kỳ có cùng số ngày và không chồng nhau, kể cả khi đi qua tháng nhuận.
    const previousEnd = new Date(start.getTime() - dayMilliseconds);
    const previousStart = new Date(start.getTime() - days * dayMilliseconds);
    if (previousStart.getUTCFullYear() < 1 || previousEnd.getUTCFullYear() > 9998) {
        return null;
    }
    return {
        from: previousStart.toISOString().slice(0, 10),
        to: previousEnd.toISOString().slice(0, 10)
    };
}

function metricChange(currentValue, previousValue) {
    const current = Number(currentValue) || 0;
    const previous = Number(previousValue) || 0;
    if (previous === 0) {
        return current === 0 ? "Không đổi (0%)" : "Kỳ trước bằng 0; chưa tính tỷ lệ";
    }
    const percentage = ((current - previous) / Math.abs(previous)) * 100;
    if (percentage === 0) {
        return "Không đổi (0%)";
    }
    const change = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(Math.abs(percentage));
    return `${percentage > 0 ? "Tăng" : "Giảm"} ${change}%`;
}

function reportComparison(current, previous, warning) {
    if (warning) {
        return el("div", { class: "notice report-comparison-warning", role: "status", text: warning });
    }
    if (!previous) {
        return null;
    }
    const metrics = [
        ["revenue", "Doanh thu thực thu", money],
        ["salesAmount", "Giá trị xe đã bán", money],
        ["vehiclesSold", "Số xe bán ra", number],
        ["totalOrders", "Tổng giao dịch", number]
    ];
    const rows = metrics.map(([key, label, format]) => [
        label,
        format(current.summary[key]),
        format(previous.summary[key]),
        metricChange(current.summary[key], previous.summary[key])
    ]);
    return card(
        "So sánh với kỳ liền trước",
        `Kỳ trước: ${calendarDate(previous.period.from)} – ${calendarDate(previous.period.to)}. Giữ cùng số ngày và cùng bộ lọc mẫu xe, nhân viên.`,
        table(["CHỈ SỐ", "KỲ ĐANG XEM", "KỲ TRƯỚC", "THAY ĐỔI"], rows)
    );
}

async function readDefaultReport(filters) {
    try {
        return { data: await api(`reports?${query(filters)}`), warning: "" };
    } catch (error) {
        if (error.status !== 400) {
            throw error;
        }
        // Cấu hình có thể giới hạn chỉ 1 ngày. Hôm nay luôn là khoảng mặc định nhỏ nhất.
        const today = isoToday();
        const data = await api(`reports?${query({ from: today, to: today })}`);
        return {
            data,
            warning: `${error.message} Đang hiển thị hôm nay; chọn lại khoảng ngày trong báo cáo nếu cần.`
        };
    }
}

function overviewPeriod(preset, today = isoToday()) {
    const year = today.slice(0, 4);
    const month = today.slice(5, 7);
    switch (preset) {
        case "TODAY":
            return { from: today, to: today };
        case "QUARTER": {
            const start = String(Math.floor((Number(month) - 1) / 3) * 3 + 1).padStart(2, "0");
            return { from: `${year}-${start}-01`, to: today };
        }
        case "YEAR":
            return { from: `${year}-01-01`, to: today };
        case "SIX_MONTHS": {
            const first = new Date(Date.UTC(Number(year), Number(month) - 6, 1));
            return { from: first.toISOString().slice(0, 10), to: today };
        }
        default:
            return { from: `${year}-${month}-01`, to: today };
    }
}

function overviewPeriodForm(period, preset) {
    const form = el(
        "form",
        {
            class: "overview-period-form",
            onsubmit: (event) => {
                event.preventDefault();
                const input = values(form);
                task(() => loadOverview({ from: input.from, to: input.to, preset: input.preset }));
            }
        },
        [
            field("preset", "Kỳ xem", "select", preset, {
                choices: [
                    { value: "MONTH", label: "Tháng này" },
                    { value: "TODAY", label: "Hôm nay" },
                    { value: "QUARTER", label: "Quý này" },
                    { value: "SIX_MONTHS", label: "6 tháng gần nhất" },
                    { value: "YEAR", label: "Năm nay" },
                    { value: "CUSTOM", label: "Tùy chọn" }
                ]
            }),
            field("from", "Từ ngày", "date", period.from, { required: true, min: "0001-01-01", max: "9998-12-31" }),
            field("to", "Đến ngày", "date", period.to, { required: true, min: "0001-01-01", max: "9998-12-31" }),
            el("button", { type: "submit", class: "button", text: "Áp dụng" })
        ]
    );
    form.addEventListener("change", (event) => {
        const presetInput = $("select[name=preset]", form);
        if (event.target.name === "preset" && presetInput.value !== "CUSTOM") {
            const dates = overviewPeriod(presetInput.value);
            $("input[name=from]", form).value = dates.from;
            $("input[name=to]", form).value = dates.to;
        } else if (["from", "to"].includes(event.target.name)) {
            presetInput.value = "CUSTOM";
        }
    });
    return form;
}

async function loadOverview(filters = state.overviewFilters) {
    if (state.view !== "overview" || !canReport()) {
        return;
    }
    const token = beginViewLoad("overview");
    const usingDefaultPeriod = !Object.keys(filters).length;
    const { preset = "CUSTOM", ...periodFilters } = usingDefaultPeriod
        ? { ...overviewPeriod("MONTH"), preset: "MONTH" }
        : filters;
    if (!periodFilters.from || !periodFilters.to || periodFilters.from > periodFilters.to) {
        throw new Error("Ngày bắt đầu phải bằng hoặc trước ngày kết thúc.");
    }
    const loaded = await readViewData(token, () =>
        usingDefaultPeriod
            ? readDefaultReport(periodFilters)
            : api(`reports?${query(periodFilters)}`).then((data) => ({ data, warning: "" }))
    );
    if (!isViewCurrent(token) || !canReport()) {
        return;
    }
    const { data, warning } = loaded;
    state.overviewFilters = { ...data.period, preset: warning ? "CUSTOM" : preset };
    const summary = data.summary;
    $("#page-actions").replaceChildren(
        button(
            "Báo cáo chi tiết ↗",
            () => {
                state.reportFilters = { ...data.period };
                state.reportComparison = "NONE";
                return showView("reports");
            },
            "button secondary small"
        )
    );
    const period = el("section", { class: "overview-period", "aria-label": "Khoảng thời gian tổng quan" }, [
        el("div", { class: "overview-period-heading" }, [
            el("div", {}, [
                el("h2", { text: "Kết quả kinh doanh" }),
                el("p", { text: `${calendarDate(data.period.from)} – ${calendarDate(data.period.to)}` })
            ]),
            el("span", { class: "overview-updated", text: `Cập nhật ${date(data.generatedAt)}` })
        ]),
        overviewPeriodForm(data.period, state.overviewFilters.preset)
    ]);
    const stats = el("div", { class: "stats-grid overview-stats" }, [
        stat("Doanh thu thực thu", money(summary.revenue), "Thanh toán thành công trong kỳ"),
        stat("Giá trị xe đã bán", money(summary.salesAmount), "Giao dịch đã thanh toán / hoàn tất"),
        stat("Số xe bán ra", number(summary.vehiclesSold), "Xe thực tế trong kỳ"),
        stat(
            "Tổng giao dịch",
            number(summary.totalOrders),
            `${number(summary.completedOrders)} hoàn tất · ${number(summary.cancelledOrders)} đã hủy`
        )
    ]);
    const charts = el("div", { class: "chart-grid overview-charts" }, [
        card(
            "Dòng tiền theo tháng",
            "Doanh thu thực thu theo ngày nhận tiền.",
            bars(data.monthly || [], "month", "revenue", money)
        ),
        card(
            "Trạng thái giao dịch",
            "Tiến độ xử lý trong kỳ đã chọn.",
            bars(data.orderStatuses || [], "status", "count")
        )
    ]);
    const recentSales = [...(data.sales || [])]
        .sort(
            (left, right) =>
                (businessDate(right.orderDate)?.getTime() || 0) - (businessDate(left.orderDate)?.getTime() || 0)
        )
        .slice(0, 6);
    const recentRows = recentSales.map((sale) => [
        cellTitle(`#${sale.orderId}`, date(sale.orderDate)),
        cellTitle(
            sale.customerName,
            (sale.vehicles || []).map((vehicle) => vehicle.modelName).join(", ") || "Chưa có xe"
        ),
        money(sale.totalAmount),
        pill(sale.status)
    ]);
    const inventoryRows = (data.inventory || []).map((item) => [
        pill(item.status),
        number(item.count),
        money(item.amount)
    ]);
    const totalInventory = (data.inventory || []).reduce((sum, item) => sum + (Number(item.count) || 0), 0);
    const inventory = card(
        "Tồn kho hiện tại",
        `${number(totalInventory)} xe · Trạng thái ở thời điểm tải dữ liệu.`,
        inventoryRows.length ? table(["TRẠNG THÁI", "SỐ XE", "GIÁ TRỊ"], inventoryRows) : empty("Chưa có xe trong kho")
    );
    const recent = card(
        "Giao dịch gần đây",
        "6 giao dịch mới nhất thuộc kỳ đã chọn.",
        recentRows.length
            ? table(["GIAO DỊCH", "KHÁCH HÀNG / XE", "GIÁ TRỊ", "TRẠNG THÁI"], recentRows)
            : empty("Chưa có giao dịch trong kỳ", "Chọn khoảng thời gian khác để xem giao dịch.")
    );
    $("#content").replaceChildren(period, stats, charts, el("div", { class: "overview-details" }, [recent, inventory]));
    const warnings = warning ? [warning] : [];
    if (summary.inconsistentOrders) {
        warnings.push(
            `${number(summary.inconsistentOrders)} giao dịch có chứng từ chưa nhất quán. Mở báo cáo để kiểm tra chi tiết.`
        );
    }
    notice(warnings.join(" "));
}

function stat(title, value, note) {
    return el("article", { class: "stat" }, [
        el("p", { text: title }),
        el("strong", { text: value }),
        el("small", { text: note })
    ]);
}

function bars(items, labelKey, valueKey, format = number) {
    if (!items.length) {
        return empty("Chưa có số liệu", "Không có dữ liệu trong khoảng thời gian đã chọn.");
    }
    const max = Math.max(...items.map((r) => Number(r[valueKey]) || 0), 1);
    return el(
        "div",
        { class: "card-body bar-chart" },
        items.map((r) =>
            el("div", { class: "bar-row" }, [
                el("span", { text: statusLabels[r[labelKey]] || r[labelKey] || "—" }),
                el(
                    "div",
                    { class: "bar-track" },
                    el("div", {
                        class: "bar-fill",
                        style: `width:${Math.min(100, (Math.max(0, Number(r[valueKey]) || 0) / max) * 100)}%`
                    })
                ),
                el("span", { class: "bar-value", text: format(r[valueKey]) })
            ])
        )
    );
}

async function loadReports(filters = state.reportFilters) {
    if (state.view !== "reports" || !canReport()) {
        return;
    }
    const token = beginViewLoad("reports");
    const usingDefaultPeriod = !Object.keys(filters).length;
    const compare = filters.compare || (filters === state.reportFilters ? state.reportComparison : "NONE") || "NONE";
    if (usingDefaultPeriod) {
        filters = lastSixMonths();
    }
    const { compare: unusedComparison, ...reportFilters } = filters;
    if (reportFilters.from && reportFilters.to && reportFilters.from > reportFilters.to) {
        throw new Error("Ngày bắt đầu phải bằng hoặc trước ngày kết thúc.");
    }
    const result = await readViewData(token, () =>
        Promise.all([
            usingDefaultPeriod
                ? readDefaultReport(reportFilters)
                : api(`reports?${query(reportFilters)}`).then((data) => ({ data, warning: "" })),
            api("catalogue").catch(() => ({ models: [] })),
            loadCatalogueMetadata()
        ])
    );
    if (!isViewCurrent(token) || !canReport()) {
        return;
    }
    const [loaded, catalogue] = result;
    const data = loaded.data;
    const appliedFilters = { ...reportFilters, from: data.period.from, to: data.period.to };
    let comparisonData = null;
    let comparisonWarning = "";
    if (compare === "PREVIOUS") {
        const period = previousPeriod(data.period);
        if (!period) {
            comparisonWarning = "Kỳ liền trước nằm ngoài phạm vi ngày hỗ trợ. Báo cáo hiện tại vẫn được hiển thị.";
        } else {
            try {
                // Kỳ hiện tại đã qua kiểm tra số ngày ở server; kỳ so sánh có đúng số ngày đó.
                comparisonData = await api(`reports?${query({ ...appliedFilters, ...period })}`);
            } catch (error) {
                comparisonWarning = `Chưa thể tải kỳ trước: ${error.message} Báo cáo hiện tại vẫn được hiển thị.`;
            }
        }
    }
    if (!isViewCurrent(token) || !canReport()) {
        return;
    }
    state.report = data;
    state.reportFilters = appliedFilters;
    state.reportComparison = compare;
    const exports = el("div", { class: "report-controls" }, [
        el("a", {
            class: "button secondary small",
            text: "↓ Excel",
            href: appUrl(`api/reports?${query({ ...state.reportFilters, format: "xlsx" })}`)
        }),
        el("a", {
            class: "button secondary small",
            text: "↓ CSV",
            href: appUrl(`api/reports?${query({ ...state.reportFilters, format: "csv" })}`)
        }),
        button("In / Lưu PDF", () => window.print(), "button secondary small")
    ]);
    $("#page-actions").replaceChildren(exports);
    // Danh sách mẫu xe độc lập với số xe đã bán trong kết quả đang lọc.
    const modelMap = new Map();
    for (const model of [...(catalogue.models || []), ...(data.models || [])]) {
        const modelId = model.modelId ?? model.modelID;
        if (modelId != null) {
            modelMap.set(modelId, { ...model, modelId });
        }
    }
    const modelChoices = [
        { value: "", label: "Tất cả mẫu xe" },
        ...Array.from(modelMap.values()).map((model) => ({
            value: model.modelId,
            label: model.modelName
        }))
    ];
    // Giữ những nhân viên đã đọc trong phiên, nên lọc một người không làm mất người khác.
    const employeeMap = new Map(state.reportEmployees.map((employee) => [employee.employeeId, employee]));
    for (const employee of data.employees || []) {
        if (employee.employeeId != null) {
            employeeMap.set(employee.employeeId, employee);
        }
    }
    state.reportEmployees = Array.from(employeeMap.values());
    const employeeChoices = [
        { value: "", label: "Tất cả nhân viên" },
        ...state.reportEmployees.map((employee) => ({
            value: employee.employeeId,
            label: employee.employeeName
        }))
    ];
    const form = el(
        "form",
        {
            class: "toolbar",
            onsubmit: (event) => {
                event.preventDefault();
                task(() => loadReports(values(form)));
            }
        },
        [
            field("from", "Từ ngày", "date", data.period.from, {
                required: true,
                min: "0001-01-01",
                max: "9998-12-31"
            }),
            field("to", "Đến ngày", "date", data.period.to, {
                required: true,
                min: "0001-01-01",
                max: "9998-12-31"
            }),
            field("modelId", "Mẫu xe", "select", filters.modelId || "", { choices: modelChoices }),
            field("employeeId", "Nhân viên", "select", filters.employeeId || "", {
                choices: employeeChoices
            }),
            field("compare", "So sánh", "select", compare, {
                choices: [
                    { value: "NONE", label: "Không so sánh" },
                    { value: "PREVIOUS", label: "Kỳ liền trước" }
                ]
            }),
            el("button", { type: "submit", class: "button", text: "Xem báo cáo" }),
            button(
                "Đặt lại bộ lọc",
                () => {
                    const input = values(form);
                    task(() =>
                        loadReports({
                            from: input.from || data.period.from,
                            to: input.to || data.period.to,
                            compare: "NONE"
                        })
                    );
                },
                "button secondary"
            ),
            button(
                "Tháng này",
                () => {
                    const today = isoToday();
                    task(() => loadReports({ from: `${today.slice(0, 7)}-01`, to: today }));
                },
                "button secondary"
            ),
            button("6 tháng", () => task(() => loadReports(lastSixMonths())), "button secondary"),
            button(
                "Quý này",
                () => {
                    const today = isoToday();
                    const startMonth = String(Math.floor((Number(today.slice(5, 7)) - 1) / 3) * 3 + 1).padStart(2, "0");
                    task(() => loadReports({ from: `${today.slice(0, 4)}-${startMonth}-01`, to: today }));
                },
                "button secondary"
            ),
            button(
                "Năm nay",
                () => {
                    const today = isoToday();
                    task(() => loadReports({ from: `${today.slice(0, 4)}-01-01`, to: today }));
                },
                "button secondary"
            )
        ]
    );
    const s = data.summary;
    const stats = el("div", { class: "stats-grid" }, [
        stat("Doanh thu thực thu", money(s.revenue), "Thanh toán thành công theo ngày nhận tiền"),
        stat("Giá trị xe đã bán", money(s.salesAmount), "Giao dịch hợp lệ đã thanh toán / hoàn tất"),
        stat("Số xe bán ra", number(s.vehiclesSold), "Mỗi xe thực tế được đếm một lần"),
        stat(
            "Tổng giao dịch",
            number(s.totalOrders),
            `${number(s.completedOrders)} hoàn tất · ${number(s.cancelledOrders)} đã hủy`
        ),
        stat("Số tiền còn phải thu", money(s.outstandingAmount), "Công nợ từ nghĩa vụ tài chính hợp lệ"),
        stat("Hợp đồng trả góp", number(s.installmentContracts), `Thu kỳ hạn: ${money(s.installmentReceipts)}`),
        stat("Công nợ quá hạn", money(s.overdueAmount), "Các kỳ trả góp đã đến hạn"),
        stat("Thanh toán thành công", number(s.completedPayments), `${number(s.contracts)} hợp đồng trong kỳ`)
    ]);
    const charts = el("div", { class: "chart-grid" }, [
        card(
            "Dòng tiền theo tháng",
            "Các khoản thanh toán thành công trong thời gian đã chọn.",
            bars(data.monthly || [], "month", "revenue", money)
        ),
        card(
            "Trạng thái giao dịch",
            "Phân bố giao dịch theo trạng thái hiện tại.",
            bars(data.orderStatuses || [], "status", "count")
        )
    ]);
    const models = (data.models || []).map((m) => [m.modelName, number(m.vehiclesSold), money(m.salesAmount)]);
    const employeeRows = (data.employees || []).map((e) => [
        e.employeeName || "Chưa phân công",
        number(e.orders),
        number(e.vehiclesSold),
        money(e.salesAmount)
    ]);
    const breakdowns = el("div", { class: "chart-grid" }, [
        card(
            "Xe bán theo mẫu",
            "Giao dịch có mẫu xe được chọn trong bộ lọc.",
            models.length ? table(["MẪU XE", "SỐ XE", "GIÁ TRỊ BÁN"], models) : empty()
        ),
        card(
            "Hiệu quả nhân viên",
            "Số giao dịch và giá trị xe đã bán.",
            employeeRows.length ? table(["NHÂN VIÊN", "GIAO DỊCH", "SỐ XE", "GIÁ TRỊ BÁN"], employeeRows) : empty()
        )
    ]);
    const sales = (data.sales || []).map((r) => [
        cellTitle(`#${r.orderId}`, date(r.orderDate)),
        cellTitle(r.customerName, r.employeeName || "Chưa phân công"),
        cellTitle(
            (r.vehicles || []).map((v) => `${v.modelName} ${v.versionName || ""}`).join(", "),
            (r.vehicles || [])
                .map((v) => v.vin)
                .filter(Boolean)
                .join(", ")
        ),
        pill(r.status),
        money(r.totalAmount),
        money(r.paidAmount),
        money(r.remainingAmount),
        cellTitle(r.contractNumber || "Chưa có hợp đồng", r.invoiceNumber || "Chưa có hóa đơn"),
        pill(r.deliveryStatus || "Chưa bàn giao", "neutral"),
        r.eligible ? pill("Dữ liệu hợp lệ", "neutral") : pill("Cần kiểm tra", "warning")
    ]);
    const inventory = (data.inventory || []).map((r) => [pill(r.status), number(r.count), money(r.amount)]);
    const reportInfo = el("p", {
        class: "section-note",
        text: `Cập nhật ${date(data.generatedAt)}. Doanh thu thực thu và giá trị xe đã bán dùng các mốc thời gian khác nhau. Thu kỳ hạn trả góp được trình bày riêng để tránh cộng trùng. Tồn kho phản ánh trạng thái hiện tại.`
    });
    const warnings = [];
    if (loaded.warning) {
        warnings.push(loaded.warning);
    }
    if (s.inconsistentOrders) {
        warnings.push(
            `${number(s.inconsistentOrders)} giao dịch có dữ liệu liên quan chưa nhất quán. Kiểm tra hợp đồng, hóa đơn và xe trước khi đối chiếu.`
        );
    }
    notice(warnings.join(" "));
    const definitions = el("details", { class: "card card-body" }, [
        el("summary", { text: "Cách đọc và đối chiếu số liệu" }),
        el(
            "div",
            { class: "report-definitions" },
            Object.values(data.definitions || {}).map((value) => el("p", { text: value }))
        )
    ]);
    const content = [
        card("Khoảng thời gian báo cáo", "Lựa chọn thời gian và phạm vi dữ liệu bạn muốn đối chiếu.", form),
        stats
    ];
    const compared = reportComparison(data, comparisonData, comparisonWarning);
    if (compared) {
        content.push(compared);
    }
    content.push(
        reportInfo,
        charts,
        breakdowns,
        card(
            "Chi tiết giao dịch",
            "Trạng thái, giá trị và chứng từ liên quan của từng giao dịch.",
            sales.length
                ? table(
                      [
                          "GIAO DỊCH",
                          "KHÁCH HÀNG / NHÂN VIÊN",
                          "XE / VIN",
                          "TRẠNG THÁI",
                          "TỔNG GIÁ TRỊ",
                          "ĐÃ THU",
                          "CÒN PHẢI THU",
                          "CHỨNG TỪ",
                          "BÀN GIAO",
                          "THỐNG KÊ"
                      ],
                      sales
                  )
                : empty("Không có giao dịch trong kỳ", "Thử chọn khoảng thời gian khác.")
        ),
        card(
            "Tồn kho hiện tại",
            "Báo cáo phân bố xe thực tế theo trạng thái.",
            inventory.length
                ? table(["TRẠNG THÁI XE", "SỐ LƯỢNG", "GIÁ TRỊ"], inventory)
                : empty("Chưa có xe trong kho")
        ),
        definitions
    );
    $("#content").replaceChildren(...content);
}
