"use strict";

(() => {
    const contextPath = document.querySelector(".brand")?.getAttribute("href")?.replace(/\/index\.jsp$/, "") || "";
    const catalogueUrl = `${contextPath}/api/catalogue`;
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
    const compareNotice = document.getElementById("compare-notice");
    const selections = new Set();
    const compareSelectionStorageKey = "electriccarsale.compareSelections";
    const serverSearchApplied = grid.dataset.serverSearchApplied === "true";
    const serverSearchKeyword = grid.dataset.serverSearchKeyword;
    const serverSearchModelIds = new Set(
        grid.dataset.serverSearchModelIds.split(",").filter(Boolean)
    );
    let models = [];
    let catalogueMetadata = {};
    let compareNoticeTimer;

    try {
        const storedSelections = JSON.parse(sessionStorage.getItem(compareSelectionStorageKey) || "[]");
        if (Array.isArray(storedSelections)) {
            storedSelections.filter((id) => typeof id === "string").forEach((id) => selections.add(id));
        }
    } catch (error) {
        console.error("Không thể khôi phục các xe đã chọn để so sánh.", error);
    }

    const text = (value) => String(value ?? "").trim();
    const normalizeSearch = (value) => String(value ?? "").normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLocaleLowerCase("vi")
        .replace(/đ/g, "d")
        .replace(/[^a-z0-9]+/g, "");
    const colorCategories = [
        { value: "white", label: "Trắng", aliases: ["trang", "white", "infinity blanc"] },
        { value: "gray", label: "Xám", aliases: ["xam", "gray", "grey", "zenith grey"] },
        { value: "silver", label: "Bạc", aliases: ["bac", "silver", "desat silver"] },
        { value: "black", label: "Đen", aliases: ["den", "black", "jet black"] },
        { value: "red", label: "Đỏ", aliases: ["do", "red", "solar ruby", "crimson red"] },
        { value: "yellow", label: "Vàng", aliases: ["vang", "yellow", "summer yellow"] },
        { value: "blue", label: "Xanh dương", aliases: ["xanh duong", "blue", "sky blue"] },
        { value: "green", label: "Xanh lá", aliases: ["xanh la", "green", "urban mint", "ivy green"] },
        { value: "pink", label: "Hồng", aliases: ["hong", "pink", "rose pink"] }
    ];
    const normalizeColor = (value) => String(value ?? "").normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLocaleLowerCase("vi")
        .replace(/đ/g, "d")
        .replace(/[^a-z0-9]+/g, " ")
        .trim();
    const colorCategory = (colorName) => {
        const normalized = normalizeColor(String(colorName ?? "").split(/[+(]/, 1)[0]);
        return colorCategories.find((category) =>
            category.aliases.some((alias) => normalized.includes(alias))
        );
    };
    const number = (value) => {
        if (value == null || text(value) === "") return null;
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : null;
    };
    const price = (value) => {
        const amount = number(value);
        return amount === null ? "Liên hệ" : `${new Intl.NumberFormat("vi-VN").format(amount)} ₫`;
    };
    const range = (model) => model.rangeKm == null ? "Đang cập nhật" : `${new Intl.NumberFormat("vi-VN").format(model.rangeKm)} km`;
    const battery = (model) => model.batteryKwh == null ? "Đang cập nhật" : `${new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(model.batteryKwh)} kWh`;
    const seats = (model) => model.seats == null ? "Đang cập nhật" : `${model.seats} chỗ`;
    const lineName = (model) => text(model.carLine || model.modelName).replace(/\s+/g, " ");
    const safeOfficialUrl = (candidate) => {
        try {
            const url = new URL(candidate);
            return url.protocol === "https:" && (url.hostname === "vinfastauto.com" || url.hostname.endsWith(".vinfastauto.com")) ? url.href : "";
        } catch (_) {
            return "";
        }
    };
    const className = (model) => {
        const line = lineName(model).toUpperCase();
        const classes = { "VF 3": "Xe đô thị", "VF 5": "SUV đô thị", "VF 6": "SUV cỡ B", "VF 7": "SUV cỡ C", "VF 8": "SUV cỡ D", "VF 9": "SUV cỡ E" };
        return classes[line] || "Xe điện VinFast";
    };
    const make = (tag, classValue, value) => {
        const element = document.createElement(tag);
        if (classValue) element.className = classValue;
        if (value !== undefined) element.textContent = value;
        return element;
    };

    function populateFilters() {
        const lines = [...new Set(models.map(lineName).filter(Boolean))];
        lines.forEach((line) => modelFilter.add(new Option(line, line)));
        const availableColors = new Set(models.flatMap((model) =>
            (Array.isArray(model.colors) ? model.colors : [])
                .map(colorCategory)
                .filter(Boolean)
                .map((category) => category.value)
        ));
        colorCategories.filter((category) => availableColors.has(category.value))
            .forEach((category) => colorFilter.add(new Option(category.label, category.value)));
    }

    function matches(model) {
        if (serverSearchApplied && !serverSearchModelIds.has(String(model.modelID))) return false;
        const searchText = normalizeSearch(search.value);
        const fields = [model.modelName, model.carLine, model.specificationVersion, model.dimensions, ...(Array.isArray(model.colors) ? model.colors : [])].map(normalizeSearch);
        const shouldMatchSearchText = !serverSearchApplied ||
            searchText !== normalizeSearch(serverSearchKeyword);
        const amount = number(model.priceVnd);
        const maxPriceMillions = number(priceFilter.value);
        const minimumRange = number(rangeFilter.value);
        return (!searchText || !shouldMatchSearchText || fields.some((value) => value.includes(searchText)))
            && (!modelFilter.value || lineName(model) === modelFilter.value)
            && (maxPriceMillions === null || amount === null || amount <= maxPriceMillions * 1_000_000)
            && (minimumRange === null || number(model.rangeKm) === null || Number(model.rangeKm) >= minimumRange)
            && (!colorFilter.value || (Array.isArray(model.colors) && model.colors.some((color) =>
                colorCategory(color)?.value === colorFilter.value
            )));
    }

    function sorted(items) {
        const result = [...items];
        if (sortFilter.value === "price-asc") result.sort((a, b) => (number(a.priceVnd) ?? Infinity) - (number(b.priceVnd) ?? Infinity));
        if (sortFilter.value === "price-desc") result.sort((a, b) => (number(b.priceVnd) ?? -Infinity) - (number(a.priceVnd) ?? -Infinity));
        if (sortFilter.value === "range-desc") result.sort((a, b) => (number(b.rangeKm) ?? -Infinity) - (number(a.rangeKm) ?? -Infinity));
        return result;
    }

    function card(model, index) {
        const id = text(model.comparisonKey);
        const selected = selections.has(id);
        const article = make("article", `vehicle-card${selected ? " selected-for-compare" : ""}`);
        const photo = make("div", "vehicle-photo");
        const badge = make("span", "model-badge", className(model));
        const selectedColor = colorFilter.value && Array.isArray(model.colors)
            ? model.colors.find((color) => colorCategory(color)?.value === colorFilter.value)
            : "";
        const colorImageUrl = selectedColor ? model.colorImages?.[selectedColor] : "";
        const imageUrl = safeOfficialUrl(colorImageUrl) || safeOfficialUrl(model.imageUrl);
        if (imageUrl) {
            const image = make("img");
            image.src = imageUrl;
            image.alt = selectedColor
                ? `${text(model.modelName)} – ${selectedColor}`
                : `${text(model.modelName)} – xe điện VinFast`;
            image.loading = index < 3 ? "eager" : "lazy";
            image.decoding = "async";
            image.referrerPolicy = "no-referrer";
            image.addEventListener("error", () => image.remove(), { once: true });
            photo.append(image);
        }
        const detailUrl = `${contextPath}/detail?id=${encodeURIComponent(model.modelID)}`;
        const detailLink = make("a", "vehicle-detail-link", "Xem chi tiết ↗");
        detailLink.href = detailUrl;
        photo.append(badge, detailLink);

        let hasPromo = false;
        let discount = 0;
        let finalPrice = null;
        let promo = null;
        let promoTag = null;
        let promoCalc = null;
        if (Array.isArray(model.promotions) && model.promotions.length > 0) {
            promo = model.promotions[0];
            const basePrice = Number(model.priceVnd) || 0;
            if (basePrice > 0 && promo.discountValue) {
                if (promo.discountType === "PERCENTAGE") {
                    discount = Math.round((basePrice * Number(promo.discountValue)) / 100);
                } else {
                    discount = Math.min(basePrice, Number(promo.discountValue));
                }
                finalPrice = Math.max(0, basePrice - discount);
                hasPromo = discount > 0;
            }

            const promoBadgeText = promo.discountType === "PERCENTAGE"
                ? `-${promo.discountValue}% ƯU ĐÃI`
                : `-${price(discount || promo.discountValue)}`;
            const promoBadge = make("span", "promo-badge", promoBadgeText);
            photo.append(promoBadge);

            if (hasPromo) {
                promoCalc = make(
                    "div",
                    "vehicle-promo-calc",
                    promo.discountType === "PERCENTAGE"
                        ? `Giảm ${promo.discountValue}% (-${price(discount)}) trực tiếp`
                        : `Giảm trực tiếp -${price(discount)}`
                );
            }
            promoTag = make("div", "home-promo-tag", `🎁 ${promo.name}`);
        }

        const body = make("div", "vehicle-card-body");
        const title = make("h3", "vehicle-name", text(model.modelName));
        const version = make("p", "vehicle-version", text(model.specificationVersion) || "Phiên bản theo danh mục");
        const specs = make("div", "vehicle-specs");
        [["Quãng đường", range(model)], ["Pin", battery(model)], ["Chỗ ngồi", seats(model)]].forEach(([label, value]) => {
            const item = make("div", "spec-item");
            item.append(make("span", "", label), make("strong", "", value));
            specs.append(item);
        });
        const priceSection = make("div", "vehicle-price-wrap");
        if (hasPromo && finalPrice != null) {
            const priceLabel = make("span", "vehicle-price-label", "Giá ưu đãi đã trừ khuyến mãi:");
            const originalPrice = make("span", "vehicle-price-original", price(model.priceVnd));
            const priceValue = make("strong", "vehicle-price has-discount", price(finalPrice));
            const priceRow = make("div", "vehicle-price-row");
            priceRow.append(priceValue, originalPrice);
            priceSection.append(priceLabel, priceRow, promoCalc);
        } else {
            const priceLabel = make("span", "vehicle-price-label", "Giá tham khảo · đã gồm pin");
            const priceValue = make("strong", "vehicle-price", price(model.priceVnd));
            priceSection.append(priceLabel, priceValue);
        }
        const footer = make("div", "vehicle-card-footer");
        const sourceUrl = safeOfficialUrl(model.priceSourceUrl);
        if (sourceUrl) {
            const source = make("a", "vehicle-source", "Nguồn giá VinFast ↗");
            source.href = sourceUrl;
            source.target = "_blank";
            source.rel = "noopener noreferrer";
            footer.append(source);
        } else {
            footer.append(make("span", "vehicle-source", "Thông tin tham khảo"));
        }
        const compareButton = make("button", `quick-compare${selected ? " selected" : ""}`, selected ? "Đã chọn ✓" : "+ So sánh");
        compareButton.type = "button";
        compareButton.setAttribute("aria-pressed", String(selected));
        compareButton.addEventListener("click", () => toggleCompare(id, model));
        footer.append(compareButton);
        body.append(title, version, ...(promoTag ? [promoTag] : []), specs, priceSection, footer);
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
                if (key === "model") modelFilter.value = "";
                if (key === "price") priceFilter.value = "";
                if (key === "range") rangeFilter.value = "";
                if (key === "color") colorFilter.value = "";
                if (key === "search") search.value = "";
                render();
            });
            chip.append(remove);
            activeFilters.append(chip);
        });
    }

    function render() {
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

    function persistCompareSelections() {
        try {
            sessionStorage.setItem(compareSelectionStorageKey, JSON.stringify([...selections]));
        } catch (error) {
            console.error("Không thể lưu các xe đã chọn để so sánh.", error);
        }
    }

    function showCompareNotice() {
        compareNotice.textContent = "Bạn chỉ có thể chọn tối đa 3 xe để so sánh.";
        compareNotice.hidden = false;
        window.clearTimeout(compareNoticeTimer);
        compareNoticeTimer = window.setTimeout(() => {
            compareNotice.hidden = true;
        }, 2800);
    }

    function toggleCompare(id, model) {
        if (selections.has(id)) selections.delete(id);
        else if (selections.size < 3) selections.add(id);
        else {
            showCompareNotice();
            return;
        }
        persistCompareSelections();
        render();
        syncCompare();
    }

    function openComparison() {
        if (selections.size === 0) {
            showCompareNotice("Vui lòng chọn xe để so sánh.");
            return;
        }
        if (selections.size === 1) {
            showCompareNotice("Vui lòng chọn thêm ít nhất 1 xe nữa để so sánh (tối thiểu 2 xe).");
            return;
        }
        persistCompareSelections();
        const query = new URLSearchParams();
        selections.forEach((key) => query.append("key", key));
        window.location.href = `${contextPath}/comparison?${query.toString()}`;
    }

    function resetFilters() {
        if (serverSearchApplied) {
            window.location.href = `${contextPath}/index.jsp#models`;
            return;
        }
        search.value = "";
        modelFilter.value = "";
        priceFilter.value = "";
        rangeFilter.value = "";
        colorFilter.value = "";
        sortFilter.value = "recommended";
        render();
    }

    async function loadCatalogue() {
        try {
            const response = await fetch(catalogueUrl, {
                credentials: "same-origin",
                headers: { Accept: "application/json" }
            });
            if (!response.ok) throw new Error(`Catalogue request failed: ${response.status}`);
            catalogueMetadata = await response.json();
            models = Array.isArray(catalogueMetadata.models) ? catalogueMetadata.models.filter((model) => text(model.modelName)) : [];
            if (!models.length) throw new Error("No catalogue models are available");
            const availableIds = new Set(models.map((model) => text(model.comparisonKey)));
            for (const id of selections) {
                if (!availableIds.has(id)) selections.delete(id);
            }
            persistCompareSelections();
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
            syncCompare();
        } catch (error) {
            grid.setAttribute("aria-busy", "false");
            grid.replaceChildren();
            grid.hidden = true;
            emptyState.hidden = false;
            emptyState.querySelector("h3").textContent = "Chưa thể tải danh mục xe";
            emptyState.querySelector("p").textContent = "Vui lòng thử tải lại trang hoặc liên hệ để được hỗ trợ.";
            summary.textContent = "Danh mục xe hiện chưa sẵn sàng.";
            console.error("Không thể tải danh mục xe VinFast.", error);
        }
    }

    [modelFilter, priceFilter, rangeFilter, colorFilter, sortFilter].forEach((control) => control.addEventListener("change", render));
    search.addEventListener("input", render);
    document.getElementById("reset-filters").addEventListener("click", resetFilters);
    document.getElementById("empty-reset").addEventListener("click", resetFilters);
    document.getElementById("compare-open").addEventListener("click", openComparison);
    document.addEventListener("compare-limit-reached", showCompareNotice);
    document.getElementById("compare-clear").addEventListener("click", () => {
        selections.clear();
        persistCompareSelections();
        render();
        syncCompare();
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
