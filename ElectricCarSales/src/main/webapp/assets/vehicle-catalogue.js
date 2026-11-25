"use strict";

let catalogueMetadataRequest;

function selectComparedModel(modelId, checked) {
    const ids = [...new Set(state.comparedIds)];
    if (checked && !ids.includes(modelId) && ids.length >= 4) {
        return false;
    }
    state.comparedIds = checked ? [...new Set([...ids, modelId])] : ids.filter((id) => id !== modelId);
    return true;
}

function matchesCatalogueFilter(vehicle, filters) {
    const search = searchText(filters.q);
    const text = searchText(
        `${vehicle.brandName || ""} ${vehicle.modelName || ""} ${vehicle.carLineName || ""} ${vehicle.description || ""} ${(vehicle.versions || []).join(" ")} ${(vehicle.colors || []).join(" ")}`
    );
    return (
        (!search || text.includes(search)) &&
        (!filters.maxPrice ||
            (vehicle.minPrice != null && Number(vehicle.minPrice) <= Number(filters.maxPrice) * 1000000)) &&
        (!filters.seats || Number(vehicle.specifications?.seatingCapacity) === Number(filters.seats))
    );
}

function modelPrice(vehicle) {
    if (vehicle.minPrice == null) {
        return "Giá bán chưa được cập nhật";
    }
    return vehicle.maxPrice && Number(vehicle.maxPrice) !== Number(vehicle.minPrice)
        ? `${money(vehicle.minPrice)} – ${money(vehicle.maxPrice)}`
        : money(vehicle.minPrice);
}

// Metadata giữ nguồn tham khảo; thông số và số xe đang bán vẫn đọc từ database.
async function loadCatalogueMetadata() {
    if (state.catalogueMetadata) {
        return;
    }
    if (!catalogueMetadataRequest) {
        catalogueMetadataRequest = (async () => {
            try {
                const response = await fetch(appUrl("assets/vinfast-catalogue.json"), { credentials: "same-origin" });
                if (response.ok) {
                    const data = await response.json();
                    if (Array.isArray(data.models)) {
                        state.catalogueMetadata = data;
                    }
                }
            } catch (_) {
                /* Danh mục database vẫn sử dụng được nếu file nguồn chưa có. */
            }
        })().finally(() => {
            catalogueMetadataRequest = null;
        });
    }
    return catalogueMetadataRequest;
}

function vehicleMetadata(vehicle) {
    if (String(vehicle.brandName || "").toLowerCase() !== "vinfast") {
        return null;
    }
    const name = String(vehicle.modelName || "")
        .trim()
        .toUpperCase();
    return (
        (state.catalogueMetadata?.models || []).find(
            (model) =>
                String(model.modelName || "")
                    .trim()
                    .toUpperCase() === name
        ) || null
    );
}

function officialUrl(value) {
    if (!value) {
        return null;
    }
    try {
        const url = new URL(value);
        if (
            url.protocol === "https:" &&
            (url.hostname === "vinfastauto.com" || url.hostname.endsWith(".vinfastauto.com"))
        ) {
            return url.href;
        }
    } catch (_) {
        /* URL không hợp lệ không được đưa vào giao diện. */
    }
    return null;
}

function sourceLink(text, url) {
    const verifiedUrl = officialUrl(url);
    return verifiedUrl
        ? el("a", {
              class: "source-link",
              href: verifiedUrl,
              target: "_blank",
              rel: "noopener noreferrer",
              text
          })
        : null;
}

function quantity(value, unit = "") {
    return value == null ? "Chưa cập nhật" : `${number(value)}${unit ? ` ${unit}` : ""}`;
}

function rangeStandard(vehicle) {
    const metadata = vehicleMetadata(vehicle);
    if (metadata?.rangeStandard) {
        return metadata.rangeStandard;
    }
    const match = String(vehicle.specifications?.description || "").match(/\b(NEDC|WLTP|EPA|CLTC)\b/i);
    return match ? match[1].toUpperCase() : "Chưa ghi chuẩn";
}

