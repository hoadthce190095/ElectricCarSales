"use strict";

let activeAIView = null;
let aiOperationSequence = 0;

function resetAIState() {
    aiOperationSequence++;
    state.aiHistory = [];
    state.aiDraft = "";
    state.aiNeeds = {};
    state.aiPending = null;
    state.cataloguePrompt = null;
    activeAIView = null;
}

function refreshAIView() {
    if (activeAIView?.current()) {
        activeAIView.render();
    }
}

function currentAIOperation(operation) {
    return (
        state.aiPending === operation &&
        operation.id === aiOperationSequence &&
        operation.sessionGeneration === state.sessionGeneration &&
        operation.history === state.aiHistory
    );
}

async function submitAIRequest(request, retryEntry = null, clearDraft = false) {
    if (state.aiPending) {
        return false;
    }
    request = { ...request, message: String(request.message || "").trim() };
    if (!request.message || request.message.length > 2000) {
        return false;
    }
    const operation = {
        id: ++aiOperationSequence,
        history: state.aiHistory,
        sessionGeneration: state.sessionGeneration,
        kind: "question"
    };
    state.aiPending = operation;
    const reply = retryEntry || {};
    Object.assign(reply, {
        text: "Đang đối chiếu với thông tin trong danh mục xe…",
        pending: true,
        error: false,
        response: null,
        request
    });
    if (!retryEntry) {
        state.aiHistory.push({ user: true, text: request.message }, reply);
    }
    if (clearDraft || (retryEntry && state.aiDraft === request.message)) {
        state.aiDraft = "";
    }
    refreshAIView();
    try {
        const response = await api("ai", { method: "POST", body: request });
        if (!currentAIOperation(operation)) {
            return false;
        }
        Object.assign(reply, { text: response.answer, response, pending: false, error: false });
        delete reply.request;
        return true;
    } catch (error) {
        if (!currentAIOperation(operation)) {
            return false;
        }
        Object.assign(reply, { text: error.message, pending: false, error: true });
        if ((clearDraft || retryEntry) && !state.aiDraft) {
            state.aiDraft = request.message;
        }
        return false;
    } finally {
        if (currentAIOperation(operation)) {
            state.aiPending = null;
            refreshAIView();
            void submitCataloguePrompt();
        }
    }
}

async function resetAIConversation() {
    if (state.aiPending) {
        return;
    }
    const operation = {
        id: ++aiOperationSequence,
        history: state.aiHistory,
        sessionGeneration: state.sessionGeneration,
        kind: "reset"
    };
    state.aiPending = operation;
    refreshAIView();
    try {
        await api("ai?action=reset", { method: "POST", body: {} });
        if (!currentAIOperation(operation)) {
            return;
        }
        state.aiHistory = [];
        operation.history = state.aiHistory;
        state.aiDraft = "";
        state.aiNeeds = {};
        state.cataloguePrompt = null;
        if (activeAIView?.current()) {
            toast("Đã bắt đầu cuộc trò chuyện mới.");
        }
    } catch (error) {
        if (currentAIOperation(operation)) {
            state.aiHistory.push({ text: error.message, error: true, reset: true });
        }
    } finally {
        if (currentAIOperation(operation)) {
            state.aiPending = null;
            refreshAIView();
            void submitCataloguePrompt();
        }
    }
}

async function submitCataloguePrompt() {
    if (state.aiPending || !state.cataloguePrompt || !activeAIView?.current()) {
        return;
    }
    const prompt = state.cataloguePrompt;
    state.cataloguePrompt = null;
    await submitAIRequest({ message: prompt.message, intent: prompt.intent, ...prompt.criteria });
}

function chatMessage(message) {
    const bubble = el("div", { class: `chat-bubble${message.error ? " chat-error" : ""}` }, [
        el("div", { text: message.text })
    ]);
    if (message.error && (message.request || message.reset)) {
        const retry = button(
            message.reset ? "Thử bắt đầu lại" : "Gửi lại câu hỏi",
            () => (message.reset ? resetAIConversation() : submitAIRequest(message.request, message)),
            "button secondary small chat-retry"
        );
        retry.disabled = !!state.aiPending;
        bubble.append(retry);
    }
    if (message.response?.recommendations?.length) {
        bubble.append(
            el(
                "div",
                { class: "recommendation-list" },
                message.response.recommendations.map((r) => vehicleCard(r.vehicle, r.reasons))
            )
        );
    }
    if (message.response?.comparison?.length) {
        bubble.append(comparison(message.response.comparison));
    }
    if (message.response) {
        bubble.append(
            el("div", {
                class: "chat-meta",
                text: `Tư vấn theo dữ liệu danh mục · ${date(message.response.generatedAt)}`
            })
        );
    }
    return el("div", { class: `chat-message ${message.user ? "user" : "assistant"}` }, [
        el("span", { class: "chat-avatar", text: message.user ? "B" : "✧" }),
        bubble
    ]);
}

