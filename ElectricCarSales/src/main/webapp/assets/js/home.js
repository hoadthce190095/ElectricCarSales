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
    const emptyState = document.getElementById("empty-state");
    const compareDock = document.getElementById("compare-dock");
    const compareDialog = document.getElementById("compare-dialog");
    const compareTable = document.getElementById("compare-table-wrap");
    const compareNotice = document.getElementById("compare-notice");
    const selections = new Set();
    const compareSelectionStorageKey = "electriccarsale.compareSelections";
    let models = [];
    let catalogueMetadata = {};
    let catalogueStatus = "loading";
    let catalogueDatabaseConnected = false;
    let inventoryAvailable = false;
    let inventoryByLine = new Map();
    let compareNoticeTimer;

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

    function populateFilters() {
        const lines = [...new Set(models.map(lineName).filter(Boolean))];
        lines.forEach((line) => modelFilter.add(new Option(line, line)));
        const colors = [
            ...new Set(
                models
                    .flatMap((model) => (Array.isArray(model.colors) ? model.colors : []))
                    .map(text)
                    .filter(Boolean)
            )
        ].sort((a, b) => a.localeCompare(b, "vi"));
        colors.forEach((color) => colorFilter.add(new Option(color, color)));
    }

    function matches(model) {
        const searchText = normalizeSearch(search.value);
        const fields = [
            model.modelName,
            model.carLine,
            model.specificationVersion,
            model.dimensions,
            ...(Array.isArray(model.colors) ? model.colors : [])
        ].map(normalizeSearch);
        const amount = number(model.priceVnd);
        const maxPriceMillions = number(priceFilter.value);
        const minimumRange = number(rangeFilter.value);
        return (
            (!searchText || fields.some((value) => value.includes(searchText))) &&
            (!modelFilter.value || lineName(model) === modelFilter.value) &&
            (maxPriceMillions === null || amount === null || amount <= maxPriceMillions * 1_000_000) &&
            (minimumRange === null || number(model.rangeKm) === null || Number(model.rangeKm) >= minimumRange) &&
            (!colorFilter.value || (Array.isArray(model.colors) && model.colors.some((color) =>
                colorFamily(color) === colorFamily(colorFilter.value)
            )))
        );
    }

    function sorted(items) {
        const result = [...items];
        if (sortFilter.value === "price-asc") {
            result.sort((a, b) => (number(a.priceVnd) ?? Infinity) - (number(b.priceVnd) ?? Infinity));
        }
        if (sortFilter.value === "price-desc") {
            result.sort((a, b) => (number(b.priceVnd) ?? -Infinity) - (number(a.priceVnd) ?? -Infinity));
        }
        if (sortFilter.value === "range-desc") {
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
        const availableColors = Array.isArray(model.colors) ? model.colors.map(text).filter(Boolean) : [];
        const selectedColor = colorFilter.value
            ? availableColors.find((color) => colorFamily(color) === colorFamily(colorFilter.value)) || ""
            : "";
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

        const compareLabel = make("label", "compare-check");
        const checkbox = make("input");
        checkbox.type = "checkbox";
        checkbox.checked = selected;
        checkbox.setAttribute("aria-label", `Chọn ${text(model.modelName)} để so sánh`);
        checkbox.addEventListener("change", () => toggleCompare(id, model));
        compareLabel.append(checkbox, document.createTextNode("So sánh"));
        photo.append(badge, compareLabel);

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
        const priceLabel = make("span", "vehicle-price-label", "Giá tham khảo · đã gồm pin");
        const priceValue = make("strong", "vehicle-price", price(model.priceVnd));
        const colorPicker = make("label", "vehicle-color-picker");
        colorPicker.append(make("span", "", "Màu xe"));
        const colorSelect = make("select", "vehicle-color-select");
        colorSelect.add(new Option("Màu mặc định", ""));
        availableColors.forEach((color) => colorSelect.add(new Option(color, color)));
        if (selectedColor) colorSelect.value = selectedColor;
        colorSelect.disabled = availableColors.length === 0;
        colorSelect.addEventListener("change", () => {
            if (!image) return;
            const nextImage = colorImageFor(model, colorSelect.value) || safeOfficialUrl(model.imageUrl);
            if (!nextImage) return;
            image.src = nextImage;
            image.alt = colorSelect.value ? `${text(model.modelName)} – ${colorSelect.value}` : `${text(model.modelName)} – xe điện VinFast`;
        });
        colorPicker.append(colorSelect);
        const stockCount = number(model.availableCars);
        const stockText = !model.inventoryConnected
            ? "Chưa kết nối được dữ liệu tồn kho"
            : stockCount > 0
                ? `${stockCount} xe sẵn có trong kho (mọi phiên bản dòng ${lineName(model)})`
                : `Hiện không có xe sẵn có trong kho dòng ${lineName(model)}`;
        const stockInfo = make("p", `home-stock${model.inventoryConnected && stockCount > 0 ? " is-available" : ""}`, stockText);
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
        body.append(title, version, specs, colorPicker, stockInfo, priceLabel, priceValue, footer);
        article.append(photo, body);
        return article;
    }

    function filterChips() {
        activeFilters.replaceChildren();
        const values = [
            ["model", modelFilter.value, modelFilter.options[modelFilter.selectedIndex]?.text],
            ["price", priceFilter.value, priceFilter.options[priceFilter.selectedIndex]?.text],
            ["range", rangeFilter.value, rangeFilter.options[rangeFilter.selectedIndex]?.text],
            ["color", colorFilter.value, colorFilter.options[colorFilter.selectedIndex]?.text],
            ["search", search.value, search.value ? `Từ khóa: ${search.value}` : ""]
        ].filter((item) => item[1]);
        values.forEach(([key, , label]) => {
            const chip = make("span", "filter-chip", label);
            const remove = make("button", "", "×");
            remove.type = "button";
            remove.setAttribute("aria-label", `Xóa bộ lọc ${label}`);
            remove.addEventListener("click", () => {
                if (key === "model") {
                    modelFilter.value = "";
                }
                if (key === "price") {
                    priceFilter.value = "";
                }
                if (key === "range") {
                    rangeFilter.value = "";
                }
                if (key === "color") {
                    colorFilter.value = "";
                }
                if (key === "search") {
                    search.value = "";
                }
                render();
            });
            chip.append(remove);
            activeFilters.append(chip);
        });
    }

    function render() {
        if (catalogueStatus !== "ready") {
            return;
        }
        const visible = sorted(models.filter(matches));
        grid.replaceChildren(...visible.map(card));
        grid.setAttribute("aria-busy", "false");
        emptyState.hidden = visible.length !== 0;
        grid.hidden = visible.length === 0;
        const visibleLines = [...new Set(visible.map((model) => lineKey(lineName(model))))];
        const inStock = inventoryAvailable
            ? visibleLines.reduce((total, key) => total + (inventoryByLine.get(key) || 0), 0)
            : null;
        summary.textContent = `Có ${visible.length} phiên bản xe trong danh mục${visible.length !== models.length ? ` · tổng ${models.length} phiên bản` : ""}`
            + (catalogueDatabaseConnected ? " · danh mục đồng bộ từ database" : " · đang dùng danh mục tham khảo do database chưa kết nối")
            + (inStock === null ? " · chưa tải được tồn kho" : ` · ${inStock} xe sẵn có trong kho theo các dòng đang hiển thị`)
            + ".";
        filterChips();
    }

    function syncCompare() {
        document.getElementById("compare-count").textContent = String(selections.size);
        compareDock.hidden = selections.size === 0;
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
        const selectedModels = [...selections]
            .map((id) => models.find((model) => (text(model.comparisonKey) || `${text(model.modelName)}-${text(model.specificationVersion)}`) === id))
            .filter(Boolean);
        if (selectedModels.length < 2) {
            showCompareNotice("Vui lòng chọn ít nhất 2 xe để so sánh.");
            return;
        }
        const comparisonQuery = new URLSearchParams();
        selectedModels.forEach((model) => comparisonQuery.append("key", text(model.comparisonKey)
            || `${text(model.modelName)}-${text(model.specificationVersion)}`));
        const pageLink = document.getElementById("compare-page-link");
        if (pageLink) pageLink.href = `${contextPath}/comparison?${comparisonQuery.toString()}`;
        const table = make("table", "compare-table");
        const head = make("tr");
        head.append(make("th", "", "Thông tin"));
        selectedModels.forEach((model) => head.append(make("th", "", text(model.modelName))));
        const thead = make("thead");
        thead.append(head);
        const tbody = make("tbody");
        const rows = [
            ["Phiên bản", (model) => text(model.specificationVersion) || "—"],
            ["Giá tham khảo", (model) => price(model.priceVnd)],
            ["Quãng đường công bố", range],
            ["Dung lượng pin", battery],
            ["Số chỗ ngồi", seats],
            ["Kích thước", (model) => text(model.dimensions) || "Đang cập nhật"],
            ["Tiêu chuẩn quãng đường", (model) => text(model.rangeStandard) || "Chưa ghi nhận"]
        ];
        rows.forEach(([label, value]) => {
            const row = make("tr");
            row.append(make("td", "", label));
            selectedModels.forEach((model) => row.append(make("td", "", value(model))));
            tbody.append(row);
        });
        table.append(thead, tbody);
        compareTable.replaceChildren(table);
        compareDialog.showModal();
    }

    function resetFilters() {
        search.value = "";
        modelFilter.value = "";
        priceFilter.value = "";
        rangeFilter.value = "";
        colorFilter.value = "";
        sortFilter.value = "recommended";
        if (catalogueStatus === "failed") {
            loadCatalogue();
        } else {
            render();
        }
    }

    async function loadCatalogue() {
        catalogueStatus = "loading";
        grid.hidden = false;
        grid.setAttribute("aria-busy", "true");
        const loadingCard = make("div", "loading-card");
        loadingCard.append(make("span", "loading-orbit"), make("span", "", "Đang tải danh sách xe..."));
        grid.replaceChildren(loadingCard);
        emptyState.hidden = true;
        summary.textContent = "Đang tải danh mục xe...";
        try {
            let referenceCatalogue = {};
            try {
                const referenceResponse = await fetch(catalogueUrl, {
                    credentials: "same-origin",
                    headers: { Accept: "application/json" }
                });
                if (referenceResponse.ok) referenceCatalogue = await referenceResponse.json();
            } catch (referenceError) {
                console.warn("Không tải được metadata danh mục dự phòng.", referenceError);
            }

            let productModels = [];
            try {
                const databaseResponse = await fetch(databaseCatalogueUrl, {
                    credentials: "same-origin",
                    cache: "no-cache",
                    headers: { Accept: "application/json" }
                });
                if (!databaseResponse.ok) throw new Error(`Vehicle catalogue request failed: ${databaseResponse.status}`);
                const databaseCatalogue = await databaseResponse.json();
                const databaseModels = Array.isArray(databaseCatalogue.models)
                    ? databaseCatalogue.models.filter((model) => text(model.modelName)) : [];
                catalogueDatabaseConnected = true;
                catalogueMetadata = { ...referenceCatalogue, ...databaseCatalogue };
                productModels = databaseModels.map((databaseModel) => {
                    const sameVersion = (referenceCatalogue.models || []).filter((item) =>
                        normalizeSearch(item.specificationVersion) === normalizeSearch(databaseModel.specificationVersion)
                    );
                    const referenceModel = sameVersion.find((item) =>
                        normalizeSearch(item.modelName) === normalizeSearch(databaseModel.modelName)
                        || (text(databaseModel.carLine) && normalizeSearch(item.carLine) === normalizeSearch(databaseModel.carLine))
                    ) || (referenceCatalogue.models || []).find((item) =>
                        normalizeSearch(item.modelName) === normalizeSearch(databaseModel.modelName)
                        || (text(databaseModel.carLine) && normalizeSearch(item.carLine) === normalizeSearch(databaseModel.carLine))
                    ) || {};
                    const merged = { ...referenceModel, ...databaseModel };
                    ["priceVnd", "dimensions", "batteryKwh", "rangeKm", "maxPowerKw", "seats", "rangeStandard",
                        "sourceUrl", "priceSourceUrl", "imageUrl", "wheelType", "airbagCount", "maxSpeedKmh",
                        "rapidChargeMinutes", "priceNote", "specificationNote"].forEach((field) => {
                        if ((merged[field] === null || merged[field] === undefined || merged[field] === "")
                            && referenceModel[field] !== null && referenceModel[field] !== undefined && referenceModel[field] !== "") {
                            merged[field] = referenceModel[field];
                        }
                    });
                    if (!Array.isArray(databaseModel.colors) || databaseModel.colors.length === 0) merged.colors = referenceModel.colors || [];
                    if (!databaseModel.colorImages || Object.keys(databaseModel.colorImages).length === 0) {
                        merged.colorImages = referenceModel.colorImages || {};
                    }
                    merged.comparisonKey = text(databaseModel.comparisonKey)
                        || `${text(databaseModel.modelName)}-${text(databaseModel.specificationVersion)}`;
                    return merged;
                });
            } catch (databaseCatalogueError) {
                catalogueDatabaseConnected = false;
                catalogueMetadata = referenceCatalogue;
                productModels = Array.isArray(referenceCatalogue.models)
                    ? referenceCatalogue.models.filter((model) => text(model.modelName)) : [];
                console.error("Không thể tải danh mục xe chính từ database; đang hiển thị dữ liệu tham khảo.", databaseCatalogueError);
            }
            inventoryAvailable = false;
            inventoryByLine = new Map();
            try {
                const inventoryResponse = await fetch(`${contextPath}/api/ai`, {
                    credentials: "same-origin",
                    headers: { Accept: "application/json" }
                });
                if (!inventoryResponse.ok) throw new Error(`Inventory request failed: ${inventoryResponse.status}`);
                const inventoryPayload = await inventoryResponse.json();
                for (const vehicle of (inventoryPayload.models || [])) {
                    const key = lineKey(vehicle.carLineName || vehicle.modelName);
                    if (!key) continue;
                    inventoryByLine.set(key, (inventoryByLine.get(key) || 0) + Number(vehicle.availableCars || 0));
                }
                inventoryAvailable = true;
            } catch (inventoryError) {
                console.error("Không thể tải tồn kho xe từ database.", inventoryError);
            }
            models = productModels.map((model) => {
                const key = lineKey(lineName(model));
                return {
                    ...model,
                    comparisonKey: text(model.comparisonKey) || `${text(model.modelName)}-${text(model.specificationVersion)}`,
                    inventoryConnected: inventoryAvailable,
                    availableCars: inventoryByLine.get(key) || 0
                };
            });
            if (!models.length) {
                throw new Error("No catalogue models are available");
            }
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
            const disclaimer = text(catalogueMetadata.note);
            document.getElementById("catalog-disclaimer").textContent =
                disclaimer ||
                "Giá và thông số hiển thị theo dữ liệu tham khảo của từng cấu hình; vui lòng đối chiếu với VinFast trước khi giao dịch.";
            emptyState.querySelector("h3").textContent = "Chưa tìm thấy mẫu xe phù hợp";
            emptyState.querySelector("p").textContent = "Thử thay đổi từ khóa hoặc bộ lọc để xem thêm lựa chọn.";
            document.getElementById("empty-reset").textContent = "Xóa bộ lọc";
            catalogueStatus = "ready";
            render();
            syncCompare();
        } catch (error) {
            catalogueStatus = "failed";
            grid.setAttribute("aria-busy", "false");
            grid.replaceChildren();
            grid.hidden = true;
            emptyState.hidden = false;
            emptyState.querySelector("h3").textContent = "Chưa thể tải danh mục xe";
            emptyState.querySelector("p").textContent = "Vui lòng thử tải lại trang hoặc liên hệ để được hỗ trợ.";
            document.getElementById("empty-reset").textContent = "Thử tải lại";
            summary.textContent = "Danh mục xe hiện chưa sẵn sàng.";
            console.error("Không thể tải danh mục xe VinFast.", error);
        }
    }

    [modelFilter, priceFilter, rangeFilter, colorFilter, sortFilter].forEach((control) =>
        control.addEventListener("change", render)
    );
    search.addEventListener("input", render);
    document.getElementById("search-form").addEventListener("submit", (event) => {
        event.preventDefault();
        const keyword = normalizeSearch(search.value);
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
    document.getElementById("reset-filters").addEventListener("click", resetFilters);
    document.getElementById("empty-reset").addEventListener("click", resetFilters);
    document.getElementById("compare-open").addEventListener("click", openComparison);
    document.getElementById("compare-clear").addEventListener("click", () => {
        selections.clear();
        persistCompareSelections();
        if (compareNotice) compareNotice.hidden = true;
        render();
        syncCompare();
    });
    document.getElementById("compare-close").addEventListener("click", () => compareDialog.close());
    compareDialog.addEventListener("click", (event) => {
        if (event.target === compareDialog) {
            compareDialog.close();
        }
    });
    const menuButton = document.querySelector(".mobile-menu-toggle");
    const nav = document.getElementById("primary-nav");
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
    loadCatalogue();
})();