function sourceInfo(vehicle) {
    const metadata = vehicleMetadata(vehicle);
    const links = [];
    if (metadata) {
        links.push(
            sourceLink("Thông số VinFast ↗", metadata.sourceUrl),
            sourceLink("Nguồn giá ↗", metadata.priceSourceUrl),
            sourceLink("Brochure VinFast ↗", metadata.brochureUrl)
        );
    }
    return el("div", { class: "vehicle-sources" }, links.filter(Boolean));
}

function priceNote(vehicle) {
    return (
        vehicleMetadata(vehicle)?.priceNote ||
        "Giá bán tại showroom; liên hệ nhân viên để đối chiếu trước khi giao dịch."
    );
}

function vehicleImage(vehicle) {
    const imageUrl = officialUrl(vehicleMetadata(vehicle)?.imageUrl);
    const fallback = el("div", { class: "vehicle-image-fallback", text: vehicle.modelName });
    if (!imageUrl) {
        return el("div", { class: "vehicle-image" }, fallback);
    }
    const image = el("img", {
        src: imageUrl,
        alt: `Xe VinFast ${vehicle.modelName}`,
        loading: "lazy",
        referrerpolicy: "no-referrer"
    });
    image.addEventListener("error", () => image.replaceWith(fallback), { once: true });
    return el("div", { class: "vehicle-image" }, image);
}

function openCustomerAssistant(message = "", intent = "AUTO", modelIds = []) {
    const params = new URLSearchParams();
    if (message) params.set("prompt", message);
    if (intent !== "AUTO") params.set("intent", intent);
    if (modelIds.length) params.set("models", modelIds.join(","));
    window.location.assign(`${appUrl("customer/ai-assistant.jsp")}${params.size ? `?${params}` : ""}`);
}

function askAboutVehicle(vehicle) {
    openCustomerAssistant(`Cho tôi biết thông số và ưu điểm của ${vehicle.modelName}.`, "PRODUCT_QA", [vehicle.modelId]);
}

function vehicleDetails(vehicle) {
    const spec = vehicle.specifications || {};
    const metadata = vehicleMetadata(vehicle);
    const rows = [
        ["Mẫu xe", `${vehicle.brandName} ${vehicle.modelName}`],
        [
            "Cấu hình đối chiếu",
            metadata?.specificationVersion || (vehicle.versions || []).join(", ") || "Chưa cập nhật"
        ],
        ["Giá showroom demo", modelPrice(vehicle)],
        ["Giá tham khảo từ hãng", metadata?.priceVnd == null ? "Chưa cập nhật" : money(metadata.priceVnd)],
        ["Kích thước D × R × C (mm)", spec.dimensions || "Chưa cập nhật"],
        ["Dung lượng pin", quantity(spec.batteryCapacity, "kWh")],
        ["Tầm hoạt động", `${quantity(spec.rangeKM, "km")} · ${rangeStandard(vehicle)}`],
        ["Công suất tối đa", quantity(spec.maxPower, "kW")],
        ["Số chỗ", quantity(spec.seatingCapacity)],
        ["Bánh xe", spec.wheelType || "Chưa cập nhật"],
        ["Số túi khí", quantity(spec.airbagCount)],
        ["Thời gian sạc (giờ)", quantity(spec.chargingTime)],
        ["Màu trong showroom", (vehicle.colors || []).join(", ") || "Chưa cập nhật"],
        ["Xe có sẵn (demo)", number(vehicle.availableCars)]
    ];
    const detail = el("div", { class: "field full vehicle-detail" }, [
        vehicleImage(vehicle),
        el("p", { text: vehicle.description || "" }),
        table(["THÔNG TIN", "GIÁ TRỊ"], rows),
        el("p", { class: "section-note", text: priceNote(vehicle) }),
        el("p", {
            class: "section-note",
            text: spec.description || "Tầm hoạt động thực tế phụ thuộc điều kiện sử dụng."
        }),
        metadata?.specificationNote
            ? el("p", { class: "section-note", text: `Đối chiếu nguồn: ${metadata.specificationNote}` })
            : null,
        sourceInfo(vehicle)
    ]);
    modal(`Chi tiết ${vehicle.modelName}`, [detail], () => askAboutVehicle(vehicle), "Nhờ trợ lý tư vấn");
}

