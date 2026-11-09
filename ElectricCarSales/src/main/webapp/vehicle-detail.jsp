<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#f3f4f6">
    <title><c:out value="${car.modelName}"/> | VinFast - ElectricCarSale</title>
    <style>
        :root { color-scheme: light; font-family: Arial, sans-serif; color: #263238; background: #f3f4f6; }
        * { box-sizing: border-box; }
        body { margin: 0; }
        .detail-header { padding: 16px max(20px, calc((100% - 1050px) / 2)); background: #fff; border-bottom: 1px solid #ddd; }
        .detail-header a, .back-link { color: #24568a; font-size: 14px; font-weight: 700; text-decoration: none; }
        main { width: min(1050px, calc(100% - 32px)); margin: 24px auto; }
        .detail-card { display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(280px, .9fr); gap: 24px; padding: 20px; border: 1px solid #ddd; background: #fff; }
        .gallery { min-height: 320px; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); align-content: start; gap: 10px; }
        .gallery img { width: 100%; height: clamp(280px, 34vw, 380px); object-fit: contain; border: 1px solid #eee; background: #f7f7f7; }
        .gallery img:first-child:nth-last-child(odd) { grid-column: 1 / -1; }
        .gallery-message { min-height: 320px; grid-column: 1 / -1; display: grid; place-items: center; margin: 0; color: #666; font-size: 14px; text-align: center; }
        .eyebrow { color: #555; font-size: 12px; font-weight: 700; }
        h1 { margin: 8px 0; font-size: 30px; }
        .description { margin: 0; color: #555; line-height: 1.6; white-space: pre-line; }
        .specs { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin: 18px 0; }
        .spec { padding: 10px; border: 1px solid #ddd; }
        .spec span { display: block; color: #666; font-size: 12px; }
        .spec strong { display: block; margin-top: 5px; font-size: 14px; }
        .spec-note { grid-column: 1 / -1; color: #555; font-size: 13px; line-height: 1.5; }
        .color-picker { margin: 18px 0; }
        .color-picker h2 { margin: 0 0 4px; font-size: 16px; }
        .selected-color { margin: 0 0 10px; color: #555; font-size: 14px; }
        .color-options { display: flex; flex-wrap: wrap; gap: 10px; }
        .color-option { width: 38px; height: 38px; display: grid; place-items: center; padding: 3px; border: 1px solid transparent; border-radius: 50%; background: #fff; cursor: pointer; }
        .color-option:disabled { cursor: wait; opacity: .65; }
        .color-option[aria-pressed="true"] { border-color: #24568a; box-shadow: 0 0 0 2px #fff, 0 0 0 3px #24568a; }
        .color-swatch { width: 28px; height: 28px; border: 1px solid rgba(0, 0, 0, .18); border-radius: 50%; }
        .color-status { min-height: 18px; margin: 5px 0 0; color: #a12626; font-size: 12px; }
        .specs-title { grid-column: 1 / -1; margin: 0 0 4px; font-size: 18px; }
        .actions { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin-top: 20px; }
        .contact-link { padding: 9px 13px; border: 1px solid #24568a; background: #24568a; color: #fff; font-size: 13px; text-decoration: none; }
        @media (max-width: 700px) { main { margin: 16px auto; } .detail-card { grid-template-columns: 1fr; gap: 18px; padding: 14px; } .gallery { min-height: 0; } .gallery img { height: clamp(230px, 68vw, 310px); } .gallery-message { min-height: 230px; } }
    </style>
</head>
<body>
<header class="detail-header">
    <a href="<c:url value='/index.jsp'/>">VinFast · ElectricCarSale</a>
</header>
<main>
    <article class="detail-card">
        <div class="gallery" id="vehicle-gallery"
             data-model-id="<c:out value='${car.modelID}'/>"
             data-model-name="<c:out value='${car.modelName}'/>"
             aria-label="Hình ảnh xe">
            <c:forEach var="image" items="${carImages}">
                <c:if test="${not empty image.imageURL}">
                    <img src="<c:out value='${image.imageURL}'/>" alt="<c:out value='${car.modelName}'/>" loading="lazy">
                </c:if>
            </c:forEach>
            <c:if test="${empty carImages}">
                <p class="gallery-message">Đang tải hình ảnh xe…</p>
            </c:if>
        </div>
        <div>
            <span class="eyebrow">THÔNG TIN MẪU XE</span>
            <h1><c:out value="${car.modelName}"/></h1>
            <p class="description"><c:out value="${car.description}" default="Thông tin mô tả đang được cập nhật."/></p>

            <section class="color-picker" id="color-picker" aria-labelledby="color-picker-title" hidden>
                <h2 id="color-picker-title">Màu ngoại thất</h2>
                <p class="selected-color" aria-live="polite">Màu đang chọn: <strong id="selected-color-name"></strong></p>
                <div class="color-options" id="color-options" aria-label="Chọn màu xe"></div>
                <p class="color-status" id="color-status" role="status" aria-live="polite"></p>
            </section>

            <section class="specs" id="detail-specs" aria-label="Thông số kỹ thuật">
                <h2 class="specs-title">Thông số kỹ thuật</h2>
                <p class="spec-note" id="specification-status" role="status" aria-live="polite">Đang tải thông số xe...</p>
            </section>
            <div class="actions">
                <a class="back-link" href="<c:url value='/index.jsp#models'/>">← Quay lại danh sách xe</a>
                <a class="contact-link" href="mailto:hotro@electriccarsale.vn?subject=Tu%20van%20xe">Liên hệ tư vấn</a>
            </div>
        </div>
    </article>
</main>
<script>
    (() => {
        const gallery = document.getElementById("vehicle-gallery");
        const modelName = gallery.dataset.modelName;
        const modelID = gallery.dataset.modelId;
        const colorPicker = document.getElementById("color-picker");
        const colorOptions = document.getElementById("color-options");
        const selectedColorName = document.getElementById("selected-color-name");
        const colorStatus = document.getElementById("color-status");
        const specifications = document.getElementById("detail-specs");
        let fallbackRequested = false;
        let cataloguePromise;
        let catalogueColorImages = {};
        let imageChangeSequence = 0;

        function showMessage(message) {
            gallery.replaceChildren();
            const notice = document.createElement("p");
            notice.className = "gallery-message";
            notice.textContent = message;
            gallery.append(notice);
        }

        function showFallbackImage(imageUrl, colorName) {
            const image = document.createElement("img");
            image.src = imageUrl;
            image.alt = colorName ? modelName + " - " + colorName : modelName;
            image.loading = "eager";
            image.onerror = () => showMessage("Chưa có hình ảnh cho mẫu xe này.");
            gallery.replaceChildren(image);
        }

        function normalize(value) {
            return String(value || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "")
                .toLocaleLowerCase("vi").replace(/đ/g, "d").replace(/[^a-z0-9]/g, "");
        }

        function renderSpecifications(model) {
            const formatNumber = (value, suffix) => value == null || value === ""
                ? "Đang cập nhật"
                : new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(value) + suffix;
            const valueOrPending = (value) => value == null || String(value).trim() === ""
                ? "Đang cập nhật"
                : String(value);
            const rows = [
                ["Phiên bản", valueOrPending(model.specificationVersion)],
                ["Giá tham khảo", model.priceVnd == null ? "Liên hệ" : formatNumber(model.priceVnd, " ₫")],
                ["Quãng đường công bố", formatNumber(model.rangeKm, " km")],
                ["Tiêu chuẩn quãng đường", valueOrPending(model.rangeStandard)],
                ["Dung lượng pin", formatNumber(model.batteryKwh, " kWh")],
                ["Công suất tối đa", formatNumber(model.maxPowerKw, " kW")],
                ["Số chỗ ngồi", formatNumber(model.seats, " chỗ")],
                ["Kích thước", valueOrPending(model.dimensions)],
                ["Mâm xe", valueOrPending(model.wheelType)],
                ["Số túi khí", Number(model.airbagCount) > 0 ? String(model.airbagCount) : "Đang cập nhật"],
                ["Tốc độ tối đa", formatNumber(model.maxSpeedKmh, " km/h")],
                ["Thời gian sạc nhanh", formatNumber(model.rapidChargeMinutes, " phút")]
            ];
            const fragment = document.createDocumentFragment();
            rows.forEach(([label, value]) => {
                const item = document.createElement("div");
                item.className = "spec";
                const name = document.createElement("span");
                name.textContent = label;
                const content = document.createElement("strong");
                content.textContent = value;
                item.append(name, content);
                fragment.append(item);
            });
            specifications.replaceChildren(fragment);
        }

        function showSpecificationError(message) {
            const notice = document.createElement("p");
            notice.className = "spec-note";
            notice.textContent = message;
            specifications.replaceChildren(notice);
        }

        function swatchBackground(colorName) {
            const color = normalize(colorName);
            if (color.includes("nocden")) return "linear-gradient(135deg, #f2c230 0 52%, #17191b 52% 100%)";
            if (color.includes("noctrang")) return "linear-gradient(135deg, #4b9bcc 0 52%, #f5f5f0 52% 100%)";
            if (color.includes("trang") || color.includes("infinityblanc")) return "#f5f5f0";
            if (color.includes("xam") || color.includes("zenithgrey")) return "#92989d";
            if (color.includes("den") || color.includes("jetblack")) return "#17191b";
            if (color.includes("do") || color.includes("solarruby") || color.includes("crimsonred")) return "#c82932";
            if (color.includes("vang") || color.includes("summeryellow")) return "#f2c230";
            if (color.includes("xanhduong") || color.includes("skyblue")) return "#4b9bcc";
            if (color.includes("xanhla")) return "#4b7b55";
            if (color.includes("hong") || color.includes("rosepink")) return "#e7a6b8";
            if (color.includes("xanhnhat") || color.includes("urbanmint")) return "#a7c7b5";
            if (color.includes("xanhdam") || color.includes("ivygreen")) return "#234c3e";
            if (color.includes("bac") || color.includes("desatsilver")) return "#bec3c8";
            return "#d7dce2";
        }

        async function setVehicleImage(imageUrl, colorName, sequence) {
            if (!imageUrl) throw new Error("Chưa có ảnh cho màu " + colorName + ".");
            const nextUrl = new URL(imageUrl);
            if (nextUrl.protocol !== "https:" ||
                !(nextUrl.hostname === "vinfastauto.com" || nextUrl.hostname.endsWith(".vinfastauto.com"))) {
                throw new Error("Catalogue color image URL is not an official VinFast URL");
            }

            const loadedImage = new Image();
            const imageLoaded = new Promise((resolve, reject) => {
                loadedImage.addEventListener("load", resolve, { once: true });
                loadedImage.addEventListener("error", () => {
                    reject(new Error("Image request failed for " + colorName));
                }, { once: true });
            });
            loadedImage.src = nextUrl.href;
            if (loadedImage.complete) {
                if (loadedImage.naturalWidth === 0) {
                    throw new Error("Image request failed for " + colorName);
                }
            } else {
                await imageLoaded;
            }
            if (sequence !== imageChangeSequence) return false;

            loadedImage.alt = colorName ? modelName + " - " + colorName : modelName;
            loadedImage.loading = "eager";
            gallery.replaceChildren(loadedImage);
            return true;
        }

        function renderColorOptions(colors) {
            if (!Array.isArray(colors) || colors.length === 0) return;
            colorOptions.replaceChildren();
            const buttons = colors.map((colorName) => {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "color-option";
                button.setAttribute("aria-label", colorName);
                button.setAttribute("aria-pressed", "false");
                button.title = colorName;

                const swatch = document.createElement("span");
                swatch.className = "color-swatch";
                swatch.style.background = swatchBackground(colorName);
                swatch.setAttribute("aria-hidden", "true");
                button.append(swatch);
                button.addEventListener("click", () => {
                    selectColor(colorName, button);
                });
                colorOptions.append(button);
                return button;
            });
            colorPicker.hidden = false;
            selectColor(colors[0], buttons[0]);
        }

        async function selectColor(colorName, button) {
            const sequence = ++imageChangeSequence;
            try {
                const colorImageUrl = catalogueColorImages?.[colorName];
                if (colorImageUrl) {
                    colorOptions.querySelectorAll(".color-option").forEach((option) => {
                        option.disabled = true;
                    });
                    colorStatus.textContent = "Đang tải ảnh màu " + colorName + "...";
                    const changed = await setVehicleImage(colorImageUrl, colorName, sequence);
                    if (!changed) return;
                }
                selectedColorName.textContent = colorName;
                colorStatus.textContent = "";
                colorOptions.querySelectorAll(".color-option").forEach((option) => {
                    option.setAttribute("aria-pressed", String(option === button));
                });
            } catch (error) {
                if (sequence === imageChangeSequence) {
                    colorStatus.textContent = "Không tải được ảnh màu " + colorName +
                        "; ảnh hiện tại vẫn được giữ nguyên.";
                    console.error("Không thể tải ảnh màu xe.", error);
                }
            } finally {
                if (sequence === imageChangeSequence) {
                    colorOptions.querySelectorAll(".color-option").forEach((option) => {
                        option.disabled = false;
                    });
                }
            }
        }

        async function loadCatalogue() {
            if (!cataloguePromise) {
                cataloguePromise = fetch("<c:url value='/api/catalogue'/>?modelId=" + encodeURIComponent(modelID), {
                    credentials: "same-origin",
                    cache: "no-cache",
                    headers: { Accept: "application/json" }
                }).then((response) => {
                    if (!response.ok) throw new Error("Vehicle catalog request failed: " + response.status);
                    return response.json();
                }).catch((error) => {
                    console.error("Không thể tải dữ liệu xe từ cơ sở dữ liệu.", error);
                    throw error;
                });
            }
            return cataloguePromise;
        }

        async function loadFallbackImage() {
            if (fallbackRequested) return;
            fallbackRequested = true;
            try {
                const catalogue = await loadCatalogue();
                const model = catalogue.models?.find((item) => Number(item.modelID) === Number(modelID));
                if (model) {
                    renderSpecifications(model);
                } else {
                    showSpecificationError("Chưa có thông số cho mẫu xe này trong danh mục.");
                }
                catalogueColorImages = model?.colorImages;
                if (model?.colors) renderColorOptions(model.colors);
                if (gallery.querySelector("img")) {
                    fallbackRequested = false;
                    return;
                }
                if (!model?.imageUrl) {
                    showMessage("Chưa có hình ảnh cho mẫu xe này.");
                    return;
                }
                const imageUrl = new URL(model.imageUrl);
                if (imageUrl.protocol !== "https:" ||
                    !(imageUrl.hostname === "vinfastauto.com" || imageUrl.hostname.endsWith(".vinfastauto.com"))) {
                    throw new Error("Catalogue image URL is not an official VinFast URL");
                }
                showFallbackImage(imageUrl.href);
            } catch (error) {
                console.error("Không thể tải dữ liệu dự phòng cho xe.", error);
                showSpecificationError("Không tải được thông số xe. Vui lòng thử tải lại trang.");
                showMessage("Chưa tải được hình ảnh xe. Vui lòng thử tải lại trang.");
            }
        }

        const existingImages = [...gallery.querySelectorAll("img")];
        existingImages.forEach((image) => image.addEventListener("error", () => {
                image.remove();
                if (gallery.querySelectorAll("img").length === 0) loadFallbackImage();
        }, { once: true }));
        loadFallbackImage();
    })();
</script>
</body>
</html>
