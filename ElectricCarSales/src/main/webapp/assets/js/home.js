"use strict";

(() => {
<<<<<<< HEAD
    const contextPath = document.querySelector(".brand")?.getAttribute("href")?.replace(/\/index\.jsp$/, "") || "";
    const catalogueUrl = `${contextPath}/assets/vinfast-catelogue.json`;
=======
    const contextPath = document.querySelector('meta[name="app-context-path"]')?.content || "";
    const catalogueUrl = `${contextPath}/assets/vinfast-catalogue.json`;
>>>>>>> origin/Phuc-Tan
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
    const selections = new Set();
    let models = [];
    let catalogueMetadata = {};
<<<<<<< HEAD

    const text = (value) => String(value ?? "").trim();
    const number = (value) => {
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : null;
    };
=======
    let catalogueStatus = "loading";

    const text = (value) => String(value ?? "").trim();

    const number = (value) => {
        if (value === null || value === undefined || text(value) === "") {
            return null;
        }
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : null;
    };

>>>>>>> origin/Phuc-Tan
    const price = (value) => {
        const amount = number(value);
        return amount === null ? "Liên hệ" : `${new Intl.NumberFormat("vi-VN").format(amount)} ₫`;
    };
<<<<<<< HEAD
    const range = (model) => model.rangeKm == null ? "Đang cập nhật" : `${new Intl.NumberFormat("vi-VN").format(model.rangeKm)} km`;
    const battery = (model) => model.batteryKwh == null ? "Đang cập nhật" : `${new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(model.batteryKwh)} kWh`;
    const seats = (model) => model.seats == null ? "Đang cập nhật" : `${model.seats} chỗ`;
    const lineName = (model) => text(model.carLine || model.modelName).replace(/\s+/g, " ");
    const safeOfficialUrl = (candidate) => {
        try {
            const url = new URL(candidate);
            return url.protocol === "https:" && (url.hostname === "vinfastauto.com" || url.hostname.endsWith(".vinfastauto.com")) ? url.href : "";
=======

    const range = (model) =>
        model.rangeKm == null ? "Đang cập nhật" : `${new Intl.NumberFormat("vi-VN").format(model.rangeKm)} km`;

    const battery = (model) =>
        model.batteryKwh == null
            ? "Đang cập nhật"
            : `${new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(model.batteryKwh)} kWh`;

    const seats = (model) => (model.seats == null ? "Đang cập nhật" : `${model.seats} chỗ`);

    const lineName = (model) => text(model.carLine || model.modelName).replace(/\s+/g, " ");

    const safeOfficialUrl = (candidate) => {
        try {
            const url = new URL(candidate);
            return url.protocol === "https:" &&
                (url.hostname === "vinfastauto.com" || url.hostname.endsWith(".vinfastauto.com"))
                ? url.href
                : "";
>>>>>>> origin/Phuc-Tan
        } catch (_) {
            return "";
        }
    };
<<<<<<< HEAD
    const className = (model) => {
        const line = lineName(model).toUpperCase();
        const classes = { "VF 3": "Xe đô thị", "VF 5": "SUV đô thị", "VF 6": "SUV cỡ B", "VF 7": "SUV cỡ C", "VF 8": "SUV cỡ D", "VF 9": "SUV cỡ E" };
        return classes[line] || "Xe điện VinFast";
    };
    const make = (tag, classValue, value) => {
        const element = document.createElement(tag);
        if (classValue) element.className = classValue;
        if (value !== undefined) element.textContent = value;
=======

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
>>>>>>> origin/Phuc-Tan
        return element;
    };

    function populateFilters() {
        const lines = [...new Set(models.map(lineName).filter(Boolean))];
        lines.forEach((line) => modelFilter.add(new Option(line, line)));
<<<<<<< HEAD
        const colors = [...new Set(models.flatMap((model) => Array.isArray(model.colors) ? model.colors : []).map(text).filter(Boolean))].sort((a, b) => a.localeCompare(b, "vi"));
=======
        const colors = [
            ...new Set(
                models
                    .flatMap((model) => (Array.isArray(model.colors) ? model.colors : []))
                    .map(text)
                    .filter(Boolean)
            )
        ].sort((a, b) => a.localeCompare(b, "vi"));
>>>>>>> origin/Phuc-Tan
        colors.forEach((color) => colorFilter.add(new Option(color, color)));
    }

    function matches(model) {
        const searchText = text(search.value).toLocaleLowerCase("vi");
<<<<<<< HEAD
        const fields = [model.modelName, model.carLine, model.specificationVersion, model.dimensions, ...(Array.isArray(model.colors) ? model.colors : [])].map((value) => text(value).toLocaleLowerCase("vi"));
        const amount = number(model.priceVnd);
        const maxPriceMillions = number(priceFilter.value);
        const minimumRange = number(rangeFilter.value);
        return (!searchText || fields.some((value) => value.includes(searchText)))
            && (!modelFilter.value || lineName(model) === modelFilter.value)
            && (maxPriceMillions === null || amount === null || amount <= maxPriceMillions * 1_000_000)
            && (minimumRange === null || number(model.rangeKm) === null || Number(model.rangeKm) >= minimumRange)
            && (!colorFilter.value || (Array.isArray(model.colors) && model.colors.includes(colorFilter.value)));
=======
        const fields = [
            model.modelName,
            model.carLine,
            model.specificationVersion,
            model.dimensions,
            ...(Array.isArray(model.colors) ? model.colors : [])
        ].map((value) => text(value).toLocaleLowerCase("vi"));
        const amount = number(model.priceVnd);
        const maxPriceMillions = number(priceFilter.value);
        const minimumRange = number(rangeFilter.value);
        return (
            (!searchText || fields.some((value) => value.includes(searchText))) &&
            (!modelFilter.value || lineName(model) === modelFilter.value) &&
            (maxPriceMillions === null || amount === null || amount <= maxPriceMillions * 1_000_000) &&
            (minimumRange === null || number(model.rangeKm) === null || Number(model.rangeKm) >= minimumRange) &&
            (!colorFilter.value || (Array.isArray(model.colors) && model.colors.includes(colorFilter.value)))
        );
>>>>>>> origin/Phuc-Tan
    }

    function sorted(items) {
        const result = [...items];
<<<<<<< HEAD
        if (sortFilter.value === "price-asc") result.sort((a, b) => (number(a.priceVnd) ?? Infinity) - (number(b.priceVnd) ?? Infinity));
        if (sortFilter.value === "price-desc") result.sort((a, b) => (number(b.priceVnd) ?? -Infinity) - (number(a.priceVnd) ?? -Infinity));
        if (sortFilter.value === "range-desc") result.sort((a, b) => (number(b.rangeKm) ?? -Infinity) - (number(a.rangeKm) ?? -Infinity));
=======
        if (sortFilter.value === "price-asc") {
            result.sort((a, b) => (number(a.priceVnd) ?? Infinity) - (number(b.priceVnd) ?? Infinity));
        }
        if (sortFilter.value === "price-desc") {
            result.sort((a, b) => (number(b.priceVnd) ?? -Infinity) - (number(a.priceVnd) ?? -Infinity));
        }
        if (sortFilter.value === "range-desc") {
            result.sort((a, b) => (number(b.rangeKm) ?? -Infinity) - (number(a.rangeKm) ?? -Infinity));
        }
>>>>>>> origin/Phuc-Tan
        return result;
    }

    function card(model, index) {
        const id = `${text(model.modelName)}-${text(model.specificationVersion)}`;
        const selected = selections.has(id);
        const article = make("article", "vehicle-card");
        const photo = make("div", "vehicle-photo");
        const badge = make("span", "model-badge", className(model));
        const imageUrl = safeOfficialUrl(model.imageUrl);
        if (imageUrl) {
            const image = make("img");
            image.src = imageUrl;
            image.alt = `${text(model.modelName)} – xe điện VinFast`;
            image.loading = index < 3 ? "eager" : "lazy";
            image.decoding = "async";
            image.referrerPolicy = "no-referrer";
            image.addEventListener("error", () => image.remove(), { once: true });
            photo.append(image);
        }
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
<<<<<<< HEAD
        [["Quãng đường", range(model)], ["Pin", battery(model)], ["Chỗ ngồi", seats(model)]].forEach(([label, value]) => {
=======
        [
            ["Quãng đường", range(model)],
            ["Pin", battery(model)],
            ["Chỗ ngồi", seats(model)]
        ].forEach(([label, value]) => {
>>>>>>> origin/Phuc-Tan
            const item = make("div", "spec-item");
            item.append(make("span", "", label), make("strong", "", value));
            specs.append(item);
        });
        const priceLabel = make("span", "vehicle-price-label", "Giá tham khảo · đã gồm pin");
        const priceValue = make("strong", "vehicle-price", price(model.priceVnd));
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
<<<<<<< HEAD
        const compareButton = make("button", `quick-compare${selected ? " selected" : ""}`, selected ? "Đã chọn ✓" : "+ So sánh");
=======
        const compareButton = make(
            "button",
            `quick-compare${selected ? " selected" : ""}`,
            selected ? "Đã chọn ✓" : "+ So sánh"
        );
>>>>>>> origin/Phuc-Tan
        compareButton.type = "button";
        compareButton.setAttribute("aria-pressed", String(selected));
        compareButton.addEventListener("click", () => toggleCompare(id, model));
        footer.append(compareButton);
        body.append(title, version, specs, priceLabel, priceValue, footer);
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
<<<<<<< HEAD
                if (key === "model") modelFilter.value = "";
                if (key === "price") priceFilter.value = "";
                if (key === "range") rangeFilter.value = "";
                if (key === "color") colorFilter.value = "";
                if (key === "search") search.value = "";
=======
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
>>>>>>> origin/Phuc-Tan
                render();
            });
            chip.append(remove);
            activeFilters.append(chip);
        });
    }

    function render() {
<<<<<<< HEAD
=======
        if (catalogueStatus !== "ready") {
            return;
        }
>>>>>>> origin/Phuc-Tan
        const visible = sorted(models.filter(matches));
        grid.replaceChildren(...visible.map(card));
        grid.setAttribute("aria-busy", "false");
        emptyState.hidden = visible.length !== 0;
        grid.hidden = visible.length === 0;
        summary.textContent = `Có ${visible.length} phiên bản xe trong danh mục${visible.length !== models.length ? ` · tổng ${models.length} phiên bản` : ""}.`;
        filterChips();
    }

    function syncCompare() {
        document.getElementById("compare-count").textContent = String(selections.size);
        compareDock.hidden = selections.size === 0;
    }

    function toggleCompare(id, model) {
<<<<<<< HEAD
        if (selections.has(id)) selections.delete(id);
        else if (selections.size < 3) selections.add(id);
        else {
=======
        if (selections.has(id)) {
            selections.delete(id);
        } else if (selections.size < 3) {
            selections.add(id);
        } else {
>>>>>>> origin/Phuc-Tan
            summary.textContent = "Bạn có thể chọn tối đa 3 phiên bản để so sánh.";
            return;
        }
        render();
        syncCompare();
    }

    function openComparison() {
<<<<<<< HEAD
        const selectedModels = [...selections].map((id) => models.find((model) => `${text(model.modelName)}-${text(model.specificationVersion)}` === id)).filter(Boolean);
=======
        const selectedModels = [...selections]
            .map((id) => models.find((model) => `${text(model.modelName)}-${text(model.specificationVersion)}` === id))
            .filter(Boolean);
>>>>>>> origin/Phuc-Tan
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
<<<<<<< HEAD
        render();
    }

    async function loadCatalogue() {
        try {
            const response = await fetch(catalogueUrl, { credentials: "same-origin", headers: { Accept: "application/json" } });
            if (!response.ok) throw new Error(`Catalogue request failed: ${response.status}`);
            catalogueMetadata = await response.json();
            models = Array.isArray(catalogueMetadata.models) ? catalogueMetadata.models.filter((model) => text(model.modelName)) : [];
            if (!models.length) throw new Error("No catalogue models are available");
            populateFilters();
            const banners = ["VF 3", "VF 7", "VF 8"];
            banners.forEach((name, index) => {
                const model = models.find((item) => lineName(item).toUpperCase() === name) || models.find((item) => text(item.modelName).toUpperCase().startsWith(name));
                const image = document.querySelector(`[data-banner="${index}"] img`);
                const officialImage = safeOfficialUrl(model?.imageUrl);
                if (image && officialImage) image.src = officialImage;
            });
            const disclaimer = text(catalogueMetadata.note);
            document.getElementById("catalog-disclaimer").textContent = disclaimer || "Giá và thông số hiển thị theo dữ liệu tham khảo của từng cấu hình; vui lòng đối chiếu với VinFast trước khi giao dịch.";
            render();
        } catch (error) {
=======
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
            const response = await fetch(catalogueUrl, {
                credentials: "same-origin",
                headers: { Accept: "application/json" }
            });
            if (!response.ok) {
                throw new Error(`Catalogue request failed: ${response.status}`);
            }
            catalogueMetadata = await response.json();
            models = Array.isArray(catalogueMetadata.models)
                ? catalogueMetadata.models.filter((model) => text(model.modelName))
                : [];
            if (!models.length) {
                throw new Error("No catalogue models are available");
            }
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
        } catch (error) {
            catalogueStatus = "failed";
>>>>>>> origin/Phuc-Tan
            grid.setAttribute("aria-busy", "false");
            grid.replaceChildren();
            grid.hidden = true;
            emptyState.hidden = false;
            emptyState.querySelector("h3").textContent = "Chưa thể tải danh mục xe";
            emptyState.querySelector("p").textContent = "Vui lòng thử tải lại trang hoặc liên hệ để được hỗ trợ.";
<<<<<<< HEAD
=======
            document.getElementById("empty-reset").textContent = "Thử tải lại";
>>>>>>> origin/Phuc-Tan
            summary.textContent = "Danh mục xe hiện chưa sẵn sàng.";
            console.error("Không thể tải danh mục xe VinFast.", error);
        }
    }

<<<<<<< HEAD
    [modelFilter, priceFilter, rangeFilter, colorFilter, sortFilter].forEach((control) => control.addEventListener("change", render));
    search.addEventListener("input", render);
    document.getElementById("search-form").addEventListener("submit", (event) => { event.preventDefault(); render(); });
    document.getElementById("reset-filters").addEventListener("click", resetFilters);
    document.getElementById("empty-reset").addEventListener("click", resetFilters);
    document.getElementById("compare-open").addEventListener("click", openComparison);
    document.getElementById("compare-clear").addEventListener("click", () => { selections.clear(); render(); syncCompare(); });
    document.getElementById("compare-close").addEventListener("click", () => compareDialog.close());
    compareDialog.addEventListener("click", (event) => { if (event.target === compareDialog) compareDialog.close(); });
=======
    [modelFilter, priceFilter, rangeFilter, colorFilter, sortFilter].forEach((control) =>
        control.addEventListener("change", render)
    );
    search.addEventListener("input", render);
    document.getElementById("search-form").addEventListener("submit", (event) => {
        event.preventDefault();
        render();
    });
    document.getElementById("reset-filters").addEventListener("click", resetFilters);
    document.getElementById("empty-reset").addEventListener("click", resetFilters);
    document.getElementById("compare-open").addEventListener("click", openComparison);
    document.getElementById("compare-clear").addEventListener("click", () => {
        selections.clear();
        render();
        syncCompare();
    });
    document.getElementById("compare-close").addEventListener("click", () => compareDialog.close());
    compareDialog.addEventListener("click", (event) => {
        if (event.target === compareDialog) {
            compareDialog.close();
        }
    });
>>>>>>> origin/Phuc-Tan
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