function catalogueCard(vehicle, onSelection) {
    const spec = vehicle.specifications || {};
    const selected = state.comparedIds.includes(vehicle.modelId);
    const compareLabel = el("label", { class: "compare-choice" }, [
        el("input", {
            type: "checkbox",
            checked: selected,
            "aria-label": `So sánh ${vehicle.modelName}`,
            onchange: (event) => onSelection(vehicle, event.currentTarget)
        }),
        el("span", { text: "So sánh" })
    ]);
    return el("article", { class: "catalogue-card" }, [
        el("div", { class: "vehicle-card-top" }, [
            el("span", { class: "vehicle-brand", text: vehicle.brandName || "Xe điện" }),
            compareLabel
        ]),
        vehicleImage(vehicle),
        el("div", { class: "vehicle-card-body" }, [
            el("h2", { text: vehicle.modelName }),
            el("p", { class: "catalogue-price", text: modelPrice(vehicle) }),
            el("p", {
                class: "price-caption",
                text: vehicleMetadata(vehicle)
                    ? "Giá tham khảo gồm pin · Chi tiết ở nguồn giá"
                    : "Giá từ danh mục showroom"
            }),
            el("div", { class: "vehicle-spec-grid" }, [
                el("div", {}, [
                    el("strong", { text: quantity(spec.rangeKM, "km") }),
                    el("small", { text: `Tầm hoạt động ${rangeStandard(vehicle)}` })
                ]),
                el("div", {}, [
                    el("strong", { text: quantity(spec.seatingCapacity, "chỗ") }),
                    el("small", { text: "Không gian hành khách" })
                ]),
                el("div", {}, [
                    el("strong", { text: quantity(spec.batteryCapacity, "kWh") }),
                    el("small", { text: "Dung lượng pin" })
                ]),
                el("div", {}, [
                    el("strong", { text: quantity(spec.maxPower, "kW") }),
                    el("small", { text: "Công suất tối đa" })
                ])
            ]),
            el("div", { class: "vehicle-stock" }, [
                pill(vehicle.availableCars ? "AVAILABLE" : "Hết xe demo", vehicle.availableCars ? "" : "neutral"),
                el("span", { text: `${number(vehicle.availableCars)} xe trong showroom demo` })
            ]),
            el("div", { class: "vehicle-actions" }, [
                button("Xem chi tiết", () => vehicleDetails(vehicle), "button secondary"),
                button("Hỏi trợ lý ↗", () => task(() => askAboutVehicle(vehicle)), "button")
            ]),
            sourceInfo(vehicle)
        ])
    ]);
}