async function loadAI() {
    if (state.view !== "ai") {
        return;
    }
    const token = beginViewLoad("ai");
    let data;
    try {
        [data] = await Promise.all([api("ai"), loadCatalogueMetadata()]);
    } catch (error) {
        if (isViewCurrent(token)) {
            throw error;
        }
        return;
    }
    if (!isViewCurrent(token)) {
        return;
    }
    state.models = data.models || [];
    const resetButton = button(
        "Cuộc trò chuyện mới",
        resetAIConversation,
        "button secondary"
    );
    $("#page-actions").replaceChildren(
        el("div", { class: "report-controls" }, [
            button("← Danh mục xe", () => showView("catalogue"), "button secondary"),
            resetButton
        ])
    );
    const messages = el("div", {
        class: "chat-messages",
        id: "chat-messages",
        role: "log",
        "aria-label": "Hội thoại tư vấn xe"
    });
    const intro = {
        text: "Chào bạn! Tôi có thể giúp bạn tìm hiểu thông số xe, so sánh mẫu xe và gợi ý theo ngân sách, quãng đường hoặc số chỗ. Bạn đang tìm chiếc xe như thế nào?"
    };
    const textarea = el("textarea", {
        name: "message",
        text: state.aiDraft || "",
        placeholder: "Ví dụ: Tôi cần xe 5 chỗ, ngân sách 800 triệu…",
        rows: 2,
        maxlength: 2000,
        required: true,
        "aria-label": "Câu hỏi tư vấn xe",
        oninput: () => {
            state.aiDraft = textarea.value;
        }
    });
    const send = el("button", { type: "submit", class: "button", text: "Gửi ↑" });
    const sendAI = (message, intent = "AUTO", criteria = {}, clearDraft = false) =>
        submitAIRequest({ message, intent, ...criteria }, null, clearDraft);
    const chatForm = el(
        "form",
        {
            class: "chat-form",
            onsubmit: (event) => {
                event.preventDefault();
                const text = textarea.value.trim();
                if (!text) {
                    return;
                }
                if (!state.aiPending) {
                    notice("");
                    void sendAI(text, "AUTO", {}, true);
                }
            }
        },
        [
            el("div", { class: "chat-input-row" }, [textarea, send]),
            el("p", {
                class: "chat-disclaimer",
                text: "Tư vấn theo dữ liệu danh mục. Trợ lý hỗ trợ thông tin xe; nhân viên showroom tiếp nhận các yêu cầu giao dịch."
            })
        ]
    );
    textarea.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
            event.preventDefault();
            chatForm.requestSubmit();
        }
    });
    const suggestions = [
        "Bạn có những mẫu xe nào?",
        "Tôi cần xe đi làm trong thành phố",
        "Xe nào có quãng đường di chuyển xa?"
    ];
    chatForm.append(
        el(
            "div",
            { class: "suggestions" },
            suggestions.map((text) =>
                button(
                    text,
                    () => {
                        textarea.value = text;
                        state.aiDraft = text;
                        textarea.focus();
                    },
                    "suggestion"
                )
            )
        )
    );
    const chat = el("section", { class: "card" }, [
        el("div", { class: "ai-hero" }, [
            el("span", { class: "ai-symbol", text: "✧" }),
            el("div", {}, [
                el("h2", { text: "Chiếc xe phù hợp, bắt đầu từ bạn." }),
                el("p", { text: `Khám phá ${number(state.models.length)} mẫu xe từ danh mục showroom.` })
            ])
        ]),
        messages,
        chatForm
    ]);
    const recommendationForm = el(
        "form",
        {
            class: "card-body",
            oninput: () => {
                state.aiNeeds = values(recommendationForm);
            },
            onsubmit: (event) => {
                event.preventDefault();
                if (state.aiPending) {
                    return;
                }
                const input = values(recommendationForm);
                state.aiNeeds = input;
                const budget = input.budget === "" ? null : Number(input.budget) * 1000000,
                    minRange = input.minRange === "" ? null : Number(input.minRange),
                    minSeats = input.minSeats === "" ? null : Number(input.minSeats);
                if (budget == null && minRange == null && minSeats == null) {
                    notice("Nhập ít nhất một nhu cầu để nhận gợi ý.");
                    return;
                }
                const parts = [
                    budget != null ? `ngân sách ${number(budget)} VND` : null,
                    minRange != null ? `quãng đường tối thiểu ${number(minRange)} km` : null,
                    minSeats != null ? `ít nhất ${minSeats} chỗ` : null
                ].filter(Boolean);
                sendAI(`Gợi ý xe phù hợp với ${parts.join(", ")}.`, "RECOMMEND", {
                    budget,
                    minRange,
                    minSeats
                });
            }
        },
        [
            field("budget", "Ngân sách tối đa (triệu đồng)", "number", state.aiNeeds?.budget || "", {
                min: 0,
                step: "0.1"
            }),
            field("minRange", "Quãng đường tối thiểu (km)", "number", state.aiNeeds?.minRange || "", {
                min: 0,
                step: 1
            }),
            field("minSeats", "Số chỗ tối thiểu", "number", state.aiNeeds?.minSeats || "", {
                min: 1,
                max: 20,
                step: 1
            }),
            el("button", { class: "button", type: "submit", text: "✧ Gợi ý xe phù hợp" })
        ]
    );
    state.comparedIds = [...new Set(state.comparedIds)].filter((id) => state.models.some((model) => model.modelId === id));
    const selectedCount = el("small", { "aria-live": "polite" });
    const modelChoices = state.models.map((model) => {
        const checkbox = el("input", {
            type: "checkbox",
            name: "models",
            value: model.modelId,
            checked: state.comparedIds.includes(model.modelId),
            onchange: () => {
                if (!selectComparedModel(model.modelId, checkbox.checked)) {
                    checkbox.checked = false;
                    notice("Bạn có thể chọn tối đa 4 mẫu xe để so sánh.");
                } else {
                    notice("");
                }
                updateCompareSelection();
            }
        });
        return el("label", { class: "model-choice" }, [
            checkbox,
            el("span", { text: `${model.brandName || ""} ${model.modelName}`.trim() })
        ]);
    });
    const compareForm = el(
        "form",
        {
            class: "card-body",
            onsubmit: (event) => {
                event.preventDefault();
                if (state.aiPending) {
                    return;
                }
                const modelIds = [...state.comparedIds];
                if (modelIds.length < 2 || modelIds.length > 4) {
                    notice("Chọn từ 2 đến 4 mẫu xe để so sánh.");
                    return;
                }
                const names = state.models.filter((m) => modelIds.includes(m.modelId)).map((m) => m.modelName);
                sendAI(`So sánh ${names.join(" và ")}.`, "COMPARE", { modelIds });
            }
        },
        [
            el("fieldset", { class: "model-choice-group" }, [
                el("legend", { text: "Mẫu xe muốn so sánh" }),
                el("p", { class: "section-note", text: "Chạm để chọn từ 2 đến 4 mẫu xe." }),
                el("div", { class: "model-choice-list" }, modelChoices),
                selectedCount
            ]),
            el("button", { class: "button secondary", type: "submit", text: "So sánh mẫu xe" })
        ]
    );
    function updateCompareSelection() {
        selectedCount.textContent = `${state.comparedIds.length}/4 mẫu xe đã chọn`;
        for (const choice of modelChoices) {
            const checkbox = $("input", choice);
            checkbox.checked = state.comparedIds.includes(Number(checkbox.value));
            choice.classList.toggle("selected", checkbox.checked);
        }
        $("[type=submit]", compareForm).disabled = !!state.aiPending || state.comparedIds.length < 2;
    }
    const side = el("aside", { class: "ai-side" }, [
        card("Nhu cầu của bạn", "Gợi ý dựa trên tiêu chí bạn cung cấp.", recommendationForm),
        card("Đặt xe cạnh nhau", "Đối chiếu giá bán và thông số sản phẩm.", compareForm)
    ]);
    if (!state.models.length) {
        notice("Danh mục chưa có mẫu xe. Cần cập nhật dữ liệu sản phẩm để tư vấn và so sánh.");
    }
    const layout = el("div", { class: "ai-layout" }, [chat, side]);
    $("#content").replaceChildren(layout);
    activeAIView = {
        current: () => isViewCurrent(token) && layout.isConnected,
        render: () => {
            messages.replaceChildren(chatMessage(intro), ...state.aiHistory.map(chatMessage));
            if (state.aiPending?.kind === "reset") {
                messages.append(chatMessage({ text: "Đang bắt đầu cuộc trò chuyện mới…" }));
            }
            messages.setAttribute("aria-busy", String(!!state.aiPending));
            messages.scrollTop = messages.scrollHeight;
            if (textarea.value !== (state.aiDraft || "")) {
                textarea.value = state.aiDraft || "";
            }
            send.disabled = !!state.aiPending;
            resetButton.disabled = !!state.aiPending;
            $("[type=submit]", recommendationForm).disabled = !!state.aiPending || !state.models.length;
            for (const name of ["budget", "minRange", "minSeats"]) {
                const input = $(`[name=${name}]`, recommendationForm);
                const value = state.aiNeeds?.[name] || "";
                if (input.value !== value) {
                    input.value = value;
                }
            }
            updateCompareSelection();
        }
    };
    refreshAIView();
    await submitCataloguePrompt();
}
