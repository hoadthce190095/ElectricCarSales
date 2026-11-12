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
        avatar.textContent = role === "user" ? "KH" : "✳";
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
                    if (pendingP) pendingP.textContent = data.answer;
                    answered = true;
                }
            }
            if (!answered) {
                const fallbackAnswer = await queryAIConsultation(q, "");
                if (pendingP) pendingP.textContent = fallbackAnswer;
            }
        } catch (err) {
            try {
                const fallbackAnswer = await queryAIConsultation(q, "");
                if (pendingP) pendingP.textContent = fallbackAnswer;
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
        const fieldLabel = document.getElementById("ecs-consult-field-label");
        const submitBtn = document.getElementById("ecs-consult-submit-btn");
        const consultMsg = document.getElementById("ecs-consult-message");
        const aiPrompts = document.getElementById("ecs-popup-ai-prompts");
        const staffSection = document.getElementById("ecs-staff-consult-section");
        if (!submitBtn) return;
        if (isCustomer) {
            if (modeBadge) {
                modeBadge.textContent = "Sales Staff";
                modeBadge.className = "ecs-mode-badge is-staff";
            }
            if (cardTitle) cardTitle.textContent = "2. Gửi yêu cầu tư vấn & lập đơn cho Sales Staff";
            if (fieldLabel) fieldLabel.textContent = "Nội dung cần tư vấn (phiên bản, màu xe, yêu cầu lập đơn...):";
            if (consultMsg) consultMsg.placeholder = "Ví dụ: Tôi muốn tư vấn VF 8 Eco màu Trắng, phương án trả thẳng và nhờ Sales lập đơn giúp...";
            if (aiPrompts) aiPrompts.hidden = true;
            if (staffSection) staffSection.hidden = false;
            submitBtn.textContent = "Gửi yêu cầu tới Sales Staff →";
        } else {
            if (modeBadge) {
                modeBadge.textContent = "Tư vấn trực tuyến";
                modeBadge.className = "ecs-mode-badge";
            }
            if (cardTitle) cardTitle.textContent = "2. Tư vấn trực tuyến & Giải đáp thông tin xe";
            if (fieldLabel) fieldLabel.textContent = "Nội dung bạn quan tâm (thông số, pin, giá bán, so sánh...):";
            if (consultMsg) consultMsg.placeholder = "Nhập câu hỏi (ví dụ: Tầm hoạt động VF 7, So sánh VF 5 và VF 6, Xe nào 7 chỗ?)...";
            if (aiPrompts) aiPrompts.hidden = false;
            submitBtn.textContent = "Xem tư vấn & thông số xe →";
        }
    }

    async function queryAIConsultation(question, selectedModelName) {
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
                    return data.answer;
                }
            }
        } catch (e) {
            console.warn("Backend /api/ai không phản hồi, chuyển sang phân tích câu hỏi bằng bộ tri thức VinFast.", e);
        }

        const normQ = normalizeVn(question);
        const normSelected = normalizeVn(selectedModelName);

        const mentionedModels = Object.keys(modelProfiles).filter(key => {
            const k = normalizeVn(key);
            return normQ.includes(k) || normSelected.includes(k);
        });

        // 0. INTENT: DANH SÁCH TẤT CẢ DÒNG XE TRONG DỰ ÁN
        if (normQ.includes("cac dong xe") || normQ.includes("nhung xe") || normQ.includes("du an") || normQ.includes("tat ca xe") || normQ.includes("danh sach xe") || normQ.includes("co nhung mau") || normQ.includes("co xe gi") || normQ.includes("mau xe gi")) {
            return `DANH MỤC CÁC DÒNG XE ĐIỆN VINFAST ĐANG KINH DOANH:

1. VinFast VF 3 (Mini e-SUV - 4 chỗ)
   - Giá niêm yết: 285.000.000 ₫ (Cọc 30%: 85.500.000 ₫)
   - Tầm hoạt động: 215 km/lần sạc · Pin 18.64 kWh · Sạc nhanh 36 phút
   - Đặc điểm: Nhỏ gọn, linh hoạt trong đô thị, chi phí vận hành siêu tiết kiệm.

2. VinFast VF 5 Plus (A-SUV - 5 chỗ)
   - Giá niêm yết: 496.000.000 ₫ (Cọc 30%: 148.800.000 ₫)
   - Tầm hoạt động: 326 km/lần sạc · Pin 37.23 kWh · 6 túi khí
   - Đặc điểm: Không gian rộng rãi, bền bỉ, tối ưu cho gia đình nhỏ và kinh doanh vận tải.

3. VinFast VF 6 (B-SUV thể thao - 5 chỗ)
   - Giá niêm yết: Từ 646.000.000 ₫ (Eco) đến 699.000.000 ₫ (Plus)
   - Tầm hoạt động: 460 - 485 km/lần sạc · Pin 59.6 kWh · ADAS cấp 2
   - Đặc điểm: Phong cách thể thao châu Âu, màn hình 12.9 inch, hỗ trợ lái an toàn.

4. VinFast VF 7 (C-SUV thể thao - 5 chỗ)
   - Giá niêm yết: Từ 740.000.000 ₫ (Eco) đến 830.000.000 ₫ (Plus FWD)
   - Tầm hoạt động: 440 - 500 km/lần sạc · Pin 59.6 - 70.0 kWh · Động cơ đến 349 mã lực
   - Đặc điểm: Thiết kế phi thuyền hiện đại, bứt tốc mạnh mẽ, khoang lái rộng rãi.

5. VinFast VF 8 (D-SUV toàn cầu cao cấp - 5 chỗ)
   - Giá niêm yết: Từ 1.079.000.000 ₫ (Eco) đến 1.259.000.000 ₫ (Plus)
   - Tầm hoạt động: 457 - 471 km/lần sạc · Pin 87.7 kWh · 11 túi khí (chuẩn 5 sao NHTSA)
   - Đặc điểm: Sang trọng, êm ái, trang bị hiển thị kính lái HUD, dẫn động 4 bánh AWD.

6. VinFast VF 9 (E-SUV Full-size hạng sang - 6 hoặc 7 chỗ)
   - Giá niêm yết: Từ 1.566.000.000 ₫ (Eco) đến 1.743.000.000 ₫ (Plus)
   - Tầm hoạt động: 602 - 626 km/lần sạc · Pin khủng 123.0 kWh · 402 mã lực
   - Đặc điểm: Dòng xe 7 chỗ duy nhất trong danh mục, tùy chọn ghế cơ trưởng VIP massage.

Quý khách có thể chọn mẫu xe ở cột bên trái hoặc đặt câu hỏi trực tiếp để được giải đáp chi tiết thông số, giá bán và phương án tài chính.`;
        }

        // INTENT: DÒNG XE RẺ NHẤT / THẤP NHẤT
        if (normQ.includes("re nhat") || normQ.includes("it tien nhat") || normQ.includes("thap nhat") || normQ.includes("tiet kiem nhat")) {
            return `MẪU XE CÓ CHI PHÍ TỐI ƯU NHẤT: VINFAST VF 3
- Giá niêm yết: 285.000.000 ₫ (đã bao gồm pin & VAT).
- Đặt cọc 30%: 85.500.000 ₫; thanh toán 70% còn lại khi nhận xe.
- Thông số chính: Thiết kế 4 chỗ ngồi, tầm hoạt động 215 km/lần sạc, chi phí nạp điện ước tính chỉ ~350 ₫/km.`;
        }

        // INTENT: DÒNG XE CAO CẤP / ĐẮT NHẤT / XA NHẤT
        if (normQ.includes("dat nhat") || normQ.includes("cao cap nhat") || normQ.includes("sang nhat") || normQ.includes("xa nhat") || normQ.includes("pin khung nhat")) {
            return `MẪU XE CAO CẤP & TẦM HOẠT ĐỘNG XA NHẤT: VINFAST VF 9 (E-SUV)
- Giá niêm yết: Từ 1.566.000.000 ₫ (Eco) đến 1.743.000.000 ₫ (Plus).
- Tầm hoạt động: Lên đến 626 km cho 1 lần sạc đầy nhờ cụm pin 123.0 kWh lớn nhất.
- Tiện nghi đẳng cấp: Tùy chọn 6 hoặc 7 chỗ ngồi (ghế cơ trưởng VIP massage, sưởi, thông gió), 11 túi khí, dẫn động AWD 402 mã lực.`;
        }

        // 1. INTENT: SO SÁNH 2 DÒNG XE
        if (normQ.includes("so sanh") || normQ.includes("khac") || normQ.includes("hay") || (normQ.includes("va") && mentionedModels.length >= 2)) {
            const m1Name = mentionedModels[0] || "VF 5";
            const m2Name = mentionedModels[1] || (m1Name === "VF 5" ? "VF 6" : "VF 5");
            const m1 = modelProfiles[m1Name] || modelProfiles["VF 5"];
            const m2 = modelProfiles[m2Name] || modelProfiles["VF 6"];

            return `BẢNG SO SÁNH CHI TIẾT: VINFAST ${m1.modelName} & ${m2.modelName}

1. Phân khúc & Kích thước:
   - ${m1.modelName}: ${m1.segment} (${m1.dimensions}, ${m1.seats}).
   - ${m2.modelName}: ${m2.segment} (${m2.dimensions}, ${m2.seats}).

2. Giá niêm yết & Đặt cọc dự kiến:
   - ${m1.modelName}: ${m1.price} (Cọc 30% ~ ${formatVnd(m1.priceNum * 0.3)}).
   - ${m2.modelName}: ${m2.price} (Cọc 30% ~ ${formatVnd(m2.priceNum * 0.3)}).

3. Pin, Tầm hoạt động & Sạc:
   - ${m1.modelName}: Pin ${m1.battery} · Tầm hoạt động ${m1.range} · ${m1.charge}.
   - ${m2.modelName}: Pin ${m2.battery} · Tầm hoạt động ${m2.range} · ${m2.charge}.

4. Vận hành & An toàn:
   - ${m1.modelName}: ${m1.power} · Túi khí: ${m1.airbags}.
   - ${m2.modelName}: ${m2.power} · Túi khí: ${m2.airbags}.

Khuyến nghị lựa chọn:
- Chọn ${m1.modelName} nếu Quý khách ưu tiên chi phí đầu tư ban đầu thấp, kích thước nhỏ gọn dễ di chuyển trong nội đô.
- Chọn ${m2.modelName} nếu Quý khách cần không gian rộng rãi hơn, công suất động cơ mạnh mẽ và nhiều trang bị an toàn hiện đại cho gia đình.`;
        }

        const targetKey = mentionedModels[0] || Object.keys(modelProfiles).find(k => normSelected.includes(normalizeVn(k))) || "VF 7";
        const car = modelProfiles[targetKey] || modelProfiles["VF 7"];

        // 2. INTENT: HỎI VỀ QUÃNG ĐƯỜNG / PIN / SẠC
        if (normQ.includes("quang duong") || normQ.includes("tam hoat dong") || normQ.includes("km") || normQ.includes("pin") || normQ.includes("sac") || normQ.includes("bao xa")) {
            return `THÔNG TIN PIN & QUÃNG ĐƯỜNG HOẠT ĐỘNG: VINFAST ${car.modelName}

- Quãng đường di chuyển: ${car.range} cho 1 lần sạc đầy (theo điều kiện tiêu chuẩn).
- Dung lượng pin: ${car.battery}.
- Thời gian nạp điện: ${car.charge}.
- Chi phí sạc ước tính: Chỉ khoảng 350 - 450 ₫/km (tiết kiệm hơn 65% so với xe xăng cùng phân khúc).
- Hệ thống trạm sạc: Tương thích với toàn bộ trạm sạc V-GREEN phủ kín 63 tỉnh thành, các trạm dừng nghỉ cao tốc và trung tâm thương mại Vincom.

Khuyến nghị: Với tầm hoạt động ${car.range}, xe đáp ứng hoàn hảo nhu cầu đi lại cả tuần trong đô thị hoặc các chuyến đi liên tỉnh dài ngày mà không cần lo lắng về trạm sạc.`;
        }

        // 3. INTENT: HỎI VỀ GIÁ BÁN / TIỀN / TRẢ GÓP / ĐẶT CỌC
        if (normQ.includes("gia") || normQ.includes("bao nhieu") || normQ.includes("tien") || normQ.includes("trieu") || normQ.includes("ty") || normQ.includes("tra gop") || normQ.includes("dat coc") || normQ.includes("vay")) {
            const deposit = formatVnd(car.priceNum * 0.3);
            const loan = formatVnd(car.priceNum * 0.7);
            return `BÁO GIÁ & PHƯƠNG ÁN THANH TOÁN: VINFAST ${car.modelName}

- Giá niêm yết chính hãng: ${car.price}.
- Phương án Trả thẳng:
  + Đặt cọc 30%: ${deposit}.
  + Thanh toán 70% còn lại (${loan}) khi ký kết hợp đồng mua bán và nhận bàn giao xe.
- Phương án Trả góp ngân hàng:
  + Hỗ trợ vay tối đa 70% giá trị xe (${loan}) trong thời hạn từ 12 đến 60 tháng.
  + Các ngân hàng đối tác liên kết: Techcombank, MB Bank, VPBank, Shinhan Finance.
  + Ước tính gốc + lãi mỗi tháng: từ ${(car.priceNum * 0.7 / 60 / 1000000).toFixed(1)} triệu ₫/tháng.

Chính sách bảo hành: Bảo hành chính hãng từ 7 đến 10 năm (hoặc 160.000 - 200.000 km), cứu hộ miễn phí 24/7 trên toàn quốc.`;
        }

        // 4. INTENT: HỎI VỀ SỐ CHỖ / GIA ĐÌNH
        if (normQ.includes("cho") || normQ.includes("ghe") || normQ.includes("nguoi") || normQ.includes("gia dinh")) {
            return `TƯ VẤN SỐ CHỖ NGỒI CÁC DÒNG XE VINFAST:

1. Dòng xe 4 chỗ:
   - VF 3 (${modelProfiles["VF 3"].price}): Mini e-SUV 4 chỗ, nhỏ gọn linh hoạt, tối ưu cho 1-2 người hoặc đưa đón con nhỏ.

2. Dòng xe 5 chỗ (Phổ biến nhất):
   - VF 5 Plus (${modelProfiles["VF 5"].price}): A-SUV 5 chỗ tiết kiệm, thực dụng cho gia đình nhỏ.
   - VF 6 (${modelProfiles["VF 6"].price}): B-SUV 5 chỗ tiện nghi, phong cách thể thao cho người trẻ.
   - VF 7 (${modelProfiles["VF 7"].price}): C-SUV 5 chỗ hiện đại, khoang lái rộng rãi, cốp xe lớn đi xa rất thoải mái.
   - VF 8 (${modelProfiles["VF 8"].price}): D-SUV 5 chỗ cỡ lớn sang trọng, nội thất cao cấp.

3. Dòng xe 6 - 7 chỗ:
   - VF 9 (${modelProfiles["VF 9"].price}): E-SUV full-size, dòng xe DUY NHẤT có tùy chọn 7 chỗ hoặc 6 chỗ ghế cơ trưởng VIP dành cho gia đình đông người hoặc đón tiếp đối tác.

Đánh giá mẫu ${car.modelName}: Xe thiết kế ${car.seats}, ${car.suitability}`;
        }

        // 5. INTENT: HỎI VỀ AN TOÀN / CÔNG NGHỆ / TÚI KHÍ / ADAS
        if (normQ.includes("an toan") || normQ.includes("tui khi") || normQ.includes("adas") || normQ.includes("cong nghe") || normQ.includes("vivi") || normQ.includes("tro lai")) {
            return `TRANG BỊ AN TOÀN & CÔNG NGHỆ: VINFAST ${car.modelName}

- Trang bị an toàn bị động: ${car.airbags}, khung gầm thép cường lực chịu va đập tiêu chuẩn quốc tế.
- Hệ thống hỗ trợ lái nâng cao ADAS:
  + Cảnh báo chệch làn & Hỗ trợ giữ làn đường.
  + Kiểm soát hành trình thích ứng (Adaptive Cruise Control).
  + Cảnh báo điểm mù & Cảnh báo phương tiện cắt ngang khi lùi.
  + Phanh tự động khẩn cấp phía trước.
- Công nghệ thông minh Smart Services:
  + Trợ lý ảo tiếng Việt Vivi điều khiển bằng giọng nói tự nhiên (điều hòa, định vị, tra cứu).
  + Quản lý xe từ xa qua ứng dụng điện thoại VinFast (kiểm tra pin, khóa/mở cửa, bật điều hòa trước).`;
        }

        // 6. DEFAULT: TỔNG HỢP CHI TIẾT THÔNG SỐ XE ĐANG QUAN TÂM
        return `THÔNG SỐ KỸ THUẬT CHI TIẾT: VINFAST ${car.modelName}

- Phân khúc & Số chỗ: ${car.segment} - ${car.seats}.
- Kích thước tổng thể: ${car.dimensions}.
- Giá niêm yết: ${car.price}.
- Pin & Tầm hoạt động: ${car.battery} · Tầm di chuyển ${car.range} cho 1 lần sạc đầy.
- Thời gian sạc: ${car.charge}.
- Động cơ & Vận hành: ${car.power}.
- Trang bị an toàn: ${car.airbags}.
- Điểm nổi bật: ${car.highlights}

Đánh giá phù hợp:
${car.suitability}

Quý khách có thể nhập thêm câu hỏi cụ thể (ví dụ: "So sánh với VF 6", "Phương án trả góp", hoặc "Thời gian sạc pin") để được giải đáp thêm.`;
    }

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

        // 1. NẾU CHƯA ĐĂNG NHẬP: CHẠY TRỢ LÝ TỰ ĐỘNG TƯ VẤN SẢN PHẨM NGAY
        if (!isCustomerSession) {
            submitButton.disabled = true;
            submitButton.textContent = "Đang tra cứu dữ liệu xe...";
            try {
                const answer = await queryAIConsultation(message, modelName);
                if (aiResultBox && aiResultText) {
                    aiResultText.textContent = answer;
                    aiResultBox.hidden = false;
                    aiResultBox.scrollIntoView({ behavior: "smooth", block: "nearest" });
                }
            } catch (err) {
                notify("Hệ thống tra cứu đang bận. Vui lòng thử lại sau.");
            } finally {
                submitButton.disabled = false;
                submitButton.textContent = "Hỏi thêm thông tin khác →";
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