async function loadCatalogue(filters = state.catalogueFilters, refresh = true) {
    if (state.view !== "catalogue") {
        return;
    }
    const token = beginViewLoad("catalogue");
    filters = { ...filters };
    if (refresh) {
        let data;
        try {
            [data] = await Promise.all([api("ai"), loadCatalogueMetadata()]);
        } catch (error) {
            if (isViewCurrent(token)) {
                throw error;
            }
            return;
        }
        if (!isViewCurrent(token)) {
            return;
        }
        state.models = data.models || [];
    }
    if (!isViewCurrent(token)) {
        return;
    }
    state.catalogueFilters = { ...filters };
    state.comparedIds = state.comparedIds.filter((id) => state.models.some((model) => model.modelId === id));
    $("#page-actions").replaceChildren(button("✧ Tư vấn chọn xe", () => openCustomerAssistant(), "button"));
    const selectedText = el("strong");
    const compareButton = button(
        "So sánh đã chọn",
        () => {
            const selected = state.models.filter((model) => state.comparedIds.includes(model.modelId));
            if (selected.length < 2) {
                notice("Chọn ít nhất 2 mẫu xe để so sánh.");
                return;
            }
            comparisonPanel.replaceChildren(
                card(
                    "Đối chiếu các phiên bản",
                    "NEDC và WLTP là các chuẩn đo khác nhau. Đọc tầm hoạt động cùng chuẩn và đúng phiên bản.",
                    comparison(selected)
                ),
                button(
                    "✧ Hỏi trợ lý về các xe này",
                    () => {
                        openCustomerAssistant(`So sánh ${selected.map((model) => model.modelName).join(" và ")}.`,
                            "COMPARE", selected.map((model) => model.modelId));
                    },
                    "button secondary"
                )
            );
            comparisonPanel.hidden = false;
            comparisonPanel.scrollIntoView({ behavior: "smooth", block: "start" });
        },
        "button secondary small"
    );
    const clearButton = button(
        "Bỏ chọn",
        () => {
            state.comparedIds = [];
            task(() => loadCatalogue(state.catalogueFilters, false));
        },
        "text-button"
    );
    function updateSelection() {
        selectedText.textContent = `${state.comparedIds.length}/4 mẫu đã chọn`;
        compareButton.disabled = state.comparedIds.length < 2;
        clearButton.disabled = !state.comparedIds.length;
    }
    function selectVehicle(vehicle, checkbox) {
        if (!selectComparedModel(vehicle.modelId, checkbox.checked)) {
            checkbox.checked = false;
            notice("Bạn có thể chọn tối đa 4 mẫu cho một bảng so sánh.");
            return;
        }
        comparisonPanel.hidden = true;
        notice("");
        updateSelection();
    }
    const comparisonPanel = el("section", {
        class: "catalogue-comparison",
        hidden: true,
        "aria-label": "Bảng so sánh mẫu xe"
    });
    const searchForm = el(
        "form",
        {
            class: "toolbar catalogue-toolbar",
            onsubmit: (event) => {
                event.preventDefault();
                task(() => loadCatalogue(values(searchForm), false));
            }
        },
        [
            field("q", "Tìm mẫu xe", "search", filters.q || "", { help: "Ví dụ: VF 6 Eco, VF 9, SUV" }),
            field("maxPrice", "Ngân sách tối đa (triệu đồng)", "number", filters.maxPrice || "", {
                min: 1,
                step: 1
            }),
            field("seats", "Số chỗ", "select", filters.seats || "", {
                choices: [
                    { value: "", label: "Tất cả" },
                    { value: 4, label: "4 chỗ" },
                    { value: 5, label: "5 chỗ" },
                    { value: 6, label: "6 chỗ" },
                    { value: 7, label: "7 chỗ" }
                ]
            }),
            el("button", { type: "submit", class: "button", text: "Lọc xe" }),
            button("Xóa bộ lọc", () => task(() => loadCatalogue({}, false)), "button secondary")
        ]
    );
    const models = state.models.filter((vehicle) => matchesCatalogueFilter(vehicle, filters));
    const hero = el("section", { class: "catalogue-hero" }, [
        el("div", {}, [
            el("span", { class: "hero-label", text: "VINFAST · XE ĐIỆN CHO CUỘC SỐNG" }),
            el("h2", { text: "Một hành trình mới, một chiếc xe phù hợp." }),
            el("p", {
                text: "Tìm chiếc xe theo ngân sách và số chỗ, so sánh đúng phiên bản rồi hỏi trợ lý khi cần thêm thông tin."
            })
        ]),
        el("div", { class: "hero-summary" }, [
            el("strong", { text: number(state.models.length) }),
            el("span", { text: "mẫu / phiên bản trong danh mục" }),
            el("small", { text: "Thông số có nguồn · Giá có chú thích" })
        ])
    ]);
    const selection = el("div", { class: "catalogue-selection" }, [
        el("span", { text: `Hiển thị ${number(models.length)} mẫu / phiên bản` }),
        el("div", {}, [selectedText, compareButton, clearButton])
    ]);
    updateSelection();
    const notes = el("section", { class: "catalogue-notes" }, [
        el("h3", { text: "Nguồn dữ liệu & cách đọc thông số" }),
        el("p", {
            text:
                state.catalogueMetadata?.note ||
                "Thông số đọc từ danh mục sản phẩm của showroom. Trường chưa xác định được ghi rõ trong từng mẫu xe."
        }),
        el("p", {
            text: "Giá trên trang dùng cho bản demo; chính sách ưu đãi và giá thực tế có thể thay đổi. Tầm hoạt động công bố phụ thuộc chuẩn đo, cấu hình và điều kiện sử dụng. Các màu hiển thị là màu xe đang có trong dữ liệu demo."
        }),
        state.catalogueMetadata?.referenceDate
            ? el("small", {
                  text: `Ngày đối chiếu nguồn: ${state.catalogueMetadata.referenceDate}. Liên kết VinFast nằm trong từng thẻ xe.`
              })
            : null
    ]);
    $("#content").replaceChildren(
        hero,
        card("Tìm chiếc xe của bạn", "Lọc theo nhu cầu, hoặc chọn 2–4 mẫu để so sánh.", searchForm),
        selection,
        models.length
            ? el(
                  "div",
                  { class: "catalogue-grid" },
                  models.map((vehicle) => catalogueCard(vehicle, selectVehicle))
              )
            : empty("Không tìm thấy mẫu xe phù hợp", "Thử xóa bộ lọc hoặc điều chỉnh ngân sách và số chỗ."),
        comparisonPanel,
        notes
    );
}

