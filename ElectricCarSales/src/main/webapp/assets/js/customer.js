"use strict";

(() => {
    const toast = document.querySelector("[data-toast]");
    const contextPath = document.body?.dataset?.contextPath
        || document.querySelector('meta[name="app-context-path"]')?.content || "";
    let toastTimer;

    function notify(message) {
        if (!toast) return;
        toast.textContent = message;
        toast.classList.add("visible");
        window.clearTimeout(toastTimer);
        toastTimer = window.setTimeout(() => toast.classList.remove("visible"), 3600);
    }

    const menuToggle = document.querySelector("[data-menu-toggle]");
    const sidebar = document.querySelector("#customer-sidebar");
    document.querySelectorAll("[data-avatar-image]").forEach((image) => {
        const setPlaceholder = () => {
            image.hidden = true;
            const placeholder = image.parentElement?.querySelector(".avatar-placeholder");
            if (placeholder) placeholder.hidden = false;
        };
        image.addEventListener("load", () => {
            image.hidden = false;
            const placeholder = image.parentElement?.querySelector(".avatar-placeholder");
            if (placeholder) placeholder.hidden = true;
        });
        image.addEventListener("error", setPlaceholder);
        if (image.complete) image.naturalWidth > 0 ? image.dispatchEvent(new Event("load")) : setPlaceholder();
    });
    menuToggle?.addEventListener("click", () => {
        const open = sidebar.classList.toggle("open");
        menuToggle.setAttribute("aria-expanded", String(open));
        menuToggle.setAttribute("aria-label", open ? "Đóng menu" : "Mở menu");
    });

    document.querySelector("[data-notification]")?.addEventListener("click", () => {
        notify("Trạng thái đơn hàng, thanh toán và hợp đồng được cập nhật trực tiếp từ hệ thống.");
    });

    document.querySelector("[data-profile-edit]")?.addEventListener("click", (event) => {
        event.preventDefault();
        const form = document.getElementById("profile-edit-form");
        if (form) {
            form.hidden = !form.hidden;
            if (!form.hidden) form.querySelector("input")?.focus();
        }
    });

    const avatarInput = document.querySelector('#profile-edit-form input[name="avatar"]');
    avatarInput?.addEventListener("change", () => {
        const file = avatarInput.files?.[0];
        if (!file || !file.type.startsWith("image/")) return;
        const previewUrl = URL.createObjectURL(file);
        document.querySelectorAll("[data-avatar-image]").forEach((image) => {
            image.src = previewUrl;
            image.hidden = false;
            const placeholder = image.parentElement?.querySelector(".avatar-placeholder");
            if (placeholder) placeholder.hidden = true;
        });
        setTimeout(() => URL.revokeObjectURL(previewUrl), 60000);
    });

    document.querySelectorAll(".filter-tab").forEach((tab) => {
        tab.addEventListener("click", () => {
            document.querySelectorAll(".filter-tab").forEach((item) => {
                item.classList.toggle("active", item === tab);
                item.setAttribute("aria-selected", String(item === tab));
            });
            const filter = tab.dataset.paymentFilter || "ALL";
            document.querySelectorAll("[data-payment-row-status]").forEach((row) => {
                const st = row.dataset.paymentRowStatus || "";
                row.style.display = (filter === "ALL" || st === filter) ? "" : "none";
            });
        });
    });

    // Connect AI Assistant form in customer/ai-assistant.jsp to real /api/ai endpoint
    const aiForm = document.querySelector("[data-ai-form]");
    const aiMessages = document.querySelector("[data-ai-messages]");
    const aiQuestion = aiForm?.querySelector("textarea");
    let csrfToken = null;

    async function ensureCsrf() {
        if (csrfToken) return csrfToken;
        const res = await fetch(`${contextPath}/api/auth`, { credentials: "same-origin" });
        if (res.ok) {
            const data = await res.json();
            csrfToken = data.csrfToken;
        }
        return csrfToken;
    }

    function appendAiBubble(role, text) {
        if (!aiMessages) return null;
        const wrap = document.createElement("div");
        wrap.className = `ai-message ${role === "user" ? "user-message" : "assistant-message"}`;
        const avatar = document.createElement("span");
        avatar.className = "message-avatar";
        if (role === "user") {
            const placeholder = document.createElementNS("http://www.w3.org/2000/svg", "svg");
            placeholder.setAttribute("viewBox", "0 0 24 24");
            placeholder.setAttribute("fill", "none");
            placeholder.setAttribute("aria-hidden", "true");
            placeholder.innerHTML = '<circle cx="12" cy="8" r="4" stroke="currentColor" stroke-width="1.8"/><path d="M4.5 21c.7-4.1 3.3-6.2 7.5-6.2s6.8 2.1 7.5 6.2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>';
            placeholder.classList.add("avatar-placeholder");
            const photo = document.createElement("img");
            photo.src = `${contextPath}/customer/avatar`;
            photo.alt = "";
            photo.hidden = true;
            photo.addEventListener("load", () => { photo.hidden = false; placeholder.hidden = true; }, { once: true });
            photo.addEventListener("error", () => photo.remove(), { once: true });
            avatar.append(placeholder, photo);
        } else {
            avatar.textContent = "✳";
        }
        const body = document.createElement("div");
        const p = document.createElement("p");
        p.style.whiteSpace = "pre-line";
        p.textContent = text;
        const small = document.createElement("small");
        small.textContent = role === "user" ? "Bạn" : "Trợ lý VinFast · Dữ liệu danh mục";
        body.append(p, small);
        wrap.append(avatar, body);
        aiMessages.appendChild(wrap);
        aiMessages.scrollTop = aiMessages.scrollHeight;
        return p;
    }

    aiForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!aiForm.reportValidity()) return;
        const q = aiQuestion.value.trim();
        if (!q) return;
        aiQuestion.value = "";
        appendAiBubble("user", q);
        const pendingP = appendAiBubble("assistant", "Đang tra cứu thông số xe trong cơ sở dữ liệu...");
        try {
            const token = await ensureCsrf();
            const res = await fetch(`${contextPath}/api/ai`, {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json",
                    ...(token ? { "X-CSRF-Token": token } : {})
                },
                body: JSON.stringify({ message: q, intent: "AUTO" })
            });
            const data = await res.json();
            if (!res.ok) {
                if (pendingP) pendingP.textContent = data.error || "Không thể xử lý câu hỏi.";
            } else {
                if (pendingP) pendingP.textContent = data.answer || "Đã hoàn tất đối chiếu.";
            }
        } catch (err) {
            if (pendingP) pendingP.textContent = "Lỗi kết nối máy chủ khi truy vấn AI.";
        }
    });

    document.querySelectorAll("[data-ai-prompt]").forEach((prompt) => {
        prompt.addEventListener("click", () => {
            if (aiQuestion) {
                aiQuestion.value = prompt.dataset.aiPrompt || "";
                aiQuestion.focus();
            }
        });
    });

    // Consultation & Pricing Popup (with Full-Screen Toggle)
    const popup = document.getElementById("ecs-consult-popup");
    const fullscreenBtn = document.getElementById("ecs-consult-fullscreen");
    const closeBtn = document.getElementById("ecs-consult-close");
    const calcModel = document.getElementById("ecs-calc-model");
    const calcMode = document.getElementById("ecs-calc-mode");
    const calcPlan = document.getElementById("ecs-calc-plan");
    const planWrap = document.getElementById("ecs-plan-wrap");
    const installmentOptions = document.getElementById("ecs-installment-options");
    const calcTerm = document.getElementById("ecs-calc-term");
    const calcPrice = document.getElementById("ecs-calc-price");
    const calcDownRate = document.getElementById("ecs-calc-down-rate");
    const calcSummary = document.getElementById("ecs-calc-summary");
    const popupForm = document.getElementById("ecs-popup-consult-form");
    const historyList = document.getElementById("ecs-consult-history-list");
    let consultationRefreshTimer = null;
    const escapeHtml = (value) => String(value ?? "")
        .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
    const financeTerms = {
        "Shinhan Finance": [{ months: 12, rate: 7.3 }, { months: 24, rate: 8.2 }, { months: 36, rate: 9.1 }, { months: 48, rate: 10.4 }, { months: 60, rate: 11.8 }],
        "Techcombank": [{ months: 24, rate: 5.0 }, { months: 36, rate: 5.7 }, { months: 48, rate: 6.3 }, { months: 60, rate: 7.0 }],
        "MB Bank": [{ months: 12, rate: 5.0 }, { months: 24, rate: 5.8 }, { months: 36, rate: 6.5 }, { months: 48, rate: 7.4 }, { months: 60, rate: 8.5 }],
        "VPBank": [{ months: 36, rate: 6.0 }, { months: 48, rate: 7.8 }, { months: 60, rate: 9.5 }]
    };
    const formatVnd = (amount) => new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0 }).format(Math.round(amount)) + " ₫";
    let popupModelsPromise = null;

    function syncConsultationPrice() {
        if (!calcModel || !calcPrice) return;
        const option = calcModel.selectedOptions[0];
        const price = Number(option?.dataset?.price || 0);
        calcPrice.value = price > 0 ? formatVnd(price) : "";
    }

    async function loadConsultationModels() {
        if (!calcModel) return;
        if (!popupModelsPromise) {
            popupModelsPromise = (async () => {
                const response = await fetch(`${contextPath}/api/ai`, {
                    credentials: "same-origin",
                    headers: { "Accept": "application/json" }
                });
                const data = await readJsonResponse(response);
                const models = Array.isArray(data.models) ? data.models.filter((model) => model.modelId && model.modelName) : [];
                if (!models.length) throw new Error("Danh mục chưa có mẫu xe để tư vấn.");
                calcModel.innerHTML = models.map((model) => {
                    const price = Number(model.inventoryMinPrice || 0);
                    const stock = Number(model.availableCars || 0);
                    const label = `${model.modelName} · ${stock} xe sẵn có`;
                    return `<option value="${escapeHtml(model.modelId)}" data-model-id="${escapeHtml(model.modelId)}" data-name="${escapeHtml(model.modelName)}" data-price="${price}" data-available="${stock}">${escapeHtml(label)}</option>`;
                }).join("");
                const preferred = models.find((model) => /VF\s*8/i.test(model.modelName)) || models[0];
                calcModel.value = String(preferred.modelId);
                syncConsultationPrice();
                updatePopupCalculation();
                return models;
            })().catch((error) => {
                popupModelsPromise = null;
                calcModel.innerHTML = `<option value="">Không tải được danh mục xe</option>`;
                if (calcSummary) calcSummary.textContent = error.message || "Không tải được danh mục xe và giá tồn kho.";
                throw error;
            });
        }
        return popupModelsPromise;
    }

    function updateFinanceTerms() {
        if (!calcPlan || !calcTerm) return;
        const options = financeTerms[calcPlan.value] || [];
        const previous = Number(calcTerm.value);
        calcTerm.innerHTML = options.map((plan) => `<option value="${plan.months}" data-rate="${plan.rate}">${plan.months} tháng · ${plan.rate.toLocaleString("vi-VN")}%/năm</option>`).join("");
        if (options.some((plan) => plan.months === previous)) calcTerm.value = String(previous);
    }

    async function readJsonResponse(response) {
        const contentType = response.headers.get("content-type") || "";
        const body = await response.text();
        if (!contentType.toLowerCase().includes("application/json")) {
            const redirectedToLogin = response.redirected && /\/login(?:[?#]|$)/i.test(response.url);
            if (response.status === 401 || redirectedToLogin) {
                throw new Error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            }
            throw new Error(body && !/^\s*</.test(body) ? body.slice(0, 240) : `Máy chủ không trả JSON (HTTP ${response.status}).`);
        }
        let data;
        try { data = body ? JSON.parse(body) : {}; }
        catch (_) { throw new Error("Máy chủ trả dữ liệu JSON không hợp lệ."); }
        if (!response.ok) throw new Error(data.error || data.message || `Lỗi máy chủ (${response.status}).`);
        return data;
    }

    function updatePopupCalculation() {
        if (!calcModel || !calcSummary) return;
        const mode = calcMode?.value || "BANK_TRANSFER";
        const installment = mode === "INSTALLMENT";
        if (planWrap) planWrap.hidden = !installment;
        if (installmentOptions) installmentOptions.hidden = !installment;
        if (mode === "BANK_TRANSFER") {
            calcSummary.innerHTML = `
                <div><strong>${escapeHtml(calcModel.selectedOptions[0]?.textContent || "Mẫu xe")}</strong></div>
                <div>Trả thẳng: đặt cọc 30%; thanh toán trực tiếp 70% còn lại sau khi ký hợp đồng.</div>
            `;
        } else {
            const opt = calcPlan.selectedOptions[0];
            const bank = opt?.dataset?.bank || "Ngân hàng đối tác";
            const selectedTerm = calcTerm?.selectedOptions[0];
            const months = Number(selectedTerm?.value || 0);
            const annualRate = Number(selectedTerm?.dataset?.rate || 0);
            const price = Number(calcModel.selectedOptions[0]?.dataset?.price || 0);
            const stock = Number(calcModel.selectedOptions[0]?.dataset?.available || 0);
            const requestedDownRate = Number(calcDownRate?.value || 30);
            const downRate = Math.min(100, Math.max(30, requestedDownRate)) / 100;
            if (calcDownRate && requestedDownRate < 30) calcDownRate.value = "30";
            const lines = price > 0 && months > 0 ? (() => {
                const down = price * downRate;
                const principal = price - down;
                const interest = principal * (annualRate / 100) * (months / 12);
                const total = principal + interest;
                return `<div style="margin-top:6px;">Trả trước (${Math.round(downRate * 100)}%): <strong>${formatVnd(down)}</strong></div>
                    <div>Khoản vay dự kiến: <strong>${formatVnd(principal)}</strong></div>
                    <div>Lãi phẳng toàn kỳ (${months} tháng, ${annualRate}%/năm): <strong>${formatVnd(interest)}</strong></div>
                    <div>Tổng trả cho đối tác (gốc + lãi): <strong>${formatVnd(total)}</strong></div>
                    <div>Mỗi tháng dự kiến: <strong>${formatVnd(total / months)}</strong> · phần lãi ${formatVnd(interest / months)}</div>`;
            })() : `<div style="margin-top:6px;">${stock > 0 ? "Xe này chưa có giá tồn kho hợp lệ để tính." : "Mẫu xe hiện không có xe AVAILABLE chưa gắn đơn trong kho, nên chưa thể tính tiền tự động."}</div>`;
            calcSummary.innerHTML = `
                <div><strong>${escapeHtml(calcModel.selectedOptions[0]?.textContent || "Mẫu xe")}</strong></div>
                <div>Phương án tham khảo: ${escapeHtml(bank)} · ${months} tháng · ${annualRate}%/năm.</div>
                <div>Tồn kho khả dụng: ${stock} xe · Giá lấy tự động từ Cars.Price.</div>
                ${lines}
                <div style="font-size:11px;color:#475569;margin-top:6px;">Ước tính lãi phẳng theo bảng tham khảo bạn cung cấp. Sales/đối tác cần xác nhận giá, lãi suất và khoản vay thực tế.</div>
            `;
        }
    }

    async function loadPopupConsultations() {
        if (!historyList) return;
        try {
            const res = await fetch(`${contextPath}/consultation?format=json`, {
                credentials: "same-origin",
                headers: { "Accept": "application/json" }
            });
            const data = await readJsonResponse(res);
            const items = data.consultations || [];
            if (!items.length) {
                historyList.innerHTML = `<div style="color:#64748b;">Bạn chưa có yêu cầu tư vấn nào. Hãy gửi câu hỏi ở trên để Sales Staff hỗ trợ!</div>`;
                return;
            }
            historyList.innerHTML = items.map(c => `
                <div class="ecs-consult-Popup-item">
                    <div style="display:flex;justify-content:space-between;font-weight:700;">
                        <span>#${escapeHtml(c.id)} · ${escapeHtml(c.modelName || "Tư vấn chung")}</span>
                        <span style="color:#1456db;">${escapeHtml(c.status)}</span>
                    </div>
                    <div style="white-space:pre-line;margin-top:4px;color:#1e293b;">${escapeHtml(c.content || "")}</div>
                    <div style="font-size:11px;color:#64748b;margin-top:4px;">NV phụ trách: ${escapeHtml(c.staffName || "Đang phân công")}</div>
                    ${["PENDING", "IN_PROGRESS"].includes(String(c.status || "").toUpperCase()) ? `
                        <form class="ecs-popup-reply-form" data-request-id="${escapeHtml(c.id)}" style="display:flex;gap:6px;margin-top:8px;">
                            <input name="message" maxlength="250" required aria-label="Phản hồi yêu cầu tư vấn #${escapeHtml(c.id)}" placeholder="Gửi phản hồi cho Sales Staff..." style="min-width:0;flex:1;padding:7px;border:1px solid #cbd5e1;border-radius:6px;">
                            <button type="submit" class="button button-secondary" style="padding:6px 10px;">Gửi</button>
                        </form>` : ""}
                </div>
            `).join("");
            historyList.querySelectorAll(".ecs-popup-reply-form").forEach((form) => {
                form.addEventListener("submit", async (event) => {
                    event.preventDefault();
                    const button = form.querySelector('button[type="submit"]');
                    if (!button || button.disabled) return;
                    button.disabled = true;
                    const oldText = button.textContent;
                    button.textContent = "Đang gửi...";
                    try {
                        const body = new URLSearchParams({
                            action: "reply",
                            format: "json",
                            requestId: form.dataset.requestId,
                            message: form.elements.message.value.trim()
                        });
                        const response = await fetch(`${contextPath}/consultation`, {
                            method: "POST", credentials: "same-origin",
                            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8", "Accept": "application/json" },
                            body: body.toString()
                        });
                        const result = await readJsonResponse(response);
                        if (!result.success) throw new Error(result.message || "Không gửi được phản hồi.");
                        notify(result.message || "Đã gửi phản hồi cho Sales Staff.");
                        await loadPopupConsultations();
                    } catch (error) {
                        notify(error.message || "Không gửi được phản hồi.");
                    } finally {
                        button.disabled = false;
                        button.textContent = oldText;
                    }
                });
            });
        } catch (error) {
            historyList.textContent = error.message || "Không tải được lịch sử tư vấn.";
        }
    }

    document.querySelectorAll("[data-open-consult-popup]").forEach((btn) => {
        btn.addEventListener("click", (event) => {
            if (btn.tagName === "A") event.preventDefault();
            if (!popup) return;
            const preferredModelId = btn.dataset.consultModelId || "";
            popup.hidden = false;
            loadConsultationModels().then(() => {
                if (preferredModelId && calcModel && [...calcModel.options].some((option) => option.value === preferredModelId)) {
                    calcModel.value = preferredModelId;
                    syncConsultationPrice();
                    updatePopupCalculation();
                }
            }).catch((error) => notify(error.message || "Không tải được danh mục tồn kho."));
            loadPopupConsultations();
            window.clearInterval(consultationRefreshTimer);
            consultationRefreshTimer = window.setInterval(() => {
                if (!popup.hidden) loadPopupConsultations();
            }, 10000);
        });
    });

    fullscreenBtn?.addEventListener("click", () => {
        if (!popup) return;
        const full = popup.classList.toggle("is-fullscreen");
        fullscreenBtn.textContent = full ? "❐ Thu nhỏ" : "⛶ Toàn màn hình";
    });

    closeBtn?.addEventListener("click", () => {
        if (!popup) return;
        popup.hidden = true;
        popup.classList.remove("is-fullscreen");
        window.clearInterval(consultationRefreshTimer);
    });

    calcPlan?.addEventListener("change", () => { updateFinanceTerms(); updatePopupCalculation(); });
    calcModel?.addEventListener("change", () => { syncConsultationPrice(); updatePopupCalculation(); });
    [calcMode, calcTerm].forEach((el) => el?.addEventListener("change", updatePopupCalculation));
    calcDownRate?.addEventListener("input", updatePopupCalculation);
    updateFinanceTerms();
    updatePopupCalculation();

    popupForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const modelName = calcModel?.selectedOptions[0]?.dataset?.name || "";
        const modelId = calcModel?.selectedOptions[0]?.dataset?.modelId || "";
        const mode = calcMode?.value || "BANK_TRANSFER";
        const bank = calcPlan?.selectedOptions[0]?.dataset?.bank || "";
        const term = calcTerm?.selectedOptions[0];
        const pref = mode === "INSTALLMENT"
            ? `Trả góp (${bank}, ${term?.value || ""} tháng, ${term?.dataset?.rate || ""}%/năm)`
            : "Trả thẳng";
        const msgInput = document.getElementById("ecs-consult-message");
        const message = msgInput?.value?.trim() || "";
        if (!message) return;
        if (!modelId) {
            notify("Vui lòng chờ danh mục xe từ kho tải xong rồi gửi yêu cầu.");
            return;
        }
        const submitButton = popupForm.querySelector('button[type="submit"]');
        if (!submitButton || submitButton.disabled) return;
        const oldButtonText = submitButton.textContent;
        submitButton.disabled = true;
        submitButton.textContent = "Đang gửi yêu cầu...";

        const body = new URLSearchParams();
        body.set("action", "create");
        body.set("format", "json");
        body.set("model", modelName);
        body.set("modelId", modelId);
        body.set("paymentPreference", pref);
        body.set("message", message);

        try {
            const res = await fetch(`${contextPath}/consultation`, {
                method: "POST",
                credentials: "same-origin",
                headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8", "Accept": "application/json" },
                body: body.toString()
            });
            const data = await readJsonResponse(res);
            if (!data.success) throw new Error(data.message || "Không gửi được yêu cầu tư vấn.");
            notify(data.message || "Đã gửi yêu cầu tư vấn.");
            if (data.success) {
                msgInput.value = "";
                await loadPopupConsultations();
            }
        } catch (error) {
            notify(error.message || "Không thể gửi yêu cầu tư vấn. Vui lòng thử lại.");
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = oldButtonText;
        }
    });
})();
