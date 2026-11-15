"use strict";

(() => {
    if (!document.querySelector("[data-customer-ai]")) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const contextPath = $('meta[name="app-context-path"]')?.content || "";
    const appUrl = (path) => `${contextPath}/${String(path).replace(/^\/+/, "")}`;
    const messages = $("#ai-messages");
    const question = $("#ai-question");
    const questionForm = $("#ai-question-form");
    const needsForm = $("#ai-needs-form");
    const compareForm = $("#ai-compare-form");
    const reset = $("#ai-reset");
    const reconnect = $("#ai-reconnect");
    const status = $("#ai-status");
    const state = { ready: false, pending: false, csrf: "", models: [], history: [], selected: [], entry: null };
    const number = (value) => new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 2 }).format(value);
    const quantity = (value, unit = "") => value == null ? "Chưa cập nhật" : `${number(value)}${unit ? ` ${unit}` : ""}`;
    const money = (value) => value == null ? "Liên hệ showroom" : `${number(value)} ₫`;
    const intro = "Chào bạn! Bạn muốn tìm xe theo ngân sách, quãng đường hay số chỗ? Bạn cũng có thể hỏi thêm về một mẫu xe vừa được gợi ý.";

    function node(tag, className = "", text) {
        const element = document.createElement(tag);
        if (className) element.className = className;
        if (text != null) element.textContent = String(text);
        return element;
    }

    function button(label, action) {
        const element = node("button", "ai-text-button", label);
        element.type = "button";
        element.disabled = state.pending || !state.ready;
        element.addEventListener("click", action);
        return element;
    }

    function notify(text = "", error = false) {
        status.textContent = text;
        status.hidden = !text;
        status.className = `ai-status${error ? " ai-status-error" : ""}`;
    }

    async function api(path, body) {
        const headers = { Accept: "application/json" };
        if (body !== undefined) {
            headers["Content-Type"] = "application/json";
            headers["X-CSRF-Token"] = state.csrf;
        }
        let response;
        try {
            response = await fetch(appUrl(`api/${path}`), {
                method: body === undefined ? "GET" : "POST",
                credentials: "same-origin",
                headers,
                ...(body === undefined ? {} : { body: JSON.stringify(body) })
            });
        } catch (_) {
            throw new Error("Chưa thể kết nối showroom. Kiểm tra mạng rồi thử lại.");
        }
        let data;
        try {
            data = await response.json();
        } catch (_) {
            throw new Error("Trợ lý chưa nhận được phản hồi hợp lệ. Vui lòng thử lại.");
        }
        if (!response.ok) {
            const error = new Error(data.error || `Yêu cầu chưa thành công (${response.status}).`);
            error.status = response.status;
            throw error;
        }
        return data;
    }

    function updateAccount(user) {
        const account = $("#ai-account-link");
        account.textContent = user ? (user.role === "CUSTOMER" ? "Tài khoản của tôi" : user.username) : "Đăng nhập";
        account.href = appUrl(user ? (user.role === "CUSTOMER" ? "customer/profile" : "index.jsp") : "login");
    }

    async function refreshSession() {
        const auth = await api("auth");
        if (!auth.csrfToken) throw new Error("Chưa tạo được phiên tư vấn. Vui lòng kết nối lại.");
        state.csrf = auth.csrfToken;
        updateAccount(auth.user);
    }

    function vehicleCard(vehicle, reasons = []) {
        const card = node("article", "ai-vehicle-card");
        card.append(node("span", "ai-eyebrow", vehicle.brandName || "XE ĐIỆN"), node("h3", "", vehicle.modelName));
        const prices = vehicle.maxPrice != null && Number(vehicle.maxPrice) !== Number(vehicle.minPrice)
            ? `${money(vehicle.minPrice)} – ${money(vehicle.maxPrice)}` : money(vehicle.minPrice);
        card.append(node("p", "ai-vehicle-price", prices));
        const spec = vehicle.specifications || {};
        card.append(node("p", "ai-vehicle-specs", `${quantity(spec.rangeKM, "km")} · ${quantity(spec.seatingCapacity, "chỗ")} · ${quantity(spec.batteryCapacity, "kWh")}`));
        if (reasons.length) {
            const list = node("ul", "ai-reasons");
            reasons.forEach((reason) => list.append(node("li", "", reason)));
            card.append(list);
        }
        card.append(button("Tìm hiểu mẫu xe này ↗", () => submit({ message: `Thông số và ưu điểm của ${vehicle.modelName}`, intent: "PRODUCT_QA", modelIds: [vehicle.modelId] })));
        return card;
    }

    function comparison(vehicles) {
        const wrap = node("div", "ai-comparison");
        wrap.tabIndex = 0;
        wrap.setAttribute("role", "region");
        wrap.setAttribute("aria-label", "Bảng so sánh thông số xe, có thể cuộn ngang");
        const table = node("table");
        table.append(node("caption", "ai-sr-only", "Thông số sản phẩm theo danh mục showroom"));
        const head = node("tr");
        head.append(node("th", "", "Thông tin"));
        vehicles.forEach((vehicle) => head.append(node("th", "", vehicle.modelName)));
        const thead = node("thead");
        thead.append(head);
        const tbody = node("tbody");
        const rows = [
            ["Giá từ", (vehicle) => money(vehicle.minPrice)],
            ["Tầm hoạt động", (vehicle) => quantity(vehicle.specifications?.rangeKM, "km")],
            ["Dung lượng pin", (vehicle) => quantity(vehicle.specifications?.batteryCapacity, "kWh")],
            ["Số chỗ", (vehicle) => quantity(vehicle.specifications?.seatingCapacity)],
            ["Công suất", (vehicle) => quantity(vehicle.specifications?.maxPower, "kW")],
            ["Kích thước (mm)", (vehicle) => vehicle.specifications?.dimensions || "Chưa cập nhật"],
            ["Phiên bản", (vehicle) => (vehicle.versions || []).join(", ") || "Chưa cập nhật"]
        ];
        rows.forEach(([label, read]) => {
            const row = node("tr");
            const heading = node("th", "", label);
            heading.setAttribute("scope", "row");
            row.append(heading);
            vehicles.forEach((vehicle) => row.append(node("td", "", read(vehicle))));
            tbody.append(row);
        });
        table.append(thead, tbody);
        wrap.append(table);
        return wrap;
    }

    function message(entry) {
        const item = node("div", `ai-message${entry.user ? " ai-user-message" : ""}${entry.error ? " ai-message-error" : ""}`);
        const avatar = node("span", "ai-message-avatar", entry.user ? "B" : "✧");
        avatar.setAttribute("aria-hidden", "true");
        const bubble = node("div", "ai-message-bubble");
        bubble.append(node("p", "ai-message-text", entry.text));
        if (entry.error && entry.request) bubble.append(button("Gửi lại câu hỏi", () => submit(entry.request, entry)));
        if (entry.error && entry.reset) bubble.append(button("Thử bắt đầu lại", resetConversation));
        const reply = entry.response;
        if (reply?.recommendations?.length) {
            const recommendations = node("div", "ai-recommendations");
            reply.recommendations.forEach((recommendation) => recommendations.append(vehicleCard(recommendation.vehicle, recommendation.reasons || [])));
            bubble.append(recommendations);
        }
        if (reply?.comparison?.length) bubble.append(comparison(reply.comparison));
        if (reply) bubble.append(node("small", "ai-response-note", "Thông tin từ danh mục showroom · Giá và thông số cần được xác nhận trước khi giao dịch."));
        item.append(avatar, bubble);
        return item;
    }

    function render() {
        messages.replaceChildren(message({ text: intro }), ...state.history.map(message));
        messages.setAttribute("aria-busy", String(state.pending));
        messages.scrollTop = messages.scrollHeight;
        $("#ai-send").disabled = state.pending || !state.ready;
        reset.disabled = state.pending || !state.ready;
        needsForm.querySelector('[type="submit"]').disabled = state.pending || !state.ready || !state.models.length;
        compareForm.querySelector('[type="submit"]').disabled = state.pending || !state.ready || state.selected.length < 2;
        $("#ai-selected-count").textContent = `${state.selected.length}/4 mẫu xe đã chọn`;
    }

    async function submit(request, retryEntry = null) {
        if (state.pending || !state.ready) return;
        request = { ...request, message: String(request.message || "").trim() };
        if (!request.message || request.message.length > 2000) {
            notify("Nhập câu hỏi từ 1 đến 2.000 ký tự.", true);
            return;
        }
        const reply = retryEntry || {};
        if (!retryEntry) state.history.push({ user: true, text: request.message }, reply);
        Object.assign(reply, { text: "Đang đối chiếu danh mục xe…", pending: true, error: false, response: null, request });
        const clearDraft = question.value.trim() === request.message;
        if (clearDraft) question.value = "";
        state.pending = true;
        notify();
        render();
        try {
            const data = await api("ai", request);
            Object.assign(reply, { text: data.answer, response: data, error: false });
            delete reply.request;
        } catch (error) {
            Object.assign(reply, { text: error.message, error: true });
            if (clearDraft && !question.value) question.value = request.message;
            if (error.status === 403 || error.status === 401) {
                try { await refreshSession(); } catch (_) { state.ready = false; reconnect.hidden = false; }
            }
        } finally {
            reply.pending = false;
            state.pending = false;
            render();
        }
    }

    async function resetConversation() {
        if (state.pending || !state.ready) return;
        state.pending = true;
        notify();
        render();
        try {
            await api("ai?action=reset", {});
            state.history = [];
            question.value = "";
            state.entry = null;
            state.selected = [];
            needsForm.reset();
            for (const input of $("#ai-model-choices").querySelectorAll("input")) input.checked = false;
            notify("Đã bắt đầu cuộc trò chuyện mới.");
        } catch (error) {
            state.history.push({ text: error.message, error: true, reset: true });
            if (error.status === 403 || error.status === 401) {
                try { await refreshSession(); } catch (_) { state.ready = false; reconnect.hidden = false; }
            }
        } finally {
            state.pending = false;
            render();
        }
    }

    function modelChoices() {
        const choices = $("#ai-model-choices");
        choices.replaceChildren();
        if (!state.models.length) choices.append(node("p", "", "Danh mục hiện chưa có mẫu xe để so sánh."));
        state.models.forEach((model) => {
            const label = node("label");
            const input = node("input");
            input.type = "checkbox";
            input.value = model.modelId;
            input.checked = state.selected.includes(model.modelId);
            input.addEventListener("change", () => {
                if (input.checked && !state.selected.includes(model.modelId) && state.selected.length >= 4) {
                    input.checked = false;
                    notify("Bạn có thể chọn tối đa 4 mẫu xe.", true);
                    return;
                }
                state.selected = input.checked ? [...new Set([...state.selected, model.modelId])] : state.selected.filter((id) => id !== model.modelId);
                notify();
                render();
            });
            label.append(input, node("span", "", model.modelName));
            choices.append(label);
        });
    }

    function readEntry() {
        const params = new URLSearchParams(window.location.search);
        const prompt = (params.get("prompt") || "").trim().slice(0, 2000);
        const ids = [...new Set((params.get("models") || "").split(",").map(Number))]
            .filter((id) => Number.isInteger(id) && state.models.some((model) => model.modelId === id)).slice(0, 4);
        const requestedIntent = params.get("intent");
        const intent = ["AUTO", "PRODUCT_QA", "RECOMMEND", "COMPARE"].includes(requestedIntent) ? requestedIntent : "AUTO";
        state.selected = ids;
        if (prompt) {
            question.value = prompt;
            state.entry = { message: prompt, intent, ...(ids.length ? { modelIds: ids } : {}) };
            notify("Câu hỏi đã được điền sẵn. Bấm Gửi để bắt đầu tư vấn.");
        }
    }

    async function initialize() {
        if (state.pending) return;
        state.ready = false;
        state.pending = true;
        reconnect.hidden = true;
        notify("Đang kết nối danh mục xe…");
        render();
        try {
            await refreshSession();
            const catalogue = await api("ai");
            state.models = Array.isArray(catalogue.models) ? catalogue.models : [];
            state.ready = true;
            $("#ai-catalogue-status").textContent = `${state.models.length} mẫu xe trong danh mục showroom`;
            notify(state.models.length ? "" : "Danh mục chưa có xe. Bạn có thể thử lại sau.");
            if (!state.history.length) readEntry();
            modelChoices();
        } catch (error) {
            notify(error.message, true);
            reconnect.hidden = false;
            $("#ai-catalogue-status").textContent = "Chưa thể kết nối danh mục";
        } finally {
            state.pending = false;
            render();
        }
    }

    questionForm.addEventListener("submit", (event) => {
        event.preventDefault();
        if (!questionForm.reportValidity()) return;
        const text = question.value.trim();
        const request = state.entry?.message === text ? state.entry : { message: text, intent: "AUTO" };
        void submit(request);
    });
    question.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
            event.preventDefault();
            questionForm.requestSubmit();
        }
    });
    document.querySelectorAll("[data-ai-prompt]").forEach((prompt) => prompt.addEventListener("click", () => {
        question.value = prompt.dataset.aiPrompt || "";
        question.focus();
    }));
    needsForm.addEventListener("submit", (event) => {
        event.preventDefault();
        if (!needsForm.reportValidity()) return;
        const read = (name) => $("[name=" + name + "]", needsForm).value;
        const budget = read("budget") === "" ? null : Number(read("budget")) * 1000000;
        const minRange = read("minRange") === "" ? null : Number(read("minRange"));
        const minSeats = read("minSeats") === "" ? null : Number(read("minSeats"));
        if ([budget, minRange, minSeats].every((value) => value == null)) {
            notify("Nhập ít nhất một nhu cầu để nhận gợi ý xe.", true);
            return;
        }
        const parts = [budget == null ? null : `ngân sách ${number(budget)} VND`, minRange == null ? null : `tầm hoạt động tối thiểu ${number(minRange)} km`, minSeats == null ? null : `ít nhất ${minSeats} chỗ`].filter(Boolean);
        void submit({ message: `Gợi ý xe với ${parts.join(", ")}.`, intent: "RECOMMEND", budget, minRange, minSeats });
    });
    compareForm.addEventListener("submit", (event) => {
        event.preventDefault();
        if (state.selected.length < 2 || state.selected.length > 4) {
            notify("Chọn từ 2 đến 4 mẫu xe để so sánh.", true);
            return;
        }
        const selected = state.models.filter((model) => state.selected.includes(model.modelId));
        void submit({ message: `So sánh ${selected.map((model) => model.modelName).join(" và ")}.`, intent: "COMPARE", modelIds: [...state.selected] });
    });
    reset.addEventListener("click", resetConversation);
    reconnect.addEventListener("click", initialize);
    void initialize();
})();