function vehicleCard(vehicle, reasons = []) {
    const spec = vehicle.specifications || {};
    return el("article", { class: "vehicle-mini" }, [
        el("h3", { text: `${vehicle.brandName || ""} ${vehicle.modelName}`.trim() }),
        el("p", { class: "price", text: modelPrice(vehicle) }),
        el("p", {
            text: `${spec.rangeKM != null ? `${number(spec.rangeKM)} km · ${rangeStandard(vehicle)}` : "Chưa có quãng đường"} · ${spec.seatingCapacity != null ? `${spec.seatingCapacity} chỗ` : "Chưa có số chỗ"}`
        }),
        el("p", {
            text: `${number(vehicle.availableCars)} xe có sẵn (demo) · ${(vehicle.versions || []).join(", ") || "Chưa có phiên bản"}`
        }),
        reasons.length
            ? el(
                  "ul",
                  {},
                  reasons.map((reason) => el("li", { text: reason }))
              )
            : null,
        sourceInfo(vehicle)
    ]);
}

function comparison(vehicles) {
    const specs = [
        ["Giá showroom demo", (v) => modelPrice(v)],
        ["Phiên bản", (v) => (v.versions || []).join(", ") || "Chưa cập nhật"],
        ["Pin (kWh)", (v) => v.specifications?.batteryCapacity],
        ["Quãng đường (km)", (v) => v.specifications?.rangeKM],
        ["Chuẩn tầm hoạt động", (v) => rangeStandard(v)],
        ["Số chỗ", (v) => v.specifications?.seatingCapacity],
        ["Công suất (kW)", (v) => v.specifications?.maxPower],
        ["Kích thước (mm)", (v) => v.specifications?.dimensions],
        ["Sạc (giờ)", (v) => v.specifications?.chargingTime],
        ["Xe có sẵn (demo)", (v) => v.availableCars],
        ["Màu sắc", (v) => (v.colors || []).join(", ") || "Chưa cập nhật"],
        ["Nguồn thông số", (v) => sourceLink("VinFast ↗", vehicleMetadata(v)?.sourceUrl) || "Danh mục showroom"],
        ["Chú thích giá", (v) => priceNote(v)]
    ];
    return el(
        "div",
        { class: "comparison-table" },
        table(
            ["THÔNG TIN", ...vehicles.map((v) => v.modelName)],
            specs.map(([name, get]) => [name, ...vehicles.map((v) => get(v) ?? "Chưa cập nhật")])
        )
    );
}
