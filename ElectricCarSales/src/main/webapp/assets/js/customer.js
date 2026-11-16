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
        const pendingP = appendAiBubble("assistant", "Đang tra cứu thông tin sản phẩm...");
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
            let answered = false;
            if (res.ok) {
                const data = await res.json();
                if (data.answer && !data.refused) {
                    if (pendingP) {
                        pendingP.style.whiteSpace = "normal";
                        pendingP.innerHTML = await queryAIConsultation(q, "", data.answer);
                    }
                    answered = true;
                }
            }
            if (!answered) {
                const fallbackAnswer = await queryAIConsultation(q, "");
                if (pendingP) {
                    pendingP.style.whiteSpace = "normal";
                    pendingP.innerHTML = fallbackAnswer;
                }
            }
        } catch (err) {
            try {
                const fallbackAnswer = await queryAIConsultation(q, "");
                if (pendingP) {
                    pendingP.style.whiteSpace = "normal";
                    pendingP.innerHTML = fallbackAnswer;
                }
            } catch (_) {
                if (pendingP) pendingP.textContent = "Trợ lý AI đang bận. Vui lòng thử lại sau giây lát.";
            }
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
                let models = [];
                try {
                    const response = await fetch(`${contextPath}/api/ai`, {
                        credentials: "same-origin",
                        headers: { "Accept": "application/json" }
                    });
                    if (response.ok) {
                        const data = await readJsonResponse(response);
                        models = Array.isArray(data.models) ? data.models.filter((model) => model.modelId && model.modelName) : [];
                    }
                } catch (apiError) {
                    console.warn("Chưa thể tải tồn kho từ /api/ai; chuẩn bị đọc dữ liệu tham khảo.", apiError);
                }

                if (!models.length) {
                    try {
                        const fallbackResponse = await fetch(`${contextPath}/assets/vinfast-catalogue.json`, {
                            headers: { "Accept": "application/json" }
                        });
                        if (fallbackResponse.ok) {
                            const fallbackData = await fallbackResponse.json();
                            if (Array.isArray(fallbackData.models) && fallbackData.models.length > 0) {
                                models = fallbackData.models.map((m, index) => ({
                                    modelId: index + 1,
                                    modelName: m.modelName || m.carLine || `VF ${index + 3}`,
                                    inventoryMinPrice: m.priceVnd || 0,
                                    availableCars: 0
                                }));
                            }
                        }
                    } catch (fallbackError) {
                        console.warn("Không thể tải vinfast-catalogue.json", fallbackError);
                    }
                }

                if (!models.length) throw new Error("Danh mục chưa có mẫu xe để tư vấn.");
                calcModel.innerHTML = models.map((model) => {
                    const price = Number(model.inventoryMinPrice || 0);
                    const stock = Number(model.availableCars || 0);
                    const stockText = stock > 0 ? `${stock} xe sẵn có` : "Đặt trước";
                    const label = `${model.modelName} · ${stockText}`;
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
        const price = Number(calcModel.selectedOptions[0]?.dataset?.price || 0);
        const stock = Number(calcModel.selectedOptions[0]?.dataset?.available || 0);
        if (mode === "BANK_TRANSFER") {
            const deposit = price > 0 ? formatVnd(price * 0.3) : "ước tính 30%";
            const balance = price > 0 ? formatVnd(price * 0.7) : "ước tính 70%";
            calcSummary.innerHTML = `
                <div><strong>${escapeHtml(calcModel.selectedOptions[0]?.textContent || "Mẫu xe")}</strong></div>
                <div>Phương án trả thẳng: đặt cọc 30% (${deposit}); thanh toán 70% còn lại (${balance}) sau khi ký hợp đồng.</div>
                <div style="font-size:11px;color:#475569;margin-top:6px;">${stock > 0 ? `Tồn kho sẵn sàng: ${stock} xe.` : "Xe kho sẵn sàng hoặc liên hệ Sales để được giữ chỗ sớm."}</div>
            `;
        } else {
            const opt = calcPlan.selectedOptions[0];
            const bank = opt?.dataset?.bank || "Ngân hàng đối tác";
            const selectedTerm = calcTerm?.selectedOptions[0];
            const months = Number(selectedTerm?.value || 0);
            const annualRate = Number(selectedTerm?.dataset?.rate || 0);
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
            })() : `<div style="margin-top:6px;">Mẫu xe hiện chưa có giá tham khảo, vui lòng liên hệ Sales để được báo giá trực tiếp.</div>`;
            calcSummary.innerHTML = `
                <div><strong>${escapeHtml(calcModel.selectedOptions[0]?.textContent || "Mẫu xe")}</strong></div>
                <div>Phương án tham khảo: ${escapeHtml(bank)} · ${months} tháng · ${annualRate}%/năm.</div>
                <div>${stock > 0 ? `Tồn kho khả dụng: ${stock} xe · Giá lấy tự động từ Cars.Price.` : `Giá niêm yết tham khảo danh mục VinFast: ${formatVnd(price)}.`}</div>
                ${lines}
                <div style="font-size:11px;color:#475569;margin-top:6px;">Ước tính lãi phẳng theo bảng tham khảo bạn cung cấp. Sales/đối tác cần xác nhận giá, lãi suất và khoản vay thực tế.</div>
            `;
        }
    }

    let isCustomerSession = false;

    const modelProfiles = {
        "VF 3": {
            modelName: "VF 3",
            segment: "Xe điện đô thị cỡ nhỏ (Mini e-SUV)",
            price: "285.000.000 ₫ (gồm pin & VAT)",
            priceNum: 285000000,
            seats: "4 chỗ",
            dimensions: "3190 x 1679 x 1652 mm (Khoảng sáng gầm 191 mm)",
            range: "215 km (chuẩn NEDC)",
            battery: "18.64 kWh",
            power: "30 kW (khoảng 40 mã lực, dẫn động cầu sau RWD)",
            charge: "Sạc nhanh 10-70% trong 36 phút (sạc tại trạm công cộng)",
            airbags: "1 túi khí người lái",
            highlights: "Nhỏ gọn linh hoạt, dễ quay đầu trong ngõ hẹp đô thị. Chi phí sạc chỉ ~350 đ/km (khoảng 70.000 ₫ đi được hơn 200 km).",
            suitability: "Rất phù hợp cho cá nhân đi làm hằng ngày trong thành phố, sinh viên, người mới lái xe hoặc gia đình muốn mua thêm chiếc xe thứ 2 che nắng mưa."
        },
        "VF 5": {
            modelName: "VF 5 Plus",
            segment: "SUV đô thị hạng A (A-SUV)",
            price: "496.000.000 ₫ (gồm pin & VAT)",
            priceNum: 496000000,
            seats: "5 chỗ",
            dimensions: "3967 x 1723 x 1579 mm (Khoảng sáng gầm 182 mm)",
            range: "326 km (chuẩn NEDC)",
            battery: "37.23 kWh",
            power: "100 kW (134 mã lực, mô-men xoắn 135 Nm)",
            charge: "Sạc nhanh 10-70% trong 30-33 phút",
            airbags: "6 túi khí an toàn",
            highlights: "Khoang cabin 5 chỗ rộng rãi hàng đầu phân khúc A, trang bị 6 túi khí, giám sát hành trình, cảnh báo điểm mù, cảnh báo phương tiện cắt ngang khi lùi.",
            suitability: "Lựa chọn số 1 cho gia đình nhỏ (vợ chồng + 1-2 con), hoặc người muốn tối ưu chi phí sử dụng chạy dịch vụ, đi làm ngoại thành."
        },
        "VF 6": {
            modelName: "VF 6",
            segment: "SUV cỡ B thể thao (B-SUV)",
            price: "Từ 646.000.000 ₫ (Eco) đến 699.000.000 ₫ (Plus)",
            priceNum: 646000000,
            seats: "5 chỗ",
            dimensions: "4241 x 1834 x 1580 mm (Chiều dài cơ sở 2730 mm)",
            range: "460 - 485 km (chuẩn NEDC)",
            battery: "59.6 kWh",
            power: "130 - 150 kW (174 - 201 mã lực, mô-men xoắn 250 - 310 Nm)",
            charge: "Sạc siêu nhanh 10-70% trong 25 phút",
            airbags: "4 - 8 túi khí",
            highlights: "Thiết kế thể thao do Torino Design chắp bút. Màn hình 12.9 inch, hỗ trợ trợ lý ảo Vivi tiếng Việt, gói an toàn chủ động ADAS cấp độ 2 (hỗ trợ giữ làn, phanh tự động khẩn cấp).",
            suitability: "Cực kỳ lý tưởng cho người trẻ năng động, gia đình trẻ hiện đại thích công nghệ và thẩm mỹ thời thượng."
        },
        "VF 7": {
            modelName: "VF 7",
            segment: "SUV cỡ C thể thao & tương lai (C-SUV)",
            price: "Từ 740.000.000 ₫ (Eco) đến 830.000.000 ₫ (Plus FWD)",
            priceNum: 740000000,
            seats: "5 chỗ",
            dimensions: "4545 x 1890 x 1636 mm (Chiều dài cơ sở 2840 mm)",
            range: "440 - 500.5 km (chuẩn NEDC)",
            battery: "59.6 - 70.0 kWh",
            power: "130 - 260 kW (174 - 349 mã lực, bản Plus AWD tăng tốc 0-100 km/h chỉ 5.8s)",
            charge: "Sạc nhanh 10-70% trong 24 - 28 phút",
            airbags: "6 - 8 túi khí",
            highlights: "Thiết kế phi thuyền phong cách phi đối xứng hướng về người lái, trần kính toàn cảnh panorama, mâm xe lớn 19-20 inch, khả năng bứt tốc vượt trội các xe xăng cùng phân khúc C.",
            suitability: "Dành cho khách hàng yêu thích cảm giác lái thể thao, thiết kế cá tính ấn tượng và không gian gia đình rộng rãi."
        },
        "VF 8": {
            modelName: "VF 8",
            segment: "D-SUV điện toàn cầu cao cấp (D-SUV)",
            price: "Từ 1.079.000.000 ₫ (Eco) đến 1.259.000.000 ₫ (Plus)",
            priceNum: 1079000000,
            seats: "5 chỗ cỡ lớn",
            dimensions: "4750 x 1934 x 1667 mm (Chiều dài cơ sở 2950 mm)",
            range: "457 - 471 km (chuẩn WLTP)",
            battery: "87.7 kWh (Pin CATL chất lượng cao)",
            power: "260 - 300 kW (349 - 402 mã lực, hệ dẫn động 4 bánh toàn thời gian AWD)",
            charge: "Sạc siêu nhanh 10-70% trong 24 phút",
            airbags: "11 túi khí an toàn cao nhất",
            highlights: "Đạt chuẩn an toàn 5 sao NHTSA/Euro NCAP. Màn hình 15.6 inch, hiển thị HUD kính lái, ghế chỉnh điện bọc da cao cấp có sưởi và làm mát, tự lái bán tự động cấp độ 2+.",
            suitability: "Dành cho gia đình trung lưu, doanh nhân hoặc những ai thường xuyên đi công tác đường dài, cần độ an toàn và độ êm ái tối đa."
        },
        "VF 9": {
            modelName: "VF 9",
            segment: "E-SUV full-size hạng sang đầu bảng (E-SUV)",
            price: "Từ 1.566.000.000 ₫ (Eco) đến 1.743.000.000 ₫ (Plus)",
            priceNum: 1566000000,
            seats: "6 hoặc 7 chỗ (có tùy chọn ghế cơ trưởng VIP massage)",
            dimensions: "5118 x 2004 x 1696 mm (Chiều dài cơ sở 3150 mm)",
            range: "602 - 626 km (chuẩn WLTP)",
            battery: "123.0 kWh (Dung lượng pin lớn nhất)",
            power: "300 kW (402 mã lực, mô-men xoắn 620 Nm, dẫn động AWD)",
            charge: "Sạc nhanh công suất cao 10-70% trong 35 phút",
            airbags: "11 túi khí an toàn",
            highlights: "Đẳng cấp xe chủ tịch với hàng ghế thứ 2 cơ trưởng tách biệt (massage, sưởi, thông gió), cửa hít, trần kính toàn cảnh, điều hòa 3 vùng, giảm xóc bóng hơi êm ái.",
            suitability: "Lựa chọn duy nhất cho gia đình đông người (6-7 người) hoặc doanh nghiệp đưa đón đối tác VIP cần không gian rộng rãi và đẳng cấp đỉnh cao."
        }
    };

    function normalizeVn(str) {
        return String(str || "")
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .replace(/đ/g, "d").replace(/Đ/g, "D")
            .toLowerCase()
            .trim();
    }

    function updateConsultationModeUI(isCustomer) {
        const modeBadge = document.getElementById("ecs-consult-mode-badge");
        const cardTitle = document.getElementById("ecs-consult-card-title");
        const submitBtn = document.getElementById("ecs-consult-submit-btn");
        const consultMsg = document.getElementById("ecs-consult-message");
        const aiPrompts = document.getElementById("ecs-popup-ai-prompts");
        const aiStream = document.getElementById("ecs-ai-consult-result");
        const loginCta = document.getElementById("ecs-ai-consult-cta");
        const staffSection = document.getElementById("ecs-staff-consult-section");
        if (!submitBtn) return;
        if (isCustomer) {
            if (modeBadge) {
                modeBadge.textContent = "SALES STAFF";
                modeBadge.className = "ecs-mode-badge is-staff";
            }
            if (cardTitle) cardTitle.textContent = "2. Tư vấn & Đặt xe với Sales Staff";
            if (consultMsg) consultMsg.placeholder = "Nhập yêu cầu tư vấn màu xe, phiên bản hoặc nhờ Sales lập đơn...";
            if (aiPrompts) aiPrompts.hidden = true;
            if (aiStream) aiStream.hidden = true;
            if (loginCta) loginCta.hidden = true;
            if (staffSection) staffSection.hidden = false;
            submitBtn.innerHTML = "Gửi yêu cầu <span>↑</span>";
        } else {
            if (modeBadge) {
                modeBadge.textContent = "AI ASSISTANT";
                modeBadge.className = "ecs-mode-badge";
            }
            if (cardTitle) cardTitle.textContent = "Trợ lý Tư vấn VinFast";
            if (consultMsg) consultMsg.placeholder = "Hỏi về mẫu xe, thông số, tầm hoạt động hoặc so sánh xe…";
            if (aiPrompts) aiPrompts.hidden = false;
            if (aiStream) aiStream.hidden = false;
            if (loginCta) loginCta.hidden = false;
            if (staffSection) staffSection.hidden = true;
            submitBtn.innerHTML = "Gửi <span>↑</span>";
        }
    }

    function renderMiniModelCard(car) {
        return `
        <div class="ecs-catalog-card">
            <div class="ecs-cat-top">
                <div>
                    <div class="ecs-cat-name">${escapeHtml(car.modelName)}</div>
                    <div class="ecs-cat-seg">${escapeHtml(car.segment)}</div>
                </div>
                <div class="ecs-cat-price">${escapeHtml(car.price.split('(')[0].trim())}</div>
            </div>
            <div class="ecs-cat-pills">
                <span>🔋 ${escapeHtml(car.range)}</span>
                <span>💺 ${escapeHtml(car.seats)}</span>
                <span>⚡ ${escapeHtml(car.battery)}</span>
            </div>
            <button type="button" class="ecs-btn-select" data-pick-model="${escapeHtml(car.modelName)}">
                Chọn ${escapeHtml(car.modelName)} tính cọc →
            </button>
        </div>`;
    }

    function renderCatalogGrid(title) {
        const cardsHtml = Object.values(modelProfiles).map(renderMiniModelCard).join("");
        return `
        <div>
            <div class="ecs-catalog-intro">
                <strong>${escapeHtml(title || "Danh mục xe điện VinFast")}</strong>
            </div>
            <div class="ecs-catalog-grid">${cardsHtml}</div>
        </div>`;
    }

    function renderSingleCarHero(car, subtitle) {
        const depNum = Math.round(car.priceNum * 0.3);
        const shortRange = car.range.replace(/\s*\(.*?\)/g, "");
        const shortPower = car.power.split("(")[0].trim();
        const shortCharge = car.charge.replace(/Sạc (siêu )?nhanh( công suất cao)?\s*/i, "").split("(")[0].trim();
        return `
        <div class="ecs-vhero">
            <div class="ecs-vhero-head">
                <div>
                    <span class="ecs-vhero-badge">${escapeHtml(car.segment)}</span>
                    <h4 class="ecs-vhero-title">VinFast ${escapeHtml(car.modelName)}</h4>
                    ${subtitle ? `<div style="font-size:10.5px;color:#858a92;margin-top:1px;">${escapeHtml(subtitle)}</div>` : ""}
                </div>
                <div class="ecs-vhero-pricebox">
                    <span class="ecs-vhero-price">${escapeHtml(car.price.split('(')[0].trim())}</span>
                    <span class="ecs-vhero-dep">Cọc 30%: <b>${formatVnd(depNum)}</b></span>
                </div>
            </div>
            <div class="ecs-vhero-metrics">
                <div class="ecs-vmetric">
                    <span class="m-icon">🔋</span>
                    <span class="m-label">Quãng đường</span>
                    <span class="m-val">${escapeHtml(shortRange)}</span>
                    <span class="m-sub">Pin ${escapeHtml(car.battery.split('(')[0].trim())}</span>
                </div>
                <div class="ecs-vmetric">
                    <span class="m-icon">⚡</span>
                    <span class="m-label">Công suất</span>
                    <span class="m-val">${escapeHtml(shortPower)}</span>
                    <span class="m-sub">${escapeHtml(car.seats.split('(')[0].trim())}</span>
                </div>
                <div class="ecs-vmetric">
                    <span class="m-icon">⏱</span>
                    <span class="m-label">Sạc nhanh</span>
                    <span class="m-val">${escapeHtml(shortCharge)}</span>
                    <span class="m-sub">Trạm V-GREEN</span>
                </div>
                <div class="ecs-vmetric">
                    <span class="m-icon">🛡</span>
                    <span class="m-label">An toàn</span>
                    <span class="m-val">${escapeHtml(car.airbags.replace(/\s*an toàn.*/i, ""))}</span>
                    <span class="m-sub">Chuẩn VinFast</span>
                </div>
            </div>
            <div class="ecs-vhero-body">
                <strong>Phù hợp:</strong> ${escapeHtml(car.suitability)}
            </div>
            <button type="button" class="ecs-btn-select-main" data-pick-model="${escapeHtml(car.modelName)}">
                ✓ Chọn ${escapeHtml(car.modelName)} để tính cọc &amp; trả góp ở Cột 1 →
            </button>
        </div>`;
    }

    function renderComparisonTable(m1, m2) {
        return `
        <div class="ecs-cmp-wrap">
            <div class="ecs-cmp-title">SO SÁNH NHANH: ${escapeHtml(m1.modelName)} &amp; ${escapeHtml(m2.modelName)}</div>
            <div class="ecs-cmp-table">
                <div class="ecs-cmp-row head">
                    <span class="label">Tiêu chí</span>
                    <span class="col1">${escapeHtml(m1.modelName)}</span>
                    <span class="col2">${escapeHtml(m2.modelName)}</span>
                </div>
                <div class="ecs-cmp-row">
                    <span class="label">Giá tham khảo</span>
                    <span class="col1 text-red">${escapeHtml(m1.price.split('(')[0].trim())}</span>
                    <span class="col2 text-red">${escapeHtml(m2.price.split('(')[0].trim())}</span>
                </div>
                <div class="ecs-cmp-row">
                    <span class="label">Cọc 30%</span>
                    <span class="col1">${formatVnd(m1.priceNum * 0.3)}</span>
                    <span class="col2">${formatVnd(m2.priceNum * 0.3)}</span>
                </div>
                <div class="ecs-cmp-row">
                    <span class="label">Tầm hoạt động</span>
                    <span class="col1"><b>${escapeHtml(m1.range)}</b></span>
                    <span class="col2"><b>${escapeHtml(m2.range)}</b></span>
                </div>
                <div class="ecs-cmp-row">
                    <span class="label">Công suất</span>
                    <span class="col1">${escapeHtml(m1.power.split('(')[0].trim())}</span>
                    <span class="col2">${escapeHtml(m2.power.split('(')[0].trim())}</span>
                </div>
                <div class="ecs-cmp-row">
                    <span class="label">Số chỗ · Túi khí</span>
                    <span class="col1">${escapeHtml(m1.seats.split('(')[0].trim())} · ${escapeHtml(m1.airbags)}</span>
                    <span class="col2">${escapeHtml(m2.seats.split('(')[0].trim())} · ${escapeHtml(m2.airbags)}</span>
                </div>
            </div>
            <div class="ecs-cmp-actions">
                <button type="button" class="ecs-btn-select" data-pick-model="${escapeHtml(m1.modelName)}">Chọn ${escapeHtml(m1.modelName)}</button>
                <button type="button" class="ecs-btn-select" data-pick-model="${escapeHtml(m2.modelName)}">Chọn ${escapeHtml(m2.modelName)}</button>
            </div>
        </div>`;
    }

    async function queryAIConsultation(question, selectedModelName, preloadedBackendAnswer) {
        const normQ = normalizeVn(question);
        const normSelected = normalizeVn(selectedModelName);

        const mentionedInQ = Object.keys(modelProfiles).filter(key => normQ.includes(normalizeVn(key)));

        // 1. SO SÁNH 2 XE: Luôn ưu tiên bảng so sánh trực quan, gọn gàng
        if (normQ.includes("so sanh") || (normQ.includes("va") && mentionedInQ.length >= 2)) {
            const m1Name = mentionedInQ[0] || "VF 5";
            const m2Name = mentionedInQ[1] || (m1Name === "VF 5" ? "VF 6" : "VF 5");
            return renderComparisonTable(modelProfiles[m1Name] || modelProfiles["VF 5"], modelProfiles[m2Name] || modelProfiles["VF 6"]);
        }

        // 2. TẤT CẢ DÒNG XE
        if (normQ.includes("cac dong xe") || normQ.includes("tat ca") || normQ.includes("danh muc")
            || normQ.includes("danh sach") || normQ.includes("co nhung mau")) {
            return renderCatalogGrid("Danh mục các dòng xe điện VinFast:");
        }

        // 3. KẾT HỢP DỮ LIỆU TỪ /api/ai (NẾU CÓ) NHƯNG HIỂN THỊ DẠNG CARD THAY VÌ ĐỐNG CHỮ DÀI
        let apiAnswer = preloadedBackendAnswer || "";
        if (!apiAnswer) {
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
                    body: JSON.stringify({ message: `${selectedModelName ? "[" + selectedModelName + "] " : ""}${question}`, intent: "AUTO" })
                });
                if (res.ok) {
                    const data = await res.json();
                    if (data.answer && !data.refused) {
                        apiAnswer = data.answer;
                    }
                }
            } catch (_) {
                // Chuyển sang bộ tri thức thẻ xe trực quan
            }
        }

        if (normQ.includes("re nhat") || normQ.includes("it tien") || normQ.includes("tiet kiem") || normQ.includes("4 cho")) {
            return renderSingleCarHero(modelProfiles["VF 3"], "Dòng xe tối ưu chi phí nhất trong danh mục");
        }
        if (normQ.includes("7 cho") || normQ.includes("bay cho") || normQ.includes("dong nguoi") || normQ.includes("cao cap nhat") || normQ.includes("xa nhat")) {
            return renderSingleCarHero(modelProfiles["VF 9"], "SUV 7 chỗ cỡ lớn đầu bảng · Pin 123 kWh");
        }

        const matchedFromApi = Object.keys(modelProfiles).filter(k =>
            apiAnswer.toLowerCase().includes(k.toLowerCase())
        );
        const targetKey = mentionedInQ[0]
            || matchedFromApi[0]
            || Object.keys(modelProfiles).find(k => normSelected.includes(normalizeVn(k)))
            || "VF 7";

        const car = modelProfiles[targetKey] || modelProfiles["VF 7"];
        return renderSingleCarHero(car, "Thông số tóm tắt & mức đặt cọc 30%");
    }

    function renderChatBubbles(content, staffName, status) {
        if (!content) return "";
        const lines = content.split("\n").map(l => l.trim()).filter(Boolean);
        const bubbles = [];
        let hasStaffReply = false;

        lines.forEach((line) => {
            const staffMatch = line.match(/^\[(.*?(?:NV|Sales|Tư vấn|Nhân viên).*?)\]:\s*(.*)$/i);
            const customerMatch = line.match(/^\[(?:Khách hàng|Bạn)\]:\s*(.*)$/i);

            if (staffMatch) {
                hasStaffReply = true;
                bubbles.push(`
                    <div class="ecs-bubble-row staff">
                        <span class="ecs-bubble-ava">✦</span>
                        <div class="ecs-bubble">
                            <span class="ecs-bubble-label">Sales Staff (${escapeHtml(staffName || staffMatch[1])})</span>
                            ${escapeHtml(staffMatch[2])}
                        </div>
                    </div>
                `);
            } else if (customerMatch) {
                bubbles.push(`
                    <div class="ecs-bubble-row customer">
                        <span class="ecs-bubble-ava">KH</span>
                        <div class="ecs-bubble">
                            <span class="ecs-bubble-label">Bạn</span>
                            ${escapeHtml(customerMatch[1])}
                        </div>
                    </div>
                `);
            } else {
                bubbles.push(`
                    <div class="ecs-bubble-row customer">
                        <span class="ecs-bubble-ava">KH</span>
                        <div class="ecs-bubble">
                            <span class="ecs-bubble-label">Yêu cầu của bạn</span>
                            ${escapeHtml(line)}
                        </div>
                    </div>
                `);
            }
        });

        if (staffName && !hasStaffReply) {
            bubbles.push(`
                <div class="ecs-bubble-row staff">
                    <span class="ecs-bubble-ava">✦</span>
                    <div class="ecs-bubble">
                        <span class="ecs-bubble-label">Sales Staff (${escapeHtml(staffName)})</span>
                        ${status === 'COMPLETED' ? 'Đã hoàn tất hỗ trợ tư vấn và lập phương án cho yêu cầu này.' : 'Đã tiếp nhận yêu cầu và đang hỗ trợ bạn.'}
                    </div>
                </div>
            `);
        }

        return bubbles.join("");
    }

    document.querySelectorAll("[data-consult-thread]").forEach((threadEl) => {
        const raw = threadEl.textContent || "";
        const staffName = threadEl.dataset.staffName || "";
        const status = (threadEl.dataset.consultStatus || "").toUpperCase();
        const html = renderChatBubbles(raw, staffName, status);
        if (html) {
            threadEl.style.whiteSpace = "normal";
            threadEl.innerHTML = html;
        }
    });

    async function loadPopupConsultations() {
        if (!historyList) return;
        try {
            const res = await fetch(`${contextPath}/consultation?format=json`, {
                credentials: "same-origin",
                headers: { "Accept": "application/json" }
            });
            const data = await readJsonResponse(res);
            isCustomerSession = data.authenticated === true && data.role === "CUSTOMER";
            updateConsultationModeUI(isCustomerSession);

            if (!isCustomerSession) {
                historyList.innerHTML = `
                    <div style="background:#f8fafc;border:1px dashed #cbd5e1;border-radius:8px;padding:14px;text-align:center;">
                        <p style="margin:0 0 8px;font-size:12px;color:#475569;line-height:1.5;">Quý khách cần nhân viên Sales liên hệ báo giá lăn bánh chi tiết hoặc làm thủ tục giữ chỗ xe?</p>
                        <a href="${contextPath}/login" style="display:inline-block;padding:7px 16px;background:#e31837;color:#fff;border-radius:6px;font-weight:600;font-size:12px;text-decoration:none;">Đăng nhập tài khoản Khách hàng →</a>
                    </div>
                `;
                return;
            }

            const items = data.consultations || [];
            if (!items.length) {
                historyList.innerHTML = `<div style="color:#64748b;font-size:12px;text-align:center;padding:12px 0;">Bạn chưa có yêu cầu tư vấn nào. Hãy gửi câu hỏi ở trên để Sales Staff hỗ trợ!</div>`;
                return;
            }
            historyList.innerHTML = items.map(c => {
                const status = String(c.status || "").toUpperCase();
                const canChat = status === "PENDING" || status === "IN_PROGRESS";
                const isCompleted = status === "COMPLETED";

                let badgeClass = "tag-pending";
                let badgeText = "⏳ Chờ Sales tiếp nhận";
                if (status === "IN_PROGRESS") {
                    badgeClass = "tag-progress";
                    badgeText = "💬 Đang tư vấn";
                } else if (status === "COMPLETED") {
                    badgeClass = "tag-completed";
                    badgeText = "✓ Đã hoàn tất";
                } else if (status === "CANCELLED") {
                    badgeClass = "tag-cancelled";
                    badgeText = "✕ Đã hủy";
                }

                return `
                <div class="ecs-item-card ${isCompleted ? 'is-completed' : ''}">
                    <div class="ecs-item-header">
                        <div class="ecs-item-title">
                            <strong>#${escapeHtml(c.id)}</strong>
                            <span>· ${escapeHtml(c.modelName || "Tư vấn chung")}</span>
                        </div>
                        <span class="ecs-tag ${badgeClass}">${badgeText}</span>
                    </div>

                    <div class="ecs-chat-thread">
                        ${renderChatBubbles(c.content, c.staffName, status)}
                    </div>

                    <div class="ecs-item-meta">
                        <span>NV phụ trách: <b>${escapeHtml(c.staffName || "Đang phân công")}</b></span>
                        ${c.createdAt ? `<span>${escapeHtml(c.createdAt)}</span>` : ""}
                    </div>

                    ${canChat ? `
                        <form class="ecs-popup-reply-form" data-request-id="${escapeHtml(c.id)}">
                            <input name="message" maxlength="250" required placeholder="Nhập tin nhắn tiếp theo cho Sales Staff...">
                            <button type="submit" class="button button-primary">Gửi tin</button>
                        </form>
                    ` : (isCompleted ? `
                        <div class="ecs-complete-notice">
                            <span>✓</span> Phiên tư vấn đã được Sales Staff hoàn tất (không thể nhắn thêm).
                        </div>
                    ` : '')}
                </div>`;
            }).join("");

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
            isCustomerSession = false;
            updateConsultationModeUI(false);
            if (/đăng nhập/i.test(error.message)) {
                historyList.innerHTML = `
                    <div style="background:#f8fafc;border:1px dashed #94a3b8;border-radius:8px;padding:12px;text-align:center;">
                        <p style="margin:0 0 8px;font-size:12px;color:#475569;">Bạn cần nhân viên Sales liên hệ báo giá lăn bánh và làm thủ tục giữ chỗ xe?</p>
                        <a href="${contextPath}/login" style="display:inline-block;padding:6px 14px;background:#1456db;color:#fff;border-radius:6px;font-weight:600;font-size:12px;text-decoration:none;">Đăng nhập tài khoản Khách hàng →</a>
                    </div>
                `;
            } else {
                historyList.textContent = error.message || "Không tải được lịch sử tư vấn.";
            }
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
        fullscreenBtn.textContent = full ? "❐ Thu nhỏ" : "⛶ Phóng to";
        fullscreenBtn.title = full ? "Thu nhỏ về cửa sổ góc phải" : "Phóng to toàn màn hình";
    });

    const popupHeader = document.getElementById("ecs-consult-header");
    popupHeader?.addEventListener("dblclick", () => {
        fullscreenBtn?.click();
    });

    closeBtn?.addEventListener("click", () => {
        if (!popup) return;
        popup.hidden = true;
        popup.classList.remove("is-fullscreen");
        window.clearInterval(consultationRefreshTimer);
    });

    // BẮT SỰ KIỆN CLICK VÀO NÚT "CHỌN XE NÀY" TRÊN CÁC THẺ XE TRỰC QUAN
    document.addEventListener("click", (event) => {
        const btn = event.target.closest("[data-pick-model]");
        if (!btn) return;
        const modelName = btn.dataset.pickModel;
        if (!modelName || !calcModel) return;
        const normTarget = normalizeVn(modelName);
        const option = [...calcModel.options].find((opt) => {
            const name = normalizeVn(opt.dataset.name || opt.textContent);
            return name.includes(normTarget) || normTarget.includes(name);
        });
        if (option) {
            calcModel.value = option.value;
            syncConsultationPrice();
            updatePopupCalculation();
            calcModel.scrollIntoView({ behavior: "smooth", block: "nearest" });
            calcModel.style.borderColor = "#e31837";
            calcModel.style.boxShadow = "0 0 0 3px rgba(227, 24, 55, 0.2)";
            setTimeout(() => {
                calcModel.style.borderColor = "";
                calcModel.style.boxShadow = "";
            }, 1200);
            notify(`Đã chọn ${option.textContent.split('·')[0].trim()} ở Cột 1 để tính phương án tài chính!`);
        }
    });

    calcPlan?.addEventListener("change", () => { updateFinanceTerms(); updatePopupCalculation(); });
    calcModel?.addEventListener("change", () => { syncConsultationPrice(); updatePopupCalculation(); });
    [calcMode, calcTerm].forEach((el) => el?.addEventListener("change", updatePopupCalculation));
    calcDownRate?.addEventListener("input", updatePopupCalculation);
    updateFinanceTerms();
    updatePopupCalculation();

    document.querySelectorAll("[data-ai-popup-prompt]").forEach((chip) => {
        chip.addEventListener("click", () => {
            const msgInput = document.getElementById("ecs-consult-message");
            if (msgInput) {
                msgInput.value = chip.dataset.aiPopupPrompt || "";
                msgInput.focus();
                const submitButton = document.getElementById("ecs-consult-submit-btn") || popupForm?.querySelector('button[type="submit"]');
                if (submitButton && !submitButton.disabled) {
                    submitButton.click();
                }
            }
        });
    });

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

        const submitButton = document.getElementById("ecs-consult-submit-btn") || popupForm.querySelector('button[type="submit"]');
        if (!submitButton || submitButton.disabled) return;
        const oldButtonText = submitButton.textContent;

        const aiResultBox = document.getElementById("ecs-ai-consult-result");
        const aiResultText = document.getElementById("ecs-ai-consult-text");

        // 1. NẾU CHƯA ĐĂNG NHẬP: HIỂN THỊ KHUNG CHAT BONG BÓNG + THẺ XE GỌN ĐẸP ĐỒNG BỘ HỆ THỐNG
        if (!isCustomerSession) {
            submitButton.disabled = true;
            submitButton.innerHTML = "Đang tra cứu…";
            msgInput.value = "";
            try {
                const answer = await queryAIConsultation(message, modelName);
                if (aiResultBox && aiResultText) {
                    aiResultText.innerHTML = `
                        <div class="ecs-msg user" style="margin-top:8px;">
                            <span class="ecs-msg-avatar">KH</span>
                            <div class="ecs-msg-bubble">
                                <div>${escapeHtml(message)}</div>
                                <small>Bạn</small>
                            </div>
                        </div>
                        <div class="ecs-msg assistant" style="margin-top:8px;">
                            <span class="ecs-msg-avatar">✳</span>
                            <div class="ecs-msg-bubble">
                                ${answer}
                                <small>Trợ lý VinFast · Thông số tóm tắt</small>
                            </div>
                        </div>
                    `;
                    aiResultBox.hidden = false;
                    aiResultBox.scrollTop = aiResultBox.scrollHeight;
                }
            } catch (err) {
                notify("Hệ thống tra cứu đang bận. Vui lòng thử lại sau.");
            } finally {
                submitButton.disabled = false;
                submitButton.innerHTML = "Gửi <span>↑</span>";
            }
            return;
        }

        // 2. NẾU ĐÃ ĐĂNG NHẬP KHÁCH HÀNG: GỬI LƯU YÊU CẦU CHO SALES STAFF
        if (!modelId) {
            notify("Vui lòng chờ danh mục xe từ kho tải xong rồi gửi yêu cầu.");
            return;
        }
        submitButton.disabled = true;
        submitButton.textContent = "Đang gửi yêu cầu tới Sales Staff...";

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
            notify(data.message || "Đã gửi yêu cầu tư vấn thành công!");
            alert(data.message || "Đã gửi yêu cầu tư vấn thành công! Nhân viên Sales sẽ liên hệ phản hồi sớm.");
            if (data.success) {
                msgInput.value = "";
                await loadPopupConsultations();
            }
        } catch (error) {
            const msg = error.message || "Không thể gửi yêu cầu tư vấn. Vui lòng thử lại.";
            notify(msg);
            alert(msg);
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = oldButtonText;
        }
    });
})();
