"use strict";

(() => {
    const toast = document.querySelector("[data-toast]");
    const contextPath = document.body?.dataset?.contextPath
        || document.querySelector('meta[name="app-context-path"]')?.content || "";
    let isAuthenticated = document.querySelector('meta[name="user-authenticated"]')?.content === "true"
        || document.body?.dataset?.authenticated === "true";
    let toastTimer;

    function notify(message) {
        if (!toast) return;
        toast.textContent = message;
        toast.classList.add("visible");
        window.clearTimeout(toastTimer);
        toastTimer = window.setTimeout(() => toast.classList.remove("visible"), 3600);
    }

    // Modal xác nhận thao tác chung (Confirmation Modal)
    window.showAppConfirmModal = function({
        title = "Xác nhận thao tác",
        message = "Bạn có chắc chắn muốn thực hiện thao tác này?",
        details = [],
        confirmText = "Có, xác nhận",
        cancelText = "Không, quay lại",
        isDanger = false,
        onConfirm = null
    }) {
        return new Promise((resolve) => {
            let dialog = document.getElementById("app-confirm-dialog");
            if (!dialog) {
                dialog = document.createElement("dialog");
                dialog.id = "app-confirm-dialog";
                dialog.className = "app-confirm-dialog";
                document.body.appendChild(dialog);
            }

            let detailsHtml = "";
            if (Array.isArray(details) && details.length > 0) {
                detailsHtml = `
                    <div class="app-confirm-details" style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:12px 14px;margin-top:12px;font-size:13px;display:flex;flex-direction:column;gap:8px;">
                        ${details.map(d => `
                            <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;">
                                <span style="color:#64748b;font-weight:500;">${d.label}:</span>
                                <span style="color:#0f172a;font-weight:600;text-align:right;">${d.value}</span>
                            </div>
                        `).join("")}
                    </div>
                `;
            }

            dialog.innerHTML = `
                <div style="padding:18px 24px;border-bottom:1px solid #e2e8f0;background:#f8fafc;display:flex;align-items:center;justify-content:space-between;">
                    <h3 style="margin:0;font-size:16px;font-weight:700;color:#0f172a;">${title}</h3>
                    <button type="button" class="app-confirm-close" aria-label="Đóng" style="background:transparent;border:none;font-size:22px;line-height:1;color:#64748b;cursor:pointer;padding:2px 6px;border-radius:6px;">&times;</button>
                </div>
                <div style="padding:20px 24px;">
                    <p style="margin:0;font-size:14px;line-height:1.5;color:#334155;">${message}</p>
                    ${detailsHtml}
                </div>
                <div style="display:flex;justify-content:flex-end;gap:10px;padding:16px 24px;border-top:1px solid #e2e8f0;background:#f8fafc;">
                    <button type="button" class="app-confirm-btn-cancel button button-secondary" style="padding:8px 18px;font-size:13px;font-weight:600;border-radius:7px;border:1px solid #cbd5e1;background:#fff;color:#334155;cursor:pointer;">${cancelText}</button>
                    <button type="button" class="app-confirm-btn-confirm button button-primary" style="padding:8px 20px;font-size:13px;font-weight:600;border-radius:7px;border:none;background:${isDanger ? '#dc2626' : '#1e3a8a'};color:#fff;cursor:pointer;">${confirmText}</button>
                </div>
            `;

            const btnCancel = dialog.querySelector(".app-confirm-btn-cancel");
            const btnConfirm = dialog.querySelector(".app-confirm-btn-confirm");
            const btnClose = dialog.querySelector(".app-confirm-close");

            const closeDialog = (val) => {
                dialog.close();
                resolve(val);
            };

            btnCancel.onclick = () => closeDialog(false);
            btnClose.onclick = () => closeDialog(false);
            dialog.oncancel = (e) => {
                e.preventDefault();
                closeDialog(false);
            };

            btnConfirm.onclick = async () => {
                // Chống double-click & double-submit
                btnConfirm.disabled = true;
                btnCancel.disabled = true;
                btnClose.disabled = true;
                const originalText = btnConfirm.textContent;
                btnConfirm.textContent = "Đang xử lý...";

                if (typeof onConfirm === "function") {
                    try {
                        await onConfirm();
                        closeDialog(true);
                    } catch (err) {
                        btnConfirm.disabled = false;
                        btnCancel.disabled = false;
                        btnClose.disabled = false;
                        btnConfirm.textContent = originalText;
                        closeDialog(false);
                    }
                } else {
                    closeDialog(true);
                }
            };

            dialog.showModal();
        });
    };

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

    const profileEditBtn = document.querySelector("[data-profile-edit]");
    const profileEditForm = document.getElementById("profile-edit-form");
    const profileDisplayView = document.getElementById("profile-display-view");
    const cancelEditBtn = document.getElementById("btn-cancel-profile-edit");

    function setProfileEditMode(isEditing) {
        if (!profileEditForm) return;
        profileEditForm.hidden = !isEditing;
        profileEditForm.style.display = isEditing ? "grid" : "none";
        if (profileDisplayView) {
            profileDisplayView.hidden = isEditing;
            profileDisplayView.style.display = isEditing ? "none" : "";
        }
        if (profileEditBtn) {
            profileEditBtn.innerHTML = isEditing ? "✕ Hủy chỉnh sửa" : "Chỉnh sửa <span>↗</span>";
            profileEditBtn.classList.toggle("is-active", isEditing);
        }
        if (isEditing) {
            profileEditForm.querySelector("input")?.focus();
        }
    }

    if (profileEditBtn) {
        profileEditBtn.addEventListener("click", (event) => {
            event.preventDefault();
            const currentlyEditing = !profileEditForm.hidden && profileEditForm.style.display !== "none";
            setProfileEditMode(!currentlyEditing);
        });
    }

    if (cancelEditBtn) {
        cancelEditBtn.addEventListener("click", (event) => {
            event.preventDefault();
            setProfileEditMode(false);
        });
    }

    // Default to view mode on page load
    setProfileEditMode(false);

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
    document.getElementById("customer-password-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        const form = event.currentTarget;
        try {
            const response = await fetch(contextPath + "/api/auth", {method: "POST",
                headers: {"Content-Type": "application/json", "X-CSRF-Token": await ensureCsrf()},
                body: JSON.stringify({action: "changePassword", ...Object.fromEntries(new FormData(form))})});
            const result = await response.json();
            if (!response.ok) throw new Error(result.error || result.message || "Không thể đổi mật khẩu.");
            form.reset();
            window.location.reload();
        } catch (error) { form.querySelector("output").textContent = error.message; }
    });
    let csrfToken = null;

    async function ensureCsrf() {
        if (csrfToken) return csrfToken;
        const domToken = document.querySelector('input[name="csrfToken"]')?.value
            || document.querySelector('meta[name="csrf-token"]')?.content
            || document.getElementById("ecs-consult-csrf")?.value;
        if (domToken && domToken.trim()) {
            csrfToken = domToken.trim();
            return csrfToken;
        }
        try {
            const res = await fetch(`${contextPath}/api/auth`, { credentials: "same-origin" });
            if (res.ok) {
                const data = await res.json();
                if (data.csrfToken) csrfToken = data.csrfToken;
            }
        } catch (_) {}
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
    const calcPriceLabel = document.getElementById("ecs-calc-price-label");
    const calcPromoBadge = document.getElementById("ecs-calc-promo-badge");
    const calcDownRate = document.getElementById("ecs-calc-down-rate");
    const calcSummary = document.getElementById("ecs-calc-summary");
    const calcVersion = document.getElementById("ecs-calc-version");
    const calcColor = document.getElementById("ecs-calc-color");
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
    const formatVnd = (amount) => new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0 }).format(Math.round(amount)) + " VND";
    let popupModelsPromise = null;
    let popupCatalog = {
        models: [],
        versions: [],
        colors: [],
        modelColors: [],
        availableCars: [],
        promotions: {},
        catalogPricesByVersion: {},
        catalogPricesByModel: {}
    };

    function syncVersionsAndColors() {
        if (!calcModel) return;
        const modelId = Number(calcModel.value || calcModel.selectedOptions[0]?.dataset?.modelId || 0);
        
        // 1. Đồng bộ danh sách Phiên bản (CarVersions) theo ModelID
        if (calcVersion) {
            const versionsForModel = (popupCatalog.versions || []).filter(v => Number(v.modelID ?? v.modelId) === modelId);
            if (versionsForModel.length === 0) {
                calcVersion.innerHTML = `<option value="" data-name="Tiêu chuẩn">Tiêu chuẩn</option>`;
            } else {
                calcVersion.innerHTML = versionsForModel.map(v => {
                    const vId = Number(v.versionID ?? v.versionId ?? 0);
                    const vName = v.versionName || "Bản tiêu chuẩn";
                    return `<option value="${vId}" data-name="${escapeHtml(vName)}">${escapeHtml(vName)}</option>`;
                }).join("");
            }
        }

        syncColorsForSelection();
        syncConsultationPrice();
    }

    function syncColorsForSelection() {
        if (!calcModel || !calcColor) return;
        const modelId = Number(calcModel.value || calcModel.selectedOptions[0]?.dataset?.modelId || 0);
        const versionId = Number(calcVersion?.value || 0);

        // Lọc màu sắc catalogue cho mẫu xe này từ ModelColors
        const allowedColorIds = new Set(
            (popupCatalog.modelColors || [])
                .filter(mc => Number(mc.modelID ?? mc.modelId) === modelId)
                .map(mc => Number(mc.colorID ?? mc.colorId))
        );
        let eligibleColors = (popupCatalog.colors || []).filter(col => {
            const cId = Number(col.colorID ?? col.colorId);
            return allowedColorIds.size === 0 || allowedColorIds.has(cId);
        });
        if (eligibleColors.length === 0) {
            eligibleColors = popupCatalog.colors || [];
        }

        if (eligibleColors.length === 0) {
            calcColor.innerHTML = `<option value="" data-name="Tiêu chuẩn">Màu tiêu chuẩn</option>`;
        } else {
            calcColor.innerHTML = eligibleColors.map(col => {
                const cId = Number(col.colorID ?? col.colorId);
                const cName = col.colorName || "Tiêu chuẩn";
                const physicalStock = (popupCatalog.availableCars || []).filter(car => {
                    const matchModel = Number(car.modelId) === modelId;
                    const matchVersion = !versionId || Number(car.versionId) === versionId;
                    const matchColor = Number(car.colorId) === cId;
                    return matchModel && matchVersion && matchColor;
                }).length;
                return `<option value="${cId}" data-name="${escapeHtml(cName)}" data-stock="${physicalStock}">${escapeHtml(cName)}</option>`;
            }).join("");
        }
    }

    function formatDiscountBadge(discountType, discountValue, discountAmount) {
        if (String(discountType || "").toUpperCase() === "PERCENTAGE" && Number(discountValue) > 0) {
            const pct = Number(discountValue);
            return `Giảm ${Number.isInteger(pct) ? pct : pct.toFixed(1).replace(/\.0$/, "")}%`;
        }
        return `Giảm ${formatVnd(discountAmount)}`;
    }

    function syncConsultationPrice() {
        if (!calcModel || !calcPrice) return;
        const modelId = Number(calcModel.value || 0);
        const versionId = Number(calcVersion?.value || 0);
        const colorId = Number(calcColor?.value || 0);

        // Ưu tiên tìm xe vật lý có sẵn khớp lựa chọn (đủ mẫu + phiên bản + màu, hoặc cùng mẫu + phiên bản)
        const cars = popupCatalog.availableCars || [];
        const matchCar = cars.find(car => {
            const m = Number(car.modelId) === modelId;
            const v = !versionId || Number(car.versionId) === versionId;
            const c = !colorId || Number(car.colorId) === colorId;
            return m && v && c;
        }) || cars.find(car => {
            const m = Number(car.modelId) === modelId;
            const v = !versionId || Number(car.versionId) === versionId;
            return m && v;
        });

        let originalPrice = 0;
        let finalPrice = 0;
        let discountAmount = 0;
        let discountType = "";
        let discountValue = 0;
        let promotionName = "";

        if (matchCar && Number(matchCar.originalPrice ?? matchCar.price) > 0) {
            originalPrice = Number(matchCar.originalPrice ?? matchCar.price);
            discountAmount = Number(matchCar.discountAmount || 0);
            discountType = matchCar.discountType || "";
            discountValue = Number(matchCar.discountValue || 0);
            promotionName = matchCar.promotionName || "";
            finalPrice = Number(matchCar.price ?? (originalPrice - discountAmount));
        } else {
            const modelOpt = calcModel.selectedOptions[0];
            originalPrice = Number(
                (versionId && popupCatalog.catalogPricesByVersion?.[versionId])
                || (modelId && popupCatalog.catalogPricesByModel?.[modelId])
                || modelOpt?.dataset?.price
                || 0
            );
            const promo = modelId ? popupCatalog.promotions?.[modelId] : null;
            if (promo && originalPrice > 0) {
                const dv = Number(promo.discountValue || 0);
                const dt = String(promo.discountType || "").toUpperCase();
                if (dv > 0) {
                    discountAmount = dt === "PERCENTAGE"
                        ? Math.round((originalPrice * dv) / 100)
                        : Math.min(originalPrice, Math.round(dv));
                    if (discountAmount > 0) {
                        promotionName = promo.name || "";
                        discountType = dt;
                        discountValue = dv;
                    }
                }
            }
            finalPrice = Math.max(0, originalPrice - discountAmount);
        }

        const hasPromo = discountAmount > 0 && promotionName && originalPrice > 0;
        calcPrice.value = finalPrice > 0 ? formatVnd(finalPrice) : "Liên hệ";
        calcPrice.dataset.rawPrice = String(finalPrice || 0);
        calcPrice.dataset.originalPrice = String(originalPrice || 0);
        calcPrice.dataset.discountAmount = String(discountAmount || 0);
        calcPrice.dataset.discountType = discountType || "";
        calcPrice.dataset.discountValue = String(discountValue || 0);
        calcPrice.dataset.promotionName = promotionName || "";

        if (calcPriceLabel) {
            calcPriceLabel.textContent = hasPromo ? "Giá sau ưu đãi (VND)" : "Giá xe niêm yết (VND)";
        }
        if (calcPromoBadge) {
            if (hasPromo) {
                const badgeText = formatDiscountBadge(discountType, discountValue, discountAmount);
                calcPromoBadge.hidden = false;
                calcPromoBadge.innerHTML = `🎁 <strong>${escapeHtml(promotionName)}</strong> · <span style="display:inline-block;background:#dc2626;color:#fff;padding:1px 6px;border-radius:4px;font-weight:700;">${escapeHtml(badgeText)}</span><br>Giá niêm yết: <del>${formatVnd(originalPrice)}</del> → <strong>Giá sau ưu đãi: ${formatVnd(finalPrice)}</strong>`;
            } else {
                calcPromoBadge.hidden = true;
                calcPromoBadge.innerHTML = "";
            }
        }
    }

    async function loadConsultationModels() {
        if (!calcModel) return;
        if (!popupModelsPromise) {
            popupModelsPromise = (async () => {
                let data = null;
                try {
                    const res = await fetch(`${contextPath}/consultation?format=json`, {
                        credentials: "same-origin",
                        headers: { "Accept": "application/json" }
                    });
                    if (res.ok) {
                        data = await readJsonResponse(res);
                    }
                } catch (e) {
                    console.warn("Lỗi tải /consultation?format=json", e);
                }

                if (!data || !Array.isArray(data.models) || data.models.length === 0) {
                    try {
                        const aiRes = await fetch(`${contextPath}/api/ai`, {
                            credentials: "same-origin",
                            headers: { "Accept": "application/json" }
                        });
                        const aiData = await readJsonResponse(aiRes);
                        if (Array.isArray(aiData.models) && aiData.models.length > 0) {
                            data = {
                                models: aiData.models.map(m => ({ modelID: m.modelId, modelName: m.modelName, basePrice: m.minPrice })),
                                versions: [],
                                colors: [],
                                modelColors: [],
                                availableCars: []
                            };
                        }
                    } catch (aiErr) {
                        console.warn("Lỗi tải fallback từ /api/ai", aiErr);
                    }
                }

                if (!data || !Array.isArray(data.models) || data.models.length === 0) {
                    throw new Error("Danh mục chưa có mẫu xe để tư vấn.");
                }

                if (data && typeof data.authenticated === "boolean") {
                    isAuthenticated = data.authenticated;
                }
                if (data && data.csrfToken) {
                    csrfToken = data.csrfToken;
                }

                popupCatalog.models = data.models || [];
                popupCatalog.versions = data.versions || [];
                popupCatalog.colors = data.colors || [];
                popupCatalog.modelColors = data.modelColors || [];
                popupCatalog.availableCars = data.availableCars || [];
                popupCatalog.promotions = data.promotions || {};
                popupCatalog.catalogPricesByVersion = data.catalogPricesByVersion || {};
                popupCatalog.catalogPricesByModel = data.catalogPricesByModel || {};

                calcModel.innerHTML = popupCatalog.models.map((model) => {
                    const mId = Number(model.modelID ?? model.modelId);
                    const mName = model.modelName || "Mẫu xe";
                    const stock = (popupCatalog.availableCars || []).filter(c => Number(c.modelId) === mId).length;
                    const minPrice = Number(model.basePrice ?? model.inventoryMinPrice ?? popupCatalog.catalogPricesByModel?.[mId] ?? 0);
                    return `<option value="${mId}" data-model-id="${mId}" data-name="${escapeHtml(mName)}" data-price="${minPrice}" data-available="${stock}">${escapeHtml(mName)}</option>`;
                }).join("");

                const preferred = popupCatalog.models.find((m) => /VF\s*8/i.test(m.modelName)) || popupCatalog.models[0];
                calcModel.value = String(preferred.modelID ?? preferred.modelId);

                syncVersionsAndColors();
                updatePopupCalculation();
                return popupCatalog.models;
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

        const modelName = calcModel.selectedOptions[0]?.dataset?.name || calcModel.selectedOptions[0]?.textContent || "Mẫu xe";
        const versionName = calcVersion?.selectedOptions[0]?.dataset?.name || "";
        const colorOpt = calcColor?.selectedOptions[0];
        const colorName = colorOpt?.dataset?.name || "";

        const vehicleDetailStr = [modelName, versionName, colorName].filter(Boolean).join(" · ");

        const price = Number(calcPrice?.dataset?.rawPrice || 0);
        const originalPrice = Number(calcPrice?.dataset?.originalPrice || price);
        const discountAmount = Number(calcPrice?.dataset?.discountAmount || 0);
        const discountType = calcPrice?.dataset?.discountType || "";
        const discountValue = Number(calcPrice?.dataset?.discountValue || 0);
        const promotionName = calcPrice?.dataset?.promotionName || "";
        const hasPromo = discountAmount > 0 && promotionName && originalPrice > 0;
        const discountBadge = hasPromo ? formatDiscountBadge(discountType, discountValue, discountAmount) : "";
        const priceSummaryHtml = hasPromo
            ? `<div style="margin-top:4px;">Giá niêm yết: <del>${formatVnd(originalPrice)}</del> · <span style="color:#b91c1c;font-weight:700;">${escapeHtml(promotionName)} (${escapeHtml(discountBadge)})</span></div>
               <div>Giá sau ưu đãi: <strong style="color:#b91c1c;">${formatVnd(price)}</strong></div>`
            : `<div style="margin-top:4px;">Giá xe: <strong>${price > 0 ? formatVnd(price) : "Liên hệ"}</strong></div>`;

        if (mode === "BANK_TRANSFER") {
            const deposit = price > 0 ? Math.round(price * 0.3) : 0;
            const remaining = price > 0 ? price - deposit : 0;
            calcSummary.innerHTML = `
                <div><strong>${escapeHtml(vehicleDetailStr)}</strong></div>
                ${priceSummaryHtml}
                <div style="margin-top:4px;">Trả thẳng: đặt cọc 30% (${price > 0 ? formatVnd(deposit) : "theo báo giá"}); thanh toán trực tiếp 70% còn lại (${price > 0 ? formatVnd(remaining) : "theo hợp đồng"}) sau khi ký hợp đồng.</div>
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
                const down = Math.round(price * downRate);
                const principal = price - down;
                const interest = principal * (annualRate / 100) * (months / 12);
                const total = principal + interest;
                return `<div style="margin-top:6px;">Trả trước (${Math.round(downRate * 100)}%): <strong>${formatVnd(down)}</strong></div>
                    <div>Khoản vay dự kiến: <strong>${formatVnd(principal)}</strong></div>
                    <div>Lãi phẳng toàn kỳ (${months} tháng, ${annualRate}%/năm): <strong>${formatVnd(interest)}</strong></div>
                    <div>Tổng trả cho đối tác (gốc + lãi): <strong>${formatVnd(total)}</strong></div>
                    <div>Mỗi tháng dự kiến: <strong>${formatVnd(total / months)}</strong> · phần lãi ${formatVnd(interest / months)}</div>`;
            })() : `<div style="margin-top:6px;">Vui lòng chọn mẫu xe và phiên bản để xem bảng tính trả góp tham khảo.</div>`;
            calcSummary.innerHTML = `
                <div><strong>${escapeHtml(vehicleDetailStr)}</strong></div>
                ${priceSummaryHtml}
                <div style="margin-top:4px;">Phương án tham khảo: ${escapeHtml(bank)} · ${months} tháng · ${annualRate}%/năm.</div>
                ${lines}
                <div style="font-size:11px;color:#475569;margin-top:6px;">Ước tính lãi phẳng theo thông tin tham khảo. Sales/đối tác sẽ xác nhận giá, lãi suất và khoản vay thực tế.</div>
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
            if (typeof data.authenticated === "boolean") {
                isAuthenticated = data.authenticated;
            }
            if (data.csrfToken) {
                csrfToken = data.csrfToken;
            }
            const guestNotice = document.getElementById("ecs-guest-consult-notice");
            if (!isAuthenticated) {
                historyList.innerHTML = `<div style="color:#64748b;">Vui lòng đăng nhập để xem yêu cầu tư vấn.</div>`;
                return;
            }
            if (guestNotice) guestNotice.hidden = true;
            const items = data.consultations || [];
            if (!items.length) {
                historyList.innerHTML = `<div style="color:#64748b;">Bạn chưa có yêu cầu tư vấn nào. Hãy gửi câu hỏi ở trên để Sales Staff hỗ trợ!</div>`;
                return;
            }
            const consultBadge = (status) => {
                const st = String(status || "PENDING").toUpperCase();
                if (st === "IN_PROGRESS") return `<span style="background:#dbeafe;color:#1e40af;border:1px solid #93c5fd;padding:2px 8px;border-radius:999px;font-size:11px;font-weight:700;">ĐANG TƯ VẤN</span>`;
                if (st === "COMPLETED") return `<span style="background:#dcfce7;color:#166534;border:1px solid #86efac;padding:2px 8px;border-radius:999px;font-size:11px;font-weight:700;">HOÀN TẤT</span>`;
                if (st === "CANCELLED") return `<span style="background:#fee2e2;color:#991b1b;border:1px solid #fca5a5;padding:2px 8px;border-radius:999px;font-size:11px;font-weight:700;">ĐÃ HỦY</span>`;
                return `<span style="background:#fef3c7;color:#92400e;border:1px solid #fcd34d;padding:2px 8px;border-radius:999px;font-size:11px;font-weight:700;">CHỜ XỬ LÝ</span>`;
            };
            historyList.innerHTML = items.map(c => `
                <div class="ecs-consult-Popup-item">
                    <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;font-weight:700;">
                        <span>#${escapeHtml(c.id)} · ${escapeHtml(c.modelName || "Tư vấn chung")}</span>
                        ${consultBadge(c.status)}
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
                            headers: { "X-CSRF-Token": await ensureCsrf(), "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8", "Accept": "application/json" },
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
                    syncVersionsAndColors();
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
    calcModel?.addEventListener("change", () => { syncVersionsAndColors(); updatePopupCalculation(); });
    calcVersion?.addEventListener("change", () => { syncColorsForSelection(); syncConsultationPrice(); updatePopupCalculation(); });
    calcColor?.addEventListener("change", () => { syncConsultationPrice(); updatePopupCalculation(); });
    [calcMode, calcTerm].forEach((el) => el?.addEventListener("change", updatePopupCalculation));
    calcDownRate?.addEventListener("input", updatePopupCalculation);
    updateFinanceTerms();
    updatePopupCalculation();

    popupForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const guestNotice = document.getElementById("ecs-guest-consult-notice");
        const feedbackEl = document.getElementById("ecs-consult-popup-feedback");
        if (feedbackEl) {
            feedbackEl.style.display = "none";
            feedbackEl.textContent = "";
        }
        if (!isAuthenticated) {
            try {
                const authRes = await fetch(`${contextPath}/api/auth`, {
                    credentials: "same-origin",
                    headers: { "Accept": "application/json" }
                });
                if (authRes.ok) {
                    const authData = await readJsonResponse(authRes);
                    if (authData && authData.user) {
                        isAuthenticated = true;
                        if (authData.csrfToken) csrfToken = authData.csrfToken;
                    }
                }
            } catch (_) {}
        }
        if (!isAuthenticated) {
            if (guestNotice) guestNotice.hidden = false;
            return;
        }
        if (guestNotice) guestNotice.hidden = true;
        const modelName = calcModel?.selectedOptions[0]?.dataset?.name || calcModel?.selectedOptions[0]?.textContent || "";
        const modelId = calcModel?.value || calcModel?.selectedOptions[0]?.dataset?.modelId || "";
        const versionName = calcVersion?.selectedOptions[0]?.dataset?.name || "";
        const versionId = calcVersion?.value || "";
        const colorName = calcColor?.selectedOptions[0]?.dataset?.name || "";
        const colorId = calcColor?.value || "";
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

        const rawPrice = Number(calcPrice?.dataset?.rawPrice || 0);
        const origPrice = Number(calcPrice?.dataset?.originalPrice || rawPrice);
        const discAmount = Number(calcPrice?.dataset?.discountAmount || 0);
        const discType = calcPrice?.dataset?.discountType || "";
        const discValue = Number(calcPrice?.dataset?.discountValue || 0);
        const promoName = calcPrice?.dataset?.promotionName || "";
        const hasPromo = discAmount > 0 && promoName && origPrice > 0;
        const priceDetails = hasPromo
            ? [
                { label: "Giá niêm yết", value: formatVnd(origPrice) },
                { label: "Ưu đãi áp dụng", value: `${promoName} (${formatDiscountBadge(discType, discValue, discAmount)})` },
                { label: "Giá sau ưu đãi", value: formatVnd(rawPrice) }
            ]
            : (rawPrice > 0 ? [{ label: "Giá xe", value: formatVnd(rawPrice) }] : []);

        const confirmed = await window.showAppConfirmModal({
            title: "Xác nhận gửi yêu cầu tư vấn & đặt xe",
            message: "Bạn có chắc chắn muốn gửi yêu cầu tư vấn và đăng ký mẫu xe này tới nhân viên Sales?",
            details: [
                { label: "Mẫu xe quan tâm", value: modelName },
                { label: "Phiên bản", value: versionName || "Tiêu chuẩn" },
                { label: "Màu sắc", value: colorName || "Tiêu chuẩn" },
                ...priceDetails,
                { label: "Phương án thanh toán", value: pref }
            ],
            confirmText: "Có, xác nhận",
            cancelText: "Không, quay lại"
        });
        if (!confirmed) return;

        const submitButton = popupForm.querySelector('button[type="submit"]');
        if (!submitButton || submitButton.disabled) return;
        const oldButtonText = submitButton.textContent;
        submitButton.disabled = true;
        submitButton.textContent = "Đang gửi yêu cầu...";

        const token = await ensureCsrf();
        const body = new URLSearchParams();
        body.set("action", "create");
        body.set("format", "json");
        body.set("model", modelName);
        body.set("modelId", modelId);
        if (versionName) body.set("version", versionName);
        if (versionId) body.set("versionId", versionId);
        if (colorName) body.set("color", colorName);
        if (colorId) body.set("colorId", colorId);
        body.set("paymentPreference", pref);
        body.set("message", message);
        if (token) {
            body.set("csrfToken", token);
        }

        try {
            const res = await fetch(`${contextPath}/consultation`, {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "X-CSRF-Token": token || "",
                    "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                    "Accept": "application/json"
                },
                body: body.toString()
            });
            const data = await readJsonResponse(res);
            if (!data.success) throw new Error(data.message || "Không gửi được yêu cầu tư vấn.");
            notify(data.message || "Đã gửi yêu cầu tư vấn thành công.");
            if (feedbackEl) {
                feedbackEl.style.display = "block";
                feedbackEl.style.background = "#ecfdf5";
                feedbackEl.style.color = "#065f46";
                feedbackEl.style.border = "1px solid #a7f3d0";
                feedbackEl.textContent = "✓ " + (data.message || "Đã gửi yêu cầu tư vấn thành công tới Sales Staff!");
            }
            if (data.success) {
                msgInput.value = "";
                await loadPopupConsultations();
            }
        } catch (error) {
            notify(error.message || "Không thể gửi yêu cầu tư vấn. Vui lòng thử lại.");
            if (feedbackEl) {
                feedbackEl.style.display = "block";
                feedbackEl.style.background = "#fef2f2";
                feedbackEl.style.color = "#991b1b";
                feedbackEl.style.border = "1px solid #fecaca";
                feedbackEl.textContent = "✕ " + (error.message || "Không thể gửi yêu cầu tư vấn. Vui lòng thử lại.");
            }
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = oldButtonText;
        }
    });

    // 1. Xác nhận lưu thay đổi thông tin cá nhân
    if (profileEditForm) {
        profileEditForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (!profileEditForm.reportValidity()) return;
            const name = profileEditForm.customerName?.value || "";
            const phone = profileEditForm.phone?.value || "";
            const address = profileEditForm.address?.value || "";

            const phoneVal = (phone || "").trim();
            if (!/^0[0-9]{9}$/.test(phoneVal)) {
                alert("Số điện thoại không hợp lệ. Phải bắt đầu bằng số 0 và có đúng 10 chữ số (ví dụ: 0912345678).");
                profileEditForm.phone?.focus();
                return;
            }
            const confirmed = await window.showAppConfirmModal({
                title: "Xác nhận lưu thay đổi thông tin cá nhân",
                message: "Bạn có chắc chắn muốn cập nhật các thông tin này vào hồ sơ cá nhân?",
                details: [
                    { label: "Họ và tên", value: name || "—" },
                    { label: "Số điện thoại", value: phone || "—" },
                    { label: "Địa chỉ nhận xe", value: address || "—" }
                ],
                confirmText: "Có, xác nhận",
                cancelText: "Không, quay lại"
            });
            if (confirmed) {
                const submitBtn = profileEditForm.querySelector('button[type="submit"]');
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.textContent = "Đang lưu thay đổi...";
                }
                profileEditForm.submit();
            }
        });
    }

    // 2. Xác nhận gửi OTP ký hợp đồng
    document.querySelectorAll(".form-request-contract-otp").forEach((form) => {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            const cid = form.dataset.contractId || form.querySelector('input[name="contractId"]')?.value;
            const cnum = form.dataset.contractNum || (`#${cid}`);
            const amount = form.dataset.amount || "";
            const confirmed = await window.showAppConfirmModal({
                title: "Xác nhận gửi mã OTP ký hợp đồng",
                message: "Hệ thống sẽ phát hành mã OTP 6 chữ số gửi đến email của bạn để xác thực ký hợp đồng điện tử.",
                details: [
                    { label: "Số hợp đồng", value: cnum },
                    ...(amount ? [{ label: "Giá trị hợp đồng", value: amount }] : [])
                ],
                confirmText: "Có, xác nhận",
                cancelText: "Không, quay lại"
            });
            if (confirmed) {
                const submitBtn = form.querySelector('button[type="submit"]');
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.textContent = "Đang gửi OTP...";
                }
                form.submit();
            }
        });
    });

    // 3. Xác nhận ký hợp đồng bằng OTP
    document.querySelectorAll(".form-sign-contract-otp").forEach((form) => {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            const otpInput = form.querySelector('input[name="otp"]');
            if (!otpInput || !otpInput.value || otpInput.value.length !== 6) {
                return form.reportValidity();
            }
            const cid = form.dataset.contractId || form.querySelector('input[name="contractId"]')?.value;
            const cnum = form.dataset.contractNum || (`#${cid}`);
            const confirmed = await window.showAppConfirmModal({
                title: "Xác nhận ký hợp đồng mua bán xe",
                message: "Sau khi xác thực mã OTP, hợp đồng sẽ chính thức được ký kết (SIGNED) và có giá trị pháp lý ràng buộc.",
                details: [
                    { label: "Số hợp đồng", value: cnum },
                    { label: "Mã OTP", value: otpInput.value }
                ],
                confirmText: "Có, xác nhận",
                cancelText: "Không, quay lại"
            });
            if (confirmed) {
                const submitBtn = form.querySelector('button[type="submit"]');
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.textContent = "Đang ký HĐ...";
                }
                form.submit();
            }
        });
    });

    // 4. Xác nhận gửi yêu cầu tư vấn tại checkout.jsp
    const checkoutPanelForm = document.querySelector(".checkout-panel form.customer-form");
    if (checkoutPanelForm) {
        checkoutPanelForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (!checkoutPanelForm.reportValidity()) return;
            const modelSelect = checkoutPanelForm.querySelector('select[name="modelId"]');
            const modelText = modelSelect?.selectedOptions[0]?.textContent || "";
            const prefSelect = checkoutPanelForm.querySelector('select[name="paymentPreference"]');
            const prefText = prefSelect?.selectedOptions[0]?.textContent || "";
            const confirmed = await window.showAppConfirmModal({
                title: "Xác nhận gửi yêu cầu tư vấn & đặt xe",
                message: "Bạn có chắc chắn muốn gửi yêu cầu tư vấn mẫu xe này tới đội ngũ Sales không?",
                details: [
                    { label: "Mẫu xe quan tâm", value: modelText },
                    { label: "Phương án thanh toán", value: prefText }
                ],
                confirmText: "Có, xác nhận",
                cancelText: "Không, quay lại"
            });
            if (confirmed) {
                const submitBtn = checkoutPanelForm.querySelector('button[type="submit"]');
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.textContent = "Đang gửi yêu cầu...";
                }
                checkoutPanelForm.submit();
            }
        });
    }

    // Global logout confirmation for Customer and Guest views
    document.addEventListener("submit", async function(e) {
        const form = e.target;
        if (!form || !form.action || !form.action.includes("/logout")) return;
        if (form.dataset.confirmedLogout === "true") return;
        e.preventDefault();
        e.stopPropagation();

        const confirmed = await window.showAppConfirmModal({
            title: "Xác nhận đăng xuất",
            message: "Bạn có chắc chắn muốn đăng xuất không?",
            confirmText: "Đăng xuất",
            cancelText: "Hủy",
            isDanger: true
        });
        if (confirmed) {
            form.dataset.confirmedLogout = "true";
            form.submit();
        }
    }, true);
})();
