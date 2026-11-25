"use strict";

(() => {
    const contextPath = document.querySelector('meta[name="app-context-path"]')?.content || "";
    const catalogueUrl = `${contextPath}/assets/vinfast-catalogue.json`;
    const databaseCatalogueUrl = `${contextPath}/api/catalogue`;
    const grid = document.getElementById("vehicle-grid");
    const search = document.getElementById("vehicle-search");
    const modelFilter = document.getElementById("model-filter");
    const priceFilter = document.getElementById("price-filter");
    const rangeFilter = document.getElementById("range-filter");
    const colorFilter = document.getElementById("color-filter");
    const sortFilter = document.getElementById("sort-filter");
    const summary = document.getElementById("result-summary");
    const activeFilters = document.getElementById("active-filters");

    // Comparison & History elements
    const compareDock = document.getElementById("compare-dock");
    const compareDialog = document.getElementById("compare-dialog");
    const compareNotice = document.getElementById("compare-notice");
    const openHistoryBtn = document.getElementById("open-history-btn");
    const historyBadge = document.getElementById("history-badge");
    const compareDockHistory = document.getElementById("compare-dock-history");
    const dockHistoryCount = document.getElementById("dock-history-count");
    const compareHistoryDialog = document.getElementById("compare-history-dialog");
    const historyDialogList = document.getElementById("history-dialog-list");
    const btnClearAllHistory = document.getElementById("btn-clear-all-history");
    const historyCloseBtn = document.getElementById("history-close-btn");
    const historyDoneBtn = document.getElementById("history-done-btn");

    const selections = new Set();
    const compareSelectionStorageKey = "electriccarsale.compareSelections";
    const selectedModels = new Set();
    const selectedPrices = new Set();
    const isAuthenticated = document.querySelector('meta[name="user-authenticated"]')?.content === "true"
        || document.body.dataset.authenticated === "true";

    let models = [];
    let catalogueMetadata = {};
    let catalogueStatus = "loading";
    let inventoryAvailable = false;
    let inventoryByLine = new Map();
    let inventoryByModel = new Map();
    let inventoryCountByModel = new Map();
    let compareNoticeTimer;

    const PRICE_RANGES = [
        {
            key: "under-500",
            label: "Dưới 500 triệu",
            match: (vnd) => vnd !== null && vnd < 500_000_000
        },
        {
            key: "500-800",
            label: "500 - 800 triệu",
            match: (vnd) => vnd !== null && vnd >= 500_000_000 && vnd <= 800_000_000
        },
        {
            key: "800-1000",
            label: "800 triệu - 1 tỷ",
            match: (vnd) => vnd !== null && vnd > 800_000_000 && vnd <= 1_000_000_000
        },
        {
            key: "1000-1500",
            label: "1 - 1,5 tỷ",
            match: (vnd) => vnd !== null && vnd > 1_000_000_000 && vnd <= 1_500_000_000
        },
        {
            key: "over-1500",
            label: "Trên 1,5 tỷ",
            match: (vnd) => vnd !== null && vnd > 1_500_000_000
        }
    ];

    try {
        const savedSelections = JSON.parse(sessionStorage.getItem(compareSelectionStorageKey) || "[]");
        if (Array.isArray(savedSelections)) {
            savedSelections.filter((key) => typeof key === "string").forEach((key) => selections.add(key));
        }
    } catch (error) {
        console.error("Không thể khôi phục các xe đã chọn để so sánh.", error);
    }

    const text = (value) => String(value ?? "").trim();

    const number = (value) => {
        if (value === null || value === undefined || text(value) === "") {
            return null;
        }
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : null;
    };

    const price = (value) => {
        const amount = number(value);
        return amount === null ? "Liên hệ" : `${new Intl.NumberFormat("vi-VN").format(amount)} ₫`;
    };

    const range = (model) =>
        model.rangeKm == null ? "Đang cập nhật" : `${new Intl.NumberFormat("vi-VN").format(model.rangeKm)} km`;

    const battery = (model) =>
        model.batteryKwh == null
            ? "Đang cập nhật"
            : `${new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(model.batteryKwh)} kWh`;

    const seats = (model) => (model.seats == null ? "Đang cập nhật" : `${model.seats} chỗ`);

    const lineName = (model) => text(model.carLine || model.modelName).replace(/\s+/g, " ");
    const lineKey = (value) => text(value).replace(/\s+/g, " ").toLocaleUpperCase("vi");
    const normalizeSearch = (value) => String(value ?? "").normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLocaleLowerCase("vi")
        .replace(/đ/g, "d")
        .replace(/[^a-z0-9]+/g, "")
        .trim();

    const normalizeColor = (value) => String(value ?? "").normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLocaleLowerCase("vi")
        .replace(/đ/g, "d")
        .replace(/[^a-z0-9]+/g, " ")
        .trim();
    const colorFamilies = [
        ["white", ["trang", "white", "infinity blanc"]],
        ["gray", ["xam", "gray", "grey", "zenith grey"]],
        ["silver", ["bac", "silver", "desat silver"]],
        ["black", ["den", "black", "jet black"]],
        ["red", ["do", "red", "solar ruby", "crimson red"]],
        ["yellow", ["vang", "yellow", "summer yellow"]],
        ["blue", ["xanh duong", "blue", "sky blue"]],
        ["green", ["xanh la", "green", "urban mint", "ivy green"]],
        ["pink", ["hong", "pink", "rose pink"]]
    ];
    const colorFamily = (value) => {
        const color = normalizeColor(String(value ?? "").split(/[+(]/, 1)[0]);
        return colorFamilies.find(([, aliases]) => aliases.some((alias) => color.includes(alias)))?.[0];
    };

    function colorImageFor(model, colorName) {
        const family = colorFamily(colorName);
        if (!family || !model.colorImages || typeof model.colorImages !== "object") return "";
        const image = Object.entries(model.colorImages).find(([name]) => colorFamily(name) === family)?.[1];
        return safeOfficialUrl(image);
    }

    const safeOfficialUrl = (candidate) => {
        try {
            const url = new URL(candidate);
            return url.protocol === "https:" &&
                (url.hostname === "vinfastauto.com" || url.hostname.endsWith(".vinfastauto.com"))
                ? url.href
                : "";
        } catch (_) {
            return "";
        }
    };

    const className = (model) => {
        const line = lineName(model).toUpperCase();
        const classes = {
            "VF 3": "Xe đô thị",
            "VF 5": "SUV đô thị",
            "VF 6": "SUV cỡ B",
            "VF 7": "SUV cỡ C",
            "VF 8": "SUV cỡ D",
            "VF 9": "SUV cỡ E"
        };
        return classes[line] || "Xe điện VinFast";
    };

    const make = (tag, classValue, value) => {
        const element = document.createElement(tag);
        if (classValue) {
            element.className = classValue;
        }
        if (value !== undefined) {
            element.textContent = value;
        }
        return element;
    };

    // Helper to check if a search term matches any car model line (e.g. "VF3", "VF 6", etc.)
    function isModelKeyword(value) {
        if (!value) return false;
        const normalized = normalizeSearch(value);
        if (!normalized) return false;
        if (/^vf\s*[0-9]+/i.test(value.trim())) return true;
        return models.some((m) => {
            const ln = normalizeSearch(lineName(m));
            return ln && (ln === normalized || normalized === normalizeSearch(m.modelName));
        });
    }

    // =========================================================================
    // Comparison History Management (Client & Server Sync - Auth only)
    // =========================================================================
    let historyRows = [];
    const CompareHistory = {
        getAll() { return historyRows; },
        async syncWithServer() {
            if (!isAuthenticated) return;
            const response = await fetch(contextPath + "/api/comparison/history");
            if (!response.ok) throw new Error("Không thể tải lịch sử so sánh.");
            const data = await response.json();
            historyRows = (data.history || []).map(row => ({...row, id: row.sessionId, dateStr: row.createDate}));
            updateHistoryBadges();
            renderHistoryDialog();
        },
        async remove(id) {
            const auth = await fetch(contextPath + "/api/auth").then(r => r.json());
            const response = await fetch(contextPath + "/api/comparison/history?id=" + encodeURIComponent(id),
                {method: "DELETE", headers: {"X-CSRF-Token": auth.csrfToken}});
            if (!response.ok || !(await response.json()).success) throw new Error("Không thể xóa lịch sử.");
        },
        async clear() {
            for (const row of historyRows) await this.remove(row.id);
        }
    };

    function updateHistoryBadges() {
        if (!isAuthenticated) return;
        const history = CompareHistory.getAll();
        const count = history.length;
        if (historyBadge) {
            historyBadge.textContent = String(count);
            historyBadge.hidden = count === 0;
        }
        if (dockHistoryCount) {
            dockHistoryCount.textContent = String(count);
        }
    }

    function renderHistoryDialog() {
        if (!historyDialogList) return;
        const history = CompareHistory.getAll();
        historyDialogList.replaceChildren();

        if (history.length === 0) {
            const empty = make("div", "history-empty");
            const icon = make("div", "history-empty-icon", "⚖");
            const title = make("h3", "", "Chưa có lịch sử so sánh");
            const msg = make("p", "", "Hãy chọn từ 2 đến 3 xe trong danh mục và bấm So sánh để lưu lịch sử tại đây.");
            empty.append(icon, title, msg);
            historyDialogList.append(empty);
            if (btnClearAllHistory) btnClearAllHistory.hidden = true;
            return;
        }

        if (btnClearAllHistory) btnClearAllHistory.hidden = false;

        history.forEach((item) => {
            const card = make("div", "history-card");

            const top = make("div", "history-card-top");
            const date = make("span", "history-date", item.dateStr || "Gần đây");
            const tag = make("span", "history-count-tag", `${item.vehicles?.length || 0} mẫu xe`);
            top.append(date, tag);

            const gridWrap = make("div", "history-vehicles-grid");
            (item.vehicles || []).forEach((v, idx) => {
                if (idx > 0) {
                    gridWrap.append(make("span", "history-vs", "vs"));
                }
                const vItem = make("div", "history-vehicle-item");
                if (v.imageUrl) {
                    const img = make("img");
                    img.src = v.imageUrl;
                    img.alt = v.modelName;
                    img.loading = "lazy";
                    img.addEventListener("error", () => img.remove(), { once: true });
                    vItem.append(img);
                }
                const info = make("div", "history-vehicle-info");
                info.append(
                    make("strong", "", v.modelName),
                    make("small", "", `${v.specificationVersion || ""} · ${v.priceText || ""}`)
                );
                vItem.append(info);
                gridWrap.append(vItem);
            });

            const bottom = make("div", "history-card-bottom");

            // 1. "Xem lại bảng so sánh"
            const viewLink = make("a", "btn-view-compare", "Xem bảng so sánh →");
            const params = new URLSearchParams();
            (item.keys || []).forEach((k) => params.append("key", k));
            viewLink.href = `${contextPath}/comparison?${params.toString()}`;

            // 2. "Nạp vào bộ chọn"
            const loadBtn = make("button", "btn-load-compare", "Nạp vào bộ chọn");
            loadBtn.type = "button";
            loadBtn.title = "Đưa các mẫu xe này vào thanh chọn so sánh ở dưới";
            loadBtn.addEventListener("click", () => {
                selections.clear();
                (item.keys || []).forEach((k) => selections.add(k));
                persistCompareSelections();
                render();
                syncCompare();
                compareHistoryDialog.close();
                showCompareNotice(`Đã nạp ${selections.size} xe vào thanh so sánh.`);
            });

            // 3. "Xóa"
            const deleteBtn = make("button", "btn-delete-history", "Xóa");
            deleteBtn.type = "button";
            deleteBtn.title = "Xóa phiên so sánh này khỏi lịch sử";
            deleteBtn.addEventListener("click", () => {
                CompareHistory.remove(item.id).then(() => CompareHistory.syncWithServer()).catch(e => alert(e.message));
                updateHistoryBadges();
                renderHistoryDialog();
            });

            bottom.append(loadBtn, viewLink, deleteBtn);
            card.append(top, gridWrap, bottom);
            historyDialogList.append(card);
        });
    }

    // =========================================================================
    // Native Select Multi-Select Sync (Hãng xe & Mức giá)
    // =========================================================================
    function syncModelFilterUI() {
        if (!modelFilter) return;
        for (let i = 1; i < modelFilter.options.length; i++) {
            const opt = modelFilter.options[i];
            const raw = opt.value;
            opt.text = selectedModels.has(raw) ? `✓ ${raw}` : raw;
        }

        if (selectedModels.size === 0) {
            modelFilter.options[0].value = "";
            modelFilter.options[0].text = "Tất cả dòng xe";
            modelFilter.value = "";
        } else if (selectedModels.size === 1) {
            const onlyModel = [...selectedModels][0];
            modelFilter.options[0].value = "";
            modelFilter.options[0].text = "Tất cả dòng xe (bỏ chọn)";
            modelFilter.value = onlyModel;
        } else {
            modelFilter.options[0].value = "__summary__";
            modelFilter.options[0].text = `Đã chọn ${selectedModels.size} xe: ${[...selectedModels].join(", ")}`;
            modelFilter.value = "__summary__";
        }
    }

    function syncPriceFilterUI() {
        if (!priceFilter) return;
        for (let i = 1; i < priceFilter.options.length; i++) {
            const opt = priceFilter.options[i];
            const r = PRICE_RANGES.find((item) => item.key === opt.value);
            const base = r ? r.label : opt.value;
            opt.text = selectedPrices.has(opt.value) ? `✓ ${base}` : base;
        }

        if (selectedPrices.size === 0) {
            priceFilter.options[0].value = "";
            priceFilter.options[0].text = "Tất cả mức giá";
            priceFilter.value = "";
        } else if (selectedPrices.size === 1) {
            const onlyKey = [...selectedPrices][0];
            priceFilter.options[0].value = "";
            priceFilter.options[0].text = "Tất cả mức giá (bỏ chọn)";
            priceFilter.value = onlyKey;
        } else {
            priceFilter.options[0].value = "__summary__";
            priceFilter.options[0].text = `Đã chọn ${selectedPrices.size} mức giá`;
            priceFilter.value = "__summary__";
        }
    }

    function populateFilters() {
        const lines = [...new Set(models.map(lineName).filter(Boolean))].sort((a, b) => a.localeCompare(b, "vi"));
        if (modelFilter) {
            modelFilter.replaceChildren(new Option("Tất cả dòng xe", ""));
            lines.forEach((line) => modelFilter.add(new Option(line, line)));
        }

        const colors = [
            ...new Set(
                models
                    .flatMap((model) => (Array.isArray(model.colors) ? model.colors : []))
                    .map(text)
                    .filter(Boolean)
            )
        ].sort((a, b) => a.localeCompare(b, "vi"));

        if (colorFilter) {
            colorFilter.replaceChildren(new Option("Tất cả màu sắc", ""));
            colors.forEach((color) => colorFilter.add(new Option(color, color)));
        }
    }

    // =========================================================================
    // Filter Matching Logic
    // =========================================================================
    function matches(model) {
        const rawSearch = search ? search.value.trim() : "";
        const searchText = normalizeSearch(rawSearch);

        // 1. Model filter: If models selected, must belong to selectedModels
        if (selectedModels.size > 0 && !selectedModels.has(lineName(model))) {
            return false;
        }

        // 2. Price filter: If price ranges selected, must match at least one
        let amount = number(model.priceVnd);
        if (Array.isArray(model.promotions) && model.promotions.length > 0 && amount && amount > 0) {
            const promo = model.promotions[0];
            const dv = Number(promo.discountValue || 0);
            if (promo.discountType === "PERCENTAGE" && dv > 0) {
                amount = Math.max(0, amount - Math.round((amount * dv) / 100));
            } else if (dv > 0) {
                amount = Math.max(0, amount - Math.round(dv));
            }
        }
        if (selectedPrices.size > 0) {
            const matchesAnyPrice = [...selectedPrices].some((key) => {
                const r = PRICE_RANGES.find((item) => item.key === key);
                return r ? (r.match(amount) || r.match(number(model.priceVnd))) : false;
            });
            if (!matchesAnyPrice) return false;
        }

        // 3. Range filter
        const minimumRange = rangeFilter ? number(rangeFilter.value) : null;
        if (minimumRange !== null && (number(model.rangeKm) === null || Number(model.rangeKm) < minimumRange)) {
            return false;
        }

        // 4. Color filter
        if (colorFilter && colorFilter.value && (!Array.isArray(model.colors) || !model.colors.some((color) =>
            colorFamily(color) === colorFamily(colorFilter.value)
        ))) {
            return false;
        }

        // 5. Search text filter
        if (searchText) {
            // If explicit model filter is active and the search text was just an old car model keyword,
            // avoid collision (so selecting VF6 doesn't filter for VF3)
            if (selectedModels.size > 0 && isModelKeyword(rawSearch)) {
                return true;
            }

            const fields = [
                model.modelName,
                model.carLine,
                model.specificationVersion,
                model.dimensions,
                ...(Array.isArray(model.colors) ? model.colors : [])
            ].map(normalizeSearch);

            if (!fields.some((value) => value.includes(searchText))) {
                return false;
            }
        }

        return true;
    }

    function sorted(items) {
        const result = [...items];
        const sortVal = sortFilter ? sortFilter.value : "recommended";
        if (sortVal === "price-asc") {
            result.sort((a, b) => (number(a.priceVnd) ?? Infinity) - (number(b.priceVnd) ?? Infinity));
        }
        if (sortVal === "price-desc") {
            result.sort((a, b) => (number(b.priceVnd) ?? -Infinity) - (number(a.priceVnd) ?? -Infinity));
        }
        if (sortVal === "range-desc") {
            result.sort((a, b) => (number(b.rangeKm) ?? -Infinity) - (number(a.rangeKm) ?? -Infinity));
        }
        return result;
    }

    function card(model, index) {
        const id = text(model.comparisonKey) || `${text(model.modelName)}-${text(model.specificationVersion)}`;
        const selected = selections.has(id);
        const article = make("article", `vehicle-card${selected ? " selected-for-compare" : ""}`);
        const photo = make("div", "vehicle-photo");
        const badge = make("span", "model-badge", className(model));
        const stockColors = inventoryByModel.get(Number(model.modelID)) || [];
        const modelColorList = [
            ...stockColors.map(({ color }) => color),
            ...(Array.isArray(model.colors) ? model.colors : [])
        ];
        const uniqueColors = [...new Set(modelColorList)].filter(Boolean);
        const selectedColor = (colorFilter && colorFilter.value
            ? uniqueColors.find((color) => colorFamily(color) === colorFamily(colorFilter.value))
            : "") || uniqueColors[0] || "";
        const imageUrl = colorImageFor(model, selectedColor) || safeOfficialUrl(model.imageUrl);
        let image = null;

        if (imageUrl) {
            image = make("img");
            image.src = imageUrl;
            image.alt = selectedColor ? `${text(model.modelName)} – ${selectedColor}` : `${text(model.modelName)} – xe điện VinFast`;
            image.loading = index < 3 ? "eager" : "lazy";
            image.decoding = "async";
            image.referrerPolicy = "no-referrer";
            image.addEventListener("error", () => image.remove(), { once: true });
            photo.append(image);
        }
        const detailLink = make("a", "vehicle-detail-link", "Xem chi tiết ↗");
        const detailParams = new URLSearchParams();
        const modelID = Number(model.modelID);
        if (Number.isInteger(modelID) && modelID > 0) detailParams.set("id", String(modelID));
        else if (text(model.carLine)) detailParams.set("line", text(model.carLine));
        else detailParams.set("name", text(model.modelName));
        detailLink.href = `${contextPath}/detail?${detailParams.toString()}`;
        photo.append(detailLink);

        photo.append(badge);

        const body = make("div", "vehicle-card-body");
        const title = make("h3", "vehicle-name", text(model.modelName));
        const version = make("p", "vehicle-version", text(model.specificationVersion) || "Phiên bản theo danh mục");
        const specs = make("div", "vehicle-specs");
        [
            ["Quãng đường", range(model)],
            ["Pin", battery(model)],
            ["Chỗ ngồi", seats(model)]
        ].forEach(([label, value]) => {
            const item = make("div", "spec-item");
            item.append(make("span", "", label), make("strong", "", value));
            specs.append(item);
        });
        // Promotion calculation
        const promos = Array.isArray(model.promotions) ? model.promotions : [];
        const activePromo = promos.length > 0 ? promos[0] : null;
        const originalPrice = number(model.priceVnd);
        let finalPrice = originalPrice;
        let discountAmount = 0;
        let discountBadgeText = "";

        if (activePromo && originalPrice && originalPrice > 0) {
            const discVal = Number(activePromo.discountValue || 0);
            if (activePromo.discountType === "PERCENTAGE" && discVal > 0) {
                discountAmount = Math.round((originalPrice * discVal) / 100);
                finalPrice = Math.max(0, originalPrice - discountAmount);
                discountBadgeText = `-${discVal}%`;
            } else if (discVal > 0) {
                discountAmount = Math.round(discVal);
                finalPrice = Math.max(0, originalPrice - discountAmount);
                discountBadgeText = `-${price(discVal)}`;
            }
        }

        if (activePromo && discountAmount > 0) {
            const promoBadge = make("span", "promo-badge", `🎁 ${text(activePromo.name)} ${discountBadgeText}`);
            photo.append(promoBadge);
        }

        const priceWrap = make("div", "vehicle-price-wrap");
        if (activePromo && discountAmount > 0) {
            const priceLabel = make("span", "vehicle-price-label", "Giá sau ưu đãi · đã gồm pin");
            const priceRow = make("div", "vehicle-price-row");
            const priceValue = make("strong", "vehicle-price has-discount", price(finalPrice));
            const priceOriginal = make("del", "vehicle-price-original", price(originalPrice));
            const discountPill = make("span", "promo-tag-pill", discountBadgeText);
            priceRow.append(priceValue, priceOriginal, discountPill);

            const promoCalc = make("div", "vehicle-promo-calc", `🎁 ${text(activePromo.name)}: Trừ ${price(discountAmount)} vào giá xe`);
            priceWrap.append(priceLabel, priceRow, promoCalc);
        } else {
            const priceLabel = make("span", "vehicle-price-label", "Giá tham khảo · đã gồm pin");
            const priceValue = make("strong", "vehicle-price", price(model.priceVnd));
            priceWrap.append(priceLabel, priceValue);
        }

        const colorPicker = make("label", "vehicle-color-picker");
        colorPicker.append(make("span", "", "Màu xe"));
        const colorSelect = make("select", "vehicle-color-select");
        uniqueColors.forEach((color) => colorSelect.add(new Option(color, color)));
        if (selectedColor) colorSelect.value = selectedColor;
        colorSelect.disabled = uniqueColors.length === 0;
        colorPicker.append(colorSelect);

        colorSelect.addEventListener("change", () => {
            const nextImage = colorImageFor(model, colorSelect.value) || safeOfficialUrl(model.imageUrl);
            if (image && nextImage) {
                image.src = nextImage;
                image.alt = colorSelect.value ? `${text(model.modelName)} – ${colorSelect.value}` : `${text(model.modelName)} – xe điện VinFast`;
            }
        });
        const footer = make("div", "vehicle-card-footer");
        const sourceUrl = safeOfficialUrl(model.priceSourceUrl || catalogueMetadata.pricePolicyPageUrl);
        if (sourceUrl) {
            const source = make("a", "vehicle-source", "Nguồn giá VinFast ↗");
            source.href = sourceUrl;
            source.target = "_blank";
            source.rel = "noopener noreferrer";
            footer.append(source);
        } else {
            footer.append(make("span", "vehicle-source", "Thông tin tham khảo"));
        }
        const compareButton = make(
            "button",
            `quick-compare${selected ? " selected" : ""}`,
            selected ? "Đã chọn ✓" : "+ So sánh"
        );
        compareButton.type = "button";
        compareButton.setAttribute("aria-pressed", String(selected));
        compareButton.addEventListener("click", () => toggleCompare(id, model));
        footer.append(compareButton);
        const hasStock = Number(inventoryCountByModel.get(modelID) || 0) > 0
            || (Array.isArray(model.colors) && model.colors.length > 0)
            || model.stockCarID != null;
        const stockInfo = hasStock ? make("p", "home-stock is-available", "Xe đang có sẵn") : null;
        body.append(title, version, specs, colorPicker, ...(stockInfo ? [stockInfo] : []), priceWrap, footer);
        article.append(photo, body);
        return article;
    }

    // =========================================================================
    // Active Filter Chips ("khi KH không chọn nữa thì tự động trừ đi")
    // =========================================================================
    function filterChips() {
        if (!activeFilters) return;
        activeFilters.replaceChildren();

        // 1. Dòng xe chips
        selectedModels.forEach((model) => {
            const chip = make("span", "filter-chip", model);
            const remove = make("button", "", "×");
            remove.type = "button";
            remove.setAttribute("aria-label", `Bỏ lọc dòng xe ${model}`);
            remove.addEventListener("click", () => {
                selectedModels.delete(model);
                if (selectedModels.size === 0 && search.value && isModelKeyword(search.value)) {
                    search.value = "";
                }
                syncModelFilterUI();
                render();
            });
            chip.append(remove);
            activeFilters.append(chip);
        });

        // 2. Mức giá chips
        selectedPrices.forEach((key) => {
            const r = PRICE_RANGES.find((item) => item.key === key);
            const label = r ? r.label : key;
            const chip = make("span", "filter-chip", label);
            const remove = make("button", "", "×");
            remove.type = "button";
            remove.setAttribute("aria-label", `Bỏ lọc mức giá ${label}`);
            remove.addEventListener("click", () => {
                selectedPrices.delete(key);
                syncPriceFilterUI();
                render();
            });
            chip.append(remove);
            activeFilters.append(chip);
        });

        // 3. Quãng đường chip
        if (rangeFilter && rangeFilter.value) {
            const label = rangeFilter.options[rangeFilter.selectedIndex]?.text;
            const chip = make("span", "filter-chip", label);
            const remove = make("button", "", "×");
            remove.type = "button";
            remove.setAttribute("aria-label", `Bỏ lọc quãng đường ${label}`);
            remove.addEventListener("click", () => {
                rangeFilter.value = "";
                render();
            });
            chip.append(remove);
            activeFilters.append(chip);
        }

        // 4. Màu sắc chip
        if (colorFilter && colorFilter.value) {
            const label = colorFilter.options[colorFilter.selectedIndex]?.text;
            const chip = make("span", "filter-chip", `Màu: ${label}`);
            const remove = make("button", "", "×");
            remove.type = "button";
            remove.setAttribute("aria-label", `Bỏ lọc màu ${label}`);
            remove.addEventListener("click", () => {
                colorFilter.value = "";
                render();
            });
            chip.append(remove);
            activeFilters.append(chip);
        }

        // 5. Từ khóa tìm kiếm chip
        if (search.value.trim()) {
            const keyword = search.value.trim();
            const chip = make("span", "filter-chip", `Từ khóa: ${keyword}`);
            const remove = make("button", "", "×");
            remove.type = "button";
            remove.setAttribute("aria-label", `Xóa từ khóa ${keyword}`);
            remove.addEventListener("click", () => {
                search.value = "";
                render();
            });
            chip.append(remove);
            activeFilters.append(chip);
        }
    }

    function render() {
        if (!grid) return;
        if (catalogueStatus !== "ready") {
            return;
        }
        const visible = sorted(models.filter(matches));
        grid.replaceChildren(...visible.map(card));
        grid.setAttribute("aria-busy", "false");
        grid.hidden = visible.length === 0;
        const visibleLines = [...new Set(visible.map((model) => lineKey(lineName(model))))];
        const inStock = inventoryAvailable
            ? visibleLines.reduce((total, key) => total + (inventoryByLine.get(key) || 0), 0)
            : null;
        if (summary) {
            summary.textContent = `Có ${visible.length} phiên bản xe trong danh mục${visible.length !== models.length ? ` · tổng ${models.length} phiên bản` : ""}.`;
        }
        filterChips();
    }

    function syncCompare() {
        const compareCountEl = document.getElementById("compare-count");
        if (compareCountEl) compareCountEl.textContent = String(selections.size);
        if (compareDock) compareDock.hidden = selections.size === 0;
        const pageLink = document.getElementById("compare-page-link");
        if (pageLink) pageLink.hidden = selections.size < 2;
    }

    function persistCompareSelections() {
        try {
            sessionStorage.setItem(compareSelectionStorageKey, JSON.stringify([...selections]));
        } catch (error) {
            console.error("Không thể lưu các xe đã chọn để so sánh.", error);
        }
    }

    function showCompareNotice(message) {
        if (!compareNotice) return;
        compareNotice.textContent = message;
        compareNotice.hidden = false;
        window.clearTimeout(compareNoticeTimer);
        compareNoticeTimer = window.setTimeout(() => { compareNotice.hidden = true; }, 2800);
    }

    function toggleCompare(id, model) {
        if (selections.has(id)) {
            selections.delete(id);
        } else if (selections.size < 3) {
            selections.add(id);
        } else {
            showCompareNotice("Bạn chỉ có thể chọn tối đa 3 xe để so sánh.");
            render();
            return;
        }
        persistCompareSelections();
        render();
        syncCompare();
    }

    function openComparison() {
        const selectedModelsList = [...selections]
            .map((id) => models.find((model) => (text(model.comparisonKey) || `${text(model.modelName)}-${text(model.specificationVersion)}`) === id))
            .filter(Boolean);
        if (selectedModelsList.length === 0) {
            showCompareNotice("Vui lòng chọn xe để so sánh.");
            return;
        }
        if (selectedModelsList.length === 1) {
            showCompareNotice("Vui lòng chọn thêm ít nhất 1 xe nữa để so sánh (tối thiểu 2 xe).");
            return;
        }
        persistCompareSelections();

        const comparisonQuery = new URLSearchParams();
        selectedModelsList.forEach((model) => comparisonQuery.append("key", text(model.comparisonKey)
            || `${text(model.modelName)}-${text(model.specificationVersion)}`));
        window.location.href = `${contextPath}/comparison?${comparisonQuery.toString()}`;
    }

    function resetFilters() {
        search.value = "";
        selectedModels.clear();
        selectedPrices.clear();
        rangeFilter.value = "";
        colorFilter.value = "";
        sortFilter.value = "recommended";
        syncModelFilterUI();
        syncPriceFilterUI();
        if (catalogueStatus === "failed") {
            loadCatalogue();
        } else {
            render();
        }
    }

    async function fetchWithTimeout(url, options = {}, timeoutMs = 2500) {
        if (typeof AbortController === "undefined") {
            return fetch(url, options);
        }
        const controller = new AbortController();
        const timerId = setTimeout(() => controller.abort(), timeoutMs);
        try {
            const res = await fetch(url, { ...options, signal: controller.signal });
            clearTimeout(timerId);
            return res;
        } catch (err) {
            clearTimeout(timerId);
            throw err;
        }
    }

    function applyAndRenderModels(rawModels, metadata) {
        if (!Array.isArray(rawModels) || rawModels.length === 0) return;
        catalogueMetadata = metadata || catalogueMetadata || {};
        models = rawModels.map((model) => {
            const key = lineKey(lineName(model));
            return {
                ...model,
                comparisonKey: text(model.comparisonKey) || `${text(model.modelName)}-${text(model.specificationVersion)}`,
                inventoryConnected: inventoryAvailable,
                availableCars: inventoryByLine.get(key) || 0
            };
        });
        const validKeys = new Set(models.map((model) => model.comparisonKey));
        for (const key of selections) if (!validKeys.has(key)) selections.delete(key);
        persistCompareSelections();
        populateFilters();
        const banners = ["VF 3", "VF 7", "VF 8"];
        banners.forEach((name, index) => {
            const model =
                models.find((item) => lineName(item).toUpperCase() === name) ||
                models.find((item) => text(item.modelName).toUpperCase().startsWith(name));
            const image = document.querySelector(`[data-banner="${index}"] img`);
            const officialImage = safeOfficialUrl(model?.imageUrl);
            if (image && officialImage) {
                image.src = officialImage;
            }
        });
        const disclaimerEl = document.getElementById("catalog-disclaimer");
        if (disclaimerEl) {
            disclaimerEl.textContent = text(catalogueMetadata.note)
                || "Giá và thông số hiển thị theo dữ liệu tham khảo của từng cấu hình; vui lòng đối chiếu với VinFast trước khi giao dịch.";
        }
        catalogueStatus = "ready";
        if (grid) grid.setAttribute("aria-busy", "false");
        render();
        syncCompare();
        if (isAuthenticated) {
            updateHistoryBadges();
        }
    }

    async function loadCatalogue() {
        catalogueStatus = "loading";
        if (grid) {
            grid.hidden = false;
            grid.setAttribute("aria-busy", "true");
        }
        if (summary) summary.textContent = "Đang tải danh mục xe...";

        try {
            const response = await fetchWithTimeout(databaseCatalogueUrl, { credentials: "same-origin" }, 5000);
            if (!response.ok) throw new Error("catalogue");
            const catalogue = await response.json();
            if (!Array.isArray(catalogue.models)) throw new Error("catalogue");
            applyAndRenderModels(catalogue.models, catalogue);
        } catch (error) {
            catalogueStatus = "failed";
            if (grid) grid.setAttribute("aria-busy", "false");
            render();
            if (summary) summary.textContent = "Không thể tải danh mục xe. Vui lòng thử lại.";
            return;
        }

        // Tải tồn kho xe từ /api/ai
        try {
            const inventoryRes = await fetchWithTimeout(`${contextPath}/api/ai`, {
                credentials: "same-origin",
                headers: { Accept: "application/json" }
            }, 3000);
            if (inventoryRes.ok) {
                const inventoryPayload = await inventoryRes.json();
                inventoryByLine = new Map();
                inventoryByModel = new Map();
                inventoryCountByModel = new Map();
                for (const vehicle of (inventoryPayload.models || [])) {
                    const key = lineKey(vehicle.carLineName || vehicle.modelName);
                    if (!key) continue;
                    const availableCount = Number(vehicle.availableCars || 0);
                    inventoryByLine.set(key, (inventoryByLine.get(key) || 0) + availableCount);
                    const modelId = Number(vehicle.modelId);
                    if (Number.isInteger(modelId) && modelId > 0) {
                        inventoryCountByModel.set(modelId, (inventoryCountByModel.get(modelId) || 0) + availableCount);
                        const colors = Object.entries(vehicle.inventoryByColor || {})
                            .map(([color, count]) => ({ color: text(color), count: Number(count) }))
                            .filter(({ color, count }) => color && Number.isFinite(count) && count > 0)
                            .sort((a, b) => a.color.localeCompare(b.color, "vi"));
                        inventoryByModel.set(modelId, colors);
                    }
                }
                inventoryAvailable = true;
                models.forEach((m) => {
                    const key = lineKey(lineName(m));
                    m.inventoryConnected = true;
                    m.availableCars = inventoryByLine.get(key) || 0;
                });
                render();
            }
        } catch (invErr) {
            console.info("Inventory unavailable or timed out.", invErr);
        }

        if (isAuthenticated) {
            CompareHistory.syncWithServer().catch(e => showCompareNotice(e.message));
        }
    }

    // =========================================================================
    // Filter Change Event Handlers
    // =========================================================================
    if (modelFilter) {
        modelFilter.addEventListener("change", () => {
            const val = modelFilter.value;
            // Clear conflicting search keyword if user selects a model
            if (search.value && isModelKeyword(search.value)) {
                search.value = "";
            }

            if (!val) {
                // If selected "Tất cả dòng xe", clear all model filters
                selectedModels.clear();
            } else if (val === "__summary__") {
                return;
            } else {
                // Toggle selection
                if (selectedModels.has(val)) {
                    selectedModels.delete(val);
                } else {
                    selectedModels.add(val);
                }
            }
            syncModelFilterUI();
            render();
        });
    }

    if (priceFilter) {
        priceFilter.addEventListener("change", () => {
            const val = priceFilter.value;
            if (!val) {
                selectedPrices.clear();
            } else if (val === "__summary__") {
                return;
            } else {
                if (selectedPrices.has(val)) {
                    selectedPrices.delete(val);
                } else {
                    selectedPrices.add(val);
                }
            }
            syncPriceFilterUI();
            render();
        });
    }

    [rangeFilter, colorFilter, sortFilter].forEach((control) => {
        if (control) control.addEventListener("change", render);
    });

    if (search) {
        search.addEventListener("input", () => {
            const val = search.value.trim();
            if (val && isModelKeyword(val) && selectedModels.size > 0) {
                const norm = normalizeSearch(val);
                const matchesSelected = [...selectedModels].some((m) => {
                    const mn = normalizeSearch(m);
                    return mn.includes(norm) || norm.includes(mn);
                });
                if (!matchesSelected) {
                    // User searched a specific model that conflicts with currently filtered models
                    selectedModels.clear();
                    syncModelFilterUI();
                }
            }
            render();
        });
    }

    const searchForm = document.getElementById("search-form");
    if (searchForm) {
        searchForm.addEventListener("submit", (event) => {
            event.preventDefault();
            const keyword = normalizeSearch(search ? search.value : "");
            const exactMatch = keyword && models.find((model) =>
                [lineName(model), model.modelName].some((value) => normalizeSearch(value) === keyword)
            );
            if (exactMatch) {
                const params = new URLSearchParams();
                const modelID = Number(exactMatch.modelID);
                if (Number.isInteger(modelID) && modelID > 0) params.set("id", String(modelID));
                else if (text(exactMatch.carLine)) params.set("line", text(exactMatch.carLine));
                else params.set("name", text(exactMatch.modelName));
                window.location.href = `${contextPath}/detail?${params.toString()}`;
                return;
            }
            render();
        });
    }

    const resetBtn = document.getElementById("reset-filters");
    if (resetBtn) resetBtn.addEventListener("click", resetFilters);

    // Banners: Selecting a banner clears conflicting search and filters to that model
    document.querySelectorAll("[data-banner][data-model]").forEach((banner) => {
        banner.addEventListener("click", (event) => {
            event.preventDefault();
            const model = banner.dataset.model;
            if (search) search.value = "";
            selectedModels.clear();
            selectedModels.add(model);
            syncModelFilterUI();
            render();
            document.getElementById("models")?.scrollIntoView({ behavior: "smooth", block: "start" });
        });
    });

    const compareOpenBtn = document.getElementById("compare-open");
    if (compareOpenBtn) compareOpenBtn.addEventListener("click", openComparison);

    const compareClearBtn = document.getElementById("compare-clear");
    if (compareClearBtn) {
        compareClearBtn.addEventListener("click", () => {
            selections.clear();
            persistCompareSelections();
            if (compareNotice) compareNotice.hidden = true;
            render();
            syncCompare();
        });
    }

    const compareCloseBtn = document.getElementById("compare-close");
    if (compareCloseBtn) compareCloseBtn.addEventListener("click", () => compareDialog?.close());
    if (compareDialog) {
        compareDialog.addEventListener("click", (event) => {
            if (event.target === compareDialog) {
                compareDialog.close();
            }
        });
    }

    // Comparison History Dialog Events
    function openHistoryModal() {
        renderHistoryDialog();
        if (compareHistoryDialog) compareHistoryDialog.showModal();
    }

    if (openHistoryBtn) openHistoryBtn.addEventListener("click", openHistoryModal);
    if (compareDockHistory) compareDockHistory.addEventListener("click", openHistoryModal);
    if (historyCloseBtn) historyCloseBtn.addEventListener("click", () => compareHistoryDialog?.close());
    if (historyDoneBtn) historyDoneBtn.addEventListener("click", () => compareHistoryDialog?.close());
    if (compareHistoryDialog) {
        compareHistoryDialog.addEventListener("click", (e) => {
            if (e.target === compareHistoryDialog) compareHistoryDialog.close();
        });
    }

    if (btnClearAllHistory) {
        btnClearAllHistory.addEventListener("click", () => {
            if (confirm("Bạn có chắc chắn muốn xóa tất cả lịch sử so sánh xe?")) {
                CompareHistory.clear().then(() => CompareHistory.syncWithServer()).catch(e => alert(e.message));
                updateHistoryBadges();
                renderHistoryDialog();
            }
        });
    }

    const menuButton = document.querySelector(".mobile-menu-toggle");
    const nav = document.getElementById("primary-nav");
    if (menuButton && nav) {
        menuButton.addEventListener("click", () => {
            const expanded = menuButton.getAttribute("aria-expanded") === "true";
            menuButton.setAttribute("aria-expanded", String(!expanded));
            nav.classList.toggle("open", !expanded);
        });
        nav.addEventListener("click", (event) => {
            if (event.target.closest("a")) {
                menuButton.setAttribute("aria-expanded", "false");
                nav.classList.remove("open");
            }
        });
    }

    // Tải danh mục ngay khi script nạp xong
    loadCatalogue();
})();
